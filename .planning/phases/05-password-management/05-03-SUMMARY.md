---
phase: 05-password-management
plan: 03
subsystem: auth
tags: [password-service, jwt-invalidation, email-service, password-reset, password-change]

# Dependency graph
requires:
  - phase: 05-password-management
    provides: "PasswordResetToken entity, repository, sealed result, request DTOs, AppUser.passwordChangedAt"
  - phase: 04-email-verification
    provides: "EmailService multipart pattern, VerificationService patterns, ResendRateLimiter"
  - phase: 02-security--api-foundation
    provides: "JwtService, JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig"
provides:
  - "PasswordService with changePassword, requestPasswordReset, resetPassword methods"
  - "EmailService.sendPasswordResetEmail method"
  - "JWT invalidation via passwordChangedAt check in JwtAuthenticationFilter"
  - "UserAuthDto with passwordChangedAt field"
  - "CustomUserDetailsService passwordChangedAt cache for filter access"
  - "JwtService.isTokenValid overload with passwordChangedAt"
affects: [05-04-controllers, 05-05-testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ConcurrentHashMap cache in CustomUserDetailsService for passwordChangedAt (avoids second DB query)"
    - "JwtService overloaded isTokenValid with passwordChangedAt comparison"
    - "SEC-01 compliant silent failure in requestPasswordReset (ifPresent pattern)"

key-files:
  created:
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordService.java"
  modified:
    - "src/main/java/com/example/usermanagement/user/UserAuthDto.java"
    - "src/main/java/com/example/usermanagement/user/UserService.java"
    - "src/main/java/com/example/usermanagement/auth/internal/CustomUserDetailsService.java"
    - "src/main/java/com/example/usermanagement/shared/email/EmailService.java"
    - "src/main/java/com/example/usermanagement/auth/JwtService.java"
    - "src/main/java/com/example/usermanagement/auth/internal/JwtAuthenticationFilter.java"
    - "src/main/java/com/example/usermanagement/auth/internal/SecurityConfig.java"

key-decisions:
  - "ConcurrentHashMap cache in CustomUserDetailsService for passwordChangedAt avoids second DB query per JWT request"
  - "JwtAuthenticationFilter accepts CustomUserDetailsService directly (not UserDetailsService interface) to access getPasswordChangedAt()"
  - "SecurityConfig updated to inject CustomUserDetailsService type (CustomUserDetailsService implements UserDetailsService so remember-me still works)"
  - "Rate limiting delegated to controllers, not embedded in PasswordService (consistent with Phase 4 separation)"
  - "PasswordService uses same expirationHours and baseUrl from AppProperties.verification() for reset tokens"
  - "Reset URL format: /reset-password?token=... (query parameter, not path variable)"

patterns-established:
  - "JWT invalidation via passwordChangedAt timestamp comparison against JWT issuedAt"
  - "CustomUserDetailsService as concrete type in filter/config (not interface) when additional methods needed"

# Metrics
duration: 6min
completed: 2026-02-11
---

# Phase 5 Plan 03: Service Layer and JWT Invalidation Summary

**PasswordService with change/request-reset/reset methods, EmailService password reset email, and JWT invalidation via passwordChangedAt timestamp check in JwtAuthenticationFilter**

## Performance

- **Duration:** 6 min
- **Started:** 2026-02-10T23:30:25Z
- **Completed:** 2026-02-10T23:36:42Z
- **Tasks:** 3
- **Files modified:** 8 (1 created, 7 modified)

## Accomplishments
- PasswordService with 3 core methods: changePassword (validates current password), requestPasswordReset (SEC-01 compliant), resetPassword (atomic token consumption)
- JWT invalidation wired end-to-end: UserAuthDto -> UserService -> CustomUserDetailsService cache -> JwtAuthenticationFilter -> JwtService.isTokenValid overload
- EmailService extended with sendPasswordResetEmail following existing multipart pattern
- Both changePassword and resetPassword set passwordChangedAt to invalidate all previously-issued JWT tokens

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend UserAuthDto, UserService, and CustomUserDetailsService for passwordChangedAt** - `a363549` (feat)
2. **Task 2: EmailService extension and PasswordService** - `a2aa32f` (feat)
3. **Task 3: JWT invalidation via passwordChangedAt in filter** - `ebebc9d` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordService.java` - Core password business logic (change, request reset, reset)
- `src/main/java/com/example/usermanagement/user/UserAuthDto.java` - Added passwordChangedAt field (6th record component)
- `src/main/java/com/example/usermanagement/user/UserService.java` - Maps AppUser.passwordChangedAt to UserAuthDto
- `src/main/java/com/example/usermanagement/auth/internal/CustomUserDetailsService.java` - ConcurrentHashMap cache for passwordChangedAt, exposed via getter
- `src/main/java/com/example/usermanagement/shared/email/EmailService.java` - Added sendPasswordResetEmail method
- `src/main/java/com/example/usermanagement/auth/JwtService.java` - Added extractIssuedAt and isTokenValid(token, userDetails, passwordChangedAt) overload
- `src/main/java/com/example/usermanagement/auth/internal/JwtAuthenticationFilter.java` - Uses CustomUserDetailsService, checks passwordChangedAt against JWT issuedAt
- `src/main/java/com/example/usermanagement/auth/internal/SecurityConfig.java` - Injects CustomUserDetailsService instead of UserDetailsService

## Decisions Made
- Used ConcurrentHashMap cache in CustomUserDetailsService to avoid second DB query for passwordChangedAt -- populated during every loadUserByUsername() call (which happens on every JWT request), so it is always fresh
- Changed JwtAuthenticationFilter and SecurityConfig to use CustomUserDetailsService concrete type instead of UserDetailsService interface -- needed to access getPasswordChangedAt() method; still works for remember-me since CustomUserDetailsService implements UserDetailsService
- Rate limiting not embedded in PasswordService -- delegated to controllers (PasswordController and PasswordWebController) for consistency with Phase 4 pattern where ResendRateLimiter is called by controllers
- Reset URL uses query parameter format (/reset-password?token=...) consistent with plan specification
- ResourceNotFoundException constructor takes (resourceType, identifier) matching existing exception class signature

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- PasswordService ready for controller wiring in Plan 04 (PasswordController REST API and PasswordWebController Thymeleaf pages)
- JWT invalidation fully wired: tokens issued before password change will be rejected
- EmailService.sendPasswordResetEmail ready for use by requestPasswordReset flow
- All existing tests pass (JwtServiceTest 7/7, SecurityConfigTest 12/12); pre-existing failures in AuthControllerTest/AuthWebControllerTest are unrelated (missing JavaMailSender mock)

---
*Phase: 05-password-management*
*Completed: 2026-02-11*
