#!/usr/bin/env bash
set -euo pipefail
SCRIPT_RELATIVE_PATH=src/adapter/entry-points/cli/check-pull-request-linked-issues.ts

if [ -f "scripts/typescript/$SCRIPT_RELATIVE_PATH" ]; then
  CHECK_SCRIPT_DIR=scripts/typescript
else
  CLONE_DIR="${RUNNER_TEMP:-/tmp}/repositories-management-linked-issues-check"
  rm -rf "$CLONE_DIR"
  gh repo clone HiromiShikata/repositories-management "$CLONE_DIR" -- --depth 1 --quiet
  CHECK_SCRIPT_DIR="$CLONE_DIR/scripts/typescript"
fi

cd "$CHECK_SCRIPT_DIR"
npm ci
npx tsx "$SCRIPT_RELATIVE_PATH"
