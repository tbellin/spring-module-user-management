#!/usr/bin/env bash
# bin/generate-config.sh -- Generate config files from templates
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Generate .env from .env.template
if [ -f "$PROJECT_ROOT/.env.template" ]; then
    cp "$PROJECT_ROOT/.env.template" "$PROJECT_ROOT/.env"
    echo "Generated .env from .env.template"
    echo "Edit .env with your actual configuration values."
else
    echo "ERROR: .env.template not found at $PROJECT_ROOT/.env.template"
    exit 1
fi
