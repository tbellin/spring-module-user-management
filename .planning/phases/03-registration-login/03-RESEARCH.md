# Phase 3: Registration & Login - Research

**Researched:** 2026-02-03
**Domain:** Spring Security form login, REST auth endpoints, Thymeleaf forms, JWT authentication, Bootstrap 5 UI
**Confidence:** HIGH (building on Phase 2 security infrastructure, well-documented patterns)

## Summary

Phase 3 implements user registration and login flows through both Thymeleaf web pages and REST API endpoints. The foundation from Phase 2 (SecurityConfig with dual filter chains, JwtService, UserService, CustomUserDetailsService, BCryptPasswordEncoder) is already in place. This phase adds the controllers, DTOs, Thymeleaf templates, and UI logic needed for user-facing authentication.

The key architectural decision is maintaining clean separation between web and API authentication flows. The web flow uses Spring Security's built-in form login with session-based authentication (for Thymeleaf pages), while the API flow uses the existing stateless JWT infrastructure. Both flows share the same `UserService` for user creation and `AuthenticationManager` for credential validation.

Per the CONTEXT.md decisions: registration collects email, password, and display name; uses a single password field with show/hide toggle; auto-logs in after registration; includes "remember me" for extended sessions; and displays validation errors in a summary at top of form with toast notifications for success feedback.

**Primary recommendation:** Create an AuthService in the auth module that encapsulates registration and login logic, exposing it through an AuthController (REST) and AuthWebController (Thymeleaf). Use Spring Security's built-in `SavedRequestAwareAuthenticationSuccessHandler` for redirect-to-original-URL behavior. Implement toast notifications using Bootstrap 5's native toast component with JavaScript initialization.

## Standard Stack

### Core (Already in Place from Phase 2)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Spring Security | 7.0.x | Authentication, form login, CSRF | In SecurityConfig |
| JJWT | 0.12.6 | JWT token creation for API | In JwtService |
| BCryptPasswordEncoder | Built-in | Password hashing | In PasswordConfig |
| spring-boot-starter-validation | Managed | Bean validation (@NotBlank, @Email, @Size) | In pom.xml |

### Supporting (To Be Used in Phase 3)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Thymeleaf | Managed by Boot 4 | Server-side HTML rendering | Login, register, logout pages |
| thymeleaf-extras-springsecurity6 | In pom.xml | `sec:authorize`, CSRF auto-injection | Conditional UI based on auth state |
| Bootstrap 5.3.3 | WebJar | CSS framework, toast component | Form styling, toast notifications |
| thymeleaf-layout-dialect | In pom.xml | Template inheritance | Consistent page layout |

### New DTOs Required

| DTO | Location | Purpose |
|-----|----------|---------|
| `RegistrationRequest` | `auth.internal` | API: email, password, displayName with validation |
| `LoginRequest` | `auth.internal` | API: email, password, rememberMe |
| `AuthResponse` | `auth` | API: JWT token, user info, expiration |
| `RegistrationForm` | `auth.internal` | Web: backing bean for Thymeleaf form |
| `LoginForm` | `auth.internal` | Web: backing bean for Thymeleaf form |

**No additional dependencies required.** All necessary libraries are already in pom.xml.

## Architecture Patterns

### Recommended Project Structure (Phase 3 additions)

```
src/main/java/com/example/usermanagement/
|-- auth/
|   |-- JwtService.java                    # EXISTING: JWT operations
|   |-- AuthResponse.java                  # NEW: API response DTO (public)
|   |-- auth.internal/
|   |   |-- SecurityConfig.java            # EXISTING: Dual filter chains
|   |   |-- AuthService.java               # NEW: Registration/login business logic
|   |   |-- AuthController.java            # NEW: REST /api/v1/auth/* endpoints
|   |   |-- AuthWebController.java         # NEW: Thymeleaf /login, /register pages
|   |   |-- RegistrationRequest.java       # NEW: API registration DTO
|   |   |-- LoginRequest.java              # NEW: API login DTO
|   |   |-- RegistrationForm.java          # NEW: Web form backing bean
|   |   |-- LoginForm.java                 # NEW: Web form backing bean
|   |   +-- CustomUserDetailsService.java  # EXISTING
|
|-- shared/config/
|   |-- AppProperties.java                 # UPDATE: Add remember-me duration config
|
src/main/resources/templates/
|-- layout/default.html                    # UPDATE: Add toast container, auth-aware nav
|-- auth/
|   |-- login.html                         # NEW: Login page
|   |-- register.html                      # NEW: Registration page
|-- fragments/
|   |-- toast.html                         # NEW: Toast notification fragment
```

### Pattern 1: Dual Controller Architecture (Web + API)

**What:** Separate controllers for Thymeleaf pages (`@Controller`) and REST API (`@RestController`), both delegating to a shared `AuthService`.

**When to use:** When supporting both server-rendered pages and API clients.

**Why:**
- Web controller returns view names, handles redirects, populates Model
- API controller returns JSON, handles request/response bodies
- Business logic stays in AuthService, avoiding duplication

**Example:**

```java
// Source: Spring Boot best practices, verified pattern
// auth/internal/AuthService.java - Shared business logic
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserDto registerUser(String email, String password, String displayName) {
        // Check for existing user (throw DuplicateResourceException)
        // Hash password
        // Create user with UserService
        // Return created user
    }

    public Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
    }

    public String generateToken(Authentication auth, boolean rememberMe) {
        // Generate JWT with appropriate expiration
    }
}

// auth/internal/AuthController.java - REST API
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserDto user = authService.registerUser(request.email(), request.password(), request.displayName());
        Authentication auth = authService.authenticate(request.email(), request.password());
        String token = jwtService.generateToken((UserDetails) auth.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponse(token, user.email(), user.username(), /* roles */));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authService.authenticate(request.email(), request.password());
        String token = authService.generateToken(auth, request.rememberMe());
        // Return token + user info
    }
}

// auth/internal/AuthWebController.java - Thymeleaf pages
@Controller
public class AuthWebController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute RegistrationForm form,
                                      BindingResult result,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.registerUser(form.getEmail(), form.getPassword(), form.getDisplayName());
            // Auto-login after registration
            request.login(form.getEmail(), form.getPassword());
            redirectAttributes.addFlashAttribute("toast",
                new Toast("success", "Registration successful! Welcome."));
            return "redirect:/";
        } catch (DuplicateResourceException e) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
            return "auth/register";
        }
    }
}
```

### Pattern 2: SavedRequest Redirect (Original URL After Login)

**What:** Spring Security's built-in mechanism to redirect users to the page they originally requested before being redirected to login.

**When to use:** For web form login when users access a protected page while unauthenticated.

**How it works:**
1. User requests `/dashboard` (protected)
2. `ExceptionTranslationFilter` saves the request to `HttpSessionRequestCache`
3. User is redirected to `/login`
4. After successful login, `SavedRequestAwareAuthenticationSuccessHandler` reads the saved request
5. User is redirected to `/dashboard`

**Configuration (already in SecurityConfig):**

```java
// The default form login configuration already uses SavedRequestAwareAuthenticationSuccessHandler
.formLogin(form -> form
    .loginPage("/login")
    .defaultSuccessUrl("/")  // Fallback if no saved request
    .permitAll())
```

**Important:** `defaultSuccessUrl("/")` is the fallback when no saved request exists (e.g., user navigates directly to login page).

### Pattern 3: Remember-Me with Extended Token Duration

**What:** Checkbox on login form that extends session/token validity.

**Decision from CONTEXT.md:** Include "Remember me" checkbox. Duration is Claude's discretion.

**Recommended durations:**
- Standard login: 1 hour (current JWT expiration)
- Remember me: 7 days (industry standard)

**Web approach (session-based):** Use Spring Security's built-in remember-me with cookie.

```java
// In SecurityConfig webFilterChain
.rememberMe(remember -> remember
    .key("unique-and-secret")  // Fixed key survives restarts
    .tokenValiditySeconds(7 * 24 * 60 * 60)  // 7 days
    .rememberMeParameter("remember-me")  // Form field name
)
```

**API approach (JWT-based):** Generate token with extended expiration.

```java
// In JwtService - add overload
public String generateToken(UserDetails userDetails, boolean rememberMe) {
    long expiration = rememberMe ? rememberMeExpirationMs : expirationMs;
    return Jwts.builder()
        .subject(userDetails.getUsername())
        // ... claims
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(signingKey)
        .compact();
}
```

**AppProperties update:**

```java
public record Jwt(String secret, long expirationMs, long rememberMeExpirationMs) {
}
```

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:}
    expiration-ms: ${JWT_EXPIRATION_MS:3600000}  # 1 hour
    remember-me-expiration-ms: ${JWT_REMEMBER_ME_EXPIRATION_MS:604800000}  # 7 days
```

### Pattern 4: Thymeleaf Form with CSRF and Validation

**What:** Thymeleaf form with automatic CSRF token injection, bean validation, and Bootstrap 5 styling.

**CSRF handling:** Thymeleaf + Spring Security automatically injects CSRF token into forms using `th:action`. No manual hidden field needed.

**Validation display (per CONTEXT.md):** Errors displayed in summary at top of form.

**Example:**

```html
<!-- auth/register.html -->
<form th:action="@{/register}" th:object="${registrationForm}" method="post" novalidate>
    <!-- CSRF token auto-injected by Thymeleaf -->

    <!-- Error summary at top (per CONTEXT.md decision) -->
    <div th:if="${#fields.hasErrors('*')}" class="alert alert-danger">
        <ul class="mb-0">
            <li th:each="err : ${#fields.errors('*')}" th:text="${err}"></li>
        </ul>
    </div>

    <div class="mb-3">
        <label for="email" class="form-label">Email address</label>
        <input type="email" class="form-control" th:field="*{email}" id="email"
               th:classappend="${#fields.hasErrors('email')} ? 'is-invalid'" required>
    </div>

    <div class="mb-3">
        <label for="displayName" class="form-label">Display Name</label>
        <input type="text" class="form-control" th:field="*{displayName}" id="displayName"
               th:classappend="${#fields.hasErrors('displayName')} ? 'is-invalid'" required>
    </div>

    <div class="mb-3">
        <label for="password" class="form-label">Password</label>
        <div class="input-group">
            <input type="password" class="form-control" th:field="*{password}" id="password"
                   th:classappend="${#fields.hasErrors('password')} ? 'is-invalid'"
                   minlength="8" required>
            <button class="btn btn-outline-secondary" type="button" id="togglePassword">
                <i class="bi bi-eye"></i>
            </button>
        </div>
        <div class="form-text">Minimum 8 characters</div>
    </div>

    <button type="submit" class="btn btn-primary">Register</button>
</form>
```

### Pattern 5: Toast Notifications with Bootstrap 5

**What:** Auto-dismissing success/error notifications using Bootstrap 5's toast component.

**Per CONTEXT.md:** Success feedback via toast notifications (auto-dismissing). Styling and timing at Claude's discretion.

**Recommended:** 5 seconds auto-dismiss (Bootstrap default), positioned top-right.

**Layout template addition:**

```html
<!-- In layout/default.html, before closing </body> -->
<div class="toast-container position-fixed top-0 end-0 p-3">
    <div th:if="${toast}" class="toast show" role="alert" data-bs-autohide="true" data-bs-delay="5000">
        <div class="toast-header"
             th:classappend="${toast.type == 'success'} ? 'bg-success text-white' : 'bg-danger text-white'">
            <strong class="me-auto" th:text="${toast.title}">Notification</strong>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
        </div>
        <div class="toast-body" th:text="${toast.message}"></div>
    </div>
</div>

<script>
    // Initialize all toasts on page load
    document.addEventListener('DOMContentLoaded', function() {
        var toastElList = [].slice.call(document.querySelectorAll('.toast'));
        toastElList.map(function(toastEl) {
            return new bootstrap.Toast(toastEl);
        });
    });
</script>
```

**Toast DTO:**

```java
public record Toast(String type, String title, String message) {
    public Toast(String type, String message) {
        this(type, type.equals("success") ? "Success" : "Error", message);
    }
}
```

### Pattern 6: Password Show/Hide Toggle

**What:** Eye icon button to toggle password visibility.

**Per CONTEXT.md:** Single password field with show/hide toggle. Implementation at Claude's discretion.

**Recommended:** Bootstrap input-group with eye icon toggle using vanilla JavaScript.

```html
<div class="input-group">
    <input type="password" class="form-control" id="password" name="password">
    <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility()">
        <svg id="eyeIcon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
            <!-- Bootstrap Icons eye SVG path -->
        </svg>
    </button>
</div>

<script>
function togglePasswordVisibility() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        // Switch to eye-slash icon
    } else {
        passwordInput.type = 'password';
        // Switch to eye icon
    }
}
</script>
```

**Alternative:** Use Bootstrap Icons (already included via WebJar) for eye/eye-slash icons.

### Anti-Patterns to Avoid

- **Separate AuthService for web vs API:** Business logic should be shared. Only the controller layer differs.
- **Manual CSRF token in forms:** Thymeleaf + Spring Security handles this automatically with `th:action`.
- **Detailed auth error messages:** "Email not found" vs "Wrong password" enables user enumeration. Always use generic "Bad credentials" (already implemented in CustomUserDetailsService).
- **Storing plain passwords:** Always use BCryptPasswordEncoder (already configured).
- **JWT in cookies for API:** JWT should be returned in response body, stored client-side, sent via Authorization header. Cookies are for session-based web auth.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| CSRF token injection | Manual hidden fields | Thymeleaf `th:action` + Spring Security | Automatic, secure, less error-prone |
| Post-login redirect | Custom redirect logic | `SavedRequestAwareAuthenticationSuccessHandler` | Built-in, handles edge cases |
| Remember-me cookies | Custom cookie management | Spring Security remember-me | Secure token generation, expiration handling |
| Form validation | Custom if/else checks | `@Valid` + Bean Validation + `BindingResult` | Standardized, declarative, i18n-ready |
| Toast notifications | Custom notification system | Bootstrap 5 Toast component | Styled, accessible, configurable |
| Password hashing | Custom hash function | `BCryptPasswordEncoder` (already configured) | Secure, salted, configurable strength |

**Key insight:** The Spring Security + Thymeleaf integration handles most security concerns automatically. The main work is creating the controller methods, DTOs, and templates.

## Common Pitfalls

### Pitfall 1: Forgetting to Permit Auth Endpoints

**What goes wrong:** Login/register pages return 403 or redirect to login (infinite loop).

**Why it happens:** SecurityConfig doesn't permit `/login`, `/register`, or `/api/v1/auth/**` endpoints.

**How to avoid:** Verify SecurityConfig permits these paths:

```java
// Web chain
.requestMatchers("/login", "/register").permitAll()

// API chain (already in Phase 2 config)
.requestMatchers("/api/auth/**").permitAll()
```

**Note:** Current SecurityConfig already has `/login`, `/register` permitted. Update API path to `/api/v1/auth/**` to match versioned API.

**Warning signs:** 302 redirect loops, 403 on auth pages.

### Pitfall 2: BindingResult Not Immediately After @Valid

**What goes wrong:** Validation errors throw `MethodArgumentNotValidException` instead of populating `BindingResult`.

**Why it happens:** Spring requires `BindingResult` to immediately follow the `@Valid` parameter.

**How to avoid:**

```java
// CORRECT
public String register(@Valid @ModelAttribute RegistrationForm form, BindingResult result)

// WRONG - BindingResult not immediately after
public String register(@Valid @ModelAttribute RegistrationForm form, Model model, BindingResult result)
```

**Warning signs:** `@ExceptionHandler` catching validation errors instead of controller handling them.

### Pitfall 3: POST Without CSRF in Tests

**What goes wrong:** MockMvc POST tests return 403 for Thymeleaf form endpoints.

**Why it happens:** Web filter chain has CSRF enabled (correctly). Tests must include CSRF token.

**How to avoid:**

```java
mockMvc.perform(post("/register")
    .with(csrf())  // REQUIRED for web chain
    .param("email", "test@example.com")
    .param("password", "password123")
    .param("displayName", "Test User"))
    .andExpect(status().is3xxRedirection());
```

**Warning signs:** All POST tests failing with 403, GET tests passing.

### Pitfall 4: Auto-Login After Registration Race Condition

**What goes wrong:** User created but `request.login()` fails because user not yet committed to database.

**Why it happens:** `UserService.createUser()` transaction not committed before Spring Security queries for authentication.

**How to avoid:** Either:
1. Use `@Transactional(propagation = REQUIRES_NEW)` for user creation
2. Manually authenticate instead of using `request.login()`:

```java
// Preferred approach - manual authentication
Authentication auth = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
SecurityContextHolder.getContext().setAuthentication(auth);
```

**Warning signs:** Intermittent "Bad credentials" errors immediately after registration.

### Pitfall 5: Missing Form Backing Bean in GET Handler

**What goes wrong:** Thymeleaf template fails with "Neither BindingResult nor plain target object for bean name 'registrationForm' available."

**Why it happens:** GET handler doesn't add the form object to the model.

**How to avoid:**

```java
@GetMapping("/register")
public String showForm(Model model) {
    model.addAttribute("registrationForm", new RegistrationForm());  // REQUIRED
    return "auth/register";
}
```

**Warning signs:** Template parsing errors mentioning missing bean.

### Pitfall 6: Display Name vs Username Confusion

**What goes wrong:** Registration fails because `displayName` is not mapped to `username` in the database.

**Why it happens:** CONTEXT.md says "collect display name" but database has `username` column.

**How to avoid:**
- Use `displayName` in UI (what user sees)
- Map to `username` in database (for URL-safe identifiers)
- Or use email as username and `displayName` as `first_name` (simpler)

**Recommended approach:** Use email as the unique identifier for login, store displayName as first_name (or add a display_name column in a future migration). For Phase 3, map displayName to username field.

```java
// In AuthService
userService.createUser(
    email,           // email
    email,           // username (use email as username for simplicity)
    passwordHash,    // passwordHash
    displayName,     // firstName (display name)
    null             // lastName
);
```

## Code Examples

### RegistrationRequest DTO with Validation

```java
// Source: Jakarta Bean Validation + Spring Boot patterns
package com.example.usermanagement.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    String displayName
) {}
```

### LoginRequest DTO

```java
package com.example.usermanagement.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    boolean rememberMe
) {}
```

### AuthResponse DTO (Public API)

```java
// Source: Standard JWT auth response pattern
package com.example.usermanagement.auth;

import java.util.Set;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresIn,
    String email,
    String displayName,
    Set<String> roles
) {
    public AuthResponse(String token, long expiresIn, String email, String displayName, Set<String> roles) {
        this(token, "Bearer", expiresIn, email, displayName, roles);
    }
}
```

### RegistrationForm (Web Form Backing Bean)

```java
// Mutable class required for Thymeleaf form binding
package com.example.usermanagement.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    private String displayName;

    // Getters and setters required for Thymeleaf binding
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
```

### Login Page Template

```html
<!-- templates/auth/login.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/default}">
<head>
    <title>Login</title>
</head>
<body>
    <div layout:fragment="content">
        <div class="row justify-content-center">
            <div class="col-md-6 col-lg-4">
                <div class="card">
                    <div class="card-body">
                        <h2 class="card-title text-center mb-4">Login</h2>

                        <!-- Error message (from Spring Security) -->
                        <div th:if="${param.error}" class="alert alert-danger">
                            Invalid email or password.
                        </div>

                        <!-- Logout message -->
                        <div th:if="${param.logout}" class="alert alert-info">
                            You have been logged out.
                        </div>

                        <!-- Login form - Spring Security handles POST to /login -->
                        <form th:action="@{/login}" method="post">
                            <!-- CSRF auto-injected -->

                            <div class="mb-3">
                                <label for="username" class="form-label">Email address</label>
                                <input type="email" class="form-control" id="username" name="username"
                                       required autofocus>
                            </div>

                            <div class="mb-3">
                                <label for="password" class="form-label">Password</label>
                                <div class="input-group">
                                    <input type="password" class="form-control" id="password" name="password" required>
                                    <button class="btn btn-outline-secondary" type="button"
                                            onclick="togglePassword('password', this)">
                                        <span class="eye-icon">Show</span>
                                    </button>
                                </div>
                            </div>

                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input" id="remember-me" name="remember-me">
                                <label class="form-check-label" for="remember-me">Remember me</label>
                            </div>

                            <button type="submit" class="btn btn-primary w-100">Sign In</button>
                        </form>

                        <p class="text-center mt-3">
                            Don't have an account? <a th:href="@{/register}">Register</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <th:block layout:fragment="scripts">
        <script th:inline="javascript">
            function togglePassword(inputId, button) {
                const input = document.getElementById(inputId);
                const icon = button.querySelector('.eye-icon');
                if (input.type === 'password') {
                    input.type = 'text';
                    icon.textContent = 'Hide';
                } else {
                    input.type = 'password';
                    icon.textContent = 'Show';
                }
            }
        </script>
    </th:block>
</body>
</html>
```

### SecurityConfig Updates for Remember-Me

```java
// In SecurityConfig.webFilterChain() - add remember-me configuration
.rememberMe(remember -> remember
    .key("${app.security.remember-me-key}")  // From config, fixed for restart persistence
    .tokenValiditySeconds(7 * 24 * 60 * 60)  // 7 days
    .rememberMeParameter("remember-me")       // Form checkbox name
    .userDetailsService(userDetailsService))
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` @Bean | Spring Security 5.7 | Already using new approach |
| `http.formLogin().loginPage()` | Lambda DSL `.formLogin(form -> form.loginPage())` | Spring Security 5.2+ | Already using lambda DSL |
| JSP/JSTL | Thymeleaf | Modern Spring Boot | Already configured |
| jQuery-based form validation | HTML5 + vanilla JS | Modern web standards | Simpler, fewer dependencies |

**Current best practices:**
- Server-side validation (Bean Validation) as primary defense
- Client-side validation for UX only (not security)
- Progressive enhancement (forms work without JavaScript)

## Open Questions

1. **Bootstrap Icons for password toggle**
   - What we know: Bootstrap 5 WebJar is included, but Bootstrap Icons is a separate package
   - What's unclear: Whether to add Bootstrap Icons WebJar or use inline SVG/text
   - Recommendation: Use simple text labels ("Show"/"Hide") or inline SVG to avoid adding another dependency. Bootstrap Icons WebJar can be added in a future enhancement.

2. **Display name storage**
   - What we know: CONTEXT.md says "collect display name", database has `username`, `first_name`, `last_name` columns
   - What's unclear: Which column should store the display name
   - Recommendation: Store display name in `first_name` column. Use email as the unique login identifier (already the case). Consider adding a dedicated `display_name` column in a future migration if needed.

3. **API versioning path**
   - What we know: Phase requirements reference `/api/v1/auth/*`, but SecurityConfig currently permits `/api/auth/**`
   - What's unclear: Whether to add `/v1/` versioning now
   - Recommendation: Use `/api/v1/auth/*` for new endpoints. Update SecurityConfig to permit both `/api/auth/**` and `/api/v1/auth/**` for forward compatibility.

## Sources

### Primary (HIGH confidence)

- [Spring Security Remember-Me Documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html) - Token configuration, cookie handling
- [Spring Security Form Login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html) - Login page configuration, default success URL
- [Thymeleaf + Spring Security Integration](https://www.baeldung.com/csrf-thymeleaf-with-spring-security) - CSRF auto-injection, sec: attributes
- [Bootstrap 5 Toast Documentation](https://getbootstrap.com/docs/5.3/components/toasts/) - Auto-hide, delay, JavaScript API
- [Bean Validation in Spring Boot](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html) - @Valid, BindingResult patterns

### Secondary (MEDIUM confidence)

- [Baeldung: Post-Login Redirect](https://www.baeldung.com/spring-security-redirect-login) - SavedRequestAwareAuthenticationSuccessHandler patterns
- [Baeldung: Thymeleaf Error Messages](https://www.baeldung.com/spring-thymeleaf-error-messages) - #fields.errors, th:errors usage
- [Bootstrap Show Password Toggle](https://github.com/coliff/bootstrap-show-password-toggle) - Eye icon toggle patterns

### Tertiary (LOW confidence)

- Password toggle implementations vary; simple text labels are reliable across all browsers

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All dependencies already in place from Phase 2
- Architecture patterns: HIGH - Standard Spring Security + Thymeleaf patterns
- Pitfalls: HIGH - Common issues well-documented
- Code examples: HIGH - Verified against existing codebase structure

**Research date:** 2026-02-03
**Valid until:** 2026-03-03 (stable domain, patterns unlikely to change)
