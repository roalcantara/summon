package com.summondemons.safely.ui.screens

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.RitualUiState
import com.summondemons.safely.ui.components.CueTone
import org.junit.Assert.assertEquals
import org.junit.Test

class RitualCueTest {

    private val demon = DemonCatalog.byId("asmodeus")!! // faces East (90°)
    private val base = RitualUiState(demon = demon, ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases))

    private fun state(
        phase: RitualPhase = RitualPhase.Seeking,
        phrase: Int = 0,
        azimuth: Float? = null,
        micGranted: Boolean? = true,
    ) = base.copy(ritual = base.ritual.copy(phase = phase, phrase = phrase), azimuth = azimuth, micGranted = micGranted)

    private fun kind(s: RitualUiState, cracked: Int? = null) = ritualCue(s, cracked).kind

    @Test
    fun errorsComeFirstInSpecOrder() {
        val all = state(micGranted = false).copy(micUnavailable = true, headingUnavailable = true)
        assertEquals(CueKind.MicDenied, kind(all, cracked = 1))
        assertEquals(CueKind.MicUnavailable, kind(all.copy(micGranted = true), cracked = 1))
        assertEquals(CueKind.NoHeading, kind(state().copy(headingUnavailable = true), cracked = 1))
        assertEquals(CueKind.Face, kind(state().copy(headingUnavailable = true, simulateHeading = true)))
        assertEquals(CueKind.Face, kind(state().copy(micUnavailable = true, micGranted = null)))
    }

    @Test
    fun aCrackShowsUntilItExpires() {
        val cue = ritualCue(state(RitualPhase.Chanting, phrase = 1), crackedPhrase = 1)
        assertEquals(CueKind.Cracked, cue.kind)
        assertEquals(1, cue.crackedPhrase)
        assertEquals(CueTone.Error, cue.kind.tone)
    }

    @Test
    fun seekingTurnsTowardTheTarget() {
        assertEquals(CueKind.TurnRight, kind(state(azimuth = 0f)))
        assertEquals(CueKind.TurnLeft, kind(state(azimuth = 180f)))
        assertEquals(CueKind.Face, kind(state(azimuth = null)))
        assertEquals(CueKind.Face, kind(state(azimuth = 95f)))
        assertEquals(CueTone.Neutral, CueKind.TurnLeft.tone)
    }

    @Test
    fun phasesMapToTheirCues() {
        assertEquals(CueKind.HoldSteady, kind(state(RitualPhase.Locking)))
        assertEquals(CueKind.Chant, kind(state(RitualPhase.Chanting)))
        assertEquals(CueKind.Wavering, kind(state(RitualPhase.Wavering, phrase = 0)))
        assertEquals(CueKind.WaveringFocus, kind(state(RitualPhase.Wavering, phrase = 2)))
        assertEquals(CueKind.Opening, kind(state(RitualPhase.Climax, phrase = 4)))
        assertEquals(CueTone.Safe, CueKind.Chant.tone)
        assertEquals(CueTone.Error, CueKind.WaveringFocus.tone)
    }

    @Test
    fun theHeaderShowsSealsSecondsOrTheDrainRate() {
        val r = base.ritual
        assertEquals(ChantHeader.SealedCount(0), chantHeader(r))
        assertEquals(ChantHeader.SealedCount(1), chantHeader(r.copy(phase = RitualPhase.Seeking, phrase = 1)))
        assertEquals(
            ChantHeader.Seconds(5),
            chantHeader(r.copy(phase = RitualPhase.Chanting, phrase = 1, focusMs = 4_001, focusBudgetMs = 9_000)),
        )
        assertEquals(ChantHeader.Drifting, chantHeader(r.copy(phase = RitualPhase.Wavering, phrase = 2, focusMs = 100)))
        assertEquals(ChantHeader.SealedCount(0), chantHeader(r.copy(phase = RitualPhase.Wavering, phrase = 0)))
        assertEquals(ChantHeader.SealedCount(4), chantHeader(r.copy(phase = RitualPhase.Climax, phrase = 4)))
    }
}
