package com.xhhao.authui.template;

import java.util.Map;

/**
 * Everything the page needs from a design, already validated, scoped and sanitised.
 *
 * @param variables CSS declarations for {@code #auth-ui} from the visual settings
 * @param attributes data attributes for layout, background and animations
 * @param css scoped design CSS
 * @param js design JavaScript, run after the page is ready
 */
public record RenderedTemplate(String name, Map<String, String> attributes, String variables,
                               String loginSlogan, String signupSlogan, String asideHtml,
                               String headerHtml, String footerHtml, String css, String js) {
    /** The plugin's own look from {@code auth/page.css}, used without a template. */
    public static final RenderedTemplate BASE = new RenderedTemplate("",
        ThemeCompiler.attributes(null, "centered"), "", "", "", "", "", "", "", "");

    public static RenderedTemplate from(AuthTemplate template) {
        var spec = template.getSpec();
        var slots = spec.getSlots();
        var code = spec.getCode();
        // Designs from the first version kept their CSS in spec.css; both are applied.
        var css = CssScoper.scope(spec.getCss()) + CssScoper.scope(code != null ? code.getCss() : null);
        return new RenderedTemplate(template.getMetadata().getName(),
            ThemeCompiler.attributes(spec.getTheme(), spec.getLayout()),
            ThemeCompiler.variables(spec.getTheme()),
            text(spec.getLoginSlogan()), text(spec.getSignupSlogan()),
            SlotSanitizer.clean(slots != null ? slots.getAside() : null),
            SlotSanitizer.clean(slots != null ? slots.getHeader() : null),
            SlotSanitizer.clean(slots != null ? slots.getFooter() : null),
            css, ScriptGuard.prepare(code != null ? code.getJs() : null));
    }

    public String layout() {
        return attributes.get("layout");
    }

    /** Whether the brand panel is shown beside the form. */
    public boolean isSplit() {
        return layout().startsWith("split");
    }

    public boolean hasAside() {
        return !asideHtml.isEmpty();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
