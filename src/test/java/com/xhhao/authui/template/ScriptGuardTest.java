package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScriptGuardTest {

    @Test
    void wrapsCodeSoErrorsAreContained() {
        var js = "AuthUI.on('ready', () => console.log(1 < 2));";
        var prepared = ScriptGuard.prepare(js);
        assertTrue(prepared.contains(js), prepared);
        assertTrue(prepared.contains("try{"), prepared);
        assertTrue(prepared.contains("catch(e)"), prepared);
        assertTrue(prepared.contains("DOMContentLoaded"), prepared);
    }

    @Test
    void cannotCloseTheScriptElement() {
        var prepared = ScriptGuard.prepare("var s = '</script><script>alert(1)</ScRiPt>'; <!-- x");
        assertFalse(prepared.toLowerCase().contains("</script"), prepared);
        assertFalse(prepared.contains("<!--"), prepared);
    }

    @Test
    void ignoresEmptyInput() {
        assertEquals("", ScriptGuard.prepare(null));
        assertEquals("", ScriptGuard.prepare(" "));
    }
}
