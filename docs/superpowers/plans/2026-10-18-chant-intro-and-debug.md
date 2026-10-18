# Chant Intro and Debug Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Device-test fixes to the chant screen (ADR-0021):
- the "How to chant" card shows before every ritual and replaces the turn-sideways hint;
- the card's ✕ (and back) start the chant instead of leaving;
- "?" moves from the top-right corner to a round button centred above the demon's name, with a round dashed "D" beside it in debug builds;
- "D" opens the debug controls in a small floating panel, closed by default, that never takes layout space;
- "FIND THE DIRECTION" / "HOLD STEADY" go, so before the lock the line under the phrase is only "I of IV".

**Architecture:**
- `chantScreenState(...)` (pure, JVM-tested) maps seeking and locking to a new `VerdictLine.Count`; `VerdictLine` then draws no label, only the count.
- `RitualViewModel` loses the "seen once" plumbing; `AppNavHost` passes `showIntro = true`.
- `RitualScreen` wires the card's ✕ and back to `dismissIntro`.
- `ChantScreen` gets a `SlabButtons` row (`RoundButton` × 2) above the name. The debug slot moves from under the verdict into a `Popup` anchored under that row.
- `DebugControls` is rewritten as compact chips.
- No engine or rule changes.

**Tech Stack:** unchanged. **No new dependency, no new art.** Decision record: `docs/decisions/0021-intro-every-ritual-and-slab-buttons.md`; read it first. The approved prototype (v12) is the visual reference: the "?" and "D" side by side above ASMODEUS, and the panel under them.

**Working directory:** the repo root. All paths are relative to it. Do not edit anything
under `reference/`.

**Commit trailer:** before starting, set `TRAILER` to the attribution trailer your
session requires (the earlier plans end every commit with a `Co-Authored-By:` line):

```bash
TRAILER='Co-Authored-By: <your agent attribution>'
```

Every commit command below passes it as the second `-m`.

**Path shorthand** (for reading only; commands spell out full paths):
- `MAIN` = `app/src/main/java/com/summondemons/safely`
- `TEST` = `app/src/test/java/com/summondemons/safely`

**Verification gate** (run at the end of every task that says "gate"):

```bash
./gradlew testDebugUnitTest lint assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Lint warnings are allowed; lint errors are not.

**Notes for the implementer**
- **Verified before this plan was written:** every "replace … with …" edit below was applied, in order, to a copy of `main` at `c11327f`, and each one matched exactly once (69 edits). `ChantScreenState.kt` and `ChantScreenStateTest.kt` after Task 2 were compiled with Kotlin 2.2.10 and run with the unchanged `RitualEngineTest` and `ChantCheckTest`: 52 tests green.
- **Not compiled:** the Compose edits of Tasks 2–4 and the new `DebugControls.kt` (no Android SDK at hand). Besides APIs already in these files, they use `Popup` / `PopupPositionProvider` / `PopupProperties` (`androidx.compose.ui.window`), `PathEffect.dashPathEffect`, `BorderStroke` and `LocalDensity`. If a signature differs, keep the behaviour, adapt the call and say so in the commit message.
- An edit step quotes the exact current text. If it doesn't match exactly once, stop and re-read the file instead of guessing. Apply the steps in order, because later edits to `ChantScreen.kt` assume the earlier ones. A whole-file step means "replace the file with exactly this".
- `chantIntroSeen` leaves `Progress`. `ProgressJson` already ignores unknown keys, so saves that still carry it load fine; a test covers it.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `docs/decisions/0021-intro-every-ritual-and-slab-buttons.md` | Exists | The decision (already committed) |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0019-chant-screen.md`, `docs/decisions/0020-direction-in-the-sigil.md` | Modify | Canon |
| `MAIN/ui/screens/ChantScreenState.kt`, `TEST/ui/screens/ChantScreenStateTest.kt` | Modify | `VerdictLine.Count` before the lock |
| `MAIN/ui/screens/ChantScreen.kt` | Modify | Count-only verdict line; hint removed; `SlabButtons`, `RoundButton`, debug `Popup`; top-right keeps only ✕ |
| `MAIN/ui/screens/RitualScreen.kt` | Modify | Card's ✕ and back → `dismissIntro`; no `showTurnHint`; no "?" in the climax |
| `MAIN/ui/screens/ChantIntro.kt` | Modify | KDoc; ✕ content description |
| `MAIN/ritual/RitualViewModel.kt`, `TEST/ritual/RitualViewModelTest.kt` | Modify | No `introOffered` / `onIntroSeen` |
| `MAIN/navigation/AppNavHost.kt` | Modify | `showIntro = true` |
| `MAIN/ui/GameViewModel.kt`, `MAIN/progress/Progress.kt`, `TEST/progress/ProgressJsonTest.kt` | Modify | `chantIntroSeen` removed |
| `MAIN/ui/components/DebugControls.kt` | Replace | Compact chips for the floating panel |
| `app/src/main/res/values/strings.xml` | Modify | Strings removed: two verdict lines and the hint's two. Added: `intro_close`, `debug_controls` |

---

## Phase 1 — Canon

### Task 1: PRODUCT.md, DESIGN.md, AGENTS.md, ADR-0019, ADR-0020

**Files:** Modify: `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0019-chant-screen.md`, `docs/decisions/0020-direction-in-the-sigil.md`

- [ ] **Step 1: Edit `PRODUCT.md`**

(a) Replace:

```markdown
Below it: "← FACE EAST" (neutral) until he is faced; under the phrase: "FIND THE DIRECTION", then "HOLD STEADY" while locking.
```

with:

```markdown
Below it: "← FACE EAST" (neutral) until he is faced; under the phrase, only the count "I of IV" until the lock (ADR-0021).
```

(b) Replace:

```markdown
On entering, a short hint asks to turn the phone sideways and keep the microphone clear.
```

with:

```markdown
Before every ritual the "How to chant" card comes down over it (ADR-0021): four generic rules and **SUMMON**; **SUMMON**, its ✕ and back all send it up and start the chant (the microphone is ignored while it is up). A round "?" above the demon's name reopens it.
```

(c) Replace:

```markdown
| Debug aids | Ritual: "Simulate heading"
```

with:

```markdown
| Debug aids | Ritual, in a small floating panel behind a round dashed "D" next to the "?" (closed by default, takes no space; ADR-0021): "Simulate heading"
```

- [ ] **Step 2: Edit `DESIGN.md`**

(a) Replace:

```markdown
| `ChantScreen` | Landscape chant screen (ADR-0019) | `state`, `flash`, `showClose`, `onClose`, `debug`; House-stage backdrop (scrim strongest behind the words), slab (surface-container-low 70 %, `large` shape) with name, `ChantSigil` and the direction line;
```

with:

```markdown
| `ChantScreen` | Landscape chant screen (ADR-0019) | `state`, `flash`, `showClose`, `onClose`, `debug`, `onHelp`, `blocker`; House-stage backdrop (scrim strongest behind the words), slab (surface-container-low 70 %, `large` shape) with a row of round buttons (36 dp disc, 48 dp target: "?" outline; "D" dashed outline in debug builds, primary fill while its panel is open; ADR-0021), name, `ChantSigil` and the direction line;
```

(b) Replace:

```markdown
verdict line (`ritualCue`) with `VoiceMeter`. Portrait
```

with:

```markdown
verdict line (`ritualCue`) with `VoiceMeter` (only the count before the lock). Portrait
```

(c) Replace:

```markdown
| `DebugChip` | Opens the debug sheet (debug builds only) | `onClick` |

```

with:

```markdown
| `DebugChip` | Opens the debug sheet (debug builds only) | `onClick` |
| `DebugControls` | The chant screen's debug panel (debug builds only) | Simulate heading, Hold to Chant and Record clip chips plus the last chant-check score, `labelSmall`; shown in a 300 dp popover (surface-container-lowest, outline-variant border, `medium` shape) under "D", never in the layout (ADR-0021) |

```

- [ ] **Step 3: Edit `AGENTS.md`**

Replace:

```markdown
  its sigil guides to the direction: ADR-0020, plan
  `docs/superpowers/plans/2026-10-18-chant-direction.md`.
```

with:

```markdown
  its sigil guides to the direction: ADR-0020, plan
  `docs/superpowers/plans/2026-10-18-chant-direction.md`. Intro card before every ritual,
  "?"/"D" buttons on the slab and the floating debug panel: ADR-0021, plan
  `docs/superpowers/plans/2026-10-18-chant-intro-and-debug.md`.
```

- [ ] **Step 4: Edit `docs/decisions/0019-chant-screen.md`**

Replace:

```markdown
  whole Ritual and its sigil carries the direction marker and the lock fill.
```

with:

```markdown
  whole Ritual and its sigil carries the direction marker and the lock fill. Amended by
  ADR-0021: the intro card shows before every ritual and replaces the turn-sideways hint,
  its ✕ starts the chant, "?" moves above the name, and the debug controls move to a
  floating panel.
```

- [ ] **Step 5: Edit `docs/decisions/0020-direction-in-the-sigil.md`**

Replace:

```markdown
  supersedes the portrait altar of ADR-0014.
```

with:

```markdown
  supersedes the portrait altar of ADR-0014. Amended by ADR-0021: no verdict text
  before the lock (only the count), and the intro card shows before every ritual.
```

- [ ] **Step 6: Commit**

```bash
git add PRODUCT.md DESIGN.md AGENTS.md docs/decisions/0019-chant-screen.md docs/decisions/0020-direction-in-the-sigil.md
git commit -m "docs(product): Intro before every ritual, slab buttons and debug panel (ADR-0021)" -m "$TRAILER"
```

## Phase 2 — Pure core (TDD)

### Task 2: Only the count before the lock

**Files:** Modify: `MAIN/ui/screens/ChantScreenState.kt`, `TEST/ui/screens/ChantScreenStateTest.kt`, `MAIN/ui/screens/ChantScreen.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Edit `app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt`**

(a) Replace:

```kotlin
    fun beforeTheLockTheLineGuidesToTheDirection() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertEquals(VerdictLine.FindDirection, s.verdict)
```

with:

```kotlin
    fun beforeTheLockTheLineIsOnlyTheCount() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertEquals(VerdictLine.Count, s.verdict)
        assertEquals(0, s.numeral)
```

(b) Replace:

```kotlin
    fun lockingFillsTheRingAndAsksToHoldSteady() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(VerdictLine.HoldSteady, s.verdict)
```

with:

```kotlin
    fun lockingFillsTheRingAndShowsOnlyTheCount() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(VerdictLine.Count, s.verdict)
```

- [ ] **Step 2: Run the test and watch it fail**

```bash
./gradlew testDebugUnitTest --tests 'com.summondemons.safely.ui.screens.ChantScreenStateTest'
```
Expected: compilation fails with `Unresolved reference 'Count'`.

- [ ] **Step 3: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt`**

(a) Replace:

```kotlin
/** The one line under the words (ADR-0019); before the lock it guides to the direction (ADR-0020). */
enum class VerdictLine { FindDirection, HoldSteady, ChantAloud, Paused, Sealed, ChantAgain, SealCracked }
```

with:

```kotlin
/** The one line under the words (ADR-0019). Before the lock it is only the count; the sigil guides (ADR-0021). */
enum class VerdictLine { Count, ChantAloud, Paused, Sealed, ChantAgain, SealCracked }
```

(b) Replace:

```kotlin
        ritual.phase == RitualPhase.Seeking -> VerdictLine.FindDirection
        ritual.phase == RitualPhase.Locking -> VerdictLine.HoldSteady

```

with:

```kotlin
        ritual.phase == RitualPhase.Seeking || ritual.phase == RitualPhase.Locking -> VerdictLine.Count

```

- [ ] **Step 4: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`**

(a) Replace:

```kotlin
/** "CHANT ALOUD · I of IV" and its siblings, with the voice meter while listening. */
```

with:

```kotlin
/** "CHANT ALOUD · I of IV" and its siblings, with the voice meter while listening; only "I of IV" before the lock. */
```

(b) Replace:

```kotlin
        VerdictLine.Sealed, VerdictLine.HoldSteady -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused, VerdictLine.FindDirection -> colors.outline
    }
    val label = stringResource(
        when (screen.verdict) {
            VerdictLine.FindDirection -> R.string.verdict_find_direction
            VerdictLine.HoldSteady -> R.string.verdict_hold_steady
            VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
            VerdictLine.Paused -> R.string.verdict_paused
            VerdictLine.Sealed -> R.string.verdict_sealed
            VerdictLine.ChantAgain -> R.string.verdict_chant_again
            VerdictLine.SealCracked -> R.string.verdict_seal_cracked
        },
    ).upper()
```

with:

```kotlin
        VerdictLine.Sealed -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused, VerdictLine.Count -> colors.outline
    }
    val label = when (screen.verdict) {
        VerdictLine.Count -> null
        VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
        VerdictLine.Paused -> R.string.verdict_paused
        VerdictLine.Sealed -> R.string.verdict_sealed
        VerdictLine.ChantAgain -> R.string.verdict_chant_again
        VerdictLine.SealCracked -> R.string.verdict_seal_cracked
    }?.let { stringResource(it).upper() }
```

(c) Replace:

```kotlin
        Text(label, style = style, color = color, maxLines = 1)
        when (screen.verdict) {
```

with:

```kotlin
        if (label != null) Text(label, style = style, color = color, maxLines = 1)
        when (screen.verdict) {
```

- [ ] **Step 5: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="verdict_find_direction">Find the direction</string>
    <string name="verdict_hold_steady">Hold steady</string>

```

with:

```xml

```

- [ ] **Step 6: Gate, then commit**

```bash
./gradlew testDebugUnitTest lint assembleDebug
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt app/src/main/res/values/strings.xml
git commit -m "fix(ui): Only the phrase count before the lock (ADR-0021)" -m "$TRAILER"
```

## Phase 3 — Screen

### Task 3: The intro card before every ritual, instead of the hint

The card opens with every ritual (the microphone is ignored while it is up, as before). **SUMMON**, its ✕ and back all put it away and reveal the chant screen. The turn-sideways hint goes, and so does everything that only existed to show the card once.

**Files:** Modify: `MAIN/ritual/RitualViewModel.kt`, `TEST/ritual/RitualViewModelTest.kt`, `MAIN/navigation/AppNavHost.kt`, `MAIN/ui/GameViewModel.kt`, `MAIN/progress/Progress.kt`, `TEST/progress/ProgressJsonTest.kt`, `MAIN/ui/screens/RitualScreen.kt`, `MAIN/ui/screens/ChantIntro.kt`, `MAIN/ui/screens/ChantScreen.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt`**

(a) Replace:

```kotlin
    /** The intro card is on screen; the microphone is ignored meanwhile (ADR-0019). */
    val introVisible: Boolean = false,
    /** The card was shown automatically this ritual, so the chant screen skips its turn-sideways hint. */
    val introOffered: Boolean = false,

```

with:

```kotlin
    /** The intro card is on screen; the microphone is ignored meanwhile (ADR-0019). */
    val introVisible: Boolean = false,

```

(b) Replace:

```kotlin
    /** Show the intro card from the lock: the player's first ritual (ADR-0019). */
    private val showIntro: Boolean = false,
    /** Called once, the first time the player dismisses the intro card. */
    private val onIntroSeen: () -> Unit = {},

```

with:

```kotlin
    /** Show the intro card when the ritual opens: the app does on every ritual (ADR-0021). */
    private val showIntro: Boolean = false,

```

(c) Replace:

```kotlin
            introVisible = showIntro,
            introOffered = showIntro,

```

with:

```kotlin
            introVisible = showIntro,

```

(d) Replace:

```kotlin
    private var introSeenReported = false

    /** **Summon** on the intro card: the chant starts listening (ADR-0019). */
    fun dismissIntro() {
        if (!state.value.introVisible) return
        state.update { it.copy(introVisible = false) }
        if (!introSeenReported) {
            introSeenReported = true
            onIntroSeen()
        }
    }

```

with:

```kotlin
    /** **Summon**, ✕ or back on the intro card: the chant starts listening (ADR-0019, ADR-0021). */
    fun dismissIntro() = state.update { it.copy(introVisible = false) }

```

(e) Replace:

```kotlin
            showIntro: Boolean = false,
            onIntroSeen: () -> Unit = {},
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon, clipRecorder, showIntro, onIntroSeen) }
```

with:

```kotlin
            showIntro: Boolean = false,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon, clipRecorder, showIntro) }
```

- [ ] **Step 2: Edit `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`**

(a) Replace:

```kotlin
    fun theIntroCardMutesTheMicUntilSummon() = runTest(dispatcher) {
        var seen = 0
        val vm = RitualViewModel(
            demon = demon, headingSource = FakeHeading(), pcmSource = chantingPcm(),
            showIntro = true, onIntroSeen = { seen++ }, analysisDispatcher = dispatcher,
        )
```

with:

```kotlin
    fun theIntroCardMutesTheMicUntilSummon() = runTest(dispatcher) {
        val vm = RitualViewModel(
            demon = demon, headingSource = FakeHeading(), pcmSource = chantingPcm(),
            showIntro = true, analysisDispatcher = dispatcher,
        )
```

(b) Replace:

```kotlin
        assertTrue(vm.uiState.value.introVisible)
        assertTrue(vm.uiState.value.introOffered)
        vm.dismissIntro()
```

with:

```kotlin
        assertTrue(vm.uiState.value.introVisible)
        vm.dismissIntro()
```

(c) Replace:

```kotlin
        vm.dismissIntro()
        runCurrent()
        assertEquals(1, seen) // reported once

```

with:

```kotlin
        vm.dismissIntro()
        runCurrent()
        assertFalse(vm.uiState.value.introVisible)

```

- [ ] **Step 3: Edit `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`**

Replace:

```kotlin
            // First ritual only; "?" on the chant screen reopens it (ADR-0019).
            showIntro = !state.progress.chantIntroSeen,
            onIntroSeen = game::markChantIntroSeen,

```

with:

```kotlin
            // Before every ritual; "?" on the chant screen reopens it (ADR-0021).
            showIntro = true,

```

- [ ] **Step 4: Edit `app/src/main/java/com/summondemons/safely/ui/GameViewModel.kt`**

Replace:

```kotlin
    /** The chant screen's intro card was dismissed: show it automatically only once (ADR-0019). */
    fun markChantIntroSeen() = update { it.copy(chantIntroSeen = true) }


```

with:

```kotlin

```

- [ ] **Step 5: Edit `app/src/main/java/com/summondemons/safely/progress/Progress.kt`**

Replace:

```kotlin
    /** The chant screen's "How to chant" card was dismissed once (ADR-0019). */
    val chantIntroSeen: Boolean = false,

```

with:

```kotlin

```

- [ ] **Step 6: Edit `app/src/test/java/com/summondemons/safely/progress/ProgressJsonTest.kt`**

(a) Replace:

```kotlin
        homeHintSeen = true,
        chantIntroSeen = true,

```

with:

```kotlin
        homeHintSeen = true,

```

(b) Replace:

```kotlin
    fun olderSavesHaveNotSeenTheChantIntro() {
        assertEquals(false, ProgressJson.decode("""{"homeHintSeen":true}""").chantIntroSeen)
    }
```

with:

```kotlin
    fun savesWithTheOldIntroFlagStillLoad() {
        // chantIntroSeen left Progress (ADR-0021): the card now shows before every ritual.
        assertTrue(ProgressJson.decode("""{"homeHintSeen":true,"chantIntroSeen":true}""").homeHintSeen)
    }
```

- [ ] **Step 7: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`**

(a) Replace:

```kotlin
    BackHandler(enabled = !inClimax) { requestClose() }

```

with:

```kotlin
    BackHandler(enabled = !inClimax) { requestClose() }
    // Registered last, so it wins while the intro card is up: back puts the card away (ADR-0021).
    BackHandler(enabled = state.introVisible && !inClimax) { onDismissIntro() }

```

(b) Replace:

```kotlin
            onHelp = onShowIntro,
            showTurnHint = !state.introOffered,

```

with:

```kotlin
            onHelp = if (inClimax) null else onShowIntro,

```

(c) Replace:

```kotlin
        // The intro card comes down over the chant screen and goes back up to reveal it (ADR-0019).
```

with:

```kotlin
        // The intro card comes down over the chant screen and goes back up to reveal it (ADR-0019);
        // Summon and ✕ both start the chant (ADR-0021).
```

(d) Replace:

```kotlin
showClose = true, onClose = requestClose, modifier = Modifier.fillMaxSize())
```

with:

```kotlin
showClose = true, onClose = onDismissIntro, modifier = Modifier.fillMaxSize())
```

- [ ] **Step 8: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantIntro.kt`**

(a) Replace:

```kotlin
 * The intro card (ADR-0019): shown at the lock on the first ritual, and from the chant
 * screen's "?". Mirrors the chant screen: art where the sigil goes, the rules where the
 * words go. Generic: nothing in it names a demon or a direction. The microphone is ignored
 * while it is up (RitualViewModel); **Summon** starts the chant.
```

with:

```kotlin
 * The intro card (ADR-0019): shown before every ritual, and from the chant screen's "?"
 * (ADR-0021). Mirrors the chant screen: art where the sigil goes, the rules where the
 * words go. Generic: nothing in it names a demon or a direction. The microphone is ignored
 * while it is up (RitualViewModel); **Summon** and ✕ start the chant.
```

(b) Replace:

```kotlin
                    contentDescription = stringResource(R.string.action_abort),
```

with:

```kotlin
                    contentDescription = stringResource(R.string.intro_close),
```

- [ ] **Step 9: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`**

(a) Replace:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background

```

with:

```kotlin
import androidx.compose.foundation.Canvas

```

(b) Replace:

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn

```

with:

```kotlin
import androidx.compose.foundation.layout.size

```

(c) Replace:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

```

with:

```kotlin
import androidx.compose.runtime.Composable

```

(d) Replace:

```kotlin
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

```

with:

```kotlin
import androidx.compose.runtime.remember

```

(e) Replace:

```kotlin
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay

```

with:

```kotlin
import com.summondemons.safely.ui.theme.upper

```

(f) Replace:

```kotlin
private const val BLINK_MIN = 0.55f
private const val HINT_MS = 2_500L
private const val HINT_SCRIM_ALPHA = 0.88f

```

with:

```kotlin
private const val BLINK_MIN = 0.55f

```

(g) Replace:

```kotlin
 * The chant screen (ADR-0019): from the lock, the demon's name, sigil (progress, direction,
 * focus clock) and direction line on a slab at the left;
```

with:

```kotlin
 * The chant screen (ADR-0019, ADR-0020): for the whole Ritual, the demon's name, sigil
 * (progress, direction, focus clock) and direction line on a slab at the left;
```

(h) Replace:

```kotlin
    /** False right after the intro card, which already asked to turn sideways. */
    showTurnHint: Boolean = true,

```

with:

```kotlin

```

(i) Replace:

```kotlin
    var hint by rememberSaveable { mutableStateOf(showTurnHint) }
    LaunchedEffect(Unit) {
        delay(HINT_MS)
        hint = false
    }
    LaunchedEffect(state.ritual.lit) { if (state.ritual.lit > 0) hint = false }


```

with:

```kotlin


```

(j) Replace:

```kotlin
        if (hint) TurnSidewaysHint(accent, Modifier.fillMaxSize())

```

with:

```kotlin

```

(k) Replace:

```kotlin


/** Shown for a moment after the lock: turn sideways, keep the microphone clear. */
@Composable
private fun TurnSidewaysHint(accent: Color, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.background(colors.surfaceContainerLowest.copy(alpha = HINT_SCRIM_ALPHA)).padding(MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.chant_turn_sideways),
            style = MaterialTheme.textRoles.name.copy(fontSize = NameSize, textAlign = TextAlign.Center),
            color = accent,
        )
        Text(
            stringResource(R.string.chant_keep_mic_clear),
            style = MaterialTheme.textRoles.body.copy(textAlign = TextAlign.Center),
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}
```

with:

```kotlin

```

- [ ] **Step 10: Edit `app/src/main/res/values/strings.xml`**

(a) Replace:

```xml
    <string name="chant_turn_sideways">Turn your phone sideways</string>
    <string name="chant_keep_mic_clear">Hold it in front of your mouth, and keep the bottom edge clear so the microphone can hear you.</string>

```

with:

```xml

```

(b) Replace:

```xml
    <string name="intro_summon">Summon</string>

```

with:

```xml
    <string name="intro_summon">Summon</string>
    <string name="intro_close">Close and start the chant</string>

```

- [ ] **Step 11: Check nothing still refers to the removed names**

```bash
grep -rn "introOffered\|onIntroSeen\|markChantIntroSeen\|showTurnHint\|TurnSidewaysHint\|chant_turn_sideways\|chant_keep_mic_clear" app/src
grep -rn "chantIntroSeen" app/src
```
Expected: the first prints nothing; the second prints only the comment and JSON string in `ProgressJsonTest.kt`.

- [ ] **Step 12: Gate, then commit**

```bash
./gradlew testDebugUnitTest lint assembleDebug
git add -A app/src
git commit -m "fix(ui): Intro card before every ritual; its close starts the chant (ADR-0021)" -m "$TRAILER"
```

### Task 4: "?" and "D" above the name; the floating debug panel

**Files:** Modify: `MAIN/ui/screens/ChantScreen.kt`, `app/src/main/res/values/strings.xml`; Replace: `MAIN/ui/components/DebugControls.kt`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`**

(a) Replace:

```kotlin
import androidx.compose.foundation.Canvas

```

with:

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas

```

(b) Replace:

```kotlin
import androidx.compose.foundation.layout.size

```

with:

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

```

(c) Replace:

```kotlin
import androidx.compose.runtime.remember

```

with:

```kotlin
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

```

(d) Replace:

```kotlin
import androidx.compose.ui.graphics.Path

```

with:

```kotlin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect

```

(e) Replace:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer

```

with:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

```

(f) Replace:

```kotlin
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

```

with:

```kotlin
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

```

(g) Replace:

```kotlin
import androidx.compose.ui.unit.sp

```

with:

```kotlin
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

```

(h) Replace:

```kotlin
private const val SYLLABLE_DOT = "·"

```

with:

```kotlin
private const val SYLLABLE_DOT = "·"
private const val ROUND_BUTTON_ALPHA = 0.8f

```

(i) Replace:

```kotlin
private val PhraseStep = 2.sp

```

with:

```kotlin
private val PhraseStep = 2.sp
private val RoundButtonSize = 36.dp
private val RoundButtonBorder = 1.5.dp
private val RoundButtonText = 16.sp
private val Dash = 4.dp
private val DashGap = 3.dp
private val DebugPanelWidth = 300.dp
private val DebugPanelBorder = 1.dp

```

(j) Replace:

```kotlin
    /** Debug controls, shown under the verdict (debug builds only). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    /** Reopens the intro card ("?"); null hides the button. */
```

with:

```kotlin
    /** Debug controls, in a floating panel the "D" button opens (debug builds only; ADR-0021). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    /** Reopens the intro card ("?" above the demon's name); null hides the button. */
```

(k) Replace:

```kotlin
                Slab(state, screen, accent, flash, vertical = true, Modifier.fillMaxHeight().weight(SLAB_WEIGHT))
                Words(state, screen, accent, debug, blocker, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
```

with:

```kotlin
                Slab(state, screen, accent, flash, onHelp, debug, vertical = true, Modifier.fillMaxHeight().weight(SLAB_WEIGHT))
                Words(state, screen, accent, blocker, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
```

(l) Replace:

```kotlin
                Slab(state, screen, accent, flash, vertical = false, Modifier.fillMaxWidth())
                Words(state, screen, accent, debug, blocker, Modifier.fillMaxWidth().weight(1f))
```

with:

```kotlin
                Slab(state, screen, accent, flash, onHelp, debug, vertical = false, Modifier.fillMaxWidth())
                Words(state, screen, accent, blocker, Modifier.fillMaxWidth().weight(1f))
```

(m) Replace:

```kotlin
        Row(Modifier.align(Alignment.TopEnd).safeDrawingPadding(), verticalAlignment = Alignment.CenterVertically) {
            if (onHelp != null) {
                val helpLabel = stringResource(R.string.chant_help)
                IconButton(onClick = onHelp, modifier = Modifier.semantics { contentDescription = helpLabel }) {
                    Text(
                        "?",
                        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = DirectionSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showClose) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_abort),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

```

with:

```kotlin
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_abort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

```

(n) Replace:

```kotlin
    flash: SigilFlash?,
    vertical: Boolean,
    modifier: Modifier,
) {
```

with:

```kotlin
    flash: SigilFlash?,
    onHelp: (() -> Unit)?,
    debug: (@Composable () -> Unit)?,
    vertical: Boolean,
    modifier: Modifier,
) {
```

(o) Replace:

```kotlin
            ) {
                Text(state.demon.name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
```

with:

```kotlin
            ) {
                SlabButtons(onHelp, debug)
                Text(state.demon.name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
```

(p) Replace:

```kotlin
            ) {
                ChantSigil(state, screen, accent, ringColor, flash, Modifier.size(BandSigil))
```

with:

```kotlin
            ) {
                SlabButtons(onHelp, debug)
                ChantSigil(state, screen, accent, ringColor, flash, Modifier.size(BandSigil))
```

(q) Replace:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor)
            }
        }
    }
}

```

with:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor)
            }
        }
    }
}

/**
 * "?" (the intro card) and, in debug builds, "D" (the debug panel), centred above the
 * demon's name (ADR-0021). The panel floats under them, so opening it moves nothing; a tap
 * outside or back closes it.
 */
@Composable
private fun SlabButtons(onHelp: (() -> Unit)?, debug: (@Composable () -> Unit)?) {
    if (onHelp == null && debug == null) return
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val gap = with(LocalDensity.current) { spacing.xs.roundToPx() }
    Box {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            if (onHelp != null) RoundButton("?", stringResource(R.string.chant_help), onHelp)
            if (debug != null) {
                RoundButton("D", stringResource(R.string.debug_controls), { debugOpen = !debugOpen }, dashed = true, selected = debugOpen)
            }
        }
        if (debug != null && debugOpen) {
            Popup(
                popupPositionProvider = remember(gap) { BelowAnchor(gap) },
                onDismissRequest = { debugOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceContainerLowest,
                    border = BorderStroke(DebugPanelBorder, colors.outlineVariant),
                    modifier = Modifier.width(DebugPanelWidth),
                ) {
                    Box(Modifier.padding(spacing.sm)) { debug() }
                }
            }
        }
    }
}

/** A small round button on the slab: solid outline, or dashed for debug only; filled while [selected]. */
@Composable
private fun RoundButton(label: String, description: String, onClick: () -> Unit, dashed: Boolean = false, selected: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val fill = if (selected) colors.primary else colors.surfaceContainerHighest.copy(alpha = ROUND_BUTTON_ALPHA)
    val edge = if (selected) colors.primary else colors.outline
    val ink = when {
        selected -> colors.onPrimary
        dashed -> colors.outline
        else -> colors.onSurface
    }
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Box(
            Modifier.size(RoundButtonSize).drawBehind {
                val width = RoundButtonBorder.toPx()
                val dashes = if (dashed) PathEffect.dashPathEffect(floatArrayOf(Dash.toPx(), DashGap.toPx())) else null
                drawCircle(fill)
                drawCircle(edge, radius = (size.minDimension - width) / 2f, style = Stroke(width, pathEffect = dashes))
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.textRoles.ritualCue.copy(fontSize = RoundButtonText), color = ink)
        }
    }
}

/** Puts the debug panel under the buttons, left edges aligned, kept inside the window. */
private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        anchorBounds.left.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
        (anchorBounds.bottom + gap).coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)),
    )
}

```

(r) Replace:

```kotlin
    accent: Color,
    debug: (@Composable () -> Unit)?,
    blocker: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
```

with:

```kotlin
    accent: Color,
    blocker: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
```

(s) Replace:

```kotlin
            VerdictLine(screen, accent, state.micLevel)
        }
        debug?.invoke()
    }
```

with:

```kotlin
            VerdictLine(screen, accent, state.micLevel)
        }
    }
```

- [ ] **Step 2: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="debug_chant_score_none">Chant check · no verdict yet</string>

```

with:

```xml
    <string name="debug_chant_score_none">Chant check · no verdict yet</string>
    <string name="debug_controls">Debug controls</string>

```

- [ ] **Step 3: Replace `app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt`**

Same parameters, so `RitualScreen`'s call is unchanged. The controls stack as small chips: Simulate heading + Hold to Chant on one row, Record clip under them, then the score line. The fixed widths and the full-width rows are gone, because the panel is 300 dp wide.

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.ChantScore
import com.summondemons.safely.ritual.ClipLabel
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing

/** As tall as a chip, so "Hold to Chant" lines up with its neighbours. */
private val ChipHeight = 32.dp

/**
 * The chant screen's debug panel, as small chips (ADR-0021). Debug builds only (caller
 * checks BuildConfig.DEBUG); shown in the floating panel under "D", never in the layout.
 */
@Composable
fun DebugControls(
    simulateHeading: Boolean,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
    /** The chant check's latest verdict (ADR-0018). */
    lastScore: ChantScore?,
    /** Clip recorder label; null = not recording (ADR-0018). */
    clipLabel: ClipLabel?,
    onCycleClipLabel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val small = MaterialTheme.typography.labelSmall
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(stringResource(R.string.debug_controls), style = small, color = colors.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = simulateHeading,
                onClick = { onSimulateHeadingChange(!simulateHeading) },
                label = { Text(stringResource(R.string.debug_simulate_heading), style = small) },
            )
            HoldToChantChip(onHoldToChantChange)
        }
        FilterChip(
            selected = clipLabel != null,
            onClick = onCycleClipLabel,
            label = {
                Text(
                    stringResource(
                        when (clipLabel) {
                            null -> R.string.debug_clip_off
                            ClipLabel.Chant -> R.string.debug_clip_chant
                            ClipLabel.Nonsense -> R.string.debug_clip_nonsense
                        },
                    ),
                    style = small,
                )
            },
        )
        Text(
            if (lastScore == null) {
                stringResource(R.string.debug_chant_score_none)
            } else {
                stringResource(
                    R.string.debug_chant_score,
                    lastScore.total,
                    if (lastScore.passed) "✓" else "✗",
                    lastScore.vowels,
                    lastScore.rhythm,
                )
            },
            style = small,
            color = colors.onSurfaceVariant,
        )
    }
}

/** Sounds the expected vowels while pressed (ADR-0018). */
@Composable
private fun HoldToChantChip(onHoldToChantChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Surface(
        shape = PillShape,
        color = if (holding) colors.secondaryContainer else colors.surfaceContainerHigh,
        contentColor = if (holding) colors.onSecondaryContainer else colors.onSurfaceVariant,
        modifier = Modifier
            .heightIn(min = ChipHeight)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        onHold(true)
                        try {
                            awaitRelease()
                        } finally {
                            holding = false
                            onHold(false)
                        }
                    },
                )
            },
    ) {
        Box(Modifier.padding(horizontal = MaterialTheme.spacing.md), contentAlignment = Alignment.Center) {
            Text(
                stringResource(if (holding) R.string.debug_chanting else R.string.debug_hold_to_chant),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Opens the debug sheet from Home and Grimoire. Debug builds only (caller checks BuildConfig.DEBUG). */
@Composable
fun DebugChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.debug_chip)) }, modifier = modifier)
}
```

- [ ] **Step 4: Check the layout no longer depends on the build type**

```bash
grep -n "debug" app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt
```
Expected: matches only in `ChantScreen` (its parameter and the two `Slab(...)` calls), `Slab` and `SlabButtons`; none in `Words` or `VerdictLine`.

- [ ] **Step 5: Gate, then commit**

```bash
./gradlew testDebugUnitTest lint assembleDebug
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt app/src/main/res/values/strings.xml
git commit -m "feat(ui): Round ? and D above the demon's name; debug controls in a floating panel (ADR-0021)" -m "$TRAILER"
```

## Phase 4 — Device check

### Task 5: Device checklist

- [ ] **Step 1: Install a debug build and run through it**

```bash
./gradlew installDebug
```

Open a ritual twice (use the debug sheet's "Reset daily summon" between attempts) and check:

1. The card comes down from the top **every** time; there is never a "Turn your phone sideways" overlay.
2. Its ✕ sends it up and shows the chant screen (you stay in the Ritual). So do **SUMMON** and system back.
3. With the card up, chanting lights nothing. After it goes up, chanting lights syllables.
4. "?" and a dashed "D" sit side by side, centred above the demon's name; the top-right corner has only ✕. "?" brings the card back down.
5. Before the lock, the line under the phrase is only "I of IV" (no "FIND THE DIRECTION" / "HOLD STEADY"); after the lock it reads "CHANT ALOUD · I of IV".
6. "D" fills amber and opens a small panel under the buttons: Simulate heading, Hold to Chant, Record clip, score. The phrase and sigil do not move or shrink. A tap outside closes it, and so does back (the Ritual stays open). Tapping "D" again also closes it; if instead it flickers closed and reopens, note it in the commit.
7. Hold to Chant still lights syllables while pressed; Simulate heading still locks.
8. In the climax, "?", "D" and the panel are gone.
9. A release build (`./gradlew assembleRelease`, or check `BuildConfig.DEBUG` false) shows only "?", in the same place.

- [ ] **Step 2: Report**

Note any item that failed, with a screenshot, in the last commit message or a follow-up issue. No commit is needed if everything passed.

---

## Spec coverage

| ADR-0021 decision | Task |
|---|---|
| Intro card before every ritual; turn-sideways hint removed; `chantIntroSeen` removed | 3 |
| ✕, back and SUMMON put the card away | 3 |
| "?" round, centred above the name; top-right only ✕ | 4 |
| "D" dashed beside "?", debug builds only; floating panel, closed by default, no layout space | 4 |
| Compact debug chips | 4 |
| No verdict text before the lock | 2 |
| Canon (PRODUCT, DESIGN, AGENTS, ADR status lines) | 1 |
