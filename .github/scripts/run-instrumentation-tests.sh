#!/usr/bin/env bash
set -uo pipefail

mkdir -p instrumentation-screenshots

if ! timeout 300 adb wait-for-device; then
  echo "adb wait-for-device timed out after 300s"
  exit 1
fi

read -r -a test_args <<< "${GRADLE_TEST_ARGS:-}"

status=0
timeout 1500 ./gradlew :app:connectedDebugAndroidTest "${test_args[@]}" \
  -Pandroid.testInstrumentationRunnerArguments.numShards="${SHARD_COUNT}" \
  -Pandroid.testInstrumentationRunnerArguments.shardIndex="${SHARD_INDEX}" || status=$?

adb pull /sdcard/termux-instrumentation-screenshots instrumentation-screenshots || true
adb pull /sdcard/session-info-area-scan-render.png instrumentation-screenshots/ || true
adb pull /sdcard/session-info-area-scene-render.png instrumentation-screenshots/ || true
ls -la instrumentation-screenshots || true

exit "$status"
