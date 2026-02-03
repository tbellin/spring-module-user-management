---
phase: 02-security--api-foundation
plan: 03
subsystem: authentication
tags: [jwt, spring-security, bcrypt, password-encoding]

dependency-graph:
  requires: [02-02]
  provides: [jwt-service, user-details-service, password-encoder]
  affects: [02-04, 02-05]

tech-stack:
  added: []
  patterns: [service-layer-security, module-boundary-enforcement]

key-files:
  created:
    - src/main/java/com/example/usermanagement/auth/JwtService.java
    - src/main/java/com/example/usermanagement/auth/internal/CustomUserDetailsService.java
    - src/main/java/com/example/usermanagement/shared/config/PasswordConfig.java
  modified: []

decisions:
  - id: 02-03-D1
    choice: JwtService in auth package (public), CustomUserDetailsService in auth.internal (private)
    rationale: JwtService is the public API for JWT operations; CustomUserDetailsService is implementation detail

  - id: 02-03-D2
    choice: PasswordConfig in shared.config (not auth.internal)
    rationale: PasswordEncoder is cross-cutting concern needed by both auth (login) and user (registration) modules

  - id: 02-03-D3
    choice: Generic "Bad credentials" message in CustomUserDetailsService
    rationale: SEC-01 compliance prevents user enumeration attacks

metrics:
  duration: 2min
  completed: 2026-02-03
---

# Phase 02 Plan 03: Core Security Components Summary

JWT token service, Spring Security UserDetailsService, and BCrypt password encoder for authentication infrastructure.

## What Was Built

### JwtService (auth package - public API)
- **generateToken**: Creates JWT with subject, roles claim, issued-at, and expiration
- **extractUsername**: Retrieves subject from token
- **isTokenValid**: Validates token against UserDetails (username match + not expired)
- **extractClaim**: Generic claim extraction with resolver function
- Uses JJWT 0.12.x API (verifyWith, parseSignedClaims)
- Injects AppProperties for configurable secret and expiration

### CustomUserDetailsService (auth.internal - module-private)
- Implements Spring Security UserDetailsService
- Bridges Spring Security to user module via UserService
- Converts UserAuthDto to Spring Security User
- Maps roles to SimpleGrantedAuthority
- Respects module boundaries (calls UserService, not repositories)
- SEC-01 compliant: Uses "Bad credentials" message to prevent enumeration

### PasswordConfig (shared.config - cross-cutting)
- Defines BCryptPasswordEncoder bean
- Placed in shared for access by both auth and user modules
- Default strength (10) for security/performance balance

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| 02-03-D1 | JwtService public, CustomUserDetailsService internal | Clear module API boundary |
| 02-03-D2 | PasswordConfig in shared.config | Cross-cutting concern for auth and user modules |
| 02-03-D3 | Generic "Bad credentials" error | SEC-01: Prevent user enumeration attacks |

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 7d19511 | feat | add JwtService for token generation and validation |
| eb67852 | feat | add CustomUserDetailsService and PasswordConfig |

## Files Created

```
src/main/java/com/example/usermanagement/
├── auth/
│   ├── JwtService.java           # Public JWT API (97 lines)
│   └── internal/
│       └── CustomUserDetailsService.java  # Spring Security bridge (58 lines)
└── shared/
    └── config/
        └── PasswordConfig.java   # BCrypt bean (30 lines)
```

## Module Boundary Enforcement

- **auth -> user**: CustomUserDetailsService calls UserService (not UserRepository)
- **auth -> shared**: JwtService injects AppProperties for configuration
- **shared**: PasswordConfig provides cross-cutting PasswordEncoder bean

## Security Compliance

- **SEC-01**: Generic error message in CustomUserDetailsService prevents user enumeration
- **SUCCESS CRITERIA #2**: BCryptPasswordEncoder available for password hashing

## Next Phase Readiness

Ready for 02-04 (SecurityConfig):
- JwtService available for JwtAuthenticationFilter
- CustomUserDetailsService available for AuthenticationManager
- PasswordEncoder available for DaoAuthenticationProvider

## Test Coverage Note

Unit tests for JwtService and CustomUserDetailsService will be added in later phases when test infrastructure is established. Current verification is via compilation and manual review.
