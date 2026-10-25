package com.xhhao.authui.web;

import com.xhhao.authui.template.AuthTemplate;

/**
 * How an authentication page is rendered.
 *
 * @param preview forms are inert, for the console preview
 * @param templateName template to render; {@code null} uses the active one
 * @param draft unsaved design to render instead of a stored template
 * @param autoSubmit submit the page's form on load, for actions already confirmed elsewhere
 */
public record RenderOptions(boolean preview, String templateName, AuthTemplate.Spec draft,
                            boolean autoSubmit) {
    public static final RenderOptions PAGE = new RenderOptions(false, null, null, false);

    public static RenderOptions preview(String templateName) {
        return new RenderOptions(true, templateName, null, false);
    }

    public static RenderOptions previewDraft(AuthTemplate.Spec draft) {
        return new RenderOptions(true, null, draft, false);
    }

    public static RenderOptions autoSubmitted() {
        return new RenderOptions(false, null, null, true);
    }
}
