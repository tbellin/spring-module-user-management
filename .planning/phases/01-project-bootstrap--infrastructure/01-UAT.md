---
status: testing
phase: 01-project-bootstrap--infrastructure
source: [01-01-SUMMARY.md, 01-02-SUMMARY.md, 01-03-SUMMARY.md, 01-04-SUMMARY.md, 01-05-SUMMARY.md, 01-06-SUMMARY.md, 01-07-SUMMARY.md, 01-08-SUMMARY.md]
started: 2026-01-30T01:10:00Z
updated: 2026-01-30T01:10:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

number: 1
name: Dev mode startup with H2
expected: |
  Run `./mvnw spring-boot:run` (or run from IDE with dev profile).
  Application starts without errors on port 8080.
  Visit http://localhost:8080/ — page loads with "Active Profile: dev" displayed.
  Visit http://localhost:8080/h2-console — H2 console login page appears.
awaiting: user response

## Tests

### 1. Dev mode startup with H2
expected: Run `./mvnw spring-boot:run` (or from IDE with dev profile). App starts on port 8080 without errors. http://localhost:8080/ shows "Active Profile: dev". http://localhost:8080/h2-console shows H2 console login.
result: [pending]

### 2. Flyway migration in dev mode
expected: During dev startup, logs show Flyway executing V1__init_schema.sql. H2 console (jdbc:h2:mem:userdb, user sa, no password) shows tables: APP_USER, APP_ROLE, USER_ROLE, VERIFICATION_TOKEN, PASSWORD_RESET_TOKEN. APP_ROLE contains ROLE_USER and ROLE_ADMIN rows.
result: [pending]

### 3. Setup script generates .env from template
expected: Run `bin/generate-config.sh`. A new `.env` file is created from `.env.template`. The file contains all variables (DB_PASSWORD, PGADMIN_PASSWORD, etc.) with CHANGE_ME placeholder values.
result: [pending]

### 4. Docker Compose full stack launch
expected: After editing `.env` with real passwords, run `docker compose up --build`. All 3 services (db, pgadmin, app) reach running state. `docker compose ps` shows db and app as "healthy". No crash-loops or restart cycles.
result: [pending]

### 5. Home page in prod mode (Docker)
expected: Visit http://localhost:8080/ while Docker stack is running. Page loads with Bootstrap styling, navbar, and "Active Profile: prod" displayed. No H2 console link visible (prod mode).
result: [pending]

### 6. PgAdmin web UI login
expected: Visit http://localhost:5050. PgAdmin login page appears. Log in with PGADMIN_EMAIL and PGADMIN_PASSWORD from your .env file. After login, the PgAdmin dashboard loads successfully.
result: [pending]

### 7. PgAdmin database connection
expected: In PgAdmin left sidebar, "User Management DB" server is pre-listed. Click it, enter DB_PASSWORD from .env. Connection succeeds. Expanding Databases > userdb > Schemas > public > Tables shows: app_user, app_role, user_role, verification_token, password_reset_token.
result: [pending]

### 8. Tests pass
expected: Run `./mvnw test`. All tests pass (ApplicationTests context load + ModularityTests module verification). No failures or errors in output.
result: [pending]

## Summary

total: 8
passed: 0
issues: 0
pending: 8
skipped: 0

## Gaps

[none yet]
