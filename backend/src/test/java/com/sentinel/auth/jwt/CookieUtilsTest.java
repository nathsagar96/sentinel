package com.sentinel.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.sentinel.config.AppProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class CookieUtilsTest {

    private CookieUtils cookieUtils;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.JwtProperties(
                        "defaultDevSecretKeyThatIsAtLeast32CharactersLong", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.CorsProperties(List.of("http://localhost:5173")),
                new AppProperties.OAuth2Properties("http://localhost:5173/oauth2/redirect"),
                false);
        cookieUtils = new CookieUtils(appProperties);
    }

    @Test
    void testCreateAccessTokenCookie() {
        ResponseCookie cookie = cookieUtils.createAccessTokenCookie("sample-token");
        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("sample-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void testCreateRefreshTokenCookie() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("sample-token");
        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("sample-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth/refresh");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void testClearAccessTokenCookie() {
        ResponseCookie cookie = cookieUtils.clearAccessTokenCookie();
        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void testClearRefreshTokenCookie() {
        ResponseCookie cookie = cookieUtils.clearRefreshTokenCookie();
        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
