---
phase: 01-project-bootstrap--infrastructure
plan: 12
subsystem: database
tags: [flyway, h2, postgresql, migrations, vendor-specific, seed-data, startup-banner]

# Dependency graph
requires:
  - phase: 01-05
    provides: Initial Flyway schema migration (V1__init_schema.sql)
  - phase: 01-08
    provides: Test infrastructure (ModularityTests pattern)
  - phase: 01-11
    provides: Vendor-specific Flyway config with {vendor} placeholder in application.yml
provides:
  - Vendor-specific Flyway migration directories (h2/ and postgresql/)
  - Dev seed data with test user tiziano and ADMIN role
  - Profile-aware startup banner component
  - Schema comparison test ensuring migration parity
affects: [phase-2-security, phase-3-auth, phase-4-email]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Vendor-specific Flyway migrations via {vendor} placeholder"
    - "Dev-only seed data in H2 migration directory"
    - "Schema comparison test for migration parity validation"
    - "Profile-aware startup logging via ApplicationReadyEvent"

key-files:
  created:
    - src/main/resources/db/migration/h2/V2__seed_dev_data.sql
    - src/main/java/com/example/usermanagement/shared/config/StartupBanner.java
    - src/test/java/com/example/usermanagement/SchemaComparisonTests.java
  modified: []

key-decisions:
  - "Dev seed data placed in h2/ directory only (dev-only, never runs in prod)"
  - "Schema comparison test filters out seed migrations from parity check"
  - "StartupBanner uses ApplicationReadyEvent for post-context-load logging"

patterns-established:
  - "Vendor-specific migration: structural migrations (Vn__schema) go in both h2/ and postgresql/; seed migrations go only in h2/"
  - "Schema parity: SchemaComparisonTests enforces identical table structures across vendors"

# Metrics
duration: 13min
completed: 2026-01-30
---

# Phase 1 Plan 12: Vendor-Specific Flyway Migrations Summary

**Vendor-specific Flyway migration directories with dev seed data, startup banner, and schema parity test**

## Performance

- **Duration:** 13 min
- **Started:** 2026-01-30T15:22:16Z
- **Completed:** 2026-01-30T15:35:26Z
- **Tasks:** 3
- **Files modified:** 3 created

## Accomplishments
- Flyway migrations split into h2/ and postgresql/ vendor-specific directories
- Dev seed data auto-creates test user tiziano with ADMIN role on H2 startup
- StartupBanner component logs mode, database type, and config status
- SchemaComparisonTests validates structural migration parity across vendors

## Task Commits

Each task was committed atomically:

1. **Task 1: Split Flyway migrations into vendor-specific directories** - Already in HEAD from 01-11 (vendor dirs, {vendor} placeholder, old migration removed)
2. **Task 2: Add dev seed data and startup banner** - `8e3f750` (feat)
3. **Task 3: Create schema comparison test** - `99fdb69` (test)

**Plan metadata:** (pending)

## Files Created/Modified
- `src/main/resources/db/migration/h2/V2__seed_dev_data.sql` - Dev-only seed data with test user tiziano and ADMIN role
- `src/main/java/com/example/usermanagement/shared/config/StartupBanner.java` - Profile-aware startup banner with mode and config detection
- `src/test/java/com/example/usermanagement/SchemaComparisonTests.java` - Validates H2 and PostgreSQL migrations have same structural versions and tables

## Decisions Made
- **Dev seed data placement:** V2__seed_dev_data.sql placed exclusively in h2/ directory. Since Flyway uses `{vendor}` placeholder, H2 only executes migrations from h2/ and PostgreSQL only from postgresql/. This naturally isolates dev seed data.
- **Schema comparison approach:** Plain unit test (no @SpringBootTest) that parses SQL files directly, extracts CREATE TABLE names, and compares sets. Fast (0.3s) and deterministic.
- **Seed migration filtering:** isStructuralMigration() filters by filename containing "seed" rather than version number, making the filter clear and extensible.
- **Task 1 overlap with 01-11:** Plan 01-11 already created vendor-specific directories, updated application.yml with {vendor} placeholder, and removed flyway.locations from templates. Task 1 of this plan only required verifying the state and removing the old shared migration (which 01-11's final commit already handled). No separate commit needed.

## Deviations from Plan

None - plan executed as written. Task 1 work was largely already completed by plan 01-11 (vendor-specific directories and configuration). Only Task 2 and Task 3 required new commits.

## Issues Encountered
- The old shared migration file `V1__init_schema.sql` was repeatedly restored and removed during plan 01-11 execution (commits b641ccd and 86c6203 then removed in 89b2eb8). By the time this plan executed, it was already cleanly removed from HEAD and vendor-specific directories were in place.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Database migration infrastructure complete with vendor-specific directories
- Dev environment auto-seeds test user for immediate development use
- Schema parity test ensures future migrations stay in sync across vendors
- Phase 1 (Project Bootstrap & Infrastructure) is now complete (12/12 plans)
- Ready to proceed to Phase 2 (Security Foundation)

---
*Phase: 01-project-bootstrap--infrastructure*
*Completed: 2026-01-30*
