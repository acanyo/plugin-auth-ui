package com.xhhao.authui.design;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xhhao.authui.template.AuthTemplate;
import java.util.Map;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Converts between JSON values (tool arguments, AI output) and {@link AuthTemplate.Spec}. */
public class DesignSpecs {
    private final JsonMapper mapper = JsonMapper.builder()
        // Callers validate against the schema first, which rejects unknown fields.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion(value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
        .build();

    /**
     * The spec in a JSON object.
     *
     * @throws IllegalArgumentException when the value is not an object or not a design
     */
    public AuthTemplate.Spec read(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("spec must be an object");
        }
        try {
            return mapper.convertValue(map, AuthTemplate.Spec.class);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("spec is not a valid design: " + error.getMessage(), error);
        }
    }

    /** The spec as JSON values, without unset fields. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> write(AuthTemplate.Spec spec) {
        return mapper.convertValue(spec, Map.class);
    }

    public String toJson(Object value) {
        return mapper.writeValueAsString(value);
    }
}
