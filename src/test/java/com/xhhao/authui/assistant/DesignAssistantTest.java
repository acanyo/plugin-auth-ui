package com.xhhao.authui.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xhhao.authui.design.DesignSpecs;
import com.xhhao.authui.template.AuthTemplate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class DesignAssistantTest {

    /** Replays canned answers and records what the assistant asked. */
    static final class ScriptedModel implements DesignModel {
        final Deque<Map<String, Object>> answers = new ArrayDeque<>();
        final List<String> prompts = new ArrayList<>();
        final List<Map<String, Object>> schemas = new ArrayList<>();
        String system;
        final List<String> modelNames = new ArrayList<>();

        ScriptedModel answer(String summary, Map<String, Object> changes) {
            answers.add(Map.of("summary", summary, "changes", changes));
            return this;
        }

        @Override
        public Mono<Availability> availability(String modelName) {
            return Mono.just(Availability.ready());
        }

        @Override
        public Mono<Answer> generate(String modelName, String system, String prompt, Map<String, Object> schema) {
            this.system = system;
            modelNames.add(modelName);
            prompts.add(prompt);
            schemas.add(schema);
            return Mono.just(new Answer(answers.removeFirst(), "test-model", 100));
        }
    }

    private static AuthTemplate.Spec design() {
        var spec = new AuthTemplate.Spec();
        spec.setDisplayName("Base");
        var theme = new AuthTemplate.Theme();
        theme.setPrimary("#176b62");
        theme.setRadius(20);
        spec.setTheme(theme);
        var code = new AuthTemplate.Code();
        code.setJs("console.log('kept')");
        spec.setCode(code);
        return spec;
    }

    private static DesignAssistant.Request request(String instruction, boolean allowScript) {
        return new DesignAssistant.Request(design(), instruction, List.of(), allowScript, "");
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesOnlyTheChangedFields() {
        var model = new ScriptedModel().answer("改成了玫红色。", Map.of("theme", Map.of("primary", "#e11d48")));
        var suggestion = new DesignAssistant(model, new DesignSpecs()).suggest(request("主色改成玫红", false)).block();
        var theme = (Map<String, Object>) suggestion.design().get("theme");
        assertEquals("#e11d48", theme.get("primary"));
        assertEquals(20, theme.get("radius"));
        assertEquals("Base", suggestion.design().get("displayName"));
        assertEquals(List.of("theme"), suggestion.changed());
        assertEquals(List.of(), suggestion.warnings());
        assertEquals(1, suggestion.turns());
        assertEquals("改成了玫红色。", suggestion.summary());
        assertTrue(model.prompts.getFirst().contains("主色改成玫红"));
        assertTrue(model.system.contains("# Design guide"));
        assertTrue(model.system.contains("# Design principles"));
        assertTrue(model.system.contains("Use the lowest layer that does the job"), "skill principles are in the prompt");
    }

    @Test
    @SuppressWarnings("unchecked")
    void repairsLinterWarningsOnce() {
        var model = new ScriptedModel()
            .answer("深色。", Map.of("theme", Map.of("text", "#222222", "surface", "#111111")))
            .answer("提高了文字对比度。", Map.of("theme", Map.of("text", "#f5f5f7")));
        var suggestion = new DesignAssistant(model, new DesignSpecs()).suggest(request("深色", false)).block();
        assertEquals(2, suggestion.turns());
        assertTrue(model.prompts.get(1).contains("contrast"), model.prompts.get(1));
        assertEquals("#f5f5f7", ((Map<String, Object>) suggestion.design().get("theme")).get("text"));
        assertEquals(List.of(), suggestion.warnings());
        assertEquals("深色。 提高了文字对比度。", suggestion.summary());
        assertEquals(200, suggestion.tokens());
    }

    @Test
    void reportsWarningsLeftAfterRepair() {
        var model = new ScriptedModel()
            .answer("a", Map.of("theme", Map.of("primary", "not a colour")))
            .answer("b", Map.of("theme", Map.of("primary", "still not")));
        var suggestion = new DesignAssistant(model, new DesignSpecs()).suggest(request("x", false)).block();
        assertEquals(2, suggestion.turns());
        assertFalse(suggestion.warnings().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void neverAcceptsScriptUnlessAllowed() {
        var model = new ScriptedModel().answer("a", Map.of("code", Map.of("js", "steal()", "css", "h1{color:red}")));
        var suggestion = new DesignAssistant(model, new DesignSpecs()).suggest(request("x", false)).block();
        var code = (Map<String, Object>) suggestion.design().get("code");
        assertEquals("console.log('kept')", code.get("js"));
        assertEquals("h1{color:red}", code.get("css"));
        var schemaCode = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>)
            model.schemas.getFirst().get("properties")).get("changes")).get("properties")).get("code");
        assertNull(((Map<String, Object>) schemaCode.get("properties")).get("js"));
        assertTrue(model.system.contains("Do not write JavaScript"));
    }

    @Test
    void rejectsAnEmptyRequest() {
        var assistant = new DesignAssistant(new ScriptedModel(), new DesignSpecs());
        assertThrows(IllegalArgumentException.class, () -> assistant.suggest(request("  ", false)).block());
    }

    @Test
    void usesTheChosenModelForEveryTurn() {
        var model = new ScriptedModel()
            .answer("a", Map.of("theme", Map.of("primary", "not a colour")))
            .answer("b", Map.of("theme", Map.of("primary", "#e11d48")));
        new DesignAssistant(model, new DesignSpecs())
            .suggest(new DesignAssistant.Request(design(), "x", List.of(), false, "gpt-x")).block();
        assertEquals(List.of("gpt-x", "gpt-x"), model.modelNames);
    }

    @Test
    void sendsEarlierRequestsAsContext() {
        var model = new ScriptedModel().answer("a", Map.of());
        new DesignAssistant(model, new DesignSpecs()).suggest(new DesignAssistant.Request(design(), "再亮一点",
            List.of("改成深色", "主色换成青色"), false, "")).block();
        var prompt = model.prompts.getFirst();
        assertTrue(prompt.contains("- 改成深色\n- 主色换成青色"), prompt);
        assertTrue(prompt.indexOf("Earlier requests") < prompt.indexOf("Request:\n再亮一点"), prompt);
    }

    @Test
    void keepsOnlyRecentHistoryWithinBudget() {
        var many = java.util.stream.IntStream.range(0, 10).mapToObj(i -> "r" + i).toList();
        assertEquals(List.of("r4", "r5", "r6", "r7", "r8", "r9"), DesignAssistant.history(many));
        var big = "x".repeat(DesignAssistant.MAX_INSTRUCTION);
        assertEquals(2, DesignAssistant.history(List.of(big, big, big, "  ")).size());
        assertEquals(List.of(), DesignAssistant.history(null));
    }
}
