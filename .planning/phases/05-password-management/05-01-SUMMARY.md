---
phase: 05-password-management
plan: 01
subsystem: auth
tags: [jpa, flyway, sealed-interface, bean-validation, password-reset]

# Dependency graph
requires:
  - phase: 01-project-bootstrap
    provides: "V1 schema with password_reset_token table"
  - phase: 04-email-verification
    provides: "VerificationToken pattern (entity, repository, sealed result)"
provides:
  - "PasswordResetToken JPA entity for password_reset_token table"
  - "PasswordResetTokenRepository with atomic markAsUsed"
  - "PasswordResetResult sealed interface (4 outcome types)"
  - "Request DTOs: ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest"
  - "Flyway migration for password_changed_at column on app_user"
  - "AppUser.passwordChangedAt field for JWT invalidation"
affects: [05-02-password-service, 05-03-email-templates, 05-04-controllers, 05-05-testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sealed interface for exhaustive outcome types (PasswordResetResult with 4 variants)"
    - "Atomic CAS query pattern for token consumption (markAsUsed)"
    - "Record DTOs with Bean Validation annotations"

key-files:
  created:
    - "src/main/resources/db/migration/postgresql/V2__add_password_changed_at.sql"
    - "src/main/resources/db/migration/h2/V3__add_password_changed_at.sql"
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetToken.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetTokenRepository.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetResult.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/ChangePasswordRequest.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/ForgotPasswordRequest.java"
    - "src/main/java/com/example/usermanagement/auth/internal/password/ResetPasswordRequest.java"
  modified:
    - "src/main/java/com/example/usermanagement/user/internal/AppUser.java"

key-decisions:
  - "PasswordResetResult has 4 types (not 5 like VerificationResult) - no AlreadyReset equivalent since password reset is not idempotent"
  - "ChangePasswordRequest has 3 fields including currentPassword (per REQUIREMENTS.md PASS-01)"
  - "password_changed_at column is nullable - existing users have no change history"

patterns-established:
  - "auth/internal/password/ package for password management infrastructure"
  - "PasswordResetToken mirrors VerificationToken structure exactly"

# Metrics
duration: 3min
completed: 2026-02-11
---

# Phase 5 Plan 01: Data Layer Foundation Summary

**PasswordResetToken entity, repository with atomic CAS, sealed result interface, and 3 request DTOs following Phase 4 VerificationToken patterns**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-10T23:23:03Z
- **Completed:** 2026-02-10T23:25:47Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments
- Flyway migrations (V2 PostgreSQL, V3 H2) add password_changed_at column to app_user for JWT invalidation
- PasswordResetToken JPA entity maps to existing password_reset_token table from V1 schema
- PasswordResetTokenRepository with atomic markAsUsed query prevents race conditions on token consumption
- PasswordResetResult sealed interface provides compile-time exhaustive outcome handling (4 types)
- Three request DTOs with Bean Validation: ChangePasswordRequest, ForgotPasswordRequest, ResetPasswordRequest

## Task Commits

Each task was committed atomically:

1. **Task 1: Flyway migrations for passwordChangedAt column** - `0b5ac01` (feat)
2. **Task 2: PasswordResetToken entity, repository, and sealed result** - `31f40b9` (feat)
3. **Task 3: Request DTO records for password endpoints** - `5b19925` (feat)

## Files Created/Modified
- `src/main/resources/db/migration/postgresql/V2__add_password_changed_at.sql` - Adds password_changed_at column to app_user (PostgreSQL)
- `src/main/resources/db/migration/h2/V3__add_password_changed_at.sql` - Same migration for H2 dev database
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetToken.java` - JPA entity for password_reset_token table
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetTokenRepository.java` - Repository with atomic markAsUsed and deleteByUser
- `src/main/java/com/example/usermanagement/auth/internal/password/PasswordResetResult.java` - Sealed interface with Success, Expired, Invalid, AlreadyUsed
- `src/main/java/com/example/usermanagement/auth/internal/password/ChangePasswordRequest.java` - DTO for authenticated password change (3 fields)
- `src/main/java/com/example/usermanagement/auth/internal/password/ForgotPasswordRequest.java` - DTO for requesting reset email
- `src/main/java/com/example/usermanagement/auth/internal/password/ResetPasswordRequest.java` - DTO for resetting password via token
- `src/main/java/com/example/usermanagement/user/internal/AppUser.java` - Added passwordChangedAt field with getter/setter

## Decisions Made
- PasswordResetResult has 4 outcome types (not 5 like VerificationResult) because password reset is not an idempotent state -- no "AlreadyReset" equivalent needed
- ChangePasswordRequest includes currentPassword field (3 fields total) per REQUIREMENTS.md PASS-01, overriding CONTEXT.md which initially described 2 fields
- password_changed_at column is nullable with no default -- existing users have no password change history

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Data layer complete: entity, repository, result types, and DTOs ready for PasswordService (Plan 02)
- AppUser.passwordChangedAt field ready for JWT invalidation logic
- All files compile cleanly with `./mvnw compile -q`

---
*Phase: 05-password-management*
*Completed: 2026-02-11*
