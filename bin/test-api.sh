#!/bin/bash
# bin/test-api.sh -- Comprehensive cURL API test suite
# Tests all REST API endpoints with colored pass/fail output
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
# Configuration (overridable via environment)
# ---------------------------------------------------------------------------
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-tizianobellin@yahoo.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-password123}"
TEST_EMAIL="testuser-$(date +%s)@example.com"
TEST_PASSWORD="TestPass123!"

# ---------------------------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------------------------
if ! command -v curl >/dev/null 2>&1; then
    red "Error: curl is not installed"
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    red "Error: jq is not installed"
    red "  macOS:  brew install jq"
    red "  Linux:  apt-get install jq"
    exit 1
fi

# ---------------------------------------------------------------------------
# Test infrastructure
# ---------------------------------------------------------------------------
PASS=0
FAIL=0
TOTAL=0

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" -eq "$expected" ]; then
        green "  PASS: $test_name (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        red "  FAIL: $test_name (expected HTTP $expected, got HTTP $actual)"
        FAIL=$((FAIL + 1))
    fi
}

do_request() {
    local method="$1"; shift
    local url="$1"; shift
    # remaining args are extra curl flags
    RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        "$@")
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$d')
}

# ---------------------------------------------------------------------------
# Health check with wait loop
# ---------------------------------------------------------------------------
yellow "Waiting for application at $BASE_URL ..."
HEALTH_OK=false
for i in $(seq 1 30); do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
    if [ "$HTTP_STATUS" = "200" ]; then
        HEALTH_OK=true
        break
    fi
    sleep 1
done

if [ "$HEALTH_OK" = false ]; then
    red "Application is not running at $BASE_URL"
    red "Start it with: ./bin/run-dev.sh"
    exit 1
fi

do_request GET "$BASE_URL/actuator/health"
assert_status "Health check" 200 "$HTTP_CODE"

# ===========================================================================
# Section: Public Endpoints
# ===========================================================================
echo ""
yellow "=== Public Endpoints ==="

# Register new user
do_request POST "$BASE_URL/api/v1/auth/register" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"firstName\":\"Test\",\"lastName\":\"User\"}"
assert_status "Register new user" 201 "$HTTP_CODE"

# Register duplicate email
do_request POST "$BASE_URL/api/v1/auth/register" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"firstName\":\"Test\",\"lastName\":\"User\"}"
assert_status "Register duplicate email" 409 "$HTTP_CODE"

# Register invalid email
do_request POST "$BASE_URL/api/v1/auth/register" \
    -d '{"email":"not-an-email","password":"TestPass123!","firstName":"Test","lastName":"User"}'
assert_status "Register invalid email" 400 "$HTTP_CODE"

# Login wrong password
do_request POST "$BASE_URL/api/v1/auth/login" \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"wrong\",\"rememberMe\":false}"
assert_status "Login wrong password" 401 "$HTTP_CODE"

# Login as admin
do_request POST "$BASE_URL/api/v1/auth/login" \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\",\"rememberMe\":false}"
assert_status "Login as admin" 200 "$HTTP_CODE"
ADMIN_TOKEN=$(echo "$BODY" | jq -r '.token')

# Forgot password (SEC-01 safe -- always returns 200)
do_request POST "$BASE_URL/api/v1/auth/forgot-password" \
    -d '{"email":"nonexistent@example.com"}'
assert_status "Forgot password (SEC-01 safe)" 200 "$HTTP_CODE"

# Resend verification
do_request POST "$BASE_URL/api/v1/auth/resend-verification" \
    -d "{\"email\":\"$TEST_EMAIL\"}"
assert_status "Resend verification" 200 "$HTTP_CODE"

# ===========================================================================
# Section: Authenticated Endpoints
# ===========================================================================
echo ""
yellow "=== Authenticated Endpoints (using admin token) ==="

# Get profile
do_request GET "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $ADMIN_TOKEN"
assert_status "Get profile" 200 "$HTTP_CODE"

# Update profile
do_request PUT "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"displayName":"Admin Updated","firstName":"Admin","lastName":"Updated"}'
assert_status "Update profile" 200 "$HTTP_CODE"

# Access without token
do_request GET "$BASE_URL/api/v1/users/me"
assert_status "Access without token" 401 "$HTTP_CODE"

# ===========================================================================
# Section: Admin Endpoints
# ===========================================================================
echo ""
yellow "=== Admin Endpoints (using admin token) ==="

# List users
do_request GET "$BASE_URL/api/v1/admin/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN"
assert_status "List users" 200 "$HTTP_CODE"

# List users with search
do_request GET "$BASE_URL/api/v1/admin/users?search=tiziano" \
    -H "Authorization: Bearer $ADMIN_TOKEN"
assert_status "List users with search" 200 "$HTTP_CODE"

# Create user
ADMIN_CREATED_EMAIL="admin-created-$(date +%s)@example.com"
do_request POST "$BASE_URL/api/v1/admin/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{\"email\":\"$ADMIN_CREATED_EMAIL\",\"username\":\"admincreated\",\"firstName\":\"Admin\",\"lastName\":\"Created\",\"enabled\":true,\"roles\":[\"ROLE_USER\"]}"
assert_status "Create user" 201 "$HTTP_CODE"
CREATED_USER_ID=$(echo "$BODY" | jq -r '.id')

# Update user
do_request PUT "$BASE_URL/api/v1/admin/users/$CREATED_USER_ID" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"firstName":"Updated","lastName":"ByAdmin","roles":["ROLE_USER"]}'
assert_status "Update user" 200 "$HTTP_CODE"

# Toggle user status
do_request PATCH "$BASE_URL/api/v1/admin/users/$CREATED_USER_ID/status" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"enabled":false}'
assert_status "Toggle user status" 200 "$HTTP_CODE"

# Admin endpoint without token
do_request GET "$BASE_URL/api/v1/admin/users"
assert_status "Admin endpoint without token" 401 "$HTTP_CODE"

# ===========================================================================
# Summary
# ===========================================================================
echo ""
echo "================================"
echo "  Results: $PASS passed, $FAIL failed (of $TOTAL tests)"
echo "================================"
if [ "$FAIL" -gt 0 ]; then
    red "SOME TESTS FAILED"
    exit 1
else
    green "ALL TESTS PASSED"
    exit 0
fi
