#!/bin/bash
# Populates the local database with realistic flight data for demos/manual testing —
# reuses the same REST endpoints already covered by the test suite (no direct SQL
# inserts for flights), so it can never drift from what the API actually accepts.
#
# Local dev only: promotes the seed user to ADMIN via a direct SQL UPDATE, since there's
# no self-promotion endpoint (closed security decision — see CHECKLIST.md M3). Never run
# this against a real environment.
#
# Usage: ./scripts/seed-flights.sh [flight_count]
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
FLIGHT_COUNT="${1:-30}"
ADMIN_EMAIL="seed-admin@example.com"
ADMIN_PASSWORD="seed-password-123"

ROUTES=("GRU:GIG" "GIG:GRU" "GRU:JFK" "JFK:GRU" "GIG:JFK" "JFK:GIG")
SEAT_CLASSES=("ECONOMY" "BUSINESS")

echo "Registering seed admin user..."
curl -s -o /dev/null -X POST "$BASE_URL/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" || true

echo "Promoting to ADMIN (direct SQL — local dev only)..."
docker compose exec -T postgres psql -U dbook -d dbook -c \
  "UPDATE app_user SET role = 'ADMIN' WHERE email = '$ADMIN_EMAIL';" > /dev/null

ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" \
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

echo "Creating $FLIGHT_COUNT flights..."
for i in $(seq 1 "$FLIGHT_COUNT"); do
  route="${ROUTES[$((RANDOM % ${#ROUTES[@]}))]}"
  origin="${route%%:*}"
  destination="${route##*:}"
  seat_class="${SEAT_CLASSES[$((RANDOM % ${#SEAT_CLASSES[@]}))]}"

  days_ahead=$((RANDOM % 60 + 1))
  base_date=$(date_days_ahead "$days_ahead")
  dep_hour=$(printf "%02d" $((RANDOM % 15 + 6))) # 06..20, never rolls past midnight below
  arr_hour=$(printf "%02d" $((10#$dep_hour + 3)))

  price=$((RANDOM % 2000 + 300))
  capacity=$((RANDOM % 150 + 50))
  flight_number="DBS$(printf '%05d' "$i")"

  http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/admin/flights" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{
      \"flightNumber\": \"$flight_number\",
      \"originIataCode\": \"$origin\",
      \"destinationIataCode\": \"$destination\",
      \"departureTime\": \"${base_date}T${dep_hour}:00:00\",
      \"arrivalTime\": \"${base_date}T${arr_hour}:00:00\",
      \"seatClass\": \"$seat_class\",
      \"price\": $price.00,
      \"totalCapacity\": $capacity
    }")

  echo "  [$http_code] $flight_number $origin->$destination on $base_date"
done

echo "Done."
