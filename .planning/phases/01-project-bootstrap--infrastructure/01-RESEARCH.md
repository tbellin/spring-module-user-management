# Phase 1: Project Bootstrap & Infrastructure - Research

**Researched:** 2026-01-28
**Domain:** Spring Boot 4 project setup, dual-database configuration, Docker Compose orchestration, Flyway migrations, .env configuration management
**Confidence:** HIGH (Spring Boot 4.0.0 GA confirmed, release notes and migration guide verified)

## Summary

This research covers everything needed to bootstrap a Spring Boot 4 project with dual-database support (H2 for dev, PostgreSQL for prod), Docker Compose orchestration (App + PostgreSQL + PgAdmin), Flyway database migrations, and .env-based configuration management with template processing.

Spring Boot 4.0.0 was released November 20, 2025 (GA), with patch 4.0.1 on December 18, 2025. The most significant change relevant to Phase 1 is the **complete modularization of Spring Boot** -- auto-configuration is no longer bundled in a single jar. Several technologies that previously "just worked" now require explicit starter dependencies (`spring-boot-starter-flyway`, `spring-boot-h2console`). Additionally, `spring-boot-starter-web` has been renamed to `spring-boot-starter-webmvc`.

The Java baseline for Spring Boot 4 is **Java 17** (not Java 21 as previously assumed from milestone announcements). Java 21 is recommended as the latest LTS but is not required. Key dependency upgrades include Hibernate 7.1, Flyway 11.11, Jackson 3.0, Tomcat 11, and Jakarta EE 11.

**Primary recommendation:** Use Spring Boot 4.0.1 parent POM, add all new modular starters explicitly (flyway, h2console), use `spring-boot-starter-webmvc` (not `web`), and write a single set of Flyway migrations using H2 PostgreSQL compatibility mode for dev.

## Standard Stack

The established libraries/tools for this phase:

### Core

| Library | Version | Purpose | Why Standard | Confidence |
|---------|---------|---------|--------------|------------|
| Spring Boot | 4.0.1 | Application framework | Latest GA with 88 bug fixes over 4.0.0 | HIGH |
| Spring Framework | 7.0.x (managed) | Core framework | Managed by Boot 4 parent POM | HIGH |
| Java | 17+ (recommend 21 LTS) | Runtime | Boot 4 minimum is Java 17; Java 21 is latest LTS | HIGH |
| Maven | 3.9.x+ with Maven Wrapper | Build tool | User-specified; `mvnw` for reproducibility | HIGH |
| H2 Database | Managed by Boot | Dev in-memory database | Zero-config, fast startup, profile-activated | HIGH |
| PostgreSQL | 17-alpine (Docker) | Prod database | Gold standard RDBMS, Docker image | HIGH |
| PostgreSQL JDBC | Managed by Boot | JDBC connectivity | Runtime-only dependency | HIGH |
| Flyway | 11.11 (managed by Boot) | Database migration | Schema versioning, dual-database support | HIGH |
| Spring Modulith | 2.0.1 (BOM) | Modular architecture | GA release compatible with Spring Boot 4 | HIGH |
| Thymeleaf | Managed by Boot | Server-side templating | User-specified, deep Spring integration | HIGH |
| Spring Security | 7.0.x (managed) | Security framework | Part of Spring Boot 4 train | HIGH |
| Spring Boot Actuator | Managed by Boot | Health checks, metrics | Essential for Docker health probes | HIGH |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-starter-flyway | 4.0.1 | Flyway auto-configuration | **NEW in Boot 4** -- required, replaces bare flyway-core |
| spring-boot-h2console | Managed by Boot | H2 web console auto-config | **NEW in Boot 4** -- required for H2 console at /h2-console |
| flyway-database-postgresql | Managed by Boot | Flyway PostgreSQL module | Required for PostgreSQL migrations |
| spring-boot-starter-webmvc | Managed by Boot | Spring MVC web starter | **Renamed from** spring-boot-starter-web in Boot 4 |
| spring-boot-devtools | Managed by Boot | Hot reload in dev | Dev-only, optional, excluded from prod JAR |
| spring-boot-docker-compose | Managed by Boot | Docker Compose dev lifecycle | Auto-starts compose services during dev |
| Bootstrap | 5.3.3 (WebJars) | CSS framework | User-specified |
| PgAdmin | dpage/pgadmin4:latest | PostgreSQL admin UI | Docker Compose sidecar service |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flyway | Liquibase | Flyway is simpler (SQL files), first-class Spring Boot support |
| H2 for dev DB | Testcontainers PostgreSQL for dev | H2 is faster startup; Testcontainers more accurate but needs Docker |
| envsubst for templates | sed, awk, custom scripts | envsubst is purpose-built, standard GNU tool, simplest |
| .env + envsubst | spring-dotenv library | Script approach is simpler, no extra dependency, works with Docker Compose natively |

### Maven POM Setup

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>2.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Web (RENAMED in Boot 4) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
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

    <!-- Thymeleaf Spring Security integration (still springsecurity6 artifact for Boot 4) -->
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

    <!-- Mail -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Actuator (Docker health probes) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Modulith -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>

    <!-- Flyway (NEW STARTER in Boot 4 -- replaces bare flyway-core) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <!-- NOTE: H2 support is built into flyway-core, no flyway-database-h2 needed -->

    <!-- Database: H2 (dev) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- H2 Console (NEW in Boot 4 -- separate module, no hyphen in artifactId) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-h2console</artifactId>
    </dependency>

    <!-- Database: PostgreSQL (prod) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- WebJars: Bootstrap -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.3</version>
    </dependency>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>webjars-locator-core</artifactId>
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
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

## Architecture Patterns

### Recommended Project Structure (Phase 1 Skeleton)

Phase 1 creates the skeleton. Feature code comes in later phases.

```
project-root/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
├── .env.template                    # Template with placeholder variables
├── .env                             # Generated from template (gitignored)
├── .gitignore
├── Dockerfile                       # Multi-stage build
├── compose.yaml                     # Docker Compose (App + PostgreSQL + PgAdmin)
├── bin/
│   ├── setup.sh                     # Initial project setup script (TOOL-01)
│   └── generate-config.sh           # Template processing script (TOOL-04)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/usermanagement/
│   │   │       ├── Application.java              # @SpringBootApplication
│   │   │       ├── auth/                          # Auth module (skeleton)
│   │   │       │   └── package-info.java
│   │   │       ├── user/                          # User module (skeleton)
│   │   │       │   └── package-info.java
│   │   │       └── shared/                        # Shared module (skeleton)
│   │   │           └── package-info.java
│   │   └── resources/
│   │       ├── application.yml                    # Common config
│   │       ├── application-dev.yml                # H2 dev profile
│   │       ├── application-prod.yml               # PostgreSQL prod profile
│   │       ├── templates/
│   │       │   └── index.html                     # Minimal smoke test page
│   │       ├── static/
│   │       │   └── css/
│   │       └── db/
│   │           └── migration/
│   │               └── V1__init_schema.sql        # Initial Flyway migration
│   └── test/
│       └── java/
│           └── com/example/usermanagement/
│               ├── ApplicationTests.java          # Smoke test
│               └── ModularityTests.java           # Module boundary verification
```

### Pattern 1: Spring Profile-Based Dual Database Configuration

**What:** Use Spring profiles `dev` and `prod` to switch between H2 and PostgreSQL without code changes.
**When to use:** Always in this project. Dev mode uses H2 for fast iteration, prod uses PostgreSQL.

```yaml
# application.yml (common)
spring:
  application:
    name: user-management
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

server:
  port: 8080
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    show-sql: true
  flyway:
    locations: classpath:db/migration
```

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:userdb}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false
  flyway:
    locations: classpath:db/migration
```

### Pattern 2: Single Flyway Migration Set with H2 PostgreSQL Mode

**What:** Write one set of SQL migrations that work on both H2 (in PostgreSQL compatibility mode) and PostgreSQL.
**When to use:** When the schema is standard SQL without vendor-specific features (which is the case for a user management app).

```sql
-- V1__init_schema.sql
-- Works on both H2 (MODE=PostgreSQL) and PostgreSQL

CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_role (
    id   BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed default roles
INSERT INTO app_role (name) VALUES ('ROLE_USER');
INSERT INTO app_role (name) VALUES ('ROLE_ADMIN');
```

**Key SQL compatibility notes:**
- Use `GENERATED BY DEFAULT AS IDENTITY` (not `SERIAL` which is PostgreSQL-only and deprecated)
- Use `BOOLEAN` (works on both)
- Use `TIMESTAMP` (not `TIMESTAMP WITH TIME ZONE` unless needed)
- Use `VARCHAR` (not `TEXT` for columns with length limits)
- Avoid PostgreSQL-specific types (`jsonb`, arrays, `uuid` generation functions)
- Use `CURRENT_TIMESTAMP` (standard SQL)

### Pattern 3: Docker Compose Full Stack with Health Checks

**What:** Docker Compose orchestrates App + PostgreSQL + PgAdmin with health checks and proper dependency ordering.
**When to use:** For production-like local environment and actual deployment.

```yaml
# compose.yaml
services:
  db:
    image: postgres:17-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-userdb}
      POSTGRES_USER: ${DB_USERNAME:-appuser}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-apppass}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-appuser} -d ${DB_NAME:-userdb}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  pgadmin:
    image: dpage/pgadmin4:latest
    restart: unless-stopped
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_EMAIL:-admin@example.com}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD:-admin}
    ports:
      - "${PGADMIN_PORT:-5050}:80"
    depends_on:
      db:
        condition: service_healthy

  app:
    build:
      context: .
      dockerfile: Dockerfile
    restart: unless-stopped
    ports:
      - "${APP_PORT:-8080}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${DB_NAME:-userdb}
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME:-appuser}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-apppass}
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 40s

volumes:
  pgdata:
```

**Critical details:**
- App container uses `db` as hostname (Docker Compose service name), NOT `localhost`
- PostgreSQL healthcheck uses `pg_isready` -- the standard readiness probe
- App healthcheck uses Spring Boot Actuator `/actuator/health` endpoint
- `depends_on: condition: service_healthy` ensures ordering
- `.env` file is automatically read by Docker Compose when in the same directory
- App `start_period: 40s` gives Spring Boot time to start and run Flyway

### Pattern 4: Multi-Stage Dockerfile for Spring Boot

**What:** Two-stage Docker build: compile with JDK, run with JRE only.
**When to use:** Always for production Docker images.

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key practices:**
- Use specific image tags (`21-jdk-jammy`, `21-jre-jammy`), not `latest`
- JRE for runtime (smaller image: ~314MB vs ~624MB)
- Non-root user for security
- Copy pom.xml and resolve dependencies first (Docker layer caching)
- Install `curl` in runtime image for health checks

### Pattern 5: .env Template Processing with Scripts (INFRA-05, INFRA-06)

**What:** Use `.env.template` as the source of truth for configuration variables. A script processes it to generate `.env` with actual values.
**When to use:** Initial project setup and when adding new configuration variables.

```bash
# .env.template
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=userdb
DB_USERNAME=appuser
DB_PASSWORD=CHANGE_ME_TO_SECURE_PASSWORD

# PgAdmin
PGADMIN_EMAIL=admin@example.com
PGADMIN_PASSWORD=CHANGE_ME
PGADMIN_PORT=5050

# Application
APP_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# JWT (needed in later phases)
JWT_SECRET=CHANGE_ME_TO_BASE64_ENCODED_256BIT_SECRET
JWT_EXPIRATION=3600000

# Mail (needed in later phases)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
```

### Pattern 6: Setup and Config Scripts (TOOL-01, TOOL-04)

**What:** Shell scripts in `./bin/` for project bootstrapping and config management.
**When to use:** On fresh clone and when config templates change.

#### Setup Script (TOOL-01): `bin/setup.sh`

```bash
#!/usr/bin/env bash
# bin/setup.sh -- Project setup script
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Project Setup ==="

# 1. Check prerequisites
command -v java >/dev/null 2>&1 || { echo "ERROR: Java is required. Install Java 21."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "ERROR: Docker is required."; exit 1; }

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17+ required (21 recommended). Found: $JAVA_VERSION"
    exit 1
fi

# 2. Generate .env from template if not exists
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "Generating .env from .env.template..."
    "$SCRIPT_DIR/generate-config.sh"
    echo "IMPORTANT: Edit .env with your actual values before running the app."
else
    echo ".env already exists. Skipping generation."
fi

# 3. Make Maven wrapper executable
chmod +x "$PROJECT_ROOT/mvnw" 2>/dev/null || true

# 4. Build project
echo "Building project..."
"$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" clean compile -B

echo ""
echo "=== Setup Complete ==="
echo "To start in dev mode:  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "To start with Docker:  docker compose up --build"
```

#### Config Template Processing Script (TOOL-04): `bin/generate-config.sh`

```bash
#!/usr/bin/env bash
# bin/generate-config.sh -- Generate config files from templates
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Generate .env from .env.template
if [ -f "$PROJECT_ROOT/.env.template" ]; then
    cp "$PROJECT_ROOT/.env.template" "$PROJECT_ROOT/.env"
    echo "Generated .env from .env.template"
    echo "Edit .env with your actual configuration values."
else
    echo "ERROR: .env.template not found at $PROJECT_ROOT/.env.template"
    exit 1
fi
```

**Note on envsubst:** For `.env` files, a simple copy is the safest approach since the user needs to edit the `CHANGE_ME` values manually. `envsubst` is more appropriate when generating files that mix framework syntax with custom variables (e.g., Kubernetes manifests, Nginx configs). Key considerations for `envsubst`:
- Only recognizes exported environment variables
- Does not support shell parameter expansions like `${VAR:-default}` (security reasons)
- Use single quotes around variable list to prevent shell substitution: `envsubst '$VAR1 $VAR2'`

### Anti-Patterns to Avoid

- **Using `spring-boot-starter-web` in Boot 4:** Renamed to `spring-boot-starter-webmvc`. The old name is deprecated and will be removed.
- **Omitting `spring-boot-starter-flyway`:** In Boot 4, bare `flyway-core` on classpath does NOT trigger auto-configuration. You MUST use the starter.
- **Omitting `spring-boot-h2console`:** In Boot 4, H2 console auto-configuration moved to a separate module. Without it, `spring.h2.console.enabled=true` does nothing.
- **Using `spring.jpa.hibernate.ddl-auto=update` in production:** Unpredictable schema changes. Use Flyway with `validate` only.
- **Hardcoding database credentials in application.yml:** Use `${ENV_VAR}` placeholders and `.env` file.
- **Using `localhost` as DB host in Docker container:** Containers use service names (`db`) as hostnames.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Database schema versioning | Custom SQL scripts with manual ordering | Flyway | Handles versioning, checksums, failure recovery, dual-database |
| Connection pooling | Custom pool management | HikariCP (default in Boot) | Best performance, auto-configured, zero setup |
| Docker container health checks | Custom health scripts | Spring Boot Actuator `/actuator/health` | Automatic, aggregates all health indicators (DB, disk, etc.) |
| Environment variable management | Custom property loaders | `.env` + Docker Compose native support + Spring `${VAR}` | Standard approach, works everywhere |
| Build reproducibility | README instructions | Maven Wrapper (`mvnw`) | Ensures exact Maven version, no system dependency |
| Configuration per environment | Custom config loaders | Spring Profiles (`dev`, `prod`) | Built-in, well-understood, profile-specific YAML files |
| Docker Compose service discovery | Manual hostname config | Docker Compose service names | `db`, `app` as hostnames work automatically |

**Key insight:** Spring Boot 4 and Docker Compose together handle nearly all infrastructure concerns out of the box. The main trap is not knowing which Boot 4 starters to add due to the modularization change.

## Common Pitfalls

### Pitfall 1: Spring Boot 4 Modularization -- Missing Starters

**What goes wrong:** Application starts but features are silently missing. H2 console does not appear at `/h2-console`. Flyway migrations do not run on startup. Auto-configuration for technologies you expect "just works" no longer activates.
**Why it happens:** Spring Boot 4 split the monolithic `spring-boot-autoconfigure` jar (~2MB in Boot 3.5) into 70+ focused modules. Having a library on the classpath is no longer sufficient -- you need the corresponding starter or auto-configuration module.
**How to avoid:** For Phase 1, ensure these NEW explicit dependencies are present:
- `spring-boot-starter-flyway` (replaces bare `flyway-core`)
- `spring-boot-h2console` (separate module for H2 console auto-config -- note: no hyphen before "console")
- `spring-boot-starter-webmvc` (replaces deprecated `spring-boot-starter-web`)
**Warning signs:** Application starts without errors but expected features are not available. No Flyway log output at startup. H2 console returns 404.

### Pitfall 2: H2/PostgreSQL SQL Incompatibility

**What goes wrong:** Migrations work on H2 in dev but fail on PostgreSQL in prod (or vice versa). Common failures: `SERIAL` type not recognized by H2, `MERGE` not recognized by PostgreSQL, case sensitivity differences.
**Why it happens:** H2's PostgreSQL compatibility mode (`MODE=PostgreSQL`) covers basic syntax but not all features. Developers write PostgreSQL-specific SQL assuming H2 will understand it.
**How to avoid:**
1. Set H2 URL with full compatibility flags: `jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`
2. Use standard SQL in migrations: `GENERATED BY DEFAULT AS IDENTITY` (not `SERIAL`), `BOOLEAN` (not bit tricks), `VARCHAR` (not `TEXT` for constrained columns)
3. Avoid PostgreSQL-specific types: `jsonb`, arrays, `uuid` generation functions
4. Use a single migration location (`classpath:db/migration`) with SQL that works on both
**Warning signs:** Migration errors mentioning "type not found" or "syntax error" on one database but not the other.

### Pitfall 3: Docker Compose Container Networking

**What goes wrong:** Application container cannot connect to PostgreSQL. Error: "Connection refused to localhost:5432."
**Why it happens:** Inside a Docker container, `localhost` refers to the container itself, not the host machine or other containers. The PostgreSQL container is reachable via its Docker Compose service name (`db`), not `localhost`.
**How to avoid:**
1. In `compose.yaml`, set `SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/userdb` (using service name `db`)
2. In `application-prod.yml`, use `${DB_HOST:localhost}` so it defaults to `localhost` for non-Docker runs but can be overridden
3. Docker Compose environment variables override Spring Boot config
**Warning signs:** "Connection refused" errors only when running inside Docker, works fine outside Docker.

### Pitfall 4: Flyway Runs Before Database is Ready

**What goes wrong:** Application fails on startup with "Connection refused" because Flyway tries to migrate before PostgreSQL is fully ready.
**Why it happens:** `depends_on` without health check condition only waits for the container to start, not for the database to accept connections.
**How to avoid:**
1. Add `healthcheck` with `pg_isready` to PostgreSQL service
2. Use `depends_on: db: condition: service_healthy` on the app service
3. Set `start_period` on the PostgreSQL healthcheck to avoid false negatives during initialization
**Warning signs:** Intermittent startup failures that resolve on retry. "Connection refused" in early log lines.

### Pitfall 5: .env File Committed to Git

**What goes wrong:** Database credentials, JWT secrets, and other sensitive values are exposed in version control.
**Why it happens:** `.env` is created and committed before adding it to `.gitignore`. Or `.gitignore` is missing the entry.
**How to avoid:**
1. Add `.env` to `.gitignore` BEFORE creating the file
2. Commit `.env.template` (with placeholder values) but NEVER `.env` (with real values)
3. The setup script copies template to `.env` only if `.env` does not already exist
**Warning signs:** `git status` shows `.env` as tracked. Credentials visible in repository history.

### Pitfall 6: Spring Security Blocks Everything by Default

**What goes wrong:** After adding `spring-boot-starter-security`, every endpoint returns 401 or redirects to a login page. The smoke test page, H2 console, and actuator health endpoint are all blocked.
**Why it happens:** Spring Security's default configuration requires authentication for ALL endpoints. In Phase 1, there is no login form or user store yet.
**How to avoid:** Create a minimal `SecurityConfig` that permits access to essential paths during bootstrap:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/actuator/health", "/h2-console/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin())) // H2 console uses frames
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        return http.build();
    }
}
```
This will be replaced with the full dual SecurityFilterChain in a later phase.
**Warning signs:** 401/403 errors on pages that should be public. H2 console loads but shows blank frame.

### Pitfall 7: Spring Boot Docker Compose Module Conflicts

**What goes wrong:** Port conflicts or stale containers when mixing Spring Boot's Docker Compose support with manual `docker compose up`.
**Why it happens:** Spring Boot's `spring-boot-docker-compose` module auto-starts compose services during application startup (default lifecycle: `start-and-stop`). If you also run `docker compose up` manually, you get conflicts.
**How to avoid:** Choose one approach:
1. **Let Spring Boot manage:** Remove `spring-boot-docker-compose` from prod profile; use in dev only
2. **Manual management:** Set `spring.docker.compose.lifecycle-management=start-only` or disable entirely with `spring.docker.compose.enabled=false`
3. **Shared services:** When sharing compose services between apps, use `start-only` lifecycle
**Warning signs:** "Port already in use" errors. Containers left running after app shutdown.

## Code Examples

Verified patterns from official sources:

### Spring Boot Application Entry Point

```java
// Source: Spring Boot 4 conventions + Spring Modulith
package com.example.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Module package-info.java Files (Skeleton)

```java
// src/main/java/com/example/usermanagement/shared/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {}
)
package com.example.usermanagement.shared;
```

```java
// src/main/java/com/example/usermanagement/user/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared" }
)
package com.example.usermanagement.user;
```

```java
// src/main/java/com/example/usermanagement/auth/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }
)
package com.example.usermanagement.auth;
```

### Modularity Verification Test

```java
// Source: Spring Modulith official documentation
package com.example.usermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

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
}
```

### Minimal Smoke Test Page (Thymeleaf)

```html
<!-- src/main/resources/templates/index.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>User Management</title>
    <link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.3/css/bootstrap.min.css}">
</head>
<body>
    <div class="container mt-5">
        <h1>User Management System</h1>
        <p class="lead">Application is running successfully.</p>
        <div class="alert alert-info">
            <strong>Profile:</strong> <span th:text="${@environment.activeProfiles[0] ?: 'default'}">unknown</span>
        </div>
    </div>
</body>
</html>
```

### Home Controller (Minimal)

```java
package com.example.usermanagement.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
```

### .gitignore Essentials

```gitignore
# Environment
.env

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# Build
target/
*.class
*.jar
*.war

# OS
.DS_Store
Thumbs.db

# Logs
*.log
```

## State of the Art

| Old Approach (Boot 3.x) | Current Approach (Boot 4.x) | When Changed | Impact |
|--------------------------|----------------------------|--------------|--------|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | Boot 4.0.0 (Nov 2025) | Must rename dependency |
| `flyway-core` on classpath triggers auto-config | `spring-boot-starter-flyway` required | Boot 4.0.0 (Nov 2025) | Must add new starter |
| H2 console bundled in `spring-boot-autoconfigure` | `spring-boot-h2console` separate module | Boot 4.0.0 (Nov 2025) | Must add new dependency |
| `com.fasterxml.jackson` (Jackson 2) | `tools.jackson` (Jackson 3) | Boot 4.0.0 (Nov 2025) | Package namespace changed |
| Spring Modulith 1.x | Spring Modulith 2.0.x | Nov 2025 | BOM version 2.0.1 for Boot 4 |
| `spring-boot-autoconfigure` monolith | 70+ focused modules | Boot 4.0.0 (Nov 2025) | Add explicit starters |
| Java 17 minimum | Java 17 minimum (21 recommended) | Boot 4.0.0 (Nov 2025) | Java 17 baseline unchanged from Boot 3 |

**Deprecated/outdated:**
- `spring-boot-starter-web`: Use `spring-boot-starter-webmvc` instead
- `spring-boot-autoconfigure` as a single dependency: Use individual starters
- `spring-boot-starter-classic`: Migration bridge only, not for new projects
- Direct `flyway-core` dependency without starter: No auto-configuration in Boot 4

## Open Questions

Things that could not be fully resolved:

1. **Spring Boot Docker Compose module behavior with compose.yaml vs docker-compose.yml**
   - What we know: Spring Boot auto-detects Docker Compose files and can manage the lifecycle
   - What's unclear: Whether Spring Boot 4 changed the filename convention or added new features
   - Recommendation: Name the file `compose.yaml` (Docker Compose v2 convention) and test auto-detection

2. **Exact Jackson 3 import changes affecting this phase**
   - What we know: Group IDs changed from `com.fasterxml.jackson` to `tools.jackson`. Several Spring Boot classes renamed.
   - What's unclear: Whether any Phase 1 code (application config, actuator) is affected by Jackson 3 changes
   - Recommendation: Phase 1 has minimal custom Jackson usage; should be fine. Flag for later phases if custom serializers are needed.

3. **Spring Boot 4 test companion starters**
   - What we know: Boot 4 introduces `spring-boot-starter-*-test` companions for each starter
   - What's unclear: Whether omitting test starters (e.g., `spring-boot-starter-webmvc-test`) causes test failures in Phase 1
   - Recommendation: Start without test companions; add if test context setup fails

4. **H2 console Spring Security frame-options handling in Spring Security 7**
   - What we know: H2 console uses iframes; Spring Security blocks frames by default
   - What's unclear: Whether Spring Security 7 changed the frame-options API
   - Recommendation: Use `.headers(h -> h.frameOptions(f -> f.sameOrigin()))` and verify at implementation time

## Sources

### Primary (HIGH confidence)
- [Spring Boot 4.0.0 GA Announcement](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/) - GA release confirmed, key features
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes) - Dependency versions: Flyway 11.11, Jackson 3, Hibernate 7.1, Tomcat 11, Jakarta EE 11
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - Java 17 baseline, modularization details, starter renames, Flyway starter requirement
- [Spring Modulith 2.0.1 Release](https://spring.io/blog/2025/12/20/spring-modulith-2-0-1-1-4-6-and-1-3-12-released/) - Boot 4 compatible version confirmed
- [Spring Modulith Reference: Appendix](https://docs.spring.io/spring-modulith/reference/appendix.html) - Compatibility matrix: Modulith 2.0 compiled against Boot 4
- [Flyway H2 Documentation](https://documentation.red-gate.com/flyway/reference/database-driver-reference/h2) - H2 support built into flyway-core
- [Spring Boot Database Initialization Docs](https://docs.spring.io/spring-boot/how-to/data-initialization.html) - `{vendor}` placeholder for migration locations
- [Spring Boot Docker Compose Docs](https://docs.spring.io/spring-boot/how-to/docker-compose.html) - Lifecycle management, service detection, JDBC labels
- [Maven Repository: spring-boot-h2console](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-h2console) - Artifact naming confirmed

### Secondary (MEDIUM confidence)
- [Dan Vega: Spring Boot 4 Modularization](https://www.danvega.dev/blog/2025/12/12/spring-boot-4-modularization) - Practical guide on new starters, H2 console fix
- [H2 Console in Spring Boot 4 (Medium)](https://medium.com/@raushan1156/h2-console-not-working-in-spring-boot-4-0-0-7873e20c82d5) - Confirmed spring-boot-h2console requirement
- [Dan Vega: PgAdmin Docker Compose](https://www.danvega.dev/blog/pgadmin-docker-compose) - PgAdmin integration pattern
- [Docker Compose Health Checks Guide](https://last9.io/blog/docker-compose-health-checks/) - pg_isready healthcheck pattern
- [envsubst Guide](https://karandeepsingh.ca/posts/leveraging-envsubst-in-bash-scripts-for-automation/) - Template processing best practices
- [Flyway in Spring Boot 4.x (Medium)](https://pranavkhodanpur.medium.com/flyway-migrations-in-spring-boot-4-x-what-changed-and-how-to-configure-it-correctly-dbe290fa4d47) - Confirmed spring-boot-starter-flyway requirement

### Tertiary (LOW confidence)
- Spring Boot `.env` native support: Still not available natively (GitHub issue #24229 open since 2020). Use Docker Compose's native `.env` support and Spring `${VAR}` placeholders instead.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Spring Boot 4.0.0 GA verified, release notes and migration guide confirmed exact versions
- Architecture: HIGH - Spring Modulith 2.0.1 GA confirmed for Boot 4, profile-based config is standard Spring
- Pitfalls: HIGH - Boot 4 modularization pitfalls verified through multiple sources and official migration guide
- Docker Compose: MEDIUM - Patterns verified from multiple community sources, no Boot 4-specific changes found
- .env management: MEDIUM - Standard approach, no native Spring Boot support confirmed

**Research date:** 2026-01-28
**Valid until:** 2026-02-28 (30 days -- stack is stable GA)
