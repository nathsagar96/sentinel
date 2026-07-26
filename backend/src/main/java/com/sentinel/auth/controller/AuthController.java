package com.sentinel.auth.controller;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.auth.service.AuthService;
import com.sentinel.auth.service.RefreshTokenService;
import com.sentinel.exception.BadRequestException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = authService.login(request);

        String accessToken = jwtTokenProvider.generateAccessToken(user.id(), user.email(), user.name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id());
        refreshTokenService.storeRefreshToken(user.id(), refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.createAccessTokenCookie(accessToken).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.createRefreshTokenCookie(refreshToken).toString())
                .body(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken, "refresh")) {
            throw new BadRequestException("Invalid or missing refresh token");
        }

        refreshTokenService.verifyRefreshToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserResponse user = authService.getCurrentUser(userId);

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenService.rotateRefreshToken(refreshToken, newRefreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.id(), user.email(), user.name());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.createAccessTokenCookie(newAccessToken).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.createRefreshTokenCookie(newRefreshToken).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken != null) {
            try {
                refreshTokenService.revokeRefreshToken(refreshToken);
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.clearAccessTokenCookie().toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.clearRefreshTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        String accessToken = extractCookie(request, "access_token");
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken, "access")) {
            throw new BadRequestException("Invalid or missing access token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
