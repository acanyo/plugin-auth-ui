package com.xhhao.authui.template;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * WCAG contrast ratio between two opaque colours, for the design linter. Only literal hex and
 * {@code rgb()} colours are understood; anything else (names, color-mix, translucent colours)
 * is skipped rather than guessed.
 */
final class ColorContrast {
    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");
    private static final Pattern RGB = Pattern.compile(
        "rgba?\\(\\s*(\\d{1,3})[\\s,]+(\\d{1,3})[\\s,]+(\\d{1,3})\\s*(?:[,/]\\s*([0-9.]+%?))?\\s*\\)");

    private ColorContrast() {
    }

    /** The contrast ratio (1 to 21), or empty when either colour is not an opaque literal. */
    static Optional<Double> ratio(String foreground, String background) {
        var fg = parse(foreground);
        var bg = parse(background);
        if (fg.isEmpty() || bg.isEmpty()) {
            return Optional.empty();
        }
        var lighter = Math.max(luminance(fg.get()), luminance(bg.get()));
        var darker = Math.min(luminance(fg.get()), luminance(bg.get()));
        return Optional.of((lighter + 0.05) / (darker + 0.05));
    }

    static Optional<int[]> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        var text = value.trim().toLowerCase(Locale.ROOT);
        var hex = HEX.matcher(text);
        if (hex.matches()) {
            var digits = hex.group(1);
            if (digits.length() == 3) {
                digits = "" + digits.charAt(0) + digits.charAt(0) + digits.charAt(1) + digits.charAt(1)
                    + digits.charAt(2) + digits.charAt(2);
            }
            return Optional.of(new int[] {
                Integer.parseInt(digits.substring(0, 2), 16),
                Integer.parseInt(digits.substring(2, 4), 16),
                Integer.parseInt(digits.substring(4, 6), 16)});
        }
        var rgb = RGB.matcher(text);
        if (rgb.matches() && opaque(rgb.group(4))) {
            var channels = new int[] {
                Integer.parseInt(rgb.group(1)), Integer.parseInt(rgb.group(2)), Integer.parseInt(rgb.group(3))};
            for (var channel : channels) {
                if (channel > 255) {
                    return Optional.empty();
                }
            }
            return Optional.of(channels);
        }
        return Optional.empty();
    }

    private static boolean opaque(String alpha) {
        if (alpha == null) {
            return true;
        }
        var value = alpha.endsWith("%")
            ? Double.parseDouble(alpha.substring(0, alpha.length() - 1)) / 100
            : Double.parseDouble(alpha);
        return value >= 0.99;
    }

    private static double luminance(int[] rgb) {
        var linear = new double[3];
        for (var i = 0; i < 3; i++) {
            var channel = rgb[i] / 255.0;
            linear[i] = channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
    }
}
