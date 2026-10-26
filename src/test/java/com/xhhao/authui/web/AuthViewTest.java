package com.xhhao.authui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthViewTest {

    @Test
    void resolvesAuthenticationPages() {
        assertEquals(Optional.of(AuthView.LOGIN), AuthView.fromPath("/login"));
        assertEquals(Optional.of(AuthView.SIGNUP), AuthView.fromPath("/signup"));
        assertTrue(AuthView.fromPath("/login/social/github").isEmpty());
    }

    @Test
    void opensPasswordResetButNotTwoFactorInModal() {
        assertTrue(AuthView.PASSWORD_RESET.opensInModal());
        assertTrue(!AuthView.TWO_FACTOR.opensInModal());
        assertEquals(Optional.of(AuthView.PASSWORD_RESET), AuthView.fromPath("/password-reset/email/abc123"));
    }

    @Test
    void resolvesPreviewPages() {
        assertEquals(Optional.of(AuthView.LOGIN), AuthView.fromPreviewPath("/auth-ui/preview/login"));
        assertEquals(Optional.of(AuthView.SIGNUP), AuthView.fromPreviewPath("/auth-ui/preview/signup"));
        assertTrue(AuthView.fromPreviewPath("/auth-ui/preview").isEmpty());
        assertTrue(AuthView.fromPreviewPath("/auth-ui/preview/uc").isEmpty());
        assertTrue(AuthView.fromPreviewPath("/login").isEmpty());
    }
}
