package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlotSanitizerTest {

    @Test
    void keepsRichMarkupAndAnimationHooks() {
        var html = "<div class=\"auth-chip\" data-animate=\"rise\" data-delay=\"200\">新功能</div>"
            + "<ul class=\"auth-features\"><li>安全</li></ul>";
        assertEquals(html, SlotSanitizer.clean(html));
    }

    @Test
    void keepsInlineSvgIcons() {
        var cleaned = SlotSanitizer.clean(
            "<svg viewBox=\"0 0 24 24\" width=\"20\"><path d=\"M5 12h14\" stroke=\"currentColor\"></path></svg>");
        assertTrue(cleaned.contains("<svg"), cleaned);
        assertTrue(cleaned.contains("d=\"M5 12h14\""), cleaned);
    }

    @Test
    void keepsSitePathsAndHttpsLinks() {
        var cleaned = SlotSanitizer.clean(
            "<img src=\"/upload/hero.png\"><a href=\"https://example.com\">文档</a>");
        assertTrue(cleaned.contains("src=\"/upload/hero.png\""), cleaned);
        assertTrue(cleaned.contains("href=\"https://example.com\""), cleaned);
    }

    @Test
    void removesScriptsHandlersAndForms() {
        var cleaned = SlotSanitizer.clean("<script>alert(1)</script><img src=x onerror=alert(1)>"
            + "<a href=\"javascript:alert(1)\">x</a><form action=\"/login\"><input name=p></form>"
            + "<iframe src=\"https://evil.example\"></iframe>");
        assertFalse(cleaned.contains("script"), cleaned);
        assertFalse(cleaned.contains("onerror"), cleaned);
        assertFalse(cleaned.contains("javascript:"), cleaned);
        assertFalse(cleaned.contains("<form"), cleaned);
        assertFalse(cleaned.contains("<input"), cleaned);
        assertFalse(cleaned.contains("<iframe"), cleaned);
    }

    @Test
    void dropsStylesThatLoadResources() {
        var cleaned = SlotSanitizer.clean("<p style=\"background:url(https://evil.example/x.png)\">a</p>"
            + "<p style=\"color:red\">b</p>");
        assertFalse(cleaned.contains("url("), cleaned);
        assertTrue(cleaned.contains("style=\"color:red\""), cleaned);
    }

    @Test
    void protectsNewTabLinks() {
        var cleaned = SlotSanitizer.clean("<a href=\"https://example.com\" target=\"_blank\">x</a>");
        assertTrue(cleaned.contains("rel=\"noopener noreferrer\""), cleaned);
    }

    @Test
    void ignoresEmptyInput() {
        assertEquals("", SlotSanitizer.clean(null));
        assertEquals("", SlotSanitizer.clean("  "));
    }
}
