package com.sentinel.auth.controller;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.auth.service.AuthService;
import com.sentinel.exception.BadRequestException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
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
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;

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
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or missing refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserResponse user = authService.getCurrentUser(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.id(), user.email(), user.name());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieUtils.createAccessTokenCookie(newAccessToken).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
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
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
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
