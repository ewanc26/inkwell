#!/usr/bin/env bash
# check-release-permissions.sh
#
# Asserts that the assembled release APK contains only the expected permission
# set. Run after assembleRelease in CI. Fails with a non-zero exit code and a
# diff when any unexpected permission appears or an expected one goes missing.
#
# Usage:
#   bash tools/check-release-permissions.sh [path/to/app.apk]
#
# If no path is given the script looks for the standard Gradle output location,
# falling back to the unsigned name AGP uses when no signing config is present
# (e.g. in CI, which has no keystore.properties secret — see Android/AGENTS.md).
# Permission declarations are identical either way; signing status is
# irrelevant to what this script checks.

set -euo pipefail

RELEASE_DIR="app/build/outputs/apk/release"
APK="${1:-}"

if [[ -z "$APK" ]]; then
  if [[ -f "$RELEASE_DIR/app-release.apk" ]]; then
    APK="$RELEASE_DIR/app-release.apk"
  else
    APK="$RELEASE_DIR/app-release-unsigned.apk"
  fi
fi

if [[ ! -f "$APK" ]]; then
  echo "::error::APK not found at $APK — run assembleRelease first"
  exit 1
fi

# Locate aapt2 from the Android SDK. Prefer ANDROID_HOME / ANDROID_SDK_ROOT.
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_ROOT" ]]; then
  # Fallback: find it in PATH
  AAPT2=$(command -v aapt2 2>/dev/null || true)
else
  AAPT2=$(find "$SDK_ROOT/build-tools" -name "aapt2" 2>/dev/null | sort -rV | head -1)
fi

if [[ -z "${AAPT2:-}" || ! -x "$AAPT2" ]]; then
  echo "::error::aapt2 not found. Set ANDROID_HOME or add aapt2 to PATH."
  exit 1
fi

# --------------------------------------------------------------------------
# Expected permission allowlist
# --------------------------------------------------------------------------
# android.permission.INTERNET                            — network access
# android.permission.ACCESS_NETWORK_STATE                — connectivity check
# android.permission.POST_NOTIFICATIONS                  — local notifications (API 33+)
# android.permission.WAKE_LOCK                           — WorkManager keeps CPU alive briefly
# android.permission.RECEIVE_BOOT_COMPLETED              — WorkManager restarts after reboot
# android.permission.FOREGROUND_SERVICE                  — WorkManager foreground task
# uk.ewancroft.inkwell.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION — internal broadcast guard
# --------------------------------------------------------------------------
ALLOWLIST=(
  "android.permission.INTERNET"
  "android.permission.ACCESS_NETWORK_STATE"
  "android.permission.POST_NOTIFICATIONS"
  "android.permission.WAKE_LOCK"
  "android.permission.RECEIVE_BOOT_COMPLETED"
  "android.permission.FOREGROUND_SERVICE"
  # Self-declared internal broadcast guard — declared and consumed by this app only
  "uk.ewancroft.inkwell.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
)

echo "Checking permissions in: $APK"
echo "Using aapt2: $AAPT2"
echo ""

# Extract uses-permission lines only (the self-declared `permission:` line is
# an internal broadcast guard, not a capability granted by the OS — skip it).
ACTUAL=$("$AAPT2" dump permissions "$APK" 2>&1 \
  | grep -E "^uses-permission:" \
  | sed "s/uses-permission: name='//;s/'$//" \
  | sort -u)

EXPECTED=$(printf '%s\n' "${ALLOWLIST[@]}" | sort -u)

MISSING=$(comm -23 <(echo "$EXPECTED") <(echo "$ACTUAL") || true)
UNEXPECTED=$(comm -13 <(echo "$EXPECTED") <(echo "$ACTUAL") || true)

FAIL=0

if [[ -n "$MISSING" ]]; then
  echo "::warning::Permissions expected but not present in APK:"
  while IFS= read -r p; do echo "  - $p"; done <<< "$MISSING"
  echo ""
fi

if [[ -n "$UNEXPECTED" ]]; then
  echo "::error::Unexpected permissions found in APK — update the allowlist in"
  echo "         tools/check-release-permissions.sh if these are intentional:"
  while IFS= read -r p; do echo "  + $p"; done <<< "$UNEXPECTED"
  echo ""
  FAIL=1
fi

if [[ $FAIL -eq 0 ]]; then
  echo "✓ Permission allowlist check passed."
fi

exit $FAIL
