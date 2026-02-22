# Architecture Guide

Overview of the application's tech stack, module structure, security model, database strategy, and configuration system.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 4.0.1 |
| Core | Spring Framework | 7 |
| Language | Java | 21 |
| Build | Maven | 3.9+ (via wrapper) |
| Architecture | Spring Modulith | 2.0.1 |
| Security | Spring Security | Dual filter chain (JWT + session) |
| JWT | JJWT (with Gson backend) | 0.12.6 |
| Templates | Thymeleaf + Layout Dialect | -- |
| CSS | Bootstrap 5 (WebJars) | 5.3.3 |
| Validation | Bean Validation (Hibernate Validator) | -- |
| Email | Spring Boot Mail | -- |
| Database (dev) | H2 (PostgreSQL compatibility mode) | -- |
| Database (prod) | PostgreSQL | 17 (Alpine) |
| Migrations | Flyway | -- |
| API Docs | SpringDoc OpenAPI | 3.0.1 |
| Monitoring | Spring Boot Actuator | -- |

> **Why JJWT with Gson?** Spring Boot 4 ships Jackson 3, but JJWT's Jackson module targets Jackson 2. Using the Gson backend avoids the Jackson 2/3 conflict entirely.

## Module Structure

The application uses Spring Modulith to enforce module boundaries at compile time and runtime. Three modules with a clear dependency hierarchy:

```
auth -> user -> shared
```

### shared -- Cross-cutting Concerns

| Package | Contents |
|---------|----------|
| `shared.dto` | `UserDto` -- cross-module user representation (no sensitive fields) |
| `shared.config` | `AppProperties` (type-safe `@ConfigurationProperties`), `PasswordConfig` (BCrypt encoder), `OpenApiConfig` (Swagger setup) |
| `shared.exception` | `DuplicateResourceException`, `ResourceNotFoundException`, `GlobalExceptionHandler` |
| `shared.entity` | Base entity classes |
| `shared.web` | `HomeController` -- landing page |

### user -- User Management

| Package | Contents |
|---------|----------|
| `user` (root) | `UserService` -- **sole public API** for the module |
| `user.internal` | `AppUser`, `AppRole` JPA entities; `UserRepository`, `RoleRepository`; admin DTOs (`CreateUserRequest`, `UpdateUserRequest`) |

External modules access user data only through `UserService`. Entities and repositories are module-private (`internal` package).

### auth -- Authentication and Authorization

| Package | Contents |
|---------|----------|
| `auth` (root) | `JwtService` -- **public API** (token creation/validation) |
| `auth.internal` | `AuthService`, `AuthController`, `AuthWebController` (login/register); `SecurityConfig`, `JwtAuthenticationFilter` (security infrastructure); `CustomUserDetailsService` |
| `auth.internal.verification` | `VerificationService`, `VerificationToken`, `VerificationTokenRepository` |
| `auth.internal.password` | `PasswordService`, `PasswordResetToken`, `PasswordResetTokenRepository`, `PasswordController`, `PasswordWebController` |
| `auth.internal.admin` | `AdminController`, `AdminWebController`, `AdminInviteService` |

## Security Architecture

### Dual SecurityFilterChain

The application uses two Spring Security filter chains to support both REST API clients and browser-based users:

**API Chain** (`@Order(1)` -- matches `/api/**`):
- Stateless session management (no cookies)
- JWT Bearer token authentication via `JwtAuthenticationFilter`
- No CSRF protection (stateless)
- Returns JSON error responses (ProblemDetail format)

**Web Chain** (`@Order(2)` -- matches everything else):
- Session-based authentication
- Form login at `/login`
- Remember-me with 7-day validity
- CSRF protection enabled
- Redirects to login page for unauthenticated access

### Authentication Flow (API)

```
1. Client sends POST /api/v1/auth/login with {email, password}
2. AuthService validates credentials via CustomUserDetailsService
3. JwtService creates a signed JWT token
4. Client includes token in subsequent requests: Authorization: Bearer <token>
5. JwtAuthenticationFilter intercepts API requests, validates token, sets SecurityContext
```

### Key Security Decisions

| Feature | Implementation | Reason |
|---------|---------------|--------|
| Password hashing | BCrypt | Industry standard, configurable work factor |
| No user enumeration | Generic "Bad credentials" for all auth failures | SEC-01 compliance |
| Email verification gate | Unverified accounts get "Bad credentials" | Same error as non-existent account |
| JWT invalidation on password change | `password_changed_at` timestamp checked against token `iat` | Tokens issued before password change are rejected |
| Non-root Docker container | `appuser` in Dockerfile | Defense in depth |

## Database Strategy

### Dual Database with Zero Code Changes

| Environment | Database | Connection |
|-------------|----------|------------|
| Dev | H2 (in-memory) | Automatic, zero setup |
| Prod | PostgreSQL 17 | Docker Compose service |

H2 runs in PostgreSQL compatibility mode (`MODE=PostgreSQL`, `DATABASE_TO_LOWER=TRUE`, `DEFAULT_NULL_ORDERING=HIGH`) so the same SQL works in both environments.

### Migration Management

Flyway manages all schema changes. Migrations are organized by database vendor:

```
src/main/resources/db/migration/
  common/           # Shared migrations (both H2 and PostgreSQL)
  h2/               # H2-specific (seed data for dev)
  postgresql/       # PostgreSQL-specific (if needed)
```

The `{vendor}` placeholder in the Flyway configuration automatically selects the correct directory at runtime.

**Key rules:**
- `ddl-auto: validate` -- Flyway owns the schema; Hibernate only validates
- `open-in-view: false` -- No lazy loading outside transactions
- Seed data (test user, roles) lives only in `h2/` -- production starts clean

### Entity Design

- Table `app_user` (avoids SQL reserved word `user`)
- `GENERATED BY DEFAULT AS IDENTITY` for IDs (H2/PostgreSQL compatible)
- `ON DELETE CASCADE` on all foreign keys for clean user deletion
- `TIMESTAMP` without timezone (application handles timezone)

## Configuration System

The project uses a template-based configuration pipeline:

```
.env.template ──> bin/setup.sh generates ──> .env (your values)
                                               |
.env + .env.local ──> bin/env.sh load ──> Shell environment
                                               |
*.template files ──> bin/env.sh substitute-all ──> Config files
```

### How It Works

1. `.env.template` defines all variables with `CHANGE_ME` placeholders
2. `bin/setup.sh` copies it to `.env` on first run
3. Developer edits `.env` with real values (and optionally `.env.local` for personal overrides)
4. `bin/env.sh load` exports variables into the shell
5. `bin/env.sh substitute-all` reads each `.template` file, replaces `@VARIABLE@` placeholders with environment values, and writes the output file (same path minus `.template` extension)
6. Generated files (`.yml`, `Dockerfile`, `compose.yaml`) are gitignored

### Template Listing

All templates are registered in `bin/env-templates.list`. Adding a new template requires only adding a line to this file -- no script changes needed.

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Spring Modulith over microservices | Single deployable with enforced module boundaries. Simpler for user management scope; can extract to microservices later if needed. |
| Email as username | Simplifies login flow (one field instead of two). Email is already required for verification and password reset. |
| JJWT with Gson backend | Avoids Jackson 2/3 conflict introduced by Spring Boot 4. Gson has no version conflicts. |
| Sealed interfaces for verification/password results | `VerificationResult` and `PasswordResetResult` use sealed types for compile-time exhaustiveness checking. Every outcome must be handled. |
| Dual filter chain | REST API clients need stateless JWT; web users need session-based auth with CSRF. Two chains cleanly separate these concerns. |
| H2 with PostgreSQL mode for dev | Zero-setup local development. PostgreSQL compatibility mode ensures SQL works identically in both environments. |
| Flyway over Hibernate auto-DDL | Explicit, versioned, repeatable migrations. `ddl-auto: validate` catches schema drift. |
| `.template` files with `@VARIABLE@` syntax | Config files contain secrets and environment-specific values. Templates are safe to commit; generated files are gitignored. |
