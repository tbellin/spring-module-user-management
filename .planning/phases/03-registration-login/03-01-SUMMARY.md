---
phase: 03-registration-login
plan: 01
subsystem: auth
tags: [jwt, spring-security, dto, password-hashing, remember-me]

# Dependency graph
requires:
  - phase: 02-security-api-foundation
    provides: JwtService, UserService, PasswordEncoder, SecurityConfig
provides:
  - AuthResponse DTO with token, user info, expiration
  - RegistrationRequest DTO with email/password/displayName validation
  - LoginRequest DTO with rememberMe flag
  - AuthService with registerUser, authenticate, generateToken methods
  - JwtService remember-me support with extended expiration
affects: [03-02, 03-03, 04-email-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Record-based DTOs with Jakarta validation annotations"
    - "AuthService as business logic layer between controllers and repositories"
    - "Remember-me pattern with extended JWT expiration"

key-files:
  created:
    - src/main/java/com/example/usermanagement/auth/AuthResponse.java
    - src/main/java/com/example/usermanagement/auth/internal/RegistrationRequest.java
    - src/main/java/com/example/usermanagement/auth/internal/LoginRequest.java
    - src/main/java/com/example/usermanagement/auth/internal/AuthService.java
  modified:
    - src/main/java/com/example/usermanagement/auth/JwtService.java
    - src/main/java/com/example/usermanagement/shared/config/AppProperties.java
    - src/main/resources/application.yml
    - src/test/java/com/example/usermanagement/auth/JwtServiceTest.java

key-decisions:
  - "Email used as username (per RESEARCH.md recommendation)"
  - "displayName stored in firstName field, lastName null"
  - "Remember-me expiration 7 days vs standard 1 hour"
  - "DuplicateResourceException for email uniqueness without exposing value (SEC-01)"

patterns-established:
  - "AuthService as the facade for authentication operations"
  - "Request DTOs in auth.internal, response DTOs in auth package"

# Metrics
duration: 3min
completed: 2026-02-04
---

# Phase 3 Plan 1: Auth Service Layer Summary

**Auth service layer with DTOs for registration/login and JwtService extended for remember-me token expiration**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-04T06:36:56Z
- **Completed:** 2026-02-04T06:39:54Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments
- AuthResponse, RegistrationRequest, and LoginRequest DTOs with validation
- JwtService extended with generateToken(UserDetails, boolean) for remember-me
- AuthService with registerUser, authenticate, generateToken, and getTokenExpiration methods
- AppProperties.Jwt extended with rememberMeExpirationMs (7 days default)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Auth DTOs** - `dff7171` (feat)
2. **Task 2: Extend JwtService for Remember-Me** - `81dade0` (feat)
3. **Task 3: Create AuthService** - `3146cab` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/auth/AuthResponse.java` - Response DTO with token, tokenType, expiresIn, user info
- `src/main/java/com/example/usermanagement/auth/internal/RegistrationRequest.java` - Request DTO with email/password/displayName validation
- `src/main/java/com/example/usermanagement/auth/internal/LoginRequest.java` - Request DTO with email/password/rememberMe
- `src/main/java/com/example/usermanagement/auth/internal/AuthService.java` - Business logic for registration and authentication
- `src/main/java/com/example/usermanagement/auth/JwtService.java` - Extended with remember-me support
- `src/main/java/com/example/usermanagement/shared/config/AppProperties.java` - Added rememberMeExpirationMs
- `src/main/resources/application.yml` - Added remember-me-expiration-ms property
- `src/test/java/com/example/usermanagement/auth/JwtServiceTest.java` - Updated for new Jwt record signature

## Decisions Made
- Email used as username per RESEARCH.md recommendation (simplifies login flow)
- displayName goes to firstName field (UserService.createUser signature unchanged)
- Remember-me expiration is 7 days (604800000ms) vs standard 1 hour
- DuplicateResourceException thrown without exposing email value (SEC-01 compliance)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated JwtServiceTest for new Jwt record signature**
- **Found during:** Task 2 (Extend JwtService for Remember-Me)
- **Issue:** JwtServiceTest created AppProperties.Jwt with 2 parameters, but new signature requires 3
- **Fix:** Added REMEMBER_ME_EXPIRATION_MS constant and updated Jwt constructor calls
- **Files modified:** src/test/java/com/example/usermanagement/auth/JwtServiceTest.java
- **Verification:** `./mvnw test -Dtest=JwtServiceTest` passes
- **Committed in:** 81dade0 (part of Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Test update required to maintain existing test coverage. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Auth service layer complete, ready for REST API endpoints (03-02)
- AuthService provides registerUser, authenticate, generateToken for controllers
- JwtService supports both standard and remember-me token generation
- All existing tests continue to pass

---
*Phase: 03-registration-login*
*Completed: 2026-02-04*
