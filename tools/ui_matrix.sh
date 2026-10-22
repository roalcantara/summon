#!/usr/bin/env bash
# Screenshots every screen in every configuration of the pre-release matrix (spec §5).
# Usage: tools/ui_matrix.sh [label]        label: e.g. s24-en (default: device model + "run")
# Needs: a debug build installed, one device on adb. Sets font scale and display size per run,
# asks you to bring the app to each screen/state (the debug panel reaches the states), captures,
# and always restores the device settings.
set -euo pipefail

PKG=com.summondemons.safely
LABEL=${1:-"$(adb shell getprop ro.product.model | tr -d '\r')-run"}
OUT="build/ui-matrix/$LABEL"
FONT_SCALES=(1.0 1.3 2.0)
SHOTS=(
  "home"            "Home"
  "grimoire-front"  "Grimoire, unbound demon, front"
  "grimoire-back"   "Grimoire, same demon, back (tap the flip button)"
  "grimoire-bound"  "Grimoire, a bound demon (debug: Bind), front"
  "grimoire-cool"   "Grimoire, cooling (after today's summon), front"
  "celebration"     "Celebration (right after a real summon; s to skip)"
  "intro"           "Intro card (Tap to summon)"
  "chant-seeking"   "Chant screen, seeking"
  "chant-chanting"  "Chant screen, after the lock (debug: Simulate heading)"
  "abort"           "Abort confirmation (after the lock, tap X)"
  "settings"        "Settings sheet"
)

orig_font=$(adb shell settings get system font_scale | tr -d '\r')
orig_density=$(adb shell wm density | sed -n 's/.*Override density: //p' | tr -d '\r')
phys_density=$(adb shell wm density | sed -n 's/Physical density: //p' | tr -d '\r')
base_density=${orig_density:-$phys_density}
DENSITIES=("$base_density" "$((base_density * 115 / 100))")

restore() {
  adb shell settings put system font_scale "$orig_font"
  if [ -n "$orig_density" ]; then adb shell wm density "$orig_density"; else adb shell wm density reset; fi
  echo "Restored font_scale=$orig_font density=${orig_density:-physical}"
}
trap restore EXIT

for density in "${DENSITIES[@]}"; do
  for font in "${FONT_SCALES[@]}"; do
    dir="$OUT/font${font}-dpi${density}"
    mkdir -p "$dir"
    adb shell wm density "$density"
    adb shell settings put system font_scale "$font"
    adb shell am force-stop "$PKG"
    adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
    echo "== font $font, density $density -> $dir"
    for ((i = 0; i < ${#SHOTS[@]}; i += 2)); do
      read -r -p "  ${SHOTS[i + 1]}: Enter to capture, s to skip " answer
      [ "$answer" = "s" ] && continue
      adb exec-out screencap -p > "$dir/${SHOTS[i]}.png"
    done
  done
done
echo "Screenshots in $OUT"
