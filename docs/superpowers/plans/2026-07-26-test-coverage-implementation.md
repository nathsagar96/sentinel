# Test Coverage Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add missing JUnit tests for AuthService, AuthController, and JwtAuthenticationFilter to increase test coverage.

**Architecture:** Unit tests for AuthService with Mockito mocks, WebMvcTest slice tests for AuthController, and unit tests for JwtAuthenticationFilter with mock servlet requests.

**Tech Stack:** JUnit 5, Mockito, AssertJ, Spring Boot Test, WebMvcTest

## Global Constraints

- Java 25 (per pom.xml)
- Spring Boot 4.1.0
- Use AssertJ for assertions (not JUnit assertions)
- Follow existing test patterns (see CookieUtilsTest, JwtTokenProviderTest)
- Test naming: `shouldDoX_whenY` pattern

---

### Task 1: AuthServiceTest - Unit Tests

**Files:**
- Create: `backend/src/test/java/com/sentinel/auth/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: UserRepository, PasswordEncoder (mocked)
- Produces: Test coverage for AuthService.signup(), login(), getCurrentUser()

- [ ] **Step 1: Create AuthServiceTest with signup tests**

```java
package com.sentinel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.exception.BadRequestException;
import com.sentinel.exception.DuplicateResourceException;
import com.sentinel.exception.ResourceNotFoundException;
import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldSignupSuccessfully() {
        var request = new SignupRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = authService.signup(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.provider()).isEqualTo(AuthProvider.LOCAL);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrow_whenEmailAlreadyExists() {
        var request = new SignupRequest("Test User", "existing@example.com", "password123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }
}
```

- [ ] **Step 2: Add login tests to AuthServiceTest**

```java
    @Test
    void shouldLoginSuccessfully() {
        var request = new com.sentinel.auth.dto.LoginRequest("test@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("encoded_password")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        UserResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrow_whenUserNotFound_login() {
        var request = new com.sentinel.auth.dto.LoginRequest("notfound@example.com", "password123");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrow_whenPasswordMismatch() {
        var request = new com.sentinel.auth.dto.LoginRequest("test@example.com", "wrongpassword");
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrow_whenOAuthUserTriesPasswordLogin() {
        var request = new com.sentinel.auth.dto.LoginRequest("oauth@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("oauth@example.com")
                .password(null)
                .provider(AuthProvider.GOOGLE)
                .build();
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Google");
    }
```

- [ ] **Step 3: Add getCurrentUser tests to AuthServiceTest**

```java
    @Test
    void shouldGetCurrentUserSuccessfully() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test User");
    }

    @Test
    void shouldThrow_whenUserNotFound_getCurrentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
```

- [ ] **Step 4: Run AuthServiceTest to verify all tests pass**

Run: `mvn test -pl backend -Dtest=AuthServiceTest -q`
Expected: All 8 tests PASS

- [ ] **Step 5: Commit AuthServiceTest**

```bash
git add backend/src/test/java/com/sentinel/auth/service/AuthServiceTest.java
git commit -m "test: add AuthService unit tests for signup, login, getCurrentUser"
```

---

### Task 2: AuthControllerTest - WebMvcTest Slice Tests

**Files:**
- Create: `backend/src/test/java/com/sentinel/auth/controller/AuthControllerTest.java`

**Interfaces:**
- Consumes: AuthService, JwtTokenProvider, CookieUtils (mocked)
- Produces: Test coverage for AuthController REST endpoints

- [ ] **Step 1: Create AuthControllerTest with signup and login tests**

```java
package com.sentinel.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers cookie;
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
import org.springframework.boot.test.autoconfigure.webmvc.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
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
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "Test User")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(cookieUtils.createAccessTokenCookie("access-token")).thenReturn(
                Cookie.builder().name("access_token").value("access-token").maxAge(Duration.ofMinutes(15)).build());
        when(cookieUtils.createRefreshTokenCookie("refresh-token")).thenReturn(
                Cookie.builder().name("refresh_token").value("refresh-token").maxAge(Duration.ofDays(7)).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
```

- [ ] **Step 2: Add refresh, logout, and me endpoint tests**

```java
    @Test
    void shouldRefreshAccessToken() throws Exception {
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(response);
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "Test User")).thenReturn("new-access-token");
        when(cookieUtils.createAccessTokenCookie("new-access-token")).thenReturn(
                Cookie.builder().name("access_token").value("new-access-token").maxAge(Duration.ofMinutes(15)).build());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        when(cookieUtils.clearAccessTokenCookie()).thenReturn(
                Cookie.builder().name("access_token").value("").maxAge(0).build());
        when(cookieUtils.clearRefreshTokenCookie()).thenReturn(
                Cookie.builder().name("refresh_token").value("").maxAge(0).build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        var response = new UserResponse(1L, "test@example.com", "Test User", null, AuthProvider.LOCAL);
        when(jwtTokenProvider.validateToken("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-access-token")).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("access_token", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
```

- [ ] **Step 3: Run AuthControllerTest to verify all tests pass**

Run: `mvn test -pl backend -Dtest=AuthControllerTest -q`
Expected: All 6 tests PASS

- [ ] **Step 4: Commit AuthControllerTest**

```bash
git add backend/src/test/java/com/sentinel/auth/controller/AuthControllerTest.java
git commit -m "test: add AuthController WebMvcTest slice tests"
```

---

### Task 3: JwtAuthenticationFilterTest - Unit Tests

**Files:**
- Create: `backend/src/test/java/com/sentinel/auth/jwt/JwtAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: JwtTokenProvider (mocked)
- Produces: Test coverage for JwtAuthenticationFilter.doFilterInternal()

- [ ] **Step 1: Create JwtAuthenticationFilterTest**

```java
package com.sentinel.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldSetAuthentication_whenValidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("access_token", "valid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(123L);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(123L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthentication_whenInvalidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("access_token", "invalid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthentication_whenNoToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
```

- [ ] **Step 2: Run JwtAuthenticationFilterTest to verify all tests pass**

Run: `mvn test -pl backend -Dtest=JwtAuthenticationFilterTest -q`
Expected: All 3 tests PASS

- [ ] **Step 3: Commit JwtAuthenticationFilterTest**

```bash
git add backend/src/test/java/com/sentinel/auth/jwt/JwtAuthenticationFilterTest.java
git commit -m "test: add JwtAuthenticationFilter unit tests"
```

---

### Task 4: Run All Tests and Verify Coverage

- [ ] **Step 1: Run full test suite**

Run: `mvn test -pl backend -q`
Expected: All tests PASS (existing + new)

- [ ] **Step 2: Generate coverage report (if JaCoCo configured)**

Run: `mvn test jacoco:report -pl backend -q`
Expected: Report at `backend/target/site/jacoco/index.html`

- [ ] **Step 3: Final commit with all changes**

```bash
git add -A
git commit -m "test: complete test coverage enhancement for auth module"
```
