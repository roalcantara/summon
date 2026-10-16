package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Cracked
import com.summondemons.safely.ritual.RitualEffect.Drift
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Resumed
import com.summondemons.safely.ritual.RitualEffect.Sealed
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
import com.summondemons.safely.ritual.RitualPhase.Chanting
import com.summondemons.safely.ritual.RitualPhase.Climax
import com.summondemons.safely.ritual.RitualPhase.Done
import com.summondemons.safely.ritual.RitualPhase.Locking
import com.summondemons.safely.ritual.RitualPhase.Seeking
import com.summondemons.safely.ritual.RitualPhase.Wavering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class RitualEngineTest {

    private val silent = VoiceFrame.SILENT
    private val voiced = VoiceFrame(rms = 3_000.0, voiced = true, onset = false)
    private val onset = VoiceFrame(rms = 3_000.0, voiced = true, onset = true)
    private val on = 0f
    private val off = 90f
    private val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()
    private val climaxFrames = (CLIMAX_MS / FRAME_MS).toInt()
    private val refractoryFrames = (ONSET_REFRACTORY_MS / FRAME_MS).toInt()

    /** The prototype's test phrases: 8, 6, 2 and 3 syllables. */
    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")

    private fun fresh(syllables: List<Int> = listOf(2, 2, 2, 2)) = RitualState(target = 0f, syllables = syllables)

    /** Runs [frames] identical ticks and collects every effect. */
    private fun run(
        start: RitualState,
        frames: Int,
        azimuth: Float? = on,
        voice: VoiceFrame = silent,
        inCourt: Boolean = false,
    ): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(frames) {
            val step = RitualEngine.step(state, RitualInput(azimuth, voice, inCourt))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked(syllables: List<Int> = listOf(2, 2, 2, 2)) = run(fresh(syllables), lockFrames).state

    /** The prototype phrases, locked (optionally while the House is in court). */
    private fun lockedPhrases(inCourt: Boolean = false) =
        run(RitualState.start(0f, phrases), lockFrames, inCourt = inCourt).state

    /** One onset frame, then silence for the refractory period. */
    private fun syllable(state: RitualState): Step {
        val lit = run(state, 1, voice = onset)
        val rest = run(lit.state, refractoryFrames)
        return Step(rest.state, lit.effects + rest.effects)
    }

    /** The prototype's helper: [count] × (silence for the refractory period, then one onset). */
    private fun chant(start: RitualState, count: Int, azimuth: Float? = on): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        repeat(count) {
            val rest = run(state, refractoryFrames, azimuth)
            val lit = run(rest.state, 1, azimuth, onset)
            state = lit.state
            effects += rest.effects + lit.effects
        }
        return Step(state, effects)
    }

    /** Phrase I of the prototype phrases sealed: phrase II's clock just started. */
    private fun firstSealed() = lockedPhrases().let { chant(it, it.syllables[0]).state }

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
    fun canLockIsTrueOnlyWhenAlignedAndLockHoldSatisfied() {
        val s = fresh()
        assertFalse(RitualEngine.canLock(s, null))
        assertFalse(RitualEngine.canLock(s, off))
        assertFalse(RitualEngine.canLock(s, on)) // Seeking with 0 hold: held = 20ms < 600ms

        val locking = s.copy(phase = Locking, lockMs = LOCK_HOLD_MS - FRAME_MS)
        assertTrue(RitualEngine.canLock(locking, on))
        assertFalse(RitualEngine.canLock(locking, off))

        val chanting = s.copy(phase = Chanting)
        assertFalse(RitualEngine.canLock(chanting, on))
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

    @Test
    fun theCourtMultiplierIsFixedAtLock() {
        assertEquals(1f + COURT_BONUS, lockedPhrases(inCourt = true).courtMultiplier, 0.0001f)
        assertEquals(1f, lockedPhrases(inCourt = false).courtMultiplier, 0.0001f)
        // Court leaving after the lock changes nothing.
        val later = run(lockedPhrases(inCourt = true), 10, inCourt = false).state
        assertEquals(1f + COURT_BONUS, later.courtMultiplier, 0.0001f)
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
    fun silenceOnPhraseOnePausesWithoutDecay() {
        val one = syllable(locked(listOf(3, 2, 2, 2))).state
        val later = run(one, 500).state
        assertEquals(Chanting, later.phase)
        assertEquals(1, later.lit)
    }

    @Test
    fun litSyllablesSealAPhrase() {
        val step = syllable(syllable(locked()).state)
        assertEquals(1, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertTrue(Sealed(0) in step.effects)
        assertEquals(0.25f, step.state.inkProgress, 0.0001f)
    }

    @Test
    fun theLastPhraseStartsTheClimax() {
        var state = locked(listOf(1, 1, 1, 1))
        repeat(3) { state = syllable(state).state }
        val step = run(state, 1, voice = onset)
        assertEquals(Climax, step.state.phase)
        assertEquals(listOf(SyllableLit, Sealed(3), ClimaxStart(flawless = true)), step.effects)
        assertEquals(1f, step.state.inkProgress)
    }

    // --- Focus clock (prototype suite) ---

    @Test
    fun prototypePhrasesHaveTheExpectedSyllables() {
        assertEquals(listOf(8, 6, 2, 3), lockedPhrases().syllables)
    }

    @Test
    fun phraseOneHasNoClockAndWaveringOnItDoesNotDrain() {
        val s = lockedPhrases()
        assertEquals(0L, s.focusBudgetMs)
        val wavering = run(s, 500, azimuth = off).state
        assertEquals(Wavering, wavering.phase)
        assertEquals(0, wavering.phrase)
        assertEquals(0L, wavering.focusMs)
    }

    @Test
    fun aSealStartsAFullClockForTheNextPhrase() {
        val start = lockedPhrases()
        val step = chant(start, start.syllables[0])
        assertTrue(Sealed(0) in step.effects)
        assertEquals(1, step.state.phrase)
        assertEquals(focusBudget(step.state.syllables[1], 1), step.state.focusBudgetMs)
        assertEquals(step.state.focusBudgetMs, step.state.focusMs)
    }

    @Test
    fun drainsOnceOnHeadingEvenInSilenceAndThreeTimesOffHeading() {
        val sealed = firstSealed()
        val onHeading = run(sealed, 10).state
        assertEquals(10 * FRAME_MS, sealed.focusMs - onHeading.focusMs)
        val offHeading = run(sealed, 10, azimuth = off).state
        assertEquals(10 * FRAME_MS * FOCUS_DRIFT_MULTIPLIER, sealed.focusMs - offHeading.focusMs)
    }

    @Test
    fun syllablesDoNotCountOffHeadingAndResumingNeedsNoReLock() {
        val drifted = chant(firstSealed(), 2, azimuth = off).state
        assertEquals(0, drifted.lit)
        val back = run(drifted, 1, voice = onset)
        assertEquals(Chanting, back.state.phase)
        assertTrue(Resumed in back.effects)
    }

    @Test
    fun aNullHeadingCountsAsOffHeading() {
        assertEquals(Wavering, run(firstSealed(), 1, azimuth = null).state.phase)
    }

    @Test
    fun onceLockedTheRitualNeverReturnsToSeekingAndWarnsOnce() {
        val step = run(locked(), 500, azimuth = null)
        assertEquals(Wavering, step.state.phase)
        assertEquals(1, step.effects.count { it == Drift })
    }

    @Test
    fun voiceIsIgnoredWhileWavering() {
        val wavering = run(syllable(locked(listOf(3, 2, 2, 2))).state, 1, azimuth = off).state
        assertEquals(1, run(wavering, 20, azimuth = off, voice = onset).state.lit)
    }

    @Test
    fun runningOutCracksTheLastSealClearsThePhraseAndEndsFlawless() {
        val s = chant(firstSealed(), 2).state
        assertEquals(2, s.lit)
        val step = run(s, ceil(s.focusMs.toDouble() / FRAME_MS).toInt() + 1)
        assertTrue(Cracked(0) in step.effects)
        assertEquals(0, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertEquals(0L, step.state.focusBudgetMs) // back on phrase I: no clock
        assertFalse(step.state.flawless)
        assertEquals(0f, step.state.inkProgress)
    }

    @Test
    fun aCrackOnPhraseThreeReopensPhraseTwoWithItsOwnFullBudget() {
        val sealed = firstSealed()
        val s = chant(sealed, sealed.syllables[1]).state
        assertEquals(2, s.phrase)
        val frames = ceil(s.focusMs.toDouble() / FRAME_MS / FOCUS_DRIFT_MULTIPLIER).toInt() + 1
        val step = run(s, frames, azimuth = off)
        assertEquals(1, step.state.phrase)
        assertTrue(Cracked(1) in step.effects)
        assertEquals(focusBudget(s.syllables[1], 1), step.state.focusBudgetMs)
    }

    @Test
    fun aFlawlessRunReachesTheClimaxAndReportsIt() {
        var s = lockedPhrases()
        val effects = mutableListOf<RitualEffect>()
        for (p in 0 until 4) {
            val step = chant(s, s.syllables[p])
            s = step.state
            effects += step.effects
        }
        assertEquals(Climax, s.phase)
        assertTrue(ClimaxStart(flawless = true) in effects)
        assertEquals(0f, s.focusFraction)
        assertEquals(Done, run(s, climaxFrames).state.phase)
    }

    @Test
    fun budgetsAreThreeSecondsPlusHalfASecondPerSyllableScaledInCourt() {
        assertEquals(12_000L, focusBudget(18, 1))
        assertEquals(7_500L, focusBudget(6, 1, 1.25f))
        assertEquals(0L, focusBudget(8, 0))
    }

    // --- Climax and done ---

    private val climaxStart = fresh().copy(phase = Climax, phrase = 4)

    @Test
    fun climaxIgnoresInputAndSummonsOnce() {
        val almost = run(climaxStart, climaxFrames - 1, azimuth = off, voice = onset)
        assertEquals(Climax, almost.state.phase)
        assertTrue(almost.effects.isEmpty())
        assertEquals(1f - 1f / climaxFrames, almost.state.climaxFraction, 0.0001f)

        val done = run(climaxStart, climaxFrames, azimuth = off, voice = onset)
        assertEquals(Done, done.state.phase)
        assertEquals(listOf(Summoned), done.effects)
    }

    @Test
    fun doneIsTerminal() {
        val done = run(climaxStart, climaxFrames).state
        val step = RitualEngine.step(done, RitualInput(on, onset))
        assertSame(done, step.state)
        assertTrue(step.effects.isEmpty())
    }
}
