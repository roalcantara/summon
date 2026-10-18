#!/usr/bin/env bash
# Pulls the debug build's session logs and chant clips into the git-ignored test folders (ADR-0025).
# Usage: tools/pull_sessions.sh [serial]
#   Without a serial: $ANDROID_SERIAL, else the only device, else the first physical device (not an emulator).
set -euo pipefail
app=com.summondemons.safely
root="$(cd "$(dirname "$0")/.." && pwd)"
serial="${1:-${ANDROID_SERIAL:-}}"
if [ -z "$serial" ]; then
  devices=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
  serial=$(echo "$devices" | grep -v '^emulator-' | head -n1 || true)
  [ -z "$serial" ] && serial=$(echo "$devices" | head -n1)
fi
[ -z "$serial" ] && { echo "no device connected" >&2; exit 1; }
echo "device: $serial"
pull() { # $1 = device folder, $2 = local folder
  mkdir -p "$2"
  for f in $(adb -s "$serial" shell run-as "$app" ls "files/$1" 2>/dev/null | tr -d '\r'); do
    adb -s "$serial" exec-out run-as "$app" cat "files/$1/$f" > "$2/$f"
    echo "pulled $1/$f"
  done
}
pull sessions "$root/app/src/test/sessions"
pull chant-clips "$root/app/src/test/chant-clips"
