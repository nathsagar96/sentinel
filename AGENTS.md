# Sentinel Repository - Agent Guide

## Overview
Full-stack authentication system with Java Spring Boot backend and React/Vite frontend.

## Key Commands

### Backend
- **Run**: `mvn spring-boot:run`
- **Test**: `mvn test`
- **Format**: `mvn spotless:apply`
- **DB Login**: `psql -U postgres -d <db_name>`

### Frontend
- **Dev**: `cd frontend && npm run dev`
- **Build**: `cd frontend && npm run build`
- **Lint**: `cd frontend && npm run lint`

## Architecture
**Backend**: Java 25 + Spring Boot 4.1.0, `com.sentinel` package, JWT auth with OAuth2 (Google, GitHub), PostgreSQL via Docker Compose.

**Frontend**: React 19 + Vite, Tailwind + Shadcn UI, React Router, Axios client.

## Development
- Backend: Spring profiles, dotenv vars, PostgreSQL required
- Frontend: Install via `cd frontend && npm install`
- Artifacts: `frontend/dist/`

## Quick Entry
1. `mvn spring-boot:run` (backend)
2. `cd frontend && npm run dev` (frontend)
3. Browser: `http://localhost:5173` (frontend), API: `http://localhost:8080`

## Navigation
- Backend: `backend/src/main/java/com/sentinel/`
- Frontend: `frontend/src/`
- Security: `com.sentinel.security` package