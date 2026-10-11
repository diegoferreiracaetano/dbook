#!/bin/bash
# Preenche o logo das companhias do banco local (docker-compose) que ainda não têm.
# Só desenvolvimento local: escreve direto no banco. Veja scripts/seed-airline-logos.sql.
#
# Uso: ./scripts/seed-airline-logos.sh
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U dbook -d dbook \
  < scripts/seed-airline-logos.sql
echo "Logos das companhias preenchidos."
