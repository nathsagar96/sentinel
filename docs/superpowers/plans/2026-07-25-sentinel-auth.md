# Sentinel Auth System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full-stack auth system with email/password + Google/GitHub OAuth2, JWT cookies, React + Spring Boot.

**Architecture:** Monorepo with `backend/` (Spring Boot 4.1.0) and `frontend/` (React 19 + Vite 8). Backend handles all auth logic — JWT generation, OAuth2 dance, cookie management. Frontend is a thin client that redirects for OAuth2, calls REST endpoints for email/password, and relies on automatic cookie transmission.

**Tech Stack:** Spring Boot 4.1.0, Spring Security 7.x, Java 25, PostgreSQL 18, React 19, Vite 8, Tailwind CSS 4, ShadCN UI, React Router DOM 7.x, Axios

## Global Constraints

- Java 25, Spring Boot 4.1.0, Spring Security 7.x
- PostgreSQL 18-alpine via Docker Compose
- Constructor injection only (`@RequiredArgsConstructor`), no field injection
- Java Records for all DTOs and config properties
- ProblemDetail (RFC 7807) for all error responses
- Virtual threads enabled (`spring.threads.virtual.enabled=true`)
- HttpOnly + SameSite=Lax cookies for JWT tokens
- CORS: `http://localhost:5173` with credentials
- Tailwind CSS 4 uses CSS-first config (no `tailwind.config.js`)
- ShadCN UI with default Base UI primitives

---

### Task 1: Project Scaffolding & Docker Compose

**Files:**
- Create: `docker-compose.yml`
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/sentinel/SentinelApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/.env.example`

**Interfaces:**
- Consumes: nothing (first task)
- Produces: runnable Spring Boot app with PostgreSQL connection, `.env.example` with required env vars

- [ ] **Step 1: Create Docker Compose file**

```yaml
# docker-compose.yml (project root)
services:
  postgres:
    image: postgres:18-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: sentinel_auth
      POSTGRES_USER: sentinel
      POSTGRES_PASSWORD: sentinel
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

- [ ] **Step 2: Create .env.example**

```env
# backend/.env.example
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
JWT_SECRET=your-256-bit-secret-key-here-min-32-chars
```

- [ ] **Step 3: Create backend pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.sentinel</groupId>
    <artifactId>sentinel-auth</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>sentinel-auth</name>
    <description>Sentinel Auth System</description>

    <properties>
        <java.version>25</java.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- OAuth2 Client -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>

        <!-- JPA + PostgreSQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- JWT (jjwt) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create application.yml**

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sentinel_auth
    username: sentinel
    password: sentinel
    hikari:
      maximum-pool-size: 20
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email, read:user
  threads:
    virtual:
      enabled: true
  mvc:
    problemdetails:
      enabled: true

app:
  jwt:
    secret: ${JWT_SECRET:defaultDevSecretKeyThatIsAtLeast32CharactersLong}
    access-token-expiry: 15m
    refresh-token-expiry: 7d
  cors:
    allowed-origins: http://localhost:5173
  oauth2:
    redirect-uri: http://localhost:5173/oauth2/redirect
```

- [ ] **Step 5: Create SentinelApplication.java**

```java
package com.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SentinelApplication {
    public static void main(String[] args) {
        SpringApplication.run(SentinelApplication.class, args);
    }
}
```

- [ ] **Step 6: Start PostgreSQL and verify Spring Boot starts**

```bash
# From project root
docker compose up -d

# From backend/
./mvnw spring-boot:run
```

Expected: App starts on port 8080, connects to PostgreSQL (Spring Security will auto-generate a password since we haven't configured security yet — that's fine).

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: project scaffolding with Spring Boot 4.1.0 and Docker Compose"
```

---

### Task 2: User Entity, Repository & Exception Handling

**Files:**
- Create: `backend/src/main/java/com/sentinel/user/entity/User.java`
- Create: `backend/src/main/java/com/sentinel/user/entity/AuthProvider.java`
- Create: `backend/src/main/java/com/sentinel/user/repository/UserRepository.java`
- Create: `backend/src/main/java/com/sentinel/exception/ApplicationException.java`
- Create: `backend/src/main/java/com/sentinel/exception/ResourceNotFoundException.java`
- Create: `backend/src/main/java/com/sentinel/exception/DuplicateResourceException.java`
- Create: `backend/src/main/java/com/sentinel/exception/BadRequestException.java`
- Create: `backend/src/main/java/com/sentinel/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: PostgreSQL connection from Task 1
- Produces:
  - `User` entity with fields: `id`, `email`, `password`, `name`, `avatarUrl`, `provider`, `providerId`, `createdAt`, `updatedAt`
  - `AuthProvider` enum: `LOCAL`, `GOOGLE`, `GITHUB`
  - `UserRepository` with `Optional<User> findByEmail(String email)` and `boolean existsByEmail(String email)`
  - Sealed exception hierarchy: `ApplicationException` → `ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`
  - `GlobalExceptionHandler` returning ProblemDetail

- [ ] **Step 1: Create AuthProvider enum**

```java
package com.sentinel.user.entity;

public enum AuthProvider {
    LOCAL, GOOGLE, GITHUB
}
```

- [ ] **Step 2: Create User entity**

```java
package com.sentinel.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Create UserRepository**

```java
package com.sentinel.user.repository;

import com.sentinel.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 4: Create sealed exception hierarchy**

```java
// ApplicationException.java
package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public sealed abstract class ApplicationException extends RuntimeException
        permits ResourceNotFoundException, DuplicateResourceException, BadRequestException {

    private final HttpStatus httpStatus;

    protected ApplicationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
```

```java
// ResourceNotFoundException.java
package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id: %s".formatted(resource, id), HttpStatus.NOT_FOUND);
    }
}
```

```java
// DuplicateResourceException.java
package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class DuplicateResourceException extends ApplicationException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
```

```java
// BadRequestException.java
package com.sentinel.exception;

import org.springframework.http.HttpStatus;

public final class BadRequestException extends ApplicationException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
```

- [ ] **Step 5: Create GlobalExceptionHandler**

```java
package com.sentinel.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException ex) {
        log.warn("Application exception: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        problem.setTitle(ex.getClass().getSimpleName());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Validation failed");
        problem.setTitle("ValidationError");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", errors);
        return ResponseEntity.status(status).body(problem);
    }
}
```

- [ ] **Step 6: Restart app, verify `users` table is created**

```bash
docker compose exec postgres psql -U sentinel -d sentinel_auth -c "\dt"
```

Expected: `users` table exists with all columns.

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: User entity, repository, and sealed exception hierarchy"
```

---

### Task 3: JWT Token Provider & Cookie Utilities

**Files:**
- Create: `backend/src/main/java/com/sentinel/config/AppProperties.java`
- Create: `backend/src/main/java/com/sentinel/auth/jwt/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/sentinel/auth/jwt/CookieUtils.java`

**Interfaces:**
- Consumes: `app.jwt.secret`, `app.jwt.access-token-expiry`, `app.jwt.refresh-token-expiry` from application.yml (Task 1)
- Produces:
  - `AppProperties` record: `jwt()` returns `JwtProperties(secret, accessTokenExpiry, refreshTokenExpiry)`; `cors()` returns `CorsProperties(allowedOrigins)`; `oauth2()` returns `OAuth2Properties(redirectUri)`
  - `JwtTokenProvider.generateAccessToken(Long userId, String email, String name)` → `String`
  - `JwtTokenProvider.generateRefreshToken(Long userId)` → `String`
  - `JwtTokenProvider.validateToken(String token)` → `boolean`
  - `JwtTokenProvider.getUserIdFromToken(String token)` → `Long`
  - `CookieUtils.createAccessTokenCookie(String token)` → `ResponseCookie`
  - `CookieUtils.createRefreshTokenCookie(String token)` → `ResponseCookie`
  - `CookieUtils.clearAccessTokenCookie()` → `ResponseCookie`
  - `CookieUtils.clearRefreshTokenCookie()` → `ResponseCookie`

- [ ] **Step 1: Create AppProperties record**

```java
package com.sentinel.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid JwtProperties jwt,
        @Valid CorsProperties cors,
        @Valid OAuth2Properties oauth2
) {
    public record JwtProperties(
            @NotBlank String secret,
            @NotNull Duration accessTokenExpiry,
            @NotNull Duration refreshTokenExpiry
    ) {}

    public record CorsProperties(
            @NotBlank String allowedOrigins
    ) {}

    public record OAuth2Properties(
            @NotBlank String redirectUri
    ) {}
}
```

- [ ] **Step 2: Create JwtTokenProvider**

```java
package com.sentinel.auth.jwt;

import com.sentinel.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final AppProperties.JwtProperties jwtProperties;

    public JwtTokenProvider(AppProperties appProperties) {
        this.jwtProperties = appProperties.jwt();
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String email, String name) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessTokenExpiry().toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", name)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTokenExpiry().toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
```

- [ ] **Step 3: Create CookieUtils**

```java
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
                .secure(false) // set true in production
                .sameSite("Lax")
                .path("/")
                .maxAge(appProperties.jwt().accessTokenExpiry())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(false) // set true in production
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(appProperties.jwt().refreshTokenExpiry())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();
    }
}
```

- [ ] **Step 4: Verify app compiles**

```bash
cd backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: JWT token provider and cookie utilities"
```

---

### Task 4: Auth DTOs, Service & Controller (Email/Password)

**Files:**
- Create: `backend/src/main/java/com/sentinel/auth/dto/SignupRequest.java`
- Create: `backend/src/main/java/com/sentinel/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/sentinel/auth/dto/UserResponse.java`
- Create: `backend/src/main/java/com/sentinel/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/sentinel/auth/controller/AuthController.java`

**Interfaces:**
- Consumes:
  - `UserRepository.findByEmail(String)` → `Optional<User>`, `UserRepository.existsByEmail(String)` → `boolean` (Task 2)
  - `JwtTokenProvider.generateAccessToken(Long, String, String)` → `String`, `JwtTokenProvider.generateRefreshToken(Long)` → `String`, `JwtTokenProvider.getUserIdFromToken(String)` → `Long`, `JwtTokenProvider.validateToken(String)` → `boolean` (Task 3)
  - `CookieUtils.createAccessTokenCookie(String)` → `ResponseCookie`, `CookieUtils.createRefreshTokenCookie(String)` → `ResponseCookie`, `CookieUtils.clearAccessTokenCookie()` → `ResponseCookie`, `CookieUtils.clearRefreshTokenCookie()` → `ResponseCookie` (Task 3)
- Produces:
  - `SignupRequest` record: `name`, `email`, `password` (validated)
  - `LoginRequest` record: `email`, `password` (validated)
  - `UserResponse` record: `id`, `email`, `name`, `avatarUrl`, `provider`
  - `AuthService.signup(SignupRequest)` → `UserResponse`
  - `AuthService.login(LoginRequest)` → `User` (returns full entity for cookie generation)
  - `AuthService.getCurrentUser(Long userId)` → `UserResponse`
  - REST endpoints: `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/me`

- [ ] **Step 1: Create DTO records**

```java
// SignupRequest.java
package com.sentinel.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {}
```

```java
// LoginRequest.java
package com.sentinel.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
```

```java
// UserResponse.java
package com.sentinel.auth.dto;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        AuthProvider provider
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getProvider()
        );
    }
}
```

- [ ] **Step 2: Create AuthService**

```java
package com.sentinel.auth.service;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.exception.BadRequestException;
import com.sentinel.exception.DuplicateResourceException;
import com.sentinel.exception.ResourceNotFoundException;
import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        return user;
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.from(user);
    }
}
```

- [ ] **Step 3: Create AuthController**

```java
package com.sentinel.auth.controller;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.auth.service.AuthService;
import com.sentinel.exception.BadRequestException;
import com.sentinel.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

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
        User user = authService.login(request);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(accessToken).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(refreshToken).toString())
                .body(UserResponse.from(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or missing refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserResponse user = authService.getCurrentUser(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.id(), user.email(), user.name());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(newAccessToken).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtils.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.clearRefreshTokenCookie().toString())
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
```

- [ ] **Step 4: Verify app compiles**

```bash
cd backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: auth DTOs, service, and controller for email/password flow"
```

---

### Task 5: Security Config, CORS & JWT Filter

**Files:**
- Create: `backend/src/main/java/com/sentinel/auth/jwt/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/sentinel/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/sentinel/config/CorsConfig.java`

**Interfaces:**
- Consumes:
  - `JwtTokenProvider.validateToken(String)` → `boolean`, `JwtTokenProvider.getUserIdFromToken(String)` → `Long` (Task 3)
  - `UserRepository.findById(Long)` → `Optional<User>` (Task 2)
  - `AppProperties.cors().allowedOrigins()` → `String` (Task 3)
- Produces:
  - `JwtAuthenticationFilter` — `OncePerRequestFilter` that reads `access_token` cookie, validates JWT, sets `SecurityContextHolder` with `UsernamePasswordAuthenticationToken`
  - `SecurityConfig` — `SecurityFilterChain` bean: stateless, CSRF disabled, public/authenticated paths, JWT filter registration
  - `CorsConfig` — `CorsConfigurationSource` bean

- [ ] **Step 1: Create JwtAuthenticationFilter**

```java
package com.sentinel.auth.jwt;

import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromCookie(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 2: Create CorsConfig**

```java
package com.sentinel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(appProperties.cors().allowedOrigins()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 3: Create SecurityConfig**

```java
package com.sentinel.config;

import com.sentinel.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Test signup and login with curl**

```bash
# Start the app
cd backend && ./mvnw spring-boot:run

# Signup
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123"}'

# Login (check Set-Cookie headers)
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

Expected: Signup returns 201 with user JSON. Login returns 200 with user JSON and `Set-Cookie` headers for `access_token` and `refresh_token`.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: security config, CORS, and JWT authentication filter"
```

---

### Task 6: OAuth2 Integration (Google & GitHub)

**Files:**
- Create: `backend/src/main/java/com/sentinel/oauth2/OAuth2UserInfo.java`
- Create: `backend/src/main/java/com/sentinel/oauth2/CustomOAuth2UserService.java`
- Create: `backend/src/main/java/com/sentinel/oauth2/OAuth2AuthenticationSuccessHandler.java`
- Create: `backend/src/main/java/com/sentinel/oauth2/OAuth2AuthenticationFailureHandler.java`
- Modify: `backend/src/main/java/com/sentinel/config/SecurityConfig.java` (add OAuth2 login config)

**Interfaces:**
- Consumes:
  - `UserRepository.findByEmail(String)` → `Optional<User>`, `UserRepository.save(User)` → `User` (Task 2)
  - `JwtTokenProvider.generateAccessToken(Long, String, String)` → `String`, `JwtTokenProvider.generateRefreshToken(Long)` → `String` (Task 3)
  - `CookieUtils.createAccessTokenCookie(String)` → `ResponseCookie`, `CookieUtils.createRefreshTokenCookie(String)` → `ResponseCookie` (Task 3)
  - `AppProperties.oauth2().redirectUri()` → `String` (Task 3)
- Produces:
  - `OAuth2UserInfo` — record with static factory `OAuth2UserInfo.of(String registrationId, Map<String, Object> attributes)` extracting `name`, `email`, `avatarUrl`, `providerId`, `provider` per provider
  - `CustomOAuth2UserService` — extends `DefaultOAuth2UserService`, finds or creates `User` by email, returns `DefaultOAuth2User` with user ID as custom attribute
  - `OAuth2AuthenticationSuccessHandler` — generates JWT cookies, redirects to frontend
  - `OAuth2AuthenticationFailureHandler` — redirects to frontend with error
  - Updated `SecurityConfig` with `.oauth2Login()` block

- [ ] **Step 1: Create OAuth2UserInfo**

```java
package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;

import java.util.Map;

public record OAuth2UserInfo(
        String name,
        String email,
        String avatarUrl,
        String providerId,
        AuthProvider provider
) {
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new OAuth2UserInfo(
                    (String) attributes.get("name"),
                    (String) attributes.get("email"),
                    (String) attributes.get("picture"),
                    (String) attributes.get("sub"),
                    AuthProvider.GOOGLE
            );
            case "github" -> new OAuth2UserInfo(
                    (String) attributes.get("name") != null
                            ? (String) attributes.get("name")
                            : (String) attributes.get("login"),
                    (String) attributes.get("email"),
                    (String) attributes.get("avatar_url"),
                    String.valueOf(attributes.get("id")),
                    AuthProvider.GITHUB
            );
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }
}
```

- [ ] **Step 2: Create CustomOAuth2UserService**

```java
package com.sentinel.oauth2;

import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByEmail(userInfo.email())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo));

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("internal_user_id", user.getId());

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.name());
        user.setAvatarUrl(userInfo.avatarUrl());
        if (user.getProvider() != userInfo.provider()) {
            user.setProvider(userInfo.provider());
            user.setProviderId(userInfo.providerId());
        }
        return userRepository.save(user);
    }

    private User createNewUser(OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.email())
                .name(userInfo.name())
                .avatarUrl(userInfo.avatarUrl())
                .provider(userInfo.provider())
                .providerId(userInfo.providerId())
                .build();
        return userRepository.save(user);
    }
}
```

- [ ] **Step 3: Create OAuth2AuthenticationSuccessHandler**

```java
package com.sentinel.oauth2;

import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.config.AppProperties;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    private final AppProperties appProperties;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = (Long) oAuth2User.getAttributes().get("internal_user_id");

        User user = userRepository.findById(userId).orElseThrow();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        response.addHeader("Set-Cookie", cookieUtils.createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", cookieUtils.createRefreshTokenCookie(refreshToken).toString());

        String redirectUrl = appProperties.oauth2().redirectUri();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

- [ ] **Step 4: Create OAuth2AuthenticationFailureHandler**

```java
package com.sentinel.oauth2;

import com.sentinel.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        String redirectUrl = appProperties.oauth2().redirectUri().replace("/oauth2/redirect", "/login")
                + "?error=" + URLEncoder.encode(exception.getLocalizedMessage(), StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

- [ ] **Step 5: Update SecurityConfig with OAuth2 login**

Modify `SecurityConfig.java` — add these imports and fields:

```java
import com.sentinel.oauth2.CustomOAuth2UserService;
import com.sentinel.oauth2.OAuth2AuthenticationSuccessHandler;
import com.sentinel.oauth2.OAuth2AuthenticationFailureHandler;
```

Add these fields to the class:
```java
private final CustomOAuth2UserService customOAuth2UserService;
private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
```

Add to the http builder chain, after `.authorizeHttpRequests(...)` and before `.addFilterBefore(...)`:
```java
.oauth2Login(oauth2 -> oauth2
    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
    .successHandler(oAuth2SuccessHandler)
    .failureHandler(oAuth2FailureHandler)
)
```

- [ ] **Step 6: Verify app compiles and starts**

```bash
cd backend && ./mvnw compile
```

Expected: BUILD SUCCESS. (OAuth2 login won't work until real client IDs are set, but the code compiles and the app starts.)

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: OAuth2 integration for Google and GitHub"
```

---

### Task 7: Frontend Scaffolding (React + Vite + Tailwind + ShadCN)

**Files:**
- Create: `frontend/` (via Vite scaffold)
- Configure: Tailwind CSS 4, ShadCN UI
- Create: `frontend/src/api/axios.js`
- Create: `frontend/src/lib/utils.js`

**Interfaces:**
- Consumes: nothing (independent frontend task)
- Produces:
  - Vite 8 project with React 19, Tailwind CSS 4, ShadCN UI initialized
  - `api` Axios instance: `baseURL=http://localhost:8080`, `withCredentials: true`, 401 interceptor that calls `/api/auth/refresh` and retries
  - ShadCN components installed: `button`, `input`, `card`, `label`, `separator`, `sonner`
  - `cn()` utility in `src/lib/utils.js`

- [ ] **Step 1: Scaffold Vite project**

```bash
cd /Users/sagar/Developer/auth
npx -y create-vite@latest frontend -- --template react
cd frontend
npm install
```

- [ ] **Step 2: Install Tailwind CSS 4**

```bash
cd frontend
npm install tailwindcss @tailwindcss/vite
```

Update `vite.config.js`:
```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
})
```

Replace the contents of `src/index.css`:
```css
@import "tailwindcss";
```

- [ ] **Step 3: Install and initialize ShadCN UI**

```bash
cd frontend
npx -y shadcn@latest init
```

Follow prompts or use defaults. Then install needed components:

```bash
npx shadcn@latest add button input card label separator sonner
```

- [ ] **Step 4: Install dependencies**

```bash
cd frontend
npm install axios react-router-dom lucide-react
```

- [ ] **Step 5: Create Axios instance with interceptor**

```jsx
// frontend/src/api/axios.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await api.post('/api/auth/refresh');
        processQueue(null);
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

- [ ] **Step 6: Verify frontend starts**

```bash
cd frontend && npm run dev
```

Expected: Vite dev server starts on `http://localhost:5173`.

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: frontend scaffolding with React, Vite, Tailwind CSS 4, and ShadCN UI"
```

---

### Task 8: Auth Context & Protected Routes

**Files:**
- Create: `frontend/src/context/AuthContext.jsx`
- Create: `frontend/src/hooks/useAuth.js`
- Create: `frontend/src/components/layout/ProtectedRoute.jsx`

**Interfaces:**
- Consumes:
  - `api` Axios instance: `api.post('/api/auth/login', data)`, `api.post('/api/auth/signup', data)`, `api.post('/api/auth/logout')`, `api.get('/api/auth/me')` (Task 7)
- Produces:
  - `AuthProvider` component wrapping app, providing `AuthContext`
  - `useAuth()` hook returning `{ user, loading, login, signup, logout, loadUser }`
  - `ProtectedRoute` component: renders children if `user` is set, redirects to `/login` if not, shows loading spinner while checking

- [ ] **Step 1: Create AuthContext**

```jsx
// frontend/src/context/AuthContext.jsx
import { createContext, useState, useEffect, useCallback } from 'react';
import api from '../api/axios';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    try {
      const response = await api.get('/api/auth/me');
      setUser(response.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = async (email, password) => {
    const response = await api.post('/api/auth/login', { email, password });
    setUser(response.data);
    return response.data;
  };

  const signup = async (name, email, password) => {
    const response = await api.post('/api/auth/signup', { name, email, password });
    return response.data;
  };

  const logout = async () => {
    await api.post('/api/auth/logout');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout, loadUser }}>
      {children}
    </AuthContext.Provider>
  );
}
```

- [ ] **Step 2: Create useAuth hook**

```jsx
// frontend/src/hooks/useAuth.js
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

- [ ] **Step 3: Create ProtectedRoute**

```jsx
// frontend/src/components/layout/ProtectedRoute.jsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: auth context, useAuth hook, and protected route"
```

---

### Task 9: Auth Pages (Login, Signup, OAuth2 Redirect)

**Files:**
- Create: `frontend/src/components/auth/LoginForm.jsx`
- Create: `frontend/src/components/auth/SignupForm.jsx`
- Create: `frontend/src/components/auth/SocialLoginButtons.jsx`
- Create: `frontend/src/pages/LoginPage.jsx`
- Create: `frontend/src/pages/SignupPage.jsx`
- Create: `frontend/src/pages/OAuth2RedirectPage.jsx`

**Interfaces:**
- Consumes:
  - `useAuth()` → `{ login, signup, loadUser }` (Task 8)
  - ShadCN components: `Button`, `Input`, `Card`, `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter`, `Label`, `Separator` (Task 7)
  - `react-router-dom`: `useNavigate`, `Link`, `useSearchParams` (Task 7)
- Produces:
  - `LoginForm` — controlled form with email + password, validation, calls `login()`, navigates to `/dashboard` on success, shows toast on error
  - `SignupForm` — controlled form with name + email + password, validation, calls `signup()`, navigates to `/login` with success message
  - `SocialLoginButtons` — two buttons that redirect to `http://localhost:8080/oauth2/authorize/google` and `/github`
  - `LoginPage` — centered card with `LoginForm`, `Separator`, `SocialLoginButtons`, link to signup, shows error from query params
  - `SignupPage` — centered card with `SignupForm`, `Separator`, `SocialLoginButtons`, link to login
  - `OAuth2RedirectPage` — calls `loadUser()` on mount, redirects to `/dashboard`

- [ ] **Step 1: Create SocialLoginButtons**

```jsx
// frontend/src/components/auth/SocialLoginButtons.jsx
import { Button } from '../ui/button';

const BACKEND_URL = 'http://localhost:8080';

export function SocialLoginButtons() {
  const handleGoogleLogin = () => {
    window.location.href = `${BACKEND_URL}/oauth2/authorize/google`;
  };

  const handleGithubLogin = () => {
    window.location.href = `${BACKEND_URL}/oauth2/authorize/github`;
  };

  return (
    <div className="grid gap-3">
      <Button
        variant="outline"
        className="w-full gap-2"
        onClick={handleGoogleLogin}
      >
        <svg className="h-5 w-5" viewBox="0 0 24 24">
          <path
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
            fill="#4285F4"
          />
          <path
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            fill="#34A853"
          />
          <path
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
            fill="#FBBC05"
          />
          <path
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            fill="#EA4335"
          />
        </svg>
        Continue with Google
      </Button>
      <Button
        variant="outline"
        className="w-full gap-2"
        onClick={handleGithubLogin}
      >
        <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
        </svg>
        Continue with GitHub
      </Button>
    </div>
  );
}
```

- [ ] **Step 2: Create LoginForm**

```jsx
// frontend/src/components/auth/LoginForm.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { toast } from 'sonner';

export function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      await login(email, password);
      toast.success('Logged in successfully!');
      navigate('/dashboard');
    } catch (error) {
      const message = error.response?.data?.detail || 'Login failed. Please try again.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="grid gap-4">
      <div className="grid gap-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          placeholder="name@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoComplete="email"
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          autoComplete="current-password"
        />
      </div>
      <Button type="submit" className="w-full" disabled={isLoading}>
        {isLoading ? 'Signing in...' : 'Sign in'}
      </Button>
    </form>
  );
}
```

- [ ] **Step 3: Create SignupForm**

```jsx
// frontend/src/components/auth/SignupForm.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { toast } from 'sonner';

export function SignupForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { signup } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (password.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }

    setIsLoading(true);

    try {
      await signup(name, email, password);
      toast.success('Account created! Please sign in.');
      navigate('/login');
    } catch (error) {
      const message = error.response?.data?.detail || 'Signup failed. Please try again.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="grid gap-4">
      <div className="grid gap-2">
        <Label htmlFor="name">Full Name</Label>
        <Input
          id="name"
          type="text"
          placeholder="John Doe"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          autoComplete="name"
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          placeholder="name@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoComplete="email"
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={8}
          autoComplete="new-password"
        />
      </div>
      <Button type="submit" className="w-full" disabled={isLoading}>
        {isLoading ? 'Creating account...' : 'Create account'}
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: Create LoginPage**

```jsx
// frontend/src/pages/LoginPage.jsx
import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { LoginForm } from '../components/auth/LoginForm';
import { SocialLoginButtons } from '../components/auth/SocialLoginButtons';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Separator } from '../components/ui/separator';
import { toast } from 'sonner';

export function LoginPage() {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const error = searchParams.get('error');
    if (error) {
      toast.error(decodeURIComponent(error));
    }
  }, [searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background via-background to-muted p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">Welcome back</CardTitle>
          <CardDescription>Sign in to your account to continue</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-6">
          <SocialLoginButtons />
          <div className="relative">
            <Separator />
            <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-xs text-muted-foreground">
              OR
            </span>
          </div>
          <LoginForm />
          <p className="text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{' '}
            <Link to="/signup" className="font-medium text-primary underline-offset-4 hover:underline">
              Sign up
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 5: Create SignupPage**

```jsx
// frontend/src/pages/SignupPage.jsx
import { Link } from 'react-router-dom';
import { SignupForm } from '../components/auth/SignupForm';
import { SocialLoginButtons } from '../components/auth/SocialLoginButtons';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Separator } from '../components/ui/separator';

export function SignupPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background via-background to-muted p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">Create an account</CardTitle>
          <CardDescription>Get started with Sentinel</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-6">
          <SocialLoginButtons />
          <div className="relative">
            <Separator />
            <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-xs text-muted-foreground">
              OR
            </span>
          </div>
          <SignupForm />
          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
              Sign in
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 6: Create OAuth2RedirectPage**

```jsx
// frontend/src/pages/OAuth2RedirectPage.jsx
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function OAuth2RedirectPage() {
  const { loadUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadUser().then(() => {
      navigate('/dashboard', { replace: true });
    });
  }, [loadUser, navigate]);

  return (
    <div className="flex h-screen items-center justify-center">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="mt-4 text-sm text-muted-foreground">Completing sign in...</p>
      </div>
    </div>
  );
}
```

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: login, signup, and OAuth2 redirect pages with ShadCN UI"
```

---

### Task 10: Dashboard Page, Navbar & App Router

**Files:**
- Create: `frontend/src/pages/DashboardPage.jsx`
- Create: `frontend/src/components/layout/Navbar.jsx`
- Modify: `frontend/src/App.jsx` (replace default content with router)
- Modify: `frontend/src/main.jsx` (wrap with AuthProvider)

**Interfaces:**
- Consumes:
  - `useAuth()` → `{ user, logout }` (Task 8)
  - `ProtectedRoute` component (Task 8)
  - All page components: `LoginPage`, `SignupPage`, `DashboardPage`, `OAuth2RedirectPage` (Task 9)
  - `react-router-dom`: `BrowserRouter`, `Routes`, `Route`, `Navigate` (Task 7)
  - `AuthProvider` component (Task 8)
  - ShadCN `Toaster` from `sonner` (Task 7)
- Produces:
  - `DashboardPage` — shows user avatar, name, email, auth provider, logout button
  - `Navbar` — app name, user greeting, logout button
  - Complete `App.jsx` with routes: `/login`, `/signup`, `/dashboard`, `/oauth2/redirect`, `/` → redirect to `/dashboard`
  - `main.jsx` wrapping App with `BrowserRouter`, `AuthProvider`, and `Toaster`

- [ ] **Step 1: Create Navbar**

```jsx
// frontend/src/components/layout/Navbar.jsx
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { Button } from '../ui/button';
import { LogOut, Shield } from 'lucide-react';

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="border-b bg-card">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2">
          <Shield className="h-6 w-6 text-primary" />
          <span className="text-lg font-bold">Sentinel</span>
        </div>
        {user && (
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted-foreground">
              {user.name}
            </span>
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </Button>
          </div>
        )}
      </div>
    </nav>
  );
}
```

- [ ] **Step 2: Create DashboardPage**

```jsx
// frontend/src/pages/DashboardPage.jsx
import { useAuth } from '../hooks/useAuth';
import { Navbar } from '../components/layout/Navbar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Shield, Mail, User, Globe } from 'lucide-react';

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="mt-2 text-muted-foreground">Welcome back, {user?.name}!</p>
        </div>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <Card>
            <CardHeader className="flex flex-row items-center gap-4">
              {user?.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.name}
                  className="h-16 w-16 rounded-full ring-2 ring-primary/20"
                />
              ) : (
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <User className="h-8 w-8" />
                </div>
              )}
              <div>
                <CardTitle>{user?.name}</CardTitle>
                <CardDescription>Your profile</CardDescription>
              </div>
            </CardHeader>
            <CardContent className="grid gap-3">
              <div className="flex items-center gap-2 text-sm">
                <Mail className="h-4 w-4 text-muted-foreground" />
                <span>{user?.email}</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <Globe className="h-4 w-4 text-muted-foreground" />
                <span>Signed in via <strong>{user?.provider}</strong></span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <Shield className="h-4 w-4 text-muted-foreground" />
                <span className="rounded-full bg-green-500/10 px-2 py-0.5 text-xs font-medium text-green-600">
                  Authenticated
                </span>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
```

- [ ] **Step 3: Update App.jsx with routes**

```jsx
// frontend/src/App.jsx
import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { DashboardPage } from './pages/DashboardPage';
import { OAuth2RedirectPage } from './pages/OAuth2RedirectPage';
import { ProtectedRoute } from './components/layout/ProtectedRoute';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/oauth2/redirect" element={<OAuth2RedirectPage />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
```

- [ ] **Step 4: Update main.jsx**

```jsx
// frontend/src/main.jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from './components/ui/sonner';
import { AuthProvider } from './context/AuthContext';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
        <Toaster richColors position="top-right" />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>
);
```

- [ ] **Step 5: Clean up default Vite files**

Delete the following files that came with the Vite scaffold:
- `src/App.css`
- `src/assets/react.svg`
- `public/vite.svg`

- [ ] **Step 6: Test the full flow**

```bash
# Terminal 1: PostgreSQL
docker compose up -d

# Terminal 2: Backend
cd backend && ./mvnw spring-boot:run

# Terminal 3: Frontend
cd frontend && npm run dev
```

Expected:
- Navigate to `http://localhost:5173` → redirects to `/login`
- Sign up → success toast → redirected to `/login`
- Login → success toast → redirected to `/dashboard` showing user info
- Logout → redirected to `/login`
- OAuth2 buttons redirect to backend (will fail without real client IDs, but the redirect itself works)

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: dashboard, navbar, app router, and complete frontend wiring"
```

---

### Task 11: UI Polish & Dark Theme

**Files:**
- Modify: `frontend/src/index.css` (add dark mode, gradient background, custom styles)
- Modify: `frontend/index.html` (add dark class, Inter font, meta tags)

**Interfaces:**
- Consumes: All existing components and pages (Tasks 7-10)
- Produces:
  - Dark theme enabled by default via `class="dark"` on `<html>`
  - Inter font loaded from Google Fonts
  - Gradient backgrounds on auth pages
  - Smooth page transitions
  - Proper meta tags for SEO

- [ ] **Step 1: Update index.html**

```html
<!doctype html>
<html lang="en" class="dark">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="Sentinel - Secure authentication system with OAuth2 and JWT" />
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
    <title>Sentinel Auth</title>
  </head>
  <body class="font-sans antialiased">
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

- [ ] **Step 2: Update index.css with custom styles**

Append to `src/index.css` (after `@import "tailwindcss"`):

```css
@theme {
  --font-sans: 'Inter', ui-sans-serif, system-ui, sans-serif;
}

/* Smooth page transitions */
#root {
  min-height: 100vh;
}

/* Custom scrollbar for dark mode */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: hsl(var(--background));
}

::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.3);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.5);
}
```

- [ ] **Step 3: Verify the app looks polished**

```bash
cd frontend && npm run dev
```

Expected: Dark theme, Inter font, gradient backgrounds on auth pages, smooth transitions.

- [ ] **Step 4: Final commit**

```bash
git add .
git commit -m "feat: dark theme, Inter font, and UI polish"
```

---

## Summary

| Task | Description | Dependencies |
|------|-------------|-------------|
| 1 | Project Scaffolding & Docker Compose | — |
| 2 | User Entity, Repository & Exception Handling | Task 1 |
| 3 | JWT Token Provider & Cookie Utilities | Task 1 |
| 4 | Auth DTOs, Service & Controller | Tasks 2, 3 |
| 5 | Security Config, CORS & JWT Filter | Tasks 2, 3 |
| 6 | OAuth2 Integration (Google & GitHub) | Tasks 2, 3, 5 |
| 7 | Frontend Scaffolding | — |
| 8 | Auth Context & Protected Routes | Task 7 |
| 9 | Auth Pages (Login, Signup, OAuth2 Redirect) | Tasks 7, 8 |
| 10 | Dashboard, Navbar & App Router | Tasks 8, 9 |
| 11 | UI Polish & Dark Theme | Tasks 7–10 |

**Parallelizable:** Tasks 1-6 (backend) and Task 7 (frontend scaffolding) are independent. Tasks 2 and 3 can run in parallel. Tasks 4 and 5 can run in parallel after 2+3. Task 8 depends only on 7.
