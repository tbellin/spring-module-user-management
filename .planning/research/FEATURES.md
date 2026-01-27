# Feature Research

**Domain:** User Management Server (Spring Boot / Spring Security)
**Researched:** 2026-01-28
**Confidence:** HIGH (Spring Security official docs verified, domain well-understood)

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete or insecure.

#### Authentication Core

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Email/password registration | Baseline entry point; every user management system has this | MEDIUM | Needs validation, duplicate detection, password strength rules. Spring Security `UserDetailsService` + `PasswordEncoder` |
| Email verification on registration | Prevents fake accounts, spam. Standard since early 2010s | MEDIUM | Requires token generation, expiry, email sending, verification endpoint. Must handle re-send and token expiry gracefully |
| Login with JWT issuance | Project requires stateless auth. JWT is the chosen mechanism | MEDIUM | Spring Security 7 supports JWT natively. Need access token + optional refresh token. Store nothing server-side for stateless |
| Logout / token invalidation | Users expect to be able to log out. Stateless JWT makes this non-trivial | MEDIUM | Options: short-lived tokens + refresh rotation, or server-side blocklist. For v1, short-lived access tokens (15-30 min) + refresh tokens is sufficient |
| Password change (authenticated) | Every account system has this. Users change passwords regularly | LOW | Requires current password verification before allowing change. Use `PasswordEncoder.matches()` |
| Lost/reset password via email | Critical flow. Users forget passwords constantly. Missing = support burden | MEDIUM | Token-based: generate reset token, email link, validate token, allow new password. Token must expire (1 hour max). Single-use tokens only |
| Form-based login page | Project specifies Thymeleaf. Users expect a proper login form, not raw HTTP Basic | LOW | Spring Security form login integration. Thymeleaf template with CSRF token |
| Registration page | Self-service registration form | LOW | Thymeleaf form with validation. Must match REST API registration fields |
| CSRF protection | Web standard. Spring Security enables by default. Removing it is a security regression | LOW | Spring Security provides this out of the box. Must include CSRF tokens in all Thymeleaf forms. REST API can use stateless CSRF or exempt JWT-authenticated endpoints |
| Password hashing (bcrypt) | Non-negotiable. Storing plaintext or weak hashes is a critical vulnerability | LOW | Use `BCryptPasswordEncoder` or Spring Security's `DelegatingPasswordEncoder`. Never roll custom hashing |

#### User Profile Management

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| View own profile | Users need to see their account details | LOW | Simple GET endpoint + Thymeleaf page. Return non-sensitive fields only |
| Update own profile | Users expect to edit their display name, email, etc. | LOW | PUT/PATCH endpoint. Email change may require re-verification (consider for v1.x) |
| Role-based access control (RBAC) | Project specifies ADMIN/USER roles. Standard authorization pattern | MEDIUM | Spring Security `@PreAuthorize`, `hasRole()`. Method-level and URL-level security. Spring Security 7 uses `AuthorizationManager` API |

#### Admin Features

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| List all users (paginated) | Admin needs to see who is in the system | LOW | Spring Data `Pageable`. Return summary DTOs, not full entities. Search/filter is a differentiator, simple list is table stakes |
| View user details | Admin needs to inspect individual accounts | LOW | GET by ID. Show all fields including role, status, creation date |
| Create user (admin) | Admin must be able to add users directly without going through registration | LOW | POST endpoint. Admin-created users may skip email verification or have it optional |
| Update user (admin) | Admin changes roles, locks accounts, updates details | MEDIUM | PUT/PATCH. Must prevent admin from removing their own ADMIN role (self-lockout protection) |
| Delete/disable user (admin) | Admin must be able to remove problematic accounts | LOW | Soft-delete (disable) is strongly preferred over hard-delete. Hard delete orphans audit trails |

#### API and Documentation

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| REST API for all operations | Project requirement. Every operation available via both UI and API | MEDIUM | Consistent REST conventions: POST for create, GET for read, PUT/PATCH for update, DELETE for remove |
| Swagger UI (OpenAPI) | Project specifies SpringDoc. Developers expect API documentation | LOW | SpringDoc OpenAPI with `springdoc-openapi-starter-webmvc-ui`. Annotate endpoints with `@Operation`, `@ApiResponse` |
| Proper HTTP status codes | REST API consumers expect 201 for creation, 404 for not found, 409 for conflict, 422 for validation errors | LOW | Consistent error response format. Use `@ResponseStatus` or `ResponseEntity` |
| Input validation | All user input must be validated. Missing = security and data integrity issues | MEDIUM | Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`). Consistent error response format for both API and Thymeleaf |

#### Infrastructure

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Spring profiles (dev/prod) | Project requires H2 dev / PostgreSQL prod. Standard Spring Boot pattern | LOW | `application-dev.yml`, `application-prod.yml`. Auto-detect or explicit activation |
| Docker Compose production stack | Project requirement. Modern deployment expectation | MEDIUM | Dockerfile for app, compose.yml for app + PostgreSQL + PgAdmin. Health checks, dependency ordering |
| Database migrations | Schema must be managed, not auto-generated in production | MEDIUM | Flyway or Liquibase. Hibernate `ddl-auto=validate` in prod, `ddl-auto=create-drop` acceptable for H2 dev only |
| Environment-based configuration | Secrets must not be in code. `.env` files for configuration | LOW | Spring Boot externalized config. `.env` template with placeholders, actual `.env` in `.gitignore` |

### Differentiators (Competitive Advantage)

Features that set the product apart from minimal auth implementations. Not required for v1 launch, but increase value significantly.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Account lockout after failed attempts | Security hardening beyond basics. Prevents brute force attacks | MEDIUM | Track failed login count per user. Lock after N attempts (e.g., 5). Auto-unlock after timeout or admin unlock. Spring Security supports `AccountLockedException` |
| Login audit trail / history | Security visibility. Users and admins can see login activity | MEDIUM | Log successful/failed logins with timestamp, IP address, user agent. Separate audit table. Spring Security authentication events can drive this |
| Refresh token rotation | Better security than long-lived access tokens. Industry best practice for JWT | HIGH | Issue refresh token alongside access token. Refresh token is single-use, rotated on each use. Requires server-side storage for refresh tokens (breaks pure statelessness) |
| Admin user search and filtering | Admins managing hundreds of users need search, not just pagination | MEDIUM | Search by name, email, role, status. Spring Data Specifications or QueryDSL. Full-text search is overkill for v1 |
| Rate limiting on auth endpoints | Prevents credential stuffing and abuse. Goes beyond basic security | MEDIUM | Rate limit login, registration, password reset endpoints. Spring Boot does not include this natively -- use Bucket4j, Resilience4j, or a servlet filter |
| Account enable/disable (soft delete) | Preserves data integrity while removing access. Better than hard delete | LOW | Boolean `enabled` field. Spring Security `UserDetails.isEnabled()` integrates natively. Admin toggle |
| Email change with re-verification | Security-conscious email update flow. Prevents account takeover via email change | MEDIUM | When user changes email: send verification to NEW email, keep old email active until verified. Adds complexity but prevents hijacking |
| Password strength meter/policy | UX improvement and security. Communicate requirements before submission | LOW | Backend validation with clear rules (min length, complexity). Frontend can add visual meter in Thymeleaf via JavaScript snippet |
| Configurable token expiry | Operational flexibility. Different environments need different token lifetimes | LOW | Externalize JWT access token TTL, refresh token TTL, email verification token TTL, password reset token TTL to application.yml |
| Graceful error messages | Security-aware error handling. Don't leak whether email exists on login failure | LOW | Use generic "Invalid credentials" for login. Use generic "If this email exists, we sent a reset link" for password reset. Prevents user enumeration |
| API versioning (URL path) | Future-proofing. `/api/v1/` prefix allows breaking changes in v2 | LOW | Simple URL prefix. No framework needed. Set up from the start to avoid migration pain later |
| Health check and actuator endpoints | Operational observability. Docker and orchestrators need health probes | LOW | Spring Boot Actuator with `/actuator/health`. Configure liveness and readiness probes for Docker |

### Anti-Features (Commonly Requested, Often Problematic in v1)

Features that seem good but create disproportionate complexity or are wrong for this project scope.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| OAuth2 / Social Login (Google, GitHub) | "Everyone uses Google login" | Adds external dependencies, callback URLs, provider-specific flows, consent screens. Doubles auth complexity. JWT + email/password is complete for v1 | Defer to v2. Build clean auth interfaces now so OAuth can be added later without rewriting |
| Two-Factor Authentication (2FA/MFA) | Security best practice | Requires TOTP library, QR code generation, backup codes, recovery flow. Significant UX complexity (what if user loses phone?). Spring Security supports it but integration is non-trivial | Defer to v2. Account lockout + rate limiting provide meaningful security for v1 |
| Session-based auth alongside JWT | "Support both for flexibility" | Maintaining two auth mechanisms doubles security surface, complicates testing, creates confusion about which is authoritative. Pick one | Use JWT only. Thymeleaf pages store JWT in HTTP-only cookie to get session-like UX without actual sessions |
| Real-time notifications (WebSocket) | "Notify admin of new registrations" | WebSocket infrastructure, connection management, fallback to SSE. Massive complexity for marginal value in user management context | Admin dashboard polls or checks on page load. Email notification to admin on registration if needed |
| User avatar/image upload | "Every profile has a picture" | File storage (local vs S3), image resizing, content type validation, storage management. Separate infrastructure concern | Defer to v2. Use Gravatar or initials-based avatar as placeholder. Profile model can include `avatarUrl` field for future use |
| Full-text search on users | "Need to search users by anything" | Requires search index (Elasticsearch/Lucene), sync strategy, query DSL. Overkill for admin searching hundreds of users | Simple LIKE queries on name/email with Spring Data. Sufficient for v1 scale. Add full-text search if user count exceeds 10K |
| Internationalization (i18n) | "Support multiple languages" | Every string externalized, locale detection, translation management, RTL support considerations. Pervasive change affecting all templates | Defer to v2. Use English with externalized strings from the start (`messages.properties`) so i18n is a translation task, not a refactoring task |
| Microservice decomposition | "Auth should be a separate service" | Network boundaries, service discovery, distributed tracing, eventual consistency. Massive infrastructure overhead for single-purpose server | Spring Modulith provides module boundaries WITHOUT network overhead. Modules can be extracted to services later if needed |
| Remember Me (persistent login) | "Users shouldn't have to log in every time" | With JWT, "remember me" becomes about refresh token lifetime, not a separate mechanism. Adding Spring Security's Remember Me alongside JWT creates dual auth paths | Use configurable JWT refresh token expiry. Short expiry = no remember me. Long expiry = remember me. Single mechanism, configurable behavior |
| Custom permission system (beyond RBAC) | "Need fine-grained permissions per feature" | Custom permission tables, permission checking infrastructure, admin UI for permission management. Massive scope increase | ADMIN/USER roles with `@PreAuthorize` is sufficient for v1. If needed later, Spring Security ACLs provide fine-grained control without custom infrastructure |
| Admin dashboard analytics | "Show user growth charts, login stats" | Charting libraries, data aggregation queries, time-series data. Separate feature domain | Defer entirely. Admin needs CRUD, not analytics. Add in v2 if there is actual demand |

## Feature Dependencies

```
[Registration Form + REST endpoint]
    |-- requires --> [Password Hashing (BCrypt)]
    |-- requires --> [Input Validation (Bean Validation)]
    |-- requires --> [Email Verification Token Generation]
                         |-- requires --> [SMTP Email Sending]
                         |-- requires --> [Token Storage (DB)]
                         |-- requires --> [Verification Endpoint]

[Login (Form + REST)]
    |-- requires --> [UserDetailsService (load user from DB)]
    |-- requires --> [Password Hashing (BCrypt)]
    |-- produces --> [JWT Access Token]
    |-- produces --> [JWT Refresh Token (optional v1)]
    |-- requires --> [Spring Security Filter Chain]

[JWT Authentication Filter]
    |-- requires --> [JWT Token Parsing/Validation]
    |-- requires --> [Spring Security SecurityContext]
    |-- enables --> [All Authenticated Endpoints]

[Password Reset Flow]
    |-- requires --> [SMTP Email Sending]
    |-- requires --> [Reset Token Generation + Storage]
    |-- requires --> [Reset Token Validation Endpoint]
    |-- requires --> [New Password Form/Endpoint]
    |-- requires --> [Password Hashing (BCrypt)]

[Admin CRUD]
    |-- requires --> [RBAC (Role Check)]
    |-- requires --> [User Entity + Repository]
    |-- requires --> [Pagination (Spring Data Pageable)]

[Thymeleaf Pages]
    |-- requires --> [Spring Security CSRF tokens in forms]
    |-- requires --> [JWT stored in HTTP-only cookie for page requests]
    |-- requires --> [Bootstrap 5 CSS/JS]
    |-- enhances --> [All auth flows with user-facing UI]

[Swagger UI]
    |-- requires --> [SpringDoc OpenAPI]
    |-- requires --> [JWT auth header support in Swagger]
    |-- enhances --> [REST API with interactive documentation]

[Docker Compose Stack]
    |-- requires --> [Spring Profiles (prod)]
    |-- requires --> [PostgreSQL driver + config]
    |-- requires --> [Dockerfile for app]
    |-- requires --> [Environment variable configuration]

[Account Lockout] -- enhances --> [Login]
[Login Audit Trail] -- enhances --> [Login]
[Rate Limiting] -- enhances --> [Login, Registration, Password Reset]
[Admin Search/Filter] -- enhances --> [Admin List Users]
```

### Dependency Notes

- **Registration requires Email Sending:** Cannot complete registration flow without working SMTP. This makes email config a Phase 1 blocker, not a "nice to add later" item.
- **All authenticated endpoints require JWT Filter:** The security filter chain must be functional before any protected endpoint works. This is foundational infrastructure.
- **Thymeleaf pages require JWT-in-cookie strategy:** Since JWT is the auth mechanism but Thymeleaf forms submit via standard HTTP, the JWT must be stored in an HTTP-only cookie so the browser sends it automatically. This is an architectural decision that affects both auth and UI modules.
- **Admin CRUD requires RBAC:** Role checking must work before admin endpoints are meaningful. Build role infrastructure with auth, not as an afterthought.
- **Password Reset requires Email Sending:** Same SMTP dependency as registration. Build once, use in both flows.
- **Swagger UI requires JWT auth support:** Swagger must be configured to send JWT in Authorization header for testing protected endpoints. Otherwise it can only test public endpoints.
- **Docker Compose requires working Spring Profiles:** The prod profile must correctly switch to PostgreSQL with externalized config before Docker deployment works.

## MVP Definition

### Launch With (v1)

Minimum viable product -- everything needed for a functional, secure user management server.

- [ ] Email/password registration with input validation -- entry point for all users
- [ ] Email verification (send, verify, resend) -- prevents spam/fake accounts
- [ ] Login with JWT issuance (access token) -- core auth mechanism
- [ ] Logout (client-side token discard + optional server-side blocklist) -- basic session end
- [ ] Password change (authenticated, requires current password) -- basic account maintenance
- [ ] Lost/reset password via email link (token-based, single-use, expiring) -- critical support reduction
- [ ] View and update own profile -- basic account self-service
- [ ] RBAC with ADMIN/USER roles -- authorization foundation
- [ ] Admin: list users (paginated), view user, create user, update user, disable user -- admin operations
- [ ] Thymeleaf pages: home, login, register, verify email, change password, lost password, reset password, profile, admin user list -- full UI
- [ ] REST API for all operations -- programmatic access
- [ ] Swagger UI -- API documentation and testing
- [ ] CSRF protection on all forms -- security baseline
- [ ] BCrypt password hashing -- security baseline
- [ ] Input validation with consistent error responses -- data integrity
- [ ] Spring profiles (dev with H2, prod with PostgreSQL) -- dual environment
- [ ] Docker Compose stack (app + PostgreSQL + PgAdmin) -- production deployment
- [ ] Environment-based config (.env template) -- secrets management
- [ ] Shell scripts for dev run, prod run, setup -- developer experience
- [ ] Graceful error messages (no user enumeration leaks) -- security awareness

### Add After Validation (v1.x)

Features to add once core is working and validated.

- [ ] Account lockout after failed attempts -- add when security hardening is prioritized
- [ ] Refresh token with rotation -- add when access token expiry feels too short for users
- [ ] Login audit trail -- add when security visibility is requested
- [ ] Admin user search and filtering -- add when admin user count makes pagination insufficient
- [ ] Rate limiting on auth endpoints -- add when deployed publicly and abuse is a concern
- [ ] Email change with re-verification -- add when users request email updates
- [ ] Database migrations with Flyway -- add before first production data exists that must survive schema changes
- [ ] Health check / Actuator endpoints -- add when Docker orchestration needs health probes

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] OAuth2 / Social Login -- complex, separate auth flow domain
- [ ] Two-Factor Authentication (2FA) -- significant UX and implementation complexity
- [ ] User avatar/image upload -- file storage infrastructure concern
- [ ] Internationalization (i18n) -- pervasive change, do before broad user base
- [ ] Admin dashboard analytics -- separate feature domain entirely
- [ ] Full-text search -- only if user scale demands it
- [ ] Fine-grained permissions beyond RBAC -- only if role-based proves insufficient

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Registration + email verification | HIGH | MEDIUM | P1 |
| Login with JWT | HIGH | MEDIUM | P1 |
| Password change (authenticated) | HIGH | LOW | P1 |
| Lost/reset password via email | HIGH | MEDIUM | P1 |
| View/update own profile | MEDIUM | LOW | P1 |
| RBAC (ADMIN/USER) | HIGH | MEDIUM | P1 |
| Admin CRUD on users | HIGH | MEDIUM | P1 |
| Thymeleaf pages (all flows) | HIGH | MEDIUM | P1 |
| REST API + Swagger UI | HIGH | LOW | P1 |
| CSRF protection | HIGH | LOW | P1 |
| BCrypt password hashing | HIGH | LOW | P1 |
| Input validation | HIGH | LOW | P1 |
| Spring profiles (dev/prod) | HIGH | LOW | P1 |
| Docker Compose stack | MEDIUM | MEDIUM | P1 |
| .env config templating | MEDIUM | LOW | P1 |
| Graceful error messages | MEDIUM | LOW | P1 |
| Shell scripts (bin/) | MEDIUM | LOW | P1 |
| API versioning (/api/v1/) | MEDIUM | LOW | P1 |
| Account lockout | MEDIUM | MEDIUM | P2 |
| Refresh token rotation | MEDIUM | HIGH | P2 |
| Login audit trail | MEDIUM | MEDIUM | P2 |
| Admin search/filter | MEDIUM | MEDIUM | P2 |
| Rate limiting | MEDIUM | MEDIUM | P2 |
| Database migrations (Flyway) | HIGH | MEDIUM | P2 |
| Actuator health checks | LOW | LOW | P2 |
| Email change + re-verify | LOW | MEDIUM | P2 |
| Password strength policy | LOW | LOW | P2 |
| OAuth2 / Social Login | MEDIUM | HIGH | P3 |
| 2FA/MFA | MEDIUM | HIGH | P3 |
| User avatar upload | LOW | MEDIUM | P3 |
| i18n | LOW | HIGH | P3 |
| Admin analytics | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for launch -- core user management functionality
- P2: Should have, add when possible -- security hardening and operational maturity
- P3: Nice to have, future consideration -- scope expansion beyond core

## Competitor Feature Analysis

Analysis based on what industry-standard identity platforms provide, to understand the feature ceiling and where a custom Spring Boot implementation fits.

| Feature | Keycloak | Auth0 | Firebase Auth | Our Approach (v1) |
|---------|----------|-------|---------------|-------------------|
| Email/password auth | Yes | Yes | Yes | Yes -- core table stakes |
| Email verification | Yes | Yes | Yes | Yes -- token-based with real SMTP |
| Password reset | Yes | Yes | Yes | Yes -- token-based email flow |
| Social/OAuth login | Yes (extensive) | Yes (extensive) | Yes (Google, Apple, etc.) | No -- defer to v2, JWT + email sufficient |
| MFA/2FA | Yes (TOTP, WebAuthn) | Yes (SMS, TOTP, Push) | Yes (Phone) | No -- defer to v2 |
| RBAC | Yes (realm + client roles) | Yes (roles + permissions) | Yes (custom claims) | Yes -- ADMIN/USER with Spring Security |
| User self-service | Yes (account console) | Yes (universal login) | Partial (via SDK) | Yes -- Thymeleaf profile pages |
| Admin console | Yes (full admin UI) | Yes (dashboard) | Yes (Firebase console) | Yes -- Thymeleaf admin pages + REST API |
| Brute force protection | Yes (built-in) | Yes (anomaly detection) | Yes (built-in) | v1.x -- account lockout after failed attempts |
| Audit logging | Yes (events) | Yes (logs) | Yes (limited) | v1.x -- login audit trail |
| API documentation | Partial (REST admin API) | Yes (extensive) | Yes (reference) | Yes -- Swagger UI with SpringDoc |
| Multi-tenancy | Yes (realms) | Yes (tenants) | Yes (projects) | No -- single-tenant, out of scope |
| Session management | Yes | Yes | Yes (client SDK) | JWT-based -- no server sessions |
| User import/export | Yes (JSON) | Yes (bulk) | Yes (CLI) | No -- out of scope for v1 |
| Passwordless / Passkeys | Yes (WebAuthn) | Yes (passwordless) | Yes (phone) | No -- defer to v2+ |

**Key insight:** Our v1 covers the core feature set that Keycloak/Auth0 provide, minus the advanced identity features (social login, MFA, multi-tenancy, passwordless). This is appropriate because we are building a focused user management server, not a general-purpose identity provider. The advantage of custom implementation is full control over UX, data model, and business logic.

## Sources

- Spring Security 7.0.2 Official Documentation - Authentication: https://docs.spring.io/spring-security/reference/servlet/authentication/index.html (HIGH confidence)
- Spring Security 7.0.2 Official Documentation - Authorization: https://docs.spring.io/spring-security/reference/servlet/authorization/index.html (HIGH confidence)
- Spring Security 7.0.2 Official Documentation - Exploit Protection: https://docs.spring.io/spring-security/reference/servlet/exploits/index.html (HIGH confidence)
- Spring Security 7.0.2 Official Documentation - Password Storage: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html (HIGH confidence)
- OWASP Authentication Testing Guide: https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/04-Authentication_Testing/ (HIGH confidence -- industry standard)
- Project context from PROJECT.md (project-specific requirements)
- Domain knowledge of Keycloak, Auth0, Firebase Auth feature sets (MEDIUM confidence -- based on training data, not live verification; feature sets are well-established and stable)

---
*Feature research for: Spring Boot 4 User Management Server*
*Researched: 2026-01-28*
