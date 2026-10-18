# Chant Check v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let honest players through on long Latin phrases while still rejecting nonsense, by judging chunks with frame-level vowel alignment against shuffled baselines, replacing the focus clock with a focus meter, and showing progress in a two-row status block.

**Architecture:** A new pure `ChantAligner` (template → DTW alignment → shuffle-baseline verdict) replaces `ChantJudge`'s nucleus scoring. `RitualEngine` judges each phrase in word-group chunks (`ChantChunks`) and drains focus only on idle silence, drift and rejections. The chant screen derives a `StatusBlock` (step state + four seals with part arcs) and a reduced verdict line from `RitualState`; the intro card carries the rules.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), JUnit 4 on the JVM (`./gradlew testDebugUnitTest`), Android lint.

**Spec:** `docs/superpowers/specs/2026-10-18-chant-check-v2-design.md`. **Prototype:** https://claude.ai/artifact/9rCV9zSJdqY5mJscEn5EiQ (v12.1).

**Branch:** `docs/chant-check-v2` (spec already committed). All paths below are relative to the repo root `/Users/roalcantara/Work/mydemon/mydemon`. Package root: `app/src/main/java/com/summondemons/safely/` (written `MAIN/`) and `app/src/test/java/com/summondemons/safely/` (written `TEST/`).

**Commit trailer:** every commit message ends with a blank line and `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

**Test command:** `./gradlew testDebugUnitTest --tests '<pattern>'` (from the repo root). Full gate: `./gradlew testDebugUnitTest lintDebug assembleDebug`.

---

## Deviations from the spec (decided while planning)

1. **Shuffles move whole slots** (vowel with its glide/neutral flags), not just vowels, and cost ties use a 1e-9 tolerance. A Python prototype of the aligner showed that moving only the vowels let a held vowel beat 98 % of shuffles, and floating-point summation order broke ties. With both fixes the prototype passed 100 % of noisy honest takes and ~30 % of random nonsense (the G1 target).
2. **`ALIGN_SKIPPED_WEIGHT` starts at 0.6** (spec suggested a small weight); at 0.3 a reversed chant reported a cost of 0.12. Still a tuning value.
3. **No side-by-side old judge in the harness.** Keeping the nucleus judge alive in test code would mean keeping the old engine path. Instead Task 2 saves the old judge's report as a baseline file before the switch, and the new harness report is compared with it.
4. **Chunks arrive together with the aligner** (Task 7), because the aligner judges chunks; the focus meter stays a separate task (Task 9).
5. **Step labels use Inter Bold**, not ExtraBold: only `inter_bold.ttf` is bundled.
6. **The talk veto is emergent as well as explicit:** DTW cuts a long held vowel into minimum-length slots, which the veto then rejects. Kept, because it rejects held vowels and reversed chants that the rank alone misses.
7. **Two visual simplifications:** the frame turns the demon accent when all four seals hold, without an extra glow; a cracked seal is drawn red (no shake animation). Both can be added later without touching the model.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `MAIN/ritual/ChantTemplate.kt` | Create | `VowelSlot` and the expected template of a text (glides, neutral endings, words) |
| `MAIN/ritual/ChantAligner.kt` | Create | `Alignment`, `ChunkVerdict`, DTW alignment, shuffle baseline, talk veto, word marks |
| `MAIN/ritual/ChantChunks.kt` | Create | `ChantChunk` and the word-group split |
| `MAIN/ritual/ChantJudge.kt` | Rewrite | Keeps `Vowel`, `WordMark`, `expectedVowels`, `distance`; new frame-based `ChantTake` |
| `MAIN/ritual/RitualConfig.kt` | Modify | New aligner, chunk and focus-meter constants; old scoring constants removed |
| `MAIN/ritual/RitualState.kt` | Modify | `chunks`, `chunk`, `quietMs`; new effects; flat `focusBudget` |
| `MAIN/ritual/RitualEngine.kt` | Modify | Chunk flow, aligner verdicts, focus meter, crack helper |
| `MAIN/ritual/RitualViewModel.kt` | Modify | `lastJudged` instead of `lastScore`; Hold to Chant reads slots |
| `MAIN/platform/RitualFeedback.kt` | Modify | Tick haptic on `ChunkPassed` |
| `MAIN/ui/components/DebugControls.kt` | Modify | Debug score line from `RitualEffect.Judged` |
| `MAIN/ui/screens/RitualScreen.kt` | Modify | Pass `lastJudged` to the debug panel |
| `MAIN/ui/screens/ChantScreenState.kt` | Modify | `StatusBlock`, seals, reduced `VerdictLine`, chunk-based word looks |
| `MAIN/ui/screens/RitualStatusBlock.kt` | Create | The two-row Compass-and-Tally composable |
| `MAIN/ui/screens/ChantScreen.kt` | Modify | Slab uses the status block; no clock label; reduced verdict line |
| `MAIN/ui/screens/ChantIntro.kt` | Modify | Rules 1. Face / 2. Chant / Lines / Focus with new icons |
| `app/src/main/res/values/strings.xml` | Modify | New and removed strings |
| `TEST/ritual/ChantTemplateTest.kt` | Create | Template tests |
| `TEST/ritual/ChantAlignerTest.kt` | Create | Alignment and verdict tests |
| `TEST/ritual/ChantChunksTest.kt` | Create | Split tests pinned to the spec table |
| `TEST/ritual/ChantJudgeTest.kt` | Rewrite | Only `expectedVowels` and `distance` |
| `TEST/ritual/ChantCheckTest.kt` | Modify | Engine with the check on: chunks, verdict type, focus meter |
| `TEST/ritual/RitualEngineTest.kt` | Modify | Focus meter rules |
| `TEST/ritual/SyntheticChantTest.kt` | Modify | New verdict type; held vowels incl. E |
| `TEST/ritual/RitualViewModelTest.kt` | Modify | `lastJudged` |
| `TEST/ritual/ChantClipReplayTest.kt` | Rewrite | Voice tags, diagnostics, sufficiency, gate |
| `TEST/ui/screens/ChantScreenStateTest.kt` | Modify | Status block, verdict line, chunk word looks |
| `docs/decisions/0022-…`, `0023-…`, `0024-…` | Create | ADRs |
| `PRODUCT.md`, `DESIGN.md` | Modify | Rows and contracts |

---

## Phase 1 — Measurement

### Task 1: Harness diagnostics with voice tags (old judge)

**Files:**
- Modify: `TEST/ritual/ChantClipReplayTest.kt`

This is test-only code that runs against today's judge, so the baseline in Task 2 can be compared with the new judge later.

- [ ] **Step 1: Replace the file with the version below**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0018). Replays tester clips through the real
 * pipeline and prints how each judged phrase scores, per voice.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices), named
 * `{demonId}-{label}-{voice}-{stamp}.wav` (label `chant` or `nonsense`); the older
 * `{demonId}-{label}-{stamp}.wav` names count as voice `unknown`. Without clips the test is
 * skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 * The report is also written to `app/build/reports/chant-clips.txt`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Clip(val name: String, val demon: String, val label: String, val voice: String)

    private data class Result(val clip: Clip, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsReport() {
        val files = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", files.isNotEmpty())
        val results = files.flatMap { file ->
            val clip = parse(file)
            val demon = DemonCatalog.byId(clip.demon) ?: error("${file.name}: unknown demon '${clip.demon}'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(clip, it) }
        }
        val report = buildString {
            results.forEach { r ->
                val s = r.judged.score
                appendLine(
                    "%-48s %-8s %-8s phrase %-3s vowels %.2f rhythm %.2f total %.2f %s".format(
                        r.clip.name, r.clip.label, r.clip.voice, ROMAN[r.judged.phrase],
                        s.vowels, s.rhythm, s.total, if (s.total >= CHANT_PASS_SCORE) "PASS" else "fail",
                    ),
                )
            }
            appendLine()
            summary(results.map { Triple(it.clip, it.judged.phrase, it.judged.score.total >= CHANT_PASS_SCORE) }).forEach(::appendLine)
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/chant-clips.txt").writeText(report)
    }

    private fun parse(file: File): Clip {
        val parts = file.nameWithoutExtension.split('-')
        require(parts.size >= 3) { "${file.name}: expected {demonId}-{label}-{voice}-{stamp}.wav" }
        val voice = if (parts.size >= 4) parts[2] else "unknown"
        return Clip(file.name, parts[0], parts[1], voice)
    }

    /** Pass rates per voice and label, per phrase for honest chants, and whether the data is enough. */
    private fun summary(rows: List<Triple<Clip, Int, Boolean>>): List<String> = buildList {
        add("voice     label     judged  passed")
        rows.groupBy { it.first.voice to it.first.label }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            add("%-9s %-9s %6d  %5.1f %%".format(key.first, key.second, list.size, list.count { it.third } * 100.0 / list.size))
        }
        add("")
        add("honest chants per phrase: takes, voices, passed")
        val chant = rows.filter { it.first.label == "chant" }
        chant.groupBy { it.first.demon to it.second }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            val takes = list.map { it.first.name }.distinct().size
            val voices = list.map { it.first.voice }.distinct().size
            add("%-10s %-3s takes %2d voices %d passed %5.1f %%".format(key.first, ROMAN[key.second], takes, voices, list.count { it.third } * 100.0 / list.size))
        }
        add("")
        add("enough data? " + (sufficiency(chant) ?: "yes"))
    }

    /** Null when every judged phrase has ≥ 4 honest takes from ≥ 2 voices; else what is missing. */
    private fun sufficiency(chant: List<Triple<Clip, Int, Boolean>>): String? {
        if (chant.isEmpty()) return "insufficient: no honest chants"
        val missing = chant.groupBy { it.first.demon to it.second }.mapNotNull { (key, list) ->
            val takes = list.map { it.first.name }.distinct().size
            val voices = list.map { it.first.voice }.distinct().size
            if (takes < MIN_TAKES || voices < MIN_VOICES) "${key.first} ${ROMAN[key.second]} ($takes takes, $voices voices)" else null
        }
        return if (missing.isEmpty()) null else "insufficient: " + missing.joinToString()
    }

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        const val MIN_TAKES = 4
        const val MIN_VOICES = 2
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`
Expected: BUILD SUCCESSFUL. With the two existing clips (voice `unknown`) the output lists 7 judged phrases and ends with `enough data? insufficient: …`.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/summondemons/safely/ritual/ChantClipReplayTest.kt
git commit -q -m "test(ritual): Report chant clips per voice and phrase, with a data check

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 2: Record clips and save the baseline (human checkpoint)

**Files:** none in git (clips and the baseline stay in the git-ignored `app/src/test/chant-clips/`).

- [ ] **Step 1: Record, with consent, using a debug build**

For each of the two voices (use short tags such as `ro` and `v2`):
- 3 full rituals per demon, chanting all four lines honestly, with the debug panel's **Record clip** set to `chant`;
- 8 nonsense takes (normal speech in their own language, humming, counting) with **Record clip** set to `nonsense`.

- [ ] **Step 2: Pull and rename**

```bash
adb shell run-as com.summondemons.safely ls files/clips
```

Copy each WAV into `app/src/test/chant-clips/` and rename it to `{demonId}-{label}-{voice}-{stamp}.wav`, e.g. `asmodeus-chant-ro-20261019-101500.wav`.

- [ ] **Step 3: Save the baseline report**

```bash
./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i
cp app/build/reports/chant-clips.txt app/src/test/chant-clips/baseline-old-judge.txt
```

Expected: the summary shows both voices. Note the honest pass rates per phrase; they are the "before" numbers. Nothing to commit.

---

## Phase 2 — The judge

### Task 3: Expected template (`ChantTemplate`)

**Files:**
- Create: `MAIN/ritual/ChantTemplate.kt`
- Modify: `MAIN/ritual/ChantJudge.kt` (add a `distance` overload)
- Test: `TEST/ritual/ChantTemplateTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantTemplateTest {

    @Test
    fun daemoniumHasAGlideAndNeutralEndings() {
        val slots = ChantTemplate.slots("DAEMONIUM RESIDENTE,")
        assertEquals(listOf(E, O, I, U, E, I, E, E), slots.map { it.vowel })
        assertEquals(listOf(false, false, true, false, false, false, false, false), slots.map { it.optional })
        assertEquals(listOf(false, false, false, true, false, false, false, true), slots.map { it.neutral })
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1, 1), slots.map { it.word })
    }

    @Test
    fun oneSyllableWordsHaveNoNeutralEnding() {
        assertTrue(ChantTemplate.slots("HIC EST ME").none { it.neutral })
    }

    @Test
    fun quAndDiphthongsAreNotGlides() {
        assertTrue(ChantTemplate.slots("QUOD FOEDUS").none { it.optional })
    }

    @Test
    fun iOrUBeforeAnotherVowelIsAGlide() {
        assertEquals(listOf(false, true, false), ChantTemplate.slots("GRATIA").map { it.optional })
        assertEquals(listOf(true, false), ChantTemplate.slots("TUI").map { it.optional })
        assertEquals(listOf(A, I, A), ChantTemplate.slots("GRATIA").map { it.vowel })
        assertEquals(listOf(U, I), ChantTemplate.slots("TUI").map { it.vowel })
        assertEquals(listOf(O), ChantTemplate.slots("QUOD").map { it.vowel })
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantTemplateTest*'`
Expected: FAIL, compilation error `Unresolved reference: ChantTemplate`.

- [ ] **Step 3: Add the `distance` overload in `MAIN/ritual/ChantJudge.kt`**

Replace the existing `distance` function:

```kotlin
    /** Distance in [0, 1] between a heard nucleus and an expected vowel at [scale]. */
    fun distance(heard: Formants?, expected: Vowel?, scale: Double): Double {
        if (heard == null || expected == null) return CHANT_UNMEASURED_COST
        val reference = expected.formants(scale)
        val d1 = ln(heard.f1 / reference.f1)
        val d2 = ln(heard.f2 / reference.f2)
        return (sqrt(d1 * d1 + d2 * d2) / VOWEL_MAX_DISTANCE).coerceAtMost(1.0)
    }
```

with:

```kotlin
    /** Distance in [0, 1] between a heard nucleus and an expected vowel at [scale]. */
    fun distance(heard: Formants?, expected: Vowel?, scale: Double): Double {
        if (heard == null || expected == null) return CHANT_UNMEASURED_COST
        return distance(heard, expected.formants(scale))
    }

    /** Log-formant distance in [0, 1] between [heard] and a [reference] vowel sound. */
    fun distance(heard: Formants, reference: Formants): Double {
        val d1 = ln(heard.f1 / reference.f1)
        val d2 = ln(heard.f2 / reference.f2)
        return (sqrt(d1 * d1 + d2 * d2) / VOWEL_MAX_DISTANCE).coerceAtMost(1.0)
    }
```

- [ ] **Step 4: Create `MAIN/ritual/ChantTemplate.kt`**

```kotlin
package com.summondemons.safely.ritual

/**
 * One expected vowel of a chant text (ADR-0022). [vowel] is null when the syllable has no vowel
 * letter. [neutral]: the unstressed last syllable of a word of two or more syllables, which may
 * also sound as a neutral "uh". [optional]: a glide (I or U right before another vowel in the same
 * word) that may be skipped cheaply. [word] counts the words of the phrase from 0.
 */
data class VowelSlot(
    val vowel: Vowel?,
    val neutral: Boolean = false,
    val optional: Boolean = false,
    val word: Int = 0,
)

/** The expected vowel slots of a chant text, one per syllable of [LatinSyllables] (ADR-0022). */
object ChantTemplate {
    private const val VOWEL_LETTERS = "AEIOUY"
    private val DIPHTHONGS = setOf("AE", "OE", "AU", "EI", "EU")

    fun slots(text: String): List<VowelSlot> {
        val upper = text.uppercase()
        val spans = LatinSyllables.spans(text)
        val vowels = ChantJudge.expectedVowels(text)
        val wordSizes = LatinSyllables.wordSizes(text)
        val wordOf = wordSizes.flatMapIndexed { w, n -> List(n) { w } }
        return spans.mapIndexed { i, span ->
            val word = wordOf[i]
            val lastOfWord = i == spans.lastIndex || wordOf[i + 1] != word
            VowelSlot(
                vowel = vowels[i],
                neutral = lastOfWord && wordSizes[word] >= 2,
                optional = isGlide(upper, span),
                word = word,
            )
        }
    }

    private fun isGlide(text: String, span: SyllableSpan): Boolean {
        val p = (span.start until span.end).firstOrNull { isNucleus(text, it) } ?: return false
        if (text[p] != 'I' && text[p] != 'U') return false
        if (p + 1 >= text.length) return false
        if (text.substring(p, p + 2) in DIPHTHONGS) return false
        return text[p + 1] in VOWEL_LETTERS
    }

    private fun isNucleus(text: String, i: Int): Boolean =
        text[i] in VOWEL_LETTERS && !(text[i] == 'U' && i > 0 && text[i - 1] == 'Q')
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantTemplateTest*' --tests '*ChantJudgeTest*'`
Expected: PASS (ChantJudgeTest still passes: the distance refactor keeps its results).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/ChantTemplate.kt app/src/main/java/com/summondemons/safely/ritual/ChantJudge.kt app/src/test/java/com/summondemons/safely/ritual/ChantTemplateTest.kt
git commit -q -m "feat(ritual): Add the chant template with glides and neutral endings (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 4: Frame-level alignment (`ChantAligner.align`)

**Files:**
- Modify: `MAIN/ritual/RitualConfig.kt`
- Create: `MAIN/ritual/ChantAligner.kt`
- Test: `TEST/ritual/ChantAlignerTest.kt`

- [ ] **Step 1: Add constants to `MAIN/ritual/RitualConfig.kt`**

Insert directly after the line `const val CHANT_PAUSE_MS = 250L` (inside the object):

```kotlin

    // Chant check v2 (ADR-0022): frame-level alignment and shuffle baseline
    /** A neutral "uh" (adult male); accepted on the unstressed last syllable of longer words. */
    val NEUTRAL_VOWEL = Formants(500.0, 1_500.0)
    /** A slot counts as heard only with at least this many frames (3 × 20 ms). */
    const val ALIGN_MIN_SLOT_FRAMES = 3
    /** Cost of leaving one frame out of every slot (a consonant or a transition). */
    const val ALIGN_FRAME_SKIP_COST = 0.6
    /** Cost of skipping a glide slot entirely (an ordinary slot costs CHANT_GAP_COST). */
    const val ALIGN_OPTIONAL_SKIP_COST = 0.15
    /** Weight of the share of skipped frames in a chunk's cost. */
    const val ALIGN_SKIPPED_WEIGHT = 0.6
    /** A chunk never passes above this cost. Calibrated from tester clips. */
    const val ALIGN_MAX_COST = 0.8
    const val BASELINE_SHUFFLES = 24
    /** Below this many valid shuffles only [ALIGN_MAX_COST] decides (low-diversity fallback). */
    const val BASELINE_MIN_SHUFFLES = 8
    /** A shuffle must differ from the real vowel order in at least this share of slots. */
    const val BASELINE_MIN_DIFFERENT = 0.3
    /** The real order must beat at least this share of the shuffles. Calibrated from tester clips. */
    const val BASELINE_PASS_RANK = 0.7
    /** A chunk whose median heard slot is shorter than this is talk, not chanting. */
    const val TALK_MAX_SLOT_MS = 70L
    const val CHUNK_MAX_SYLLABLES = 8
    const val CHUNK_MIN_SYLLABLES = 3
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ALIGN_OPTIONAL_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.NEUTRAL_VOWEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frame-level alignment (ADR-0022). Hand-made frames: each vowel held for [k] 20 ms frames. */
class ChantAlignerTest {

    private val dae = ChantTemplate.slots("DAEMONIUM RESIDENTE,") // E O I(glide) U(neutral) E I E E(neutral)
    private val ostende = ChantTemplate.slots("OSTENDE MIHI FACIEM") // O E E I I A I E

    private fun frames(vowels: List<Vowel>, k: Int = 6, scale: Double = 1.0): List<Formants?> =
        vowels.flatMap { v -> List(k) { v.formants(scale) } }

    private fun vowels(slots: List<VowelSlot>) = slots.map { it.vowel!! }

    @Test
    fun theRightVowelsAlignAtZeroCost() {
        val a = ChantAligner.align(ostende, frames(vowels(ostende)))
        assertEquals(0.0, a.cost, 1e-9)
        assertTrue(a.slotFrames.all { it == 6 })
        assertEquals(0, a.skippedFrames)
    }

    @Test
    fun aHigherVoiceAlignsAtItsScale() {
        assertEquals(0.0, ChantAligner.align(ostende, frames(vowels(ostende), scale = 1.2)).cost, 1e-9)
    }

    @Test
    fun shortVowelsStillFillEverySlot() {
        val a = ChantAligner.align(dae, frames(vowels(dae), k = 4))
        assertEquals(0.0, a.cost, 1e-9)
        assertTrue(a.slotFrames.all { it == 4 })
    }

    @Test
    fun aSkippedGlideCostsLittle() {
        val spoken = vowels(dae).filterIndexed { i, _ -> i != 2 } // "DAE-MO-NYUM": no separate I
        val a = ChantAligner.align(dae, frames(spoken))
        assertNull(a.slotCosts[2])
        assertEquals(ALIGN_OPTIONAL_SKIP_COST / dae.size, a.cost, 1e-6)
    }

    @Test
    fun aNeutralEndingCountsAsHeard() {
        val spoken = frames(vowels(dae).dropLast(1)) + List(6) { NEUTRAL_VOWEL }
        assertEquals(0.0, ChantAligner.align(dae, spoken).cost, 1e-9)
    }

    @Test
    fun aStrayFrameBetweenVowelsIsSkipped() {
        val v = vowels(ostende)
        // A sits 0.65 from O and 0.74 from E: skipping it (0.6) is cheaper than joining either slot.
        val spoken = frames(v.take(1)) + listOf(Vowel.A.formants()) + frames(v.drop(1))
        val a = ChantAligner.align(ostende, spoken)
        assertEquals(1, a.skippedFrames)
        assertTrue(a.cost < 0.1)
    }

    @Test
    fun wrongVowelsCostMore() {
        assertTrue(ChantAligner.align(ostende, frames(List(8) { Vowel.A })).cost > 0.4)
    }

    @Test
    fun nothingHeardSkipsEverySlot() {
        val a = ChantAligner.align(ostende, emptyList())
        assertTrue(a.slotCosts.all { it == null })
        assertTrue(a.cost > 0.6)
    }
}
```

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantAlignerTest*'`
Expected: FAIL, `Unresolved reference: ChantAligner`.

- [ ] **Step 4: Create `MAIN/ritual/ChantAligner.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ALIGN_FRAME_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_MIN_SLOT_FRAMES
import com.summondemons.safely.ritual.RitualConfig.ALIGN_OPTIONAL_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_SKIPPED_WEIGHT
import com.summondemons.safely.ritual.RitualConfig.CHANT_GAP_COST
import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.RitualConfig.NEUTRAL_VOWEL
import com.summondemons.safely.ritual.RitualConfig.VOWEL_SCALES

/** How a chunk's frames line up with its slots at the best vocal-tract scale (ADR-0022). */
data class Alignment(
    /** Per slot: the mean cost of its frames when heard, or null when skipped. */
    val slotCosts: List<Double?>,
    /** Frames per slot; 0 when skipped. */
    val slotFrames: List<Int>,
    /** Frames left out of every slot (consonants, transitions). */
    val skippedFrames: Int,
    /** Mean slot cost (a skipped slot costs its gap) plus [ALIGN_SKIPPED_WEIGHT] × share of skipped frames. */
    val cost: Double,
)

/**
 * Judges a chanted chunk against its expected vowels (ADR-0022). Not word recognition: it
 * aligns every voiced frame's F1/F2 with the vowel slots the Latin spelling predicts.
 */
object ChantAligner {

    /** The best alignment over the vocal-tract scales. */
    fun align(slots: List<VowelSlot>, frames: List<Formants?>): Alignment =
        VOWEL_SCALES.map { alignAt(slots, frames, it) }.minBy { it.cost }

    /** Cost of putting [frame] in [slot]: distance to its vowel, or to "uh" when that is closer and allowed. */
    fun slotDistance(frame: Formants?, slot: VowelSlot, scale: Double): Double {
        if (frame == null || slot.vowel == null) return CHANT_UNMEASURED_COST
        val vowel = ChantJudge.distance(frame, slot.vowel, scale)
        if (!slot.neutral) return vowel
        val neutral = ChantJudge.distance(frame, Formants(NEUTRAL_VOWEL.f1 * scale, NEUTRAL_VOWEL.f2 * scale))
        return minOf(vowel, neutral)
    }

    fun gapCost(slot: VowelSlot): Double = if (slot.optional) ALIGN_OPTIONAL_SKIP_COST else CHANT_GAP_COST

    /**
     * Viterbi over states (slot j, frames c in it; c = [ALIGN_MIN_SLOT_FRAMES] means "that many or
     * more"). Per frame: join slot j, or skip the frame. Between frames: leave slot j when it has
     * no frames (skipped, its gap cost) or enough (heard, free). The path is the cheapest by these
     * additive costs; the reported [Alignment.cost] is then computed from the path.
     */
    private fun alignAt(slots: List<VowelSlot>, frames: List<Formants?>, scale: Double): Alignment {
        val s = slots.size
        val n = frames.size
        val m = ALIGN_MIN_SLOT_FRAMES
        val width = m + 1
        val size = (s + 1) * width
        fun idx(j: Int, c: Int) = j * width + c
        val d = Array(n) { i -> DoubleArray(s) { j -> slotDistance(frames[i], slots[j], scale) } }

        val advance = Array(n + 1) { IntArray(size) { -1 } }
        val frameBack = Array(n) { IntArray(size) { -1 } }
        val frameJoined = Array(n) { BooleanArray(size) }
        var cost = DoubleArray(size) { Double.POSITIVE_INFINITY }.also { it[idx(0, 0)] = 0.0 }

        fun closure(layer: DoubleArray, back: IntArray) {
            for (j in 0 until s) {
                for (c in intArrayOf(0, m)) {
                    val v = layer[idx(j, c)]
                    if (v == Double.POSITIVE_INFINITY) continue
                    val next = v + if (c == 0) gapCost(slots[j]) else 0.0
                    val target = idx(j + 1, 0)
                    if (next < layer[target]) {
                        layer[target] = next
                        back[target] = idx(j, c)
                    }
                }
            }
        }

        closure(cost, advance[0])
        for (i in 0 until n) {
            val next = DoubleArray(size) { Double.POSITIVE_INFINITY }
            for (state in 0 until size) {
                val v = cost[state]
                if (v == Double.POSITIVE_INFINITY) continue
                val j = state / width
                val c = state % width
                if (j < s) {
                    val joined = idx(j, minOf(c + 1, m))
                    val viaJoin = v + d[i][j]
                    if (viaJoin < next[joined]) {
                        next[joined] = viaJoin
                        frameBack[i][joined] = state
                        frameJoined[i][joined] = true
                    }
                }
                val viaSkip = v + ALIGN_FRAME_SKIP_COST
                if (viaSkip < next[state]) {
                    next[state] = viaSkip
                    frameBack[i][state] = state
                    frameJoined[i][state] = false
                }
            }
            closure(next, advance[i + 1])
            cost = next
        }

        // Walk the path back from "all slots done" after the last frame.
        val assigned = List(s) { mutableListOf<Int>() }
        var skipped = 0
        var state = idx(s, 0)
        var layer = n
        while (true) {
            val previous = advance[layer][state]
            if (previous != -1) {
                state = previous
                continue
            }
            if (layer == 0) break
            val i = layer - 1
            if (frameJoined[i][state]) assigned[state / width] += i else skipped++
            state = frameBack[i][state]
            layer = i
        }

        val slotCosts = assigned.mapIndexed { j, list -> if (list.isEmpty()) null else list.sumOf { d[it][j] } / list.size }
        val mean = if (s == 0) 0.0 else slotCosts.mapIndexed { j, c -> c ?: gapCost(slots[j]) }.sum() / s
        val total = mean + ALIGN_SKIPPED_WEIGHT * skipped / maxOf(1, n)
        return Alignment(slotCosts, assigned.map { it.size }, skipped, total)
    }
}
```

- [ ] **Step 5: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantAlignerTest*'`
Expected: PASS (8 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt app/src/main/java/com/summondemons/safely/ritual/ChantAligner.kt app/src/test/java/com/summondemons/safely/ritual/ChantAlignerTest.kt
git commit -q -m "feat(ritual): Align chanted frames with expected vowels (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 5: Relative verdict (`ChantAligner.judge`)

**Files:**
- Modify: `MAIN/ritual/ChantAligner.kt`
- Test: `TEST/ritual/ChantAlignerTest.kt`

- [ ] **Step 1: Add the failing tests to `ChantAlignerTest`**

Append inside the class:

```kotlin
    // --- Verdict: shuffle baseline, talk veto, word marks ---

    private val seed = "OSTENDE MIHI FACIEM"

    @Test
    fun theRightOrderBeatsEveryShuffleAndPasses() {
        val v = ChantAligner.judge(ostende, frames(vowels(ostende)), seed)
        assertTrue(v.passed)
        assertEquals(1.0, v.rank!!, 1e-9)
        assertTrue(v.shuffles >= 8)
        assertTrue(!v.talk)
    }

    @Test
    fun aHeldVowelTiesWithEveryShuffleAndFails() {
        listOf(Vowel.A, Vowel.E, Vowel.I).forEach { held ->
            val v = ChantAligner.judge(ostende, frames(List(8) { held }), seed)
            assertTrue("$held passed", !v.passed)
            assertEquals("$held rank", 0.5, v.rank!!, 1e-9)
        }
    }

    @Test
    fun babbleFails() {
        val babble = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O)
        assertTrue(!ChantAligner.judge(dae, frames(babble, k = 10), "DAEMONIUM RESIDENTE").passed)
    }

    @Test
    fun conversationalSpeedIsVetoed() {
        val fast = ChantAligner.judge(ostende, frames(vowels(ostende), k = 3), seed)
        assertTrue(fast.talk)
        assertTrue(!fast.passed)
        assertTrue(!ChantAligner.judge(ostende, frames(vowels(ostende), k = 5), seed).talk)
    }

    @Test
    fun aOneVowelChunkFallsBackToTheCeiling() {
        val estMe = ChantTemplate.slots("EST ME")
        val right = ChantAligner.judge(estMe, frames(listOf(Vowel.E, Vowel.E)), "EST ME")
        assertNull(right.rank)
        assertEquals(0, right.shuffles)
        assertTrue(right.passed)
        assertTrue(!ChantAligner.judge(estMe, frames(listOf(Vowel.A, Vowel.A)), "EST ME").passed)
    }

    @Test
    fun shufflesAreDeterministicAndMoveWholeSlots() {
        val a = ChantAligner.shuffles(dae, "DAEMONIUM RESIDENTE")
        assertEquals(a, ChantAligner.shuffles(dae, "DAEMONIUM RESIDENTE"))
        a.forEach { order -> assertEquals(dae.map { it.copy(word = 0) }.sortedBy { it.toString() }, order.map { it.copy(word = 0) }.sortedBy { it.toString() }) }
        a.forEach { order -> assertEquals(dae.map { it.word }, order.map { it.word }) }
    }

    @Test
    fun aWrongWordIsMarkedDoubtful() {
        val v = vowels(ostende)
        val spoken = v.take(3) + listOf(Vowel.U, Vowel.U) + v.drop(5) // MIHI sung as "MU-HU"
        val verdict = ChantAligner.judge(ostende, frames(spoken), seed)
        assertEquals(listOf(WordMark.Heard, WordMark.Doubtful, WordMark.Heard), verdict.marks)
    }
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantAlignerTest*'`
Expected: FAIL, `Unresolved reference: judge` / `shuffles`.

- [ ] **Step 3: Implement the verdict in `MAIN/ritual/ChantAligner.kt`**

Add these imports at the top (keep the existing ones):

```kotlin
import com.summondemons.safely.ritual.RitualConfig.ALIGN_MAX_COST
import com.summondemons.safely.ritual.RitualConfig.BASELINE_MIN_DIFFERENT
import com.summondemons.safely.ritual.RitualConfig.BASELINE_MIN_SHUFFLES
import com.summondemons.safely.ritual.RitualConfig.BASELINE_PASS_RANK
import com.summondemons.safely.ritual.RitualConfig.BASELINE_SHUFFLES
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.TALK_MAX_SLOT_MS
import com.summondemons.safely.ritual.RitualConfig.WORD_DOUBT_DISTANCE
import kotlin.math.abs
import kotlin.random.Random
```

Add this data class after `Alignment`:

```kotlin
/** The chant check's verdict on one chunk (ADR-0022). */
data class ChunkVerdict(
    val passed: Boolean,
    /** Alignment cost against the real vowel order (0 = perfect). */
    val cost: Double,
    /** Share of valid shuffles the real order beat (ties count half), or null in the low-diversity fallback. */
    val rank: Double?,
    /** Valid shuffles compared. */
    val shuffles: Int,
    /** Vetoed as conversational speed. */
    val talk: Boolean,
    /** One hint per word of the chunk. */
    val marks: List<WordMark>,
    /** The real alignment, for diagnostics. */
    val alignment: Alignment,
)
```

Add these functions inside `object ChantAligner` (after `gapCost`):

```kotlin
    /**
     * Passes when the real order costs no more than [ALIGN_MAX_COST], beats at least
     * [BASELINE_PASS_RANK] of the shuffles (unless fewer than [BASELINE_MIN_SHUFFLES] survive), and
     * the median heard slot is not shorter than [TALK_MAX_SLOT_MS]. [seed] is the chunk's text.
     */
    fun judge(slots: List<VowelSlot>, frames: List<Formants?>, seed: String): ChunkVerdict {
        val real = align(slots, frames)
        val shuffled = shuffles(slots, seed).map { align(it, frames).cost }
        val rank = if (shuffled.size >= BASELINE_MIN_SHUFFLES) {
            shuffled.sumOf { c ->
                when {
                    abs(c - real.cost) <= TIE -> 0.5
                    c > real.cost -> 1.0
                    else -> 0.0
                }
            } / shuffled.size
        } else {
            null
        }
        val heard = real.slotFrames.filter { it > 0 }.sorted()
        val talk = heard.isNotEmpty() && heard[heard.size / 2] * FRAME_MS < TALK_MAX_SLOT_MS
        val passed = !talk && real.cost <= ALIGN_MAX_COST && (rank == null || rank >= BASELINE_PASS_RANK)
        return ChunkVerdict(passed, real.cost, rank, shuffled.size, talk, marks(slots, real), real)
    }

    /**
     * [BASELINE_SHUFFLES] reorderings of whole slots (each vowel keeps its glide and neutral flags;
     * word positions stay put), seeded by [seed]; those differing from the real vowel order in
     * fewer than [BASELINE_MIN_DIFFERENT] of the slots are dropped.
     */
    fun shuffles(slots: List<VowelSlot>, seed: String): List<List<VowelSlot>> {
        val random = Random(seed.hashCode())
        val vowels = slots.map { it.vowel }
        return List(BASELINE_SHUFFLES) { slots.shuffled(random) }
            .filter { order -> order.indices.count { order[it].vowel != vowels[it] } >= BASELINE_MIN_DIFFERENT * slots.size }
            .map { order -> order.mapIndexed { i, slot -> slot.copy(word = slots[i].word) } }
    }

    /** Per word of the chunk: doubtful when its slots' mean cost reaches [WORD_DOUBT_DISTANCE]. */
    fun marks(slots: List<VowelSlot>, alignment: Alignment): List<WordMark> =
        slots.indices.groupBy { slots[it].word }.toSortedMap().values.map { indices ->
            val mean = indices.map { alignment.slotCosts[it] ?: gapCost(slots[it]) }.average()
            if (mean >= WORD_DOUBT_DISTANCE) WordMark.Doubtful else WordMark.Heard
        }

    private const val TIE = 1e-9
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantAlignerTest*'`
Expected: PASS (15 tests). The outcomes match the Python prototype run on 2026-10-18 (honest rank 1.0; held vowels rank 0.5; babble rank ≈ 0.30; 3-frame slots vetoed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/ChantAligner.kt app/src/test/java/com/summondemons/safely/ritual/ChantAlignerTest.kt
git commit -q -m "feat(ritual): Judge chunks against shuffled vowel orders (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 6: Chunks (`ChantChunks`)

**Files:**
- Create: `MAIN/ritual/ChantChunks.kt`
- Test: `TEST/ritual/ChantChunksTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/** The word groups judged together, pinned to the spec's table (§5). */
class ChantChunksTest {

    private fun texts(phrase: String) = ChantChunks.of(phrase).map { it.text to (it.syllables.last - it.syllables.first + 1) }

    @Test
    fun asmodeusSplitsHisLongLines() {
        val p = DemonCatalog.byId("asmodeus")!!.chantPhrases
        assertEquals(listOf("DAEMONIUM RESIDENTE" to 8), texts(p[0]))
        assertEquals(listOf("OSTENDE MIHI FACIEM" to 8, "TUAM POTENTIAM" to 6, "VENIT AD ME" to 4), texts(p[1]))
        assertEquals(listOf("DONA MIHI PER FOEDUS" to 7, "QUOD EST IRE AD PRAESENS" to 7, "SUM VOBIS" to 3), texts(p[2]))
        assertEquals(listOf("UT VOTA OMNIA" to 6, "OPUS EST" to 3), texts(p[3]))
    }

    @Test
    fun astarothKeepsEveryLineWhole() {
        DemonCatalog.byId("astaroth")!!.chantPhrases.forEach { assertEquals(1, ChantChunks.of(it).size) }
    }

    @Test
    fun chunksCoverThePhraseInOrder() {
        val phrase = DemonCatalog.byId("asmodeus")!!.chantPhrases[2]
        val chunks = ChantChunks.of(phrase)
        assertEquals(0, chunks.first().syllables.first)
        assertEquals(LatinSyllables.count(phrase) - 1, chunks.last().syllables.last)
        chunks.zipWithNext { a, b -> assertEquals(a.syllables.last + 1, b.syllables.first) }
        chunks.zipWithNext { a, b -> assertEquals(a.words.last + 1, b.words.first) }
        assertEquals(ChantTemplate.slots(phrase), chunks.flatMap { it.slots })
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantChunksTest*'`
Expected: FAIL, `Unresolved reference: ChantChunks`.

- [ ] **Step 3: Create `MAIN/ritual/ChantChunks.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHUNK_MAX_SYLLABLES
import com.summondemons.safely.ritual.RitualConfig.CHUNK_MIN_SYLLABLES
import kotlin.math.ceil

/**
 * A group of words judged together (ADR-0022): [text] (letters only, the shuffle seed), its word
 * and syllable index ranges in the phrase, and its expected vowel slots.
 */
data class ChantChunk(val text: String, val words: IntRange, val syllables: IntRange, val slots: List<VowelSlot>)

object ChantChunks {
    private val WORD = Regex("[A-Za-z]+")

    fun of(phrase: String): List<ChantChunk> {
        val sizes = LatinSyllables.wordSizes(phrase)
        val words = WORD.findAll(phrase).map { it.value.uppercase() }.toList()
        val slots = ChantTemplate.slots(phrase)
        var syllable = 0
        return split(sizes).map { range ->
            val count = range.sumOf { sizes[it] }
            val chunk = ChantChunk(
                text = words.slice(range).joinToString(" "),
                words = range,
                syllables = syllable until syllable + count,
                slots = slots.subList(syllable, syllable + count),
            )
            syllable += count
            chunk
        }
    }

    /**
     * Word-index ranges: the whole phrase when it has at most [CHUNK_MAX_SYLLABLES] syllables;
     * otherwise groups of at most that many, as even as possible, never under [CHUNK_MIN_SYLLABLES].
     */
    fun split(sizes: List<Int>): List<IntRange> {
        val total = sizes.sum()
        if (total <= CHUNK_MAX_SYLLABLES) return listOf(sizes.indices)
        val groups = ceil(total / CHUNK_MAX_SYLLABLES.toDouble()).toInt()
        val target = total.toDouble() / groups
        val out = mutableListOf<IntRange>()
        var start = 0
        var acc = 0
        var done = 0
        for (i in sizes.indices) {
            acc += sizes[i]
            val left = total - done - acc
            val groupsLeft = groups - out.size - 1
            val canEnd = groupsLeft > 0 && i < sizes.lastIndex && acc >= CHUNK_MIN_SYLLABLES && left >= CHUNK_MIN_SYLLABLES
            if (canEnd && (acc >= target || acc + sizes[i + 1] > CHUNK_MAX_SYLLABLES)) {
                out += start..i
                done += acc
                start = i + 1
                acc = 0
            }
        }
        out += start..sizes.lastIndex
        return out
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantChunksTest*'`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/ChantChunks.kt app/src/test/java/com/summondemons/safely/ritual/ChantChunksTest.kt
git commit -q -m "feat(ritual): Split long chant lines into chunks (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 7: Engine judges chunks with the aligner

**Files:**
- Rewrite: `MAIN/ritual/ChantJudge.kt`
- Modify: `MAIN/ritual/RitualConfig.kt`, `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`, `MAIN/ritual/RitualViewModel.kt`, `MAIN/platform/RitualFeedback.kt`, `MAIN/ui/components/DebugControls.kt`, `MAIN/ui/screens/RitualScreen.kt`, `MAIN/ui/screens/ChantScreenState.kt`, `app/src/main/res/values/strings.xml`
- Rewrite: `TEST/ritual/ChantJudgeTest.kt`, `TEST/ritual/ChantClipReplayTest.kt`
- Modify: `TEST/ritual/ChantCheckTest.kt`, `TEST/ritual/RitualViewModelTest.kt`, `TEST/ritual/SyntheticChantTest.kt`, `TEST/ui/screens/ChantScreenStateTest.kt`

This task changes types used across the module, so it compiles only as a whole. Do all steps before running the build.

- [ ] **Step 1: Write the new engine test for chunks in `TEST/ritual/ChantCheckTest.kt`**

Add at the end of the class:

```kotlin
    @Test
    fun aLongPhraseIsJudgedChunkByChunkAndARejectionReopensOnlyItsChunk() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val start = run(RitualState.start(0f, long, judged = true), List(lockFrames) { VoiceFrame.SILENT }).state
        val second = run(start, sing(ChantJudge.expectedVowels(long[0]).map { it!! }) + List(200) { VoiceFrame.SILENT }).state
        assertEquals(1, second.phrase)
        val vowels = ChantJudge.expectedVowels(long[1]).map { it!! }

        val first = run(second, sing(vowels.take(8)))
        assertTrue(RitualEffect.ChunkPassed(1, 0) in first.effects)
        assertEquals(1, first.state.chunk)
        assertEquals(8, first.state.lit)

        val wrong = run(first.state, sing(List(6) { Vowel.U }))
        assertTrue(Rejected(1, 1) in wrong.effects)
        assertEquals(1, wrong.state.chunk)
        assertEquals(8, wrong.state.lit)
        assertEquals(WordMark.Heard, wrong.state.verdict!!.marks[0])
        assertEquals(WordMark.Pending, wrong.state.verdict!!.marks[7])

        val rest = run(wrong.state, sing(vowels.drop(8)))
        assertTrue(Sealed(1) in rest.effects)
        assertEquals(2, rest.state.phrase)
        assertEquals(0, rest.state.chunk)
    }
```

- [ ] **Step 2: Update the other `ChantCheckTest` cases**

Replace `startJudgedFillsTheVowels`:

```kotlin
    @Test
    fun startJudgedBuildsTheChunks() {
        assertNull(RitualState.start(0f, phrases).chunks)
        assertEquals(phrases.map(ChantChunks::of), RitualState.start(0f, phrases, judged = true).chunks)
    }
```

Then make these edits in the file:
- `assertTrue(judged.score.passed)` → `assertTrue(judged.verdict.passed)`
- `assertFalse(step.effects.filterIsInstance<Judged>().single().score.passed)` → `assertFalse(step.effects.filterIsInstance<Judged>().single().verdict.passed)`
- `assertTrue(Rejected(0) in step.effects)` → `assertTrue(Rejected(0, 0) in step.effects)`
- `listOf(Rejected(0), Sealed(0))` → `listOf(Rejected(0, 0), Sealed(0))`
- `assertTrue(Rejected(1) in step.effects)` → `assertTrue(Rejected(1, 0) in step.effects)`

- [ ] **Step 3: Rewrite `TEST/ritual/ChantJudgeTest.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Test

class ChantJudgeTest {

    private val phrase = "DAEMONIUM RESIDENTE,"
    private val expected = ChantJudge.expectedVowels(phrase)

    @Test
    fun expectedVowelsFollowTheSpelling() {
        // DAE-MO-NI-UM RE-SI-DEN-TE
        assertEquals(listOf(E, O, I, U, E, I, E, E), expected)
    }

    @Test
    fun diphthongsAndQuAreHandled() {
        // DO-NA FOE-DUS QUOD: AE/OE read as E, the U of QU is a consonant.
        assertEquals(listOf(O, A, E, U, O), ChantJudge.expectedVowels("DONA FOEDUS QUOD"))
        assertEquals(LatinSyllables.count(phrase), expected.size)
    }

    @Test
    fun aVowelIsAtZeroDistanceFromItselfAtItsScale() {
        assertEquals(0.0, ChantJudge.distance(E.formants(1.2), E, 1.2), 1e-9)
    }

    @Test
    fun distantVowelsCapAtOne() {
        assertEquals(1.0, ChantJudge.distance(U.formants(), A, 1.0), 1e-9)
    }

    @Test
    fun unmeasuredCostsHalf() {
        assertEquals(CHANT_UNMEASURED_COST, ChantJudge.distance(null, A, 1.0), 1e-9)
    }
}
```

- [ ] **Step 4: Rewrite `MAIN/ritual/ChantJudge.kt`**

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.VOWEL_MAX_DISTANCE
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

/**
 * What has been heard of the current chunk since its first lit syllable (ADR-0022): the formants
 * of every voiced frame (null = voiced but unmeasured). Immutable; the engine advances it once per tick.
 */
data class ChantTake(
    val frames: List<Formants?> = emptyList(),
    /** ≥ 0 while the last syllable's vowel is still being heard before the verdict; -1 otherwise. */
    val closingMs: Long = -1L,
) {
    val closing: Boolean get() = closingMs >= 0L

    /** A tick that lights no syllable. Nothing counts before the chunk's first syllable. */
    fun hear(voice: VoiceFrame, begun: Boolean): ChantTake {
        if (!begun) return this
        val closing = if (closing) closingMs + FRAME_MS else -1L
        return if (voice.voiced) copy(frames = frames + voice.formants, closingMs = closing) else copy(closingMs = closing)
    }

    /** A tick that lights a syllable; [first] starts a new chunk's take. */
    fun light(voice: VoiceFrame, first: Boolean, last: Boolean): ChantTake {
        val base = if (first) ChantTake() else this
        return base.copy(frames = base.frames + voice.formants, closingMs = if (last) 0L else -1L)
    }
}

/** Expected vowels from Latin spelling, and distances between vowel sounds (ADR-0018). The verdict is [ChantAligner]'s (ADR-0022). */
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

    /** Distance in [0, 1] between a heard nucleus and an expected vowel at [scale]. */
    fun distance(heard: Formants?, expected: Vowel?, scale: Double): Double {
        if (heard == null || expected == null) return CHANT_UNMEASURED_COST
        return distance(heard, expected.formants(scale))
    }

    /** Log-formant distance in [0, 1] between [heard] and a [reference] vowel sound. */
    fun distance(heard: Formants, reference: Formants): Double {
        val d1 = ln(heard.f1 / reference.f1)
        val d2 = ln(heard.f2 / reference.f2)
        return (sqrt(d1 * d1 + d2 * d2) / VOWEL_MAX_DISTANCE).coerceAtMost(1.0)
    }

    private fun isVowel(s: String, i: Int): Boolean =
        s[i] in "AEIOUY" && !(s[i] == 'U' && i > 0 && s[i - 1] == 'Q')
}
```

- [ ] **Step 5: Remove the old scoring constants from `MAIN/ritual/RitualConfig.kt`**

Delete these lines (keep `VOWEL_SCALES`, `VOWEL_MAX_DISTANCE`, `CHANT_GAP_COST`, `CHANT_UNMEASURED_COST`, `SEAL_LISTEN_MS`):

```kotlin
    const val CHANT_SYLLABLES_PER_PAUSE = 6
    const val CHANT_PAUSE_PENALTY = 0.25f
    const val CHANT_FAST_INTERVAL_MS = 170L
    const val CHANT_RHYTHM_WEIGHT = 0.2f
    /** A phrase seals only when its [ChantScore.total] reaches this. Calibrated from tester clips. */
    const val CHANT_PASS_SCORE = 0.6f
```

```kotlin
    /** An unvoiced run at least this long inside a phrase counts as a pause (breath). */
    const val CHANT_PAUSE_MS = 250L
```

Change the section comment `// Chant plausibility check (ADR-0018)` to `// Chant check (ADR-0018, ADR-0022)`.

- [ ] **Step 6: Update `MAIN/ritual/RitualState.kt`**

a) In `RitualEffect`, replace the `Judged` and `Rejected` declarations:

```kotlin
    /** The chant check judged [chunk] of [phrase] (ADR-0022); followed by [ChunkPassed], [Sealed] or [Rejected]. */
    data class Judged(val phrase: Int, val chunk: Int, val verdict: ChunkVerdict) : RitualEffect

    /** [chunk] of [phrase] passed and was not its last: the next chunk opens (ADR-0022). */
    data class ChunkPassed(val phrase: Int, val chunk: Int) : RitualEffect

    /** [chunk] of [phrase] didn't sound like its text: it reopens at its first syllable (ADR-0022). */
    data class Rejected(val phrase: Int, val chunk: Int) : RitualEffect
```

b) In `RitualState`, replace

```kotlin
    /** Expected vowel per syllable of each phrase; null turns the chant check off (ADR-0018). */
    val vowels: List<List<Vowel?>>? = null,
    /** What has been heard of the current phrase; only advanced while the chant check is on. */
    val take: ChantTake = ChantTake(),
```

with

```kotlin
    /** Each phrase's chunks; null turns the chant check off (ADR-0018, ADR-0022). */
    val chunks: List<List<ChantChunk>>? = null,
    /** Index of the open chunk in the current phrase. */
    val chunk: Int = 0,
    /** What has been heard of the current chunk; only advanced while the chant check is on. */
    val take: ChantTake = ChantTake(),
```

c) Replace the init check `require(vowels == null || vowels.size == PHRASE_COUNT) { "need vowels for exactly $PHRASE_COUNT phrases" }` with

```kotlin
        require(chunks == null || chunks.size == PHRASE_COUNT) { "need chunks for exactly $PHRASE_COUNT phrases" }
```

d) In `start(...)`, replace `vowels = if (judged) phrases.map(ChantJudge::expectedVowels) else null,` with

```kotlin
            chunks = if (judged) phrases.map(ChantChunks::of) else null,
```

- [ ] **Step 7: Update `MAIN/ritual/RitualEngine.kt`**

a) In `chant(...)`, replace from `if (!lights) return drain(chanting.hearing { hear(voice, begun = s.lit > 0) }, 1, effects)` through the end of the function with:

```kotlin
        if (!lights) return drain(chanting.hearing { hear(voice, begun = s.lit > s.chunkStart()) }, 1, effects)

        effects += RitualEffect.SyllableLit
        val lit = s.lit + 1
        val last = lit == s.chunkEnd()
        // Chanting again clears the last verdict (ADR-0019).
        val reset = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L, lit = lit, verdict = null)
            .hearing { light(voice, first = s.lit == s.chunkStart(), last = last) }
        // With the chant check on, the last syllable's vowel is heard before the verdict (ADR-0018).
        if (!last || reset.take.closing) return drain(reset, 1, effects)
        return seal(reset, effects)
    }
```

b) Replace the whole `close(...)` function with:

```kotlin
    /** Hears the end of the chunk's last syllable, then judges it (ADR-0022). */
    private fun close(s: RitualState, voice: VoiceFrame, effects: MutableList<RitualEffect>): Step {
        val take = s.take.hear(voice, begun = true)
        if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), 1, effects)
        val chunk = s.currentChunk()!!
        val verdict = ChantAligner.judge(chunk.slots, take.frames, chunk.text)
        effects += RitualEffect.Judged(s.phrase, s.chunk, verdict)
        val marks = phraseMarks(s, chunk, verdict.marks)
        if (verdict.passed) {
            if (s.chunk < s.chunks!![s.phrase].lastIndex) {
                effects += RitualEffect.ChunkPassed(s.phrase, s.chunk)
                return drain(s.copy(chunk = s.chunk + 1, take = ChantTake()), 1, effects)
            }
            return seal(s.copy(take = ChantTake(), verdict = ChantVerdict(ChantVerdict.Kind.Sealed, s.phrase, marks)), effects)
        }
        effects += RitualEffect.Rejected(s.phrase, s.chunk)
        val rejected = ChantVerdict(ChantVerdict.Kind.Rejected, s.phrase, marks)
        return Step(s.copy(lit = chunk.syllables.first, take = ChantTake(), verdict = rejected), effects)
    }

    /** Words before the chunk were heard; the chunk's own come from the verdict; later ones are pending. */
    private fun phraseMarks(s: RitualState, chunk: ChantChunk, marks: List<WordMark>): List<WordMark> =
        List(s.wordSizes!![s.phrase].size) { w ->
            when {
                w < chunk.words.first -> WordMark.Heard
                w in chunk.words -> marks[w - chunk.words.first]
                else -> WordMark.Pending
            }
        }

    private fun RitualState.currentChunk(): ChantChunk? = chunks?.getOrNull(phrase)?.getOrNull(chunk)

    private fun RitualState.chunkStart(): Int = currentChunk()?.syllables?.first ?: 0

    private fun RitualState.chunkEnd(): Int = currentChunk()?.let { it.syllables.last + 1 } ?: syllables[phrase]
```

c) In `seal(...)`, change `return Step(s.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget), effects)` to

```kotlin
            return Step(s.copy(phrase = phrase, lit = 0, chunk = 0, focusMs = budget, focusBudgetMs = budget), effects)
```

d) Replace `private inline fun RitualState.hearing(update: ChantTake.() -> ChantTake): RitualState =\n        if (vowels == null) this else copy(take = take.update())` with

```kotlin
    private inline fun RitualState.hearing(update: ChantTake.() -> ChantTake): RitualState =
        if (chunks == null) this else copy(take = take.update())
```

e) In `drain(...)`, change `val cracked = if (s.vowels != null) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null` to `val cracked = if (s.chunks != null) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null`, and in the returned copy add `chunk = 0,` after `lit = 0,`.

f) Update the object's KDoc first line to mention ADR-0022: `/** Pure ritual rules (ADR-0008, the focus clock of ADR-0010, the chant check of ADR-0018/ADR-0022, verdicts of ADR-0019): one call per 20 ms tick. */`

- [ ] **Step 8: Update `MAIN/ritual/RitualViewModel.kt`**

a) Replace

```kotlin
    /** The chant check's latest verdict, for the debug readout (ADR-0018). */
    val lastScore: ChantScore? = null,
```

with

```kotlin
    /** The chant check's latest verdict, for the debug readout (ADR-0022). */
    val lastJudged: RitualEffect.Judged? = null,
```

b) Replace `lastScore = step.effects.filterIsInstance<RitualEffect.Judged>().lastOrNull()?.score ?: it.lastScore,` with

```kotlin
                lastJudged = step.effects.filterIsInstance<RitualEffect.Judged>().lastOrNull() ?: it.lastJudged,
```

c) In `syntheticVoice(...)`, replace

```kotlin
        val vowels = ritual.vowels?.getOrNull(ritual.phrase).orEmpty()
        val index = (if (onset) ritual.lit else ritual.lit - 1).coerceIn(0, maxOf(0, vowels.lastIndex))
        return VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = onset, formants = vowels.getOrNull(index)?.formants())
```

with

```kotlin
        val slots = ritual.chunks?.getOrNull(ritual.phrase)?.flatMap { it.slots }.orEmpty()
        val index = (if (onset) ritual.lit else ritual.lit - 1).coerceIn(0, maxOf(0, slots.lastIndex))
        return VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = onset, formants = slots.getOrNull(index)?.vowel?.formants())
```

Remove the now-unused `ChantScore` import if present.

- [ ] **Step 9: Update `MAIN/platform/RitualFeedback.kt`**

After the line `is RitualEffect.Judged -> Unit` add:

```kotlin
        is RitualEffect.ChunkPassed -> Haptics.tick(context)
```

- [ ] **Step 10: Update the debug panel**

In `app/src/main/res/values/strings.xml` replace

```xml
    <string name="debug_chant_score">Chant check %1$.2f %2$s · vowels %3$.2f · rhythm %4$.2f</string>
```

with

```xml
    <string name="debug_chant_score">Chant check %1$s·%2$d · cost %3$.2f · rank %4$s %5$s</string>
```

In `MAIN/ui/components/DebugControls.kt`:
- replace the import `com.summondemons.safely.ritual.ChantScore` with `com.summondemons.safely.ritual.RitualEffect`;
- replace the parameter `lastScore: ChantScore?,` with `lastJudged: RitualEffect.Judged?,` and its KDoc `/** The chant check's latest verdict (ADR-0018). */` with `/** The chant check's latest chunk verdict (ADR-0022). */`;
- replace the `Text(...)` that shows the score with:

```kotlin
        Text(
            if (lastJudged == null) {
                stringResource(R.string.debug_chant_score_none)
            } else {
                val v = lastJudged.verdict
                stringResource(
                    R.string.debug_chant_score,
                    ROMAN_NUMERALS[lastJudged.phrase],
                    lastJudged.chunk + 1,
                    v.cost,
                    v.rank?.let { "%.0f %%".format(it * 100) } ?: "–",
                    if (v.passed) "✓" else if (v.talk) "✗ talk" else "✗",
                )
            },
            style = small,
            color = colors.onSurfaceVariant,
        )
```

In `MAIN/ui/screens/RitualScreen.kt`, change `lastScore = state.lastScore,` to `lastJudged = state.lastJudged,`.

- [ ] **Step 11: Update `MAIN/ui/screens/ChantScreenState.kt`**

a) In `chantScreenState(...)`, replace the rejected branch

```kotlin
        verdict?.kind == ChantVerdict.Kind.Rejected -> text.mapIndexed { i, syllables ->
            val look = if (verdict.marks.getOrNull(i) == WordMark.Doubtful) WordLook.Rejected else WordLook.Heard
            ChantWord(syllables, 0, look)
        }
```

with

```kotlin
        verdict?.kind == ChantVerdict.Kind.Rejected -> text.mapIndexed { i, syllables ->
            val look = when (verdict.marks.getOrNull(i)) {
                WordMark.Doubtful -> WordLook.Rejected
                WordMark.Pending -> WordLook.Pending
                else -> WordLook.Heard
            }
            ChantWord(syllables, 0, look)
        }
```

b) Replace `chantingWords(...)` with:

```kotlin
/** While chanting: lit syllables, the current word, and the words of chunks already passed (ADR-0022). */
private fun chantingWords(ritual: RitualState, phrase: Int, text: List<List<String>>): List<ChantWord> {
    val passedWords = ritual.chunks?.getOrNull(phrase)?.getOrNull(ritual.chunk)?.words?.first ?: 0
    var start = 0
    return text.mapIndexed { i, syllables ->
        val lit = (ritual.lit - start).coerceIn(0, syllables.size)
        val current = ritual.lit in start until start + syllables.size
        start += syllables.size
        val look = when {
            current -> WordLook.Current
            i < passedWords -> WordLook.Heard
            else -> WordLook.Pending
        }
        ChantWord(syllables, lit, look)
    }
}
```

c) Remove the unused import `com.summondemons.safely.ritual.ChantJudge`.

- [ ] **Step 12: Update `TEST/ui/screens/ChantScreenStateTest.kt`**

Replace `aHeardWordGetsItsHintOnceItsSyllablesClose` with:

```kotlin
    @Test
    fun wordsOfPassedChunksAreHeard() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val r = RitualState.start(90f, long, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f, phrase = 1, chunk = 1, lit = 8)
        val s = chantScreenState(r, long, 90f, 90f)
        assertEquals(List(3) { WordLook.Heard } + WordLook.Current, s.words.take(4).map { it.look })
        assertEquals(WordLook.Pending, s.words[4].look)
    }
```

Remove the now-unused imports `ChantTake`, `Formants` and `Vowel` from that file.

- [ ] **Step 13: Update `TEST/ritual/RitualViewModelTest.kt`**

Replace

```kotlin
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.Judged },
```

with

```kotlin
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.Judged && it !is RitualEffect.ChunkPassed },
```

and replace

```kotlin
        assertEquals(4, effects.count { it is RitualEffect.Judged && it.score.passed })
        assertTrue(vm.uiState.value.lastScore!!.passed)
```

with

```kotlin
        val judged = effects.filterIsInstance<RitualEffect.Judged>()
        assertTrue(judged.size >= 4)
        assertTrue(judged.all { it.verdict.passed })
        assertTrue(vm.uiState.value.lastJudged!!.verdict.passed)
```

- [ ] **Step 14: Update `TEST/ritual/SyntheticChantTest.kt`**

- every `it.score.passed` / `judged.single().score.passed` → `it.verdict.passed` / `judged.single().verdict.passed`;
- every `"score=${judged.single().score}"` → `"verdict=${judged.single().verdict}"`;
- in `theFirstTwoPhrasesInARowBothSeal` replace `assertEquals(listOf(0, 1), judged.map { it.phrase })` with `assertEquals(listOf(0, 1), judged.map { it.phrase }.distinct())` and add `assertEquals(3, judged.count { it.phrase == 1 })` (phrase II has three chunks);
- in `repeatingOlaMeuAmigoNeverSealsPhraseI` rename it to `repeatingOlaMeuAmigoIsRejected`, and replace the last assertion with `assertTrue("verdicts=${judged.map { it.verdict }}", judged.any { !it.verdict.passed })`;
- in `holdingOneVowelNeverSealsPhraseI` replace `(Vowel.entries - Vowel.E).forEach { v ->` with `Vowel.entries.forEach { v ->`, delete the comment line above it, and change `"$v scores=${judged.map { it.score.total }}"` to `"$v verdicts=${judged.map { it.verdict }}"` and `it.score.passed` to `it.verdict.passed`.

- [ ] **Step 15: Rewrite `TEST/ritual/ChantClipReplayTest.kt` for chunk verdicts**

Replace the file with:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0022). Replays tester clips through the real
 * pipeline and prints every chunk verdict with its aligned path, per voice.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices), named
 * `{demonId}-{label}-{voice}-{stamp}.wav` (label `chant` or `nonsense`); the older
 * `{demonId}-{label}-{stamp}.wav` names count as voice `unknown`. Without clips the test is
 * skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 * The report is also written to `app/build/reports/chant-clips.txt`; compare it with
 * `app/src/test/chant-clips/baseline-old-judge.txt`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Clip(val name: String, val demon: String, val label: String, val voice: String)

    private data class Result(val clip: Clip, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsMeetTheGate() {
        val files = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", files.isNotEmpty())
        val results = files.flatMap { file ->
            val clip = parse(file)
            val demon = DemonCatalog.byId(clip.demon) ?: error("${file.name}: unknown demon '${clip.demon}'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(clip, it) }
        }
        val report = buildString {
            results.forEach { r -> appendLine(line(r)); appendLine(path(r)) }
            appendLine()
            summary(results).forEach(::appendLine)
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/chant-clips.txt").writeText(report)

        val chant = results.filter { it.clip.label == "chant" }
        assumeTrue("not enough data", sufficiency(chant) == null)
        chant.groupBy { it.clip.voice }.forEach { (voice, list) ->
            val rejected = list.count { !it.judged.verdict.passed } * 100.0 / list.size
            assertTrue("voice $voice: %.1f %% honest chunks rejected".format(rejected), rejected <= HONEST_REJECT_MAX)
        }
    }

    private fun line(r: Result): String {
        val v = r.judged.verdict
        return "%-48s %-8s %-6s %-3s·%d cost %.2f rank %s shuffles %2d%s %s".format(
            r.clip.name, r.clip.label, r.clip.voice, ROMAN[r.judged.phrase], r.judged.chunk + 1, v.cost,
            v.rank?.let { "%.2f".format(it) } ?: " –  ", v.shuffles, if (v.talk) " talk" else "", if (v.passed) "PASS" else "fail",
        )
    }

    /** Expected vowel → frames (cost, or "skip") for each slot, then skipped frames. */
    private fun path(r: Result): String {
        val demon = DemonCatalog.byId(r.clip.demon)!!
        val slots = ChantChunks.of(demon.chantPhrases[r.judged.phrase])[r.judged.chunk].slots
        val a = r.judged.verdict.alignment
        val parts = slots.mapIndexed { j, slot ->
            val c = a.slotCosts[j]
            "${slot.vowel ?: "-"}${if (slot.optional) "?" else ""}:" + if (c == null) "skip" else "${a.slotFrames[j]}f %.2f".format(c)
        }
        return "    " + parts.joinToString("  ") + "  | skipped ${a.skippedFrames}f"
    }

    private fun parse(file: File): Clip {
        val parts = file.nameWithoutExtension.split('-')
        require(parts.size >= 3) { "${file.name}: expected {demonId}-{label}-{voice}-{stamp}.wav" }
        val voice = if (parts.size >= 4) parts[2] else "unknown"
        return Clip(file.name, parts[0], parts[1], voice)
    }

    private fun summary(results: List<Result>): List<String> = buildList {
        add("voice     label     chunks  passed")
        results.groupBy { it.clip.voice to it.clip.label }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            add("%-9s %-9s %6d  %5.1f %%".format(key.first, key.second, list.size, list.count { it.judged.verdict.passed } * 100.0 / list.size))
        }
        add("")
        add("honest chants per phrase: takes, voices, chunks passed")
        val chant = results.filter { it.clip.label == "chant" }
        chant.groupBy { it.clip.demon to it.judged.phrase }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            val takes = list.map { it.clip.name }.distinct().size
            val voices = list.map { it.clip.voice }.distinct().size
            add("%-10s %-3s takes %2d voices %d passed %5.1f %%".format(key.first, ROMAN[key.second], takes, voices, list.count { it.judged.verdict.passed } * 100.0 / list.size))
        }
        add("")
        add("enough data? " + (sufficiency(chant) ?: "yes"))
    }

    /** Null when every judged phrase has ≥ 4 honest takes from ≥ 2 voices; else what is missing. */
    private fun sufficiency(chant: List<Result>): String? {
        if (chant.isEmpty()) return "insufficient: no honest chants"
        val missing = chant.groupBy { it.clip.demon to it.judged.phrase }.mapNotNull { (key, list) ->
            val takes = list.map { it.clip.name }.distinct().size
            val voices = list.map { it.clip.voice }.distinct().size
            if (takes < MIN_TAKES || voices < MIN_VOICES) "${key.first} ${ROMAN[key.second]} ($takes takes, $voices voices)" else null
        }
        return if (missing.isEmpty()) null else "insufficient: " + missing.joinToString()
    }

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        const val MIN_TAKES = 4
        const val MIN_VOICES = 2
        const val HONEST_REJECT_MAX = 2.0
    }
}
```

- [ ] **Step 16: Build and run every test**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass. If `SyntheticChantTest.repeatingOlaMeuAmigoIsRejected` fails because every repetition passed, do not loosen the test: note the verdicts it prints and carry them to the tuning checkpoint in Task 8.

- [ ] **Step 17: Commit**

```bash
git add -A app/src
git commit -q -m "feat(ritual): Judge each chunk with the aligner and reopen only a rejected chunk (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 8: Tune from the clips (human checkpoint)

**Files:** possibly `MAIN/ritual/RitualConfig.kt` (tuning values only).

- [ ] **Step 1: Run the harness**

Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`
Expected: per-chunk lines with paths, the summary, and either PASS (gate met) or an assertion naming the voice whose honest chunks were rejected more than 2 % of the time.

- [ ] **Step 2: If the gate fails, adjust one value at a time and re-run**

Read the `path` lines of the rejected honest chunks:
- many `skip` on non-glide slots → lower `ALIGN_FRAME_SKIP_COST` (0.6 → 0.5) or `ALIGN_MIN_SLOT_FRAMES` (3 → 2);
- high per-slot costs on correct-looking paths → raise `ALIGN_MAX_COST` or `VOWEL_MAX_DISTANCE`;
- low `rank` on honest chunks with sensible paths → lower `BASELINE_PASS_RANK` (0.7 → 0.6);
- `talk` on honest chunks → lower `TALK_MAX_SLOT_MS`.

After each change also run `./gradlew testDebugUnitTest --tests '*ChantAligner*' --tests '*ChantCheck*' --tests '*SyntheticChant*'` so the synthetic expectations still hold. Stop when both voices are at ≤ 2 % honest rejects; then read the nonsense pass rate in the summary and note it.

- [ ] **Step 3: Commit any tuned values**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt
git commit -q -m "chore(ritual): Tune the chant check from tester clips (ADR-0022)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Record the final values and the measured rates; Task 13 copies them into ADR-0022.

---

## Phase 3 — Focus meter

### Task 9: Focus drains only on idle silence, drift and rejections

**Files:**
- Modify: `MAIN/ritual/RitualConfig.kt`, `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`
- Test: `TEST/ritual/RitualEngineTest.kt`, `TEST/ritual/ChantCheckTest.kt`

- [ ] **Step 1: Update the engine tests first**

In `TEST/ritual/RitualEngineTest.kt`:

a) Add imports `com.summondemons.safely.ritual.RitualConfig.FOCUS_IDLE_GRACE_MS`.

b) In `aSealStartsAFullClockForTheNextPhrase`, change `assertEquals(focusBudget(step.state.syllables[1], 1), step.state.focusBudgetMs)` to `assertEquals(focusBudget(1), step.state.focusBudgetMs)`.

c) Replace `drainsOnceOnHeadingEvenInSilenceAndThreeTimesOffHeading` with:

```kotlin
    @Test
    fun silenceDrainsOnlyAfterTheIdleGraceAndOffHeadingDrainsThreeTimes() {
        val sealed = firstSealed()
        val grace = (FOCUS_IDLE_GRACE_MS / FRAME_MS).toInt()
        assertEquals(sealed.focusMs, run(sealed, grace).state.focusMs)
        assertEquals(10 * FRAME_MS, sealed.focusMs - run(sealed, grace + 10).state.focusMs)
        val offHeading = run(sealed, 10, azimuth = off).state
        assertEquals(10 * FRAME_MS * FOCUS_DRIFT_MULTIPLIER, sealed.focusMs - offHeading.focusMs)
    }

    @Test
    fun voicingNeverDrains() {
        val sealed = firstSealed()
        assertEquals(sealed.focusMs, run(sealed, 15, voice = voiced).state.focusMs)
    }
```

d) In `runningOutCracksTheLastSealClearsThePhraseAndEndsFlawless`, replace `val step = run(s, ceil(s.focusMs.toDouble() / FRAME_MS).toInt() + 1)` with

```kotlin
        val step = run(s, (FOCUS_IDLE_GRACE_MS / FRAME_MS).toInt() + ceil(s.focusMs.toDouble() / FRAME_MS).toInt() + 1)
```

e) In `aCrackOnPhraseThreeReopensPhraseTwoWithItsOwnFullBudget`, change `assertEquals(focusBudget(s.syllables[1], 1), step.state.focusBudgetMs)` to `assertEquals(focusBudget(1), step.state.focusBudgetMs)`.

f) Replace `budgetsAreThreeSecondsPlusHalfASecondPerSyllableScaledInCourt` with:

```kotlin
    @Test
    fun budgetsAreAFlatEightSecondsScaledInCourt() {
        assertEquals(8_000L, focusBudget(1))
        assertEquals(10_000L, focusBudget(1, 1.25f))
        assertEquals(0L, focusBudget(0))
    }
```

In `TEST/ritual/ChantCheckTest.kt`:

g) Add imports `com.summondemons.safely.ritual.RitualConfig.FOCUS_IDLE_GRACE_MS`, `com.summondemons.safely.ritual.RitualConfig.FOCUS_REJECT_COST` and `kotlin.math.roundToLong`.

h) In `aVerdictHoldsTheClockUntilChantingResumesOrTheHoldEnds`, replace the last line `assertTrue(chanting.focusMs < budget)` with `assertEquals(budget, chanting.focusMs) // chanting never drains`.

i) In `aRejectionKeepsTheDoubtfulWordsAndHoldsTheClock`, after `assertTrue(WordMark.Doubtful in verdict.marks)` add:

```kotlin
        assertEquals(second.focusMs - (second.focusBudgetMs * FOCUS_REJECT_COST).roundToLong(), step.state.focusMs)
```

j) In `aCrackLeavesACrackedVerdict`, change `.copy(focusMs = FRAME_MS, verdict = null)` to `.copy(focusMs = FRAME_MS, verdict = null, quietMs = FOCUS_IDLE_GRACE_MS)`.

k) Replace `aCrackWhileListeningClearsTheTake` with:

```kotlin
    @Test
    fun aRejectionThatEmptiesFocusCracksAndClearsTheTake() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = 1_000L, verdict = null)
        val step = run(second, sing(List(expected(1).size) { Vowel.U }))
        assertTrue(Rejected(1, 0) in step.effects)
        assertTrue(Cracked(0) in step.effects)
        assertEquals(ChantTake(), step.state.take)
        assertEquals(0, step.state.phrase)
    }
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*RitualEngineTest*' --tests '*ChantCheckTest*'`
Expected: FAIL to compile (`focusBudget` has no 1-argument form, `quietMs`, `FOCUS_IDLE_GRACE_MS` unknown).

- [ ] **Step 3: Constants in `MAIN/ritual/RitualConfig.kt`**

Replace

```kotlin
    const val FOCUS_BASE_MS = 4_000L
    const val FOCUS_PER_SYLLABLE_MS = 750L
```

with

```kotlin
    /** Lost focus allowed per phrase II–IV (ADR-0023). */
    const val FOCUS_BUDGET_MS = 8_000L
    /** Silence shorter than this costs no focus (ADR-0023). */
    const val FOCUS_IDLE_GRACE_MS = 2_000L
    /** Share of the budget a rejected chunk costs at once (ADR-0023). */
    const val FOCUS_REJECT_COST = 0.25f
```

and update the section comment above them to `// Focus meter (ADR-0023) and the Court Hours bonus (ADR-0012)`.

- [ ] **Step 4: State in `MAIN/ritual/RitualState.kt`**

a) Replace the imports of `FOCUS_BASE_MS` and `FOCUS_PER_SYLLABLE_MS` with `import com.summondemons.safely.ritual.RitualConfig.FOCUS_BUDGET_MS`.

b) Add a field after `voicedRunMs`:

```kotlin
    /** Silence since the last voiced frame while chanting; focus drains only past FOCUS_IDLE_GRACE_MS (ADR-0023). */
    val quietMs: Long = 0L,
```

c) Replace `focusBudget`:

```kotlin
/** Focus budget in ms for phrase index [phrase] (ADR-0023): flat, scaled in court; none for phrase I. */
fun focusBudget(phrase: Int, courtMultiplier: Float = 1f): Long =
    if (phrase <= 0) 0L else (FOCUS_BUDGET_MS * courtMultiplier).roundToLong()
```

- [ ] **Step 5: Engine in `MAIN/ritual/RitualEngine.kt`**

a) Add imports:

```kotlin
import com.summondemons.safely.ritual.RitualConfig.FOCUS_IDLE_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_REJECT_COST
import kotlin.math.roundToLong
```

b) In `chant(...)`, replace

```kotlin
        val chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run)
```

with

```kotlin
        val quiet = if (voice.voiced) 0L else s.quietMs + FRAME_MS
        val chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run, quietMs = quiet)
```

c) In `chant(...)` and `close(...)`, replace every `drain(<state>, 1, effects)` with `drain(<state>, idle(<state>), effects)`, i.e.:
- `return drain(chanting.hearing { hear(voice, begun = s.lit > s.chunkStart()) }, 1, effects)` → `return drain(chanting.hearing { hear(voice, begun = s.lit > s.chunkStart()) }, idle(chanting), effects)`
- `if (!last || reset.take.closing) return drain(reset, 1, effects)` → `if (!last || reset.take.closing) return drain(reset, idle(reset), effects)`
- `if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), 1, effects)` → `if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), idle(s), effects)`
- `return drain(s.copy(chunk = s.chunk + 1, take = ChantTake()), 1, effects)` → `return drain(s.copy(chunk = s.chunk + 1, take = ChantTake()), idle(s), effects)`

and add the helper:

```kotlin
    /** Focus drains 1× only after FOCUS_IDLE_GRACE_MS of silence; chanting and short pauses cost nothing (ADR-0023). */
    private fun idle(s: RitualState): Int = if (s.quietMs > FOCUS_IDLE_GRACE_MS) 1 else 0
```

d) In `close(...)`, replace the rejection tail

```kotlin
        effects += RitualEffect.Rejected(s.phrase, s.chunk)
        val rejected = ChantVerdict(ChantVerdict.Kind.Rejected, s.phrase, marks)
        return Step(s.copy(lit = chunk.syllables.first, take = ChantTake(), verdict = rejected), effects)
```

with

```kotlin
        effects += RitualEffect.Rejected(s.phrase, s.chunk)
        val rejected = s.copy(lit = chunk.syllables.first, take = ChantTake(), verdict = ChantVerdict(ChantVerdict.Kind.Rejected, s.phrase, marks))
        if (s.phrase < 1) return Step(rejected, effects)
        val focus = rejected.focusMs - (rejected.focusBudgetMs * FOCUS_REJECT_COST).roundToLong()
        return if (focus > 0L) Step(rejected.copy(focusMs = focus), effects) else crack(rejected, effects)
```

e) In `seal(...)`, change `val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)` to `val budget = focusBudget(phrase, s.courtMultiplier)`.

f) Replace the crack part of `drain(...)` (from `val phrase = s.phrase - 1` to the end of the function) with `return crack(s, effects)` preceded by the focus check, so `drain` reads:

```kotlin
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val verdict = s.verdict
        if (verdict != null && verdict.holdsClock) return Step(s.copy(verdict = verdict.copy(heldMs = verdict.heldMs + FRAME_MS)), effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        return crack(s, effects)
    }

    /** Focus ran out: the last seal cracks and its phrase reopens with a full meter (ADR-0010, ADR-0023). */
    private fun crack(s: RitualState, effects: MutableList<RitualEffect>): Step {
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(phrase, s.courtMultiplier)
        val cracked = if (s.chunks != null) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null
        return Step(
            s.copy(
                phrase = phrase, lit = 0, chunk = 0, focusMs = budget, focusBudgetMs = budget,
                flawless = false, take = ChantTake(), verdict = cracked, quietMs = 0L,
            ),
            effects,
        )
    }
```

g) Update the KDoc of `drain` to: `/** Drains the focus meter by [multiplier] frames (phrases II–IV only); at 0 the last seal cracks. While a fresh verdict is on screen the meter waits instead (ADR-0019, ADR-0023). */`

- [ ] **Step 6: Run the tests**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add -A app/src
git commit -q -m "feat(ritual): Replace the focus clock with a focus meter (ADR-0023)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 4 — Chant screen

### Task 10: Status block model and the reduced verdict line

**Files:**
- Modify: `MAIN/ui/screens/ChantScreenState.kt`
- Test: `TEST/ui/screens/ChantScreenStateTest.kt`

- [ ] **Step 1: Update the tests first**

In `TEST/ui/screens/ChantScreenStateTest.kt`:

a) In `theWordBeingChantedIsCurrentWithItsLitSyllables`, replace the last two lines with `assertEquals(VerdictLine.Listening, s.verdict)`.

b) Replace `sealedShowsThePhraseJustSealed` with:

```kotlin
    @Test
    fun aSealedPhraseLightsItsSealAndTheLineKeepsListening() {
        val r = chanting.copy(phrase = 1, verdict = ChantVerdict(ChantVerdict.Kind.Sealed, 0))
        val s = state(r)
        assertEquals(VerdictLine.Listening, s.verdict)
        assertEquals(SealLook.Sealed, s.status.seals[0].look)
        assertEquals(SealLook.Current, s.status.seals[1].look)
        assertEquals(SealLook.Pending, s.status.seals[2].look)
    }
```

c) Replace `aCrackSaysSo` with:

```kotlin
    @Test
    fun aCrackSaysSoAndMarksItsSeal() {
        val s = state(chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0)))
        assertEquals(VerdictLine.SealCracked, s.verdict)
        assertEquals(SealLook.Cracked, s.status.seals[0].look)
    }
```

d) In `offCourseItPausesAndPointsTheWay`, replace `assertEquals(VerdictLine.Paused, s.verdict)` with:

```kotlin
        assertEquals(VerdictLine.Listening, s.verdict)
        assertEquals(StepState.Drifting, s.status.step)
        assertEquals(TurnDirection.Right, s.status.turn)
```

e) Replace `phraseIHasAFullRingAndNoSeconds` with:

```kotlin
    @Test
    fun phraseIHasAFullRingAndStepOneIsDone() {
        val s = state(chanting)
        assertTrue(s.facing)
        assertNull(s.turn)
        assertEquals(1f, s.clockFraction)
        assertEquals(StepState.Aligned, s.status.step)
        assertNull(s.status.turn)
    }
```

f) In `afterTheFourthSealEveryWordIsHeard`, replace `assertEquals(3, s.numeral)` with:

```kotlin
        assertEquals(StepState.Done, s.status.step)
        assertTrue(s.status.seals.all { it.look == SealLook.Sealed })
```

g) In `beforeTheLockTheLineIsOnlyTheCount` rename it to `beforeTheLockTheLineIsEmptyAndStepOneSeeks`, replace `assertEquals(VerdictLine.Count, s.verdict)` with `assertEquals(VerdictLine.None, s.verdict)`, replace `assertEquals(0, s.numeral)` with `assertEquals(StepState.Seeking, s.status.step)`.

h) In `lockingFillsTheRingAndShowsOnlyTheCount` replace `assertEquals(VerdictLine.Count, s.verdict)` with `assertEquals(VerdictLine.None, s.verdict)`.

i) Add:

```kotlin
    @Test
    fun theCurrentSealCountsItsPassedParts() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val r = RitualState.start(90f, long, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f, phrase = 1, chunk = 1, lit = 8)
        val seal = chantScreenState(r, long, 90f, 90f).status.seals[1]
        assertEquals(SealState(SealLook.Current, parts = 3, passed = 1), seal)
    }
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: FAIL to compile (`status`, `SealLook`, `StepState`, `VerdictLine.Listening` unknown).

- [ ] **Step 3: Update `MAIN/ui/screens/ChantScreenState.kt`**

a) Replace the `VerdictLine` enum and its KDoc with:

```kotlin
/** The one line under the words (ADR-0024): nothing before the lock, the voice meter, or a message that needs action. */
enum class VerdictLine { None, Listening, ChantAgain, SealCracked }

/** Step 1 of the status block (ADR-0024): finding the direction, holding it, lost after the lock, or done. */
enum class StepState { Seeking, Aligned, Drifting, Done }

/** How one phrase's seal is drawn (ADR-0024). */
enum class SealLook { Pending, Current, Sealed, Cracked }

/** One seal: its look, the phrase's chunk count, and how many of them have passed. */
data class SealState(val look: SealLook, val parts: Int, val passed: Int)

/** The two-row status block under the sigil (ADR-0024). [turn] is null when aligned or done. */
data class StatusBlock(val step: StepState, val turn: TurnDirection?, val seals: List<SealState>)
```

b) In `ChantScreenState`, delete the fields `numeral`, `clockSeconds` and `clockHeld` (with their KDoc) and add after `verdict`:

```kotlin
    /** Step 1 and the four seals (ADR-0024). */
    val status: StatusBlock,
```

c) In `chantScreenState(...)`, replace the `val line = when { … }` block with:

```kotlin
    val line = when {
        verdict?.kind == ChantVerdict.Kind.Rejected -> VerdictLine.ChantAgain
        verdict?.kind == ChantVerdict.Kind.Cracked -> VerdictLine.SealCracked
        ritual.phase == RitualPhase.Chanting || ritual.phase == RitualPhase.Wavering -> VerdictLine.Listening
        else -> VerdictLine.None
    }
```

and in the returned `ChantScreenState(...)` delete the `numeral = …`, `clockSeconds = …` and `clockHeld = …` arguments and add `status = statusBlock(ritual, azimuth, target, verdict),` after `verdict = line,`.

d) Add at the end of the file:

```kotlin
private fun statusBlock(ritual: RitualState, azimuth: Float?, target: Float, verdict: ChantVerdict?): StatusBlock {
    val done = ritual.phrase >= PHRASE_COUNT
    val step = when {
        done -> StepState.Done
        ritual.phase == RitualPhase.Wavering -> StepState.Drifting
        ritual.aligned -> StepState.Aligned
        else -> StepState.Seeking
    }
    val seals = (0 until PHRASE_COUNT).map { p ->
        val parts = ritual.chunks?.getOrNull(p)?.size ?: 1
        when {
            verdict?.kind == ChantVerdict.Kind.Cracked && p == verdict.phrase -> SealState(SealLook.Cracked, parts, 0)
            p < ritual.phrase -> SealState(SealLook.Sealed, parts, parts)
            p == ritual.phrase -> SealState(SealLook.Current, parts, ritual.chunk)
            else -> SealState(SealLook.Pending, parts, 0)
        }
    }
    val turn = if (step == StepState.Aligned || step == StepState.Done) null else turnDirection(azimuth, target)
    return StatusBlock(step, turn, seals)
}
```

e) Remove the now-unused import `kotlin.math.ceil`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: the test task fails to compile **only** because `ChantScreen.kt` still uses `numeral`, `clockSeconds`, `clockHeld` and the old `VerdictLine` values. That is fixed in Task 11; do not commit yet.

### Task 11: Status block composable and chant screen wiring

**Files:**
- Create: `MAIN/ui/screens/RitualStatusBlock.kt`
- Modify: `MAIN/ui/screens/ChantScreen.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Strings**

In `app/src/main/res/values/strings.xml`, delete:

```xml
    <string name="verdict_chant_aloud">Chant aloud</string>
    <string name="verdict_paused">Paused</string>
    <string name="verdict_sealed">Sealed</string>
    <string name="verdict_of">of</string>
```

and the two strings `chant_clock_paused` and `chant_clock_seconds`. Add, next to `chant_face`:

```xml
    <string name="chant_step_chant">Chant</string>
    <string name="chant_a11y_seals">%1$d of 4 lines sealed</string>
```

- [ ] **Step 2: Create `MAIN/ui/screens/RitualStatusBlock.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private val StepLabelSize = 15.sp
private val SealNumeralSize = 11.sp
private val SealSize = 30.dp
private val RowMinHeight = 40.dp
private val FrameWidth = 2.dp
private const val DIM_ALPHA = 0.45f
private const val BREATH_MIN = 0.55f
private const val BREATH_HALF_MS = 900
private const val BLINK_MIN = 0.55f
private const val BLINK_HALF_MS = 300
private const val FLARE_SCALE = 1.18f
private const val FLARE_MS = 450
private const val PART_GAP_DEG = 14f

/**
 * The Compass-and-Tally block under the sigil (ADR-0024): one frame, two rows. Both labels share
 * one muted style; only the step numbers, the arrow and the frame carry state.
 */
@Composable
internal fun RitualStatusBlock(status: StatusBlock, target: Float, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val frame = when (status.step) {
        StepState.Seeking -> colors.outlineVariant
        StepState.Aligned -> safe
        StepState.Drifting -> colors.error
        StepState.Done -> accent
    }
    val one = when (status.step) {
        StepState.Seeking -> colors.primary
        StepState.Drifting -> colors.error
        StepState.Aligned, StepState.Done -> safe
    }
    val two = when (status.step) {
        StepState.Aligned -> colors.primary
        StepState.Done -> safe
        StepState.Seeking, StepState.Drifting -> colors.outlineVariant
    }
    val label = MaterialTheme.textRoles.ritualCue.copy(fontSize = StepLabelSize, letterSpacing = 0.12.em)
    Column(
        modifier
            .fillMaxWidth()
            .border(FrameWidth, frame, MaterialTheme.shapes.medium)
            .padding(horizontal = MaterialTheme.spacing.md),
    ) {
        DirectionRow(status, target, one, label)
        HorizontalDivider(color = colors.outlineVariant)
        ChantRow(status, two, accent, label)
    }
}

@Composable
private fun DirectionRow(status: StatusBlock, target: Float, stateColor: Color, label: TextStyle) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (status.step == StepState.Drifting && !reduceMotion) {
        rememberInfiniteTransition(label = "step1").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "step1Blink",
        ).value
    } else {
        1f
    }
    val aligned = status.step == StepState.Aligned || status.step == StepState.Done
    val direction = headingLabel(target)
    val description = when {
        aligned -> stringResource(R.string.chant_a11y_facing, direction)
        status.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .graphicsLayer { alpha = blink }
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepLabel("1.", stringResource(R.string.chant_face, direction), stateColor, label, Modifier.weight(1f, fill = false))
        val icon = when {
            aligned -> "✓"
            status.turn == TurnDirection.Right -> "→"
            else -> "←"
        }
        Text(icon, style = label, color = stateColor)
    }
}

@Composable
private fun ChantRow(status: StatusBlock, stateColor: Color, accent: Color, label: TextStyle) {
    val sealed = status.seals.count { it.look == SealLook.Sealed }
    val description = stringResource(R.string.chant_a11y_seals, sealed)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepLabel("2.", stringResource(R.string.chant_step_chant), stateColor, label, Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            status.seals.forEachIndexed { i, seal ->
                Seal(ROMAN_NUMERALS[i], seal, accent, dimmed = status.step == StepState.Drifting, Modifier.size(SealSize))
            }
        }
    }
}

/** "1. FACE EAST": the number in [numberColor], the words always in the muted outline colour. */
@Composable
private fun StepLabel(number: String, text: String, numberColor: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = numberColor)) { append(number) }
            append(" ")
            append(text.upper())
        },
        style = style,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A numeral disc inside a ring split into one arc per chunk; passed arcs in the accent. */
@Composable
private fun Seal(numeral: String, seal: SealState, accent: Color, dimmed: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val current = seal.look == SealLook.Current
    val breath = if (current && !dimmed && !reduceMotion) {
        rememberInfiniteTransition(label = "seal").animateFloat(
            1f, BREATH_MIN, infiniteRepeatable(tween(BREATH_HALF_MS), RepeatMode.Reverse), label = "sealBreath",
        ).value
    } else {
        1f
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(seal.look) {
        if (seal.look == SealLook.Sealed && !reduceMotion) {
            pulse.snapTo(FLARE_SCALE)
            pulse.animateTo(1f, tween(FLARE_MS))
        }
    }
    val lost = seal.look == SealLook.Cracked || (dimmed && current)
    val ring = when {
        lost -> colors.error
        seal.look == SealLook.Pending -> colors.outlineVariant
        else -> accent
    }
    val numeralColor = when {
        seal.look == SealLook.Sealed -> colors.surfaceContainerLowest
        lost -> colors.error
        seal.look == SealLook.Pending -> colors.outline
        else -> accent
    }
    val track = colors.surfaceContainerHighest
    val alpha = if (dimmed && !current) DIM_ALPHA else breath
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = pulse.value
            scaleY = pulse.value
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.065f
            val outer = size.minDimension / 2f - stroke / 2f
            val disc = outer * 0.72f
            val each = 360f / seal.parts
            val gap = if (seal.parts > 1) PART_GAP_DEG else 0f
            repeat(seal.parts) { k ->
                drawArc(
                    color = if (k < seal.passed) accent else track,
                    startAngle = -90f + k * each + gap / 2f,
                    sweepAngle = each - gap,
                    useCenter = false,
                    topLeft = Offset(center.x - outer, center.y - outer),
                    size = Size(outer * 2f, outer * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            if (seal.look == SealLook.Sealed) drawCircle(accent, radius = disc)
            drawCircle(ring, radius = disc, style = Stroke(stroke * 0.8f))
        }
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = SealNumeralSize), color = numeralColor, maxLines = 1)
    }
}
```

- [ ] **Step 3: Wire it into `MAIN/ui/screens/ChantScreen.kt`**

a) In `Slab(...)`, delete the `val lineColor = when { … }` block. Replace

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor, Modifier.padding(top = spacing.sm))
```

with

```kotlin
                RitualStatusBlock(screen.status, state.demon.ritualHeading, accent, Modifier.padding(top = spacing.sm))
```

and

```kotlin
                DirectionCue(screen, state.demon.ritualHeading, lineColor)
```

with

```kotlin
                RitualStatusBlock(screen.status, state.demon.ritualHeading, accent, Modifier.weight(1f))
```

b) Delete the whole `DirectionCue` composable and its KDoc, and the constant `private val DirectionSize = 18.sp`.

c) In `ChantSigil(...)`, delete the block

```kotlin
        screen.clockSeconds?.let { seconds ->
            Text(
                if (screen.clockHeld) stringResource(R.string.chant_clock_paused).upper() else stringResource(R.string.chant_clock_seconds, seconds),
                style = MaterialTheme.textRoles.meta,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
```

d) Replace the whole `VerdictLine` composable with:

```kotlin
/** The voice meter while chanting, or the one message that needs action (ADR-0024). */
@Composable
private fun VerdictLine(screen: ChantScreenState, accent: Color, micLevel: Float) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.textRoles.ritualCue.copy(fontSize = VerdictSize)
    Row(
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        when (screen.verdict) {
            VerdictLine.None -> Unit
            VerdictLine.Listening -> VoiceMeter(
                if (screen.drifting) 0f else micLevel,
                if (screen.drifting) colors.outline else accent,
                Modifier.size(MeterWidth, MeterHeight),
            )
            VerdictLine.ChantAgain, VerdictLine.SealCracked -> {
                val label = if (screen.verdict == VerdictLine.ChantAgain) R.string.verdict_chant_again else R.string.verdict_seal_cracked
                Text(stringResource(label).upper(), style = style, color = colors.error, maxLines = 1)
                Text("✕", style = style, color = colors.error)
            }
        }
    }
}
```

e) Remove imports the compiler reports as unused (e.g. `buildAnnotatedString`, `withStyle`, `SpanStyle`, `em`, `ROMAN_NUMERALS`, `TurnDirection`, `rememberInfiniteTransition`) only if nothing else in the file uses them.

- [ ] **Step 4: Build and test**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: BUILD SUCCESSFUL; all tests pass; lint reports no errors.

- [ ] **Step 5: Commit (Tasks 10 and 11 together)**

```bash
git add -A app/src
git commit -q -m "feat(ui): Two-row status block with phrase seals; verdict line keeps only what needs action (ADR-0024)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 12: Intro card carries the rules

**Files:**
- Modify: `MAIN/ui/screens/ChantIntro.kt`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Strings**

Replace the intro rule strings with:

```xml
    <string name="intro_direction_title">1. Face</string>
    <string name="intro_direction_body">Turn until the frame turns green; the arrow shows which way. Turning away pauses the chant.</string>
    <string name="intro_chant_title">2. Chant</string>
    <string name="intro_chant_body">Read each line aloud. A finished line lights its seal. Long lines are checked in parts, shown on the ring around its seal.</string>
    <string name="intro_lines_title">Lines</string>
    <string name="intro_lines_body">Green under a word: heard. Red: say that part again.</string>
    <string name="intro_focus_title">Focus</string>
    <string name="intro_focus_body">The ring around the sigil is your focus. Long silences, turning away and parts you must repeat drain it. If it empties, your last sealed line cracks.</string>
```

and delete `intro_seal_title` and `intro_seal_body`.

- [ ] **Step 2: Rules and icons in `MAIN/ui/screens/ChantIntro.kt`**

a) Replace

```kotlin
        Rule(R.string.intro_chant_title, R.string.intro_chant_body) { VoiceMeter(0.8f, accent, Modifier.size(RuleIcon, RuleIcon * 0.7f)) }
        Rule(R.string.intro_lines_title, R.string.intro_lines_body) { LinesIcon() }
        Rule(R.string.intro_seal_title, R.string.intro_seal_body) {
            Text(
                ROMAN_NUMERALS.joinToString(" "),
                style = MaterialTheme.textRoles.name.copy(fontSize = NumeralSize),
                color = accent,
                maxLines = 1,
            )
        }
```

with

```kotlin
        Rule(R.string.intro_chant_title, R.string.intro_chant_body) { SealsIcon(accent) }
        Rule(R.string.intro_lines_title, R.string.intro_lines_body) { LinesIcon() }
        Rule(R.string.intro_focus_title, R.string.intro_focus_body) { FocusIcon(accent) }
```

b) Add at the end of the file:

```kotlin
/** Four small seals, the first lit and the second under way: "a finished line lights its seal". */
@Composable
private fun SealsIcon(accent: Color) {
    val dim = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(RuleIcon, RuleIcon * 0.7f)) {
        val r = size.width / 9f
        repeat(4) { k ->
            val c = Offset(r + k * (size.width - 2f * r) / 3f, size.height / 2f)
            when (k) {
                0 -> drawCircle(accent, r, c)
                1 -> drawCircle(accent, r, c, style = Stroke(r * 0.3f))
                else -> drawCircle(dim, r, c, style = Stroke(r * 0.3f))
            }
        }
    }
}

/** The focus ring, three quarters left. */
@Composable
private fun FocusIcon(accent: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.size(RuleIcon * 0.8f)) {
        val w = size.minDimension * 0.12f
        drawCircle(track, size.minDimension / 2f - w / 2f, style = Stroke(w))
        drawArc(
            accent, startAngle = -90f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w),
            style = Stroke(w, cap = StrokeCap.Round),
        )
    }
}
```

c) Add the import `androidx.compose.ui.geometry.Size`. Remove the constant `private val NumeralSize = 11.sp` and the import of `ROMAN_NUMERALS`, plus the `VoiceMeter` import if it is no longer used in this file.

- [ ] **Step 3: Build and test**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A app/src
git commit -q -m "feat(ui): The How-to-chant card explains steps, seals, lines and focus (ADR-0024)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## Phase 5 — Docs and release check

### Task 13: ADRs, PRODUCT.md and DESIGN.md

**Files:**
- Create: `docs/decisions/0022-chant-check-v2.md`, `docs/decisions/0023-focus-meter.md`, `docs/decisions/0024-status-column.md`
- Modify: `PRODUCT.md`, `DESIGN.md`, `docs/decisions/0018-chant-plausibility-check.md`, `docs/decisions/0010-focus-clock.md`, `docs/decisions/0019-chant-screen.md`, `docs/decisions/0021-intro-every-ritual-and-slab-buttons.md`

- [ ] **Step 1: Write `docs/decisions/0022-chant-check-v2.md`**

```markdown
# 0022 — Chant check v2: alignment and shuffle baseline

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0018 (the verdict and rhythm parts; recording, privacy and alternatives stand).

## Context
The ADR-0018 check judged one median formant per lit syllable. Lighting follows loudness
onsets, which voiced consonants (M, N, L) suppress, so on long phrases the nuclei drifted
away from the speech. On the tester clips honest chanting (vowel score 0.50–0.52) and
nonsense (0.40–0.53) were barely apart, and careful readers lost points for breathing.

## Decision
- **Template:** one slot per syllable vowel (`ChantTemplate`); a glide (I or U before
  another vowel in the word) is optional; the unstressed last syllable of a longer word
  also accepts a neutral "uh".
- **Alignment:** every voiced frame's F1/F2 is aligned with the slots by dynamic
  programming (`ChantAligner`): a frame joins a slot or is skipped; a slot needs ≥ 3 frames
  to be heard. Best of the six vocal-tract scales. Lighting drives only the karaoke.
- **Relative verdict:** the same frames are aligned with 24 seeded shuffles of whole slots;
  a chunk passes when its cost is ≤ the ceiling, it beats ≥ 70 % of the shuffles (ties
  count half), and its median heard slot is ≥ 70 ms (talk veto). With fewer than 8 valid
  shuffles only the ceiling applies.
- **Chunks:** lines over 8 syllables are judged in word groups (`ChantChunks`); a
  rejection reopens only its chunk.
- **Rhythm** is no longer scored; pauses never count.

## Tuning (from Task 8 of the plan)
Fill in after calibration: the final `ALIGN_*`, `BASELINE_*`, `TALK_MAX_SLOT_MS` values,
the honest reject rate per voice and the nonsense pass rate, with the clip counts.

## Consequences
"No speech or words are recognised" stays true. Held vowels and reversed chants no
longer pass. Plan: `docs/superpowers/plans/2026-10-18-chant-check-v2.md`.
```

- [ ] **Step 2: Write `docs/decisions/0023-focus-meter.md`**

```markdown
# 0023 — Focus meter

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0010 and ADR-0019 (budget, drain, seconds label).

## Context
The focus clock drained while the player chanted, so careful readers of long lines
cracked seals without doing anything wrong.

## Decision
Phrases II–IV have a flat 8 s meter of lost focus (+25 % in court), full at the start of
each line and refilled on each seal. Chanting and pauses under 2 s cost nothing; silence
beyond 2 s drains 1×; wavering drains 3×; a rejected chunk costs 25 % at once. Empty →
the last seal cracks, as before. The verdict hold of ADR-0019 stands. The ring shows the
meter; the seconds and "PAUSED" labels are gone.

## Consequences
Reading speed no longer matters; attention and accuracy do. Repeated nonsense still
cracks after about four rejected chunks.
```

- [ ] **Step 3: Write `docs/decisions/0024-status-column.md`**

```markdown
# 0024 — Status column and rules on the intro card

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0019 (direction line, verdict line, "I of IV") and ADR-0021 (intro card rules).

## Context
Progress was split between the sigil's ink and an "I of IV" count in the verdict line;
the direction line and the progress did not read as one task. Prototype iterations v10–v12.1
(https://claude.ai/artifact/9rCV9zSJdqY5mJscEn5EiQ) and a Stitch exploration
(`docs/stitch-prompts/2026-10-18-ritual-status-block.md`, option "Compass-and-Tally")
settled the layout.

## Decision
- Under the sigil, one framed block with two rows: "1. FACE EAST" with → / ← / ✓, and
  "2. CHANT" with four round seals whose rings show the line's parts.
- Both labels: Inter Bold, upper case, letter-spacing 0.12 em, `outline` colour, one line.
  Only the numbers, the arrow and the frame change colour: yellow while turning or
  chanting, green when aligned or done, red when drifting after the lock (the frame then
  red, row 1 blinking); the frame takes the demon accent when all four are sealed.
- The verdict line keeps the voice meter and only "CHANT AGAIN ✕" and "SEAL CRACKED ✕".
- The intro card carries the rules: 1. Face, 2. Chant, Lines, Focus.

## Consequences
The left panel is the single status display. Less text on the chant screen; the intro
card must be read once per ritual (ADR-0021 already shows it every time).
```

- [ ] **Step 4: Amend the older ADRs' status lines**

Append to the `Status:` line of each:
- `0018-chant-plausibility-check.md`: ` Amended by ADR-0022 (alignment, chunks, no rhythm score).`
- `0010-focus-clock.md`: ` Replaced by the focus meter of ADR-0023.`
- `0019-chant-screen.md`: ` Amended by ADR-0023 (no seconds label) and ADR-0024 (status column, reduced verdict line).`
- `0021-intro-every-ritual-and-slab-buttons.md`: ` Amended by ADR-0024 (the card's four rules).`

- [ ] **Step 5: Update `PRODUCT.md`**

a) In the **Chant screen** row, replace `and one line: "FACE EAST ✓" (green) or "← FACE EAST" (red, blinking, arrow = way to turn).` with:

`and a framed two-row status block (ADR-0024): "1. FACE EAST" with → / ← while turning and ✓ when facing him, and "2. CHANT" with four seals I–IV whose rings show the line's parts; both labels in one muted style, with only the numbers, the arrow and the frame showing state (yellow turning or chanting, green aligned or done, red drifting).`

and replace `and one line: "CHANT ALOUD · I of IV" with a voice meter, "PAUSED", "SEALED ✓", "CHANT AGAIN ✕" or "SEAL CRACKED ✕".` with `and one line: the voice meter, or "CHANT AGAIN ✕" / "SEAL CRACKED ✕".` and `four generic rules` with `four rules (1. Face, 2. Chant, Lines, Focus)`.

b) Replace the **Chant** row's text from `All syllables lit → the phrase is **judged**` to the end of the row with:

`Lines over 8 syllables are judged in parts of whole words (ADR-0022). When a part's last syllable is lit and its vowel ends (voicing stops, or after 300 ms), it is **judged**: every voiced moment is aligned with the vowels the spelling predicts (glides may be skipped, word endings may sound as "uh") and must fit the real order clearly better than shuffled orders, at a chanting pace. A passed part turns its words green and lights an arc of the line's seal; the last part **seals** the line (chime + haptic). Otherwise the line reads "CHANT AGAIN ✕", the doubtful words of that part turn red, the phone buzzes, the sigil strokes flash red and only that part reopens; a rejection costs 25 % focus. Words are marked heard or doubtful as a hint only (ADR-0019). |`

c) Replace the whole **Focus clock** row with:

`| Focus meter | Lines II–IV each have a focus meter of 8 s of lost focus (+25 % in court; ADR-0023), shown by the sigil's ring with no numbers. Chanting and pauses under 2 s cost nothing; silence beyond 2 s drains it, wavering drains it 3× as fast, and a rejected part costs 25 % at once. After a verdict it waits up to 3 s, or until the next syllable. When it empties the last seal **cracks**: that line reopens with a full meter (line I has none). Failure never resets the whole ritual (ADR-0010). |`

- [ ] **Step 6: Update `DESIGN.md`**

a) In the text-roles table row for `ritualCue`, replace `18 sp (direction) or 22 sp (verdict)` with `15 sp with 0.12 em letter-spacing for the status-block step labels, 22 sp for the verdict line`.

b) In the components table, in the `ChantScreen` row replace `name, \`ChantSigil\` and the direction line;` with `name, \`ChantSigil\` and \`RitualStatusBlock\`;` and replace `verdict line (\`ritualCue\`) with \`VoiceMeter\` (only the count before the lock).` with `verdict line (\`ritualCue\`): \`VoiceMeter\` while chanting, or "CHANT AGAIN ✕" / "SEAL CRACKED ✕" in error.`

c) Add a row after it:

`| \`RitualStatusBlock\` | Status column under the sigil (ADR-0024) | \`status\`, \`target\`, \`accent\`; 2 dp frame (\`medium\` shape) in outline-variant / ritual-safe / error / accent by step; row 1 "1. FACE {DIR}" + → ← ✓, row 2 "2. CHANT" + four 30 dp seals (numeral disc, ring split per chunk, passed arcs in accent; current breathes, sealed filled with a flare, cracked or lost in error, dimmed while drifting); labels \`ritualCue\` 15 sp, 0.12 em, \`outline\`, numbers and arrow in primary / ritual-safe / error. |`

- [ ] **Step 7: Check the privacy policy and store listing**

Run: `grep -n -i "vowel\|rhythm\|timing\|recogni" docs/PRIVACY_POLICY.md docs/STORE_LISTING.md`
Expected: they say that vowel sounds and timing are compared with the spelling and nothing is recognised or stored. That remains true (ADR-0022 measures the same things); no edit needed. If any sentence mentions "rhythm score" or pauses being counted, rewrite it to "vowel sounds and pacing".

- [ ] **Step 8: Commit**

```bash
git add docs PRODUCT.md DESIGN.md
git commit -q -m "docs: Record chant check v2, the focus meter and the status column (ADR-0022–0024)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 14: Final verification and device check

- [ ] **Step 1: Full gate**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Clip gate**

Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`
Expected: `enough data? yes` and the test passes (both voices ≤ 2 % honest chunk rejects). Copy the final numbers into ADR-0022's "Tuning" section and commit.

- [ ] **Step 3: Device check (both voices, phone in landscape)**

Install with `./gradlew installDebug` and confirm:
- the status block shows "1. FACE …" yellow while turning, green ✓ when facing, red and blinking when turning away after the lock;
- "2. CHANT": the current seal breathes; on Asmodeus II and III its ring fills in three arcs; a sealed line fills its seal with a flare;
- reading Asmodeus III slowly, with pauses between words, never drains the ring;
- a deliberately wrong part shows "CHANT AGAIN ✕", reopens only that part and takes a quarter of the ring;
- the intro card shows the four rules and every label fits on one line.

- [ ] **Step 4: Report**

Summarise the gate numbers and the device check results in the PR description or hand-off note.
