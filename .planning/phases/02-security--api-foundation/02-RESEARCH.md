# Phase 2: Security & API Foundation - Research

**Researched:** 2026-01-31
**Domain:** Spring Security 7, JWT authentication, BCrypt, CSRF, error handling, JPA entities for User/Role
**Confidence:** HIGH (official docs, verified library APIs, confirmed Spring Boot 4 / Spring Security 7 patterns)

## Summary

Phase 2 establishes the security infrastructure for the entire application. The core challenge is building a **dual SecurityFilterChain** configuration: one stateless chain for `/api/**` endpoints (JWT-based, no CSRF) and one session-based chain for `/**` web pages (CSRF enabled, form login). This is a well-documented pattern in Spring Security 7, but requires careful attention to filter ordering, the Jackson 2/3 compatibility issue with JJWT, and Spring Security 7's migration from `AntPathRequestMatcher` to `PathPatternRequestMatcher`.

The phase also requires JPA entities (`AppUser`, `AppRole`) that map to the existing Flyway schema, a `UserDetailsService` implementation that bridges Spring Security with the User module, BCrypt password encoding, and a `GlobalExceptionHandler` producing RFC 9457 ProblemDetail JSON error responses that prevent user enumeration.

A critical discovery is that **JJWT (io.jsonwebtoken) does not yet support Jackson 3**, which is the default JSON library in Spring Boot 4. The recommended solution is to use `jjwt-gson` instead of `jjwt-jackson`, completely sidestepping the conflict. This avoids pulling in the deprecated `spring-boot-jackson2` bridge module.

**Primary recommendation:** Build the dual SecurityFilterChain in `auth.internal`, use `jjwt-gson` for JWT processing, implement RFC 9457 ProblemDetail error responses in `shared.exception`, and create JPA entities in `user.internal` mapping to the existing Flyway-managed schema.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Security | 7.0.x (managed by Boot 4.0.1) | Authentication, authorization, filter chains, CSRF | The standard security framework for Spring. Version 7 aligns with Spring Framework 7 / Boot 4. |
| JJWT (io.jsonwebtoken) | 0.12.6 or 0.13.0 | JWT token creation, signing, parsing, validation | De facto standard JWT library for Java. Modular design, type-safe API, algorithm enforcement. |
| Spring Security Crypto (BCryptPasswordEncoder) | Included in spring-boot-starter-security | Password hashing | Built into Spring Security. BCrypt with configurable strength. No extra dependency. |
| Spring Data JPA | Managed by Boot | Entity persistence, repositories | Standard JPA abstraction layer. Hibernate 7 under the hood with Boot 4. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `jjwt-gson` | Same as jjwt-api | JSON serialization for JWT internals | Use INSTEAD of `jjwt-jackson` to avoid Jackson 2/3 conflict in Spring Boot 4 |
| `spring-security-test` | Managed by Boot | Security testing (`@WithMockUser`, `csrf()`, `jwt()`) | Always. Required for testing 401/403 responses and CSRF-protected endpoints |
| Jakarta Validation (`spring-boot-starter-validation`) | Managed by Boot | Bean validation for DTOs (`@NotBlank`, `@Email`, `@Size`) | Always. Validate registration requests, login requests, etc. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `jjwt-gson` | `jjwt-jackson` + `spring-boot-jackson2` | Would work but pulls in deprecated Jackson 2 bridge module. Adds complexity. `jjwt-gson` is cleaner. |
| `jjwt-gson` | Custom `Serializer`/`Deserializer` for Jackson 3 | JJWT supports custom serializer interface, but writing a Jackson 3 adapter is unnecessary work when Gson works out of the box. |
| JJWT 0.13.0 | JJWT 0.12.6 | 0.13.0 only adds one constructor change. 0.12.6 is well-tested. Either works. Recommend 0.12.6 for maximum compatibility since it's the version most Spring Boot tutorials use. |
| Self-issued JWT | Spring Security OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`) | OAuth2 RS is designed for external identity providers. For self-issued JWT, it adds unnecessary abstraction. JJWT + custom filter is the standard community pattern. |
| ProblemDetail (RFC 9457) | Custom error DTO | ProblemDetail is built into Spring Framework 7, auto-serialized by Jackson, supports i18n. No reason to hand-roll. |

**Dependencies to add to pom.xml:**

```xml
<!-- JWT (replace jjwt-jackson with jjwt-gson for Spring Boot 4 compatibility) -->
<properties>
    <jjwt.version>0.12.6</jjwt.version>
</properties>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-gson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

## Architecture Patterns

### Recommended Project Structure (Phase 2 additions)

```
src/main/java/com/example/usermanagement/
|-- auth/
|   |-- JwtService.java                    # PUBLIC: JWT creation and validation
|   |-- auth.internal/
|   |   |-- SecurityConfig.java            # Dual SecurityFilterChain (MOVED from shared/config)
|   |   |-- JwtAuthenticationFilter.java   # OncePerRequestFilter for JWT extraction
|   |   +-- CustomUserDetailsService.java  # UserDetailsService bridging to User module
|
|-- user/
|   |-- UserService.java                   # PUBLIC: getUserByEmail, createUser, etc.
|   |-- user.internal/
|   |   |-- AppUser.java                   # JPA entity mapping to app_user table
|   |   |-- AppRole.java                   # JPA entity mapping to app_role table
|   |   |-- UserRepository.java            # Spring Data JPA repository
|   |   +-- RoleRepository.java            # Spring Data JPA repository
|
|-- shared/
|   |-- config/
|   |   +-- AppProperties.java             # @ConfigurationProperties for app.jwt.* etc.
|   |-- dto/
|   |   +-- UserDto.java                   # User data exposed across module boundaries
|   |-- exception/
|   |   |-- GlobalExceptionHandler.java    # @ControllerAdvice with ProblemDetail
|   |   |-- ResourceNotFoundException.java
|   |   +-- DuplicateResourceException.java
```

### Pattern 1: Dual SecurityFilterChain

**What:** Two `SecurityFilterChain` beans with `@Order` -- API chain first (stateless, JWT), web chain second (session, CSRF, form login).

**When to use:** Hybrid applications serving both REST API and server-rendered pages.

**Critical rules:**
- API chain must have `@Order(1)` with `securityMatcher("/api/**")` -- it is more specific and must match first
- Web chain has `@Order(2)` with no `securityMatcher` (or `"/**"`) -- it is the catch-all
- Spring Security 7 uses `PathPatternRequestMatcher` internally (replaces `AntPathRequestMatcher` and `MvcRequestMatcher`)
- The `securityMatcher(String...)` and `requestMatchers(String...)` DSL methods work the same as in Security 6.x -- the underlying matcher has changed but the configuration API is the same

**Example:**

```java
// Source: Spring Security 7 docs + community best practice
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                               JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiAuthenticationEntryPoint())
                .accessDeniedHandler(apiAccessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // H2 console
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );
        return http.build();
    }
}
```

### Pattern 2: JWT Authentication Filter (OncePerRequestFilter)

**What:** A filter that extracts the Bearer token from the `Authorization` header, validates it, and sets the `SecurityContext`.

**When to use:** For all requests matching the API SecurityFilterChain.

**Critical rules:**
- Do NOT annotate the filter as `@Bean` / `@Component` if registering it in only one SecurityFilterChain. Spring Boot auto-registers all `OncePerRequestFilter` beans in the global servlet filter chain, causing the filter to run on ALL requests, not just `/api/**`. Instead, instantiate it with `new` in the config or use `FilterRegistrationBean` to disable auto-registration.
- Handle exceptions within the filter itself -- exceptions thrown before `ExceptionTranslationFilter` will not be caught by `AuthenticationEntryPoint`.
- Always call `filterChain.doFilter()` even when no token is present (let the chain continue to authorization checks).

**Example:**

```java
// Source: Community best practice, verified against Spring Security 7 docs
// NOT a @Component -- instantiated manually in SecurityConfig
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log but don't throw -- let the chain continue, authorization will fail naturally
            logger.debug("JWT authentication failed", e);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Pattern 3: JwtService with JJWT 0.12.x API

**What:** Service class encapsulating JWT token creation, parsing, and validation.

**When to use:** Always. Central place for all JWT operations.

**Critical JJWT 0.12+ API notes:**
- `Jwts.builder()` (not `Jwts.parser()` or deprecated `Jwts.parserBuilder()`)
- `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` (replaces deprecated `parseClaimsJws`)
- `Keys.hmacShaKeyFor(bytes)` to create `SecretKey` from Base64-decoded secret
- `signWith(key)` auto-detects algorithm from key size (256-bit key = HS256)
- The old `Keys.secretKeyFor()` is deprecated since 0.12.0

**Example:**

```java
// Source: JJWT 0.12.x official README (https://github.com/jwtk/jjwt)
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return resolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
```

### Pattern 4: RFC 9457 ProblemDetail Error Responses

**What:** Consistent JSON error responses using Spring Framework's built-in `ProblemDetail` class.

**When to use:** For ALL API error responses (400, 401, 403, 404, 409, 500).

**Critical rules:**
- Enable with `spring.mvc.problemdetails.enabled=true` in application.yml (Spring Boot does NOT enable this by default)
- Extend `ResponseEntityExceptionHandler` in your `@ControllerAdvice` for built-in Spring MVC exception handling
- Customize `AuthenticationEntryPoint` and `AccessDeniedHandler` to produce ProblemDetail JSON for security exceptions (these bypass `@ControllerAdvice`)
- Use `ProblemDetail.setProperty("key", value)` for custom fields -- Jackson's `ProblemDetailJacksonMixin` unwraps them as top-level JSON properties
- Content type is `application/problem+json` (RFC standard)

**Example:**

```java
// Source: Spring Framework 7 docs (https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Duplicate Resource");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
```

**API security error handlers (separate from @ControllerAdvice):**

```java
// Source: Community pattern verified against Spring Security 7 docs
// These produce JSON responses for security exceptions that bypass @ControllerAdvice
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setContentType("application/problem+json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Authentication is required to access this resource");
        // Write using Jackson 3 JsonMapper (Spring Boot 4 default)
        new tools.jackson.databind.json.JsonMapper().writeValue(response.getOutputStream(), problem);
    }
}
```

### Pattern 5: UserDetailsService with Module Boundary Respect

**What:** A `UserDetailsService` implementation in the auth module that calls the user module's public `UserService` API.

**When to use:** Always. Bridges Spring Security authentication with the domain model.

**Critical rules:**
- Lives in `auth.internal` (not exposed as module API)
- Calls `UserService.getUserByEmail()` (user module's public API) -- does NOT import user entities directly
- Returns `org.springframework.security.core.userdetails.User` (Spring Security's built-in implementation) -- not a custom class
- Throws `UsernameNotFoundException` when user not found -- but the message must NOT reveal whether the email exists (SEC-01: no user enumeration)

**Anti-enumeration pattern:**

```java
// BAD: "No user found with email: foo@bar.com" -- reveals email does not exist
// GOOD: "Bad credentials" -- generic message for all auth failures
```

### Anti-Patterns to Avoid

- **Registering JwtAuthenticationFilter as @Bean/@Component:** Causes it to run on ALL requests (including web pages), not just `/api/**`. Spring Boot auto-registers all `OncePerRequestFilter` beans globally.
- **Single SecurityFilterChain for both API and web:** Mixing form login redirects (302) with JSON error responses (401) creates confusing behavior.
- **Putting SecurityConfig in shared module:** Security configuration is auth module's responsibility. The Phase 1 placeholder in `shared/config` must be moved to `auth.internal`.
- **User entity implementing UserDetails directly:** Couples domain model to Spring Security. Use a wrapper or Spring Security's built-in `User` builder.
- **Detailed error messages on auth failure:** "Email not found" or "Wrong password" enables user enumeration. Always use a generic "Bad credentials" message.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Custom hash function or SHA-256 | `BCryptPasswordEncoder` (Spring Security) | BCrypt includes salt, is intentionally slow, resistant to rainbow tables. Configurable strength factor. |
| JWT creation/parsing | Manual Base64 + HMAC + JSON | JJWT library (`Jwts.builder()`, `Jwts.parser()`) | Handles algorithm enforcement, key validation, claim parsing, expiration checking. Manual JWT is error-prone. |
| CSRF token management | Custom token generation and validation | Spring Security CSRF (enabled by default) | Handles token generation, storage (session or cookie), validation, BREACH protection, Thymeleaf integration. |
| Error response format | Custom `ErrorResponse` class | `ProblemDetail` (Spring Framework 7, RFC 9457) | Built-in, standard format, auto-serialized by Jackson, supports i18n, content-negotiation aware. |
| Request path matching | Custom URL pattern matching | `PathPatternRequestMatcher` (Spring Security 7 default) | Automatically used by `securityMatcher()` and `requestMatchers()` DSL. No manual instantiation needed. |
| JSON serialization for JWT | Custom serializer for Jackson 3 | `jjwt-gson` module | Drop-in replacement. Zero code changes. Avoids Jackson 2/3 conflict entirely. |

**Key insight:** Spring Security 7 and Spring Framework 7 provide built-in solutions for every security concern in this phase. The only external library needed is JJWT (with Gson backend) for self-issued JWT tokens.

## Common Pitfalls

### Pitfall 1: JJWT Jackson 2 / Spring Boot 4 Jackson 3 Conflict

**What goes wrong:** Adding `jjwt-jackson` to a Spring Boot 4 project pulls in Jackson 2 (`com.fasterxml.jackson`), which conflicts with Boot 4's default Jackson 3 (`tools.jackson`). The application may fail at runtime with class loading errors or silently use the wrong ObjectMapper.

**Why it happens:** JJWT 0.12.x and 0.13.0 depend on Jackson 2. Spring Boot 4 switched to Jackson 3 (released Oct 2025). JJWT has not yet released a Jackson 3 compatible version.

**How to avoid:** Use `jjwt-gson` instead of `jjwt-jackson`. JJWT's modular design allows swapping the JSON backend with zero code changes -- it auto-discovers the serializer via `ServiceLoader`.

**Warning signs:** Build warnings about conflicting Jackson versions; `NoClassDefFoundError` or `ClassNotFoundException` for `com.fasterxml.jackson.databind.ObjectMapper` at runtime.

### Pitfall 2: JwtAuthenticationFilter Running on All Requests

**What goes wrong:** The JWT filter runs on web page requests (Thymeleaf), not just API requests. This causes session-based authentication to conflict with JWT validation.

**Why it happens:** If `JwtAuthenticationFilter` is annotated with `@Component` or `@Bean`, Spring Boot automatically registers it in the global servlet filter chain, making it run on every request regardless of which `SecurityFilterChain` it's added to.

**How to avoid:** Do NOT annotate the filter as a Spring bean. Instantiate it with `new JwtAuthenticationFilter(jwtService, userDetailsService)` directly in the `SecurityConfig` when calling `addFilterBefore()`. Alternatively, register it as a `FilterRegistrationBean` with `setEnabled(false)`.

**Warning signs:** JWT-related log messages appearing on page load requests; Thymeleaf pages returning 401 instead of login redirect.

### Pitfall 3: AntPathRequestMatcher Removed in Spring Security 7

**What goes wrong:** Code using `new AntPathRequestMatcher("/api/**")` or `new MvcRequestMatcher(dispatcher, "/api/**")` fails to compile.

**Why it happens:** Spring Security 7 removed `AntPathRequestMatcher` and `MvcRequestMatcher` in favor of `PathPatternRequestMatcher`.

**How to avoid:** Use the `securityMatcher(String...)` and `requestMatchers(String...)` DSL methods, which automatically use `PathPatternRequestMatcher` internally. You do NOT need to instantiate `PathPatternRequestMatcher` directly in most cases.

**Warning signs:** Compile errors referencing `AntPathRequestMatcher` or `MvcRequestMatcher`.

### Pitfall 4: Security Exceptions Bypassing @ControllerAdvice

**What goes wrong:** Spring Security's `AuthenticationEntryPoint` and `AccessDeniedHandler` produce HTML error pages or raw 401/403 responses instead of JSON ProblemDetail on API endpoints.

**Why it happens:** Security exceptions are handled by the security filter chain BEFORE reaching Spring MVC's `@ControllerAdvice`. The `ExceptionTranslationFilter` delegates to entry points and access denied handlers, not to `@ExceptionHandler` methods.

**How to avoid:** Configure custom `AuthenticationEntryPoint` and `AccessDeniedHandler` on the API SecurityFilterChain that produce JSON ProblemDetail responses. The `@ControllerAdvice` handles application-level exceptions; security exceptions need separate handling.

**Warning signs:** API returning HTML on 401/403; `@ExceptionHandler` for `AuthenticationException` never being called.

### Pitfall 5: User Enumeration via Error Messages

**What goes wrong:** Login error messages like "Email not found" or "Wrong password" reveal whether an email is registered in the system, enabling account enumeration attacks.

**Why it happens:** Developers use specific error messages for user-facing feedback without considering security implications.

**How to avoid:** Always return a generic "Bad credentials" message for ALL authentication failures. Apply the same principle to registration (if email exists, don't say "email already registered" -- instead say "If an account with this email exists, you will receive a verification email"). Apply to password reset ("If this email is registered, you will receive a reset link").

**Warning signs:** Different HTTP status codes or messages for "user not found" vs "wrong password" scenarios.

### Pitfall 6: CSRF Token Missing in MockMvc POST Tests

**What goes wrong:** MockMvc POST requests to web endpoints return 403 Forbidden instead of the expected result.

**Why it happens:** Spring Security's CSRF protection is enabled on the web SecurityFilterChain. MockMvc POST requests without a CSRF token are rejected.

**How to avoid:** Add `.with(csrf())` to all POST/PUT/DELETE MockMvc requests for the web chain. Import from `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf`.

**Warning signs:** All POST tests returning 403; GET tests working fine.

### Pitfall 7: SecurityConfig Placement Violating Module Boundaries

**What goes wrong:** `SecurityConfig` in `shared/config` is accessible from all modules, but it depends on auth-specific beans (JwtService, JwtAuthenticationFilter), creating a hidden dependency from shared to auth.

**Why it happens:** Phase 1 placed a minimal SecurityConfig in shared as a placeholder. Phase 2 must move it.

**How to avoid:** Move `SecurityConfig` to `auth.internal` where it belongs. It is an internal implementation detail of the auth module. Delete the Phase 1 placeholder from `shared/config/`.

**Warning signs:** `ApplicationModules.verify()` failing with dependency violations between shared and auth.

## Code Examples

### Complete BCrypt Password Encoder Bean

```java
// Source: Spring Security official docs
// Place in SecurityConfig or a separate @Configuration
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // Default strength 10
}
```

Usage:
```java
// Encoding (registration)
String hashed = passwordEncoder.encode(rawPassword);

// Matching (login)
boolean matches = passwordEncoder.matches(rawPassword, storedHash);
```

### JPA Entity: AppUser (maps to existing app_user table)

```java
// Source: Standard JPA mapping to existing Flyway-managed schema
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();

    // No-arg constructor (JPA requirement)
    protected AppUser() {}

    // PrePersist / PreUpdate for timestamps
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters, setters, equals/hashCode by id only
}
```

### JPA Entity: AppRole (maps to existing app_role table)

```java
@Entity
@Table(name = "app_role")
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected AppRole() {}

    public AppRole(String name) {
        this.name = name;
    }

    // Getters, equals/hashCode by id only
}
```

### Custom AuthenticationEntryPoint for API (JSON ProblemDetail)

```java
// Source: Spring Security docs + RFC 9457 pattern
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final tools.jackson.databind.json.JsonMapper jsonMapper =
        tools.jackson.databind.json.JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/problem+json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Authentication is required to access this resource");
        problem.setInstance(URI.create(request.getRequestURI()));

        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
```

### Custom AccessDeniedHandler for API (JSON ProblemDetail)

```java
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final tools.jackson.databind.json.JsonMapper jsonMapper =
        tools.jackson.databind.json.JsonMapper.builder().build();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/problem+json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to access this resource");
        problem.setInstance(URI.create(request.getRequestURI()));

        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
```

### Testing Security with MockMvc

```java
// Source: Spring Security Test docs
@WebMvcTest(SomeController.class)
@Import(SecurityConfig.class) // MUST import to load custom security rules
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userAccessingAdminEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAccessingAdminEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk());
    }

    @Test
    void webPostWithoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/login")
            .param("username", "test")
            .param("password", "pass"))
            .andExpect(status().isForbidden());
    }

    @Test
    void webPostWithCsrf_succeeds() throws Exception {
        mockMvc.perform(post("/login")
            .param("username", "test")
            .param("password", "pass")
            .with(csrf()))
            .andExpect(status().is3xxRedirection()); // or 200 depending on config
    }
}
```

### Application Configuration Additions

```yaml
# application.yml additions for Phase 2
spring:
  mvc:
    problemdetails:
      enabled: true  # Enable RFC 9457 ProblemDetail error responses

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION:3600000}  # 1 hour default
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` @Bean | Spring Security 5.7+ (removed in 6.0) | Configuration is component-based, not inheritance-based |
| `AntPathRequestMatcher` / `MvcRequestMatcher` | `PathPatternRequestMatcher` | Spring Security 7.0 | Use DSL methods (`securityMatcher`, `requestMatchers`) which auto-use PathPattern |
| `authorizeRequests()` (AccessDecisionManager) | `authorizeHttpRequests()` (AuthorizationManager) | Spring Security 6.0 (old fully removed in 7.0) | Must use lambda DSL with `authorizeHttpRequests()` |
| `.and()` chaining | Lambda DSL only | Spring Security 7.0 | `.and()` method removed entirely |
| `Jwts.parserBuilder().setSigningKey().build().parseClaimsJws()` | `Jwts.parser().verifyWith(key).build().parseSignedClaims()` | JJWT 0.12.0 | New fluent API; old methods deprecated |
| `Keys.secretKeyFor(SignatureAlgorithm)` | `Jwts.SIG.HS256.key().build()` | JJWT 0.12.0 | Old method deprecated |
| `ObjectMapper` (Jackson 2, mutable) | `JsonMapper` (Jackson 3, immutable) | Jackson 3.0.0 / Spring Boot 4.0 | Jackson 3 is the default in Boot 4; `@JsonComponent` renamed to `@JacksonComponent` |
| `spring.jackson.*` properties | `spring.jackson.json.read.*` / `spring.jackson.json.write.*` | Spring Boot 4.0 | Read/write properties reorganized |
| `@JsonComponent` | `@JacksonComponent` | Spring Boot 4.0 | Annotation renamed for clarity |

**Deprecated/outdated:**
- `spring-boot-jackson2` module: Exists in Boot 4 but deprecated and will be removed. Avoid depending on it.
- `jjwt-jackson`: Still works with Jackson 2 but creates dependency conflict in Boot 4. Use `jjwt-gson`.
- `thymeleaf-extras-springsecurity6`: May need version update for Spring Security 7. Verify at build time.

## Open Questions

1. **`thymeleaf-extras-springsecurity6` compatibility with Spring Security 7**
   - What we know: The artifact name references Spring Security 6. Spring Security 7 may require a renamed artifact (`springsecurity7`).
   - What's unclear: Whether the existing artifact works with Security 7, or whether a new version has been released.
   - Recommendation: Test the existing dependency at build time. If it fails, search Maven Central for `thymeleaf-extras-springsecurity7`. This primarily affects the `sec:authorize` and `sec:authentication` attributes in Thymeleaf templates, which are not critical for Phase 2 (more relevant in Phase 3+ when building login/register pages).

2. **ProblemDetail serialization with Jackson 3 JsonMapper**
   - What we know: Spring Framework 7 includes `ProblemDetailJacksonMixin` for Jackson 3 which handles serialization of the `properties` map as top-level fields. The `application/problem+json` content type is handled automatically.
   - What's unclear: Whether `AuthenticationEntryPoint` / `AccessDeniedHandler` can use the auto-configured `JsonMapper` bean or need to create their own instance.
   - Recommendation: Try injecting the `JsonMapper` bean into the security handlers. If that's not possible (they're not Spring-managed beans), create a new `JsonMapper` instance directly. Both approaches will work.

3. **SecurityConfig location: auth.internal vs. separate shared config**
   - What we know: The architecture research recommends SecurityConfig in `auth.internal`. The Phase 1 placeholder is in `shared/config`. Spring Modulith verification will reject a dependency from shared to auth.
   - What's unclear: Whether having the PasswordEncoder bean in `auth.internal` causes issues for the user module (which also needs to encode passwords for admin-created users).
   - Recommendation: Define `PasswordEncoder` as a `@Bean` in a separate `shared/config/PasswordConfig.java` class (it's a cross-cutting infrastructure concern like a DataSource), but move the full SecurityConfig to `auth.internal`. Alternatively, Spring Boot auto-configures a `PasswordEncoder` bean if one is defined anywhere -- any `@Configuration` class can provide it.

## Sources

### Primary (HIGH confidence)

- [Spring Security 7 docs: CSRF Protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) - CSRF configuration, ignoringRequestMatchers, deferred token loading
- [Spring Security 7 docs: Authorize HTTP Requests](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) - authorizeHttpRequests DSL, role-based access
- [Spring Security 7 docs: HttpSecurity API](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/builders/HttpSecurity.html) - securityMatcher, Lambda DSL
- [Spring Security 7 docs: PathPatternRequestMatcher](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/servlet/util/matcher/PathPatternRequestMatcher.html) - Replacement for AntPathRequestMatcher
- [Spring Security 7 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html) - Breaking changes from 6.x
- [Spring Framework 7 docs: Error Responses (RFC 9457)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html) - ProblemDetail, ResponseEntityExceptionHandler
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - Jackson 3, annotation renames, security changes
- [JJWT GitHub](https://github.com/jwtk/jjwt) - Official README with 0.12.x API examples, Gson/Jackson backend options
- [Spring Blog: Introducing Jackson 3 Support in Spring](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/) - Jackson 3 transition, spring-boot-jackson2 deprecation

### Secondary (MEDIUM confidence)

- [Baeldung: CSRF Protection with Spring Security](https://www.baeldung.com/spring-security-csrf) - Verified against official docs
- [Dan Vega: Multiple Spring Security Configurations](https://www.danvega.dev/blog/multiple-spring-security-configs) - Dual SecurityFilterChain pattern with @Order
- [Medium: Multiple Security Filter Chains (Dec 2025)](https://medium.com/@samrat.alam/multiple-security-filter-chains-in-spring-security-for-separate-paths-in-spring-boot-avoiding-d6107445f3a8) - Global filter registration pitfall
- [Medium: Spring Security Filters Best Practices (Jul 2025)](https://medium.com/@persolenom/spring-security-the-right-way-to-use-filters-93616f863872) - OncePerRequestFilter patterns

### Tertiary (LOW confidence)

- [JJWT 0.13.0 release notes](https://github.com/jwtk/jjwt/releases) - 0.13.0 adds one constructor; 0.14.0 planned for Java 8+ minimum. No Jackson 3 timeline.
- [ITNEXT: Jackson 3 in Spring 7 and Spring Boot 4](https://itnext.io/an-introduction-to-jackson-3-in-spring-7-and-spring-boot-4-cba114aa36b1) - Community article on Jackson 3 migration

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Spring Security 7 and JJWT APIs verified against official docs and release notes
- Architecture: HIGH - Dual SecurityFilterChain pattern well-documented; module boundaries from prior ARCHITECTURE.md research
- Pitfalls: HIGH - Jackson 2/3 conflict confirmed by multiple sources; filter registration issue documented in Spring Boot docs
- JPA entities: HIGH - Mapping directly to existing Flyway-managed schema with known column names
- Error handling: HIGH - ProblemDetail is built into Spring Framework 7; RFC 9457 is the standard

**Research date:** 2026-01-31
**Valid until:** 2026-03-01 (stable domain -- Spring Security 7 and JJWT APIs unlikely to change significantly)
