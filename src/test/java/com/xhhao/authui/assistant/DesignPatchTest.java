package com.xhhao.authui.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DesignPatchTest {

    @Test
    void mergesObjectsAndKeepsUntouchedFields() {
        var current = Map.<String, Object>of(
            "displayName", "A",
            "theme", Map.of("primary", "#111111", "radius", 20, "motion", Map.of("entrance", "rise")),
            "code", Map.of("css", "h1{}", "js", "console.log(1)"));
        var changes = Map.<String, Object>of(
            "theme", Map.of("primary", "#e11d48", "motion", Map.of("ambient", "float")),
            "code", Map.of("css", "h2{}"));
        var merged = DesignPatch.apply(current, changes);
        assertEquals("A", merged.get("displayName"));
        assertEquals(Map.of("primary", "#e11d48", "radius", 20, "motion", Map.of("entrance", "rise", "ambient", "float")),
            merged.get("theme"));
        // Strings replace as a whole, objects merge, so js survives a CSS edit.
        assertEquals(Map.of("css", "h2{}", "js", "console.log(1)"), merged.get("code"));
    }

    @Test
    void nullRemovesAField() {
        var changes = new HashMap<String, Object>();
        changes.put("loginSlogan", null);
        assertEquals(Map.of("displayName", "A"),
            DesignPatch.apply(Map.of("displayName", "A", "loginSlogan", "Hi"), changes));
    }

    @Test
    void reportsOnlyKnownTopLevelFields() {
        assertEquals(List.of("theme"), DesignPatch.touched(Map.of("theme", Map.of(), "bogus", 1), Set.of("theme", "code")));
    }
}
