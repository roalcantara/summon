package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.WordMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantScreenStateTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val chanting = RitualState.start(90f, phrases, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f)

    private fun state(r: RitualState, azimuth: Float? = 90f) = chantScreenState(r, phrases, azimuth, 90f)

    @Test
    fun wordsKeepTheirSyllablesAndPunctuation() {
        assertEquals(listOf(listOf("DAE", "MO", "NI", "UM"), listOf("RE", "SI", "DE", "NTE,")), chantWords(phrases[0]))
        assertEquals(listOf(listOf("GRA", "TI", "A"), listOf("SIT"), listOf("CU", "RA.")), chantWords(phrases[1]))
    }

    @Test
    fun theWordBeingChantedIsCurrentWithItsLitSyllables() {
        val s = state(chanting.copy(lit = 2))
        assertEquals(WordLook.Current, s.words[0].look)
        assertEquals(2, s.words[0].lit)
        assertEquals(WordLook.Pending, s.words[1].look)
        assertEquals(VerdictLine.Listening, s.verdict)
    }

    @Test
    fun wordsOfPassedChunksAreHeard() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val r = RitualState.start(90f, long, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f, phrase = 1, chunk = 1, lit = 8)
        val s = chantScreenState(r, long, 90f, 90f)
        assertEquals(List(3) { WordLook.Heard } + WordLook.Current, s.words.take(4).map { it.look })
        assertEquals(WordLook.Pending, s.words[4].look)
    }

    @Test
    fun aRejectionShowsTheDoubtfulWordsUntilChantingResumes() {
        val r = chanting.copy(lit = 0, verdict = ChantVerdict(ChantVerdict.Kind.Rejected, 0, listOf(WordMark.Heard, WordMark.Doubtful)))
        val s = state(r)
        assertEquals(listOf(WordLook.Heard, WordLook.Rejected), s.words.map { it.look })
        assertEquals(VerdictLine.ChantAgain, s.verdict)
    }

    @Test
    fun aSealedPhraseLightsItsSealAndTheLineKeepsListening() {
        val r = chanting.copy(phrase = 1, verdict = ChantVerdict(ChantVerdict.Kind.Sealed, 0))
        val s = state(r)
        assertEquals(VerdictLine.Listening, s.verdict)
        assertEquals(SealLook.Sealed, s.status.seals[0].look)
        assertEquals(SealLook.Current, s.status.seals[1].look)
        assertEquals(SealLook.Pending, s.status.seals[2].look)
    }

    @Test
    fun aCrackSaysSoAndMarksItsSeal() {
        val s = state(chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0)))
        assertEquals(VerdictLine.SealCracked, s.verdict)
        assertEquals(SealLook.Cracked, s.status.seals[0].look)
    }

    @Test
    fun offCourseItPausesAndPointsTheWay() {
        val s = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertFalse(s.facing)
        assertEquals(TurnDirection.Right, s.turn)
        assertEquals(VerdictLine.Listening, s.verdict)
        assertEquals(StepState.Drifting, s.status.step)
        assertEquals(TurnDirection.Right, s.status.turn)
    }

    @Test
    fun phraseIHasAFullRingAndStepOneIsDone() {
        val s = state(chanting)
        assertTrue(s.facing)
        assertNull(s.turn)
        assertEquals(1f, s.clockFraction)
        assertEquals(StepState.Aligned, s.status.step)
        assertNull(s.status.turn)
    }

    @Test
    fun afterTheFourthSealEveryWordIsHeard() {
        val s = state(chanting.copy(phase = RitualPhase.Climax, phrase = 4))
        assertTrue(s.words.all { it.look == WordLook.Heard })
        assertEquals(StepState.Done, s.status.step)
        assertTrue(s.status.seals.all { it.look == SealLook.Sealed })
    }

    // --- Direction in the sigil (ADR-0020) ---

    private val seeking = RitualState.start(90f, phrases, judged = true)

    @Test
    fun beforeTheLockTheLineIsEmptyAndStepOneSeeks() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertEquals(VerdictLine.None, s.verdict)
        assertEquals(StepState.Seeking, s.status.step)
        assertEquals(RingMode.Seeking, s.ring)
        assertFalse(s.facing)
        assertFalse("not an error before the lock", s.drifting)
    }

    @Test
    fun theMarkerPointsAtTheDemonRelativeToThePhone() {
        // Demon at 90° (East): facing North puts him a quarter turn clockwise, facing South a quarter turn back.
        assertEquals(90f, state(seeking.copy(azimuth = 0f), azimuth = 0f).markerDegrees!!, 0.01f)
        assertEquals(-90f, state(seeking.copy(azimuth = 180f), azimuth = 180f).markerDegrees!!, 0.01f)
        assertEquals(0f, state(seeking.copy(azimuth = 90f), azimuth = 90f).markerDegrees!!, 0.01f)
        assertNull(state(seeking, azimuth = null).markerDegrees)
    }

    @Test
    fun lockingFillsTheRingAndShowsOnlyTheCount() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(VerdictLine.None, s.verdict)
    }

    @Test
    fun afterTheLockTheRingIsTheClockAndDriftingIsRed() {
        assertEquals(RingMode.Clock, state(chanting).ring)
        assertEquals(0f, state(chanting).lockFraction, 0f)
        val drift = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertTrue(drift.drifting)
        assertEquals(90f, drift.markerDegrees!!, 0.01f)
    }

    @Test
    fun theCurrentSealCountsItsPassedParts() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val r = RitualState.start(90f, long, judged = true).copy(phase = RitualPhase.Chanting, azimuth = 90f, phrase = 1, chunk = 1, lit = 8)
        val seal = chantScreenState(r, long, 90f, 90f).status.seals[1]
        assertEquals(SealState(SealLook.Current, parts = 3, passed = 1), seal)
    }
}
