package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RitualStateTest {

    @Test
    fun startCountsSyllablesPerPhrase() {
        val demon = DemonCatalog.byId("astaroth")!!
        val state = RitualState.start(demon.ritualHeading, demon.chantPhrases)
        assertEquals(listOf(7, 6, 7, 8), state.syllables)
        assertEquals(RitualPhase.Seeking, state.phase)
        assertEquals(0f, state.inkProgress)
    }

    @Test
    fun inkProgressIsContinuous() {
        val state = RitualState(target = 0f, syllables = listOf(4, 2, 2, 2), phrase = 0, lit = 1)
        assertEquals(1f / 16f, state.inkProgress, 0.0001f)
        assertEquals(0.5f, state.copy(phrase = 2, lit = 0).inkProgress, 0.0001f)
        assertEquals(1f, state.copy(phrase = 4, lit = 0).inkProgress, 0.0001f)
    }

    @Test
    fun alignmentUsesTheTarget() {
        val state = RitualState(target = 0f, syllables = listOf(1, 1, 1, 1))
        assertFalse(state.aligned)
        assertTrue(state.copy(azimuth = 350f).aligned)
        assertFalse(state.copy(azimuth = 30f).aligned)
    }

    @Test
    fun fractionsOnlyApplyInTheirPhase() {
        val state = RitualState(target = 0f, syllables = listOf(1, 1, 1, 1))
        assertEquals(0f, state.focusFraction)
        assertEquals(0.25f, state.copy(focusMs = 1_000, focusBudgetMs = 4_000).focusFraction, 0.0001f)
        assertEquals(0.5f, state.copy(phase = RitualPhase.Climax, climaxMs = 1_250).climaxFraction, 0.0001f)
        assertEquals(1f, state.copy(phase = RitualPhase.Done).climaxFraction)
    }

    @Test
    fun theClockRunsOnPhrasesTwoToFourWhileChantingOrWavering() {
        val state = RitualState(target = 0f, syllables = listOf(1, 1, 1, 1))
        assertFalse(state.copy(phase = RitualPhase.Chanting, phrase = 0).clockActive)
        assertTrue(state.copy(phase = RitualPhase.Chanting, phrase = 1).clockActive)
        assertTrue(state.copy(phase = RitualPhase.Wavering, phrase = 3).clockActive)
        assertFalse(state.copy(phase = RitualPhase.Climax, phrase = 4).clockActive)
        assertFalse(state.copy(phase = RitualPhase.Locking, phrase = 2).clockActive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun needsFourPhrases() {
        RitualState(target = 0f, syllables = listOf(1, 1, 1))
    }
}
