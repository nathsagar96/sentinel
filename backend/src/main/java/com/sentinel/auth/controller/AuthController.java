package com.sentinel.auth.controller;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = authService.login(request);
        AuthService.AuthTokens tokens = authService.issueTokens(user.id(), user.email(), user.name());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils
                                .createAccessTokenCookie(tokens.accessToken())
                                .toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils
                                .createRefreshTokenCookie(tokens.refreshToken())
                                .toString())
                .body(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String refreshToken =
                CookieUtils.extractCookie(request, "refresh_token").orElse(null);
        AuthService.AuthTokens tokens = authService.refreshTokens(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils
                                .createAccessTokenCookie(tokens.accessToken())
                                .toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils
                                .createRefreshTokenCookie(tokens.refreshToken())
                                .toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken =
                CookieUtils.extractCookie(request, "refresh_token").orElse(null);
        if (refreshToken != null) {
            try {
                authService.revokeRefreshToken(refreshToken);
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token: {}", e.getMessage());
            }
        }

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.clearAccessTokenCookie().toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.clearRefreshTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
