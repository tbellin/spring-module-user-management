# Milestones

## v1.0 MVP (Shipped: 2026-02-23)

**Phases completed:** 8 phases, 47 plans, 4 tasks

**Key accomplishments:**
- Spring Boot 4 + Spring Modulith project with dual H2/PostgreSQL, Flyway migrations, Docker Compose, and env templating CLI (`bin/env.sh`)
- Dual SecurityFilterChain (stateless JWT for API, session+CSRF for web), BCrypt, GlobalExceptionHandler with RFC 9457 ProblemDetail responses
- User registration, login/logout via Thymeleaf pages and REST API endpoints with Bootstrap 5 UI and remember-me support
- Full email verification flow with real SMTP, resend with rate limiting, and account activation gate blocking unverified login
- Change password, forgot/reset password via email token, JWT invalidation on password change, session invalidation on web change
- User profile (view/edit) + admin CRUD (list, search/filter, paginate, inline-edit, enable/disable toggle with undo, invite by email)
- SpringDoc OpenAPI 3.0.1 + Swagger UI with JWT bearer auth, annotated across all endpoints — full API documentation
- Dev/prod run scripts, cURL test suite exercising all features, API reference + deployment docs, comprehensive README

---


## v1.2.0 Foundation Upgrade (Shipped: 2026-02-24)

**Phases completed:** 3 phases (9-11), 6 plans, 7 tasks
**Files modified:** 176 | **Java LOC:** 7,289 | **Timeline:** 2026-02-24 (1 day)
**Git range:** `feat(09-01)` → `docs(11-03)`

**Key accomplishments:**
- Renamed all 77 Java sources from `com.example.usermanagement` to `org.jbelt.module` namespace; bumped pom.xml to `org.jbelt:1.2.0-SNAPSHOT` — `mvn verify` baseline unchanged
- Updated `.env.example`/`.env.template` with Gmail SMTP defaults (`smtp.gmail.com:587`) and step-by-step App Password inline instructions
- Added `doc/gmail-smtp-setup.md` — standalone Gmail SMTP configuration guide with troubleshooting table
- Removed `pom.xml` from `.gitignore` to restore Git tracking (was incorrectly listed as generated)
- Added `.github/workflows/ci.yml` — GitHub Actions CI targeting Java 21 Temurin with Maven cache, triggers on push and PR to main
- Published project at `github.com/tbellin/spring-module-user-management` with full commit history; resolved GitHub OAuth workflow scope and CI Flyway/SMTP issues
- Updated `README.md` with live CI badge and repository clone URL; CI green on first push (build: 49s)

---

