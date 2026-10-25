package com.xhhao.authui.design;

import com.xhhao.authui.template.ThemeCompiler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * JSON Schema of an {@code AuthTemplate} spec, for MCP tool inputs and AI structured output.
 * Keywords and ranges come from {@link ThemeCompiler}, so the schema and the renderer accept the
 * same values.
 */
public final class DesignSchema {
    private static final String COLOR =
        "CSS colour: #hex, rgb(), hsl(), oklch(), oklab(), color-mix() or a colour name.";
    private static final String URL = "Site path such as /upload/a.png, or an http(s) URL. No quotes.";

    private DesignSchema() {
    }

    /** The full design spec. {@code displayName} is required when creating. */
    public static Map<String, Object> spec(boolean requireName) {
        return spec(requireName, true);
    }

    /**
     * The fields an AI assistant may change: no deprecated fields or gallery order, and no
     * JavaScript unless the user allowed it.
     */
    public static Map<String, Object> forAssistant(boolean allowScript) {
        var schema = spec(false, allowScript);
        @SuppressWarnings("unchecked")
        var properties = new LinkedHashMap<>((Map<String, Object>) schema.get("properties"));
        properties.keySet().removeAll(Set.of("css", "layout", "priority"));
        schema.put("properties", properties);
        return schema;
    }

    private static Map<String, Object> spec(boolean requireName, boolean allowScript) {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("displayName", string("Name shown in the console design gallery.", 1, 40));
        properties.put("description", string("One-line summary of the design.", 0, 200));
        properties.put("loginSlogan", string("Brand panel text on the login page (split layouts, no aside slot).", 0, 80));
        properties.put("signupSlogan", string("Brand panel text on the signup page (split layouts, no aside slot).", 0, 80));
        properties.put("priority", Map.of("type", "integer", "description", "Gallery order; lower first."));
        properties.put("theme", theme());
        properties.put("slots", object("HTML placed around Halo's forms. Sanitised: no scripts, event handlers,"
            + " iframes or forms; data-animate and data-delay are kept.", Map.of(
            "aside", string("Brand panel content; replaces the slogan. Only shown in split layouts.", 0, 20000),
            "header", string("Shown above the form title.", 0, 10000),
            "footer", string("Shown below the form.", 0, 10000))));
        var code = new LinkedHashMap<String, Object>();
        code.put("css", string("CSS scoped to #auth-ui. :root/html/body mean the container. Only @media, @supports,"
            + " @keyframes and @font-face at-rules are kept.", 0, 50000));
        if (allowScript) {
            code.put("js", string("JavaScript run on the live login page after it is ready (never in previews)."
                + " Use the AuthUI API; errors are caught.", 0, 50000));
        }
        properties.put("code", object("Free code for anything the theme does not cover.", code));
        properties.put("css", string("Deprecated: CSS of first-version designs. Use code.css.", 0, 20000));
        properties.put("layout", enumOf("Deprecated: use theme.layout.", Set.of("centered", "split")));
        var schema = object("A login page design.", properties);
        if (requireName) {
            schema.put("required", List.of("displayName"));
        }
        return schema;
    }

    private static Map<String, Object> theme() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("primary", color("Primary colour: buttons, links, focus rings."));
        properties.put("onPrimary", color("Text colour on the primary colour."));
        properties.put("surface", color("Card background. Translucent values work with backdrop-filter in code.css."));
        properties.put("text", color("Main text colour."));
        properties.put("muted", color("Secondary text colour."));
        properties.put("border", color("Border colour."));
        properties.put("field", color("Input background colour."));
        properties.put("colorScheme", enumOf("Dark tokens apply for dark (or system in dark mode)"
            + " unless colours are set.", ThemeCompiler.SCHEMES));
        properties.put("radius", integer("Card corner radius in px.", ThemeCompiler.MIN_RADIUS, ThemeCompiler.MAX_RADIUS));
        properties.put("formWidth", integer("Form width in px.", ThemeCompiler.MIN_FORM_WIDTH, ThemeCompiler.MAX_FORM_WIDTH));
        properties.put("shadow", enumOf("Card shadow.", ThemeCompiler.SHADOW_NAMES));
        properties.put("layout", enumOf("centered card; split = brand panel left, form right; split-reverse;"
            + " fullscreen = form as a full-height side column.", ThemeCompiler.LAYOUTS));
        properties.put("font", string("Font family stack, e.g. \"Inter\", \"PingFang SC\", sans-serif.", 0, 200));
        properties.put("background", object("Page background behind the card.", Map.of(
            "type", enumOf("solid uses color; gradient uses color, color2 (and color3) at angle; image uses image;"
                + " mesh and aurora blend color, color2 and color3.", ThemeCompiler.BACKGROUNDS),
            "color", color("First background colour."),
            "color2", color("Second background colour."),
            "color3", color("Third background colour."),
            "angle", integer("Gradient angle in degrees.", 0, 360),
            "image", Map.of("type", "string", "description", URL),
            "overlay", Map.of("type", "number", "minimum", 0, "maximum", 0.9,
                "description", "Darkens an image background."),
            "blur", integer("Blur on an image background in px.", 0, 40))));
        properties.put("brand", object("Brand panel of split layouts.", Map.of(
            "background", color("Brand panel colour."),
            "image", Map.of("type", "string", "description", URL),
            "decoration", enumOf("Animated art on the brand panel.", ThemeCompiler.DECORATIONS))));
        properties.put("motion", object("Animations. All respect prefers-reduced-motion.", Map.of(
            "entrance", enumOf("How the card enters.", ThemeCompiler.ENTRANCES),
            "ambient", enumOf("Animated background effect.", ThemeCompiler.AMBIENTS),
            "stagger", Map.of("type", "boolean", "description", "Reveal heading, fields and links one after another."),
            "interactive", Map.of("type", "boolean", "description", "Lift, glow and press feedback on inputs and buttons."),
            "speed", enumOf("Scales every animation duration.", ThemeCompiler.SPEEDS))));
        properties.put("variables", Map.of(
            "type", "object",
            "description", "Extra CSS custom properties on #auth-ui, e.g. {\"--brand-gap\": \"32px\"}."
                + " Values may only use letters, digits, spaces and # % . , ( ) / + _ -.",
            "propertyNames", Map.of("pattern", "^--[a-zA-Z][a-zA-Z0-9-]{0,40}$"),
            "additionalProperties", Map.of("type", "string", "maxLength", 200)));
        return object("Visual settings, validated before they reach the page.", properties);
    }

    public static Map<String, Object> object(String description, Map<String, ?> properties) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        if (description != null) {
            schema.put("description", description);
        }
        schema.put("properties", properties);
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> string(String description, int min, int max) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "string");
        schema.put("description", description);
        if (min > 0) {
            schema.put("minLength", min);
        }
        schema.put("maxLength", max);
        return schema;
    }

    private static Map<String, Object> integer(String description, int min, int max) {
        return Map.of("type", "integer", "description", description, "minimum", min, "maximum", max);
    }

    private static Map<String, Object> color(String description) {
        return Map.of("type", "string", "maxLength", 160, "description", description + " " + COLOR);
    }

    private static Map<String, Object> enumOf(String description, Set<String> values) {
        // Sorted, so tool listings are stable.
        return Map.of("type", "string", "description", description, "enum", List.copyOf(new TreeSet<>(values)));
    }
}
