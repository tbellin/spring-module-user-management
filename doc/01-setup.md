# Setup Guide

How to go from a fresh clone to a running application in dev mode (H2) or prod mode (Docker + PostgreSQL).

## Prerequisites

| Tool | Version | Required For | Notes |
|------|---------|--------------|-------|
| Java | 21+ | All modes | OpenJDK or Eclipse Temurin recommended |
| Maven | 3.9+ (via wrapper) | All modes | Included as `./mvnw` -- no separate install needed |
| Docker + Docker Compose | v2+ | Prod mode | Docker Desktop includes Compose v2 |
| jq | any | API tests only | `brew install jq` on macOS |
| SMTP server | -- | Email features | Real SMTP credentials required (e.g. Gmail, Mailgun) |

## Quick Start (Dev Mode)

Dev mode uses an embedded H2 database -- no Docker required.

```bash
# 1. Clone the repository
git clone <repo-url> && cd user-management

# 2. Run initial setup (generates .env, builds project)
./bin/setup.sh

# 3. Edit .env with your real values
#    At minimum, set SMTP credentials for email features:
#    MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD
#    See .env.example for descriptions of all variables.

# 4. (Optional) Create .env.local for personal overrides
#    This file is gitignored and takes precedence over .env.
cp .env .env.local
# Edit .env.local with your personal values

# 5. Start the application
./bin/run-dev.sh

# 6. Open in browser
open http://localhost:8080
```

Once running, the following are available:

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Application home page |
| http://localhost:8080/h2-console | H2 database console (dev only) |
| http://localhost:8080/swagger-ui.html | Swagger UI -- interactive API docs |
| http://localhost:8080/v3/api-docs | OpenAPI 3 JSON spec |

## Quick Start (Prod Mode)

Prod mode runs the full stack in Docker: Spring Boot app + PostgreSQL + PgAdmin.

```bash
# 1. Same environment setup as dev (steps 1-4 above)
#    Make sure .env has production-strength values:
#    - Strong DB_PASSWORD
#    - Strong JWT_SECRET (256-bit base64)
#    - Real SMTP credentials

# 2. Start the production stack
./bin/run-prod.sh
# (or explicitly: ./bin/run-prod.sh up)

# 3. Open in browser
open http://localhost:8080
```

Once running:

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Application (served from Docker) |
| http://localhost:5050 | PgAdmin web UI |
| http://localhost:8080/swagger-ui.html | Swagger UI |

### Management Commands

```bash
./bin/run-prod.sh up       # Start the stack (default)
./bin/run-prod.sh down     # Stop and remove containers
./bin/run-prod.sh logs     # Follow container logs
./bin/run-prod.sh status   # Show container status
./bin/run-prod.sh restart  # Full restart with rebuild
./bin/run-prod.sh help     # Show all available commands
```

## Environment Configuration

The project uses a template-based configuration system. Config files are never committed directly -- only `.template` files are tracked in Git.

### File Roles

| File | Purpose | In Git? |
|------|---------|---------|
| `.env.example` | Reference doc listing all variables with descriptions | Yes |
| `.env.template` | Template for generating `.env` (used by `setup.sh`) | Yes |
| `.env` | Active configuration with your real values | No (gitignored) |
| `.env.local` | Personal overrides, loaded after `.env` | No (gitignored) |

### Template System

Config files use `@VARIABLE@` placeholders that get replaced with values from `.env`:

```
# In compose.yaml.template:
POSTGRES_DB: @DB_NAME@

# After substitution, compose.yaml becomes:
POSTGRES_DB: userdb
```

The `substitute-all` command processes all templates listed in `bin/env-templates.list`:

```bash
./bin/env.sh substitute-all   # Generate all config files from templates
./bin/env.sh check            # Check if generated files are up to date
./bin/env.sh clean            # Remove all generated files
./bin/env.sh show             # Display current variable values
source ./bin/env.sh load      # Export variables to current shell
```

### Template Files

The following templates are processed (from `bin/env-templates.list`):

| Template | Generated File |
|----------|---------------|
| `src/main/resources/application.yml.template` | `application.yml` |
| `src/main/resources/application-dev.yml.template` | `application-dev.yml` |
| `src/main/resources/application-prod.yml.template` | `application-prod.yml` |
| `Dockerfile.template` | `Dockerfile` |
| `compose.yaml.template` | `compose.yaml` |
| `docker/pgadmin/servers.json.template` | `servers.json` |
| `src/main/resources/banner.txt.template` | `banner.txt` |

### Environment Variables

All variables are defined in `.env.example`. Key groups:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `userdb` | Database name |
| `DB_USERNAME` | `appuser` | Database user |
| `DB_PASSWORD` | *(required)* | Database password |
| `PGADMIN_EMAIL` | `admin@example.com` | PgAdmin login email |
| `PGADMIN_PASSWORD` | *(required)* | PgAdmin login password |
| `PGADMIN_PORT` | `5050` | PgAdmin web UI port |
| `APP_PORT` | `8080` | Application HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `JWT_SECRET` | *(required)* | JWT signing key (base64, 256-bit) |
| `JWT_EXPIRATION_MS` | `3600000` | JWT token lifetime (1 hour) |
| `JWT_REMEMBER_ME_EXPIRATION_MS` | `604800000` | Remember-me token lifetime (7 days) |
| `MAIL_HOST` | `smtp.example.com` | SMTP server host |
| `MAIL_PORT` | `587` | SMTP server port |
| `MAIL_USERNAME` | *(required)* | SMTP auth username |
| `MAIL_PASSWORD` | *(required)* | SMTP auth password |
| `MAIL_FROM` | `noreply@jbeltsolution.com` | Sender email address |
| `VERIFICATION_EXPIRATION_HOURS` | `24` | Email verification token lifetime |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL for email links |

## Test User

In dev mode, the H2 database is seeded with a pre-configured admin user:

| Field | Value |
|-------|-------|
| Username | `tiziano` |
| Email | `tizianobellin@yahoo.com` |
| Password | `password123` |
| Role | `ADMIN` |
| Email Verified | Yes |

This user is available immediately after startup -- no registration or verification needed. Use it for testing all features including admin operations.

> **Note:** This seed data exists only in dev mode (H2). Production (PostgreSQL) starts with an empty database.

## Running API Tests

The project includes a comprehensive cURL test suite that exercises all REST API endpoints:

```bash
# Requires: application running + jq installed
./bin/test-api.sh
```

The test script uses the pre-seeded admin user to authenticate and tests registration, login, profile management, password flows, and admin operations. Results are displayed with colored pass/fail output.

You can override defaults via environment variables:

```bash
BASE_URL=http://localhost:9090 ./bin/test-api.sh
ADMIN_EMAIL=custom@example.com ADMIN_PASSWORD=secret ./bin/test-api.sh
```
