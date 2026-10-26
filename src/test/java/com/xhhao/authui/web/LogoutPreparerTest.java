package com.xhhao.authui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class LogoutPreparerTest {
    private final LogoutPreparer preparer = new LogoutPreparer();

    private static ServerWebExchange exchange(String uri) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, URI.create(uri)));
    }

    private static String redirect(ServerWebExchange exchange) {
        return exchange.getRequest().getQueryParams().getFirst("redirect_uri");
    }

    @Test
    void keepsHaloDefaultWithoutTarget() {
        assertNull(redirect(preparer.prepare(exchange("/logout"), false)));
    }

    @Test
    void keepsModalLoggedOutPage() {
        var prepared = preparer.prepare(
            exchange("/logout?redirect_uri=%2Fauth-ui%2Flogged-out%3Ftarget%3D%252Farchives"), false);
        assertEquals("/auth-ui/logged-out?target=%2Farchives", redirect(prepared));
    }

    @Test
    void reducesOtherTargetsToSafePaths() {
        assertEquals("/", redirect(preparer.prepare(
            exchange("/logout?redirect_uri=https%3A%2F%2Fevil.example.com%2F"), false)));
        assertEquals("/archives", redirect(preparer.prepare(
            exchange("/logout?redirect_uri=%2Farchives"), false)));
    }

    @Test
    void consoleLogoutContinuesToLoginWithNotice() {
        assertEquals(LogoutPreparer.AFTER_APP_LOGOUT,
            redirect(preparer.prepare(exchange("/logout"), true)));
    }
}
