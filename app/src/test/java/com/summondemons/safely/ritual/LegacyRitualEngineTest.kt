package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.TICK_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRitualEngineTest {

    private fun run(start: RitualProgress, ticks: Int, aligned: Boolean = true, loud: Boolean = true) =
        (1..ticks).fold(start) { p, _ -> LegacyRitualEngine.step(p, aligned, loud, TICK_MS) }

    @Test
    fun twentyLoudAlignedTicksAdvanceOnePhrase() {
        assertEquals(RitualProgress(phraseIndex = 0, sustainedMs = 1_900), run(RitualProgress(), 19))
        assertEquals(RitualProgress(phraseIndex = 1, sustainedMs = 0), run(RitualProgress(), 20))
    }

    @Test
    fun leavingTheHeadingResetsEverything() {
        val midway = RitualProgress(phraseIndex = 2, sustainedMs = 1_200)
        assertEquals(RitualProgress(), LegacyRitualEngine.step(midway, aligned = false, loud = true, dtMs = TICK_MS))
    }

    @Test
    fun silenceDecaysAtHalfRateAndNeverBelowZero() {
        assertEquals(
            RitualProgress(1, 950),
            LegacyRitualEngine.step(RitualProgress(1, 1_000), aligned = true, loud = false, dtMs = TICK_MS),
        )
        assertEquals(
            RitualProgress(1, 0),
            LegacyRitualEngine.step(RitualProgress(1, 30), aligned = true, loud = false, dtMs = TICK_MS),
        )
    }

    @Test
    fun eightySecondsWorthOfTicksCompletesTheRitual() {
        val done = run(RitualProgress(), 80)
        assertTrue(done.completed)
        assertEquals(4, done.phraseIndex)
    }

    @Test
    fun stepAfterCompletionIsANoOp() {
        val done = RitualProgress(phraseIndex = 4)
        assertEquals(done, LegacyRitualEngine.step(done, aligned = false, loud = false, dtMs = TICK_MS))
    }

    @Test
    fun opacityFollowsProductSteps() {
        assertEquals(listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f), (0..4).map { RitualProgress(it).sigilOpacity })
    }

    @Test
    fun sustainFractionIsClamped() {
        assertEquals(0.5f, RitualProgress(0, 1_000).sustainFraction, 0.001f)
        assertFalse(RitualProgress(0, 0).completed)
    }
}
