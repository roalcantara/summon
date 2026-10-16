# Play Store Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Take the functionally complete game to a signed, shrunk, lint-clean Android App
Bundle (`.aab`) with a proper launcher icon, store graphics, a published privacy policy
and a store listing, ready to upload to the Play Console.

**Already done (do not redo):**
- `docs/PRIVACY_POLICY.md`: privacy policy draft (placeholders `<DEVELOPER_NAME>` and
  `<CONTACT_EMAIL>`, filled by the owner in Task 9).
- `docs/STORE_LISTING.md`: title, short and full description, category, Data safety
  answers, content-rating and target-audience notes, and graphics checklist.

**Architecture / scope:** No gameplay changes, with one exception: Task 2 makes the
microphone (and heading sensor) stop the moment the Ritual leaves the foreground, because
the privacy policy promises it. Everything else is build configuration, resources, a small
Pillow generator in `tools/`, and docs.

**Tech stack:** AGP 9.4.1 (Kotlin 2.2.10, new `compileSdk { version = release(37) }` and
`optimization {}` DSL), JDK 25 toolchain, Compose BOM 2026.09.00. `minSdk 24`,
`targetSdk 37`. Python 3 + Pillow for generators (same style as `tools/gen_audio.py`).

**Working directory:** the repo root. Do not edit anything under `reference/`.

**Branch:** from `main`:

```bash
git fetch origin main && git checkout -b <your-branch> origin/main
```

**Commit trailer:** set `TRAILER` to the attribution trailer your session requires; every
commit passes it as the second `-m`.

**Path shorthand** (reading only; commands spell out full paths):
- `MAIN` = `app/src/main/java/com/summondemons/safely`
- `TEST` = `app/src/test/java/com/summondemons/safely`
- `RES` = `app/src/main/res`

**Environment prerequisites.** The build needs the Android SDK (platform 37, build-tools)
and a JDK 25 toolchain. Some cloud containers have neither, and their proxy blocks
Gradle's toolchain download (foojay). If `./gradlew` fails with *Unable to download
toolchain* or *SDK location not found*, **stop and report**. Do not commit untested
build changes. A local machine with Android Studio works.

**Verification gate** (end of every phase, and wherever a task says "gate"):

```bash
./gradlew testDebugUnitTest lint assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Lint warnings are allowed; lint errors are not.

---

## Phase 0: Canon

### Task 1: ADR-0017 Release & distribution, plus doc pointers

**Files:** create `docs/decisions/0017-release-and-distribution.md`; modify `AGENTS.md`,
`PRODUCT.md`.

- [ ] **Step 1:** Write the ADR in the style of the existing ones (read
  `docs/decisions/0013-persistence-and-flawless.md` first for format). Decisions to
  record:
  1. **Distribution:** Google Play, Android App Bundle, **Play App Signing**. The local
     keystore is the *upload key* only; Google holds the app signing key.
  2. **Signing secrets** never enter git. The release `signingConfig` reads
     `SUMMON_UPLOAD_STORE_FILE`, `SUMMON_UPLOAD_STORE_PASSWORD`,
     `SUMMON_UPLOAD_KEY_ALIAS`, `SUMMON_UPLOAD_KEY_PASSWORD` from environment variables,
     falling back to the same keys in `local.properties` (already git-ignored). Without
     them, release builds stay unsigned and the build says so.
  3. **Privacy stance:** no `INTERNET` permission, no SDKs that collect data. The mic and
     heading sensor run only while the Ritual is in the foreground. Audio is analysed in
     memory and discarded. Data safety answer: "no data collected". Any future network
     feature needs a new ADR and a policy update.
  4. **Shrinking:** R8 code optimization + resource shrinking on for release; the
     mapping file ships inside the AAB for Play deobfuscation.
  5. **Versioning:** `versionCode` is a monotonically increasing integer, bumped on
     every upload; `versionName` is SemVer (`1.0.0` for the first release).
  6. **Launcher icon** is derived from `ic_game_logo` by `tools/gen_icons.py`. Known
     limitation: the logo is mostly lettering, which is barely legible at launcher size.
     A text-free emblem is a possible later upgrade (DESIGN.md Gaps).
  7. **Large screens:** portrait lock stays (the compass reading assumes it). On
     Android 16+ large screens (sw ≥ 600 dp) the system may ignore the lock; accepted
     for v1.
- [ ] **Step 2:** `AGENTS.md` → Repo map: add rows for `docs/PRIVACY_POLICY.md`,
  `docs/STORE_LISTING.md`, `docs/play-store/` (store graphics) and
  `tools/gen_icons.py`. Commands: add `./gradlew bundleRelease  # signed AAB (needs
  upload key, see ADR-0017)`. Current state: one line "Release prep: ADR-0017".
- [ ] **Step 3:** `PRODUCT.md` → Screens and rules → add a row:
  `| Privacy | No network access, no data collection. The mic and heading sensor run only while the Ritual is in the foreground; audio is analysed in memory and never stored or sent (ADR-0017, docs/PRIVACY_POLICY.md). |`
- [ ] **Step 4:** Commit:
  `git commit -m "docs(release): ADR-0017 release and distribution" -m "$TRAILER"`

---

## Phase 1: Privacy-policy guarantees in code

### Task 2: Stop mic and heading sensor as soon as the Ritual is stopped

**Why:** `RitualViewModel.uiState` uses `SharingStarted.WhileSubscribed(5_000)`, so
`AudioRecord` keeps capturing for up to 5 s after the Ritual leaves the screen (the
system mic indicator stays on). `setActive(false)` (Ritual `ON_STOP`) only pauses the
ticks. The policy says "When you leave the Ritual screen, the microphone stops."

**Files:** modify `MAIN/ritual/RitualViewModel.kt`; test in
`TEST/ritual/RitualViewModelTest.kt`.

- [ ] **Step 1: Failing tests.** Extend the test's `FakePcm` and `FakeHeading` to count
  live collectors (increment in `onStart`, decrement in `onCompletion` of the returned
  flow). Add:
  - `mic stops when ritual becomes inactive`: mic granted, collect `uiState`, advance;
    assert 1 live PCM collector; `setActive(false)`, advance (less than 5 s); assert 0;
    `setActive(true)`, advance; assert 1.
  - `heading sensor stops when ritual becomes inactive`: same for `FakeHeading`.
  - `restart after inactive waits for a fresh heading`: after `setActive(false)` then
    `true`, the first tick before any new azimuth does not drain focus or waver (the
    existing heading-hold behaviour still holds).
  Run `./gradlew testDebugUnitTest --tests '*RitualViewModelTest*'` → new tests FAIL.
- [ ] **Step 2: Implement.** Gate both source flows on `active`:
  ```kotlin
  private val voiceFrames: Flow<VoiceFrame> = state
      .map { it.micGranted == true && it.active }
      .distinctUntilChanged()
      .flatMapLatest { live -> if (live) micFrames() else silentFrames() }
  ```
  Do the same for `azimuths`: when not active, emit nothing (`emptyFlow()`), unless
  simulating. In `setActive(false)` also clear `azimuth` (unless `simulateHeading`), so a
  restart waits for a fresh reading exactly as the `channelFlow` start does. Keep
  `WhileSubscribed(5_000)`: it is still right for configuration changes.
- [ ] **Step 3:** The tests PASS, and so does the whole `RitualViewModelTest`. Gate.
- [ ] **Step 4:** Commit:
  `git commit -m "fix(ritual): Stop mic and heading sensor when the ritual is stopped" -m "$TRAILER"`

---

## Phase 2: Art: launcher icon and store graphics

### Task 3: `tools/gen_icons.py`, a reproducible icon generator

**Files:** create `tools/gen_icons.py`; outputs into `RES/mipmap-*/` and
`docs/play-store/`.

Current state: the adaptive icon (`RES/mipmap-anydpi-v26/ic_launcher*.xml`) is
`@color/obsidian` behind `RES/drawable/ic_launcher_foreground.xml`, an `<inset>` (18 %)
of the 1024² opaque `ic_game_logo.webp`. The legacy `mipmap-*/ic_launcher*.webp` files
are logo downscales, and the "round" ones are **not** circle-masked. There is no
monochrome layer and no 512 px store icon.

- [ ] **Step 1:** Write `tools/gen_icons.py` (Pillow only, run with `python3 -I`, argv:
  `<logo.webp> <res_dir> <store_dir>`, docstring with usage like `gen_audio.py`). It
  writes:
  1. **Adaptive foreground**, `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.webp`
     at 108 dp (108/162/216/324/432 px). Fit the logo's emblem circle (≈ 92 % of the
     logo's width) to the **72 dp** diameter safe zone, centred on a transparent canvas.
     Feather the logo's square corners to transparent outside the emblem circle (radial
     alpha mask, 2–3 px soft edge), so no square edge ever shows under any mask shape.
  2. **Legacy icons** (pre-API 26): `ic_launcher.webp` at 48 dp (48/72/96/144/192 px):
     obsidian rounded square with the emblem at ~85 %. `ic_launcher_round.webp`: the
     same, **circle-masked** with transparent corners.
  3. **Store icon**, `docs/play-store/icon-512.png`: 512 × 512, 32-bit PNG, full-bleed
     opaque (Play applies its own mask; no pre-rounded corners, no drop shadow), emblem
     at ~90 % on obsidian.
  4. **Feature graphic**, `docs/play-store/feature-graphic.png`: 1024 × 500, RGB (no
     alpha). Background: `RES/drawable-nodpi/stage_home.webp` cover-cropped and
     darkened ~35 %. Logo at 440 px height, centred-left. Keep the outer 10 % clear of
     anything important.
  Read the obsidian hex from `RES/values/colors.xml` (`<color name="obsidian">`) rather
  than hard-coding it. Save WebP lossless for the mipmaps (they are small) and PNG for
  the store.
- [ ] **Step 2:** Run it:
  ```bash
  python3 -I tools/gen_icons.py app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store
  ```
  **Look at every output** (open the images): the emblem must not be clipped in a circle
  mask of 66 dp (the strictest launcher mask), and no square edge may be visible.
  Make a temporary contact sheet in the scratchpad to check all of them at once.
- [ ] **Step 3:** Commit:
  `git commit -m "feat(art): Icon generator for launcher and store graphics" -m "$TRAILER"`

### Task 4: Wire the adaptive icon (with a monochrome layer)

**Files:** modify `RES/mipmap-anydpi-v26/ic_launcher.xml`,
`RES/mipmap-anydpi-v26/ic_launcher_round.xml`; delete
`RES/drawable/ic_launcher_foreground.xml`; create
`RES/drawable/ic_launcher_monochrome.xml`.

- [ ] **Step 1:** Both adaptive XMLs become:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
      <background android:drawable="@color/obsidian" />
      <foreground android:drawable="@mipmap/ic_launcher_foreground" />
      <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
  </adaptive-icon>
  ```
- [ ] **Step 2:** `ic_launcher_monochrome.xml`: a 108 × 108 dp vector, single solid fill
  (`#FFFFFFFF`; Android tints it), inside the 66 dp centre. Use an emblem echoing the
  logo: an outer ring (stroke ≈ 4 dp, radius ≈ 30 dp), and inside it the logo's upward
  triangle in a small circle. No text. Keep it under 30 path commands.
- [ ] **Step 3:** Delete `RES/drawable/ic_launcher_foreground.xml` (replaced by the
  mipmap). `grep -rn "ic_launcher_foreground" app/src` must only find the adaptive XMLs.
- [ ] **Step 4:** Gate. Then `./gradlew installDebug` on a device or emulator if you have
  one, and check the launcher icon, the themed (monochrome) icon (Android 13+:
  Wallpaper & style → Themed icons) and the splash icon. Otherwise record "visual check
  pending (owner, Task 9)".
- [ ] **Step 5:** Update DESIGN.md → Asset catalog: add rows for
  `ic_launcher_foreground` (mipmaps, generated) and `ic_launcher_monochrome` (vector), and
  a sentence: "Regenerate launcher and store graphics with `python3 -I tools/gen_icons.py
  …`". Commit:
  `git commit -m "feat(art): Adaptive launcher icon from the game logo, with themed layer" -m "$TRAILER"`

### Task 5: Splash icon check

- [ ] `Theme.SummonDemonsSafely.Starting` (in `RES/values/themes.xml`) sets only
  `windowSplashScreenBackground` (obsidian), so the splash uses the launcher icon from
  Task 4. Android 12+ masks the splash icon to a circle of 2/3 of its size. Confirm on a
  device or emulator that the emblem isn't clipped. Only if it is, add
  `windowSplashScreenAnimatedIcon` pointing at a smaller inset of
  `@mipmap/ic_launcher_foreground`. Gate; commit only if changed:
  `fix(art): Splash icon fits the Android 12+ mask`.

---

## Phase 3: Release build

### Task 6: Upload keystore and release `signingConfig`

**Files:** modify `.gitignore`, `app/build.gradle.kts`.

- [ ] **Step 1: Ignore secrets.** Append to `.gitignore`:
  ```
  # Signing (ADR-0017): never commit keystores
  *.jks
  *.keystore
  !debug.keystore
  ```
- [ ] **Step 2: Generate the upload key, on the owner's machine.** A key generated in an
  ephemeral cloud container is lost when the container is reclaimed. In a cloud
  session, **do not** generate the real key: generate a throwaway one under the
  scratchpad only to test the wiring, and leave the real generation to Task 9. Command
  (PKCS12, RSA 4096, valid ~27 years; Play requires validity past 2033-10-22):
  ```bash
  keytool -genkeypair -v -storetype PKCS12 \
    -keystore ~/.android/summon-upload.jks -alias summon-upload \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=<DEVELOPER_NAME>, O=Summon Demons Safely"
  ```
  Keep the keystore **outside the repo**, and back up the file and both passwords
  (password manager). With Play App Signing a lost upload key can be reset through
  Play support, but that takes days.
- [ ] **Step 3: Wire signing.** In `app/build.gradle.kts` (top of file):
  ```kotlin
  import java.util.Properties

  // Upload key (ADR-0017): environment variables first, then local.properties; never in git.
  val localProperties = Properties().apply {
      rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
  }
  fun signingValue(key: String): String? =
      providers.environmentVariable(key).orNull ?: localProperties.getProperty(key)
  val uploadStoreFile = signingValue("SUMMON_UPLOAD_STORE_FILE")
  val hasUploadKey = listOf(
      "SUMMON_UPLOAD_STORE_FILE", "SUMMON_UPLOAD_STORE_PASSWORD",
      "SUMMON_UPLOAD_KEY_ALIAS", "SUMMON_UPLOAD_KEY_PASSWORD",
  ).all { !signingValue(it).isNullOrBlank() }
  ```
  Inside `android { … }`:
  ```kotlin
  signingConfigs {
      if (hasUploadKey) {
          create("release") {
              storeFile = file(uploadStoreFile!!)
              storePassword = signingValue("SUMMON_UPLOAD_STORE_PASSWORD")
              keyAlias = signingValue("SUMMON_UPLOAD_KEY_ALIAS")
              keyPassword = signingValue("SUMMON_UPLOAD_KEY_PASSWORD")
          }
      }
  }
  ```
  and in `buildTypes.release { … }`:
  ```kotlin
  if (hasUploadKey) {
      signingConfig = signingConfigs.getByName("release")
  } else {
      logger.warn("Release is UNSIGNED: set SUMMON_UPLOAD_* (env or local.properties), see ADR-0017.")
  }
  ```
  Debug builds and CI without secrets must still configure and build. `file()` resolves
  relative to `app/`, so a `~` path will not expand: document that values are absolute
  paths.
- [ ] **Step 4:** Document the four keys with example values (absolute path, fake
  passwords) in ADR-0017 → "How to build a release", including the `local.properties`
  form:
  ```properties
  SUMMON_UPLOAD_STORE_FILE=/Users/me/.android/summon-upload.jks
  SUMMON_UPLOAD_STORE_PASSWORD=…
  SUMMON_UPLOAD_KEY_ALIAS=summon-upload
  SUMMON_UPLOAD_KEY_PASSWORD=…
  ```
- [ ] **Step 5:** Check both modes:
  - No secrets: `./gradlew assembleDebug bundleRelease` succeeds and prints the
    UNSIGNED warning.
  - Throwaway key via env vars: `./gradlew bundleRelease`, then
    `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab`
    reports `jar verified` and the throwaway DN.
  Gate. Commit:
  `git commit -m "build(release): Upload-key signing config from env or local.properties" -m "$TRAILER"`

### Task 7: Check and finish R8 / resource shrinking

**Files:** possibly `app/build.gradle.kts`, `app/proguard-rules.pro`.

Current state: `buildTypes.release.optimization { enable = true; packageScope =
setOf("androidx.**", "kotlin.**", "kotlinx.**") }` (AGP 9 DSL). There is no
`proguard-rules.pro`.

- [ ] **Step 1: Learn what the DSL does.** Look up AGP 9.4's `optimization` block (AGP
  release notes or the DSL reference in the AGP jar's sources). Answer three questions:
  Does `enable` turn on R8 code shrinking/obfuscation **and** resource shrinking? Does
  `packageScope` limit optimization to those packages, leaving
  `com.summondemons.safely.**` unoptimized? Is that intended? Write the answers into
  ADR-0017 → "Shrinking". If the app's own code is excluded by accident, widen the scope
  (or remove `packageScope`) so app code is optimized too, and turn resource shrinking
  on if `enable` doesn't already.
- [ ] **Step 2: Build and inspect.**
  ```bash
  ./gradlew clean bundleRelease
  ls app/build/outputs/mapping/release/   # mapping.txt, usage.txt, seeds.txt
  ```
  Check:
  - `mapping.txt` exists and is inside the AAB:
    `unzip -l app/build/outputs/bundle/release/app-release.aab | grep -i mapping`
    (expect `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`).
  - Resource shrinking kept every resource the app loads: all `R.raw.*` (sigils,
    sounds) and `drawable-nodpi/*` are referenced statically, so they must survive.
    `unzip -l` the AAB and confirm `res/raw/sigil_*.txt`, every `.wav`, and every
    `drawable-nodpi` WebP are present.
  - Record the AAB size before/after in the PR description.
- [ ] **Step 3: Keep rules only if needed.** Libraries in use ship their own consumer
  rules (kotlinx-serialization ≥ 1.5, Navigation type-safe routes, DataStore). Add
  `app/proguard-rules.pro`, wired with `proguardFiles(...)` or the AGP 9 equivalent,
  **only** for a crash you have reproduced in Step 4. Each rule needs a comment
  explaining why.
- [ ] **Step 4: Release smoke test.** R8 bugs show up only at runtime. Install the release
  build on a device or emulator:
  ```bash
  # bundletool from https://github.com/google/bundletool/releases
  java -jar bundletool.jar build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
    --output=/tmp/summon.apks --local-testing --connected-device <signing flags for the throwaway key>
  java -jar bundletool.jar install-apks --apks=/tmp/summon.apks
  ```
  Walk through: cold start (splash → Home), Grimoire (both demons, swipe, badges,
  dossier), Ritual (mic permission prompt, chant), climax → celebration, kill and
  relaunch (progress restored: this exercises kotlinx-serialization through R8), and
  confirm the Debug chip is **absent**. Any crash: `adb logcat`, retrace with
  `retrace mapping.txt`, fix with a minimal keep rule (Step 3). With no device, record
  "release smoke test pending (owner, Task 9)".
- [ ] **Step 5:** Gate. Commit (if anything changed):
  `git commit -m "build(release): Verify R8 and resource shrinking for release" -m "$TRAILER"`

### Task 8: Release hygiene

**Files:** `app/build.gradle.kts`, `AndroidManifest.xml`, `docs/`.

- [ ] **Step 1: Version.** Set `versionCode = 1`, `versionName = "1.0.0"` (ADR-0017).
- [ ] **Step 2: Merged-manifest audit.** After `bundleRelease`, inspect
  `app/build/intermediates/merged_manifests/release/**/AndroidManifest.xml` (or
  `aapt2 dump xmltree` on the base module). Required:
  - permissions are exactly `RECORD_AUDIO` and `VIBRATE` (plus any library-added
    `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which is fine). **No
    `android.permission.INTERNET`**: the privacy policy depends on it. If a library adds
    it, remove it with `tools:node="remove"` and record that in ADR-0017.
  - `android:debuggable` absent/false; `MainActivity` is the only exported component.
- [ ] **Step 3: Lint for release.** `./gradlew lintRelease` (or `lintVitalRelease`, which
  `bundleRelease` runs). Fix every **error**. Review warnings about
  `LockedOrientationActivity`, `DiscouragedApi` (orientation on large screens) and
  `MonochromeLauncherIcon`, and either fix them or suppress them in
  `app/lint.xml` with a comment pointing to ADR-0017.
- [ ] **Step 4: Release-only code paths.** `grep -rn "BuildConfig.DEBUG" app/src/main`:
  the Debug sheet, debug chip, Ritual debug controls and `DataStoreDebugSettingsStore`
  must all be gated (they are today; confirm nothing new leaked in).
- [ ] **Step 5: Store graphics folder.** Create `docs/play-store/README.md` listing each
  asset, its spec (copy from `docs/STORE_LISTING.md` → Graphics checklist) and how to
  regenerate it. Create an empty `docs/play-store/screenshots/.gitkeep`.
- [ ] **Step 6:** Gate. Commit:
  `git commit -m "chore(release): Version 1.0.0, manifest audit, release lint" -m "$TRAILER"`

---

## Phase 4: Final verification and owner handoff

### Task 9: Final build and owner checklist

- [ ] **Step 1: Final build.**
  ```bash
  ./gradlew clean testDebugUnitTest lint bundleRelease
  ```
  Expected `BUILD SUCCESSFUL`, no lint errors, and
  `app/build/outputs/bundle/release/app-release.aab` present. If the upload key is
  configured, `jarsigner -verify` passes. Otherwise the build is unsigned, which is
  expected in CI or cloud, and the owner signs it with the real key.
- [ ] **Step 2: Static checks.**
  ```bash
  git diff --stat origin/main -- reference/      # must be empty
  git ls-files | grep -Ei '\.(jks|keystore)$'    # must be empty
  grep -rn "<CONTACT_EMAIL>\|<DEVELOPER_NAME>" docs/   # lists what the owner must fill
  ```
- [ ] **Step 3: Open a PR** (only if the user asked for one) with: the AAB size, the
  merged-manifest permission list, the answers from Task 7 Step 1, and the owner
  checklist below **verbatim**.
- [ ] **Step 4: Owner checklist** (these steps need a person, a real device and the Play
  Console account; the agent cannot do them):
  1. Fill `<DEVELOPER_NAME>` and `<CONTACT_EMAIL>` in `docs/PRIVACY_POLICY.md` and
     `docs/STORE_LISTING.md`.
  2. **Host the privacy policy at a public URL.** Play needs a reachable, non-PDF
     page, e.g. GitHub Pages, or the GitHub file view if the repo is public. Paste it
     into Console → App content → Privacy policy.
  3. Generate the real upload key (Task 6 Step 2) on your own machine, back it up, set
     the `SUMMON_UPLOAD_*` values, and run `./gradlew bundleRelease`.
  4. If Task 4/7 recorded pending checks: install the release build on a phone and run
     the smoke test (Task 7 Step 4) and the icon checks (Task 4 Step 4).
  5. Capture 4–8 portrait screenshots (1080 × 1920) on a device, following
     `docs/STORE_LISTING.md` → screenshot order, into `docs/play-store/screenshots/`.
  6. Play Console: create the app (Game, Free). Store listing from
     `docs/STORE_LISTING.md`. Upload `icon-512.png` and `feature-graphic.png`. App
     content: privacy policy, Ads = No, Data safety = no data collected, content rating
     questionnaire, target audience 13+, App access = all features available without
     login.
  7. **Testing requirement:** personal developer accounts created after 2023-11-13 must
     run a **closed test with at least 12 testers opted in for 14 continuous days**
     before they can apply for production access. Upload the AAB to a closed track
     first. Organisation accounts can go to production directly.
  8. Upload the AAB, opting in to Play App Signing (the default), and roll out.
