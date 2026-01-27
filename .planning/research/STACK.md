# Stack Research

**Domain:** Spring Boot 4 User Management Server with JWT Auth, Spring Modulith, Thymeleaf, Dual-Database
**Researched:** 2026-01-28
**Confidence:** MEDIUM (WebSearch/WebFetch unavailable; based on training data through May 2025 plus Spring Boot 4 milestone announcements. Spring Boot 4 / Spring Framework 7 versions must be validated against current releases.)

---

## IMPORTANT: Version Validation Required

Spring Boot 4 and Spring Framework 7 were in milestone/RC phase as of my training cutoff (May 2025). The project context specifies these as the target versions. **Before starting development, verify:**

1. Whether Spring Boot 4.0.0 has reached GA (expected late 2025 / early 2026)
2. The exact GA version number (4.0.0 or possibly 4.0.x)
3. Whether Spring Modulith has a release compatible with Spring Boot 4
4. Whether SpringDoc OpenAPI has released a Spring Boot 4 compatible version

**If Spring Boot 4 is not yet GA**, consider starting with Spring Boot 3.4.x (latest 3.x GA) and upgrading when 4.0 GA drops -- the migration from 3.x to 4.x is designed to be incremental.

**Confidence note:** Version numbers below reflect my best understanding from milestone announcements. All versions marked with `[VERIFY]` must be checked against https://spring.io/projects and Maven Central before use.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **Java** | 21 (LTS) | Runtime & language | Required minimum for Spring Boot 4 / Spring Framework 7. Java 21 is the current LTS with virtual threads, pattern matching, record patterns, sequenced collections. | HIGH |
| **Spring Boot** | 4.0.x `[VERIFY]` | Application framework | Auto-configuration, embedded server, production-ready features. Version 4 requires Java 21+, uses Jakarta EE 11, Spring Framework 7. | MEDIUM |
| **Spring Framework** | 7.0.x `[VERIFY]` | Core framework | Managed by Spring Boot 4 parent POM -- do not set version independently. Includes virtual thread support, improved AOT, Micrometer integration. | MEDIUM |
| **Spring Security** | 7.0.x `[VERIFY]` | Authentication & authorization | The standard for Spring security. Version 7.x aligns with Spring Framework 7. Covers JWT, RBAC, CSRF, session management. | MEDIUM |
| **Spring Data JPA** | Managed by Boot | Database access (ORM) | Standard JPA implementation with Spring repository abstractions. Hibernate 7 under the hood with Spring Boot 4. | MEDIUM |
| **Spring Modulith** | 1.3.x or 2.0.x `[VERIFY]` | Modular architecture | Enforces module boundaries within a single deployable. Event-based inter-module communication. Spring Boot 4 compatible version needed. | LOW |
| **Thymeleaf** | 3.1.x (managed by Boot) | Server-side templating | Standard Spring Boot template engine. Deep Spring Security integration (sec: dialect). Natural templates. | HIGH |
| **Maven** | 3.9.x+ | Build tool | User-specified. Spring Boot 4 parent POM provides dependency management. Use Maven Wrapper (mvnw) for reproducibility. | HIGH |

### Database Technologies

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **H2 Database** | 2.x (managed by Boot) | Dev database | In-memory, zero-config, fast startup. H2 Console for inspection. Spring profile `dev` activates it. | HIGH |
| **PostgreSQL** | 16 or 17 | Prod database | The gold standard for production RDBMS. Excellent Spring Data JPA support. Runs in Docker Compose. | HIGH |
| **HikariCP** | Managed by Boot | Connection pooling | Default connection pool in Spring Boot. Best performance, smallest footprint. Auto-configured. | HIGH |
| **Flyway** | 10.x (managed by Boot) | Database migration | Schema versioning and migration. Works identically across H2 and PostgreSQL. Critical for reproducible schema. | HIGH |

### Authentication & Security Libraries

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **jjwt (io.jsonwebtoken)** | 0.12.x | JWT creation & parsing | De facto standard for JWT in Java. Type-safe API, algorithm enforcement, claim validation. Maintained by Okta/Auth0 contributor. | HIGH |
| **Spring Security Crypto** | Included in spring-security | Password hashing | BCrypt password encoder. Part of Spring Security -- no extra dependency. | HIGH |

### Email

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **Spring Boot Mail Starter** | Managed by Boot | SMTP email | `spring-boot-starter-mail` wraps Jakarta Mail. Auto-configured from `spring.mail.*` properties. | HIGH |
| **Thymeleaf (for email)** | Same as UI | Email templates | Reuse Thymeleaf for HTML email templates. Spring provides `SpringTemplateEngine` for non-web rendering. | HIGH |

### API Documentation

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **SpringDoc OpenAPI** | 2.8.x or 3.0.x `[VERIFY]` | Swagger UI & OpenAPI spec | Auto-generates OpenAPI 3.1 spec from Spring MVC controllers. Swagger UI included. springdoc-openapi-starter-webmvc-ui artifact. Must verify Spring Boot 4 compatibility. | LOW |

### Frontend (Server-Side)

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **Bootstrap** | 5.3.x | CSS framework | User-specified. Include via WebJars or CDN. Responsive, accessible, no jQuery dependency since v5. | HIGH |
| **Bootstrap Icons** | 1.11.x | Icon set | Companion to Bootstrap 5. SVG icons, no font dependency option. | HIGH |
| **WebJars** | N/A | Frontend dependency management | Serve Bootstrap/JS libraries as Maven dependencies. Spring Boot auto-configures `/webjars/` path mapping. | HIGH |

### Infrastructure

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| **Docker** | 24+ | Containerization | Production deployment. Multi-stage Dockerfile for Java apps. | HIGH |
| **Docker Compose** | v2 (compose v2 plugin) | Stack orchestration | App + PostgreSQL + PgAdmin in one `docker compose up`. User-specified. | HIGH |
| **PgAdmin** | 4 (latest) | PostgreSQL admin UI | Browser-based PostgreSQL management. User-specified in Docker Compose. | HIGH |
| **PostgreSQL JDBC Driver** | 42.7.x (managed by Boot) | JDBC connectivity | Spring Boot manages the version. Runtime-only dependency. | HIGH |

### Development Tools

| Tool | Purpose | Notes | Confidence |
|------|---------|-------|------------|
| **Maven Wrapper (mvnw)** | Reproducible builds | `./mvnw` ensures consistent Maven version. Generated by `mvn wrapper:wrapper` or Spring Initializr. | HIGH |
| **Spring Boot DevTools** | Hot reload in dev | Auto-restart on classpath changes. LiveReload for Thymeleaf. Dev-only dependency (excluded from production JAR). | HIGH |
| **Spring Boot Actuator** | Production monitoring | Health checks, metrics, env info. Essential for Docker health probes. | HIGH |
| **Spring Boot Docker Compose** | Dev container support | `spring-boot-docker-compose` module can auto-start Docker Compose services during dev. Available since Spring Boot 3.1+. | MEDIUM |
| **Lombok** | Boilerplate reduction | `@Data`, `@Builder`, `@Slf4j`. Widely used but controversial. **Recommend AGAINST** for this project -- Java 21 records + IDE generation is sufficient. Spring Modulith examples don't use it. | HIGH |
| **H2 Console** | Dev database inspection | Enabled via `spring.h2.console.enabled=true`. Web-based SQL console at `/h2-console`. | HIGH |

---

## Supporting Libraries

| Library | Maven Coordinates | Purpose | When to Use | Confidence |
|---------|-------------------|---------|-------------|------------|
| **Jakarta Validation** | `spring-boot-starter-validation` | Bean validation | Always. `@Valid`, `@NotBlank`, `@Email` on DTOs. Included via starter. | HIGH |
| **Spring Boot Test** | `spring-boot-starter-test` | Testing framework | Always. JUnit 5 + Mockito + AssertJ + Spring Test + MockMvc. | HIGH |
| **Spring Security Test** | `spring-security-test` | Security testing | Always. `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`. | HIGH |
| **Testcontainers** | `org.testcontainers:postgresql` | Integration testing | For PostgreSQL integration tests. Spins up real PostgreSQL in Docker. Use `@ServiceConnection` annotation. | HIGH |
| **Spring Modulith Test** | `spring-modulith-starter-test` | Module boundary testing | Always. `ApplicationModuleTest` verifies module boundaries and allowed dependencies. | MEDIUM |
| **Jackson** | Managed by Boot | JSON serialization | Auto-configured. Used for REST API request/response bodies and JWT claims. | HIGH |
| **Thymeleaf Spring Security** | `thymeleaf-extras-springsecurity6` `[VERIFY]` | Security in templates | For `sec:authorize`, `sec:authentication` in Thymeleaf. May need version update for Spring Security 7. | LOW |
| **Thymeleaf Layout Dialect** | `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` | Template layouts | For shared header/footer/nav layouts across pages. Alternative to Thymeleaf fragment includes. | HIGH |

---

## Maven POM Structure

### Parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.0</version> <!-- [VERIFY exact version] -->
    <relativePath/>
</parent>
```

### Properties

```xml
<properties>
    <java.version>21</java.version>
    <!-- jjwt version not managed by Spring Boot -->
    <jjwt.version>0.12.6</jjwt.version>
    <!-- SpringDoc version not managed by Spring Boot -->
    <springdoc.version>2.8.4</springdoc.version> <!-- [VERIFY for Boot 4 compat] -->
</properties>
```

### Core Dependencies

```xml
<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- Thymeleaf Spring Security integration -->
    <!-- [VERIFY] artifact name may change for Spring Security 7 -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>

    <!-- Thymeleaf Layout Dialect -->
    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Email -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Spring Modulith -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- JWT -->
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
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- SpringDoc OpenAPI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>

    <!-- WebJars: Bootstrap -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.3</version>
    </dependency>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap-icons</artifactId>
        <version>1.11.3</version>
    </dependency>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>webjars-locator-core</artifactId>
    </dependency>

    <!-- Database: H2 (dev) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Database: PostgreSQL (prod) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Database Migration -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- DevTools (dev only) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <!-- Docker Compose support (dev only) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-docker-compose</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Modulith Test -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers (for PostgreSQL integration tests) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Spring Modulith BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>1.3.1</version> <!-- [VERIFY for Boot 4 compat] -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Build Plugins

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not the Alternative |
|----------|-------------|-------------|------------------------|
| **JWT library** | jjwt (io.jsonwebtoken) | Nimbus JOSE+JWT, Spring Security OAuth2 Resource Server | jjwt is purpose-built for JWT, simpler API, lighter weight. Nimbus is lower-level. OAuth2 Resource Server is designed for external IdP scenarios, not self-issued JWT. |
| **Template engine** | Thymeleaf | FreeMarker, JSP | User-specified Thymeleaf. Also: best Spring Security integration, natural templates (viewable in browser without server), active development. |
| **Database migration** | Flyway | Liquibase | Flyway is simpler (SQL files), first-class Spring Boot support, lower learning curve. Liquibase is more powerful but overkill for this scope. |
| **Connection pool** | HikariCP (default) | Tomcat JDBC, DBCP2 | HikariCP is Spring Boot's default, fastest, most reliable. No reason to change. |
| **CSS framework** | Bootstrap 5 (WebJars) | Tailwind CSS, Bootstrap CDN | User-specified Bootstrap 5. WebJars over CDN because: works offline, version-locked, Maven-managed. |
| **API docs** | SpringDoc OpenAPI | Springfox | Springfox is abandoned (last release 2020). SpringDoc is the active successor, supports Spring Boot 3+, OpenAPI 3.1. |
| **Build tool** | Maven | Gradle | User-specified Maven. Spring Boot supports both equally. |
| **Boilerplate** | Java 21 records + IDE generation | Lombok | Java 21 records replace most Lombok use cases for DTOs. Avoids annotation processor issues, IDE plugin requirements, and compile-time magic. |
| **Architecture** | Spring Modulith | Spring Cloud microservices | User-specified. Modulith is right for this scope -- single deployable with enforced boundaries. Microservices adds distributed complexity with zero benefit here. |
| **Testing DB** | Testcontainers | H2 for all tests | Testcontainers gives real PostgreSQL in tests. H2 has SQL dialect differences that cause false positives. Use H2 for unit tests, Testcontainers for integration tests. |
| **Email templates** | Thymeleaf (reuse) | Apache FreeMarker, plain text | Already using Thymeleaf for web pages. Reusing it for email templates avoids adding another template engine. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **Springfox** | Abandoned since 2020. Does not support Spring Boot 3+, let alone 4. | SpringDoc OpenAPI |
| **Lombok** | Annotation processor complications, IDE plugin dependency, unnecessary with Java 21 records. Spring team discourages it in official samples. | Java 21 records for DTOs, IDE generation for entities |
| **javax.\* packages** | Spring Boot 3+ uses Jakarta EE. Spring Boot 4 requires Jakarta EE 11. `javax.persistence`, `javax.servlet` etc. will not compile. | `jakarta.persistence`, `jakarta.servlet`, etc. |
| **Spring Security WebSecurityConfigurerAdapter** | Removed in Spring Security 6+. | Component-based configuration with `SecurityFilterChain` @Bean |
| **spring.datasource.initialization-mode** | Deprecated. | Flyway for schema migration, `spring.sql.init.*` if needed |
| **application.properties with hardcoded secrets** | Security risk, not portable. | `.env` files + `spring.config.import=optional:file:.env[.properties]` or environment variables |
| **Spring Cloud for module communication** | Overkill. Spring Modulith uses in-process events. | `ApplicationEventPublisher` + `@ApplicationModuleListener` |
| **Manual JWT filter registration** | Error-prone, misses Spring Security's filter chain ordering. | Extend `OncePerRequestFilter`, register via `HttpSecurity.addFilterBefore()` |
| **JPA entity equals/hashCode with all fields** | Breaks with lazy loading proxies, causes subtle bugs. | Use business key or `@Id` field only. Implement consistently. |
| **`spring.jpa.hibernate.ddl-auto=update` in production** | Unpredictable schema changes, potential data loss. | Flyway migrations. Use `validate` in prod. |
| **Storing JWT secret in application.yml** | Checked into version control. | Environment variable or `.env` file: `JWT_SECRET` |
| **Using `@Autowired` field injection** | Untestable, hides dependencies, not immutable. | Constructor injection (implicit with single constructor in Spring). |

---

## Stack Patterns by Variant

### If Spring Boot 4 is GA:

- Use `spring-boot-starter-parent:4.0.x` directly
- Expect Jakarta EE 11, Hibernate 7, Tomcat 11
- Virtual threads likely enabled by default (`spring.threads.virtual.enabled=true`)
- Spring Modulith should have a compatible release -- check BOM version

### If Spring Boot 4 is NOT yet GA (still RC/milestone):

- **Option A (recommended):** Start with Spring Boot 3.4.x (latest 3.x LTS)
  - Migration to 4.x is incremental (same as 2.x -> 3.x pattern)
  - All libraries are proven compatible
  - Upgrade when 4.0 goes GA
- **Option B:** Use Spring Boot 4.0.0-RC1 or latest milestone
  - Accept potential breaking changes between milestones
  - Some third-party libraries may lag (SpringDoc, Thymeleaf extras)
  - Good for learning, risky for production

### If SpringDoc is not yet compatible with Spring Boot 4:

- Temporarily exclude SpringDoc
- Build REST API endpoints first
- Add Swagger UI when SpringDoc releases Boot 4 compatible version
- Alternative: hand-written OpenAPI YAML with Swagger UI static hosting (not recommended)

### For Dual Database (H2 + PostgreSQL):

- Use Spring Profiles: `dev` (H2) and `prod` (PostgreSQL)
- Flyway with `flyway.locations=classpath:db/migration/{vendor}` for dialect-specific migrations
- Or: write H2-compatible SQL that also works on PostgreSQL (subset SQL)
- Use `spring.sql.init.platform` for schema/data split if not using Flyway
- **Recommended:** Flyway with a common migration path, since H2 supports most PostgreSQL syntax

---

## Version Compatibility Matrix

| Spring Boot | Spring Framework | Spring Security | Java Minimum | Jakarta EE | Hibernate | Confidence |
|-------------|------------------|-----------------|--------------|------------|-----------|------------|
| 3.4.x | 6.2.x | 6.4.x | 17 | 10 | 6.6.x | HIGH |
| 4.0.x | 7.0.x | 7.0.x | 21 | 11 | 7.0.x | MEDIUM |

| Library | Compatible Boot 3.4.x | Compatible Boot 4.0.x | Notes | Confidence |
|---------|----------------------|----------------------|-------|------------|
| jjwt 0.12.x | Yes | Likely yes | Pure Java, no Spring dependency | HIGH |
| SpringDoc 2.8.x | Yes | `[VERIFY]` | May need 3.0.x for Boot 4 | LOW |
| Thymeleaf extras springsecurity6 | Yes | `[VERIFY]` | May need springsecurity7 artifact | LOW |
| Spring Modulith 1.3.x | Yes (Boot 3.4) | `[VERIFY]` | May need 2.0.x for Boot 4 | LOW |
| Flyway 10.x | Yes | Yes | Managed by Boot | MEDIUM |
| Testcontainers 1.20.x | Yes | Likely yes | Independent of Spring version | HIGH |
| WebJars Bootstrap 5.3.x | Yes | Yes | Static assets, no Spring coupling | HIGH |
| H2 2.x | Yes | Yes | Managed by Boot | HIGH |
| PostgreSQL Driver 42.7.x | Yes | Yes | Managed by Boot | HIGH |

---

## Spring Boot 4 / Spring Framework 7 Key Changes

These changes affect how the project is built compared to Spring Boot 3.x tutorials and examples.

### Confirmed Changes (from milestone announcements, MEDIUM confidence)

1. **Java 21 minimum** -- No more Java 17 support. Enables records, sealed classes, pattern matching, virtual threads at the language level.

2. **Jakarta EE 11** -- Updated from Jakarta EE 10 (Boot 3.x). Primarily affects servlet API version and JPA spec version. Most application code unchanged.

3. **Virtual threads by default** -- `spring.threads.virtual.enabled=true` is expected to be the default. Improves throughput for I/O-bound operations (database, email, HTTP calls) without reactive programming.

4. **Hibernate 7** -- Updated from Hibernate 6.x. New features but also potential breaking changes in HQL/JPQL edge cases.

5. **Tomcat 11** -- Embedded server upgrade. Transparent to application code.

6. **Improved AOT (Ahead-of-Time) compilation** -- Better GraalVM native image support. Not critical for this project but available.

7. **Spring Security 7** -- Continued refinement of the component-based security configuration model. Possible deprecation of additional legacy patterns.

### What This Means for the Project

- **Use Java 21 features freely:** Records for DTOs, sealed interfaces for result types, pattern matching in switch, text blocks for SQL/templates.
- **Virtual threads help email sending:** Blocking SMTP calls on virtual threads means better throughput without async complexity.
- **Same security model:** `SecurityFilterChain` bean-based configuration (introduced in Spring Security 5.7+) remains the standard.
- **Same data access model:** Spring Data JPA repositories, `@Entity` classes, same as Boot 3.x.
- **Same Thymeleaf model:** Template resolution, Spring integration unchanged.

---

## Project-Specific Configuration

### application.yml (common)

```yaml
spring:
  application:
    name: user-management
  threads:
    virtual:
      enabled: true  # Spring Boot 4 likely defaults this
  jpa:
    open-in-view: false  # Disable OSIV anti-pattern
  flyway:
    enabled: true
```

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema
    show-sql: true
  flyway:
    locations: classpath:db/migration/common,classpath:db/migration/h2
```

### application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:userdb}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    locations: classpath:db/migration/common,classpath:db/migration/postgresql
```

### Mail Configuration (via .env)

```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### JWT Configuration (via .env)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION:3600000}  # 1 hour
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION:86400000}  # 24 hours
```

---

## Docker Stack

### Dockerfile (multi-stage)

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose Services

| Service | Image | Purpose |
|---------|-------|---------|
| app | Built from Dockerfile | Spring Boot application |
| postgres | postgres:17-alpine | Production database |
| pgadmin | dpage/pgadmin4 | Database administration UI |

---

## Sources & Confidence Summary

| Source | What Was Used | Confidence Impact |
|--------|---------------|-------------------|
| Training data (Spring Boot 3.x ecosystem) | Core stack patterns, library choices, configuration | HIGH for patterns, tools, and libraries that existed before May 2025 |
| Training data (Spring Boot 4 milestone announcements) | Version numbers, Java 21 requirement, Jakarta EE 11 | MEDIUM -- milestones were announced but GA not confirmed |
| Training data (Spring Modulith) | Module structure, testing patterns | MEDIUM -- 1.x was GA, but Boot 4 compat version unknown |
| Training data (SpringDoc OpenAPI) | Artifact names, configuration | LOW for Boot 4 compat -- may need new major version |
| Training data (Thymeleaf extras) | Artifact names | LOW for Security 7 compat -- artifact name may change |
| **WebSearch / WebFetch** | **UNAVAILABLE** | Could not verify current release status |

### What Could NOT Be Verified

1. **Spring Boot 4 GA status** -- Is it released? What exact version?
2. **Spring Modulith Boot 4 compatible version** -- Is it 1.3.x, 2.0.x, or something else?
3. **SpringDoc OpenAPI Boot 4 version** -- Is 2.8.x compatible or is 3.0.x needed?
4. **Thymeleaf Spring Security 7 extras** -- Is the artifact name `springsecurity6` or `springsecurity7`?
5. **Flyway version** -- Managed by Boot, but exact version in Boot 4 not confirmed
6. **Any new starters or features** added between May 2025 and January 2026

### Recommended Verification Steps

Before starting development:

```bash
# 1. Check latest Spring Boot version
curl -s https://api.github.com/repos/spring-projects/spring-boot/releases | head -20

# 2. Use Spring Initializr to generate compatible project
# Visit https://start.spring.io with Spring Boot 4.0.x selected
# Add: Web, Security, Data JPA, Thymeleaf, Mail, Validation, Actuator, DevTools, H2, PostgreSQL, Flyway, Docker Compose
# This generates a pom.xml with all correct, compatible versions

# 3. Search Maven Central for Spring Modulith Boot 4 compatible version
# https://central.sonatype.com/search?q=spring-modulith-bom

# 4. Search Maven Central for SpringDoc Boot 4 compatible version
# https://central.sonatype.com/search?q=springdoc-openapi-starter-webmvc-ui

# 5. Search for Thymeleaf Spring Security extras latest version
# https://central.sonatype.com/search?q=thymeleaf-extras-springsecurity
```

**Strongest recommendation: Use Spring Initializr (start.spring.io) to generate the initial POM.** It will select all compatible versions automatically. Then add the non-standard dependencies (jjwt, SpringDoc, WebJars, Spring Modulith) manually.

---
*Stack research for: Spring Boot 4 User Management Server*
*Researched: 2026-01-28*
*Research limitations: WebSearch and WebFetch unavailable. Findings based on training data (cutoff May 2025). All `[VERIFY]` items require validation before development begins.*
