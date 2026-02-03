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

- [x] **ROLE-01**: System enforces ADMIN and USER roles on all protected endpoints
- [x] **ROLE-02**: New users are assigned USER role by default

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
- [x] **API-03**: API endpoints return consistent JSON error responses

### Security

- [x] **SEC-01**: Error messages do not leak whether email exists (no user enumeration)
- [x] **SEC-02**: Passwords stored with BCrypt hashing
- [x] **SEC-03**: CSRF protection on all Thymeleaf forms
- [x] **SEC-04**: JWT API endpoints are stateless (no CSRF needed)

### Infrastructure

- [x] **INFRA-01**: Dev mode with H2 in-memory database (Spring profile)
- [x] **INFRA-02**: Prod mode with PostgreSQL (Spring profile)
- [x] **INFRA-03**: Docker Compose orchestrates App + PostgreSQL + PgAdmin
- [x] **INFRA-04**: Flyway manages database schema migrations
- [x] **INFRA-05**: `.env` file configures all environment-specific variables
- [x] **INFRA-06**: `.template` files processed by scripts to generate config files

### Tooling

- [x] **TOOL-01**: `./bin/` setup script (dependencies, initial config)
- [ ] **TOOL-02**: `./bin/` dev mode run script
- [ ] **TOOL-03**: `./bin/` prod mode run script (Docker)
- [x] **TOOL-04**: `./bin/` config template processing script
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
| Two-factor authentication (2FA) | Requires TOTP library, QR codes, backup codes, recovery flow -- significant scope |
| Mobile app / SPA frontend | Thymeleaf server-side rendering for v1 |
| Multi-tenancy | Single-tenant server -- out of project scope |
| Real-time notifications (WebSocket) | Massive complexity for marginal value in user management context |
| User avatar/image upload | File storage infrastructure concern -- defer to v2 |
| Internationalization (i18n) | Pervasive change affecting all templates -- defer to v2 |
| Microservice decomposition | Spring Modulith provides boundaries without network overhead |
| Full-text search | Simple LIKE queries sufficient for v1 user scale |
| Admin dashboard analytics | Separate feature domain -- admin needs CRUD, not charts |
| Session-based auth alongside JWT | Maintaining two auth mechanisms doubles security surface |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 3 | Pending |
| AUTH-02 | Phase 4 | Pending |
| AUTH-03 | Phase 4 | Pending |
| AUTH-04 | Phase 3 | Pending |
| AUTH-05 | Phase 3 | Pending |
| PASS-01 | Phase 5 | Pending |
| PASS-02 | Phase 5 | Pending |
| PASS-03 | Phase 5 | Pending |
| ROLE-01 | Phase 2 | Complete |
| ROLE-02 | Phase 2 | Complete |
| ADMIN-01 | Phase 6 | Pending |
| ADMIN-02 | Phase 6 | Pending |
| ADMIN-03 | Phase 6 | Pending |
| ADMIN-04 | Phase 6 | Pending |
| ADMIN-05 | Phase 6 | Pending |
| PROF-01 | Phase 6 | Pending |
| PAGE-01 | Phase 3 | Pending |
| PAGE-02 | Phase 3 | Pending |
| PAGE-03 | Phase 3 | Pending |
| PAGE-04 | Phase 4 | Pending |
| PAGE-05 | Phase 5 | Pending |
| PAGE-06 | Phase 5 | Pending |
| PAGE-07 | Phase 5 | Pending |
| PAGE-08 | Phase 3 | Pending |
| API-01 | Phase 7 | Pending |
| API-02 | Phase 7 | Pending |
| API-03 | Phase 2 | Complete |
| SEC-01 | Phase 2 | Complete |
| SEC-02 | Phase 2 | Complete |
| SEC-03 | Phase 2 | Complete |
| SEC-04 | Phase 2 | Complete |
| INFRA-01 | Phase 1 | Complete |
| INFRA-02 | Phase 1 | Complete |
| INFRA-03 | Phase 1 | Complete |
| INFRA-04 | Phase 1 | Complete |
| INFRA-05 | Phase 1 | Complete |
| INFRA-06 | Phase 1 | Complete |
| TOOL-01 | Phase 1 | Complete |
| TOOL-02 | Phase 8 | Pending |
| TOOL-03 | Phase 8 | Pending |
| TOOL-04 | Phase 1 | Complete |
| TOOL-05 | Phase 8 | Pending |
| TOOL-06 | Phase 8 | Pending |
| TOOL-07 | Phase 8 | Pending |

**Coverage:**
- v1 requirements: 44 total
- Mapped to phases: 44
- Unmapped: 0

---
*Requirements defined: 2026-01-28*
*Last updated: 2026-02-03 (Phase 2 requirements marked Complete)*
