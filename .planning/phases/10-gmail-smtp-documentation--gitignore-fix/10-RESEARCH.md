# Phase 10: Gmail SMTP Documentation + .gitignore Fix - Research

**Researched:** 2026-02-24
**Domain:** Documentation, environment configuration, Git housekeeping
**Confidence:** HIGH

## Summary

This phase is purely a documentation and configuration phase -- no application code changes are required. The work involves three concerns:

1. **Gmail SMTP defaults and documentation**: Update `.env.example` and `.env.template` to use `smtp.gmail.com:587` as the default SMTP host/port, add inline comments explaining Gmail 2FA and App Password setup, and create a standalone Gmail SMTP guide in `doc/`.

2. **`.gitignore` fix for `pom.xml`**: Remove `pom.xml` from `.gitignore`. Currently `pom.xml` is listed under "Generated config files (from .template processing)" but this is incorrect -- `pom.xml` contains no `@VARIABLE@` placeholders, has no corresponding `.template` file, and is NOT listed in `bin/env-templates.list`. It has been a tracked file with 5 commits of history. The `.gitignore` entry effectively prevents Git from tracking future changes, which would break CI on a fresh clone.

3. **Variable name alignment**: During research, a mismatch was discovered between `.env.example` (which uses `JWT_EXPIRATION_MS` and `JWT_REMEMBER_ME_EXPIRATION_MS`) and `.env.template` (which uses `JWT_EXPIRATION`). The `.env.example` also has `PROJECT_NAME` and `PROJECT_VERSION` that `.env.template` lacks. The application template (`application.yml.template`) uses `@JWT_EXPIRATION_MS@` and `@JWT_REMEMBER_ME_EXPIRATION_MS@`, confirming `.env.example` has the correct names. This is worth noting but may be out of scope for this phase.

**Primary recommendation:** This phase is straightforward documentation work. The planner should create 2-3 small tasks: (1) update `.env.example` and `.env.template` with Gmail SMTP defaults and inline setup comments, (2) create `doc/gmail-smtp-setup.md`, (3) remove `pom.xml` from `.gitignore` and verify tracking.

## Standard Stack

### Core

No new libraries or dependencies are needed. This phase is documentation-only.

| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Git | any | Remove `.gitignore` entry, verify tracking | Standard VCS |

### Supporting

N/A -- no code dependencies.

### Alternatives Considered

N/A -- documentation phase.

## Architecture Patterns

### Current File Layout (Relevant)

```
.env.example          # Reference doc: all variables with descriptions (tracked)
.env.template         # Template for generating .env via setup.sh (tracked)
.env                  # Active config with real values (gitignored)
.env.local            # Personal overrides (gitignored)
.gitignore            # Lists pom.xml incorrectly as "generated"
doc/
  01-setup.md         # Existing setup guide (references SMTP)
  04-deployment.md    # Existing deployment guide (has Gmail example)
  rate-limit-options.md
```

### Pattern: Inline Comment Documentation in .env.example

The existing `.env.example` uses section headers (`# ===`) and inline `# WARNING:` comments for required fields. Gmail SMTP instructions should follow this established pattern -- step-by-step inline comments within the Mail Configuration section.

### Pattern: Standalone Guide in doc/

Existing guides (`01-setup.md`, `04-deployment.md`) follow a consistent format: title, purpose sentence, prerequisites table, step-by-step instructions with code blocks, troubleshooting section. The Gmail SMTP guide should follow this established structure.

### Anti-Patterns to Avoid

- **Duplicating deployment.md content:** `doc/04-deployment.md` already has a production `.env` example showing `smtp.gmail.com`. The new Gmail guide should complement it, not repeat it.
- **Putting secrets in tracked files:** The `.env.example` must use placeholder values only (`your-gmail@gmail.com`, `abcd-efgh-ijkl-mnop`), never real credentials.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Gmail SMTP instructions | Custom wording | Google's official App Password documentation format | Users expect the steps they see in Google's own UI |

**Key insight:** Gmail App Password setup instructions should match what users actually see in their Google Account settings UI. The steps are stable and well-documented by Google.

## Common Pitfalls

### Pitfall 1: Forgetting `git rm --cached` After Removing from .gitignore

**What goes wrong:** Simply removing `pom.xml` from `.gitignore` is not sufficient if the file is already being ignored. Git needs to be told to track the file again.
**Why it happens:** Git caches ignore decisions.
**How to avoid:** After editing `.gitignore`, run `git add pom.xml` explicitly and verify with `git status` that it shows as tracked.
**Warning signs:** `git status` still does not show `pom.xml` after `.gitignore` change.

**NOTE:** In this project, `pom.xml` is actually already tracked (it has 5 commits of history). The `.gitignore` entry is preventing Git from seeing new modifications. Once the `.gitignore` entry is removed, `git status` should immediately show `pom.xml` changes (if any). No `git rm --cached` is needed -- just removing the `.gitignore` entry is sufficient because the file was never un-tracked.

### Pitfall 2: Gmail SMTP Requirements Changing

**What goes wrong:** Google periodically updates its security requirements for SMTP access.
**Why it happens:** Google's security policies evolve.
**How to avoid:** Document the current flow (as of 2026: 2FA required, App Passwords via myaccount.google.com), and include a link to Google's official page so users can check for updates.
**Warning signs:** Users report authentication failures despite following documented steps.

### Pitfall 3: .env.template and .env.example Divergence

**What goes wrong:** The two files already have variable name mismatches (e.g., `JWT_EXPIRATION` vs `JWT_EXPIRATION_MS`). Adding Gmail changes to one but not the other would worsen the drift.
**Why it happens:** The files serve different purposes but should remain synchronized in their variable names and defaults.
**How to avoid:** Always update BOTH files together. The planner should ensure a single task covers both files.
**Warning signs:** `diff` between the two files shows unexpected variable name differences.

### Pitfall 4: The .gitignore Comment is Wrong

**What goes wrong:** The `.gitignore` comment says `pom.xml` is a "Generated config file (from .template processing)" but no `pom.xml.template` exists and `pom.xml` is not in `bin/env-templates.list`.
**Why it happens:** The `.gitignore` was likely created early in the project before the template system was fully defined.
**How to avoid:** When removing `pom.xml`, also clean up the comment to reflect that only the truly generated files remain in that section.

## Code Examples

### Current .gitignore Section (to be modified)

```gitignore
# Generated config files (from .template processing)
# Run: ./bin/env.sh substitute-all
# These contain resolved secrets -- never commit.
pom.xml          # <-- INCORRECT: not generated, no template exists
compose.yaml
Dockerfile
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
docker/pgadmin/servers.json
```

After fix -- remove the `pom.xml` line:

```gitignore
# Generated config files (from .template processing)
# Run: ./bin/env.sh substitute-all
# These contain resolved secrets -- never commit.
compose.yaml
Dockerfile
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
docker/pgadmin/servers.json
```

### Current .env.example Mail Section (to be updated)

```bash
# ===========================================
# Mail Configuration
# ===========================================
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=        # WARNING: Required for SMTP auth
MAIL_PASSWORD=        # WARNING: Required for SMTP auth
MAIL_FROM=noreply@jbeltsolution.com
```

Target state:

```bash
# ===========================================
# Mail Configuration (Gmail SMTP)
# ===========================================
# Default: Gmail SMTP with TLS (port 587)
#
# Gmail App Password Setup (required):
#   1. Go to https://myaccount.google.com/security
#   2. Enable 2-Step Verification (if not already enabled)
#   3. Go to https://myaccount.google.com/apppasswords
#   4. Select app: "Mail", device: "Other" (enter a name like "UserMgmt")
#   5. Click "Generate" -- Google gives you a 16-character password
#   6. Copy that password into MAIL_PASSWORD below (spaces optional)
#
# See doc/gmail-smtp-setup.md for full instructions with screenshots guidance.
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com      # Your full Gmail address
MAIL_PASSWORD=abcd-efgh-ijkl-mnop       # 16-char App Password (NOT your Gmail password)
MAIL_FROM=your-gmail@gmail.com
```

### Gmail SMTP Guide Structure (doc/gmail-smtp-setup.md)

```markdown
# Gmail SMTP Configuration Guide

## Overview
How to configure Gmail as the SMTP provider for sending emails
(verification, password reset, invitations).

## Prerequisites
- A Gmail account
- 2-Step Verification enabled on the Gmail account

## Step 1: Enable 2-Step Verification
[instructions with Google Account Security URL]

## Step 2: Generate an App Password
[instructions with App Passwords URL]

## Step 3: Configure .env
[copy-paste block with MAIL_* variables]

## Step 4: Verify Configuration
[how to test: start app, register user, check email]

## Troubleshooting
- AuthenticationFailedException: wrong password or 2FA not enabled
- Connection timeout: firewall blocking port 587
- "Less secure app access" is deprecated -- App Passwords are required

## Alternative SMTP Providers
[brief mention that any SMTP provider works by changing MAIL_HOST/PORT]
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Gmail "Less Secure Apps" toggle | App Passwords with 2FA | May 2022 (Google deprecated LSA) | Must use App Passwords; "allow less secure apps" no longer exists |
| `smtp.gmail.com:465` (SSL) | `smtp.gmail.com:587` (STARTTLS) preferred | Long-standing | Port 587 with STARTTLS is the standard; 465 works but 587 is recommended |

**Deprecated/outdated:**
- "Less Secure App Access": Google removed this option in May 2022. All documentation must reference App Passwords exclusively.
- "Display Unlock Captcha" (accounts.google.com/DisplayUnlockCaptcha): No longer relevant with App Passwords.

## Observations from Codebase Investigation

### Variable Name Mismatch (pre-existing issue)

| Variable | `.env.example` | `.env.template` | `application.yml.template` |
|----------|---------------|-----------------|---------------------------|
| JWT expiration | `JWT_EXPIRATION_MS` | `JWT_EXPIRATION` | `@JWT_EXPIRATION_MS@` |
| JWT remember-me | `JWT_REMEMBER_ME_EXPIRATION_MS` | *(missing)* | `@JWT_REMEMBER_ME_EXPIRATION_MS@` |
| Project name | `PROJECT_NAME` | *(missing)* | *(not used)* |
| Project version | `PROJECT_VERSION` | *(missing)* | *(not used)* |

This is a pre-existing issue. The planner should decide whether to fix it in this phase (since we are already editing both files) or defer it.

### pom.xml is Already Tracked

Despite being in `.gitignore`, `pom.xml` has 5 commits of history. This means it was added to Git before the `.gitignore` entry was created. The `.gitignore` entry prevents Git from detecting new modifications to `pom.xml`, which is the actual problem -- changes to dependencies would silently not be committed. Removing the `.gitignore` entry is the only action needed (no `git add -f` or `git rm --cached` required).

### Existing SMTP References in Documentation

- `doc/04-deployment.md` already shows `smtp.gmail.com` in an example `.env` block (line 60-62)
- `doc/01-setup.md` mentions "Real SMTP credentials required (e.g. Gmail, Mailgun)" in prerequisites
- Neither document has step-by-step Gmail App Password instructions

## Open Questions

1. **Should the `.env.template` variable mismatch be fixed in this phase?**
   - What we know: `.env.template` uses `JWT_EXPIRATION` but `application.yml.template` expects `@JWT_EXPIRATION_MS@`. This means template substitution for JWT expiration likely fails silently.
   - What's unclear: Whether this causes actual runtime issues (it may fall back to a default)
   - Recommendation: Fix it in this phase since we are already editing `.env.template`. It is a 2-line change and prevents confusion.

2. **Should `MAIL_FROM` default change from `noreply@jbeltsolution.com`?**
   - What we know: Gmail requires the sender to match the authenticated account (or an alias). Using `noreply@jbeltsolution.com` with a Gmail account will likely result in Gmail overwriting the From address.
   - What's unclear: Whether the user wants to keep the existing `MAIL_FROM` for production or switch to the Gmail address as default.
   - Recommendation: Change the default in `.env.example` to `your-gmail@gmail.com` (a placeholder), but keep `noreply@jbeltsolution.com` in `.env.template` since that is the user's actual production value pattern.

## Sources

### Primary (HIGH confidence)
- **Codebase inspection**: `.gitignore`, `.env.example`, `.env.template`, `pom.xml`, `application.yml.template`, `bin/env-templates.list`, `doc/01-setup.md`, `doc/04-deployment.md` -- all read directly
- **Git history**: `git log pom.xml` confirms 5 commits, file is tracked despite `.gitignore`

### Secondary (MEDIUM confidence)
- **Gmail App Password flow**: Google deprecated "Less Secure Apps" in May 2022. App Passwords require 2FA. URL: https://myaccount.google.com/apppasswords. This is well-established and stable.
- **SMTP port 587 with STARTTLS**: Standard for Gmail SMTP, confirmed by Spring Mail's existing configuration (`mail.smtp.starttls.enable: true` in `application.yml.template`)

### Tertiary (LOW confidence)
- None -- this phase deals with well-understood, stable technologies.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new libraries, documentation-only phase
- Architecture: HIGH -- follows existing project patterns for `.env` files and `doc/` guides
- Pitfalls: HIGH -- `.gitignore` behavior and Gmail App Password requirements are well-documented

**Research date:** 2026-02-24
**Valid until:** 2026-06-24 (Gmail SMTP and App Passwords are stable; unlikely to change)
