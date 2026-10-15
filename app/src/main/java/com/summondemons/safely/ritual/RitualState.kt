package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.DRIFT_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT

enum class RitualPhase { Seeking, Locking, Chanting, Wavering, Climax, Done }

/** One 20 ms tick: the latest heading (null = unknown) and the analysed mic frame. */
data class RitualInput(val azimuth: Float?, val voice: VoiceFrame)

/** What happened during a step; the UI turns these into haptics and sounds. */
sealed interface RitualEffect {
    data object ProximityTick : RitualEffect
    data object Locked : RitualEffect
    data object SyllableLit : RitualEffect
    data class PhraseComplete(val phrase: Int) : RitualEffect
    data object DriftWarning : RitualEffect
    data object Resumed : RitualEffect
    data object PhraseLost : RitualEffect
    data object ClimaxStart : RitualEffect
    data object Summoned : RitualEffect
}

data class RitualState(
    val target: Float,
    /** Syllables per phrase: exactly [PHRASE_COUNT] entries, each >= 1. */
    val syllables: List<Int>,
    val phase: RitualPhase = RitualPhase.Seeking,
    val azimuth: Float? = null,
    /** Phrases completed so far (0..4). */
    val phrase: Int = 0,
    /** Syllables lit in the current phrase. */
    val lit: Int = 0,
    /** Time spent in the current Locking, Wavering or Climax phase. */
    val phaseMs: Long = 0L,
    val sinceSyllableMs: Long = ONSET_REFRACTORY_MS,
    val voicedRunMs: Long = 0L,
    /** Last 5 degree proximity bucket while seeking; null when out of range or unknown. */
    val proximityBucket: Int? = null,
) {
    init {
        require(syllables.size == PHRASE_COUNT) { "need exactly $PHRASE_COUNT phrases" }
        require(syllables.all { it >= 1 }) { "every phrase needs at least one syllable" }
    }

    val aligned: Boolean get() = azimuth != null && isAligned(azimuth, target)

    /** Sigil ink in [0, 1] (ADR-0009). */
    val inkProgress: Float
        get() = if (phrase >= PHRASE_COUNT) 1f else (phrase + lit.toFloat() / syllables[phrase]) / PHRASE_COUNT

    val graceFraction: Float
        get() = if (phase == RitualPhase.Wavering) (phaseMs.toFloat() / DRIFT_GRACE_MS).coerceIn(0f, 1f) else 0f

    val climaxFraction: Float
        get() = when (phase) {
            RitualPhase.Climax -> (phaseMs.toFloat() / CLIMAX_MS).coerceIn(0f, 1f)
            RitualPhase.Done -> 1f
            else -> 0f
        }

    companion object {
        fun start(target: Float, phrases: List<String>): RitualState =
            RitualState(target, phrases.map { LatinSyllables.count(it).coerceAtLeast(1) })
    }
}

data class Step(val state: RitualState, val effects: List<RitualEffect> = emptyList())
