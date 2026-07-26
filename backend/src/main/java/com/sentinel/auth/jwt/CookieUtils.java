package com.sentinel.auth.jwt;

import com.sentinel.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String INTERNAL_USER_ID_ATTR = "internal_user_id";

    private final AppProperties appProperties;

    public static Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Strict")
                .path("/")
                .maxAge(appProperties.jwt().accessTokenExpiry())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(appProperties.jwt().refreshTokenExpiry())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();
    }
}
