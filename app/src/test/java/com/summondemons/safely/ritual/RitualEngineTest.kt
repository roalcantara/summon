package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.DRIFT_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.DriftWarning
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.PhraseComplete
import com.summondemons.safely.ritual.RitualEffect.PhraseLost
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Resumed
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
import com.summondemons.safely.ritual.RitualPhase.Chanting
import com.summondemons.safely.ritual.RitualPhase.Climax
import com.summondemons.safely.ritual.RitualPhase.Done
import com.summondemons.safely.ritual.RitualPhase.Locking
import com.summondemons.safely.ritual.RitualPhase.Seeking
import com.summondemons.safely.ritual.RitualPhase.Wavering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RitualEngineTest {

    private val silent = VoiceFrame.SILENT
    private val voiced = VoiceFrame(rms = 3_000.0, voiced = true, onset = false)
    private val onset = VoiceFrame(rms = 3_000.0, voiced = true, onset = true)
    private val on = 0f
    private val off = 90f
    private val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()
    private val graceFrames = (DRIFT_GRACE_MS / FRAME_MS).toInt()
    private val climaxFrames = (CLIMAX_MS / FRAME_MS).toInt()
    private val refractoryFrames = (ONSET_REFRACTORY_MS / FRAME_MS).toInt()

    private fun fresh(syllables: List<Int> = listOf(2, 2, 2, 2)) = RitualState(target = 0f, syllables = syllables)

    /** Runs [frames] identical ticks and collects every effect. */
    private fun run(start: RitualState, frames: Int, azimuth: Float? = on, voice: VoiceFrame = silent): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(frames) {
            val step = RitualEngine.step(state, RitualInput(azimuth, voice))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked(syllables: List<Int> = listOf(2, 2, 2, 2)) = run(fresh(syllables), lockFrames).state

    /** One onset frame, then silence for the refractory period. */
    private fun syllable(state: RitualState): Step {
        val lit = run(state, 1, voice = onset)
        val rest = run(lit.state, refractoryFrames)
        return Step(rest.state, lit.effects + rest.effects)
    }

    // --- Seeking and locking ---

    @Test
    fun startsSeekingWithNoInk() {
        assertEquals(Seeking, fresh().phase)
        assertEquals(0f, fresh().inkProgress)
    }

    @Test
    fun unknownHeadingStaysSeekingQuietly() {
        val step = run(fresh(), 100, azimuth = null)
        assertEquals(Seeking, step.state.phase)
        assertEquals(emptyList<RitualEffect>(), step.effects)
    }

    @Test
    fun locksAfterExactly600ms() {
        assertEquals(Locking, run(fresh(), lockFrames - 1).state.phase)
        val step = run(fresh(), lockFrames)
        assertEquals(Chanting, step.state.phase)
        assertEquals(1, step.effects.count { it == Locked })
    }

    @Test
    fun leavingBeforeTheLockStartsOver() {
        val holding = run(fresh(), 10).state
        val left = run(holding, 1, azimuth = off)
        assertEquals(Seeking, left.state.phase)
        assertTrue(Locked !in left.effects)
        assertEquals(Locking, run(left.state, lockFrames - 1).state.phase)
    }

    @Test
    fun proximityTicksOnlyWhenGettingCloser() {
        var state = fresh()
        val ticks = listOf(70f, 58f, 57f, 52f, 56f, 50f).map { azimuth ->
            val step = RitualEngine.step(state, RitualInput(azimuth, silent))
            state = step.state
            ProximityTick in step.effects
        }
        assertEquals(listOf(false, true, false, true, false, true), ticks)
    }

    // --- Chanting ---

    @Test
    fun anOnsetLightsASyllable() {
        val step = run(locked(), 1, voice = onset)
        assertEquals(1, step.state.lit)
        assertEquals(listOf(SyllableLit), step.effects)
    }

    @Test
    fun onsetsInsideTheRefractoryPeriodAreIgnored() {
        val first = run(locked(listOf(5, 2, 2, 2)), 1, voice = onset).state
        val tooSoon = run(run(first, refractoryFrames - 2).state, 1, voice = onset).state
        assertEquals(1, tooSoon.lit)
        val inTime = run(run(first, refractoryFrames - 1).state, 1, voice = onset).state
        assertEquals(2, inTime.lit)
    }

    @Test
    fun sustainedVoicingLightsASyllableEvery350ms() {
        val start = locked(listOf(5, 2, 2, 2))
        assertEquals(0, run(start, 17, voice = voiced).state.lit)
        assertEquals(1, run(start, 18, voice = voiced).state.lit)
        assertEquals(2, run(start, 36, voice = voiced).state.lit)
    }

    @Test
    fun silencePausesWithoutDecay() {
        val one = syllable(locked(listOf(3, 2, 2, 2))).state
        val later = run(one, 500).state
        assertEquals(Chanting, later.phase)
        assertEquals(1, later.lit)
    }

    @Test
    fun litSyllablesCompleteAPhrase() {
        val step = syllable(syllable(locked()).state)
        assertEquals(1, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertTrue(PhraseComplete(0) in step.effects)
        assertEquals(0.25f, step.state.inkProgress, 0.0001f)
    }

    @Test
    fun theLastPhraseStartsTheClimax() {
        var state = locked(listOf(1, 1, 1, 1))
        repeat(3) { state = syllable(state).state }
        val step = run(state, 1, voice = onset)
        assertEquals(Climax, step.state.phase)
        assertEquals(listOf(SyllableLit, PhraseComplete(3), ClimaxStart), step.effects)
        assertEquals(1f, step.state.inkProgress)
    }
}
