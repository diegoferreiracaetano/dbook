#!/bin/bash
# Populates the local database with realistic flight data for demos/manual testing —
# through POST /v1/admin/flights/import (the same endpoint the portal uses, covered by the test suite), so it can
# never drift from what the API actually accepts and runs again without duplicating flights.
#
# Local dev only: promotes the seed user to SUPER_ADMIN via a direct SQL UPDATE, since there's
# no self-promotion endpoint (closed security decision — see CHECKLIST.md M3). Never run
# this against a real environment.
#
# Usage: ./scripts/seed-flights.sh [flight_count]
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
FLIGHT_COUNT="${1:-1000}"
ADMIN_EMAIL="seed-admin@example.com"
ADMIN_PASSWORD="seed-password-123"

ROUTES=(
  "GRU:GIG" "GIG:GRU" "GRU:JFK" "JFK:GRU" "GIG:JFK" "JFK:GIG"
  "GRU:LHR" "LHR:GRU" "GRU:CDG" "CDG:GRU" "GRU:LIS" "LIS:GRU"
  "GRU:MIA" "MIA:GRU" "GRU:EZE" "EZE:GRU"
  "GIG:MIA" "MIA:GIG" "GIG:LIS" "LIS:GIG"
  "JFK:LHR" "LHR:JFK" "JFK:CDG" "CDG:JFK" "JFK:MIA" "MIA:JFK"
  "GRU:NRT" "NRT:GRU" "JFK:NRT" "NRT:JFK" "LHR:NRT" "NRT:LHR"
)
SEAT_CLASSES=("ECONOMY" "BUSINESS")
AIRLINES=("LA" "AD" "G3" "AA" "DL" "UA")
# Must match SeatLayout.kt's KNOWN_AIRCRAFT_TYPES — varying this per flight is what gives
# the seat map real 2+2 / 3+3 / 3+4+3 variety across searches, not just a cosmetic label.
AIRCRAFT_TYPES=("Embraer E195" "Airbus A320" "Boeing 777")

echo "Registering seed admin user..."
curl -s -o /dev/null -X POST "$BASE_URL/v1/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\",\"name\":\"Seed Admin\"}" || true

echo "Promoting to SUPER_ADMIN (direct SQL — local dev only)..."
docker compose exec -T postgres psql -U dbook -d dbook -c \
  "UPDATE app_user SET role = 'SUPER_ADMIN' WHERE email = '$ADMIN_EMAIL';" > /dev/null

ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/v1/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Could not obtain an access token — is the app running on $BASE_URL?" >&2
  exit 1
fi

# macOS (BSD date) and Linux (GNU date) disagree on relative-date flags — detect once.
if date -v+1d >/dev/null 2>&1; then
  DATE_FLAVOR="bsd"
else
  DATE_FLAVOR="gnu"
fi

date_days_ahead() {
  if [ "$DATE_FLAVOR" = "bsd" ]; then
    date -v+"$1"d +"%Y-%m-%d"
  else
    date -d "+$1 days" +"%Y-%m-%d"
  fi
}

echo "Building $FLIGHT_COUNT flights..."
HEADER="flightNumber,airlineIataCode,originIataCode,destinationIataCode,departureTime,arrivalTime,seatClass,price,totalCapacity,aircraftType"
CSV_FILE="$(mktemp)"
trap 'rm -f "$CSV_FILE"' EXIT
echo "$HEADER" > "$CSV_FILE"
for i in $(seq 1 "$FLIGHT_COUNT"); do
  route="${ROUTES[$((RANDOM % ${#ROUTES[@]}))]}"
  origin="${route%%:*}"
  destination="${route##*:}"
  seat_class="${SEAT_CLASSES[$((RANDOM % ${#SEAT_CLASSES[@]}))]}"
  airline="${AIRLINES[$((RANDOM % ${#AIRLINES[@]}))]}"
  aircraft_type="${AIRCRAFT_TYPES[$((RANDOM % ${#AIRCRAFT_TYPES[@]}))]}"

  # Janela de 21 dias (era 60) — mesma quantidade de voos, mais concentrados
  # nas datas que uma busca de verdade tende a testar; quase triplica a
  # densidade por rota+data sem mudar a distribuição de rotas/companhias.
  days_ahead=$((RANDOM % 21 + 1))
  base_date=$(date_days_ahead "$days_ahead")
  dep_hour=$(printf "%02d" $((RANDOM % 15 + 6))) # 06..20, never rolls past midnight below
  arr_hour=$(printf "%02d" $((10#$dep_hour + 3)))

  price=$((RANDOM % 2000 + 300))
  capacity=$((RANDOM % 150 + 50))
  flight_number="DBS$(printf '%05d' "$i")"

  echo "$flight_number,$airline,$origin,$destination,${base_date}T${dep_hour}:00:00,${base_date}T${arr_hour}:00:00,$seat_class,$price.00,$capacity,$aircraft_type" >> "$CSV_FILE"
done

# One request per 5000 lines (the endpoint's limit), all or nothing within each. The endpoint skips a flight whose number
# and departure already exist, but this script draws new random dates on every run, so each run adds new flights.
BATCH=5000
TOTAL=$(($(wc -l < "$CSV_FILE") - 1))
echo "Importing $TOTAL flights through POST /v1/admin/flights/import (dryRun=false)..."
start=2
while [ "$start" -le $((TOTAL + 1)) ]; do
  chunk="$(mktemp)"
  echo "$HEADER" > "$chunk"
  sed -n "${start},$((start + BATCH - 1))p" "$CSV_FILE" >> "$chunk"
  response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/v1/admin/flights/import?dryRun=false" \
    -H "Content-Type: text/csv" -H "Authorization: Bearer $ACCESS_TOKEN" --data-binary "@$chunk")
  rm -f "$chunk"
  echo "  [$(echo "$response" | tail -n1)] $(echo "$response" | head -n1)"
  start=$((start + BATCH))
done

echo "Done."
