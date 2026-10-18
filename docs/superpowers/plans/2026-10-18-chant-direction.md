# Chant Direction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the portrait altar's direction-finding into the chant screen's sigil, now that the chant screen covers the whole Ritual: a marker on the sigil's ring points at the demon, the ring fills during the lock hold, the sigil shows faintly before it inks, and the lines under it guide ("FIND THE DIRECTION", "HOLD STEADY"). Also bring back the altar's microphone/heading error panels and delete the altar's dead components (ADR-0020).

**Architecture:** `chantScreenState(...)` (pure, JVM-tested) gains `drifting`, `markerDegrees` (`signedDelta(azimuth, target)`, the same angle `AltarRing` used), `ring` (`RingMode.Seeking / Locking / Clock`) and `lockFraction`, plus two verdict lines. `ChantSigil` draws the marker and the lock fill from them; `DirectionCue` is red only while drifting. `ChantScreen` takes a `blocker` slot that replaces the phrase with the error panel `RitualScreen` builds. No engine or rule changes.

**Tech Stack:** unchanged. **No new dependency, no new art.** Decision record: `docs/decisions/0020-direction-in-the-sigil.md`; read it first.

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

**Verification gate** (run at the end of every phase, and wherever a task says "gate"):

```bash
./gradlew testDebugUnitTest lint assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Lint warnings are allowed; lint errors are not.

**Notes for the implementer**
- **Verified before this plan was written:** `ChantScreenState.kt` and
  `ChantScreenStateTest.kt` (Task 2) were compiled with Kotlin 2.2.10 against `main` at
  `3e80e98` and run with the unchanged `RitualEngineTest` and `ChantCheckTest`: 52 tests
  green. Every "replace … with …" edit was checked to match `main` exactly once.
- **Not compiled:** the Compose edits of Tasks 3–5 (no Android SDK at hand). They use only
  APIs already in these files plus `drawscope.rotate`. If a signature differs, keep the
  behaviour, adapt the call and say so in the commit message.
- An edit step quotes the exact current text; if it doesn't match exactly once, stop and
  re-read the file instead of guessing. A whole-file step means "replace the file with
  exactly this".

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `docs/decisions/0020-direction-in-the-sigil.md` | Exists | The decision (already committed) |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0019-chant-screen.md` | Modify | Canon |
| `MAIN/ui/screens/ChantScreenState.kt`, `TEST/ui/screens/ChantScreenStateTest.kt` | Replace | Marker angle, ring mode, lock fill, drifting, two verdict lines |
| `MAIN/ui/screens/ChantScreen.kt` | Modify | Marker and lock fill on `ChantSigil`, ghost sigil, line colours, verdict lines, `blocker` slot |
| `MAIN/ui/screens/RitualScreen.kt` | Modify | Error panels as the `blocker`; dead altar code removed |
| `app/src/main/res/values/strings.xml` | Modify | Two verdict lines; unused altar strings removed |
| `MAIN/ui/components/RomanNumerals.kt` | Create | `ROMAN_NUMERALS`, moved out of `ChantTablet.kt` |
| `MAIN/ui/components/AltarRing.kt`, `ChantTablet.kt`, `KaraokeLine.kt`, `MAIN/ui/screens/RitualCue.kt`, `TEST/ui/screens/RitualCueTest.kt` | Delete | The portrait altar's components |

---

## Phase 1 — Canon

### Task 1: PRODUCT.md, DESIGN.md, AGENTS.md, ADR-0019

**Files:**
- Modify: `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0019-chant-screen.md`

- [ ] **Step 1: Check ADR-0020 is present**

```bash
ls docs/decisions/0020-direction-in-the-sigil.md
```

- [ ] **Step 2: Edit `PRODUCT.md`**

(a) Replace:

```markdown
| Ritual setup | Full-bleed altar: a faint ghost of the demon's sigil inside the altar ring (a marker on the ring shows where his direction lies relative to the phone), a cue chip (e.g. "Turn left · Face East") and the chant tablet. |
```

with:

```markdown
| Ritual setup | The whole Ritual happens on the chant screen (ADR-0020). Its sigil guides to the direction: a marker on its ring points at the demon relative to the phone (at the top and green when facing him), the ring fills green during the 0.6 s lock, and the sigil shows faintly before it inks. Below it: "← FACE EAST" (neutral) until he is faced; under the phrase: "FIND THE DIRECTION", then "HOLD STEADY" while locking. Microphone or heading problems show their panel in place of the phrase. |
```

(b) Replace:

```markdown
From the lock until the Ritual closes, a dedicated **landscape** screen takes over (ADR-0019)
```

with:

```markdown
For the whole Ritual, a dedicated **landscape** screen (ADR-0019, ADR-0020)
```

(c) Replace:

```markdown
(red ring, pulsing sigil, voice ignored)
```

with:

```markdown
(red ring with the marker showing the way back, red blinking direction line, voice ignored)
```

- [ ] **Step 3: Edit `DESIGN.md`**

(a) Replace:

```markdown
| `AltarRing` | Ring around the sigil on the Ritual | `azimuth`, `target`, `phase`, `lockFraction`, `accent`; outline track, `ritual-safe` lock fill / locked ring, `error` while wavering, a marker toward the target |

```

with:

```markdown

```

(b) Replace:

```markdown
| `FocusBar` | Focus left under the current phrase | `fraction`, `tone` (normal / drift / court), `accent`; heartbeat below `FOCUS_LOW_FRACTION` |

```

with:

```markdown

```

(c) Replace:

```markdown
| `ChantSigil` | `InkedSigil` inside a ring | Ring colour = direction (ritual-safe / error), arc = focus clock (full on phrase I), seconds or "Paused" under it |
```

with:

```markdown
| `ChantSigil` | `InkedSigil` (12 % ghost before it inks) inside a ring | Marker triangle between ring and sigil pointing at the demon relative to the phone (ritual-safe when facing, accent otherwise; ADR-0020). Ring: track while seeking, ritual-safe fill with the lock hold, then the focus clock (full on phrase I); colour ritual-safe facing / outline seeking / error drifting; seconds or "Paused" under it |
```

(d) Replace:

```markdown
| `ChantTablet` | Framed chant panel |
```

with:

```markdown
| ~~`ChantTablet`~~ | Removed with the portrait altar (ADR-0020) |
```

(e) Replace:

```markdown
| `KaraokeLine` | Current chant phrase | `text`, `lit` syllables, `accent`, `dim`, `micLevel` (newest-syllable glow) |

```

with:

```markdown

```

- [ ] **Step 4: Edit `AGENTS.md`**

Replace:

```markdown
  `docs/superpowers/plans/2026-10-18-chant-check.md`. Landscape chant screen: ADR-0019,
  plan `docs/superpowers/plans/2026-10-18-chant-screen.md`.
```

with:

```markdown
  `docs/superpowers/plans/2026-10-18-chant-check.md`. Landscape chant screen: ADR-0019,
  plan `docs/superpowers/plans/2026-10-18-chant-screen.md`; it covers the whole Ritual and
  its sigil guides to the direction: ADR-0020, plan
  `docs/superpowers/plans/2026-10-18-chant-direction.md`.
```

- [ ] **Step 5: Edit `docs/decisions/0019-chant-screen.md`**

Replace:

```markdown
  "Portrait only" rule in PRODUCT.md.
```

with:

```markdown
  "Portrait only" rule in PRODUCT.md. Amended by ADR-0020: the chant screen covers the
  whole Ritual and its sigil carries the direction marker and the lock fill.
```

- [ ] **Step 6: Commit**

```bash
git add PRODUCT.md DESIGN.md AGENTS.md docs/decisions
git commit -q -m "docs(product): The chant screen's sigil guides to the direction (ADR-0020)" -m "$TRAILER"
```

---

## Phase 2 — Pure core (TDD)

### Task 2: Screen state for the marker, the ring and the guidance lines

**Files:**
- Replace: `MAIN/ui/screens/ChantScreenState.kt`, `TEST/ui/screens/ChantScreenStateTest.kt`

- [ ] **Step 1: Replace the test** (the existing tests are unchanged; four new ones at the end)

`app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.Formants
import com.summondemons.safely.ritual.ChantTake
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.Vowel
import com.summondemons.safely.ritual.WordMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantScreenStateTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val chanting = RitualState.start(90f, phrases, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f)

    private fun state(r: RitualState, azimuth: Float? = 90f) = chantScreenState(r, phrases, azimuth, 90f)

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
        assertEquals(VerdictLine.ChantAloud, s.verdict)
        assertEquals(0, s.numeral)
    }

    @Test
    fun aHeardWordGetsItsHintOnceItsSyllablesClose() {
        val good = listOf(Vowel.E, Vowel.O, Vowel.I, Vowel.U).map { it.formants() }
        val s = state(chanting.copy(lit = 5, take = ChantTake(nuclei = good)))
        assertEquals(WordLook.Heard, s.words[0].look)
        assertEquals(WordLook.Current, s.words[1].look)
        val bad = List(4) { Vowel.A.formants() }
        assertEquals(WordLook.Doubtful, state(chanting.copy(lit = 5, take = ChantTake(nuclei = bad))).words[0].look)
    }

    @Test
    fun aRejectionShowsTheDoubtfulWordsUntilChantingResumes() {
        val r = chanting.copy(lit = 0, verdict = ChantVerdict(ChantVerdict.Kind.Rejected, 0, listOf(WordMark.Heard, WordMark.Doubtful)))
        val s = state(r)
        assertEquals(listOf(WordLook.Heard, WordLook.Rejected), s.words.map { it.look })
        assertEquals(VerdictLine.ChantAgain, s.verdict)
    }

    @Test
    fun sealedShowsThePhraseJustSealed() {
        val r = chanting.copy(phrase = 1, focusMs = 9_000, focusBudgetMs = 9_000, verdict = ChantVerdict(ChantVerdict.Kind.Sealed, 0))
        val s = state(r)
        assertEquals(VerdictLine.Sealed, s.verdict)
        assertEquals(0, s.numeral)
        assertTrue(s.clockHeld)
        assertEquals(9, s.clockSeconds)
    }

    @Test
    fun aCrackSaysSo() {
        val r = chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0))
        assertEquals(VerdictLine.SealCracked, state(r).verdict)
    }

    @Test
    fun offCourseItPausesAndPointsTheWay() {
        val s = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertFalse(s.facing)
        assertEquals(TurnDirection.Right, s.turn)
        assertEquals(VerdictLine.Paused, s.verdict)
    }

    @Test
    fun phraseIHasAFullRingAndNoSeconds() {
        val s = state(chanting)
        assertTrue(s.facing)
        assertNull(s.turn)
        assertEquals(1f, s.clockFraction)
        assertNull(s.clockSeconds)
        assertFalse(s.clockHeld)
    }

    @Test
    fun afterTheFourthSealEveryWordIsHeard() {
        val s = state(chanting.copy(phase = RitualPhase.Climax, phrase = 4))
        assertTrue(s.words.all { it.look == WordLook.Heard })
        assertEquals(3, s.numeral)
    }

    // --- Direction in the sigil (ADR-0020) ---

    private val seeking = RitualState.start(90f, phrases, judged = true)

    @Test
    fun beforeTheLockTheLineGuidesToTheDirection() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertEquals(VerdictLine.FindDirection, s.verdict)
        assertEquals(RingMode.Seeking, s.ring)
        assertFalse(s.facing)
        assertFalse("not an error before the lock", s.drifting)
    }

    @Test
    fun theMarkerPointsAtTheDemonRelativeToThePhone() {
        // Demon at 90° (East): facing North puts him a quarter turn clockwise, facing South a quarter turn back.
        assertEquals(90f, state(seeking.copy(azimuth = 0f), azimuth = 0f).markerDegrees!!, 0.01f)
        assertEquals(-90f, state(seeking.copy(azimuth = 180f), azimuth = 180f).markerDegrees!!, 0.01f)
        assertEquals(0f, state(seeking.copy(azimuth = 90f), azimuth = 90f).markerDegrees!!, 0.01f)
        assertNull(state(seeking, azimuth = null).markerDegrees)
    }

    @Test
    fun lockingFillsTheRingAndAsksToHoldSteady() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(VerdictLine.HoldSteady, s.verdict)
    }

    @Test
    fun afterTheLockTheRingIsTheClockAndDriftingIsRed() {
        assertEquals(RingMode.Clock, state(chanting).ring)
        assertEquals(0f, state(chanting).lockFraction, 0f)
        val drift = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertTrue(drift.drifting)
        assertEquals(90f, drift.markerDegrees!!, 0.01f)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: compilation fails (`RingMode`, `markerDegrees`, `VerdictLine.FindDirection` unresolved).

- [ ] **Step 3: Replace the mapper**

`app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantJudge
import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.WordMark
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ritual.turnDirection
import kotlin.math.ceil

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard, Doubtful, Rejected }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** The one line under the words (ADR-0019); before the lock it guides to the direction (ADR-0020). */
enum class VerdictLine { FindDirection, HoldSteady, ChantAloud, Paused, Sealed, ChantAgain, SealCracked }

/** What the sigil's outer ring shows (ADR-0020): the track while seeking, the hold while locking, then the focus clock. */
enum class RingMode { Seeking, Locking, Clock }

/** Everything the chant screen draws, derived from the ritual (pure; ADR-0019). */
data class ChantScreenState(
    val words: List<ChantWord>,
    val facing: Boolean,
    /** Which way to turn while not facing the demon; null when facing or unknown. */
    val turn: TurnDirection?,
    /** Off course after the lock (wavering): the direction line and the ring turn red. */
    val drifting: Boolean,
    /**
     * Where the demon is relative to where the phone faces, in degrees clockwise in
     * (−180, 180]: the marker on the sigil's ring (0 = at the top). Null when the heading is unknown.
     */
    val markerDegrees: Float?,
    val ring: RingMode,
    /** How much of the 0.6 s lock hold is done, 0..1 (only while locking). */
    val lockFraction: Float,
    val verdict: VerdictLine,
    /** Phrase shown as "{I..IV} of IV", 0-based. */
    val numeral: Int,
    /** The sigil ring's arc: focus left, or 1 on phrase I (no clock). */
    val clockFraction: Float,
    /** Whole seconds left, or null on phrase I. */
    val clockSeconds: Int?,
    /** The clock waits while a fresh verdict is on screen. */
    val clockHeld: Boolean,
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

fun chantScreenState(ritual: RitualState, phrases: List<String>, azimuth: Float?, target: Float): ChantScreenState {
    val phrase = ritual.phrase.coerceAtMost(PHRASE_COUNT - 1)
    val verdict = ritual.verdict?.takeIf { ritual.phrase < PHRASE_COUNT }
    val text = chantWords(phrases[phrase])
    val words = when {
        ritual.phrase >= PHRASE_COUNT -> text.map { ChantWord(it, it.size, WordLook.Heard) }
        verdict?.kind == ChantVerdict.Kind.Rejected -> text.mapIndexed { i, syllables ->
            val look = if (verdict.marks.getOrNull(i) == WordMark.Doubtful) WordLook.Rejected else WordLook.Heard
            ChantWord(syllables, 0, look)
        }
        else -> chantingWords(ritual, phrase, text)
    }
    val line = when {
        verdict?.kind == ChantVerdict.Kind.Rejected -> VerdictLine.ChantAgain
        verdict?.kind == ChantVerdict.Kind.Cracked -> VerdictLine.SealCracked
        verdict?.kind == ChantVerdict.Kind.Sealed -> VerdictLine.Sealed
        ritual.phase == RitualPhase.Seeking -> VerdictLine.FindDirection
        ritual.phase == RitualPhase.Locking -> VerdictLine.HoldSteady
        ritual.phase == RitualPhase.Wavering -> VerdictLine.Paused
        else -> VerdictLine.ChantAloud
    }
    val clock = ritual.phrase in 1 until PHRASE_COUNT
    return ChantScreenState(
        words = words,
        facing = ritual.aligned,
        turn = turnDirection(azimuth, target),
        drifting = ritual.phase == RitualPhase.Wavering,
        markerDegrees = azimuth?.let { signedDelta(it, target) },
        ring = when (ritual.phase) {
            RitualPhase.Seeking -> RingMode.Seeking
            RitualPhase.Locking -> RingMode.Locking
            else -> RingMode.Clock
        },
        lockFraction = if (ritual.phase == RitualPhase.Locking) (ritual.lockMs.toFloat() / LOCK_HOLD_MS).coerceIn(0f, 1f) else 0f,
        verdict = line,
        numeral = if (verdict?.kind == ChantVerdict.Kind.Sealed) verdict.phrase else phrase,
        clockFraction = if (clock) ritual.focusFraction else 1f,
        clockSeconds = if (clock) ceil(ritual.focusMs / 1_000.0).toInt() else null,
        clockHeld = clock && verdict?.holdsClock == true,
    )
}

/** While chanting: lit syllables, the current word, and hints for words already heard. */
private fun chantingWords(ritual: RitualState, phrase: Int, text: List<List<String>>): List<ChantWord> {
    val marks = ritual.vowels?.let { vowels ->
        ritual.wordSizes?.let { ChantJudge.wordMarks(vowels[phrase], ritual.take.nuclei, it[phrase]) }
    }
    var start = 0
    return text.mapIndexed { i, syllables ->
        val lit = (ritual.lit - start).coerceIn(0, syllables.size)
        val current = ritual.lit in start until start + syllables.size
        start += syllables.size
        val look = when {
            current -> WordLook.Current
            marks?.getOrNull(i) == WordMark.Heard -> WordLook.Heard
            marks?.getOrNull(i) == WordMark.Doubtful -> WordLook.Doubtful
            else -> WordLook.Pending
        }
        ChantWord(syllables, lit, look)
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: 13 tests pass. (`assembleDebug` fails until Task 3: `VerdictLine` gained two
values that `ChantScreen`'s `when` must handle. Don't commit yet.)

---

## Phase 3 — Screen

### Task 3: The sigil guides to the direction

Continue from Task 2 (same commit). The marker sits between the ring and the sigil,
pointing outwards at the demon; the ring fills with the lock hold, then becomes the
focus clock; the sigil shows at 12 % before it inks.

**Files:**
- Modify: `MAIN/ui/screens/ChantScreen.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`**

(a) Replace:

```kotlin
import androidx.compose.ui.graphics.drawscope.Stroke

```

with:

```kotlin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

```

(b) Replace:

```kotlin
private const val SIGIL_IN_RING = 0.84f

```

with:

```kotlin
private const val SIGIL_IN_RING = 0.84f
/** The sigil before it inks, as the portrait altar showed it (ADR-0020). */
private const val GHOST_ALPHA = 0.12f
/** Direction marker: its length as a fraction of the sigil's side, and its half-width as a fraction of its length. */
private const val MARKER_FRACTION = 0.045f
private const val MARKER_HALF_WIDTH = 0.6f

```

(c) Replace:

```kotlin
    val ringColor = if (screen.facing) MaterialTheme.demonColors.ritualSafe else colors.error

```

with:

```kotlin
    val safe = MaterialTheme.demonColors.ritualSafe
    // Red only once drifting after the lock; before it, finding the direction is not an error (ADR-0020).
    val ringColor = when {
        screen.facing -> safe
        screen.drifting -> colors.error
        else -> colors.outline
    }
    val lineColor = when {
        screen.facing -> safe
        screen.drifting -> colors.error
        else -> colors.onSurface
    }

```

(d) Replace:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, ringColor, Modifier.padding(top = spacing.sm))
```

with:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor, Modifier.padding(top = spacing.sm))
```

(e) Replace:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, ringColor)
```

with:

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor)
```

(f) Replace:

```kotlin
/** The sigil inside a ring that shows the direction (colour) and the focus clock (arc). */
```

with:

```kotlin
/**
 * The sigil inside a ring that guides to the direction and keeps time (ADR-0019, ADR-0020):
 * a marker points at the demon relative to the phone; the ring fills with the lock hold,
 * then drains as the focus clock; its colour says facing / seeking / drifting.
 */
```

(g) Replace:

```kotlin
    val ring by animateColorAsState(ringColor, label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            drawArc(track, 0f, 360f, false, topLeft, Size(side, side), style = Stroke(stroke))
            drawArc(ring, -90f, 360f * sweep, false, topLeft, Size(side, side), style = Stroke(stroke, cap = StrokeCap.Round))
        }
```

with:

```kotlin
    val ring by animateColorAsState(ringColor, label = "ring")
    val marker by animateColorAsState(if (screen.facing) MaterialTheme.demonColors.ritualSafe else accent, label = "marker")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            drawArc(track, 0f, 360f, false, topLeft, Size(side, side), style = Stroke(stroke))
            val arc = when (screen.ring) {
                RingMode.Seeking -> 0f
                RingMode.Locking -> screen.lockFraction
                RingMode.Clock -> sweep
            }
            if (arc > 0f) drawArc(ring, -90f, 360f * arc, false, topLeft, Size(side, side), style = Stroke(stroke, cap = StrokeCap.Round))
            // Between the ring and the sigil, pointing outwards at the demon (0° = top).
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
```

(h) Replace:

```kotlin
            modifier = Modifier.fillMaxSize(SIGIL_IN_RING),
            micLevel = state.micLevel,
            flash = flash,
        )
```

with:

```kotlin
            modifier = Modifier.fillMaxSize(SIGIL_IN_RING),
            micLevel = state.micLevel,
            ghostAlpha = GHOST_ALPHA,
            flash = flash,
        )
```

(i) Replace:

```kotlin
/** "FACE EAST ✓" in green, or "← FACE EAST" in red and blinking while off course. */
@Composable
private fun DirectionCue(screen: ChantScreenState, target: Float, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (!screen.facing && !reduceMotion) {
```

with:

```kotlin
/** "FACE EAST ✓" in green; "← FACE EAST" neutral while seeking, red and blinking while drifting (ADR-0020). */
@Composable
private fun DirectionCue(screen: ChantScreenState, target: Float, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (screen.drifting && !reduceMotion) {
```

(j) Replace:

```kotlin
        VerdictLine.Sealed -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused -> colors.outline
    }
```

with:

```kotlin
        VerdictLine.Sealed, VerdictLine.HoldSteady -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused, VerdictLine.FindDirection -> colors.outline
    }
```

(k) Replace:

```kotlin
            VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
```

with:

```kotlin
            VerdictLine.FindDirection -> R.string.verdict_find_direction
            VerdictLine.HoldSteady -> R.string.verdict_hold_steady
            VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
```

- [ ] **Step 2: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="verdict_of">of</string>

```

with:

```xml
    <string name="verdict_of">of</string>
    <string name="verdict_find_direction">Find the direction</string>
    <string name="verdict_hold_steady">Hold steady</string>

```

- [ ] **Step 3: Gate, then commit Tasks 2–3 together**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens app/src/test/java/com/summondemons/safely/ui/screens app/src/main/res/values/strings.xml
git commit -q -m "feat(ui): The sigil guides to the direction: marker, lock fill, ghost (ADR-0020)" -m "$TRAILER"
```

### Task 4: The error panels, on the chant screen

When the microphone is denied (Retry / Open settings), unavailable, or the device has no
heading sensor, the altar's panel replaces the phrase and the verdict line.

**Files:**
- Modify: `MAIN/ui/screens/ChantScreen.kt`, `MAIN/ui/screens/RitualScreen.kt`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`**

(a) Replace:

```kotlin
    showTurnHint: Boolean = true,
) {
    val demon = state.demon
```

with:

```kotlin
    showTurnHint: Boolean = true,
    /** Shown instead of the phrase when the ritual can't hear or can't sense direction (ADR-0020). */
    blocker: (@Composable () -> Unit)? = null,
) {
    val demon = state.demon
```

(b) Replace:

```kotlin
                Words(state, screen, accent, debug, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
```

with:

```kotlin
                Words(state, screen, accent, debug, blocker, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
```

(c) Replace:

```kotlin
                Words(state, screen, accent, debug, Modifier.fillMaxWidth().weight(1f))
```

with:

```kotlin
                Words(state, screen, accent, debug, blocker, Modifier.fillMaxWidth().weight(1f))
```

(d) Replace:

```kotlin
    debug: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Phrase(screen.words, accent, Modifier.fillMaxWidth().weight(1f))
        VerdictLine(screen, accent, state.micLevel)
```

with:

```kotlin
    debug: (@Composable () -> Unit)?,
    blocker: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (blocker != null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { blocker() }
        } else {
            Phrase(screen.words, accent, Modifier.fillMaxWidth().weight(1f))
            VerdictLine(screen, accent, state.micLevel)
        }
```

- [ ] **Step 2: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`**

(a) Replace:

```kotlin
    RequestLandscape(true)

```

with:

```kotlin
    RequestLandscape(true)
    // The altar's error panels, now in place of the phrase (ADR-0020).
    val blocker: (@Composable () -> Unit)? = when {
        inClimax -> null
        state.micGranted == false -> {
            {
                ErrorPanel(stringResource(R.string.mic_rationale)) {
                    PrimaryActionButton(
                        text = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_retry),
                        onClick = if (permanentlyDenied) onOpenSettings else onRetryPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        state.micGranted == true && state.micUnavailable -> {
            { ErrorPanel(stringResource(R.string.mic_unavailable)) }
        }
        state.headingUnavailable && !state.simulateHeading -> {
            { ErrorPanel(stringResource(R.string.heading_unavailable)) }
        }
        else -> null
    }

```

(b) Replace:

```kotlin
            showTurnHint = !state.introOffered,

```

with:

```kotlin
            showTurnHint = !state.introOffered,
            blocker = blocker,

```

- [ ] **Step 3: Gate, then commit**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens
git commit -q -m "fix(ui): Microphone and heading error panels on the chant screen (ADR-0020)" -m "$TRAILER"
```

### Task 5: Remove the portrait altar's leftovers

Nothing calls these since the chant screen covers the whole Ritual. Delete them; the
compiler and lint point at anything that still refers to them.

**Files:**
- Create: `MAIN/ui/components/RomanNumerals.kt`
- Delete: `MAIN/ui/components/AltarRing.kt`, `MAIN/ui/components/ChantTablet.kt`, `MAIN/ui/components/KaraokeLine.kt`, `MAIN/ui/screens/RitualCue.kt`, `TEST/ui/screens/RitualCueTest.kt`
- Modify: `MAIN/ui/screens/RitualScreen.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Move `ROMAN_NUMERALS` out of `ChantTablet.kt`** (the chant screen and the
  intro card import it from `com.summondemons.safely.ui.components`, so the imports don't change)

`app/src/main/java/com/summondemons/safely/ui/components/RomanNumerals.kt`:

```kotlin
package com.summondemons.safely.ui.components

/** Phrase numerals, I–IV. */
val ROMAN_NUMERALS = listOf("I", "II", "III", "IV")
```

- [ ] **Step 2: Delete the altar's components**

```bash
git rm app/src/main/java/com/summondemons/safely/ui/components/AltarRing.kt \
       app/src/main/java/com/summondemons/safely/ui/components/ChantTablet.kt \
       app/src/main/java/com/summondemons/safely/ui/components/KaraokeLine.kt \
       app/src/main/java/com/summondemons/safely/ui/screens/RitualCue.kt \
       app/src/test/java/com/summondemons/safely/ui/screens/RitualCueTest.kt
```

Keep `CueChip.kt` and `CueTone`: Home uses them.

- [ ] **Step 3: Remove the dead code in `RitualScreen.kt`**

Delete, and their now-unused imports:
- the private functions `rememberHeartbeat`, `cueText`, `chantHeaderText`;
- the constants `SIGIL_WIDTH_FRACTION`, `SIGIL_IN_RING_FRACTION`, `SIGIL_MAX_HEIGHT_FRACTION`,
  `GHOST_ALPHA`, `RING_ANIM_MS`, `HEARTBEAT_HALF_MS`, `WAVER_MIN_ALPHA`, `EMBER_ALPHA`,
  `NameSize`, `PRE_LOCK_PHASES` and `SEALED_PHASES`;
- in `RitualContent`: the `ringProgress`, `wavering` and `heartbeat` locals;
- the `RitualContent` parameters `flare`, `cracked` and `rejected` and what feeds them in
  `RitualScreen` (the `flare` / `cracked` / `rejected` marks and their launches), **but keep
  the `flash` (`SigilFlash`) logic**: the chant screen's sigil still flares on a seal and
  flashes red on a crack or a rejection.

Keep `ErrorPanel` (Task 4 uses it), `ClimaxOverlay`, `RequestLandscape` and the abandon
dialog. Build with `./gradlew assembleDebug`; fix any "unresolved" error by deleting the
remaining reference, never by restoring a deleted component.

- [ ] **Step 4: Remove strings only the altar used**

Run `./gradlew lint` and open `app/build/reports/lint-results-debug.html` → **UnusedResources**.
Delete each listed string that belonged to the altar (expected: the `cue_*` strings,
`hint_turn_left`, `hint_turn_right`, `hint_face`, `hint_hold_steady`,
`hint_summoning`, `chant_title`, `chant_sealed_count`, `chant_seconds`, `chant_drift`,
`focus_label`, the `cue_rejected` array). Check each with
`grep -rn "R.string.NAME\|R.array.NAME" app/src/main/java` before deleting; keep anything
still referenced.

- [ ] **Step 5: Gate, then commit**

Run the verification gate. Expected: `BUILD SUCCESSFUL`, no lint error, fewer
UnusedResources warnings.

```bash
git add -A app/src/main app/src/test
git commit -q -m "refactor(ui): Remove the portrait altar's components (ADR-0020)" -m "$TRAILER"
```

---

## Phase 4 — Device check

### Task 6: Device checklist

Install with `./gradlew installDebug` and check on a phone:

1. **Seeking:** the sigil shows faintly; a small triangle sits between its ring and the
   sigil, pointing towards the demon. Turning the phone moves it around the ring; when you
   face the demon it reaches the top and turns green, and the line reads "FACE EAST ✓".
   Before that the line is neutral (not red, not blinking): "← FACE EAST" or "→ FACE EAST"
   with the correct arrow. Under the phrase: "FIND THE DIRECTION · I of IV".
2. **Locking:** facing the demon, the ring fills green over about 0.6 s and the line under
   the phrase reads "HOLD STEADY"; turning away before it fills empties it.
3. **Chanting:** the ring becomes the focus clock as before; the marker stays at the top in
   green while you face him.
4. **Drifting:** turning away after the lock turns the ring red, the direction line red and
   blinking, and the marker points the way back; "PAUSED · I of IV" under the phrase.
5. **Simulate heading** (debug) puts the marker at the top in green.
6. **Microphone denied** (deny the permission): the panel with **Retry** (or **Open
   settings** after a permanent denial) replaces the phrase; granting it brings the phrase
   back. **No heading sensor** (emulator without sensors): its panel shows.
7. **Reduced motion:** the drifting line doesn't blink; the marker still moves.
8. Everything from the chant-screen checklist still works (intro card, verdicts, word
   lines, climax).

---

## Spec coverage

| ADR-0020 item | Task |
|---|---|
| Marker on the sigil ring (angle, colours, hidden without heading) | 2, 3 |
| Ring by phase: seeking track, lock fill, clock, red when drifting | 2, 3 |
| Faint sigil before it inks | 3 |
| Direction line neutral while seeking, red only while drifting | 2, 3 |
| "FIND THE DIRECTION" / "HOLD STEADY" | 2, 3 |
| Error panels on the chant screen | 4 |
| Remove the altar's components and strings; `ROMAN_NUMERALS` moved | 5 |
| Canon updates | 1 |
