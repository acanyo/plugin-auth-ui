package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DesignLinterTest {

    private static AuthTemplate.Spec spec() {
        var spec = new AuthTemplate.Spec();
        spec.setDisplayName("Test");
        return spec;
    }

    private static boolean mentions(List<String> warnings, String text) {
        return warnings.stream().anyMatch(warning -> warning.contains(text));
    }

    @Test
    void acceptsAValidDesign() {
        var spec = spec();
        var theme = new AuthTemplate.Theme();
        theme.setPrimary("#176b62");
        theme.setLayout("split");
        theme.setVariables(Map.of("--brand-gap", "32px"));
        spec.setTheme(theme);
        var slots = new AuthTemplate.Slots();
        slots.setAside("<p class=\"auth-big\" data-animate=\"rise\" data-delay=\"120\">Hi <a href=\"/about\">us</a></p>");
        spec.setSlots(slots);
        var code = new AuthTemplate.Code();
        code.setCss("@media (max-width: 600px) { h1 { font-size: 20px } }");
        spec.setCode(code);
        assertEquals(List.of(), DesignLinter.lint(spec));
    }

    @Test
    void reportsValuesTheRendererDrops() {
        var spec = spec();
        var theme = new AuthTemplate.Theme();
        theme.setPrimary("red;}body{display:none");
        theme.setVariables(Map.of("color", "red"));
        var background = new AuthTemplate.Background();
        background.setType("gradient");
        background.setColor("#fff");
        theme.setBackground(background);
        spec.setTheme(theme);
        var warnings = DesignLinter.lint(spec);
        assertTrue(mentions(warnings, "theme.primary"), warnings.toString());
        assertTrue(mentions(warnings, "theme.variables.color"), warnings.toString());
        assertTrue(mentions(warnings, "gradient background needs"), warnings.toString());
    }

    @Test
    void reportsRemovedHtml() {
        var spec = spec();
        var slots = new AuthTemplate.Slots();
        slots.setHeader("<div onclick=\"x()\">Hi</div><script>alert(1)</script>");
        spec.setSlots(slots);
        var warnings = DesignLinter.lint(spec);
        assertTrue(mentions(warnings, "<div onclick>"), warnings.toString());
        assertTrue(mentions(warnings, "<script>"), warnings.toString());
    }

    @Test
    void reportsSlotsTheLayoutHides() {
        var spec = spec();
        var slots = new AuthTemplate.Slots();
        slots.setAside("<p>Brand</p>");
        spec.setSlots(slots);
        spec.setLoginSlogan("Welcome");
        var warnings = DesignLinter.lint(spec);
        assertTrue(mentions(warnings, "slots.aside is only shown"), warnings.toString());
        assertTrue(mentions(warnings, "loginSlogan"), warnings.toString());
    }

    @Test
    void reportsDroppedAtRules() {
        var spec = spec();
        var code = new AuthTemplate.Code();
        code.setCss("@import url(https://evil.example/a.css); /* @layer ok */ @keyframes a { to { opacity: 1 } }");
        spec.setCode(code);
        var warnings = DesignLinter.lint(spec);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(mentions(warnings, "@import"), warnings.toString());
    }

    @Test
    void reportsUnreadableText() {
        var spec = spec();
        var theme = new AuthTemplate.Theme();
        theme.setText("#777777");
        theme.setSurface("#888888");
        spec.setTheme(theme);
        var warnings = DesignLinter.lint(spec);
        assertTrue(mentions(warnings, "theme.text on theme.surface has a contrast"), warnings.toString());
    }
}
