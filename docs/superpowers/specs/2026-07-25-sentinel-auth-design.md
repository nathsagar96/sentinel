# Sentinel Auth System — Design Spec

## Overview

Full-stack authentication system with email/password signup+login, Google and GitHub OAuth2, JWT-based session management via HttpOnly secure cookies.

**Tech Stack Versions:**

| Technology | Version |
|-----------|--------|
| Spring Boot | 4.1.0 |
| Spring Security | 7.x (bundled with Boot 4.1.0) |
| Java | 25 |
| React | 19.x |
| Vite | 8.x (Rolldown bundler) |
| Tailwind CSS | 4.x (CSS-first config, no JS config file) |
| ShadCN UI | latest CLI (Base UI default) |
| React Router | 7.x (latest stable, `react-router-dom`) |
| Axios | latest |
| PostgreSQL | 18-alpine |
| Docker Compose | v2 |

**Monorepo layout:**

```
auth/
├── backend/            # Spring Boot 4.1.0, Java 25
├── frontend/           # React 19 + Vite 8 + Tailwind CSS 4 + ShadCN UI
└── docker-compose.yml  # PostgreSQL 18
```

## Auth Flows

### Email/Password

1. **Signup**: `POST /api/auth/signup` — validates input, hashes password (BCrypt), creates user with `provider=LOCAL`, returns 201 with user info (no auto-login).
2. **Login**: `POST /api/auth/login` — validates credentials, generates access + refresh JWTs, sets both as HttpOnly cookies, returns user info in response body.
3. **Refresh**: `POST /api/auth/refresh` — reads `refresh_token` cookie, validates, issues new `access_token` cookie. Refresh token itself is NOT rotated (simplicity).
4. **Logout**: `POST /api/auth/logout` — clears both cookies (sets Max-Age=0).
5. **Me**: `GET /api/auth/me` — reads `access_token` cookie, validates JWT, returns current user info.

### OAuth2 (Google & GitHub) — Server-Side Flow

1. Frontend redirects browser to `GET /oauth2/authorize/google` (or `/github`).
2. Spring Security redirects to the provider's consent screen.
3. Provider redirects back to `GET /login/oauth2/code/{provider}` with auth code.
4. Spring Security exchanges code for tokens, fetches user info via `CustomOAuth2UserService`.
5. `OAuth2AuthenticationSuccessHandler`:
   - Finds or creates user by email. If user exists with `provider=LOCAL`, links the OAuth provider (updates provider + providerId).
   - Generates access + refresh JWTs.
   - Sets HttpOnly cookies.
   - Redirects to `http://localhost:5173/oauth2/redirect`.
6. `OAuth2AuthenticationFailureHandler`: Redirects to `http://localhost:5173/login?error=<message>`.

## JWT Cookie Strategy

| Cookie | Lifetime | HttpOnly | Secure | SameSite | Path |
|--------|----------|----------|--------|----------|------|
| `access_token` | 15 minutes | Yes | Yes (prod) | Lax | `/` |
| `refresh_token` | 7 days | Yes | Yes (prod) | Lax | `/api/auth/refresh` |

- Frontend never reads or stores tokens — cookies sent automatically.
- `SameSite=Lax` allows OAuth2 redirect flows while blocking CSRF on POST from other origins.
- `Secure` flag disabled in dev (HTTP), enabled in prod (HTTPS).

## JWT Token Structure

**Access Token Claims:**
- `sub`: user ID (Long)
- `email`: user email
- `name`: display name
- `iat`: issued at
- `exp`: expiration (15 min)

**Refresh Token Claims:**
- `sub`: user ID (Long)
- `iat`: issued at
- `exp`: expiration (7 days)

Signing algorithm: HMAC-SHA256. Secret loaded from `app.jwt.secret` property.

## Backend Architecture

### Package Structure

```
com.sentinel/
├── SentinelApplication.java
├── config/
│   ├── SecurityConfig.java        # SecurityFilterChain, CORS, OAuth2, session policy
│   ├── CorsConfig.java            # Allowed origins, methods, credentials
│   └── AppProperties.java         # @ConfigurationProperties record (jwt secret, expiry)
├── auth/
│   ├── controller/
│   │   └── AuthController.java    # /api/auth/** endpoints
│   ├── dto/
│   │   ├── SignupRequest.java     # record: email, password, name (validated)
│   │   ├── LoginRequest.java      # record: email, password (validated)
│   │   └── UserResponse.java      # record: id, email, name, avatarUrl, provider
│   ├── service/
│   │   └── AuthService.java       # signup, login, refresh, getCurrentUser
│   └── jwt/
│       ├── JwtTokenProvider.java       # generate, validate, extract claims
│       ├── JwtAuthenticationFilter.java # OncePerRequestFilter, reads cookie
│       └── CookieUtils.java            # create/clear HttpOnly cookies
├── oauth2/
│   ├── OAuth2AuthenticationSuccessHandler.java
│   ├── OAuth2AuthenticationFailureHandler.java
│   ├── CustomOAuth2UserService.java     # loads/creates user from OAuth2 profile
│   └── OAuth2UserInfo.java              # extracts name/email/avatar per provider
├── user/
│   ├── entity/
│   │   └── User.java              # JPA entity
│   └── repository/
│       └── UserRepository.java    # findByEmail, existsByEmail
└── exception/
    ├── ApplicationException.java          # sealed abstract
    ├── ResourceNotFoundException.java     # 404
    ├── DuplicateResourceException.java    # 409
    ├── BadRequestException.java           # 400
    └── GlobalExceptionHandler.java        # ProblemDetail (RFC 7807)
```

### User Entity

```java
@Entity
@Table(name = "users")
public class User {
    Long id;                          // @GeneratedValue(IDENTITY)
    String email;                     // @Column(unique = true, nullable = false)
    String password;                  // nullable (OAuth users have no password)
    String name;                      // @Column(nullable = false)
    String avatarUrl;                 // nullable
    AuthProvider provider;            // enum: LOCAL, GOOGLE, GITHUB
    String providerId;                // nullable (null for LOCAL)
    LocalDateTime createdAt;          // @CreationTimestamp
    LocalDateTime updatedAt;          // @UpdateTimestamp
}
```

### Security Filter Chain

```
Request → CorsFilter → JwtAuthenticationFilter → SecurityFilterChain
```

- Session: `STATELESS`
- CSRF: disabled (cookies are SameSite=Lax, stateless JWT)
- Public paths: `/api/auth/signup`, `/api/auth/login`, `/oauth2/**`, `/login/oauth2/**`
- Authenticated paths: `/api/auth/me`, `/api/auth/logout`, `/api/auth/refresh`
- OAuth2 login: configured with `CustomOAuth2UserService`, success/failure handlers
- `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`

### Error Handling

Sealed exception hierarchy with ProblemDetail (RFC 7807):

- `ApplicationException` (sealed) → `ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`
- `GlobalExceptionHandler` (@RestControllerAdvice) catches all and returns ProblemDetail JSON.

### Configuration

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sentinel_auth
    username: sentinel
    password: sentinel
  jpa:
    hibernate.ddl-auto: update
    open-in-view: false
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

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry: 15m
    refresh-token-expiry: 7d
  cors:
    allowed-origins: http://localhost:5173
  oauth2:
    redirect-uri: http://localhost:5173/oauth2/redirect
```

## Frontend Architecture

### Tech Stack
- React 19 + Vite
- Tailwind CSS v4
- ShadCN UI (Button, Input, Card, Label, Separator, Sonner/Toast)
- Axios with interceptors
- React Router DOM v7

### Page Structure

| Route | Page | Auth Required |
|-------|------|---------------|
| `/login` | LoginPage | No |
| `/signup` | SignupPage | No |
| `/dashboard` | DashboardPage | Yes |
| `/oauth2/redirect` | OAuth2RedirectPage | No (transient) |

### Components

```
src/
├── components/
│   ├── ui/                    # ShadCN: Button, Input, Card, Label, Separator
│   ├── auth/
│   │   ├── LoginForm.jsx      # Email + password fields, validation, submit
│   │   ├── SignupForm.jsx     # Name + email + password fields, validation, submit
│   │   └── SocialLoginButtons.jsx  # Google + GitHub buttons (redirect to backend)
│   └── layout/
│       ├── Navbar.jsx         # Logo, user info, logout button
│       └── ProtectedRoute.jsx # Redirects to /login if not authenticated
├── pages/
│   ├── LoginPage.jsx          # LoginForm + SocialLoginButtons + link to signup
│   ├── SignupPage.jsx         # SignupForm + SocialLoginButtons + link to login
│   ├── DashboardPage.jsx      # Welcome message, user info, protected content
│   └── OAuth2RedirectPage.jsx # Calls /api/auth/me to load user, redirects to dashboard
├── api/
│   └── axios.js               # baseURL=http://localhost:8080, withCredentials=true, 401 interceptor
├── context/
│   └── AuthContext.jsx        # user state, login/signup/logout/loadUser functions
├── hooks/
│   └── useAuth.js             # useContext(AuthContext) shortcut
└── lib/
    └── utils.js               # ShadCN cn() utility
```

### Axios Configuration

- `baseURL`: `http://localhost:8080`
- `withCredentials: true` (sends cookies cross-origin)
- Response interceptor: on 401, attempt `POST /api/auth/refresh`. If refresh succeeds, retry original request. If refresh fails, redirect to `/login`.

### Auth Context

```
AuthContext provides:
  - user: object | null
  - loading: boolean
  - login(email, password): Promise
  - signup(name, email, password): Promise
  - logout(): Promise
  - loadUser(): Promise  (calls /api/auth/me)
```

On app mount, `loadUser()` is called to check if valid cookies exist.

## Docker Compose

```yaml
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

## CORS Configuration

- Allowed origins: `http://localhost:5173`
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `*`
- Allow credentials: `true` (required for cookies)
- Max age: `3600`

## UI Design

- Dark theme by default with ShadCN's dark mode
- Auth pages: centered card layout with gradient background
- Social login buttons with provider icons (Google, GitHub)
- Form validation with inline error messages
- Toast notifications for success/error feedback
- Dashboard: minimal, shows user avatar, name, email, provider used
- Smooth transitions between pages
