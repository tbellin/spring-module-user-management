# Project Research Summary

**Project:** Spring Boot 4 User Management Server with JWT Auth, Spring Modulith, Thymeleaf, Dual-Database
**Domain:** User Authentication & Authorization System
**Researched:** 2026-01-28
**Confidence:** MEDIUM-HIGH (Stack and architecture well-understood; Spring Boot 4 versions require validation)

## Executive Summary

This project builds a modern user management server using Spring Boot 4 with Spring Modulith for internal modularization, JWT-based authentication, and dual-database support (H2 for development, PostgreSQL for production). The recommended approach follows Spring Boot best practices with a modular monolith architecture: three internal modules (Auth, User, Shared) enforced through Spring Modulith's verification API rather than Maven multi-module complexity. The hybrid nature—serving both Thymeleaf-rendered pages AND REST API endpoints—requires careful security configuration with separate SecurityFilterChain beans for web vs API paths.

The stack is well-proven except for Spring Boot 4 version compatibility. Since Spring Boot 4 may still be in RC/milestone status (training data cutoff May 2025), critical dependencies like Spring Modulith, SpringDoc OpenAPI, and Thymeleaf Spring Security extras need version verification before development begins. The recommendation is to use Spring Initializr to generate the base project with verified compatible versions, then add specialized dependencies manually.

Key risks center on security boundaries: CSRF must be selectively configured (enabled for Thymeleaf, disabled for JWT API), JWT signing must use strong secrets from environment variables (never hardcoded), email verification tokens must be single-use with expiry, and Spring Security 7's explicit SecurityContext saving requirement must be followed. H2/PostgreSQL compatibility divergence is mitigated by using Testcontainers for integration tests and Flyway for migrations. Spring Modulith boundary violations are prevented through architectural testing with `ApplicationModules.verify()` run continuously.

## Key Findings

### Recommended Stack

The stack centers on Spring Boot 4 (Java 21 baseline) with Spring Security 7 for authentication, Spring Data JPA for persistence, Thymeleaf for server-rendered UI, and Spring Modulith for module boundaries within a single deployable artifact. All core Spring dependencies are managed through the Spring Boot parent POM, with manual version properties only for non-Spring libraries (JJWT 0.12.x for JWT handling, SpringDoc 2.8.x+ for OpenAPI documentation).

**Core technologies:**
- **Spring Boot 4.0.x with Java 21**: Application framework with auto-configuration, embedded Tomcat 11, virtual thread support, Jakarta EE 11 baseline. Requires verification that GA release is available.
- **Spring Security 7.0.x**: Authentication and authorization with native JWT support, BCrypt password encoding, RBAC with `@PreAuthorize`, and dual SecurityFilterChain for hybrid web+API endpoints.
- **Spring Modulith 1.3.x or 2.0.x**: Enforces module boundaries within single Maven module through package conventions, event-based inter-module communication, and compile-time verification via `ApplicationModules.verify()`.
- **Spring Data JPA + Hibernate 7**: ORM persistence layer with repository abstractions, works identically across H2 (dev) and PostgreSQL (prod) when paired with Flyway migrations.
- **Thymeleaf 3.1.x + Bootstrap 5**: Server-side templating with Spring Security dialect integration for `sec:authorize` directives, WebJars for dependency management.
- **JJWT (io.jsonwebtoken) 0.12.x**: De facto standard for JWT creation and validation in Java, type-safe API with algorithm enforcement.
- **Flyway 10.x**: Database migration and schema versioning, critical for reproducible schema across H2 and PostgreSQL.
- **Docker Compose**: Production deployment stack with app container, PostgreSQL 16/17, and PgAdmin 4 sidecar.

**Critical version warnings:**
All items marked `[VERIFY]` in STACK.md must be validated against Maven Central before starting development: Spring Boot 4 GA status, Spring Modulith Boot 4 compatible version, SpringDoc OpenAPI version, Thymeleaf Spring Security 7 extras artifact name. Recommendation is to use Spring Initializr (start.spring.io) to generate a verified-compatible base POM.

### Expected Features

Research identified clear feature tiers based on Spring Security patterns and competitive analysis against identity platforms (Keycloak, Auth0, Firebase Auth).

**Must have (table stakes):**
- Email/password registration with input validation and duplicate detection
- Email verification via token-based flow (generate, email link, verify endpoint)
- Login with JWT issuance (access token, optional refresh token)
- Logout with token invalidation strategy
- Password change for authenticated users (requires current password verification)
- Lost/reset password flow via email link with single-use expiring tokens
- User profile view/update (self-service)
- RBAC with ADMIN and USER roles using Spring Security `hasRole()`
- Admin CRUD operations: list users (paginated), view, create, update, disable
- Thymeleaf pages for all auth flows (login, register, verify, reset, profile, admin)
- REST API for all operations with consistent HTTP status codes
- Swagger UI via SpringDoc OpenAPI
- CSRF protection on all Thymeleaf forms (but disabled for `/api/**` stateless endpoints)
- BCrypt password hashing with `BCryptPasswordEncoder`
- Input validation via Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`)
- Spring profiles: `dev` (H2) and `prod` (PostgreSQL)
- Docker Compose stack for production deployment
- Environment-based configuration via `.env` files (secrets not in source code)

**Should have (competitive differentiators):**
- Account lockout after N failed login attempts (prevents brute force)
- Refresh token with rotation (better security than long-lived access tokens)
- Login audit trail (timestamp, IP, user agent per successful/failed login)
- Admin user search and filtering (beyond basic pagination)
- Rate limiting on auth endpoints (login, registration, password reset)
- Email change with re-verification flow
- Configurable token expiry (access, refresh, verification, reset)
- Graceful error messages (no user enumeration via different responses)
- API versioning with `/api/v1/` prefix
- Health check actuator endpoints for Docker probes

**Defer (v2+):**
- OAuth2 / Social Login (Google, GitHub) — complex, separate auth domain
- Two-Factor Authentication (2FA/MFA) — significant UX and implementation complexity
- User avatar/image upload — file storage infrastructure concern
- Internationalization (i18n) — pervasive change affecting all templates
- Admin dashboard analytics — separate feature domain
- Full-text search on users — only needed at 10K+ user scale
- Fine-grained permissions beyond RBAC — only if roles prove insufficient

### Architecture Approach

The recommended architecture is a **modular monolith** using Spring Modulith's package-level boundaries within a single Maven module. This avoids the distributed complexity of microservices while enforcing clean module separation through compile-time verification. The three application modules (Auth, User, Shared) communicate via direct method calls for synchronous operations and Spring events for asynchronous side effects.

**Major components:**

1. **Auth Module** — Owns authentication, authorization configuration, JWT lifecycle, email verification tokens, password reset tokens, and email sending. Exposes `AuthService` and `JwtService` as public API. Depends on User module for user lookup and creation. Internal: `SecurityConfig`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, token repositories. Publishes `UserRegisteredEvent` and `PasswordResetRequestedEvent`.

2. **User Module** — Owns User and Role entities, persistence, profile management, and admin CRUD operations. Exposes `UserService` and `AdminService` as public API. Depends only on Shared module. Internal: `User` entity, `Role` entity, repositories, mapping utilities. Publishes `UserUpdatedEvent` and `UserDeletedEvent`.

3. **Shared Module** — Cross-cutting types only. Contains DTOs (Java records like `UserDto`, `AuthResponseDto`, `RegistrationRequest`), exception hierarchy, `BaseEntity` for auditing, and `GlobalExceptionHandler`. Depends on nothing. No business logic.

**Key patterns:**
- **Dual SecurityFilterChain**: Separate `@Bean` configurations with `@Order` for `/api/**` (stateless JWT, CSRF disabled) and `/**` (session-based form login, CSRF enabled).
- **Self-issued JWT**: Application generates and validates its own tokens using JJWT library. No external authorization server.
- **Event-based async**: Email sending triggered by `@ApplicationModuleListener` on `UserRegisteredEvent`, runs in new transaction, prevents blocking registration request.
- **Spring Modulith verification**: `ApplicationModules.of(Application.class).verify()` test enforces that Auth cannot access `user.internal`, User cannot import Auth classes, and Shared has no dependencies.
- **Testcontainers for integration tests**: Real PostgreSQL in Docker for repository tests, avoiding H2/PostgreSQL dialect divergence.

**Module dependency graph:**
```
Auth --> User (direct API calls for getUserByEmail, createUser)
Auth --> Shared (DTOs, exceptions)
User --> Shared (DTOs, BaseEntity, exceptions)
Shared --> (nothing)
```

### Critical Pitfalls

Research identified seven critical pitfalls that would cause rewrites, security vulnerabilities, or major architectural issues if not addressed proactively.

1. **Spring Security 7 implicit SecurityContext saving removed** — Authentication succeeds but user appears anonymous on next request. Must explicitly call `securityContextRepository.saveContext()` after programmatic authentication. Affects Thymeleaf form login, not JWT stateless endpoints. **Prevention:** Set pattern correctly in Phase 1 Security Foundation.

2. **CSRF misconfiguration in Thymeleaf+REST hybrid** — Either REST API rejects requests with 403 (CSRF enabled globally) or Thymeleaf forms are vulnerable (CSRF disabled globally). Must configure selectively: `csrf().ignoringRequestMatchers("/api/**")` so CSRF protects Thymeleaf but not JWT endpoints. **Prevention:** Define dual SecurityFilterChain in Phase 1.

3. **JWT token signing with weak or hardcoded secrets** — Tokens can be forged if secret is too short (<256 bits), hardcoded in source, or uses weak algorithm. Must use environment variable, minimum 32 bytes for HMAC-SHA256, or prefer asymmetric RS256. Never allow JWT header to dictate algorithm. **Prevention:** Configure secure signing in Phase 1, load from `.env`.

4. **H2/PostgreSQL compatibility divergence** — Code works in H2 dev but fails in PostgreSQL prod due to SQL dialect differences (case sensitivity, function names, type handling). Use Testcontainers for integration tests with real PostgreSQL, set H2 compatibility mode (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`), and avoid native queries. **Prevention:** Phase 1 dual-database setup, Phase 2 Testcontainers.

5. **Spring Modulith boundary violations through shared entities** — Auth module directly accesses `UserRepository` or User entity, violating modularity. User entity must live in `user.internal`, Auth accesses data via `UserService` public API returning `UserDto`. Run `ApplicationModules.verify()` test continuously. **Prevention:** Define module boundaries in Phase 1, verify in every phase.

6. **Email verification token exploits** — Tokens vulnerable to user enumeration (different responses for exists/not-exists), reuse (not single-use), predictable generation (not cryptographic random), no expiry, or logged in access logs (URL query param). Must: return identical responses regardless of email existence, use `SecureRandom` 32+ bytes, store hashed (SHA-256), 1-hour expiry for reset, single-use enforcement, rate limiting. **Prevention:** Implement secure token lifecycle in Phase 3.

7. **Spring Modulith event publication registry not configured** — Events silently lost when listener fails or app crashes between publication and consumption. Users never receive verification emails. Must add `spring-modulith-starter-jpa`, enable republish-on-restart, configure completion mode (DELETE or ARCHIVE), and monitor incomplete publications. **Prevention:** Configure event registry in Phase 2 Modulith setup.

## Implications for Roadmap

Based on research findings, the natural phase structure follows module dependency order (Shared → User → Auth) and critical path to first working authentication. Early phases focus on security foundation to avoid costly retrofitting of JWT signing, CSRF configuration, and module boundaries.

### Phase 1: Project Bootstrap & Security Foundation
**Rationale:** Establish architecture and security patterns before any feature implementation. Changing JWT algorithm, CSRF strategy, or module boundaries later invalidates work. Spring Boot 4 version verification must happen first to avoid compatibility surprises mid-development.

**Delivers:**
- Verified Spring Boot 4 compatible POM (via Spring Initializr validation)
- Dual-database Spring profiles (`dev` with H2, `prod` with PostgreSQL)
- Shared module with BaseEntity, DTOs, exception hierarchy, GlobalExceptionHandler
- Spring Modulith structure with `package-info.java` and `ApplicationModules.verify()` test
- Docker Compose stack (app, PostgreSQL, PgAdmin) with health checks
- `.env` template and externalized configuration
- Dual SecurityFilterChain (API: stateless JWT, Web: form login with CSRF)
- JWT signing configured securely (environment variable secret, algorithm enforcement)

**Addresses features:**
- Spring profiles (dev/prod) — table stakes
- Docker Compose stack — table stakes
- Environment-based config — table stakes

**Avoids pitfalls:**
- JWT weak signing (Pitfall 3) — configured correctly from start
- CSRF misconfiguration (Pitfall 2) — dual SecurityFilterChain from start
- H2/PostgreSQL divergence (Pitfall 4) — dual-database setup from start
- Module boundary violations (Pitfall 5) — verification test from start

**Phase complexity:** MEDIUM (architecture decisions, security configuration, Docker setup)

**Research flag:** SKIP RESEARCH — well-documented Spring Boot patterns

---

### Phase 2: User Module & Data Layer
**Rationale:** User module has no dependencies except Shared, so it can be built and tested independently. Provides the `UserService` API that Auth module needs. Testcontainers integration tests prevent H2/PostgreSQL divergence from being discovered in production.

**Delivers:**
- User entity (extending BaseEntity) with email (unique), passwordHash, roles, enabled flag
- Role entity or enum (ADMIN, USER)
- UserRepository, RoleRepository with Spring Data JPA
- UserService: getUserByEmail, getUserById, createUser, updateProfile, changePassword
- AdminService: listUsers (paginated), getUserDetails, createUser, updateUser, disableUser
- Flyway migrations for users and roles tables (common SQL working on both H2 and PostgreSQL)
- Testcontainers integration tests with real PostgreSQL
- `@ApplicationModuleTest` for User module isolation

**Addresses features:**
- User profile management (view/update) — table stakes
- Admin CRUD on users — table stakes
- Database migrations — should-have (v1.x)
- RBAC (ADMIN/USER roles) — table stakes

**Avoids pitfalls:**
- H2/PostgreSQL divergence (Pitfall 4) — Testcontainers tests verify compatibility
- Module boundaries (Pitfall 5) — User module tested in isolation, no Auth dependency

**Uses from STACK.md:**
- Spring Data JPA, Hibernate 7
- Flyway 10.x
- Testcontainers PostgreSQL
- H2 compatibility mode

**Phase complexity:** MEDIUM (JPA entities, repository patterns, pagination, Testcontainers setup)

**Research flag:** SKIP RESEARCH — standard Spring Data patterns

---

### Phase 3: Auth Module Core (Login & Registration)
**Rationale:** Auth module depends on User module API, so must come after Phase 2. Login and registration form the critical path to first working authentication. Email verification deferred to next phase to keep scope focused.

**Delivers:**
- SecurityConfig with dual SecurityFilterChain (completed from Phase 1)
- JwtService: generateToken, validateToken, extractUsername (using JJWT library)
- JwtAuthenticationFilter extending OncePerRequestFilter
- CustomUserDetailsService calling UserService.getUserByEmail
- AuthService: login (authenticate, issue JWT), register (validate, call UserService.createUser)
- AuthController (REST): POST /api/auth/login, POST /api/auth/register
- AuthPageController (Thymeleaf): GET/POST /login, GET/POST /register
- Thymeleaf templates: login.html, register.html with CSRF tokens
- Spring Modulith event publication registry configured (spring-modulith-starter-jpa)
- `@ApplicationModuleTest` for Auth module

**Addresses features:**
- Login with JWT issuance — table stakes
- Email/password registration — table stakes
- Form-based login page — table stakes
- Registration page — table stakes
- REST API for auth operations — table stakes
- Input validation — table stakes

**Avoids pitfalls:**
- SecurityContext not saved (Pitfall 1) — explicit saveContext() call after form login
- CSRF misconfiguration (Pitfall 2) — already configured in dual SecurityFilterChain
- JWT weak signing (Pitfall 3) — already configured with strong secret
- Event publication registry missing (Pitfall 7) — configured in this phase

**Uses from STACK.md:**
- Spring Security 7 (SecurityFilterChain, OncePerRequestFilter)
- JJWT 0.12.x
- Thymeleaf + Thymeleaf Spring Security extras
- Spring Modulith events

**Phase complexity:** HIGH (Spring Security integration, JWT implementation, dual auth flows)

**Research flag:** SKIP RESEARCH — Spring Security official docs cover this

---

### Phase 4: Email Verification & Password Reset
**Rationale:** Email sending is an async side effect that should not block core auth flows. Deferred until login/registration work. Both verification and reset share token lifecycle patterns (generate, store, email link, validate, single-use, expiry), so implemented together for pattern reuse.

**Delivers:**
- EmailService (SMTP via Spring Boot Mail Starter, configured from `.env`)
- VerificationToken entity with token (hashed SHA-256), userId, expiresAt, usedAt
- PasswordResetToken entity with same structure
- VerificationTokenRepository, PasswordResetTokenRepository
- AuthService: verifyEmail(token), requestPasswordReset(email), resetPassword(token, newPassword)
- Event listeners: `@ApplicationModuleListener` on UserRegisteredEvent (send verification email), PasswordResetRequestedEvent (send reset email)
- AuthController (REST): POST /api/auth/verify-email, POST /api/auth/forgot-password, POST /api/auth/reset-password
- AuthPageController (Thymeleaf): GET /verify-email?token=, GET/POST /forgot-password, GET/POST /reset-password
- Thymeleaf email templates (verification, password reset) using SpringTemplateEngine
- Token lifecycle: SecureRandom generation, hashed storage, 24h expiry (verification), 1h expiry (reset), single-use enforcement
- Graceful error messages (identical response for exists/not-exists email)
- Rate limiting on forgot-password endpoint

**Addresses features:**
- Email verification on registration — table stakes
- Lost/reset password via email — table stakes
- Email sending via SMTP — table stakes
- Graceful error messages — should-have

**Avoids pitfalls:**
- Email token exploits (Pitfall 6) — secure token lifecycle from first implementation
- Synchronous email blocking (Performance Trap) — async via @ApplicationModuleListener

**Uses from STACK.md:**
- Spring Boot Mail Starter
- Thymeleaf for email templates
- Spring Modulith event listeners

**Implements from ARCHITECTURE.md:**
- Event-based async pattern for email sending
- Token entity pattern with expiry and single-use

**Phase complexity:** MEDIUM-HIGH (SMTP configuration, token lifecycle security, async event handling, email templating)

**Research flag:** SKIP RESEARCH — Spring Mail well-documented, token patterns standard

---

### Phase 5: User Self-Service & Admin UI
**Rationale:** Core auth flows (login, register, verify, reset) are complete. Now add self-service features and admin management UI. UserService and AdminService already exist from Phase 2, this phase adds controllers and Thymeleaf pages.

**Delivers:**
- UserController (REST): GET /api/users/profile, PATCH /api/users/profile, POST /api/users/change-password, DELETE /api/users/logout
- UserPageController (Thymeleaf): GET /profile, GET/POST /change-password
- AdminController (REST): GET /api/admin/users (paginated), GET /api/admin/users/{id}, POST /api/admin/users, PUT /api/admin/users/{id}, DELETE /api/admin/users/{id}
- AdminPageController (Thymeleaf): GET /admin/users (list), GET /admin/users/{id}, GET/POST /admin/users/new, GET/POST /admin/users/{id}/edit
- Thymeleaf templates: profile.html, change-password.html, admin/user-list.html, admin/user-form.html, admin/user-detail.html
- Authorization: `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- Password change requires current password verification
- Admin self-lockout prevention (cannot remove own ADMIN role)
- Soft delete (disable user) vs hard delete

**Addresses features:**
- View and update own profile — table stakes
- Password change (authenticated) — table stakes
- Admin list users (paginated) — table stakes
- Admin view/create/update/delete user — table stakes
- Logout — table stakes

**Uses from STACK.md:**
- Spring Security `@PreAuthorize`
- Spring Data Pageable
- Thymeleaf + Bootstrap 5

**Phase complexity:** MEDIUM (controller layer, Thymeleaf forms, authorization rules)

**Research flag:** SKIP RESEARCH — standard CRUD patterns

---

### Phase 6: API Documentation & Swagger UI
**Rationale:** Deferred until REST API endpoints are complete. SpringDoc OpenAPI auto-generates spec from annotations, so best added when endpoints are stable.

**Delivers:**
- SpringDoc OpenAPI dependency (version verified for Spring Boot 4 compatibility)
- Swagger UI at /swagger-ui.html
- OpenAPI spec at /v3/api-docs
- `@Operation` and `@ApiResponse` annotations on REST controllers
- JWT bearer auth configuration in Swagger UI (test protected endpoints)
- Proper HTTP status code documentation
- DTO schema documentation via Jakarta Validation annotations

**Addresses features:**
- Swagger UI (OpenAPI) — table stakes
- API documentation — table stakes
- Proper HTTP status codes — table stakes

**Uses from STACK.md:**
- SpringDoc OpenAPI 2.8.x or 3.0.x (version verified)

**Phase complexity:** LOW (mostly annotation additions, configuration)

**Research flag:** MAY NEED RESEARCH if SpringDoc Spring Boot 4 compatible version is unclear

---

### Phase 7: Security Hardening & Operational Readiness
**Rationale:** Core features complete. Now add production-grade security and operational features.

**Delivers:**
- Account lockout after N failed login attempts (track attempts, auto-unlock after timeout)
- Login audit trail (table: userId, timestamp, ipAddress, userAgent, success/failure)
- Health check actuator endpoints (/actuator/health, liveness, readiness)
- Docker Compose health probes referencing actuator endpoints
- Monitoring for incomplete event publications (alert if > 10 outstanding)
- Scheduled job to purge expired tokens and completed events
- API versioning enforcement (/api/v1/)
- Password strength policy enforcement (min length, complexity)
- Shell scripts in ./bin/ (dev-run.sh, prod-run.sh, setup.sh)

**Addresses features:**
- Account lockout — should-have
- Login audit trail — should-have
- Health check actuator — should-have
- Password strength policy — should-have
- Shell scripts — table stakes

**Avoids pitfalls:**
- No rate limiting (Security Mistake) — addressed
- Unbounded token table growth (Performance Trap) — purge job added

**Phase complexity:** MEDIUM (audit logging, scheduled jobs, health checks)

**Research flag:** SKIP RESEARCH — Actuator and Spring Security well-documented

---

### Phase Ordering Rationale

The phase order follows three principles derived from research:

1. **Dependency order:** Shared (no deps) → User (depends on Shared) → Auth (depends on User and Shared). Matches Spring Modulith module dependency graph from ARCHITECTURE.md.

2. **Security-first:** JWT signing, CSRF configuration, and module boundaries established in Phase 1 before any feature code. Changing these later invalidates tokens, breaks security assumptions, or requires refactoring. Addresses Pitfalls 1-3 and 5 proactively.

3. **Critical path to working auth:** Bootstrap (Phase 1) → Data layer (Phase 2) → Core auth (Phase 3) delivers a working login/registration flow. Email verification (Phase 4) and admin UI (Phase 5) add completeness but don't block basic auth. Matches the "launch with" vs "add after validation" split from FEATURES.md.

**Grouping logic:**
- Phases 1-3: Foundation and core auth (cannot defer)
- Phases 4-5: Complete the feature set (table stakes but can iterate)
- Phases 6-7: Documentation and hardening (polishing, can be parallel)

**How this avoids pitfalls:**
- Phase 1 addresses Pitfalls 2, 3, 4, 5 before they can occur
- Phase 2 adds Testcontainers to verify Pitfall 4 prevention
- Phase 3 addresses Pitfalls 1 and 7 when implementing first auth flows
- Phase 4 addresses Pitfall 6 with secure token lifecycle from first implementation
- Phase 7 addresses remaining security hardening and operational concerns

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1:** MAY NEED RESEARCH — Spring Boot 4 version verification. If GA not available, need research on whether to use milestone/RC or start with Spring Boot 3.4.x and upgrade path.
- **Phase 6:** MAY NEED RESEARCH — SpringDoc OpenAPI Spring Boot 4 compatibility. If 2.8.x incompatible, need research on version 3.0.x or alternative.

Phases with standard patterns (skip research-phase):
- **Phase 2:** Standard Spring Data JPA patterns, Testcontainers well-documented
- **Phase 3:** Spring Security official docs cover SecurityFilterChain, JWT, form login
- **Phase 4:** Spring Boot Mail and Spring Modulith events well-documented
- **Phase 5:** Standard controller + Thymeleaf patterns
- **Phase 7:** Spring Boot Actuator official docs, scheduled tasks standard

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Core Spring Boot patterns HIGH; Spring Boot 4 specific versions MEDIUM due to potential milestone status. JJWT, Flyway, Testcontainers HIGH. Spring Modulith Boot 4 compat LOW (needs verification). SpringDoc Boot 4 compat LOW (needs verification). |
| Features | HIGH | Based on Spring Security official docs, OWASP guidelines, and competitive analysis. Feature tiers clearly defined (table stakes vs differentiators vs defer). MVP scope validated against industry patterns. |
| Architecture | HIGH | Spring Modulith official docs verified. Dual SecurityFilterChain pattern verified. Self-issued JWT pattern is community standard. Event-based async well-documented. Module dependency graph clear. |
| Pitfalls | MEDIUM-HIGH | Spring Security 7 session management and CSRF docs verified (HIGH). JWT signing best practices from OWASP (HIGH). H2/PostgreSQL divergence is well-known community experience (HIGH). Spring Modulith boundary violations verified in official docs (HIGH). Spring Boot 4 specific migration issues MEDIUM (based on training data, not live verification). |

**Overall confidence:** MEDIUM-HIGH

Research is solid for established Spring Boot 3.x patterns. Medium confidence items are all version-related: Spring Boot 4 GA status, Spring Modulith Boot 4 compatible version, SpringDoc version, Thymeleaf Spring Security 7 extras artifact name. These require verification at project start but do not affect architectural decisions.

### Gaps to Address

**Version verification required (before Phase 1 starts):**
- Spring Boot 4 GA status: Is 4.0.0 released, or should project start with 3.4.x and upgrade later? Verify at https://spring.io/projects/spring-boot and Maven Central.
- Spring Modulith Boot 4 compatible version: Check if 1.3.x works or if 2.0.x is required. Verify against Spring Modulith appendix compatibility matrix.
- SpringDoc OpenAPI Boot 4 version: Is 2.8.x compatible or is 3.0.x needed? Search Maven Central: `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- Thymeleaf Spring Security extras: Artifact name `thymeleaf-extras-springsecurity6` or `springsecurity7`? Search Maven Central.

**Recommendation:** Use Spring Initializr (start.spring.io) with Spring Boot 4.0.x selected (if available) and add dependencies: Web, Security, Data JPA, Thymeleaf, Mail, Validation, Actuator, DevTools, H2, PostgreSQL, Flyway, Docker Compose. This auto-generates a POM with all compatible managed versions. Then manually add: JJWT, Spring Modulith BOM, SpringDoc OpenAPI, WebJars (Bootstrap), Testcontainers, Thymeleaf extras.

**Architectural validations during development:**
- Run `ApplicationModules.verify()` test after every phase to catch boundary violations early.
- Run Testcontainers integration tests against PostgreSQL in Phase 2 and continuously to prevent dialect divergence.
- Load test email sending in Phase 4 to verify async listeners prevent request blocking.
- Penetration test token lifecycle in Phase 4: verify reuse protection, expiry enforcement, no user enumeration.

**No functional gaps:** Research covered all required features (table stakes + competitive differentiators). Anti-features clearly identified (OAuth2, 2FA, avatars, i18n deferred to v2+). Architecture patterns proven and well-documented.

## Sources

### Primary (HIGH confidence)
- Spring Security 7.0.2 Official Documentation (Authentication, Authorization, Exploit Protection, Password Storage, Session Management, CSRF, JWT Resource Server) — fetched 2026-01-28
- Spring Security 7.0 Migration Guide (Jackson 3 migration) — fetched 2026-01-28
- Spring Modulith Reference (Fundamentals, Events, Verification, Testing, Documentation) — fetched 2026-01-28
- Spring Boot Reference (Web Security, Web Servlet, Docker Compose) — official docs
- Spring Data JPA documentation (repositories, pagination, specifications)
- Flyway documentation (migrations, versioning)
- Testcontainers documentation (Spring Boot integration, PostgreSQL module)
- JJWT (io.jsonwebtoken) documentation (JWT creation, validation)

### Secondary (MEDIUM confidence)
- Spring Boot 4.0 milestone announcements — training data through May 2025; GA status requires verification
- Spring Modulith 1.3.x / 2.0.x compatibility with Spring Boot 4 — appendix references Spring Boot 4 support but exact version mapping not verified
- SpringDoc OpenAPI Spring Boot 4 compatibility — likely requires 3.0.x but not verified
- OWASP Forgot Password Cheat Sheet principles (token security, user enumeration prevention)
- OWASP JWT Cheat Sheet principles (algorithm enforcement, claim validation)
- Domain knowledge of Keycloak, Auth0, Firebase Auth feature sets (competitive analysis)

### Tertiary (LOW confidence, needs validation)
- Thymeleaf Spring Security 7 extras artifact name (`springsecurity6` vs `springsecurity7`) — not verified against Maven Central
- Spring Security 7 specific deprecations beyond session management — training data indicates likely removals, verify against servlet migration guide
- Java 21 virtual threads default in Spring Boot 4 — announced plans, actual default behavior requires verification

---
*Research completed: 2026-01-28*
*Ready for roadmap: YES*
