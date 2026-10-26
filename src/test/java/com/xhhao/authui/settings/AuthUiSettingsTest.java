package com.xhhao.authui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AuthUiSettingsTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static Map<String, JsonNode> groups(String... pairs) {
        var groups = new HashMap<String, JsonNode>();
        for (int i = 0; i < pairs.length; i += 2) {
            groups.put(pairs[i], JSON.readTree(pairs[i + 1]));
        }
        return groups;
    }

    @Test
    void defaultsToModalWithoutTemplate() {
        var settings = AuthUiSettings.from(Map.of());
        assertTrue(settings.isModalEnabled());
        assertEquals("", settings.getTemplateName());
        assertEquals("", settings.getLogo());
    }

    @Test
    void readsLogoDisplayModeAndTemplate() {
        var settings = AuthUiSettings.from(groups(
            "basic", "{\"logo\":\"/upload/logo.png\",\"displayMode\":\"page\"}",
            "template", "{\"name\":\"aurora\"}"));
        assertEquals("/upload/logo.png", settings.getLogo());
        assertFalse(settings.isModalEnabled());
        assertEquals("aurora", settings.getTemplateName());
    }

    @Test
    void defaultsToTheModalWithoutSettings() {
        var settings = AuthUiSettings.from(groups());
        assertEquals("", settings.getLogo());
        assertTrue(settings.isModalEnabled());
        assertEquals("", settings.getTemplateName());
    }

    @Test
    void ignoresUnknownOrUnsafeValues() {
        var settings = AuthUiSettings.from(groups(
            "basic", "{\"logo\":\"javascript:alert(1)\",\"displayMode\":\"popup\"}"));
        assertEquals("", settings.getLogo());
        assertTrue(settings.isModalEnabled());
    }

    @Test
    void disabledSettingsKeepHaloPages() {
        var settings = AuthUiSettings.disabled();
        assertFalse(settings.isEnabled());
        assertFalse(settings.isModalEnabled());
    }
}
