#!/usr/bin/env bash
# Mocked-client test harness for the Unified Service Scheduler.
# Start the app first:  ./mvnw spring-boot:run
# Then run:             ./docs/curl-examples.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080/api/v1}"
SLOT="${SLOT:-2030-06-01T09:00:00}"

hr() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }

hr "Reference data: dealerships"
curl -s "$BASE/dealerships"; echo

hr "Reference data: service types"
curl -s "$BASE/service-types"; echo

hr "Availability probe (Oil Change @ dealership 1, $SLOT)"
curl -s "$BASE/appointments/availability?dealershipId=1&serviceTypeId=1&desiredStart=$SLOT"; echo

hr "Booking #1 (expect 201 Created)"
curl -s -w "\n-> HTTP %{http_code}\n" -X POST "$BASE/appointments" \
  -H 'Content-Type: application/json' \
  -d "{\"dealershipId\":1,\"vehicleId\":1,\"serviceTypeId\":1,\"desiredStart\":\"$SLOT\"}"

hr "Booking #2 same slot (expect 201 — second of two bays)"
curl -s -w "\n-> HTTP %{http_code}\n" -X POST "$BASE/appointments" \
  -H 'Content-Type: application/json' \
  -d "{\"dealershipId\":1,\"vehicleId\":1,\"serviceTypeId\":1,\"desiredStart\":\"$SLOT\"}"

hr "Booking #3 same slot (expect 409 — no bay left)"
curl -s -w "\n-> HTTP %{http_code}\n" -X POST "$BASE/appointments" \
  -H 'Content-Type: application/json' \
  -d "{\"dealershipId\":1,\"vehicleId\":1,\"serviceTypeId\":1,\"desiredStart\":\"$SLOT\"}"

hr "Validation error (expect 400)"
curl -s -w "\n-> HTTP %{http_code}\n" -X POST "$BASE/appointments" \
  -H 'Content-Type: application/json' -d '{}'

hr "Outside opening hours (expect 422)"
curl -s -w "\n-> HTTP %{http_code}\n" -X POST "$BASE/appointments" \
  -H 'Content-Type: application/json' \
  -d '{"dealershipId":1,"vehicleId":1,"serviceTypeId":1,"desiredStart":"2030-06-01T07:30:00"}'

hr "Get appointment #1 (expect 200)"
curl -s -w "\n-> HTTP %{http_code}\n" "$BASE/appointments/1"

hr "Get missing appointment (expect 404)"
curl -s -w "\n-> HTTP %{http_code}\n" "$BASE/appointments/99999"
