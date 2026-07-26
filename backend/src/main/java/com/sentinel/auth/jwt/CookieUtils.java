package com.sentinel.auth.jwt;

import com.sentinel.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final AppProperties appProperties;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(appProperties.jwt().accessTokenExpiry())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(appProperties.jwt().refreshTokenExpiry())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(appProperties.secureCookies())
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();
    }
}
