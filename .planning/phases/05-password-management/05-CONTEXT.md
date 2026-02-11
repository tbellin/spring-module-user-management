# Phase 5: Password Management - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Two password flows: (1) **Change password** for authenticated users to update their current password, and (2) **Lost/reset password** for unauthenticated users to recover account access via email token. Both flows require Thymeleaf pages and REST API endpoints. Reuses existing VerificationToken infrastructure from Phase 4.

</domain>

<decisions>
## Implementation Decisions

### Change Password Flow
- Link lives in the **navbar dropdown** (alongside Logout, under user menu)
- Form has **3 fields**: current password + new password + confirm new password (requires current password per PASS-01)
- Minimum password length: **8 characters**, no complexity rules
- After successful change: **redirect to /login** with success toast (session invalidated, user must re-login)

### Reset Password Flow
- Use **separate PasswordResetToken entity** mapping to existing `password_reset_token` table from V1 migration (cleaner separation, no schema change needed)
- Reset tokens valid for **24 hours** (consistent with email verification tokens)
- Non-existent email requests: **always show same success message** ("If an account exists, we sent a reset link") — SEC-01 compliance, prevents user enumeration
- "Forgot your password?" link appears **on the login page only** (below the login form)

### Email Content & Pages
- Reset email tone: **professional/formal** ("You have requested a password reset for your account...")
- Email format: **both HTML and plain text** (multipart, consistent with Phase 4 verification emails)
- After successful reset via email link: **success page with prominent "Log in" button** (no auto-login)
- Expired/invalid token: **error page with a button to request a new reset email** (links back to lost password form)

### Security Behavior
- After password change or reset: **invalidate all sessions/JWT tokens** — force logout everywhere, user must re-login with new password
- Rate limiting on reset requests: **same 60-second cooldown per email** as verification email resend (Phase 4 pattern)
- No password history check — user can set any password including their previous one (v1 simplicity)
- Reset tokens are **single-use** — deleted immediately after successful password reset

### Claude's Discretion
- Exact form layout and field positioning on change/reset pages
- Email template HTML styling (consistent with verification email)
- Validation error message wording
- How session/JWT invalidation is implemented technically

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches consistent with existing Phase 3/4 patterns.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 05-password-management*
*Context gathered: 2026-02-06*
