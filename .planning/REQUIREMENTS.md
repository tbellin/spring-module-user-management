# Requirements: Spring Boot User Management Server

**Defined:** 2026-01-28
**Core Value:** Secure, modular user authentication and management that works identically in dev (H2, local) and prod (PostgreSQL, Docker) with zero code changes between environments.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [ ] **AUTH-01**: User can register with email and password
- [ ] **AUTH-02**: User receives email verification link after registration
- [ ] **AUTH-03**: User can resend verification email
- [ ] **AUTH-04**: User can log in with verified email/password, receiving JWT
- [ ] **AUTH-05**: User can log out (client-side token discard)

### Password Management

- [ ] **PASS-01**: Authenticated user can change password (requires current password)
- [ ] **PASS-02**: User can request password reset via email link
- [ ] **PASS-03**: User can set new password using valid reset token

### Roles & Authorization

- [ ] **ROLE-01**: System enforces ADMIN and USER roles on all protected endpoints
- [ ] **ROLE-02**: New users are assigned USER role by default

### Admin Operations

- [ ] **ADMIN-01**: Admin can list all users with pagination
- [ ] **ADMIN-02**: Admin can create new user with assigned role
- [ ] **ADMIN-03**: Admin can update user details
- [ ] **ADMIN-04**: Admin can enable/disable user accounts (soft delete)
- [ ] **ADMIN-05**: Admin can search/filter users by name, email, role, status

### User Profile

- [ ] **PROF-01**: Authenticated user can view own profile details

### Pages (Thymeleaf + Bootstrap 5)

- [ ] **PAGE-01**: Home page accessible to all visitors
- [ ] **PAGE-02**: Login page with email/password form
- [ ] **PAGE-03**: Registration page with email/password form
- [ ] **PAGE-04**: Email verification confirmation page
- [ ] **PAGE-05**: Change password page (authenticated)
- [ ] **PAGE-06**: Lost password page (request reset link)
- [ ] **PAGE-07**: Reset password page (set new password via token)
- [ ] **PAGE-08**: Logout redirects to login page

### API & Documentation

- [ ] **API-01**: All features accessible via REST API under `/api/v1/`
- [ ] **API-02**: Swagger UI available for interactive API testing
- [ ] **API-03**: API endpoints return consistent JSON error responses

### Security

- [ ] **SEC-01**: Error messages do not leak whether email exists (no user enumeration)
- [ ] **SEC-02**: Passwords stored with BCrypt hashing
- [ ] **SEC-03**: CSRF protection on all Thymeleaf forms
- [ ] **SEC-04**: JWT API endpoints are stateless (no CSRF needed)

### Infrastructure

- [ ] **INFRA-01**: Dev mode with H2 in-memory database (Spring profile)
- [ ] **INFRA-02**: Prod mode with PostgreSQL (Spring profile)
- [ ] **INFRA-03**: Docker Compose orchestrates App + PostgreSQL + PgAdmin
- [ ] **INFRA-04**: Flyway manages database schema migrations
- [ ] **INFRA-05**: `.env` file configures all environment-specific variables
- [ ] **INFRA-06**: `.template` files processed by scripts to generate config files

### Tooling

- [ ] **TOOL-01**: `./bin/` setup script (dependencies, initial config)
- [ ] **TOOL-02**: `./bin/` dev mode run script
- [ ] **TOOL-03**: `./bin/` prod mode run script (Docker)
- [ ] **TOOL-04**: `./bin/` config template processing script
- [ ] **TOOL-05**: `./bin/` cURL test scripts for all features
- [ ] **TOOL-06**: `./doc/` project documentation
- [ ] **TOOL-07**: `README.md` links to all doc files

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### User Profile

- **PROF-02**: User can edit profile (display name, bio)
- **PROF-03**: User can change email with re-verification

### Password

- **PASS-04**: Password strength policy with inline requirements display

### Security Hardening

- **SEC-05**: Account lockout after N failed login attempts
- **SEC-06**: Rate limiting on authentication endpoints
- **SEC-07**: Login audit trail (timestamp, IP, user agent)
- **SEC-08**: Refresh token with rotation

### Advanced

- **ADV-01**: OAuth2 / Social login (Google, GitHub)
- **ADV-02**: Two-factor authentication (2FA/MFA)
- **ADV-03**: User avatar/image upload
- **ADV-04**: Internationalization (i18n)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| OAuth / Social login | JWT with email/password sufficient for v1; adds external dependencies and callback complexity |
| Two-factor authentication (2FA) | Requires TOTP library, QR codes, backup codes, recovery flow — significant scope |
| Mobile app / SPA frontend | Thymeleaf server-side rendering for v1 |
| Multi-tenancy | Single-tenant server — out of project scope |
| Real-time notifications (WebSocket) | Massive complexity for marginal value in user management context |
| User avatar/image upload | File storage infrastructure concern — defer to v2 |
| Internationalization (i18n) | Pervasive change affecting all templates — defer to v2 |
| Microservice decomposition | Spring Modulith provides boundaries without network overhead |
| Full-text search | Simple LIKE queries sufficient for v1 user scale |
| Admin dashboard analytics | Separate feature domain — admin needs CRUD, not charts |
| Session-based auth alongside JWT | Maintaining two auth mechanisms doubles security surface |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | — | Pending |
| AUTH-02 | — | Pending |
| AUTH-03 | — | Pending |
| AUTH-04 | — | Pending |
| AUTH-05 | — | Pending |
| PASS-01 | — | Pending |
| PASS-02 | — | Pending |
| PASS-03 | — | Pending |
| ROLE-01 | — | Pending |
| ROLE-02 | — | Pending |
| ADMIN-01 | — | Pending |
| ADMIN-02 | — | Pending |
| ADMIN-03 | — | Pending |
| ADMIN-04 | — | Pending |
| ADMIN-05 | — | Pending |
| PROF-01 | — | Pending |
| PAGE-01 | — | Pending |
| PAGE-02 | — | Pending |
| PAGE-03 | — | Pending |
| PAGE-04 | — | Pending |
| PAGE-05 | — | Pending |
| PAGE-06 | — | Pending |
| PAGE-07 | — | Pending |
| PAGE-08 | — | Pending |
| API-01 | — | Pending |
| API-02 | — | Pending |
| API-03 | — | Pending |
| SEC-01 | — | Pending |
| SEC-02 | — | Pending |
| SEC-03 | — | Pending |
| SEC-04 | — | Pending |
| INFRA-01 | — | Pending |
| INFRA-02 | — | Pending |
| INFRA-03 | — | Pending |
| INFRA-04 | — | Pending |
| INFRA-05 | — | Pending |
| INFRA-06 | — | Pending |
| TOOL-01 | — | Pending |
| TOOL-02 | — | Pending |
| TOOL-03 | — | Pending |
| TOOL-04 | — | Pending |
| TOOL-05 | — | Pending |
| TOOL-06 | — | Pending |
| TOOL-07 | — | Pending |

**Coverage:**
- v1 requirements: 37 total
- Mapped to phases: 0
- Unmapped: 37

---
*Requirements defined: 2026-01-28*
*Last updated: 2026-01-28 after initial definition*
