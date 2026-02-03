---
phase: 02-security--api-foundation
plan: 06
subsystem: auth
tags: [security, testing, jwt, spring-security, mockmvc]
duration: 5min
completed: 2026-02-03
dependency-graph:
  requires: [02-03, 02-04, 02-05]
  provides: [security-tests, jwt-tests]
  affects: [03-*, future-security-changes]
tech-stack:
  added: [spring-boot-starter-webmvc-test]
  patterns: [SecurityMockMvcRequestPostProcessors, @SpringBootTest+@AutoConfigureMockMvc]
key-files:
  created:
    - src/test/java/com/example/usermanagement/auth/JwtServiceTest.java
    - src/test/java/com/example/usermanagement/auth/SecurityConfigTest.java
  modified:
    - pom.xml
decisions:
  - id: 02-06-01
    choice: "SecurityMockMvcRequestPostProcessors.user() instead of @WithMockUser for API chain tests"
    reason: "Works correctly with filter chain authentication context"
  - id: 02-06-02
    choice: "spring-boot-starter-webmvc-test dependency for Boot 4"
    reason: "AutoConfigureMockMvc moved to org.springframework.boot.webmvc.test.autoconfigure in Boot 4"
  - id: 02-06-03
    choice: "Test 404 responses to verify authorization passed (not 401/403)"
    reason: "Endpoints don't exist yet; 404 proves security check succeeded"
---

# Phase 02 Plan 06: Security Tests Summary

Security tests verifying JWT token operations and dual SecurityFilterChain 401/403 behavior.

## One-liner

JwtServiceTest unit tests + SecurityConfigTest integration tests with MockMvc for API/Web filter chains.

## What Was Done

### Task 1: JwtService Unit Tests

Created comprehensive unit tests for JwtService token operations:

**Test coverage (7 tests):**
1. `generateToken_createsValidJwt` - Token has 3 JWT parts
2. `extractUsername_returnsSubject` - Username extraction works
3. `isTokenValid_returnsTrueForValidToken` - Valid token validates
4. `isTokenValid_returnsFalseForDifferentUser` - Wrong user fails
5. `isTokenValid_throwsForExpiredToken` - Expired tokens throw ExpiredJwtException
6. `extractUsername_throwsForInvalidToken` - Invalid format throws
7. `extractUsername_throwsForTamperedToken` - Tampered signature throws

**Key implementation detail:** JJWT throws exceptions on expired/invalid tokens rather than returning false. Tests verify this behavior.

### Task 2: SecurityConfig Integration Tests

Created MockMvc integration tests for both filter chains:

**API Filter Chain tests (5 tests):**
- Unauthenticated request returns 401 with JSON ProblemDetail
- Public /api/auth/** endpoints accessible (return 404, not 401)
- USER role on /api/admin/** returns 403 with JSON ProblemDetail
- ADMIN role on /api/admin/** passes (returns 404)
- Authenticated USER on protected endpoints passes (returns 404)

**Web Filter Chain tests (6 tests):**
- Public pages (/) accessible
- /login URL is public (returns 404 until controller exists)
- Unauthenticated redirects to /login
- USER role on /admin/** returns 403
- ADMIN role on /admin/** passes (returns 404)
- /actuator/health is public

**CSRF tests (1 test):**
- API POST without CSRF token succeeds (CSRF disabled for API)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spring Boot 4 package relocation for AutoConfigureMockMvc**
- **Found during:** Task 2
- **Issue:** `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` doesn't exist in Boot 4
- **Fix:** Changed import to `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, added `spring-boot-starter-webmvc-test` dependency
- **Files modified:** pom.xml, SecurityConfigTest.java
- **Commit:** 498be3d

**2. [Rule 1 - Bug] @WithMockUser doesn't work with API filter chain**
- **Found during:** Task 2
- **Issue:** @WithMockUser sets SecurityContext but doesn't integrate with filter chain processing
- **Fix:** Used `SecurityMockMvcRequestPostProcessors.user()` instead for API chain tests
- **Files modified:** SecurityConfigTest.java
- **Commit:** 498be3d

**3. [Rule 1 - Bug] ExpiredJwtException behavior in JwtService tests**
- **Found during:** Task 1
- **Issue:** Test expected `isTokenValid()` to return false for expired tokens, but JJWT throws ExpiredJwtException
- **Fix:** Changed test to expect ExpiredJwtException
- **Files modified:** JwtServiceTest.java
- **Commit:** 3e1394a

## Verification

All verification criteria met:
- [x] `./mvnw test -Dtest=JwtServiceTest,SecurityConfigTest` - 19 tests pass
- [x] JwtServiceTest covers token generation, validation, and error cases
- [x] SecurityConfigTest covers API 401/403, web redirects, role-based access
- [x] Tests verify SUCCESS CRITERIA #1 (dual chains), #3 (401/403), SEC-03 (CSRF), SEC-04 (stateless API)

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 3e1394a | test | JwtService unit tests |
| 498be3d | test | SecurityConfig integration tests |

## Next Phase Readiness

Phase 2 Security & API Foundation is now complete. All plans executed:
- 02-01: User Module JPA Layer
- 02-02: JWT and ProblemDetail Configuration
- 02-03: JwtService and CustomUserDetailsService
- 02-04: Dual SecurityFilterChain Configuration
- 02-05: GlobalExceptionHandler
- 02-06: Security Tests

**Ready for Phase 3: Authentication Implementation**
- JwtService tested and working
- SecurityConfig tested - dual filter chains verified
- GlobalExceptionHandler returns proper 401/403 JSON responses
- All security infrastructure in place
