#!/usr/bin/env bash
set -euo pipefail

manifest="app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
if [[ ! -f "$manifest" ]]; then
  echo "Release merged manifest not found: $manifest" >&2
  exit 1
fi

actual=$(sed -n '/<uses-permission /s/.*android:name="\([^"]*\)".*/\1/p' "$manifest" | sort -u)
expected=$(cat <<'EOF'
android.permission.ACCESS_NETWORK_STATE
android.permission.FOREGROUND_SERVICE
android.permission.INTERNET
android.permission.POST_NOTIFICATIONS
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.WAKE_LOCK
uk.ewancroft.inkwell.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
EOF
)

if [[ "$actual" != "$expected" ]]; then
  echo "Unexpected release permission set:" >&2
  diff -u <(printf '%s\n' "$expected") <(printf '%s\n' "$actual") >&2 || true
  exit 1
fi
