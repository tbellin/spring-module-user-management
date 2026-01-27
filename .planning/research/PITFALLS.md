# Pitfalls Research

**Domain:** Spring Boot 4 / Spring Modulith User Management Server
**Researched:** 2026-01-28
**Confidence:** MEDIUM (Spring Boot 4 / Spring Security 7 are new; verified against official docs where possible, training-data-only claims flagged)

---

## Critical Pitfalls

Mistakes that cause rewrites, security vulnerabilities, or major architectural issues.

### Pitfall 1: Spring Security 7 -- Implicit SecurityContext Saving Removed

**What goes wrong:**
Authentication succeeds but the user is not recognized on subsequent requests. Login appears to work, then the very next page load shows the user as anonymous. This is the single most confusing migration issue from Spring Security 6.x to 7.x.

**Why it happens:**
Spring Security 6+ replaced `SecurityContextPersistenceFilter` with `SecurityContextHolderFilter`, which does NOT automatically save the `SecurityContext` back to the repository after a request. In Spring Security 7, the old automatic-save behavior is fully removed. Developers who follow older tutorials or copy Spring Security 5.x patterns will silently lose authentication state.

**How to avoid:**
After authenticating a user programmatically (e.g., custom login endpoint, self-registration auto-login), you MUST explicitly save the context:

```java
SecurityContext context = securityContextHolderStrategy.createEmptyContext();
context.setAuthentication(authentication);
securityContextHolderStrategy.setContext(context);
securityContextRepository.saveContext(context, request, response);  // REQUIRED
```

For JWT/stateless endpoints, this is irrelevant (you use `SessionCreationPolicy.STATELESS`). But for Thymeleaf server-rendered pages that use session-based auth (login form, post-registration redirect), this is critical.

**Warning signs:**
- Login POST returns 200/302 but next GET shows user as unauthenticated
- `SecurityContextHolder.getContext().getAuthentication()` returns `AnonymousAuthenticationToken` on subsequent requests
- Tests pass with `@WithMockUser` but fail with actual login flow

**Phase to address:**
Phase 1 (Security Foundation) -- Set this pattern correctly from the very first authentication implementation.

**Confidence:** HIGH -- Verified in official Spring Security session management documentation.

---

### Pitfall 2: CSRF Misconfiguration in Thymeleaf + REST API Hybrid

**What goes wrong:**
Either (a) REST API endpoints reject legitimate requests with 403 Forbidden because CSRF tokens are missing, or (b) CSRF is globally disabled for convenience, leaving Thymeleaf form endpoints vulnerable to cross-site request forgery.

**Why it happens:**
The project is a hybrid: Thymeleaf server-rendered pages (login form, registration, email verification) AND REST API endpoints (JWT-authenticated). These require fundamentally different CSRF strategies:
- **Thymeleaf forms:** CSRF must be enabled. Thymeleaf auto-includes the token.
- **REST API (JWT):** CSRF should be disabled for stateless token-authenticated endpoints because the JWT itself serves as a CSRF-equivalent bearer token.

Developers either disable CSRF globally (breaking Thymeleaf security) or leave it enabled globally (breaking API calls from non-browser clients).

**How to avoid:**
Configure CSRF selectively per request path:

```java
http.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**")  // Stateless JWT endpoints
    // CSRF remains enabled for /login, /register, /reset-password, etc.
);
```

For Thymeleaf templates, CSRF inclusion is automatic -- no manual `<input type="hidden">` needed if using `th:action`.

If any Thymeleaf pages also make AJAX calls, use the `.spa()` configuration or include CSRF via meta tags:
```html
<meta name="_csrf" content="${_csrf.token}"/>
<meta name="_csrf_header" content="${_csrf.headerName}"/>
```

**Warning signs:**
- 403 errors on API POST/PUT/DELETE requests
- `csrf().disable()` in the security filter chain without path discrimination
- Thymeleaf forms submitting without CSRF but "working" in tests (tests may auto-inject CSRF)

**Phase to address:**
Phase 1 (Security Foundation) -- Define the security filter chain correctly from the start with separate CSRF handling for web vs API paths.

**Confidence:** HIGH -- Verified in official Spring Security CSRF documentation.

---

### Pitfall 3: JWT Token Signing with Weak or Hardcoded Secrets

**What goes wrong:**
JWT tokens can be forged by attackers because the signing secret is too short, hardcoded in source code, uses a weak algorithm, or is committed to version control.

**Why it happens:**
Tutorials frequently show hardcoded secrets like `secret` or `my-secret-key` with HMAC-SHA256. Developers copy this and never replace it. Even when using `.env` files, the secret may be too short (HMAC-SHA256 requires 256+ bits), or the algorithm choice (symmetric HMAC) makes key rotation impossible without invalidating all tokens.

**How to avoid:**
1. **Use asymmetric signing (RS256 or ES256)** -- Private key signs, public key verifies. This allows key rotation and separation of concerns.
2. **If using symmetric (HS256):** Secret MUST be at least 32 bytes (256 bits). Use `SecureRandom` to generate.
3. **Never hardcode secrets.** Load from environment variable or secrets manager. The `.env` file must be in `.gitignore`.
4. **Set the algorithm explicitly** in the `JwtDecoder`. Never allow the JWT header to dictate the algorithm (prevents algorithm confusion attacks).

```java
// Prefer asymmetric:
@Bean
JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(rsaPublicKey)
        .signatureAlgorithm(SignatureAlgorithm.RS256)
        .build();
}
```

5. **Always validate `iss` (issuer) and `aud` (audience) claims.** Spring Security supports this via `JwtValidators.createDefaultWithIssuer()` and the `audiences` property.

**Warning signs:**
- JWT secret is a readable string shorter than 32 characters
- Secret appears in `application.yml` or `application.properties` without `${ENV_VAR}` indirection
- No audience or issuer claim validation configured
- Algorithm not explicitly constrained in `JwtDecoder` configuration

**Phase to address:**
Phase 1 (Security Foundation) -- JWT signing must be secure from the first implementation. Changing the algorithm or key later invalidates all existing tokens.

**Confidence:** HIGH -- Based on OWASP JWT guidelines and Spring Security JWT official documentation.

---

### Pitfall 4: H2/PostgreSQL Compatibility Divergence

**What goes wrong:**
Code works perfectly in development with H2, then fails in production with PostgreSQL. Or worse, it works in both but produces different results silently (data corruption). Common divergences include:
- SQL syntax differences (H2 `MERGE`, PostgreSQL `ON CONFLICT`)
- Function name differences (`DATEDIFF` vs `DATE_PART`, `RANDOM()` vs `RANDOM()`)
- Case sensitivity in identifiers (H2 uppercases by default, PostgreSQL lowercases)
- `BOOLEAN` handling differences
- `TEXT` vs `CLOB` type mapping
- Sequence/auto-increment behavior (`GENERATED BY DEFAULT AS IDENTITY` vs `SERIAL` vs `GENERATED ALWAYS`)
- JSON column support (PostgreSQL `jsonb` has no H2 equivalent)
- `TIMESTAMP WITH TIME ZONE` behavior differences

**Why it happens:**
H2's PostgreSQL compatibility mode (`MODE=PostgreSQL`) covers basic syntax but does NOT replicate PostgreSQL semantics fully. It is a "best effort" compatibility layer. Teams rely on H2 for fast dev cycles and discover incompatibilities only during staging/production deployment.

**How to avoid:**
1. **Use Testcontainers with PostgreSQL for integration tests.** Never test SQL against H2 if production runs PostgreSQL.
2. **Use H2 only for rapid local development**, not as a test oracle.
3. **Set H2 compatibility mode** for dev: `jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`
4. **Use Flyway or Liquibase** for database migrations. Maintain a single migration set that works on both (or separate dev/prod migration sets).
5. **Avoid H2-specific or PostgreSQL-specific SQL** in repository queries. Use JPQL or Criteria API where possible.
6. **If using native queries,** test them against real PostgreSQL via Testcontainers.
7. **Configure identifier quoting** consistently: `spring.jpa.properties.hibernate.globally_quoted_identifiers=true`

**Warning signs:**
- Native SQL queries in repositories (`@Query(nativeQuery = true)`)
- No Testcontainers dependency in test scope
- Tests pass but staging/production deployments fail with SQL errors
- Using H2-specific functions like `CSVREAD`, `MERGE`, or `LINK_SCHEMA`

**Phase to address:**
Phase 1 (Project Bootstrap) -- Set up dual-database profile configuration from day one. Phase 2 (Data Layer) -- Add Testcontainers immediately when writing repository tests.

**Confidence:** HIGH -- Well-documented and widely experienced issue. H2 docs themselves warn about compatibility limitations.

---

### Pitfall 5: Spring Modulith Boundary Violations Through Shared Entities

**What goes wrong:**
The Auth module directly accesses User module entities (e.g., `UserEntity`, `UserRepository`), violating module boundaries. Or the Shared module becomes a dumping ground for everything, effectively bypassing modularity. Spring Modulith's `verify()` catches some of these, but not all -- especially when entities are in a "shared" package that was intended to be narrow.

**Why it happens:**
Authentication inherently needs user data. The naive approach is: Auth module imports `UserRepository` and queries the user table directly. This creates a tight coupling that defeats the purpose of modular architecture. Similarly, developers put DTOs, entities, and utilities in the Shared module to avoid circular dependencies, gradually making Shared a "god module."

**How to avoid:**
1. **Define clear API boundaries** for each module. The User module exposes a `UserManagement` service (public API), not `UserRepository` (internal).
2. **Auth module depends on User module's API only:**
   ```java
   // User module API (public)
   public interface UserManagement {
       Optional<UserDetails> findByEmail(String email);
       UserRegistrationResult register(UserRegistrationRequest request);
   }

   // Auth module uses UserManagement, NOT UserRepository
   ```
3. **Use `@ApplicationModule(allowedDependencies = ...)`** to enforce:
   ```java
   @ApplicationModule(allowedDependencies = "user")
   package com.example.auth;
   ```
4. **Shared module must contain ONLY:**
   - Value objects (DTOs, enums)
   - Cross-cutting interfaces
   - Events
   - NOT entities, NOT repositories, NOT services
5. **Run `ApplicationModules.of(Application.class).verify()` in a test** -- this catches illegal cross-module references at test time.
6. **Prefer events for loose coupling** between Auth and User modules for non-query operations (e.g., "user registered" event rather than Auth calling User directly for side effects).

**Warning signs:**
- `import com.example.user.internal.*` in the auth module
- Shared module has more than ~10 classes
- `ApplicationModules.verify()` test does not exist or is `@Disabled`
- Circular dependency compiler errors between modules

**Phase to address:**
Phase 1 (Project Bootstrap / Architecture) -- Define module boundaries and API surfaces before writing implementation code. Add `verify()` test in Phase 1.

**Confidence:** HIGH -- Verified in official Spring Modulith documentation on fundamentals, verification, and named interfaces.

---

### Pitfall 6: Email Verification Token Exploits

**What goes wrong:**
Email verification and password reset tokens are vulnerable to:
1. **User enumeration:** Different responses for "email exists" vs "email not found" reveal which emails are registered.
2. **Token reuse:** Verification tokens remain valid after use, allowing replay attacks.
3. **Predictable tokens:** Using sequential IDs or timestamps instead of cryptographic randomness.
4. **No expiry:** Tokens valid forever, widening the attack window.
5. **Token in URL query parameter:** Gets logged in server access logs, proxy logs, referrer headers.

**Why it happens:**
Email verification is treated as a "nice to have" feature rather than a security boundary. Developers focus on making it work (email sends, link clicks) without hardening the token lifecycle.

**How to avoid:**
1. **Return identical responses** regardless of whether the email exists: "If this email is registered, you will receive a verification link."
2. **Generate tokens with `SecureRandom`** -- at least 32 bytes, URL-safe Base64 encoded.
3. **Store tokens hashed (SHA-256)** in the database, not plaintext. Hash the token from the URL, compare to stored hash.
4. **Set aggressive expiry:** 24 hours for email verification, 1 hour for password reset.
5. **Invalidate on use:** Mark token as consumed immediately on first use. Delete or set `used_at` timestamp.
6. **Rate-limit token generation:** Max 3 verification emails per hour per account.
7. **Use constant-time comparison** for token validation to prevent timing attacks.
8. **Put tokens in URL path, not query params** if possible. Or accept the logging risk and ensure logs are secured.
9. **Bind tokens to the specific action:** A verification token cannot be used as a password reset token and vice versa.

**Warning signs:**
- Token is a UUID v4 (128 bits of randomness is acceptable but ensure it is not UUID v1 which is timestamp-based)
- Token stored in plaintext in the database
- No `expires_at` column in the token table
- Different API responses for existing vs non-existing emails
- No rate limiting on `/forgot-password` or `/resend-verification`

**Phase to address:**
Phase 3 (Email Verification & Password Reset) -- Implement token lifecycle security from the first iteration. Retrofitting hashed storage and expiry is painful.

**Confidence:** HIGH -- Based on OWASP Forgot Password Cheat Sheet principles and standard security practices.

---

### Pitfall 7: Spring Modulith Event Publication Registry Not Configured

**What goes wrong:**
Events published between modules are silently lost when a listener fails or the application crashes between publication and consumption. In a user management context: a "UserRegistered" event triggers email verification, but if the email service is temporarily down, the event is lost and the user never receives their verification email. There is no retry, no audit trail, no alert.

**Why it happens:**
Default Spring `ApplicationEventPublisher` is fire-and-forget. Spring Modulith provides an Event Publication Registry that persists events transactionally, but it requires explicit setup (a starter dependency and database table). Developers use `@ApplicationModuleListener` thinking it provides reliability, but without the registry, events are lost on failure.

**How to avoid:**
1. **Add the event registry starter:**
   ```xml
   <dependency>
       <groupId>org.springframework.modulith</groupId>
       <artifactId>spring-modulith-starter-jpa</artifactId>
   </dependency>
   ```
2. **Enable republishing on restart:**
   ```properties
   spring.modulith.events.republish-outstanding-events-on-restart=true
   ```
3. **Configure completion mode** to prevent table bloat:
   ```properties
   spring.modulith.events.completion-mode=DELETE
   ```
   Or use `ARCHIVE` if you need an audit trail.
4. **Purge completed events** periodically:
   ```java
   completedPublications.purgeOlderThan(Duration.ofDays(7));
   ```
5. **Monitor incomplete publications** -- if they accumulate, a listener is systematically failing.
6. **Test event flows** using `@ApplicationModuleTest` and `Scenario` API.

**Warning signs:**
- No `spring-modulith-starter-jpa` (or jdbc/mongodb) dependency
- No `event_publication` table in the database schema
- Users report not receiving verification emails intermittently
- No monitoring on incomplete event publications

**Phase to address:**
Phase 2 (Modulith Architecture) -- Configure the event publication registry when setting up inter-module communication. This is infrastructure, not a feature.

**Confidence:** HIGH -- Verified in official Spring Modulith events documentation.

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| `csrf().disable()` globally | API calls work without tokens | Thymeleaf forms vulnerable to CSRF | Never in a hybrid Thymeleaf+REST app |
| Hardcoded JWT secret in `application.yml` | Quick setup, no env config | Secret in source control, no rotation | Only in earliest prototype; replace before any deployment |
| Single `SecurityFilterChain` for all endpoints | Simpler configuration | Auth rules become tangled, hard to reason about | Acceptable if kept clean; split when complexity grows |
| Storing verification tokens in plaintext | Simpler queries | Token DB leak = account takeover | Never for password reset tokens; arguable for low-risk verification |
| Skipping `ApplicationModules.verify()` test | Faster builds | Module boundaries silently violated | Never -- this test is the whole point of Modulith |
| Using `@SpringBootTest` instead of `@ApplicationModuleTest` | Tests "just work" without mock setup | Modules not tested in isolation, boundary violations hidden | Only for full integration/smoke tests |
| H2 for all tests (no Testcontainers) | Fast tests, no Docker needed | SQL incompatibilities discovered only in prod | Acceptable for unit tests; integration tests need real PostgreSQL |
| `spring.jpa.hibernate.ddl-auto=update` in production | No migration scripts needed | Schema drift, data loss risk, no rollback | Never in production; Flyway/Liquibase required |
| Synchronous event listeners | Simpler reasoning, same transaction | Transaction expansion, cascading failures | Only when the listener MUST be in the same transaction |

---

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| SMTP Email | Blocking the request thread waiting for SMTP | Use `@Async` or `@ApplicationModuleListener` (async) for sending email. Never block the registration request on email delivery |
| SMTP Email | Not handling SMTP failures | Wrap email sending in try/catch, log failures, use event publication registry for retry. Return success to user regardless (prevents user enumeration) |
| SMTP Email | Using `localhost:25` default | Configure real SMTP (e.g., Mailpit for dev, SES/SendGrid for prod) via environment variables. No hardcoded SMTP hosts |
| PostgreSQL via Docker | Using `localhost` as DB host from inside another container | Use Docker Compose service names (`db`, `postgres`) as hostnames between containers; use `localhost` only from the host machine |
| PostgreSQL | Not waiting for DB readiness | Add `healthcheck` and `depends_on.condition: service_healthy` in Docker Compose. Spring Boot retry alone is not sufficient for initial startup |
| H2 Console | Leaving H2 console enabled in production profile | Guard with `spring.h2.console.enabled=true` only in `application-dev.yml`. H2 console is an admin shell |
| Docker Compose | Spring Boot Docker Compose support conflicting with manual `docker compose up` | Choose one approach: either let Spring Boot manage compose lifecycle OR manage it manually. Mixing causes port conflicts and stale containers |

---

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| N+1 queries in user listing | Page loads slow, database CPU high | Use `JOIN FETCH` in JPQL or `@EntityGraph` for associations. Profile with `spring.jpa.show-sql=true` in dev | 100+ users with roles/permissions associations |
| Unbounded token table growth | Verification/reset token table grows indefinitely, queries slow | Add `expires_at` index, scheduled cleanup job for expired tokens, event publication purge | 10K+ tokens (a few months of active registrations) |
| BCrypt cost factor too high | Login/registration takes 2+ seconds | Default BCrypt strength (10) is fine. Do NOT increase beyond 12 without benchmarking. At strength 14, each hash takes ~1 second | Immediately at strength 14+, or under load at strength 12 |
| No connection pooling config | Database connections exhausted under load | Configure HikariCP: `spring.datasource.hikari.maximum-pool-size=10` (default). Monitor with actuator `/metrics` | 50+ concurrent users with default pool of 10 |
| Synchronous email in request thread | Registration endpoint takes 3-5 seconds (SMTP round-trip) | Send email asynchronously. Return HTTP response immediately, send email via async listener or background task | First user on a slow SMTP server |
| JWT without expiry or with very long expiry | Revoked users retain access, token DB grows if tracking blacklist | Set short access token expiry (15-30 minutes). Use refresh tokens (hours/days) for session continuity | When first user account needs to be deactivated |

---

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| No rate limiting on login endpoint | Brute-force password attacks | Implement rate limiting: 5 attempts per IP per minute, exponential backoff. Consider Spring Security's built-in `AuthenticationFailureHandler` for account lockout |
| Password reset link valid indefinitely | Attacker with old email access can reset password weeks later | 1-hour expiry maximum. Invalidate ALL pending reset tokens when a new one is generated or when password is successfully changed |
| Returning different errors for "user not found" vs "wrong password" | User enumeration -- attackers learn which emails are registered | Return identical error: "Invalid email or password." Use constant-time comparison for both paths |
| Not invalidating JWT after password change | User changes password but old JWTs still work until expiry | Store a `tokenInvalidatedBefore` timestamp per user. Check JWT `iat` (issued-at) against this during validation. Or use short-lived tokens + refresh token rotation |
| Self-registration without email verification gate | Spam accounts, fake emails pollute user table | Require email verification before account is "active." Unverified accounts should have restricted access or be auto-deleted after 48 hours |
| Exposing user IDs in API responses | Sequential IDs reveal user count and allow enumeration | Use UUIDs as public-facing identifiers. Keep sequential IDs internal. Never expose auto-increment IDs in URLs or API responses |
| Missing `Secure`, `HttpOnly`, `SameSite` flags on auth cookies | Session hijacking via XSS, CSRF via cross-site cookie inclusion | Spring Security sets these by default for session cookies, but verify: `server.servlet.session.cookie.http-only=true`, `server.servlet.session.cookie.secure=true`, `server.servlet.session.cookie.same-site=lax` |
| Storing JWT in `localStorage` | XSS can steal the token and impersonate the user | For browser clients, store JWT in `HttpOnly` cookie (not accessible to JavaScript). For API-only clients (mobile), `localStorage` is acceptable but XSS must be mitigated |

---

## UX Pitfalls

Common user experience mistakes in user management systems.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No feedback after registration | User does not know if registration succeeded or if they need to check email | Show clear success page: "Check your email for a verification link. It may take a few minutes." |
| Verification link expired with no resend option | User cannot complete registration, must re-register | Show "Link expired" page with a prominent "Resend verification email" button |
| Password requirements not shown until submission fails | User tries multiple times to guess password rules | Show password requirements inline, validate in real-time as user types |
| Password reset sends email even if account does not exist | Creates confusion; user thinks they have an account | Still return "If this email is registered..." but do NOT send an email. This prevents email spam while maintaining security |
| Login error does not explain next steps | User stuck with "Invalid credentials" and no path forward | Show "Invalid email or password" with links to "Forgot password?" and "Create account" |
| Email verification link is very long and gets line-wrapped | Link breaks in email clients, user cannot click through | Use URL shortener or encode token as a shorter path parameter. Test in multiple email clients |

---

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Login:** Does login work across server restarts? (Session persistence configured, or JWT fully stateless)
- [ ] **Registration:** Does registration prevent duplicate emails? (Unique constraint + proper error handling, not just "hope for the best")
- [ ] **Email verification:** Are tokens single-use? (Test: click the link twice -- second click should show "already verified" or "link expired")
- [ ] **Password reset:** Does resetting password invalidate all existing sessions/tokens? (Change password, verify old session is dead)
- [ ] **JWT:** Is the token validated on EVERY request? (Not just decoded -- signature, expiry, issuer, audience all checked)
- [ ] **CSRF:** Are Thymeleaf form submissions tested with CSRF enabled? (Easy to miss if you only test APIs)
- [ ] **Docker:** Does the app start correctly with `docker compose up` from a clean state? (Not just when containers are already warm)
- [ ] **Database:** Do migrations run clean on an empty PostgreSQL? (Not just on H2 where DDL-auto papers over issues)
- [ ] **Error handling:** Does a 500 error on email send prevent registration? (It should not -- email is async)
- [ ] **Logout:** Does logout actually clear the session AND respond correctly? (Test: logout, hit back button, verify no authenticated content shown)
- [ ] **Module boundaries:** Does `ApplicationModules.verify()` pass? (Run it after every new cross-module dependency)
- [ ] **Environment:** Does the app start without `.env` file? (It should fail fast with clear error, not start with null/default secrets)

---

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Leaked JWT signing secret | HIGH | Rotate secret immediately. All existing tokens become invalid. Users must re-authenticate. Audit logs for unauthorized access during exposure window |
| CSRF disabled globally in production | MEDIUM | Re-enable CSRF with path discrimination. Deploy immediately. Audit access logs for suspicious cross-origin form submissions |
| Module boundary violations accumulated | HIGH | Run `ApplicationModules.verify()`, list all violations. Refactor one module at a time. Extract shared types to Shared module API. This is a multi-sprint effort if boundaries are deeply violated |
| H2-specific SQL in production | MEDIUM | Identify all native queries. Rewrite in JPQL or PostgreSQL-compatible SQL. Run full test suite against PostgreSQL via Testcontainers to catch remaining issues |
| Plaintext verification tokens in DB leaked | HIGH | Invalidate ALL existing tokens. Migrate to hashed storage (SHA-256). Force re-verification for all pending users. Notify affected users |
| Event publication registry not configured (lost events) | MEDIUM | Add registry dependency and configuration. For already-lost events: identify affected users (e.g., registered but never received verification email). Trigger re-send manually or via admin endpoint |
| Docker networking misconfiguration (wrong DB host) | LOW | Fix `application-docker.yml` to use service name. Restart containers. No data loss, just connectivity fix |

---

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| SecurityContext not saved explicitly | Phase 1: Security Foundation | Manual test: login via form, navigate to protected page, verify authenticated |
| CSRF misconfiguration (hybrid app) | Phase 1: Security Foundation | Test both: Thymeleaf form POST with CSRF, API POST without CSRF |
| JWT weak signing / hardcoded secret | Phase 1: Security Foundation | Code review: no secrets in source files; verify key length >= 256 bits |
| H2/PostgreSQL divergence | Phase 1: Bootstrap + Phase 2: Data Layer | Testcontainers integration test against real PostgreSQL passes |
| Module boundary violations | Phase 1: Bootstrap (structure) + continuous | `ApplicationModules.verify()` test is green and runs in CI |
| Email token exploits | Phase 3: Email Verification | Penetration test: reuse token, enumerate users, check expiry |
| Event publication registry missing | Phase 2: Modulith Setup | Verify `event_publication` table exists; test listener failure + recovery |
| No rate limiting on auth endpoints | Phase 3: Hardening | Load test: send 100 login attempts in 10 seconds, verify rate limiting kicks in |
| Password reset token indefinite validity | Phase 3: Password Reset | Test: generate reset token, wait > 1 hour, verify it is rejected |
| Sequential user IDs exposed | Phase 2: Data Layer | API review: no auto-increment IDs in responses; UUIDs only |
| `ddl-auto=update` in production | Phase 1: Bootstrap | Profile check: production profile uses `validate` or `none`, never `update` or `create` |
| Synchronous email blocking requests | Phase 3: Email Integration | Measure: registration endpoint returns < 500ms regardless of SMTP availability |
| Docker container networking | Phase 1: Docker Setup | `docker compose up` from clean state succeeds; app connects to DB |
| Jackson 2 to Jackson 3 migration | Phase 1: Bootstrap | Compile succeeds; no `com.fasterxml.jackson` imports; serialization tests pass |

---

## Spring Boot 4 / Spring Security 7 Specific Warnings

These are specific to the Spring Boot 4 + Spring Framework 7 + Spring Security 7 stack. Older tutorials and Stack Overflow answers will be WRONG for these.

### Jackson 2 to Jackson 3 Migration

**What changed:** Spring Boot 4 uses Jackson 3 (`tools.jackson.core:jackson-databind`) instead of Jackson 2 (`com.fasterxml.jackson.core:jackson-databind`). The package namespace changed. All Spring Security serialization modules were renamed:
- `SecurityJackson2Modules` becomes `SecurityJacksonModules`
- Module registration API changed from `ObjectMapper.registerModules()` to `JsonMapper.Builder.addModules()`

**Impact:** Any custom serialization, `@JsonProperty` annotations, or session serialization using Jackson will need updating. If using Spring Session with JSON serialization (for Redis/JDBC session storage), this is a compile-time break.

**Confidence:** HIGH -- Verified in official Spring Security 7.0 migration guide.

### SecurityFilterChain Configuration Changes

**What changed:** (LOW confidence -- based on training data, verify against Spring Security 7 servlet migration docs)
Spring Security 7 removes methods that were deprecated in 6.x. Key removals likely include:
- `sessionManagement().sessionAuthenticationErrorUrl()` -- replaced by `AuthenticationFailureHandler`
- `sessionManagement().sessionAuthenticationFailureHandler()` -- configure in auth mechanism directly
- `sessionManagement().sessionAuthenticationStrategy()` -- configure in auth mechanism directly
- Various `and()` chaining calls (already deprecated in 6.x)

**Impact:** If copying configuration from Spring Security 5.x or early 6.x tutorials, compilation will fail.

**Mitigation:** Use the Spring Security 7.0 migration guide. First upgrade to Spring Security 6.5 which provides deprecation warnings, then to 7.0.

**Confidence:** MEDIUM -- Session management deprecations verified in official docs; full removal list needs verification against Spring Security 7 servlet migration guide (which returned 404 at time of research).

### Java 21 Baseline

**What changed:** Spring Boot 4 / Spring Framework 7 requires Java 21 minimum. Virtual threads are available but not default.

**Impact:** If using `@Async` for email sending, consider whether virtual threads (Project Loom) would simplify the model. Standard platform threads are still the default.

**Confidence:** MEDIUM -- Java 21 requirement is well-established for Spring Framework 7; virtual thread defaults need verification.

---

## Sources

- Spring Security Session Management Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - Explicit SecurityContext saving requirement, session fixation, stateless configuration
- Spring Security CSRF Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - SPA configuration, BREACH protection, Thymeleaf integration, selective disabling
- Spring Security JWT Resource Server Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - Algorithm configuration, issuer/audience validation, clock skew, caching
- Spring Security 7.0 Migration Guide (official, fetched 2026-01-28) -- HIGH confidence
  - Jackson 3 migration, upgrade path from 6.5 to 7.0
- Spring Security Password Storage Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - DelegatingPasswordEncoder, BCrypt configuration
- Spring Modulith Fundamentals Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - Module types, boundary rules, named interfaces, verification
- Spring Modulith Events Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - Event publication registry, completion modes, retry, transaction boundaries
- Spring Modulith Testing Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - `@ApplicationModuleTest`, Scenario API, bootstrap modes, mocking
- Spring Modulith Verification Documentation (official, fetched 2026-01-28) -- HIGH confidence
  - Cycle detection, internal access rejection, `allowedDependencies` enforcement
- Spring Boot Docker Compose Documentation (official, fetched 2026-01-28) -- MEDIUM confidence
  - Lifecycle management, JDBC labels (limited content retrieved)
- OWASP Forgot Password Cheat Sheet principles -- MEDIUM confidence (URL inaccessible, applied from training knowledge)
- OWASP JWT Cheat Sheet principles -- MEDIUM confidence (URL inaccessible, applied from training knowledge)
- H2 PostgreSQL Compatibility -- MEDIUM confidence (URL inaccessible, based on well-known community experience)
- Spring Boot 4.0 Release Notes / Migration Guide -- LOW confidence (both URLs returned 404; information based on training data about announced plans)

---
*Pitfalls research for: Spring Boot 4 / Spring Modulith User Management Server*
*Researched: 2026-01-28*
