# Compose port — design

- Date: 2026-10-15
- Status: Approved
- Builds on: `PRODUCT.md`, `DESIGN.md`, ADR-0001…0006
- Input (non-canonical): `reference/src/*.kt`, `reference/stitch/`

## Goal

Replace the Android Studio "Basic Views Activity" template in `app/` with a
Jetpack Compose implementation of the game described in `PRODUCT.md`, styled per
`DESIGN.md`, using the draft code in `reference/src/` as a starting point only.
Where the draft disagrees with canon, canon wins. Known disagreements:

| Draft | Canon (used) |
|---|---|
| Asmodeus unlocked by default | Both demons start locked |
| Asmodeus chant split 1 / 2 / 1 / 1 phrases | PRODUCT.md split (exactly 4 phrases) |
| Astaroth chant has 3 phrases | 4 phrases (ADR-0005) |
| Astaroth description / quote differ | PRODUCT.md text |
| Stats, power, level, synergy, LV badge | Non-goals — dropped |
| Hard-coded North (340°–20°) | `ritualHeading` per demon (ADR-0006) |
| Crimson `RibbonBanner`, hex colors in UI | Theme tokens only (DESIGN.md) |
| Sigil opacity starts at 0.15 | 0.10 |

## Decisions taken during brainstorming

| Topic | Decision |
|---|---|
| State | One activity-scoped `DemonViewModel` (catalog + unlocked set) plus a `RitualViewModel` per ritual entry. Manual construction via `viewModelFactory`; no DI framework. |
| Heading | `TYPE_ROTATION_VECTOR`, fallback to accelerometer + magnetometer; sin/cos low-pass smoothing. |
| Navigation | Type-safe navigation-compose routes (`@Serializable`). |
| Tada | Placeholder synthesized by a stdlib-only Python script, committed as `res/raw/tada.wav`. |
| Launcher icon | Adaptive + legacy icons from `ic_game_logo`. |
| Tests | JVM unit tests on logic only; template instrumented test deleted. |
| Locked card | Card art through a near-black color filter + lock glyph. |

## Build and packaging

- `namespace` / `applicationId` / Kotlin packages: `com.summondemons.safely` (ADR-0002).
  `rootProject.name` becomes `"Summon Demons Safely"`.
- AGP 9.4.1 ships built-in Kotlin 2.2.10, so the Kotlin plugins are pinned to the
  same version: `org.jetbrains.kotlin.plugin.compose` and
  `org.jetbrains.kotlin.plugin.serialization` at `2.2.10`.
- `gradle/libs.versions.toml` gains: Compose BOM `2026.09.00` (ui, ui-graphics,
  ui-tooling-preview, material3, ui-tooling debug-only), activity-compose `1.13.0`,
  navigation-compose `2.10.2`, lifecycle-viewmodel-compose and
  lifecycle-runtime-compose `2.11.0`, kotlinx-coroutines-test `1.11.0` (test).
  If a library fails with a Kotlin metadata-version error, step it down to the
  newest release that compiles with Kotlin 2.2.10 and record that in the plan's
  task notes.
- Removed: appcompat, material (MDC), constraintlayout, navigation-fragment-ktx,
  navigation-ui-ktx, espresso, androidx-junit, `viewBinding`.
- `buildFeatures { compose = true; buildConfig = true }` (`BuildConfig.DEBUG` gates
  the debug controls).
- Manifest: `RECORD_AUDIO`, `VIBRATE`; `MainActivity` (a `ComponentActivity`)
  `android:screenOrientation="portrait"`; label "Summon Demons Safely".
- `res/values/themes.xml`: one `Theme.SummonDemonsSafely` with parent
  `android:Theme.Material.NoActionBar`, `android:windowBackground` = `#0b1326`,
  dark status/navigation bars. `values-night/`, `values-v23/`, `dimens*`,
  `colors.xml` template entries, layouts, menu and `navigation/nav_graph.xml` are
  deleted. `MainActivity` calls `enableEdgeToEdge()`.

## Architecture

```
com.summondemons.safely
├── MainActivity.kt                 setContent { SummonDemonsSafelyTheme { AppNavHost() } }
├── data/
│   ├── Demon.kt                    Demon, DemonAccent
│   └── DemonCatalog.kt             the two MVP demons
├── navigation/
│   ├── Routes.kt                   @Serializable Grimoire, Ritual(demonId), Profile(demonId, isNewSummon)
│   └── AppNavHost.kt
├── ritual/
│   ├── HeadingMath.kt
│   ├── RitualEngine.kt             pure state machine
│   ├── RitualConfig.kt             tuning constants
│   ├── HeadingSource.kt            interface
│   ├── VolumeSource.kt             interface
│   └── RitualViewModel.kt
├── platform/
│   ├── AndroidHeadingSource.kt
│   ├── AndroidVolumeSource.kt
│   ├── Haptics.kt
│   └── TadaPlayer.kt
└── ui/
    ├── DemonViewModel.kt
    ├── theme/  Color.kt, Theme.kt, Type.kt, Shape.kt, Spacing.kt, DemonColors.kt
    ├── components/  PrimaryActionButton, DemonCardBase, RibbonBanner,
    │                RotatingSunburstBackground, SigilView, CompassIndicator, DebugControls
    └── screens/  GrimoireScreen.kt, RitualScreen.kt, ProfileScreen.kt
```

Dependency direction: `ui` → `ritual` → `data`; `platform` implements `ritual`
interfaces; nothing in `ritual` or `data` imports Android UI or Compose (except
`@DrawableRes` annotations in `Demon`).

### Data

```kotlin
enum class DemonAccent { Asmodeus, Astaroth }

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val quote: String,
    val accent: DemonAccent,
    val ritualHeading: Float,          // compass degrees, [0, 360)
    val chantPhrases: List<String>,    // exactly 4
    @DrawableRes val cardArt: Int,
    @DrawableRes val profileArt: Int,
    @DrawableRes val sigil: Int?,      // null → Canvas fallback
) { init { require(chantPhrases.size == 4); require(ritualHeading in 0f..<360f) } }
```

- `DemonCatalog.all: List<Demon>` holds Asmodeus and Astaroth with PRODUCT.md text
  verbatim (including "LOVIS"). Both `ritualHeading = 0f`.
- Art mapping: Asmodeus `cardArt = demon_asmodeus_card`,
  `profileArt = demon_asmodeus_full`, `sigil = null`; Astaroth
  `cardArt = profileArt = demon_astaroth_card`, `sigil = sigil_astaroth`.
  When the DESIGN.md gaps are filled, only the catalog changes.
- `DemonColors` (CompositionLocal) exposes `asmodeus`, `astaroth`, `ritualSafe`
  and `fun accent(a: DemonAccent): Color`. The `when` over `DemonAccent` lives
  there only; UI code never branches on `id`.
- A `headingLabel(degrees)` helper maps to the nearest of 8 compass names
  ("North", "North-East", …) for the instruction text.

### DemonViewModel (activity-scoped)

- `demons: StateFlow<List<DemonEntry>>` where `DemonEntry(demon, unlocked)`.
- `unlocked` is a `MutableStateFlow<Set<String>>`, initially empty; in-memory only.
- `fun demon(id): Demon?`, `fun unlock(id)`.
- Obtained in `AppNavHost` via `viewModel()` scoped to the activity and passed to
  screens as state + lambdas (screens do not take the ViewModel itself, except
  `RitualScreen` which takes its own `RitualViewModel`).

### Ritual core

**HeadingMath**
- `angularDistance(a, b)`: smallest absolute difference on the circle, in [0, 180].
- `isAligned(azimuth, target, tolerance = RitualConfig.HEADING_TOLERANCE_DEG)`:
  `angularDistance ≤ tolerance`. For target 0 this is [340, 360) ∪ [0, 20].

**RitualConfig**
- `HEADING_TOLERANCE_DEG = 20f`, `PHRASE_SUSTAIN_MS = 2000L`,
  `TICK_MS = 100L`, `SILENCE_DECAY_FACTOR = 0.5f`, `VOLUME_RMS_THRESHOLD = 1800.0`,
  `SUCCESS_HOLD_MS = 800L`, `OPACITY_STEPS = [0.10, 0.25, 0.50, 0.75, 1.00]`.

**RitualEngine** (pure, no coroutines)
```kotlin
data class RitualProgress(val phraseIndex: Int = 0, val sustainedMs: Long = 0) {
    val completed get() = phraseIndex >= 4
    val sigilOpacity get() = OPACITY_STEPS[phraseIndex]
    val sustainFraction get() = sustainedMs / PHRASE_SUSTAIN_MS.toFloat()
}
fun step(p: RitualProgress, aligned: Boolean, loud: Boolean, dtMs: Long): RitualProgress
```
Rules, in order:
1. `p.completed` → return `p` unchanged.
2. `!aligned` → `RitualProgress()` (reset to 0).
3. `loud` → `sustainedMs += dtMs`; when `≥ PHRASE_SUSTAIN_MS`, `phraseIndex + 1` and
   `sustainedMs = 0`.
4. otherwise → `sustainedMs = max(0, sustainedMs − dtMs × SILENCE_DECAY_FACTOR)`.

Phrase `i` (0-based) is "reached" when `i < phraseIndex`; reached phrases render
in the demon accent, the rest grey. The current phrase (`i == phraseIndex`) gets
bold weight and, while aligned, the accent at reduced alpha so the player knows
what to chant next.

**HeadingSource / VolumeSource**
```kotlin
fun interface HeadingSource { fun azimuths(): Flow<Float> }   // degrees [0,360)
fun interface VolumeSource  { fun rmsLevels(): Flow<Double> } // one value per ~TICK_MS window
```
- `AndroidHeadingSource(context)`: `callbackFlow`; registers
  `TYPE_ROTATION_VECTOR` at `SENSOR_DELAY_UI`; if absent, accelerometer +
  magnetometer via `getRotationMatrix`. Azimuth from `getOrientation`, normalized
  to [0, 360). Smoothing: exponential low-pass on the unit vector
  (`sin`, `cos`, α = 0.15) to avoid the 359°→0° jump. `awaitClose` unregisters.
  If no usable sensor exists the flow emits nothing (UI shows "--°"; debug
  Simulate heading still works).
- `AndroidVolumeSource()`: `flow { }` on `Dispatchers.IO`; `AudioRecord` MIC,
  16 kHz, mono, PCM16; reads `16000 × TICK_MS / 1000` samples per window, emits
  RMS. `release()` in `finally`. Requires the permission to be already granted;
  a `SecurityException` or `STATE_UNINITIALIZED` ends the flow quietly (the VM
  treats an ended mic flow as "silent").

**RitualViewModel**
- Constructor: `(savedStateHandle, catalog lookup, headingSource, volumeSource)`;
  `demonId` from `savedStateHandle.toRoute<Ritual>()`. Built with a
  `viewModelFactory { initializer { … } }` in `AppNavHost`; sources created from
  the application context.
- Inputs (MutableStateFlows): `micGranted: Boolean?` (null = not checked yet),
  `simulateHeading: Boolean`, `holdToChant: Boolean`.
- Pipeline (the only place the ritual runs):
  - `azimuth`: `simulateHeading` → `flowOf(demon.ritualHeading)`, else
    `headingSource.azimuths()`; held as latest value (`null` until first reading).
  - `loudTicks`: `micGranted == true` → `volumeSource.rmsLevels().map { it > THRESHOLD }`
    followed by an infinite `false` ticker if the mic flow ends; else a
    `TICK_MS` ticker emitting `false`. Combined with `holdToChant` (`loud || hold`).
    `flatMapLatest` on `micGranted` only — heading changes never restart the mic.
  - Each tick: `progress = step(progress, isAligned(azimuth, heading), loud, TICK_MS)`
    via `scan`.
  - `uiState = combine(...) .stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
    Collected with `collectAsStateWithLifecycle`, so sensors and mic stop when the
    screen is not visible.
- `RitualUiState(demon, azimuth: Float?, aligned, progress, micGranted,
  simulateHeading, holdToChant, completed: Boolean)`.
  `completed` becomes true `SUCCESS_HOLD_MS` after `progress.completed` (the sigil
  shows full glow during the hold) and stays true; ticking stops after completion.
- All state is StateFlow-based; nothing writes Compose state from a background
  thread.

### Screens

**AppNavHost**: start destination `Grimoire`.
- Grimoire → locked card **Summon** → `Ritual(id)`; unlocked card **View** →
  `Profile(id, false)`.
- Ritual success → `demonViewModel.unlock(id)`, navigate `Profile(id, true)` with
  `popUpTo<Grimoire>()`; **Abort** → `popBackStack()`.
- Profile **Awesome!** / **Back** → `popBackStack<Grimoire>(inclusive = false)`.

**GrimoireScreen**: `Scaffold` + `TopAppBar` titled "My Grimoire" with a small
`ic_game_logo` avatar; subtitle line; `LazyVerticalGrid(GridCells.Fixed(2))` of
`DemonCardBase`. Locked: `surfaceContainer` with grey tint, card art through
`ColorFilter.tint(near-black, SrcAtop)` at reduced alpha, lock glyph, name, button
**Summon**. Unlocked: accent border + faint accent tint, full art, name, title,
button **View**.

**RitualScreen**
- Mic permission: on entry, `checkSelfPermission`; only if not granted, launch the
  request. Result → `vm.onMicPermission(granted)`. If denied: an inline card
  explaining the mic is used only to measure chant loudness, with **Retry**
  (re-launch) — or **Open settings** when `shouldShowRequestPermissionRationale`
  is false after a denial. The ritual remains usable via debug controls in debug
  builds.
- Layout (fixed `horizontalAlignment = CenterHorizontally` bug): header card with
  "SUMMONING RITUAL", demon name, "Hold the phone with both hands and face
  {headingLabel(demon.ritualHeading)}" and `CompassIndicator`; centre `SigilView`
  with animated opacity; chant card with 4 phrases and a sustain
  `LinearProgressIndicator`; `DebugControls` (only when `BuildConfig.DEBUG`);
  **Abort Ritual** button.
- On `completed`: `Haptics.success(context)` (waveform 0/80/60/140 ms, API-gated as
  in the draft) then `onSummoned(id)`, inside `LaunchedEffect(completed)`.

**ProfileScreen**: rotating sunburst in the accent; if `isNewSummon`:
`RibbonBanner("New Demon Received!")` and `TadaPlayer` plays once
(`LaunchedEffect(Unit)` + release in `DisposableEffect`); bouncing `profileArt`
(spring scale 0 → 1); name (display), title, description, quote; bottom-pinned
`PrimaryActionButton` "Awesome!" (new) or "Back" (view).

### Components

| Component | Notes vs draft |
|---|---|
| `PrimaryActionButton(text, onClick, modifier, enabled)` | Pill shape from theme, `primaryContainer`/`onPrimaryContainer`; disabled colors from scheme; press scale kept. A `tone` param (`Primary`/`Danger`) covers Abort. |
| `DemonCardBase(locked, accent, modifier, content)` | Border/tint per DESIGN.md. |
| `RibbonBanner(text)` | `primaryContainer` (not crimson). |
| `RotatingSunburstBackground(color)` | Ported as-is, tokens only. |
| `SigilView(sigil: Int?, opacity, accent)` | `Image` with `alpha` when `sigil != null`, else ported `SigilAltarCanvas`. Accent glow behind. |
| `CompassIndicator(azimuth: Float?, target, aligned)` | Arrow pointing to the target relative to the current azimuth (`target − azimuth`), `ritualSafe` when aligned else `error`; readout "{az}° / {label}". |
| `DebugControls(simulateHeading, onToggleSimulate, onHoldChange)` | Toggle "Simulate heading"; **Hold to Chant** is press-and-hold (`pointerInput` + `awaitRelease`). |

### Theme

- `Color.kt`: one `val` per DESIGN.md color token; `Theme.kt` maps every token to
  its `darkColorScheme` slot (every non-`demon-*`, non-`ritual-safe` key in the DESIGN.md frontmatter). Dark only.
- `Type.kt`: DESIGN.md scale on `FontFamily.Serif` (Cinzel fallback) and
  `FontFamily.SansSerif` (Inter fallback), mapped to M3 slots
  (`displaySmall`, `headlineLarge`, `headlineMedium`, `titleLarge`, `bodyLarge`,
  `bodyMedium`, `bodySmall`, `labelMedium`, `labelSmall`); body/label numbers use
  `fontFeatureSettings = "tnum"`. No colors baked into text styles.
- `Shape.kt`: small 12, medium 16, large 24 dp; `Pill = RoundedCornerShape(50)`.
- `Spacing.kt`: `Spacing(xs 4, sm 8, md 12, lg 16, xl 24)` via `LocalSpacing`,
  read as `MaterialTheme.spacing` extension.
- `DemonColors.kt`: `LocalDemonColors`, read as `MaterialTheme.demonColors`.

## Assets

- PNGs copied from `reference/stitch/` to `app/src/main/res/drawable-nodpi/` under
  the DESIGN.md catalog names (`nodpi` because 1024 px sources in `drawable/`
  would be upscaled 3× on xxhdpi). Only the drawables the catalog uses:
  `ic_game_logo`, `avatar_asmodeus`, `demon_asmodeus_full`, `demon_asmodeus_card`,
  `demon_astaroth_card`, `sigil_astaroth`. (`avatar_astaroth` is skipped until
  regenerated — gap 4 — and avatars are not used by any screen yet.)
- Launcher: `mipmap-anydpi-v26/ic_launcher(_round).xml` adaptive icon with
  background color `#0b1326` and foreground `<inset>` of `ic_game_logo`
  (inset 18 % so the baked logo stays in the safe zone); legacy
  `mipmap-*/ic_launcher(_round).png` at 48/72/96/144/192 px generated with `sips`.
  Template `.webp` icons and `ic_launcher_background/foreground.xml` deleted.
- Tada: `tools/gen_tada.py` (Python stdlib only: `wave`, `math`, `struct`)
  synthesizes a ~1.2 s rising arpeggio + chord (22.05 kHz mono PCM16, fade-out),
  output `app/src/main/res/raw/tada.wav`. WAV instead of the `.ogg` in DESIGN.md
  because no ogg encoder is available; size ≈ 55 KB. Original work, CC0.

## Docs updated by the port

- `AGENTS.md` → "Current state": Compose app ported; next steps are the DESIGN.md
  art gaps.
- `DESIGN.md` → asset catalog notes `drawable-nodpi/`; "Non-Stitch gap" records
  the placeholder `tada.wav`, its generator script and CC0 status.
- `PRODUCT.md` → Chant row: silence between syllables decays progress at half rate
  instead of resetting it; Permissions row: no re-request when already granted;
  add "Portrait only".
- New `docs/decisions/0007-chant-sustain-tolerance.md`: why silence decays rather
  than resets, the RMS 1800 threshold, and that these are global tuning constants
  (not demon data).

## Error handling

| Case | Behavior |
|---|---|
| Mic permission denied | Rationale card + Retry / Open settings; no crash; debug controls still work. |
| `AudioRecord` fails to init / `SecurityException` | Mic flow ends; treated as silence. |
| No heading sensor | No azimuth; compass shows "--°"; never aligned unless simulated. |
| Unknown `demonId` in route | `RitualUiState.demon` / Profile lookup is null; the screen pops back to Grimoire. |
| Tada resource fails to play | Caught and ignored; visuals unaffected. |

## Testing

JVM unit tests (`app/src/test`, JUnit 4 + kotlinx-coroutines-test):

- `HeadingMathTest` — wrap-around distances, tolerance edges (340, 20, 20.1, 339.9)
  for target 0, and a non-North target (e.g. 180).
- `RitualEngineTest` — 20 loud ticks advance one phrase; leaving alignment resets;
  silence decays at half rate and never below 0; 4 phrases complete; step after
  completion is a no-op; opacity per phrase index.
- `DemonCatalogTest` — unique ids, 4 phrases each, headings in range, PRODUCT.md
  strings present.
- `DemonViewModelTest` — starts all locked; `unlock` flips only that demon.
- `RitualViewModelTest` — fake `HeadingSource`/`VolumeSource` with
  `StandardTestDispatcher` + `Dispatchers.setMain`; loud and aligned for 8 s
  of virtual time → `completed`; turning away resets; simulate heading aligns
  without a heading; hold-to-chant counts as loud with mic denied; granting the
  mic once does not resubscribe it when the heading changes.

The template `ExampleUnitTest` and `ExampleInstrumentedTest` are deleted. No
instrumented/UI tests in this pass.

**Done when:** `./gradlew assembleDebug testDebugUnitTest` passes, no file under
`app/` references `com.example`, and the manual checklist in the plan has been
run on a device or emulator (with the debug controls on an emulator).

## Out of scope

Everything under PRODUCT.md "Non-goals"; bundling Cinzel/Inter fonts; filling the
DESIGN.md art gaps; persistence; UI tests; release signing.
