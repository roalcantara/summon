# Chant Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the portrait chant tablet, from the lock on, with a dedicated **landscape chant screen** that a player can read at mouth distance: the demon's sigil shows progress, direction and the focus clock; the phrase fills the screen; one line says what is happening; words are marked as they are heard and stay marked after a rejection (ADR-0019).

**Architecture:** Pure, JVM-tested core first: the heading follows the screen rotation (`azimuthFromRotationMatrix(r, screenRotation)`); `LatinSyllables.wordSizes` and `ChantJudge.wordMarks` give per-word hints; `RitualState.verdict` keeps the last verdict (sealed / rejected with word marks / cracked) until the next syllable and holds the focus clock up to 3 s; the clock becomes 4 s + 0.75 s per syllable; `chantScreenState(...)` maps the ritual to exactly what the screen draws. Then Compose: two text roles, `ChantScreen` (backdrop, slab, `ChantSigil`, phrase with word lines, verdict line, `VoiceMeter`, turn-sideways hint), and `RitualScreen` switches to it from the lock and requests landscape. The activity handles the rotation itself so nothing restarts.

**Tech Stack:** unchanged: AGP 9.4.1 (Kotlin 2.2.10), Compose BOM 2026.09.00 (uses `BasicText(autoSize = TextAutoSize.StepBased(...))` from foundation), JUnit 4. **No new dependency, no new art** (the backdrop reuses `stage_sol` / `stage_venus` through `StageBackground`). Decision record: `docs/decisions/0019-chant-screen.md`; read it first. Approved design: the claude.ai artifact "Chant Screen Prototype", version 6 (ask the product owner for the link if you want to see it; the ADR describes it fully).

**Prerequisite:** `docs/superpowers/plans/2026-10-18-chant-check.md` Tasks 1–9 are on `main` (the chant check, ADR-0018). Its Task 11 (calibration with tester clips) may run before or after this plan; Task 10 below adds two numbers to calibrate.

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
- **What was verified before this plan was written.** Every Kotlin file shown whole in
  Phase 2 and every test (new or extended) was compiled with Kotlin 2.2.10 and run on the
  JVM against the current `main`: 110 tests green (the ritual, judge, synthetic, heading,
  syllable, recorder and screen-state suites), no compiler warnings. Copy them exactly.
  Every "replace … with …" edit was checked to match the current file exactly once.
- **What was not compiled:** `ChantScreen.kt` (Task 7) and the Compose/Android edits of
  Tasks 2, 6 and 8 (no Android SDK at hand). They only use APIs this repo already uses,
  plus `BasicText(autoSize = …)`. If a signature differs in BOM 2026.09.00, keep the
  behaviour, adapt the call, and say so in the commit message.
- Every step that shows a whole file means "create or replace the file with exactly
  this". An edit step quotes the exact current text; if it doesn't match exactly once,
  stop and re-read the file instead of guessing.
- UI reads colors, type, shapes and spacing from the theme only (DESIGN.md). Constants
  that are not theme tokens live as `private const val` / `private val` at the top of
  the file that uses them (the existing pattern).
- Do not change `RitualConfig` numbers except where a task says so.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `docs/decisions/0019-chant-screen.md` | Exists | The decision (already committed) |
| `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0010-*.md`, `0014-*.md`, `0018-*.md` | Modify | Canon |
| `MAIN/ritual/HeadingMath.kt` | Replace | `azimuthFromRotationMatrix(r, screenRotation)` |
| `MAIN/platform/AndroidHeadingSource.kt` | Modify | Pass the display rotation |
| `MAIN/ritual/LatinSyllables.kt` | Replace | `wordSizes` |
| `MAIN/ritual/ChantJudge.kt` | Replace | `WordMark`, `wordMarks` |
| `MAIN/ritual/RitualConfig.kt`, `RitualState.kt`, `RitualEngine.kt` | Replace | Longer clock, `ChantVerdict`, verdict hold |
| `MAIN/ui/screens/ChantScreenState.kt` | Create | Pure mapper: ritual → what the chant screen draws |
| `MAIN/ui/theme/TextRoles.kt`, `app/src/main/res/values/strings.xml` | Modify | `ritualCue`, `incantationLarge`; chant screen copy |
| `MAIN/ui/screens/ChantScreen.kt` | Create | The landscape chant screen |
| `MAIN/ui/screens/RitualScreen.kt`, `app/src/main/AndroidManifest.xml` | Modify | Switch to the chant screen, landscape, climax fit, rotation without restart |
| `TEST/ritual/RotationAzimuthTest.kt`, `LatinSyllablesTest.kt`, `ChantJudgeTest.kt`, `RitualEngineTest.kt` | Modify | New cases; clock numbers |
| `TEST/ritual/ChantCheckTest.kt` | Replace | Verdict hold replaces "a rejection drains" |
| `TEST/ui/screens/ChantScreenStateTest.kt` | Create | Mapper tests |

---

## Phase 1 — Canon

### Task 1: PRODUCT.md, DESIGN.md, ADR amendments, AGENTS.md

**Files:**
- Modify: `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, `docs/decisions/0010-focus-clock.md`, `docs/decisions/0014-home-and-text-roles.md`, `docs/decisions/0018-chant-plausibility-check.md`

- [ ] **Step 1: Check ADR-0019 is present**

```bash
ls docs/decisions/0019-chant-screen.md && ls docs/decisions | tail -2
```
Expected: the path; the highest number is `0019`.

- [ ] **Step 2: Edit `PRODUCT.md`**

(a) Replace:

```markdown
| Ritual setup | Full-bleed altar: a faint ghost of the demon's sigil inside the altar ring (a marker on the ring shows where his direction lies relative to the phone), a cue chip (e.g. "Turn left · Face East") and the chant tablet. |
```

with:

```markdown
| Ritual setup | Full-bleed altar: a faint ghost of the demon's sigil inside the altar ring (a marker on the ring shows where his direction lies relative to the phone), a cue chip (e.g. "Turn left · Face East") and the chant tablet. |
| Chant screen | From the lock until the Ritual closes, a dedicated **landscape** screen takes over (ADR-0019), over the demon's House stage darkened behind the words. Left (25 %), on a soft slab: the demon's name, his sigil at full width (it inks as he is chanted; its outer ring is green while facing him, red when not, and drains as the focus clock), and one line: "FACE EAST ✓" (green) or "← FACE EAST" (red, blinking, arrow = way to turn). Right: the current phrase as large as fits, syllables split by dots, a line under each word (grey pending, green heard, dimmer amber doubtful, red wavy with ✕ when rejected), and one line: "CHANT ALOUD · I of IV" with a voice meter, "PAUSED", "SEALED ✓", "CHANT AGAIN ✕" or "SEAL CRACKED ✕". On entering, a short hint asks to turn the phone sideways and keep the microphone clear. Large screens that ignore the rotation get the slab as a top band. |
```

(b) Replace:

```markdown
| Chant | The incantation is **exactly 4 phrases** on the chant tablet: sealed phrases in the demon's colour with a filled medallion, the current one lit syllable by syllable (karaoke), upcoming ones dim.
```

with:

```markdown
| Chant | The incantation is **exactly 4 phrases**, read on the chant screen one at a time, the current one lit syllable by syllable (karaoke). Before the lock the chant tablet previews them dim.
```

(c) Replace:

```markdown
A match **seals** the phrase (chime + haptic); otherwise the demon scoffs (rotating cue line, warning haptic, red flash) and the phrase reopens with nothing lit. A rejection is not a crack and costs only time. |
```

with:

```markdown
A match **seals** the phrase (chime + haptic, "SEALED ✓"); otherwise the line reads "CHANT AGAIN ✕", the doubtful words turn red until the next syllable, the phone buzzes, that phrase's sigil strokes flash red and the phrase reopens with nothing lit. A rejection is not a crack and costs only time. Each word is also marked heard or doubtful as soon as its syllables are heard: a hint only, never the verdict (ADR-0019). |
```

(d) Replace:

```markdown
| Focus clock | Phrases II–IV each have a focus clock of 3 s + 0.5 s per syllable (+25 % in court). It drains while the phrase is open — also in silence — and 3× as fast while wavering.
```

with:

```markdown
| Focus clock | Phrases II–IV each have a focus clock of 4 s + 0.75 s per syllable (+25 % in court; ADR-0019). It drains while the phrase is open — also in silence — and 3× as fast while wavering, except that after a verdict (sealed, rejected, cracked) it waits up to 3 s, or until the next syllable.
```

(e) Replace:

```markdown
| Orientation | Portrait only (the compass reading assumes it). |
```

with:

```markdown
| Orientation | Portrait, except the chant screen, which is landscape (ADR-0019). The heading follows the screen's rotation. |
```

- [ ] **Step 3: Edit `DESIGN.md`**

(a) Replace:

```markdown
| `incantation` | Cinzel 15 sp Bold, line 22 sp, letter spacing 0.02 em | Every chant phrase on the tablet |
```

with:

```markdown
| `incantation` | Cinzel 15 sp Bold, line 22 sp, letter spacing 0.02 em | Every chant phrase on the tablet |
| `incantationLarge` | Cinzel Bold, line 1.3 em, letter spacing 0.02 em, auto-sized 18–96 sp | The phrase on the chant screen (ADR-0019) |
| `ritualCue` | Inter Bold, upper case, letter spacing 0.05 em; 18 sp (direction) or 22 sp (verdict) | The two lines of the chant screen, read at mouth distance (ADR-0019) |
```

(b) Replace:

```markdown
| `ChantTablet` | Framed chant panel |
```

with:

```markdown
| `ChantScreen` | Landscape chant screen (ADR-0019) | `state`, `flash`, `showClose`, `onClose`, `debug`; House-stage backdrop (scrim strongest behind the words), slab (surface-container-low 70 %, `large` shape) with name, `ChantSigil` and the direction line; the phrase (`incantationLarge`) with a line per word: pending outline-variant, current on-surface-variant, heard ritual-safe, doubtful primary 70 %, rejected error wavy + ✕ badge; verdict line (`ritualCue`) with `VoiceMeter`. Portrait (large screens) puts the slab in a top band. |
| `ChantSigil` | `InkedSigil` inside a ring | Ring colour = direction (ritual-safe / error), arc = focus clock (full on phrase I), seconds or "Paused" under it |
| `VoiceMeter` | Five ember bars | `level` (mic level, 0–1), `color`; flat at 22 % when silent; decides nothing |
| `ChantTablet` | Framed chant panel |
```

- [ ] **Step 4: Edit `docs/decisions/0010-focus-clock.md`**

Replace:

```markdown
- Status: Accepted (supersedes the drift grace of ADR-0008)
```

with:

```markdown
- Status: Accepted (supersedes the drift grace of ADR-0008); budget raised to 4 s + 0.75 s per syllable and a verdict hold added by ADR-0019
```

- [ ] **Step 5: Edit `docs/decisions/0014-home-and-text-roles.md`**

Replace:

```markdown
- Status: Accepted
```

with:

```markdown
- Status: Accepted; text roles `ritualCue` and `incantationLarge` added by ADR-0019 (chant screen)
```

- [ ] **Step 6: Edit `docs/decisions/0018-chant-plausibility-check.md`**

Replace:

```markdown
- Status: Accepted (direction); thresholds pending measurement. Amends ADR-0008.
```

with:

```markdown
- Status: Accepted (direction); thresholds pending measurement. Amends ADR-0008. The scoff cue is replaced by the chant screen's verdict line and word hints (ADR-0019).
```

- [ ] **Step 7: Edit `AGENTS.md`**

Replace:

```markdown
- Release prep: ADR-0017. Chant plausibility check (vowels + rhythm): ADR-0018, plan
  `docs/superpowers/plans/2026-10-18-chant-check.md`.
```

with:

```markdown
- Release prep: ADR-0017. Chant plausibility check (vowels + rhythm): ADR-0018, plan
  `docs/superpowers/plans/2026-10-18-chant-check.md`. Landscape chant screen: ADR-0019,
  plan `docs/superpowers/plans/2026-10-18-chant-screen.md`.
```

- [ ] **Step 8: Commit**

```bash
git add PRODUCT.md DESIGN.md AGENTS.md docs/decisions
git commit -q -m "docs(product): Landscape chant screen canon (ADR-0019)" -m "$TRAILER"
```

---

## Phase 2 — Pure core (TDD)

### Task 2: The heading follows the screen rotation

Upright (the phone's back) readings don't change. Flat or tilted, the "top" used for the
heading must be the **screen's** top, which in landscape is the device's ±X axis.

**Files:**
- Replace: `MAIN/ritual/HeadingMath.kt`
- Modify: `MAIN/platform/AndroidHeadingSource.kt`, `TEST/ritual/RotationAzimuthTest.kt`

- [ ] **Step 1: Write the failing tests**

Append inside `RotationAzimuthTest` (before the class's closing `}`):

```kotlin
    @Test
    fun landscapeFlatUsesTheScreensTop() {
        // Rolled ±90° flat: the screen's top is the device's ±X axis, not its Y axis.
        assertHeading(30f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = 90f), screenRotation = 3))
        assertHeading(30f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = -90f), screenRotation = 1))
        // Ignoring the rotation would read 90° off.
        assertHeading(300f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = 90f)))
    }

    @Test
    fun landscapeUprightUsesTheBack() {
        assertHeading(120f, azimuthFromRotationMatrix(pose(heading = 120f, tilt = 90f, roll = 90f), screenRotation = 3))
        assertHeading(120f, azimuthFromRotationMatrix(pose(heading = 120f, tilt = 85f, roll = -90f), screenRotation = 1))
    }

    @Test
    fun landscapeTiltedAgreesWithBothAxes() {
        assertHeading(200f, azimuthFromRotationMatrix(pose(heading = 200f, tilt = 45f, roll = 90f), screenRotation = 3))
    }
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*RotationAzimuthTest*'`
Expected: compilation fails (no `screenRotation` parameter).

- [ ] **Step 3: Replace `HeadingMath.kt`**

`app/src/main/java/com/summondemons/safely/ritual/HeadingMath.kt` (only
`azimuthFromRotationMatrix` and its KDoc change):

```kotlin
package com.summondemons.safely.ritual

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/** Smallest absolute difference between two headings, in [0, 180]. */
fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(normalizeDegrees(a) - normalizeDegrees(b))
    return if (diff > 180f) 360f - diff else diff
}

fun isAligned(
    azimuth: Float,
    target: Float,
    tolerance: Float = RitualConfig.HEADING_TOLERANCE_DEG,
): Boolean = angularDistance(azimuth, target) <= tolerance

private val COMPASS_POINTS = listOf(
    "North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West",
)

private val COMPASS_LETTERS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

private fun compassIndex(degrees: Float): Int = ((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size

/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String = COMPASS_POINTS[compassIndex(degrees)]

/** Abbreviation of [headingLabel] ("N", "NE"…), for the Direction badge glyph. */
fun headingLetter(degrees: Float): String = COMPASS_LETTERS[compassIndex(degrees)]

/**
 * Heading the player faces, in degrees [0, 360), from a rotation matrix as filled by
 * `SensorManager.getRotationMatrix*` (row-major, device → East/North/Up).
 *
 * `SensorManager.getOrientation` only uses the device's top (Y axis), whose horizontal
 * projection vanishes when the phone is held upright. Here the screen's top is used while
 * the phone is closer to flat and the back (−Z axis) while it is closer to upright —
 * whichever has the longer horizontal projection. Without roll both point the same way,
 * so the switch is seamless.
 *
 * [screenRotation] is the display rotation in quarter turns (`Surface.ROTATION_0..270` =
 * 0..3). The back doesn't depend on it; the screen's top does: device +Y in portrait,
 * −X at 90°, −Y at 180°, +X at 270° (the landscape chant screen, ADR-0019).
 */
fun azimuthFromRotationMatrix(r: FloatArray, screenRotation: Int = 0): Float {
    val (topEast, topNorth) = when (screenRotation and 3) {
        1 -> -r[0] to -r[3]
        2 -> -r[1] to -r[4]
        3 -> r[0] to r[3]
        else -> r[1] to r[4]
    }
    val backEast = -r[2]
    val backNorth = -r[5]
    val useBack = hypot(backEast, backNorth) > hypot(topEast, topNorth)
    val east = if (useBack) backEast else topEast
    val north = if (useBack) backNorth else topNorth
    return normalizeDegrees(Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat())
}

/** Exponential low-pass on the unit vector so 359° → 0° doesn't swing through 180°. */
class AzimuthSmoother(private val alpha: Float = 0.15f) {
    private var sinAcc = 0f
    private var cosAcc = 0f
    private var initialized = false

    fun next(degrees: Float): Float {
        val radians = Math.toRadians(degrees.toDouble())
        val s = sin(radians).toFloat()
        val c = cos(radians).toFloat()
        if (initialized) {
            sinAcc += alpha * (s - sinAcc)
            cosAcc += alpha * (c - cosAcc)
        } else {
            sinAcc = s
            cosAcc = c
            initialized = true
        }
        return normalizeDegrees(Math.toDegrees(atan2(sinAcc.toDouble(), cosAcc.toDouble())).toFloat())
    }
}

/** Signed shortest turn from [from] to [to], in (-180, 180]; positive = clockwise (turn right). */
fun signedDelta(from: Float, to: Float): Float {
    val delta = ((to - from) % 360f + 540f) % 360f - 180f
    return if (delta == -180f) 180f else delta
}

enum class TurnDirection { Left, Right }

/** Which way to turn to face [target]; null when the heading is unknown or already aligned. */
fun turnDirection(azimuth: Float?, target: Float): TurnDirection? = when {
    azimuth == null || isAligned(azimuth, target) -> null
    signedDelta(azimuth, target) > 0f -> TurnDirection.Right
    else -> TurnDirection.Left
}
```

- [ ] **Step 4: Edit `app/src/main/java/com/summondemons/safely/platform/AndroidHeadingSource.kt`**

(a) Replace:

```kotlin
import android.hardware.SensorManager

```

with:

```kotlin
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display

```

(b) Replace:

```kotlin
 * held flat (see [azimuthFromRotationMatrix]). Assumes portrait (the activity is locked to it).
```

with:

```kotlin
 * held flat, whichever way the screen is rotated (see [azimuthFromRotationMatrix]; the chant
 * screen is landscape, ADR-0019).
```

(c) Replace:

```kotlin
    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)

```

with:

```kotlin
    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)
    private val displayManager: DisplayManager? = context.getSystemService(DisplayManager::class.java)

    /** Current display rotation in quarter turns (`Surface.ROTATION_*`), read per reading. */
    private fun screenRotation(): Int = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

```

(d) Replace:

```kotlin
            trySend(smoother.next(azimuthFromRotationMatrix(rotationMatrix)))
```

with:

```kotlin
            trySend(smoother.next(azimuthFromRotationMatrix(rotationMatrix, screenRotation())))
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*RotationAzimuthTest*' --tests '*HeadingMathTest*'`
Expected: all pass (the existing portrait cases are unchanged: `screenRotation` defaults to 0).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/HeadingMath.kt app/src/main/java/com/summondemons/safely/platform/AndroidHeadingSource.kt app/src/test/java/com/summondemons/safely/ritual/RotationAzimuthTest.kt
git commit -q -m "feat(ritual): Heading follows the screen rotation (ADR-0019)" -m "$TRAILER"
```

### Task 3: Word hints

A word is marked heard or doubtful once all its syllables have been heard. It is only a
hint; the phrase verdict (ADR-0018) is unchanged.

**Files:**
- Replace: `MAIN/ritual/LatinSyllables.kt`, `MAIN/ritual/ChantJudge.kt`
- Modify: `TEST/ritual/LatinSyllablesTest.kt`, `TEST/ritual/ChantJudgeTest.kt`

- [ ] **Step 1: Write the failing tests**

Append inside `LatinSyllablesTest`:

```kotlin
    @Test
    fun wordSizesCountEachWordsSyllables() {
        assertEquals(listOf(4, 4), LatinSyllables.wordSizes("DAEMONIUM RESIDENTE,"))
        assertEquals(listOf(3, 1, 2), LatinSyllables.wordSizes("GRATIA SIT CURA."))
        DemonCatalog.all.flatMap { it.chantPhrases }.forEach { phrase ->
            assertEquals(phrase, LatinSyllables.count(phrase), LatinSyllables.wordSizes(phrase).sum())
        }
    }
```

Append inside `ChantJudgeTest`:

```kotlin
    @Test
    fun wordMarksJudgeEachWordOnceItsSyllablesAreHeard() {
        val sizes = LatinSyllables.wordSizes(phrase) // [4, 4]
        val good = expected.map { it?.formants(1.2) }
        assertEquals(listOf(WordMark.Pending, WordMark.Pending), ChantJudge.wordMarks(expected, good.take(3), sizes))
        assertEquals(listOf(WordMark.Heard, WordMark.Pending), ChantJudge.wordMarks(expected, good.take(5), sizes))
        val secondWrong = good.take(4) + List(4) { Vowel.A.formants(1.2) }
        assertEquals(listOf(WordMark.Heard, WordMark.Doubtful), ChantJudge.wordMarks(expected, secondWrong, sizes))
    }
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*LatinSyllablesTest*' --tests '*ChantJudgeTest*'`
Expected: compilation fails (`wordSizes`, `wordMarks`, `WordMark` unresolved).

- [ ] **Step 3: Replace `LatinSyllables.kt`** (adds `wordSizes`)

`app/src/main/java/com/summondemons/safely/ritual/LatinSyllables.kt`:

```kotlin
package com.summondemons.safely.ritual

/** Character range [start, end) of one syllable in the original text. */
data class SyllableSpan(val start: Int, val end: Int)

/**
 * Approximate Latin syllables for chant pacing and karaoke (ADR-0008). Not real
 * scansion: each diphthong (AE, OE, AU, EI, EU) or other single vowel is one nucleus;
 * a U after Q is a consonant; consonants join the following nucleus and the last
 * nucleus takes any trailing consonants.
 */
object LatinSyllables {
    private const val VOWELS = "AEIOUY"
    private val DIPHTHONGS = setOf("AE", "OE", "AU", "EI", "EU")
    private val WORD = Regex("[A-Za-z]+")

    fun count(text: String): Int = spans(text).size

    /** Syllables of each word of [text], in order; they sum to [count] (ADR-0019 word hints). */
    fun wordSizes(text: String): List<Int> = WORD.findAll(text).map { wordSpans(it.value.uppercase(), 0).size }.toList()

    fun spans(text: String): List<SyllableSpan> =
        WORD.findAll(text).flatMap { wordSpans(it.value.uppercase(), it.range.first) }.toList()

    private fun wordSpans(word: String, offset: Int): List<SyllableSpan> {
        val nucleusEnds = mutableListOf<Int>() // exclusive end of each nucleus
        var i = 0
        while (i < word.length) {
            if (isVowel(word, i)) {
                val diphthong = i + 1 < word.length && word.substring(i, i + 2) in DIPHTHONGS
                i += if (diphthong) 2 else 1
                nucleusEnds += i
            } else {
                i++
            }
        }
        if (nucleusEnds.isEmpty()) return listOf(SyllableSpan(offset, offset + word.length))
        var start = 0
        return nucleusEnds.mapIndexed { k, nucleusEnd ->
            val end = if (k == nucleusEnds.lastIndex) word.length else nucleusEnd
            SyllableSpan(offset + start, offset + end).also { start = end }
        }
    }

    private fun isVowel(word: String, i: Int): Boolean =
        word[i] in VOWELS && !(word[i] == 'U' && i > 0 && word[i - 1] == 'Q')
}
```

- [ ] **Step 4: Add the chant-screen constants to `RitualConfig`**

Task 4 replaces the whole file; for now add, after `const val CHANT_PAUSE_MS = 250L`:

```kotlin

    // Chant screen (ADR-0019)
    /** After a verdict (sealed, rejected, cracked) the focus clock waits this long, or until the next syllable. */
    const val VERDICT_HOLD_MS = 3_000L
    /** A word whose syllables average at least this vowel distance is marked doubtful (a hint only). */
    const val WORD_DOUBT_DISTANCE = 0.6
```

- [ ] **Step 5: Replace `ChantJudge.kt`** (adds `WordMark` and `wordMarks`)

`app/src/main/java/com/summondemons/safely/ritual/ChantJudge.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_FAST_INTERVAL_MS
import com.summondemons.safely.ritual.RitualConfig.CHANT_GAP_COST
import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import com.summondemons.safely.ritual.RitualConfig.CHANT_PAUSE_MS
import com.summondemons.safely.ritual.RitualConfig.CHANT_PAUSE_PENALTY
import com.summondemons.safely.ritual.RitualConfig.CHANT_RHYTHM_WEIGHT
import com.summondemons.safely.ritual.RitualConfig.CHANT_SYLLABLES_PER_PAUSE
import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.VOWEL_MAX_DISTANCE
import com.summondemons.safely.ritual.RitualConfig.VOWEL_SCALES
import com.summondemons.safely.ritual.RitualConfig.WORD_DOUBT_DISTANCE
import kotlin.math.ln
import kotlin.math.sqrt

/** The five Latin vowels with reference formants for an adult male voice (ADR-0018). */
enum class Vowel(val f1: Double, val f2: Double) {
    A(750.0, 1_250.0),
    E(480.0, 1_850.0),
    I(300.0, 2_250.0),
    O(500.0, 900.0),
    U(320.0, 750.0);

    /** This vowel's formants for a vocal tract [scale] times shorter than the reference. */
    fun formants(scale: Double = 1.0) = Formants(f1 * scale, f2 * scale)
}

/** A hint under each word of the phrase being chanted (ADR-0019); it never decides the verdict. */
enum class WordMark { Pending, Heard, Doubtful }

/** How one chanted phrase sounded: vowel match and rhythm in [0, 1], and their weighted total. */
data class ChantScore(val vowels: Float, val rhythm: Float) {
    val total: Float get() = (1f - CHANT_RHYTHM_WEIGHT) * vowels + CHANT_RHYTHM_WEIGHT * rhythm
    val passed: Boolean get() = total >= CHANT_PASS_SCORE
}

/**
 * What has been heard of the current phrase since its first lit syllable (ADR-0018).
 * Immutable; the engine advances it once per tick.
 */
data class ChantTake(
    /** Median formants of each closed syllable, in order (null = none measured). */
    val nuclei: List<Formants?> = emptyList(),
    /** Frames of the syllable lit last. */
    val current: SyllableFormants = SyllableFormants(),
    val sinceFirstMs: Long = 0L,
    /** Time from the first to the last lit syllable. */
    val litSpanMs: Long = 0L,
    /** Unvoiced runs of at least [CHANT_PAUSE_MS] between syllables. */
    val pauses: Int = 0,
    val silentMs: Long = 0L,
    /** ≥ 0 while the last syllable's vowel is still being heard before the verdict; -1 otherwise. */
    val closingMs: Long = -1L,
) {
    val closing: Boolean get() = closingMs >= 0L

    /** One nucleus per lit syllable, the last one included. */
    val heard: List<Formants?> get() = nuclei + current.median()

    /** A tick that lights no syllable. Nothing counts before the phrase's first syllable. */
    fun hear(voice: VoiceFrame, begun: Boolean): ChantTake {
        if (!begun) return this
        val t = copy(sinceFirstMs = sinceFirstMs + FRAME_MS, closingMs = if (closing) closingMs + FRAME_MS else -1L)
        return if (voice.voiced) {
            t.copy(current = current + voice.formants, pauses = pauses + pauseEnded(), silentMs = 0L)
        } else {
            t.copy(silentMs = silentMs + FRAME_MS)
        }
    }

    /** A tick that lights a syllable: the previous syllable closes and this frame starts the next. */
    fun light(voice: VoiceFrame, first: Boolean, last: Boolean): ChantTake {
        val t = if (first) ChantTake() else copy(
            nuclei = nuclei + current.median(),
            sinceFirstMs = sinceFirstMs + FRAME_MS,
            pauses = pauses + pauseEnded(),
        )
        return t.copy(
            current = SyllableFormants() + voice.formants,
            litSpanMs = t.sinceFirstMs,
            silentMs = 0L,
            closingMs = if (last) 0L else -1L,
        )
    }

    private fun pauseEnded(): Int = if (silentMs >= CHANT_PAUSE_MS) 1 else 0
}

/**
 * Judges whether a chanted phrase plausibly matches its text (ADR-0018). Not word
 * recognition: it compares the vowel colour of each syllable with the vowels the Latin
 * spelling predicts, and checks the pacing. Lenient by design: the goal is to reject
 * speech that clearly isn't the phrase.
 */
object ChantJudge {

    /** One expected vowel per syllable of [text], aligned with [LatinSyllables.spans]; null if a syllable has no vowel. */
    fun expectedVowels(text: String): List<Vowel?> = LatinSyllables.spans(text).map { span ->
        val syllable = text.substring(span.start, span.end).uppercase()
        val i = syllable.indices.firstOrNull { isVowel(syllable, it) } ?: return@map null
        val pair = syllable.substring(i, minOf(i + 2, syllable.length))
        when (pair) {
            "AE", "OE", "EI", "EU" -> Vowel.E
            "AU" -> Vowel.O
            else -> when (syllable[i]) {
                'A' -> Vowel.A
                'E' -> Vowel.E
                'I', 'Y' -> Vowel.I
                'O' -> Vowel.O
                else -> Vowel.U
            }
        }
    }

    fun score(expected: List<Vowel?>, take: ChantTake): ChantScore =
        ChantScore(vowelScore(expected, take.heard), rhythmScore(expected.size, take))

    /**
     * 1 − (alignment cost / expected length), best over the vocal-tract scales. Substituting
     * a heard nucleus for an expected vowel costs their formant distance (0..1); skipping
     * either side costs [CHANT_GAP_COST]; an unmeasured nucleus or vowel costs [CHANT_UNMEASURED_COST].
     */
    fun vowelScore(expected: List<Vowel?>, heard: List<Formants?>): Float {
        if (expected.isEmpty()) return 1f
        val best = VOWEL_SCALES.minOf { scale -> alignmentCost(expected, heard, scale) }
        return (1.0 - best / expected.size).coerceIn(0.0, 1.0).toFloat()
    }

    /** Distance in [0, 1] between a heard nucleus and an expected vowel at [scale]. */
    fun distance(heard: Formants?, expected: Vowel?, scale: Double): Double {
        if (heard == null || expected == null) return CHANT_UNMEASURED_COST
        val reference = expected.formants(scale)
        val d1 = ln(heard.f1 / reference.f1)
        val d2 = ln(heard.f2 / reference.f2)
        return (sqrt(d1 * d1 + d2 * d2) / VOWEL_MAX_DISTANCE).coerceAtMost(1.0)
    }

    /**
     * One mark per word: [WordMark.Pending] until all its syllables are heard, then
     * [WordMark.Heard] or [WordMark.Doubtful] (average vowel distance ≥ [WORD_DOUBT_DISTANCE]).
     * Heard nuclei match expected vowels by position, at the vocal-tract scale that best fits
     * everything heard so far. A word is too short to judge alone, so this is only a hint.
     */
    fun wordMarks(expected: List<Vowel?>, heard: List<Formants?>, wordSizes: List<Int>): List<WordMark> {
        val n = minOf(expected.size, heard.size)
        val scale = VOWEL_SCALES.minBy { sc -> (0 until n).sumOf { distance(heard[it], expected[it], sc) } }
        var start = 0
        return wordSizes.map { size ->
            val end = start + size
            val mark = when {
                end > n -> WordMark.Pending
                (start until end).sumOf { distance(heard[it], expected[it], scale) } / size >= WORD_DOUBT_DISTANCE -> WordMark.Doubtful
                else -> WordMark.Heard
            }
            start = end
            mark
        }
    }

    private fun alignmentCost(expected: List<Vowel?>, heard: List<Formants?>, scale: Double): Double {
        val n = expected.size
        val m = heard.size
        var previous = DoubleArray(m + 1) { it * CHANT_GAP_COST }
        for (i in 1..n) {
            val row = DoubleArray(m + 1)
            row[0] = i * CHANT_GAP_COST
            for (j in 1..m) {
                row[j] = minOf(
                    previous[j - 1] + distance(heard[j - 1], expected[i - 1], scale),
                    previous[j] + CHANT_GAP_COST,
                    row[j - 1] + CHANT_GAP_COST,
                )
            }
            previous = row
        }
        return previous[m]
    }

    /**
     * 1, minus [CHANT_PAUSE_PENALTY] per pause beyond 1 + syllables / [CHANT_SYLLABLES_PER_PAUSE],
     * and halved when syllables came faster than [CHANT_FAST_INTERVAL_MS] apart on average
     * (conversation, not chanting).
     */
    fun rhythmScore(syllables: Int, take: ChantTake): Float {
        val allowed = 1 + syllables / CHANT_SYLLABLES_PER_PAUSE
        var score = 1f - CHANT_PAUSE_PENALTY * maxOf(0, take.pauses - allowed)
        val intervals = take.heard.size - 1
        if (intervals > 0 && take.litSpanMs / intervals < CHANT_FAST_INTERVAL_MS) score *= 0.5f
        return score.coerceIn(0f, 1f)
    }

    private fun isVowel(s: String, i: Int): Boolean =
        s[i] in "AEIOUY" && !(s[i] == 'U' && i > 0 && s[i - 1] == 'Q')
}
```

- [ ] **Step 6: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*LatinSyllablesTest*' --tests '*ChantJudgeTest*'`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): Word hints for the chant screen (ADR-0019)" -m "$TRAILER"
```

### Task 4: Verdicts stay until the next syllable; longer clock

`RitualState.verdict` keeps the last verdict while the chant check is on, until a syllable
lights. While fresh (`VERDICT_HOLD_MS`) it holds the focus clock. A rejection no longer
drains on its own tick and keeps its word marks. The clock becomes 4 s + 0.75 s per
syllable. With the check off (most `RitualEngineTest` cases) nothing changes except the
two hard-coded budget numbers.

**Files:**
- Replace: `MAIN/ritual/RitualConfig.kt`, `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`, `TEST/ritual/ChantCheckTest.kt`
- Modify: `TEST/ritual/RitualEngineTest.kt`

- [ ] **Step 1: Replace `ChantCheckTest`** (the rejection-drain test becomes three verdict tests)

`app/src/test/java/com/summondemons/safely/ritual/ChantCheckTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.SEAL_LISTEN_MS
import com.summondemons.safely.ritual.RitualConfig.VERDICT_HOLD_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Cracked
import com.summondemons.safely.ritual.RitualEffect.Judged
import com.summondemons.safely.ritual.RitualEffect.Rejected
import com.summondemons.safely.ritual.RitualEffect.Sealed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine with the chant check on (ADR-0018), fed hand-made voice frames. */
class ChantCheckTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()

    private fun frame(vowel: Vowel?, onset: Boolean) =
        if (vowel == null) VoiceFrame.SILENT else VoiceFrame(3_000.0, voiced = true, onset = onset, formants = vowel.formants())

    private fun run(start: RitualState, frames: List<VoiceFrame>): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        frames.forEach {
            val step = RitualEngine.step(state, RitualInput(0f, it))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked() = run(RitualState.start(0f, phrases, judged = true), List(lockFrames) { VoiceFrame.SILENT }).state

    /** Each vowel as one 200 ms syllable (onset + 9 voiced frames), then 60 ms of silence. */
    private fun sing(vowels: List<Vowel>): List<VoiceFrame> =
        vowels.flatMap { v -> listOf(frame(v, true)) + List(9) { frame(v, false) } + List(3) { frame(null, false) } }

    private fun expected(phrase: Int) = ChantJudge.expectedVowels(phrases[phrase]).map { it!! }

    @Test
    fun startJudgedFillsTheVowels() {
        assertNull(RitualState.start(0f, phrases).vowels)
        assertEquals(phrases.map(ChantJudge::expectedVowels), RitualState.start(0f, phrases, judged = true).vowels)
    }

    @Test
    fun theRightVowelsSealAfterTheLastSyllableEnds() {
        val step = run(locked(), sing(expected(0)))
        assertEquals(1, step.state.phrase)
        assertEquals(0, step.state.lit)
        val judged = step.effects.filterIsInstance<Judged>().single()
        assertTrue(judged.score.passed)
        assertEquals(listOf(judged, Sealed(0)), step.effects.filter { it is Judged || it is Sealed })
        assertEquals(ChantTake(), step.state.take)
    }

    @Test
    fun theSealWaitsForTheLastVowel() {
        val vowels = expected(0)
        val upToLastOnset = sing(vowels).take((vowels.size - 1) * 13 + 1)
        val step = run(locked(), upToLastOnset)
        assertEquals(0, step.state.phrase)
        assertEquals(vowels.size, step.state.lit) // every syllable lit…
        assertTrue(step.state.take.closing) // …but still listening
        assertTrue(step.effects.none { it is Sealed })
    }

    @Test
    fun aHeldVowelIsJudgedAfterTheListenWindow() {
        val vowels = expected(0)
        val lastOnset = (vowels.size - 1) * 13
        val listen = (SEAL_LISTEN_MS / FRAME_MS).toInt()
        val held = sing(vowels).take(lastOnset + 1) + List(100) { frame(vowels.last(), false) }
        assertTrue(run(locked(), held.take(lastOnset + listen)).effects.none { it is Sealed })
        assertTrue(Sealed(0) in run(locked(), held.take(lastOnset + listen + 1)).effects)
    }

    @Test
    fun babbleIsRejectedAndThePhraseReopens() {
        val babble = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O)
        val step = run(locked(), sing(babble))
        assertEquals(0, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertFalse(step.effects.filterIsInstance<Judged>().single().score.passed)
        assertTrue(Rejected(0) in step.effects)
        assertTrue(step.effects.none { it is Sealed })
        assertTrue("a rejection is not a crack", step.state.flawless)
    }

    @Test
    fun afterARejectionTheRightChantStillSeals() {
        val babble = List(8) { Vowel.A }
        val step = run(locked(), sing(babble) + sing(expected(0)))
        assertEquals(1, step.state.phrase)
        assertEquals(listOf(Rejected(0), Sealed(0)), step.effects.filter { it is Rejected || it is Sealed })
    }

    @Test
    fun aVerdictHoldsTheClockUntilChantingResumesOrTheHoldEnds() {
        val second = run(locked(), sing(expected(0))).state
        val budget = second.focusMs
        assertEquals(ChantVerdict.Kind.Sealed, second.verdict!!.kind)
        // Silence: the clock waits out the rest of VERDICT_HOLD_MS, then drains.
        val holdFrames = ((VERDICT_HOLD_MS - second.verdict!!.heldMs) / FRAME_MS).toInt()
        assertEquals(budget, run(second, List(holdFrames) { VoiceFrame.SILENT }).state.focusMs)
        assertTrue(run(second, List(holdFrames + 5) { VoiceFrame.SILENT }).state.focusMs < budget)
        // A syllable clears the verdict and the clock runs again.
        val chanting = run(second, listOf(frame(Vowel.A, true)) + List(5) { frame(Vowel.A, false) }).state
        assertNull(chanting.verdict)
        assertTrue(chanting.focusMs < budget)
    }

    @Test
    fun aRejectionKeepsTheDoubtfulWordsAndHoldsTheClock() {
        val second = run(locked(), sing(expected(0)) + List(200) { VoiceFrame.SILENT }).state
        val step = run(second, sing(List(expected(1).size) { Vowel.U }))
        assertTrue(Rejected(1) in step.effects)
        val verdict = step.state.verdict!!
        assertEquals(ChantVerdict.Kind.Rejected, verdict.kind)
        assertTrue(WordMark.Doubtful in verdict.marks)
        val focus = step.state.focusMs
        assertEquals(focus, run(step.state, List(10) { VoiceFrame.SILENT }).state.focusMs)
    }

    @Test
    fun aCrackLeavesACrackedVerdict() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = FRAME_MS, verdict = null)
        val step = run(second, listOf(frame(null, false)))
        assertTrue(Cracked(0) in step.effects)
        assertEquals(ChantVerdict(ChantVerdict.Kind.Cracked, 0), step.state.verdict)
    }

    @Test
    fun aCrackWhileListeningClearsTheTake() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = FRAME_MS, verdict = null)
        val step = run(second, listOf(frame(Vowel.A, true), frame(Vowel.A, false)))
        assertTrue(Cracked(0) in step.effects)
        assertEquals(ChantTake(), step.state.take)
    }

    @Test
    fun theWholeRitualWithTheRightVowelsSummons() {
        val all = (0 until 4).flatMap { sing(expected(it)) + List(10) { VoiceFrame.SILENT } }
        val step = run(locked(), all)
        assertEquals(listOf(Sealed(0), Sealed(1), Sealed(2), Sealed(3)), step.effects.filterIsInstance<Sealed>())
        assertTrue(ClimaxStart(flawless = true) in step.effects)
    }

    @Test
    fun withoutTheCheckTheLastOnsetSealsAtOnce() {
        val unjudged = run(RitualState.start(0f, phrases), List(lockFrames) { VoiceFrame.SILENT }).state
        val vowels = expected(0)
        val step = run(unjudged, sing(vowels).take((vowels.size - 1) * 13 + 1))
        assertEquals(listOf(Sealed(0)), step.effects.filterIsInstance<Sealed>())
        assertEquals(ChantTake(), step.state.take)
    }
}
```

- [ ] **Step 2: Edit `app/src/test/java/com/summondemons/safely/ritual/RitualEngineTest.kt`**

Replace:

```kotlin
        assertEquals(12_000L, focusBudget(18, 1))
        assertEquals(7_500L, focusBudget(6, 1, 1.25f))
```

with:

```kotlin
        assertEquals(17_500L, focusBudget(18, 1))
        assertEquals(10_625L, focusBudget(6, 1, 1.25f))
```

- [ ] **Step 3: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantCheckTest*' --tests '*RitualEngineTest*'`
Expected: compilation fails (`ChantVerdict`, `verdict` unresolved).

- [ ] **Step 4: Replace `RitualConfig.kt`**

`app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt` (the clock numbers
change; the "Chant screen" block from Task 3 is included):

```kotlin
package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0008, ADR-0010, ADR-0018, ADR-0019). Per-demon parameters live in `Demon` (ADR-0006). */
object RitualConfig {
    const val PHRASE_COUNT = 4
    const val HEADING_TOLERANCE_DEG = 20f

    // Ritual 2.0 state machine (ADR-0008)
    const val FRAME_MS = 20L
    const val LOCK_HOLD_MS = 600L
    const val CLIMAX_MS = 2_500L
    const val ONSET_REFRACTORY_MS = 120L
    const val SUSTAIN_SYLLABLE_MS = 350L
    const val PROXIMITY_RANGE_DEG = 60f

    // Focus clock (ADR-0010, longer per ADR-0019) and the Court Hours bonus (ADR-0012)
    const val FOCUS_BASE_MS = 4_000L
    const val FOCUS_PER_SYLLABLE_MS = 750L
    const val FOCUS_DRIFT_MULTIPLIER = 3
    const val COURT_BONUS = 0.25f
    /** Below this focus fraction the focus bar beats. */
    const val FOCUS_LOW_FRACTION = 0.25f
    const val PROXIMITY_STEP_DEG = 5f

    // Voice analysis on PCM16 (ADR-0008)
    const val VOICE_RMS_FLOOR = 1_200.0
    const val VOICING_MIN_CORRELATION = 0.5
    const val VOICE_MIN_HZ = 70
    const val VOICE_MAX_HZ = 400
    /** Autocorrelation strides samples above this rate to keep the cost flat. */
    const val ANALYSIS_RATE = 16_000
    const val ONSET_RATIO = 1.8
    const val ONSET_REARM_RATIO = 0.6
    const val ONSET_WINDOW_FRAMES = 5

    // Formants by linear prediction (ADR-0018)
    /** Formant analysis decimates the mic to about this rate (F1/F2 live below 3 kHz). */
    const val FORMANT_RATE = 8_000
    const val LPC_ORDER = 10
    const val PRE_EMPHASIS = 0.94
    const val FORMANT_MAX_BANDWIDTH_HZ = 500.0
    const val FORMANT_F1_MIN_HZ = 200.0
    const val FORMANT_F1_MAX_HZ = 1_200.0
    const val FORMANT_F2_MAX_HZ = 3_400.0

    // Chant plausibility check (ADR-0018)
    /** Vocal-tract scales tried per phrase: adult male (1.0) to child (~1.45). */
    val VOWEL_SCALES = listOf(0.9, 1.0, 1.1, 1.2, 1.3, 1.45)
    /** Log-formant distance (natural log units) at which two vowels count as fully different. */
    const val VOWEL_MAX_DISTANCE = 0.8
    const val CHANT_GAP_COST = 0.7
    const val CHANT_UNMEASURED_COST = 0.5
    const val CHANT_SYLLABLES_PER_PAUSE = 6
    const val CHANT_PAUSE_PENALTY = 0.25f
    const val CHANT_FAST_INTERVAL_MS = 170L
    const val CHANT_RHYTHM_WEIGHT = 0.2f
    /** A phrase seals only when its [ChantScore.total] reaches this. Calibrated from tester clips. */
    const val CHANT_PASS_SCORE = 0.6f
    /** After the last syllable lights, its vowel is heard until voicing stops or this elapses. */
    const val SEAL_LISTEN_MS = 300L
    /** An unvoiced run at least this long inside a phrase counts as a pause (breath). */
    const val CHANT_PAUSE_MS = 250L

    // Chant screen (ADR-0019)
    /** After a verdict (sealed, rejected, cracked) the focus clock waits this long, or until the next syllable. */
    const val VERDICT_HOLD_MS = 3_000L
    /** A word whose syllables average at least this vowel distance is marked doubtful (a hint only). */
    const val WORD_DOUBT_DISTANCE = 0.6

    // Mic level for glow only (never progress)
    const val MIC_LEVEL_CEILING = 8_000.0
    const val MIC_LEVEL_ATTACK_MS = 40f
    const val MIC_LEVEL_RELEASE_MS = 250f

    // Debug "Hold to Chant"
    const val HOLD_TO_CHANT_RMS = 4_000.0
    const val HOLD_TO_CHANT_ONSET_FRAMES = 12
}
```

- [ ] **Step 5: Replace `RitualState.kt`**

`app/src/main/java/com/summondemons/safely/ritual/RitualState.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_BASE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_PER_SYLLABLE_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.VERDICT_HOLD_MS
import kotlin.math.roundToLong

enum class RitualPhase { Seeking, Locking, Chanting, Wavering, Climax, Done }

/**
 * One 20 ms tick: the latest heading (null = unknown, which counts as off heading once
 * locked), the analysed mic frame, and whether the demon's House is in court (read only
 * at the moment of lock, ADR-0012).
 */
data class RitualInput(val azimuth: Float?, val voice: VoiceFrame, val inCourt: Boolean = false)

/** What happened during a step; the UI turns these into haptics, sounds and flashes. */
sealed interface RitualEffect {
    data object ProximityTick : RitualEffect
    data object Locked : RitualEffect
    data object SyllableLit : RitualEffect

    /** Every syllable of [phrase] lit (ADR-0010). */
    data class Sealed(val phrase: Int) : RitualEffect

    /** Focus ran out: [phrase], the last sealed one, reopened. */
    data class Cracked(val phrase: Int) : RitualEffect
    data object Drift : RitualEffect
    data object Resumed : RitualEffect

    /** The chant check judged [phrase] (ADR-0018); always followed by [Sealed] or [Rejected]. */
    data class Judged(val phrase: Int, val score: ChantScore) : RitualEffect

    /** [phrase] didn't sound like its text: it stays open with no syllable lit (ADR-0018). */
    data class Rejected(val phrase: Int) : RitualEffect
    data class ClimaxStart(val flawless: Boolean) : RitualEffect
    data object Summoned : RitualEffect
}

data class RitualState(
    val target: Float,
    /** Syllables per phrase: exactly [PHRASE_COUNT] entries, each >= 1. */
    val syllables: List<Int>,
    val phase: RitualPhase = RitualPhase.Seeking,
    val azimuth: Float? = null,
    /** Phrases sealed so far = index of the current phrase (0..4). */
    val phrase: Int = 0,
    /** Syllables lit in the current phrase. */
    val lit: Int = 0,
    /** Time held on heading while Locking. */
    val lockMs: Long = 0L,
    /** Focus left for the current phrase; 0 and unused on phrase I (ADR-0010). */
    val focusMs: Long = 0L,
    val focusBudgetMs: Long = 0L,
    /** 1, or 1 + COURT_BONUS if the House was in court at the moment of lock; fixed for the attempt. */
    val courtMultiplier: Float = 1f,
    /** No crack so far in this attempt (ADR-0013). */
    val flawless: Boolean = true,
    /** Time spent in the climax. */
    val climaxMs: Long = 0L,
    val sinceSyllableMs: Long = ONSET_REFRACTORY_MS,
    val voicedRunMs: Long = 0L,
    /** Last 5 degree proximity bucket while seeking; null when out of range or unknown. */
    val proximityBucket: Int? = null,
    /** Expected vowel per syllable of each phrase; null turns the chant check off (ADR-0018). */
    val vowels: List<List<Vowel?>>? = null,
    /** What has been heard of the current phrase; only advanced while the chant check is on. */
    val take: ChantTake = ChantTake(),
    /** Syllables of each word, per phrase; set with [vowels] (ADR-0019 word hints). */
    val wordSizes: List<List<Int>>? = null,
    /** The last verdict, until the next syllable lights; only while the chant check is on (ADR-0019). */
    val verdict: ChantVerdict? = null,
) {
    init {
        require(syllables.size == PHRASE_COUNT) { "need exactly $PHRASE_COUNT phrases" }
        require(syllables.all { it >= 1 }) { "every phrase needs at least one syllable" }
        require(vowels == null || vowels.size == PHRASE_COUNT) { "need vowels for exactly $PHRASE_COUNT phrases" }
    }

    val aligned: Boolean get() = azimuth != null && isAligned(azimuth, target)

    /** Sigil ink in [0, 1] (ADR-0009). */
    val inkProgress: Float
        get() = if (phrase >= PHRASE_COUNT) 1f else (phrase + lit.toFloat() / syllables[phrase]) / PHRASE_COUNT

    /** Focus left in [0, 1]; 0 when the current phrase has no clock. */
    val focusFraction: Float
        get() = if (focusBudgetMs > 0L) (focusMs.toFloat() / focusBudgetMs).coerceIn(0f, 1f) else 0f

    /** The focus clock runs on phrases II–IV while Chanting or Wavering. */
    val clockActive: Boolean
        get() = phrase in 1 until PHRASE_COUNT && (phase == RitualPhase.Chanting || phase == RitualPhase.Wavering)

    val climaxFraction: Float
        get() = when (phase) {
            RitualPhase.Climax -> (climaxMs.toFloat() / CLIMAX_MS).coerceIn(0f, 1f)
            RitualPhase.Done -> 1f
            else -> 0f
        }

    companion object {
        /** [judged] turns the chant check on (ADR-0018); the app always does, most engine tests don't. */
        fun start(target: Float, phrases: List<String>, judged: Boolean = false): RitualState = RitualState(
            target = target,
            syllables = phrases.map { LatinSyllables.count(it).coerceAtLeast(1) },
            vowels = if (judged) phrases.map(ChantJudge::expectedVowels) else null,
            wordSizes = if (judged) phrases.map(LatinSyllables::wordSizes) else null,
        )
    }
}

/** Focus budget in ms for phrase index [phrase] with [syllables] syllables (ADR-0010); none for phrase I. */
fun focusBudget(syllables: Int, phrase: Int, courtMultiplier: Float = 1f): Long =
    if (phrase <= 0) 0L else ((FOCUS_BASE_MS + FOCUS_PER_SYLLABLE_MS * syllables) * courtMultiplier).roundToLong()

data class Step(val state: RitualState, val effects: List<RitualEffect> = emptyList())

/**
 * The last phrase verdict while the chant check is on (ADR-0019): shown on the chant screen
 * until the next syllable lights. While it is fresh ([holdsClock]) the focus clock waits.
 */
data class ChantVerdict(
    val kind: Kind,
    val phrase: Int,
    /** Word hints of the judged attempt; doubtful words of a rejection are the ones to fix. */
    val marks: List<WordMark> = emptyList(),
    val heldMs: Long = 0L,
) {
    enum class Kind { Sealed, Rejected, Cracked }

    val holdsClock: Boolean get() = heldMs < VERDICT_HOLD_MS
}
```

- [ ] **Step 6: Replace `RitualEngine.kt`**

`app/src/main/java/com/summondemons/safely/ritual/RitualEngine.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SEAL_LISTEN_MS
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

/** Pure ritual rules (ADR-0008, the focus clock of ADR-0010, the chant check of ADR-0018, verdicts of ADR-0019): one call per 20 ms tick. */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s, input.inCourt)
            RitualPhase.Chanting, RitualPhase.Wavering -> chant(s, input.voice)
            RitualPhase.Climax -> climax(s)
            RitualPhase.Done -> Step(s)
        }
    }
    /**
     * Whether a step with [state] and [azimuth] can lock on this tick.
     * Keeps RitualEngine pure: inputs and state only.
     */
    fun canLock(state: RitualState, azimuth: Float?): Boolean {
        if (state.phase != RitualPhase.Seeking && state.phase != RitualPhase.Locking) return false
        val s = state.copy(azimuth = azimuth)
        if (!s.aligned) return false
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        return held >= LOCK_HOLD_MS
    }


    private fun seek(s: RitualState, inCourt: Boolean): Step {
        val effects = mutableListOf<RitualEffect>()
        val bucket = s.azimuth?.let { proximityBucket(angularDistance(it, s.target)) }
        val previous = s.proximityBucket
        if (bucket != null && (previous == null || bucket < previous)) effects += RitualEffect.ProximityTick
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Seeking, lockMs = 0L, proximityBucket = bucket), effects)
        }
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        if (held < LOCK_HOLD_MS) {
            return Step(s.copy(phase = RitualPhase.Locking, lockMs = held, proximityBucket = bucket), effects)
        }
        effects += RitualEffect.Locked
        val chanting = s.copy(
            phase = RitualPhase.Chanting,
            lockMs = 0L,
            proximityBucket = bucket,
            courtMultiplier = if (inCourt) 1f + COURT_BONUS else 1f,
            sinceSyllableMs = ONSET_REFRACTORY_MS,
            voicedRunMs = 0L,
        )
        return Step(chanting, effects)
    }

    private fun chant(s: RitualState, voice: VoiceFrame): Step {
        val effects = mutableListOf<RitualEffect>()
        if (!s.aligned) {
            // Off heading: nothing counts; once locked, never back to Seeking (ADR-0010).
            if (s.phase == RitualPhase.Chanting) effects += RitualEffect.Drift
            return drain(s.copy(phase = RitualPhase.Wavering, voicedRunMs = 0L), FOCUS_DRIFT_MULTIPLIER, effects)
        }
        if (s.phase == RitualPhase.Wavering) effects += RitualEffect.Resumed
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        val chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run)
        if (s.take.closing) return close(chanting, voice, effects)
        val lights = voice.voiced && ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        if (!lights) return drain(chanting.hearing { hear(voice, begun = s.lit > 0) }, 1, effects)

        effects += RitualEffect.SyllableLit
        val lit = s.lit + 1
        val last = lit == s.syllables[s.phrase]
        // Chanting again clears the last verdict (ADR-0019).
        val reset = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L, lit = lit, verdict = null)
            .hearing { light(voice, first = s.lit == 0, last = last) }
        // With the chant check on, the last syllable's vowel is heard before the verdict (ADR-0018).
        if (!last || reset.take.closing) return drain(reset, 1, effects)
        return seal(reset, effects)
    }

    /** Hears the end of the last syllable, then judges the phrase: sealed, or rejected and reopened (ADR-0018). */
    private fun close(s: RitualState, voice: VoiceFrame, effects: MutableList<RitualEffect>): Step {
        val take = s.take.hear(voice, begun = true)
        if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), 1, effects)
        val expected = s.vowels!![s.phrase]
        val score = ChantJudge.score(expected, take)
        val marks = s.wordSizes?.let { ChantJudge.wordMarks(expected, take.heard, it[s.phrase]) }.orEmpty()
        effects += RitualEffect.Judged(s.phrase, score)
        if (score.passed) {
            return seal(s.copy(take = ChantTake(), verdict = ChantVerdict(ChantVerdict.Kind.Sealed, s.phrase, marks)), effects)
        }
        effects += RitualEffect.Rejected(s.phrase)
        val rejected = ChantVerdict(ChantVerdict.Kind.Rejected, s.phrase, marks)
        return Step(s.copy(lit = 0, take = ChantTake(), verdict = rejected), effects)
    }

    /** Seals the current phrase: the next one starts with a full clock, or the 4th starts the climax. */
    private fun seal(s: RitualState, effects: MutableList<RitualEffect>): Step {
        effects += RitualEffect.Sealed(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) {
            val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)
            return Step(s.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget), effects)
        }
        effects += RitualEffect.ClimaxStart(s.flawless)
        val climax = s.copy(phase = RitualPhase.Climax, phrase = phrase, lit = 0, focusMs = 0L, focusBudgetMs = 0L, climaxMs = 0L)
        return Step(climax, effects)
    }

    /** Advances the chant take only while the chant check is on. */
    private inline fun RitualState.hearing(update: ChantTake.() -> ChantTake): RitualState =
        if (vowels == null) this else copy(take = take.update())

    /**
     * Drains the focus clock (phrases II–IV only); at 0 the last seal cracks. While a fresh
     * verdict is on screen the clock waits instead (ADR-0019).
     */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val verdict = s.verdict
        if (verdict != null && verdict.holdsClock) return Step(s.copy(verdict = verdict.copy(heldMs = verdict.heldMs + FRAME_MS)), effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)
        val cracked = if (s.vowels != null) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null
        return Step(
            s.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget, flawless = false, take = ChantTake(), verdict = cracked),
            effects,
        )
    }

    private fun climax(s: RitualState): Step {
        val elapsed = s.climaxMs + FRAME_MS
        if (elapsed < CLIMAX_MS) return Step(s.copy(climaxMs = elapsed))
        return Step(s.copy(phase = RitualPhase.Done, climaxMs = CLIMAX_MS), listOf(RitualEffect.Summoned))
    }

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
```

- [ ] **Step 7: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: all pass. `RitualViewModelTest` is unchanged: Hold to Chant lights a syllable
right after each seal, which clears the verdict, so its timing doesn't move. If a
`RitualStateTest` or `RitualCueTest` case fails on a clock number, it hard-codes the old
budget: recompute it from `focusBudget(...)` rather than changing the rule.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): Verdicts hold until the next syllable; longer focus clock (ADR-0019)" -m "$TRAILER"
```

### Task 5: Chant screen state

Everything the screen draws, derived purely from `RitualState`: words with their syllables
and looks, direction, the verdict line and numeral, and the clock ring.

**Files:**
- Create: `MAIN/ui/screens/ChantScreenState.kt`, `TEST/ui/screens/ChantScreenStateTest.kt`

- [ ] **Step 1: Write the failing test**

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
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: compilation fails (`chantScreenState` unresolved).

- [ ] **Step 3: Implement**

`app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantJudge
import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.WordMark
import com.summondemons.safely.ritual.turnDirection
import kotlin.math.ceil

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard, Doubtful, Rejected }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** The one line under the words (ADR-0019). */
enum class VerdictLine { ChantAloud, Paused, Sealed, ChantAgain, SealCracked }

/** Everything the chant screen draws, derived from the ritual (pure; ADR-0019). */
data class ChantScreenState(
    val words: List<ChantWord>,
    val facing: Boolean,
    /** Which way to turn while not facing the demon; null when facing or unknown. */
    val turn: TurnDirection?,
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
        ritual.phase == RitualPhase.Wavering -> VerdictLine.Paused
        else -> VerdictLine.ChantAloud
    }
    val clock = ritual.phrase in 1 until PHRASE_COUNT
    return ChantScreenState(
        words = words,
        facing = ritual.aligned,
        turn = turnDirection(azimuth, target),
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
Expected: 9 tests pass.

- [ ] **Step 5: Gate, then commit**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt
git commit -q -m "feat(ui): Chant screen state (ADR-0019)" -m "$TRAILER"
```

---

## Phase 3 — Screen

### Task 6: Text roles and copy

**Files:**
- Modify: `MAIN/ui/theme/TextRoles.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ui/theme/TextRoles.kt`**

(a) Replace:

```kotlin
    /** Cinzel Bold; the font size is set per use. */
    val name: TextStyle,
    val body: TextStyle,
)
```

with:

```kotlin
    /** Cinzel Bold; the font size is set per use. */
    val name: TextStyle,
    val body: TextStyle,
    /** Chant screen lines (ADR-0019): Inter Bold, upper case; 18 sp direction, 22 sp verdict, set per use. */
    val ritualCue: TextStyle,
    /** Chant screen phrase (ADR-0019): Cinzel Bold, auto-sized at the call site. */
    val incantationLarge: TextStyle,
)
```

(b) Replace:

```kotlin
    name = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold),
    body = AppTypography.bodyMedium,
)
```

with:

```kotlin
    name = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold),
    body = AppTypography.bodyMedium,
    ritualCue = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.05.em),
    incantationLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        lineHeight = 1.3.em,
        letterSpacing = 0.02.em,
    ),
)
```

(c) Replace:

```kotlin
/** Compose has no text-transform: the `cue` and `meta` roles upper-case at the call site. */
```

with:

```kotlin
/** Compose has no text-transform: the `cue`, `meta` and `ritualCue` roles upper-case at the call site. */
```

- [ ] **Step 2: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="chant_drift">⏱ 3×</string>

```

with:

```xml
    <string name="chant_drift">⏱ 3×</string>
    <!-- Chant screen (ADR-0019) -->
    <string name="chant_face">Face %1$s</string>
    <string name="chant_a11y_facing">Facing %1$s</string>
    <string name="chant_a11y_turn_left">Turn left to face %1$s</string>
    <string name="chant_a11y_turn_right">Turn right to face %1$s</string>
    <string name="chant_clock_seconds">%1$d s</string>
    <string name="chant_clock_paused">Paused</string>
    <string name="verdict_chant_aloud">Chant aloud</string>
    <string name="verdict_paused">Paused</string>
    <string name="verdict_sealed">Sealed</string>
    <string name="verdict_chant_again">Chant again</string>
    <string name="verdict_seal_cracked">Seal cracked</string>
    <string name="verdict_of">of</string>
    <string name="chant_turn_sideways">Turn your phone sideways</string>
    <string name="chant_keep_mic_clear">Hold it in front of your mouth, and keep the bottom edge clear so the microphone can hear you.</string>

```

- [ ] **Step 3: Build and commit**

Run: `./gradlew assembleDebug`. Expected: `BUILD SUCCESSFUL` (the new roles and strings are
unused until Task 7; lint may warn about unused resources, which is fine).

```bash
git add app/src/main/java/com/summondemons/safely/ui/theme/TextRoles.kt app/src/main/res/values/strings.xml
git commit -q -m "feat(ui): ritualCue and incantationLarge text roles, chant screen copy (ADR-0019)" -m "$TRAILER"
```

### Task 7: The chant screen

One file. Layout (landscape): the House stage fills the screen edge to edge, darkened
most behind the words; a 25 % slab on the left holds the name, `ChantSigil` and the
direction line; the right holds the phrase (one auto-sized `BasicText`, word lines drawn
from its layout) over the verdict line, with the debug controls under it in debug builds.
A portrait window (large screens that ignore the rotation request) puts the slab in a top
band. A turn-sideways hint covers the screen for 2.5 s or until the first syllable.

**Files:**
- Create: `MAIN/ui/screens/ChantScreen.kt`

- [ ] **Step 1: Create the screen**

`app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt`:

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.components.StageBackground
import com.summondemons.safely.ui.fx.InkedSigil
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.SigilFlash
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper
import kotlinx.coroutines.delay

private const val SLAB_WEIGHT = 0.25f
private const val SLAB_ALPHA = 0.7f
private const val SIGIL_IN_RING = 0.84f
private const val RING_STROKE_FRACTION = 0.035f
private const val CLOCK_TWEEN_MS = 120
private const val CURRENT_WORD_SCALE = 1.15f
private const val DIM_ALPHA = 0.55f
private const val DOUBT_ALPHA = 0.7f
private const val LINE_FRACTION = 0.06f
private const val PENDING_LINE_SCALE = 0.7f
private const val WAVE_AMPLITUDE = 0.08f
private const val BADGE_FRACTION = 0.22f
private const val BLINK_HALF_MS = 300
private const val BLINK_MIN = 0.55f
private const val HINT_MS = 2_500L
private const val HINT_SCRIM_ALPHA = 0.88f
private const val BACKDROP_DIM = 0.55f
private const val WORDS_SCRIM_ALPHA = 0.85f
private const val METER_TWEEN_MS = 120
private const val METER_FLOOR = 0.22f
private const val SYLLABLE_DOT = "·"
private val METER_HEIGHTS = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)
private val NameSize = 24.sp
private val DirectionSize = 18.sp
private val VerdictSize = 22.sp
private val BandSigil = 96.dp
private val MeterWidth = 26.dp
private val MeterHeight = 22.dp
private val MinPhraseSize = 18.sp
private val MaxPhraseSize = 96.sp
private val PhraseStep = 2.sp

/**
 * The chant screen (ADR-0019): from the lock, the demon's name, sigil (progress, direction,
 * focus clock) and direction line on a slab at the left; the phrase, as large as fits, and
 * one verdict line at the right. Portrait screens get the slab as a top band. The House
 * stage fills the screen edge to edge; the content keeps clear of the system bars.
 */
@Composable
fun ChantScreen(
    state: RitualUiState,
    flash: SigilFlash?,
    showClose: Boolean,
    onClose: () -> Unit,
    /** Debug controls, shown under the verdict (debug builds only). */
    debug: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val screen = remember(state.ritual, state.azimuth) {
        chantScreenState(state.ritual, demon.chantPhrases, state.azimuth, demon.ritualHeading)
    }
    var hint by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(HINT_MS)
        hint = false
    }
    LaunchedEffect(state.ritual.lit) { if (state.ritual.lit > 0) hint = false }

    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        val spacing = MaterialTheme.spacing
        Backdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, vertical = true, Modifier.fillMaxHeight().weight(SLAB_WEIGHT))
                Words(state, screen, accent, debug, Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT))
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(state, screen, accent, flash, vertical = false, Modifier.fillMaxWidth())
                Words(state, screen, accent, debug, Modifier.fillMaxWidth().weight(1f))
            }
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
        if (hint) TurnSidewaysHint(accent, Modifier.fillMaxSize())
    }
}

/** The House stage, darkened most behind the words so the phrase keeps its contrast. */
@Composable
private fun Backdrop(stage: Stage, landscape: Boolean, modifier: Modifier) {
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

@Composable
private fun Slab(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    flash: SigilFlash?,
    vertical: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val ringColor = if (screen.facing) MaterialTheme.demonColors.ritualSafe else colors.error
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = SLAB_ALPHA)) {
        if (vertical) {
            Column(
                Modifier.padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                Text(state.demon.name, style = MaterialTheme.textRoles.name.copy(fontSize = NameSize), color = accent, maxLines = 1)
                ChantSigil(
                    state, screen, accent, ringColor, flash,
                    Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
                DirectionCue(screen, state.demon.ritualHeading, ringColor, Modifier.padding(top = spacing.sm))
            }
        } else {
            Row(
                Modifier.padding(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ChantSigil(state, screen, accent, ringColor, flash, Modifier.size(BandSigil))
                DirectionCue(screen, state.demon.ritualHeading, ringColor)
            }
        }
    }
}

/** The sigil inside a ring that shows the direction (colour) and the focus clock (arc). */
@Composable
private fun ChantSigil(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
    ringColor: Color,
    flash: SigilFlash?,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainerHighest
    val sweep by animateFloatAsState(screen.clockFraction, tween(CLOCK_TWEEN_MS, easing = LinearEasing), label = "clock")
    val ring by animateColorAsState(ringColor, label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_FRACTION
            val side = size.minDimension - stroke
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            drawArc(track, 0f, 360f, false, topLeft, Size(side, side), style = Stroke(stroke))
            drawArc(ring, -90f, 360f * sweep, false, topLeft, Size(side, side), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        InkedSigil(
            sigil = state.demon.sigil,
            inkProgress = state.ritual.inkProgress,
            accent = accent,
            rimText = state.demon.name.uppercase(),
            modifier = Modifier.fillMaxSize(SIGIL_IN_RING),
            micLevel = state.micLevel,
            flash = flash,
        )
        screen.clockSeconds?.let { seconds ->
            Text(
                if (screen.clockHeld) stringResource(R.string.chant_clock_paused).upper() else stringResource(R.string.chant_clock_seconds, seconds),
                style = MaterialTheme.textRoles.meta,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** "FACE EAST ✓" in green, or "← FACE EAST" in red and blinking while off course. */
@Composable
private fun DirectionCue(screen: ChantScreenState, target: Float, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (!screen.facing && !reduceMotion) {
        rememberInfiniteTransition(label = "direction").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "directionBlink",
        ).value
    } else {
        1f
    }
    val direction = headingLabel(target)
    val description = when {
        screen.facing -> stringResource(R.string.chant_a11y_facing, direction)
        screen.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = DirectionSize)
    Row(
        modifier.graphicsLayer { alpha = blink }.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        if (!screen.facing) Text(if (screen.turn == TurnDirection.Right) "→" else "←", style = style, color = color)
        Text(stringResource(R.string.chant_face, direction).upper(), style = style, color = color, maxLines = 1)
        if (screen.facing) Text("✓", style = style, color = color)
    }
}

@Composable
private fun Words(
    state: RitualUiState,
    screen: ChantScreenState,
    accent: Color,
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
        debug?.invoke()
    }
}

/** The phrase as one auto-sized text; a line under each word is drawn from the layout. */
@Composable
private fun Phrase(words: List<ChantWord>, accent: Color, modifier: Modifier) {
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
                if (si > 0) withStyle(SpanStyle(color = colors.outlineVariant)) { append(SYLLABLE_DOT) }
                val color = when {
                    word.look == WordLook.Rejected -> colors.error
                    word.look == WordLook.Heard || word.look == WordLook.Doubtful -> accent
                    si < word.lit -> accent
                    else -> dim
                }
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
            WordLook.Doubtful -> colors.primary.copy(alpha = DOUBT_ALPHA)
            WordLook.Rejected -> colors.error
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicText(
        text = text,
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(minFontSize = MinPhraseSize, maxFontSize = MaxPhraseSize, stepSize = PhraseStep),
        onTextLayout = { layout = it },
        modifier = modifier.wrapContentHeight(Alignment.CenterVertically).drawBehind {
            layout?.let { drawWordLines(it, ranges, words, lineColors, colors.onError) }
        },
    )
}

private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
    badgeInk: Color,
) {
    ranges.forEachIndexed { i, range ->
        if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return@forEachIndexed
        if (layout.getLineForOffset(range.first) != layout.getLineForOffset(range.last)) return@forEachIndexed
        val first = layout.getBoundingBox(range.first)
        val last = layout.getBoundingBox(range.last)
        val height = last.bottom - last.top
        val thickness = height * LINE_FRACTION
        val y = last.bottom + thickness
        val color = lineColors[i]
        when (words[i].look) {
            WordLook.Rejected -> {
                drawWave(first.left, last.right, y, height * WAVE_AMPLITUDE, color, thickness)
                val r = height * BADGE_FRACTION
                val c = Offset(last.right + r * 0.4f, first.top + r * 0.6f)
                drawCircle(color, r, c)
                val d = r * 0.45f
                drawLine(badgeInk, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), thickness, StrokeCap.Round)
                drawLine(badgeInk, Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), thickness, StrokeCap.Round)
            }
            WordLook.Pending -> drawLine(color, Offset(first.left, y), Offset(last.right, y), thickness * PENDING_LINE_SCALE, StrokeCap.Round)
            else -> drawLine(color, Offset(first.left, y), Offset(last.right, y), thickness, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawWave(from: Float, to: Float, y: Float, amplitude: Float, color: Color, width: Float) {
    val step = amplitude * 4f
    val path = Path().apply {
        moveTo(from, y)
        var x = from
        var up = true
        while (x < to) {
            val next = minOf(x + step, to)
            quadraticTo((x + next) / 2f, if (up) y - amplitude * 2f else y + amplitude * 2f, next, y)
            x = next
            up = !up
        }
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
}

/** "CHANT ALOUD · I of IV" and its siblings, with the voice meter while listening. */
@Composable
private fun VerdictLine(screen: ChantScreenState, accent: Color, micLevel: Float) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val color = when (screen.verdict) {
        VerdictLine.Sealed -> safe
        VerdictLine.ChantAgain, VerdictLine.SealCracked -> colors.error
        VerdictLine.ChantAloud, VerdictLine.Paused -> colors.outline
    }
    val label = stringResource(
        when (screen.verdict) {
            VerdictLine.ChantAloud -> R.string.verdict_chant_aloud
            VerdictLine.Paused -> R.string.verdict_paused
            VerdictLine.Sealed -> R.string.verdict_sealed
            VerdictLine.ChantAgain -> R.string.verdict_chant_again
            VerdictLine.SealCracked -> R.string.verdict_seal_cracked
        },
    ).upper()
    val of = stringResource(R.string.verdict_of).upper()
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = VerdictSize)
    val nameFamily = MaterialTheme.textRoles.name.fontFamily
    val count = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = nameFamily, color = accent)) { append(ROMAN_NUMERALS[screen.numeral]) }
        withStyle(SpanStyle(color = colors.outline, fontSize = 0.7.em)) { append(" $of ") }
        withStyle(SpanStyle(fontFamily = nameFamily, color = accent)) { append(ROMAN_NUMERALS.last()) }
    }
    Row(
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        if (screen.verdict == VerdictLine.ChantAloud || screen.verdict == VerdictLine.Paused) {
            val listening = screen.verdict == VerdictLine.ChantAloud
            VoiceMeter(if (listening) micLevel else 0f, if (listening) accent else colors.outline, Modifier.size(MeterWidth, MeterHeight))
        }
        Text(label, style = style, color = color, maxLines = 1)
        when (screen.verdict) {
            VerdictLine.Sealed -> Text("✓", style = style, color = color)
            VerdictLine.ChantAgain, VerdictLine.SealCracked -> Text("✕", style = style, color = color)
            else -> Unit
        }
        Text(count, style = style, maxLines = 1)
    }
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

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. If the compiler rejects an API (for example a different
`TextAutoSize.StepBased` parameter name, or `ritualSafe` living elsewhere in
`DemonColors`), adapt the call without changing the behaviour and note it in the commit.
`ChantScreen` is not called yet.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt
git commit -q -m "feat(ui): Landscape chant screen (ADR-0019)" -m "$TRAILER"
```

### Task 8: Switch to it from the lock, in landscape

From the lock (unless an error panel must show) `RitualContent` renders `ChantScreen`
instead of the portrait column, and requests sensor-landscape; portrait comes back when
it closes. The activity handles rotation itself, so the ViewModel's mic and sensor flows
don't restart. The climax overlay keeps playing on top, now fitting landscape.

**Files:**
- Modify: `MAIN/ui/screens/RitualScreen.kt`, `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Edit `app/src/main/AndroidManifest.xml`**

Replace:

```xml
            android:screenOrientation="portrait"

```

with:

```xml
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"

```

- [ ] **Step 2: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`**

(a) Replace:

```kotlin
import android.content.Intent

```

with:

```kotlin
import android.content.Intent
import android.content.pm.ActivityInfo

```

(b) Replace:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize

```

with:

```kotlin
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize

```

(c) Replace:

```kotlin
import androidx.compose.foundation.layout.size

```

with:

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize

```

(d) Replace:

```kotlin
import androidx.compose.runtime.Composable

```

with:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

```

(e) Replace:

```kotlin
private val SEALED_PHASES = setOf(RitualPhase.Chanting, RitualPhase.Wavering, RitualPhase.Climax, RitualPhase.Done)

```

with:

```kotlin
private val SEALED_PHASES = setOf(RitualPhase.Chanting, RitualPhase.Wavering, RitualPhase.Climax, RitualPhase.Done)

/** Before the lock the portrait altar stays; from the lock the chant screen takes over (ADR-0019). */
private val PRE_LOCK_PHASES = setOf(RitualPhase.Seeking, RitualPhase.Locking)

```

(f) Replace:

```kotlin
    val wavering = ritual.phase == RitualPhase.Wavering
    val heartbeat = if (wavering && !reduceMotion) rememberHeartbeat() else null

```

with:

```kotlin
    val wavering = ritual.phase == RitualPhase.Wavering
    val heartbeat = if (wavering && !reduceMotion) rememberHeartbeat() else null
    // Errors (no mic, no heading) keep the portrait screen and its panels.
    val chantLayout = ritual.phase !in PRE_LOCK_PHASES &&
        state.micGranted != false &&
        !(state.micGranted == true && state.micUnavailable) &&
        !(state.headingUnavailable && !state.simulateHeading)
    RequestLandscape(chantLayout)

```

(g) Replace:

```kotlin
        ParticleField(accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize())
        Column(
```

with:

```kotlin
        if (chantLayout) {
            ChantScreen(
                state = state,
                flash = flash,
                showClose = !inClimax,
                onClose = requestClose,
                debug = if (BuildConfig.DEBUG && !inClimax) {
                    {
                        DebugControls(
                            simulateHeading = state.simulateHeading,
                            onSimulateHeadingChange = onSimulateHeadingChange,
                            onHoldToChantChange = onHoldToChantChange,
                            lastScore = state.lastScore,
                            clipLabel = state.clipLabel,
                            onCycleClipLabel = onCycleClipLabel,
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha },
            )
        } else {
        ParticleField(accent.copy(alpha = EMBER_ALPHA), ParticleStyle.Embers, Modifier.fillMaxSize())
        Column(
```

(h) Replace:

```kotlin
        }
        if (inClimax) ClimaxOverlay(demon, accent, ritual.climaxFraction, reduceMotion)
```

with:

```kotlin
        }
        }
        if (inClimax) ClimaxOverlay(demon, accent, ritual.climaxFraction, reduceMotion)
```

(i) Replace:

```kotlin
@Composable
private fun rememberHeartbeat(): State<Float>
```

with:

```kotlin
/** The chant screen is landscape (ADR-0019); portrait comes back when it closes or the Ritual leaves. */
@Composable
private fun RequestLandscape(landscape: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, landscape) {
        activity.requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
}

@Composable
private fun rememberHeartbeat(): State<Float>
```

(j) Replace:

```kotlin
                .fillMaxWidth(CUTOUT_WIDTH_FRACTION)
                .aspectRatio(CUTOUT_ASPECT)
```

with:

```kotlin
                // Fits both dimensions: the climax plays in landscape too (ADR-0019).
                .fillMaxWidth(CUTOUT_WIDTH_FRACTION)
                .fillMaxHeight(CUTOUT_WIDTH_FRACTION)
                .wrapContentSize()
                .aspectRatio(CUTOUT_ASPECT)
```

- [ ] **Step 3: Re-indent**

The `else {` branch added around the existing `ParticleField(...)` and `Column(...)` leaves
that block one level too shallow. Re-indent it by 4 spaces (formatting only, no other
change), e.g. with the IDE's reformat on that range.

- [ ] **Step 4: Gate, and the release build**

Run the verification gate, then `./gradlew assembleRelease`.
Expected: both `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt app/src/main/AndroidManifest.xml
git commit -q -m "feat(ui): Chant screen from the lock, in landscape (ADR-0019)" -m "$TRAILER"
```

---

## Phase 4 — Device check and calibration

### Task 9: Device checklist

Install with `./gradlew installDebug`, on a phone, and with glasses if you wear them:

1. **Seeking is unchanged:** portrait altar, compass ring, cue chip, dim tablet preview.
2. **Lock:** the screen turns to landscape; the hint "Turn your phone sideways" shows,
   then goes after 2.5 s or the first syllable. The ritual does not restart: no second
   lock, the mic keeps running (the voice meter moves), the heading stays green.
3. **Landscape heading:** with the phone sideways and upright in front of your mouth,
   "FACE EAST ✓" stays green while facing the demon; turning away shows "← FACE EAST"
   or "→ FACE EAST" in red, blinking, with the correct arrow; the ring turns red. Tilt
   the phone towards flat: the heading must not jump by 90°.
4. **Readability at mouth distance:** the phrase is large enough for all four phrases of
   both demons (phrase III of Asmodeus is the longest); the current word is larger;
   syllables light in the accent; the direction and verdict lines are readable.
5. **Word lines:** heard words get a green line one word after you finish them;
   mumbling a word gives it a dimmer amber line.
6. **Rejection** (say "Olá meu amigo, como está hoje?"): "CHANT AGAIN ✕ · I of IV" in red,
   doubtful words red with a wavy line and a ✕ badge, the phone buzzes, the sigil strokes
   flash red. Everything stays until you chant again; on phrases II–IV the ring shows
   "Paused" for about 3 s, then drains.
7. **Seal:** "SEALED ✓ · I of IV" in green until the next phrase's first syllable; the
   next phrase's clock waits up to 3 s.
8. **Clock and crack:** on phrase II stay silent: after the 3 s hold the ring drains
   (longer than before: about 4 s + 0.75 s per syllable), then "SEAL CRACKED ✕".
9. **Abandon:** the ✕ at the top right and the back gesture still ask "Abandon the
   ritual?" once a phrase is sealed.
10. **Climax:** plays in landscape with the demon art fully visible; back on the Grimoire
    the app is portrait again and the celebration shows.
11. **Reduced motion:** no blinking direction line; everything still updates.
12. **TalkBack:** the direction line reads "Facing East" / "Turn left to face East"; the
    verdict line is announced when it changes.
13. **Large screen or foldable** (or an emulator ≥ 600 dp): if the app stays portrait, the
    slab becomes a top band and everything still fits.
14. `./gradlew installRelease`: no debug controls under the verdict.

Fix anything that fails before Task 10.

### Task 10: Calibrate the two new numbers

Run with the tester clips of `2026-10-18-chant-check.md` Task 11 (same consent rules, same
`ChantClipReplayTest`). Two first guesses from ADR-0019 need checking on real voices:

- `WORD_DOUBT_DISTANCE` (0.6): on honest chant clips, fewer than about 1 word in 10
  should come out doubtful. Raise it if honest words are often amber; lower it if nonsense
  words are rarely marked. To measure, add a temporary `println` of
  `ChantJudge.wordMarks(...)` per `Judged` effect in the harness (don't commit it).
- `VERDICT_HOLD_MS` (3 s): ask testers whether they had time to read the verdict and the
  marked words before the clock moved.

Record the values in ADR-0019 ("Calibration result", as ADR-0018 does) and in
`RitualConfig`, then commit:

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt docs/decisions/0019-chant-screen.md
git commit -q -m "tune(ritual): Calibrate word hints and the verdict hold (ADR-0019)" -m "$TRAILER"
```

---

## Follow-ups (not in this plan)

- **Remove the portrait rejection cue** (ADR-0018 Task 8): `CueKind.Rejected`, its
  `ritualCue(..., rejected)` parameter and test, the `rejected` mark and `REJECT_MS` in
  `RitualScreen`, `RitualUiState.rejections`, and the `cue_rejected` string array. It can
  no longer show, because rejections only happen on the chant screen. Keep the red sigil
  flash on `Rejected`: the chant screen uses it.
- **A comic sound on rejection** (a scoff or a yawn) to bring back the demon's humour
  without text, if the product owner wants it.

---

## Spec coverage

| ADR-0019 item | Task |
|---|---|
| Dedicated screen from the lock; portrait altar before it | 8 |
| Landscape, rotation without restart, portrait fallback, turn-sideways hint | 7, 8 |
| Heading follows the screen rotation | 2 |
| Slab: name, sigil (progress, direction ring, clock arc), direction line | 5, 7 |
| Phrase auto-sized, syllable dots, current word larger, word lines | 5, 7 |
| Verdict line with numeral and voice meter | 5, 7 |
| Word hints (never decide) | 3, 5 |
| Verdicts stay until the next syllable; 3 s clock hold; rejection never refills | 4 |
| Longer clock (4 s + 0.75 s per syllable) | 4 |
| Text roles `ritualCue`, `incantationLarge` | 6 |
| House-stage backdrop with scrim strongest behind the words | 7 |
| Climax fits landscape | 8 |
| Canon: PRODUCT.md, DESIGN.md, ADR-0010/0014/0018 status lines, AGENTS.md | 1 |
| Calibration of the word-hint distance and the hold | 10 |
