---
status: resolved
trigger: "Bug 1: Profile save returns 500 Internal Server Error. Bug 2: Admin user list has no username field"
created: 2026-02-23T00:00:00Z
updated: 2026-02-23T00:30:00Z
symptoms_prefilled: true
---

## Current Focus

hypothesis: CONFIRMED - both root causes identified
test: full code trace completed
expecting: n/a
next_action: report findings

## Symptoms

expected:
  bug1: PUT /api/v1/users/me or form submit to /profile succeeds and saves profile
  bug2: Admin user list shows username column alongside other user fields

actual:
  bug1: 500 Internal Server Error with "An unexpected error occurred"
  bug2: Admin user list is missing username field

errors:
  bug1: '{"detail":"An unexpected error occurred. Please try again later.","instance":"/profile","status":500,"title":"Internal Server Error"}'
  bug2: No error, just missing field in UI

reproduction:
  bug1: Login as user, navigate to /profile, submit the profile form (POST /profile or PUT /api/v1/users/me)
  bug2: Login as admin, navigate to admin user list page

started: unknown

## Eliminated

- hypothesis: displayName field missing from ProfileUpdateRequest
  evidence: ProfileUpdateRequest.java has displayName, firstName, lastName fields
  timestamp: 2026-02-23T00:10:00Z

- hypothesis: ProfileWebController doesn't call updateProfile correctly
  evidence: ProfileWebController.java line 66 passes displayName, firstName, lastName - matches service signature exactly
  timestamp: 2026-02-23T00:10:00Z

- hypothesis: UserDto is missing username field (Bug 2)
  evidence: UserDto.java has username field at line 31; UserService.toUserDto passes user.getUsername() at line 240
  timestamp: 2026-02-23T00:15:00Z

## Evidence

- timestamp: 2026-02-23T00:10:00Z
  checked: UserService.updateProfile (UserService.java:159-171)
  found: |
    The method calls user.setUsername(displayName) when displayName is non-null/non-blank.
    The username column in app_user has a UNIQUE constraint (V1__init_schema.sql:9, AppUser.java:38).
    The profile form sends displayName - if the user types a display name that already exists
    as another user's username, the DB save will fail with a DataIntegrityViolationException,
    which is NOT caught by any specific exception handler and falls through to the generic
    Exception handler in GlobalExceptionHandler.java:158 -> returns 500.
    BUT there is a more fundamental issue: updateProfile blindly sets username to displayName
    without checking if that username is already taken by another user. Even if the user
    types their OWN current username, it should succeed (same value, no conflict). But if
    they type a displayName that happens to match another user's username, it fails with 500.
    ALSO: if the user submits the form with displayName=null or blank, username is NOT updated,
    which is correct. The form does send displayName because the input has name="displayName"
    and th:value="${user.username()}". So the current username is pre-filled - if user does
    not change it and saves, the same value is set - no conflict. BUT if they clear the field
    or type a taken name, it fails.
    HOWEVER: re-examining more carefully: setting the same username value on save should not
    violate uniqueness since it's the same row. The real problem is more subtle:
    the DataIntegrityViolationException from a duplicate username attempt is not handled -> 500.
  implication: |
    The 500 is triggered when displayName conflicts with another user's username,
    or when there is an unhandled database exception. The missing handler is the root cause.

- timestamp: 2026-02-23T00:12:00Z
  checked: GlobalExceptionHandler.java - all exception handlers
  found: |
    Handlers exist for: ResourceNotFoundException, DuplicateResourceException,
    BadRequestException, MethodArgumentNotValidException, IllegalArgumentException,
    AuthenticationException. The catch-all Exception handler returns 500 with generic message.
    There is NO handler for DataIntegrityViolationException (Spring/Hibernate exception
    thrown when DB UNIQUE constraint is violated).
    DuplicateResourceException is an application-level exception thrown by application code,
    NOT the DB-level exception Spring Data translates constraint violations into.
  implication: |
    When userRepository.save(user) fails due to unique constraint violation on username,
    Spring Data throws DataIntegrityViolationException -> falls to generic 500 handler.

- timestamp: 2026-02-23T00:15:00Z
  checked: ProfileWebController.java - updateProfile POST handler
  found: |
    Line 60-66: @RequestParam String displayName (required=true, NOT required=false).
    If the form submits with displayName empty string (user clears the field),
    Spring will receive the empty string (not missing param), which will be passed to
    updateProfile(). In updateProfile, the check is: if (displayName != null && !displayName.isBlank())
    so an empty string is isBlank()=true -> username NOT updated -> safe.
    BUT: the @RequestParam String displayName is NOT optional (required=true by default).
    If displayName param is missing entirely from form submission, Spring throws
    MissingServletRequestParameterException -> which ResponseEntityExceptionHandler handles -> 400.
    This path is not the issue because the form always sends it.
    The profile form at profile.html:70 has name="displayName" and th:value="${user.username()}"
    - it's always populated and sent with the form.
  implication: |
    The form sends displayName correctly. The issue is not missing param.
    The real 500 scenario: user changes displayName to a value that conflicts with
    another user's username column (UNIQUE constraint). DB throws DataIntegrityViolationException.

- timestamp: 2026-02-23T00:18:00Z
  checked: admin/users.html Thymeleaf template - table header and data rows
  found: |
    Table headers (lines 64-106): Email, Name, Role, Status, Created, Actions.
    Data rows (lines 110-165):
      - td: user.email()
      - td: user.firstName() + ' ' + user.lastName() (combined "Name" column)
      - td: user.roles() badges
      - td: status toggle + email verified badge
      - td: createdAt formatted
      - td: Edit/Save/Cancel buttons
    There is NO column for username (user.username()) anywhere in the table.
    The UserDto DOES have a username field, but the template does not render it.
  implication: |
    Bug 2 root cause confirmed: the admin/users.html template simply does not include
    a username column. The data is available in UserDto, just not displayed.

- timestamp: 2026-02-23T00:20:00Z
  checked: AppUser entity (AppUser.java) - username field
  found: |
    Line 38-39: @Column(nullable=false, unique=true, length=100) private String username;
    Username is a proper, separate field from email. It's NOT the same as email.
    It has a UNIQUE constraint. UserService.updateProfile sets username = displayName.
    The profile page labels this "Display Name" in the UI.
  implication: |
    username and email are separate fields. The admin list template not showing username
    is a genuine missing feature/bug - users have both email and username, but only email
    is shown in the admin list, not username.

## Resolution

root_cause_bug1: |
  UserService.updateProfile() (UserService.java:163-165) sets user.setUsername(displayName)
  without checking if the new username is already taken by another user.
  When userRepository.save(user) is called and the new username violates the UNIQUE
  constraint on app_user.username, Spring Data JPA throws DataIntegrityViolationException.
  GlobalExceptionHandler has no handler for DataIntegrityViolationException, so it falls
  through to the generic Exception handler (GlobalExceptionHandler.java:158) which returns
  HTTP 500 with "An unexpected error occurred."

  The secondary root cause is that UserService.updateProfile() lacks a pre-save uniqueness
  check (calling userRepository.existsByUsername() before saving) and does not translate
  DB constraint violations into the application's DuplicateResourceException.

root_cause_bug2: |
  The admin user list Thymeleaf template (admin/users.html) does not include a "Username"
  column in its table. The table has: Email, Name, Role, Status, Created, Actions.
  UserDto (shared/dto/UserDto.java:31) DOES have a username field, and UserService.toUserDto()
  (UserService.java:240) correctly populates it from user.getUsername(). The data is
  available but simply not rendered in the template. A new <th>Username</th> header
  and <td th:text="${user.username()}"> data cell need to be added to the template.

fix_bug1: |
  In UserService.updateProfile() (UserService.java:159-171), before calling setUsername(),
  check if the new username is already taken by a DIFFERENT user:

  if (displayName != null && !displayName.isBlank()) {
      if (!displayName.equals(user.getUsername()) && userRepository.existsByUsername(displayName)) {
          throw new DuplicateResourceException("User", "username", displayName);
      }
      user.setUsername(displayName);
  }

  This converts DB-level constraint violations into application-level DuplicateResourceException
  (which GlobalExceptionHandler already handles as HTTP 409 Conflict), preventing the 500.

fix_bug2: |
  Add a "Username" column to admin/users.html table:
  1. In <thead>, add <th>Username</th> after the Email <th>
  2. In <tbody> <tr>, add <td th:text="${user.username()}">username</td> after the email <td>
  3. Update colspan="6" to colspan="7" on the empty state row (line 167)

files_changed:
  - src/main/java/com/example/usermanagement/user/UserService.java
  - src/main/resources/templates/admin/users.html
