#!/usr/bin/env bash
# Pulls the debug build's session logs and chant clips into the git-ignored test folders (ADR-0025).
# Usage: tools/pull_sessions.sh   (one device connected over adb)
set -euo pipefail
app=com.summondemons.safely
root="$(cd "$(dirname "$0")/.." && pwd)"
pull() { # $1 = device folder, $2 = local folder
  mkdir -p "$2"
  for f in $(adb shell run-as "$app" ls "files/$1" 2>/dev/null | tr -d '\r'); do
    adb exec-out run-as "$app" cat "files/$1/$f" > "$2/$f"
    echo "pulled $1/$f"
  done
}
pull sessions "$root/app/src/test/sessions"
pull chant-clips "$root/app/src/test/chant-clips"
