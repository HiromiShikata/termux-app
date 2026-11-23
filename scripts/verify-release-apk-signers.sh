#!/usr/bin/env bash
# Asserts the signers of a release APK: the v2 signer must be the old debug certificate and the
# v3 signer must be the rotated release certificate. Exits 0 only if the APK also verifies.
#
# Usage: scripts/verify-release-apk-signers.sh <apk>
# Optional environment: APKSIGNER (default: newest apksigner under $ANDROID_HOME or $ANDROID_SDK_ROOT)
set -euo pipefail

OLD_CERT_SHA256=b6da01480eefd5fbf2cd3771b8d1021ec791304bdd6c4bf41d3faabad48ee5e1
NEW_CERT_SHA256=719b2c246051e72b82eb0849e16a510f3c389e9b5ec0081308dd536d175f5bf0

die() {
  echo "ERROR: $*" >&2
  exit 1
}

if [ "$#" -ne 1 ]; then
  die "usage: $0 <apk>"
fi
APK="$1"
if [ ! -f "$APK" ]; then
  die "APK not found: $APK"
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

# The default verification reports the v3 signer; --max-sdk-version 27 keeps verification on v2.
V3_OUT="$("$APKSIGNER" verify --print-certs -v "$APK" 2>&1)" \
  || die "apksigner could not verify $APK (default scheme): $V3_OUT"
V2_OUT="$("$APKSIGNER" verify --print-certs -v --max-sdk-version 27 "$APK" 2>&1)" \
  || die "apksigner could not verify $APK (v2 scheme): $V2_OUT"

V3_SHA="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' <<<"$V3_OUT" | sed -n '1p')"
V2_SHA="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' <<<"$V2_OUT" | sed -n '1p')"

if [ "$V2_SHA" != "$OLD_CERT_SHA256" ]; then
  die "$APK: v2 signer SHA-256 is '${V2_SHA:-<none>}', expected $OLD_CERT_SHA256"
fi
if [ "$V3_SHA" != "$NEW_CERT_SHA256" ]; then
  die "$APK: v3 signer SHA-256 is '${V3_SHA:-<none>}', expected $NEW_CERT_SHA256"
fi

echo "OK: $APK verifies; v2 signer $OLD_CERT_SHA256, v3 signer $NEW_CERT_SHA256"
