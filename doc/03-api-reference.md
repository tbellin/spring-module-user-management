# API Reference

Complete catalog of REST API endpoints with request/response examples and cURL commands.

## Overview

All REST endpoints are under `/api/v1/`. Authentication uses JWT Bearer tokens. Interactive API documentation is available via Swagger UI.

| Resource | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI Spec (JSON) | http://localhost:8080/v3/api-docs |

## Authentication

### Obtaining a Token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "tizianobellin@yahoo.com", "password": "password123", "rememberMe": false}'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "tizianobellin@yahoo.com",
  "displayName": "tiziano",
  "roles": ["ROLE_ADMIN"]
}
```

### Using a Token

Include the token in the `Authorization` header for all authenticated requests:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users/me
```

### Token Lifetime

| Type | Duration | When |
|------|----------|------|
| Standard | 1 hour | `rememberMe: false` |
| Remember-me | 7 days | `rememberMe: true` |

Tokens are invalidated automatically if the user changes their password.

---

## Public Endpoints

No authentication required.

### POST /api/v1/auth/register

Register a new user account.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123!",
    "firstName": "Jane",
    "lastName": "Doe"
  }'
```

**Response:** `201 Created`

```json
{
  "id": 2,
  "email": "newuser@example.com",
  "displayName": "Jane",
  "firstName": "Jane",
  "lastName": "Doe",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "emailVerified": false
}
```

> A verification email is sent automatically. The account cannot log in until the email is verified.

---

### POST /api/v1/auth/login

Authenticate and receive a JWT token.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tizianobellin@yahoo.com",
    "password": "password123",
    "rememberMe": false
  }'
```

**Response:** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "tizianobellin@yahoo.com",
  "displayName": "tiziano",
  "roles": ["ROLE_ADMIN"]
}
```

---

### POST /api/v1/auth/resend-verification

Resend the email verification link. Rate-limited to one request per email per 60 seconds.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{"email": "newuser@example.com"}'
```

**Response:** `200 OK`

> Always returns 200 regardless of whether the email exists (SEC-01 compliance -- no user enumeration).

---

### POST /api/v1/auth/forgot-password

Request a password reset email. Rate-limited to one request per email per 60 seconds.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "tizianobellin@yahoo.com"}'
```

**Response:** `200 OK`

> Always returns 200 regardless of whether the email exists (SEC-01 compliance).

---

### POST /api/v1/auth/reset-password

Reset password using a token received via email.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "abc123-reset-token",
    "newPassword": "NewSecurePass456!",
    "confirmPassword": "NewSecurePass456!"
  }'
```

**Response:** `200 OK`

---

### GET /actuator/health

Application health check endpoint.

**Request:**

```bash
curl -s http://localhost:8080/actuator/health
```

**Response:** `200 OK`

```json
{
  "status": "UP"
}
```

---

## Authenticated Endpoints

Require a valid JWT token in the `Authorization: Bearer <token>` header.

### GET /api/v1/users/me

Get the authenticated user's profile.

**Request:**

```bash
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <token>"
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "email": "tizianobellin@yahoo.com",
  "displayName": "tiziano",
  "firstName": "tiziano",
  "lastName": null,
  "roles": ["ROLE_ADMIN"],
  "enabled": true,
  "emailVerified": true
}
```

---

### PUT /api/v1/users/me

Update the authenticated user's profile.

**Request:**

```bash
curl -s -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Tiziano B.",
    "firstName": "Tiziano",
    "lastName": "Bellin"
  }'
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "email": "tizianobellin@yahoo.com",
  "displayName": "Tiziano B.",
  "firstName": "Tiziano",
  "lastName": "Bellin",
  "roles": ["ROLE_ADMIN"],
  "enabled": true,
  "emailVerified": true
}
```

---

### POST /api/v1/auth/change-password

Change the authenticated user's password.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/change-password \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "password123",
    "newPassword": "NewSecurePass456!",
    "confirmPassword": "NewSecurePass456!"
  }'
```

**Response:** `200 OK`

> After changing password, all previously issued JWT tokens are invalidated. You must log in again to get a new token.

---

## Admin Endpoints

Require a valid JWT token with `ROLE_ADMIN`.

### GET /api/v1/admin/users

List users with optional filtering, searching, and pagination.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `search` | string | Search across email, first name, last name |
| `role` | string | Filter by role (e.g. `ROLE_ADMIN`, `ROLE_USER`) |
| `status` | string | Filter by status (`active`, `inactive`) |
| `page` | int | Page number (0-based, default: 0) |
| `size` | int | Page size (default: 20) |
| `sort` | string | Sort field and direction (e.g. `email,asc`) |

**Request:**

```bash
curl -s "http://localhost:8080/api/v1/admin/users?page=0&size=10&sort=email,asc" \
  -H "Authorization: Bearer <token>"
```

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "email": "tizianobellin@yahoo.com",
      "displayName": "tiziano",
      "firstName": "tiziano",
      "lastName": null,
      "roles": ["ROLE_ADMIN"],
      "enabled": true,
      "emailVerified": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

**Filtered request:**

```bash
curl -s "http://localhost:8080/api/v1/admin/users?search=tiziano&role=ROLE_ADMIN&status=active" \
  -H "Authorization: Bearer <token>"
```

---

### POST /api/v1/admin/users

Create a new user (admin invite). An email with a set-password link is sent to the new user.

**Request:**

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invited@example.com",
    "username": "invited",
    "firstName": "Invited",
    "lastName": "User",
    "enabled": true,
    "roles": ["ROLE_USER"]
  }'
```

**Response:** `201 Created`

```json
{
  "id": 3,
  "email": "invited@example.com",
  "displayName": "Invited",
  "firstName": "Invited",
  "lastName": "User",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "emailVerified": true
}
```

---

### PUT /api/v1/admin/users/{id}

Update an existing user's profile and roles.

**Request:**

```bash
curl -s -X PUT http://localhost:8080/api/v1/admin/users/2 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "roles": ["ROLE_USER", "ROLE_ADMIN"]
  }'
```

**Response:** `200 OK`

```json
{
  "id": 2,
  "email": "newuser@example.com",
  "displayName": "Updated",
  "firstName": "Updated",
  "lastName": "Name",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "enabled": true,
  "emailVerified": true
}
```

---

### PATCH /api/v1/admin/users/{id}/status

Enable or disable a user account.

**Request:**

```bash
curl -s -X PATCH http://localhost:8080/api/v1/admin/users/2/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

**Response:** `200 OK`

> Admins cannot disable their own account (returns 400).

---

## Error Responses

All error responses use the RFC 9457 ProblemDetail format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/auth/register"
}
```

### Common Status Codes

| Code | Meaning | When |
|------|---------|------|
| `400` | Bad Request | Validation errors, invalid input |
| `401` | Unauthorized | Missing or invalid JWT token |
| `403` | Forbidden | Valid token but insufficient permissions |
| `404` | Not Found | Resource does not exist |
| `409` | Conflict | Duplicate resource (e.g. email already registered) |
| `500` | Internal Server Error | Unexpected server error (generic message, details logged) |

---

## Swagger UI

For the definitive, always-up-to-date API documentation generated from source code annotations, visit:

**http://localhost:8080/swagger-ui.html**

Swagger UI provides:
- Interactive "Try it out" for every endpoint
- Request/response schema documentation
- Authentication support (click "Authorize" and paste your JWT token)
- Auto-generated from `@Operation`, `@Schema`, and `@Parameter` annotations in the source code
