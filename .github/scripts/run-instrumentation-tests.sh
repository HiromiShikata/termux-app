#!/usr/bin/env bash
set -uo pipefail

mkdir -p instrumentation-screenshots

if ! timeout 300 adb wait-for-device; then
  echo "adb wait-for-device timed out after 300s"
  exit 1
fi

# Wait for the emulator to be FULLY booted before touching the input service.
# `adb wait-for-device` only waits for the device to come online, not for the
# system to finish booting. The reactivecircus/android-emulator-runner action
# already sends an unlock keyevent (`adb shell input keyevent 82`) the instant
# `sys.boot_completed` returns 1, but the `input` system service can still be
# unregistered at that moment (especially right after a snapshot restore),
# producing `SecurityException: No service published for: input` and aborting
# the run before any test executes. Re-assert full boot readiness here so the
# gradle run below never races the package manager, and re-issue the
# unlock/wake keyevent with retries so a transiently-unpublished input service
# does not leave the screen locked.
wait_for_full_boot() {
  local deadline=$((SECONDS + 300))
  while [ "$SECONDS" -lt "$deadline" ]; do
    local boot_completed bootanim pm_ready
    boot_completed=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    bootanim=$(adb shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r')
    if adb shell pm path android >/dev/null 2>&1; then
      pm_ready=1
    else
      pm_ready=0
    fi
    if [ "$boot_completed" = "1" ] && [ "$bootanim" = "stopped" ] && [ "$pm_ready" = "1" ]; then
      echo "Emulator fully booted (sys.boot_completed=1, init.svc.bootanim=stopped, package manager ready)."
      return 0
    fi
    echo "Waiting for full boot (sys.boot_completed=${boot_completed:-?}, init.svc.bootanim=${bootanim:-?}, pm_ready=${pm_ready})..."
    sleep 3
  done
  echo "Timed out after 300s waiting for the emulator to finish booting."
  return 1
}

if ! wait_for_full_boot; then
  exit 1
fi

# Re-issue the unlock/wake keyevent ourselves with retries. The input service
# is sometimes not yet published the first time, so retry until it succeeds.
unlock_screen() {
  local attempt
  for attempt in 1 2 3 4 5; do
    if adb shell input keyevent 82 >/dev/null 2>&1; then
      echo "Unlock keyevent succeeded (attempt ${attempt})."
      return 0
    fi
    echo "Unlock keyevent failed (attempt ${attempt}); the input service may not be published yet. Retrying..."
    sleep 3
  done
  echo "Warning: unlock keyevent did not succeed after retries; continuing anyway."
  return 0
}

unlock_screen

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
