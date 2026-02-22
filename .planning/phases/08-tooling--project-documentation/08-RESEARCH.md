# Phase 8: Tooling & Project Documentation - Research

**Researched:** 2026-02-22
**Domain:** Shell scripting, cURL API testing, Markdown documentation
**Confidence:** HIGH

## Summary

Phase 8 is a "polish and package" phase with no new application code. It produces five deliverables: (1) a dev-mode run script, (2) a prod-mode run script using Docker Compose, (3) cURL test scripts that exercise every API endpoint, (4) comprehensive documentation in `doc/`, and (5) a `README.md` that links everything together.

The project already has significant infrastructure in `bin/` (env.sh CLI, setup.sh, generate-config.sh, check-port.sh, run-spring-dev-mode.sh) and a well-defined template/env system (`@VARIABLE@` substitution via `bin/env.sh substitute-all`). The existing `run-spring-dev-mode.sh` is a minimal 3-line script that just invokes `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. It lacks prerequisite checks, environment loading, and port-conflict detection. The prod script does not exist at all. No cURL test scripts exist. The `doc/` directory contains only a tangential file (`rate-limit-options.md`). No `README.md` exists.

**Primary recommendation:** Build on the existing env.sh/setup.sh infrastructure. The dev script should source env.sh, run substitute-all, check prerequisites, and start Spring Boot. The prod script should similarly prepare the environment then orchestrate Docker Compose. The cURL test script should be a single self-contained bash script that registers a user, logs in, captures the JWT token, and exercises all endpoints sequentially with pass/fail assertions. Documentation should be four focused Markdown files covering setup, architecture, API, and deployment.

## Standard Stack

### Core

| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Bash | 3.2+ (macOS default) | Shell scripting for all bin/ scripts | Universally available, project already uses bash scripts |
| cURL | 7.x+ | HTTP client for API testing | Available on all dev machines, best for reproducible API tests |
| jq | 1.6+ | JSON parsing in cURL test scripts | Standard tool for extracting values (JWT tokens) from JSON responses |
| Docker Compose | v2 (plugin) | Production orchestration | Already configured with compose.yaml.template |
| Maven Wrapper | 3.9.9 | Build tool invocation | Already in project as ./mvnw |

### Supporting

| Tool | Purpose | When to Use |
|------|---------|-------------|
| `bin/env.sh` | Environment loading and template substitution | Both run scripts must source it before starting |
| `bin/setup.sh` | First-time project setup | Referenced from README quick-start |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| cURL scripts | Postman/Newman collection | cURL is simpler, no extra tooling, version-controllable as plain text |
| jq for JSON parsing | grep/sed/awk | jq is purpose-built for JSON, much more reliable for extracting nested values |
| Separate doc files | Single long README | Separate files keep README focused, doc/ scales better |

## Architecture Patterns

### Recommended Script Structure

```
bin/
  env.sh                    # [EXISTS] CLI: load, show, substitute-all, check, clean
  env-templates.list        # [EXISTS] Template file registry
  setup.sh                  # [EXISTS] First-time setup
  generate-config.sh        # [EXISTS] Generates .env from .env.template
  check-port.sh             # [EXISTS] Port scanning utility
  run-spring-dev-mode.sh    # [EXISTS - NEEDS ENHANCEMENT] Dev mode launcher
  run-dev.sh                # [NEW - TOOL-02] Enhanced dev mode run script
  run-prod.sh               # [NEW - TOOL-03] Docker Compose prod launcher
  test-api.sh               # [NEW - TOOL-05] cURL test script for all features

doc/
  01-setup.md               # [NEW - TOOL-06] Prerequisites, installation, first run
  02-architecture.md        # [NEW - TOOL-06] Module structure, tech stack, design decisions
  03-api-reference.md       # [NEW - TOOL-06] Endpoint catalog with cURL examples
  04-deployment.md          # [NEW - TOOL-06] Docker Compose prod deployment guide

README.md                   # [NEW - TOOL-07] Project overview, quick-start, doc links
```

### Pattern 1: Robust Bash Script Header

**What:** Every script should have a consistent, defensive header.
**When to use:** All new and enhanced scripts in `bin/`.
**Example:**
```bash
#!/usr/bin/env bash
# bin/run-dev.sh -- Start application in development mode
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Color helpers (consistent with env.sh)
if [ -t 1 ]; then
    green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
    red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
    yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
else
    green()  { printf '%s\n' "$*"; }
    red()    { printf '%s\n' "$*"; }
    yellow() { printf '%s\n' "$*"; }
fi
```
**Confidence:** HIGH -- this pattern is already used in `env.sh` and `setup.sh`.

### Pattern 2: cURL Test Script with Token Chain

**What:** A sequential test script that passes state (JWT tokens, user IDs) between test cases.
**When to use:** The test-api.sh script.
**Example flow:**
```
1. Health check (GET /actuator/health) -- verify app is running
2. Register user (POST /api/v1/auth/register) -- capture email
3. Login (POST /api/v1/auth/login) -- capture JWT token
4. Get profile (GET /api/v1/users/me) with Bearer token
5. Update profile (PUT /api/v1/users/me) with Bearer token
6. Change password (POST /api/v1/auth/change-password) with Bearer token
7. Forgot password (POST /api/v1/auth/forgot-password) -- SEC-01 response
8. Admin: list users (GET /api/v1/admin/users) -- login as admin first
9. Admin: create user (POST /api/v1/admin/users)
10. Admin: update user (PUT /api/v1/admin/users/{id})
11. Admin: toggle status (PATCH /api/v1/admin/users/{id}/status)
```
**Confidence:** HIGH -- all endpoints are verified from source code review.

### Pattern 3: Pass/Fail Assertion Helper

**What:** A reusable function that checks HTTP status codes and prints colored pass/fail.
**Example:**
```bash
PASS=0; FAIL=0; TOTAL=0

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" -eq "$expected" ]; then
        green "  PASS: $test_name (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        red "  FAIL: $test_name (expected HTTP $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}
```
**Confidence:** HIGH -- standard shell scripting pattern.

### Anti-Patterns to Avoid

- **Hardcoded URLs in tests:** Use a `BASE_URL` variable (default `http://localhost:8080`) so tests work in both dev and prod.
- **Hardcoded credentials in scripts:** Use the seed data admin (`tizianobellin@yahoo.com` / `password123`) but make them overridable via environment variables.
- **Not checking prerequisites:** Dev script must verify Java 21+ and Maven wrapper; prod script must verify Docker and Docker Compose.
- **Silent failures in cURL:** Always use `-s` (silent) with `-w "%{http_code}"` to capture status codes. Never let cURL errors go undetected.
- **Storing generated files in git:** The `.gitignore` already excludes generated config files. Any new generated files must also be excluded.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON parsing in bash | grep/sed on JSON | `jq` | JSON is not line-oriented; grep/sed break on multiline, escaped quotes, nested objects |
| HTTP testing framework | Custom assertion library | Simple status code checking with cURL `-w` | Don't over-engineer; pass/fail on HTTP status is sufficient for smoke tests |
| Documentation site generator | Jekyll/Hugo/MkDocs | Plain Markdown files in `doc/` | Project is not large enough to warrant a doc site; Markdown renders on GitHub |
| API documentation | Manual endpoint listing | Reference to Swagger UI at `/swagger-ui.html` | OpenAPI spec is already generated from code annotations |

**Key insight:** This phase is about shell scripts and Markdown files. The temptation to over-engineer (test frameworks, doc generators, CI pipelines) should be resisted. Simple, readable bash scripts that a developer can understand in 5 minutes are the goal.

## Common Pitfalls

### Pitfall 1: Email Verification Blocks Login in Tests

**What goes wrong:** The cURL test registers a user but cannot log in because email is not verified, and there is no programmatic way to verify from the API.
**Why it happens:** The registration flow requires email verification before login is allowed. In dev mode with H2, the seed user (`tiziano` / `password123`) is already verified, but newly registered users are not.
**How to avoid:** The test script should:
1. Use the pre-seeded admin user for admin tests (already verified in V2__seed_dev_data.sql)
2. For registration tests, assert the 201 response but note that login will fail until verified
3. Document that full end-to-end testing of the register-verify-login flow requires manual email verification
**Warning signs:** Test script hangs or gets 401 after registration.

### Pitfall 2: Port Already in Use

**What goes wrong:** Dev script fails to start because port 8080 is already occupied by a previous run or another service.
**Why it happens:** Spring Boot does not automatically detect and release ports. The existing `check-port.sh` can kill processes but requires sudo.
**How to avoid:** The dev run script should check if the port is in use BEFORE starting Spring Boot and warn the user with a clear message. Do NOT automatically kill processes (too aggressive).
**Warning signs:** "Address already in use" error on startup.

### Pitfall 3: Template Substitution Not Run Before Start

**What goes wrong:** Application starts with `@VARIABLE@` literal strings in config files, causing cryptic errors (invalid JDBC URL, JWT secret parse failures).
**Why it happens:** Developer forgets to run `./bin/env.sh substitute-all` after pulling or after editing `.env`.
**How to avoid:** Both run scripts (dev and prod) should call `source ./bin/env.sh load` and `./bin/env.sh substitute-all` automatically before starting the application.
**Warning signs:** Application crashes at startup with configuration-related errors.

### Pitfall 4: Docker Compose .env Resolution Conflicts

**What goes wrong:** Docker Compose reads its own `.env` file and the variable values clash with the template-generated compose.yaml.
**Why it happens:** Docker Compose automatically loads `.env` from the project root. Since compose.yaml is ALREADY generated from the template with resolved values, the `.env` variables are not needed by Docker Compose. But if env vars are also exported, they can override the resolved values in unexpected ways.
**How to avoid:** The prod script should NOT export `.env` variables into the shell when running `docker compose`. The compose.yaml is already resolved. Alternatively, always use `docker compose --env-file /dev/null` to prevent double-resolution.
**Warning signs:** Docker containers use wrong database credentials or port numbers.

### Pitfall 5: cURL Tests Assume App is Running

**What goes wrong:** Test script runs but gets "connection refused" because the application is not started.
**Why it happens:** Test script does not check if the app is actually listening.
**How to avoid:** First test should be a health check (`GET /actuator/health`) with a retry/wait loop (up to 30 seconds). If health check fails, abort with a clear message.
**Warning signs:** All tests fail with "connection refused".

### Pitfall 6: jq Not Installed

**What goes wrong:** cURL test script fails immediately because `jq` is not available.
**Why it happens:** `jq` is not installed by default on macOS (requires Homebrew) or many Linux distros.
**How to avoid:** Check for `jq` at the start of test-api.sh and provide install instructions. Alternatively, provide a fallback using `grep`/`python3 -c` for basic JSON extraction, but this adds complexity.
**Warning signs:** "command not found: jq" error.

## Code Examples

### Dev Run Script Structure (TOOL-02)

```bash
#!/usr/bin/env bash
# bin/run-dev.sh -- Start application in development mode (H2 database)
set -euo pipefail

# ... header and color helpers ...

# 1. Check prerequisites
command -v java >/dev/null 2>&1 || { red "ERROR: Java is required"; exit 1; }
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
[ "$JAVA_VERSION" -ge 17 ] || { red "ERROR: Java 17+ required, found $JAVA_VERSION"; exit 1; }

# 2. Load environment and generate configs
source ./bin/env.sh load
./bin/env.sh substitute-all

# 3. Check port availability
PORT="${APP_PORT:-8080}"
if lsof -i ":$PORT" >/dev/null 2>&1; then
    red "ERROR: Port $PORT is already in use"
    yellow "  Run: ./bin/check-port.sh $PORT go"
    exit 1
fi

# 4. Build and run
green "Starting in dev mode (H2) on port $PORT..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
**Confidence:** HIGH -- follows existing patterns from setup.sh and env.sh.

### Prod Run Script Structure (TOOL-03)

```bash
#!/usr/bin/env bash
# bin/run-prod.sh -- Start full production stack with Docker Compose
set -euo pipefail

# ... header and color helpers ...

# 1. Check prerequisites
command -v docker >/dev/null 2>&1 || { red "ERROR: Docker is required"; exit 1; }
docker compose version >/dev/null 2>&1 || { red "ERROR: Docker Compose v2 required"; exit 1; }

# 2. Load environment and generate configs (compose.yaml, Dockerfile, etc.)
source ./bin/env.sh load
./bin/env.sh substitute-all

# 3. Build and start
green "Starting production stack..."
docker compose up --build -d

# 4. Show status
docker compose ps
green "Application: http://localhost:${APP_PORT:-8080}"
green "PgAdmin:     http://localhost:${PGADMIN_PORT:-5050}"
green "Swagger UI:  http://localhost:${APP_PORT:-8080}/swagger-ui.html"
```
**Confidence:** HIGH -- compose.yaml.template already defines all services.

### cURL Test Pattern (TOOL-05)

```bash
# Login as admin and capture token
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"'"$ADMIN_EMAIL"'","password":"'"$ADMIN_PASSWORD"'","rememberMe":false}')

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')
assert_status "Admin login" 200 "$HTTP_CODE"

TOKEN=$(echo "$BODY" | jq -r '.token')
AUTH_HEADER="Authorization: Bearer $TOKEN"

# Use token for protected endpoint
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/v1/users/me" \
    -H "$AUTH_HEADER")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
assert_status "Get admin profile" 200 "$HTTP_CODE"
```
**Confidence:** HIGH -- verified against actual controller endpoints and DTO structures.

### Documentation Structure (TOOL-06)

Each doc file follows this pattern:
```markdown
# Title

Brief description of what this document covers.

## Section 1
Content...

## Section 2
Content...

---
*Last updated: YYYY-MM-DD*
```
**Confidence:** HIGH -- standard Markdown documentation.

## Existing Asset Inventory

This is critical for the planner to understand what already exists vs. what needs to be created.

### Already Exists (DO NOT recreate)

| File | Status | Notes |
|------|--------|-------|
| `bin/env.sh` | Complete | 6 subcommands, color helpers, template processing |
| `bin/env-templates.list` | Complete | Lists all 7 templates |
| `bin/setup.sh` | Complete | First-time setup (Java check, .env generation, Maven build) |
| `bin/generate-config.sh` | Complete | Generates .env from .env.template |
| `bin/check-port.sh` | Complete | Port scanning with optional kill |
| `bin/run-spring-dev-mode.sh` | Minimal (3 lines) | Just runs mvnw, no env loading, no checks |
| `.env.example` | Complete | Documents all variables |
| `.env.template` | Complete | Template for .env generation |
| `compose.yaml.template` | Complete | Full stack: db + pgadmin + app |
| `Dockerfile.template` | Complete | Multi-stage build, non-root user |
| `.gitignore` | Complete | Excludes .env, generated configs |

### Needs to Be Created

| Deliverable | Requirement | Key Details |
|-------------|-------------|-------------|
| `bin/run-dev.sh` | TOOL-02 | Enhanced dev script with prereq checks, env loading, port check |
| `bin/run-prod.sh` | TOOL-03 | Docker Compose orchestration with prereq checks, env loading |
| `bin/test-api.sh` | TOOL-05 | cURL tests for all 11+ API endpoints with pass/fail reporting |
| `doc/01-setup.md` | TOOL-06 | Prerequisites, installation, first run for dev and prod |
| `doc/02-architecture.md` | TOOL-06 | Modulith structure, tech stack, design decisions |
| `doc/03-api-reference.md` | TOOL-06 | Endpoint catalog linking to Swagger UI |
| `doc/04-deployment.md` | TOOL-06 | Docker prod deployment guide |
| `README.md` | TOOL-07 | Quick-start, feature summary, links to all doc files |

### Decision: run-dev.sh vs Enhancing run-spring-dev-mode.sh

The existing `run-spring-dev-mode.sh` is 3 lines with no structure. Two options:
1. **Create new `run-dev.sh`** -- cleaner, follows naming convention with `run-prod.sh`
2. **Enhance existing `run-spring-dev-mode.sh`** -- maintains continuity

**Recommendation:** Create new `run-dev.sh` as the "official" dev script. Leave `run-spring-dev-mode.sh` as-is (it still works as a quick-and-dirty launcher). The new script name pairs with `run-prod.sh` for consistency.

## Complete API Endpoint Catalog

This is the definitive list for cURL test coverage, verified from controller source code:

### Public Endpoints (No Auth Required)

| Method | Path | Request Body | Expected Status |
|--------|------|-------------|-----------------|
| GET | `/actuator/health` | none | 200 |
| POST | `/api/v1/auth/register` | `{email, password, firstName, lastName}` | 201 |
| POST | `/api/v1/auth/login` | `{email, password, rememberMe}` | 200 (returns JWT) |
| POST | `/api/v1/auth/resend-verification` | `{email}` | 200 |
| POST | `/api/v1/auth/forgot-password` | `{email}` | 200 |
| POST | `/api/v1/auth/reset-password` | `{token, newPassword, confirmPassword}` | 200 |

### Authenticated Endpoints (Bearer JWT)

| Method | Path | Request Body | Expected Status |
|--------|------|-------------|-----------------|
| GET | `/api/v1/users/me` | none | 200 |
| PUT | `/api/v1/users/me` | `{displayName, firstName, lastName}` | 200 |
| POST | `/api/v1/auth/change-password` | `{currentPassword, newPassword, confirmPassword}` | 200 |

### Admin Endpoints (Bearer JWT + ROLE_ADMIN)

| Method | Path | Request Body | Expected Status |
|--------|------|-------------|-----------------|
| GET | `/api/v1/admin/users` | none (query params: search, role, status, page, size) | 200 |
| POST | `/api/v1/admin/users` | `{email, username, firstName, lastName, enabled, roles}` | 201 |
| PUT | `/api/v1/admin/users/{id}` | `{firstName, lastName, roles}` | 200 |
| PATCH | `/api/v1/admin/users/{id}/status` | `{enabled: true/false}` | 200 |

### Error Cases Worth Testing

| Scenario | Method | Path | Expected Status |
|----------|--------|------|-----------------|
| Login with wrong password | POST | `/api/v1/auth/login` | 401 |
| Register duplicate email | POST | `/api/v1/auth/register` | 409 |
| Access admin endpoint as USER | GET | `/api/v1/admin/users` | 403 |
| Access protected endpoint without JWT | GET | `/api/v1/users/me` | 401 |
| Register with invalid email | POST | `/api/v1/auth/register` | 400 |

**Total test cases:** ~18-20 (11 happy path + 5-8 error cases)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `docker-compose` (standalone) | `docker compose` (plugin) | Docker Compose v2, 2022+ | Scripts must use `docker compose` (space, not hyphen) |
| `#!/bin/bash` | `#!/usr/bin/env bash` | Best practice | More portable across systems; existing scripts are mixed |
| Manual README | README linking to doc/ | Common in mature projects | Keeps README scannable, details in dedicated files |

## Open Questions

1. **What to do about email-dependent test flows?**
   - What we know: Registration sends a real email. Login requires verified email. There is no API endpoint to verify programmatically.
   - What's unclear: Whether the cURL test should try to test the full register-verify-login flow or just test each endpoint individually.
   - Recommendation: Test registration (expect 201) and login with pre-seeded user separately. Document that full flow requires manual email check. The seed user `tiziano` is already verified and has ADMIN role, making it perfect for testing all authenticated endpoints.

2. **Should the existing `run-spring-dev-mode.sh` be deleted or kept?**
   - What we know: It works but is minimal. A new `run-dev.sh` will be the official script.
   - What's unclear: Whether to remove the old script to avoid confusion or keep it for backward compatibility.
   - Recommendation: Keep it. No harm in having both. The README and docs will reference `run-dev.sh` as the official one.

3. **Should `doc/rate-limit-options.md` be kept, moved, or removed?**
   - What we know: It exists but is informal (written in Italian, contains emojis, not a standard doc).
   - What's unclear: Whether it is still relevant or was a scratch note.
   - Recommendation: Leave it as-is. It is not part of the new documentation deliverables. Do not reference it from README.

4. **Should jq be a hard requirement or optional with fallback?**
   - What we know: jq makes JSON parsing reliable. Not all systems have it.
   - Recommendation: Make it a hard requirement. Add a check at the start of test-api.sh with install instructions (`brew install jq` on macOS, `apt-get install jq` on Linux).

## Sources

### Primary (HIGH confidence)
- Project source code review: All controllers, DTOs, security config, existing scripts, templates, and migrations
- Existing `bin/env.sh` -- verified CLI interface and patterns
- Existing `compose.yaml.template` -- verified Docker Compose service definitions
- Existing `Dockerfile.template` -- verified multi-stage build setup
- Seed data `V2__seed_dev_data.sql` -- verified test user credentials

### Secondary (MEDIUM confidence)
- Bash scripting best practices for `set -euo pipefail`, color helpers, prerequisite checks
- cURL testing patterns with `-w "%{http_code}"` for status code extraction

## Metadata

**Confidence breakdown:**
- Existing infrastructure: HIGH -- verified by reading all source files
- API endpoint catalog: HIGH -- verified from controller source code and DTOs
- Script patterns: HIGH -- following existing project conventions from env.sh/setup.sh
- Documentation structure: HIGH -- standard Markdown documentation patterns
- cURL test design: HIGH -- all request/response shapes verified from Java records

**Research date:** 2026-02-22
**Valid until:** 2026-04-22 (stable -- no external dependencies that change rapidly)
