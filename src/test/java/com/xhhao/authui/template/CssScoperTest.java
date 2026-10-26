package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CssScoperTest {

    @Test
    void prefixesEverySelector() {
        assertEquals("#auth-ui .auth-card,#auth-ui h1{color: red;}",
            CssScoper.scope(".auth-card, h1 { color: red; }"));
    }

    @Test
    void mapsRootSelectorsToTheContainer() {
        assertEquals("#auth-ui{--auth-primary:#111;}#auth-ui .x{a:b}#auth-ui[data-view=login]{c:d}",
            CssScoper.scope(":root{--auth-primary:#111;} body .x{a:b} html[data-view=login]{c:d}"));
    }

    @Test
    void keepsAlreadyScopedSelectors() {
        assertEquals("#auth-ui[data-layout=split] .auth-story{a:b}",
            CssScoper.scope("#auth-ui[data-layout=split] .auth-story{a:b}"));
    }

    @Test
    void splitsSelectorListsOnlyAtTopLevel() {
        assertEquals("#auth-ui :is(a, b),#auth-ui c{x:y}", CssScoper.scope(":is(a, b), c{x:y}"));
    }

    @Test
    void scopesInsideMediaAndSupports() {
        assertEquals("@media (max-width: 700px){#auth-ui .auth-card{padding: 0}}",
            CssScoper.scope("@media (max-width: 700px) { .auth-card { padding: 0 } }"));
    }

    @Test
    void keepsKeyframesAndFontFace() {
        var css = "@keyframes spin{from{opacity:0}to{opacity:1}}";
        assertEquals(css, CssScoper.scope(css));
    }

    @Test
    void dropsImportsAndUnsupportedAtRules() {
        var scoped = CssScoper.scope("@import url(https://evil.example/x.css); @page{margin:0} a{b:c}");
        assertEquals("#auth-ui a{b:c}", scoped);
    }

    @Test
    void cannotCloseTheStyleElement() {
        var scoped = CssScoper.scope("a{b:c}</style><script>alert(1)</script><style>d{e:f}");
        assertFalse(scoped.contains("<"), scoped);
        assertTrue(scoped.startsWith("#auth-ui a{b:c}"), scoped);
    }

    @Test
    void removesComments() {
        assertEquals("#auth-ui a{b:c}", CssScoper.scope("/* note */a{b:c}"));
    }

    @Test
    void ignoresEmptyAndUnbalancedInput() {
        assertEquals("", CssScoper.scope(null));
        assertEquals("", CssScoper.scope("   "));
        assertEquals("#auth-ui a{b:c}", CssScoper.scope("a{b:c} d{e:f"));
    }
}
