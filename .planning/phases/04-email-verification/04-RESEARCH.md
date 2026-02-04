# Phase 4: Email Verification - Research

**Researched:** 2026-02-04
**Domain:** Email verification flow with Spring Boot Mail, token management, SEC-01 compliance
**Confidence:** HIGH

## Summary

This phase implements email-based account verification for new user registrations. The flow involves: (1) generating a secure verification token at registration, (2) sending an HTML email with verification link via SMTP, (3) handling link clicks to activate accounts, and (4) providing a resend mechanism with rate limiting.

The existing codebase already has `spring-boot-starter-mail` in pom.xml, the `verification_token` table in the database schema, and `emailVerified` field on `AppUser`. The infrastructure is ready - this phase adds the business logic, email service, templates, and endpoints.

**Primary recommendation:** Use Spring's `JavaMailSender` with Thymeleaf email templates for HTML+text multipart emails, `UUID.randomUUID()` for token generation (cryptographically sufficient for this use case), and simple in-memory rate limiting via `ConcurrentHashMap` for the resend cooldown.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-boot-starter-mail | 4.0.1 (Boot managed) | JavaMailSender auto-configuration | Already in pom.xml, standard Spring approach |
| Thymeleaf | 3.x (Boot managed) | Email template rendering | Already used for web pages, reuse for emails |
| java.util.UUID | JDK 21 | Token generation | Uses SecureRandom internally, sufficient for verification tokens |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MimeMessageHelper | Spring Mail | Multipart HTML+text emails | Always - simplifies MIME construction |
| ConcurrentHashMap | JDK 21 | In-memory rate limit tracking | Resend cooldown (simple, no external deps) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| UUID tokens | JWT tokens | JWT is stateless (no DB lookup) but longer URLs, more complexity |
| In-memory rate limit | Bucket4j | Bucket4j is more sophisticated but adds dependency for simple 60s cooldown |
| Thymeleaf templates | Plain string concatenation | Templates are cleaner, maintainable, support HTML+text |

**No new dependencies required.** All libraries are already in pom.xml or JDK standard.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/example/usermanagement/
├── auth/
│   └── internal/
│       ├── verification/          # New package for verification
│       │   ├── VerificationToken.java       # JPA entity
│       │   ├── VerificationTokenRepository.java
│       │   ├── VerificationService.java     # Token CRUD, validation
│       │   └── EmailVerificationController.java  # Web endpoints
│       └── AuthService.java       # Modify to call VerificationService
├── shared/
│   └── email/                     # New package for email
│       ├── EmailService.java      # Send emails via JavaMailSender
│       └── EmailTemplateConfig.java  # Thymeleaf engine for emails
└── ...

src/main/resources/
├── templates/
│   ├── email/                     # New folder for email templates
│   │   ├── verification.html      # HTML email template
│   │   └── verification.txt       # Plain text fallback
│   └── auth/
│       ├── verify-success.html    # Verification success page (PAGE-04)
│       ├── verify-error.html      # Error page (expired, invalid, etc.)
│       └── resend-verification.html  # Resend form page
```

### Pattern 1: Token Entity with Lifecycle
**What:** VerificationToken entity tracks token state (created, used, expired)
**When to use:** Always - need to invalidate tokens and prevent reuse
**Example:**
```java
// Source: Existing schema + standard pattern
@Entity
@Table(name = "verification_token")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
```

### Pattern 2: Token Generation with UUID
**What:** Use `UUID.randomUUID()` for secure, unique token strings
**When to use:** Always for verification tokens
**Example:**
```java
// Source: Java standard library - UUID uses SecureRandom
public String generateToken() {
    return UUID.randomUUID().toString();
}
```
**Why UUID is sufficient:** Java's `UUID.randomUUID()` uses `SecureRandom` internally, generating 122 bits of cryptographically secure random data. For email verification (not cryptographic secrets), this provides adequate security against brute-force attacks.

### Pattern 3: Multipart Email with Thymeleaf
**What:** Send HTML email with plain text fallback using MimeMessageHelper
**When to use:** Always - ensures email clients without HTML support can read the message
**Example:**
```java
// Source: Spring Framework MimeMessageHelper + Thymeleaf docs
public void sendVerificationEmail(String to, String verificationLink) {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setFrom(fromAddress);
    helper.setTo(to);
    helper.setSubject("Verify your email");

    // Process both templates
    Context ctx = new Context();
    ctx.setVariable("verificationLink", verificationLink);

    String htmlContent = templateEngine.process("email/verification.html", ctx);
    String textContent = templateEngine.process("email/verification.txt", ctx);

    // setText(text, html) - order matters for multipart/alternative
    helper.setText(textContent, htmlContent);

    mailSender.send(message);
}
```

### Pattern 4: SEC-01 Compliant Resend Response
**What:** Return identical response regardless of whether email exists
**When to use:** Resend verification endpoint to prevent user enumeration
**Example:**
```java
// Source: OWASP user enumeration prevention guidelines
@PostMapping("/resend-verification")
public String resendVerification(@RequestParam String email, Model model) {
    // Always show same message - SEC-01 compliance
    verificationService.resendVerificationEmail(email);
    model.addAttribute("message", "If an account exists with this email, " +
        "a verification link has been sent.");
    return "auth/resend-verification";
}
```

### Pattern 5: Simple In-Memory Rate Limiting
**What:** ConcurrentHashMap tracks last resend time per email
**When to use:** Resend endpoint to enforce cooldown
**Example:**
```java
// Source: Standard Java concurrent pattern
private final ConcurrentHashMap<String, Instant> lastResendTime = new ConcurrentHashMap<>();
private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

public boolean canResend(String email) {
    Instant lastTime = lastResendTime.get(email);
    if (lastTime == null) return true;
    return Instant.now().isAfter(lastTime.plus(RESEND_COOLDOWN));
}

public void recordResend(String email) {
    lastResendTime.put(email, Instant.now());
}
```

### Pattern 6: Block Unverified Login in UserDetailsService
**What:** Check `emailVerified` in CustomUserDetailsService, return generic error
**When to use:** Login flow - unverified users cannot authenticate
**Example:**
```java
// Source: Existing CustomUserDetailsService + SEC-01 requirement
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAuthDto authDto = userService.getUserAuthDetailsByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

    // SEC-01: Same error for non-existent AND unverified accounts
    if (!authDto.emailVerified()) {
        throw new UsernameNotFoundException("Bad credentials");
    }

    // ... build UserDetails as before
}
```

### Anti-Patterns to Avoid
- **Revealing email existence on resend:** "Email not found" vs "Email sent" reveals account state
- **Storing plaintext tokens:** Always use unique tokens, consider hashing if storing long-term
- **Infinite token validity:** Always set expiration (24 hours per CONTEXT.md)
- **Synchronous email sending in request thread:** Consider async, but acceptable for MVP
- **Auto-login after verification:** CONTEXT.md explicitly says "no auto-login" - show success page with login link

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| MIME email construction | Manual multipart building | MimeMessageHelper | Handles encoding, content types, boundaries |
| Secure random tokens | Custom random string | UUID.randomUUID() | Uses SecureRandom, collision-resistant |
| Email template rendering | String concatenation | Thymeleaf | Maintainable, escapes HTML, supports i18n |
| SMTP configuration | Manual JavaMail Session | spring.mail.* properties | Auto-configured, externalized config |

**Key insight:** Email handling has many edge cases (encoding, MIME types, SMTP timeouts). Spring Mail abstracts these; don't fight the framework.

## Common Pitfalls

### Pitfall 1: SMTP Timeout Blocking Requests
**What goes wrong:** Default SMTP timeouts are infinite; unresponsive mail server blocks request thread
**Why it happens:** Spring Boot doesn't set mail timeouts by default
**How to avoid:** Always configure timeouts in application.yml
```yaml
spring:
  mail:
    properties:
      "[mail.smtp.connectiontimeout]": 5000
      "[mail.smtp.timeout]": 3000
      "[mail.smtp.writetimeout]": 5000
```
**Warning signs:** Requests hang indefinitely when mail server is slow/unreachable

### Pitfall 2: Token Already Used Race Condition
**What goes wrong:** User clicks verification link twice quickly, second request processes before first commits
**Why it happens:** Non-atomic check-then-update on `used` flag
**How to avoid:** Use optimistic locking or atomic update query
```java
@Modifying
@Query("UPDATE VerificationToken t SET t.used = true WHERE t.token = :token AND t.used = false")
int markAsUsed(@Param("token") String token);
// Returns 1 if updated, 0 if already used
```
**Warning signs:** Occasional errors about already-verified accounts

### Pitfall 3: Email Enumeration via Timing
**What goes wrong:** Resend for existing email takes longer (DB lookup, email send) than non-existing
**Why it happens:** Different code paths have different execution times
**How to avoid:** Perform similar work for both paths, or introduce artificial delay
**Warning signs:** Security audit finds timing-based enumeration

### Pitfall 4: Token in URL Logged/Cached
**What goes wrong:** Verification token appears in server logs, browser history, or proxy caches
**Why it happens:** Token is in URL query parameter
**How to avoid:**
- Use path parameter: `/verify/{token}` instead of `/verify?token=xxx`
- Set appropriate cache headers: `Cache-Control: no-store`
- Single-use tokens mitigate exposure risk
**Warning signs:** Token visible in access logs

### Pitfall 5: Registration Creates Enabled User
**What goes wrong:** User is `enabled=true` at registration, can log in before verification
**Why it happens:** Current code sets `user.setEnabled(true)` in UserService.createUser
**How to avoid:** Either:
- Set `enabled=false` at registration, set `true` on verification (recommended)
- OR check `emailVerified` in login flow (current approach in CustomUserDetailsService)
**Warning signs:** Unverified users can access protected resources

### Pitfall 6: Multiple Valid Tokens Per User
**What goes wrong:** User requests resend multiple times, all tokens remain valid
**Why it happens:** New token created without invalidating old ones
**How to avoid:** Per CONTEXT.md, "requesting new token invalidates old one"
```java
// Before creating new token:
tokenRepository.deleteByUser(user);  // Or mark all as used
```
**Warning signs:** Old verification links still work after resend

## Code Examples

Verified patterns from official sources:

### Mail Configuration (application.yml)
```yaml
# Source: Spring Boot documentation
spring:
  mail:
    host: ${MAIL_HOST:smtp.example.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      "[mail.smtp.auth]": true
      "[mail.smtp.starttls.enable]": true
      "[mail.smtp.connectiontimeout]": 5000
      "[mail.smtp.timeout]": 3000
      "[mail.smtp.writetimeout]": 5000

app:
  mail:
    from: noreply@jbeltsolution.com  # Per CONTEXT.md
  verification:
    expiration-hours: 24  # Per CONTEXT.md
    base-url: ${APP_BASE_URL:http://localhost:8080}
```

### Email Template (verification.html)
```html
<!-- Source: Thymeleaf email template pattern -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <p>Please verify your email address by clicking the link below:</p>
    <p><a th:href="${verificationLink}">Verify Email</a></p>
    <p>This link expires in 24 hours.</p>
    <p>If you did not create an account, please ignore this email.</p>
</body>
</html>
```

### Email Template (verification.txt)
```text
Please verify your email address by visiting the following link:

[(${verificationLink})]

This link expires in 24 hours.

If you did not create an account, please ignore this email.
```

### Verification Controller Endpoint
```java
// Source: Standard Spring MVC pattern
@GetMapping("/verify/{token}")
public String verifyEmail(@PathVariable String token, Model model) {
    VerificationResult result = verificationService.verifyToken(token);

    return switch (result.status()) {
        case SUCCESS -> {
            model.addAttribute("message", "Your email has been verified. You can now log in.");
            yield "auth/verify-success";
        }
        case ALREADY_VERIFIED -> {
            model.addAttribute("message", "This email has already been verified.");
            yield "auth/verify-success";  // Still success page, just different message
        }
        case EXPIRED -> {
            model.addAttribute("error", "This verification link has expired. Please request a new one.");
            model.addAttribute("showResendLink", true);
            yield "auth/verify-error";
        }
        case INVALID -> {
            model.addAttribute("error", "Invalid verification link.");
            yield "auth/verify-error";
        }
    };
}
```

### Verification Service Result Pattern
```java
// Source: Standard sealed type pattern for explicit states
public sealed interface VerificationResult {
    enum Status { SUCCESS, ALREADY_VERIFIED, EXPIRED, INVALID }
    Status status();

    record Success(Status status) implements VerificationResult {
        public Success() { this(Status.SUCCESS); }
    }
    record AlreadyVerified(Status status) implements VerificationResult {
        public AlreadyVerified() { this(Status.ALREADY_VERIFIED); }
    }
    record Expired(Status status) implements VerificationResult {
        public Expired() { this(Status.EXPIRED); }
    }
    record Invalid(Status status) implements VerificationResult {
        public Invalid() { this(Status.INVALID); }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate TemplateEngine for email | Share SpringTemplateEngine, separate resolvers | Thymeleaf 3.x | Simpler config |
| JavaMail Session manual config | spring.mail.* auto-configuration | Spring Boot 1.x+ | Externalized config |
| String tokens in cookies | Path-based tokens (/verify/{token}) | Security best practice | Cleaner URLs, no cookie issues |

**Deprecated/outdated:**
- `MailSender` interface: Use `JavaMailSender` for MIME support
- Manual `MimeMessage` construction: Use `MimeMessageHelper`

## Open Questions

Things that couldn't be fully resolved:

1. **Async email sending**
   - What we know: Synchronous sending blocks request; async can use @Async + @EnableAsync
   - What's unclear: Whether to implement in Phase 4 or defer
   - Recommendation: Keep synchronous for simplicity in Phase 4; email sending is typically fast (<1s). Add async in future phase if needed.

2. **Email delivery monitoring**
   - What we know: SMTP send() success doesn't mean delivery; emails can bounce or go to spam
   - What's unclear: Whether to track delivery status
   - Recommendation: Out of scope for Phase 4. Rely on resend mechanism. Consider bounce handling in future.

3. **Cleanup of expired tokens**
   - What we know: Old tokens accumulate in database
   - What's unclear: When/how to clean up
   - Recommendation: Add scheduled cleanup task, but defer to future phase. Expired tokens don't affect functionality.

## Sources

### Primary (HIGH confidence)
- [Spring Boot Email Documentation](https://docs.spring.io/spring-boot/reference/io/email.html) - Mail auto-configuration, properties, timeouts
- [Thymeleaf Email Article](https://www.thymeleaf.org/doc/articles/springmail.html) - Template resolver config, multipart emails
- Java 21 UUID javadoc - SecureRandom usage confirmation

### Secondary (MEDIUM confidence)
- [Baeldung Spring Events](https://www.baeldung.com/spring-events) - @TransactionalEventListener, @Async patterns
- [Baeldung User Enumeration Prevention](https://www.baeldung.com/spring-security-enumeration-attacks) - SEC-01 compliance patterns
- [Bucket4j Rate Limiting](https://www.baeldung.com/spring-bucket4j) - In-memory rate limiting approach

### Tertiary (LOW confidence)
- Various Medium/dev.to articles on email verification - Patterns cross-validated with official docs

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Uses existing dependencies, official Spring patterns
- Architecture: HIGH - Follows existing codebase structure (auth/internal, shared packages)
- Pitfalls: HIGH - Well-documented in official sources and security literature

**Research date:** 2026-02-04
**Valid until:** 2026-03-04 (30 days - stable domain, no rapid changes expected)
