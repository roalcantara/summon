# 0017 — Release and distribution

- Date: 2026-10-16
- Status: Accepted

## Context
The game is functionally complete and ready for distribution via Google Play. We need
clear policies and automated build setup for release artifacts, code and resource
shrinking, signing secrets, versioning, privacy commitments, and launcher icon assets.

## Decision

1. **Distribution:** Distributed via Google Play as an Android App Bundle (`.aab`)
   using **Play App Signing**. The local keystore is the *upload key* only; Google holds
   the app signing key.
2. **Signing secrets:** Secrets never enter git. The release `signingConfig` reads
   `SUMMON_UPLOAD_STORE_FILE`, `SUMMON_UPLOAD_STORE_PASSWORD`,
   `SUMMON_UPLOAD_KEY_ALIAS`, and `SUMMON_UPLOAD_KEY_PASSWORD` from environment variables,
   falling back to `local.properties` (git-ignored). Without them, release builds stay
   unsigned with a logger warning.
3. **Privacy stance:** No `INTERNET` permission and no third-party SDKs collecting user
   data. The microphone and heading sensor operate strictly while the Ritual screen is in
   the foreground; audio data is analysed in memory and immediately discarded. Data safety
   answer is "no data collected". Any future network feature requires a new ADR and
   privacy policy update.
4. **Shrinking:** R8 code optimization and resource shrinking are enabled for release builds.
   In AGP 9.4+, `optimization { enable = true }` enables R8 code shrinking/obfuscation and
   resource shrinking. Specifying `packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")`
   restricts optimization to external dependency packages while leaving the app's own code
   (`com.summondemons.safely.**`) in its original form. This deliberate configuration avoids
   aggressive reflection/serialization pitfalls with kotlinx-serialization models and
   Navigation route types without requiring brittle custom keep rules, while stripping unused
   framework/library bytecode and all unreferenced resources. The obfuscation mapping file
   is included inside the AAB (`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`)
   for Play Console deobfuscation.
5. **Versioning:** `versionCode` is a monotonically increasing integer bumped on every
   upload; `versionName` follows SemVer (`1.0.0` for initial launch).
6. **Launcher icon:** Generated reproducibly from `ic_game_logo.webp` via `tools/gen_icons.py`.
   Adaptive foreground, circle-masked legacy icons, and monochrome vector layer are provided.
   A text-free emblem remains a future upgrade opportunity (DESIGN.md Gaps).
7. **Large screens:** Portrait orientation lock is retained as compass headings assume it.
   Large screen system overrides (sw >= 600 dp) on Android 16+ are accepted for v1.

## How to build a release

To produce a signed release App Bundle, provide the upload keystore path and passwords via
environment variables or `local.properties`:

```properties
SUMMON_UPLOAD_STORE_FILE=/Users/me/.android/summon-upload.jks
SUMMON_UPLOAD_STORE_PASSWORD=keystore-password
SUMMON_UPLOAD_KEY_ALIAS=summon-upload
SUMMON_UPLOAD_KEY_PASSWORD=key-password
```

Then run:
```bash
./gradlew bundleRelease
```
The bundle output will be at `app/build/outputs/bundle/release/app-release.aab`.

## Consequences
- Guarantees zero network leakage and clear privacy compliance for store review.
- Safe release pipeline where secret leaks in git are prevented by design.
- Builds without upload keys succeed in CI/debug environments as unsigned release bundles.
