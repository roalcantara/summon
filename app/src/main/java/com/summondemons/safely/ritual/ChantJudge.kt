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
