# Feature Research

**Domain:** v1.2 Foundation Upgrade — Spring Boot 4 User Management Server
**Researched:** 2026-02-23
**Confidence:** HIGH (package rename and Gmail SMTP well-documented; GitHub Actions from official docs; repo setup from official GitHub docs)

---

> **Scope note:** This research covers ONLY the four new v1.2 features. All v1.0 features
> (auth, JWT, profiles, email verification, admin CRUD, Thymeleaf UI, Docker, etc.) are
> already built and must NOT be re-researched or re-scoped.

---

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Package rename: `com.example.usermanagement` → `org.jbelt.module` | `com.example` is a placeholder — dev convention, not production | LOW-MEDIUM | Mechanical but wide-reaching: 77 Java files + pom.xml groupId + directory tree. IDE handles Java files; manual sweep for non-Java artifacts |
| Version bump: `0.0.1-SNAPSHOT` → `1.2.0-SNAPSHOT` | Version must match the milestone label | LOW | One-line pom.xml change + banner.txt.template check |
| Gmail SMTP configuration support | Gmail is the most-common developer SMTP provider — users setting up locally reach for it first | LOW | Zero code changes. Pure config: `.env.example` + doc update |
| GitHub Actions CI workflow | A public repo without CI is incomplete — proves build is reproducible outside the developer's machine | MEDIUM | New `.github/workflows/ci.yml`. Must handle JDK 21/Temurin, Maven cache, `mvn verify`. Mail tests mocked in CI. |
| GitHub repository setup | Push to remote + update README with real clone URL — without this "Quick Start" is broken | LOW | `git remote add origin`, push, update README placeholder |

### Differentiators

| Feature | Value | Complexity |
|---------|-------|------------|
| CI: upload surefire reports as artifact on failure | Download HTML test reports without reading raw logs | LOW |
| CI: Maven dependency graph submission | Enables Dependabot security alerts for transitive deps | LOW |
| Gmail `.env.example` with inline doc comments | Reduces setup time — copy, read comments, generate App Password in 5 min | LOW |
| README: repo URL + CI badge | Signals the repo is live and healthy | LOW |
| GitHub branch protection on `main` | Requires CI to pass before merge | LOW |

### Anti-Features (Avoid)

| Feature | Why Problematic | Alternative |
|---------|-----------------|-------------|
| Real email integration tests in CI | Gmail App Password stored as GitHub Secret; real email sent on every push | Tag with `@Tag("mail-integration")`, exclude in CI |
| OAuth2 SMTP auth for Gmail | GCP OAuth client + refresh token rotation — massively disproportionate | App Password is Google's own recommended path for scripts/apps |
| Maven multi-module split during package rename | Completely out of scope for a namespace rename | Stay single-module Maven artifact |
| CD pipeline | No deployment target configured for v1.2 | CI only; CD is v2+ |

---

## Feature Details

### Feature 1: Package Rename

**Files that must change beyond Java source:**

| File / Location | What changes | How |
|-----------------|-------------|-----|
| `pom.xml` `<groupId>` | `com.example` → `org.jbelt` | Manual edit |
| `pom.xml` `<version>` | `0.0.1-SNAPSHOT` → `1.2.0-SNAPSHOT` | Manual edit (same commit) |
| `src/main/java/` directory tree | `com/example/usermanagement/` → `org/jbelt/module/` | IDE rename refactoring |
| `src/test/java/` directory tree | Same | IDE rename refactoring |
| `src/main/resources/banner.txt.template` | May reference app name/version — check | Manual check |
| `Dockerfile` | `COPY --from=build /app/target/*.jar` — wildcard, no change needed | N/A |

**Important:** Spring Modulith discovers modules by scanning sub-packages of `@SpringBootApplication` root. After rename, root is `org.jbelt.module`. Modulith will look for `org.jbelt.module.auth`, `org.jbelt.module.user`, `org.jbelt.module.shared`. Directory rename must preserve sub-package names exactly or `ModularityTests` fail.

**pom.xml tracking concern:** The `.gitignore` lists `pom.xml` as generated, but no `pom.xml.template` exists. Run `git ls-files pom.xml` to confirm it is tracked. The rename edits whichever file is actually committed.

---

### Feature 2: Version Bump

| Location | Old Value | New Value |
|----------|-----------|-----------|
| `pom.xml` `<version>` | `0.0.1-SNAPSHOT` | `1.2.0-SNAPSHOT` |
| `banner.txt.template` | Check for hardcoded version | Update if hardcoded |

---

### Feature 3: Gmail SMTP Configuration

**Current state:** SMTP already works. `application.yml.template` has `@MAIL_HOST@`, `@MAIL_PORT@`, `@MAIL_USERNAME@`, `@MAIL_PASSWORD@` placeholders with STARTTLS already configured. App is SMTP-provider-agnostic by design.

**Gmail-specific requirements (confirmed as of 2025):**

| Requirement | Details |
|-------------|---------|
| "Less Secure Apps" removed | Google shut this down permanently Sept 30, 2024 (personal). Cannot use regular Gmail password. |
| App Password required | Only viable SMTP auth path. Generate: Google Account → Security → 2-Step Verification → App passwords |
| 2-Step Verification prerequisite | App Passwords cannot be generated without 2FA enabled |

**Working Gmail `.env` values:**

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.email@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx  # 16-char App Password, no spaces
MAIL_FROM=your.email@gmail.com  # Must match MAIL_USERNAME for Gmail
```

**`application.yml.template` STARTTLS config is already correct for Gmail — no YAML changes needed.**

**Gotchas to document:**

| Gotcha | Symptom | Resolution |
|--------|---------|-----------|
| Using regular Gmail password | `535-5.7.8 Username and Password not accepted` | Generate App Password |
| 2FA not enabled | App passwords menu is hidden | Enable 2-Step Verification first |
| App Password copied with spaces | Auth fails | Strip spaces |
| `MAIL_FROM` ≠ `MAIL_USERNAME` | Gmail rewrites From header to authenticated account | Set both to same Gmail address |

---

### Feature 4: GitHub Actions CI Workflow

**Recommended `.github/workflows/ci.yml`:**

```yaml
name: CI

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up JDK 21 (Temurin)
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and run tests
        run: mvn --batch-mode verify

      - name: Upload test reports on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: target/surefire-reports/
```

**The mail-in-CI concern:**
- If `application.yml` is tracked with real credentials → clean git history before public push
- Existing tests use `@MockBean JavaMailSender` (confirmed from STATE.md notes) → CI will pass without SMTP secrets

**Security pre-flight (blocker):**
```bash
git ls-files src/main/resources/application.yml
git ls-files | grep -E "\.env$|\.env\.local"
```
Any output = tracked sensitive file = must resolve before public push.

---

### Feature 5: GitHub Repository Setup

| Task | Command |
|------|---------|
| Create GitHub repo | `gh repo create tbellin/<repo-name> --public` or GitHub web UI |
| Add remote | `git remote add origin https://github.com/tbellin/<repo-name>.git` |
| Initial push | `git push -u origin main --tags` |
| Update README clone URL | Replace `<repo-url>` placeholder |
| Add CI badge | `![CI](https://github.com/tbellin/<repo>/actions/workflows/ci.yml/badge.svg)` |

---

## Feature Dependencies

```
[Package rename + version bump]  ← do in same pom.xml commit
    → must complete before GitHub CI is green

[Gmail SMTP config]  ← independent, no blockers

[GitHub Actions CI]
    → requires package rename complete
    → requires security audit (no secrets in tracked files)

[GitHub repo push]
    → final step: requires all others complete
    → security audit is hard blocker for public push
```

---

## MVP for v1.2

- [ ] Package rename: all 77 Java files + pom.xml groupId + directory tree. All existing tests pass. `ModularityTests` pass.
- [ ] Version bump: `pom.xml` `<version>` = `1.2.0-SNAPSHOT`.
- [ ] Gmail SMTP: `.env.example` updated with Gmail values + App Password instructions. Doc updated.
- [ ] GitHub Actions CI: `.github/workflows/ci.yml` created. CI badge added to README.
- [ ] GitHub repo: pushed to `tbellin/<repo>`. README clone URL updated. No secrets in tracked files.

---

## Sources

- Google Workspace blog — LSA shutdown: workspaceupdates.googleblog.com
- Baeldung: Guide to Spring Email (Gmail + App Password config)
- GitHub Docs: Building and testing Java with Maven
- actions/setup-java repository — `cache: maven` + Temurin distribution
- Project codebase: 77 Java files confirmed by direct inspection, existing SMTP config structure

---
*Feature research for: v1.2 Foundation Upgrade*
*Researched: 2026-02-23*
