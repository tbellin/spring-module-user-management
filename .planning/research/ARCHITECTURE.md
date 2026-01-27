# Architecture Research

**Domain:** Spring Boot 4 + Spring Modulith User Management Server
**Researched:** 2026-01-28
**Confidence:** HIGH (Spring Modulith official docs verified, Spring Security official docs verified)

## Standard Architecture

### System Overview

```
                          HTTP Requests
                               |
                    +----------v-----------+
                    |   Spring Security    |
                    |   Filter Chain       |
                    |  (JWT validation /   |
                    |   form login)        |
                    +----------+-----------+
                               |
              +----------------+----------------+
              |                |                |
     +--------v------+  +-----v--------+  +---v---------+
     |  Auth Module  |  |  User Module |  | Shared Mod  |
     |  (auth)       |  |  (user)      |  | (shared)    |
     +---------------+  +--------------+  +-------------+
     | Controllers:  |  | Controllers: |  | DTOs        |
     |  - REST API   |  |  - REST API  |  | Exceptions  |
     |  - Thymeleaf  |  |  - Thymeleaf |  | Base Entity |
     | Services:     |  | Services:    |  | Events      |
     |  - AuthSvc    |  |  - UserSvc   |  | Config      |
     |  - JwtSvc     |  |  - AdminSvc  |  |             |
     |  - EmailSvc   |  | Repository:  |  |             |
     | Repository:   |  |  - UserRepo  |  |             |
     |  - TokenRepo  |  |  - RoleRepo  |  |             |
     | Security:     |  +--------------+  +-------------+
     |  - Config     |        |
     |  - Filter     |   (publishes &
     |  - Provider   |    listens to
     +---------------+    events)
              |               |
              +-------+-------+
                      |
               +------v------+
               |  Database   |
               |  H2 / PG   |
               +-------------+
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Auth Module | Authentication, authorization, JWT lifecycle, email verification, password reset tokens | Spring Security config, JWT filter, token service, email service |
| User Module | User CRUD, profile management, admin operations, role management | UserService, AdminService, UserRepository, RoleRepository |
| Shared Module | Cross-cutting concerns: common DTOs, exceptions, base entities, shared events | Record DTOs, exception hierarchy, base auditable entity |
| Spring Security Filter Chain | Request interception, JWT extraction/validation, authentication context | SecurityFilterChain bean, OncePerRequestFilter for JWT |
| Database Layer | Persistence via Spring Data JPA, dual-database via profiles | H2 (dev profile), PostgreSQL (prod profile), Flyway/schema.sql for DDL |

## Recommended Project Structure

### Option: Single Maven Module with Package Conventions (Recommended)

Spring Modulith works best as a **single Maven module** with package-level boundaries. This is the idiomatic approach and avoids the complexity of multi-module Maven builds while still enforcing strict module separation at compile and test time.

```
src/main/java/
  com/example/usermanagement/
  |-- Application.java                          # @Modulithic @SpringBootApplication
  |-- package-info.java                         # (optional) root package metadata
  |
  |-- auth/                                     # === AUTH MODULE (application module) ===
  |   |-- package-info.java                     # @ApplicationModule
  |   |-- AuthController.java                   # REST: /api/auth/** endpoints
  |   |-- AuthPageController.java               # Thymeleaf: /login, /register, /forgot-password
  |   |-- AuthService.java                      # Public API: login, register, verify, reset
  |   |-- JwtService.java                       # JWT creation and validation
  |   |-- EmailService.java                     # Email sending (verification, reset links)
  |   |-- auth.internal/                        # --- INTERNAL (hidden from other modules) ---
  |   |   |-- SecurityConfig.java               # SecurityFilterChain bean
  |   |   |-- JwtAuthenticationFilter.java      # OncePerRequestFilter for JWT
  |   |   |-- CustomUserDetailsService.java     # UserDetailsService implementation
  |   |   |-- VerificationToken.java            # Entity
  |   |   |-- VerificationTokenRepository.java  # Repository
  |   |   |-- PasswordResetToken.java           # Entity
  |   |   |-- PasswordResetTokenRepository.java # Repository
  |   |   +-- AuthMapper.java                   # Internal mapping utilities
  |   |
  |   +-- auth.events/                          # --- EVENTS (public API) ---
  |       |-- UserRegisteredEvent.java          # Published when user registers
  |       +-- PasswordResetRequestedEvent.java  # Published when reset requested
  |
  |-- user/                                     # === USER MODULE (application module) ===
  |   |-- package-info.java                     # @ApplicationModule
  |   |-- UserController.java                   # REST: /api/users/** endpoints
  |   |-- UserPageController.java               # Thymeleaf: /profile, /change-password
  |   |-- AdminController.java                  # REST: /api/admin/users/** endpoints
  |   |-- AdminPageController.java              # Thymeleaf: /admin/users/**
  |   |-- UserService.java                      # Public API: get/update profile, change password
  |   |-- AdminService.java                     # Public API: CRUD users, manage roles
  |   |-- user.internal/                        # --- INTERNAL (hidden from other modules) ---
  |   |   |-- User.java                         # Entity
  |   |   |-- Role.java                         # Entity (or Enum)
  |   |   |-- UserRepository.java               # Repository
  |   |   |-- RoleRepository.java               # Repository
  |   |   +-- UserMapper.java                   # Internal mapping utilities
  |   |
  |   +-- user.events/                          # --- EVENTS (public API via NamedInterface) ---
  |       |-- UserUpdatedEvent.java             # Published when profile updated
  |       +-- UserDeletedEvent.java             # Published when user deleted
  |
  +-- shared/                                   # === SHARED MODULE ===
      |-- package-info.java                     # @ApplicationModule
      |-- dto/                                  # Public DTOs (Java records)
      |   |-- UserDto.java                      # User data exposed to other modules
      |   |-- AuthResponseDto.java              # Auth response with JWT token
      |   |-- RegistrationRequest.java          # Registration form data
      |   +-- PagedResponse.java                # Generic paged result wrapper
      |-- exception/                            # Public exception hierarchy
      |   |-- ResourceNotFoundException.java
      |   |-- DuplicateResourceException.java
      |   |-- TokenExpiredException.java
      |   +-- GlobalExceptionHandler.java       # @ControllerAdvice
      |-- entity/                               # Base entities
      |   +-- BaseEntity.java                   # Auditable base (id, createdAt, updatedAt)
      +-- config/                               # Shared configuration
          +-- AppProperties.java                # @ConfigurationProperties for app-level config
```

```
src/main/resources/
  |-- application.yml                           # Common config
  |-- application-dev.yml                       # H2 database, debug logging
  |-- application-prod.yml                      # PostgreSQL, Docker settings
  |-- templates/                                # Thymeleaf templates
  |   |-- layout.html                           # Base layout with Bootstrap 5
  |   |-- home.html
  |   |-- auth/
  |   |   |-- login.html
  |   |   |-- register.html
  |   |   |-- forgot-password.html
  |   |   |-- reset-password.html
  |   |   +-- verify-email.html
  |   |-- user/
  |   |   |-- profile.html
  |   |   +-- change-password.html
  |   +-- admin/
  |       |-- user-list.html
  |       |-- user-form.html
  |       +-- user-detail.html
  |-- static/
  |   |-- css/
  |   +-- js/
  +-- db/
      +-- migration/                            # Flyway migrations (or schema.sql / data.sql)
```

```
src/test/java/
  com/example/usermanagement/
  |-- ModularityTests.java                      # ApplicationModules.of(...).verify()
  |-- DocumentationTests.java                   # Documenter for PlantUML diagrams
  |-- auth/
  |   +-- AuthModuleIntegrationTests.java       # @ApplicationModuleTest
  |-- user/
  |   +-- UserModuleIntegrationTests.java       # @ApplicationModuleTest
  +-- shared/
      +-- SharedModuleTests.java
```

### Structure Rationale

- **Single Maven module:** Spring Modulith enforces module boundaries through its verification API (`ApplicationModules.of(App.class).verify()`) and package visibility (Java's `package-private` scope). A multi-module Maven build adds build complexity without adding enforcement power that Modulith does not already provide.
- **`auth/` at the package root:** Auth module's public API (AuthService, JwtService) is accessible to other modules. Internal details (SecurityConfig, JwtAuthenticationFilter, token repositories) live in `auth.internal/` and are invisible to other modules.
- **`auth.events/` as a NamedInterface:** Events are published types that other modules must consume. By placing them in a sub-package with `@NamedInterface`, they become an explicit public contract.
- **`user/` at the package root:** Same pattern -- public service API in the root, internal entities and repositories hidden in `user.internal/`.
- **`shared/` for DTOs and exceptions:** The Shared module contains only data-transfer objects (Java records), a base entity, and the global exception handler. It has no business logic. Other modules depend on Shared but Shared depends on nothing.
- **Thymeleaf templates organized by module:** The `templates/` folder mirrors module boundaries (auth/, user/, admin/) for clarity.

## Module Boundaries: What Belongs Where

### Auth Module

**Owns:**
- Authentication and authorization configuration (SecurityFilterChain)
- JWT token creation, validation, and refresh
- Login/logout flows (REST and Thymeleaf)
- Registration flow (accepts form data, creates user via User module event/API, sends verification email)
- Email verification token lifecycle
- Password reset token lifecycle
- Email sending (verification and reset emails)

**Exposes (public API):**
- `AuthService` -- login, register, verifyEmail, requestPasswordReset, resetPassword
- `JwtService` -- generateToken, validateToken, extractUsername
- `auth.events.UserRegisteredEvent` -- published when a new user completes registration
- `auth.events.PasswordResetRequestedEvent` -- published when a password reset is requested

**Does NOT own:**
- User entity persistence (delegates to User module)
- User profile or admin CRUD operations
- Role management

### User Module

**Owns:**
- User entity and its persistence (User, Role, UserRepository, RoleRepository)
- User profile operations (get profile, update profile, change password)
- Admin CRUD operations on users (list, create, update, delete, assign roles)
- User-related business rules (unique email constraint, password policy enforcement)

**Exposes (public API):**
- `UserService` -- getUserByEmail, getUserById, updateProfile, changePassword, createUser
- `AdminService` -- listUsers, getUserDetails, createUser, updateUser, deleteUser, assignRole
- `user.events.UserUpdatedEvent` -- published when user profile changes
- `user.events.UserDeletedEvent` -- published when user is removed

**Does NOT own:**
- Authentication logic (no login, JWT handling)
- Email sending
- Security configuration

### Shared Module

**Owns:**
- DTOs used across module boundaries (UserDto, AuthResponseDto, RegistrationRequest, PagedResponse)
- Common exception types and the GlobalExceptionHandler
- Base entity (BaseEntity with id, createdAt, updatedAt, @MappedSuperclass)
- Application-level configuration properties (@ConfigurationProperties)

**Does NOT own:**
- Any business logic
- Any repository or entity beyond BaseEntity
- Any controller or service

### The Critical Question: Where Does the User Entity Live?

**Recommendation: User entity lives in User module (user.internal.User), NOT in Shared.**

This is the most consequential boundary decision. The User entity is internal to the User module. The Auth module needs user data for authentication -- it gets this through one of two mechanisms:

1. **Direct API call (recommended for this project):** Auth module depends on User module's public `UserService.getUserByEmail()` which returns a `UserDto` (from Shared). The Auth module's `CustomUserDetailsService` calls `UserService` to load user details for Spring Security.

2. **Event-based (overkill for this project):** Auth module publishes a `UserRegistrationRequestedEvent`, User module creates the user and publishes `UserCreatedEvent`. This adds latency and complexity for a synchronous flow that should just be a method call.

**Rationale for direct calls over events for user lookup:** Spring Modulith documentation explicitly supports direct bean dependencies between modules for synchronous operations. Events are recommended for decoupling fire-and-forget side effects, not for request-response patterns like "look up this user."

## Architectural Patterns

### Pattern 1: Module Communication via Direct API + Events Hybrid

**What:** Use direct method calls for synchronous queries and commands. Use events for asynchronous side effects.

**When to use:** Always in this project. This is the Spring Modulith recommended approach.

**Trade-offs:** Simple, testable, type-safe for synchronous calls. Events provide decoupling for notifications.

**Example:**

```java
// Auth module calls User module directly for synchronous user lookup
// File: auth/internal/CustomUserDetailsService.java
@Service
@RequiredArgsConstructor
class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService; // Direct dependency on User module's public API

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserDto user = userService.getUserByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.email())
            .password(user.passwordHash())
            .roles(user.roles().toArray(String[]::new))
            .accountLocked(!user.enabled())
            .build();
    }
}
```

```java
// Auth module publishes event for asynchronous side effect
// File: auth/AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    @Transactional
    public void register(RegistrationRequest request) {
        // 1. Direct call: create the user synchronously
        UserDto created = userService.createUser(request);

        // 2. Event: trigger async side effects (send verification email)
        events.publishEvent(new UserRegisteredEvent(created.id(), created.email()));
    }
}
```

```java
// Auth module listens for its own event to send the email
// File: auth/internal/VerificationEmailListener.java
@Component
class VerificationEmailListener {

    @ApplicationModuleListener
    void on(UserRegisteredEvent event) {
        // Create verification token, send email
        // Runs async + transactional (REQUIRES_NEW) automatically
    }
}
```

### Pattern 2: Self-Issued JWT with Spring Security Resource Server

**What:** The application itself issues JWT tokens (no external authorization server). Spring Security's Resource Server support validates them.

**When to use:** When you are your own identity provider, as in this project.

**Trade-offs:** Simpler than OAuth2 authorization server. No token refresh endpoint needed for basic use. Must manage signing keys yourself.

**Example:**

```java
// File: auth/JwtService.java (PUBLIC API of Auth module)
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
            && !isTokenExpired(token);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
```

```java
// File: auth/internal/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

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

        filterChain.doFilter(request, response);
    }
}
```

### Pattern 3: Dual Endpoint Strategy (Thymeleaf + REST)

**What:** Separate controllers for page rendering (@Controller) and API (@RestController). Both share the same service layer.

**When to use:** Hybrid applications serving both server-rendered HTML and JSON API.

**Trade-offs:** More controllers, but clear separation of concerns. Each controller has its own URL namespace. API consumers get clean JSON, browser users get full HTML pages.

**Example:**

```java
// File: auth/AuthPageController.java -- Thymeleaf pages
@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";  // templates/auth/login.html
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registration", new RegistrationRequest("", "", "", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute RegistrationRequest request,
                                       BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        authService.register(request);
        return "redirect:/login?registered";
    }
}
```

```java
// File: auth/AuthController.java -- REST API
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

### Pattern 4: Spring Modulith Verification as Architecture Test

**What:** A test that runs `ApplicationModules.of(Application.class).verify()` to enforce module boundaries at build time.

**When to use:** Always. This is the primary enforcement mechanism for module boundaries.

**Trade-offs:** None -- this is free architecture enforcement.

**Example:**

```java
// File: src/test/java/.../ModularityTests.java
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void printsModuleArrangement() {
        modules.forEach(System.out::println);
    }

    @Test
    void generatesDocumentation() {
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases();
    }
}
```

### Pattern 5: Shared Module as @ApplicationModule with No Dependencies

**What:** The Shared module is annotated as an `@ApplicationModule` but declares no `allowedDependencies`. It is a leaf module that all others may depend on.

**When to use:** When you have genuine cross-cutting types (DTOs, exceptions, base entities) that multiple modules need.

**Trade-offs:** Shared modules can become dumping grounds. Keep it strictly limited to data types and infrastructure concerns. If you find business logic creeping in, it belongs in Auth or User.

**Example (package-info.java):**

```java
// File: shared/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {} // Shared depends on NOTHING
)
package com.example.usermanagement.shared;
```

```java
// File: auth/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }
)
package com.example.usermanagement.auth;
```

```java
// File: user/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared" }
)
package com.example.usermanagement.user;
```

## Data Flow

### Registration Flow

```
Browser/Client
    |
    | POST /register (form) or POST /api/auth/register (JSON)
    v
AuthPageController / AuthController
    |
    | calls
    v
AuthService.register(RegistrationRequest)
    |
    | 1. Validate input (email not taken, passwords match)
    | 2. Encode password with PasswordEncoder
    | 3. Direct call: UserService.createUser(request) --> returns UserDto
    | 4. Publish event: UserRegisteredEvent(userId, email)
    v
[Transaction commits]
    |
    | @ApplicationModuleListener (async, new transaction)
    v
VerificationEmailListener.on(UserRegisteredEvent)
    |
    | 1. Create VerificationToken (UUID, expiry)
    | 2. Save token to VerificationTokenRepository
    | 3. Send verification email via EmailService
    v
[Email sent with link: /verify-email?token=UUID]
    |
    | User clicks link
    v
GET /verify-email?token=UUID
    |
    v
AuthService.verifyEmail(token)
    |
    | 1. Look up VerificationToken
    | 2. Check not expired
    | 3. Direct call: UserService.enableUser(userId)
    | 4. Delete token
    v
[User account is now enabled]
```

### Login Flow (JWT)

```
Browser/Client
    |
    | POST /api/auth/login { email, password }
    v
AuthController.login(LoginRequest)
    |
    v
AuthService.login(LoginRequest)
    |
    | 1. AuthenticationManager.authenticate(
    |      UsernamePasswordAuthenticationToken(email, password))
    |    --> triggers CustomUserDetailsService.loadUserByUsername(email)
    |    --> calls UserService.getUserByEmail(email) (direct, synchronous)
    |    --> PasswordEncoder.matches(raw, encoded)
    |
    | 2. If authenticated: JwtService.generateToken(userDetails)
    | 3. Return AuthResponseDto(token, expiresIn, roles)
    v
{ "token": "eyJ...", "expiresIn": 3600000, "roles": ["USER"] }

--- Subsequent requests with JWT ---

HTTP Request with "Authorization: Bearer eyJ..."
    |
    v
JwtAuthenticationFilter.doFilterInternal()
    |
    | 1. Extract JWT from Authorization header
    | 2. JwtService.extractUsername(jwt) --> email
    | 3. UserDetailsService.loadUserByUsername(email)
    | 4. JwtService.isTokenValid(jwt, userDetails)
    | 5. Set SecurityContext authentication
    v
SecurityFilterChain proceeds --> Controller --> Response
```

### Password Reset Flow

```
Browser/Client
    |
    | POST /api/auth/forgot-password { email }
    v
AuthService.requestPasswordReset(email)
    |
    | 1. Verify user exists: UserService.getUserByEmail(email)
    | 2. Create PasswordResetToken (UUID, expiry)
    | 3. Save token to PasswordResetTokenRepository
    | 4. Publish PasswordResetRequestedEvent(userId, email)
    v
[Transaction commits]
    |
    | @ApplicationModuleListener (async)
    v
PasswordResetEmailListener.on(PasswordResetRequestedEvent)
    |
    | Send reset email with link: /reset-password?token=UUID
    v
[Email sent]
    |
    | User clicks link
    v
GET /reset-password?token=UUID --> shows form
    |
    | POST /reset-password { token, newPassword, confirmPassword }
    v
AuthService.resetPassword(token, newPassword)
    |
    | 1. Look up PasswordResetToken
    | 2. Check not expired
    | 3. Direct call: UserService.updatePassword(userId, encodedPassword)
    | 4. Delete token
    v
[Password updated, redirect to login]
```

### Change Password Flow (Authenticated)

```
Authenticated User
    |
    | POST /api/users/change-password { currentPassword, newPassword }
    v
UserController.changePassword(request, @AuthenticationPrincipal)
    |
    v
UserService.changePassword(userId, currentPassword, newPassword)
    |
    | 1. Load user from UserRepository
    | 2. Verify current password matches (PasswordEncoder.matches)
    | 3. Encode new password
    | 4. Save updated user
    v
[Password changed]
```

### Admin CRUD Flow

```
Admin User (ROLE_ADMIN)
    |
    | GET /api/admin/users?page=0&size=20
    v
AdminController.listUsers(pageable) --> AdminService.listUsers(pageable)
    |
    | Returns PagedResponse<UserDto>
    v
[User list with pagination]

    | POST /api/admin/users { email, password, role }
    v
AdminController.createUser(request) --> AdminService.createUser(request)
    |
    | 1. Validate email uniqueness
    | 2. Encode password
    | 3. Assign role
    | 4. Save user
    v
[User created]

    | DELETE /api/admin/users/{id}
    v
AdminController.deleteUser(id) --> AdminService.deleteUser(id)
    |
    | 1. Verify not deleting self
    | 2. Delete user
    | 3. Publish UserDeletedEvent(userId)
    v
[User deleted]
```

## Module Dependency Graph

```
              +---------+
              | shared  |   <-- depends on nothing
              +---------+
               ^       ^
              /         \
             /           \
      +------+         +------+
      | auth |-------->| user |
      +------+         +------+

Auth depends on: User (direct API calls), Shared (DTOs, exceptions)
User depends on: Shared (DTOs, exceptions, BaseEntity)
Shared depends on: (nothing)
```

**Dependency direction matters.** Auth --> User is a one-way dependency. The User module does NOT depend on Auth. If User needs to react to an Auth event (e.g., user registered), it listens to the event -- it does not import Auth classes.

### Enforced Boundaries via package-info.java

```java
// shared/package-info.java
@ApplicationModule(allowedDependencies = {})
package com.example.usermanagement.shared;

// user/package-info.java
@ApplicationModule(allowedDependencies = { "shared" })
package com.example.usermanagement.user;

// auth/package-info.java
@ApplicationModule(allowedDependencies = { "user", "shared" })
package com.example.usermanagement.auth;
```

This means:
- Auth can call User's public API and Shared's types. **Verified at test time.**
- User can use Shared's types but cannot import anything from Auth. **Verified at test time.**
- Shared cannot import anything from Auth or User. **Verified at test time.**

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | Current architecture is ideal. Single Spring Boot app, H2 or PostgreSQL, no caching needed. |
| 1k-10k users | Add Redis/Caffeine cache for JWT validation (avoid DB hit per request). Add connection pooling (HikariCP is default). Consider rate limiting on auth endpoints. |
| 10k-100k users | Extract Auth module to its own service if needed. Add Redis for session/token blacklisting. Database read replicas. CDN for static resources. |
| 100k+ users | Full microservices extraction (each Modulith module becomes a service). Dedicated auth service (consider Keycloak/Auth0). Message broker for events. |

### Scaling Priorities

1. **First bottleneck: Database queries per JWT validation.** Every request currently loads UserDetails from database. Fix: cache user details in memory (Caffeine) with short TTL (5 min), invalidate on password change.
2. **Second bottleneck: Email sending blocks registration.** Already mitigated by `@ApplicationModuleListener` which runs async. If email provider is slow, consider a queue.
3. **Third bottleneck: Admin user list pagination.** Standard Spring Data pagination handles this out of the box up to ~100k users.

## Anti-Patterns

### Anti-Pattern 1: Putting User Entity in Shared Module

**What people do:** Place `User.java` entity in the Shared module so both Auth and User can access it directly.
**Why it is wrong:** This violates the principle that the Shared module should contain only data types, not business entities. It makes Shared a God module that everything depends on for persistence. The User entity has business rules, lifecycle, and repository -- these belong in the User module.
**Do this instead:** User entity lives in `user.internal`. Auth module accesses user data via `UserService` public API which returns `UserDto` (a record in Shared).

### Anti-Pattern 2: Circular Module Dependencies

**What people do:** Auth depends on User (to load users), and User depends on Auth (to check JWT or get current user).
**Why it is wrong:** Spring Modulith verification rejects cycles. More importantly, it signals confused boundaries.
**Do this instead:** User module accesses the current authenticated user via `SecurityContextHolder.getContext().getAuthentication()` (Spring Security API), NOT by importing Auth module classes. The `@AuthenticationPrincipal` annotation in controller methods provides the current user without any Auth module dependency.

### Anti-Pattern 3: Events for Synchronous Queries

**What people do:** Auth module publishes `LoadUserRequestEvent` and listens for `LoadUserResponseEvent` to get user data.
**Why it is wrong:** Events are for fire-and-forget side effects, not request-response. This adds latency, complexity, and makes error handling painful.
**Do this instead:** Direct method call: `userService.getUserByEmail(email)`. Spring Modulith fully supports direct bean dependencies between modules for synchronous operations.

### Anti-Pattern 4: Exposing Internal Types as Public API

**What people do:** Make `User.java` entity public and let other modules use it directly.
**Why it is wrong:** Couples other modules to internal persistence representation. Any schema change breaks all consumers.
**Do this instead:** Expose `UserDto` (a record in Shared) from public service methods. Map entities to DTOs at the service boundary.

### Anti-Pattern 5: Single SecurityFilterChain for Both Thymeleaf and REST

**What people do:** Write one `SecurityFilterChain` that tries to handle both form login and JWT for all endpoints.
**Why it is wrong:** Form login returns redirects (302), JWT expects JSON errors (401). Mixing them creates confusing behavior.
**Do this instead:** Define two `SecurityFilterChain` beans with `@Order` and different `securityMatchers`:

```java
@Bean
@Order(1)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
        .securityMatcher("/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/register", "/forgot-password",
                             "/reset-password", "/verify-email", "/css/**", "/js/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/")
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
        );
    return http.build();
}
```

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| SMTP Server | JavaMailSender (Spring Boot auto-configured) | Configure via `spring.mail.*` properties. Use `.env` for credentials. Real SMTP in all environments per project constraint. |
| PostgreSQL (prod) | Spring Data JPA + HikariCP | Docker Compose service. Configure via `spring.datasource.*` in `application-prod.yml`. |
| H2 (dev) | Spring Data JPA in-memory | Auto-configured with `spring.datasource.url=jdbc:h2:mem:testdb`. Console at `/h2-console`. |
| PgAdmin (prod) | Docker Compose sidecar | No application integration needed. Connects to PostgreSQL container. |

### Internal Module Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Auth --> User | Direct method call via UserService bean | Synchronous. Auth depends on User's public API. Used for: user lookup, user creation, enable user, update password. |
| Auth --> Shared | Import DTOs and exceptions | Compile-time dependency on data types only. |
| User --> Shared | Import DTOs, exceptions, BaseEntity | Compile-time dependency. User entities extend BaseEntity. |
| Auth internal events | @ApplicationModuleListener | UserRegisteredEvent triggers verification email. PasswordResetRequestedEvent triggers reset email. Both async with REQUIRES_NEW transaction. |
| User internal events | @ApplicationModuleListener | UserDeletedEvent can trigger cleanup (e.g., revoke tokens). Async. |

## Build Order Implications

The module dependency graph dictates the natural build order for incremental development:

### Phase 1: Shared Module (foundation)
Build first because everything depends on it. Contains:
- BaseEntity (id, createdAt, updatedAt)
- DTOs (UserDto, RegistrationRequest, AuthResponseDto, PagedResponse, LoginRequest)
- Exception hierarchy (ResourceNotFoundException, DuplicateResourceException, TokenExpiredException)
- GlobalExceptionHandler (@ControllerAdvice)
- AppProperties (@ConfigurationProperties)

**Rationale:** No dependencies. Pure data types. Quick to build. Unblocks all other work.

### Phase 2: User Module (data layer)
Build second because Auth needs UserService:
- User entity, Role entity (extending BaseEntity)
- UserRepository, RoleRepository
- UserService (CRUD, profile, password change)
- AdminService (admin CRUD, role management)
- UserController, UserPageController, AdminController, AdminPageController

**Rationale:** Provides the UserService API that Auth module needs. Can be tested independently with `@ApplicationModuleTest`.

### Phase 3: Auth Module (security layer)
Build third because it depends on both Shared and User:
- SecurityConfig (dual SecurityFilterChain)
- JwtService (token generation/validation)
- JwtAuthenticationFilter
- CustomUserDetailsService (calls UserService)
- AuthService (login, register, verify, reset)
- EmailService
- Token entities and repositories (VerificationToken, PasswordResetToken)
- AuthController, AuthPageController

**Rationale:** Requires UserService to be available. Most complex module with security, JWT, email, and token lifecycle.

### Phase 4: Integration and verification
- ModularityTests (ApplicationModules.verify())
- DocumentationTests (Documenter)
- Integration tests per module (@ApplicationModuleTest)
- Docker Compose setup
- Shell scripts in ./bin/

## Spring Modulith Conventions Summary

| Convention | Our Application |
|------------|-----------------|
| Module = direct sub-package of main package | `auth/`, `user/`, `shared/` under `com.example.usermanagement` |
| Public types in module root = module API | `AuthService`, `UserService`, DTOs in Shared root |
| Sub-packages = internal by default | `auth.internal/`, `user.internal/` hidden from other modules |
| `@ApplicationModule` on package-info.java | Yes, with `allowedDependencies` for each module |
| `@NamedInterface` for additional public sub-packages | `auth.events/`, `user.events/` expose event types |
| `@ApplicationModuleListener` for async event handling | Used for email sending after registration and password reset |
| `ApplicationModules.verify()` in tests | Yes, in `ModularityTests.java` |
| `@ApplicationModuleTest` for isolated module testing | Yes, one per module |
| `@Modulithic` on main application class | Yes, on `Application.java` |
| Single Maven module, not multi-module | Yes -- package conventions enforce boundaries, not Maven modules |

## Spring Modulith Starters Needed

| Starter | Purpose | Scope |
|---------|---------|-------|
| `spring-modulith-starter-core` | Core Modulith support (API, runtime) | compile |
| `spring-modulith-starter-jpa` | Event publication registry with JPA persistence | compile |
| `spring-modulith-starter-test` | Testing and documentation support | test |

Maven BOM:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>2.0.2</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Note on Spring Boot 4 compatibility:** Spring Modulith 2.0 is the version compatible with Spring Boot 4.0. The current stable is 2.0.2. The snapshot versions track Spring Boot 4.0 SNAPSHOT. Verify the exact compatible release version at build time, as Spring Boot 4 may still be in milestone/RC at time of implementation.

## Sources

- Spring Modulith Reference: Fundamentals -- https://docs.spring.io/spring-modulith/reference/fundamentals.html [HIGH confidence, official docs]
- Spring Modulith Reference: Events -- https://docs.spring.io/spring-modulith/reference/events.html [HIGH confidence, official docs]
- Spring Modulith Reference: Verification -- https://docs.spring.io/spring-modulith/reference/verification.html [HIGH confidence, official docs]
- Spring Modulith Reference: Testing -- https://docs.spring.io/spring-modulith/reference/testing.html [HIGH confidence, official docs]
- Spring Modulith Reference: Documentation -- https://docs.spring.io/spring-modulith/reference/documentation.html [HIGH confidence, official docs]
- Spring Modulith Reference: Appendix (compatibility matrix) -- https://docs.spring.io/spring-modulith/reference/appendix.html [HIGH confidence, official docs]
- Spring Security Reference: Authentication Architecture -- https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html [HIGH confidence, official docs]
- Spring Security Reference: JWT Resource Server -- https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html [HIGH confidence, official docs]
- Spring Security Reference: Authorize HTTP Requests -- https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html [HIGH confidence, official docs]
- Spring Boot Reference: Web Security -- https://docs.spring.io/spring-boot/reference/web/spring-security.html [HIGH confidence, official docs]
- Spring Boot Reference: Web Servlet -- https://docs.spring.io/spring-boot/reference/web/servlet.html [HIGH confidence, official docs]
- Self-issued JWT pattern (JwtService + JwtAuthenticationFilter): Based on standard Spring Security patterns. Spring Security does not provide built-in JWT generation for self-issued tokens -- manual implementation with io.jsonwebtoken (JJWT) library is the community standard. [MEDIUM confidence -- community pattern, not official Spring feature]

---
*Architecture research for: Spring Boot 4 + Spring Modulith User Management Server*
*Researched: 2026-01-28*
