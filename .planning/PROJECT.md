# Spring Boot User Management Server

## What This Is

A Spring Boot 4 user management server built with Spring Modulith architecture and JWT authentication. It provides self-registration with email verification, role-based access control (ADMIN/USER), password management flows, and Thymeleaf-rendered pages with Bootstrap 5. Runs locally with H2 for development and in Docker with PostgreSQL + PgAdmin for production.

## Core Value

Secure, modular user authentication and management that works identically in dev (H2, local) and prod (PostgreSQL, Docker) with zero code changes between environments.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Self-registration with email verification (real SMTP)
- [ ] JWT-based authentication (login/logout)
- [ ] Change password (authenticated user)
- [ ] Lost/reset password via email link
- [ ] Role-based access control (ADMIN, USER roles)
- [ ] User profile management (update own profile/password)
- [ ] Admin CRUD operations on users (create, list, update, delete)
- [ ] Thymeleaf pages: home, login, logout, register, change password, lost password (Bootstrap 5)
- [ ] REST API endpoints for all user operations
- [ ] Swagger UI for API testing (SpringDoc OpenAPI)
- [ ] Spring Modulith structure: Auth, User, Shared modules
- [ ] Dev mode: H2 in-memory database, run from Mac terminal
- [ ] Prod mode: Full Docker Compose stack (App + PostgreSQL + PgAdmin)
- [ ] Shell scripts in `./bin/` for setup, dev run, prod run, config templating, cURL tests
- [ ] Documentation in `./doc/` with links from README.md
- [ ] `.env`-driven configuration with `.template` file processing
- [ ] cURL test scripts exercising all features

### Out of Scope

- OAuth / social login — JWT with email/password sufficient for v1
- Mobile app or SPA frontend — Thymeleaf server-side rendering for v1
- Multi-tenancy — single-tenant server
- Two-factor authentication (2FA) — defer to v2
- User avatar/image upload — defer to v2
- Internationalization (i18n) — defer to v2

## Context

- **Framework versions:** Spring Boot 4, Spring Framework 7, Java 21
- **Build tool:** Maven
- **Spring Modulith** enforces module boundaries: Auth module handles JWT and authentication flows, User module handles profile and admin CRUD, Shared module provides cross-cutting concerns (DTOs, exceptions, base entities)
- **Dual database strategy:** H2 for fast local development, PostgreSQL for production — Spring profiles switch between them
- **Email:** Real SMTP configured via `.env` for both dev and prod (no fake mail server)
- **Config templating:** `.template` files contain placeholders; scripts in `./bin/` process them using `.env` values to produce actual config files
- **Docker Compose:** Full production stack — application JAR, PostgreSQL, PgAdmin — all orchestrated together
- **Pages:** Thymeleaf with Bootstrap 5, server-side rendered — home page, login/register forms, password management flows
- **Test user:** username `tiziano`, email `tizianobellin@yahoo.com` — use for email verification and password reset testing

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
| Spring Modulith over microservices | Single deployable with enforced module boundaries — simpler for user management scope | — Pending |
| JWT over session-based auth | Stateless, works well for REST API + Thymeleaf hybrid | — Pending |
| Thymeleaf + Bootstrap 5 over SPA | Server-side rendering keeps stack unified (Java only), faster to build | — Pending |
| Real SMTP over Mailpit/console | User wants real email delivery in all environments | — Pending |
| H2 for dev over containerized PostgreSQL | Fastest local startup, no Docker dependency for dev workflow | — Pending |
| Full Docker Compose for prod | App + PostgreSQL + PgAdmin managed together, single `docker compose up` | — Pending |

---
*Last updated: 2026-01-28 after initialization*
