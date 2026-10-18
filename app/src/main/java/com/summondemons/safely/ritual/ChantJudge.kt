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
