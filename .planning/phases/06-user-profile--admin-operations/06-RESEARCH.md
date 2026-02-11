# Phase 6: User Profile & Admin Operations - Research

**Researched:** 2026-02-11
**Domain:** User profile viewing/editing, admin CRUD with pagination/search/filter, invite email flow, Thymeleaf + REST API
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Profile page
- Simple card layout with user info and an Edit button that enables inline editing
- Read-only fields: email, role, account status, member since
- Editable fields: display name, first name, last name
- Access via "My Profile" link in existing navbar user dropdown (next to logout)
- Save updates inline without navigating away

#### Admin user list
- Data table with columns: email, name, role, status, created date
- Sortable columns, clean rows
- Single search bar that searches across name and email
- Dropdown filters beside search bar for role and status
- Classic numbered pagination (1, 2, 3... N) with prev/next buttons and total count
- 10 users per page by default

#### Admin CRUD flow
- Create user: separate dedicated page (/admin/users/new) with full form (email, role)
- System sends invite/set-password email -- admin does not set the password
- User is marked as verified upon invite; user sets their own password via email link
- Edit user: inline table editing -- click edit icon on a row to make it editable in-place, save without leaving the list
- Enable/disable: toggle switch on each row that acts immediately with an undo toast notification, no confirmation dialog

#### Admin navigation
- Dedicated /admin/* URL section: /admin/users, /admin/users/new
- REST API mirrors: /api/v1/admin/users for admin endpoints
- Conditional "Admin" link in main navbar, visible only to ADMIN role users, leads to /admin/users
- Reuse main layout (same navbar and footer) -- no separate admin layout or sidebar
- Non-admin access to /admin/* shows styled 403 "Access Denied" error page

### Claude's Discretion
- Table styling and responsive behavior
- Inline edit UX details (save/cancel buttons, validation display)
- Invite email template content and design
- Undo toast timing and behavior for enable/disable toggle
- 403 error page design

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

## Summary

This phase adds two major feature areas to the existing user management system: (1) a **profile page** where authenticated users can view and edit their own display name, first name, and last name, and (2) a full **admin user management** interface with list/create/update/enable-disable/search/filter capabilities. Both areas need Thymeleaf web pages and REST API endpoints.

The existing codebase provides strong foundations. The `UserService` (sole public API of the user module) already has `getUserByEmail()`, `createUser()`, and `existsByEmail()` methods. The `UserDto` record transfers user data across module boundaries. The `UserRepository` has basic `findByEmail()` and `existsByEmail()` queries. The `SecurityConfig` already has `/admin/**` restricted to `ROLE_ADMIN` in both the API and web filter chains. The email infrastructure (`EmailService`, Thymeleaf email templates, `EmailTemplateConfig`) is fully operational. The layout template (`layout/default.html`) already has a user dropdown in the navbar with Bootstrap styling. The `Toast` DTO and toast rendering is already implemented in the layout.

The primary new work involves: extending `UserDto` with `createdAt` (needed for "member since" and admin table), adding pagination/search/filter query methods to `UserRepository`, expanding `UserService` with update/findAll/findById/toggleEnabled/createWithRole methods, creating profile and admin controllers (both web and API), building Thymeleaf templates for profile page, admin user list, admin create user form, and a 403 error page, creating an invite email template, and adding JavaScript for inline editing and toggle switches on the admin table.

**Primary recommendation:** Extend the existing `UserService` as the sole public API for all new operations, add `JpaSpecificationExecutor` to `UserRepository` for search/filter, use Spring Data `Pageable`/`Page` for pagination, and follow the established dual-controller pattern (web + API) used by auth and password features.

## Critical Findings

### 1. UserDto Needs `createdAt` Field

The current `UserDto` record does NOT include `createdAt`:

```java
public record UserDto(Long id, String email, String username, String firstName, String lastName,
                      boolean enabled, boolean emailVerified, Set<String> roles) { ... }
```

The profile page needs "member since" and the admin table needs "created date." The `AppUser` entity already has `createdAt` (type `LocalDateTime`), so this is a matter of adding the field to `UserDto` and updating the `toUserDto()` method in `UserService`.

**Impact:** This is a breaking change to the `UserDto` record's factory method signature. All callers of `UserDto.from()` must be updated. Review of the codebase shows `UserDto.from()` is called only inside `UserService.toUserDto()`, so the impact is contained.

**Confidence:** HIGH -- direct code observation.

### 2. Admin Invite Flow Reuses Password Reset Infrastructure

The invite flow requires: admin creates user (email + role), user gets an email with a "set password" link, user sets their password. This is functionally identical to the **password reset flow**: generate a token, send an email with a link, user clicks the link and sets a password. The existing `PasswordResetToken`, `PasswordResetTokenRepository`, and the reset-password page can be reused directly.

The key difference: when admin creates a user via invite, the user is created with `emailVerified = true` (per CONTEXT.md: "User is marked as verified upon invite") and `enabled = true`, but with a **random placeholder password hash** (the user cannot log in until they set their actual password via the email link). A password reset token is then generated and an invite email sent.

Alternatively, a simpler approach: create the user with `enabled = false` or a sentinel password, generate a password reset token, and reuse the existing `/reset-password` page flow. The invite email template just needs different wording than the password-reset email.

**Recommendation:** Create user with `emailVerified = true`, `enabled = true`, and a non-matchable placeholder password hash (e.g., `{noop}INVITE_PENDING_` + UUID). Generate a `PasswordResetToken` and send an invite email with a link to `/reset-password?token=...`. The existing reset-password page handles the rest. This avoids duplicating token infrastructure.

**Confidence:** HIGH -- based on direct observation of `PasswordService.resetPassword()`, `PasswordResetToken`, and the invite requirements.

### 3. Spring Modulith Constraints Shape Where Code Lives

The module dependency rules are strict:
- `user` module: `allowedDependencies = { "shared" }` -- cannot depend on `auth`
- `auth` module: `allowedDependencies = { "user", "shared" }` -- can depend on `user`
- `shared` module: `allowedDependencies = {}` -- cannot depend on anything

**Impact on Phase 6:**
- Profile controller must live in `auth` module (it needs `UserService` from `user` module) OR in a new location. However, the profile controller is a user-facing feature, not auth-specific. The cleanest approach: place web controllers that only need `UserService` in the `shared.web` package (where `HomeController` already lives), since `shared` has no allowed dependencies and `UserService` is in the `user` package root (public API). **Wait** -- `shared` cannot depend on `user`. So profile/admin controllers must live either in `auth` (which can depend on `user`) or we need a new module.
- Actually, looking more carefully: the `shared` module's `allowedDependencies = {}` means it cannot access `user` module types. But `UserService` is in `com.example.usermanagement.user` package -- that's the `user` module. So `shared` cannot use it.
- **The profile and admin controllers need to live in the `auth` module** (which has `allowedDependencies = { "user", "shared" }`) or in a new module that depends on `user` and `shared`.
- Looking at precedent: `AuthWebController`, `PasswordWebController`, `PasswordController` all live in `auth.internal`. The profile and admin features are user-management features, not authentication features. Putting them in `auth` would be semantically wrong.
- **Best approach:** Create the profile and admin controllers within the `user` module itself. The `user` module can access its own internal types directly. `UserService` is in the `user` package root. New controllers can go in `user.internal` (for web controllers that access `UserRepository` directly) or even just `user` (if they only use `UserService`). Actually, the `user` module only allows dependency on `shared`, not `auth`. It can use `shared.dto.UserDto`, `shared.dto.Toast`, `shared.exception.*` -- everything it needs.
- **But** profile and admin controllers need `@Controller`/`@RestController` and `Authentication` (from Spring Security, not from `auth` module). Spring Security classes are framework dependencies, not module dependencies. Spring Modulith only tracks application module dependencies, not framework library dependencies. So using `org.springframework.security.core.Authentication` in `user.internal` is fine.

**Recommendation:** Place profile and admin controllers in `user.internal` (package-private) since they primarily operate on user data and the `user` module already depends on `shared` (for `UserDto`, `Toast`, exception classes). This keeps user management concerns in the user module.

**Confidence:** HIGH -- verified against `package-info.java` annotations and `ModularityTests`.

### 4. Security Config Already Has Admin Routes Protected

Both filter chains already include admin route protection:
- API chain: `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`
- Web chain: `.requestMatchers("/admin/**").hasRole("ADMIN")`

Existing tests in `SecurityConfigTest` already verify:
- USER role accessing `/admin/users` returns 403
- ADMIN role accessing `/admin/users` returns 404 (endpoint not yet created)
- USER role accessing `/api/admin/users` returns 403
- ADMIN role accessing `/api/admin/users` returns 404

**No changes needed to SecurityConfig.** The 403 error page just needs to be created as a Thymeleaf template that Spring Security's default web `AccessDeniedHandler` renders.

**Confidence:** HIGH -- direct code and test observation.

### 5. Web Filter Chain 403 Handling for Thymeleaf

The API filter chain has a custom `ApiAccessDeniedHandler` that returns JSON ProblemDetail. The web filter chain does NOT have a custom `AccessDeniedHandler` -- it uses Spring Security's default, which returns a plain 403 status. For a styled 403 page, we have two options:

**Option A:** Create a custom `AccessDeniedHandler` for the web filter chain that forwards to a Thymeleaf template.
**Option B:** Use Spring Boot's error page mechanism -- create `src/main/resources/templates/error/403.html`. Spring Boot auto-registers an `ErrorController` that resolves error pages by HTTP status code.

**Recommendation:** Option B is simpler and follows Spring Boot conventions. Create `templates/error/403.html` using the layout decorator. Spring Boot will automatically render this template for 403 responses on the web filter chain.

**Confidence:** HIGH -- standard Spring Boot error page resolution mechanism.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Data JPA | Boot 4 managed | Pagination, Specification, sorting | Already in use; `Pageable`/`Page` are the standard Spring pagination API |
| Spring Security | 6.x (Boot 4 managed) | Authentication principal, role checking | Already configured with admin route protection |
| Thymeleaf + Layout Dialect | Boot 4 managed | Profile page, admin pages, 403 error | Already used for all existing pages |
| thymeleaf-extras-springsecurity6 | Boot managed | `sec:authorize` for conditional admin link | Already in pom.xml, used in layout for authenticated/anonymous checks |
| Bootstrap 5.3.3 | WebJar | Tables, cards, forms, pagination, toggles | Already in use via WebJar |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JpaSpecificationExecutor | Spring Data | Dynamic search/filter queries | Extend UserRepository to support multi-field search |
| Spring Data `Pageable`/`Page` | Spring Data | Pagination support | Used in repository and controller layer for page requests |
| EmailService | Existing | Send invite emails | Reuse for admin invite flow |
| PasswordResetToken/Repository | Existing | Token for set-password link | Reuse for invite flow password setup |
| Toast DTO | Existing (shared.dto) | Flash notifications | Reuse for enable/disable undo toast |
| Bootstrap Icons (CDN or WebJar) | Latest | Edit/toggle icons in admin table | Optional -- can use Unicode or text buttons instead |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JpaSpecificationExecutor | JPQL @Query with LIKE | Specifications are more composable for multi-field dynamic search; @Query is simpler but rigid |
| Server-side inline editing | JavaScript SPA approach (fetch API) | Server-side Thymeleaf fragments avoid JS complexity but need full page state management |
| Reusing PasswordResetToken for invite | New InviteToken entity/table | Reuse avoids new schema migration and duplicated code |
| Bootstrap form-switch for toggle | Custom checkbox styling | Bootstrap form-switch is standard, well-known, works out of the box |

**No new Maven dependencies required.** All libraries are already in pom.xml or JDK standard.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/example/usermanagement/
  user/
    UserService.java              # Extended: add profile update, admin CRUD, pagination
    UserAuthDto.java              # Unchanged
    package-info.java             # allowedDependencies = { "shared" }
    internal/
      AppUser.java                # Unchanged
      AppRole.java                # Unchanged
      UserRepository.java         # Extended: add JpaSpecificationExecutor, pagination queries
      RoleRepository.java         # Unchanged
      UserSpecifications.java     # NEW: Specification builders for search/filter
      ProfileController.java      # NEW: REST API for profile (GET/PUT /api/v1/users/me)
      ProfileWebController.java   # NEW: Thymeleaf for profile (/profile)
      AdminController.java        # NEW: REST API for admin (CRUD /api/v1/admin/users)
      AdminWebController.java     # NEW: Thymeleaf for admin (/admin/users, /admin/users/new)
      AdminInviteService.java     # NEW: Coordinates user creation + invite email
  shared/
    dto/
      UserDto.java                # Extended: add createdAt field
    email/
      EmailService.java           # Extended: add sendInviteEmail() method

src/main/resources/templates/
  profile/
    profile.html                  # NEW: User profile page (card layout)
  admin/
    users.html                    # NEW: Admin user list (table + pagination + search)
    user-new.html                 # NEW: Admin create user form
  error/
    403.html                      # NEW: Styled "Access Denied" page
  email/
    invite.html                   # NEW: Invite email HTML template
    invite.txt                    # NEW: Invite email text template
  layout/
    default.html                  # Modified: add "My Profile" and conditional "Admin" links
```

### Pattern 1: Dual Controller Pattern (Established)
**What:** Every feature has two controllers -- a `@RestController` for API and a `@Controller` for Thymeleaf. Both delegate to the same service layer.
**When to use:** All new endpoints in this phase.
**Example:** (Already established in auth module)
```java
// REST API controller
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {
    private final UserService userService;
    // ... endpoints return ResponseEntity with JSON
}

// Web controller
@Controller
public class AdminWebController {
    private final UserService userService;
    // ... endpoints return view names with Model
}
```

### Pattern 2: Spring Data Pageable for Pagination
**What:** Use `Pageable` parameter in controller methods and `Page<T>` return types from repository. Spring automatically resolves page/size/sort from request parameters.
**When to use:** Admin user list endpoint.
**Example:**
```java
// Controller
@GetMapping("/admin/users")
public String listUsers(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        Model model) {
    Page<UserDto> page = userService.findUsers(search, role, status, pageable);
    model.addAttribute("users", page);
    // ... add search/filter params back to model for form state
    return "admin/users";
}

// Repository (extend JpaSpecificationExecutor)
public interface UserRepository extends JpaRepository<AppUser, Long>,
                                         JpaSpecificationExecutor<AppUser> { ... }

// Service
public Page<UserDto> findUsers(String search, String role, String status, Pageable pageable) {
    Specification<AppUser> spec = UserSpecifications.withFilters(search, role, status);
    return userRepository.findAll(spec, pageable).map(this::toUserDto);
}
```

### Pattern 3: JPA Specification for Dynamic Search/Filter
**What:** Use `Specification<AppUser>` to build dynamic WHERE clauses based on which filter parameters are provided.
**When to use:** Admin user list with optional search + role filter + status filter.
**Example:**
```java
public class UserSpecifications {

    public static Specification<AppUser> withFilters(String search, String role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like)
                ));
            }

            if (role != null && !role.isBlank()) {
                predicates.add(root.join("roles").get("name").in(role));
            }

            if (status != null && !status.isBlank()) {
                boolean enabled = "active".equalsIgnoreCase(status);
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

### Pattern 4: Inline Editing with JavaScript (Claude's Discretion)
**What:** Each row in the admin table has an edit icon. Clicking it replaces static text cells with input fields and shows Save/Cancel buttons. Save posts to the server via form submission or fetch API.
**When to use:** Admin user list inline editing.
**Recommendation:** Use `fetch()` API for inline save/cancel to avoid full page reload. The server returns JSON; JavaScript updates the DOM. This is cleaner than Thymeleaf fragment replacement for inline edits.
```javascript
function editRow(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    // Toggle between display and edit mode
    row.querySelectorAll('.display-mode').forEach(el => el.classList.add('d-none'));
    row.querySelectorAll('.edit-mode').forEach(el => el.classList.remove('d-none'));
}

function saveRow(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    const data = { /* collect input values */ };
    fetch(`/api/v1/admin/users/${userId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify(data)
    }).then(response => { /* update row display values, toggle modes */ });
}
```

### Pattern 5: Toggle Switch with Undo Toast (Claude's Discretion)
**What:** Enable/disable toggle uses Bootstrap's `form-switch`. On toggle, immediately POST to server, show toast with "Undo" button. Toast auto-dismisses after 8 seconds.
**Recommendation:** 8-second undo window, toast appears in existing toast container (already in layout). Undo reverses the toggle by POSTing the opposite value.
```html
<!-- In table row -->
<div class="form-check form-switch">
    <input class="form-check-input" type="checkbox" role="switch"
           th:checked="${user.enabled()}"
           onchange="toggleUserStatus(this, [[${user.id()}]])">
</div>
```

### Anti-Patterns to Avoid
- **Leaking AppUser entity outside user module:** All controllers must use `UserDto`, never `AppUser`. The `toUserDto()` conversion happens in `UserService` only.
- **Putting admin controllers in auth module:** Admin is user-management, not authentication. Keep it in `user.internal`.
- **Building custom pagination logic:** Use Spring Data `Page`/`Pageable` -- never manually calculate offsets.
- **N+1 queries on user roles:** `AppUser.roles` is `FetchType.EAGER`, so roles are loaded with each user. For the admin list, consider a `@Query` with explicit JOIN FETCH if pagination with specifications causes issues.
- **CSRF token omission in JavaScript fetch:** The web filter chain has CSRF enabled. All fetch requests from Thymeleaf pages must include the CSRF token.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pagination | Manual LIMIT/OFFSET queries | Spring Data `Pageable`/`Page` | Handles page calculation, sort parameters, total count automatically |
| Dynamic search queries | String concatenation SQL | `JpaSpecificationExecutor` + `Specification` | Composable, type-safe, prevents SQL injection |
| 403 error page routing | Custom interceptor/filter | Spring Boot `templates/error/403.html` convention | Auto-discovered by Spring Boot error handling |
| Invite token infrastructure | New InviteToken entity + table | Existing `PasswordResetToken` + `PasswordResetTokenRepository` | Already has create/validate/expire/mark-used logic |
| CSRF in JavaScript | Manual token extraction | Thymeleaf `<meta>` tag + `document.querySelector` | Standard Spring Security + Thymeleaf pattern |
| Page number rendering | Manual page arithmetic | Thymeleaf iteration over `page.totalPages` | `Page` object provides all metadata (totalPages, totalElements, number, hasNext, hasPrevious) |

**Key insight:** This phase is mostly wiring -- connecting existing infrastructure (security rules, email service, token flow, layout, toast notifications) with new controllers and templates. The only genuinely new technical element is the JPA Specification-based search/filter.

## Common Pitfalls

### Pitfall 1: CSRF Token Missing in JavaScript Fetch Calls
**What goes wrong:** Inline editing and toggle switch use JavaScript `fetch()` to POST/PUT to the server. The web filter chain has CSRF enabled. Forgetting the CSRF token causes 403 errors.
**Why it happens:** CSRF is automatically handled by Thymeleaf form submissions (`th:action`) but NOT by JavaScript fetch calls.
**How to avoid:** Add a `<meta>` tag with the CSRF token in the layout, then include it in all fetch headers:
```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```
```javascript
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
fetch(url, { headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' } });
```
**Warning signs:** 403 errors on fetch requests that work fine as form submissions.

### Pitfall 2: Spring Modulith Boundary Violations
**What goes wrong:** New controllers accidentally import types from wrong modules (e.g., admin controller in `shared` importing `UserService` from `user`).
**Why it happens:** IDE auto-import doesn't know about Modulith boundaries.
**How to avoid:** Place profile/admin controllers in `user.internal`. Run `ModularityTests.verifiesModularStructure()` after adding new classes.
**Warning signs:** `ModularityTests` fails with dependency violation messages.

### Pitfall 3: N+1 Queries on Paginated User List
**What goes wrong:** Loading a page of 10 users triggers 10 additional queries to load roles for each user.
**Why it happens:** Even though `roles` is `FetchType.EAGER`, JPA Specifications with pagination may not honor the eager fetch.
**How to avoid:** If N+1 occurs, add `@EntityGraph(attributePaths = "roles")` to the repository method or use a custom `@Query` with `JOIN FETCH`. Test with `spring.jpa.show-sql=true` in dev.
**Warning signs:** Slow admin list page, excessive SQL in dev logs.

### Pitfall 4: Invite Email Using Wrong Token Infrastructure
**What goes wrong:** Creating a new invite-specific token entity/table when the password reset flow already handles everything needed.
**Why it happens:** Not recognizing that "set initial password via email link" is functionally identical to "reset password via email link."
**How to avoid:** Reuse `PasswordResetToken` and the existing `/reset-password` page. Only create a new invite email template (different wording, same link target).
**Warning signs:** Unnecessary Flyway migrations, duplicated token handling code.

### Pitfall 5: Admin Self-Disable
**What goes wrong:** Admin disables their own account, locking themselves out.
**Why it happens:** The toggle switch on the admin list doesn't check if the target user is the current user.
**How to avoid:** The service layer should reject disable requests where `userId == currentUserId`. Return a 400 error with a clear message.
**Warning signs:** Admin accounts becoming inaccessible.

### Pitfall 6: UserDto Record Change Breaking Existing Code
**What goes wrong:** Adding `createdAt` to `UserDto` breaks the existing `from()` factory method signature and all callers.
**Why it happens:** Java records have positional constructors.
**How to avoid:** Update `UserDto.from()` to include the new `createdAt` parameter. Check all callers (only `UserService.toUserDto()` in current codebase). Update `toUserDto()` to pass `user.getCreatedAt()`.
**Warning signs:** Compilation errors in `UserService`.

### Pitfall 7: Pagination Parameters Not Preserved in Search
**What goes wrong:** User performs a search, then clicks page 2, and the search term is lost.
**Why it happens:** Page links don't include the current search/filter parameters.
**How to avoid:** Include search, role, and status parameters in all pagination links. In Thymeleaf:
```html
<a th:href="@{/admin/users(page=${i}, search=${search}, role=${role}, status=${status})}">
```
**Warning signs:** Search results reset when navigating between pages.

## Code Examples

### Profile Update Service Method
```java
// In UserService.java
@Transactional
public UserDto updateProfile(String email, String displayName, String firstName, String lastName) {
    AppUser user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", email));

    user.setUsername(displayName != null ? displayName : user.getUsername());
    user.setFirstName(firstName);
    user.setLastName(lastName);

    AppUser saved = userRepository.save(user);
    return toUserDto(saved);
}
```

### Profile REST Controller
```java
@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        UserDto user = userService.getUserByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User", authentication.getName()));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserDto updated = userService.updateProfile(
            authentication.getName(), request.displayName(),
            request.firstName(), request.lastName());
        return ResponseEntity.ok(updated);
    }
}
```

### Admin Create User (Invite Flow)
```java
// In AdminInviteService.java (user.internal)
@Service
public class AdminInviteService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Transactional
    public UserDto inviteUser(String email, String roleName) {
        if (userService.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email");
        }

        // Create user with placeholder password (can't log in until they set real one)
        String placeholder = "{bcrypt}" + UUID.randomUUID(); // non-matchable hash
        AppRole role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new BadRequestException("Invalid role: " + roleName));

        AppUser user = new AppUser(email, email, placeholder);
        user.setEmailVerified(true); // Verified by admin invite
        user.setEnabled(true);
        user.addRole(role);
        AppUser saved = userRepository.save(user);

        // Generate password-set token and send invite email
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(
            appProperties.verification().expirationHours());
        tokenRepository.save(new PasswordResetToken(tokenValue, saved, expiryDate));

        String resetUrl = appProperties.verification().baseUrl()
            + "/reset-password?token=" + tokenValue;
        emailService.sendInviteEmail(email, resetUrl);

        return userService.getUserByEmail(email).orElseThrow();
    }
}
```

### Admin Toggle Enable/Disable
```java
// In UserService.java
@Transactional
public UserDto toggleUserEnabled(Long userId, boolean enabled) {
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    user.setEnabled(enabled);
    AppUser saved = userRepository.save(user);
    return toUserDto(saved);
}

// In AdminController.java
@PatchMapping("/{id}/status")
public ResponseEntity<UserDto> toggleStatus(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body,
        Authentication authentication) {
    // Prevent self-disable
    UserDto target = userService.getUserById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    if (target.email().equals(authentication.getName())) {
        throw new BadRequestException("Cannot disable your own account");
    }
    UserDto updated = userService.toggleUserEnabled(id, body.get("enabled"));
    return ResponseEntity.ok(updated);
}
```

### Thymeleaf Pagination Component
```html
<!-- Pagination for admin user list -->
<nav th:if="${users.totalPages > 1}" aria-label="User list pagination">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <span class="text-muted"
              th:text="'Showing ' + ${users.numberOfElements} + ' of ' + ${users.totalElements} + ' users'">
        </span>
    </div>
    <ul class="pagination justify-content-center">
        <li class="page-item" th:classappend="${!users.hasPrevious()} ? 'disabled'">
            <a class="page-link" th:href="@{/admin/users(page=${users.number - 1},
               search=${search}, role=${role}, status=${status})}">Previous</a>
        </li>
        <li th:each="i : ${#numbers.sequence(0, users.totalPages - 1)}"
            class="page-item" th:classappend="${i == users.number} ? 'active'">
            <a class="page-link" th:href="@{/admin/users(page=${i},
               search=${search}, role=${role}, status=${status})}"
               th:text="${i + 1}">1</a>
        </li>
        <li class="page-item" th:classappend="${!users.hasNext()} ? 'disabled'">
            <a class="page-link" th:href="@{/admin/users(page=${users.number + 1},
               search=${search}, role=${role}, status=${status})}">Next</a>
        </li>
    </ul>
</nav>
```

### CSRF Meta Tags for JavaScript (Add to Layout)
```html
<!-- In layout/default.html <head> -->
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual SQL LIKE queries | JPA Specifications + Criteria API | Spring Data JPA 2.x | Composable, type-safe dynamic queries |
| Custom pagination helpers | Spring Data `Pageable`/`Page` | Spring Data 1.x+ (mature) | Built-in page calculation, sorting, total count |
| Separate admin layout | Reuse main layout with conditional elements | N/A (user decision) | Less template duplication, consistent nav experience |
| Full-page form for user edits | Inline table editing with JS fetch | Modern UX pattern | Faster workflow for admin bulk operations |

**Deprecated/outdated:**
- Spring Data `QueryByExampleExecutor`: Works for simple exact matches but cannot handle LIKE/OR queries needed for cross-field search. Use `JpaSpecificationExecutor` instead.
- `@QuerydslPredicate`: Requires additional QueryDSL dependency and annotation processor. JPA Specifications are simpler for this use case.

## Open Questions

1. **Profile page: "display name" vs "username" field**
   - What we know: Registration uses `displayName` which maps to `firstName` in `UserService.createUser()`. The `username` field is set to the email address. The profile page's "display name" likely means `firstName` (or a combination of firstName + lastName).
   - What's unclear: Should the "display name" editable field map to `firstName` only, or should we introduce a proper `displayName` field? Currently there is no dedicated display name column.
   - Recommendation: Map "display name" to `username` field (which currently stores email). The `username` column is `VARCHAR(100)` and unique. However, changing username from email to a display name could break `existsByUsername()` checks. **Safer approach:** Use `firstName` as the display name for now, since that is what registration already does (`displayName -> firstName`). The profile page edits `firstName`, `lastName` as separate fields, and the "display name" shown in the navbar would be `firstName` (or `firstName + " " + lastName` if both present). This aligns with the existing registration flow.

2. **Admin inline edit: Which fields are editable?**
   - What we know: CONTEXT.md says "inline table editing -- click edit icon on a row to make it editable in-place." The table columns are: email, name, role, status, created date.
   - What's unclear: Can admin change a user's email? Can admin change a user's role inline?
   - Recommendation: Allow editing of: first name, last name, role (dropdown). Email should be read-only (it's the login identifier). Status is handled by the toggle switch. Created date is always read-only.

3. **API endpoint structure for profile vs admin**
   - What we know: Profile is at `/api/v1/users/me`, admin is at `/api/v1/admin/users`.
   - What's unclear: `/api/v1/users/me` is under `/api/**` which requires authentication (any role). But it's NOT under `/api/v1/auth/**` (which is permitAll). Need to verify this works.
   - Recommendation: This works correctly. The API filter chain permits `/api/v1/auth/**` and requires auth for all other `/api/**`. The `/api/v1/users/me` path requires authentication (any authenticated user), which is correct for profile. The `/api/v1/admin/**` path requires ADMIN role, which is already configured.

## Sources

### Primary (HIGH confidence)
- **Codebase direct observation** -- all findings verified against actual source files:
  - `UserService.java`, `AppUser.java`, `UserRepository.java`, `UserDto.java` (user module structure)
  - `SecurityConfig.java` (admin route protection already in place)
  - `SecurityConfigTest.java` (existing tests verify admin 403/404 behavior)
  - `EmailService.java`, `EmailTemplateConfig.java` (email infrastructure)
  - `PasswordService.java`, `PasswordResetToken` (token reuse for invite flow)
  - `layout/default.html` (navbar structure, toast rendering)
  - `package-info.java` files (Spring Modulith dependency constraints)
  - Flyway migrations V1-V3 (database schema)
  - `pom.xml` (Spring Boot 4.0.1, Spring Modulith 2.0.1, Bootstrap 5.3.3)

### Secondary (MEDIUM confidence)
- **Spring Data JPA Specification pattern** -- standard pattern documented in Spring Data reference. Applied to this project's specific entity structure based on direct schema observation.
- **Spring Boot error page convention** (`templates/error/{status}.html`) -- well-established Spring Boot mechanism.
- **CSRF meta tag pattern for JavaScript fetch** -- standard Spring Security + Thymeleaf integration pattern.

### Tertiary (LOW confidence)
- None -- all findings are based on direct codebase observation or well-established Spring patterns.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all libraries already in use
- Architecture: HIGH -- follows established patterns from phases 2-5, verified against Modulith constraints
- Pitfalls: HIGH -- based on direct codebase observation and known Spring patterns
- Invite flow: HIGH -- reuses existing password reset infrastructure, verified by code reading
- Inline editing UX: MEDIUM -- JavaScript approach is recommended but exact implementation details are Claude's discretion

**Research date:** 2026-02-11
**Valid until:** 2026-03-11 (stable -- no external dependency changes expected)
