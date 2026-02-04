# Phase 4: Email Verification - Context

**Gathered:** 2026-02-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Email-based account activation flow. After registration, users receive a verification email with a link. Clicking the link activates the account. Users can request a resend if the original email was lost or expired. Unverified accounts cannot log in.

</domain>

<decisions>
## Implementation Decisions

### Email content & sender
- From address: noreply@jbeltsolution.com
- Test recipient: tizianobellin@yahoo.com (for development testing)
- Format: HTML with plain text fallback
- Branding: Minimal - just text and link, no logo/colors
- Subject line: Action-focused, e.g., "Verify your email"

### Verification link behavior
- One-click activation: Link click auto-activates account and shows success page
- After success: Stay on success page with login link (no auto-redirect, no auto-login)
- Error messages: Specific - distinguish "Already verified" vs "Expired" vs "Invalid token"
- Post-registration: Show message on registration page (no dedicated pending verification page)

### Token lifecycle
- Expiration: 24 hours
- Single-use: Yes - token invalidated after first successful use
- Expired accounts: Account persists, user can request new token (no auto-delete)
- Token replacement: Yes - requesting new token invalidates old one

### Resend flow
- Trigger: Dedicated resend page where user enters email
- Rate limiting: Cooldown between resends (e.g., 60 seconds)
- SEC-01 compliance: Same success message regardless of account state ("If account exists, email sent")
- API endpoint: POST /api/v1/auth/resend-verification (unauthenticated - no auth required)

### Claude's Discretion
- Exact email body wording
- HTML email styling within "minimal" constraint
- Token format and length
- Cooldown implementation details
- Database schema for verification tokens

</decisions>

<specifics>
## Specific Ideas

- From address must be noreply@jbeltsolution.com
- All test emails should go to tizianobellin@yahoo.com during development
- Resend endpoint must be unauthenticated (user hasn't verified yet, can't log in)

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope

</deferred>

---

*Phase: 04-email-verification*
*Context gathered: 2026-02-04*
