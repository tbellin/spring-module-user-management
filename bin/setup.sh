#!/usr/bin/env bash
# bin/setup.sh -- Project setup script
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Project Setup ==="

# 1. Check prerequisites
command -v java >/dev/null 2>&1 || { echo "ERROR: Java is required. Install Java 21."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "ERROR: Docker is required."; exit 1; }

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17+ required (21 recommended). Found: $JAVA_VERSION"
    exit 1
fi

# 2. Generate .env from template if not exists
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "Generating .env from .env.template..."
    "$SCRIPT_DIR/generate-config.sh"
    echo "IMPORTANT: Edit .env with your actual values before running the app."
else
    echo ".env already exists. Skipping generation."
fi

# 3. Make Maven wrapper executable
chmod +x "$PROJECT_ROOT/mvnw" 2>/dev/null || true

# 4. Build project (if Maven wrapper exists)
if [ -f "$PROJECT_ROOT/mvnw" ]; then
    echo "Building project..."
    "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" clean compile -B
fi

echo ""
echo "=== Setup Complete ==="
echo "To start in dev mode:  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "To start with Docker:  docker compose up --build"
