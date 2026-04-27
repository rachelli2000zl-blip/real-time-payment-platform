#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE_DIR="$ROOT_DIR/backend/ingestion-service"
TMP_DIR="$ROOT_DIR/.tmp"
COMPILE_LOG="$TMP_DIR/ingestion-compile.log"
APP_LOG="$TMP_DIR/ingestion-service.log"
EVENT_RESPONSE_FILE="$TMP_DIR/ingestion-events-response.json"
APP_PORT=18081
APP_URL="http://localhost:${APP_PORT}"
POSTGRES_CONTAINER="dataplatform-postgres"
POSTGRES_HEALTH_TIMEOUT_SECONDS=120
APP_HEALTH_TIMEOUT_SECONDS=120
APP_PID=""

mkdir -p "$TMP_DIR"

pass() {
  printf '✅ %s\n' "$1"
}

info() {
  printf 'ℹ️ %s\n' "$1"
}

fail() {
  printf '❌ %s\n' "$1" >&2
  exit 1
}

cleanup() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  fail "Neither 'docker compose' nor 'docker-compose' is installed."
fi

info "Starting PostgreSQL with Docker Compose..."
if ! (cd "$ROOT_DIR" && "${COMPOSE_CMD[@]}" up -d postgres >/dev/null); then
  fail "Failed to start PostgreSQL container."
fi

info "Waiting for PostgreSQL health check..."
postgres_status=""
postgres_deadline=$((SECONDS + POSTGRES_HEALTH_TIMEOUT_SECONDS))
while (( SECONDS < postgres_deadline )); do
  postgres_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$POSTGRES_CONTAINER" 2>/dev/null || true)"
  if [[ "$postgres_status" == "healthy" ]]; then
    pass "Docker PostgreSQL is healthy"
    break
  fi
  sleep 2
done

if [[ "$postgres_status" != "healthy" ]]; then
  fail "PostgreSQL container is not healthy (status: ${postgres_status:-unknown})."
fi

info "Compiling ingestion-service..."
if ! (cd "$SERVICE_DIR" && mvn clean compile >"$COMPILE_LOG" 2>&1); then
  printf '\nLast compile log lines:\n' >&2
  tail -n 80 "$COMPILE_LOG" >&2 || true
  fail "ingestion-service compilation failed."
fi
pass "ingestion-service compiled"

info "Starting ingestion-service on port ${APP_PORT}..."
(
  cd "$SERVICE_DIR"
  SERVER_PORT="$APP_PORT" mvn spring-boot:run >"$APP_LOG" 2>&1
) &
APP_PID=$!

health_response=""
health_deadline=$((SECONDS + APP_HEALTH_TIMEOUT_SECONDS))
while (( SECONDS < health_deadline )); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    printf '\nLast application log lines:\n' >&2
    tail -n 120 "$APP_LOG" >&2 || true
    fail "ingestion-service exited before health check passed."
  fi

  health_response="$(curl -s "$APP_URL/actuator/health" || true)"
  if [[ "$health_response" == *'"status":"UP"'* ]]; then
    pass "ingestion-service started on port ${APP_PORT}"
    pass "health check passed"
    break
  fi

  sleep 2
done

if [[ "$health_response" != *'"status":"UP"'* ]]; then
  printf '\nLast application log lines:\n' >&2
  tail -n 120 "$APP_LOG" >&2 || true
  fail "Health endpoint did not return UP within ${APP_HEALTH_TIMEOUT_SECONDS}s."
fi

valid_payload='{
  "eventId":"5ca37e90-70a5-445b-8644-f8ca6b5f7478",
  "eventType":"payment.created",
  "occurredAt":"2026-01-15T10:15:30Z",
  "customerId":"cust-123",
  "amount":12.50,
  "currency":"USD",
  "schemaVersion":1,
  "metadata":{"source":"web"}
}'

http_code="$(curl -sS -o "$EVENT_RESPONSE_FILE" -w "%{http_code}" \
  -X POST "$APP_URL/events" \
  -H "Content-Type: application/json" \
  --data "$valid_payload" || true)"

if [[ "$http_code" != "202" ]]; then
  printf '\nResponse body:\n%s\n' "$(cat "$EVENT_RESPONSE_FILE" 2>/dev/null || true)" >&2
  printf '\nLast application log lines:\n' >&2
  tail -n 120 "$APP_LOG" >&2 || true
  fail "/events returned HTTP ${http_code} (expected 202)."
fi

if ! grep -q '"status":"accepted"' "$EVENT_RESPONSE_FILE"; then
  printf '\nResponse body:\n%s\n' "$(cat "$EVENT_RESPONSE_FILE" 2>/dev/null || true)" >&2
  fail "/events response does not contain status=accepted."
fi

pass "/events returned 202 accepted"
printf '🎉 Local ingestion pipeline test passed\n'
info "PostgreSQL container was left running."
