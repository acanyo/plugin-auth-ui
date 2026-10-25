package com.xhhao.authui.web;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** The page a navigation came from, trusted only when the browser reports a same-origin request. */
final class SameSiteReferer {
    /** Halo's own applications, which have no theme head scripts and confirm actions themselves. */
    static final Pattern APP_PAGE = Pattern.compile("^/(console|uc)(/|$)");

    private SameSiteReferer() {
    }

    static Optional<URI> of(ServerHttpRequest request) {
        var referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
        if (referer == null || !"same-origin".equals(request.getHeaders().getFirst("Sec-Fetch-Site"))) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(referer));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /** Whether the request came from the console or the user center. */
    static boolean fromAppPage(ServerHttpRequest request) {
        return of(request).map(URI::getPath)
            .filter(path -> APP_PAGE.matcher(path).find())
            .isPresent();
    }
}
