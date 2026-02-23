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

