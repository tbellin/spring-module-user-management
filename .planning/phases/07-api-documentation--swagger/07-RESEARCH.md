# Phase 7: API Documentation & Swagger - Research

**Researched:** 2026-02-14
**Domain:** SpringDoc OpenAPI 3 with Spring Boot 4, Swagger UI, JWT Bearer Auth
**Confidence:** MEDIUM (Jackson 3 compatibility verified via demos but active issues exist)

## Summary

Phase 7 integrates SpringDoc OpenAPI to auto-generate Swagger UI documentation for all existing REST API endpoints. The project already has a complete set of REST controllers under `/api/v1/` -- no new endpoints need to be created. The work is: (1) add the SpringDoc dependency, (2) configure security to permit Swagger UI paths, (3) add an OpenAPI configuration bean with JWT bearer scheme, (4) add OpenAPI annotations to controllers and DTOs for rich documentation, and (5) verify everything works.

The critical blocker noted in STATE.md ("SpringDoc OpenAPI compatibility with Spring Boot 4 must be verified") is now resolved. SpringDoc 3.0.1 is the correct version for Spring Boot 4.0.1. The official springdoc-openapi-demos repository has a working 4.x branch using Spring Boot 4.0.1 + springdoc 3.0.1. However, there is an active Jackson 3 compatibility issue (#3200) where swagger-core internally still depends on Jackson 2. The workaround is to add `spring-boot-jackson2` if startup fails, but the official demos run WITHOUT it -- so it may only affect specific configurations. This needs validation during implementation.

**Primary recommendation:** Use `springdoc-openapi-starter-webmvc-ui:3.0.1`, filter docs to `/api/v1/**` paths only via `springdoc.pathsToMatch`, configure JWT bearer security scheme globally, and add `@Tag`/`@Operation`/`@Schema` annotations to controllers and DTOs for documentation richness. If Jackson 3 startup errors occur, add `spring-boot-jackson2` as a fallback.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| springdoc-openapi-starter-webmvc-ui | 3.0.1 | Swagger UI + OpenAPI spec generation | The only maintained OpenAPI library for Spring Boot. v3.x is the Spring Boot 4 compatible line. Auto-generates OpenAPI 3.1 spec from Spring MVC annotations. Includes Swagger UI. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-jackson2 | Managed by Boot 4 | Jackson 2 backward compat | ONLY if springdoc startup fails with ClassNotFoundException for Jackson 2 classes. Official demos work without it. Deprecated module -- stopgap only. |
| io.swagger.core.v3:swagger-annotations | Transitive (via springdoc) | `@Tag`, `@Operation`, `@Schema`, `@SecurityRequirement` annotations | Always -- these annotations enrich the auto-generated docs |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| springdoc-openapi | Hand-written OpenAPI YAML + static Swagger UI | Full control over spec but enormous maintenance burden, no auto-sync with code |
| springdoc-openapi | Springfox | Springfox is abandoned since 2020. Does not support Spring Boot 3+, let alone 4. Not an option. |

**Installation (Maven):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.1</version>
</dependency>
```

## Architecture Patterns

### Recommended Configuration Structure
```
src/main/java/.../shared/config/
    OpenApiConfig.java        # OpenAPI bean with info + JWT security scheme
src/main/java/.../auth/internal/
    AuthController.java       # Add @Tag, @Operation annotations
    AdminController.java      # Add @Tag, @Operation annotations
    password/
        PasswordController.java  # Add @Tag, @Operation annotations
src/main/java/.../user/internal/
    ProfileController.java    # Add @Tag, @Operation annotations
src/main/resources/
    application.yml           # springdoc.* properties
```

### Pattern 1: OpenAPI Configuration Bean
**What:** A `@Configuration` class that defines the OpenAPI metadata and JWT security scheme.
**When to use:** Always -- needed for API info, JWT bearer auth in Swagger UI.
**Example:**
```java
// Source: springdoc.org + verified from multiple sources
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User Management API")
                .version("1.0.0")
                .description("REST API for user management, authentication, and administration"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token obtained from /api/v1/auth/login")));
    }
}
```

### Pattern 2: Path Filtering via Properties
**What:** Use `springdoc.pathsToMatch` to include only API endpoints, excluding Thymeleaf web controllers.
**When to use:** When project has both REST and web (Thymeleaf) controllers -- exactly this project's case.
**Example (application.yml):**
```yaml
springdoc:
  pathsToMatch: /api/v1/**
  swagger-ui:
    path: /swagger-ui.html
    display-request-duration: true
    operationsSorter: method
```

### Pattern 3: Controller-Level @Tag Annotations
**What:** Group endpoints by controller using `@Tag` on the class.
**When to use:** Always -- provides logical grouping in Swagger UI navigation.
**Example:**
```java
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and verification")
public class AuthController { ... }
```

### Pattern 4: Method-Level @Operation Annotations
**What:** Add summary and description to each endpoint method.
**When to use:** For every public API endpoint.
**Example:**
```java
@Operation(
    summary = "Authenticate user",
    description = "Validates credentials and returns a JWT token for API access"
)
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { ... }
```

### Pattern 5: SecurityFilterChain Update for Swagger UI Paths
**What:** Permit Swagger UI and OpenAPI spec paths in the web SecurityFilterChain.
**When to use:** Always -- without this, Swagger UI returns 401/302.
**Critical note:** Swagger UI paths (`/swagger-ui/**`, `/v3/api-docs/**`) should be permitted in the web filter chain (Order 2), NOT the API filter chain (Order 1), because these are NOT under `/api/**`.
**Example:**
```java
// In SecurityConfig, web chain:
.requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                 "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
```

### Pattern 6: @Schema Annotations on DTOs
**What:** Add descriptions and examples to request/response DTOs.
**When to use:** For all DTOs that appear in the API spec -- makes Swagger UI self-documenting.
**Example:**
```java
@Schema(description = "Login credentials")
public record LoginRequest(
    @Schema(description = "User's email address", example = "user@example.com")
    @NotBlank @Email String email,

    @Schema(description = "User's password", example = "password123")
    @NotBlank String password,

    @Schema(description = "Extended token expiration", example = "false")
    boolean rememberMe
) {}
```

### Anti-Patterns to Avoid
- **Documenting web controllers in Swagger:** The Thymeleaf controllers (AuthWebController, PasswordWebController, etc.) return HTML, not JSON. Use `springdoc.pathsToMatch=/api/v1/**` to exclude them automatically. Do NOT add @Tag/@Operation to web controllers.
- **Manually writing OpenAPI YAML:** SpringDoc auto-generates the spec from code. Hand-written specs go stale instantly.
- **Adding @SecurityRequirement per-method for all protected endpoints:** Use global security via `addSecurityItem()` on the OpenAPI bean instead. Public endpoints can override with `@SecurityRequirement` (empty) to remove the lock icon.
- **Disabling Swagger UI in production via profile-specific properties:** This is a documentation phase -- leave it enabled everywhere for now (Phase 8 can address prod hardening if needed).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| API documentation | Hand-written OpenAPI YAML or Markdown API docs | springdoc-openapi auto-generation | Stays in sync with code automatically; annotations are the single source of truth |
| Swagger UI hosting | Static HTML deployment of Swagger UI | springdoc-openapi-starter-webmvc-ui | Auto-deployed, auto-wired to generated spec, includes "Try it out" functionality |
| JWT auth in Swagger UI | Custom auth interceptor or login form | SecurityScheme bean config | Standard OpenAPI security scheme, renders "Authorize" button automatically |
| API grouping | Custom endpoints or pages | @Tag annotations + GroupedOpenApi beans | Standard OpenAPI grouping, renders as sections in Swagger UI sidebar |

**Key insight:** SpringDoc's value is that it auto-generates documentation from your existing code. The less custom code you write, the more reliable the docs stay.

## Common Pitfalls

### Pitfall 1: Jackson 3 ClassNotFoundException at Startup
**What goes wrong:** Application fails to start with `ClassNotFoundException: com.fasterxml.jackson.databind.node.ObjectNode` because swagger-core internally depends on Jackson 2 while Spring Boot 4 uses Jackson 3.
**Why it happens:** swagger-core (used by springdoc) hasn't fully migrated to Jackson 3 yet (upstream issue: swagger-api/swagger-core#4991).
**How to avoid:** Add `spring-boot-jackson2` dependency if startup fails. The official springdoc demos (4.x branch, Spring Boot 4.0.1, springdoc 3.0.1) work WITHOUT it, so try without first.
**Warning signs:** `ClassNotFoundException` or `NoClassDefFoundError` for Jackson 2 classes during Spring context initialization.

### Pitfall 2: Swagger UI Blocked by Spring Security
**What goes wrong:** Swagger UI returns 401 or redirects to login page.
**Why it happens:** Swagger UI paths (`/swagger-ui/**`, `/v3/api-docs/**`) are not in the `permitAll()` list.
**How to avoid:** Add these paths to the web SecurityFilterChain (Order 2) permitAll list. These paths are NOT under `/api/**`, so they won't match the API chain.
**Warning signs:** 302 redirect to `/login` or 401 when accessing `/swagger-ui/index.html`.

### Pitfall 3: Web Controllers Appearing in Swagger UI
**What goes wrong:** Thymeleaf controllers (HomeController, AuthWebController, etc.) appear in the documentation with HTML response types.
**Why it happens:** By default, springdoc scans ALL @Controller and @RestController beans.
**How to avoid:** Set `springdoc.pathsToMatch=/api/v1/**` to include only versioned API paths.
**Warning signs:** Endpoints like `/login`, `/register`, `/admin/users` (the web routes) appearing in Swagger UI.

### Pitfall 4: @Schema on Records in Internal Packages
**What goes wrong:** DTOs in `.internal` packages (like `LoginRequest`, `RegistrationRequest`) may not be scanned by springdoc if module boundaries restrict access.
**Why it happens:** Spring Modulith module visibility rules.
**How to avoid:** springdoc generates schemas based on the types returned/accepted by controllers, which it can access regardless of package visibility. The DTOs don't need to be in public packages -- they're discovered via reflection from controller method signatures.
**Warning signs:** Empty or missing request/response schemas in Swagger UI.

### Pitfall 5: Global Security Applied to Public Endpoints
**What goes wrong:** Login and register endpoints show a lock icon in Swagger UI, implying they need a JWT token.
**Why it happens:** Using `addSecurityItem()` on the OpenAPI bean applies security globally to ALL documented endpoints.
**How to avoid:** On public endpoints, annotate with `@SecurityRequirements` (empty, note the plural form) to override the global security requirement: `@SecurityRequirements` at method level removes the global requirement for that endpoint. Alternatively, use `@SecurityRequirement` with an empty name.
**Warning signs:** Lock icon on `/api/v1/auth/login` and `/api/v1/auth/register` in Swagger UI.

### Pitfall 6: Authentication Parameter Showing in Swagger UI
**What goes wrong:** Endpoints that accept `Authentication` as a Spring Security parameter show it as a required parameter in Swagger UI.
**Why it happens:** springdoc may not automatically hide Spring Security injected parameters.
**How to avoid:** springdoc auto-hides `Authentication`, `Principal`, `HttpServletRequest`, `HttpServletResponse` parameters from documentation. If any still appear, use `@Parameter(hidden = true)`.
**Warning signs:** A parameter named "authentication" appearing in the Swagger UI form for protected endpoints.

## Code Examples

Verified patterns from official sources:

### Complete OpenAPI Configuration Bean
```java
// Source: springdoc.org + springdoc-openapi-demos 4.x branch
package com.example.usermanagement.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
            .info(new Info()
                .title("User Management API")
                .version("1.0.0")
                .description("REST API for user authentication, profile management, "
                    + "and administration. Use /api/v1/auth/login to obtain a JWT token, "
                    + "then click 'Authorize' and paste the token to access protected endpoints."))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token from /api/v1/auth/login")));
    }
}
```

### Controller Annotations Example
```java
// Source: springdoc.org documentation
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, email verification, and password management")
public class AuthController {

    @Operation(
        summary = "Register new user",
        description = "Creates a user account and sends a verification email. "
            + "The user must verify their email before logging in."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @SecurityRequirements  // Override global security -- this is a public endpoint
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request) { ... }
}
```

### Application Properties
```yaml
# Source: springdoc.org + verified against springdoc-openapi-demos 4.x
springdoc:
  pathsToMatch: /api/v1/**
  swagger-ui:
    path: /swagger-ui.html
    display-request-duration: true
    operationsSorter: method
    tagsSorter: alpha
```

### SecurityConfig Update
```java
// In SecurityConfig.webFilterChain():
.requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                 "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Springfox | SpringDoc OpenAPI | 2020 (Springfox abandoned) | Springfox is dead. SpringDoc is the only option. |
| springdoc-openapi 2.x | springdoc-openapi 3.x | November 2025 (v3.0.0) | 3.x required for Spring Boot 4. 2.x stays on Boot 3.x. |
| Jackson 2 (com.fasterxml) | Jackson 3 (tools.jackson) | Spring Boot 4.0.0 | Boot 4 defaults to Jackson 3. springdoc/swagger-core still internally use Jackson 2 -- the `spring-boot-jackson2` bridge may be needed. |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | Spring Boot 4.0.0 | Renamed in Boot 4 for modularization. springdoc artifact name (`springdoc-openapi-starter-webmvc-ui`) already used "webmvc" -- no change needed. |
| OpenAPI 3.0 | OpenAPI 3.1 | springdoc 2.x+ | Minor spec differences. springdoc 3.x generates OpenAPI 3.1 by default. |

**Deprecated/outdated:**
- Springfox: Abandoned since 2020, incompatible with Spring Boot 3+
- springdoc-openapi 1.x: For Spring Boot 2.x only
- springdoc-openapi 2.x: For Spring Boot 3.x only, will not work with Boot 4

## Existing Endpoints Inventory

All REST controllers already exist and serve `/api/v1/**`. The phase adds documentation, not new endpoints.

### AuthController (`/api/v1/auth`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /register | Public | User registration |
| POST | /login | Public | User authentication, returns JWT |
| POST | /resend-verification | Public | Resend verification email |

### PasswordController (`/api/v1/auth`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /change-password | JWT (manual check) | Change password for authenticated user |
| POST | /forgot-password | Public | Request password reset email |
| POST | /reset-password | Public | Reset password with token |

### ProfileController (`/api/v1/users`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /me | JWT | Get current user profile |
| PUT | /me | JWT | Update current user profile |

### AdminController (`/api/v1/admin/users`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | / | ADMIN | List users with pagination/search/filter |
| POST | / | ADMIN | Create user via admin invite |
| PUT | /{id} | ADMIN | Update user details |
| PATCH | /{id}/status | ADMIN | Toggle user enabled/disabled |

### DTOs Requiring @Schema Annotations
**Request DTOs:**
- `RegistrationRequest` (email, password, firstName, lastName)
- `LoginRequest` (email, password, rememberMe)
- `ResendVerificationRequest` (email)
- `ChangePasswordRequest` (currentPassword, newPassword, confirmPassword)
- `ForgotPasswordRequest` (email)
- `ResetPasswordRequest` (token, newPassword, confirmPassword)
- `ProfileUpdateRequest` (displayName, firstName, lastName)
- `CreateUserRequest` (email, username, firstName, lastName, enabled, roles)
- `UpdateUserRequest` (firstName, lastName, roles)

**Response DTOs:**
- `AuthResponse` (token, tokenType, expiresIn, email, displayName, roles)
- `UserDto` (id, email, username, firstName, lastName, enabled, emailVerified, roles, createdAt)
- `AdminInviteService.InviteResult` (need to check its structure)
- `Map<String, Object>` and `Map<String, String>` (ad-hoc responses from some endpoints)

## Open Questions

1. **Jackson 3 startup compatibility**
   - What we know: springdoc 3.0.1 demos work with Spring Boot 4.0.1 without `spring-boot-jackson2`. But issue #3200 reports `ClassNotFoundException` for Jackson 2 classes.
   - What's unclear: Whether this project's specific dependency tree triggers the issue. The demos don't use Spring Security, JPA, etc.
   - Recommendation: Try WITHOUT `spring-boot-jackson2` first. If startup fails, add it. Flag as validation step in plan.

2. **OpenApiConfig package placement**
   - What we know: Project uses Spring Modulith with auth, user, shared modules. Cross-cutting config goes in `shared.config` (e.g., PasswordConfig, AppProperties).
   - What's unclear: Whether springdoc auto-configuration and OpenApiConfig need any special module handling.
   - Recommendation: Place `OpenApiConfig` in `shared.config` (same as other cross-cutting configuration). SpringDoc auto-configuration is a Spring Boot starter -- it should work outside module boundaries.

3. **Ad-hoc Map responses**
   - What we know: Some endpoints return `Map<String, Object>` or `Map<String, String>` instead of typed DTOs (e.g., register returns `Map<String, Object>` with "message" and "email").
   - What's unclear: Whether springdoc generates useful schemas for untyped Maps.
   - Recommendation: For this phase, accept map responses as-is and use `@Operation` + `@ApiResponse` with `@Content`/`@Schema` annotations to describe the response structure. Creating typed response DTOs is optional improvement.

4. **springdoc-openapi BOM vs direct version**
   - What we know: The demos use `springdoc-openapi-bom:3.0.1` in dependencyManagement. Our project could use either the BOM or a direct version on the single dependency.
   - Recommendation: Use direct version on the single dependency (simpler). BOM is useful if you need multiple springdoc artifacts.

## Sources

### Primary (HIGH confidence)
- [springdoc-openapi-demos 4.x branch](https://github.com/springdoc/springdoc-openapi-demos/tree/4.x) - Working Spring Boot 4.0.1 + springdoc 3.0.1 examples (parent pom.xml verified)
- [springdoc.org](https://springdoc.org/) - Official documentation for configuration, annotations, properties
- [springdoc.org/faq.html](https://springdoc.org/faq.html) - FAQ on security schemes, @Hidden, path filtering, @SecurityRequirement
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - `spring-boot-jackson2` module documentation

### Secondary (MEDIUM confidence)
- [springdoc-openapi GitHub issue #3095](https://github.com/springdoc/springdoc-openapi/issues/3095) - Spring Boot 4 support confirmed, Jackson 3 migration completed for springdoc codebase
- [springdoc-openapi GitHub issue #3157](https://github.com/springdoc/springdoc-openapi/issues/3157) - `spring-boot-jackson2` workaround documented
- [springdoc-openapi releases](https://github.com/springdoc/springdoc-openapi/releases) - v3.0.1 (latest, Nov 2025) supports Spring Boot 4

### Tertiary (LOW confidence)
- [springdoc-openapi GitHub issue #3200](https://github.com/springdoc/springdoc-openapi/issues/3200) - Jackson 3 ClassNotFoundException still OPEN (Feb 2026). Contradicts working demos. May be dependency-tree-specific.
- [springdoc-openapi GitHub issue #3175](https://github.com/springdoc/springdoc-openapi/issues/3175) - @Schema annotation issues with Kotlin. Closed wontfix. Java records not affected.

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - springdoc 3.0.1 is confirmed for Boot 4, but Jackson 3 compat has an open issue. Demos work; some users report problems. Need validation.
- Architecture: HIGH - Configuration patterns are well-documented and consistent across springdoc versions. Path filtering, security schemes, and annotations are stable APIs.
- Pitfalls: HIGH - All pitfalls are well-documented across multiple sources (security config, path filtering, Jackson compat).

**Research date:** 2026-02-14
**Valid until:** 2026-03-14 (30 days -- springdoc may release 3.0.2+ fixing Jackson 3 issues)
