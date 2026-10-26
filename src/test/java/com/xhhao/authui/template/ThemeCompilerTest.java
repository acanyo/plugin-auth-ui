package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ThemeCompilerTest {

    private static AuthTemplate.Theme theme() {
        return new AuthTemplate.Theme();
    }

    @Test
    void writesValidatedTokens() {
        var theme = theme();
        theme.setPrimary("#e11d48");
        theme.setSurface("rgb(255 255 255 / 0.9)");
        theme.setRadius(16);
        theme.setFormWidth(420);
        theme.setShadow("strong");
        var css = ThemeCompiler.variables(theme);
        assertTrue(css.contains("--auth-primary:#e11d48;"), css);
        assertTrue(css.contains("--auth-card:rgb(255 255 255 / 0.9);"), css);
        assertTrue(css.contains("--auth-radius:16px;"), css);
        assertTrue(css.contains("--auth-width:420px;"), css);
        assertTrue(css.contains("--auth-shadow:"), css);
    }

    @Test
    void dropsValuesThatAreNotColours() {
        var theme = theme();
        theme.setPrimary("red;}body{display:none");
        theme.setText("url(https://evil.example)");
        theme.setMuted("inherit");
        assertEquals("", ThemeCompiler.variables(theme));
    }

    @Test
    void clampsLengths() {
        var theme = theme();
        theme.setRadius(999);
        theme.setFormWidth(10);
        var css = ThemeCompiler.variables(theme);
        assertTrue(css.contains("--auth-radius:40px;"), css);
        assertTrue(css.contains("--auth-width:300px;"), css);
    }

    @Test
    void buildsGradientAndImageBackgrounds() {
        var theme = theme();
        var bg = new AuthTemplate.Background();
        bg.setType("gradient");
        bg.setColor("#111");
        bg.setColor2("#222");
        bg.setAngle(90);
        theme.setBackground(bg);
        assertTrue(ThemeCompiler.variables(theme).contains("--auth-page:linear-gradient(90deg,#111,#222);"));

        bg.setType("image");
        bg.setImage("/upload/bg.jpg");
        bg.setOverlay(0.4);
        var css = ThemeCompiler.variables(theme);
        assertTrue(css.contains("--auth-page-image:url(\"/upload/bg.jpg\");"), css);
        assertTrue(css.contains("--auth-page-overlay:0.40;"), css);
    }

    @Test
    void rejectsUnsafeImageUrls() {
        var theme = theme();
        var bg = new AuthTemplate.Background();
        bg.setType("image");
        bg.setImage("https://evil.example/x.png\");}*{display:none");
        theme.setBackground(bg);
        assertFalse(ThemeCompiler.variables(theme).contains("--auth-page-image"));
    }

    @Test
    void acceptsOnlyCustomPropertyVariables() {
        var theme = theme();
        theme.setVariables(Map.of("--auth-brand-gap", "24px", "color", "red", "--x", "a;b}"));
        var css = ThemeCompiler.variables(theme);
        assertTrue(css.contains("--auth-brand-gap:24px;"), css);
        assertFalse(css.contains("color:red"), css);
        assertFalse(css.contains("--x"), css);
    }

    @Test
    void attributesFallBackToSafeDefaults() {
        var attrs = ThemeCompiler.attributes(null);
        assertEquals("centered", attrs.get("layout"));
        assertEquals("rise", attrs.get("entrance"));
        assertEquals("true", attrs.get("stagger"));

        var theme = theme();
        theme.setLayout("bogus");
        var motion = new AuthTemplate.Motion();
        motion.setEntrance("flip");
        motion.setAmbient("aurora");
        motion.setStagger(false);
        theme.setMotion(motion);
        attrs = ThemeCompiler.attributes(theme);
        assertEquals("centered", attrs.get("layout"));
        assertEquals("flip", attrs.get("entrance"));
        assertEquals("aurora", attrs.get("ambient"));
        assertEquals("false", attrs.get("stagger"));
    }
}
