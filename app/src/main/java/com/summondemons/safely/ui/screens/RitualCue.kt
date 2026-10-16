package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.turnDirection
import com.summondemons.safely.ui.components.CueTone
import kotlin.math.ceil

/** The Ritual cue chip's rows (§4); the first matching row wins. Pure: the screen maps kinds to copy. */
enum class CueKind(val tone: CueTone) {
    MicDenied(CueTone.Error),
    MicUnavailable(CueTone.Error),
    NoHeading(CueTone.Error),
    Cracked(CueTone.Error),
    TurnLeft(CueTone.Neutral),
    TurnRight(CueTone.Neutral),
    Face(CueTone.Neutral),
    HoldSteady(CueTone.Safe),
    Chant(CueTone.Safe),
    Wavering(CueTone.Error),
    WaveringFocus(CueTone.Error),
    Opening(CueTone.Safe),
}

data class RitualCue(val kind: CueKind, val crackedPhrase: Int? = null)

/** [crackedPhrase] is the phrase that cracked within the last 1.5 s, or null. */
fun ritualCue(state: RitualUiState, crackedPhrase: Int?): RitualCue {
    val ritual = state.ritual
    val kind = when {
        state.micGranted == false -> CueKind.MicDenied
        state.micGranted == true && state.micUnavailable -> CueKind.MicUnavailable
        state.headingUnavailable && !state.simulateHeading -> CueKind.NoHeading
        crackedPhrase != null -> CueKind.Cracked
        else -> when (ritual.phase) {
            RitualPhase.Seeking -> when (turnDirection(state.azimuth, state.demon.ritualHeading)) {
                TurnDirection.Left -> CueKind.TurnLeft
                TurnDirection.Right -> CueKind.TurnRight
                null -> CueKind.Face
            }
            RitualPhase.Locking -> CueKind.HoldSteady
            RitualPhase.Chanting -> CueKind.Chant
            RitualPhase.Wavering -> if (ritual.phrase >= 1) CueKind.WaveringFocus else CueKind.Wavering
            RitualPhase.Climax, RitualPhase.Done -> CueKind.Opening
        }
    }
    return RitualCue(kind, crackedPhrase.takeIf { kind == CueKind.Cracked })
}

/** The chant tablet's right-hand header (§4). */
sealed interface ChantHeader {
    /** "{numeral} / IV" ("0 / IV" before the first seal). */
    data class SealedCount(val count: Int) : ChantHeader

    /** "⏱ {s} s": focus left, rounded up. */
    data class Seconds(val seconds: Int) : ChantHeader

    /** "⏱ 3×": wavering while the clock runs. */
    data object Drifting : ChantHeader
}

fun chantHeader(ritual: RitualState): ChantHeader = when {
    ritual.clockActive && ritual.phase == RitualPhase.Wavering -> ChantHeader.Drifting
    ritual.clockActive -> ChantHeader.Seconds(ceil(ritual.focusMs / 1_000.0).toInt())
    else -> ChantHeader.SealedCount(ritual.phrase.coerceAtMost(PHRASE_COUNT))
}
