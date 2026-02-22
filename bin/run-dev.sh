#!/usr/bin/env bash
# bin/run-dev.sh -- Start the application in dev mode (H2 database, local)
# Usage: ./bin/run-dev.sh
set -euo pipefail

# ---------------------------------------------------------------------------
# Path resolution
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

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
# Prerequisite checks
# ---------------------------------------------------------------------------
# Check Java is installed
if ! command -v java >/dev/null 2>&1; then
    red "Error: Java is required. Install Java 21+."
    exit 1
fi

# Check Java version >= 17
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    red "Error: Java 17+ required (21 recommended). Found: $JAVA_VERSION"
    exit 1
fi
green "Java $JAVA_VERSION detected"

# Check Maven wrapper
if [ ! -x "./mvnw" ]; then
    red "Error: Maven wrapper not found or not executable. Run: ./bin/setup.sh"
    exit 1
fi

# ---------------------------------------------------------------------------
# Environment loading
# ---------------------------------------------------------------------------
source ./bin/env.sh load

# ---------------------------------------------------------------------------
# Template substitution
# ---------------------------------------------------------------------------
./bin/env.sh substitute-all

# ---------------------------------------------------------------------------
# Port availability check
# ---------------------------------------------------------------------------
PORT="${APP_PORT:-8080}"

if lsof -i ":$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    red "Error: Port $PORT is already in use."
    yellow "  Run: ./bin/check-port.sh $PORT go"
    exit 1
fi

# ---------------------------------------------------------------------------
# Start application
# ---------------------------------------------------------------------------
green "Starting in dev mode (H2) on port $PORT..."
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
