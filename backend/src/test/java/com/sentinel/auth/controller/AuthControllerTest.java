package com.sentinel.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.auth.service.AuthService;
import com.sentinel.user.entity.AuthProvider;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CookieUtils cookieUtils;

    @Test
    void shouldSignupSuccessfully() throws Exception {
        var request = new SignupRequest("Test User", "test@example.com", "password123");
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void shouldReturn400_onInvalidSignupRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"invalid","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        var request = new LoginRequest("test@example.com", "password123");
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "Test User"))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(cookieUtils.createAccessTokenCookie("access-token"))
                .thenReturn(ResponseCookie.from("access_token", "access-token")
                        .maxAge(Duration.ofMinutes(15))
                        .build());
        when(cookieUtils.createRefreshTokenCookie("refresh-token"))
                .thenReturn(ResponseCookie.from("refresh_token", "refresh-token")
                        .maxAge(Duration.ofDays(7))
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(jwtTokenProvider.validateToken("valid-refresh-token", "refresh")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(response);
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "Test User"))
                .thenReturn("new-access-token");
        when(cookieUtils.createAccessTokenCookie("new-access-token"))
                .thenReturn(ResponseCookie.from("access_token", "new-access-token")
                        .maxAge(Duration.ofMinutes(15))
                        .build());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        when(cookieUtils.clearAccessTokenCookie())
                .thenReturn(ResponseCookie.from("access_token", "").maxAge(0).build());
        when(cookieUtils.clearRefreshTokenCookie())
                .thenReturn(ResponseCookie.from("refresh_token", "").maxAge(0).build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(jwtTokenProvider.validateToken("valid-access-token", "access")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-access-token")).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("access_token", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
