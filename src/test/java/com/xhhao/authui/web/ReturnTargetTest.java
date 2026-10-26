package com.xhhao.authui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReturnTargetTest {

    @Test
    void keepsApplicationPaths() {
        assertEquals("/archives/hello?page=2#comments",
            ReturnTarget.safe("/archives/hello?page=2#comments"));
    }

    @Test
    void fallsBackToHomeWithoutTarget() {
        assertEquals("/", ReturnTarget.safe(null));
        assertEquals("/", ReturnTarget.forAuthentication(null, false));
    }

    @Test
    void keepsConsoleTargetFromAbsoluteUrl() {
        // The console's auth guard sends redirect_uri=window.location.href.
        assertEquals("/console/posts?keyword=a",
            ReturnTarget.safe("https://blog.example.com/console/posts?keyword=a"));
        assertEquals("/console", ReturnTarget.forAuthentication("http://localhost:8090/console", false));
    }

    @Test
    void neverLeavesTheSiteForAbsoluteUrls() {
        assertEquals("/", ReturnTarget.safe("https://evil.example.com"));
        assertEquals("/path", ReturnTarget.safe("https://evil.example.com/path"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "//evil.example.com", "/\\evil.example.com", "javascript:alert(1)", "relative/path",
        "/login", "/signup?x=1", "/auth-ui/complete?target=/", "/%2F%2Fevil.example.com",
        "http://host//evil.example.com", "/a\nb"
    })
    void rejectsUnsafeTargets(String value) {
        assertEquals("/", ReturnTarget.safe(value));
    }

    @Test
    void encodesNonAsciiPaths() {
        assertEquals("/tags/%E9%9A%8F%E7%AC%94", ReturnTarget.safe("/tags/随笔"));
    }

    @Test
    void keepsModalCallbackInsideTheModal() {
        var callback = "/auth-ui/complete?target=%2Farchives%2Fhello%3Fpage%3D2";
        assertEquals(callback, ReturnTarget.forAuthentication(callback, true));
    }

    @Test
    void unwrapsModalCallbackForStandalonePages() {
        var callback = "/auth-ui/complete?target=%2Farchives%2Fhello%3Fpage%3D2";
        assertEquals("/archives/hello?page=2", ReturnTarget.forAuthentication(callback, false));
        assertEquals("/", ReturnTarget.forAuthentication("/auth-ui/complete?target=//evil", false));
        assertEquals("/", ReturnTarget.forAuthentication("/auth-ui/complete?x=1", false));
    }
}
