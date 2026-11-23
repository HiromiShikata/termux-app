#!/usr/bin/env bash
# Tests scripts/sign-release-apk.sh and scripts/verify-release-apk-signers.sh.
# Run from the repository root:  bash scripts/sign-release-apk.test.sh
# Every key, keystore, lineage and APK is generated in a mktemp directory.
# Secret values are never printed; log lines containing them are filtered out.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SIGN_SCRIPT="$REPO_ROOT/scripts/sign-release-apk.sh"
VERIFY_SCRIPT="$REPO_ROOT/scripts/verify-release-apk-signers.sh"
COMMITTED_KEYSTORE="$REPO_ROOT/app/testkey_untrusted.jks"
BUILD_GRADLE="$REPO_ROOT/app/build.gradle"

# Expected signer certificates of the real release pipeline (used only where the test asserts them).
REAL_OLD_CERT_SHA256=b6da01480eefd5fbf2cd3771b8d1021ec791304bdd6c4bf41d3faabad48ee5e1
REAL_NEW_CERT_SHA256=719b2c246051e72b82eb0849e16a510f3c389e9b5ec0081308dd536d175f5bf0

FAILURES=0
pass() { echo "PASS: $1"; }
fail() {
  echo "FAIL: $1${2:+ ($2)}"
  FAILURES=$((FAILURES + 1))
}
check_eq() {
  if [ "$2" = "$3" ]; then pass "$1"; else fail "$1" "expected ${3}, got ${2:-<empty>}"; fi
}

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
RUNNER_TMP_DIR="$TMP/runner-temp"
mkdir -p "$RUNNER_TMP_DIR"

# ---- Toolchain: apksigner, aapt2 and android.jar from the latest installed SDK ----
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_HOME="$SDK_ROOT"
latest_entry() { ls -1d "$1"/* 2>/dev/null | sort -V | tail -n 1; }
BUILD_TOOLS_DIR="$(latest_entry "$SDK_ROOT/build-tools")"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
AAPT2="$BUILD_TOOLS_DIR/aapt2"
ANDROID_JAR="$(latest_entry "$SDK_ROOT/platforms")/android.jar"
for required in "$APKSIGNER" "$AAPT2" "$ANDROID_JAR"; do
  if [ ! -f "$required" ]; then
    echo "FAIL: Android SDK file not found: $required"
    exit 1
  fi
done
for script in "$SIGN_SCRIPT" "$VERIFY_SCRIPT"; do
  if [ ! -f "$script" ]; then
    fail "script exists: ${script#"$REPO_ROOT"/}"
  fi
done
if ! command -v keytool >/dev/null 2>&1; then
  echo "FAIL: keytool not found on PATH"
  exit 1
fi

# ---- Throwaway keys (old alias "alias", new alias "release") ----
OLD_PW="$(head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n')"
NEW_PW="$(head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n')"
keytool -genkeypair -keystore "$TMP/old.jks" -storetype PKCS12 -alias alias \
  -storepass "$OLD_PW" -keypass "$OLD_PW" -keyalg RSA -keysize 2048 -validity 3650 \
  -dname "CN=throwaway-old" >/dev/null 2>&1 || { echo "FAIL: keytool could not create the old keystore"; exit 1; }
keytool -genkeypair -keystore "$TMP/new.jks" -storetype PKCS12 -alias release \
  -storepass "$NEW_PW" -keypass "$NEW_PW" -keyalg RSA -keysize 2048 -validity 3650 \
  -dname "CN=throwaway-new" >/dev/null 2>&1 || { echo "FAIL: keytool could not create the new keystore"; exit 1; }

cert_sha256() { # $1 keystore, $2 alias, $3 password
  keytool -exportcert -keystore "$1" -storetype PKCS12 -alias "$2" -storepass "$3" | sha256sum | cut -d' ' -f1
}
OLD_SHA="$(cert_sha256 "$TMP/old.jks" alias "$OLD_PW")"
NEW_SHA="$(cert_sha256 "$TMP/new.jks" release "$NEW_PW")"

# ---- Rotation lineage old -> new ----
env ROT_OLD="$OLD_PW" ROT_NEW="$NEW_PW" "$APKSIGNER" rotate --out "$TMP/lineage.bin" \
  --old-signer --ks "$TMP/old.jks" --ks-key-alias alias --ks-pass env:ROT_OLD \
  --new-signer --ks "$TMP/new.jks" --ks-key-alias release --ks-pass env:ROT_NEW >/dev/null 2>&1 \
  || { echo "FAIL: apksigner rotate could not create the lineage"; exit 1; }

# ---- Throwaway APK: minimal manifest linked by aapt2 (minSdk 24 matches the app's signing shape) ----
cat >"$TMP/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.signingtest">
  <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>
</manifest>
EOF
"$AAPT2" link -o "$TMP/unsigned.apk" --manifest "$TMP/AndroidManifest.xml" -I "$ANDROID_JAR" >/dev/null 2>&1 \
  || { echo "FAIL: aapt2 could not link the throwaway APK"; exit 1; }

NEW_B64="$(base64 -w0 "$TMP/new.jks")"
LINEAGE_B64="$(base64 -w0 "$TMP/lineage.bin")"

# ---- Helpers ----
# run_sign <log> <output-apk> [VAR=value ...]  runs the signing script in a clean environment.
run_sign() {
  local log="$1" out="$2"
  shift 2
  local java_env=()
  if [ -n "${JAVA_HOME:-}" ]; then java_env=(JAVA_HOME="$JAVA_HOME"); fi
  (cd "$REPO_ROOT" && env -i PATH="$PATH" HOME="$HOME" "${java_env[@]}" ANDROID_HOME="$SDK_ROOT" \
    RUNNER_TEMP="$RUNNER_TMP_DIR" "$@" bash "$SIGN_SCRIPT" "$TMP/unsigned.apk" "$out") >"$log" 2>&1
}
# Signer certificate SHA-256 digest from `apksigner verify --print-certs -v`: the first line containing
# "certificate SHA-256 digest:" with any prefix (e.g. "Signer #1 certificate", "Signer (minSdkVersion=24) certificate").
# $1 APK, $2... extra apksigner verify arguments. Prints nothing when no such line exists.
apksigner_cert_sha() {
  local apk="$1"
  shift
  "$APKSIGNER" verify --print-certs -v "$@" "$apk" 2>/dev/null |
    awk '/certificate SHA-256 digest:/ { sub(/^.*digest: /, ""); sub(/[[:space:]]+$/, ""); print; exit }'
}
# Prints the full apksigner verify output of an APK to stderr, every line prefixed with "apksigner output:".
# Contains only public certificate digests and DNs.
# $1 APK, $2... extra apksigner verify arguments.
print_apksigner_output() {
  local apk="$1" out rc
  shift
  out="$("$APKSIGNER" verify --print-certs -v "$@" "$apk" 2>&1)"
  rc=$?
  echo "apksigner path: $APKSIGNER (exit $rc)" >&2
  if [ -z "$out" ]; then
    echo "apksigner output: <empty>" >&2
  else
    printf '%s\n' "$out" | sed 's/^/apksigner output: /' >&2
  fi
}
# check_signer_cert <check name> <APK> <expected digest> [extra apksigner verify args...]
# Passes when the extracted digest equals the expected one; otherwise fails and prints the apksigner output.
check_signer_cert() {
  local name="$1" apk="$2" expected="$3" got
  shift 3
  got="$(apksigner_cert_sha "$apk" "$@")"
  if [ "$got" = "$expected" ]; then
    pass "$name"
  else
    fail "$name" "expected ${expected}, got ${got:-<empty>}"
    print_apksigner_output "$apk" "$@"
  fi
}
apk_verifies() {
  local out
  out="$("$APKSIGNER" verify -v "$1" 2>&1)" && printf '%s\n' "$out" | grep -qx 'Verifies'
}
log_leaks_secret() {
  grep -F -q -e "$OLD_PW" -e "$NEW_PW" -e "$NEW_B64" -e "$LINEAGE_B64" "$1"
}
# Prints the last lines of a log with any secret-bearing line removed.
show_log_tail() {
  tail -n 5 "$1" | grep -F -v -e "$OLD_PW" -e "$NEW_PW" -e "$NEW_B64" -e "$LINEAGE_B64" | sed 's/^/    | /'
}

# ---- Case 1: signing with throwaway keys through the override variables ----
OK_OUT="$TMP/signed.apk"
OK_LOG="$TMP/sign-ok.log"
run_sign "$OK_LOG" "$OK_OUT" \
  RELEASE_KEYSTORE_BASE64="$NEW_B64" RELEASE_KEYSTORE_PASSWORD="$NEW_PW" \
  RELEASE_SIGNING_LINEAGE_BASE64="$LINEAGE_B64" \
  OLD_KEYSTORE="$TMP/old.jks" OLD_KEYSTORE_PASSWORD="$OLD_PW" APKSIGNER="$APKSIGNER"
SIGN_RC=$?
if [ "$SIGN_RC" -eq 0 ]; then
  pass "signing script exits 0 with all secrets set"
else
  fail "signing script exits 0 with all secrets set" "exit $SIGN_RC"
  show_log_tail "$OK_LOG"
fi
if [ -s "$OK_OUT" ]; then
  pass "signing script writes the output APK"
  if apk_verifies "$OK_OUT"; then pass "re-signed APK verifies"; else fail "re-signed APK verifies"; fi
  check_signer_cert "v2 signer certificate is the old throwaway certificate" "$OK_OUT" "$OLD_SHA" --max-sdk-version 27
  check_signer_cert "v3 signer certificate is the new throwaway certificate" "$OK_OUT" "$NEW_SHA"
else
  fail "signing script writes the output APK" "$OK_OUT missing or empty"
fi
if [ ! -f "$SIGN_SCRIPT" ]; then
  fail "signing script output contains no secret value" "signing script not found"
elif log_leaks_secret "$OK_LOG"; then
  fail "signing script output contains no secret value"
else
  pass "signing script output contains no secret value"
fi
if [ ! -f "$SIGN_SCRIPT" ]; then
  fail "decoded secret files are removed from RUNNER_TEMP after signing" "signing script not found"
elif [ -z "$(ls -A "$RUNNER_TMP_DIR")" ]; then
  pass "decoded secret files are removed from RUNNER_TEMP after signing"
else
  fail "decoded secret files are removed from RUNNER_TEMP after signing" "$(ls -A "$RUNNER_TMP_DIR" | tr '\n' ' ')"
fi

# ---- Case 2: each secret empty in turn must fail, name the variable and produce no APK ----
declare -A SECRET_VALUE=(
  [RELEASE_KEYSTORE_BASE64]="$NEW_B64"
  [RELEASE_KEYSTORE_PASSWORD]="$NEW_PW"
  [RELEASE_SIGNING_LINEAGE_BASE64]="$LINEAGE_B64"
)
for missing in RELEASE_KEYSTORE_BASE64 RELEASE_KEYSTORE_PASSWORD RELEASE_SIGNING_LINEAGE_BASE64; do
  args=(OLD_KEYSTORE="$TMP/old.jks" OLD_KEYSTORE_PASSWORD="$OLD_PW" APKSIGNER="$APKSIGNER")
  for name in "${!SECRET_VALUE[@]}"; do
    if [ "$name" = "$missing" ]; then args+=("$name="); else args+=("$name=${SECRET_VALUE[$name]}"); fi
  done
  out="$TMP/missing-$missing.apk"
  log="$TMP/missing-$missing.log"
  run_sign "$log" "$out" "${args[@]}"
  rc=$?
  if [ ! -f "$SIGN_SCRIPT" ]; then
    fail "exits non-zero when $missing is empty" "signing script not found"
  elif [ "$rc" -ne 0 ]; then
    pass "exits non-zero when $missing is empty"
  else
    fail "exits non-zero when $missing is empty" "exit 0"
  fi
  if grep -qF "$missing" "$log"; then
    pass "stderr names $missing when it is empty"
  else
    fail "stderr names $missing when it is empty"
    show_log_tail "$log"
  fi
  if [ -f "$SIGN_SCRIPT" ] && [ ! -e "$out" ]; then
    pass "no APK is produced when $missing is empty (no unsigned or old-only fallback)"
  else
    fail "no APK is produced when $missing is empty (no unsigned or old-only fallback)"
  fi
done

# ---- Case 3: default old keystore is the committed debug key, so its certificate is the old public test key ----
COMMITTED_PW="$(sed -n "s/^ *storePassword '\(.*\)'$/\1/p" "$BUILD_GRADLE" | head -n 1)"
if [ -n "$COMMITTED_PW" ] && [ -f "$COMMITTED_KEYSTORE" ]; then
  env CMT_PW="$COMMITTED_PW" ROT_NEW="$NEW_PW" "$APKSIGNER" rotate --out "$TMP/committed-lineage.bin" \
    --old-signer --ks "$COMMITTED_KEYSTORE" --ks-key-alias alias --ks-pass env:CMT_PW \
    --new-signer --ks "$TMP/new.jks" --ks-key-alias release --ks-pass env:ROT_NEW >/dev/null 2>&1
  if [ -s "$TMP/committed-lineage.bin" ]; then
    DEFAULT_OUT="$TMP/signed-default.apk"
    DEFAULT_LOG="$TMP/sign-default.log"
    run_sign "$DEFAULT_LOG" "$DEFAULT_OUT" \
      RELEASE_KEYSTORE_BASE64="$NEW_B64" RELEASE_KEYSTORE_PASSWORD="$NEW_PW" \
      RELEASE_SIGNING_LINEAGE_BASE64="$(base64 -w0 "$TMP/committed-lineage.bin")"
    DEFAULT_RC=$?
    if [ "$DEFAULT_RC" -eq 0 ] && [ -s "$DEFAULT_OUT" ]; then
      pass "signing with the default old keystore succeeds"
      check_signer_cert "default old keystore: v2 signer is the committed public test certificate" \
        "$DEFAULT_OUT" "$REAL_OLD_CERT_SHA256" --max-sdk-version 27
      check_signer_cert "default old keystore: v3 signer is the new certificate" "$DEFAULT_OUT" "$NEW_SHA"
    else
      fail "signing with the default old keystore succeeds" "exit $DEFAULT_RC"
      show_log_tail "$DEFAULT_LOG"
    fi
  else
    fail "could not create a lineage from the committed debug keystore for the default-path case"
  fi
else
  fail "committed debug keystore or storePassword not found in app/build.gradle" "$COMMITTED_KEYSTORE"
fi

# ---- Case 4: verify script ----
# 4a. The real constants must reject a throwaway-signed APK.
if [ -s "$OK_OUT" ]; then
  VERIFY_OUT="$(env -i PATH="$PATH" HOME="$HOME" ANDROID_HOME="$SDK_ROOT" bash "$VERIFY_SCRIPT" "$OK_OUT" 2>&1)"
  VERIFY_RC=$?
  if [ "$VERIFY_RC" -ne 0 ] && [ -n "$VERIFY_OUT" ]; then
    pass "verify script exits non-zero with a message on a throwaway-signed APK (real constants)"
  else
    fail "verify script exits non-zero with a message on a throwaway-signed APK (real constants)" "exit $VERIFY_RC"
  fi
else
  fail "verify script exits non-zero on a throwaway-signed APK" "no signed APK from the signing case"
fi

# 4b. A copy of the verify script whose two expected digests are replaced by the throwaway digests
#     must accept the correctly signed APK and reject an old-only APK.
THROWAWAY_VERIFY="$TMP/verify-throwaway.sh"
sed -e "s/$REAL_OLD_CERT_SHA256/$OLD_SHA/g" -e "s/$REAL_NEW_CERT_SHA256/$NEW_SHA/g" "$VERIFY_SCRIPT" >"$THROWAWAY_VERIFY"
if grep -qF "$OLD_SHA" "$THROWAWAY_VERIFY" && grep -qF "$NEW_SHA" "$THROWAWAY_VERIFY"; then
  if [ -s "$OK_OUT" ]; then
    VERIFY_OK_OUT="$(env -i PATH="$PATH" HOME="$HOME" ANDROID_HOME="$SDK_ROOT" bash "$THROWAWAY_VERIFY" "$OK_OUT" 2>&1)"
    VERIFY_OK_RC=$?
    if [ "$VERIFY_OK_RC" -eq 0 ]; then
      pass "verify script (throwaway constants) exits 0 on the correctly signed APK"
    else
      fail "verify script (throwaway constants) exits 0 on the correctly signed APK" "exit $VERIFY_OK_RC: $(printf '%s' "$VERIFY_OK_OUT" | tail -n 1)"
      print_apksigner_output "$OK_OUT"
    fi
  else
    fail "verify script (throwaway constants) exits 0 on the correctly signed APK" "no signed APK from the signing case"
  fi
  OLD_ONLY_OUT="$TMP/old-only.apk"
  env ONLY_OLD="$OLD_PW" "$APKSIGNER" sign --ks "$TMP/old.jks" --ks-key-alias alias --ks-pass env:ONLY_OLD \
    --out "$OLD_ONLY_OUT" "$TMP/unsigned.apk" >/dev/null 2>&1
  if [ -s "$OLD_ONLY_OUT" ]; then
    if env -i PATH="$PATH" HOME="$HOME" ANDROID_HOME="$SDK_ROOT" bash "$THROWAWAY_VERIFY" "$OLD_ONLY_OUT" >/dev/null 2>&1; then
      fail "verify script (throwaway constants) rejects an old-only APK" "exit 0"
    else
      pass "verify script (throwaway constants) rejects an old-only APK"
    fi
  else
    fail "could not create the old-only APK for the verify negative case"
  fi
else
  fail "verify script hard-codes the expected certificate digests" "constants not found in $VERIFY_SCRIPT"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "ALL TESTS PASSED"
  exit 0
fi
echo "$FAILURES CHECK(S) FAILED"
exit 1
