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
) {
    init {
        require(syllables.size == PHRASE_COUNT) { "need exactly $PHRASE_COUNT phrases" }
        require(syllables.all { it >= 1 }) { "every phrase needs at least one syllable" }
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
        fun start(target: Float, phrases: List<String>): RitualState =
            RitualState(target, phrases.map { LatinSyllables.count(it).coerceAtLeast(1) })
    }
}

/** Focus budget in ms for phrase index [phrase] with [syllables] syllables (ADR-0010); none for phrase I. */
fun focusBudget(syllables: Int, phrase: Int, courtMultiplier: Float = 1f): Long =
    if (phrase <= 0) 0L else ((FOCUS_BASE_MS + FOCUS_PER_SYLLABLE_MS * syllables) * courtMultiplier).roundToLong()

data class Step(val state: RitualState, val effects: List<RitualEffect> = emptyList())
