# Chant Screen UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the chant screen from shared pieces (gated line box, tilting sigil with proximity arc, direction pill, feedback slot with score and heard text) in two arrangements, split and centred, behind a live debug switch, and log the phrase size each arrangement reaches.

**Architecture:** Feedback rules are pure (`LineFeedback.next`) and run in `RitualViewModel` once per tick. `chantScreenState` (pure) turns the ritual, heading, check state and feedback into everything drawn. The composables live in `ChantPieces.kt`; `ChantArrangements.kt` places them (Split, Centred, Portrait), and `ChantScreen.kt` picks one from `RitualUiState.layout` and reports the settled phrase size to the session log.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, `BasicText` auto-size), kotlinx.serialization (session log), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-18-chant-screen-ux-design.md`

**Branch:** `docs/chant-check-v2` (continue on it).

**Commands used throughout:**
- Unit tests: `./gradlew testDebugUnitTest`
- One class: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
- Build and lint: `./gradlew assembleDebug lintDebug`

**Deviations from the spec (decided while planning):**
- `ChantLayout` lives in `ritual/` (not `ui/screens/`): `RitualViewModel` holds it and must not depend on UI packages.
- Crack feedback stays on `ChantVerdict` (as today); `LineFeedback.Kind` has only `Sealed` and `Missed`.
- The miss rule uses "a `LineChecked` miss and a `Retry` in the same step", which is exactly the engine's "miss while every syllable was lit" (`RitualEngine.chant`).
- The paused pill reads `PAUSED ←` without a ⏸ glyph (it may render as an emoji). Amber colour, the word, the arrow and a blink carry the state.
- "Feedback over checking" (the §10 priority) wins over "newest wins" (§3.5): feedback lasts only 2.5 s.

---

## File structure

| File | Change | Responsibility |
|---|---|---|
| `app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt` | Modify | `FEEDBACK_HOLD_MS` |
| `app/src/main/java/com/summondemons/safely/ritual/LineFeedback.kt` | Create | Score, heard text, when feedback appears and expires (pure) |
| `app/src/main/java/com/summondemons/safely/ritual/ChantLayout.kt` | Create | `enum class ChantLayout { Split, Centred }` |
| `app/src/main/java/com/summondemons/safely/ritual/SessionLog.kt` | Modify | `SessionHeader.layout`, `UiRecord`, `SessionTick.ui` |
| `app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt` | Modify | `feedback`, `layout`, `setLayout`, `onPhraseLaid` |
| `app/src/main/java/com/summondemons/safely/platform/DebugPrefs.kt` | Create | Debug pref for the layout |
| `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt` | Modify | Pass the layout to the ViewModel |
| `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt` | Rewrite | Pill, seals, slot, tilt, gate (pure) |
| `app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt` | Create | Corner buttons, title + seals, pill, sigil, feedback slot, line box |
| `app/src/main/java/com/summondemons/safely/ui/screens/ChantArrangements.kt` | Create | `ChantParts`, Split, Centred, Portrait |
| `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt` | Rewrite | Backdrop, choose the arrangement, ✕, phrase-size report |
| `app/src/main/java/com/summondemons/safely/ui/screens/RitualStatusBlock.kt` | Delete | Replaced by the title and pill |
| `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt` | Modify | Wire the layout switch and phrase-size callback |
| `app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt` | Modify | `LayoutChip` |
| `app/src/main/res/drawable/ic_lock.xml` | Create | Lock glyph for the gated line |
| `app/src/main/res/values/strings.xml` | Modify | New strings; drop `chant_step_chant` |
| `app/src/test/java/com/summondemons/safely/ritual/LineFeedbackTest.kt` | Create | Feedback rules |
| `app/src/test/java/com/summondemons/safely/ritual/SessionLogTest.kt` | Modify | Round trip with `layout` and `ui` |
| `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt` | Modify | Layout note and `ui` records in the log |
| `app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt` | Rewrite | New screen state |
| `app/src/test/java/com/summondemons/safely/ritual/SessionReplayTest.kt` | Modify | Phrase size table |
| `docs/decisions/0029-chant-screen-ux.md` | Create | ADR |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md` | Modify | Chant screen row, component contracts, current state |

---

### Task 1: Feedback rules

**Files:**
- Modify: `app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt`
- Create: `app/src/main/java/com/summondemons/safely/ritual/LineFeedback.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/LineFeedbackTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** What the chant screen shows after an attempt at a line (ADR-0029). */
class LineFeedbackTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val state = RitualState.start(90f, phrases, mode = CheckMode.Whisper)

    private fun step(vararg effects: RitualEffect) = Step(state, effects.toList())

    @Test
    fun theScoreIsTheShareOfTheLineHeard() {
        assertEquals(40, LineFeedback.score(0.60))
        assertEquals(82, LineFeedback.score(0.18))
        assertEquals(100, LineFeedback.score(-0.2))
        assertEquals(0, LineFeedback.score(1.5))
    }

    @Test
    fun theHeardTextIsLowerCaseLettersAndSpaces() {
        assertEquals("gracia tu pi", LineFeedback.heardText("  Gracia, tu-pi! "))
        assertEquals("", LineFeedback.heardText("..."))
    }

    @Test
    fun aSealShowsItsScore() {
        val f = LineFeedback.next(
            step(RitualEffect.LineChecked(1, true, 0.18), RitualEffect.Sealed(1)), "gratia sit cura", null, nowMs = 1_000,
        )!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertEquals(1, f.phrase)
        assertEquals(82, f.score)
        assertNull("no heard text on a seal", f.heard)
        assertEquals(1_000 + RitualConfig.FEEDBACK_HOLD_MS, f.untilMs)
    }

    @Test
    fun aLenientSealHasNoScore() {
        val f = LineFeedback.next(step(RitualEffect.SyllableLit, RitualEffect.Sealed(0)), null, null, nowMs = 0)!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertNull(f.score)
    }

    @Test
    fun aMissAfterTheWholeLineShowsScoreAndHeard() {
        val f = LineFeedback.next(
            step(RitualEffect.LineChecked(1, false, 0.75), RitualEffect.Retry(1)), "Gracia tu pi.", null, nowMs = 500,
        )!!
        assertEquals(LineFeedback.Kind.Missed, f.kind)
        assertEquals(25, f.score)
        assertEquals("gracia tu pi", f.heard)
    }

    @Test
    fun aMidLineMissShowsNothing() {
        assertNull(LineFeedback.next(step(RitualEffect.LineChecked(1, false, 0.7)), "gratia", null, nowMs = 0))
    }

    @Test
    fun aMissWithAnEmptyTranscriptHasNoHeardText() {
        val f = LineFeedback.next(step(RitualEffect.LineChecked(1, false, 0.9), RitualEffect.Retry(1)), "", null, nowMs = 0)!!
        assertNull(f.heard)
    }

    @Test
    fun feedbackStaysUntilItExpires() {
        val shown = LineFeedback(LineFeedback.Kind.Sealed, 0, 90, null, untilMs = 2_500)
        assertSame(shown, LineFeedback.next(step(), null, shown, nowMs = 2_480))
        assertNull(LineFeedback.next(step(), null, shown, nowMs = 2_500))
    }

    @Test
    fun aNewResultReplacesTheOldOne() {
        val shown = LineFeedback(LineFeedback.Kind.Missed, 1, 25, "x", untilMs = 9_000)
        val f = LineFeedback.next(step(RitualEffect.LineChecked(1, true, 0.1), RitualEffect.Sealed(1)), null, shown, nowMs = 100)!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertEquals(90, f.score)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests '*LineFeedbackTest*'`
Expected: compilation FAILS with `Unresolved reference: LineFeedback`.

- [ ] **Step 3: Add the constant**

In `RitualConfig.kt`, below `VERDICT_HOLD_MS`:

```kotlin
    /** How long the chant screen shows a line's score (and, on a miss, what was heard) (ADR-0029). */
    const val FEEDBACK_HOLD_MS = 2_500L
```

- [ ] **Step 4: Write the implementation**

Create `LineFeedback.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FEEDBACK_HOLD_MS
import java.util.Locale
import kotlin.math.roundToInt

/**
 * What the chant screen shows after an attempt at a line ends (ADR-0029): the line's score, and on
 * a miss what the phone heard. Shown until [untilMs] (ritual time, ticks × 20 ms).
 */
data class LineFeedback(val kind: Kind, val phrase: Int, val score: Int?, val heard: String?, val untilMs: Long) {
    enum class Kind { Sealed, Missed }

    companion object {
        private val NOT_LETTERS = Regex("[^\\p{L}]+")

        /** 0–100 as 100 × (1 − distance); 40 or more seals (`LINE_MATCH_MAX` = 0.60). */
        fun score(distance: Double): Int = (100 * (1 - distance)).roundToInt().coerceIn(0, 100)

        /** The transcript as shown: lower case, words of letters separated by single spaces. */
        fun heardText(transcript: String): String = transcript.lowercase(Locale.ROOT).replace(NOT_LETTERS, " ").trim()

        /**
         * The feedback after [step] at ritual time [nowMs]. A seal shows its score (none in lenient
         * mode, where no check ran). A miss shows its score and [transcript] only when it ended an
         * attempt: the whole line was lit, so the words clear for another try (a `Retry` in the same
         * step, ADR-0028). Mid-line misses show nothing. Otherwise [current] stays until it expires.
         */
        fun next(step: Step, transcript: String?, current: LineFeedback?, nowMs: Long): LineFeedback? {
            val checked = step.effects.filterIsInstance<RitualEffect.LineChecked>().firstOrNull()
            val sealed = step.effects.filterIsInstance<RitualEffect.Sealed>().firstOrNull()
            val until = nowMs + FEEDBACK_HOLD_MS
            return when {
                sealed != null -> LineFeedback(Kind.Sealed, sealed.phrase, checked?.takeIf { it.found }?.let { score(it.distance) }, null, until)
                checked != null && !checked.found && step.effects.any { it is RitualEffect.Retry } ->
                    LineFeedback(Kind.Missed, checked.phrase, score(checked.distance), transcript?.let(::heardText)?.ifEmpty { null }, until)
                current != null && nowMs < current.untilMs -> current
                else -> null
            }
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests '*LineFeedbackTest*'`
Expected: PASS (9 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt app/src/main/java/com/summondemons/safely/ritual/LineFeedback.kt app/src/test/java/com/summondemons/safely/ritual/LineFeedbackTest.kt
git commit -m "feat(ritual): Line feedback: score, heard text, shown when an attempt ends (ADR-0029)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Layout and phrase size in the session log

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ritual/ChantLayout.kt`
- Modify: `app/src/main/java/com/summondemons/safely/ritual/SessionLog.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/SessionLogTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `SessionLogTest`:

```kotlin
    @Test
    fun theLayoutAndPhraseSizesRoundTrip() {
        val h = header.copy(layout = "Centred")
        val ui = SessionTick(t = 1_000, ui = UiRecord("Centred", phrase = 1, phraseSp = 42f, boxWidthDp = 700, boxHeightDp = 90, sigilDp = 104))
        val (decoded, ticks) = SessionRecords.decode(listOf(SessionRecords.encode(h), SessionRecords.encode(ui)))
        assertEquals(h, decoded)
        assertEquals(listOf(ui), ticks)
        assertTrue("a ui record is not a tick", !ticks.single().isTick)
        assertTrue("the header always names its layout", SessionRecords.encode(header).contains("\"layout\":\"Split\""))
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests '*SessionLogTest*'`
Expected: compilation FAILS (`layout`, `UiRecord` unresolved).

- [ ] **Step 3: Write the implementation**

Create `ChantLayout.kt`:

```kotlin
package com.summondemons.safely.ritual

/** How the chant screen arranges its pieces (ADR-0029); a debug switch until the device comparison decides. */
enum class ChantLayout { Split, Centred }
```

In `SessionLog.kt`, add `layout` to `SessionHeader` after `mode`:

```kotlin
    /** CheckMode name (ADR-0027). */
    val mode: String = "Whisper",
    /** ChantLayout name (ADR-0029). */
    val layout: String = "Split",
    val startedMs: Long,
```

Add `ui` to `SessionTick` after `check`:

```kotlin
    /** Details of a check that finished on this tick (debug readout, threshold re-check). */
    val check: CheckRecord? = null,
    /** The phrase size the chant screen settled on; an event line, not a tick (ADR-0029). */
    val ui: UiRecord? = null,
    val note: String? = null,
```

Add after `CheckRecord`:

```kotlin
/** The chant screen's settled phrase size and boxes in one arrangement (ADR-0029). */
@Serializable
data class UiRecord(
    val layout: String,
    val phrase: Int,
    val phraseSp: Float,
    val boxWidthDp: Int,
    val boxHeightDp: Int,
    val sigilDp: Int,
)
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests '*SessionLogTest*' --tests '*SessionReplayTest*'`
Expected: PASS (the replay test is skipped when `src/test/sessions` is empty).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/ChantLayout.kt app/src/main/java/com/summondemons/safely/ritual/SessionLog.kt app/src/test/java/com/summondemons/safely/ritual/SessionLogTest.kt
git commit -m "feat(ritual): Log the chant layout and the phrase size it reaches (ADR-0029)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: ViewModel: feedback, layout, phrase-size records

**Files:**
- Modify: `app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt`
- Test: `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `RitualViewModelTest`:

```kotlin
    private class FakeLog : SessionLog {
        var header: SessionHeader? = null
        val lines = mutableListOf<SessionTick>()
        override fun start(header: SessionHeader) { this.header = header }
        override fun write(tick: SessionTick) { lines += tick }
        override fun stop() = Unit
    }

    @Test
    fun theLayoutIsLoggedAndEachSettledSizeOnce() {
        val log = FakeLog()
        val vm = RitualViewModel(demon, FakeHeading(), silentPcm(), analysisDispatcher = dispatcher, sessionLog = log, layout = ChantLayout.Centred)
        assertEquals("Centred", log.header!!.layout)

        vm.setLayout(ChantLayout.Split)
        assertEquals("layout:Split", log.lines.last().note)

        val record = UiRecord("Split", 0, 64f, 600, 250, 160)
        vm.onPhraseLaid(record)
        vm.onPhraseLaid(record)
        assertEquals(listOf(record), log.lines.mapNotNull { it.ui })
        vm.onPhraseLaid(record.copy(phraseSp = 66f))
        assertEquals(2, log.lines.mapNotNull { it.ui }.size)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests '*RitualViewModelTest*'`
Expected: compilation FAILS (`layout` parameter, `setLayout`, `onPhraseLaid` unresolved).

- [ ] **Step 3: Write the implementation**

In `RitualUiState`, after `introVisible`:

```kotlin
    /** The intro card is on screen; the microphone is ignored meanwhile (ADR-0019). */
    val introVisible: Boolean = false,
    /** The last attempt's score and heard text while shown (ADR-0029). */
    val feedback: LineFeedback? = null,
    /** How the chant screen is arranged (debug switch; ADR-0029). */
    val layout: ChantLayout = ChantLayout.Split,
)
```

Add the constructor parameter after `checkMode`:

```kotlin
    private val checkMode: () -> Pair<CheckMode, String> = { CheckMode.Lenient to "" },
    /** The chant screen's arrangement when the ritual opens (ADR-0029). */
    layout: ChantLayout = ChantLayout.Split,
) : ViewModel() {
```

Initial state: add `layout = layout,` to the `RitualUiState(...)` in `MutableStateFlow(...)`.

Header in `init`: pass `layout = layout.name`:

```kotlin
        sessionLog.start(
            SessionHeader(demon.id, demon.ritualHeading, demon.chantPhrases, mode = mode.first.name, layout = layout.name, startedMs = System.currentTimeMillis()),
        )
```

`init` reads the constructor parameter `layout`. Kotlin allows that inside `init` blocks. Keep this `init` block where it is: it comes after the `ticks` property, which `note` uses.

Add after `openIntro()`:

```kotlin
    /** Debug: switch the chant screen's arrangement live (ADR-0029). */
    fun setLayout(layout: ChantLayout) {
        note("layout:${layout.name}")
        state.update { it.copy(layout = layout) }
    }

    private var lastUi: UiRecord? = null

    /** The chant screen settled on a phrase size; logged once per change (ADR-0029). */
    fun onPhraseLaid(record: UiRecord) {
        if (record == lastUi) return
        lastUi = record
        sessionLog.write(SessionTick(t = ticks * RitualConfig.FRAME_MS, ui = record))
    }
```

`ticks` is declared below the methods that use it. It is a class property, so this compiles. Move the `private var ticks` and `note` declarations above `setLayout` if lint complains about ordering.

In `tick(...)`, replace the session-log write and the state update:

```kotlin
        val step = RitualEngine.step(ritual, input)
        val now = ticks * RitualConfig.FRAME_MS
        sessionLog.write(SessionRecords.tick(now, input, step, hold = current.holdToChant, check = report))
        ticks++
        // The summon is saved here, not through the lossy effects flow, before the climax plays.
        step.effects.filterIsInstance<RitualEffect.ClimaxStart>().firstOrNull()?.let { commitSummon(it.flawless) }
        if (step.effects.any { it is RitualEffect.Retry }) listener?.restart()
        val feedback = LineFeedback.next(step, report?.transcript, current.feedback, now)
        state.update {
            it.copy(
                ritual = step.state,
                micLevel = smoothMicLevel(it.micLevel, voice.rms),
                lastCheck = report ?: it.lastCheck,
                checking = listener?.checking ?: false,
                feedback = feedback,
            )
        }
```

Factory: add `layout: ChantLayout = ChantLayout.Split,` after `checkMode`, and pass `layout = layout` to the constructor.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests '*RitualViewModelTest*' --tests '*WhisperModeTest*' --tests '*SessionLogTest*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt
git commit -m "feat(ritual): ViewModel keeps line feedback and the chant layout; logs phrase sizes (ADR-0029)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Chant screen state

**Files:**
- Rewrite: `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt`
- Rewrite: `app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt`

This task breaks `ChantScreen.kt` and `RitualStatusBlock.kt` (they use `status` and `verdict`). Tasks 5–7 replace them. Run only the unit tests of this file until Task 7, and do **not** commit this task separately; it is committed with Task 7.

- [ ] **Step 1: Write the failing test (replace the whole file)**

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.LineFeedback
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantScreenStateTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val seeking = RitualState.start(90f, phrases, mode = CheckMode.Whisper)
    private val chanting = seeking.copy(phase = RitualPhase.Chanting, azimuth = 90f)

    private fun state(r: RitualState, azimuth: Float? = 90f, checking: Boolean = false, feedback: LineFeedback? = null) =
        chantScreenState(r, phrases, azimuth, 90f, checking, feedback)

    // --- Words (ADR-0019, ADR-0027) ---

    @Test
    fun wordsKeepTheirSyllablesAndPunctuation() {
        assertEquals(listOf(listOf("DAE", "MO", "NI", "UM"), listOf("RE", "SI", "DE", "NTE,")), chantWords(phrases[0]))
        assertEquals(listOf(listOf("GRA", "TI", "A"), listOf("SIT"), listOf("CU", "RA.")), chantWords(phrases[1]))
    }

    @Test
    fun theWordBeingChantedIsCurrentWithItsLitSyllables() {
        val s = state(chanting.copy(lit = 2))
        assertEquals(WordLook.Current, s.words[0].look)
        assertEquals(2, s.words[0].lit)
        assertEquals(WordLook.Pending, s.words[1].look)
    }

    @Test
    fun coveredWordsAreHeardInWhisperMode() {
        assertEquals(listOf(WordLook.Current, WordLook.Heard), state(chanting.copy(lit = 2, covered = setOf(1))).words.map { it.look })
    }

    @Test
    fun fullyLitWordsAreHeardInLenientMode() {
        val r = RitualState.start(90f, phrases).copy(phase = RitualPhase.Chanting, azimuth = 90f, lit = 5)
        assertEquals(listOf(WordLook.Heard, WordLook.Current), state(r).words.map { it.look })
    }

    // --- Gate and pill (ADR-0029 §3.3, §3.6) ---

    @Test
    fun beforeTheLockTheLineIsGatedAndThePillPointsTheWay() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertTrue(s.gated)
        assertEquals(SlotContent.Empty, s.slot)
        assertEquals(DirectionPill(PillLook.Seeking, TurnDirection.Right), s.pill)
        assertEquals(RingMode.Seeking, s.ring)
        assertFalse(s.facing)
        assertFalse("not paused before the lock", s.drifting)
    }

    @Test
    fun lockingIsStillGatedButFacing() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertTrue(s.gated)
        assertEquals(DirectionPill(PillLook.Facing, null), s.pill)
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(0f, s.tiltDegrees, 0f)
    }

    @Test
    fun chantingOpensTheLineAndListens() {
        val s = state(chanting)
        assertFalse(s.gated)
        assertEquals(SlotContent.Meter, s.slot)
        assertEquals(DirectionPill(PillLook.Facing, null), s.pill)
        assertEquals(RingMode.Clock, s.ring)
        assertEquals("phrase I has no clock: a full ring", 1f, s.clockFraction, 0f)
    }

    @Test
    fun offCourseItPausesPointsBackAndTilts() {
        val s = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertTrue(s.drifting)
        assertFalse(s.gated)
        assertEquals(DirectionPill(PillLook.Paused, TurnDirection.Right), s.pill)
        assertEquals(SIGIL_TILT_MAX_DEG, s.tiltDegrees, 0.01f)
        assertEquals(SlotContent.Meter, s.slot)
    }

    @Test
    fun afterTheFourthSealThePillHidesAndEveryWordIsHeard() {
        val s = state(chanting.copy(phase = RitualPhase.Climax, phrase = 4))
        assertEquals(PillLook.Hidden, s.pill.look)
        assertTrue(s.words.all { it.look == WordLook.Heard })
        assertEquals(SlotContent.Empty, s.slot)
        assertTrue(s.seals.all { it == SealLook.Sealed })
    }

    // --- Sigil (ADR-0020, ADR-0029 §3.4) ---

    @Test
    fun theMarkerPointsAtTheDemonRelativeToThePhone() {
        assertEquals(90f, state(seeking.copy(azimuth = 0f), azimuth = 0f).markerDegrees!!, 0.01f)
        assertEquals(-90f, state(seeking.copy(azimuth = 180f), azimuth = 180f).markerDegrees!!, 0.01f)
        assertNull(state(seeking, azimuth = null).markerDegrees)
    }

    @Test
    fun whileSeekingTheSigilTiltsTowardTheDemonClamped() {
        assertEquals(10f, state(seeking.copy(azimuth = 80f), azimuth = 80f).tiltDegrees, 0.01f)
        assertEquals(-SIGIL_TILT_MAX_DEG, state(seeking.copy(azimuth = 180f), azimuth = 180f).tiltDegrees, 0.01f)
        assertEquals(0f, state(seeking, azimuth = null).tiltDegrees, 0f)
        assertEquals("upright while chanting", 0f, state(chanting.copy(azimuth = 100f), azimuth = 100f).tiltDegrees, 0f)
    }

    // --- Seals (ADR-0029 §3.2) ---

    @Test
    fun sealsFollowTheLines() {
        assertEquals(listOf(SealLook.Sealed, SealLook.Current, SealLook.Pending, SealLook.Pending), state(chanting.copy(phrase = 1)).seals)
        assertTrue(state(chanting, checking = true).checking)
    }

    // --- Feedback slot (ADR-0029 §3.5) ---

    @Test
    fun aRunningCheckShowsChecking() {
        assertEquals(SlotContent.Checking, state(chanting, checking = true).slot)
    }

    @Test
    fun feedbackWinsOverChecking() {
        val sealed = LineFeedback(LineFeedback.Kind.Sealed, 0, 82, null, untilMs = 9_999)
        assertEquals(SlotContent.Sealed(82), state(chanting.copy(phrase = 1), checking = true, feedback = sealed).slot)
    }

    @Test
    fun aMissShowsItsScoreAndWhatWasHeard() {
        val missed = LineFeedback(LineFeedback.Kind.Missed, 1, 25, "gracia tu pi", untilMs = 9_999)
        assertEquals(SlotContent.Missed(25, "gracia tu pi"), state(chanting.copy(phrase = 1), feedback = missed).slot)
    }

    @Test
    fun aCrackWinsOverEverything() {
        val sealed = LineFeedback(LineFeedback.Kind.Sealed, 0, 82, null, untilMs = 9_999)
        val s = state(chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0)), checking = true, feedback = sealed)
        assertEquals(SlotContent.Cracked, s.slot)
        assertEquals(SealLook.Cracked, s.seals[0])
    }
}
```

- [ ] **Step 2: Write the implementation (replace the whole file)**

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.LineFeedback
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ritual.turnDirection

/** The sigil's tilt toward the demon is clamped to this (ADR-0029). */
const val SIGIL_TILT_MAX_DEG = 35f

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** How one line's seal mark is drawn (ADR-0029 §3.2). */
enum class SealLook { Pending, Current, Sealed, Cracked }

/** What the sigil's outer ring shows (ADR-0020): proximity while seeking, the hold while locking, then the focus clock. */
enum class RingMode { Seeking, Locking, Clock }

/** The direction pill (ADR-0029 §3.3), the only place that names the direction. */
enum class PillLook { Seeking, Facing, Paused, Hidden }

/** [turn] is shown while seeking or paused; null otherwise. */
data class DirectionPill(val look: PillLook, val turn: TurnDirection?)

/** The one slot above the phrase (ADR-0029 §3.5). */
sealed interface SlotContent {
    data object Empty : SlotContent
    data object Meter : SlotContent
    data object Checking : SlotContent
    /** [score] is null in lenient mode. */
    data class Sealed(val score: Int?) : SlotContent
    data class Missed(val score: Int, val heard: String?) : SlotContent
    data object Cracked : SlotContent
}

/** Everything the chant screen draws, derived from the ritual (pure; ADR-0019, ADR-0029). */
data class ChantScreenState(
    val words: List<ChantWord>,
    val facing: Boolean,
    /** Off course after the lock (wavering): paused, amber. */
    val drifting: Boolean,
    /**
     * Where the demon is relative to where the phone faces, in degrees clockwise in
     * (−180, 180]: the marker on the sigil's ring (0 = at the top). Null when the heading is unknown.
     */
    val markerDegrees: Float?,
    val ring: RingMode,
    /** How much of the 0.6 s lock hold is done, 0..1 (only while locking). */
    val lockFraction: Float,
    /** The ring's arc after the lock: focus left, or 1 on phrase I (no clock). */
    val clockFraction: Float,
    val pill: DirectionPill,
    val seals: List<SealLook>,
    /** A Whisper check is running: the current seal breathes faster. */
    val checking: Boolean,
    val slot: SlotContent,
    /** The sigil tile's rotation: toward the demon while seeking or paused, 0 otherwise. */
    val tiltDegrees: Float,
    /** Before the lock: the line is shown but closed. */
    val gated: Boolean,
)

/** Splits a chant phrase into words of syllables for display, matching [LatinSyllables]. */
fun chantWords(text: String): List<List<String>> = text.split(' ').filter { it.isNotBlank() }.map { token ->
    val spans = LatinSyllables.spans(token)
    if (spans.isEmpty()) return@map listOf(token)
    spans.mapIndexed { i, span ->
        val end = if (i == spans.lastIndex) token.length else span.end
        token.substring(if (i == 0) 0 else span.start, end)
    }
}

fun chantScreenState(
    ritual: RitualState,
    phrases: List<String>,
    azimuth: Float?,
    target: Float,
    checking: Boolean = false,
    feedback: LineFeedback? = null,
): ChantScreenState {
    val done = ritual.phrase >= PHRASE_COUNT
    val phrase = ritual.phrase.coerceAtMost(PHRASE_COUNT - 1)
    val verdict = ritual.verdict?.takeIf { !done }
    val text = chantWords(phrases[phrase])
    val words = if (done) text.map { ChantWord(it, it.size, WordLook.Heard) } else chantingWords(ritual, text)
    val gated = ritual.phase == RitualPhase.Seeking || ritual.phase == RitualPhase.Locking
    val drifting = ritual.phase == RitualPhase.Wavering
    val marker = azimuth?.let { signedDelta(it, target) }
    val cracked = verdict?.kind == ChantVerdict.Kind.Cracked
    val pillLook = when {
        done || ritual.phase == RitualPhase.Climax || ritual.phase == RitualPhase.Done -> PillLook.Hidden
        drifting -> PillLook.Paused
        ritual.phase == RitualPhase.Seeking -> PillLook.Seeking
        else -> PillLook.Facing
    }
    val pointing = pillLook == PillLook.Seeking || pillLook == PillLook.Paused
    return ChantScreenState(
        words = words,
        facing = ritual.aligned || ritual.phase == RitualPhase.Chanting,
        drifting = drifting,
        markerDegrees = marker,
        ring = when (ritual.phase) {
            RitualPhase.Seeking -> RingMode.Seeking
            RitualPhase.Locking -> RingMode.Locking
            else -> RingMode.Clock
        },
        lockFraction = if (ritual.phase == RitualPhase.Locking) (ritual.lockMs.toFloat() / LOCK_HOLD_MS).coerceIn(0f, 1f) else 0f,
        clockFraction = if (ritual.phrase in 1 until PHRASE_COUNT) ritual.focusFraction else 1f,
        pill = DirectionPill(pillLook, if (pointing) turnDirection(azimuth, target) else null),
        seals = seals(ritual, verdict),
        checking = checking && !done,
        slot = when {
            done || gated -> SlotContent.Empty
            cracked -> SlotContent.Cracked
            feedback?.kind == LineFeedback.Kind.Sealed -> SlotContent.Sealed(feedback.score)
            feedback?.kind == LineFeedback.Kind.Missed -> SlotContent.Missed(feedback.score ?: 0, feedback.heard)
            checking -> SlotContent.Checking
            else -> SlotContent.Meter
        },
        tiltDegrees = if (pointing && marker != null) marker.coerceIn(-SIGIL_TILT_MAX_DEG, SIGIL_TILT_MAX_DEG) else 0f,
        gated = gated,
    )
}

/** While chanting: lit syllables, the current word, and words heard (covered by checks, or fully lit in lenient mode). */
private fun chantingWords(ritual: RitualState, text: List<List<String>>): List<ChantWord> {
    var start = 0
    return text.mapIndexed { i, syllables ->
        val lit = (ritual.lit - start).coerceIn(0, syllables.size)
        val current = ritual.lit in start until start + syllables.size
        start += syllables.size
        val heard = if (ritual.mode == CheckMode.Whisper) i in ritual.covered else lit == syllables.size
        val look = when {
            heard -> WordLook.Heard
            current -> WordLook.Current
            else -> WordLook.Pending
        }
        ChantWord(syllables, lit, look)
    }
}

private fun seals(ritual: RitualState, verdict: ChantVerdict?): List<SealLook> = (0 until PHRASE_COUNT).map { p ->
    when {
        verdict?.kind == ChantVerdict.Kind.Cracked && p == verdict.phrase -> SealLook.Cracked
        p < ritual.phrase -> SealLook.Sealed
        p == ritual.phrase -> SealLook.Current
        else -> SealLook.Pending
    }
}
```

- [ ] **Step 3: Check that the state compiles in isolation**

The full test task will not compile until Task 7 (the composables still use `status` and `verdict`). Go on to Task 5; the tests of this task run in Task 7, Step 5.

---

### Task 5: The shared pieces

**Files:**
- Create: `app/src/main/res/drawable/ic_lock.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt`

- [ ] **Step 1: Lock glyph**

`ic_lock.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z" />
</vector>
```

- [ ] **Step 2: Strings**

In `strings.xml`, delete `<string name="chant_step_chant">Chant</string>` and add next to the other `chant_` strings:

```xml
    <string name="chant_paused">Paused</string>
    <string name="chant_a11y_paused">Paused. Turn back to face %1$s</string>
    <string name="chant_checking">Checking…</string>
    <string name="chant_heard">heard: “%1$s”</string>
    <string name="chant_a11y_locked">Face the demon to chant</string>
```

and next to `debug_check_lenient`:

```xml
    <string name="debug_layout_split">Layout: split</string>
    <string name="debug_layout_centred">Layout: centred</string>
```

- [ ] **Step 3: Write `ChantPieces.kt`**

`CornerButtons`, `RoundButton` and `BelowAnchor` move unchanged from `ChantScreen.kt` (formerly `SlabButtons`). `Phrase`, `drawWordLines` and `VoiceMeter` move from there too: `Phrase` gains `onPhraseSize`. The seal mark is the old `Seal` from `RitualStatusBlock.kt`, simplified to one arc.

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlin.math.abs

// --- Sizes and tuning (ADR-0019, ADR-0029) ---
private val NameSize = 24.sp
private val SealSize = 26.dp
private val SealNumeralSize = 11.sp
private const val SEAL_STROKE_FRACTION = 0.065f
private val PillTextSize = 15.sp
private val PillBorder = 1.5.dp
private val SlotHeight = 32.dp
private val SlotTextSize = 18.sp
private val MeterWidth = 26.dp
private val MeterHeight = 22.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp
private val FrameWidth = 2.dp
private val FlareExtra = 4.dp
private val LockSize = 20.dp
private val RoundButtonSize = 36.dp
private val RoundButtonBorder = 1.5.dp
private val RoundButtonText = 16.sp
private val Dash = 4.dp
private val DashGap = 3.dp
private val DebugPanelWidth = 300.dp
private val DebugPanelBorder = 1.dp
private const val ROUND_BUTTON_ALPHA = 0.8f
private const val TILE_IN_RING = 0.62f
private const val SIGIL_IN_TILE = 0.92f
private const val TILE_ALPHA = 0.85f
private const val TILE_CORNER = 0.18f
private const val GLOW_ALPHA = 0.28f
private const val GHOST_ALPHA = 0.12f
private const val MARKER_FRACTION = 0.045f
private const val MARKER_HALF_WIDTH = 0.6f
private const val RING_STROKE_FRACTION = 0.035f
/** Within this many degrees the proximity arc turns from outline to the accent. */
private const val PROXIMITY_NEAR_DEG = 45f
private const val CLOCK_TWEEN_MS = 120
private const val TILT_TWEEN_MS = 250
private const val UNLOCK_FLARE_MS = 450
private const val FLARE_FILL_ALPHA = 0.18f
private const val GATED_ALPHA = 0.7f
private const val CURRENT_WORD_SCALE = 1.15f
private const val DIM_ALPHA = 0.55f
private const val LINE_FRACTION = 0.06f
private const val PENDING_LINE_SCALE = 0.7f
private const val BLINK_HALF_MS = 300
private const val BLINK_MIN = 0.55f
private const val BREATH_MIN = 0.55f
private const val BREATH_HALF_MS = 900
private const val FLARE_SCALE = 1.18f
private const val FLARE_MS = 450
private const val METER_TWEEN_MS = 120
private const val METER_FLOOR = 0.22f
private const val CHECKING_HALF_MS = 250
private const val CHECKING_LEVEL = 0.6f
private val METER_HEIGHTS = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)

// --- Corner buttons (ADR-0021; ADR-0029 §3.1) ---

/** "?" (the intro card) and, in debug builds, "D" (the debug panel); the panel floats under them. */
@Composable
internal fun CornerButtons(onHelp: (() -> Unit)?, debug: (@Composable () -> Unit)?) {
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

/** A small round button: solid outline, or dashed for debug only; filled while [selected]. */
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

// --- Title: name and seals (ADR-0029 §3.2) ---

/** The demon's name with the four seal marks; [stacked] puts the marks under the name (narrow slab). */
@Composable
internal fun ChantTitle(name: String, seals: List<SealLook>, checking: Boolean, paused: Boolean, accent: Color, stacked: Boolean) {
    val spacing = MaterialTheme.spacing
    val description = stringResource(R.string.chant_a11y_seals, seals.count { it == SealLook.Sealed })
    val nameText: @Composable () -> Unit = {
        Text(name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
    }
    val marks: @Composable () -> Unit = {
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            seals.forEachIndexed { i, look -> SealMark(ROMAN_NUMERALS[i], look, accent, paused, checking, Modifier.size(SealSize)) }
        }
    }
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            nameText()
            marks()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            nameText()
            marks()
        }
    }
}

/** A numeral in a ring: filled when sealed, error when cracked, amber when current and paused; the current one breathes. */
@Composable
private fun SealMark(numeral: String, look: SealLook, accent: Color, paused: Boolean, checking: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val current = look == SealLook.Current
    val breath = if (current && !paused && !reduceMotion) {
        rememberInfiniteTransition(label = "seal").animateFloat(
            1f, BREATH_MIN, infiniteRepeatable(tween(if (checking) BREATH_HALF_MS / 3 else BREATH_HALF_MS), RepeatMode.Reverse), label = "sealBreath",
        ).value
    } else {
        1f
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(look) {
        if (look == SealLook.Sealed && !reduceMotion) {
            pulse.snapTo(FLARE_SCALE)
            pulse.animateTo(1f, tween(FLARE_MS))
        }
    }
    val ring = when {
        look == SealLook.Cracked -> colors.error
        current && paused -> colors.primary
        look == SealLook.Pending -> colors.outlineVariant
        else -> accent
    }
    val numeralColor = when {
        look == SealLook.Sealed -> colors.surfaceContainerLowest
        look == SealLook.Cracked -> colors.error
        current && paused -> colors.primary
        look == SealLook.Pending -> colors.outline
        else -> accent
    }
    Box(
        modifier.graphicsLayer {
            alpha = breath
            scaleX = pulse.value
            scaleY = pulse.value
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * SEAL_STROKE_FRACTION
            val radius = size.minDimension / 2f - stroke
            if (look == SealLook.Sealed) drawCircle(accent, radius = radius)
            drawCircle(ring, radius = radius, style = Stroke(stroke))
        }
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = SealNumeralSize), color = numeralColor, maxLines = 1)
    }
}

// --- Direction pill (ADR-0029 §3.3) ---

@Composable
internal fun DirectionPillView(pill: DirectionPill, target: Float) {
    if (pill.look == PillLook.Hidden) return
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val direction = headingLabel(target)
    val color = when (pill.look) {
        PillLook.Facing -> MaterialTheme.demonColors.ritualSafe
        PillLook.Paused -> colors.primary
        PillLook.Seeking, PillLook.Hidden -> colors.outline
    }
    val arrow = when (pill.turn) {
        TurnDirection.Right -> "→"
        TurnDirection.Left -> "←"
        null -> ""
    }
    val label = when (pill.look) {
        PillLook.Paused -> "${stringResource(R.string.chant_paused).upper()} $arrow"
        PillLook.Facing -> "${stringResource(R.string.chant_face, direction).upper()} ✓"
        PillLook.Seeking, PillLook.Hidden -> "${stringResource(R.string.chant_face, direction).upper()} $arrow"
    }.trim()
    val description = when {
        pill.look == PillLook.Facing -> stringResource(R.string.chant_a11y_facing, direction)
        pill.look == PillLook.Paused -> stringResource(R.string.chant_a11y_paused, direction)
        pill.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    val blink = if (pill.look == PillLook.Paused && !reduceMotion) {
        rememberInfiniteTransition(label = "pill").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "pillBlink",
        ).value
    } else {
        1f
    }
    Text(
        label,
        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = PillTextSize, letterSpacing = 0.12.em),
        color = color,
        maxLines = 1,
        modifier = Modifier
            .graphicsLayer { alpha = blink }
            .border(PillBorder, color, CircleShape)
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
            .semantics { contentDescription = description },
    )
}

// --- Sigil (ADR-0020; ADR-0029 §3.4) ---

/**
 * The ring (proximity arc while seeking, lock hold, then focus clock; amber while paused) with the
 * marker pointing at the demon relative to the phone, around a glowing tile that tilts toward him.
 * Only the tile turns: the ring and marker stay in the phone's frame.
 */
@Composable
internal fun ChantSigil(state: RitualUiState, screen: ChantScreenState, accent: Color, flash: SigilFlash?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val reduceMotion = LocalReduceMotion.current
    val near = screen.markerDegrees?.let { abs(it) <= PROXIMITY_NEAR_DEG } == true
    val ringTarget = when {
        screen.drifting -> colors.primary
        screen.facing -> safe
        near -> accent
        else -> colors.outline
    }
    val ring by animateColorAsState(ringTarget, label = "ring")
    val marker by animateColorAsState(if (screen.facing) safe else accent, label = "marker")
    val sweep by animateFloatAsState(screen.clockFraction, tween(CLOCK_TWEEN_MS, easing = LinearEasing), label = "clock")
    val tilt by animateFloatAsState(screen.tiltDegrees, if (reduceMotion) snap() else tween(TILT_TWEEN_MS), label = "tilt")
    val track = colors.surfaceContainerHighest
    val tile = colors.surfaceContainerLowest
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val arcSize = Size(side, side)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val cap = Stroke(stroke, cap = StrokeCap.Round)
            when (screen.ring) {
                // From where the phone points (top) to the demon: shorter as the player gets closer.
                RingMode.Seeking -> screen.markerDegrees?.takeIf { it != 0f }?.let { drawArc(ring, -90f, it, false, topLeft, arcSize, style = cap) }
                RingMode.Locking -> if (screen.lockFraction > 0f) drawArc(ring, -90f, 360f * screen.lockFraction, false, topLeft, arcSize, style = cap)
                RingMode.Clock -> if (sweep > 0f) drawArc(ring, -90f, 360f * sweep, false, topLeft, arcSize, style = cap)
            }
            screen.markerDegrees?.let { degrees ->
                val length = size.minDimension * MARKER_FRACTION
                val tip = center.y - side / 2f + stroke
                rotate(degrees, pivot = center) {
                    drawPath(
                        Path().apply {
                            moveTo(center.x, tip)
                            lineTo(center.x + length * MARKER_HALF_WIDTH, tip + length)
                            lineTo(center.x - length * MARKER_HALF_WIDTH, tip + length)
                            close()
                        },
                        marker,
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize(TILE_IN_RING)
                .graphicsLayer { rotationZ = tilt }
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = GLOW_ALPHA), Color.Transparent)), radius = size.minDimension * 0.75f)
                    drawRoundRect(tile.copy(alpha = TILE_ALPHA), cornerRadius = CornerRadius(size.minDimension * TILE_CORNER))
                },
            contentAlignment = Alignment.Center,
        ) {
            InkedSigil(
                sigil = state.demon.sigil,
                inkProgress = state.ritual.inkProgress,
                accent = accent,
                rimText = state.demon.name.uppercase(),
                modifier = Modifier.fillMaxSize(SIGIL_IN_TILE),
                micLevel = state.micLevel,
                ghostAlpha = GHOST_ALPHA,
                flash = flash,
            )
        }
    }
}

// --- Feedback slot (ADR-0029 §3.5) ---

/** One fixed-height line above the phrase: meter, checking, score, or seal cracked. */
@Composable
internal fun FeedbackSlot(slot: SlotContent, accent: Color, micLevel: Float, paused: Boolean) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = SlotTextSize)
    Row(
        Modifier.fillMaxWidth().height(SlotHeight).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        when (slot) {
            SlotContent.Empty -> Unit
            SlotContent.Meter -> VoiceMeter(if (paused) 0f else micLevel, if (paused) colors.outline else accent, Modifier.size(MeterWidth, MeterHeight))
            SlotContent.Checking -> {
                CheckingMeter(accent)
                Text(stringResource(R.string.chant_checking).upper(), style = style, color = colors.onSurfaceVariant, maxLines = 1)
            }
            is SlotContent.Sealed -> Text(slot.score?.let { "$it % ✓" } ?: "✓", style = style, color = safe, maxLines = 1)
            is SlotContent.Missed -> {
                Text("${slot.score} %", style = style, color = colors.onSurface, maxLines = 1)
                if (slot.heard != null) {
                    Text(
                        stringResource(R.string.chant_heard, slot.heard),
                        style = style.copy(fontStyle = FontStyle.Italic),
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text("↺", style = style, color = colors.onSurfaceVariant)
            }
            SlotContent.Cracked -> {
                Text(stringResource(R.string.verdict_seal_cracked).upper(), style = style, color = colors.error, maxLines = 1)
                Text("✕", style = style, color = colors.error)
            }
        }
    }
}

/** The meter pulsing on its own while the phone checks what was said. */
@Composable
private fun CheckingMeter(color: Color) {
    val level = if (LocalReduceMotion.current) {
        CHECKING_LEVEL
    } else {
        rememberInfiniteTransition(label = "checking").animateFloat(
            METER_FLOOR, 1f, infiniteRepeatable(tween(CHECKING_HALF_MS), RepeatMode.Reverse), label = "checkingLevel",
        ).value
    }
    VoiceMeter(level, color, Modifier.size(MeterWidth, MeterHeight))
}

/** Five ember bars that follow the mic level. Shows that the mic hears you; decides nothing. */
@Composable
fun VoiceMeter(level: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(level.coerceIn(0f, 1f), tween(METER_TWEEN_MS), label = "meter")
    Canvas(modifier) {
        val bar = size.width / (METER_HEIGHTS.size * 2 - 1)
        METER_HEIGHTS.forEachIndexed { i, h ->
            val height = size.height * (METER_FLOOR + animated * h * (1f - METER_FLOOR))
            drawRoundRect(
                color,
                topLeft = Offset(i * 2 * bar, (size.height - height) / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}

// --- Line box (ADR-0029 §3.6) ---

/**
 * The phrase in a frame: closed (dim, lock glyph, outline) before the lock; on the lock the frame
 * turns the accent and flares once; amber while paused. [onPhraseSize] gets the auto-sized sp.
 */
@Composable
internal fun LineBox(
    words: List<ChantWord>,
    gated: Boolean,
    paused: Boolean,
    accent: Color,
    onPhraseSize: (Float) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val frame by animateColorAsState(
        when {
            gated -> colors.outlineVariant
            paused -> colors.primary
            else -> accent
        },
        label = "frame",
    )
    val flare = remember { Animatable(0f) }
    var wasGated by remember { mutableStateOf(gated) }
    LaunchedEffect(gated) {
        if (wasGated && !gated && !reduceMotion) {
            flare.snapTo(1f)
            flare.animateTo(0f, tween(UNLOCK_FLARE_MS))
        }
        wasGated = gated
    }
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .background(accent.copy(alpha = FLARE_FILL_ALPHA * flare.value), shape)
            .border(FrameWidth + FlareExtra * flare.value, frame, shape)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (gated) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.chant_a11y_locked),
                tint = colors.outline,
                modifier = Modifier.size(LockSize),
            )
        }
        Phrase(words, accent, onPhraseSize, Modifier.fillMaxWidth().weight(1f).graphicsLayer { alpha = if (gated) GATED_ALPHA else 1f })
    }
}

/** The phrase as one auto-sized text; a line under each word is drawn from the layout. */
@Composable
private fun Phrase(words: List<ChantWord>, accent: Color, onPhraseSize: (Float) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val dim = colors.onSurface.copy(alpha = DIM_ALPHA)
    val ranges = ArrayList<IntRange>(words.size)
    val text = buildAnnotatedString {
        words.forEachIndexed { wi, word ->
            if (wi > 0) append(' ')
            val start = length
            val sized = if (word.look == WordLook.Current) pushStyle(SpanStyle(fontSize = CURRENT_WORD_SCALE.em)) else null
            word.syllables.forEachIndexed { si, syllable ->
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
                withStyle(SpanStyle(color = color)) { append(syllable) }
            }
            if (sized != null) pop(sized)
            ranges += start until length
        }
    }
    val lineColors = words.map {
        when (it.look) {
            WordLook.Pending -> colors.outlineVariant
            WordLook.Current -> colors.onSurfaceVariant
            WordLook.Heard -> safe
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = text,
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(minFontSize = MinPhraseSize, maxFontSize = MaxPhraseSize, stepSize = PhraseStep),
        onTextLayout = {
            layout = it
            onPhraseSize(it.layoutInput.style.fontSize.value)
        },
        modifier = modifier.wrapContentHeight(Alignment.CenterVertically).drawBehind {
            layout?.let { drawWordLines(it, ranges, words, lineColors) }
        },
    )
}

private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
) {
    ranges.forEachIndexed { i, range ->
        if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return@forEachIndexed
        if (layout.getLineForOffset(range.first) != layout.getLineForOffset(range.last)) return@forEachIndexed
        val first = layout.getBoundingBox(range.first)
        val last = layout.getBoundingBox(range.last)
        val height = last.bottom - last.top
        val thickness = height * LINE_FRACTION
        val y = last.bottom + thickness
        val width = if (words[i].look == WordLook.Pending) thickness * PENDING_LINE_SCALE else thickness
        drawLine(lineColors[i], Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
    }
}
```

Nothing to run yet; it is compiled in Task 7.

---

### Task 6: The arrangements

**Files:**
- Create: `app/src/main/java/com/summondemons/safely/ui/screens/ChantArrangements.kt`

- [ ] **Step 1: Write `ChantArrangements.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

/** The shared pieces (ChantPieces.kt), handed to an arrangement (ADR-0029). */
internal class ChantParts(
    val buttons: @Composable () -> Unit,
    /** `stacked`: the seals under the name (narrow slab) or beside it. */
    val title: @Composable (stacked: Boolean) -> Unit,
    val pill: @Composable () -> Unit,
    val sigil: @Composable (Modifier) -> Unit,
    val slot: @Composable () -> Unit,
    val line: @Composable (Modifier) -> Unit,
)

private const val SLAB_WEIGHT = 0.25f
private const val SLAB_ALPHA = 0.7f
/** Centred: the sigil's side as a fraction of the height under the top row (ADR-0029). */
private const val SIGIL_CENTRED_FRACTION = 0.38f
private val TopRowHeight = 48.dp
private val BandSigil = 96.dp

/** Today's columns (ADR-0019): a 25 % slab with buttons, title, sigil and pill; the slot and line at the right. */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Surface(
            Modifier.fillMaxHeight().weight(SLAB_WEIGHT),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SLAB_ALPHA),
        ) {
            Column(
                Modifier.padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { parts.buttons() }
                parts.title(true)
                parts.sigil(Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true))
                parts.pill()
            }
        }
        Column(
            Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** Stitch 02: buttons and title on top, then pill, sigil, slot and the line across the full width. */
@Composable
internal fun CentredArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier) {
        val sigil = (maxHeight - TopRowHeight) * SIGIL_CENTRED_FRACTION
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(Modifier.fillMaxWidth().height(TopRowHeight)) {
                Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
                Box(Modifier.align(Alignment.Center)) { parts.title(false) }
            }
            parts.pill()
            parts.sigil(Modifier.size(sigil))
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** Large screens that ignore the rotation: a top band, then the slot and the line (not compared). */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            parts.buttons()
            parts.sigil(Modifier.size(BandSigil))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                parts.title(true)
                parts.pill()
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}
```

Compiled in Task 7.

---

### Task 7: Chant screen, debug switch and wiring

**Files:**
- Rewrite: `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`
- Delete: `app/src/main/java/com/summondemons/safely/ui/screens/RitualStatusBlock.kt`
- Create: `app/src/main/java/com/summondemons/safely/platform/DebugPrefs.kt`
- Modify: `app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt`
- Modify: `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`
- Modify: `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`

- [ ] **Step 1: Rewrite `ChantScreen.kt`**

`ChantBackdrop` is kept exactly as it is (`ChantIntro` uses it). Replace the whole file:

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.summondemons.safely.R
import com.summondemons.safely.ritual.ChantLayout
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.UiRecord
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val BACKDROP_DIM = 0.55f
private const val WORDS_SCRIM_ALPHA = 0.85f
/** The auto-sizer settles within a frame or two; wait this long before logging its size. */
private const val PHRASE_SETTLE_MS = 300L

/**
 * The chant screen (ADR-0019, ADR-0029): the shared pieces of ChantPieces.kt, arranged split or
 * centred (debug switch; ChantArrangements.kt), over the House stage. ✕ sits alone top-right.
 */
@Composable
fun ChantScreen(
    state: RitualUiState,
    flash: SigilFlash?,
    showClose: Boolean,
    onClose: () -> Unit,
    /** Debug controls, in a floating panel the "D" button opens (debug builds only; ADR-0021). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    /** Reopens the intro card ("?"); null hides the button. */
    onHelp: (() -> Unit)? = null,
    /** Shown instead of the line when the ritual can't hear or can't sense direction (ADR-0020). */
    blocker: (@Composable () -> Unit)? = null,
    /** The phrase size each arrangement settles on, for the debug log (ADR-0029). */
    onPhraseLaid: (UiRecord) -> Unit = {},
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val screen = remember(state.ritual, state.azimuth, state.checking, state.feedback) {
        chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading, state.checking, state.feedback)
    }
    var phraseSp by remember { mutableFloatStateOf(0f) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var sigilPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        val parts = ChantParts(
            buttons = { CornerButtons(onHelp, debug) },
            title = { stacked -> ChantTitle(demon.name, screen.seals, screen.checking, screen.drifting, accent, stacked) },
            pill = { DirectionPillView(screen.pill, demon.ritualHeading) },
            sigil = { m -> ChantSigil(state, screen, accent, flash, m.onSizeChanged { sigilPx = it.width }) },
            slot = { if (blocker == null) FeedbackSlot(screen.slot, accent, state.micLevel, screen.drifting) },
            line = { m ->
                if (blocker != null) {
                    Box(m, contentAlignment = Alignment.Center) { blocker() }
                } else {
                    LineBox(screen.words, screen.gated, screen.drifting, accent, onPhraseSize = { phraseSp = it }, modifier = m.onSizeChanged { box = it })
                }
            },
        )
        val content = Modifier.fillMaxSize().safeDrawingPadding().padding(MaterialTheme.spacing.lg)
        when {
            !landscape -> PortraitArrangement(parts, content)
            state.layout == ChantLayout.Centred -> CentredArrangement(parts, content)
            else -> SplitArrangement(parts, content)
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_abort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Logged once the size settles; any change restarts the wait (ADR-0029).
    val density = LocalDensity.current
    val phrase = state.ritual.phrase
    LaunchedEffect(state.layout, phrase, phraseSp, box, sigilPx, blocker == null) {
        if (phraseSp <= 0f || blocker != null) return@LaunchedEffect
        delay(PHRASE_SETTLE_MS)
        with(density) {
            onPhraseLaid(
                UiRecord(
                    layout = state.layout.name,
                    phrase = phrase,
                    phraseSp = phraseSp,
                    boxWidthDp = box.width.toDp().value.roundToInt(),
                    boxHeightDp = box.height.toDp().value.roundToInt(),
                    sigilDp = sigilPx.toDp().value.roundToInt(),
                ),
            )
        }
    }
}

/** The House stage, darkened most behind the words so the phrase keeps its contrast (also behind the intro card). */
@Composable
internal fun ChantBackdrop(stage: Stage, landscape: Boolean, modifier: Modifier) {
    val scrim = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(modifier) {
        StageBackground(stage, dim = BACKDROP_DIM, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val stops = arrayOf(
                0f to scrim.copy(alpha = BACKDROP_DIM),
                0.3f to scrim.copy(alpha = WORDS_SCRIM_ALPHA),
                1f to scrim.copy(alpha = WORDS_SCRIM_ALPHA),
            )
            drawRect(if (landscape) Brush.horizontalGradient(*stops) else Brush.verticalGradient(*stops))
        }
    }
}
```

Check the import of `StageBackground` in the old file (`grep -n StageBackground app/src/main/java -r`) and keep the same import if it lives in another package.

- [ ] **Step 2: Delete the status block**

```bash
git rm app/src/main/java/com/summondemons/safely/ui/screens/RitualStatusBlock.kt
```

- [ ] **Step 3: Debug pref, chip and wiring**

Create `platform/DebugPrefs.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ChantLayout

/** Debug-only switches that outlive a ritual (ADR-0029). Release builds never read them. */
object DebugPrefs {
    private const val PREFS = "debug"
    private const val KEY_LAYOUT = "chant_layout"

    fun chantLayout(context: Context): ChantLayout =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAYOUT, null)
            ?.let { name -> ChantLayout.entries.firstOrNull { it.name == name } }
            ?: ChantLayout.Split

    fun setChantLayout(context: Context, layout: ChantLayout) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAYOUT, layout.name).apply()
    }
}
```

In `DebugControls.kt`, add two parameters before `modifier`:

```kotlin
    onCycleClipLabel: () -> Unit,
    /** The chant screen's arrangement; the chip switches it live (ADR-0029). */
    layout: ChantLayout,
    onLayoutChange: (ChantLayout) -> Unit,
    modifier: Modifier = Modifier,
```

Call the chip right after `CheckModeChip(small)`:

```kotlin
        CheckModeChip(small)
        LayoutChip(layout, onLayoutChange, small)
```

and add, below `CheckModeChip`:

```kotlin
/** Debug: split ↔ centred, applied at once (ADR-0029). */
@Composable
private fun LayoutChip(layout: ChantLayout, onLayoutChange: (ChantLayout) -> Unit, style: TextStyle) {
    FilterChip(
        selected = layout == ChantLayout.Centred,
        onClick = { onLayoutChange(if (layout == ChantLayout.Split) ChantLayout.Centred else ChantLayout.Split) },
        label = {
            Text(
                stringResource(if (layout == ChantLayout.Split) R.string.debug_layout_split else R.string.debug_layout_centred),
                style = style,
            )
        },
    )
}
```

Import `com.summondemons.safely.ritual.ChantLayout`.

In `RitualScreen.kt`:
- `RitualScreen` passes two more arguments to `RitualContent`:

```kotlin
        onShowIntro = viewModel::openIntro,
        onLayoutChange = { layout ->
            DebugPrefs.setChantLayout(context, layout)
            viewModel.setLayout(layout)
        },
        onPhraseLaid = viewModel::onPhraseLaid,
        onAbort = onAbort,
```

- `RitualContent` gains the parameters `onLayoutChange: (ChantLayout) -> Unit` and `onPhraseLaid: (UiRecord) -> Unit` (after `onShowIntro`). It passes `onPhraseLaid = onPhraseLaid` to `ChantScreen`, and `layout = state.layout, onLayoutChange = onLayoutChange` to `DebugControls`.
- Imports: `com.summondemons.safely.platform.DebugPrefs`, `com.summondemons.safely.ritual.ChantLayout`, `com.summondemons.safely.ritual.UiRecord`.

In `AppNavHost.kt`, add after the `checkMode = { ... }` argument of `RitualViewModel.factory(...)`:

```kotlin
            // Debug builds remember the chant screen's arrangement from the debug panel (ADR-0029).
            layout = if (BuildConfig.DEBUG) DebugPrefs.chantLayout(appContext) else ChantLayout.Split,
```

and import `DebugPrefs` and `ChantLayout`.

- [ ] **Step 4: Build and lint**

Run: `./gradlew assembleDebug lintDebug`
Expected: BUILD SUCCESSFUL, with no new lint errors. Unused resources were removed in Task 5.

- [ ] **Step 5: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS, including `ChantScreenStateTest` (16 tests), `LineFeedbackTest`, `SessionLogTest` and `RitualViewModelTest`.

- [ ] **Step 6: Commit Tasks 4–7 together**

```bash
git add -A app/src/main app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt
git commit -m "feat(ui): Chant screen from shared pieces in split and centred arrangements behind a debug switch (ADR-0029)

Gated line box that unlocks with a flare, sigil tile that tilts toward the demon with a
proximity arc, one direction pill, title with seal marks, and one feedback slot above the
line (meter, checking, score with heard text, seal cracked). RitualStatusBlock is removed.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Phrase size table in the replay report

**Files:**
- Modify: `app/src/test/java/com/summondemons/safely/ritual/SessionReplayTest.kt`

- [ ] **Step 1: Add the table**

In `loggedSessionsReport`, after the `appendLine("== ${file.name} ...")` line and the notes loop, add:

```kotlin
                appendLine("   layout ${header.layout}")
                val sizes = ticks.mapNotNull { it.ui }
                if (sizes.isNotEmpty()) {
                    appendLine("   layout    line  phrase sp  box dp      sigil dp")
                    sizes.forEach { u ->
                        appendLine("   %-8s  %-4s  %9.0f  %4dx%-5d  %d".format(u.layout, ROMAN[u.phrase.coerceAtMost(3)], u.phraseSp, u.boxWidthDp, u.boxHeightDp, u.sigilDp))
                    }
                }
```

Update the class KDoc's first sentence to: "...and prints every verdict as it was on the phone and as it would be now, the aligned path, and the phrase size each chant layout reached (ADR-0029)."

- [ ] **Step 2: Run it**

Run: `./gradlew testDebugUnitTest --tests '*SessionReplayTest*' -i`
Expected: PASS. It is skipped without sessions; with older sessions it prints `layout Split` and no table.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/summondemons/safely/ritual/SessionReplayTest.kt
git commit -m "test(ritual): Replay report prints the phrase size per chant layout (ADR-0029)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Docs

**Files:**
- Create: `docs/decisions/0029-chant-screen-ux.md`
- Modify: `PRODUCT.md` (the "Chant screen" row, line 33)
- Modify: `DESIGN.md` (text-role row `ritualCue`; component rows `ChantScreen`, `RitualStatusBlock`, `ChantSigil`, `DebugControls`)
- Modify: `AGENTS.md` ("Current state")

- [ ] **Step 1: Write the ADR**

```markdown
# ADR-0029: Chant screen UX: shared pieces, two arrangements on trial

- **Status:** Accepted (arrangement pending the device comparison)
- **Date:** 2026-10-18
- **Amends:** ADR-0019 (chant screen), ADR-0020 (direction in the sigil), ADR-0024 (status column)
- **Spec:** `docs/superpowers/specs/2026-10-18-chant-screen-ux-design.md`

## Context

Play-testers did not see that they must face the direction before chanting, could not tell how
close they were, did not know how well a line went or what the phone heard, and missed that
turning away paused the chant. Two Stitch rounds (proposals 01, 02, 03, 08) gave the pieces; the
open question is whether a centred layout keeps the phrase as large as today's columns do.

## Decision

1. One place per fact: the direction pill is the only place that names the direction; the title
   carries the four seal marks; the slot above the line carries all feedback.
2. The line is gated before the lock (dim, lock glyph, outline frame) and unlocks with one flare.
3. The sigil tile tilts toward the demon (±35°) and the ring draws a proximity arc while seeking;
   after the lock the ring is the lock hold, then the focus clock.
4. A score (100 × (1 − distance); 40 % seals) shows for 2.5 s when an attempt ends: on a seal,
   or on a miss after the whole line was lit, with what the phone heard. Mid-line checks show only
   "checking".
5. Paused is amber, not red.
6. Split and centred arrangements of the same pieces sit behind a live debug switch; the session
   log records the layout and the phrase size reached. After the S24 comparison one is kept and
   the other, the switch and the layout fields are removed.

## Consequences

- `RitualStatusBlock` is removed; `ChantScreen` is split into pieces and arrangements.
- Release builds use the split arrangement until the comparison decides.
- Transcripts reach the screen for 2.5 s; they are still never stored or sent in release builds.

## Device comparison

_To be filled after the play-test: per demon and line, the phrase sp in each arrangement, and the
choice._
```

- [ ] **Step 2: PRODUCT.md "Chant screen" row**

Replace the row's description cell (from "For the whole Ritual, …" up to "…as a top band.") with:

```markdown
For the whole Ritual, a dedicated **landscape** screen (ADR-0019, ADR-0029) over the demon's House stage. **?** (rules) top-left, **✕** alone top-right. Pieces, each fact in one place: the demon's name with four seal marks I–IV (current one breathing, faster while the phone checks); a **direction pill** ("FACE EAST →" while turning, "FACE EAST ✓" once facing, amber "PAUSED ←" after turning away); the **sigil** on a glowing tile that tilts toward the demon, inside a ring that shows how far off you are while turning, then fills over the 0.6 s lock, then drains as the focus clock; a **feedback slot** directly above the line (voice meter; "CHECKING…"; "82 % ✓" on a seal; "25 % · heard: “…” ↺" when a whole recital missed; "SEAL CRACKED ✕"), each score shown 2.5 s; and the **line** in a frame, closed (dim, lock glyph) until the direction is locked, unlocking with one flare, its syllables lighting in turn with a green line under each heard word. Two arrangements are on trial behind a debug switch (split columns or centred); one will be kept (ADR-0029). Before every ritual the "How to chant" card comes down over it (ADR-0021). Large screens that ignore the rotation get a top band.
```

- [ ] **Step 3: DESIGN.md**

Replace the `ritualCue` text-role row's middle and right cells with:

```markdown
| `ritualCue` | Inter Bold, upper case, letter spacing 0.05 em; 15 sp with 0.12 em for the direction pill, 18 sp for the feedback slot | Short cues on the chant screen, read at mouth distance (ADR-0019, ADR-0029) |
```

Replace the `ChantScreen` row with:

```markdown
| `ChantScreen` | Landscape chant screen (ADR-0019, ADR-0029) | `state` (incl. `feedback`, `layout`), `flash`, `showClose`, `onClose`, `debug`, `onHelp`, `blocker`, `onPhraseLaid`; House-stage backdrop; pieces from `ChantPieces.kt` (`CornerButtons` "?"/"D" top-left, `ChantTitle`, `DirectionPillView`, `ChantSigil`, `FeedbackSlot`, `LineBox`) placed by `SplitArrangement` (25 % slab, surface-container-low 70 %, `large` shape) or `CentredArrangement` (sigil 38 % of the height under the top row); ✕ alone top-right. Portrait: `PortraitArrangement` top band. |
| `DirectionPillView` | The only place that names the direction (ADR-0029) | `pill`, `target`; `ritualCue` 15 sp in a 1.5 dp `CircleShape` frame: outline + arrow seeking, ritual-safe + ✓ facing, primary (amber) "PAUSED" + arrow blinking while paused; hidden in the climax |
| `FeedbackSlot` | One 32 dp line above the phrase (ADR-0029) | `slot`, `accent`, `micLevel`, `paused`; `VoiceMeter`; "CHECKING…" with a pulsing meter; score ✓ in ritual-safe; score · heard (italic, on-surface-variant, ellipsised) ↺; "SEAL CRACKED ✕" in error; polite live region |
| `LineBox` | The phrase in a 2 dp `large` frame (ADR-0029) | `words`, `gated`, `paused`, `accent`, `onPhraseSize`; gated: outline-variant frame, lock glyph, 70 % alpha; on unlock a 450 ms accent flare (no flare with reduced motion); paused: primary frame; phrase `incantationLarge` auto-sized 18–96 sp with a line per word (pending outline-variant, current on-surface-variant, heard ritual-safe) |
```

Replace the `RitualStatusBlock` row with:

```markdown
| ~~`RitualStatusBlock`~~ | Removed: the title's seal marks and the direction pill replace it (ADR-0029) |
```

Replace the `ChantSigil` row's right cell with:

```markdown
`InkedSigil` (12 % ghost before it inks) on a rounded surface-container-lowest tile (85 %) with an accent glow; the tile tilts toward the demon up to ±35° while seeking or paused (eased 250 ms; instant with reduced motion). Ring: proximity arc from the top to the marker while seeking (outline, accent within 45°), lock hold in ritual-safe, then the focus clock; primary (amber) while paused. Marker triangle between ring and tile pointing at the demon relative to the phone (ritual-safe when facing, accent otherwise). |
```

In the `DebugControls` row, after "Simulate heading, Hold to Chant", insert ", check mode, layout (split ↔ centred, live; ADR-0029)".

- [ ] **Step 4: AGENTS.md**

In "Current state", after the sentence about the chant check (ADR-0027), add:

```markdown
- Chant screen UX (ADR-0029, plan `docs/superpowers/plans/2026-10-18-chant-screen-ux.md`): shared
  pieces in split or centred arrangements behind a debug switch; the session log records the
  phrase size each reaches, printed by `SessionReplayTest`.
```

- [ ] **Step 5: Commit**

```bash
git add docs/decisions/0029-chant-screen-ux.md PRODUCT.md DESIGN.md AGENTS.md
git commit -m "docs(adr): ADR-0029 chant screen UX; PRODUCT, DESIGN and AGENTS updated

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Device check (with the user)

- [ ] **Step 1: Install**

Run: `./gradlew installDebug` (S24 connected).

- [ ] **Step 2: Walk the states on the device, in Split, then switch to Centred with the D panel**

Check each item in both arrangements:
- Before the lock: the line is dim with the lock glyph; the pill shows FACE … with the arrow; the tile tilts toward the demon and the ring arc shrinks as you turn.
- Lock: the ring fills green, the pill shows ✓, the line flares once and opens.
- Chanting: the meter shows in the slot; "CHECKING…" appears after a pause.
- Seal: "NN % ✓" for about 2.5 s, then the meter.
- A full recital of nonsense: "NN % · heard: “…” ↺" and the words clear.
- Turning away: amber "PAUSED ←", tile tilted, words kept, line frame amber.
- ? top-left; ✕ alone top-right; nothing overlaps.

- [ ] **Step 3: Verify the logged phrase size is real**

Run `tools/pull_sessions.sh`, copy the files to `app/src/test/sessions/`, then run `./gradlew testDebugUnitTest --tests '*SessionReplayTest*' -i`.
Expected: the table shows different `phrase sp` values for short and long lines. If every value is 96 (the auto-sizer's maximum), then `layoutInput.style.fontSize` does not hold the resolved size. In that case, change `Phrase`'s `onTextLayout` to derive it from the first line's height:

```kotlin
        onTextLayout = {
            layout = it
            val lineHeightPx = it.getLineBottom(0) - it.getLineTop(0)
            onPhraseSize(with(density) { (lineHeightPx / LINE_HEIGHT_EM).toSp().value })
        },
```

with `val density = LocalDensity.current` at the top of `Phrase` and `private const val LINE_HEIGHT_EM = 1.3f` (the `incantationLarge` line height). Commit as `fix(ui): Log the phrase size from the laid-out line height (ADR-0029)`.

- [ ] **Step 4: Comparison run (the user)**

Read each of the four lines of both demons once per arrangement at about 30 cm, with glasses. Pull the sessions and print the table. Then fill in ADR-0029's "Device comparison" section with the numbers and the user's choice. Removing the losing arrangement is a follow-up change: spec §8.3.
