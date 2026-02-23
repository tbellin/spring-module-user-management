# Architecture Research

**Domain:** Spring Boot 4 + Spring Modulith — v1.2 Foundation Upgrade integration
**Researched:** 2026-02-23
**Confidence:** HIGH (direct codebase inspection — 77 Java files, all config files, all scripts verified)

---

## Standard Architecture

### System Overview

```
┌────────────────────────────────────────────────────────────────┐
│                     Spring Modulith App                        │
│   com.example.usermanagement  →  org.jbelt.module (rename)    │
├───────────┬───────────────────────┬────────────────────────────┤
│  auth     │  user                 │  shared                    │
│  module   │  module               │  module                    │
│  (JWT,    │  (profile, admin CRUD)│  (DTOs, config, email,     │
│  flows)   │                       │   exceptions)              │
│  depends: │  depends:             │  depends: (none)           │
│  user,    │  shared               │                            │
│  shared   │                       │                            │
├───────────┴───────────────────────┴────────────────────────────┤
│                     Persistence Layer                          │
│  H2 (dev, MODE=PostgreSQL)  /  PostgreSQL (prod via Docker)    │
│  Flyway: db/migration/{h2,postgresql}/VN__*.sql               │
├────────────────────────────────────────────────────────────────┤
│  Config Layer: .env + .template files → env.sh substitute-all │
│  (.env, .env.local) → @VARIABLE@ → application.yml, etc.      │
└────────────────────────────────────────────────────────────────┘

New in v1.2:
  .github/workflows/ci.yml  (GitHub Actions — NEW FILE)
  pom.xml: groupId + version  (MODIFIED)
  .env.example: Gmail SMTP docs  (MODIFIED)
```

### Component Responsibilities

| Component | Responsibility | Files Touched by v1.2 |
|-----------|---------------|----------------------|
| Application entry point | @SpringBootApplication, @ConfigurationPropertiesScan | Application.java — package declaration |
| auth module | JWT, auth flows, admin invite, email verification, password reset | All .java in auth/** — package declarations + imports |
| user module | Profile CRUD, admin CRUD, user entity | All .java in user/** — package declarations + imports |
| shared module | AppProperties, EmailService, exceptions, DTOs | All .java in shared/** — package declarations + imports |
| Spring Modulith @ApplicationModule | Module boundary enforcement | 3 package-info.java files — package declaration only |
| ModularityTests | Verifies module boundaries at test time | ModularityTests.java — package + ApplicationModules.of(Application.class) ref |
| Config templating | @VARIABLE@ substitution from .env | application.yml.template — no change; .env.example — Gmail section update |
| CI pipeline | mvn verify on push/PR | .github/workflows/ci.yml — NEW FILE |
| Maven build | groupId, artifactId, version | pom.xml — groupId org.jbelt, version 1.2.0-SNAPSHOT |
| Docker image | Multi-stage build from Maven, runs JAR | Dockerfile.template — no package ref, NOT modified |
| Banner | Startup log display of project name + version | banner.txt.template — uses @PROJECT_NAME@ + @PROJECT_VERSION@ env vars |

---

## Recommended Project Structure After Rename

```
src/
├── main/
│   └── java/
│       └── org/jbelt/module/             # renamed from com/example/usermanagement/
│           ├── Application.java          # package org.jbelt.module
│           ├── auth/
│           │   ├── package-info.java     # package org.jbelt.module.auth
│           │   ├── JwtService.java
│           │   ├── AuthResponse.java
│           │   └── internal/
│           │       ├── ... (all auth internal classes)
│           │       ├── password/
│           │       └── verification/
│           ├── user/
│           │   ├── package-info.java     # package org.jbelt.module.user
│           │   ├── UserService.java
│           │   ├── UserAuthDto.java
│           │   └── internal/
│           └── shared/
│               ├── package-info.java     # package org.jbelt.module.shared
│               ├── config/
│               ├── dto/
│               ├── email/
│               ├── exception/
│               └── web/
└── test/
    └── java/
        └── org/jbelt/module/             # renamed from com/example/usermanagement/
            ├── ApplicationTests.java
            ├── ModularityTests.java
            ├── SchemaComparisonTests.java
            ├── auth/
            └── user/

.github/
└── workflows/
    └── ci.yml                            # NEW — GitHub Actions CI

pom.xml                                   # groupId: org.jbelt, version: 1.2.0-SNAPSHOT
```

### Structure Rationale

- **Directory path mirrors package:** Java convention — org.jbelt.module lives at src/main/java/org/jbelt/module/. The old com/example/usermanagement/ directory tree is deleted; a new org/jbelt/module/ tree is created. All 62 main + 15 test Java files move.
- **Spring Modulith detection is automatic:** Modulith discovers modules by scanning subdirectories of the main application class's package. No YAML config is needed. After rename, org.jbelt.module.auth, org.jbelt.module.user, org.jbelt.module.shared are detected automatically from classpath structure.
- **.github/workflows/** is the GitHub Actions standard location — no alternative path is valid.

---

## Architectural Patterns

### Pattern 1: Exhaustive File-by-File Rename (Package Propagation)

**What:** When renaming the root Java package, every occurrence of the old package string must be updated. Spring Boot 4 + Spring Modulith have NO runtime component-scan YAML settings to update (auto-configuration handles it from classpath), but all Java source files must be touched.

**When to use:** Any package rename that changes the root package path.

**Trade-offs:** IDE refactoring ("Rename Package" in IntelliJ/VS Code) handles 95% atomically. The remaining 5% risk is string-literal occurrences — none exist in this codebase (verified: no "com.example.usermanagement" string literals in non-comment source code).

**Complete list of files requiring change:**

| Category | Count | What Changes |
|----------|-------|-------------|
| Main Java sources (src/main/java/) | 62 files | package com.example.usermanagement.* → package org.jbelt.module.*; all import com.example.usermanagement.* → import org.jbelt.module.* |
| Test Java sources (src/test/java/) | 15 files | Same as above |
| package-info.java (3 files) | 3 files | package declaration only — see note below |
| pom.xml | 1 file | groupId com.example → org.jbelt; version 0.0.1-SNAPSHOT → 1.2.0-SNAPSHOT |
| Directory tree | Entire com/example/usermanagement/ tree | Physical filesystem restructure — old path deleted, new org/jbelt/module/ path created |
| banner.txt.template | 0 changes | Uses @PROJECT_NAME@ env var, not package name |
| Dockerfile.template | 0 changes | No package reference — copies target/*.jar by glob |
| application.yml.template and profile YAMLs | 0 changes | No component-scan YAML setting — Spring Boot 4 auto-configures from classpath |
| Flyway SQL migrations | 0 changes | No Java package references in SQL |
| .env, .env.example, .env.template | 0 changes for rename | Gmail update is separate task |
| compose.yaml.template | 0 changes | No package references |
| Shell scripts in bin/ | 0 changes | Verified: zero com.example references found |
| doc/ markdown files | 0 changes | Verified: zero com.example references found |
| README.md | 0 changes for rename | Repo URL update is separate task |
| META-INF/spring | Not present | No Spring factories or AOT service files found in this project |

**Key insight — @ApplicationModule args do NOT change:** The allowedDependencies values in package-info.java use relative module names ("user", "shared"), not fully-qualified package names. Spring Modulith resolves these relative to the root application package. They survive the rename unchanged:

Before rename — in auth/package-info.java:
```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }
)
package com.example.usermanagement.auth;
```

After rename — only the package declaration changes:
```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }   // UNCHANGED
)
package org.jbelt.module.auth;
```

### Pattern 2: Gmail SMTP Integration (Config-Only Change)

**What:** Gmail App Password flow requires specific SMTP settings. The core application.yml.template already has all correct STARTTLS properties. The only change is updating .env.example and .env.template with Gmail-specific defaults and setup documentation.

**Current application.yml.template SMTP block — NO CHANGE NEEDED:**

```yaml
spring:
  mail:
    host: @MAIL_HOST@         # → smtp.gmail.com
    port: @MAIL_PORT@         # → 587
    username: @MAIL_USERNAME@ # → Gmail address
    password: @MAIL_PASSWORD@ # → App Password (16-char, no spaces)
    properties:
      "[mail.smtp.auth]": true
      "[mail.smtp.starttls.enable]": true
      "[mail.smtp.connectiontimeout]": 5000
      "[mail.smtp.timeout]": 3000
      "[mail.smtp.writetimeout]": 5000
```

**Updated .env.example Gmail section (target state):**

```bash
# ===========================================
# Mail Configuration — Gmail SMTP
# ===========================================
# Gmail requires an App Password (NOT your Google account password).
# Steps to set up:
#   1. Enable 2-Step Verification: https://myaccount.google.com/security
#   2. Generate App Password: https://myaccount.google.com/apppasswords
#      Select app: "Mail", device: "Other (custom name)" → copy 16-char password
#   3. Set MAIL_USERNAME to your full Gmail address
#   4. Set MAIL_PASSWORD to the 16-char App Password (no spaces)
#   5. MAIL_FROM can be the same Gmail address or an alias
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.address@gmail.com
MAIL_PASSWORD=       # WARNING: App Password, not your Google account password
MAIL_FROM=your.address@gmail.com
```

**Files to modify:** Only .env.example and .env.template. The application.yml.template SMTP block is already correct for Gmail — no change.

**Files NOT modified:** EmailService.java, AppProperties.java, application.yml.template SMTP block — all work as-is with Gmail credentials.

### Pattern 3: GitHub Actions CI Workflow (H2-Only, No External Services)

**What:** mvn verify in CI must run the full test suite. All 15 existing test files use @ActiveProfiles("dev") which activates application-dev.yml with H2 in-memory database. CI needs no PostgreSQL service container, no Docker, no real SMTP.

**How tests handle email in CI:** Tests that involve email use @MockitoBean EmailService emailService (mocking the service layer, not JavaMailSender itself). This:
- Prevents actual SMTP calls during tests
- Keeps JavaMailSender bean live (required by Spring Boot MailHealthContributorAutoConfiguration for actuator health endpoint)
- Means CI does NOT need real MAIL_* credentials — placeholder values suffice

**Why MAIL_* env vars are still required in CI:** Spring Boot's MailSenderAutoConfiguration creates a JavaMailSender bean eagerly from spring.mail.host. If MAIL_HOST resolves to blank/empty, bean creation fails and the application context crashes before any test runs — even though EmailService would have been mocked. Setting placeholder strings prevents this failure without enabling real SMTP.

**CI workflow file location:** .github/workflows/ci.yml

**Minimal working workflow content:**

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and verify
        run: ./mvnw verify -B
        env:
          SPRING_PROFILES_ACTIVE: dev
          # Mail placeholders — JavaMailSender bean requires host at context load.
          # EmailService is mocked in tests; no real SMTP connection is made.
          MAIL_HOST: smtp.gmail.com
          MAIL_PORT: "587"
          MAIL_USERNAME: ci@example.com
          MAIL_PASSWORD: ci-placeholder
          MAIL_FROM: ci@example.com
          # JWT required by AppProperties binding
          JWT_SECRET: dGVzdC1jaS1zZWNyZXQta2V5LWZvci1jaS10ZXN0aW5nLW9ubHk=
          JWT_EXPIRATION_MS: "3600000"
          JWT_REMEMBER_ME_EXPIRATION_MS: "604800000"
          # App config required by AppProperties binding
          APP_BASE_URL: http://localhost:8080
          VERIFICATION_EXPIRATION_HOURS: "24"
          APP_PORT: "8080"
          # Banner vars
          PROJECT_NAME: org.jbelt.module
          PROJECT_VERSION: 1.2.0-SNAPSHOT
```

**Why no PostgreSQL service container:** All test classes use @ActiveProfiles("dev") with H2. There is no test that connects to PostgreSQL. SchemaComparisonTests reads SQL migration files as classpath resources — it does not execute SQL against any database server. Adding PostgreSQL to CI adds 30-60 seconds of container startup time with zero benefit.

**Why no real credentials in GitHub Secrets for CI:** No real email is sent during mvn verify. Do NOT store Gmail App Passwords as GitHub Secrets for CI — they are not needed and create unnecessary attack surface.

### Pattern 4: GitHub Repository Setup (gitignore and pom.xml)

**What:** Publishing to GitHub requires auditing what gets committed. The .gitignore currently excludes pom.xml as a "generated file," but pom.xml is not actually generated in this project (no pom.xml.template exists, no @VARIABLE@ placeholders in the file, not listed in bin/env-templates.list). This is a historical artifact that must be corrected before push.

**Critical finding — pom.xml in .gitignore:**

Current .gitignore entry:
```
# Generated config files (from .template processing)
pom.xml
compose.yaml
Dockerfile
src/main/resources/application.yml
...
```

pom.xml appears here but is NOT generated. If left in .gitignore, GitHub push will silently exclude pom.xml. GitHub Actions mvn verify will then fail immediately: "The specified POM file doesn't exist."

**Required fix:** Remove the pom.xml line from .gitignore. Stage pom.xml explicitly with git add pom.xml.

**.env.example status:** Already tracked by git (not in .gitignore). No gitignore change needed for it. It correctly shows placeholder values, not real secrets.

**No other .gitignore changes needed** beyond removing the pom.xml line.

---

## Data Flow

### Package Rename Data Flow

```
Source file: src/main/java/com/example/usermanagement/auth/JwtService.java
    | IDE "Rename Package" or sed/find+mv
    v
Target file: src/main/java/org/jbelt/module/auth/JwtService.java
    | file content updated:
    | package org.jbelt.module.auth;
    | import org.jbelt.module.shared.config.AppProperties;
    v
./mvnw compile  →  target/classes/org/jbelt/module/auth/JwtService.class
    v
./mvnw verify   →  all 15 test classes pass with new package root
    v
Verification: grep -r "com.example.usermanagement" src/  →  zero results
```

### Gmail SMTP Config Data Flow

```
.env.example (updated with Gmail defaults + setup instructions)
    | developer copies to .env, fills real App Password
    v
.env (MAIL_HOST=smtp.gmail.com, MAIL_PORT=587, MAIL_PASSWORD=<16-char>)
    | bin/env.sh substitute-all
    v
application.yml (spring.mail.host=smtp.gmail.com, port=587, ...)
    | Spring Boot MailSenderAutoConfiguration
    v
JavaMailSender bean (SMTP session configured for Gmail STARTTLS)
    | EmailService.sendVerificationEmail / sendPasswordResetEmail / sendInviteEmail
    v
Gmail SMTP (smtp.gmail.com:587) → recipient inbox
```

### CI Verification Data Flow

```
git push → GitHub (main or PR branch)
    | triggers .github/workflows/ci.yml
    v
ubuntu-latest runner
    | actions/setup-java — Java 21 Temurin, Maven dependency cache
    v
./mvnw verify -B (env: SPRING_PROFILES_ACTIVE=dev, MAIL_HOST=placeholder, ...)
    | application-dev.yml: H2 in-memory, Flyway h2/ migrations
    v
Spring context loads (H2 + JavaMailSender with placeholder host)
    | @MockitoBean EmailService in email-related tests
    v
77 Java source files compiled + 15 test files compiled
    | surefire runs all test classes
    v
BUILD SUCCESS → green check on GitHub commit / PR
```

---

## Integration Points

### New vs Modified File Matrix

| File | Status | Change Description |
|------|--------|--------------------|
| src/main/java/com/example/usermanagement/**/*.java (62 files) | MODIFIED | Package declarations + imports; physical path moves to org/jbelt/module/ |
| src/test/java/com/example/usermanagement/**/*.java (15 files) | MODIFIED | Package declarations + imports; physical path moves |
| pom.xml | MODIFIED | groupId → org.jbelt; version → 1.2.0-SNAPSHOT |
| .gitignore | MODIFIED | Remove pom.xml line — pom.xml must be committed for CI |
| .env.example | MODIFIED | Gmail SMTP section: update host/port defaults, add App Password setup instructions |
| .env.template | MODIFIED | Same Gmail SMTP documentation as .env.example |
| .github/workflows/ci.yml | NEW | GitHub Actions CI workflow (mvn verify with H2 profile) |
| README.md | MODIFIED | Add GitHub repository URL (https://github.com/tbellin/...) |
| banner.txt.template | MAY UPDATE | @PROJECT_VERSION@ will reflect 1.2.0-SNAPSHOT if PROJECT_VERSION var updated in .env |
| application.yml.template | NOT MODIFIED | SMTP properties already correct for Gmail |
| application-dev.yml.template | NOT MODIFIED | H2 config unchanged |
| application-prod.yml.template | NOT MODIFIED | PostgreSQL config unchanged |
| Dockerfile.template | NOT MODIFIED | No package references; target/*.jar glob is version-agnostic |
| compose.yaml.template | NOT MODIFIED | No package references |
| src/main/resources/db/migration/**/*.sql | NOT MODIFIED | No Java package references in SQL |
| bin/env-templates.list | NOT MODIFIED | No new template files added for v1.2 |
| bin/*.sh | NOT MODIFIED | Zero com.example references — verified by grep |
| doc/*.md | NOT MODIFIED | Zero com.example references — verified by grep |
| META-INF/spring | NOT PRESENT | No service loader files in this project |

### Internal Module Boundaries After Rename

| Boundary | Communication | v1.2 Impact |
|----------|---------------|-------------|
| auth → user | Direct import of user.internal classes (AppUser, UserRepository, UpdateUserRequest) | Package path changes in import statements; boundary rules and allowedDependencies unchanged |
| auth → shared | Direct import of shared.* classes (EmailService, AppProperties, JwtService) | Package path changes in import statements |
| user → shared | Direct import of shared.* classes (UserDto, exceptions) | Package path changes in import statements |
| shared → (none) | No outgoing module dependencies | Unchanged |
| @ApplicationModule.allowedDependencies args | Relative names: "user", "shared" (not FQN) | NOT updated — relative names survive rename |

### External Services

| Service | Integration Pattern | v1.2 Change |
|---------|---------------------|-------------|
| Gmail SMTP | JavaMailSender via spring.mail.* config | .env.example gains Gmail defaults + App Password documentation |
| GitHub | git remote add origin + git push | New remote; .github/workflows/ci.yml added |
| GitHub Actions | .github/workflows/ci.yml on push/PR | New workflow file; H2 profile; placeholder MAIL_* vars |
| H2 (CI + dev) | In-memory, no container needed | No change — already the test database |
| PostgreSQL (prod) | Docker Compose service | No change |

---

## Anti-Patterns

### Anti-Pattern 1: Partial Package Rename

**What people do:** Rename the directory tree but miss import statements in some files, or forget package-info.java files.

**Why it is wrong:** Spring Modulith's ApplicationModules.of(Application.class) scans from the application class's package root. If any .java file still declares package com.example.usermanagement.*, Spring component-scan will not discover it (wrong package root), causing NoSuchBeanDefinitionException at runtime. Tests that use @SpringBootTest will fail during context load.

**Detection:** Run grep -r "com.example.usermanagement" src/ after rename. Must return zero results.

**Do this instead:** Use IDE "Rename Package" refactoring which updates all usages atomically. After rename, run the grep verification. Fix any remaining occurrences manually.

### Anti-Pattern 2: Leaving pom.xml in .gitignore

**What people do:** Push to GitHub without removing pom.xml from .gitignore because it was already listed there.

**Why it is wrong:** pom.xml is listed in .gitignore as a "generated file" but it is NOT generated from a template in this project — it has no @VARIABLE@ placeholders and is not in bin/env-templates.list. If left in .gitignore, the file is silently excluded from the pushed repository. GitHub Actions mvn verify immediately fails: "Could not open POM file" or "The specified POM file doesn't exist."

**Do this instead:** Remove the pom.xml line from .gitignore before the first push. Stage it explicitly: git add pom.xml.

### Anti-Pattern 3: Using Real SMTP Credentials in CI

**What people do:** Add MAIL_USERNAME and MAIL_PASSWORD as GitHub Secrets with a real Gmail App Password so "CI matches production configuration."

**Why it is wrong:** Tests mock EmailService at the service level — no real SMTP connection is ever attempted. Real credentials in CI secrets create unnecessary attack surface and rotation overhead, providing zero benefit since the mock prevents any SMTP call.

**Do this instead:** Use hardcoded placeholder strings for all MAIL_* env vars directly in the CI workflow YAML. They only exist to satisfy Spring Boot's MailSenderAutoConfiguration bean creation at context load — no authentication to Gmail ever occurs.

### Anti-Pattern 4: Adding PostgreSQL Service Container to CI

**What people do:** Add a services: postgres: block to the GitHub Actions workflow because production uses PostgreSQL.

**Why it is wrong:** All 15 test files use @ActiveProfiles("dev") which loads the H2 in-memory database. There is no test that connects to PostgreSQL. SchemaComparisonTests reads SQL migration files as classpath resources — it does not execute SQL against any database server. Adding PostgreSQL to CI adds 30-60 seconds of container startup time with zero benefit.

**Do this instead:** CI runs with SPRING_PROFILES_ACTIVE=dev only. The H2 MODE=PostgreSQL setting provides sufficient parity for test execution. The existing SchemaComparisonTests validates that H2 and PostgreSQL migration files create the same tables.

### Anti-Pattern 5: Updating @ApplicationModule allowedDependencies After Rename

**What people do:** See allowedDependencies = { "user", "shared" } in package-info.java and think these need to become "org.jbelt.module.user" after the rename.

**Why it is wrong:** Spring Modulith resolves module names from the simple subdirectory name relative to the root application package, not from the full package name. The value "user" means <root-package>.user regardless of what the root package is. Changing them to FQNs would break module detection.

**Do this instead:** Leave allowedDependencies values unchanged. Only update the package declaration line in each package-info.java.

---

## Build Order

The 4 features have the following dependency ordering:

```
Step 1+2: Package Rename + Version Bump
  (atomic — both modify pom.xml; rename must compile before CI can verify)

Step 3: Gmail SMTP documentation
  (independent — can be done before or after rename)

Step 4: .gitignore fix (remove pom.xml line)
  (must happen before any git commit involving pom.xml)

Step 5: .github/workflows/ci.yml creation
  (needs rename complete so CI tests the right package)

Step 6: README + repo URL
  (needs GitHub repo URL available)

Step 7: git commit + push
  (triggers CI)
```

**Recommended build order:**

| Step | Action | Verification Gate |
|------|--------|-------------------|
| 1 | Package rename: update all 77 Java files, move directory tree from com/example/usermanagement/ to org/jbelt/module/ | ./mvnw compile exits 0 |
| 2 | Version bump + groupId: edit pom.xml (groupId: org.jbelt, version: 1.2.0-SNAPSHOT) | ./mvnw verify exits 0 (all tests pass) |
| 3 | Gmail SMTP docs: update .env.example and .env.template Mail section | Manual review |
| 4 | Fix .gitignore: remove pom.xml line | git status shows pom.xml as untracked/modified (not ignored) |
| 5 | Create .github/workflows/ci.yml | Valid YAML syntax |
| 6 | Update README.md with GitHub repository URL | — |
| 7 | git add pom.xml .github/ .gitignore .env.example .env.template README.md src/ | git status clean |
| 8 | git commit | Commit created |
| 9 | git remote add origin https://github.com/tbellin/<repo>.git | Remote configured |
| 10 | git push -u origin main | Push succeeds; CI triggers |
| 11 | Verify GitHub Actions CI run passes | Green check on commit |

**Critical path:** Steps 1 → 2 → 4 → 7 → 8 → 9 → 10 → 11. Steps 3, 5, and 6 are independent and can be done in any order before step 7.

---

## Scaling Considerations

Not applicable to v1.2 — this milestone makes no runtime behavior changes. Package rename, version bump, SMTP documentation, and CI setup do not affect scalability characteristics.

---

## Sources

- Direct codebase inspection (HIGH confidence): all 77 Java files, pom.xml, application.yml.template, .env.example, .env.template, .gitignore, bin/env-templates.list, bin/env.sh, Dockerfile.template, compose.yaml.template, banner.txt.template, all SQL migrations, all 15 test files — zero com.example references found outside Java sources and pom.xml
- Spring Modulith @ApplicationModule documentation: module names in allowedDependencies are relative simple names, not fully-qualified package names — confirmed by package-info.java inspection showing { "user", "shared" } as values
- Spring Boot MailSenderAutoConfiguration behavior: requires spring.mail.host non-null at context load even when EmailService is @MockitoBean — confirmed by comment in EmailVerificationIntegrationTest.java explaining the design decision
- GitHub Actions: .github/workflows/ is the only valid workflow directory path — documented in GitHub Actions documentation
- Gmail SMTP: smtp.gmail.com:587 + STARTTLS + App Password is the standard Gmail SMTP configuration — all required properties already present in application.yml.template

---

*Architecture research for: Spring Boot 4 + Spring Modulith — v1.2 Foundation Upgrade integration*
*Researched: 2026-02-23*
