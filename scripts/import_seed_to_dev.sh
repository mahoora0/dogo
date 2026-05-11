#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: scripts/import_seed_to_dev.sh seed/dogo_seed_YYYYMMDD.sql.gz" >&2
  exit 2
fi

DUMP_FILE="$1"
DEV_DB_NAME="${DEV_DB_NAME:-dogo}"
DEV_DB_USER="${DEV_DB_USER:-root}"
DEV_DB_PASSWORD="${DEV_DB_PASSWORD:-${DB_PASSWORD:-}}"
DEV_DB_HOST="${DEV_DB_HOST:-127.0.0.1}"
DEV_DB_PORT="${DEV_DB_PORT:-3306}"

if [[ ! -f "$DUMP_FILE" ]]; then
  echo "Dump file not found: $DUMP_FILE" >&2
  exit 2
fi

MYSQL_PWD="$DEV_DB_PASSWORD" mysql \
  --host="$DEV_DB_HOST" \
  --port="$DEV_DB_PORT" \
  --user="$DEV_DB_USER" \
  --default-character-set=utf8mb4 \
  -e "CREATE DATABASE IF NOT EXISTS \`${DEV_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if [[ "$DUMP_FILE" == *.gz ]]; then
  gunzip -c "$DUMP_FILE" | MYSQL_PWD="$DEV_DB_PASSWORD" mysql \
    --host="$DEV_DB_HOST" \
    --port="$DEV_DB_PORT" \
    --user="$DEV_DB_USER" \
    --default-character-set=utf8mb4 \
    "$DEV_DB_NAME"
else
  MYSQL_PWD="$DEV_DB_PASSWORD" mysql \
    --host="$DEV_DB_HOST" \
    --port="$DEV_DB_PORT" \
    --user="$DEV_DB_USER" \
    --default-character-set=utf8mb4 \
    "$DEV_DB_NAME" < "$DUMP_FILE"
fi

echo "Imported $DUMP_FILE into $DEV_DB_NAME"
