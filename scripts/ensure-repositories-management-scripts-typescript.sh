#!/usr/bin/env bash
set -euo pipefail
ADD_PROJECT_BOARD_ITEM_RELATIVE_PATH=scripts/typescript/src/adapter/entry-points/cli/add-project-board-item.ts

if [ -f "$ADD_PROJECT_BOARD_ITEM_RELATIVE_PATH" ]; then
  exit 0
fi

CLONE_DIR="${RUNNER_TEMP:-/tmp}/repositories-management-scripts-typescript-fallback"
rm -rf "$CLONE_DIR"
gh repo clone HiromiShikata/repositories-management "$CLONE_DIR" -- --depth 1 --quiet

mkdir -p scripts
rm -rf scripts/typescript
cp -R "$CLONE_DIR/scripts/typescript" scripts/typescript
