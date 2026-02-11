---
phase: 06-user-profile--admin-operations
plan: 03
subsystem: auth, ui, api
tags: [rest-api, thymeleaf, bootstrap, admin, crud, pagination, inline-editing, toggle-switch, csrf, javascript]

# Dependency graph
requires:
  - phase: 06-01
    provides: "UserService with findUsers, updateUser, toggleUserEnabled; UserSpecifications; UserDto with createdAt"
  - phase: 06-02
    provides: "AdminInviteService.inviteUser() for admin user creation with invite email; DuplicateResourceException"
provides:
  - "AdminController REST API at /api/v1/admin/users with list, create, update, toggle endpoints"
  - "AdminWebController Thymeleaf at /admin/users (list) and /admin/users/new (create)"
  - "CreateUserRequest and UpdateUserRequest DTOs"
  - "Admin user list page with search, filters, sortable columns, inline editing, toggle switches, pagination"
  - "Admin create user page with email + role invite form"
affects: [06-04-admin-testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "REST+Thymeleaf dual controller pattern: AdminController (API) and AdminWebController (pages) co-located in auth.internal"
    - "Table inline editing pattern: display-mode/edit-mode cell toggling with JavaScript fetch to REST API"
    - "Toggle switch with undo toast: Bootstrap form-switch + PATCH API + toast with undo button"
    - "Sortable column headers pattern: anchor-based sort links preserving all query params"

key-files:
  created:
    - src/main/java/com/example/usermanagement/user/internal/CreateUserRequest.java
    - src/main/java/com/example/usermanagement/user/internal/UpdateUserRequest.java
    - src/main/java/com/example/usermanagement/auth/internal/AdminController.java
    - src/main/java/com/example/usermanagement/auth/internal/AdminWebController.java
    - src/main/resources/templates/admin/users.html
    - src/main/resources/templates/admin/user-new.html
  modified: []

key-decisions:
  - "AdminController and AdminWebController both placed in auth.internal (not user.internal) to access both UserService and AdminInviteService without module boundary violations"
  - "DTOs (CreateUserRequest, UpdateUserRequest) remain in user.internal as they are simple records with no cross-module dependencies"
  - "Inline editing uses JavaScript fetch to REST API (not form POST) for seamless UX without page reload"
  - "Toggle undo toast auto-dismisses after 8 seconds with undo button that reverses the toggle"

patterns-established:
  - "Admin dual-controller pattern: @RestController for API + @Controller for Thymeleaf in same package"
  - "Table inline editing: display-mode/edit-mode TD cells toggled via JavaScript with REST API save"
  - "Toggle with undo: checkbox onchange -> PATCH API -> undo toast with callback to reverse"
  - "Pagination preserving params: all sort/search/filter params passed in pagination links"

# Metrics
duration: 3min
completed: 2026-02-11
---

# Phase 6 Plan 3: Admin User Management Interface Summary

**Complete admin CRUD interface with paginated user table, search/filter/sort, inline editing via REST API, enable/disable toggle with undo toast, and invite-based user creation form**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-11T14:50:59Z
- **Completed:** 2026-02-11T14:54:30Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- AdminController REST API at /api/v1/admin/users with 4 endpoints: GET (list+search+filter+pagination), POST (create/invite), PUT (update name/role), PATCH (toggle enabled/disabled with self-disable prevention)
- AdminWebController with GET /admin/users (list page), GET /admin/users/new (create form), POST /admin/users/new (form submit with invite)
- Admin user list page with search bar, role/status dropdowns, sortable column headers, Bootstrap data table with inline editing, toggle switches with undo toast, numbered pagination preserving all query parameters
- Admin create user page with email + role form that triggers invite email flow

## Task Commits

Each task was committed atomically:

1. **Task 1: Create admin REST API controller and DTOs** - `0089108` (feat)
2. **Task 2: Create admin web controller and Thymeleaf templates with inline editing** - `48de723` (feat)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/user/internal/CreateUserRequest.java` - Record DTO with @NotBlank @Email validation for admin user creation
- `src/main/java/com/example/usermanagement/user/internal/UpdateUserRequest.java` - Record DTO with optional firstName, lastName, role fields for admin updates
- `src/main/java/com/example/usermanagement/auth/internal/AdminController.java` - REST API with GET/POST/PUT/PATCH at /api/v1/admin/users, including self-disable prevention
- `src/main/java/com/example/usermanagement/auth/internal/AdminWebController.java` - Thymeleaf controller for /admin/users list and /admin/users/new create form
- `src/main/resources/templates/admin/users.html` - Full admin data table with search, filters, sortable columns, inline edit JS, toggle JS, pagination
- `src/main/resources/templates/admin/user-new.html` - Bootstrap card form with email + role for invite-based user creation

## Decisions Made
- Both AdminController and AdminWebController placed in `auth.internal` package (not `user.internal`) because the auth module can depend on both user and shared modules, allowing access to UserService and AdminInviteService without violating Spring Modulith module boundaries
- DTOs (CreateUserRequest, UpdateUserRequest) kept in `user.internal` since they are simple records that the auth module can import via its allowed dependencies
- Inline editing implemented via JavaScript fetch to REST API endpoints (not traditional form POST) for seamless UX without page reload, matching the established CSRF meta tag pattern from the layout
- Toggle undo toast uses Bootstrap Toast API with 8-second auto-dismiss and an undo button that reverses the PATCH call

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Full admin CRUD interface complete and operational
- REST API at /api/v1/admin/users ready for testing in Plan 04
- SecurityConfig already secures /admin/** (hasRole ADMIN) and /api/v1/admin/** (hasRole ADMIN)
- All artifacts compile successfully with zero errors

## Self-Check: PASSED

All 6 created files verified present on disk. Both task commits (0089108, 48de723) verified in git log. Compilation succeeds with zero errors.

---
*Phase: 06-user-profile--admin-operations*
*Completed: 2026-02-11*
