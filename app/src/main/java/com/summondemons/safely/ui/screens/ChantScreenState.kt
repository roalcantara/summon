package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ritual.turnDirection

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** The one line under the words (ADR-0027): nothing before the lock, the voice meter, or a crack. */
enum class VerdictLine { None, Listening, SealCracked }

/** Step 1 of the status block (ADR-0024): finding the direction, holding it, lost after the lock, or done. */
enum class StepState { Seeking, Aligned, Drifting, Done }

/** How one phrase's seal is drawn (ADR-0024). */
enum class SealLook { Pending, Current, Sealed, Cracked }

/** One seal: its look, the phrase's chunk count, and how many of them have passed. */
data class SealState(val look: SealLook, val parts: Int, val passed: Int)

/** The two-row status block under the sigil (ADR-0024). [turn] is null when aligned or done; [checking] pulses the current seal (ADR-0027). */
data class StatusBlock(val step: StepState, val turn: TurnDirection?, val seals: List<SealState>, val checking: Boolean = false)

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
    /** Step 1 and the four seals (ADR-0024). */
    val status: StatusBlock,
    /** The sigil ring's arc: focus left, or 1 on phrase I (no clock). */
    val clockFraction: Float,
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

fun chantScreenState(ritual: RitualState, phrases: List<String>, azimuth: Float?, target: Float, checking: Boolean = false): ChantScreenState {
    val phrase = ritual.phrase.coerceAtMost(PHRASE_COUNT - 1)
    val verdict = ritual.verdict?.takeIf { ritual.phrase < PHRASE_COUNT }
    val text = chantWords(phrases[phrase])
    val words = if (ritual.phrase >= PHRASE_COUNT) {
        text.map { ChantWord(it, it.size, WordLook.Heard) }
    } else {
        chantingWords(ritual, text)
    }
    val line = when {
        verdict?.kind == ChantVerdict.Kind.Cracked -> VerdictLine.SealCracked
        ritual.phase == RitualPhase.Chanting || ritual.phase == RitualPhase.Wavering -> VerdictLine.Listening
        else -> VerdictLine.None
    }
    val clock = ritual.phrase in 1 until PHRASE_COUNT
    return ChantScreenState(
        words = words,
        facing = ritual.aligned || ritual.phase == RitualPhase.Chanting,
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
        status = statusBlock(ritual, azimuth, target, verdict, checking),
        clockFraction = if (clock) ritual.focusFraction else 1f,
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

private fun statusBlock(ritual: RitualState, azimuth: Float?, target: Float, verdict: ChantVerdict?, checking: Boolean): StatusBlock {
    val done = ritual.phrase >= PHRASE_COUNT
    val step = when {
        done -> StepState.Done
        ritual.phase == RitualPhase.Wavering -> StepState.Drifting
        ritual.aligned || ritual.phase == RitualPhase.Chanting -> StepState.Aligned
        else -> StepState.Seeking
    }
    val seals = (0 until PHRASE_COUNT).map { p ->
        when {
            verdict?.kind == ChantVerdict.Kind.Cracked && p == verdict.phrase -> SealState(SealLook.Cracked, 1, 0)
            p < ritual.phrase -> SealState(SealLook.Sealed, 1, 1)
            p == ritual.phrase -> SealState(SealLook.Current, 1, 0)
            else -> SealState(SealLook.Pending, 1, 0)
        }
    }
    val turn = if (step == StepState.Aligned || step == StepState.Done) null else turnDirection(azimuth, target)
    return StatusBlock(step, turn, seals, checking && !done)
}
