package com.xhhao.authui.web;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** The Halo pages whose presentation this plugin replaces. */
public enum AuthView {
    LOGIN("login", "/login", "preAuthLoginEndpoints", Map.of("login", "login")),
    SIGNUP("signup", "/signup", "preAuthSignUpEndpoints", Map.of("signup", "signup")),
    LOGOUT("logout", "/logout", "logoutPage", Map.of("logout", "logout")),
    PASSWORD_RESET("password-reset", "/password-reset/email", "preAuthPasswordResetEndpoints",
        Map.of("password-reset/email/send", "password-reset-send",
            "password-reset/email/reset", "password-reset-reset")),
    TWO_FACTOR("two-factor", "/challenges/two-factor/totp", "preAuthTwoFactorEndpoints",
        Map.of("challenges/two-factor/totp", "two-factor"));

    static final String PREVIEW_PREFIX = "/auth-ui/preview";

    private final String id;
    private final String path;
    private final String routerBean;
    /** Halo template name to the page id used by the plugin template and scripts. */
    private final Map<String, String> pages;

    AuthView(String id, String path, String routerBean, Map<String, String> pages) {
        this.id = id;
        this.path = path;
        this.routerBean = routerBean;
        this.pages = pages;
    }

    public String id() {
        return id;
    }

    public String path() {
        return path;
    }

    /** Name of Halo's root-context router bean that handles this view. */
    String routerBean() {
        return routerBean;
    }

    /** Whether Halo rendered one of this view's pages, rather than e.g. an error page. */
    public boolean renders(String templateName) {
        return pages.containsKey(templateName);
    }

    /** Page id for a template Halo rendered, e.g. {@code password-reset-send}. */
    public String pageId(String templateName) {
        return pages.getOrDefault(templateName, id);
    }

    /**
     * Views that a direct visit opens in the site modal. Two-factor only follows a password
     * login, so it stays wherever that login happened.
     */
    public boolean opensInModal() {
        return this != TWO_FACTOR;
    }

    public static Optional<AuthView> fromPath(String path) {
        if (path == null) {
            return Optional.empty();
        }
        var exact = Arrays.stream(values()).filter(view -> view.path.equals(path)).findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        // The reset form lives at /password-reset/email/{token}.
        var prefix = PASSWORD_RESET.path + "/";
        if (path.startsWith(prefix) && path.length() > prefix.length()
            && path.indexOf('/', prefix.length()) < 0) {
            return Optional.of(PASSWORD_RESET);
        }
        return Optional.empty();
    }

    /** Resolves {@code /auth-ui/preview/{id}}, e.g. {@code /auth-ui/preview/two-factor}. */
    public static Optional<AuthView> fromPreviewPath(String path) {
        if (path == null || !path.startsWith(PREVIEW_PREFIX + "/")) {
            return Optional.empty();
        }
        var id = path.substring(PREVIEW_PREFIX.length() + 1);
        return Arrays.stream(values()).filter(view -> view.id.equals(id)).findFirst();
    }
}
