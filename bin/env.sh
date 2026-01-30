#!/usr/bin/env bash
# bin/env.sh -- Environment management CLI
# Subcommands: load, show, substitute-all, check, clean, help
set -euo pipefail

# ---------------------------------------------------------------------------
# Color helpers (disabled if stdout is not a terminal)
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
    green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
    red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
    yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
else
    green()  { printf '%s\n' "$*"; }
    red()    { printf '%s\n' "$*"; }
    yellow() { printf '%s\n' "$*"; }
fi

# ---------------------------------------------------------------------------
# Usage / help
# ---------------------------------------------------------------------------
show_help() {
    cat <<'USAGE'
Usage: ./bin/env.sh <command>

Commands:
  load            Load .env variables into shell (use: source ./bin/env.sh load)
  show            Display all environment variables
  substitute-all  Generate config files from templates
  check           Check if generated files are up to date
  clean           Remove all generated config files
  help            Show this help message

Examples:
  source ./bin/env.sh load        # Export variables to current shell
  ./bin/env.sh substitute-all     # Generate all config files
  ./bin/env.sh check              # Check for stale generated files
  ./bin/env.sh clean              # Remove generated files
USAGE
}

# ---------------------------------------------------------------------------
# Root directory check
# ---------------------------------------------------------------------------
check_project_root() {
    if [ ! -f "bin/env-templates.list" ]; then
        red "Error: Must run from project root directory"
        red "Expected to find bin/env-templates.list"
        exit 1
    fi
}

# ---------------------------------------------------------------------------
# Parse .env.example for variable names
# ---------------------------------------------------------------------------
get_variable_names() {
    # Extract variable names from .env.example (lines matching VAR_NAME=...)
    # Ignore comments and blank lines
    grep -E '^[A-Za-z_][A-Za-z0-9_]*=' .env.example 2>/dev/null \
        | cut -d= -f1 \
        || true
}

# ---------------------------------------------------------------------------
# Parse env-templates.list for template paths
# ---------------------------------------------------------------------------
get_template_paths() {
    # Read bin/env-templates.list, skip comments and blank lines
    grep -v '^\s*#' bin/env-templates.list 2>/dev/null \
        | grep -v '^\s*$' \
        || true
}

# ---------------------------------------------------------------------------
# Subcommand: load
# ---------------------------------------------------------------------------
cmd_load() {
    if [ ! -f ".env" ]; then
        yellow "Warning: .env file not found. Create it from .env.example"
        yellow "  cp .env.example .env"
        return 0 2>/dev/null || exit 0
    fi

    # Export all variables from .env
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a

    # Count loaded variables
    local count
    count=$(grep -cE '^[A-Za-z_][A-Za-z0-9_]*=' .env 2>/dev/null || echo 0)

    # Validate required variables from .env.example
    local var_names missing=0
    var_names=$(get_variable_names)
    for var in $var_names; do
        if [ -z "${!var:-}" ]; then
            yellow "Warning: $var is not set (required by .env.example)"
            missing=$((missing + 1))
        fi
    done

    green "Environment loaded: $count variables from .env"
    if [ "$missing" -gt 0 ]; then
        yellow "  $missing variable(s) not set -- see warnings above"
    fi

    return 0 2>/dev/null || exit 0
}

# ---------------------------------------------------------------------------
# Subcommand: show
# ---------------------------------------------------------------------------
cmd_show() {
    local var_names
    var_names=$(get_variable_names)

    if [ -z "$var_names" ]; then
        yellow "No variables found in .env.example"
        return
    fi

    for var in $var_names; do
        local value="${!var:-}"
        if [ -n "$value" ]; then
            echo "$var=$value"
        else
            echo "$var=[not set]"
        fi
    done
}

# ---------------------------------------------------------------------------
# Subcommand: substitute-all
# ---------------------------------------------------------------------------
cmd_substitute_all() {
    local templates count=0
    templates=$(get_template_paths)

    if [ -z "$templates" ]; then
        yellow "No templates listed in bin/env-templates.list"
        return
    fi

    while IFS= read -r template_path; do
        # Verify template exists
        if [ ! -f "$template_path" ]; then
            red "Error: Template file not found: $template_path"
            exit 1
        fi

        # Determine output path (remove .template extension)
        local output_path="${template_path%.template}"

        # Read template content
        local content
        content=$(cat "$template_path")

        # Find all @VARIABLE@ placeholders and substitute
        # Build sed expression from all env variables
        local sed_args=()
        local placeholders
        placeholders=$(grep -oE '@[A-Za-z_][A-Za-z0-9_]*@' "$template_path" 2>/dev/null | sort -u || true)

        for placeholder in $placeholders; do
            # Strip @ delimiters to get variable name
            local var_name="${placeholder#@}"
            var_name="${var_name%@}"

            local var_value="${!var_name:-}"
            if [ -z "$var_value" ] && [ "${!var_name+set}" != "set" ]; then
                red "Error: Variable $var_name is not set (required by $template_path)"
                exit 1
            fi

            # Escape special sed characters in the value
            local escaped_value
            escaped_value=$(printf '%s\n' "$var_value" | sed 's/[&/\]/\\&/g')
            sed_args+=(-e "s|@${var_name}@|${escaped_value}|g")
        done

        # Apply substitutions and write output
        if [ ${#sed_args[@]} -gt 0 ]; then
            sed "${sed_args[@]}" "$template_path" > "$output_path"
        else
            # No placeholders -- just copy
            cp "$template_path" "$output_path"
        fi

        green "  $template_path -> $output_path"
        count=$((count + 1))
    done <<< "$templates"

    green "Generated $count config file(s)"
}

# ---------------------------------------------------------------------------
# Subcommand: check
# ---------------------------------------------------------------------------
cmd_check() {
    local templates
    templates=$(get_template_paths)

    if [ -z "$templates" ]; then
        yellow "No templates listed in bin/env-templates.list"
        return
    fi

    while IFS= read -r template_path; do
        local output_path="${template_path%.template}"

        if [ ! -f "$output_path" ]; then
            yellow "  $output_path: not generated (run substitute-all)"
        elif [ "$template_path" -nt "$output_path" ]; then
            yellow "  $output_path: stale (template modified, run substitute-all)"
        else
            green "  $output_path: up to date"
        fi
    done <<< "$templates"
}

# ---------------------------------------------------------------------------
# Subcommand: clean
# ---------------------------------------------------------------------------
cmd_clean() {
    local templates count=0
    templates=$(get_template_paths)

    if [ -z "$templates" ]; then
        yellow "No templates listed in bin/env-templates.list"
        return
    fi

    while IFS= read -r template_path; do
        local output_path="${template_path%.template}"
        if [ -f "$output_path" ]; then
            rm "$output_path"
            echo "  Deleted: $output_path"
            count=$((count + 1))
        fi
    done <<< "$templates"

    green "Cleaned $count generated file(s)"
}

# ---------------------------------------------------------------------------
# Main dispatch
# ---------------------------------------------------------------------------
COMMAND="${1:-help}"

# help does not require project root check
if [ "$COMMAND" = "help" ]; then
    show_help
    exit 0
fi

check_project_root

case "$COMMAND" in
    load)
        cmd_load
        ;;
    show)
        cmd_show
        ;;
    substitute-all)
        cmd_substitute_all
        ;;
    check)
        cmd_check
        ;;
    clean)
        cmd_clean
        ;;
    *)
        red "Error: Unknown command '$COMMAND'"
        echo ""
        show_help
        exit 1
        ;;
esac
