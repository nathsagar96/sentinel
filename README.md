<div align="center">

# 🛡️ Sentinel

**A full-stack authentication system with OAuth2 social login, JWT sessions, and a modern React UI.**

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

</div>

---

## ✨ Features

- **Email / Password authentication** — secure sign-up and login with validation
- **OAuth2 Social Login** — one-click sign-in with **Google** and **GitHub**
- **JWT-based sessions** — stateless, short-lived access tokens with type discrimination (access vs refresh)
- **Refresh token revocation** — server-side token storage with SHA-256 hashing, rotation on refresh, and revocation on logout
- **Protected routes** — React Router guards for authenticated pages
- **Dark / Light theme** — system-aware theme toggle powered by `next-themes`
- **Dockerized database** — PostgreSQL spun up automatically via Docker Compose
- **Input validation** — Bean Validation on the backend, real-time feedback on the frontend
- **Observability** — health checks, metrics, structured ECS JSON logging with correlation IDs

---

## 🏗️ Tech Stack

### Backend

| Technology | Version | Purpose |
| --- | --- | --- |
| Java | 25 | Language |
| Spring Boot | 4.1.0 | Application framework |
| Spring Security + OAuth2 Client | — | Authentication & authorization |
| Spring Data JPA | — | ORM / database access |
| Spring Boot Actuator | — | Health checks, metrics, monitoring |
| JJWT | 0.13.0 | JWT creation & validation |
| PostgreSQL | 18 | Relational database |
| Lombok | — | Boilerplate reduction |
| Spotless (Palantir) | 3.8.0 | Code formatting |
| Testcontainers | — | Integration testing |

### Frontend

| Technology | Version | Purpose |
| --- | --- | --- |
| React | 19 | UI framework |
| Vite | 8 | Build tool & dev server |
| React Router | 7 | Client-side routing |
| Tailwind CSS v4 | — | Utility-first styling |
| shadcn/ui | — | Component library |
| Axios | — | HTTP client |
| Sonner | — | Toast notifications |
| next-themes | — | Theme management |

---

## 📁 Project Structure

```
sentinel/
├── backend/                  # Spring Boot application
│   ├── src/
│   │   └── main/java/com/sentinel/
│   │       ├── auth/         # Auth controllers, services, DTOs, JWT
│   │       ├── config/       # Security & app configuration
│   │       ├── exception/    # Global exception handling
│   │       ├── oauth2/       # OAuth2 success/failure handlers
│   │       └── user/         # User entity & repository
│   ├── src/main/resources/db/migration/  # Flyway migration scripts
│   ├── compose.yaml          # Docker Compose (PostgreSQL)
│   ├── .env.example          # Environment variable template
│   └── pom.xml
└── frontend/                 # React + Vite application
    └── src/
        ├── api/              # Axios API client
        ├── components/       # Reusable UI components
        ├── context/          # React context (auth state)
        ├── hooks/            # Custom React hooks
        ├── pages/            # Route-level page components
        │   ├── LoginPage.jsx
        │   ├── SignupPage.jsx
        │   ├── DashboardPage.jsx
        │   └── OAuth2RedirectPage.jsx
        └── lib/              # Utility helpers
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 25+** — [Download](https://openjdk.org/)
- **Maven** — bundled via `./mvnw`
- **Node.js 20+** — [Download](https://nodejs.org/)
- **Docker & Docker Compose** — [Download](https://docs.docker.com/get-docker/)
- A **Google** and/or **GitHub** OAuth2 application (see [Configuration](#️-configuration))

---

### 1. Clone the repository

```bash
git clone https://github.com/nathsagar96/sentinel.git
cd sentinel
```

### 2. Configure environment variables

```bash
cp backend/.env.example backend/.env
```

Open `backend/.env` and fill in your credentials:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
JWT_SECRET=your-256-bit-secret-key-here-min-32-chars
```

### 3. Start the backend

The backend automatically starts PostgreSQL via Spring Boot's Docker Compose integration.

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

---

## ⚙️ Configuration

### OAuth2 Setup

#### Google

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project → **APIs & Services** → **Credentials**
3. Create an **OAuth 2.0 Client ID** (Web application)
4. Add `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI

#### GitHub

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. **New OAuth App**
3. Set **Authorization callback URL** to `http://localhost:8080/login/oauth2/code/github`

### Production Profile

For production deployment, activate the `prod` Spring profile:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

The production profile requires these environment variables:

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) |
| `CORS_ALLOWED_ORIGIN` | Allowed frontend origin (e.g., `https://yourdomain.com`) |
| `OAUTH2_REDIRECT_URI` | OAuth2 redirect URI (e.g., `https://yourdomain.com/oauth2/redirect`) |

Secure defaults enabled: `SameSite=Strict` cookies, HSTS (1 year), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, no stacktraces in error responses.

---

## 🔒 Security

- **Token type discrimination** — JWT tokens include a `typ` claim (`access` or `refresh`) to prevent cross-use
- **Refresh token revocation** — Server-side storage with SHA-256 hashing; tokens are revoked on logout and rotated on refresh
- **Secure cookies** — `HttpOnly`, `SameSite=Strict`, `Secure` (in production)
- **Security headers** — `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security` (1 year)
- **CORS hardening** — Explicit allowed headers (`Content-Type`, `Authorization`, `X-Requested-With`)
- **Safe OAuth2 failure handling** — Generic error messages, no internal details exposed
- **Production secrets** — All secrets injected via environment variables, no hardcoded fallbacks

---

## 🧪 Running Tests

```bash
cd backend
./mvnw test
```

Integration tests use **Testcontainers** and spin up a real PostgreSQL instance automatically — no manual setup required.

## 📄 Database Migrations

The application uses **Flyway** for database schema management. Migrations are version-controlled and automatically applied on startup.

### Running Migrations

```bash
cd backend
./mvnw flyway:clean  # Clean the database (destructive)
./mvnw flyway:migrate  # Apply all pending migrations
./mvnw flyway:info    # Check migration status and version
./mvnw flyway:validate  # Validate that schema matches code
```

### Migration Files

Migrations are located in `backend/src/main/resources/db/migration/` and follow Flyway naming convention: `V{version}__{description}.sql`.

Example migration file:

```sql
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    name        VARCHAR(255) NOT NULL,
    avatar_url  VARCHAR(255),
    provider    VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255),
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6)
);

CREATE INDEX idx_users_provider_provider_id ON users (provider, provider_id);
```

---

## 🛠️ Development Commands

### Backend

| Command | Description |
| --- | --- |
| `./mvnw spring-boot:run` | Start the backend server |
| `./mvnw test` | Run all tests |
| `./mvnw spotless:apply` | Format code (Palantir style) |
| `./mvnw flyway:info` | Check migration status and version |
| `./mvnw flyway:migrate` | Apply all pending migrations |
| `./mvnw flyway:validate` | Validate that schema matches code |
| `curl http://localhost:8080/actuator/health` | Check application health |

### Observability

The backend produces structured JSON logs in **Elastic Common Schema (ECS)** format, compatible with ELK, Loki, CloudWatch, and other log aggregators. Each request is tagged with a `correlationId` (via `X-Correlation-Id` header or auto-generated UUID) for request tracing across log lines.

**Key logging conventions:**

- `AUTH_SUCCESS` — successful login (includes `user_id`, `provider`)
- `AUTH_FAILURE` — failed login attempt (includes masked email domain, reason)
- No PII (email addresses) logged in plaintext — domains are masked as `***@domain`
- MDC fields: `correlationId`, `userId`

### Frontend

| Command | Description |
| --- | --- |
| `npm run dev` | Start the dev server |
| `npm run build` | Build for production |
| `npm run lint` | Lint with oxlint |
| `npm run preview` | Preview the production build |

---

## 🔒 API Endpoints

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Login with email & password |
| `GET` | `/api/auth/me` | JWT | Get current user profile |
| `GET` | `/oauth2/authorization/google` | Public | Initiate Google OAuth2 flow |
| `GET` | `/oauth2/authorization/github` | Public | Initiate GitHub OAuth2 flow |
| `GET` | `/actuator/health` | Public | Application health status |
| `GET` | `/actuator/info` | Public | Application metadata |
| `GET` | `/actuator/metrics` | Public | Application metrics |

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Format your code: `./mvnw spotless:apply` (backend) / `npm run lint` (frontend)
4. Commit your changes: `git commit -m 'feat: add my feature'`
5. Push and open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](./LICENSE) file for details.
