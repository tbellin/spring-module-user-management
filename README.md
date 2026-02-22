# Spring Boot User Management Server

A Spring Boot 4 user management server with Spring Modulith architecture, JWT authentication, and dual-database support (H2 for development, PostgreSQL for production).

## Features

- Self-registration with email verification
- JWT-based authentication (login/logout) with remember-me
- Password management (change, forgot, reset via email)
- User profile (view, edit)
- Admin user management (CRUD, search, filter, enable/disable)
- Role-based access control (ADMIN, USER)
- REST API under `/api/v1/` with Swagger UI documentation
- Thymeleaf pages with Bootstrap 5
- Spring Modulith enforced module boundaries
- Dual environment: H2 (dev) / PostgreSQL + Docker Compose (prod)

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 4.0.1, Spring Framework 7 |
| Language | Java 21 |
| Architecture | Spring Modulith (Auth, User, Shared modules) |
| Security | Spring Security, JWT (JJWT), BCrypt |
| Database | H2 (dev), PostgreSQL (prod), Flyway migrations |
| UI | Thymeleaf, Bootstrap 5 |
| API Docs | SpringDoc OpenAPI 3.0.1, Swagger UI |
| Build | Maven (wrapper included) |
| Deployment | Docker Compose (App + PostgreSQL + PgAdmin) |

## Quick Start

```bash
# Clone and setup
git clone <repo-url>
cd <project-dir>
./bin/setup.sh          # Generate .env from template
# Edit .env with your SMTP credentials

# Dev mode (H2 in-memory database)
./bin/run-dev.sh

# Prod mode (Docker Compose stack)
./bin/run-prod.sh
```

Then open:
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

See [Setup Guide](doc/01-setup.md) for detailed instructions.

## Scripts

| Script | Purpose |
|--------|---------|
| `bin/run-dev.sh` | Start in dev mode (H2, local) |
| `bin/run-prod.sh` | Start production stack (Docker Compose) |
| `bin/test-api.sh` | Run cURL API tests |
| `bin/setup.sh` | First-time project setup |
| `bin/env.sh` | Environment and config management CLI |
| `bin/check-port.sh` | Check/free port conflicts |

## Test User

Pre-seeded admin account for dev mode:

| Field | Value |
|-------|-------|
| Email | `tizianobellin@yahoo.com` |
| Password | `password123` |
| Role | ADMIN |
| Email Verified | Yes |

## Documentation

| Document | Description |
|----------|-------------|
| [Setup Guide](doc/01-setup.md) | Prerequisites, installation, first run |
| [Architecture](doc/02-architecture.md) | Module structure, tech stack, design decisions |
| [API Reference](doc/03-api-reference.md) | Endpoint catalog with cURL examples |
| [Deployment](doc/04-deployment.md) | Docker Compose production deployment |

## API Testing

Start the application first, then run the test suite:

```bash
./bin/test-api.sh
```

See [API Reference](doc/03-api-reference.md) for the full endpoint catalog with cURL examples.

## License

This project is for educational purposes.
