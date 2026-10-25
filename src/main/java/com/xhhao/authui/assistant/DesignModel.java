package com.xhhao.authui.assistant;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * A language model that answers with a JSON object matching a schema. The assistant depends on
 * this interface only, so it can be tested without AI Foundation, which may not be installed.
 *
 * <p>{@code modelName} names a model chosen by the user; {@code null} or blank uses the default.</p>
 */
public interface DesignModel {

    /** Whether the model can be called now; {@link #generate} fails otherwise. */
    Mono<Availability> availability(String modelName);

    /**
     * Generates one JSON object.
     *
     * @param schema JSON Schema the answer must match; checked again by the caller
     */
    Mono<Answer> generate(String modelName, String system, String prompt, Map<String, Object> schema);

    /**
     * @param canChooseModel whether the user can pick another model, so the studio shows a model
     *     selector even when the default model is missing
     */
    record Availability(boolean available, boolean canChooseModel, String reason, String setupHint) {
        public static Availability ready() {
            return new Availability(true, true, "", "");
        }

        /** No model can be used at all, e.g. AI Foundation is not installed. */
        public static Availability unavailable(String reason, String setupHint) {
            return new Availability(false, false, reason, setupHint);
        }

        /** The requested model cannot be used, but the user may pick another one. */
        public static Availability unconfigured(String reason, String setupHint) {
            return new Availability(false, true, reason, setupHint);
        }
    }

    /**
     * @param output the parsed JSON object
     * @param model the model that answered, for display
     * @param tokens total tokens used, or {@code null} when the provider does not report them
     */
    record Answer(Map<String, Object> output, String model, Integer tokens) {
    }

    /** A failure worth showing to the user as is, e.g. "default model is not configured". */
    class DesignModelException extends RuntimeException {
        private final String code;

        public DesignModelException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
