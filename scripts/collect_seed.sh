#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

if [[ ! -x .venv-seed/bin/python ]]; then
  python3 -m venv .venv-seed
  .venv-seed/bin/python -m pip install -r scripts/requirements-seed.txt
fi

.venv-seed/bin/python scripts/police_seed_collector.py "$@"
