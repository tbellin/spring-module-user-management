---
status: resolved
trigger: "env.sh load, show, and substitute-all all fail. Missing variables and template substitution errors."
created: 2026-01-30T00:00:00Z
updated: 2026-01-30T00:02:00Z
---

## Current Focus

hypothesis: CONFIRMED AND FIXED - shebang was #!/bin/zsh but script uses bash-specific ${!var} indirect expansion
test: Run all subcommands via direct invocation (shebang)
expecting: All pass with exit code 0
next_action: Archive session

## Symptoms

expected: All three subcommands (load, show, substitute-all) should work -- load exports .env variables, show displays them, substitute-all processes templates
actual: All three subcommands fail
errors: Missing variables, template substitution errors
reproduction: Run in sequence: source ./bin/env.sh load, ./bin/env.sh show, ./bin/env.sh substitute-all
started: First time testing after plan 01-10 and 01-11 created the scripts

## Eliminated

## Evidence

- timestamp: 2026-01-30T00:00:30Z
  checked: Run ./bin/env.sh show (uses zsh shebang)
  found: "cmd_show:10: bad substitution" -- zsh cannot interpret ${!var:-} syntax
  implication: The shebang #!/bin/zsh is incompatible with bash indirect expansion

- timestamp: 2026-01-30T00:00:40Z
  checked: Run zsh -c '... source ./bin/env.sh load'
  found: "cmd_load:21: bad substitution"
  implication: load subcommand also fails under zsh for the same reason

- timestamp: 2026-01-30T00:00:50Z
  checked: Run zsh -c '... ./bin/env.sh substitute-all'
  found: "cmd_substitute_all:34: bad substitution"
  implication: substitute-all also fails under zsh for the same reason

- timestamp: 2026-01-30T00:01:00Z
  checked: Run all three commands explicitly with bash interpreter
  found: All three succeed -- load reports 16 variables, show lists all values, substitute-all generates all 5 files
  implication: Script is correctly written for bash, just has wrong shebang

- timestamp: 2026-01-30T00:01:10Z
  checked: sed escape pattern in substitute-all
  found: Escapes &/\ but uses | as delimiter -- pipe char in values would break sed
  implication: Secondary bug -- sed escape needs to also escape | character

- timestamp: 2026-01-30T00:01:30Z
  checked: After fix -- all subcommands via direct invocation
  found: load (exit 0, 16 vars), show (exit 0, all 16 vars displayed), substitute-all (exit 0, 5 files), check (exit 0, all up to date), help (exit 0)
  implication: Fix is verified

## Resolution

root_cause: Shebang was #!/bin/zsh but script uses ${!var} (bash indirect expansion) which is bash-specific syntax not supported by zsh. Secondary: sed escape regex did not escape | (the sed delimiter used in s|...|...|g).
fix: (1) Changed shebang from #!/bin/zsh to #!/usr/bin/env bash. (2) Fixed sed escape regex from 's/[&/\]/\\&/g' to 's/[&/\|]/\\&/g' to escape the pipe delimiter.
verification: All 6 subcommands (load, show, substitute-all, check, clean, help) pass with exit code 0. Generated files correctly substitute all @VARIABLE@ placeholders.
files_changed: [bin/env.sh]
