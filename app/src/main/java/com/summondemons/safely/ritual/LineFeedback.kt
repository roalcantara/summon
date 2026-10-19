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
