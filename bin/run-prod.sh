#!/usr/bin/env bash
# bin/run-prod.sh -- Production stack launcher (Docker Compose)
# Usage: ./bin/run-prod.sh [up|down|logs|status|restart|help]
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
# Usage
# ---------------------------------------------------------------------------
show_usage() {
    cat <<'USAGE'
Usage: ./bin/run-prod.sh [command]

Commands:
  up        Start the full production stack (default)
  down      Stop and remove all containers
  logs      Follow container logs
  status    Show container status
  restart   Stop and restart the full stack
  help      Show this help message

Examples:
  ./bin/run-prod.sh          # Start production stack
  ./bin/run-prod.sh up       # Same as above
  ./bin/run-prod.sh logs     # Follow logs
  ./bin/run-prod.sh down     # Stop everything
  ./bin/run-prod.sh restart  # Full restart with rebuild
USAGE
}

# ---------------------------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------------------------
check_prerequisites() {
    # Check Docker is installed
    if ! command -v docker >/dev/null 2>&1; then
        red "Error: Docker is required. Install Docker Desktop."
        exit 1
    fi

    # Check Docker Compose v2
    if ! docker compose version >/dev/null 2>&1; then
        red "Error: Docker Compose v2 required. Update Docker Desktop."
        exit 1
    fi

    # Check Docker daemon is running
    if ! docker info >/dev/null 2>&1; then
        red "Error: Docker daemon is not running. Start Docker Desktop."
        exit 1
    fi

    green "Docker prerequisites OK"
}

# ---------------------------------------------------------------------------
# Environment loading and template substitution
# ---------------------------------------------------------------------------
load_environment() {
    source ./bin/env.sh load
    ./bin/env.sh substitute-all
}

# ---------------------------------------------------------------------------
# Subcommand: up
# ---------------------------------------------------------------------------
cmd_up() {
    local app_port="${APP_PORT:-8080}"
    local pgadmin_port="${PGADMIN_PORT:-5050}"

    green "Starting production stack..."
    docker compose up --build -d

    echo ""
    green "=== Production Stack Running ==="
    docker compose ps
    echo ""
    green "Application:  http://localhost:${app_port}"
    green "Swagger UI:   http://localhost:${app_port}/swagger-ui.html"
    green "PgAdmin:      http://localhost:${pgadmin_port}"
    echo ""
    yellow "Use './bin/run-prod.sh logs' to follow container logs"
    yellow "Use './bin/run-prod.sh down' to stop the stack"
}

# ---------------------------------------------------------------------------
# Subcommand: down
# ---------------------------------------------------------------------------
cmd_down() {
    yellow "Stopping production stack..."
    docker compose down
    green "Production stack stopped."
}

# ---------------------------------------------------------------------------
# Subcommand: logs
# ---------------------------------------------------------------------------
cmd_logs() {
    docker compose logs -f
}

# ---------------------------------------------------------------------------
# Subcommand: status
# ---------------------------------------------------------------------------
cmd_status() {
    docker compose ps
}

# ---------------------------------------------------------------------------
# Subcommand: restart
# ---------------------------------------------------------------------------
cmd_restart() {
    yellow "Restarting production stack..."
    docker compose down
    docker compose up --build -d

    echo ""
    green "Production stack restarted."
    docker compose ps
}

# ---------------------------------------------------------------------------
# Main dispatch
# ---------------------------------------------------------------------------
COMMAND="${1:-up}"

# Help does not require prerequisites
if [ "$COMMAND" = "help" ] || [ "$COMMAND" = "--help" ] || [ "$COMMAND" = "-h" ]; then
    show_usage
    exit 0
fi

check_prerequisites
load_environment

case "$COMMAND" in
    up)
        cmd_up
        ;;
    down)
        cmd_down
        ;;
    logs)
        cmd_logs
        ;;
    status)
        cmd_status
        ;;
    restart)
        cmd_restart
        ;;
    *)
        red "Error: Unknown command '$COMMAND'"
        echo ""
        show_usage
        exit 1
        ;;
esac
