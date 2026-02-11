---
phase: 05-password-management
plan: 04
subsystem: auth
tags: [spring-mvc, thymeleaf, rest-api, password, security-config, session-management]

# Dependency graph
requires:
  - phase: 05-02
    provides: "Thymeleaf templates for change-password, forgot-password, reset-password, reset-success, reset-error pages"
  - phase: 05-03
    provides: "PasswordService with changePassword, requestPasswordReset, resetPassword methods; PasswordResetResult sealed interface; JWT invalidation via passwordChangedAt"
provides:
  - "PasswordController REST API with 3 endpoints under /api/v1/auth/"
  - "PasswordWebController with 6 handler methods (GET/POST for change, forgot, reset)"
  - "SecurityConfig permitAll for /forgot-password and /reset-password web URLs"
affects: [05-05-integration-testing]

# Tech tracking
tech-stack:
  added: []
  patterns: [sealed-interface-switch-routing, session-invalidation-on-password-change, null-auth-check-for-permitAll-endpoints]

key-files:
  created:
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordController.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordWebController.java"
  modified:
    - "src/main/java/com/example/usermanagement/auth/internal/SecurityConfig.java"

key-decisions:
  - "Null authentication check in PasswordController.changePassword since /api/v1/auth/** is permitAll"
  - "Session invalidation + SecurityContext clear on web password change forces re-login"
  - "Rate limiter reused from verification module (ResendRateLimiter) for forgot-password flow"

patterns-established:
  - "Sealed interface switch for result routing: PasswordResetResult cases map to different views/responses"
  - "Null auth guard pattern: endpoints under permitAll paths that logically require auth check authentication == null"
  - "Web password change flow: clear SecurityContext, invalidate session, redirect to login with toast"

# Metrics
duration: 4min
completed: 2026-02-11
---

# Phase 5 Plan 4: Controllers & Security Config Summary

**REST API and Thymeleaf controllers for all password operations with SecurityConfig permitAll for public reset URLs**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-11T07:38:43Z
- **Completed:** 2026-02-11T07:42:49Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- PasswordController REST API with 3 endpoints: change-password (authenticated), forgot-password (SEC-01 compliant, rate limited), reset-password (token-based with exhaustive result switching)
- PasswordWebController with 6 handlers: GET/POST for change-password, forgot-password, and reset-password flows
- SecurityConfig updated to permitAll /forgot-password and /reset-password for unauthenticated access
- Web password change flow invalidates session and clears SecurityContext, forcing re-login

## Task Commits

Each task was committed atomically:

1. **Task 1: PasswordController (REST API endpoints)** - `39051d6` (feat)
2. **Task 2: PasswordWebController and SecurityConfig update** - `01696d8` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordController.java` - REST API for change-password, forgot-password, reset-password
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordWebController.java` - Thymeleaf controllers for all password web flows
- `src/main/java/com/example/usermanagement/auth/internal/SecurityConfig.java` - Added /forgot-password and /reset-password to web chain permitAll

## Decisions Made
- **Null auth check for change-password API:** Since /api/v1/auth/** is permitAll in the API chain, the change-password endpoint checks `authentication == null` and returns 401 rather than restructuring URL patterns. Simpler than moving the endpoint outside the auth path.
- **Reused ResendRateLimiter:** The existing rate limiter from Phase 4 verification is reused for the forgot-password flow (same 60-second cooldown per email), keeping rate limiting consistent across the application.
- **Session invalidation pattern:** Web password change explicitly clears SecurityContextHolder and invalidates the HttpSession before redirecting to login. This ensures the user cannot continue with the old session.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

Pre-existing test failures (3) confirmed unrelated to changes:
- ModularityTests.verifiesModularStructure (known blocker in STATE.md)
- SchemaComparisonTests.devAndProdMigrationsShouldHaveSameVersions (migration version mismatch)
- AuthWebControllerTest.login_withValidCredentials_redirectsToHome (pre-existing)

All SecurityConfigTest (12), AuthControllerTest, JwtServiceTest, EmailVerificationIntegrationTest, and ApplicationTests (29 total) pass successfully.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All password endpoints (API and web) are wired and compiling
- Ready for Plan 05 (integration testing / manual verification)
- Templates from Plan 02 are connected to controllers
- PasswordService from Plan 03 is fully consumed by both controllers

## Self-Check: PASSED

All files verified present. All commit hashes found in git log.

---
*Phase: 05-password-management*
*Completed: 2026-02-11*
