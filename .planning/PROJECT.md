# Spring Boot User Management Server

## What This Is

A Spring Boot 4 user management server built with Spring Modulith architecture and JWT authentication. It provides self-registration with email verification, role-based access control (ADMIN/USER), password management flows (change/forgot/reset), user profile management, full admin CRUD operations, Thymeleaf-rendered pages with Bootstrap 5, and a complete REST API documented with Swagger UI. Runs locally with H2 for development and in Docker with PostgreSQL + PgAdmin for production with zero code changes between environments.

## Core Value

Secure, modular user authentication and management that works identically in dev (H2, local) and prod (PostgreSQL, Docker) with zero code changes between environments.

## Current Milestone: v1.2 Foundation Upgrade

**Goal:** Modernize the project's foundations (package rename, version, SMTP config) and add GitHub OAuth as a second login path.

**Target features:**
- GitHub OAuth login (alongside email/password — auto-creates USER account from GitHub profile)
- Package rename: `com.example.usermanagement` → `org.jbelt.module`
- App version: `1.2.0-SNAPSHOT` (dev) / `1.2.0` (release)
- Gmail SMTP configuration support (well-documented `.env` template for Gmail App Password flow)

## Requirements

### Validated

- ✓ Self-registration with email verification (real SMTP) — v1.0
- ✓ JWT-based authentication (login/logout) — v1.0
- ✓ Change password (authenticated user) — v1.0
- ✓ Lost/reset password via email link — v1.0
- ✓ Role-based access control (ADMIN, USER roles) — v1.0
- ✓ User profile management (update own profile/password) — v1.0
- ✓ Admin CRUD operations on users (create, list, update, enable/disable, search) — v1.0
- ✓ Thymeleaf pages: home, login, register, logout, email verification, change password, lost/reset password, admin user list — v1.0
- ✓ REST API endpoints for all user operations under `/api/v1/` — v1.0
- ✓ Swagger UI for API testing (SpringDoc OpenAPI 3.0.1) — v1.0
- ✓ Spring Modulith structure: Auth, User, Shared modules — v1.0
- ✓ Dev mode: H2 in-memory database, run from Mac terminal — v1.0
- ✓ Prod mode: Full Docker Compose stack (App + PostgreSQL + PgAdmin) — v1.0
- ✓ Shell scripts in `./bin/` for setup, dev run, prod run, config templating, cURL tests — v1.0
- ✓ Documentation in `./doc/` with links from README.md — v1.0
- ✓ `.env` + `env.sh` for environment variable management and template substitution — v1.0
- ✓ Config files produced from `.template` files via `env.sh substitute-all` — v1.0
- ✓ `.gitignore` excludes `.env` and generated config files — v1.0
- ✓ cURL test scripts exercising all features with pass/fail reporting — v1.0

### Active

- [ ] GitHub OAuth login alongside email/password — v1.2
- [ ] Package rename: `com.example.usermanagement` → `org.jbelt.module` — v1.2
- [ ] App version 1.2.0-SNAPSHOT / 1.2.0 release — v1.2
- [ ] Gmail SMTP configuration support in `.env` template — v1.2

### Out of Scope

- OAuth / social login beyond GitHub — GitHub added in v1.2; other providers (Google, Apple) deferred
- Mobile app or SPA frontend — Thymeleaf server-side rendering for v1
- Multi-tenancy — single-tenant server
- Two-factor authentication (2FA) — defer to v2
- User avatar/image upload — defer to v2
- Internationalization (i18n) — defer to v2
- Account lockout after failed attempts — defer to v2
- Rate limiting on auth endpoints — defer to v2 (resend has basic rate limiting)
- Login audit trail (IP, user agent) — defer to v2
- Refresh token rotation — defer to v2

## Context

- **Shipped:** v1.0 MVP — 2026-02-23
- **Scale:** 8 phases, 47 plans, 254 files, ~7,275 Java LOC
- **Framework versions:** Spring Boot 4.0.1, Spring Framework 7, Java 21
- **Build tool:** Maven with Maven Wrapper 3.9.9
- **Spring Modulith** enforces module boundaries: Auth module handles JWT and authentication flows, User module handles profile and admin CRUD, Shared module provides cross-cutting concerns (DTOs, exceptions, base entities, config)
- **Dual database strategy:** H2 (`MODE=PostgreSQL`) for fast local development, PostgreSQL for production — Spring profiles switch between them with schema parity
- **Email:** Real SMTP configured via `.env` for both dev and prod (no fake mail server)
- **Config templating:** `.template` files contain `@VARIABLE@` placeholders; `bin/env.sh substitute-all` processes them using `.env` values to produce actual config files
- **Docker Compose:** Full production stack — application JAR, PostgreSQL, PgAdmin — all orchestrated together via `bin/run-prod.sh`
- **Pages:** Thymeleaf with Bootstrap 5, server-side rendered — home, login/register, password management flows, admin user management
- **Test user:** username `tiziano`, email `tizianobellin@yahoo.com` — use for email verification and password reset testing
- **Known issues:** ModularityTests has a pre-existing false-positive failure (reports violations for allowed dependencies) — cosmetic issue, does not affect runtime behavior

## Constraints

- **Tech stack**: Spring Boot 4 + Spring Framework 7 + Java 21 — user-specified versions
- **Build**: Maven — user-specified
- **Architecture**: Spring Modulith with 3 modules (Auth, User, Shared) — enforced module boundaries
- **Database**: H2 (dev) / PostgreSQL (prod) — no other databases
- **Docker**: Full stack in Docker Compose for prod — App + PostgreSQL + PgAdmin
- **UI**: Thymeleaf + Bootstrap 5 — no JavaScript framework
- **Email**: Real SMTP — no dev-only mail catchers
- **Scripts**: All tooling in `./bin/`, all docs in `./doc/`

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Spring Modulith over microservices | Single deployable with enforced module boundaries — simpler for user management scope | ✓ Good — boundaries enforced cleanly; `auth → user → shared` hierarchy held throughout |
| JWT over session-based auth | Stateless, works well for REST API + Thymeleaf hybrid | ✓ Good — dual chain (JWT for API, session for web) works cleanly |
| Thymeleaf + Bootstrap 5 over SPA | Server-side rendering keeps stack unified (Java only), faster to build | ✓ Good — all pages delivered without JavaScript framework complexity |
| Real SMTP over Mailpit/console | User wants real email delivery in all environments | ✓ Good — works in both dev and prod; requires valid SMTP credentials in `.env` |
| H2 for dev over containerized PostgreSQL | Fastest local startup, no Docker dependency for dev workflow | ✓ Good — `H2 MODE=PostgreSQL` provided adequate parity; schema comparison test catches divergence |
| Full Docker Compose for prod | App + PostgreSQL + PgAdmin managed together, single `docker compose up` | ✓ Good — `bin/run-prod.sh` orchestrates full stack cleanly |
| jjwt-gson over jjwt-jackson | Jackson 2/3 conflict with Spring Boot 4 | ✓ Good — avoided dependency conflict entirely |
| `GenerationType.IDENTITY` for IDs | H2 + PostgreSQL compatibility | ✓ Good — consistent behavior across both databases |
| AdminInviteService in auth.internal | Needs PasswordResetTokenRepository from auth.internal.password | ✓ Good — no module boundary violation; reuses reset token infrastructure for invite flow |
| DTOs in user.internal, importable by auth | Keeps DTO ownership clear while avoiding boundary violations | ✓ Good — UpdateUserRequest, CreateUserRequest live in user.internal, imported cleanly by auth module |
| Email as username (login identifier) | Simplifies login flow, avoids separate username lookup | ✓ Good — consistent pattern throughout; `displayName` field is the public username |
| `@VARIABLE@` substitution syntax | Avoids envsubst conflicts with shell variables | ✓ Good — clean template processing with `env.sh substitute-all` |
| OpenAPI/Swagger paths in web chain permitAll | `/swagger-ui/**` not under `/api/**` so belongs in web chain | ✓ Good — Swagger UI accessible without JWT |
| `@ParameterObject` on Pageable | SpringDoc needs this to explode Pageable into individual query params | ✓ Good — fixes Swagger UI sending pageable as complex object (500 error) |

---
*Last updated: 2026-02-23 after v1.2 milestone start*
