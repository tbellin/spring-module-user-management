---
phase: 06-user-profile--admin-operations
plan: 01
subsystem: user, api, ui
tags: [spring-data, jpa-specification, thymeleaf, profile, rest-api, bootstrap, inline-editing]

# Dependency graph
requires:
  - phase: 05-password-management
    provides: "Complete auth flow with password management; UserService, UserDto, SecurityConfig patterns"
  - phase: 02-security--api-foundation
    provides: "Dual filter chain (API + web), ResourceNotFoundException, BadRequestException, Toast DTO"
provides:
  - "UserDto with createdAt field for profile display"
  - "UserSpecifications for composable search/filter queries"
  - "UserService CRUD methods: getUserById, findUsers, updateProfile, updateUser, toggleUserEnabled"
  - "ProfileController REST API at /api/v1/users/me (GET/PUT)"
  - "ProfileWebController Thymeleaf at /profile (GET/POST)"
  - "Profile page with card layout and inline editing"
  - "Navbar: My Profile link, conditional Admin link, CSRF meta tags"
affects: [06-02, 06-03, 06-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JPA Specification pattern for dynamic query composition"
    - "Inline editing toggle pattern with JavaScript view/edit mode switching"
    - "CSRF meta tags in layout head for JavaScript fetch calls"

key-files:
  created:
    - src/main/java/com/example/usermanagement/user/internal/UserSpecifications.java
    - src/main/java/com/example/usermanagement/user/internal/ProfileController.java
    - src/main/java/com/example/usermanagement/user/internal/ProfileWebController.java
    - src/main/java/com/example/usermanagement/user/internal/ProfileUpdateRequest.java
    - src/main/resources/templates/profile/profile.html
  modified:
    - src/main/java/com/example/usermanagement/shared/dto/UserDto.java
    - src/main/java/com/example/usermanagement/user/UserService.java
    - src/main/java/com/example/usermanagement/user/internal/UserRepository.java
    - src/main/resources/templates/layout/default.html

key-decisions:
  - "ResourceNotFoundException uses (resourceType, identifier) two-arg constructor matching existing pattern"
  - "JPA Specification withFilters combines search/role/status with AND; search matches email/firstName/lastName with OR"
  - "Profile inline editing uses JavaScript view/edit mode toggle (no AJAX, form POST with redirect)"

patterns-established:
  - "UserSpecifications.withFilters() for composable dynamic queries consumed by admin features"
  - "ProfileUpdateRequest record DTO with Size validation for REST profile updates"
  - "Inline edit toggle: viewMode/editMode divs toggled by JavaScript, Edit button hidden in edit mode"

# Metrics
duration: 4min
completed: 2026-02-11
---

# Phase 6 Plan 1: User Profile & Admin Service Foundation Summary

**Extended UserDto/UserService/UserRepository with profile CRUD and search, plus complete profile feature (REST + Thymeleaf) with card layout and inline editing**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-11T14:37:57Z
- **Completed:** 2026-02-11T14:42:32Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Extended UserDto with createdAt field; updated UserService.toUserDto() to pass it
- Added JpaSpecificationExecutor to UserRepository and created UserSpecifications with composable search/filter/status criteria
- Added 5 new methods to UserService: getUserById, findUsers, updateProfile, updateUser, toggleUserEnabled
- Created ProfileController REST API (GET/PUT /api/v1/users/me) and ProfileWebController (GET/POST /profile)
- Built profile.html page with Bootstrap card layout, read-only info section, and inline editing toggle
- Updated navbar with "My Profile" dropdown item, conditional "Admin" link for admins, and CSRF meta tags

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend UserDto, UserRepository, UserSpecifications, and UserService** - `903ad26` (feat)
2. **Task 2: Create profile controllers, template, and navbar link** - `de32df4` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/shared/dto/UserDto.java` - Added createdAt field and updated factory method
- `src/main/java/com/example/usermanagement/user/UserService.java` - Added getUserById, findUsers, updateProfile, updateUser, toggleUserEnabled methods
- `src/main/java/com/example/usermanagement/user/internal/UserRepository.java` - Added JpaSpecificationExecutor extension
- `src/main/java/com/example/usermanagement/user/internal/UserSpecifications.java` - Dynamic query builder with search/role/status filters
- `src/main/java/com/example/usermanagement/user/internal/ProfileController.java` - REST API for /api/v1/users/me (GET/PUT)
- `src/main/java/com/example/usermanagement/user/internal/ProfileWebController.java` - Thymeleaf controller for /profile (GET/POST)
- `src/main/java/com/example/usermanagement/user/internal/ProfileUpdateRequest.java` - Record DTO with Size validation
- `src/main/resources/templates/profile/profile.html` - Profile page with card layout and inline editing
- `src/main/resources/templates/layout/default.html` - Added My Profile link, Admin link, CSRF meta tags

## Decisions Made
- ResourceNotFoundException uses two-arg constructor (resourceType, identifier) matching existing project convention
- JPA Specification pattern used for dynamic queries: withFilters combines search/role/status with AND; search uses OR across email/firstName/lastName
- Profile inline editing uses JavaScript view/edit mode toggle with form POST and redirect (no AJAX) for simplicity and consistency with existing patterns

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed ResourceNotFoundException constructor calls**
- **Found during:** Task 1 (UserService new methods)
- **Issue:** Plan specified single-arg constructor `new ResourceNotFoundException("User not found")` but existing class requires two args `(resourceType, identifier)`
- **Fix:** Changed to `new ResourceNotFoundException("User", email)` and `new ResourceNotFoundException("User", id.toString())`
- **Files modified:** src/main/java/com/example/usermanagement/user/UserService.java
- **Verification:** Compilation succeeds after fix
- **Committed in:** 903ad26 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Constructor signature mismatch was a minor plan oversight. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Service layer foundation complete: UserService has all CRUD methods for admin features (Plans 02-04)
- UserSpecifications ready for admin user list search/filter (Plan 03)
- Profile feature complete end-to-end (REST + Thymeleaf)
- Navbar has Admin link ready for admin pages (Plan 02)

## Self-Check: PASSED

All 9 files verified present. Both task commits (903ad26, de32df4) verified in git log. Compilation succeeds.

---
*Phase: 06-user-profile--admin-operations*
*Completed: 2026-02-11*
