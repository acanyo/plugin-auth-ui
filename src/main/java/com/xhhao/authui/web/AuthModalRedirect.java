package com.xhhao.authui.web;

import com.xhhao.authui.settings.AuthUiSettings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

/**
 * Turns a direct visit to /login, /signup or /logout into the site modal, without theme changes.
 *
 * <p>The visitor is sent to a site page with {@code #auth-ui=<auth URL>}. The page loads
 * {@code modal.js} through the theme head, which opens the modal from that fragment. The modal
 * iframe then requests the auth page again, and that request renders the form as usual.</p>
 *
 * <p>Protected pages (console, user center) cannot host the modal. When Halo asks for sign-in
 * there, the modal opens on the home page and continues to the protected page afterwards.</p>
 */
@Component
public class AuthModalRedirect {
    /** Forces the full page, e.g. for the modal's fallback link or browsers without dialogs. */
    static final String PAGE_PARAM = "auth_ui_page";
    static final String FRAGMENT_KEY = "auth-ui=";

    private static final String HOME = "/";
    // Pages without theme head scripts, where the modal cannot open.
    private static final Pattern NON_SITE_PAGE = Pattern.compile(
        "^/(console|uc|system|challenges|password-reset|login|signup|logout|apis?|actuator|plugins|themes|upload|auth-ui)(/|$)");

    private final ReturnTargetPreparer returnTargets;

    public AuthModalRedirect(ReturnTargetPreparer returnTargets) {
        this.returnTargets = returnTargets;
    }

    /** Where to open the modal instead of rendering {@code view}; empty to render the full page. */
    public Mono<Optional<URI>> location(ServerWebExchange exchange, AuthView view,
        AuthUiSettings config) {
        var request = exchange.getRequest();
        if (!config.isModalEnabled() || !isTopLevelNavigation(request)
            || "1".equals(request.getQueryParams().getFirst(PAGE_PARAM))
            // Results of a full-page form (login errors, signup success) stay on the full page.
            || fromFullAuthPage(request)) {
            return Mono.just(Optional.empty());
        }
        var background = background(request);
        if (view == AuthView.LOGOUT) {
            return Mono.just(Optional.of(fragment(background, view.path())));
        }
        return returnTargets.requested(exchange, false)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .map(target -> {
                // A background page that itself requires sign-in would send the visitor back here.
                if (request.getQueryParams().containsKey("authentication_required")
                    && target.filter(value -> samePath(value, background)).isPresent()) {
                    return Optional.<URI>empty();
                }
                return modalLocation(request, background, target);
            });
    }

    private static Optional<URI> modalLocation(ServerHttpRequest request, String background,
        Optional<String> target) {
        try {
            return Optional.of(fragment(background, modalUrl(request, target)));
        } catch (IllegalArgumentException malformedQuery) {
            return Optional.empty();
        }
    }

    public Mono<Void> send(ServerWebExchange exchange, URI location) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(location);
        response.getHeaders().setCacheControl("no-store");
        return response.setComplete();
    }

    /**
     * Only real page loads are redirected. Requests without Fetch Metadata (older browsers, scripts)
     * keep the full page, so a modal iframe can never be redirected into a loop.
     */
    private static boolean isTopLevelNavigation(ServerHttpRequest request) {
        return "document".equals(request.getHeaders().getFirst("Sec-Fetch-Dest"));
    }

    private static boolean fromFullAuthPage(ServerHttpRequest request) {
        return SameSiteReferer.of(request)
            .flatMap(referer -> AuthView.fromPath(referer.getPath()))
            .filter(view -> view != AuthView.LOGOUT)
            .isPresent();
    }

    /** The same-site page the visitor came from, otherwise the home page. */
    private static String background(ServerHttpRequest request) {
        var referer = SameSiteReferer.of(request);
        if (referer.isEmpty()) {
            return HOME;
        }
        var page = URI.create(ReturnTarget.safe(referer.get().toString()));
        if (NON_SITE_PAGE.matcher(page.getPath()).find()) {
            return HOME;
        }
        return page.getRawPath() + (page.getRawQuery() == null ? "" : "?" + page.getRawQuery());
    }

    /** The auth URL the modal loads: the original query (e.g. error messages) plus the target. */
    private static String modalUrl(ServerHttpRequest request, Optional<String> target) {
        // The request path, so the reset token in /password-reset/email/{token} is kept.
        var builder = UriComponentsBuilder.fromPath(request.getPath().pathWithinApplication().value())
            .query(request.getURI().getRawQuery())
            .replaceQueryParam("redirect_uri")
            .replaceQueryParam("authentication_required")
            .replaceQueryParam(PAGE_PARAM);
        target.ifPresent(value ->
            builder.queryParam("redirect_uri", UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)));
        return builder.build(true).toUriString();
    }

    private static URI fragment(String background, String modalUrl) {
        return URI.create(background + "#" + FRAGMENT_KEY
            + UriUtils.encode(modalUrl, StandardCharsets.UTF_8));
    }

    private static boolean samePath(String first, String second) {
        return URI.create(first).getPath().equals(URI.create(second).getPath());
    }
}
