#!/usr/bin/env bash
set -euo pipefail

if [ -z "${REPOSITORY:-}" ]; then
  echo "REPOSITORY environment variable is required" >&2
  exit 1
fi
if [ -z "${ISSUE_TITLE:-}" ]; then
  echo "ISSUE_TITLE environment variable is required" >&2
  exit 1
fi
if [ -z "${CHECK_RESULTS_URL:-}" ]; then
  echo "CHECK_RESULTS_URL environment variable is required" >&2
  exit 1
fi

BODY="CI failed on the default branch. [View failed checks](${CHECK_RESULTS_URL})"

EXISTING_ISSUE_NUMBER=$(gh issue list \
  --repo "$REPOSITORY" \
  --state open \
  --search "$ISSUE_TITLE in:title" \
  --json number,title \
  | jq -r --arg title "$ISSUE_TITLE" '.[] | select(.title == $title) | .number' \
  | head -n1)

if [ -n "$EXISTING_ISSUE_NUMBER" ]; then
  gh issue comment "$EXISTING_ISSUE_NUMBER" \
    --repo "$REPOSITORY" \
    --body "$BODY"
else
  gh issue create \
    --repo "$REPOSITORY" \
    --title "$ISSUE_TITLE" \
    --body "$BODY"
fi
