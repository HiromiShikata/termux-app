#!/usr/bin/env bash
set -uo pipefail
RESPONSE=$(cat)
echo "$RESPONSE"
if ! echo "$RESPONSE" | jq -e '.errors' >/dev/null 2>&1; then
  echo "Auto merge enabled successfully"
  exit 0
fi
ERROR_MSG=$(echo "$RESPONSE" | jq -r '.errors[0].message // ""')
ERROR_TYPE=$(echo "$RESPONSE" | jq -r '.errors[0].type // ""')
if [ "$ERROR_TYPE" = "RATE_LIMIT" ] || echo "$ERROR_MSG" | grep -qi "unstable\|already.*auto.merge\|rate.limit\|rate_limit"; then
  echo "Warning: could not enable auto merge (PR status: $ERROR_MSG). PR remains mergeable manually."
  exit 0
fi
echo "Failed to enable auto merge: $ERROR_MSG"
exit 1
