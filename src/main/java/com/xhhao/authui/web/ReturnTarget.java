package com.xhhao.authui.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/** A return target is always an application path, never an external origin. */
public final class ReturnTarget {
    private static final String HOME = "/";
    private static final String COMPLETE_PATH = "/auth-ui/complete";
    static final String LOGGED_OUT_PATH = "/auth-ui/logged-out";

    private ReturnTarget() {
    }

    /**
     * Accepts an application path, or an absolute http(s) URL such as the console's
     * {@code window.location.href}. Like Halo's request cache, only the path, query and fragment
     * of an absolute URL are kept, so the result can never leave this site.
     */
    public static String safe(String value) {
        if (value == null || value.contains("\\") || value.chars().anyMatch(Character::isISOControl)) {
            return HOME;
        }
        try {
            var local = applicationPath(URI.create(value));
            if (local == null || !local.startsWith("/") || local.startsWith("//")) {
                return HOME;
            }
            var uri = URI.create(local);
            var decoded = uri.getPath();
            if (decoded == null || decoded.startsWith("//") || decoded.contains("\\")
                || decoded.chars().anyMatch(Character::isISOControl)
                || decoded.equals("/login") || decoded.equals("/signup")
                || decoded.startsWith("/auth-ui/")) {
                return HOME;
            }
            // Non-ASCII characters are percent-encoded so the target can be reused in a URI.
            return uri.toASCIIString();
        } catch (IllegalArgumentException ignored) {
            return HOME;
        }
    }

    /**
     * Resolves where Halo should send the visitor after authenticating.
     *
     * @param embedded whether the authentication page is rendered inside the site modal. Only the
     *     modal may finish on the completion page; a top-level page reusing a callback left in the
     *     session goes straight to the callback's own target instead.
     */
    /**
     * Where Halo sends the visitor after logging out. Inside the site modal this is the
     * plugin's logged-out page, which tells the opener page to refresh.
     */
    public static String forLogout(String value) {
        if (value != null && value.startsWith(LOGGED_OUT_PATH + "?")) {
            try {
                var uri = URI.create(value);
                if (uri.getPath().equals(LOGGED_OUT_PATH) && !value.contains("\\")
                    && value.chars().noneMatch(Character::isISOControl)) {
                    return uri.toASCIIString();
                }
            } catch (IllegalArgumentException ignored) {
                return HOME;
            }
        }
        return safe(value);
    }

    public static String forAuthentication(String value, boolean embedded) {
        if (value != null && value.startsWith(COMPLETE_PATH + "?")) {
            try {
                var uri = URI.create(value);
                if (uri.getPath().equals(COMPLETE_PATH) && !value.contains("\\")
                    && value.chars().noneMatch(Character::isISOControl)) {
                    return embedded ? uri.toASCIIString() : safe(callbackTarget(value));
                }
            } catch (IllegalArgumentException ignored) {
                return HOME;
            }
        }
        return safe(value);
    }

    private static String callbackTarget(String completeUri) {
        var encoded = UriComponentsBuilder.fromUriString(completeUri).build()
            .getQueryParams().getFirst("target");
        return encoded == null ? null : UriUtils.decode(encoded, StandardCharsets.UTF_8);
    }

    private static String applicationPath(URI uri) {
        if (!uri.isAbsolute()) {
            return uri.toString();
        }
        if (!Set.of("http", "https").contains(uri.getScheme().toLowerCase()) || uri.isOpaque()) {
            return null;
        }
        var path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? HOME : uri.getRawPath();
        return path
            + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
            + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
    }
}
