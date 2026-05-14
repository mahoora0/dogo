#!/usr/bin/env bash
set -euo pipefail

SEED_DB_NAME="${SEED_DB_NAME:-dogo_seed}"
SEED_DB_USER="${SEED_DB_USER:-root}"
SEED_DB_PASSWORD="${SEED_DB_PASSWORD:-${DB_PASSWORD:-}}"
SEED_DB_HOST="${SEED_DB_HOST:-127.0.0.1}"
SEED_DB_PORT="${SEED_DB_PORT:-3306}"
SEED_DB_DOCKER_CONTAINER="${SEED_DB_DOCKER_CONTAINER:-dogo-mysql}"
SEED_DB_USE_DOCKER="${SEED_DB_USE_DOCKER:-auto}"
OUT_DIR="${SEED_DUMP_DIR:-seed}"
OUT_FILE="${1:-${OUT_DIR}/dogo_seed_$(date +%Y%m%d).sql.gz}"

mkdir -p "$(dirname "$OUT_FILE")"

use_docker=false
if [[ "$SEED_DB_USE_DOCKER" == "true" ]]; then
  use_docker=true
elif [[ "$SEED_DB_USE_DOCKER" == "auto" ]] && docker ps --format '{{.Names}}' | grep -qx "$SEED_DB_DOCKER_CONTAINER"; then
  use_docker=true
fi

if [[ "$use_docker" == "true" ]]; then
  docker exec "$SEED_DB_DOCKER_CONTAINER" sh -c \
    "mysqldump -u'$SEED_DB_USER' -p\"\${MYSQL_ROOT_PASSWORD:-$SEED_DB_PASSWORD}\" --default-character-set=utf8mb4 --single-transaction --skip-add-drop-table --no-create-db '$SEED_DB_NAME'" \
    | gzip -c > "$OUT_FILE"
else
  MYSQL_PWD="$SEED_DB_PASSWORD" mysqldump \
    --host="$SEED_DB_HOST" \
    --port="$SEED_DB_PORT" \
    --user="$SEED_DB_USER" \
    --default-character-set=utf8mb4 \
    --single-transaction \
    --skip-add-drop-table \
    --no-create-db \
    "$SEED_DB_NAME" \
    | gzip -c > "$OUT_FILE"
fi

echo "Wrote $OUT_FILE"
