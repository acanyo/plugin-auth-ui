package com.xhhao.authui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xhhao.authui.settings.AuthUiSettings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.util.UriUtils;

class AuthModalRedirectTest {
    private final AuthModalRedirect redirect = new AuthModalRedirect(new ReturnTargetPreparer());

    private static MockServerHttpRequest.BaseBuilder<?> navigation(String uri) {
        // Pass a URI so the already-encoded query is not encoded a second time.
        return MockServerHttpRequest.method(HttpMethod.GET, URI.create(uri))
            .header("Sec-Fetch-Dest", "document");
    }

    private Optional<URI> location(MockServerHttpRequest.BaseBuilder<?> request, AuthView view) {
        return redirect.location(MockServerWebExchange.from(request), view, new AuthUiSettings())
            .block();
    }

    private static String modalOf(URI location) {
        var fragment = location.getRawFragment();
        assertTrue(fragment.startsWith(AuthModalRedirect.FRAGMENT_KEY), fragment);
        return UriUtils.decode(fragment.substring(AuthModalRedirect.FRAGMENT_KEY.length()),
            StandardCharsets.UTF_8);
    }

    @Test
    void directLoginOpensModalOnHome() {
        var location = location(navigation("/login"), AuthView.LOGIN).orElseThrow();
        assertEquals("/", location.getRawPath());
        assertEquals("/login", modalOf(location));
    }

    @Test
    void directSignupOpensSignupModal() {
        var location = location(navigation("/signup"), AuthView.SIGNUP).orElseThrow();
        assertEquals("/signup", modalOf(location));
    }

    @Test
    void passwordResetOpensModal() {
        var location = location(navigation("/password-reset/email")
            .header("Referer", "http://localhost:8090/archives/hello")
            .header("Sec-Fetch-Site", "same-origin"), AuthView.PASSWORD_RESET).orElseThrow();
        assertEquals("/archives/hello", location.getRawPath());
        assertEquals("/password-reset/email", modalOf(location));
    }

    @Test
    void emailedResetLinkOpensModalWithItsToken() {
        var location = location(navigation("/password-reset/email/abc123"), AuthView.PASSWORD_RESET)
            .orElseThrow();
        assertEquals("/", location.getRawPath());
        assertEquals("/password-reset/email/abc123", modalOf(location));
    }

    @Test
    void opensModalOnTheSitePageTheVisitorCameFrom() {
        var location = location(navigation("/login")
            .header("Referer", "http://localhost:8090/archives/hello?page=2")
            .header("Sec-Fetch-Site", "same-origin"), AuthView.LOGIN).orElseThrow();
        assertEquals("/archives/hello", location.getRawPath());
        assertEquals("page=2", location.getRawQuery());
    }

    @Test
    void keepsRequestedTargetAndMessages() {
        var location = location(navigation("/login?redirect_uri=%2Farchives%3Fp%3D1&logout"),
            AuthView.LOGIN).orElseThrow();
        assertEquals("/login?logout&redirect_uri=/archives?p%3D1", modalOf(location));
    }

    @Test
    void ignoresCrossSiteReferers() {
        var location = location(navigation("/login")
            .header("Referer", "https://evil.example.com/page")
            .header("Sec-Fetch-Site", "cross-site"), AuthView.LOGIN).orElseThrow();
        assertEquals("/", location.getRawPath());
    }

    @Test
    void neverUsesNonSitePagesAsBackground() {
        var location = location(navigation("/login")
            .header("Referer", "http://localhost:8090/uc/profile")
            .header("Sec-Fetch-Site", "same-origin"), AuthView.LOGIN).orElseThrow();
        assertEquals("/", location.getRawPath());
    }

    @Test
    void keepsFullPageWhenRequested() {
        assertTrue(location(navigation("/login?auth_ui_page=1"), AuthView.LOGIN).isEmpty());
    }

    @Test
    void keepsFullPageInsideTheModalIframe() {
        var request = MockServerHttpRequest.get("/login").header("Sec-Fetch-Dest", "iframe");
        assertTrue(location(request, AuthView.LOGIN).isEmpty());
    }

    @Test
    void keepsFullPageWithoutFetchMetadata() {
        assertTrue(location(MockServerHttpRequest.get("/login"), AuthView.LOGIN).isEmpty());
    }

    @Test
    void consoleSignInOpensModalOnHomeAndContinuesToConsole() {
        // Halo's entry point for an unauthenticated /console visit.
        var location = location(navigation("/login?authentication_required")
            .header("Referer", "http://localhost:8090/console/posts")
            .header("Sec-Fetch-Site", "same-origin"), AuthView.LOGIN).orElseThrow();
        assertEquals("/", location.getRawPath());
        assertEquals("/login", modalOf(location));
    }

    @Test
    void consoleTargetsOpenModalOnHome() {
        // The console's own guard sends redirect_uri=window.location.href.
        var request = navigation("/login?redirect_uri="
            + UriUtils.encodeQueryParam("http://localhost:8090/console/posts", StandardCharsets.UTF_8));
        var location = location(request, AuthView.LOGIN).orElseThrow();
        assertEquals("/", location.getRawPath());
        assertEquals("/login?redirect_uri=/console/posts", modalOf(location));
    }

    @Test
    void keepsFullPageWhenThePageBehindTheModalItselfRequiresSignIn() {
        var request = navigation("/login?authentication_required&redirect_uri=%2Fmembers")
            .header("Referer", "http://localhost:8090/members")
            .header("Sec-Fetch-Site", "same-origin");
        assertTrue(location(request, AuthView.LOGIN).isEmpty());
    }

    @Test
    void directLogoutOpensModal() {
        var location = location(navigation("/logout")
            .header("Referer", "http://localhost:8090/archives/hello")
            .header("Sec-Fetch-Site", "same-origin"), AuthView.LOGOUT).orElseThrow();
        assertEquals("/archives/hello", location.getRawPath());
        assertEquals("/logout", modalOf(location));
    }

    @Test
    void keepsFullPageAfterFullPageSubmission() {
        var request = navigation("/login?error=invalid-credential&method=local")
            .header("Referer", "http://localhost:8090/login?auth_ui_page=1")
            .header("Sec-Fetch-Site", "same-origin");
        assertTrue(location(request, AuthView.LOGIN).isEmpty());
    }

    @Test
    void keepsFullPageWhenModalIsDisabled() {
        var settings = AuthUiSettings.from(Map.of("basic",
            tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree("{\"displayMode\":\"page\"}")));
        var exchange = MockServerWebExchange.from(navigation("/login"));
        assertTrue(redirect.location(exchange, AuthView.LOGIN, settings).block().isEmpty());
    }
}
