#!/usr/bin/env bash
set -euo pipefail

if [ -z "${SERVER_URL:-}" ]; then
  echo "SERVER_URL environment variable is required" >&2
  exit 1
fi
if [ -z "${REPOSITORY:-}" ]; then
  echo "REPOSITORY environment variable is required" >&2
  exit 1
fi
if [ -z "${HEAD_SHA:-}" ]; then
  echo "HEAD_SHA environment variable is required" >&2
  exit 1
fi
if [ -z "${WORKFLOW_NAME:-}" ]; then
  echo "WORKFLOW_NAME environment variable is required" >&2
  exit 1
fi

ISSUE_TITLE="[CI] Default branch: ${WORKFLOW_NAME} CI failure"
CHECK_RESULTS_URL="${SERVER_URL}/${REPOSITORY}/commit/${HEAD_SHA}/checks"

{
  echo "issue_title=${ISSUE_TITLE}"
  echo "check_results_url=${CHECK_RESULTS_URL}"
} >> "$GITHUB_OUTPUT"
