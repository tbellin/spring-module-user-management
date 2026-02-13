# Roadmap: Spring Boot User Management Server

## Overview

This roadmap delivers a complete Spring Boot 4 user management server through 8 phases, progressing from infrastructure foundation through security, authentication flows, user management, and finally developer tooling. Each phase delivers a coherent, verifiable capability. The first three phases establish the foundation and core auth; phases 4-6 complete all user-facing features; phases 7-8 add API documentation and developer experience polish.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Project Bootstrap & Infrastructure** - Spring Boot 4 project with dual-database, Docker Compose, Flyway, and config tooling
- [x] **Phase 2: Security & API Foundation** - Dual SecurityFilterChain, BCrypt, CSRF, JWT infrastructure, role model, error handling
- [x] **Phase 3: Registration & Login** - User registration, login, logout with Thymeleaf pages and REST endpoints
- [x] **Phase 4: Email Verification** - Email verification flow with SMTP, token lifecycle, and resend capability
- [x] **Phase 5: Password Management** - Change password, lost password request, and reset password via email token
- [x] **Phase 6: User Profile & Admin Operations** - User self-service profile and admin CRUD with search/filter
- [ ] **Phase 7: API Documentation & Swagger** - REST API completeness verification and Swagger UI integration
- [ ] **Phase 8: Tooling & Project Documentation** - Dev/prod run scripts, cURL tests, project docs, and README

## Phase Details

### Phase 1: Project Bootstrap & Infrastructure
**Goal**: Developer can clone the project, run setup, and launch the application in both dev (H2) and prod (PostgreSQL + Docker) modes with proper database migrations and environment configuration
**Depends on**: Nothing (first phase)
**Requirements**: INFRA-01, INFRA-02, INFRA-03, INFRA-04, INFRA-05, INFRA-06, TOOL-01, TOOL-04
**Success Criteria** (what must be TRUE):
  1. Application starts in dev mode with H2 in-memory database using Spring `dev` profile
  2. Application starts in prod mode with PostgreSQL using Spring `prod` profile
  3. `docker compose up` launches App + PostgreSQL + PgAdmin and all containers reach healthy state
  4. Flyway executes initial schema migration on startup in both dev and prod profiles
  5. `.env` values are injected into application config via `.template` file processing, and setup script bootstraps a working environment from scratch
**Plans**: 12 plans in 7 waves

Plans:
- [x] 01-01-PLAN.md - Maven project foundation with Spring Boot 4.0.1 dependencies (wave 1)
- [x] 01-02-PLAN.md - Environment configuration templates and setup scripts (wave 1)
- [x] 01-03-PLAN.md - Spring profiles for dual-database configuration (wave 2)
- [x] 01-04-PLAN.md - Module structure and minimal security configuration (wave 2)
- [x] 01-05-PLAN.md - Flyway migration with user management schema (wave 3)
- [x] 01-06-PLAN.md - Home page controller and Thymeleaf templates (wave 3)
- [x] 01-07-PLAN.md - Docker infrastructure (Dockerfile and compose.yaml) (wave 3)
- [x] 01-08-PLAN.md - Application and modularity tests (wave 4)
- [x] 01-10-PLAN.md - env.sh CLI tool and template infrastructure (wave 5)
- [x] 01-11-PLAN.md - Convert config files to @VARIABLE@ templates (wave 6)
- [x] 01-12-PLAN.md - Flyway split, seed data, startup banner, and schema test (wave 6)
- [x] 01-09-PLAN.md - Phase verification checkpoint (wave 7, depends on ALL)

### Phase 2: Security & API Foundation
**Goal**: Security infrastructure is fully configured with role-based access control, password hashing, selective CSRF protection, stateless JWT for API endpoints, and consistent error responses that prevent user enumeration
**Depends on**: Phase 1
**Requirements**: ROLE-01, ROLE-02, SEC-01, SEC-02, SEC-03, SEC-04, API-03
**Success Criteria** (what must be TRUE):
  1. Two SecurityFilterChain beans are active: one for `/api/**` (stateless, no CSRF, JWT-based) and one for `/**` (session-based, CSRF enabled, form login)
  2. Passwords are stored using BCrypt hashing (raw passwords never persisted)
  3. Protected endpoints return 401/403 for unauthenticated/unauthorized requests, and ADMIN-only endpoints reject USER-role access
  4. API error responses follow a consistent JSON structure with appropriate HTTP status codes, and authentication error messages do not reveal whether an email exists in the system
  5. New user records are assigned the USER role by default
**Plans**: 6 plans in 4 waves

Plans:
- [x] 02-01-PLAN.md - User Module JPA Layer (entities, repositories, UserService API) (wave 1)
- [x] 02-02-PLAN.md - JJWT dependencies and JWT/error configuration (wave 1)
- [x] 02-03-PLAN.md - JwtService, CustomUserDetailsService, BCryptPasswordEncoder (wave 2)
- [x] 02-04-PLAN.md - Dual SecurityFilterChain with JWT filter and API error handlers (wave 3)
- [x] 02-05-PLAN.md - GlobalExceptionHandler and custom exception classes (wave 3)
- [x] 02-06-PLAN.md - Security integration tests (wave 4)

### Phase 3: Registration & Login
**Goal**: Users can create accounts, log in with email and password to receive a JWT, and log out -- through both Thymeleaf pages and REST API endpoints
**Depends on**: Phase 2
**Requirements**: AUTH-01, AUTH-04, AUTH-05, PAGE-01, PAGE-02, PAGE-03, PAGE-08
**Success Criteria** (what must be TRUE):
  1. User can register with email and password via the registration page or POST to `/api/v1/auth/register`, and account is created with hashed password and default USER role
  2. User can log in with verified email and password via the login page or POST to `/api/v1/auth/login`, receiving a valid JWT token
  3. User can log out, which discards the token (client-side) and redirects to the login page from Thymeleaf, or returns success from the API
  4. Home page is accessible to all visitors (authenticated and anonymous)
  5. Login, registration, and logout pages render correctly with Bootstrap 5 styling and CSRF tokens on forms
**Plans**: 5 plans in 4 waves

Plans:
- [x] 03-01-PLAN.md - Auth DTOs, JwtService remember-me, and AuthService (wave 1)
- [x] 03-02-PLAN.md - REST API AuthController with /register and /login (wave 2)
- [x] 03-03-PLAN.md - Thymeleaf login/register pages and AuthWebController (wave 2)
- [x] 03-04-PLAN.md - Integration tests for API and web auth (wave 3)
- [x] 03-05-PLAN.md - Manual verification checkpoint (wave 4)

### Phase 4: Email Verification
**Goal**: New user registrations require email verification before the account is activated, with the ability to resend the verification email
**Depends on**: Phase 3
**Requirements**: AUTH-02, AUTH-03, PAGE-04
**Success Criteria** (what must be TRUE):
  1. After registration, user receives an email containing a verification link sent via real SMTP
  2. Clicking the verification link activates the account and displays a confirmation page
  3. User can request a new verification email if the original was lost or expired
  4. Unverified accounts cannot log in (login attempt returns appropriate error without revealing account existence)
**Plans**: 8 plans in 5 waves

Plans:
- [x] 04-01-PLAN.md — Mail and verification configuration (wave 1)
- [x] 04-02-PLAN.md — EmailService and email templates (wave 2)
- [x] 04-03-PLAN.md — VerificationToken entity and VerificationService (wave 1)
- [x] 04-04-PLAN.md — Registration email trigger and resend API endpoint (wave 3)
- [x] 04-05-PLAN.md — EmailVerificationController and success/error pages (wave 2)
- [x] 04-06-PLAN.md — Resend verification page and AuthWebController resend (wave 4)
- [x] 04-07-PLAN.md — Block unverified user login (wave 4)
- [x] 04-08-PLAN.md — Integration tests and manual verification (wave 5)

### Phase 5: Password Management
**Goal**: Users can change their password while authenticated and recover access to their account through an email-based password reset flow
**Depends on**: Phase 4
**Requirements**: PASS-01, PASS-02, PASS-03, PAGE-05, PAGE-06, PAGE-07
**Success Criteria** (what must be TRUE):
  1. Authenticated user can change their password by providing current password and new password, via the change password page or REST API
  2. User can request a password reset by entering their email on the lost password page or via REST API, and receives an email with a reset link
  3. User can set a new password using a valid, non-expired, single-use reset token via the reset password page or REST API
  4. Reset tokens expire after a defined period and cannot be reused; error responses do not reveal whether the email exists
**Plans**: 5 plans in 4 waves

Plans:
- [x] 05-01-PLAN.md — Database migration, PasswordResetToken entity, sealed result, and request DTOs (wave 1)
- [x] 05-02-PLAN.md — Email templates, Thymeleaf pages, navbar dropdown, and login page link (wave 1)
- [x] 05-03-PLAN.md — PasswordService, EmailService extension, and JWT invalidation wiring (wave 2)
- [x] 05-04-PLAN.md — REST API and web controllers, SecurityConfig updates (wave 3)
- [x] 05-05-PLAN.md — Integration tests and manual verification (wave 4)

### Phase 6: User Profile & Admin Operations
**Goal**: Authenticated users can view their own profile, and administrators can fully manage all user accounts including creation, updates, enable/disable, and search
**Depends on**: Phase 5
**Requirements**: PROF-01, ADMIN-01, ADMIN-02, ADMIN-03, ADMIN-04, ADMIN-05
**Success Criteria** (what must be TRUE):
  1. Authenticated user can view their own profile details (email, role, account status) via Thymeleaf page or REST API
  2. Admin can list all users with pagination via Thymeleaf admin page or REST API
  3. Admin can create a new user with a specified role, and update existing user details
  4. Admin can enable or disable user accounts (soft delete) without destroying account data
  5. Admin can search and filter users by name, email, role, or status
**Plans**: 4 plans in 3 waves

Plans:
- [ ] 06-01-PLAN.md -- Foundation + Profile: UserDto/UserService/UserRepo extensions, profile controllers, profile template, navbar links (wave 1)
- [ ] 06-02-PLAN.md -- Invite infrastructure: EmailService.sendInviteEmail, invite templates, 403 page, AdminInviteService (wave 1)
- [ ] 06-03-PLAN.md -- Admin CRUD: AdminController/AdminWebController, admin list page with pagination/search/filter/inline-edit/toggle, create user page (wave 2)
- [ ] 06-04-PLAN.md -- Integration tests and manual verification (wave 3)

### Phase 7: API Documentation & Swagger
**Goal**: Every feature is accessible through a versioned REST API, and all endpoints are documented and testable through Swagger UI
**Depends on**: Phase 6
**Requirements**: API-01, API-02
**Success Criteria** (what must be TRUE):
  1. All features (auth, password management, profile, admin operations) are accessible via REST API endpoints under `/api/v1/`
  2. Swagger UI is available at a known URL and displays all API endpoints with request/response schemas
  3. Protected API endpoints can be tested directly from Swagger UI using JWT bearer token authentication
**Plans**: TBD

Plans:
- [ ] 07-01: TBD

### Phase 8: Tooling & Project Documentation
**Goal**: Complete developer experience with run scripts for both environments, automated cURL test coverage of all features, and comprehensive project documentation
**Depends on**: Phase 7
**Requirements**: TOOL-02, TOOL-03, TOOL-05, TOOL-06, TOOL-07
**Success Criteria** (what must be TRUE):
  1. Developer can start the application in dev mode using `./bin/` dev run script (single command)
  2. Developer can start the full production stack using `./bin/` prod run script (Docker Compose orchestration)
  3. cURL test scripts in `./bin/` exercise all features (registration, login, verification, password flows, profile, admin CRUD) and report pass/fail
  4. Project documentation in `./doc/` covers setup, architecture, API usage, and deployment
  5. `README.md` links to all documentation files and provides quick-start instructions
**Plans**: TBD

Plans:
- [ ] 08-01: TBD
- [ ] 08-02: TBD
- [ ] 08-03: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8

| Phase | Plans Complete | Status | Completed |
|-------|---------------|--------|-----------|
| 1. Project Bootstrap & Infrastructure | 12/12 | Complete | 2026-01-30 |
| 2. Security & API Foundation | 6/6 | Complete | 2026-02-03 |
| 3. Registration & Login | 5/5 | Complete | 2026-02-04 |
| 4. Email Verification | 8/8 | Complete | 2026-02-06 |
| 5. Password Management | 5/5 | Complete | 2026-02-11 |
| 6. User Profile & Admin Operations | 4/4 | Complete | 2026-02-13 |
| 7. API Documentation & Swagger | 0/TBD | Not started | - |
| 8. Tooling & Project Documentation | 0/TBD | Not started | - |

---
*Roadmap created: 2026-01-28*
*Last updated: 2026-02-13*
