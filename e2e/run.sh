#!/usr/bin/env bash
# Run the e2e suite against the docker compose stack.
#
# Usage:
#   e2e/run.sh                  # build + start stack, run all tests
#   e2e/run.sh e2e/tests/health.hurl   # run specific file(s)
#
#   BASE_URL=...  target another host (default http://localhost:${APP_PORT:-8080})
#   SKIP_STACK=1  assume the stack is already running
#   E2E_LLM=1     also run e2e/tests-llm (real Gemini; needs GEMINI_API_KEY in .env)
set -euo pipefail
cd "$(dirname "$0")/.."

BASE_URL=${BASE_URL:-http://localhost:${APP_PORT:-8080}}

if [[ "${E2E_LLM:-0}" == "1" ]] && ! grep -qE '^GEMINI_API_KEY=.+' .env 2>/dev/null; then
  echo "E2E_LLM=1 requires GEMINI_API_KEY to be set in .env" >&2
  exit 1
fi

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
if [[ ${#files[@]} == 0 ]]; then
  files=(e2e/tests/*.hurl)
  # The real-LLM scenarios live outside e2e/tests so the default glob (and CI)
  # never picks them up.
  [[ "${E2E_LLM:-0}" == "1" ]] && files+=(e2e/tests-llm/*.hurl)
fi

exec hurl --test \
  --variable base_url="$BASE_URL" \
  --report-html e2e/report \
  "${files[@]}"
