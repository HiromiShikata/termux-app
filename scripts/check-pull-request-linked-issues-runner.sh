#!/usr/bin/env bash
set -euo pipefail
SCRIPT_RELATIVE_PATH=src/adapter/entry-points/cli/check-pull-request-linked-issues.ts
PHASE="${1:-all}"
CHECK_DIR_FILE="${RUNNER_TEMP:-/tmp}/check-pull-request-linked-issues-dir"

fetch_check_script() {
  if [ -f "scripts/typescript/$SCRIPT_RELATIVE_PATH" ]; then
    CHECK_SCRIPT_DIR="$PWD/scripts/typescript"
  else
    CLONE_DIR="${RUNNER_TEMP:-/tmp}/repositories-management-linked-issues-check"
    rm -rf "$CLONE_DIR"
    gh repo clone HiromiShikata/repositories-management "$CLONE_DIR" -- --depth 1 --quiet
    CHECK_SCRIPT_DIR="$CLONE_DIR/scripts/typescript"
  fi
  printf '%s\n' "$CHECK_SCRIPT_DIR" > "$CHECK_DIR_FILE"
}

install_dependencies() {
  cd "$(cat "$CHECK_DIR_FILE")"
  npm ci --omit=dev --ignore-scripts
}

run_check() {
  cd "$(cat "$CHECK_DIR_FILE")"
  npx tsx "$SCRIPT_RELATIVE_PATH"
}

case "$PHASE" in
  fetch) fetch_check_script ;;
  install) install_dependencies ;;
  run) run_check ;;
  all)
    fetch_check_script
    install_dependencies
    run_check
    ;;
  *)
    echo "unknown phase: $PHASE (expected fetch, install, run or no argument)" >&2
    exit 2
    ;;
esac
