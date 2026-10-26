package com.xhhao.authui.web;

/**
 * Keeps theme and plugin code out of the plugin's pages, as Halo does for its own login pages.
 *
 * <p>Halo's {@code InjectionExcluderProcessor} skips global head and footer injection (site code
 * injection, plugin {@code TemplateHeadProcessor}s) for templates named {@code login},
 * {@code signup}, {@code challenges/*} and so on. Plugin templates are not on that list, so the
 * same flag is set as a model attribute; Halo's processors check it with
 * {@code context.containsVariable}. Scripts added by web filters (e.g. passkey login) are
 * unaffected.</p>
 */
final class HaloInjection {
    /** Value of {@code InjectionExcluderProcessor.EXCLUDE_INJECTION_VARIABLE} in Halo 2.20+. */
    static final String EXCLUDE = "run.halo.app.theme.dialect.InjectionExcluderProcessor.EXCLUDE_INJECTION";

    private HaloInjection() {
    }
}
