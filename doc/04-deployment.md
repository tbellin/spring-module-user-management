# Deployment Guide

How to deploy the application to production using Docker Compose.

## Production Stack

Docker Compose orchestrates three services:

| Service | Image | Container Name | Purpose |
|---------|-------|----------------|---------|
| `app` | Built from Dockerfile | `usermgmt-app` | Spring Boot application |
| `db` | `postgres:17-alpine` | `usermgmt-db` | PostgreSQL database |
| `pgadmin` | `dpage/pgadmin4:latest` | `usermgmt-pgadmin` | Database web UI |

The application container runs as a non-root user (`appuser`) for security. It uses a multi-stage Docker build with `eclipse-temurin:21-jdk` for compilation and `eclipse-temurin:21-jre` for the runtime image.

## Prerequisites

- Docker Engine installed and running
- Docker Compose v2 (included with Docker Desktop)
- Configured `.env` file with production values (see [Configuration Checklist](#configuration-checklist))

## Configuration Checklist

Before deploying to production, **all** of these variables in `.env` must be changed from their defaults:

| Variable | Requirement | Default |
|----------|-------------|---------|
| `DB_PASSWORD` | Strong password for PostgreSQL | *(empty -- must set)* |
| `DB_USERNAME` | Database user (change from default) | `appuser` |
| `JWT_SECRET` | Base64-encoded 256-bit random string | *(empty -- must set)* |
| `MAIL_HOST` | Real SMTP server hostname | `smtp.example.com` |
| `MAIL_PORT` | SMTP port (587 for TLS) | `587` |
| `MAIL_USERNAME` | SMTP authentication username | *(empty -- must set)* |
| `MAIL_PASSWORD` | SMTP authentication password | *(empty -- must set)* |
| `PGADMIN_EMAIL` | PgAdmin login email | `admin@example.com` |
| `PGADMIN_PASSWORD` | PgAdmin login password | *(empty -- must set)* |
| `APP_BASE_URL` | Public URL for email links | `http://localhost:8080` |

### Generating a JWT Secret

```bash
# Generate a random 256-bit base64 secret:
openssl rand -base64 32
```

### Example Production .env

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=userdb
DB_USERNAME=produser
DB_PASSWORD=very-strong-random-password

JWT_SECRET=dGhpcyBpcyBhIHNlY3VyZSByYW5kb20gc2VjcmV0IGtleQ==
JWT_EXPIRATION_MS=3600000
JWT_REMEMBER_ME_EXPIRATION_MS=604800000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=yourapp@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@yourdomain.com

PGADMIN_EMAIL=admin@yourdomain.com
PGADMIN_PASSWORD=strong-pgadmin-password
PGADMIN_PORT=5050

APP_PORT=8080
APP_BASE_URL=https://yourdomain.com
VERIFICATION_EXPIRATION_HOURS=24
```

## Starting the Stack

```bash
# Start (builds app image, starts all services)
./bin/run-prod.sh

# Or explicitly:
./bin/run-prod.sh up
```

### What Happens

1. `bin/env.sh load` -- exports all `.env` variables (and `.env.local` overrides) into the shell
2. `bin/env.sh substitute-all` -- generates config files from `.template` files:
   - `compose.yaml` from `compose.yaml.template`
   - `Dockerfile` from `Dockerfile.template`
   - `application-prod.yml` from `application-prod.yml.template`
   - All other templates in `bin/env-templates.list`
3. `docker compose up --build -d` -- builds the app image and starts all containers in the background
4. PostgreSQL starts first (healthcheck: `pg_isready`)
5. PgAdmin starts after PostgreSQL is healthy
6. App starts after PostgreSQL is healthy, runs Flyway migrations, then serves traffic

## Management Commands

All commands are subcommands of `./bin/run-prod.sh`:

```bash
./bin/run-prod.sh up       # Start the full stack (default)
./bin/run-prod.sh down     # Stop and remove all containers
./bin/run-prod.sh logs     # Follow container logs (Ctrl+C to exit)
./bin/run-prod.sh status   # Show container status
./bin/run-prod.sh restart  # Stop, rebuild, and restart
./bin/run-prod.sh help     # Show usage
```

### Direct Docker Compose Commands

For advanced operations, use `docker compose` directly:

```bash
# View logs for a specific service
docker compose logs -f app

# Start only the database (no app)
docker compose up db pgadmin

# Shell into the app container
docker compose exec app sh

# Shell into the database
docker compose exec db psql -U appuser -d userdb

# Rebuild only the app (after code changes)
docker compose up --build -d app
```

## Health Checks

Docker Compose uses health checks to manage startup order and monitor service health.

| Service | Health Check | Interval | Start Period |
|---------|-------------|----------|-------------|
| `db` | `pg_isready -U <user> -d <db>` | 10s | 10s |
| `app` | `curl -f http://localhost:8080/actuator/health` | 15s | 40s |

The `depends_on` configuration with `condition: service_healthy` ensures:
- PgAdmin waits for PostgreSQL to be ready
- App waits for PostgreSQL to be ready
- Flyway migrations run only after the database is accepting connections

### Checking Health Manually

```bash
# All containers
docker compose ps

# App health endpoint
curl -s http://localhost:8080/actuator/health | jq

# Database connectivity
docker compose exec db pg_isready -U appuser -d userdb
```

## Data Persistence

Docker volumes persist data across container restarts:

| Volume | Purpose |
|--------|---------|
| `pgdata` | PostgreSQL database files |
| `pgadmindata` | PgAdmin configuration and saved queries |

To reset all data:

```bash
./bin/run-prod.sh down
docker volume rm $(docker volume ls -q --filter name=pgdata)
docker volume rm $(docker volume ls -q --filter name=pgadmindata)
./bin/run-prod.sh up
```

## Troubleshooting

### Port Conflict

**Symptom:** `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Fix:** Either stop the process using the port or change `APP_PORT` in `.env`:

```bash
# Find what's using port 8080
lsof -i :8080

# Or change the port
echo "APP_PORT=9090" >> .env.local
./bin/run-prod.sh restart
```

### Template Not Substituted (Stale Config)

**Symptom:** Application fails to connect to database, config values show `@VARIABLE@` literals.

**Fix:** Re-run template substitution:

```bash
./bin/env.sh clean
source ./bin/env.sh load
./bin/env.sh substitute-all
./bin/run-prod.sh restart
```

### Docker Not Running

**Symptom:** `Cannot connect to the Docker daemon`

**Fix:** Start Docker Desktop (macOS/Windows) or the Docker service (Linux):

```bash
# Linux
sudo systemctl start docker

# macOS/Windows -- open Docker Desktop application
```

### Database Connection Refused

**Symptom:** App container restarts with `Connection refused` to PostgreSQL.

**Fix:** Check that the database is healthy and the startup order is correct:

```bash
# Check container status
docker compose ps

# Check database logs
docker compose logs db

# If db is not healthy, restart it
docker compose restart db
```

The `depends_on: condition: service_healthy` in `compose.yaml` should handle startup ordering automatically. If the database keeps failing, check `DB_PASSWORD` matches between the app and database environment variables.

### App Container Keeps Restarting

**Symptom:** `usermgmt-app` shows status `Restarting` repeatedly.

**Fix:** Check application logs for the root cause:

```bash
docker compose logs app --tail 50
```

Common causes:
- Missing or invalid `JWT_SECRET`
- SMTP server unreachable (check `MAIL_HOST` and `MAIL_PORT`)
- Flyway migration failure (schema conflict with existing data)
