# Roadmap: Spring Boot User Management Server

## Milestones

- ✅ **v1.0 MVP** — Phases 1-8 (shipped 2026-02-23)
- 🚧 **v1.2 Foundation Upgrade** — Phases 9-11 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1-8) — SHIPPED 2026-02-23</summary>

- [x] Phase 1: Project Bootstrap & Infrastructure (12/12 plans) — completed 2026-01-30
- [x] Phase 2: Security & API Foundation (6/6 plans) — completed 2026-02-03
- [x] Phase 3: Registration & Login (5/5 plans) — completed 2026-02-04
- [x] Phase 4: Email Verification (8/8 plans) — completed 2026-02-06
- [x] Phase 5: Password Management (5/5 plans) — completed 2026-02-11
- [x] Phase 6: User Profile & Admin Operations (4/4 plans) — completed 2026-02-13
- [x] Phase 7: API Documentation & Swagger (3/3 plans) — completed 2026-02-22
- [x] Phase 8: Tooling & Project Documentation (4/4 plans) — completed 2026-02-22

</details>

### 🚧 v1.2 Foundation Upgrade (In Progress)

**Milestone Goal:** Modernize the project foundations — rename the root package, bump the version, document Gmail SMTP configuration, and publish to GitHub with CI.

- [ ] **Phase 9: Package Rename + Version Bump** — Rename all Java sources from `com.example.usermanagement` to `org.jbelt.module`, update `pom.xml` groupId and version, verify `mvn verify` passes clean
- [ ] **Phase 10: Gmail SMTP Documentation + .gitignore Fix** — Update `.env.example` and `.env.template` with Gmail SMTP defaults and App Password instructions, add `doc/` guide, remove `pom.xml` from `.gitignore`
- [ ] **Phase 11: GitHub Repository Setup + CI + README** — Security pre-flight, create `.github/workflows/ci.yml`, push to `tbellin` account, update `README.md` with repo URL and CI badge

## Phase Details

### Phase 9: Package Rename + Version Bump
**Goal**: The project compiles, tests pass, and all Java sources carry the new `org.jbelt.module` namespace with version `1.2.0-SNAPSHOT`
**Depends on**: Nothing (first v1.2 phase)
**Requirements**: PKG-01, PKG-02, PKG-03, PKG-04
**Success Criteria** (what must be TRUE):
  1. `grep -r "com.example.usermanagement" src/` returns zero results
  2. `pom.xml` declares `<groupId>org.jbelt</groupId>` and `<version>1.2.0-SNAPSHOT</version>`
  3. `mvn verify` completes without compilation errors or test failures
  4. `ModularityTests` produces no new failures beyond the pre-existing cosmetic false-positive
**Plans:** 1 plan
Plans:
- [ ] 09-01-PLAN.md — Rename all 77 Java files, move directory trees, update pom.xml, verify with mvn verify

### Phase 10: Gmail SMTP Documentation + .gitignore Fix
**Goal**: Developers can configure Gmail SMTP by following documented instructions in `.env.example`, and `pom.xml` is no longer excluded from version control
**Depends on**: Phase 9
**Requirements**: SMTP-01, SMTP-02, SMTP-03, GH-01
**Success Criteria** (what must be TRUE):
  1. `.env.example` and `.env.template` show `smtp.gmail.com:587` as the default SMTP host/port
  2. `.env.example` includes step-by-step inline comments covering 2FA enablement, App Password generation, and the 16-character format
  3. `doc/` contains a Gmail SMTP configuration guide (standalone or integrated into existing SMTP doc)
  4. `pom.xml` is no longer listed in `.gitignore` and `git status` shows it as a tracked file
**Plans**: TBD

### Phase 11: GitHub Repository Setup + CI + README
**Goal**: The project is publicly accessible on GitHub under `tbellin`, CI runs `mvn verify` on every push, and the README reflects the live repository URL and CI build status
**Depends on**: Phase 10 (gitignore fix is a hard blocker for CI; clean codebase from Phase 9 required)
**Requirements**: GH-02, GH-03, GH-04, GH-05
**Success Criteria** (what must be TRUE):
  1. Security pre-flight confirms zero real credentials in any tracked file before push (no `.env`, no untemplatized `application.yml` with real values)
  2. `.github/workflows/ci.yml` exists, targets Java 21 Temurin with Maven cache, and runs `mvn --batch-mode verify`
  3. Repository is accessible at `github.com/tbellin/<repo>` with full commit history
  4. CI badge in `README.md` shows green (passing) after the first push triggers the workflow
  5. `README.md` clone URL points to the live `github.com/tbellin/<repo>` address
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Project Bootstrap & Infrastructure | v1.0 | 12/12 | Complete | 2026-01-30 |
| 2. Security & API Foundation | v1.0 | 6/6 | Complete | 2026-02-03 |
| 3. Registration & Login | v1.0 | 5/5 | Complete | 2026-02-04 |
| 4. Email Verification | v1.0 | 8/8 | Complete | 2026-02-06 |
| 5. Password Management | v1.0 | 5/5 | Complete | 2026-02-11 |
| 6. User Profile & Admin Operations | v1.0 | 4/4 | Complete | 2026-02-13 |
| 7. API Documentation & Swagger | v1.0 | 3/3 | Complete | 2026-02-22 |
| 8. Tooling & Project Documentation | v1.0 | 4/4 | Complete | 2026-02-22 |
| 9. Package Rename + Version Bump | v1.2 | 0/1 | Planned | - |
| 10. Gmail SMTP Documentation + .gitignore Fix | v1.2 | 0/TBD | Not started | - |
| 11. GitHub Repository Setup + CI + README | v1.2 | 0/TBD | Not started | - |

Full v1.0 phase details: `.planning/milestones/v1.0-ROADMAP.md`

---
*Roadmap created: 2026-01-28*
*v1.0 shipped: 2026-02-23*
*v1.2 roadmap added: 2026-02-23*
