package com.xhhao.authui.settings;

import com.xhhao.authui.util.SafeUrl;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * Public appearance values only; authentication and registration remain Halo settings.
 *
 * <p>{@code basic} holds the Setting form (logo, display mode). {@code template} is written by
 * the console's template tab and names the active {@code AuthTemplate}; it is not part of the
 * Setting form, so saving one never clears the other.</p>
 */
@Getter
public final class AuthUiSettings {
    public static final String MODAL = "modal";
    public static final String PAGE = "page";

    private boolean enabled = true;
    private String logo = "";
    private String displayMode = MODAL;
    /** Name of the active template; empty uses the plugin's base look. */
    private String templateName = "";

    public static AuthUiSettings from(Map<String, JsonNode> groups) {
        var settings = new AuthUiSettings();
        var basic = groups.get("basic");
        settings.logo = SafeUrl.of(text(basic, "logo", ""));
        var mode = text(basic, "displayMode", MODAL);
        settings.displayMode = Set.of(MODAL, PAGE).contains(mode) ? mode : MODAL;
        settings.templateName = text(groups.get("template"), "name", "");
        return settings;
    }

    /** Used when settings cannot be read: Halo's own pages stay in place. */
    public static AuthUiSettings disabled() {
        var settings = new AuthUiSettings();
        settings.enabled = false;
        return settings;
    }

    /** Whether site login links and direct visits open the modal instead of the full page. */
    public boolean isModalEnabled() {
        return enabled && MODAL.equals(displayMode);
    }

    private static String text(JsonNode group, String key, String fallback) {
        var value = group == null ? null : group.get(key);
        return value == null || !value.isString() || value.asString().isEmpty()
            ? fallback : value.asString();
    }
}
