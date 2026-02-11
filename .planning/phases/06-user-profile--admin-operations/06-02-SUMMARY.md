---
phase: 06-user-profile--admin-operations
plan: 02
subsystem: auth
tags: [email, invite, thymeleaf, password-reset-token, admin, 403-error]

# Dependency graph
requires:
  - phase: 05-password-management
    provides: "PasswordResetToken entity and repository for token-based password setting"
  - phase: 04-email-verification
    provides: "EmailService with multipart email pattern, Thymeleaf email templates"
  - phase: 06-01
    provides: "UserService with findUsers, updateUser, toggleUserEnabled methods"
provides:
  - "AdminInviteService.inviteUser() for admin user creation with invite email"
  - "EmailService.sendInviteEmail() for invite email dispatch"
  - "Invite email templates (HTML and plain text)"
  - "403 Access Denied error page for non-admin access"
affects: [06-03-admin-controllers, 06-04-admin-testing]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Admin invite reuses PasswordResetToken infrastructure (no new DB tables)", "Email uses email as username for invited users"]

key-files:
  created:
    - "src/main/java/com/example/usermanagement/auth/internal/AdminInviteService.java"
    - "src/main/resources/templates/email/invite.html"
    - "src/main/resources/templates/email/invite.txt"
    - "src/main/resources/templates/error/403.html"
  modified:
    - "src/main/java/com/example/usermanagement/shared/email/EmailService.java"

key-decisions:
  - "AdminInviteService placed in auth.internal (not user.internal) because it needs PasswordResetTokenRepository access"
  - "Invited users get email as username (same as self-registration pattern)"
  - "Reuse PasswordResetToken for invite set-password flow (no new DB migration needed)"
  - "UserService.getUserByEmail used for DTO conversion to avoid duplicating toUserDto logic"

patterns-established:
  - "Admin invite pattern: create user with placeholder password + PasswordResetToken + invite email"

# Metrics
duration: 2min
completed: 2026-02-11
---

# Phase 6 Plan 2: Admin Invite Infrastructure Summary

**AdminInviteService coordinating user creation with invite email via PasswordResetToken reuse, plus invite templates and 403 error page**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-11T14:45:21Z
- **Completed:** 2026-02-11T14:47:33Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- EmailService extended with sendInviteEmail method following established multipart email pattern
- AdminInviteService in auth.internal creates users with emailVerified=true, placeholder password, and sends invite email with set-password link
- HTML and plain text invite email templates matching existing password-reset template style
- Styled 403 Access Denied page extending default layout for non-admin access to /admin/* routes

## Task Commits

Each task was committed atomically:

1. **Task 1: Add sendInviteEmail to EmailService and create invite email templates** - `5e18395` (feat)
2. **Task 2: Create AdminInviteService for user invite flow** - `bee8245` (feat)

**Plan metadata:** pending (docs: complete plan)

## Files Created/Modified
- `src/main/java/com/example/usermanagement/shared/email/EmailService.java` - Added sendInviteEmail method
- `src/main/java/com/example/usermanagement/auth/internal/AdminInviteService.java` - Service coordinating admin user invite flow
- `src/main/resources/templates/email/invite.html` - HTML invite email template with set-password button
- `src/main/resources/templates/email/invite.txt` - Plain text invite email template
- `src/main/resources/templates/error/403.html` - Styled 403 Access Denied page with Bootstrap card

## Decisions Made
- AdminInviteService placed in `auth.internal` package (not `user.internal` as initially suggested in plan) because it needs `PasswordResetTokenRepository` from `auth.internal.password`. The auth module has `allowedDependencies = { "user", "shared" }` so it can access both user repositories and email service.
- Invite uses email as username for the created user (same pattern as self-registration per 03-01 decision)
- DTO conversion delegated to `UserService.getUserByEmail()` to avoid duplicating `toUserDto` logic across modules
- Reused `PasswordResetToken` infrastructure for invite set-password link -- no new database tables or Flyway migrations needed

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- AdminInviteService ready for admin controllers in Plan 03
- EmailService.sendInviteEmail ready for admin invite flow
- 403 error page ready for Spring Security access denied responses
- All artifacts compile successfully with zero errors

## Self-Check: PASSED

All 5 created/modified files verified present on disk. Both task commits (5e18395, bee8245) verified in git log.

---
*Phase: 06-user-profile--admin-operations*
*Completed: 2026-02-11*
