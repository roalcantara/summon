# Chant Check (Vowels + Rhythm) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the ritual from accepting any speech. A phrase seals only when the vowel of each syllable and the pacing plausibly match its Latin text; clearly unrelated talk ("Olá meu amigo, como está hoje?"), mumbling or one held vowel is rejected with a comedic scoff. No word recognition, no model, no network (ADR-0018).

**Architecture:** Pure Kotlin, JVM-unit-tested core in `ritual/`: `FormantEstimator` (LPC on the mic window → F1/F2), `VoiceFrame.formants`, `ChantJudge` (expected vowels from the spelling, tolerant alignment, rhythm), and a `ChantTake` the `RitualEngine` accumulates per phrase. The engine judges a phrase when its last vowel ends and emits `Judged` then `Sealed` or `Rejected`. The check is opt-in on `RitualState` (`judged = true`), so existing engine tests keep their meaning; `RitualViewModel` always turns it on. A debug-only clip recorder (debug source set; release gets a no-op) plus a JVM replay harness calibrate the thresholds from tester recordings.

**Tech Stack:** unchanged: AGP 9.4.1 (Kotlin 2.2.10), Compose BOM 2026.09.00, JUnit 4, kotlinx-coroutines-test. **No new dependency.** Decision record: `docs/decisions/0018-chant-plausibility-check.md` (read it first: it explains every number in `RitualConfig` below).

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
- `DEBUG` = `app/src/debug/java/com/summondemons/safely` (new source set)
- `RELEASE` = `app/src/release/java/com/summondemons/safely` (new source set)
- `TESTDEBUG` = `app/src/testDebug/java/com/summondemons/safely` (new source set)

**Verification gate** (run at the end of every phase, and wherever a task says "gate"):

```bash
./gradlew testDebugUnitTest lint assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Lint warnings are allowed; lint errors are not.

**Notes for the implementer**
- **What was verified before this plan was written.** Every file shown whole in Phase 2
  (and `ClipRecorder.kt`, `WavClipRecorder.kt`, `ChantReplay.kt`, `ChantClipReplayTest.kt`,
  `WavClipRecorderTest.kt`) was compiled with Kotlin 2.2.10 and its tests run on the JVM
  together with the unchanged `RitualEngineTest` and the extended `VoiceAnalyzerTest`:
  74 tests green, no compiler warnings. Copy them exactly. Every "replace … with …" edit
  was checked to match the current file exactly once. The Android/Compose edits
  (Tasks 7–9) were **not** compiled (no Android SDK at hand): if a Compose API signature
  differs, keep the behaviour, adapt the call and say so in the commit message.
- Every step that shows a whole file means "create or replace the file with exactly
  this". An edit step quotes the exact current text to replace; if it doesn't match
  exactly once, stop and re-read the file instead of guessing.
- UI reads colors, type, shapes and spacing from the theme only (DESIGN.md). Ritual
  tuning lives in `RitualConfig`, never in UI code (ADR-0006).
- Do not change any number in `RitualConfig` except in Task 11 (calibration).
- `./gradlew testDebugUnitTest` is the test command throughout. The new
  `app/src/testDebug` test sees the debug source set; `testReleaseUnitTest` doesn't
  compile it, which is intended.

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `PRODUCT.md`, `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md`, `AGENTS.md`, `docs/decisions/0008-ritual-2-rules.md` | Modify | Canon for the chant check (ADR-0018 already exists) |
| `MAIN/ritual/RitualConfig.kt` | Replace | Formant and chant-check tuning |
| `MAIN/ritual/Formants.kt` | Create | `Formants`, `SyllableFormants`, `FormantEstimator` (LPC) |
| `MAIN/ritual/VoiceAnalyzer.kt` | Replace | `VoiceFrame.formants` on voiced frames |
| `MAIN/ritual/ChantJudge.kt` | Create | `Vowel`, `ChantScore`, `ChantTake`, `ChantJudge` |
| `MAIN/ritual/RitualState.kt`, `RitualEngine.kt` | Replace | `vowels`, `take`, judging, `Judged` / `Rejected` effects |
| `MAIN/ritual/ClipRecorder.kt` | Create | `ClipRecorder` interface + `None`, `ClipLabel` |
| `MAIN/ritual/RitualViewModel.kt` | Modify | Check on, Hold to Chant vowels, score/rejections, clip label |
| `MAIN/platform/RitualFeedback.kt` | Modify | Warning haptic on `Rejected` |
| `MAIN/ui/screens/RitualCue.kt`, `RitualScreen.kt` | Modify | `Rejected` cue row, scoff lines, red sigil flash |
| `MAIN/ui/components/DebugControls.kt` | Modify | Score readout, "Record clip" chip |
| `MAIN/navigation/AppNavHost.kt` | Modify | Pass the clip recorder |
| `app/src/main/res/values/strings.xml` | Modify | Scoff lines, debug strings |
| `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Modify | Exclude `chant-clips/` |
| `DEBUG/platform/WavClipRecorder.kt`, `DEBUG/platform/ClipRecorders.kt` | Create | WAV clip writer; factory |
| `RELEASE/platform/ClipRecorders.kt` | Create | Factory returning `ClipRecorder.None` |
| `.gitignore` | Modify | Ignore `app/src/test/chant-clips/` |
| `TEST/ritual/SyntheticVoice.kt`, `ChantReplay.kt` | Create | Test helpers: synthetic voice, replay + WAV reader |
| `TEST/ritual/FormantEstimatorTest.kt`, `ChantJudgeTest.kt`, `ChantCheckTest.kt`, `SyntheticChantTest.kt`, `ChantClipReplayTest.kt` | Create | Unit tests and calibration harness |
| `TEST/ritual/VoiceAnalyzerTest.kt`, `RitualViewModelTest.kt`, `TEST/ui/screens/RitualCueTest.kt` | Modify | New cases |
| `TESTDEBUG/platform/WavClipRecorderTest.kt` | Create | WAV round trip |

---

## Phase 1 — Canon

### Task 1: PRODUCT.md, privacy texts, ADR-0008, AGENTS.md

ADR-0018 is already written and is the rationale for everything below. These edits make
the canon describe the new behaviour. The privacy policy and store listing must match the
**shipped** app: if version 1.0.0 is released before this plan lands, these texts still
change here, in the same release as the code (Task 12 republishes them).

**Files:**
- Modify: `PRODUCT.md`, `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md`, `docs/decisions/0008-ritual-2-rules.md`, `AGENTS.md`

- [ ] **Step 1: Check ADR-0018 is present**

```bash
ls docs/decisions/0018-chant-plausibility-check.md && grep -c "Starting values" docs/decisions/0018-chant-plausibility-check.md
```
Expected: the path, then `1`.

- [ ] **Step 2: Edit `PRODUCT.md`**

(a) Replace:

```markdown
All syllables lit → the phrase is **sealed** (chime + haptic). |
```

with:

```markdown
All syllables lit → the phrase is **judged** once the last vowel ends (voicing stops, or after 300 ms): the vowel of each syllable and the pacing must plausibly match the text (ADR-0018). A match **seals** the phrase (chime + haptic); otherwise the demon scoffs (rotating cue line, warning haptic, red flash) and the phrase reopens with nothing lit. A rejection is not a crack and costs only time. |
```

(b) Replace:

```markdown
| Debug aids | Ritual: "Simulate heading" (snaps to the demon's `ritualHeading`) and "Hold to Chant" (synthetic voiced syllables).
```

with:

```markdown
| Debug aids | Ritual: "Simulate heading" (snaps to the demon's `ritualHeading`), "Hold to Chant" (synthetic voiced syllables sounding the expected vowels), the last chant-check score, and "Record clip" (Off / chant / nonsense: saves raw mic audio as WAV for calibration, ADR-0018).
```

(c) Replace:

```markdown
audio is analysed in memory and never stored or sent (ADR-0017, docs/PRIVACY_POLICY.md). |
```

with:

```markdown
audio is analysed in memory and never stored or sent (ADR-0017, docs/PRIVACY_POLICY.md). Debug builds can save tester clips on the device (ADR-0018); release builds contain no recording code. |
```

- [ ] **Step 3: Edit `docs/PRIVACY_POLICY.md`**

Replace:

```markdown
- Audio is processed in memory, about 20 milliseconds at a time, to measure loudness,
  voice-like pitch and syllable timing. Each piece of audio is thrown away as soon as it
  has been measured.
- **No audio is ever recorded, saved to storage, or transmitted.** No speech or words are
  recognised. The app cannot tell what you said, only that you said something.
```

with:

```markdown
- Audio is processed in memory, about 20 milliseconds at a time, to measure loudness,
  voice-like pitch, syllable timing and vowel sounds. Each piece of audio is thrown away as
  soon as it has been measured.
- The vowel sounds are compared with the vowels the incantation's spelling predicts, to
  tell chanting apart from unrelated talk. This happens on your device and its result is
  never stored.
- **No audio is ever recorded, saved to storage, or transmitted.** No speech or words are
  recognised: the app cannot tell what you said, only whether it sounded like the chant.
```

- [ ] **Step 4: Edit `docs/STORE_LISTING.md`**

Replace:

```markdown
No audio is recorded, saved or sent anywhere, and no words are recognised.
```

with:

```markdown
No audio is recorded, saved or sent anywhere, and no words are recognised: the app only checks that your vowels and rhythm sound like the chant.
```

- [ ] **Step 5: Edit `docs/decisions/0008-ritual-2-rules.md`**

Replace:

```markdown
- Status: Accepted (supersedes ADR-0007); the grace period and "silence pauses" are superseded by ADR-0010
```

with:

```markdown
- Status: Accepted (supersedes ADR-0007); the grace period and "silence pauses" are superseded by ADR-0010; sealing on the last syllable is amended by ADR-0018 (the phrase is judged first)
```

- [ ] **Step 6: Edit `AGENTS.md`**

(a) Replace:

```markdown
- Release prep: ADR-0017.
```

with:

```markdown
- Release prep: ADR-0017. Chant plausibility check (vowels + rhythm): ADR-0018, plan
  `docs/superpowers/plans/2026-10-18-chant-check.md`.
```

(b) Replace:

```markdown
  thresholds (ADR-0008); the chant plausibility check and its thresholds (ADR-0018);
```

with:

```markdown
  thresholds (ADR-0008); calibrating the chant check's thresholds from tester clips (ADR-0018);
```

- [ ] **Step 7: Commit**

```bash
git add PRODUCT.md docs/PRIVACY_POLICY.md docs/STORE_LISTING.md docs/decisions/0008-ritual-2-rules.md AGENTS.md
git commit -q -m "docs(product): Chant check rules, privacy and store texts (ADR-0018)" -m "$TRAILER"
```

---

## Phase 2 — Pure core (TDD)

### Task 2: Formant estimator

LPC formant estimation on the 40 ms window the analyzer already builds. The test voice is
a glottal pulse train through formant resonators, so the expected F1/F2 are known.

**Files:**
- Replace: `MAIN/ritual/RitualConfig.kt`
- Create: `MAIN/ritual/Formants.kt`, `TEST/ritual/SyntheticVoice.kt`, `TEST/ritual/FormantEstimatorTest.kt`

- [ ] **Step 1: Add the test voice**

`app/src/test/java/com/summondemons/safely/ritual/SyntheticVoice.kt`:

```kotlin
package com.summondemons.safely.ritual

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Test-only source-filter voice: a glottal pulse train at [f0] through cascade resonators
 * at F1–F4 (ADR-0018). Deterministic for a given [seed].
 */
class SyntheticVoice(
    private val rate: Int = 16_000,
    private val f0: Double = 120.0,
    /** Vocal-tract scale: 1.0 adult male, ~1.2 adult female, ~1.4 child. */
    private val scale: Double = 1.0,
    seed: Int = 7,
) {
    private val random = Random(seed)

    /** [ms] of [vowel] (or silence when null) as PCM16, peak ≈ [amplitude], with 20 ms ramps. */
    fun vowel(vowel: Vowel?, ms: Int, amplitude: Double = 8_000.0): ShortArray {
        val n = rate * ms / 1_000
        if (vowel == null) return ShortArray(n)
        val f = vowel.formants(scale)
        return render(n, listOf(f.f1, f.f2, 2_600.0 * scale, 3_400.0 * scale), amplitude)
    }

    /** [ms] of a vowel with explicit formants. */
    fun formants(f1: Double, f2: Double, ms: Int, amplitude: Double = 8_000.0): ShortArray =
        render(rate * ms / 1_000, listOf(f1, f2, 2_600.0 * scale, 3_400.0 * scale), amplitude)

    /** Syllables: [gapMs] of silence, then [vowelMs] of each vowel. */
    fun syllables(vowels: List<Vowel?>, vowelMs: Int = 220, gapMs: Int = 60): ShortArray =
        vowels.map { vowel(null, gapMs) + vowel(it, vowelMs) }.fold(ShortArray(0)) { acc, s -> acc + s }

    private fun render(n: Int, formants: List<Double>, amplitude: Double): ShortArray {
        // Glottal source: pulses every period (±1 % jitter), smoothed by a one-pole low-pass (spectral tilt).
        val source = DoubleArray(n)
        var next = 0.0
        var tilt = 0.0
        for (i in 0 until n) {
            var pulse = 0.0
            if (i >= next) {
                pulse = 1.0
                next += rate / f0 * (1 + (random.nextDouble() - 0.5) * 0.02)
            }
            tilt = 0.9 * tilt + pulse
            source[i] = tilt
        }
        var signal = source
        formants.forEachIndexed { k, freq ->
            if (freq < rate / 2.0 - 200) signal = resonate(signal, freq, BANDWIDTHS[k])
        }
        val peak = signal.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val ramp = rate / 50
        return ShortArray(n) { i ->
            val envelope = minOf(1.0, i / ramp.toDouble(), (n - 1 - i) / ramp.toDouble())
            val noise = (random.nextDouble() - 0.5) * 0.01
            ((signal[i] / peak + noise) * amplitude * envelope).roundToInt().coerceIn(-32_768, 32_767).toShort()
        }
    }

    private fun resonate(x: DoubleArray, freq: Double, bandwidth: Double): DoubleArray {
        val c = -exp(-2 * PI * bandwidth / rate)
        val b = 2 * exp(-PI * bandwidth / rate) * cos(2 * PI * freq / rate)
        val a = 1 - b - c
        val y = DoubleArray(x.size)
        for (i in x.indices) {
            y[i] = a * x[i] + b * (if (i > 0) y[i - 1] else 0.0) + c * (if (i > 1) y[i - 2] else 0.0)
        }
        return y
    }

    private companion object {
        val BANDWIDTHS = listOf(80.0, 100.0, 140.0, 180.0)
    }
}

/** Splits PCM into 20 ms frames, like `AndroidPcmSource`. */
fun ShortArray.frames(rate: Int): List<ShortArray> {
    val size = (rate * RitualConfig.FRAME_MS / 1_000).toInt()
    return (0 until this.size / size).map { copyOfRange(it * size, (it + 1) * size) }
}
```

It uses `Vowel`, which Step 6 adds (the rest of `ChantJudge.kt` follows in Task 4).

- [ ] **Step 2: Write the failing test**

`app/src/test/java/com/summondemons/safely/ritual/FormantEstimatorTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormantEstimatorTest {

    /** Formants of a 40 ms window taken from the middle of a 300 ms synthetic vowel. */
    private fun measure(voice: SyntheticVoice, vowel: Vowel, rate: Int = 16_000): Formants? {
        val pcm = voice.vowel(vowel, 300)
        val window = rate * 40 / 1_000
        val start = pcm.size / 2 - window / 2
        return FormantEstimator(rate).estimate(pcm.copyOfRange(start, start + window))
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double, label: String) =
        assertEquals(label, expected, actual, expected * tolerance)

    @Test
    fun findsF1AndF2OfEveryVowelForAMaleVoice() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(f0 = 110.0), v)!!
            assertNear(v.f1, f.f1, 0.15, "$v F1")
            assertNear(v.f2, f.f2, 0.15, "$v F2")
        }
    }

    @Test
    fun findsScaledFormantsForAFemaleVoice() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(f0 = 210.0, scale = 1.2), v)!!
            assertNear(v.f1 * 1.2, f.f1, 0.2, "$v F1")
            assertNear(v.f2 * 1.2, f.f2, 0.2, "$v F2")
        }
    }

    @Test
    fun worksAt44kHz() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(rate = 44_100, f0 = 120.0), v, 44_100)!!
            assertNear(v.f1, f.f1, 0.2, "$v F1")
            assertNear(v.f2, f.f2, 0.2, "$v F2")
        }
    }

    @Test
    fun silenceHasNoFormants() {
        assertNull(FormantEstimator(16_000).estimate(ShortArray(640)))
    }
}
```

- [ ] **Step 3: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*FormantEstimatorTest*'`
Expected: compilation fails (`FormantEstimator`, `Formants`, `Vowel` unresolved).

- [ ] **Step 4: Replace `RitualConfig`**

`app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt` (adds the
"Formants" and "Chant plausibility check" blocks; everything else is unchanged):

```kotlin
package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0008, ADR-0010, ADR-0018). Per-demon parameters live in `Demon` (ADR-0006). */
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

    // Focus clock (ADR-0010) and the Court Hours bonus (ADR-0012)
    const val FOCUS_BASE_MS = 3_000L
    const val FOCUS_PER_SYLLABLE_MS = 500L
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

    // Mic level for glow only (never progress)
    const val MIC_LEVEL_CEILING = 8_000.0
    const val MIC_LEVEL_ATTACK_MS = 40f
    const val MIC_LEVEL_RELEASE_MS = 250f

    // Debug "Hold to Chant"
    const val HOLD_TO_CHANT_RMS = 4_000.0
    const val HOLD_TO_CHANT_ONSET_FRAMES = 12
}
```

- [ ] **Step 5: Implement the estimator**

`app/src/main/java/com/summondemons/safely/ritual/Formants.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FORMANT_F1_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_F1_MIN_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_F2_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_MAX_BANDWIDTH_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_RATE
import com.summondemons.safely.ritual.RitualConfig.LPC_ORDER
import com.summondemons.safely.ritual.RitualConfig.PRE_EMPHASIS
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

/** The first two formants of a voiced sound, in Hz (ADR-0018). */
data class Formants(val f1: Double, val f2: Double)

/**
 * The formants measured on the frames of one syllable. Immutable: [plus] returns a new list.
 * The median resists the odd frame where the estimator locks onto a pitch harmonic.
 */
data class SyllableFormants(val frames: List<Formants> = emptyList()) {
    operator fun plus(f: Formants?): SyllableFormants = if (f == null) this else SyllableFormants(frames + f)

    /** Null when no frame of the syllable had measurable formants. */
    fun median(): Formants? {
        if (frames.isEmpty()) return null
        return Formants(median(frames.map { it.f1 }), median(frames.map { it.f2 }))
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}

/**
 * Estimates F1 and F2 from a PCM16 window by linear prediction (ADR-0018). Pure, no Android
 * types; one instance per sample rate. The window is low-pass filtered and decimated to
 * about [FORMANT_RATE], pre-emphasised, Hamming-windowed, fitted with an order-[LPC_ORDER]
 * all-pole model (autocorrelation + Levinson-Durbin), and the model's roots give the
 * resonances. Returns null when no plausible F1/F2 pair is found.
 */
class FormantEstimator(val sampleRate: Int) {
    private val decimation = maxOf(1, sampleRate / FORMANT_RATE)
    private val rate = sampleRate.toDouble() / decimation
    private val lowPass = if (decimation > 1) lowPassTaps(decimation) else doubleArrayOf(1.0)

    fun estimate(samples: ShortArray, count: Int = samples.size): Formants? {
        val x = emphasise(decimate(samples, count.coerceAtMost(samples.size)))
        if (x.size <= LPC_ORDER * 2) return null
        val a = lpc(x) ?: return null
        val resonances = roots(a)
            .filter { it.second < FORMANT_MAX_BANDWIDTH_HZ }
            .map { it.first }
            .sorted()
        val f1 = resonances.firstOrNull { it in FORMANT_F1_MIN_HZ..FORMANT_F1_MAX_HZ } ?: return null
        val f2 = resonances.firstOrNull { it > f1 && it <= FORMANT_F2_MAX_HZ } ?: return null
        return Formants(f1, f2)
    }

    private fun decimate(samples: ShortArray, n: Int): DoubleArray {
        if (decimation == 1) return DoubleArray(n) { samples[it].toDouble() }
        val half = lowPass.size / 2
        return DoubleArray(n / decimation) { k ->
            val center = k * decimation
            var acc = 0.0
            for (t in lowPass.indices) {
                val i = center + t - half
                if (i in 0 until n) acc += lowPass[t] * samples[i]
            }
            acc
        }
    }

    /** Pre-emphasis then a Hamming window. */
    private fun emphasise(x: DoubleArray): DoubleArray {
        val n = x.size
        return DoubleArray(n) { i ->
            val e = x[i] - if (i > 0) PRE_EMPHASIS * x[i - 1] else 0.0
            e * (0.54 - 0.46 * cos(2 * PI * i / (n - 1)))
        }
    }

    /** Prediction polynomial [1, a1..ap] by Levinson-Durbin; null for silence or an unstable fit. */
    private fun lpc(x: DoubleArray): DoubleArray? {
        val p = LPC_ORDER
        val r = DoubleArray(p + 1) { lag ->
            var s = 0.0
            for (i in 0 until x.size - lag) s += x[i] * x[i + lag]
            s
        }
        if (r[0] <= 0.0) return null
        r[0] *= 1.0 + 1e-9 // white-noise floor keeps the recursion stable
        val a = DoubleArray(p + 1).also { it[0] = 1.0 }
        var error = r[0]
        for (i in 1..p) {
            var acc = r[i]
            for (j in 1 until i) acc += a[j] * r[i - j]
            val k = -acc / error
            val previous = a.copyOf()
            for (j in 1 until i) a[j] = previous[j] + k * previous[i - j]
            a[i] = k
            error *= 1 - k * k
            if (error <= 0.0) return null
        }
        return a
    }

    /** (frequency, bandwidth) in Hz of each upper-half-plane root of the polynomial (Durand-Kerner). */
    private fun roots(a: DoubleArray): List<Pair<Double, Double>> {
        val p = a.size - 1
        val re = DoubleArray(p) { k -> ROOT_SEED_RADIUS * cos(ROOT_SEED_ANGLE * k + 0.25) }
        val im = DoubleArray(p) { k -> ROOT_SEED_RADIUS * sin(ROOT_SEED_ANGLE * k + 0.25) }
        for (iteration in 0 until ROOT_ITERATIONS) {
            var moved = 0.0
            for (i in 0 until p) {
                // Numerator: polynomial value at root i (Horner).
                var nr = 1.0
                var ni = 0.0
                for (c in 1..p) {
                    val tr = nr * re[i] - ni * im[i] + a[c]
                    ni = nr * im[i] + ni * re[i]
                    nr = tr
                }
                // Denominator: product of (z_i - z_j).
                var dr = 1.0
                var di = 0.0
                for (j in 0 until p) {
                    if (j == i) continue
                    val xr = re[i] - re[j]
                    val xi = im[i] - im[j]
                    val tr = dr * xr - di * xi
                    di = dr * xi + di * xr
                    dr = tr
                }
                val mag = dr * dr + di * di
                if (mag == 0.0) continue
                val qr = (nr * dr + ni * di) / mag
                val qi = (ni * dr - nr * di) / mag
                re[i] -= qr
                im[i] -= qi
                moved = maxOf(moved, hypot(qr, qi))
            }
            if (moved < ROOT_TOLERANCE) break
        }
        return (0 until p).filter { im[it] > 0.0 }.map { i ->
            val radius = hypot(re[i], im[i])
            val frequency = atan2(im[i], re[i]) * rate / (2 * PI)
            val bandwidth = -ln(radius) * rate / PI
            frequency to bandwidth
        }
    }

    private companion object {
        const val ROOT_ITERATIONS = 200
        const val ROOT_TOLERANCE = 1e-10
        const val ROOT_SEED_RADIUS = 0.9
        const val ROOT_SEED_ANGLE = 2 * PI / 10.3

        /** Windowed-sinc low-pass at 0.45 × the decimated rate. */
        fun lowPassTaps(decimation: Int): DoubleArray {
            val n = 12 * decimation + 1
            val cutoff = 0.45 / decimation // cycles per input sample
            val m = n / 2
            val taps = DoubleArray(n) { i ->
                val t = (i - m).toDouble()
                val sinc = if (t == 0.0) 2 * cutoff else sin(2 * PI * cutoff * t) / (PI * t)
                sinc * (0.54 - 0.46 * cos(2 * PI * i / (n - 1)))
            }
            val sum = taps.sum()
            return DoubleArray(n) { taps[it] / sum }
        }
    }
}
```

- [ ] **Step 6: Add `Vowel` now (the rest of `ChantJudge.kt` comes in Task 4)**

`app/src/main/java/com/summondemons/safely/ritual/ChantJudge.kt`:

```kotlin
package com.summondemons.safely.ritual

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
```

- [ ] **Step 7: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*FormantEstimatorTest*'`
Expected: 4 tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "feat(ritual): LPC formant estimator and test voice (ADR-0018)" -m "$TRAILER"
```

### Task 3: Formants on voiced frames

**Files:**
- Replace: `MAIN/ritual/VoiceAnalyzer.kt`
- Modify: `TEST/ritual/VoiceAnalyzerTest.kt`

- [ ] **Step 1: Write the failing test**

Append this test inside `VoiceAnalyzerTest` (before the class's closing `}`):

```kotlin
    @Test
    fun voicedFramesCarryFormantsAndSilenceDoesNot() {
        val pcm = SyntheticVoice(f0 = 120.0).vowel(null, 100) + SyntheticVoice(f0 = 120.0).vowel(Vowel.A, 300)
        val result = analyze(16_000, pcm.frames(16_000))
        assertTrue(result.take(5).all { it.formants == null })
        val voiced = result.drop(7).dropLast(2).filter { it.voiced }
        assertTrue(voiced.isNotEmpty())
        voiced.forEach { assertEquals(Vowel.A.f1, it.formants!!.f1, Vowel.A.f1 * 0.15) }
    }
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*VoiceAnalyzerTest*'`
Expected: compilation fails (`formants` unresolved on `VoiceFrame`).

- [ ] **Step 3: Replace `VoiceAnalyzer`**

`app/src/main/java/com/summondemons/safely/ritual/VoiceAnalyzer.kt` (only the
`formants` field, its KDoc line, the estimator field and the last `return` change):

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ANALYSIS_RATE
import com.summondemons.safely.ritual.RitualConfig.ONSET_RATIO
import com.summondemons.safely.ritual.RitualConfig.ONSET_REARM_RATIO
import com.summondemons.safely.ritual.RitualConfig.ONSET_WINDOW_FRAMES
import com.summondemons.safely.ritual.RitualConfig.VOICE_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.VOICE_MIN_HZ
import com.summondemons.safely.ritual.RitualConfig.VOICE_RMS_FLOOR
import com.summondemons.safely.ritual.RitualConfig.VOICING_MIN_CORRELATION
import kotlin.math.sqrt

/** One analysed 20 ms mic frame (ADR-0008); [formants] only on voiced frames (ADR-0018). */
data class VoiceFrame(val rms: Double, val voiced: Boolean, val onset: Boolean, val formants: Formants? = null) {
    companion object {
        val SILENT = VoiceFrame(0.0, voiced = false, onset = false)
    }
}

/**
 * Turns consecutive PCM16 frames into [VoiceFrame]s. Stateful (keeps the previous frame
 * and a short RMS history) but pure: no Android types.
 * A new instance is needed when the sample rate changes.
 * - voiced: loud enough and periodic at a voice pitch (70–400 Hz) over the last 40 ms.
 * - onset: a voiced energy rise while armed; re-arms after a dip or an unvoiced frame.
 * - formants: F1/F2 of voiced frames over the same 40 ms window (ADR-0018).
 */
class VoiceAnalyzer(val sampleRate: Int) {
    private val stride = maxOf(1, sampleRate / ANALYSIS_RATE)
    private val minLag = sampleRate / VOICE_MAX_HZ
    private val maxLag = sampleRate / VOICE_MIN_HZ
    private var previous = ShortArray(0)
    private val recent = ArrayDeque<Double>()
    private var armed = true
    private var peak = 0.0
    private val formantEstimator = FormantEstimator(sampleRate)

    fun analyze(samples: ShortArray, count: Int): VoiceFrame {
        if (count <= 0) return VoiceFrame.SILENT
        val n = count.coerceAtMost(samples.size)
        val current = samples.copyOf(n)
        val level = rms(current, n)
        val window = previous + current
        previous = current
        val voiced = level > VOICE_RMS_FLOOR && periodicity(window) >= VOICING_MIN_CORRELATION

        val floor = recent.minOrNull() ?: 0.0
        var onset = false
        if (armed && voiced && level >= ONSET_RATIO * floor) {
            onset = true
            armed = false
            peak = level
        } else if (!armed) {
            peak = maxOf(peak, level)
            if (!voiced || level < ONSET_REARM_RATIO * peak) armed = true
        }
        recent.addLast(level)
        if (recent.size > ONSET_WINDOW_FRAMES) recent.removeFirst()
        return VoiceFrame(level, voiced, onset, if (voiced) formantEstimator.estimate(window) else null)
    }

    /** Highest normalized autocorrelation over the voice-pitch lags, in [0, 1]. */
    private fun periodicity(x: ShortArray): Double {
        val upper = minOf(maxLag, x.size / 2)
        var best = 0.0
        var lag = minLag
        while (lag <= upper) {
            var num = 0.0
            var e1 = 0.0
            var e2 = 0.0
            var i = 0
            while (i < x.size - lag) {
                val a = x[i].toDouble()
                val b = x[i + lag].toDouble()
                num += a * b
                e1 += a * a
                e2 += b * b
                i += stride
            }
            if (e1 > 0.0 && e2 > 0.0) best = maxOf(best, num / sqrt(e1 * e2))
            lag += stride
        }
        return best
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*VoiceAnalyzerTest*'`
Expected: all `VoiceAnalyzerTest` tests pass (the old ones are unchanged: `formants`
defaults to null, so `VoiceFrame(0.0, voiced = false, onset = false)` still equals `SILENT`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/VoiceAnalyzer.kt app/src/test/java/com/summondemons/safely/ritual/VoiceAnalyzerTest.kt
git commit -q -m "feat(ritual): Formants on voiced frames (ADR-0018)" -m "$TRAILER"
```

### Task 4: ChantJudge

Expected vowels from the spelling, a tolerant alignment against the heard vowel colours,
and the rhythm terms. `ChantTake` is what the engine accumulates per phrase (Task 5).

**Files:**
- Replace: `MAIN/ritual/ChantJudge.kt` (Task 2 created it with `Vowel` only)
- Create: `TEST/ritual/ChantJudgeTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/ritual/ChantJudgeTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantJudgeTest {

    private val phrase = "DAEMONIUM RESIDENTE,"
    private val expected = ChantJudge.expectedVowels(phrase)

    /** A take whose nuclei are [vowels] at vocal-tract [scale], at a chanting pace. */
    private fun take(vowels: List<Vowel?>, scale: Double = 1.0, pauses: Int = 0, intervalMs: Long = 300L) =
        ChantTake(
            nuclei = vowels.dropLast(1).map { it?.formants(scale) },
            current = SyllableFormants() + vowels.last()?.formants(scale),
            pauses = pauses,
            litSpanMs = intervalMs * (vowels.size - 1),
        )

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
    fun theRightVowelsScoreFullMarks() {
        val score = ChantJudge.score(expected, take(expected))
        assertEquals(1f, score.vowels, 1e-4f)
        assertEquals(1f, score.rhythm, 1e-4f)
        assertTrue(score.passed)
    }

    @Test
    fun aHigherVoiceStillMatches() {
        assertTrue(ChantJudge.score(expected, take(expected, scale = 1.2)).passed)
        assertTrue(ChantJudge.score(expected, take(expected, scale = 1.4)).passed)
    }

    @Test
    fun oneMissedSyllableStillPasses() {
        val missing = expected.toMutableList().also { it.removeAt(3) }
        val score = ChantJudge.score(expected, take(missing + listOf(E)))
        assertTrue("total=${score.total}", score.passed)
    }

    @Test
    fun unrelatedPortugueseFails() {
        // "Olá meu amigo como está hoje", cut at 8 syllables like the karaoke does.
        val babble = listOf(O, A, E, U, A, I, O, O)
        val score = ChantJudge.score(expected, take(babble))
        assertTrue("total=${score.total}", score.total < CHANT_PASS_SCORE)
    }

    @Test
    fun oneHeldVowelFails() {
        // E is half of this phrase; holding it is close enough to chanting and may pass (ADR-0018).
        (Vowel.entries - E).forEach { v ->
            val score = ChantJudge.score(expected, take(List(expected.size) { v }))
            assertFalse("$v total=${score.total}", score.passed)
        }
    }

    @Test
    fun unmeasuredSyllablesCostHalf() {
        val score = ChantJudge.vowelScore(expected, List(expected.size) { null })
        assertEquals(0.5f, score, 1e-4f)
    }

    @Test
    fun rattlingItOffTooFastHalvesTheRhythm() {
        assertEquals(0.5f, ChantJudge.score(expected, take(expected, intervalMs = 120L)).rhythm, 1e-4f)
    }

    @Test
    fun extraPausesCostRhythm() {
        // 8 syllables allow 1 + 8 / 6 = 2 pauses.
        assertEquals(1f, ChantJudge.score(expected, take(expected, pauses = 2)).rhythm, 1e-4f)
        assertEquals(0.5f, ChantJudge.score(expected, take(expected, pauses = 4)).rhythm, 1e-4f)
    }
}
```

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantJudgeTest*'`
Expected: compilation fails (`ChantJudge`, `ChantTake`, `ChantScore` unresolved).

- [ ] **Step 3: Implement**

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

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantJudgeTest*'`
Expected: 10 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual/ChantJudge.kt app/src/test/java/com/summondemons/safely/ritual/ChantJudgeTest.kt
git commit -q -m "feat(ritual): ChantJudge, vowel alignment and rhythm (ADR-0018)" -m "$TRAILER"
```

### Task 5: The engine judges each phrase

`RitualState` gains `vowels` (null = check off) and `take`; `RitualEngine` hears the last
vowel before judging and emits `Judged` + `Sealed` or `Judged` + `Rejected`. With the check
off nothing changes, which is why `RitualEngineTest` must stay green **without edits**.

The two new effects join a sealed interface that `RitualFeedback.handle` matches with an
exhaustive `when`, and unit tests compile the whole main source set, so this task also
adds those two branches (the warning haptic on `Rejected`).

**Files:**
- Replace: `MAIN/ritual/RitualState.kt`, `MAIN/ritual/RitualEngine.kt`
- Modify: `MAIN/platform/RitualFeedback.kt`
- Create: `TEST/ritual/ChantCheckTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/summondemons/safely/ritual/ChantCheckTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.SEAL_LISTEN_MS
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
    fun aRejectionDrainsTheClockLikeAnyOtherTick() {
        val second = run(locked(), sing(expected(0))).state
        val budget = second.focusMs
        val babble = run(second, sing(List(expected(1).size) { Vowel.U }))
        assertTrue(Rejected(1) in babble.effects)
        assertTrue(babble.state.focusMs < budget)
    }

    @Test
    fun aCrackWhileListeningClearsTheTake() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = FRAME_MS)
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

- [ ] **Step 2: Run it to see it fail**

Run: `./gradlew testDebugUnitTest --tests '*ChantCheckTest*'`
Expected: compilation fails (`judged`, `Judged`, `Rejected`, `take` unresolved).

- [ ] **Step 3: Replace `RitualState`**

`app/src/main/java/com/summondemons/safely/ritual/RitualState.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_BASE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_PER_SYLLABLE_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
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
        )
    }
}

/** Focus budget in ms for phrase index [phrase] with [syllables] syllables (ADR-0010); none for phrase I. */
fun focusBudget(syllables: Int, phrase: Int, courtMultiplier: Float = 1f): Long =
    if (phrase <= 0) 0L else ((FOCUS_BASE_MS + FOCUS_PER_SYLLABLE_MS * syllables) * courtMultiplier).roundToLong()

data class Step(val state: RitualState, val effects: List<RitualEffect> = emptyList())
```

- [ ] **Step 4: Replace `RitualEngine`**

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

/** Pure ritual rules (ADR-0008, the focus clock of ADR-0010, the chant check of ADR-0018): one call per 20 ms tick. */
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
        val reset = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L, lit = lit)
            .hearing { light(voice, first = s.lit == 0, last = last) }
        // With the chant check on, the last syllable's vowel is heard before the verdict (ADR-0018).
        if (!last || reset.take.closing) return drain(reset, 1, effects)
        return seal(reset, effects)
    }

    /** Hears the end of the last syllable, then judges the phrase: sealed, or rejected and reopened (ADR-0018). */
    private fun close(s: RitualState, voice: VoiceFrame, effects: MutableList<RitualEffect>): Step {
        val take = s.take.hear(voice, begun = true)
        if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), 1, effects)
        val score = ChantJudge.score(s.vowels!![s.phrase], take)
        effects += RitualEffect.Judged(s.phrase, score)
        if (score.passed) return seal(s.copy(take = ChantTake()), effects)
        effects += RitualEffect.Rejected(s.phrase)
        return drain(s.copy(lit = 0, take = ChantTake()), 1, effects)
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

    /** Drains the focus clock (phrases II–IV only); at 0 the last seal cracks. */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(s.syllables[phrase], phrase, s.courtMultiplier)
        return Step(s.copy(phrase = phrase, lit = 0, focusMs = budget, focusBudgetMs = budget, flawless = false, take = ChantTake()), effects)
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

- [ ] **Step 5: Edit `app/src/main/java/com/summondemons/safely/platform/RitualFeedback.kt`**

Replace:

```kotlin
        RitualEffect.Resumed -> fadeBed(BED_VOLUME, DUCK_MS)
```

with:

```kotlin
        RitualEffect.Resumed -> fadeBed(BED_VOLUME, DUCK_MS)
        is RitualEffect.Judged -> Unit
        is RitualEffect.Rejected -> Haptics.warn(context)
```

- [ ] **Step 6: Run the ritual tests**

Run: `./gradlew testDebugUnitTest --tests '*ChantCheckTest*' --tests '*RitualEngineTest*' --tests '*RitualStateTest*'`
Expected: all pass; `RitualEngineTest` and `RitualStateTest` are unchanged.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/main/java/com/summondemons/safely/platform/RitualFeedback.kt app/src/test/java/com/summondemons/safely/ritual/ChantCheckTest.kt
git commit -q -m "feat(ritual): Judge each phrase before sealing; reject what doesn't match (ADR-0018)" -m "$TRAILER"
```

### Task 6: End to end on synthetic audio

PCM → `VoiceAnalyzer` → `RitualEngine`, the way the app runs, through a replay helper the
calibration harness (Task 9) reuses.

**Files:**
- Create: `TEST/ritual/ChantReplay.kt`, `TEST/ritual/SyntheticChantTest.kt`

- [ ] **Step 1: Add the replay helper and WAV reader**

`app/src/test/java/com/summondemons/safely/ritual/ChantReplay.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Replays audio through the real pipeline (VoiceAnalyzer → RitualEngine, chant check on, always on heading). */
object ChantReplay {

    /** Every [RitualEffect.Judged] the ritual produces while hearing [pcm] (plus 1 s of trailing silence). */
    fun judge(pcm: ShortArray, rate: Int, phrases: List<String>): List<RitualEffect.Judged> {
        var state = RitualState.start(0f, phrases, judged = true)
        repeat((LOCK_HOLD_MS / FRAME_MS).toInt()) { state = RitualEngine.step(state, RitualInput(0f, VoiceFrame.SILENT)).state }
        val analyzer = VoiceAnalyzer(rate)
        val judged = mutableListOf<RitualEffect.Judged>()
        (pcm + ShortArray(rate)).frames(rate).forEach { frame ->
            val step = RitualEngine.step(state, RitualInput(0f, analyzer.analyze(frame, frame.size)))
            state = step.state
            judged += step.effects.filterIsInstance<RitualEffect.Judged>()
        }
        return judged
    }
}

/** A mono PCM16 WAV file. */
class Wav(val rate: Int, val samples: ShortArray) {
    companion object {
        /** Reads the `fmt ` and `data` chunks; rejects anything but mono 16-bit PCM. */
        fun read(file: File): Wav {
            val raw = file.readBytes()
            require(raw.size >= 12 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") { "$file: not a WAV file" }
            val bytes = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
            bytes.position(12)
            var rate = 0
            while (bytes.remaining() >= 8) {
                val id = ByteArray(4).also { bytes.get(it) }.toString(Charsets.US_ASCII)
                val size = bytes.int
                when (id) {
                    "fmt " -> {
                        val format = bytes.short.toInt()
                        val channels = bytes.short.toInt()
                        rate = bytes.int
                        bytes.position(bytes.position() + 6)
                        val bits = bytes.short.toInt()
                        require(format == 1 && channels == 1 && bits == 16) { "$file: need mono 16-bit PCM" }
                        bytes.position(bytes.position() + size - 16)
                    }
                    "data" -> {
                        val n = minOf(size, bytes.remaining()) / 2
                        return Wav(rate, ShortArray(n) { bytes.short })
                    }
                    else -> bytes.position(bytes.position() + size)
                }
            }
            error("$file: no data chunk")
        }
    }
}
```

- [ ] **Step 2: Write the test**

`app/src/test/java/com/summondemons/safely/ritual/SyntheticChantTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualEffect.Judged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End to end (ADR-0018): synthetic PCM → VoiceAnalyzer → RitualEngine with the chant check on. */
class SyntheticChantTest {

    private val phrases = listOf(
        "DAEMONIUM RESIDENTE,",
        "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
        "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
        "UT VOTA OMNIA OPUS EST.",
    )

    private fun judge(pcm: ShortArray, rate: Int = 16_000): List<Judged> = ChantReplay.judge(pcm, rate, phrases)

    private fun vowels(phrase: Int) = ChantJudge.expectedVowels(phrases[phrase])

    @Test
    fun aMaleVoiceChantingPhraseISealsIt() {
        val judged = judge(SyntheticVoice(f0 = 110.0).syllables(vowels(0)))
        assertEquals(1, judged.size)
        assertTrue("score=${judged.single().score}", judged.single().score.passed)
    }

    @Test
    fun aFemaleVoiceChantingPhraseISealsIt() {
        val judged = judge(SyntheticVoice(f0 = 210.0, scale = 1.2).syllables(vowels(0)))
        assertTrue("score=${judged.single().score}", judged.single().score.passed)
    }

    @Test
    fun aChantRecordedAt44kHzSealsIt() {
        val judged = judge(SyntheticVoice(rate = 44_100, f0 = 120.0).syllables(vowels(0)), 44_100)
        assertTrue("score=${judged.single().score}", judged.single().score.passed)
    }

    @Test
    fun theFirstTwoPhrasesInARowBothSeal() {
        val voice = SyntheticVoice(f0 = 130.0)
        val pcm = voice.syllables(vowels(0)) + voice.vowel(null, 600) + voice.syllables(vowels(1))
        val judged = judge(pcm)
        assertEquals(listOf(0, 1), judged.map { it.phrase })
        assertTrue(judged.all { it.score.passed })
    }

    @Test
    fun repeatingOlaMeuAmigoNeverSealsPhraseI() {
        // "O-lá meu a-mi-go, co-mo es-tá ho-je?" three times, as in the play-test.
        val ola = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O, Vowel.O, Vowel.E, Vowel.A, Vowel.O, Vowel.E)
        val voice = SyntheticVoice(f0 = 120.0)
        val pcm = List(3) { voice.syllables(ola, vowelMs = 160, gapMs = 50) + voice.vowel(null, 400) }
            .fold(ShortArray(0)) { acc, s -> acc + s }
        val judged = judge(pcm)
        assertTrue("judged ${judged.size}", judged.size >= 3)
        assertTrue("scores=${judged.map { it.score.total }}", judged.none { it.score.passed })
    }

    @Test
    fun holdingOneVowelNeverSealsPhraseI() {
        // E is half of phrase I; holding it is close enough to chanting and may pass (ADR-0018).
        (Vowel.entries - Vowel.E).forEach { v ->
            val judged = judge(SyntheticVoice(f0 = 120.0).syllables(List(16) { v }))
            assertTrue("$v scores=${judged.map { it.score.total }}", judged.isNotEmpty() && judged.none { it.score.passed })
        }
    }
}
```

- [ ] **Step 3: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*SyntheticChantTest*'`
Expected: 6 tests pass (the core is already in place; this task is a regression net for
the whole pipeline).

- [ ] **Step 4: Gate, then commit**

Run the verification gate. Expected: `BUILD SUCCESSFUL`.

```bash
git add app/src/test/java/com/summondemons/safely/ritual
git commit -q -m "test(ritual): End-to-end chant check on synthetic voices (ADR-0018)" -m "$TRAILER"
```

---

## Phase 3 — App

### Task 7: RitualViewModel

The app always runs with the check on. Hold to Chant sounds the expected vowels so debug
runs still complete. The UI state carries the last score, a rejection count (it picks the
scoff line) and the debug clip label.

**Files:**
- Create: `MAIN/ritual/ClipRecorder.kt`
- Modify: `MAIN/ritual/RitualViewModel.kt`, `TEST/ritual/RitualViewModelTest.kt`

- [ ] **Step 1: Add the recorder interface**

`app/src/main/java/com/summondemons/safely/ritual/ClipRecorder.kt`:

```kotlin
package com.summondemons.safely.ritual

/**
 * Debug-only capture of raw mic audio, for calibrating the chant check (ADR-0018). Release
 * builds get [None] from their own source set, so they contain no recording code.
 */
interface ClipRecorder {
    /** Starts a clip named [name]; frames go to it until [stop]. Replaces any clip in progress. */
    fun start(name: String)

    fun write(frame: PcmFrame)

    fun stop()

    object None : ClipRecorder {
        override fun start(name: String) = Unit
        override fun write(frame: PcmFrame) = Unit
        override fun stop() = Unit
    }
}

/** What a debug clip holds; the tag goes into the file name `{demonId}-{tag}-{stamp}.wav`. */
enum class ClipLabel(val tag: String) { Chant("chant"), Nonsense("nonsense") }
```

- [ ] **Step 2: Edit `app/src/main/java/com/summondemons/safely/ritual/RitualViewModel.kt`**

(a) Replace:

```kotlin
    /** The Ritual screen is started; no ticks run otherwise, so the focus clock pauses (§2). */
    val active: Boolean = true,
)
```

with:

```kotlin
    /** The Ritual screen is started; no ticks run otherwise, so the focus clock pauses (§2). */
    val active: Boolean = true,
    /** The chant check's latest verdict, for the debug readout (ADR-0018). */
    val lastScore: ChantScore? = null,
    /** Phrases rejected this attempt; picks the scoff line (ADR-0018). */
    val rejections: Int = 0,
    /** Debug clip recorder label; null = not recording (ADR-0018). */
    val clipLabel: ClipLabel? = null,
)
```

(b) Replace:

```kotlin
    /** Saves the summon. Called inside the tick, exactly once, on the step that starts the climax (ADR-0013). */
    private val commitSummon: (flawless: Boolean) -> Unit = {},
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
```

with:

```kotlin
    /** Saves the summon. Called inside the tick, exactly once, on the step that starts the climax (ADR-0013). */
    private val commitSummon: (flawless: Boolean) -> Unit = {},
    /** Debug builds record tester clips here; release builds pass [ClipRecorder.None] (ADR-0018). */
    private val clipRecorder: ClipRecorder = ClipRecorder.None,
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
```

(c) Replace:

```kotlin
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases),
```

with:

```kotlin
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases, judged = true),
```

(d) Replace:

```kotlin
    /** Called from the Ritual screen's ON_START / ON_STOP. */
```

with:

```kotlin
    /** Debug: Off → Chant → Nonsense → Off. Each label starts a new clip (ADR-0018). */
    fun cycleClipLabel() {
        val next = when (state.value.clipLabel) {
            null -> ClipLabel.Chant
            ClipLabel.Chant -> ClipLabel.Nonsense
            ClipLabel.Nonsense -> null
        }
        clipRecorder.stop()
        if (next != null) clipRecorder.start("${demon.id}-${next.tag}")
        state.update { it.copy(clipLabel = next) }
    }

    override fun onCleared() = clipRecorder.stop()

    /** Called from the Ritual screen's ON_START / ON_STOP. */
```

(e) Replace:

```kotlin
        val voice = if (current.holdToChant) syntheticVoice() else micFrame
```

with:

```kotlin
        val voice = if (current.holdToChant) syntheticVoice(current.ritual) else micFrame
```

(f) Replace:

```kotlin
        state.update { it.copy(ritual = step.state, micLevel = smoothMicLevel(it.micLevel, voice.rms)) }
```

with:

```kotlin
        state.update {
            it.copy(
                ritual = step.state,
                micLevel = smoothMicLevel(it.micLevel, voice.rms),
                lastScore = step.effects.filterIsInstance<RitualEffect.Judged>().lastOrNull()?.score ?: it.lastScore,
                rejections = it.rejections + step.effects.count { effect -> effect is RitualEffect.Rejected },
            )
        }
```

(g) Replace:

```kotlin
    /** Debug "Hold to Chant": continuous voicing with an onset every ~240 ms. */
    private fun syntheticVoice(): VoiceFrame =
        VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0)
```

with:

```kotlin
    /**
     * Debug "Hold to Chant": continuous voicing with an onset every ~240 ms, sounding the
     * vowel the chant check expects (ADR-0018): the next syllable's on an onset, else the current one's.
     */
    private fun syntheticVoice(ritual: RitualState): VoiceFrame {
        val onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0
        val vowels = ritual.vowels?.getOrNull(ritual.phrase).orEmpty()
        val index = (if (onset) ritual.lit else ritual.lit - 1).coerceIn(0, maxOf(0, vowels.lastIndex))
        return VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = onset, formants = vowels.getOrNull(index)?.formants())
    }
```

(h) Replace:

```kotlin
        pcmSource.frames().collect { frame ->
            val current
```

with:

```kotlin
        pcmSource.frames().collect { frame ->
            clipRecorder.write(frame)
            val current
```

(i) Replace:

```kotlin
            inCourt: () -> Boolean = { false },
            commitSummon: (flawless: Boolean) -> Unit = {},
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon) } }
```

with:

```kotlin
            inCourt: () -> Boolean = { false },
            commitSummon: (flawless: Boolean) -> Unit = {},
            clipRecorder: ClipRecorder = ClipRecorder.None,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon, clipRecorder) }
        }
```

- [ ] **Step 3: Edit `app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt`**

Replace:

```kotlin
            listOf(Locked, Sealed(0), Sealed(1), Sealed(2), Sealed(3), ClimaxStart(flawless = true), Summoned),
            effects.filter { it != SyllableLit && it != ProximityTick },
        )
        assertEquals(0, pcm.subscriptions)
    }
```

with:

```kotlin
            listOf(Locked, Sealed(0), Sealed(1), Sealed(2), Sealed(3), ClimaxStart(flawless = true), Summoned),
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.Judged },
        )
        assertEquals(0, pcm.subscriptions)
        // Hold to Chant sounds the expected vowels, so every phrase passes the chant check (ADR-0018).
        assertEquals(4, effects.count { it is RitualEffect.Judged && it.score.passed })
        assertTrue(vm.uiState.value.lastScore!!.passed)
        assertEquals(0, vm.uiState.value.rejections)
    }

    private class FakeRecorder : ClipRecorder {
        val calls = mutableListOf<String>()
        var frames = 0
        override fun start(name: String) {
            calls += "start $name"
        }
        override fun write(frame: PcmFrame) {
            frames++
        }
        override fun stop() {
            calls += "stop"
        }
    }

    @Test
    fun clipLabelCyclesAndTheRecorderGetsMicFrames() = runTest(dispatcher) {
        val recorder = FakeRecorder()
        val vm = RitualViewModel(demon, { emptyFlow() }, chantingPcm(), clipRecorder = recorder, analysisDispatcher = dispatcher)
        observe(vm)
        vm.onMicPermission(true)
        vm.cycleClipLabel()
        assertEquals(ClipLabel.Chant, vm.uiState.value.clipLabel)
        advanceTimeBy(1_000); runCurrent()
        vm.cycleClipLabel()
        assertEquals(ClipLabel.Nonsense, vm.uiState.value.clipLabel)
        vm.cycleClipLabel()
        assertNull(vm.uiState.value.clipLabel)
        assertEquals(listOf("stop", "start asmodeus-chant", "stop", "start asmodeus-nonsense", "stop"), recorder.calls)
        assertTrue(recorder.frames > 0)
    }
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew testDebugUnitTest --tests '*RitualViewModelTest*'`
Expected: all pass, including `clipLabelCyclesAndTheRecorderGetsMicFrames` and the
extended `holdToChantWithSimulatedHeadingCompletesTheRitual`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ritual app/src/test/java/com/summondemons/safely/ritual/RitualViewModelTest.kt
git commit -q -m "feat(ritual): Chant check on in the app; Hold to Chant sounds the vowels (ADR-0018)" -m "$TRAILER"
```

### Task 8: The demon scoffs

A rejected phrase shows a rotating scoff line in the cue chip for 1.5 s (after errors and
cracks, before the phase rows), flashes that phrase's sigil strokes red, and buzzes the
warning haptic (Task 5 added the haptic). The phrase simply un-lights; no new component,
sound or token. (The debug wiring in `RitualScreen` comes in Task 9.)

**Files:**
- Modify: `MAIN/ui/screens/RitualCue.kt`, `MAIN/ui/screens/RitualScreen.kt`, `app/src/main/res/values/strings.xml`, `TEST/ui/screens/RitualCueTest.kt`

- [ ] **Step 1: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualCue.kt`**

(a) Replace:

```kotlin
    Cracked(CueTone.Error),
    TurnLeft(CueTone.Neutral),
```

with:

```kotlin
    Cracked(CueTone.Error),
    Rejected(CueTone.Error),
    TurnLeft(CueTone.Neutral),
```

(b) Replace:

```kotlin
data class RitualCue(val kind: CueKind, val crackedPhrase: Int? = null)

/** [crackedPhrase] is the phrase that cracked within the last 1.5 s, or null. */
fun ritualCue(state: RitualUiState, crackedPhrase: Int?): RitualCue {
```

with:

```kotlin
/** [rejections] counts the attempt's rejected phrases while [kind] is [CueKind.Rejected] (it picks the scoff line), else 0. */
data class RitualCue(val kind: CueKind, val crackedPhrase: Int? = null, val rejections: Int = 0)

/**
 * [crackedPhrase] is the phrase that cracked within the last 1.5 s, or null; [rejected] is
 * true within 1.5 s of a phrase the chant check rejected (ADR-0018).
 */
fun ritualCue(state: RitualUiState, crackedPhrase: Int?, rejected: Boolean = false): RitualCue {
```

(c) Replace:

```kotlin
        crackedPhrase != null -> CueKind.Cracked
        else -> when (ritual.phase) {
```

with:

```kotlin
        crackedPhrase != null -> CueKind.Cracked
        rejected -> CueKind.Rejected
        else -> when (ritual.phase) {
```

(d) Replace:

```kotlin
    return RitualCue(kind, crackedPhrase.takeIf { kind == CueKind.Cracked })
```

with:

```kotlin
    return RitualCue(
        kind,
        crackedPhrase.takeIf { kind == CueKind.Cracked },
        if (kind == CueKind.Rejected) state.rejections else 0,
    )
```

- [ ] **Step 2: Edit `app/src/test/java/com/summondemons/safely/ui/screens/RitualCueTest.kt`**

(a) Replace:

```kotlin
    private fun kind(s: RitualUiState, cracked: Int? = null) = ritualCue(s, cracked).kind
```

with:

```kotlin
    private fun kind(s: RitualUiState, cracked: Int? = null, rejected: Boolean = false) = ritualCue(s, cracked, rejected).kind
```

(b) Replace:

```kotlin
    @Test
    fun seekingTurnsTowardTheTarget() {
```

with:

```kotlin
    @Test
    fun aRejectionShowsAfterErrorsAndCracksButBeforeThePhaseRows() {
        val chanting = state(RitualPhase.Chanting).copy(rejections = 2)
        val cue = ritualCue(chanting, crackedPhrase = null, rejected = true)
        assertEquals(CueKind.Rejected, cue.kind)
        assertEquals(2, cue.rejections)
        assertEquals(CueTone.Error, cue.kind.tone)
        assertEquals(CueKind.Cracked, kind(chanting, cracked = 0, rejected = true))
        assertEquals(CueKind.MicDenied, kind(chanting.copy(micGranted = false), rejected = true))
        assertEquals(CueKind.Chant, kind(chanting))
        assertEquals(0, ritualCue(chanting, crackedPhrase = null).rejections)
    }

    @Test
    fun seekingTurnsTowardTheTarget() {
```

- [ ] **Step 3: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`**

(a) Replace:

```kotlin
import androidx.compose.ui.res.stringResource

```

with:

```kotlin
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource

```

(b) Replace:

```kotlin
private const val CRACK_MS = 1_500

```

with:

```kotlin
private const val CRACK_MS = 1_500
private const val REJECT_MS = 1_500

```

(c) Replace:

```kotlin
/** A phrase that just sealed or cracked; [id] keeps repeats distinct. */
```

with:

```kotlin
/** A phrase that just sealed, cracked or was rejected; [id] keeps repeats distinct. */
```

(d) Replace:

```kotlin
    var cracked by remember { mutableStateOf<Mark?>(null) }
    var flash
```

with:

```kotlin
    var cracked by remember { mutableStateOf<Mark?>(null) }
    var rejected by remember { mutableStateOf<Mark?>(null) }
    var flash
```

(e) Replace:

```kotlin
                        if (cracked == mark) cracked = null
                    }
                }
                else -> Unit
```

with:

```kotlin
                        if (cracked == mark) cracked = null
                    }
                }
                is RitualEffect.Rejected -> {
                    val mark = Mark(effect.phrase, ++serial)
                    rejected = mark
                    flash = SigilFlash(effect.phrase + 1, crackColor, REJECT_MS, mark.id)
                    launch {
                        delay(REJECT_MS.toLong())
                        if (rejected == mark) rejected = null
                    }
                }
                else -> Unit
```

(f) Replace:

```kotlin
        cracked = cracked?.phrase,
        flash = flash,
```

with:

```kotlin
        cracked = cracked?.phrase,
        rejected = rejected != null,
        flash = flash,
```

(g) Replace:

```kotlin
    cracked: Int?,
    flash: SigilFlash?,
```

with:

```kotlin
    cracked: Int?,
    rejected: Boolean,
    flash: SigilFlash?,
```

(h) Replace:

```kotlin
            val cue = ritualCue(state, cracked)
```

with:

```kotlin
            val cue = ritualCue(state, cracked, rejected)
```

(i) Replace:

```kotlin
        CueKind.Cracked -> stringResource(R.string.cue_cracked, ROMAN_NUMERALS[cue.crackedPhrase ?: 0])
```

with:

```kotlin
        CueKind.Cracked -> stringResource(R.string.cue_cracked, ROMAN_NUMERALS[cue.crackedPhrase ?: 0])
        CueKind.Rejected -> stringArrayResource(R.array.cue_rejected).let { it[(cue.rejections - 1).coerceAtLeast(0) % it.size] }
```

- [ ] **Step 4: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="cue_cracked">The seal cracked · Chant %1$s again</string>

```

with:

```xml
    <string name="cue_cracked">The seal cracked · Chant %1$s again</string>
    <!-- Chant check (ADR-0018): shown in turn, one per rejected phrase. -->
    <string-array name="cue_rejected">
        <item>That wasn\'t Latin · Chant it again</item>
        <item>He\'s heard better from a parrot · Again</item>
        <item>The demon yawns · Read the tablet</item>
        <item>Nice try · The real words, please</item>
    </string-array>

```

- [ ] **Step 5: Run the tests and build**

Run: `./gradlew testDebugUnitTest --tests '*RitualCueTest*'` then `./gradlew assembleDebug`.
Expected: tests pass and the debug build succeeds.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens app/src/main/res/values/strings.xml app/src/test/java/com/summondemons/safely/ui/screens/RitualCueTest.kt
git commit -q -m "feat(ui): The demon scoffs at a rejected phrase (ADR-0018)" -m "$TRAILER"
```

### Task 9: Debug clip recorder, readout and calibration harness

Debug builds get a "Record clip" chip (Off → chant → nonsense → Off) and the last score.
Clips are WAV files in the app's private `files/chant-clips/`, excluded from backups. The
release source set supplies a no-op factory, so release builds contain no recording code.

**Files:**
- Create: `DEBUG/platform/WavClipRecorder.kt`, `DEBUG/platform/ClipRecorders.kt`, `RELEASE/platform/ClipRecorders.kt`, `TESTDEBUG/platform/WavClipRecorderTest.kt`, `TEST/ritual/ChantClipReplayTest.kt`
- Modify: `MAIN/ui/components/DebugControls.kt`, `MAIN/ui/screens/RitualScreen.kt`, `MAIN/navigation/AppNavHost.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `.gitignore`

- [ ] **Step 1: Write the failing recorder test**

`app/src/testDebug/java/com/summondemons/safely/platform/WavClipRecorderTest.kt`:

```kotlin
package com.summondemons.safely.platform

import com.summondemons.safely.ritual.PcmFrame
import com.summondemons.safely.ritual.Wav
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Date

class WavClipRecorderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun frame(vararg samples: Short, rate: Int = 16_000) = PcmFrame(shortArrayOf(*samples), samples.size, rate)

    @Test
    fun writesAReadableWavWithTheFirstFramesRate() {
        val recorder = WavClipRecorder(folder.root) { Date(0) }
        recorder.start("asmodeus-chant")
        recorder.write(frame(1, -2, 3))
        recorder.write(frame(4, 5, rate = 44_100)) // another rate: dropped
        recorder.write(frame(-6))
        recorder.stop()

        val file = folder.root.listFiles()!!.single()
        assertTrue(file.name, file.name.startsWith("asmodeus-chant-") && file.name.endsWith(".wav"))
        val wav = Wav.read(file)
        assertEquals(16_000, wav.rate)
        assertArrayEquals(shortArrayOf(1, -2, 3, -6), wav.samples)
    }

    @Test
    fun nothingIsWrittenBeforeStartOrAfterStop() {
        val recorder = WavClipRecorder(folder.root)
        recorder.write(frame(1))
        recorder.start("astaroth-nonsense")
        recorder.stop()
        recorder.write(frame(2))
        assertEquals(0, folder.root.listFiles()!!.size)
    }
}
```

- [ ] **Step 2: Implement the WAV recorder (debug source set)**

`app/src/debug/java/com/summondemons/safely/platform/WavClipRecorder.kt`:

```kotlin
package com.summondemons.safely.platform

import com.summondemons.safely.ritual.ClipRecorder
import com.summondemons.safely.ritual.PcmFrame
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug builds only (ADR-0018): writes each clip as a mono PCM16 WAV file
 * `{name}-{yyyyMMdd-HHmmss}.wav` in [dir]. The file opens on the first frame and takes
 * its sample rate; frames at another rate are dropped. Thread-safe: frames arrive on
 * the analysis thread, start/stop on the main thread.
 */
class WavClipRecorder(
    private val dir: File,
    private val now: () -> Date = { Date() },
) : ClipRecorder {
    private var name: String? = null
    private var file: RandomAccessFile? = null
    private var rate = 0
    private var dataBytes = 0L

    @Synchronized
    override fun start(name: String) {
        stop()
        this.name = name
    }

    @Synchronized
    override fun write(frame: PcmFrame) {
        val base = name ?: return
        val out = file ?: open(base, frame.sampleRate)
        if (frame.sampleRate != rate || frame.count <= 0) return
        val bytes = ByteBuffer.allocate(frame.count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frame.count) bytes.putShort(frame.samples[i])
        out.write(bytes.array())
        dataBytes += frame.count * 2
    }

    @Synchronized
    override fun stop() {
        file?.let {
            it.seek(0)
            it.write(header(rate, dataBytes))
            it.close()
        }
        file = null
        name = null
        dataBytes = 0L
    }

    private fun open(base: String, sampleRate: Int): RandomAccessFile {
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now())
        rate = sampleRate
        return RandomAccessFile(File(dir, "$base-$stamp.wav"), "rw").also {
            it.setLength(0)
            it.write(header(sampleRate, 0L))
            file = it
        }
    }

    companion object {
        /** The 44-byte RIFF/WAVE header for mono PCM16. */
        fun header(sampleRate: Int, dataBytes: Long): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((36 + dataBytes).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1) // PCM
            putShort(1) // mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataBytes.toInt())
        }.array()
    }
}
```

- [ ] **Step 3: Add the two factories**

`app/src/debug/java/com/summondemons/safely/platform/ClipRecorders.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ClipRecorder
import java.io.File

/** Debug builds: tester clips go to the app's private `files/chant-clips/` (ADR-0018). */
object ClipRecorders {
    fun create(context: Context): ClipRecorder = WavClipRecorder(File(context.filesDir, "chant-clips"))
}
```

`app/src/release/java/com/summondemons/safely/platform/ClipRecorders.kt`:

```kotlin
package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ClipRecorder

/** Release builds never record audio (ADR-0018). */
object ClipRecorders {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): ClipRecorder = ClipRecorder.None
}
```

- [ ] **Step 4: Edit `app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt`**

(a) Replace:

```kotlin
        onHoldToChantChange = viewModel::setHoldToChant,
        onAbort = onAbort,
```

with:

```kotlin
        onHoldToChantChange = viewModel::setHoldToChant,
        onCycleClipLabel = viewModel::cycleClipLabel,
        onAbort = onAbort,
```

(b) Replace:

```kotlin
    onHoldToChantChange: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
```

with:

```kotlin
    onHoldToChantChange: (Boolean) -> Unit,
    onCycleClipLabel: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val demon = state.demon
```

(c) Replace:

```kotlin
                    onHoldToChantChange = onHoldToChantChange,
                )
```

with:

```kotlin
                    onHoldToChantChange = onHoldToChantChange,
                    lastScore = state.lastScore,
                    clipLabel = state.clipLabel,
                    onCycleClipLabel = onCycleClipLabel,
                )
```

- [ ] **Step 5: Edit `app/src/main/res/values/strings.xml`**

Replace:

```xml
    <string name="debug_chanting">Chanting…</string>

```

with:

```xml
    <string name="debug_chanting">Chanting…</string>
    <string name="debug_clip_off">Record clip</string>
    <string name="debug_clip_chant">Recording · chant</string>
    <string name="debug_clip_nonsense">Recording · nonsense</string>
    <string name="debug_chant_score">Chant check %1$.2f %2$s · vowels %3$.2f · rhythm %4$.2f</string>
    <string name="debug_chant_score_none">Chant check · no verdict yet</string>

```

- [ ] **Step 6: Edit `app/src/main/java/com/summondemons/safely/ui/components/DebugControls.kt`**

(a) Replace:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
```

with:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
```

(b) Replace:

```kotlin
import com.summondemons.safely.R
import com.summondemons.safely.ui.theme.PillShape
```

with:

```kotlin
import com.summondemons.safely.R
import com.summondemons.safely.ritual.ChantScore
import com.summondemons.safely.ritual.ClipLabel
import com.summondemons.safely.ui.theme.PillShape
```

(c) Replace:

```kotlin
    onHoldToChantChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
```

with:

```kotlin
    onHoldToChantChange: (Boolean) -> Unit,
    /** The chant check's latest verdict (ADR-0018). */
    lastScore: ChantScore?,
    /** Clip recorder label; null = not recording (ADR-0018). */
    clipLabel: ClipLabel?,
    onCycleClipLabel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        ChantControls(simulateHeading, onSimulateHeadingChange, onHoldToChantChange)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChantControls(
    simulateHeading: Boolean,
    onSimulateHeadingChange: (Boolean) -> Unit,
    onHoldToChantChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val onHold by rememberUpdatedState(onHoldToChantChange)
    var holding by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
```

- [ ] **Step 7: Edit `app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt`**

(a) Replace:

```kotlin
import com.summondemons.safely.platform.AndroidPcmSource

```

with:

```kotlin
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.platform.ClipRecorders

```

(b) Replace:

```kotlin
            commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
        )
```

with:

```kotlin
            commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
            clipRecorder = ClipRecorders.create(appContext),
        )
```

- [ ] **Step 8: Edit `app/src/main/res/xml/backup_rules.xml`**

Replace:

```xml
<!-- Everything is backed up except the debug-only settings (ADR-0013). -->
<full-backup-content>
    <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
```

with:

```xml
<!-- Everything is backed up except the debug-only settings (ADR-0013) and tester clips (ADR-0018). -->
<full-backup-content>
    <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
    <exclude domain="file" path="chant-clips/" />
```

- [ ] **Step 9: Edit `app/src/main/res/xml/data_extraction_rules.xml`**

(a) Replace:

```xml
<!-- Everything is backed up or transferred except the debug-only settings (ADR-0013). -->
```

with:

```xml
<!-- Everything is backed up or transferred except the debug-only settings (ADR-0013) and tester clips (ADR-0018). -->
```

(b) Replace:

```xml
    <cloud-backup>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
```

with:

```xml
    <cloud-backup>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
        <exclude domain="file" path="chant-clips/" />
```

(c) Replace:

```xml
    <device-transfer>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
```

with:

```xml
    <device-transfer>
        <exclude domain="file" path="datastore/debug_settings.preferences_pb" />
        <exclude domain="file" path="chant-clips/" />
```

- [ ] **Step 10: Edit `.gitignore`**

Replace:

```gitignore
# Signing (ADR-0017): never commit keystores
```

with:

```gitignore
# Tester voice clips for chant-check calibration (ADR-0018): people's voices, never committed
app/src/test/chant-clips/

# Signing (ADR-0017): never commit keystores
```

- [ ] **Step 11: Add the calibration harness**

`app/src/test/java/com/summondemons/safely/ritual/ChantClipReplayTest.kt`:

```kotlin
package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0018). Replays tester clips recorded by the
 * debug build's clip recorder through the real pipeline and prints how each label scores.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices) and are
 * named `{demonId}-{label}-{stamp}.wav`, label `chant` or `nonsense`. Without clips the test
 * is skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Result(val clip: String, val label: String, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsMeetTheTargets() {
        val clips = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", clips.isNotEmpty())
        val results = clips.flatMap { file ->
            val (demonId, label) = file.nameWithoutExtension.split('-').let { it[0] to it.getOrElse(1) { "" } }
            val demon = DemonCatalog.byId(demonId) ?: error("${file.name}: unknown demon '$demonId'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(file.name, label, it) }
        }
        results.forEach { r ->
            val s = r.judged.score
            println("%-44s %-8s phrase %s  vowels %.2f  rhythm %.2f  total %.2f  %s".format(
                r.clip, r.label, ROMAN[r.judged.phrase], s.vowels, s.rhythm, s.total, if (s.passed) "PASS" else "fail",
            ))
        }
        val chant = results.filter { it.label == "chant" }.map { it.judged.score.total }
        val nonsense = results.filter { it.label == "nonsense" }.map { it.judged.score.total }
        println("\nthreshold  chant pass  nonsense pass   (chant n=${chant.size}, nonsense n=${nonsense.size})")
        for (t in THRESHOLDS) {
            println("  %.2f%s     %5.1f %%      %5.1f %%".format(t, if (t == CHANT_PASS_SCORE) "*" else " ", rate(chant, t), rate(nonsense, t)))
        }
        assumeTrue("need ≥ $MIN_JUDGED judged phrases per label", chant.size >= MIN_JUDGED && nonsense.size >= MIN_JUDGED)
        assertTrue("honest chants rejected too often", rate(chant, CHANT_PASS_SCORE) >= 95.0)
        assertTrue("nonsense accepted too often", rate(nonsense, CHANT_PASS_SCORE) <= 40.0)
    }

    private fun rate(scores: List<Float>, threshold: Float) =
        if (scores.isEmpty()) 0.0 else scores.count { it >= threshold } * 100.0 / scores.size

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        val THRESHOLDS = listOf(0.45f, 0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f)
        const val MIN_JUDGED = 20
    }
}
```

With no clips it is skipped (JUnit "assumption failed"), so CI stays green.

- [ ] **Step 12: Gate, and prove release has no recorder**

Run the verification gate, then:

```bash
./gradlew assembleRelease
find app/build -path '*release*' -name 'WavClipRecorder*.class' | wc -l
```
Expected: gate `BUILD SUCCESSFUL`; `assembleRelease` succeeds (unsigned is fine); the
count is `0`.

- [ ] **Step 13: Commit**

```bash
git add app/src/debug app/src/release app/src/testDebug app/src/main/java/com/summondemons/safely/ui app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt app/src/main/res .gitignore app/src/test/java/com/summondemons/safely/ritual/ChantClipReplayTest.kt
git commit -q -m "feat(debug): Clip recorder, chant score readout and calibration harness (ADR-0018)" -m "$TRAILER"
```

---

## Phase 4 — Device check and calibration

### Task 10: Device checklist (debug build)

Install with `./gradlew installDebug`. On a real phone:

1. **Hold to Chant + Simulate heading** completes the ritual: every phrase seals a beat
   after its last syllable lights; the debug readout shows ✓ scores near 1.00.
2. **Read the chant aloud** (both demons): phrases seal. Note any phrase that is rejected
   while chanted honestly; that is calibration input.
3. **Say "Olá meu amigo, como está hoje?" over and over** on phrase I: the karaoke still
   lights, then the cue scoffs (the four lines rotate), the phrase un-lights and its
   sigil strokes flash red, the phone buzzes. The readout shows ✗.
4. **Hum one note** or **hold "aaaa"**: rejected.
5. **Nonsense on phrase II–IV**: rejections keep the focus clock draining; it eventually
   cracks the previous seal (the existing crack cue wins over the scoff while both show).
6. **Flawless:** a ritual with rejections but no crack still ends flawless.
7. **Record clip:** tap it once (Recording · chant), chant, tap twice (Off). Then
   `adb exec-out run-as com.summondemons.safely ls files/chant-clips` lists one WAV.
8. **Background mid-phrase** and return: nothing judged while away, no crash.
9. `./gradlew installRelease` (or the release AAB via internal testing): no Debug
   controls, chanting behaves as in 2–4.

Fix anything that fails before Task 11.

### Task 11: Calibrate with tester clips

The starting numbers come from simulation (ADR-0018). Real voices decide.

- [ ] **Step 1: Record (debug build, with consent)**

Ask each tester (aim for at least 6 people across low, high and child voices, quiet and
noisy rooms) to consent to a voice recording used only for tuning and deleted afterwards.
For each demon, per tester: one **chant** clip (read the four phrases as a player would)
and one **nonsense** clip (small talk in their language, mumbling, humming, counting). Use
the "Record clip" chip: it names files `{demonId}-chant-…wav` / `{demonId}-nonsense-…wav`.

- [ ] **Step 2: Pull the clips (never commit them)**

```bash
mkdir -p app/src/test/chant-clips
adb exec-out run-as com.summondemons.safely tar c files/chant-clips | tar x -C app/src/test/chant-clips --strip-components=2
adb shell run-as com.summondemons.safely rm -r files/chant-clips
git status --short app/src/test/chant-clips   # must print nothing (git-ignored)
```

- [ ] **Step 3: Replay and read the table**

```bash
./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i | grep -E 'phrase|threshold|^  0\.'
```
The table prints honest-chant and nonsense pass rates for thresholds 0.45–0.75 (`*`
marks the current `CHANT_PASS_SCORE`). Targets: chant ≥ 95 %, nonsense ≤ 40 % (lower is
better; ≤ 40 % per phrase already lets < 3 % of nonsense rituals through).

- [ ] **Step 4: Tune, in this order, re-running Step 3 after each change**
1. `CHANT_PASS_SCORE`: the highest value that keeps chant ≥ 95 %.
2. If chant < 95 % at every threshold: raise `VOWEL_MAX_DISTANCE` (0.8 → 1.0), then
   lower `CHANT_RHYTHM_WEIGHT` toward 0 if the rhythm column is what fails honest chants.
3. If nonsense > 40 % at the chosen threshold: lower `VOWEL_MAX_DISTANCE` (0.8 → 0.65).
4. Look at the per-clip lines: a whole tester failing points at their voice range
   (consider adding a scale to `VOWEL_SCALES`); one phrase failing for everyone points
   at its expected vowels (check `ChantJudge.expectedVowels` against how people read it).

Then run the full `./gradlew testDebugUnitTest`: the synthetic tests must still pass. If a
synthetic test fails because of a tuned value, re-check the tuning before touching the test.

- [ ] **Step 5: If the targets can't be met**

Stop tuning. Record the best table in ADR-0018 and follow its deferred alternative:
run the desktop `sherpa-onnx` keyword spotter over the same clips before any Android
work, and ask the product owner how to proceed.

- [ ] **Step 6: Record the result and delete the clips**

Update `RitualConfig` with the chosen values, and in ADR-0018 replace "thresholds pending
measurement" in the Status line with "calibrated 20YY-MM-DD", and add a **Calibration
result** paragraph: date, number of testers and clips, the chosen values, and the chant /
nonsense pass rates. If any number named in PRODUCT.md changed (300 ms, 0.6), update it.
Then delete the clips: `rm -r app/src/test/chant-clips`.

```bash
git add app/src/main/java/com/summondemons/safely/ritual/RitualConfig.kt docs/decisions/0018-chant-plausibility-check.md PRODUCT.md
git commit -q -m "tune(ritual): Calibrate the chant check from tester clips (ADR-0018)" -m "$TRAILER"
```

---

## Phase 5 — Release

### Task 12: Ship it

- [ ] **Step 1:** If 1.0.0 (versionCode 1) is already on Play, bump `versionCode` to 2 and
  `versionName` to `1.1.0` in `app/build.gradle.kts`; otherwise leave them.
- [ ] **Step 2:** Re-publish the updated `docs/PRIVACY_POLICY.md` at the URL given in the
  Play listing, and paste the updated microphone paragraph of `docs/STORE_LISTING.md`
  into the listing. The Data safety answers don't change (still "no data collected":
  audio never leaves the device and the app has no `INTERNET` permission).
- [ ] **Step 3:** `./gradlew bundleRelease` (upload key per ADR-0017), upload to internal
  testing, and repeat Task 10 items 2–4 on the release build.
- [ ] **Step 4:** Commit the version bump if any:

```bash
git add app/build.gradle.kts
git commit -q -m "chore(release): Version 1.1.0, chant check" -m "$TRAILER"
```

---

## Spec coverage

| ADR-0018 item | Task |
|---|---|
| Vowel sequence: LPC formants, median per syllable, expected vowels from spelling, tolerant alignment, vocal-tract scales | 2, 3, 4 |
| Rhythm: breaths, pace | 4, 5 (pauses and span are measured by `ChantTake`) |
| Verdict after the last vowel; `Judged` / `Sealed` / `Rejected` | 5 |
| Rejection is not a crack; clock keeps draining | 5 (`ChantCheckTest`) |
| Check opt-in on state; app always on; Hold to Chant vowels | 5, 7 |
| Scoff: cue line, haptic, red flash | 5 (haptic), 8 |
| Debug recorder (debug source set, release no-op, backup excluded, clips git-ignored) and harness | 9 |
| Targets and calibration; sherpa-onnx fallback | 11 |
| Canon: PRODUCT.md, privacy policy, store listing, ADR-0008 amendment, AGENTS.md | 1, 12 |
| Known limits (one-vowel phrases, phrase I retries, high voices) | documented in ADR-0018; `ChantJudgeTest` / `SyntheticChantTest` encode the one-vowel case |
