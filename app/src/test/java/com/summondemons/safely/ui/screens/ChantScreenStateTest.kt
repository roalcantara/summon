package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.Formants
import com.summondemons.safely.ritual.ChantTake
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.Vowel
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
    fun aHeardWordGetsItsHintOnceItsSyllablesClose() {
        val good = listOf(Vowel.E, Vowel.O, Vowel.I, Vowel.U).map { it.formants() }
        val s = state(chanting.copy(lit = 5, take = ChantTake(nuclei = good)))
        assertEquals(WordLook.Heard, s.words[0].look)
        assertEquals(WordLook.Current, s.words[1].look)
        val bad = List(4) { Vowel.A.formants() }
        assertEquals(WordLook.Doubtful, state(chanting.copy(lit = 5, take = ChantTake(nuclei = bad))).words[0].look)
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
}
