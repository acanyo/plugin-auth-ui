package com.xhhao.authui.template;

import com.xhhao.authui.util.SafeUrl;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Explains what the renderer will ignore or change in a design, without rejecting it.
 *
 * <p>Rendering never fails on a bad value: invalid colours are dropped, unsafe HTML is removed
 * and long code is cut. That keeps sign-in working, but silently. Agents and the console use
 * these warnings to fix a design before it goes live.</p>
 */
public final class DesignLinter {
    private static final Set<String> KEPT_AT_RULES =
        Set.of("media", "supports", "keyframes", "-webkit-keyframes", "font-face");
    private static final Pattern AT_RULE = Pattern.compile("@([a-zA-Z-]+)");
    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private DesignLinter() {
    }

    public static List<String> lint(AuthTemplate.Spec spec) {
        var warnings = new ArrayList<String>();
        if (spec == null) {
            warnings.add("The design is empty.");
            return warnings;
        }
        theme(spec.getTheme(), warnings::add);
        var split = ThemeCompiler.attributes(spec.getTheme()).get("layout").startsWith("split");
        var slots = spec.getSlots();
        if (slots != null) {
            if (!split && notBlank(slots.getAside())) {
                warnings.add("slots.aside is only shown in the split and split-reverse layouts.");
            }
            slot("slots.aside", slots.getAside(), warnings::add);
            slot("slots.header", slots.getHeader(), warnings::add);
            slot("slots.footer", slots.getFooter(), warnings::add);
        }
        if (!split && (notBlank(spec.getLoginSlogan()) || notBlank(spec.getSignupSlogan()))) {
            warnings.add("loginSlogan and signupSlogan are only shown on the brand panel of split layouts.");
        }
        var code = spec.getCode();
        if (code != null) {
            css("code.css", code.getCss(), warnings::add);
            if (code.getJs() != null && code.getJs().length() > ScriptGuard.MAX_LENGTH) {
                warnings.add("code.js is longer than " + ScriptGuard.MAX_LENGTH + " characters and will be cut.");
            }
        }
        return warnings;
    }

    private static void theme(AuthTemplate.Theme theme, Consumer<String> warn) {
        if (theme == null) {
            return;
        }
        contrast("theme.text", theme.getText(), "theme.surface", theme.getSurface(), 4.5, warn);
        contrast("theme.muted", theme.getMuted(), "theme.surface", theme.getSurface(), 4.5, warn);
        contrast("theme.onPrimary", theme.getOnPrimary(), "theme.primary", theme.getPrimary(), 4.5, warn);
        color("theme.primary", theme.getPrimary(), warn);
        color("theme.onPrimary", theme.getOnPrimary(), warn);
        color("theme.surface", theme.getSurface(), warn);
        color("theme.text", theme.getText(), warn);
        color("theme.muted", theme.getMuted(), warn);
        color("theme.border", theme.getBorder(), warn);
        color("theme.field", theme.getField(), warn);
        if (theme.getFont() != null && !ThemeCompiler.isFont(theme.getFont())) {
            warn.accept("theme.font contains characters that are not allowed and will be ignored.");
        }
        if (theme.getVariables() != null) {
            theme.getVariables().forEach((name, value) -> {
                if (!ThemeCompiler.isVariable(name, value)) {
                    warn.accept("theme.variables." + name + " is ignored: names must look like --my-name"
                        + " and values may only use letters, digits, spaces and # % . , ( ) / + _ -.");
                }
            });
        }
        var background = theme.getBackground();
        if (background != null) {
            color("theme.background.color", background.getColor(), warn);
            color("theme.background.color2", background.getColor2(), warn);
            color("theme.background.color3", background.getColor3(), warn);
            url("theme.background.image", background.getImage(), warn);
            var type = background.getType() == null ? "" : background.getType();
            if (type.equals("solid") && ThemeCompiler.color(background.getColor()) == null) {
                warn.accept("A solid background needs theme.background.color.");
            }
            if (type.equals("gradient") && (ThemeCompiler.color(background.getColor()) == null
                || ThemeCompiler.color(background.getColor2()) == null)) {
                warn.accept("A gradient background needs theme.background.color and color2.");
            }
            if (type.equals("image") && SafeUrl.of(background.getImage()).isEmpty()) {
                warn.accept("An image background needs theme.background.image.");
            }
        }
        var brand = theme.getBrand();
        if (brand != null) {
            color("theme.brand.background", brand.getBackground(), warn);
            url("theme.brand.image", brand.getImage(), warn);
        }
    }

    /** Reports the tags and attributes the sanitiser removes from a slot. */
    private static void slot(String field, String html, Consumer<String> warn) {
        if (!notBlank(html)) {
            return;
        }
        if (html.length() > SlotSanitizer.MAX_LENGTH) {
            warn.accept(field + " is longer than " + SlotSanitizer.MAX_LENGTH + " characters and will be cut.");
        }
        var kept = parts(SlotSanitizer.clean(html));
        var removed = new LinkedHashSet<>(parts(html));
        removed.removeAll(kept);
        for (var part : removed) {
            warn.accept(field + ": " + part + " is removed when the design is rendered.");
        }
    }

    /** Tags as {@code <tag>} and attributes as {@code <tag attr>}, e.g. {@code <div onclick>}. */
    private static Set<String> parts(String html) {
        var parts = new LinkedHashSet<String>();
        for (Element element : Jsoup.parseBodyFragment(html).body().getAllElements()) {
            if (element.tagName().equals("body")) {
                continue;
            }
            var tag = element.tagName().toLowerCase(Locale.ROOT);
            parts.add("<" + tag + ">");
            element.attributes().forEach(attribute ->
                parts.add("<" + tag + " " + attribute.getKey().toLowerCase(Locale.ROOT) + ">"));
        }
        return parts;
    }

    private static void css(String field, String css, Consumer<String> warn) {
        if (!notBlank(css)) {
            return;
        }
        if (css.length() > CssScoper.MAX_LENGTH) {
            warn.accept(field + " is longer than " + CssScoper.MAX_LENGTH + " characters and will be cut.");
        }
        var matcher = AT_RULE.matcher(CSS_COMMENT.matcher(css).replaceAll(""));
        var dropped = new LinkedHashSet<String>();
        while (matcher.find()) {
            var name = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!KEPT_AT_RULES.contains(name)) {
                dropped.add("@" + name);
            }
        }
        dropped.forEach(rule -> warn.accept(field + ": " + rule + " rules are dropped; only @media, @supports,"
            + " @keyframes and @font-face are kept."));
    }

    private static void color(String field, String value, Consumer<String> warn) {
        if (notBlank(value) && ThemeCompiler.color(value) == null) {
            warn.accept(field + " \"" + value + "\" is not a colour and will be ignored"
                + " (use #hex, rgb(), hsl(), oklch(), color-mix() or a colour name).");
        }
    }

    /** Text must stay readable (WCAG AA). Translucent or unparsable colours are not judged. */
    private static void contrast(String field, String foreground, String backgroundField, String background,
        double minimum, Consumer<String> warn) {
        ColorContrast.ratio(foreground, background).filter(ratio -> ratio < minimum).ifPresent(ratio ->
            warn.accept(String.format(Locale.ROOT, "%s on %s has a contrast of %.1f:1; readable text needs %.1f:1.",
                field, backgroundField, ratio, minimum)));
    }

    private static void url(String field, String value, Consumer<String> warn) {
        if (notBlank(value) && SafeUrl.of(value).isEmpty()) {
            warn.accept(field + " must be a site path such as /upload/a.png or an http(s) URL without quotes.");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
