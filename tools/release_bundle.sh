#!/usr/bin/env bash
# Builds the signed release App Bundle for a Play upload (ADR-0017): bumps versionCode, runs the
# unit tests, builds, and verifies what will be uploaded.
#
# Usage: tools/release_bundle.sh [--no-bump] [--name <versionName>]
#   --no-bump  rebuild at the current versionCode (only if that code was never uploaded)
#   --name     also set versionName, e.g. 1.0.1 (players see this; Play does not enforce it)
#
# Needs: the four SUMMON_UPLOAD_* values (environment or local.properties), a clean tree of tracked
# files (the bump must be its own change). Checks: jarsigner verifies the bundle, its signer is the
# upload key in the keystore, the manifest carries the new version and is not debuggable. Prints the
# file to upload, its size, SHA-256 and permissions. If anything fails after the bump, the bump is
# undone. The bump is left uncommitted for you to commit.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

BUMP=1
NAME=""
while [ $# -gt 0 ]; do
  case "$1" in
    --no-bump) BUMP=0 ;;
    --name) NAME=${2:?--name needs a value}; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

GRADLE_FILE=app/build.gradle.kts
AAB=app/build/outputs/bundle/release/app-release.aab
MANIFEST=app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
fail() { echo "✗ $*" >&2; exit 1; }

# --- Upload key: environment first, then local.properties (same order as the build) ---
prop() {
  local key=$1
  if [ -n "${!key:-}" ]; then printf '%s' "${!key}"; return; fi
  [ -f local.properties ] || return 0
  python3 -I -c 'import sys
for l in open("local.properties", encoding="utf-8"):
    l = l.strip()
    if l and not l.startswith("#") and "=" in l:
        k, v = l.split("=", 1)
        if k.strip() == sys.argv[1]: print(v.strip(), end="")' "$key"
}
for key in SUMMON_UPLOAD_STORE_FILE SUMMON_UPLOAD_STORE_PASSWORD SUMMON_UPLOAD_KEY_ALIAS SUMMON_UPLOAD_KEY_PASSWORD; do
  [ -n "$(prop "$key")" ] || fail "$key is not set (environment or local.properties); see ADR-0017"
done
STORE_FILE=$(prop SUMMON_UPLOAD_STORE_FILE)
STORE_FILE=${STORE_FILE/#\~/$HOME}
KEY_ALIAS=$(prop SUMMON_UPLOAD_KEY_ALIAS)
[ -f "$STORE_FILE" ] || fail "keystore not found: $STORE_FILE"
# Passed to keytool through the environment, never on the command line or in the output.
SUMMON_STOREPASS=$(prop SUMMON_UPLOAD_STORE_PASSWORD)
export SUMMON_STOREPASS
KEY_SHA=$(keytool -list -v -keystore "$STORE_FILE" -storepass:env SUMMON_STOREPASS -alias "$KEY_ALIAS" 2>/dev/null \
  | awk '/SHA256:/ {print $2; exit}') || true
[ -n "$KEY_SHA" ] || fail "cannot open alias '$KEY_ALIAS' in $STORE_FILE with the stored password"
echo "✓ upload key: $KEY_ALIAS ($KEY_SHA)"

# --- Version bump ---
OLD_CODE=$(sed -nE 's/^ *versionCode = ([0-9]+).*/\1/p' "$GRADLE_FILE")
OLD_NAME=$(sed -nE 's/^ *versionName = "([^"]*)".*/\1/p' "$GRADLE_FILE")
[ -n "$OLD_CODE" ] || fail "versionCode not found in $GRADLE_FILE"
CODE=$OLD_CODE
VNAME=${NAME:-$OLD_NAME}
BACKUP=""
restore() { if [ -n "$BACKUP" ]; then cp "$BACKUP" "$GRADLE_FILE"; rm -f "$BACKUP"; echo "↺ version bump undone" >&2; fi; }
if [ "$BUMP" = 1 ] || [ -n "$NAME" ]; then
  git diff --quiet HEAD -- . || fail "tracked files have changes; commit or stash them first (the bump must be its own change)"
  BACKUP=$(mktemp)
  cp "$GRADLE_FILE" "$BACKUP"
  trap restore ERR INT
  [ "$BUMP" = 1 ] && CODE=$((OLD_CODE + 1))
  sed -i '' -E "s/^( *versionCode = )[0-9]+/\1$CODE/" "$GRADLE_FILE"
  sed -i '' -E "s/^( *versionName = \")[^\"]*/\1$VNAME/" "$GRADLE_FILE"
  echo "✓ version: $OLD_CODE ($OLD_NAME) → $CODE ($VNAME)"
else
  echo "• version unchanged: $CODE ($VNAME) — only for a code never uploaded"
fi

# --- Tests and build ---
echo "• unit tests…"
./gradlew -q testDebugUnitTest
echo "✓ unit tests pass"
rm -f "$AAB"
echo "• bundleRelease…"
LOG=$(mktemp)
./gradlew bundleRelease >"$LOG" 2>&1 || { tail -30 "$LOG" >&2; fail "bundleRelease failed"; }
grep -q "UNSIGNED" "$LOG" && fail "the build reported an UNSIGNED release; check SUMMON_UPLOAD_*"
rm -f "$LOG"
[ -f "$AAB" ] || fail "no bundle at $AAB"

# --- Verify what will be uploaded ---
jarsigner -verify "$AAB" 2>/dev/null | grep -q "^jar verified" || fail "jarsigner could not verify $AAB"
SIGNER_SHA=$(keytool -printcert -jarfile "$AAB" 2>/dev/null | awk '/SHA256:/ {print $2; exit}')
[ "$SIGNER_SHA" = "$KEY_SHA" ] || fail "bundle is signed by $SIGNER_SHA, not the upload key $KEY_SHA"
echo "✓ signature verified, signer is the upload key"
grep -q "android:versionCode=\"$CODE\"" "$MANIFEST" || fail "manifest does not carry versionCode $CODE"
grep -q 'android:debuggable="true"' "$MANIFEST" && fail "release manifest is debuggable"
echo "✓ manifest: versionCode $CODE, versionName $VNAME, not debuggable"
echo "  permissions: $(grep -oE 'uses-permission android:name="android\.permission\.[A-Z_]+"' "$MANIFEST" \
  | sed -E 's/.*permission\.([A-Z_]+)"/\1/' | sort -u | tr '\n' ' ')"

trap - ERR INT
[ -n "$BACKUP" ] && rm -f "$BACKUP"
echo
echo "Ready to upload: $AAB"
echo "  size $(du -h "$AAB" | cut -f1) · sha256 $(shasum -a 256 "$AAB" | cut -c1-16)…"
if [ "$CODE" != "$OLD_CODE" ] || [ "$VNAME" != "$OLD_NAME" ]; then
  echo "  Commit the bump: git commit -am \"chore(release): versionCode $CODE ($VNAME)\""
fi
