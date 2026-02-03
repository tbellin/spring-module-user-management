---
phase: 02-security--api-foundation
plan: 05
subsystem: api
tags: [exception-handling, problemdetail, rfc9457, spring-mvc]

# Dependency graph
requires:
  - phase: 02-02
    provides: ProblemDetail configuration (spring.mvc.problemdetails.enabled)
provides:
  - GlobalExceptionHandler for RFC 9457 ProblemDetail responses
  - Custom exception classes (ResourceNotFoundException, DuplicateResourceException, BadRequestException)
  - Consistent JSON error structure for application exceptions
affects: [auth-endpoints, user-endpoints, admin-endpoints, api-controllers]

# Tech tracking
tech-stack:
  added: []
  patterns: [RFC 9457 ProblemDetail, ResponseEntityExceptionHandler extension]

key-files:
  created:
    - src/main/java/com/example/usermanagement/shared/exception/GlobalExceptionHandler.java
    - src/main/java/com/example/usermanagement/shared/exception/ResourceNotFoundException.java
    - src/main/java/com/example/usermanagement/shared/exception/DuplicateResourceException.java
    - src/main/java/com/example/usermanagement/shared/exception/BadRequestException.java
  modified: []

key-decisions:
  - "GlobalExceptionHandler extends ResponseEntityExceptionHandler for Spring MVC exception inheritance"
  - "Custom properties on ProblemDetail for additional context (resourceType, field, errors map)"
  - "Generic 500 message to avoid leaking internal details"
  - "Debug-level logging for expected errors (404, 409, 400); ERROR level for unexpected (500)"

patterns-established:
  - "ProblemDetail.forStatus() for all error responses"
  - "Custom exception classes with context fields (resourceType, identifier, field)"
  - "Validation errors include field-level error map via setProperty('errors', Map)"

# Metrics
duration: 1min
completed: 2026-02-03
---

# Phase 02 Plan 05: GlobalExceptionHandler Summary

**RFC 9457 ProblemDetail-based error handling with GlobalExceptionHandler and custom exceptions for 404/409/400 responses**

## Performance

- **Duration:** 1 min 24 sec
- **Started:** 2026-02-03T16:07:07Z
- **Completed:** 2026-02-03T16:08:31Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Custom exception classes for common HTTP error scenarios (404, 409, 400)
- GlobalExceptionHandler producing RFC 9457 ProblemDetail JSON responses
- Field-level validation error details for MethodArgumentNotValidException
- Generic 500 error message that prevents internal detail leakage

## Task Commits

Each task was committed atomically:

1. **Task 1: Create custom exception classes** - `143c41e` (feat)
2. **Task 2: Create GlobalExceptionHandler** - `167547d` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/shared/exception/ResourceNotFoundException.java` - 404 Not Found exception with resourceType/identifier
- `src/main/java/com/example/usermanagement/shared/exception/DuplicateResourceException.java` - 409 Conflict exception with resourceType/field
- `src/main/java/com/example/usermanagement/shared/exception/BadRequestException.java` - 400 Bad Request exception
- `src/main/java/com/example/usermanagement/shared/exception/GlobalExceptionHandler.java` - @RestControllerAdvice extending ResponseEntityExceptionHandler

## Decisions Made
- GlobalExceptionHandler extends ResponseEntityExceptionHandler to inherit Spring MVC exception handling
- Uses ProblemDetail.setProperty() for additional context (resourceType, field, errors map)
- Generic "An unexpected error occurred" message for 500 errors to prevent internal detail leakage
- Debug-level logging for expected errors (404, 409, 400); ERROR level for unexpected exceptions (500)
- IllegalArgumentException mapped to 400 Bad Request (catches programming errors escaping validation)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- GlobalExceptionHandler ready for use by all API controllers
- Custom exceptions can be thrown from service layer
- Security exceptions (401, 403) handled separately by Spring Security handlers (not this @ControllerAdvice)
- SUCCESS CRITERIA #4 satisfied for application-level exceptions

---
*Phase: 02-security--api-foundation*
*Completed: 2026-02-03*
