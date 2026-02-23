# Project Research Summary

**Project:** Spring Boot 4 + Spring Modulith User Management Server — v1.2
**Domain:** Foundation upgrade — package namespace, version, SMTP documentation, GitHub CI
**Researched:** 2026-02-23
**Confidence:** HIGH

## Executive Summary

Milestone v1.2 is a pure housekeeping milestone with zero new runtime features. The four tasks are: rename the root Java package from `com.example.usermanagement` to `org.jbelt.module` across 77 Java files and `pom.xml`; bump the Maven version to `1.2.0-SNAPSHOT`; document Gmail SMTP configuration in `.env.example`; and publish the project to GitHub under the `tbellin` account with a GitHub Actions CI workflow that runs `mvn verify`. No new dependencies are introduced. The existing stack is unchanged.

The recommended execution order is driven by two hard constraints: the package rename must compile cleanly before CI can produce a green build, and `pom.xml` must be removed from `.gitignore` before push — otherwise CI immediately fails with "POM file not found." Gmail SMTP documentation and the CI workflow file are independent tasks. The GitHub push is the final gate.

The principal risks are mechanical: a partial rename leaving stray `com.example` references (Spring Modulith module detection fails), the `pom.xml`-in-`.gitignore` trap (CI fails), and credential exposure during the public push. All three are fully preventable with explicit verification steps.

## Key Findings

### Stack Additions

**No new `pom.xml` dependencies for any of the 4 features.**

| Area | Finding |
|------|---------|
| GitHub Actions | `actions/setup-java@v4` + `cache: maven` + `mvn --batch-mode verify` on `ubuntu-latest`. No separate cache step. |
| Gmail SMTP | `smtp.gmail.com:587` + STARTTLS. Existing `application.yml.template` SMTP block already correct — only `.env` values change. App Password mandatory (Google removed plain-password auth 2024). |
| Package rename | IntelliJ IDE Refactor → Rename. Do NOT use `sed`/`find` or Maven plugin — they miss `@ApplicationModule` annotation values. |
| Version bump | Direct `pom.xml` edit. `mvn versions:set` is overkill for single-module project. |

### Feature Table Stakes

- Package rename: all 77 Java files + `pom.xml` `<groupId>` + directory tree. `mvn verify` passes. `ModularityTests` passes (same pre-existing failure, no new failures).
- Version bump: `pom.xml` `<version>` = `1.2.0-SNAPSHOT`.
- Gmail SMTP: `.env.example` + `.env.template` updated with Gmail defaults + App Password setup instructions.
- GitHub Actions CI: `.github/workflows/ci.yml` committed. CI badge in `README.md`. First push triggers green build.
- GitHub repository: pushed to `tbellin/<repo>`. `README.md` clone URL updated. No secrets in tracked files.

### Architecture

v1.2 makes **no changes to the Spring Modulith module architecture**. The rename is a pure namespace change.

Key findings from direct codebase inspection:
- `@ApplicationModule(allowedDependencies = {...})` uses relative names (`"user"`, `"shared"`) — **do not change these values after rename**
- Zero `com.example` references found outside Java sources and `pom.xml` — no SQL, no scripts, no YAML
- `Dockerfile.template` uses `target/*.jar` glob — no update needed
- All 15 test files use `@ActiveProfiles("dev")` with H2 — CI needs no PostgreSQL service container
- Tests mock `EmailService` (not `JavaMailSender`) — CI needs no SMTP credentials

**v1.2 file changes summary:**

| File | Change |
|------|--------|
| 77 Java files + directory tree | Package declarations + imports renamed |
| `pom.xml` | `<groupId>org.jbelt</groupId>` + `<version>1.2.0-SNAPSHOT</version>` |
| `.gitignore` | Remove `pom.xml` line (currently listed as "generated" — no template exists) |
| `.env.example` / `.env.template` | Gmail SMTP defaults + App Password instructions |
| `.github/workflows/ci.yml` | New file |
| `README.md` | Repo URL + CI badge |

### Watch Out For

1. **`pom.xml` silently excluded from push** — `pom.xml` is in `.gitignore` as "generated" but has no `.template` counterpart. If not removed from `.gitignore`, the file is absent from GitHub and CI fails with "POM file not found." **Fix:** remove `pom.xml` from `.gitignore`, then `git add pom.xml`.

2. **Partial rename breaks Spring Modulith module detection** — Any `.java` file still declaring `package com.example.usermanagement.*` after rename causes `NoSuchBeanDefinitionException` and `ModularityTests` failures. **Fix:** after IDE rename, run `grep -r "com.example.usermanagement" src/` and verify zero results.

3. **Gmail App Password not set** — `535-5.7.8 Username and Password not accepted` is the symptom. Only the 16-char App Password works. **Fix:** document exact generation steps (enable 2FA first, then Google Account → Security → App passwords).

4. **Real credentials committed before public push** — `application.yml` may be tracked with real credentials. Once pushed to public repo, git history retains the exposure. **Fix:** run `git ls-files src/main/resources/application.yml` and `git ls-files | grep -E "\.env$"` as a hard blocker before `git push`.

5. **`@ApplicationModule allowedDependencies` values incorrectly updated** — Values `"user"` and `"shared"` are relative names resolved by Spring Modulith from the root package. Changing to FQNs breaks module detection. **Fix:** leave `allowedDependencies` values unchanged.

6. **`mvnw` executable bit** — Use `mvn --batch-mode verify` in CI (not `./mvnw`) to avoid executable-bit permission issues on Linux runners.

## Implications for Roadmap

3 phases, ordered by hard dependencies. Critical path: package rename compiles → gitignore fixed → local `mvn verify` passes → GitHub push → CI green.

### Phase 9: Package Rename + Version Bump
Share the same `pom.xml` commit. Rename is the highest-complexity task and must compile before everything downstream. **Gate:** `mvn verify` passes, `ModularityTests` no new failures, `grep -r "com.example.usermanagement" src/` = zero results.

### Phase 10: Gmail SMTP Documentation + .gitignore Fix
Independent of rename but `.gitignore` fix is a hard blocker for Phase 11. **Gate:** `.env.example` updated with Gmail values, `pom.xml` line removed from `.gitignore`, `git status` shows `pom.xml` as untracked/modified.

### Phase 11: GitHub Repository Setup + CI Workflow + README
Final gate. Requires Phase 9 (renamed code) + Phase 10 (gitignore fixed, no credentials). Security pre-flight is step 1. **Gate:** CI badge green in README, clone URL updated, repo accessible at `github.com/tbellin/<repo>`.

### Open Items (Confirm Before Phase 11)
- Exact GitHub repo name (affects CI badge URL in README)
- `ModularityTests` baseline output before rename (to distinguish pre-existing failures from new ones)
- Confirm `mvnw` executable bit: `git ls-files --stage mvnw`

## Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| Stack | HIGH | Direct codebase inspection; official GitHub Actions + Google docs |
| Features | HIGH | All 77 Java files inspected; scope fully enumerated |
| Architecture | HIGH | Direct inspection of all config, scripts, templates, tests |
| Pitfalls | HIGH | `pom.xml`-in-gitignore confirmed by direct `.gitignore` + `env-templates.list` inspection |

---
*Research completed: 2026-02-23*
*Ready for roadmap: yes — 3 phases, no `/gsd:research-phase` needed during planning*
