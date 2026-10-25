package com.xhhao.authui.util;

import java.net.URI;
import java.util.Set;

/** URLs that are safe to place in an attribute or a quoted CSS {@code url()}. */
public final class SafeUrl {
    private SafeUrl() {
    }

    /**
     * Returns {@code value} when it is an application path or an absolute http(s) URL, otherwise
     * an empty string. Quotes, backslashes and control characters are rejected so the value can
     * never end the surrounding attribute or CSS string.
     */
    public static String of(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains("\\") || value.contains("\"") || value.contains("'")
            || value.chars().anyMatch(Character::isISOControl)) {
            return "";
        }
        try {
            var uri = URI.create(value);
            if (value.startsWith("/") && !value.startsWith("//")) {
                return value;
            }
            return Set.of("https", "http").contains(uri.getScheme()) && uri.getHost() != null
                ? value : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
