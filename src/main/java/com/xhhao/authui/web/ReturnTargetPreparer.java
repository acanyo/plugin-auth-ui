package com.xhhao.authui.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

/**
 * Decides where a visitor returns after authenticating, before Halo renders the page.
 *
 * <p>An explicit {@code redirect_uri} wins over the request Halo saved when it asked for
 * authentication; without either the visitor returns home. The safe target is both saved with
 * Spring's request cache (used by password login) and written back to {@code redirect_uri} (kept
 * by the login form and social providers).</p>
 */
@Component
public class ReturnTargetPreparer {
    private static final String HOME = "/";

    private final WebSessionServerRequestCache requestCache = new WebSessionServerRequestCache();

    /**
     * The safe target the visitor asked for, explicitly or through Halo's saved request.
     * Empty when neither exists.
     */
    public Mono<String> requested(ServerWebExchange exchange, boolean embedded) {
        var explicit = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
        var candidate = explicit != null ? Mono.just(explicit)
            : requestCache.getRedirectUri(exchange).map(URI::toString);
        return candidate.map(value -> ReturnTarget.forAuthentication(value, embedded));
    }

    public Mono<ServerWebExchange> prepare(ServerWebExchange exchange) {
        // Browsers mark navigations inside the site modal's iframe with Sec-Fetch-Dest: iframe.
        var embedded = "iframe".equals(exchange.getRequest().getHeaders().getFirst("Sec-Fetch-Dest"));
        return requested(exchange, embedded).defaultIfEmpty(HOME)
            .flatMap(value -> {
                var target = URI.create(value);
                var targetUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                    .replacePath(target.getRawPath()).replaceQuery(target.getRawQuery())
                    .fragment(target.getRawFragment()).build(true).toUri();
                var targetExchange = exchange.mutate().request(builder ->
                    builder.uri(targetUri).headers(headers -> headers.setAccept(List.of(MediaType.TEXT_HTML))))
                    .build();
                var authUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                    .replaceQueryParam("redirect_uri",
                        UriUtils.encodeQueryParam(target.toString(), StandardCharsets.UTF_8))
                    .build(true).toUri();
                var prepared = exchange.mutate().request(builder -> builder.uri(authUri)).build();
                // Use Spring's request-cache API; do not write authentication session attributes.
                return requestCache.saveRequest(targetExchange).thenReturn(prepared);
            });
    }
}
