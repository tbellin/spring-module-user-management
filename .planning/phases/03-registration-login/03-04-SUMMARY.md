---
phase: 03-registration-login
plan: 04
subsystem: auth
tags: [testing, integration-tests, mockMvc, jwt, session]
dependency-graph:
  requires: [03-02, 03-03]
  provides: [auth-integration-tests]
  affects: []
tech-stack:
  added: []
  patterns: [integration-testing-pattern, unique-test-data]
key-files:
  created:
    - src/test/java/com/example/usermanagement/auth/AuthControllerTest.java
    - src/test/java/com/example/usermanagement/auth/AuthWebControllerTest.java
  modified:
    - src/main/java/com/example/usermanagement/shared/exception/GlobalExceptionHandler.java
    - src/main/resources/templates/auth/register.html
decisions:
  - id: 03-04-01
    area: testing
    choice: "Jackson 3 JsonMapper for test JSON serialization (not ObjectMapper)"
  - id: 03-04-02
    area: testing
    choice: "Unique UUIDs per test for email addresses to avoid test interference"
  - id: 03-04-03
    area: bugfix
    choice: "AuthenticationException handler added for 401 responses in controller"
metrics:
  duration: 7min
  completed: 2026-02-04
---

# Phase 03 Plan 04: Auth Integration Tests Summary

JWT API and session-based web integration tests with MockMvc covering registration, login, and logout flows.

## What Was Built

### Task 1: AuthController Integration Tests

Created `/src/test/java/com/example/usermanagement/auth/AuthControllerTest.java` with 8 integration tests:

1. `register_withValidData_returns201WithJwt` - Verifies successful registration returns JWT
2. `register_withDuplicateEmail_returns409` - Verifies conflict on duplicate email
3. `register_withInvalidEmail_returns400` - Verifies validation for invalid email format
4. `register_withShortPassword_returns400` - Verifies password minimum length validation
5. `login_withValidCredentials_returns200WithJwt` - Verifies successful login returns JWT
6. `login_withRememberMe_returnsLongerExpiration` - Verifies rememberMe extends token expiration
7. `login_withInvalidPassword_returns401` - Verifies 401 for wrong password
8. `login_withNonexistentEmail_returns401` - Verifies same 401 message (no user enumeration)

**Key patterns:**
- UUID-based unique emails per test to avoid H2 state interference
- Jackson 3 `JsonMapper` (not Jackson 2 `ObjectMapper`) for Spring Boot 4 compatibility
- Helper method `registerUser()` for test setup

### Task 2: AuthWebController Integration Tests

Created `/src/test/java/com/example/usermanagement/auth/AuthWebControllerTest.java` with 8 integration tests:

1. `getLoginPage_returnsLoginView` - GET /login returns auth/login view
2. `getRegisterPage_returnsRegisterView` - GET /register returns form with registrationForm model
3. `register_withValidData_redirectsToHome` - POST /register redirects with toast
4. `register_withDuplicateEmail_returnsRegisterViewWithError` - Duplicate email shows error
5. `register_withInvalidData_returnsRegisterViewWithErrors` - Validation errors stay on page
6. `login_withValidCredentials_redirectsToHome` - POST /login redirects to /
7. `login_withInvalidCredentials_redirectsToLoginWithError` - Bad credentials redirect to /login?error
8. `logout_redirectsToLoginWithLogout` - POST /logout redirects to /login?logout

**Key patterns:**
- `.with(csrf())` for all POST requests
- Form params (not JSON) for web endpoints
- API helper `registerUserViaApi()` for faster test setup

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] AuthenticationException returning 500 instead of 401**
- **Found during:** Task 1 (login_withInvalidPassword test)
- **Issue:** AuthenticationException from AuthenticationManager.authenticate() was falling through to generic Exception handler, returning 500
- **Fix:** Added `@ExceptionHandler(AuthenticationException.class)` to GlobalExceptionHandler returning 401 with "Bad credentials"
- **Files modified:** GlobalExceptionHandler.java
- **Commit:** 2fdb00a

**2. [Rule 1 - Bug] Thymeleaf #fields.errors() outside form context**
- **Found during:** Task 2 (getRegisterPage test)
- **Issue:** Error summary using `${#fields.hasErrors('*')}` was placed before `<form th:object>`, causing TemplateProcessingException
- **Fix:** Moved error summary div inside the form element where #fields context is available
- **Files modified:** register.html
- **Commit:** 5833083

## Test Coverage Verification

| Requirement | Test Coverage |
|------------|---------------|
| AUTH-01 (Registration) | 4 API tests + 3 web tests |
| AUTH-04 (Login) | 4 API tests + 2 web tests |
| AUTH-05 (Logout) | 1 web test |
| SEC-01 (No user enumeration) | 2 API tests (same 401 message) |

## Decisions Made

1. **Jackson 3 JsonMapper for tests**: Spring Boot 4 uses Jackson 3.x (`tools.jackson.*` namespace), not Jackson 2.x (`com.fasterxml.jackson.*`). Tests create JsonMapper manually as there's no autowired ObjectMapper bean.

2. **UUID-based unique emails**: Each test generates `{prefix}-{UUID}@example.com` to ensure test isolation without needing `@DirtiesContext`.

3. **API helper for web tests**: Web tests use `registerUserViaApi()` for faster user creation rather than form submission.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 2fdb00a | test | AuthController integration tests (8 tests) + AuthenticationException handler fix |
| 5833083 | test | AuthWebController integration tests (8 tests) + Thymeleaf #fields fix |

## Next Phase Readiness

**Ready:** Phase 03 Plan 05 (complete flow testing) can proceed.

**Note:** Pre-existing ModularityTests failure unrelated to this plan. The test reports violations even for allowed dependencies (user->shared, auth->shared). This appears to be a Spring Modulith configuration issue that existed before this plan.
