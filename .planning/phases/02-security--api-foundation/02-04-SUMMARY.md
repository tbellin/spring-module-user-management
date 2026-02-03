---
phase: 02-security--api-foundation
plan: 04
subsystem: auth
tags: [spring-security, jwt, filter-chain, problem-detail, 401, 403]

# Dependency graph
requires:
  - phase: 02-03
    provides: JwtService, CustomUserDetailsService, PasswordConfig
provides:
  - Dual SecurityFilterChain (API + Web)
  - JwtAuthenticationFilter for API requests
  - JSON ProblemDetail error handlers (401/403)
  - AuthenticationManager bean
affects: [03-auth-endpoints, api-controllers, admin-endpoints]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Dual SecurityFilterChain with @Order
    - Non-bean filter instantiation to avoid global registration
    - RFC 9457 ProblemDetail for API errors

key-files:
  created:
    - src/main/java/com/example/usermanagement/auth/internal/SecurityConfig.java
    - src/main/java/com/example/usermanagement/auth/internal/JwtAuthenticationFilter.java
    - src/main/java/com/example/usermanagement/auth/internal/ApiAuthenticationEntryPoint.java
    - src/main/java/com/example/usermanagement/auth/internal/ApiAccessDeniedHandler.java
  modified: []

key-decisions:
  - "JwtAuthenticationFilter not a Spring bean (prevents global registration)"
  - "API chain @Order(1) before web chain @Order(2) for proper matcher priority"
  - "SecurityConfig moved from shared.config to auth.internal where it belongs"

patterns-established:
  - "API errors use ProblemDetail JSON via custom entry point and access denied handler"
  - "API chain is stateless (no session), web chain uses sessions"
  - "CSRF disabled for API, enabled for web (except H2 console)"

# Metrics
duration: 2min
completed: 2026-02-03
---

# Phase 02 Plan 04: Dual SecurityFilterChain Configuration Summary

**Dual SecurityFilterChain with stateless JWT API chain (@Order 1) and session-based web chain (@Order 2), plus JSON ProblemDetail error handlers for 401/403**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-03T16:06:01Z
- **Completed:** 2026-02-03T16:07:58Z
- **Tasks:** 3
- **Files modified:** 5 (4 created, 1 deleted)

## Accomplishments
- Created dual SecurityFilterChain: API chain (stateless, JWT, CSRF disabled) and web chain (sessions, form login, CSRF enabled)
- Implemented JwtAuthenticationFilter that extracts Bearer tokens and sets SecurityContext
- Created ApiAuthenticationEntryPoint and ApiAccessDeniedHandler for RFC 9457 JSON error responses
- Deleted Phase 1 placeholder SecurityConfig from shared.config
- Exposed AuthenticationManager bean for programmatic authentication in controllers

## Task Commits

Each task was committed atomically:

1. **Task 1: Create API error handlers** - `6d583d1` (feat)
2. **Task 2: Create JwtAuthenticationFilter** - `2470f22` (feat)
3. **Task 3: Create dual SecurityFilterChain and delete placeholder** - `5f27ba8` (feat)

## Files Created/Modified
- `auth/internal/ApiAuthenticationEntryPoint.java` - Returns ProblemDetail JSON for 401 Unauthorized
- `auth/internal/ApiAccessDeniedHandler.java` - Returns ProblemDetail JSON for 403 Forbidden
- `auth/internal/JwtAuthenticationFilter.java` - Extracts/validates Bearer JWT, sets SecurityContext
- `auth/internal/SecurityConfig.java` - Dual filter chain: API @Order(1), Web @Order(2)
- `shared/config/SecurityConfig.java` - DELETED (Phase 1 placeholder)

## Decisions Made
- **JwtAuthenticationFilter is NOT a Spring bean**: Instantiated directly in SecurityConfig to avoid Spring Boot auto-registering it in the global servlet filter chain (which would run it on ALL requests including web pages)
- **SecurityConfig moved to auth.internal**: Security configuration is auth module's concern, not a shared cross-cutting concern
- **API chain matches first**: @Order(1) ensures /api/** requests hit the stateless JWT chain before falling through to web chain

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Security infrastructure complete for both API and web requests
- Ready for Phase 3 auth endpoint implementation (login/register/refresh)
- JwtAuthenticationFilter validates tokens; auth endpoints will generate them
- /api/auth/** is permitAll for public registration/login endpoints

---
*Phase: 02-security--api-foundation*
*Completed: 2026-02-03*
