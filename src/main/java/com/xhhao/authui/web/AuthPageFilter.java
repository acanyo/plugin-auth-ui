package com.xhhao.authui.web;

import com.xhhao.authui.settings.AuthUiSettings;
import com.xhhao.authui.settings.AuthUiSettingsService;
import com.xhhao.authui.template.PreviewDraftStore;
import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RenderingResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import run.halo.app.security.AfterSecurityWebFilter;

/**
 * Routes authentication pages to the plugin's presentation.
 * Runs after CSRF, authorization and authentication. POST /login and POST /logout are never
 * intercepted; Halo authenticates and logs out as usual.
 */
@Component
public class AuthPageFilter implements AfterSecurityWebFilter {
    private static final String COMPLETE_PATH = "/auth-ui/complete";
    // The plugin's console page, where designs are previewed.
    private static final String CONSOLE_PAGE = "/console/auth-ui";

    private final AuthUiSettingsService settings;
    private final AuthPageRenderer renderer;
    private final ReturnTargetPreparer returnTargets;
    private final LogoutPreparer logout;
    private final AuthModalRedirect modalRedirect;
    private final PreviewDraftStore drafts;

    public AuthPageFilter(AuthUiSettingsService settings, AuthPageRenderer renderer,
        ReturnTargetPreparer returnTargets, LogoutPreparer logout, AuthModalRedirect modalRedirect,
        PreviewDraftStore drafts) {
        this.settings = settings;
        this.renderer = renderer;
        this.returnTargets = returnTargets;
        this.logout = logout;
        this.modalRedirect = modalRedirect;
        this.drafts = drafts;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var path = exchange.getRequest().getPath().pathWithinApplication().value();
        var method = exchange.getRequest().getMethod();
        if (method == HttpMethod.GET && path.equals(COMPLETE_PATH)) {
            return complete(exchange);
        }
        if (method == HttpMethod.GET && path.equals(ReturnTarget.LOGGED_OUT_PATH)) {
            return loggedOut(exchange);
        }
        if (method == HttpMethod.GET) {
            var preview = AuthView.fromPreviewPath(path);
            if (preview.isPresent()) {
                return preview(exchange, preview.get());
            }
        }
        var view = AuthView.fromPath(path).filter(value -> handles(value, method));
        if (view.isEmpty()) {
            return chain.filter(exchange);
        }
        return settings.get().flatMap(config -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }
            if (method != HttpMethod.GET || !view.get().opensInModal()) {
                return renderer.render(exchange, view.get(), config, RenderOptions.PAGE,
                    chain.filter(exchange));
            }
            return view.get() == AuthView.LOGOUT
                ? logoutPage(exchange, config, chain)
                : authPage(exchange, view.get(), config, chain);
        });
    }

    /**
     * GET renders every page. POST is restyled only where Halo answers a submitted form with a
     * page (signup and password reset errors); POST /login and /logout stay with Halo.
     */
    private static boolean handles(AuthView view, HttpMethod method) {
        return method == HttpMethod.GET
            || (method == HttpMethod.POST && (view == AuthView.SIGNUP || view == AuthView.PASSWORD_RESET));
    }

    /**
     * Opens the page in the site modal, or renders it. Password reset prepares the return target
     * too: Halo sends it to /login afterwards, and that login then finishes where it started.
     */
    private Mono<Void> authPage(ServerWebExchange exchange, AuthView view, AuthUiSettings config,
        WebFilterChain chain) {
        return modalRedirect.location(exchange, view, config)
            .flatMap(location -> location.isPresent()
                ? modalRedirect.send(exchange, location.get())
                : returnTargets.prepare(exchange).flatMap(prepared ->
                    renderer.render(prepared, view, config, RenderOptions.PAGE,
                        chain.filter(prepared))));
    }

    /**
     * The console and user center confirm logout in their own dialog and then open /logout, so
     * that request submits Halo's logout form right away and lands on the login modal. A logout
     * started on the site shows the confirmation in the modal instead of a separate page.
     */
    private Mono<Void> logoutPage(ServerWebExchange exchange, AuthUiSettings config,
        WebFilterChain chain) {
        var request = exchange.getRequest();
        var confirmedInApp = SameSiteReferer.fromAppPage(request)
            && "document".equals(request.getHeaders().getFirst("Sec-Fetch-Dest"));
        if (confirmedInApp) {
            var prepared = logout.prepare(exchange, config.isModalEnabled());
            return renderer.render(prepared, AuthView.LOGOUT, config, RenderOptions.autoSubmitted(),
                chain.filter(prepared));
        }
        return modalRedirect.location(exchange, AuthView.LOGOUT, config)
            .flatMap(location -> {
                if (location.isPresent()) {
                    return modalRedirect.send(exchange, location.get());
                }
                var prepared = logout.prepare(exchange, false);
                return renderer.render(prepared, AuthView.LOGOUT, config, RenderOptions.PAGE,
                    chain.filter(prepared));
            });
    }

    /** Last step of a modal login: tells the opener page to refresh, or continues top-level. */
    private Mono<Void> complete(ServerWebExchange exchange) {
        var target = ReturnTarget.safe(exchange.getRequest().getQueryParams().getFirst("target"));
        return renderer.write(exchange, signedIn(exchange).flatMap(authenticated -> authenticated
            ? RenderingResponse.create("plugin:auth-ui:complete")
                .header("Cache-Control", "no-store")
                .modelAttribute("authUiTarget", target).build().cast(ServerResponse.class)
            : ServerResponse.temporaryRedirect(URI.create("/login")).build()));
    }

    /** Last step of a modal logout: tells the opener page to refresh, or continues top-level. */
    private Mono<Void> loggedOut(ServerWebExchange exchange) {
        var target = ReturnTarget.safe(exchange.getRequest().getQueryParams().getFirst("target"));
        return renderer.write(exchange, RenderingResponse.create("plugin:auth-ui:logged-out")
            .header("Cache-Control", "no-store")
            .modelAttribute("authUiTarget", target).build().cast(ServerResponse.class));
    }

    /**
     * Renders a design for the console preview: a stored template ({@code ?template=}) or an
     * unsaved draft from the design studio ({@code ?draft=}). Halo redirects signed-in visitors
     * away from /login and /signup, so the preview calls the native handler on a separate path.
     * Forms are inert in preview.
     */
    private Mono<Void> preview(ServerWebExchange exchange, AuthView view) {
        return currentUser(exchange).flatMap(user -> {
            if (user.isEmpty()) {
                var login = UriComponentsBuilder.fromPath(AuthView.LOGIN.path())
                    .queryParam("redirect_uri", CONSOLE_PAGE).build().toUri();
                return renderer.write(exchange, ServerResponse.temporaryRedirect(login).build());
            }
            var nativeExchange = PreviewRequests.nativeExchange(exchange, view);
            var query = exchange.getRequest().getQueryParams();
            var draft = drafts.get(query.getFirst("draft"), user);
            var options = draft.map(RenderOptions::previewDraft)
                .orElseGet(() -> RenderOptions.preview(query.getFirst("template")));
            return settings.get().flatMap(config -> renderer.render(nativeExchange, view, config,
                options, renderer.write(exchange, ServerResponse.notFound().build())));
        });
    }

    private static Mono<String> currentUser(ServerWebExchange exchange) {
        return exchange.getPrincipal().ofType(Authentication.class)
            .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
            .map(Authentication::getName)
            .defaultIfEmpty("");
    }

    private static Mono<Boolean> signedIn(ServerWebExchange exchange) {
        return currentUser(exchange).map(user -> !user.isEmpty());
    }
}
