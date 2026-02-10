# Phase 5: Password Management - Research

**Researched:** 2026-02-10
**Domain:** Password change/reset flows with Spring Security, JWT invalidation, Thymeleaf pages
**Confidence:** HIGH

## Summary

This phase implements two distinct password flows: (1) **Change password** for authenticated users and (2) **Forgot/reset password** for unauthenticated users recovering account access via email. Both flows need Thymeleaf pages (PAGE-05, PAGE-06, PAGE-07) and REST API endpoints. The reset flow sends a password reset email with a single-use token, following the same multipart HTML+text pattern established in Phase 4.

The existing codebase provides nearly all required infrastructure. The database schema already includes a dedicated `password_reset_token` table (created in V1 migration). The email sending pipeline (`EmailService`, `EmailTemplateConfig`, `MimeMessageHelper` multipart pattern) is in place. The rate limiter (`ResendRateLimiter`) can be reused for reset request throttling. The sealed result pattern (`VerificationResult`) provides a proven template for `PasswordResetResult`. The primary new work is creating the PasswordResetToken entity, a PasswordService, new controllers and pages, password reset email templates, and implementing JWT/session invalidation on password change.

**Primary recommendation:** Create a separate `PasswordResetToken` entity mapping to the existing `password_reset_token` table (not reuse VerificationToken), add a `passwordChangedAt` timestamp to `AppUser` for JWT invalidation, and follow the Phase 4 sealed-result/service/controller pattern exactly.

## Critical Finding: Token Strategy Discrepancy

The CONTEXT.md says "Reuse existing VerificationToken entity with a token_type column to distinguish EMAIL_VERIFICATION from PASSWORD_RESET." However, the V1 migration already defines a **separate** `password_reset_token` table with an identical schema to `verification_token`. Creating a `token_type` column on `verification_token` would require a new Flyway migration and leave the pre-provisioned `password_reset_token` table unused.

**Recommendation:** Use the existing separate `password_reset_token` table with a new `PasswordResetToken` entity. This approach:
- Requires no schema migration (table already exists with correct structure)
- Avoids modifying existing `VerificationToken` entity
- Provides cleaner separation of concerns (different lifecycle, different cleanup policies)
- Aligns with what was planned during the initial schema design in Phase 1

**Confidence:** HIGH -- direct observation of existing database schema in both `h2/V1__init_schema.sql` and `postgresql/V1__init_schema.sql`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Security | 6.x (Boot 4 managed) | Password encoding, session management, SecurityContext | Already in use, provides BCrypt, form login, session control |
| Spring Data JPA | Boot 4 managed | PasswordResetToken repository | Already in use for VerificationToken, same pattern |
| Thymeleaf | 3.x (Boot managed) | Password pages + email templates | Already used for all existing pages and email templates |
| JavaMailSender | Boot 4 managed | Send password reset emails | Already configured and used for verification emails |
| java.util.UUID | JDK 21 | Token generation | Same approach as VerificationToken, uses SecureRandom internally |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MimeMessageHelper | Spring Mail | Multipart reset email | Same pattern as verification email |
| BCryptPasswordEncoder | Spring Security | Hash new passwords | Already configured in PasswordConfig |
| ConcurrentHashMap | JDK 21 | Rate limit reset requests | Reuse existing ResendRateLimiter component |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Separate PasswordResetToken entity | Reuse VerificationToken with token_type | Separate entity is cleaner; DB table already exists |
| passwordChangedAt timestamp for JWT invalidation | Token blacklist (in-memory or Redis) | Timestamp approach is simpler, no memory growth, no external dependency |
| In-memory rate limit | Bucket4j | Existing pattern sufficient for single-server deployment |

**No new dependencies required.** All libraries are already in pom.xml or JDK standard.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/example/usermanagement/
  auth/
    internal/
      password/                         # NEW package for password management
        PasswordResetToken.java          # JPA entity (maps to password_reset_token table)
        PasswordResetTokenRepository.java
        PasswordResetResult.java         # Sealed interface (like VerificationResult)
        PasswordService.java             # Token CRUD, change password, reset password
        PasswordController.java          # REST API endpoints (/api/v1/auth/*)
        PasswordWebController.java       # Thymeleaf page controllers
        ChangePasswordRequest.java       # API request record
        ResetPasswordRequest.java        # API request record
        ForgotPasswordRequest.java       # API request record
      AuthService.java                   # Possibly minor changes
      SecurityConfig.java               # Add permitAll for reset URLs
  user/
    internal/
      AppUser.java                      # Add passwordChangedAt field
  shared/
    email/
      EmailService.java                 # Add sendPasswordResetEmail method

src/main/resources/
  templates/
    email/
      password-reset.html               # NEW - HTML password reset email
      password-reset.txt                 # NEW - Plain text password reset email
    auth/
      change-password.html              # NEW - PAGE-05
      forgot-password.html              # NEW - PAGE-06
      reset-password.html               # NEW - PAGE-07
      reset-success.html                # NEW - Success page after reset
      reset-error.html                  # NEW - Error page (expired/invalid token)
    layout/
      default.html                      # MODIFY - Add navbar dropdown for authenticated users
```

### Pattern 1: Separate PasswordResetToken Entity
**What:** Dedicated JPA entity mapping to existing `password_reset_token` table
**When to use:** Always -- the table is already provisioned
**Example:**
```java
// Maps to existing password_reset_token table from V1 migration
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Same lifecycle patterns as VerificationToken:
    // isExpired(), isValid(), @PrePersist for createdAt
}
```

### Pattern 2: Sealed PasswordResetResult
**What:** Sealed interface with exhaustive outcome types for the reset token validation
**When to use:** Always -- proven in Phase 4 with VerificationResult
**Example:**
```java
// Follows VerificationResult pattern exactly
public sealed interface PasswordResetResult {
    record Success() implements PasswordResetResult {}
    record Expired() implements PasswordResetResult {}
    record Invalid() implements PasswordResetResult {}
    record AlreadyUsed() implements PasswordResetResult {}
}
```

### Pattern 3: JWT Invalidation via passwordChangedAt Timestamp
**What:** Add `passwordChangedAt` column to `app_user`, check JWT `iat` claim against it
**When to use:** After password change or reset to invalidate all existing JWT tokens
**Example:**
```java
// In AppUser entity - new field
@Column(name = "password_changed_at")
private LocalDateTime passwordChangedAt;

// In JwtAuthenticationFilter - enhanced validation
// After standard token validation, check if password was changed after token was issued
Date issuedAt = jwtService.extractClaim(jwt, Claims::getIssuedAt);
if (user.getPasswordChangedAt() != null &&
    issuedAt.before(toDate(user.getPasswordChangedAt()))) {
    // Token was issued before password change - reject
    filterChain.doFilter(request, response);
    return;
}
```
**Why this approach:**
- No token blacklist needed (no memory growth)
- Works across server restarts (persisted in DB)
- Invalidates ALL tokens at once (not just specific ones)
- Simple DB column addition via Flyway migration
- The JwtAuthenticationFilter already calls `userDetailsService.loadUserByUsername()` on every request, so the user data is available

**Tradeoff:** Adds one DB query per JWT-authenticated request to check the timestamp. However, this query already happens -- `JwtAuthenticationFilter` already loads `UserDetails` from the database on every request. The `passwordChangedAt` value just needs to be included in what's loaded.

### Pattern 4: Web Session Invalidation
**What:** Clear SecurityContext and invalidate HTTP session after password change
**When to use:** Change password flow (authenticated user, web chain)
**Example:**
```java
// In PasswordWebController after successful password change
@PostMapping("/change-password")
public String changePassword(..., HttpServletRequest request) {
    passwordService.changePassword(currentUser, newPassword);

    // Invalidate current session (forces re-login)
    SecurityContextHolder.clearContext();
    request.getSession(false).invalidate();

    // Redirect to login with success message
    redirectAttributes.addFlashAttribute("toast",
        new Toast("success", "Password changed successfully. Please log in with your new password."));
    return "redirect:/login";
}
```

### Pattern 5: SEC-01 Compliant Reset Request
**What:** Return identical response regardless of whether email exists
**When to use:** Forgot password endpoint (both API and web)
**Example:**
```java
// Same pattern as verification email resend
@PostMapping("/forgot-password")
public String requestPasswordReset(@RequestParam String email, Model model) {
    // Always show same message - SEC-01 compliance
    passwordService.requestPasswordReset(email);  // Silently does nothing if email not found
    model.addAttribute("message",
        "If an account exists with this email, we sent a reset link.");
    return "auth/forgot-password";
}
```

### Pattern 6: Navbar Dropdown for Authenticated Users
**What:** Bootstrap 5 dropdown in navbar for user actions (Change Password, Logout)
**When to use:** Layout template update -- CONTEXT.md says link lives in navbar dropdown
**Example:**
```html
<!-- Replaces the simple Logout button in layout/default.html -->
<li class="nav-item dropdown" sec:authorize="isAuthenticated()">
    <a class="nav-link dropdown-toggle" href="#" role="button"
       data-bs-toggle="dropdown" aria-expanded="false"
       sec:authentication="name">User</a>
    <ul class="dropdown-menu dropdown-menu-end">
        <li><a class="dropdown-item" th:href="@{/change-password}">Change Password</a></li>
        <li><hr class="dropdown-divider"></li>
        <li>
            <form th:action="@{/logout}" method="post">
                <button type="submit" class="dropdown-item">Logout</button>
            </form>
        </li>
    </ul>
</li>
```
**Note:** The existing layout has a simple "Logout" button. This needs to be converted to a dropdown. The `sec:authentication="name"` attribute displays the current user's email.

### Anti-Patterns to Avoid
- **Reusing VerificationToken for password reset:** The DB already has a separate table; adding token_type would be a migration that gains nothing
- **Storing old password hashes for comparison:** CONTEXT.md says no password history -- user can reuse previous password
- **Auto-login after password reset:** CONTEXT.md says show success page with login button
- **Different error messages for non-existent vs real email on reset request:** Violates SEC-01
- **Blocking the reset email send on rate limit without SEC-01 response:** Always return same message
- **Not invalidating JWT tokens after password change:** Leaves stale tokens valid until natural expiry

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Custom hash function | BCryptPasswordEncoder (PasswordConfig) | Already configured, timing-safe comparison |
| Token generation | Custom random string | UUID.randomUUID() | Proven pattern from Phase 4, uses SecureRandom |
| Email construction | String concatenation | MimeMessageHelper + Thymeleaf templates | Proven pattern from Phase 4, handles encoding |
| Rate limiting | Custom counter logic | Reuse ResendRateLimiter | Already tested, 60-second cooldown, ConcurrentHashMap |
| Session invalidation | Custom session tracking | HttpServletRequest.getSession().invalidate() + SecurityContextHolder.clearContext() | Spring Security standard approach |
| Password validation | Custom length check | Bean Validation @Size(min=8) | Framework-integrated, consistent with RegistrationForm pattern |

**Key insight:** Nearly all infrastructure for this phase was built in Phase 4. The password reset flow mirrors the email verification flow: generate token, send email, validate token, perform action, show result. Maximize reuse of established patterns.

## Common Pitfalls

### Pitfall 1: Not Invalidating JWT Tokens After Password Change
**What goes wrong:** Old JWT tokens remain valid after password change; attacker with stolen token retains access
**Why it happens:** JWTs are stateless -- the server cannot revoke them without additional mechanism
**How to avoid:** Add `passwordChangedAt` timestamp to `app_user`, compare against JWT `iat` claim in `JwtAuthenticationFilter`. Reject tokens issued before the password change.
**Warning signs:** User changes password but old API tokens still work

### Pitfall 2: Flyway Migration Ordering
**What goes wrong:** New migration (V2) for `passwordChangedAt` column conflicts with existing H2 seed data migration (V2__seed_dev_data.sql)
**Why it happens:** Both H2 and PostgreSQL need matching version numbers but may have different migration files
**How to avoid:** The `passwordChangedAt` column addition should be V2 for PostgreSQL (which only has V1). For H2, it needs to be V3 (V2 is seed data). BUT both directories must have consistent structural migrations. Check existing migration numbering carefully.
**Warning signs:** Flyway validation errors on startup, `SchemaComparisonTests` failure

### Pitfall 3: CSRF Token Missing on Password Change Form
**What goes wrong:** POST request to change password fails with 403 Forbidden
**Why it happens:** Thymeleaf form doesn't include CSRF token; web chain requires CSRF
**How to avoid:** Use `th:action` on forms (Thymeleaf auto-injects CSRF token). Verify all forms use `th:action="@{/path}"` not `action="/path"`.
**Warning signs:** 403 on form submission, "Invalid CSRF token" in logs

### Pitfall 4: Getting the Authenticated User in Change Password Flow
**What goes wrong:** Cannot identify which user is changing their password
**Why it happens:** SecurityContext contains UserDetails (Spring Security's User), not AppUser entity
**How to avoid:** Extract email from `Authentication.getName()` (which returns the username/email), then look up the user. The PasswordService needs to accept the email, not a User entity.
**Warning signs:** NullPointerException or ClassCastException when trying to get user

### Pitfall 5: Race Condition on Password Reset Token Usage
**What goes wrong:** Double-click on reset link processes twice, second attempt gets confusing error
**Why it happens:** Non-atomic check-then-update on `used` flag
**How to avoid:** Same pattern as Phase 4 -- atomic `markAsUsed` query:
```java
@Modifying
@Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token AND t.used = false")
int markAsUsed(@Param("token") String token);
```
**Warning signs:** Race condition errors in logs

### Pitfall 6: Forgot Password Link on Login Page CSRF
**What goes wrong:** The "Forgot your password?" link is a GET, not a form submission, so no CSRF concern. But if the forgot-password page has a form, that form needs CSRF.
**Why it happens:** Confusion about which pages need CSRF
**How to avoid:** "Forgot your password?" is an `<a>` tag (GET request), the form on the forgot-password page is a POST (Thymeleaf auto-adds CSRF).
**Warning signs:** CSRF error when submitting email on forgot-password page

### Pitfall 7: SecurityConfig Missing permitAll for Reset URLs
**What goes wrong:** Password reset link from email returns 401/redirect-to-login
**Why it happens:** Reset URLs are not in SecurityConfig's permitAll list
**How to avoid:** Add `/forgot-password`, `/reset-password/**` to SecurityConfig web chain `permitAll()` (and `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password` are already covered by `/api/v1/auth/**` permitAll).
**Warning signs:** Users clicking reset link get redirected to login

## Code Examples

### Flyway Migration: Add passwordChangedAt Column
```sql
-- V2__add_password_changed_at.sql (PostgreSQL)
-- V3__add_password_changed_at.sql (H2, since V2 is seed data)
ALTER TABLE app_user ADD COLUMN password_changed_at TIMESTAMP;
```
Note: Column is nullable (existing users have no password change history). No default value needed.

### PasswordResetTokenRepository
```java
// Follows VerificationTokenRepository pattern exactly
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token AND t.used = false")
    int markAsUsed(@Param("token") String token);

    void deleteByUser(AppUser user);
}
```

### PasswordService Core Methods
```java
@Service
@Transactional(readOnly = true)
public class PasswordService {

    // Change password for authenticated user
    @Transactional
    public void changePassword(String email, String newPassword) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Request password reset (SEC-01 compliant - silent on missing email)
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Only send if email is verified (don't help unverified accounts)
            if (user.isEmailVerified()) {
                PasswordResetToken token = createResetToken(user);
                String resetUrl = buildResetUrl(token);
                emailService.sendPasswordResetEmail(email, resetUrl);
            }
        });
    }

    // Validate and consume reset token, set new password
    @Transactional
    public PasswordResetResult resetPassword(String tokenValue, String newPassword) {
        // Same validation pattern as VerificationService.verifyToken()
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenValue);
        if (tokenOpt.isEmpty()) return new PasswordResetResult.Invalid();

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) return new PasswordResetResult.Expired();

        int updated = tokenRepository.markAsUsed(tokenValue);
        if (updated == 0) return new PasswordResetResult.AlreadyUsed();

        // Set new password and update timestamp
        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Delete all reset tokens for this user (cleanup)
        tokenRepository.deleteByUser(user);

        return new PasswordResetResult.Success();
    }
}
```

### EmailService Extension
```java
// Add to existing EmailService
public void sendPasswordResetEmail(String to, String resetLink) {
    Context ctx = new Context();
    ctx.setVariable("resetLink", resetLink);

    String htmlContent = templateEngine.process("email/password-reset", ctx);
    String textContent = templateEngine.process("email/password-reset.txt", ctx);

    sendMultipartEmail(to, "Reset your password", textContent, htmlContent);
    log.info("Password reset email sent to {}", to);
}
```

### Password Reset Email Templates
```html
<!-- email/password-reset.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Reset your password</title></head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <p>You have requested a password reset for your account.</p>
    <p>Click the link below to set a new password:</p>
    <p>
        <a th:href="${resetLink}"
           style="display: inline-block; padding: 10px 20px; background-color: #0d6efd; color: white; text-decoration: none; border-radius: 4px;">
            Reset Password
        </a>
    </p>
    <p>Or copy and paste this URL into your browser:</p>
    <p th:text="${resetLink}"></p>
    <p>This link expires in 24 hours.</p>
    <p>If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>
</body>
</html>
```

```text
<!-- email/password-reset.txt (TEXT mode, Thymeleaf TEXT syntax) -->
You have requested a password reset for your account.

Please visit the following link to set a new password:

[(${resetLink})]

This link expires in 24 hours.

If you did not request a password reset, please ignore this email. Your password will remain unchanged.
```

### JWT Invalidation in JwtAuthenticationFilter
```java
// Enhanced validation in JwtAuthenticationFilter.doFilterInternal()
if (jwtService.isTokenValid(jwt, userDetails)) {
    // Check if password was changed after this token was issued
    Date issuedAt = jwtService.extractClaim(jwt, Claims::getIssuedAt);

    // Need passwordChangedAt from UserDetails or a separate lookup
    // Option: Add to UserAuthDto and expose via UserService
    // Option: Load from UserService directly
    // The simpler approach: extend the isTokenValid check in JwtService

    var authToken = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
}
```

**Implementation note on passwordChangedAt access:** The `JwtAuthenticationFilter` already calls `userDetailsService.loadUserByUsername()` which goes through `CustomUserDetailsService` -> `UserService.getUserAuthDetailsByEmail()`. The `UserAuthDto` needs to include the `passwordChangedAt` field, and `JwtService.isTokenValid()` or the filter itself needs to compare it. The cleanest approach:
1. Add `passwordChangedAt` to `UserAuthDto`
2. Create a custom `UserDetails` implementation (or use the `User.builder()` approach with a custom wrapper) that carries the timestamp
3. OR: Add a separate method to `UserService` to check password change timestamp, called from the filter

The simplest v1 approach: add `LocalDateTime passwordChangedAt` to `UserAuthDto`, and in `JwtService.isTokenValid()` add an overload that accepts the timestamp.

### SecurityConfig Updates
```java
// In webFilterChain - add these to permitAll
.requestMatchers("/forgot-password").permitAll()
.requestMatchers("/reset-password/**").permitAll()
// Note: /change-password requires authentication (already covered by anyRequest().authenticated())

// /api/v1/auth/** is already permitAll - covers forgot-password and reset-password API endpoints
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Token blacklist for JWT invalidation | Timestamp-based invalidation (passwordChangedAt) | Current best practice | Simpler, no memory growth, survives restart |
| Single verification_token table with type column | Separate tables per token type | Schema design | Cleaner separation, no migration needed |
| Session-only auth | Dual JWT (API) + Session (Web) invalidation | Phase 2 design | Both chains need invalidation on password change |

**Deprecated/outdated:**
- In-memory JWT blacklists without TTL: Can grow unbounded; prefer timestamp approach
- `SecurityContextLogoutHandler` for programmatic logout: Modern approach is `SecurityContextHolder.clearContext()` + `session.invalidate()`

## Open Questions

1. **passwordChangedAt propagation to JwtAuthenticationFilter**
   - What we know: The filter already loads UserDetails, and we need the timestamp available there
   - What's unclear: Whether to extend UserAuthDto + CustomUserDetailsService, or create a separate lookup
   - Recommendation: Extend UserAuthDto to include passwordChangedAt, pass it through CustomUserDetailsService. Add a `passwordChangedAt` check in JwtService or the filter. The planner should decide the exact wiring.

2. **Rate limiter reuse vs new instance**
   - What we know: `ResendRateLimiter` is a `@Component` in `verification` package; password reset needs the same cooldown
   - What's unclear: Whether to rename/move ResendRateLimiter to be more generic, or create a second instance
   - Recommendation: Rename to `EmailRateLimiter` and move to `shared.email` package (or keep existing and add a separate bean). The simplest approach is to inject the existing `ResendRateLimiter` and reuse it -- it's already keyed by email, and the 60-second cooldown applies per email regardless of the type of email. A user should not be able to request both a verification resend and a password reset within 60 seconds for the same email.

3. **Flyway migration versioning for H2 vs PostgreSQL**
   - What we know: PostgreSQL has V1 only. H2 has V1 + V2 (seed data). The `passwordChangedAt` column needs to be added to both.
   - What's unclear: Best version numbering strategy to keep structural parity
   - Recommendation: For PostgreSQL: V2__add_password_changed_at.sql. For H2: V3__add_password_changed_at.sql. The `SchemaComparisonTests` already handles this by filtering seed migrations from structural parity checks.

4. **Change password page: no current password required?**
   - What we know: CONTEXT.md explicitly states "Form has 2 fields: new password + confirm new password (no current password required -- user is already authenticated)"
   - What's unclear: This is unusual for a change password flow (most require current password for defense against session hijacking). But REQUIREMENTS.md says "PASS-01: Authenticated user can change password (requires current password)"
   - Recommendation: **Follow REQUIREMENTS.md** which says "requires current password". The CONTEXT.md simplified this but the requirement is explicit. The change password form should have 3 fields: current password, new password, confirm new password. The API endpoint should also require the current password. Flag this for the planner to confirm with the user.

## Sources

### Primary (HIGH confidence)
- Existing codebase: `VerificationToken.java`, `VerificationService.java`, `VerificationResult.java`, `ResendRateLimiter.java` -- Phase 4 established patterns
- Existing schema: `h2/V1__init_schema.sql` and `postgresql/V1__init_schema.sql` -- `password_reset_token` table already provisioned
- `SecurityConfig.java` -- current security chain configuration
- `JwtAuthenticationFilter.java` -- current JWT validation flow
- `EmailService.java` -- current email sending pattern

### Secondary (MEDIUM confidence)
- [Spring Security Session Management Docs](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html) -- SessionRegistry, session invalidation
- [StackAbuse: Spring Security Forgot Password](https://stackabuse.com/spring-security-forgot-password-functionality/) -- Token-based reset flow patterns
- [StackAbuse: JWT In-Memory Invalidation](https://stackabuse.com/spring-security-in-memory-invalidation-of-jwt-token-during-user-logout/) -- JWT blacklist approach (used for comparison)

### Tertiary (LOW confidence)
- [Medium: 5 Approaches to JWT Invalidation](https://medium.com/@mmichaelb/5-different-approaches-to-invalidate-json-web-tokens-e4cc4e027343) -- Timestamp-based invalidation concept
- [Sophea Mak: JWT Token Invalidation](https://sopheamak.medium.com/springboot-how-to-invalidate-jwt-token-such-as-logout-or-reset-all-active-tokens-73f55289d47b) -- passwordChangedAt column approach
- Various WebSearch results on Spring Boot password reset patterns (2025-2026)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- No new dependencies, all patterns established in Phases 2-4
- Architecture: HIGH -- Follows existing codebase patterns exactly (sealed results, service layer, dual controllers)
- Token strategy: HIGH -- Separate table already exists in schema, no migration needed for token
- JWT invalidation: MEDIUM -- passwordChangedAt approach is well-known but wiring through the existing filter needs implementation-time validation
- Pitfalls: HIGH -- Well-documented in official sources and directly observed in codebase

**Research date:** 2026-02-10
**Valid until:** 2026-03-10 (30 days -- stable domain, no rapid changes expected)
