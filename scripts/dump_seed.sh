#!/usr/bin/env bash
set -euo pipefail

SEED_DB_NAME="${SEED_DB_NAME:-dogo_seed}"
SEED_DB_USER="${SEED_DB_USER:-root}"
SEED_DB_PASSWORD="${SEED_DB_PASSWORD:-${DB_PASSWORD:-}}"
SEED_DB_HOST="${SEED_DB_HOST:-127.0.0.1}"
SEED_DB_PORT="${SEED_DB_PORT:-3306}"
OUT_DIR="${SEED_DUMP_DIR:-seed}"
OUT_FILE="${1:-${OUT_DIR}/dogo_seed_$(date +%Y%m%d).sql.gz}"

mkdir -p "$(dirname "$OUT_FILE")"

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

echo "Wrote $OUT_FILE"
