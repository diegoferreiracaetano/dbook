#!/bin/bash
# Popula o banco local (docker-compose) com hotéis e tipos de quarto de demonstração, 2 por aeroporto.
# Só desenvolvimento local: escreve direto no banco. Veja scripts/seed-hotels.sql.
#
# Uso: ./scripts/seed-hotels.sh
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U dbook -d dbook \
  < scripts/seed-hotels.sql
echo "Hotéis populados."
