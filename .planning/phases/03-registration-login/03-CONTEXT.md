# Phase 3: Registration & Login - Context

**Gathered:** 2026-02-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can create accounts, log in with email and password to receive a JWT, and log out — through both Thymeleaf pages and REST API endpoints. Home page remains accessible to all visitors. Email verification is a separate phase (Phase 4).

</domain>

<decisions>
## Implementation Decisions

### Registration Flow
- Collect email, password, and display name (all required)
- Single password field with show/hide toggle (no confirmation field)
- After successful registration: auto-login and redirect to home
- API registration returns JWT immediately (consistent with web auto-login behavior)

### Login Behavior
- Include "Remember me" checkbox that extends session/token duration
- No rate limiting on failed attempts (handle at infrastructure level if needed)
- After web login: redirect to original destination (the page user tried to access before login prompt)
- API login response includes JWT plus user info (email, role, display name)

### Form Validation & Feedback
- Client-side validation on form submit (not inline as user types)
- Validation errors displayed in summary at top of form
- Password requirement: minimum 8 characters, no complexity rules
- Success feedback via toast notifications (auto-dismissing)

### Claude's Discretion
- Exact toast notification styling and timing
- Show/hide password toggle implementation
- "Remember me" token duration (standard vs extended)
- Form field ordering and labels
- Loading states during form submission

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-registration-login*
*Context gathered: 2026-02-03*
