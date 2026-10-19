package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.LineFeedback
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantScreenStateTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val seeking = RitualState.start(90f, phrases, mode = CheckMode.Whisper)
    private val chanting = seeking.copy(phase = RitualPhase.Chanting, azimuth = 90f)

    private fun state(r: RitualState, azimuth: Float? = 90f, checking: Boolean = false, feedback: LineFeedback? = null) =
        chantScreenState(r, phrases, azimuth, 90f, checking, feedback)

    // --- Words (ADR-0019, ADR-0027) ---

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
    }

    @Test
    fun coveredWordsAreHeardInWhisperMode() {
        assertEquals(listOf(WordLook.Current, WordLook.Heard), state(chanting.copy(lit = 2, covered = setOf(1))).words.map { it.look })
    }

    @Test
    fun fullyLitWordsAreHeardInLenientMode() {
        val r = RitualState.start(90f, phrases).copy(phase = RitualPhase.Chanting, azimuth = 90f, lit = 5)
        assertEquals(listOf(WordLook.Heard, WordLook.Current), state(r).words.map { it.look })
    }

    // --- Gate and pill (ADR-0029 §3.3, §3.6) ---

    @Test
    fun beforeTheLockTheLineIsGatedAndThePillPointsTheWay() {
        val s = state(seeking.copy(azimuth = 0f), azimuth = 0f)
        assertTrue(s.gated)
        assertEquals(SlotContent.Locked, s.slot)
        assertEquals(DirectionPill(PillLook.Seeking, TurnDirection.Right), s.pill)
        assertEquals(RingMode.Seeking, s.ring)
        assertFalse(s.facing)
        assertFalse("not paused before the lock", s.drifting)
    }

    @Test
    fun lockingIsStillGatedButFacing() {
        val s = state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L))
        assertTrue(s.gated)
        assertEquals(DirectionPill(PillLook.Facing, null), s.pill)
        assertEquals(RingMode.Locking, s.ring)
        assertEquals(0.5f, s.lockFraction, 0.001f)
        assertEquals(0f, s.tiltDegrees, 0f)
    }

    @Test
    fun chantingOpensTheLineAndListens() {
        val s = state(chanting)
        assertFalse(s.gated)
        assertEquals(SlotContent.Meter, s.slot)
        assertEquals(DirectionPill(PillLook.Facing, null), s.pill)
        assertEquals(RingMode.Clock, s.ring)
        assertEquals("phrase I has no clock: a full ring", 1f, s.clockFraction, 0f)
    }

    @Test
    fun offCourseItPausesPointsBackAndTilts() {
        val s = state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f)
        assertTrue(s.drifting)
        assertFalse(s.gated)
        assertEquals(DirectionPill(PillLook.Paused, TurnDirection.Right), s.pill)
        assertEquals(SIGIL_TILT_MAX_DEG, s.tiltDegrees, 0.01f)
        assertEquals(SlotContent.Meter, s.slot)
    }

    @Test
    fun afterTheFourthSealThePillHidesAndEveryWordIsHeard() {
        val s = state(chanting.copy(phase = RitualPhase.Climax, phrase = 4))
        assertEquals(PillLook.Hidden, s.pill.look)
        assertTrue(s.words.all { it.look == WordLook.Heard })
        assertEquals(SlotContent.Empty, s.slot)
        assertTrue(s.seals.all { it == SealLook.Sealed })
    }

    // --- Sigil (ADR-0020, ADR-0029 §3.4) ---

    @Test
    fun theMarkerPointsAtTheDemonRelativeToThePhone() {
        assertEquals(90f, state(seeking.copy(azimuth = 0f), azimuth = 0f).markerDegrees!!, 0.01f)
        assertEquals(-90f, state(seeking.copy(azimuth = 180f), azimuth = 180f).markerDegrees!!, 0.01f)
        assertNull(state(seeking, azimuth = null).markerDegrees)
    }

    @Test
    fun whileSeekingTheSigilTiltsTowardTheDemonClamped() {
        assertEquals(10f, state(seeking.copy(azimuth = 80f), azimuth = 80f).tiltDegrees, 0.01f)
        assertEquals(-SIGIL_TILT_MAX_DEG, state(seeking.copy(azimuth = 180f), azimuth = 180f).tiltDegrees, 0.01f)
        assertEquals(0f, state(seeking, azimuth = null).tiltDegrees, 0f)
        assertEquals("upright while chanting", 0f, state(chanting.copy(azimuth = 100f), azimuth = 100f).tiltDegrees, 0f)
    }

    // --- Seals (ADR-0029 §3.2) ---

    @Test
    fun sealsFollowTheLines() {
        assertEquals(listOf(SealLook.Sealed, SealLook.Current, SealLook.Pending, SealLook.Pending), state(chanting.copy(phrase = 1)).seals)
        assertTrue(state(chanting, checking = true).checking)
    }

    // --- Feedback slot (ADR-0029 §3.5) ---

    @Test
    fun aRunningCheckShowsChecking() {
        assertEquals(SlotContent.Checking, state(chanting, checking = true).slot)
    }

    @Test
    fun feedbackWinsOverChecking() {
        val sealed = LineFeedback(LineFeedback.Kind.Sealed, 0, 82, null, untilMs = 9_999)
        assertEquals(SlotContent.Sealed(82), state(chanting.copy(phrase = 1), checking = true, feedback = sealed).slot)
    }

    @Test
    fun aMissShowsItsScoreAndWhatWasHeard() {
        val missed = LineFeedback(LineFeedback.Kind.Missed, 1, 25, "gracia tu pi", untilMs = 9_999)
        assertEquals(SlotContent.Missed(25, "gracia tu pi"), state(chanting.copy(phrase = 1), feedback = missed).slot)
    }

    @Test
    fun aCrackWinsOverEverything() {
        val sealed = LineFeedback(LineFeedback.Kind.Sealed, 0, 82, null, untilMs = 9_999)
        val s = state(chanting.copy(verdict = ChantVerdict(ChantVerdict.Kind.Cracked, 0)), checking = true, feedback = sealed)
        assertEquals(SlotContent.Cracked, s.slot)
        assertEquals(SealLook.Cracked, s.seals[0])
    }

    // --- Whole words (ADR-0029) ---

    @Test
    fun aWordBrokenAcrossLinesIsDetected() {
        val text = "DAEMONIUM RESIDENTE,"
        assertTrue("DAEMONIU / M", splitsAWord(text) { if (it < 8) 0 else 1 })
        assertFalse("break at the space", splitsAWord(text) { if (it < 10) 0 else 1 })
        assertFalse("one line", splitsAWord(text) { 0 })
    }
}
