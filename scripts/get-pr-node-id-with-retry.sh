#!/usr/bin/env bash
set -euo pipefail

if [ -z "${REPOSITORY:-}" ]; then
  echo "REPOSITORY environment variable is required" >&2
  exit 1
fi
if [ -z "${PR_NUMBER:-}" ]; then
  echo "PR_NUMBER environment variable is required" >&2
  exit 1
fi
if [ -z "${GH_TOKEN:-}" ]; then
  echo "GH_TOKEN environment variable is required" >&2
  exit 1
fi

CURL_COMMAND="${CURL_COMMAND:-curl}"
MAX_ATTEMPTS=3
ATTEMPT=0

while [ "$ATTEMPT" -lt "$MAX_ATTEMPTS" ]; do
  ATTEMPT=$((ATTEMPT + 1))
  set +e
  RAW_RESPONSE=$("$CURL_COMMAND" -s -w '\n%{http_code}' -H "Authorization: token ${GH_TOKEN}" -H "Accept: application/vnd.github.v3+json" "https://api.github.com/repos/${REPOSITORY}/pulls/${PR_NUMBER}")
  CURL_EXIT_CODE=$?
  set -e

  if [ "$CURL_EXIT_CODE" -ne 0 ]; then
    continue
  fi

  HTTP_STATUS=$(printf '%s' "$RAW_RESPONSE" | tail -n 1)
  RESPONSE_BODY=$(printf '%s' "$RAW_RESPONSE" | sed '$d')

  if [ "$HTTP_STATUS" != "200" ]; then
    continue
  fi

  set +e
  NODE_ID=$(printf '%s' "$RESPONSE_BODY" | jq -r '.node_id' 2>/dev/null)
  JQ_EXIT_CODE=$?
  set -e

  if [ "$JQ_EXIT_CODE" -ne 0 ] || [ -z "$NODE_ID" ] || [ "$NODE_ID" = "null" ]; then
    continue
  fi

  echo "node_id=${NODE_ID}" >> "$GITHUB_OUTPUT"
  exit 0
done

echo "Failed to retrieve PR node_id after ${ATTEMPT} attempts" >&2
exit 1
