package com.xhhao.authui.assistant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies an assistant's answer to the current design. The answer only lists what changes, so
 * fields the user did not mention survive even when a model forgets them.
 *
 * <p>Objects merge key by key; any other value replaces the current one. {@code null} clears a
 * field. Free text fields that hold code or HTML are replaced as a whole.</p>
 */
final class DesignPatch {
    private DesignPatch() {
    }

    static Map<String, Object> apply(Map<String, Object> current, Map<String, Object> changes) {
        return merge(current, changes);
    }

    /** Top-level fields the answer touched, e.g. {@code theme}, {@code code}, for the summary. */
    static List<String> touched(Map<String, Object> changes, Set<String> known) {
        return changes.keySet().stream().filter(known::contains).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> changes) {
        var result = new LinkedHashMap<>(base == null ? Map.<String, Object>of() : base);
        changes.forEach((key, value) -> {
            if (value == null) {
                result.remove(key);
            } else if (value instanceof Map<?, ?> nested && result.get(key) instanceof Map<?, ?> existing) {
                result.put(key, merge((Map<String, Object>) existing, (Map<String, Object>) nested));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }
}
