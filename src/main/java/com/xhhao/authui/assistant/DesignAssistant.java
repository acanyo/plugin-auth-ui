package com.xhhao.authui.assistant;

import com.xhhao.authui.design.DesignGuide;
import com.xhhao.authui.design.DesignSchema;
import com.xhhao.authui.design.DesignSkill;
import com.xhhao.authui.design.DesignSpecs;
import com.xhhao.authui.template.AuthTemplate;
import com.xhhao.authui.template.DesignLinter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * Turns a request such as "make it liquid glass" into a new draft of the design being edited.
 *
 * <p>The model answers with only the fields it changes, which are merged into the current
 * design. The result is linted; when the linter finds problems the model gets one more turn to
 * fix them. Nothing is saved: the studio shows the draft and the user decides.</p>
 */
public class DesignAssistant {
    /** Extra turns given to the model to fix linter warnings. */
    static final int REPAIR_TURNS = 1;
    static final int MAX_INSTRUCTION = 2000;
    /** Earlier requests sent as context, so "a bit brighter" refers to the last change. */
    static final int MAX_HISTORY = 6;
    static final int MAX_HISTORY_CHARS = 4000;
    private static final Set<String> SPEC_FIELDS =
        Set.of("displayName", "description", "loginSlogan", "signupSlogan", "theme", "slots", "code");

    private final DesignModel model;
    private final DesignSpecs specs;

    public DesignAssistant(DesignModel model, DesignSpecs specs) {
        this.model = model;
        this.specs = specs;
    }

    public Mono<DesignModel.Availability> availability(String modelName) {
        return model.availability(modelName);
    }

    /** Asks the model for a new draft of {@code request.design()}. */
    public Mono<Suggestion> suggest(Request request) {
        var instruction = request.instruction() == null ? "" : request.instruction().strip();
        if (instruction.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Describe the change you want."));
        }
        var turn = new Turn(request.modelName(), systemPrompt(request.allowScript()),
            answerSchema(DesignSchema.forAssistant(request.allowScript())), request.allowScript());
        var current = specs.write(request.design() == null ? new AuthTemplate.Spec() : request.design());
        var prompt = prompt(current, history(request.history()), clip(instruction, MAX_INSTRUCTION));
        return model.generate(turn.modelName(), turn.system(), prompt, turn.schema())
            .flatMap(answer -> review(current, answer, turn, 0, new Usage(), List.of()));
    }

    private Mono<Suggestion> review(Map<String, Object> current, DesignModel.Answer answer, Turn turn,
        int round, Usage usage, List<String> changedBefore) {
        usage.add(answer);
        var changes = changes(answer.output(), turn.allowScript());
        var merged = DesignPatch.apply(current, changes.design());
        var spec = specs.read(merged);
        var warnings = DesignLinter.lint(spec);
        var changed = new ArrayList<>(changedBefore);
        DesignPatch.touched(changes.design(), SPEC_FIELDS).stream().filter(field -> !changed.contains(field))
            .forEach(changed::add);
        if (warnings.isEmpty() || round >= REPAIR_TURNS) {
            return Mono.just(new Suggestion(specs.write(spec), changes.summary(), changed, warnings, answer.model(),
                usage.tokens, round + 1));
        }
        // One more turn: the model sees what the renderer would drop and corrects its own design.
        return model.generate(turn.modelName(), turn.system(), repairPrompt(merged, warnings), turn.schema())
            .flatMap(next -> review(merged, next, turn, round + 1, usage, changed)
                .map(fixed -> fixed.withSummary(join(changes.summary(), fixed.summary()))));
    }

    /** The most recent earlier requests that fit the budget, oldest first. */
    static List<String> history(List<String> requests) {
        if (requests == null) {
            return List.of();
        }
        var kept = new ArrayList<String>();
        int remaining = MAX_HISTORY_CHARS;
        for (int i = requests.size() - 1; i >= 0 && kept.size() < MAX_HISTORY; i--) {
            var text = requests.get(i) == null ? "" : clip(requests.get(i).strip(), MAX_INSTRUCTION);
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > remaining) {
                break;
            }
            kept.addFirst(text);
            remaining -= text.length();
        }
        return kept;
    }

    private static String clip(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    /** Splits the answer into the design changes and the model's explanation. */
    @SuppressWarnings("unchecked")
    private static Changes changes(Map<String, Object> output, boolean allowScript) {
        var design = new LinkedHashMap<String, Object>();
        if (output.get("changes") instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach((key, value) -> {
                if (SPEC_FIELDS.contains(key)) {
                    design.put(key, value);
                }
            });
        }
        if (!allowScript && design.get("code") instanceof Map<?, ?> code && code.containsKey("js")) {
            // The schema already omits js; this guards models that ignore it.
            var withoutScript = new LinkedHashMap<>((Map<String, Object>) code);
            withoutScript.remove("js");
            design.put("code", withoutScript);
        }
        var summary = output.get("summary") instanceof String text ? text.strip() : "";
        return new Changes(design, summary);
    }

    /** The answer the model must give: a short explanation and only the changed fields. */
    static Map<String, Object> answerSchema(Map<String, Object> designSchema) {
        var schema = DesignSchema.object("A short explanation and the fields to change.", Map.of(
            "summary", DesignSchema.string("One or two sentences, in the user's language, on what changed.", 1, 400),
            "changes", designSchema));
        schema.put("required", List.of("summary", "changes"));
        return schema;
    }

    private static String systemPrompt(boolean allowScript) {
        return """
            You design the login, signup and logout pages of a Halo site by editing an AuthTemplate design.
            Follow the design principles below. The design guide after them is the authoritative reference for every
            field, CSS variable, class name and motion keyword.

            # Answer format

            - Answer with JSON only: {"summary": "...", "changes": {...}}.
            - "changes" holds only the fields you change, nested like the design. Objects are merged into the current design;
              strings (including code.css and slots) replace the current value as a whole, so include the full new text.
              Set a field to null to remove it.
            - Write "summary" in the user's language.
            %s

            # Design principles

            %s

            # Design guide

            %s
            """.formatted(allowScript
                ? "- You may write code.js when behaviour is needed. Never read, store or send form values."
                : "- Do not write JavaScript; code.js is not available, so leave it out even where the principles mention it.",
            DesignSkill.principles(),
            DesignGuide.text());
    }

    private String prompt(Map<String, Object> current, List<String> history, String instruction) {
        var earlier = history.isEmpty() ? "" : """
            Earlier requests in this conversation, oldest first (the current design already reflects them):
            - %s

            """.formatted(String.join("\n- ", history));
        return """
            Current design (JSON):
            %s

            %sRequest:
            %s
            """.formatted(specs.toJson(current), earlier, instruction);
    }

    private String repairPrompt(Map<String, Object> merged, List<String> warnings) {
        return """
            Your design was applied, but the renderer will drop or change these parts:
            - %s

            Design after your changes (JSON):
            %s

            Answer with the changes that fix every problem, keeping the intended look.
            """.formatted(String.join("\n- ", warnings), specs.toJson(merged));
    }

    private static String join(String first, String second) {
        if (second == null || second.isBlank() || Objects.equals(first, second)) {
            return first;
        }
        return first + " " + second;
    }

    private record Changes(Map<String, Object> design, String summary) {
    }

    /** What stays the same across the first answer and its repair. */
    private record Turn(String modelName, String system, Map<String, Object> schema, boolean allowScript) {
    }

    /**
     * @param design the design as currently edited, possibly unsaved
     * @param history earlier requests in the conversation, oldest first
     * @param allowScript whether the model may write {@code code.js}
     * @param modelName AI Foundation model to use; {@code null} or blank uses the default
     */
    public record Request(AuthTemplate.Spec design, String instruction, List<String> history, boolean allowScript,
                          String modelName) {
    }

    private static final class Usage {
        private Integer tokens;

        void add(DesignModel.Answer answer) {
            if (answer.tokens() != null) {
                tokens = (tokens == null ? 0 : tokens) + answer.tokens();
            }
        }
    }

    /**
     * @param design the new draft, as the studio edits it
     * @param changed top-level fields that changed, e.g. {@code theme}
     * @param warnings problems the model could not fix; shown to the user
     * @param turns model calls used, including repairs
     */
    public record Suggestion(Map<String, Object> design, String summary, List<String> changed, List<String> warnings,
                             String model, Integer tokens, int turns) {
        public Suggestion {
            changed = List.copyOf(changed);
            warnings = List.copyOf(warnings);
        }

        Suggestion withSummary(String text) {
            return new Suggestion(design, text, changed, warnings, model, tokens, turns);
        }
    }
}
