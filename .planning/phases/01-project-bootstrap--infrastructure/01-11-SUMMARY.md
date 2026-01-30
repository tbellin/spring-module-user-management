---
phase: 01-project-bootstrap--infrastructure
plan: 11
subsystem: infra
tags: [docker, templates, env-substitution, compose, pgadmin]

# Dependency graph
requires:
  - phase: 01-03
    provides: "application-dev.yml and application-prod.yml Spring profiles"
  - phase: 01-07
    provides: "Dockerfile and compose.yaml with Docker infrastructure"
  - phase: 01-10
    provides: "env.sh CLI with substitute-all command and env-templates.list"
provides:
  - "@VARIABLE@ template files for all config (application YMLs, Dockerfile, compose.yaml, servers.json)"
  - "env-templates.list populated with 5 template paths"
  - "Docker Compose with usermgmt- container name prefixes"
  - "servers.json.template for PgAdmin auto-configuration"
affects: ["01-12", "phase-2"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@VARIABLE@ template substitution for all config files"
    - "usermgmt- container name prefix convention"
    - "db-only mode via docker compose up db pgadmin"

key-files:
  created:
    - src/main/resources/application-dev.yml.template
    - src/main/resources/application-prod.yml.template
    - Dockerfile.template
    - compose.yaml.template
    - docker/pgadmin/servers.json.template
  modified:
    - bin/env-templates.list

key-decisions:
  - "Preserved flyway.locations in dev and prod templates (present in originals)"
  - "servers.json.template Port is unquoted integer (JSON number, not string)"
  - "db-only mode via service selection (docker compose up db pgadmin) instead of --profile flag"

patterns-established:
  - "All config files are .template with @VARIABLE@ placeholders, never committed with resolved values"
  - "env-templates.list is the single registry of all template files"
  - "Container names follow usermgmt-{service} convention"

# Metrics
duration: 2min
completed: 2026-01-30
---

# Phase 1 Plan 11: Config Template Conversion Summary

**Converted 5 config files to @VARIABLE@ templates with Docker Compose enhancements (usermgmt- prefixes, PgAdmin auto-config), registered in env-templates.list for env.sh substitute-all processing**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-30T15:22:11Z
- **Completed:** 2026-01-30T15:24:10Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Converted application-dev.yml, application-prod.yml, and Dockerfile to @VARIABLE@ template files
- Converted compose.yaml to template with usermgmt- container name prefixes and @VARIABLE@ placeholders
- Created servers.json.template for PgAdmin auto-configuration with DB credentials
- Populated env-templates.list with all 5 template paths
- Verified env.sh substitute-all generates all 5 config files from templates

## Task Commits

Each task was committed atomically:

1. **Task 1: Convert application YML and Dockerfile to templates** - `d4ccd3e` (feat)
2. **Task 2: Convert compose.yaml and servers.json to templates with Docker enhancements** - `1945079` (feat)

**Plan metadata:** [pending] (docs: complete plan)

## Files Created/Modified
- `src/main/resources/application-dev.yml.template` - H2 dev profile template with @DB_NAME@ for DB name consistency
- `src/main/resources/application-prod.yml.template` - PostgreSQL prod profile with @DB_HOST@, @DB_PORT@, @DB_NAME@, @DB_USERNAME@, @DB_PASSWORD@
- `Dockerfile.template` - Multi-stage Docker build with @APP_PORT@ for EXPOSE directive
- `compose.yaml.template` - Docker Compose with usermgmt- container prefixes, @VARIABLE@ placeholders, db-only documentation
- `docker/pgadmin/servers.json.template` - PgAdmin server auto-config with @DB_PORT@, @DB_NAME@, @DB_USERNAME@
- `bin/env-templates.list` - Registry of all 5 template file paths

## Decisions Made
- **Preserved flyway.locations in templates:** The original application-dev.yml and application-prod.yml both had `flyway.locations: classpath:db/migration`. This was preserved in the templates to maintain parity with the existing config.
- **servers.json Port as integer:** The @DB_PORT@ placeholder in servers.json.template is unquoted, producing a JSON number value (not a string). This matches the PgAdmin JSON schema requirement.
- **db-only via service selection:** Instead of Docker Compose profiles (which have fundamental limitations -- services with profiles don't start by default), db-only mode is achieved via `docker compose up db pgadmin`. This preserves `docker compose up` starting all services while providing a clean db-only command.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 5 template files are in place and registered in env-templates.list
- env.sh substitute-all generates all config files from templates
- Ready for 01-12 (final phase plan) to complete Phase 1
- Docker Compose infrastructure fully templated for consistent dev/prod environments

---
*Phase: 01-project-bootstrap--infrastructure*
*Completed: 2026-01-30*
