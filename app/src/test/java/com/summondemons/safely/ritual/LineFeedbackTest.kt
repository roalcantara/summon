package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** What the chant screen shows after an attempt at a line (ADR-0029). */
class LineFeedbackTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val state = RitualState.start(90f, phrases, mode = CheckMode.Whisper)

    private fun step(vararg effects: RitualEffect) = Step(state, effects.toList())

    @Test
    fun theScoreIsTheShareOfTheLineHeard() {
        assertEquals(40, LineFeedback.score(0.60))
        assertEquals(82, LineFeedback.score(0.18))
        assertEquals(100, LineFeedback.score(-0.2))
        assertEquals(0, LineFeedback.score(1.5))
    }

    @Test
    fun theHeardTextIsLowerCaseLettersAndSpaces() {
        assertEquals("gracia tu pi", LineFeedback.heardText("  Gracia, tu-pi! "))
        assertEquals("", LineFeedback.heardText("..."))
    }

    @Test
    fun aSealShowsItsScore() {
        val f = LineFeedback.next(
            step(RitualEffect.LineChecked(1, true, 0.18), RitualEffect.Sealed(1)), "gratia sit cura", null, nowMs = 1_000,
        )!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertEquals(1, f.phrase)
        assertEquals(82, f.score)
        assertNull("no heard text on a seal", f.heard)
        assertEquals(1_000 + RitualConfig.FEEDBACK_HOLD_MS, f.untilMs)
    }

    @Test
    fun aLenientSealHasNoScore() {
        val f = LineFeedback.next(step(RitualEffect.SyllableLit, RitualEffect.Sealed(0)), null, null, nowMs = 0)!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertNull(f.score)
    }

    @Test
    fun aMissAfterTheWholeLineShowsScoreAndHeard() {
        val f = LineFeedback.next(
            step(RitualEffect.LineChecked(1, false, 0.75), RitualEffect.Retry(1)), "Gracia tu pi.", null, nowMs = 500,
        )!!
        assertEquals(LineFeedback.Kind.Missed, f.kind)
        assertEquals(25, f.score)
        assertEquals("gracia tu pi", f.heard)
    }

    @Test
    fun aMidLineMissShowsNothing() {
        assertNull(LineFeedback.next(step(RitualEffect.LineChecked(1, false, 0.7)), "gratia", null, nowMs = 0))
    }

    @Test
    fun aMissWithAnEmptyTranscriptHasNoHeardText() {
        val f = LineFeedback.next(step(RitualEffect.LineChecked(1, false, 0.9), RitualEffect.Retry(1)), "", null, nowMs = 0)!!
        assertNull(f.heard)
    }

    @Test
    fun feedbackStaysUntilItExpires() {
        val shown = LineFeedback(LineFeedback.Kind.Sealed, 0, 90, null, untilMs = 2_500)
        assertSame(shown, LineFeedback.next(step(), null, shown, nowMs = 2_480))
        assertNull(LineFeedback.next(step(), null, shown, nowMs = 2_500))
    }

    @Test
    fun aNewResultReplacesTheOldOne() {
        val shown = LineFeedback(LineFeedback.Kind.Missed, 1, 25, "x", untilMs = 9_000)
        val f = LineFeedback.next(step(RitualEffect.LineChecked(1, true, 0.1), RitualEffect.Sealed(1)), null, shown, nowMs = 100)!!
        assertEquals(LineFeedback.Kind.Sealed, f.kind)
        assertEquals(90, f.score)
    }
}
