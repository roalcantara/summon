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
        assertEquals(VerdictLine.ChantAloud, s.verdict)
        assertEquals(0, s.numeral)
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
    fun sealedShowsThePhraseJustSealed() {
        val r = chanting.copy(phrase = 1, focusMs = 9_000, focusBudgetMs = 9_000, verdict = ChantVerdict(ChantVerdict.Kind.Sealed, 0))
        val s = state(r)
        assertEquals(VerdictLine.Sealed, s.verdict)
        assertEquals(0, s.numeral)
        assertTrue(s.clockHeld)
        assertEquals(9, s.clockSeconds)
    }

    @Test
    fun aCrackSaysSo() {
        val r = chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0))
        assertEquals(VerdictLine.SealCracked, state(r).verdict)
    }

    @Test
    fun offCourseItPausesAndPointsTheWay() {
        val s = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertFalse(s.facing)
        assertEquals(TurnDirection.Right, s.turn)
        assertEquals(VerdictLine.Paused, s.verdict)
    }

    @Test
    fun phraseIHasAFullRingAndNoSeconds() {
        val s = state(chanting)
        assertTrue(s.facing)
        assertNull(s.turn)
        assertEquals(1f, s.clockFraction)
        assertNull(s.clockSeconds)
        assertFalse(s.clockHeld)
    }

    @Test
    fun afterTheFourthSealEveryWordIsHeard() {
        val s = state(chanting.copy(phase = RitualPhase.Climax, phrase = 4))
        assertTrue(s.words.all { it.look == WordLook.Heard })
        assertEquals(3, s.numeral)
    }

    // --- Direction in the sigil (ADR-0020) ---

    private val seeking = RitualState.start(90f, phrases, judged = true)

    @Test
    fun beforeTheLockTheLineIsOnlyTheCount() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertEquals(VerdictLine.Count, s.verdict)
        assertEquals(0, s.numeral)
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
        assertEquals(VerdictLine.Count, s.verdict)
    }

    @Test
    fun afterTheLockTheRingIsTheClockAndDriftingIsRed() {
        assertEquals(RingMode.Clock, state(chanting).ring)
        assertEquals(0f, state(chanting).lockFraction, 0f)
        val drift = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertTrue(drift.drifting)
        assertEquals(90f, drift.markerDegrees!!, 0.01f)
    }
}
