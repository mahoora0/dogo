#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

DAYS="${RECENT_SEED_DAYS:-15}"
OUT_FILE="${RECENT_SEED_DUMP:-seed/dogo_seed_recent15.sql.gz}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --days)
      DAYS="$2"
      shift 2
      ;;
    --output)
      OUT_FILE="$2"
      shift 2
      ;;
    -h|--help)
      cat <<'EOF'
Usage: scripts/import_recent15_seed_to_dev.sh [--days 15] [--output seed/dogo_seed_recent15.sql.gz]

Builds a recent-window dump from the existing seed DB, then resets and imports
the local development DB. This script does not call the Police OpenAPI.

Environment:
  SEED_DB_NAME       source DB, default dogo_seed
  DEV_DB_NAME        target DB, default dogo
  DB_PASSWORD        fallback password for both DBs
  SEED_DB_PASSWORD   source DB password override
  DEV_DB_PASSWORD    target DB password override
  RECENT_SEED_DAYS   default window size, default 15
  RECENT_SEED_DUMP   default output path
EOF
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

export UV_CACHE_DIR="${UV_CACHE_DIR:-.tmp/uv-cache}"
mkdir -p "$UV_CACHE_DIR"

if command -v uv >/dev/null 2>&1; then
  if [[ ! -x .venv-seed/bin/python ]]; then
    uv venv .venv-seed
  fi
  uv pip install --python .venv-seed/bin/python -r scripts/requirements-seed.txt
elif [[ ! -x .venv-seed/bin/python ]]; then
  python3 -m venv .venv-seed
  .venv-seed/bin/python -m pip install -r scripts/requirements-seed.txt
fi

.venv-seed/bin/python scripts/build_recent_seed_dump.py --days "$DAYS" --output "$OUT_FILE"
.venv-seed/bin/python scripts/import_seed_to_dev.py "$OUT_FILE" --reset-db
