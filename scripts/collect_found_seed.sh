#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

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

.venv-seed/bin/python scripts/police_found_seed_collector.py "$@"
