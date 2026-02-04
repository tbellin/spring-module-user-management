---
phase: 04-email-verification
plan: 03
subsystem: auth
tags: [jpa, verification-token, sealed-interface, atomic-update, spring-data]

# Dependency graph
requires:
  - phase: 01-project-bootstrap
    provides: verification_token table schema (V1__init_schema.sql)
  - phase: 02-security--api-foundation
    provides: AppUser entity, UserRepository, AppProperties config records
  - phase: 04-email-verification (plan 01)
    provides: AppProperties.Verification and AppProperties.Mail config records
provides:
  - VerificationToken JPA entity mapping to verification_token table
  - VerificationTokenRepository with atomic markAsUsed query
  - VerificationService for token lifecycle (create, validate, consume)
  - VerificationResult sealed interface for exhaustive outcome handling
affects:
  - 04-email-verification (plans 04-08): endpoints, email sending, and web controllers use VerificationService

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sealed interface for operation outcomes (VerificationResult)"
    - "Atomic CAS-style update via @Modifying @Query for race condition prevention"
    - "Old token invalidation on new token creation (deleteByUser before save)"

key-files:
  created:
    - src/main/java/com/example/usermanagement/auth/internal/verification/VerificationToken.java
    - src/main/java/com/example/usermanagement/auth/internal/verification/VerificationTokenRepository.java
    - src/main/java/com/example/usermanagement/auth/internal/verification/VerificationResult.java
    - src/main/java/com/example/usermanagement/auth/internal/verification/VerificationService.java
  modified: []

key-decisions:
  - "VerificationService directly uses UserRepository (auth -> user internal access allowed by module hierarchy)"
  - "Sealed interface VerificationResult with 5 outcome types for compile-time exhaustiveness"
  - "Token invalidation via deleteByUser (delete all old tokens) rather than marking old tokens unused"

patterns-established:
  - "Sealed interface for service operation outcomes: enables exhaustive switch in callers"
  - "Atomic state transitions via @Modifying @Query: prevents double-verification race conditions"
  - "Token lifecycle: create (invalidate old) -> validate -> consume (atomic markAsUsed) -> activate user"

# Metrics
duration: 3min
completed: 2026-02-05
---

# Phase 4 Plan 3: Verification Token Infrastructure Summary

**VerificationToken entity, repository with atomic markAsUsed, and VerificationService with sealed VerificationResult for token lifecycle management**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-04T23:26:26Z
- **Completed:** 2026-02-04T23:29:38Z
- **Tasks:** 3
- **Files created:** 4

## Accomplishments
- VerificationToken JPA entity mapping to existing verification_token table with @ManyToOne to AppUser
- VerificationTokenRepository with atomic markAsUsed query preventing race conditions on double-click
- VerificationResult sealed interface with 5 exhaustive outcome types (Success, AlreadyVerified, Expired, Invalid, AlreadyUsed)
- VerificationService handling full token lifecycle: create with old-token invalidation, verify with atomic consumption, build verification URL

## Task Commits

Each task was committed atomically:

1. **Task 1: Create VerificationToken JPA entity** - `a70d44b` (feat)
2. **Task 2: Create VerificationTokenRepository with atomic update** - `18add25` (feat)
3. **Task 3: Create VerificationResult sealed interface and VerificationService** - `6a18072` (feat)

## Files Created/Modified
- `src/main/java/.../auth/internal/verification/VerificationToken.java` - JPA entity mapping to verification_token table with isExpired(), isValid() methods
- `src/main/java/.../auth/internal/verification/VerificationTokenRepository.java` - Repository with atomic markAsUsed, findByToken, deleteByUser
- `src/main/java/.../auth/internal/verification/VerificationResult.java` - Sealed interface with 5 verification outcome types
- `src/main/java/.../auth/internal/verification/VerificationService.java` - Token CRUD, validation, atomic consumption, URL building

## Decisions Made
- VerificationService directly uses UserRepository (auth -> user internal access is allowed by module hierarchy, same pattern as CustomUserDetailsService)
- Sealed interface VerificationResult with 5 outcome types enables compile-time exhaustiveness checking in future switch expressions
- Old token invalidation uses deleteByUser (removes all old tokens) rather than just marking them unused, keeping the table clean
- Token verification checks order: invalid token -> already verified user -> expired token -> already used (atomic) -> success

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- VerificationService ready for use by verification endpoints (plan 04-04) and email sending service (plan 04-05)
- Sealed VerificationResult enables clean handling in controllers with exhaustive pattern matching
- All four files compile cleanly with existing codebase

---
*Phase: 04-email-verification*
*Completed: 2026-02-05*
