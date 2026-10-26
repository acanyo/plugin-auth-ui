package com.xhhao.authui.design;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xhhao.authui.template.ThemeCompiler;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DesignSchemaTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> schema, String... path) {
        var current = schema;
        for (var name : path) {
            current = (Map<String, Object>) ((Map<String, Object>) current.get("properties")).get(name);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> enumOf(Map<String, Object> schema) {
        return Set.copyOf((List<String>) schema.get("enum"));
    }

    @Test
    void usesTheRenderersKeywords() {
        var spec = DesignSchema.spec(true);
        assertEquals(ThemeCompiler.LAYOUTS, enumOf(property(spec, "theme", "layout")));
        assertEquals(ThemeCompiler.ENTRANCES, enumOf(property(spec, "theme", "motion", "entrance")));
        assertEquals(ThemeCompiler.AMBIENTS, enumOf(property(spec, "theme", "motion", "ambient")));
        assertEquals(ThemeCompiler.BACKGROUNDS, enumOf(property(spec, "theme", "background", "type")));
        assertEquals(ThemeCompiler.MAX_FORM_WIDTH, property(spec, "theme", "formWidth").get("maximum"));
    }

    @Test
    void requiresANameOnlyWhenCreating() {
        assertEquals(List.of("displayName"), DesignSchema.spec(true).get("required"));
        assertEquals(null, DesignSchema.spec(false).get("required"));
        assertEquals(false, DesignSchema.spec(false).get("additionalProperties"));
    }
}
