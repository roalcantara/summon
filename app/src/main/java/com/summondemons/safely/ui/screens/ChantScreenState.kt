package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.LineFeedback
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ritual.turnDirection

/** The sigil's tilt toward the demon is clamped to this (ADR-0029). */
const val SIGIL_TILT_MAX_DEG = 35f

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** How one line's seal mark is drawn (ADR-0029 §3.2). */
enum class SealLook { Pending, Current, Sealed, Cracked }

/** What the sigil's outer ring shows (ADR-0020): proximity while seeking, the hold while locking, then the focus clock. */
enum class RingMode { Seeking, Locking, Clock }

/** The direction pill (ADR-0029 §3.3), the only place that names the direction. */
enum class PillLook { Seeking, Facing, Paused, Hidden }

/** [turn] is shown while seeking or paused; null otherwise. */
data class DirectionPill(val look: PillLook, val turn: TurnDirection?)

/** The one slot above the phrase (ADR-0029 §3.5). */
sealed interface SlotContent {
    data object Empty : SlotContent
    data object Meter : SlotContent
    data object Checking : SlotContent
    /** [score] is null in lenient mode. */
    data class Sealed(val score: Int?) : SlotContent
    data class Missed(val score: Int, val heard: String?) : SlotContent
    data object Cracked : SlotContent
}

/** Everything the chant screen draws, derived from the ritual (pure; ADR-0019, ADR-0029). */
data class ChantScreenState(
    val words: List<ChantWord>,
    val facing: Boolean,
    /** Off course after the lock (wavering): paused, amber. */
    val drifting: Boolean,
    /**
     * Where the demon is relative to where the phone faces, in degrees clockwise in
     * (−180, 180]: the marker on the sigil's ring (0 = at the top). Null when the heading is unknown.
     */
    val markerDegrees: Float?,
    val ring: RingMode,
    /** How much of the 0.6 s lock hold is done, 0..1 (only while locking). */
    val lockFraction: Float,
    /** The ring's arc after the lock: focus left, or 1 on phrase I (no clock). */
    val clockFraction: Float,
    val pill: DirectionPill,
    val seals: List<SealLook>,
    /** A Whisper check is running: the current seal breathes faster. */
    val checking: Boolean,
    val slot: SlotContent,
    /** The sigil tile's rotation: toward the demon while seeking or paused, 0 otherwise. */
    val tiltDegrees: Float,
    /** Before the lock: the line is shown but closed. */
    val gated: Boolean,
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

fun chantScreenState(
    ritual: RitualState,
    phrases: List<String>,
    azimuth: Float?,
    target: Float,
    checking: Boolean = false,
    feedback: LineFeedback? = null,
): ChantScreenState {
    val done = ritual.phrase >= PHRASE_COUNT
    val phrase = ritual.phrase.coerceAtMost(PHRASE_COUNT - 1)
    val verdict = ritual.verdict?.takeIf { !done }
    val text = chantWords(phrases[phrase])
    val words = if (done) text.map { ChantWord(it, it.size, WordLook.Heard) } else chantingWords(ritual, text)
    val gated = ritual.phase == RitualPhase.Seeking || ritual.phase == RitualPhase.Locking
    val drifting = ritual.phase == RitualPhase.Wavering
    val marker = azimuth?.let { signedDelta(it, target) }
    val cracked = verdict?.kind == ChantVerdict.Kind.Cracked
    val pillLook = when {
        done || ritual.phase == RitualPhase.Climax || ritual.phase == RitualPhase.Done -> PillLook.Hidden
        drifting -> PillLook.Paused
        ritual.phase == RitualPhase.Seeking -> PillLook.Seeking
        else -> PillLook.Facing
    }
    val pointing = pillLook == PillLook.Seeking || pillLook == PillLook.Paused
    return ChantScreenState(
        words = words,
        facing = ritual.aligned || ritual.phase == RitualPhase.Chanting,
        drifting = drifting,
        markerDegrees = marker,
        ring = when (ritual.phase) {
            RitualPhase.Seeking -> RingMode.Seeking
            RitualPhase.Locking -> RingMode.Locking
            else -> RingMode.Clock
        },
        lockFraction = if (ritual.phase == RitualPhase.Locking) (ritual.lockMs.toFloat() / LOCK_HOLD_MS).coerceIn(0f, 1f) else 0f,
        clockFraction = if (ritual.phrase in 1 until PHRASE_COUNT) ritual.focusFraction else 1f,
        pill = DirectionPill(pillLook, if (pointing) turnDirection(azimuth, target) else null),
        seals = seals(ritual, verdict),
        checking = checking && !done,
        slot = when {
            done || gated -> SlotContent.Empty
            cracked -> SlotContent.Cracked
            feedback?.kind == LineFeedback.Kind.Sealed -> SlotContent.Sealed(feedback.score)
            feedback?.kind == LineFeedback.Kind.Missed -> SlotContent.Missed(feedback.score ?: 0, feedback.heard)
            checking -> SlotContent.Checking
            else -> SlotContent.Meter
        },
        tiltDegrees = if (pointing && marker != null) marker.coerceIn(-SIGIL_TILT_MAX_DEG, SIGIL_TILT_MAX_DEG) else 0f,
        gated = gated,
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

private fun seals(ritual: RitualState, verdict: ChantVerdict?): List<SealLook> = (0 until PHRASE_COUNT).map { p ->
    when {
        verdict?.kind == ChantVerdict.Kind.Cracked && p == verdict.phrase -> SealLook.Cracked
        p < ritual.phrase -> SealLook.Sealed
        p == ritual.phrase -> SealLook.Current
        else -> SealLook.Pending
    }
}
