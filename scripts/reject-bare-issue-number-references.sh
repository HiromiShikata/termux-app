#!/usr/bin/env bash
set -euo pipefail

COMMIT_SUBJECTS_FILE="$1"
BARE_ISSUE_NUMBER_PATTERN='#[0-9]+'
VIOLATION_FOUND=0

if [ -n "${PR_TITLE:-}" ] && echo "$PR_TITLE" | grep -Eq "$BARE_ISSUE_NUMBER_PATTERN"; then
  echo "Bare issue-number shorthand in pull request title: $PR_TITLE" >&2
  VIOLATION_FOUND=1
fi

while IFS= read -r COMMIT_SUBJECT || [ -n "$COMMIT_SUBJECT" ]; do
  if [ -z "$COMMIT_SUBJECT" ]; then
    continue
  fi
  if echo "$COMMIT_SUBJECT" | grep -Eq "$BARE_ISSUE_NUMBER_PATTERN"; then
    echo "Bare issue-number shorthand in commit subject: $COMMIT_SUBJECT" >&2
    VIOLATION_FOUND=1
  fi
done <"$COMMIT_SUBJECTS_FILE"

if [ "$VIOLATION_FOUND" -eq 1 ]; then
  echo "Use the complete GitHub issue/pull request URL" >&2
  exit 1
fi

exit 0
