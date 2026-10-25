package com.xhhao.authui.web;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

/** Builds the request Halo's native handler sees for a console preview. */
final class PreviewRequests {
    /** A reset token that never exists, so the reset form renders without a real token. */
    static final String SAMPLE_RESET_TOKEN = "preview";

    private PreviewRequests() {
    }

    /**
     * Rewrites a preview request to the view's own path, dropping the query so Halo's handler
     * cannot save {@code redirect_uri} into the session. Preview-only states are mapped to the
     * query parameters Halo's pages read, e.g. a login error or a sent reset email.
     */
    static ServerWebExchange nativeExchange(ServerWebExchange exchange, AuthView view) {
        var state = exchange.getRequest().getQueryParams().getFirst("state");
        var builder = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
            .replacePath(view.path()).replaceQuery(null);
        if (view == AuthView.LOGIN && "error".equals(state)) {
            builder.queryParam("error", "invalid-credential").queryParam("method", "local");
        } else if (view == AuthView.LOGIN && "local".equals(state)) {
            builder.queryParam("method", "local");
        } else if (view == AuthView.LOGIN && "logout".equals(state)) {
            builder.queryParam("logout");
        }
        var uri = builder.build(true).toUri();
        return exchange.mutate().request(request -> request.uri(uri)).build();
    }
}
