---
phase: 01-project-bootstrap--infrastructure
plan: 03
subsystem: infra
tags: [spring-boot, h2, postgresql, flyway, spring-profiles, jpa, actuator]

# Dependency graph
requires:
  - phase: 01-project-bootstrap--infrastructure (plan 01)
    provides: Maven project skeleton with Spring Boot 4.0.1 and resource directory structure
provides:
  - Common Spring Boot configuration (application.yml)
  - H2 dev profile with PostgreSQL compatibility mode (application-dev.yml)
  - PostgreSQL prod profile with env var substitution (application-prod.yml)
  - Flyway migration support across both profiles
affects: [01-04, 01-05, 01-06, 02-security-foundation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Spring profiles for environment-specific database config"
    - "H2 PostgreSQL compatibility mode for dev/prod parity"
    - "Flyway manages schema, Hibernate validates only (ddl-auto: validate)"
    - "Environment variable substitution for production secrets"

key-files:
  created:
    - src/main/resources/application.yml
    - src/main/resources/application-dev.yml
    - src/main/resources/application-prod.yml
  modified: []

key-decisions:
  - "open-in-view: false to prevent lazy loading in views (performance best practice)"
  - "ddl-auto: validate with Flyway managing schema migrations"
  - "H2 URL flags: MODE=PostgreSQL, DATABASE_TO_LOWER=TRUE, DEFAULT_NULL_ORDERING=HIGH for max compatibility"
  - "Docker Compose integration disabled in both profiles (handled externally)"

patterns-established:
  - "Profile convention: dev profile for H2, prod profile for PostgreSQL"
  - "Flyway migration location: classpath:db/migration (consistent across profiles)"
  - "Production secrets via ${ENV_VAR} substitution, no defaults for credentials"

# Metrics
duration: 2min
completed: 2026-01-28
---

# Phase 1 Plan 3: Spring Profiles Summary

**Dual-database Spring profiles with H2 PostgreSQL-compatible dev mode and PostgreSQL prod mode via env var substitution**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-28T17:31:01Z
- **Completed:** 2026-01-28T17:32:23Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Common configuration with Flyway enabled, JPA validation mode, and Actuator health endpoints
- H2 dev profile with full PostgreSQL compatibility (MODE, case-insensitive identifiers, null ordering)
- PostgreSQL prod profile with secure env var substitution and no credential defaults

## Task Commits

Each task was committed atomically:

1. **Task 1: Create application.yml (common configuration)** - `06fae5a` (feat)
2. **Task 2: Create application-dev.yml (H2 configuration)** - `f5f5f0b` (feat)
3. **Task 3: Create application-prod.yml (PostgreSQL configuration)** - `12d1657` (feat)

## Files Created/Modified
- `src/main/resources/application.yml` - Common config: app name, JPA settings, Flyway, Actuator endpoints
- `src/main/resources/application-dev.yml` - H2 in-memory DB with PostgreSQL compatibility, H2 console, SQL logging
- `src/main/resources/application-prod.yml` - PostgreSQL with DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD env vars

## Decisions Made
- open-in-view set to false (avoids lazy loading issues in controller layer, Spring Boot best practice)
- ddl-auto set to validate (Flyway handles all schema changes, Hibernate only validates consistency)
- H2 URL uses MODE=PostgreSQL + DATABASE_TO_LOWER=TRUE + DEFAULT_NULL_ORDERING=HIGH for maximum dev/prod parity
- DB_USERNAME and DB_PASSWORD have no defaults in prod (must be explicitly provided, prevents accidental insecure defaults)
- Docker Compose integration disabled in both profiles (compose.yaml manages containers externally)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Profile configuration ready for Flyway migration files (plan 04)
- Database connection settings ready for Docker Compose PostgreSQL service (plan 05)
- Actuator health endpoint available for Docker healthcheck configuration

---
*Phase: 01-project-bootstrap--infrastructure*
*Completed: 2026-01-28*
