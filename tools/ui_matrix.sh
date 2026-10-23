#!/usr/bin/env bash
# Screenshots the app's screens in every configuration of the pre-release matrix (spec §5).
#
# Usage: tools/ui_matrix.sh [label] [lang]
#   label  output folder name, e.g. s24-en (default: device model + "-run")
#   lang   the app's current language: en, es, pt-BR, it, fr (default: en). Set the app to it in
#          Settings first: the script finds buttons by their (translated) labels.
#
# Needs: a debug build installed, one device on adb (ANDROID_SERIAL to pick one).
# Navigates by itself (UI Automator, with animations off so the screen can be read), using the
# debug panel for the bound and cooling states. It **unsummons both demons and resets the daily
# summon** before and after each configuration. Not automated (need a sealed line or a real
# summon): the celebration and the abort confirmation; capture those by hand.
# Always restores font scale, density and animation scale.
set -euo pipefail

PKG=com.summondemons.safely
LABEL=${1:-"$(adb shell getprop ro.product.model | tr -d '\r')-run"}
LANG_CODE=${2:-en}
OUT="build/ui-matrix/$LABEL"
FONT_SCALES=(1.0 1.3 2.0)
RES=app/src/main/res
case "$LANG_CODE" in
  en) VALUES=values ;;
  pt-BR) VALUES=values-pt-rBR ;;
  es | it | fr) VALUES="values-$LANG_CODE" ;;
  *) echo "Unknown language: $LANG_CODE" >&2; exit 2 ;;
esac
TMP=$(mktemp -d)

# A string resource in the app's language (debug strings are English only).
str() {
  python3 -I - "$RES/$VALUES/strings.xml" "$RES/values/strings_debug.xml" "$1" <<'EOF'
import sys, xml.etree.ElementTree as ET
key = sys.argv[3]
for path in sys.argv[1:3]:
    for e in ET.parse(path).getroot():
        if e.get("name") == key and e.text:
            print(e.text.replace("\\'", "'").replace("\\n", "\n").split("%")[0].strip())
            sys.exit(0)
sys.exit(f"missing string {key}")
EOF
}

# Taps the first on-screen element whose text or content description matches (case-insensitive;
# prefix match with "prefix:"), retrying while the screen settles. Skip-th match with $2.
tap() {
  local want=$1 nth=${2:-0} scroll=${3:-} try
  for try in 1 2 3 4 5 6 7 8; do
    # In a scrolling sheet, rows below the fold are not in the dump: scroll up and look again.
    if [ -n "$scroll" ] && [ "$try" -gt 1 ]; then adb shell input swipe 540 1900 540 1100 300; sleep 0.8; fi
    adb shell uiautomator dump /sdcard/ui_matrix.xml >/dev/null 2>&1 || true
    adb exec-out cat /sdcard/ui_matrix.xml > "$TMP/ui.xml" 2>/dev/null || true
    local xy
    xy=$(python3 -I - "$TMP/ui.xml" "$want" "$nth" <<'EOF' || true
import re, sys, xml.etree.ElementTree as ET
path, want, nth = sys.argv[1], sys.argv[2], int(sys.argv[3])
prefix = want.startswith("prefix:")
want = (want[7:] if prefix else want).casefold()
try:
    nodes = ET.parse(path).iter("node")
except ET.ParseError:
    sys.exit(1)
hits = []
for n in nodes:
    for label in (n.get("text", ""), n.get("content-desc", "")):
        l = label.casefold().strip()
        if l and (l.startswith(want) if prefix else l == want):
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
            hits.append(((x1 + x2) // 2, (y1 + y2) // 2))
            break
if len(hits) > nth:
    print(*hits[nth])
else:
    sys.exit(1)
EOF
)
    if [ -n "$xy" ]; then
      # shellcheck disable=SC2086
      adb shell input tap $xy
      sleep 1.5
      return 0
    fi
    sleep 1
  done
  echo "    ! not found: $want" >&2
  return 1
}

shot() { adb exec-out screencap -p > "$dir/$1.png"; echo "    $1"; }
back() { adb shell input keyevent KEYCODE_BACK; sleep 1.2; }

# True while the Settings or Debug sheet is up (their titles are text nodes; the gear is a
# content description, so Home never matches).
sheet_open() {
  adb shell uiautomator dump /sdcard/ui_matrix.xml >/dev/null 2>&1 || true
  adb exec-out cat /sdcard/ui_matrix.xml > "$TMP/ui.xml" 2>/dev/null || true
  python3 -I - "$TMP/ui.xml" "$(str settings_title)" "$(str debug_chip)" <<'EOF'
import sys, xml.etree.ElementTree as ET
titles = {t.casefold() for t in sys.argv[2:]}
try:
    found = any(n.get("text", "").casefold() in titles for n in ET.parse(sys.argv[1]).iter("node"))
except ET.ParseError:
    found = False
sys.exit(0 if found else 1)
EOF
}

# Closes any open sheet with back, checking each time (never backs out of the screen below).
close_sheets() {
  local i
  for i in 1 2 3; do sheet_open || return 0; back; done
}

# Settings → Debug controls, then [actions], then close the sheet.
debug_sheet() {
  local ok=0
  if tap "$(str settings_title)" && tap "$(str debug_controls)"; then "$@" || ok=1; else ok=1; fi
  close_sheets
  return $ok
}
clean_state() {
  tap "$(str debug_reset_daily)" 0 scroll || true
  tap "$(str debug_unsummon)" 0 scroll || true
  tap "$(str debug_unsummon)" 1 scroll || true
}
bind_first() { tap "$(str debug_bind)" 0 scroll; }

orig_font=$(adb shell settings get system font_scale | tr -d '\r')
orig_anim=$(adb shell settings get global animator_duration_scale | tr -d '\r')
orig_density=$(adb shell wm density | sed -n 's/.*Override density: //p' | tr -d '\r')
phys_density=$(adb shell wm density | sed -n 's/Physical density: //p' | tr -d '\r')
base_density=${orig_density:-$phys_density}
DENSITIES=("$base_density" "$((base_density * 115 / 100))")

restore() {
  adb shell settings put system font_scale "$orig_font"
  adb shell settings put global animator_duration_scale "${orig_anim/null/1.0}"
  if [ -n "$orig_density" ]; then adb shell wm density "$orig_density"; else adb shell wm density reset; fi
  rm -rf "$TMP"
  echo "Restored font_scale=$orig_font density=${orig_density:-physical} animator_duration_scale=${orig_anim/null/1.0}"
}
trap restore EXIT
# A killed run (kill -9, a tool timeout) cannot restore: print how to undo it by hand.
echo "If interrupted, restore with: adb shell settings put system font_scale $orig_font;" \
  "adb shell settings put global animator_duration_scale ${orig_anim/null/1.0};" \
  "adb shell wm density ${orig_density:-reset}"

adb shell pm grant "$PKG" android.permission.RECORD_AUDIO 2>/dev/null || true
# Infinite animations keep UI Automator from ever seeing an idle screen.
adb shell settings put global animator_duration_scale 0

for density in "${DENSITIES[@]}"; do
  for font in "${FONT_SCALES[@]}"; do
    dir="$OUT/font${font}-dpi${density}"
    mkdir -p "$dir"
    adb shell wm density "$density"
    adb shell settings put system font_scale "$font"
    adb shell am force-stop "$PKG"
    adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    sleep 6
    echo "== font $font, density $density -> $dir"
    debug_sheet clean_state || true

    shot home
    tap "$(str settings_title)" && shot settings
    close_sheets

    tap "$(str home_open)" || { echo "    ! skipping the rest of this configuration" >&2; continue; }
    sleep 1
    shot grimoire-front
    tap "$(str grimoire_show_dossier)" && { shot grimoire-back; tap "$(str grimoire_show_profile)" || back; }

    if tap "prefix:$(str cd_summon)"; then
      sleep 2
      shot intro
      if tap "$(str intro_summon)"; then
        sleep 2
        shot chant-seeking
        tap "$(str debug_controls)" && tap "$(str debug_simulate_heading)" && back
        sleep 2
        shot chant-chanting
      fi
      back # before the first seal, back leaves the Ritual without asking
      sleep 2
    fi

    if debug_sheet bind_first; then
      shot grimoire-bound
      tap "$(str grimoire_next)" && { sleep 1; shot grimoire-cool; }
    fi
    debug_sheet clean_state || true
  done
done
echo "Screenshots in $OUT (celebration and abort confirmation: by hand)"
