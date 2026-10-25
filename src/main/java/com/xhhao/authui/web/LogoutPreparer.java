package com.xhhao.authui.web;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Sets where Halo sends the visitor after logging out.
 *
 * <p>Halo's logout page saves {@code redirect_uri} with its request cache and redirects there
 * once the logout form is submitted. A value from the site modal must be the plugin's
 * logged-out page; anything else is reduced to a safe application path.</p>
 */
@Component
public class LogoutPreparer {
    /** After a logout confirmed in the console, Halo's login page shows "logged out" in the modal. */
    static final String AFTER_APP_LOGOUT = "/login?logout";

    public ServerWebExchange prepare(ServerWebExchange exchange, boolean confirmedInApp) {
        var requested = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
        String target;
        if (confirmedInApp) {
            target = AFTER_APP_LOGOUT;
        } else if (requested != null) {
            target = ReturnTarget.forLogout(requested);
        } else {
            // Without a target Halo keeps its default: the login page with a logged-out notice.
            return exchange;
        }
        var uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
            .replaceQueryParam("redirect_uri", UriUtils.encodeQueryParam(target, StandardCharsets.UTF_8))
            .build(true).toUri();
        return exchange.mutate().request(builder -> builder.uri(uri)).build();
    }
}
