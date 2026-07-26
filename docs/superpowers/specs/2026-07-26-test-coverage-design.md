# Test Coverage Enhancement Design

**Date**: 2026-07-26
**Scope**: AuthService, AuthController, JwtAuthenticationFilter

## Current State

Existing test files:
- `GlobalExceptionHandlerTest` - Exception handling
- `UserRepositoryTest` - Repository with Testcontainers
- `CookieUtilsTest` - Cookie utilities
- `JwtTokenProviderTest` - JWT token generation/validation

Missing test coverage:
- `AuthService` - signup, login, getCurrentUser methods
- `AuthController` - REST endpoints
- `JwtAuthenticationFilter` - JWT authentication filter

## Proposed Tests

### 1. AuthServiceTest (Unit Tests)

**File**: `backend/src/test/java/com/sentinel/auth/service/AuthServiceTest.java`

**Strategy**: Unit tests with Mockito mocking `UserRepository` and `PasswordEncoder`.

**Test Cases**:

| Test Name | Method | Scenario |
|-----------|--------|----------|
| `shouldSignupSuccessfully` | signup() | New user registration with valid input |
| `shouldThrow_whenEmailAlreadyExists` | signup() | Duplicate email handling |
| `shouldLoginSuccessfully` | login() | Valid credentials authentication |
| `shouldThrow_whenUserNotFound` | login() | Invalid email address |
| `shouldThrow_whenPasswordMismatch` | login() | Wrong password provided |
| `shouldThrow_whenOAuthUserTriesPasswordLogin` | login() | OAuth user without password attempts login |
| `shouldGetCurrentUserSuccessfully` | getCurrentUser() | Fetch existing user by ID |
| `shouldThrow_whenUserNotFound_getCurrentUser` | getCurrentUser() | Invalid user ID lookup |

**Dependencies to Mock**:
- `UserRepository` - Database operations
- `PasswordEncoder` - Password hashing/matching

### 2. AuthControllerTest (WebMvcTest Slice Tests)

**File**: `backend/src/test/java/com/sentinel/auth/controller/AuthControllerTest.java`

**Strategy**: `@WebMvcTest` slice tests for controller layer isolation.

**Test Cases**:

| Test Name | Endpoint | Scenario |
|-----------|----------|----------|
| `shouldSignupSuccessfully` | POST /api/auth/signup | Valid signup request |
| `shouldReturn400_onInvalidSignupRequest` | POST /api/auth/signup | Missing/invalid fields |
| `shouldLoginSuccessfully` | POST /api/auth/login | Valid login credentials |
| `shouldRefreshAccessToken` | POST /api/auth/refresh | Valid refresh token |
| `shouldLogoutSuccessfully` | POST /api/auth/logout | Clear cookies |
| `shouldReturnCurrentUser` | GET /api/auth/me | Valid access token |

**Dependencies to Mock**:
- `AuthService` - Business logic
- `JwtTokenProvider` - Token operations
- `CookieUtils` - Cookie creation

### 3. JwtAuthenticationFilterTest (Unit Tests)

**File**: `backend/src/test/java/com/sentinel/auth/jwt/JwtAuthenticationFilterTest.java`

**Strategy**: Unit tests with mocked dependencies and mock servlet requests.

**Test Cases**:

| Test Name | Scenario |
|-----------|----------|
| `shouldSetAuthentication_whenValidToken` | Valid JWT in access_token cookie |
| `shouldNotSetAuthentication_whenInvalidToken` | Invalid JWT token |
| `shouldNotSetAuthentication_whenNoToken` | No cookie present |

**Dependencies to Mock**:
- `JwtTokenProvider` - Token validation/extraction
- `FilterChain` - Request processing

## Implementation Approach

1. **AuthServiceTest**: Pure unit tests, no Spring context required
2. **AuthControllerTest**: `@WebMvcTest` for fast controller testing
3. **JwtAuthenticationFilterTest**: Unit tests with mock servlet requests

## Expected Coverage

After implementation:
- **AuthService**: 100% method coverage
- **AuthController**: All endpoints covered
- **JwtAuthenticationFilter**: Core filter logic covered

## File Changes

| File | Action |
|------|--------|
| `backend/src/test/java/com/sentinel/auth/service/AuthServiceTest.java` | Create |
| `backend/src/test/java/com/sentinel/auth/controller/AuthControllerTest.java` | Create |
| `backend/src/test/java/com/sentinel/auth/jwt/JwtAuthenticationFilterTest.java` | Create |
