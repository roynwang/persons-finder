#!/usr/bin/env bash
# Run the e2e suite against the docker compose stack.
#
# Usage:
#   e2e/run.sh                  # build + start stack, run all tests
#   e2e/run.sh e2e/tests/health.hurl   # run specific file(s)
#
#   BASE_URL=...  target another host (default http://localhost:${APP_PORT:-8080})
#   SKIP_STACK=1  assume the stack is already running
set -euo pipefail
cd "$(dirname "$0")/.."

BASE_URL=${BASE_URL:-http://localhost:${APP_PORT:-8080}}

if [[ "${SKIP_STACK:-0}" != "1" ]]; then
  docker compose up -d --build --wait
fi

echo "Waiting for app at $BASE_URL ..."
for i in {1..30}; do
  curl -fsS --max-time 2 "$BASE_URL/api/v1/health" >/dev/null 2>&1 && break
  if [[ $i == 30 ]]; then
    echo "App did not become ready at $BASE_URL" >&2
    docker compose logs app | tail -20 >&2
    exit 1
  fi
  sleep 2
done

files=("$@")
[[ ${#files[@]} == 0 ]] && files=(e2e/tests/*.hurl)

exec hurl --test \
  --variable base_url="$BASE_URL" \
  --report-html e2e/report \
  "${files[@]}"
