package com.xhhao.authui.template;

import com.xhhao.authui.util.SafeUrl;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns a design's visual settings into CSS custom properties and data attributes.
 *
 * <p>Every value is validated against its type (colour, length, keyword) so the output can be
 * written into a style element without escaping concerns. Invalid values are dropped and the
 * base look in {@code auth/page.css} applies.</p>
 */
public final class ThemeCompiler {
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{3,8}");
    private static final Pattern FUNCTION_COLOR =
        Pattern.compile("(rgb|rgba|hsl|hsla|oklch|oklab|color-mix)\\([0-9a-zA-Z.,%/ #+-]{1,120}\\)");
    private static final Pattern NAMED_COLOR = Pattern.compile("[a-zA-Z]{3,20}");
    private static final Pattern FONT = Pattern.compile("[0-9a-zA-Z\\u4e00-\\u9fa5 ,'\"_-]{1,200}");
    private static final Pattern VARIABLE_NAME = Pattern.compile("--[a-zA-Z][a-zA-Z0-9-]{0,40}");
    private static final Pattern VARIABLE_VALUE = Pattern.compile("[0-9a-zA-Z#%.,()/ +_-]{1,200}");
    // Allowed keywords; the MCP tool schema and the design linter use the same sets.
    public static final Set<String> LAYOUTS = Set.of("centered", "split", "split-reverse", "fullscreen");
    public static final Set<String> SCHEMES = Set.of("light", "dark", "system");
    public static final Set<String> BACKGROUNDS = Set.of("solid", "gradient", "image", "mesh", "aurora");
    public static final Set<String> DECORATIONS = Set.of("orbit", "grid", "waves", "none");
    public static final Set<String> ENTRANCES = Set.of("none", "fade", "rise", "zoom", "slide", "flip");
    public static final Set<String> AMBIENTS = Set.of("none", "gradient", "float", "particles", "aurora");
    public static final Set<String> SPEEDS = Set.of("slow", "normal", "fast");
    public static final Set<String> SHADOW_NAMES = Set.of("none", "soft", "strong");
    public static final int MIN_RADIUS = 0;
    public static final int MAX_RADIUS = 40;
    public static final int MIN_FORM_WIDTH = 300;
    public static final int MAX_FORM_WIDTH = 560;

    private static final Map<String, String> SHADOWS = Map.of(
        "none", "none",
        "soft", "0 24px 80px rgb(15 40 37 / 0.12)",
        "strong", "0 40px 120px rgb(10 25 30 / 0.32)");

    private ThemeCompiler() {
    }

    /** CSS declarations for {@code #auth-ui}, without the selector. */
    public static String variables(AuthTemplate.Theme theme) {
        if (theme == null) {
            return "";
        }
        var vars = new LinkedHashMap<String, String>();
        putColor(vars, "--auth-primary", theme.getPrimary());
        putColor(vars, "--auth-on-primary", theme.getOnPrimary());
        putColor(vars, "--auth-card", theme.getSurface());
        putColor(vars, "--auth-text", theme.getText());
        putColor(vars, "--auth-muted", theme.getMuted());
        putColor(vars, "--auth-border", theme.getBorder());
        putColor(vars, "--auth-field", theme.getField());
        putPixels(vars, "--auth-radius", theme.getRadius(), MIN_RADIUS, MAX_RADIUS);
        putPixels(vars, "--auth-width", theme.getFormWidth(), MIN_FORM_WIDTH, MAX_FORM_WIDTH);
        if (theme.getShadow() != null && SHADOWS.containsKey(theme.getShadow())) {
            vars.put("--auth-shadow", SHADOWS.get(theme.getShadow()));
        }
        if (isFont(theme.getFont())) {
            vars.put("--auth-font", theme.getFont());
        }
        background(vars, theme.getBackground());
        brand(vars, theme.getBrand());
        if (theme.getVariables() != null) {
            theme.getVariables().forEach((name, value) -> {
                if (isVariable(name, value)) {
                    vars.putIfAbsent(name, value);
                }
            });
        }
        var css = new StringBuilder();
        vars.forEach((name, value) -> css.append(name).append(':').append(value).append(';'));
        if ("dark".equals(theme.getColorScheme())) {
            css.append("color-scheme:dark;");
        }
        return css.toString();
    }

    /** Data attributes that select layout, background and animation rules in page.css. */
    public static Map<String, String> attributes(AuthTemplate.Theme theme, String legacyLayout) {
        var attrs = new LinkedHashMap<String, String>();
        var layout = theme != null ? theme.getLayout() : null;
        attrs.put("layout", choice(layout, LAYOUTS,
            "split".equals(legacyLayout) ? "split" : "centered"));
        attrs.put("scheme", choice(theme != null ? theme.getColorScheme() : null, SCHEMES, "light"));
        var background = theme != null ? theme.getBackground() : null;
        attrs.put("bg", choice(background != null ? background.getType() : null, BACKGROUNDS, "default"));
        var brand = theme != null ? theme.getBrand() : null;
        attrs.put("decoration", choice(brand != null ? brand.getDecoration() : null, DECORATIONS, "orbit"));
        var motion = theme != null ? theme.getMotion() : null;
        attrs.put("entrance", choice(motion != null ? motion.getEntrance() : null, ENTRANCES, "rise"));
        attrs.put("ambient", choice(motion != null ? motion.getAmbient() : null, AMBIENTS, "none"));
        attrs.put("stagger", String.valueOf(motion == null || !Boolean.FALSE.equals(motion.getStagger())));
        attrs.put("interactive",
            String.valueOf(motion == null || !Boolean.FALSE.equals(motion.getInteractive())));
        attrs.put("speed", choice(motion != null ? motion.getSpeed() : null, SPEEDS, "normal"));
        return attrs;
    }

    private static void background(Map<String, String> vars, AuthTemplate.Background bg) {
        if (bg == null) {
            return;
        }
        var c1 = color(bg.getColor());
        var c2 = color(bg.getColor2());
        var c3 = color(bg.getColor3());
        var angle = bg.getAngle() == null ? 140 : Math.clamp(bg.getAngle(), 0, 360);
        if (c1 != null) {
            vars.put("--auth-bg-1", c1);
        }
        if (c2 != null) {
            vars.put("--auth-bg-2", c2);
        }
        if (c3 != null) {
            vars.put("--auth-bg-3", c3);
        }
        vars.put("--auth-bg-angle", angle + "deg");
        switch (bg.getType() == null ? "" : bg.getType()) {
            case "solid" -> {
                if (c1 != null) {
                    vars.put("--auth-page", c1);
                }
            }
            case "gradient" -> {
                if (c1 != null && c2 != null) {
                    vars.put("--auth-page", "linear-gradient(" + angle + "deg," + c1 + "," + c2
                        + (c3 != null ? "," + c3 : "") + ")");
                }
            }
            case "image" -> {
                var image = SafeUrl.of(bg.getImage());
                if (!image.isEmpty()) {
                    // SafeUrl rejects quotes, so the value cannot end the CSS string.
                    vars.put("--auth-page-image", "url(\"" + image + "\")");
                }
                var overlay = bg.getOverlay() == null ? 0.25 : Math.clamp(bg.getOverlay(), 0, 0.9);
                vars.put("--auth-page-overlay", String.format(Locale.ROOT, "%.2f", overlay));
                var blur = bg.getBlur() == null ? 0 : Math.clamp(bg.getBlur(), 0, 40);
                vars.put("--auth-page-blur", blur + "px");
            }
            default -> {
                // mesh and aurora use --auth-bg-1..3 directly in page.css.
            }
        }
    }

    private static void brand(Map<String, String> vars, AuthTemplate.Brand brand) {
        if (brand == null) {
            return;
        }
        var background = color(brand.getBackground());
        if (background != null) {
            vars.put("--auth-story", background);
        }
        var image = SafeUrl.of(brand.getImage());
        if (!image.isEmpty()) {
            vars.put("--auth-story-image", "url(\"" + image + "\")");
        }
    }

    private static void putColor(Map<String, String> vars, String name, String value) {
        var color = color(value);
        if (color != null) {
            vars.put(name, color);
        }
    }

    private static void putPixels(Map<String, String> vars, String name, Integer value, int min, int max) {
        if (value != null) {
            vars.put(name, Math.clamp(value, min, max) + "px");
        }
    }

    /** A colour literal, or {@code null} when the value is not a colour. */
    static String color(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        if (HEX.matcher(trimmed).matches() || FUNCTION_COLOR.matcher(trimmed).matches()
            || (NAMED_COLOR.matcher(trimmed).matches()
                && !Set.of("inherit", "initial", "unset", "revert").contains(trimmed.toLowerCase(Locale.ROOT)))) {
            return trimmed;
        }
        return null;
    }

    static boolean isFont(String value) {
        return value != null && FONT.matcher(value).matches();
    }

    /** Whether a custom property is written to the page: a {@code --name} and a plain value. */
    static boolean isVariable(String name, String value) {
        return name != null && value != null && VARIABLE_NAME.matcher(name).matches()
            && VARIABLE_VALUE.matcher(value).matches();
    }

    private static String choice(String value, Set<String> choices, String fallback) {
        return value != null && choices.contains(value) ? value : fallback;
    }
}
