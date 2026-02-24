# Phase 9: Package Rename + Version Bump - Research

**Researched:** 2026-02-24
**Domain:** Java package rename, Maven version bump, Spring Modulith namespace migration
**Confidence:** HIGH

## Summary

Phase 9 is a purely mechanical refactoring task: rename the root Java package from `com.example.usermanagement` to `org.jbelt.module` across all 77 Java source files (62 main + 15 test), restructure the corresponding directory trees, and update `pom.xml` with the new groupId (`org.jbelt`) and version (`1.2.0-SNAPSHOT`). No new dependencies are introduced. No runtime behavior changes. No configuration files (YAML, properties, SQL, Dockerfile, shell scripts) contain the old package name -- the rename is confined entirely to `.java` files, directory paths, and `pom.xml`.

The task is low-risk but high-breadth: every Java file must be touched, and the directory tree must be physically restructured from `com/example/usermanagement/` to `org/jbelt/module/`. The critical trap is an incomplete rename leaving stray `com.example.usermanagement` references, which would cause Spring Boot's component scan (rooted at `Application.class`) to miss beans, breaking both runtime and tests.

**Primary recommendation:** Use a scripted `find`+`sed` approach to update all `package` declarations and `import` statements across all 77 Java files, then physically move the directory tree. Verify with `grep -r "com.example.usermanagement" src/` returning zero results, then `mvn verify`.

## Standard Stack

### Core

No new libraries. This phase modifies existing files only.

| Tool | Purpose | Why Standard |
|------|---------|--------------|
| `sed` / `find` | Batch text replacement across 77 Java files | Deterministic, scriptable, no IDE dependency |
| `mv` / `mkdir` | Directory tree restructure | File-level atomic move |
| Maven 3.x (`mvn verify`) | Compilation + test verification gate | Already configured in project |

### Supporting

| Tool | Purpose | When to Use |
|------|---------|-------------|
| `grep -r` | Post-rename verification sweep | After all replacements, to catch any missed references |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `sed` batch rename | IDE "Rename Package" refactor | IDE handles it atomically but requires interactive IDE session; `sed` is scriptable and reproducible in CLI/CI context |
| `mvn versions:set` for version bump | Direct `pom.xml` edit | `versions:set` is overkill for a single-module project with one version change |

## Architecture Patterns

### Current Directory Structure (before rename)
```
src/main/java/
└── com/example/usermanagement/
    ├── Application.java                    # @SpringBootApplication root
    ├── auth/
    │   ├── package-info.java               # @ApplicationModule(allowedDependencies = {"user", "shared"})
    │   ├── AuthResponse.java
    │   ├── JwtService.java
    │   └── internal/
    │       ├── password/                   # 8 files
    │       └── verification/               # 6 files
    ├── shared/
    │   ├── package-info.java               # @ApplicationModule(allowedDependencies = {})
    │   ├── config/                         # 4 files
    │   ├── dto/                            # 2 files
    │   ├── email/                          # 3 files
    │   ├── exception/                      # 4 files
    │   └── web/                            # 1 file
    └── user/
        ├── package-info.java               # @ApplicationModule(allowedDependencies = {"shared"})
        ├── UserAuthDto.java
        ├── UserService.java
        └── internal/                       # 9 files

src/test/java/
└── com/example/usermanagement/
    ├── ApplicationTests.java
    ├── ModularityTests.java
    ├── SchemaComparisonTests.java
    ├── auth/                               # 9 test files
    └── user/                               # 2 test files
```

### Target Directory Structure (after rename)
```
src/main/java/
└── org/jbelt/module/
    ├── Application.java
    ├── auth/
    │   ├── package-info.java
    │   ├── ...
    │   └── internal/
    │       ├── password/
    │       └── verification/
    ├── shared/
    │   ├── package-info.java
    │   ├── config/
    │   ├── dto/
    │   ├── email/
    │   ├── exception/
    │   └── web/
    └── user/
        ├── package-info.java
        ├── ...
        └── internal/

src/test/java/
└── org/jbelt/module/
    ├── ApplicationTests.java
    ├── ModularityTests.java
    ├── SchemaComparisonTests.java
    ├── auth/
    └── user/
```

### Pattern 1: Batch Package Rename (find + sed)

**What:** Replace all occurrences of `com.example.usermanagement` with `org.jbelt.module` in every `.java` file, then move the directory tree.

**When to use:** CLI-driven refactoring without IDE.

**Steps:**
```bash
# Step 1: Replace package/import references in all Java files
find src -name "*.java" -exec sed -i '' 's/com\.example\.usermanagement/org.jbelt.module/g' {} +

# Step 2: Create new directory trees
mkdir -p src/main/java/org/jbelt/module
mkdir -p src/test/java/org/jbelt/module

# Step 3: Move files from old to new tree
mv src/main/java/com/example/usermanagement/* src/main/java/org/jbelt/module/
mv src/test/java/com/example/usermanagement/* src/test/java/org/jbelt/module/

# Step 4: Remove old empty directory tree
rm -rf src/main/java/com
rm -rf src/test/java/com

# Step 5: Verify zero stale references
grep -r "com.example.usermanagement" src/
# Must return zero results

# Step 6: Update pom.xml
sed -i '' 's|<groupId>com.example</groupId>|<groupId>org.jbelt</groupId>|' pom.xml
sed -i '' 's|<version>0.0.1-SNAPSHOT</version>|<version>1.2.0-SNAPSHOT</version>|' pom.xml
```

### Pattern 2: pom.xml Version Bump

**What:** Change `<groupId>com.example</groupId>` to `<groupId>org.jbelt</groupId>` and `<version>0.0.1-SNAPSHOT</version>` to `<version>1.2.0-SNAPSHOT</version>`.

**Caution:** The `<version>` tag appears in multiple places (parent, dependencies). Only the project-level `<version>` on line 16 should change. The parent Spring Boot version (`4.0.1`) and dependency versions must NOT be modified.

**Current pom.xml values (lines 13-16):**
```xml
<groupId>com.example</groupId>
<artifactId>user-management</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

**Target pom.xml values:**
```xml
<groupId>org.jbelt</groupId>
<artifactId>user-management</artifactId>
<version>1.2.0-SNAPSHOT</version>
```

### Anti-Patterns to Avoid

- **Changing @ApplicationModule allowedDependencies values:** The values `"user"`, `"shared"` are relative module names resolved by Spring Modulith from the root package. They are NOT fully-qualified package names. They MUST remain unchanged after rename.
- **Using `sed` with overly broad patterns:** A naive `s/com.example/org.jbelt/g` could hit unrelated text. Always use the full `com.example.usermanagement` string for replacement in Java files.
- **Changing the pom.xml `<version>` for parent or dependencies:** Only the project-level `<version>` (line 16) should change.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Package rename across 77 files | Manual file-by-file editing | `find + sed` batch replacement | Human error guaranteed with 77 files; sed is deterministic |
| Directory tree restructure | Creating files one by one | `mv` command to move entire subtree | Preserves file contents, timestamps; atomic operation |
| Version bump | Custom Maven plugin | Direct pom.xml edit (2 lines) | Single-module project; `mvn versions:set` adds unnecessary complexity |

**Key insight:** This is a mechanical text transformation. The correctness verification (`grep` + `mvn verify`) is more important than the transformation method.

## Common Pitfalls

### Pitfall 1: Incomplete package rename breaks Spring component scan

**What goes wrong:** If even one `.java` file retains `package com.example.usermanagement.*`, Spring Boot's `@SpringBootApplication` (now rooted at `org.jbelt.module`) will not discover beans in that file. Tests fail with `NoSuchBeanDefinitionException`. `ModularityTests` fails with "module not found."

**Why it happens:** `sed` misses a file, or a file is added after the rename script runs but before commit.

**How to avoid:** Run `grep -r "com.example.usermanagement" src/` after rename. Must return zero results. This is the primary verification gate.

**Warning signs:** Any compilation error mentioning "cannot find symbol" or "package does not exist" after rename.

### Pitfall 2: pom.xml version tag ambiguity

**What goes wrong:** The `<version>` XML tag appears multiple times in `pom.xml` -- once for the parent (line 10: `4.0.1`), once for the project (line 16: `0.0.1-SNAPSHOT`), and multiple times in dependency declarations. A broad `sed` replacement changes the wrong version.

**Why it happens:** Naive `sed 's/0.0.1-SNAPSHOT/1.2.0-SNAPSHOT/'` is safe because `0.0.1-SNAPSHOT` only appears once. But a pattern like `s/<version>.*<\/version>/...` would be catastrophic.

**How to avoid:** Use targeted replacement: match the exact string `0.0.1-SNAPSHOT` (unique in pom.xml) or use line-specific editing.

**Warning signs:** Maven resolution errors, parent version mismatch, dependency version corruption.

### Pitfall 3: Old directory tree not fully removed

**What goes wrong:** After moving files, the empty `com/example/usermanagement/` directory tree remains. While harmless for compilation, it creates git noise and confusion.

**Why it happens:** `mv` moves contents but leaves empty parent directories behind.

**How to avoid:** After moving, `rm -rf src/main/java/com` and `rm -rf src/test/java/com` to clean up the entire old tree.

**Warning signs:** `git status` showing deleted files AND untracked files (instead of renamed files).

### Pitfall 4: ModularityTests false-positive confusion

**What goes wrong:** After rename, `ModularityTests.verifiesModularStructure` fails. Developer assumes the rename broke something and wastes time debugging.

**Why it happens:** This test ALREADY fails before rename. The pre-existing failure is an ERROR (not a test failure) caused by Spring Modulith `Violations` exception -- auth and user modules access each other's internal types. This is a known architectural debt, not a rename-caused regression.

**How to avoid:** Capture the baseline before rename. After rename, the error should be identical in structure (same violation count, same cross-module dependency list) with only the package prefix changed from `com.example.usermanagement` to `org.jbelt.module`.

**Pre-existing baseline (captured 2026-02-24):**
- `ModularityTests.verifiesModularStructure`: ERROR (51 cross-module violations, all "Allowed targets" match)
- `ModularityTests.printsModuleArrangement`: PASS
- `SchemaComparisonTests.devAndProdMigrationsShouldHaveSameVersions`: FAILURE (V2 vs V3 mismatch -- unrelated to rename)

**Warning signs:** New violation types or violation count increasing after rename = something went wrong. Same violations with updated package prefix = expected.

## Code Examples

### Example 1: package-info.java before and after

Before:
```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }
)
package com.example.usermanagement.auth;
```

After:
```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "user", "shared" }   // UNCHANGED - relative names
)
package org.jbelt.module.auth;
```

### Example 2: Import statements before and after

Before:
```java
package com.example.usermanagement.user;

import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.BadRequestException;
import com.example.usermanagement.user.internal.AppUser;
```

After:
```java
package org.jbelt.module.user;

import org.jbelt.module.shared.dto.UserDto;
import org.jbelt.module.shared.exception.BadRequestException;
import org.jbelt.module.user.internal.AppUser;
```

### Example 3: pom.xml changes

Before:
```xml
<groupId>com.example</groupId>
<artifactId>user-management</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

After:
```xml
<groupId>org.jbelt</groupId>
<artifactId>user-management</artifactId>
<version>1.2.0-SNAPSHOT</version>
```

## Scope Verification: What Does NOT Change

Verified by direct codebase inspection (2026-02-24):

| File / Category | Change Needed? | Reason |
|-----------------|----------------|--------|
| `application.yml` | NO | Zero `com.example` references; no component-scan config |
| `application-dev.yml` | NO | Zero `com.example` references |
| `application-prod.yml` | NO | Zero `com.example` references |
| Flyway SQL migrations (`db/migration/`) | NO | SQL files have zero Java package references |
| Thymeleaf templates (`templates/`) | NO | Zero `com.example` references |
| `Dockerfile` | NO | Uses `target/*.jar` glob, no package reference |
| Shell scripts (`bin/`) | NO | Zero `com.example` references |
| `@ApplicationModule` `allowedDependencies` values | NO | Relative names `"user"`, `"shared"` -- NOT FQNs |
| `pom.xml` `<artifactId>` | NO | Stays `user-management` |
| `pom.xml` `<name>` | NO | Stays `User Management Server` |
| `pom.xml` parent/dependency versions | NO | Only project-level groupId and version change |
| `banner.txt` / `banner.txt.template` | NO | Uses Spring Boot property substitution, not package names |

## Complete File Inventory

### Files that MUST change (78 total)

**62 main Java files** -- package declarations and import statements:
- `src/main/java/com/example/usermanagement/Application.java`
- `src/main/java/com/example/usermanagement/auth/` (2 public + 1 package-info + 14 internal = 17 files)
- `src/main/java/com/example/usermanagement/shared/` (1 package-info + 14 files = 15 files)
- `src/main/java/com/example/usermanagement/user/` (1 package-info + 2 public + 9 internal = 12 files)
- Total: 1 + 17 + 15 + 12 + (password: 8) + (verification: 6) + remaining = 62 files

**15 test Java files** -- package declarations and import statements:
- `src/test/java/com/example/usermanagement/ApplicationTests.java`
- `src/test/java/com/example/usermanagement/ModularityTests.java`
- `src/test/java/com/example/usermanagement/SchemaComparisonTests.java`
- `src/test/java/com/example/usermanagement/auth/` (9 test files)
- `src/test/java/com/example/usermanagement/user/` (2 test files)

**1 pom.xml** -- groupId and version only

### Directory trees that MUST be restructured

- `src/main/java/com/example/usermanagement/` --> `src/main/java/org/jbelt/module/`
- `src/test/java/com/example/usermanagement/` --> `src/test/java/org/jbelt/module/`

Old `com/` directory trees must be deleted after move.

## Pre-existing Test Baseline (2026-02-24)

Total tests: 95
- Passing: 93
- Failing: 1 (`SchemaComparisonTests.devAndProdMigrationsShouldHaveSameVersions` -- V2/V3 migration name mismatch)
- Error: 1 (`ModularityTests.verifiesModularStructure` -- 51 cross-module internal-type violations)

After Phase 9 rename, the expectation is:
- Same 93 tests pass (with updated package prefixes in class names)
- `SchemaComparisonTests` same failure (unrelated to rename)
- `ModularityTests.verifiesModularStructure` same ERROR with same violation structure, just `org.jbelt.module` prefix instead of `com.example.usermanagement`

**Success metric:** `mvn verify` exits with same test result summary (95 run, 1 failure, 1 error, 0 skipped). If the failure/error count increases, the rename introduced a regression.

## Execution Order

The rename must be done as a single atomic operation:

1. Replace all `com.example.usermanagement` with `org.jbelt.module` in all 77 `.java` files
2. Move directory trees from old path to new path
3. Delete old empty `com/` directory trees
4. Update `pom.xml` groupId and version
5. Verify: `grep -r "com.example.usermanagement" src/` returns zero results
6. Verify: `grep "com.example" pom.xml` returns zero results
7. Gate: `mvn verify` -- same test outcome as baseline (no new failures)

**Do NOT split into separate commits** (e.g., rename first, version bump second). A partial rename will not compile.

## Open Questions

1. **`artifactId` stays `user-management`?**
   - What we know: Requirements PKG-01 through PKG-04 specify package rename and groupId/version change. No mention of changing `artifactId`.
   - What's unclear: Whether `user-management` should also change.
   - Recommendation: Leave `artifactId` as `user-management` per requirements. If user wants to change it, that's a separate decision.

2. **`mvn verify` baseline includes 2 pre-existing failures -- is that acceptable?**
   - What we know: Success criteria say "ModularityTests produces no new failures beyond the pre-existing cosmetic false-positive." There are actually 2 pre-existing failures (ModularityTests + SchemaComparisonTests).
   - What's unclear: Whether SchemaComparisonTests failure is also considered "pre-existing and acceptable."
   - Recommendation: Treat both as pre-existing. The rename does not change SQL migration files, so SchemaComparisonTests result will be identical. Verify same failure count after rename.

## Sources

### Primary (HIGH confidence)
- Direct codebase inspection of all 77 Java files, pom.xml, application.yml, Dockerfile, shell scripts, SQL migrations, Thymeleaf templates (2026-02-24)
- `mvn verify` baseline run (2026-02-24): 95 tests, 1 failure, 1 error
- `ModularityTests` standalone run with full surefire report capture
- `SchemaComparisonTests` surefire report capture
- Prior v1.2 research: `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md`, `.planning/research/SUMMARY.md`

### Secondary (MEDIUM confidence)
- Spring Modulith module name resolution behavior (relative names in `allowedDependencies`) -- verified by inspecting existing `package-info.java` files and cross-referencing with prior research documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new libraries, pure text transformation
- Architecture: HIGH -- verified every file in codebase, zero ambiguity on what changes
- Pitfalls: HIGH -- all pitfalls identified from prior research and verified against actual codebase state
- Test baseline: HIGH -- captured actual `mvn verify` output on 2026-02-24

**Research date:** 2026-02-24
**Valid until:** indefinite (mechanical refactoring, no external dependencies to go stale)
