#!/usr/bin/env bash
# Re-signs a debug-keystore APK with the rotated release signing key, keeping the old key as
# the v2 signer so that the in-app updater still verifies the APK against the old certificate.
#
# Usage: scripts/sign-release-apk.sh <input-apk> <output-apk>
#
# Required environment (secret values are never printed):
#   RELEASE_KEYSTORE_BASE64          PKCS12 release keystore, alias "release", base64
#   RELEASE_KEYSTORE_PASSWORD        store and key password of the release keystore
#   RELEASE_SIGNING_LINEAGE_BASE64   apksigner rotation lineage (old -> new), base64
# Optional environment:
#   OLD_KEYSTORE           default: app/testkey_untrusted.jks (alias "alias")
#   OLD_KEYSTORE_PASSWORD  default: the storePassword of the debug signingConfig in app/build.gradle
#   APKSIGNER              default: the newest apksigner under $ANDROID_HOME or $ANDROID_SDK_ROOT
#
# The output APK is written only when signing succeeds. Decoded secret files live in a
# mktemp directory under $RUNNER_TEMP that is removed on exit.
set -euo pipefail

die() {
  echo "ERROR: $*" >&2
  exit 1
}

if [ "$#" -ne 2 ]; then
  die "usage: $0 <input-apk> <output-apk>"
fi
INPUT_APK="$1"
OUTPUT_APK="$2"

for name in RELEASE_KEYSTORE_BASE64 RELEASE_KEYSTORE_PASSWORD RELEASE_SIGNING_LINEAGE_BASE64; do
  if [ -z "${!name:-}" ]; then
    die "$name is empty; the release signing secret is required, so no APK is signed or published"
  fi
done
if [ ! -f "$INPUT_APK" ]; then
  die "input APK not found: $INPUT_APK"
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD_KEYSTORE="${OLD_KEYSTORE:-$REPO_ROOT/app/testkey_untrusted.jks}"
if [ -z "${OLD_KEYSTORE_PASSWORD:-}" ]; then
  OLD_KEYSTORE_PASSWORD="$(sed -n "s/^ *storePassword '\(.*\)'\$/\1/p" "$REPO_ROOT/app/build.gradle" | sed -n '1p')"
fi
if [ -z "$OLD_KEYSTORE_PASSWORD" ]; then
  die "could not read the debug storePassword from app/build.gradle"
fi
if [ ! -f "$OLD_KEYSTORE" ]; then
  die "old keystore not found: $OLD_KEYSTORE"
fi

if [ -z "${APKSIGNER:-}" ]; then
  SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -z "$SDK_ROOT" ]; then
    die "set ANDROID_HOME or ANDROID_SDK_ROOT, or set APKSIGNER"
  fi
  APKSIGNER="$(find "$SDK_ROOT/build-tools" -mindepth 2 -maxdepth 2 -name apksigner -type f 2>/dev/null | sort -V | sed -n '$p')"
  if [ -z "$APKSIGNER" ]; then
    die "no apksigner found under $SDK_ROOT/build-tools"
  fi
fi
if [ ! -x "$APKSIGNER" ]; then
  die "apksigner is not executable: $APKSIGNER"
fi

umask 077
WORK_DIR="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/sign-release-apk.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

# decode_secret <variable name> <destination file>: writes the decoded value without echoing it.
decode_secret() {
  printf '%s' "${!1}" | base64 -d >"$2" || die "$1 is not valid base64"
  chmod 600 "$2"
}
decode_secret RELEASE_KEYSTORE_BASE64 "$WORK_DIR/release.jks"
decode_secret RELEASE_SIGNING_LINEAGE_BASE64 "$WORK_DIR/lineage.bin"

cp "$INPUT_APK" "$WORK_DIR/signed.apk"
# apksigner reads the passwords from the OLD and NEW variables, which exist only for this command.
if ! OLD="$OLD_KEYSTORE_PASSWORD" NEW="$RELEASE_KEYSTORE_PASSWORD" "$APKSIGNER" sign \
  --ks "$OLD_KEYSTORE" --ks-key-alias alias --ks-pass env:OLD \
  --next-signer --ks "$WORK_DIR/release.jks" --ks-key-alias release --ks-pass env:NEW \
  --lineage "$WORK_DIR/lineage.bin" --rotation-min-sdk-version 28 \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  "$WORK_DIR/signed.apk"; then
  die "apksigner failed to sign $INPUT_APK"
fi

mv -f "$WORK_DIR/signed.apk" "$OUTPUT_APK"
echo "Signed $INPUT_APK -> $OUTPUT_APK"
