# Phase 6: User Profile & Admin Operations - Context

**Gathered:** 2026-02-11
**Status:** Ready for planning

<domain>
## Phase Boundary

Authenticated users can view and edit their own profile (display name, first/last name). Administrators can fully manage all user accounts: list with pagination, create via invite email, update inline, enable/disable, and search/filter. Both web (Thymeleaf) and REST API interfaces.

</domain>

<decisions>
## Implementation Decisions

### Profile page
- Simple card layout with user info and an Edit button that enables inline editing
- Read-only fields: email, role, account status, member since
- Editable fields: display name, first name, last name
- Access via "My Profile" link in existing navbar user dropdown (next to logout)
- Save updates inline without navigating away

### Admin user list
- Data table with columns: email, name, role, status, created date
- Sortable columns, clean rows
- Single search bar that searches across name and email
- Dropdown filters beside search bar for role and status
- Classic numbered pagination (1, 2, 3... N) with prev/next buttons and total count
- 10 users per page by default

### Admin CRUD flow
- Create user: separate dedicated page (/admin/users/new) with full form (email, role)
- System sends invite/set-password email — admin does not set the password
- User is marked as verified upon invite; user sets their own password via email link
- Edit user: inline table editing — click edit icon on a row to make it editable in-place, save without leaving the list
- Enable/disable: toggle switch on each row that acts immediately with an undo toast notification, no confirmation dialog

### Admin navigation
- Dedicated /admin/* URL section: /admin/users, /admin/users/new
- REST API mirrors: /api/v1/admin/users for admin endpoints
- Conditional "Admin" link in main navbar, visible only to ADMIN role users, leads to /admin/users
- Reuse main layout (same navbar and footer) — no separate admin layout or sidebar
- Non-admin access to /admin/* shows styled 403 "Access Denied" error page

### Claude's Discretion
- Table styling and responsive behavior
- Inline edit UX details (save/cancel buttons, validation display)
- Invite email template content and design
- Undo toast timing and behavior for enable/disable toggle
- 403 error page design

</decisions>

<specifics>
## Specific Ideas

- Profile page follows the same simple card pattern used elsewhere in the app (Bootstrap card component)
- Invite email reuses the existing email template infrastructure from Phase 4
- Inline table editing should feel lightweight — edit icon per row, not a heavy form

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 06-user-profile--admin-operations*
*Context gathered: 2026-02-11*
