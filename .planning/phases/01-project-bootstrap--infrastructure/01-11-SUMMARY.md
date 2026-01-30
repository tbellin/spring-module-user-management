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
  - "Flyway locations centralized in application.yml with {vendor} path, removed from profile templates"
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

- **Duration:** 4 min
- **Started:** 2026-01-30T15:22:11Z
- **Completed:** 2026-01-30T15:26:00Z
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

**Plan metadata:** `7f3c9a8` (docs: complete plan)

**Fix commits:**
- `b641ccd` - Restored accidentally deleted V1 Flyway migration
- `d0e2a72` - Removed redundant flyway.locations from profile templates
- `86c6203` - Re-restored V1 Flyway migration file

## Files Created/Modified
- `src/main/resources/application-dev.yml.template` - H2 dev profile template with @DB_NAME@ for DB name consistency
- `src/main/resources/application-prod.yml.template` - PostgreSQL prod profile with @DB_HOST@, @DB_PORT@, @DB_NAME@, @DB_USERNAME@, @DB_PASSWORD@
- `Dockerfile.template` - Multi-stage Docker build with @APP_PORT@ for EXPOSE directive
- `compose.yaml.template` - Docker Compose with usermgmt- container prefixes, @VARIABLE@ placeholders, db-only documentation
- `docker/pgadmin/servers.json.template` - PgAdmin server auto-config with @DB_PORT@, @DB_NAME@, @DB_USERNAME@
- `bin/env-templates.list` - Registry of all 5 template file paths

## Decisions Made
- **Flyway locations centralized in application.yml:** The `flyway.locations` config was moved to `application.yml` with `{vendor}` path support (h2/ and postgresql/ subdirectories). Profile templates no longer override flyway config.
- **servers.json Port as integer:** The @DB_PORT@ placeholder in servers.json.template is unquoted, producing a JSON number value (not a string). This matches the PgAdmin JSON schema requirement.
- **db-only via service selection:** Instead of Docker Compose profiles (which have fundamental limitations -- services with profiles don't start by default), db-only mode is achieved via `docker compose up db pgadmin`. This preserves `docker compose up` starting all services while providing a clean db-only command.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Restored accidentally deleted V1 Flyway migration**
- **Found during:** Metadata commit
- **Issue:** V1__init_schema.sql was inadvertently removed from git tracking when staging the metadata commit, due to the file's deletion being present in the working tree
- **Fix:** Restored the file from prior commit and committed it back; also committed vendor-specific h2/ and postgresql/ migration variants that existed in working tree
- **Files modified:** src/main/resources/db/migration/V1__init_schema.sql, h2/V1__init_schema.sql, postgresql/V1__init_schema.sql
- **Committed in:** b641ccd, 86c6203

**2. [Rule 1 - Bug] Removed redundant flyway.locations from profile templates**
- **Found during:** Post-commit verification
- **Issue:** The user had centralized flyway.locations in application.yml with `{vendor}` path. Profile templates should not override this.
- **Fix:** Removed flyway.locations from both application-dev.yml.template and application-prod.yml.template
- **Files modified:** src/main/resources/application-dev.yml.template, src/main/resources/application-prod.yml.template
- **Committed in:** d0e2a72

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correct operation. No scope creep.

## Issues Encountered
- Working tree had pre-existing modifications (vendor-specific migrations, modified application.yml) that interfered with git staging. Required multiple fix commits to resolve.

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
