package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualEffect.Drift
import com.summondemons.safely.ritual.RitualEffect.LineChecked
import com.summondemons.safely.ritual.RitualEffect.Resumed
import com.summondemons.safely.ritual.RitualEffect.Sealed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine in Whisper mode (ADR-0027): lines seal on LineHeard, not on lit syllables. */
class WhisperModeTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val onset = VoiceFrame(3_000.0, voiced = true, onset = true)
    private val silent = VoiceFrame.SILENT

    private fun run(start: RitualState, frames: Int, azimuth: Float? = 0f, voice: VoiceFrame = silent, heard: LineHeard? = null): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(frames) { i ->
            val step = RitualEngine.step(state, RitualInput(azimuth, voice, heard = if (i == 0) heard else null))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked(mode: CheckMode) = run(RitualState.start(0f, phrases, mode = mode), (LOCK_HOLD_MS / FRAME_MS).toInt()).state

    /** [count] syllables: an onset, then silence for the refractory period. */
    private fun syllables(start: RitualState, count: Int): RitualState {
        var s = start
        repeat(count) {
            s = run(s, 1, voice = onset).state
            s = run(s, (ONSET_REFRACTORY_MS / FRAME_MS).toInt()).state
        }
        return s
    }

    @Test
    fun lightingEverySyllableDoesNotSealInWhisperMode() {
        val s = syllables(locked(CheckMode.Whisper), 8)
        assertEquals(0, s.phrase)
        assertEquals(8, s.lit)
    }

    @Test
    fun chantingPastTheEndStartsTheLineOver() {
        val full = syllables(locked(CheckMode.Whisper), 8).copy(covered = setOf(0))
        val again = run(full, 1, voice = onset)
        assertTrue(RitualEffect.Retry(0) in again.effects)
        assertEquals(1, again.state.lit)
        assertEquals(emptySet<Int>(), again.state.covered)
        val rested = run(again.state, (ONSET_REFRACTORY_MS / FRAME_MS).toInt()).state
        assertEquals(4, syllables(rested, 3).lit)
    }

    @Test
    fun aMissAfterTheWholeLineClearsItForAnotherTry() {
        val full = syllables(locked(CheckMode.Whisper), 8)
        val step = run(full, 1, heard = LineHeard(0, found = false, distance = 0.65, covered = setOf(1)))
        assertTrue(RitualEffect.Retry(0) in step.effects)
        assertEquals(0, step.state.lit)
        assertEquals(emptySet<Int>(), step.state.covered)
        assertEquals(0, step.state.phrase)
    }

    @Test
    fun aMissMidLineKeepsTheLitSyllables() {
        val partial = syllables(locked(CheckMode.Whisper), 3)
        val step = run(partial, 1, heard = LineHeard(0, found = false, distance = 0.65, covered = emptySet()))
        assertTrue(step.effects.none { it is RitualEffect.Retry })
        assertEquals(3, step.state.lit)
    }

    @Test
    fun babbleTurnsNoWordGreen() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(0, found = false, distance = 0.86, covered = setOf(0)))
        assertEquals(emptySet<Int>(), step.state.covered)
    }

    @Test
    fun aFoundLineSealsWithAVerdictAndAFullTimerNext() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(0, found = true, distance = 0.1, covered = setOf(0, 1)))
        assertTrue(LineChecked(0, true, 0.1) in step.effects)
        assertTrue(Sealed(0) in step.effects)
        assertEquals(1, step.state.phrase)
        assertEquals(step.state.focusBudgetMs, step.state.focusMs)
        assertEquals(emptySet<Int>(), step.state.covered)
        assertEquals(ChantVerdict.Kind.Sealed, step.state.verdict!!.kind)
    }

    @Test
    fun aMissAddsCoverageAndKeepsListening() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(0, found = false, distance = 0.7, covered = setOf(1)))
        assertTrue(LineChecked(0, false, 0.7) in step.effects)
        assertEquals(0, step.state.phrase)
        assertEquals(setOf(1), step.state.covered)
    }

    @Test
    fun aResultForAnotherLineIsIgnored() {
        val step = run(locked(CheckMode.Whisper), 1, heard = LineHeard(2, found = true, distance = 0.0, covered = emptySet()))
        assertTrue(step.effects.none { it is LineChecked || it is Sealed })
    }

    @Test
    fun lenientModeSealsWhenEverySyllableLit() {
        val s = syllables(locked(CheckMode.Lenient), 8)
        assertEquals(1, s.phrase)
    }

    @Test
    fun onceLockedTheRitualWaversOnlyBeyondTwentyFiveDegreesAndResumesWithinTwenty() {
        val chanting = locked(CheckMode.Whisper)
        assertEquals(RitualPhase.Chanting, run(chanting, 5, azimuth = 22f).state.phase)
        val wavering = run(chanting, 1, azimuth = 26f)
        assertTrue(Drift in wavering.effects)
        assertEquals(RitualPhase.Wavering, run(wavering.state, 5, azimuth = 22f).state.phase)
        val back = run(wavering.state, 1, azimuth = 19f)
        assertTrue(Resumed in back.effects)
        assertEquals(RitualPhase.Chanting, back.state.phase)
    }
}
