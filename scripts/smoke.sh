#!/usr/bin/env bash
set -e

PORT=${PORT:-8080}
BASE_URL="http://localhost:${PORT}"
COOKIE_JAR=$(mktemp 2>/dev/null || echo "/tmp/smoke_cookies_$$.txt")
SERVER_PID=""

# Ensure JAVA_HOME is in Unix format if in Cygwin/MSYS/Git Bash
if [ -n "$JAVA_HOME" ] && [ ! -x "$JAVA_HOME/bin/java" ]; then
    if command -v cygpath >/dev/null 2>&1; then
        export JAVA_HOME=$(cygpath -u "$JAVA_HOME")
    else
        unset JAVA_HOME
    fi
fi

cleanup() {
    rm -f "$COOKIE_JAR" 2>/dev/null || true
    if [ -n "$SERVER_PID" ]; then
        echo "Stopping background server (PID: $SERVER_PID)..."
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

# 0. Check if server is already running, or start it
if curl -s -o /dev/null -m 2 "$BASE_URL/api/v1/feed/public/explore"; then
    echo "Server is already running on $BASE_URL."
else
    echo "Starting InstaClone application with profile 'dev'..."
    if [ -f "./mvnw" ]; then
        ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
    else
        mvn spring-boot:run -Dspring-boot.run.profiles=dev &
    fi
    SERVER_PID=$!
    echo "Waiting for server to become healthy on $BASE_URL..."
    MAX_ATTEMPTS=60
    ATTEMPT=0
    while ! curl -s -o /dev/null -m 2 "$BASE_URL/api/v1/feed/public/explore"; do
        sleep 2
        ATTEMPT=$((ATTEMPT + 1))
        if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
            echo "ERROR: Server failed to start within $((MAX_ATTEMPTS * 2)) seconds."
            exit 1
        fi
    done
    echo "Server is up and healthy!"
fi

echo "=========================================="
echo "Starting Smoke Test Suite (12 Scenarios)..."
echo "=========================================="

TOTAL=0
PASSED=0

assert_status() {
    local name="$1"
    local expected="$2"
    local actual="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual" -eq "$expected" ]; then
        echo "[PASS] ($TOTAL/12) $name -> HTTP $actual"
        PASSED=$((PASSED + 1))
    else
        echo "[FAIL] ($TOTAL/12) $name -> Expected HTTP $expected, got $actual"
        exit 1
    fi
}

# 1. Public Feed on Chain 1 (Order 1) -> 200 OK
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/feed/public/explore")
assert_status "1. Public Feed Explore (Anonymous)" 200 "$CODE"

# 2. Anonymous POST /posts without credentials -> 401 Unauthorized
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/posts" \
  -H "Content-Type: application/json" \
  -d '{"caption":"Anon post"}')
assert_status "2. Anonymous POST /posts (Unauthenticated)" 401 "$CODE"

# 3. User alice accesses moderation reports -> 403 Forbidden
CODE=$(curl -s -o /dev/null -w "%{http_code}" -u alice:Password123! -X GET "$BASE_URL/api/v1/moderation/reports")
assert_status "3. Regular User accesses Moderation (Forbidden)" 403 "$CODE"

# 4. GET /api/v1/csrf/token and Login via session (Chain 3) -> 200 OK
CSRF_JSON=$(curl -s -c "$COOKIE_JAR" "$BASE_URL/api/v1/csrf/token")
CSRF_TOKEN=$(echo "$CSRF_JSON" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)

LOGIN_CODE=$(curl -s -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" -c "$COOKIE_JAR" -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d '{"username":"alice","password":"Password123!"}')
assert_status "4. Session Login with CSRF Token" 200 "$LOGIN_CODE"

# 5. POST /posts with valid session but WITHOUT CSRF token -> 403 Forbidden
CODE=$(curl -s -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" -X POST "$BASE_URL/api/v1/posts" \
  -H "Content-Type: application/json" \
  -d '{"caption":"Missing CSRF"}')
assert_status "5. Authenticated session write missing CSRF Token" 403 "$CODE"

# 6. POST /api/v1/auth/register (Chain 2 permitAll) -> 201 Created
UNIQUE_USER="smoke_$(date +%s)_$$"
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$UNIQUE_USER\",\"email\":\"${UNIQUE_USER}@test.com\",\"password\":\"Password123!\"}")
assert_status "6. User Registration via Chain 2" 201 "$CODE"

# 7. POST /api/v1/auth/token -> 200 OK (Generates JWT Access + Refresh token)
TOKEN_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Password123!"}')
CODE=$(echo "$TOKEN_RESP" | tail -n1)
TOKEN_BODY=$(echo "$TOKEN_RESP" | head -n -1)
assert_status "7. Generate JWT Token Pair" 200 "$CODE"

ACCESS_TOKEN=$(echo "$TOKEN_BODY" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4 || true)
if [ -z "$ACCESS_TOKEN" ]; then
    echo "ERROR: Failed to extract accessToken from response."
    exit 1
fi

# 8. GET /api/v1/users/me with Bearer JWT -> 200 OK (Chain 2 Stateless Resource Server)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/users/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
assert_status "8. Access /users/me with Bearer JWT" 200 "$CODE"

# 9. POST /api/v1/posts with Bearer JWT (no CSRF needed on Chain 2) -> 201 Created
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/posts" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"caption":"Stateless smoke post","imageUrl":"https://img/smoke.jpg"}')
assert_status "9. Stateless Write without CSRF" 201 "$CODE"

# 10. POST /api/v1/auth/logout-all with Bearer JWT -> 200 OK
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/logout-all" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
assert_status "10. Revoke all tokens & blacklist JTI (logout-all)" 200 "$CODE"

# 11. GET /api/v1/users/me with revoked Bearer JWT -> 401 Unauthorized
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/users/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
assert_status "11. Blacklisted JTI rejected immediately" 401 "$CODE"

# 12. GET /oauth2/authorization/github -> 302 Found
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/oauth2/authorization/github")
assert_status "12. OAuth 2.1 PKCE Authorization Endpoint" 302 "$CODE"

echo "=========================================="
echo "All 12 smoke tests PASSED successfully!"
echo "=========================================="
exit 0
