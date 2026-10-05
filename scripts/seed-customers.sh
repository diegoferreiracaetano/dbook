#!/bin/bash
# Loads a CRM-sized dataset (default 50 000 customers, with bookings, payments and reviews) into the local database
# started by docker-compose. Local dev only: it writes straight to the database. See scripts/seed-customers.sql.
#
# Usage: ./scripts/seed-customers.sh [customer_count]
set -euo pipefail

CUSTOMERS="${1:-50000}"
cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -v customers="$CUSTOMERS" -U dbook -d dbook \
  < scripts/seed-customers.sql
echo "Seeded $CUSTOMERS customers."
