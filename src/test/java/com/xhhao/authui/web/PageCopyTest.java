package com.xhhao.authui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageCopyTest {

    @Test
    void everyPageHasATitle() {
        var pageIds = Arrays.stream(AuthView.values())
            .flatMap(view -> Map.of(
                    "login", "login", "signup", "signup", "logout", "logout",
                    "password-reset/email/send", "password-reset/email/send",
                    "password-reset/email/reset", "password-reset/email/reset",
                    "challenges/two-factor/totp", "challenges/two-factor/totp")
                .keySet().stream().filter(view::renders).map(view::pageId))
            .toList();
        assertFalse(pageIds.isEmpty());
        pageIds.forEach(id -> assertFalse(PageCopy.of(id).title().equals("账号"), id));
    }

    @Test
    void fallsBackForUnknownPages() {
        assertEquals("账号", PageCopy.of("unknown").title());
    }
}
