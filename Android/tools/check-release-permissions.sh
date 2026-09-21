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

apk="${1:-}"
if [[ -n "$apk" ]]; then
  if [[ ! -f "$apk" ]]; then
    echo "Release APK not found: $apk" >&2
    exit 1
  fi
else
  release_apks=()
  while IFS= read -r apk_path; do
    release_apks+=("$apk_path")
  done < <(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print | sort)
  if [[ "${#release_apks[@]}" -ne 1 ]]; then
    echo "Expected exactly one release APK, found ${#release_apks[@]}" >&2
    printf '%s\n' "${release_apks[@]}" >&2
    exit 1
  fi
  apk="${release_apks[0]}"
fi

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_dir" && -f local.properties ]]; then
  sdk_dir=$(sed -n 's/^sdk.dir=//p' local.properties)
fi
aapt=""
if [[ -n "$sdk_dir" ]]; then
  aapt=$(find "$sdk_dir/build-tools" -mindepth 2 -maxdepth 2 -type f -name aapt 2>/dev/null | sort | tail -1)
fi
if [[ -z "$aapt" ]]; then
  aapt=$(command -v aapt || true)
fi
if [[ -z "$aapt" ]]; then
  echo "aapt is required to inspect the assembled release APK" >&2
  exit 1
fi

apk_actual=$(
  "$aapt" dump permissions "$apk" |
    sed -n "/^uses-permission: name='/s/^uses-permission: name='\([^']*\)'.*/\1/p" |
    sort -u
)
if [[ "$apk_actual" != "$expected" ]]; then
  echo "Unexpected release APK permission set:" >&2
  diff -u <(printf '%s\n' "$expected") <(printf '%s\n' "$apk_actual") >&2 || true
  exit 1
fi

echo "Release merged manifest and APK permissions match the allowlist."
