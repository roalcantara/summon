package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantJudgeTest {

    private val phrase = "DAEMONIUM RESIDENTE,"
    private val expected = ChantJudge.expectedVowels(phrase)

    /** A take whose nuclei are [vowels] at vocal-tract [scale], at a chanting pace. */
    private fun take(vowels: List<Vowel?>, scale: Double = 1.0, pauses: Int = 0, intervalMs: Long = 300L) =
        ChantTake(
            nuclei = vowels.dropLast(1).map { it?.formants(scale) },
            current = SyllableFormants() + vowels.last()?.formants(scale),
            pauses = pauses,
            litSpanMs = intervalMs * (vowels.size - 1),
        )

    @Test
    fun expectedVowelsFollowTheSpelling() {
        // DAE-MO-NI-UM RE-SI-DEN-TE
        assertEquals(listOf(E, O, I, U, E, I, E, E), expected)
    }

    @Test
    fun diphthongsAndQuAreHandled() {
        // DO-NA FOE-DUS QUOD: AE/OE read as E, the U of QU is a consonant.
        assertEquals(listOf(O, A, E, U, O), ChantJudge.expectedVowels("DONA FOEDUS QUOD"))
        assertEquals(LatinSyllables.count(phrase), expected.size)
    }

    @Test
    fun theRightVowelsScoreFullMarks() {
        val score = ChantJudge.score(expected, take(expected))
        assertEquals(1f, score.vowels, 1e-4f)
        assertEquals(1f, score.rhythm, 1e-4f)
        assertTrue(score.passed)
    }

    @Test
    fun aHigherVoiceStillMatches() {
        assertTrue(ChantJudge.score(expected, take(expected, scale = 1.2)).passed)
        assertTrue(ChantJudge.score(expected, take(expected, scale = 1.4)).passed)
    }

    @Test
    fun oneMissedSyllableStillPasses() {
        val missing = expected.toMutableList().also { it.removeAt(3) }
        val score = ChantJudge.score(expected, take(missing + listOf(E)))
        assertTrue("total=${score.total}", score.passed)
    }

    @Test
    fun unrelatedPortugueseFails() {
        // "Olá meu amigo como está hoje", cut at 8 syllables like the karaoke does.
        val babble = listOf(O, A, E, U, A, I, O, O)
        val score = ChantJudge.score(expected, take(babble))
        assertTrue("total=${score.total}", score.total < CHANT_PASS_SCORE)
    }

    @Test
    fun oneHeldVowelFails() {
        // E is half of this phrase; holding it is close enough to chanting and may pass (ADR-0018).
        (Vowel.entries - E).forEach { v ->
            val score = ChantJudge.score(expected, take(List(expected.size) { v }))
            assertFalse("$v total=${score.total}", score.passed)
        }
    }

    @Test
    fun unmeasuredSyllablesCostHalf() {
        val score = ChantJudge.vowelScore(expected, List(expected.size) { null })
        assertEquals(0.5f, score, 1e-4f)
    }

    @Test
    fun rattlingItOffTooFastHalvesTheRhythm() {
        assertEquals(0.5f, ChantJudge.score(expected, take(expected, intervalMs = 120L)).rhythm, 1e-4f)
    }

    @Test
    fun extraPausesCostRhythm() {
        // 8 syllables allow 1 + 8 / 6 = 2 pauses.
        assertEquals(1f, ChantJudge.score(expected, take(expected, pauses = 2)).rhythm, 1e-4f)
        assertEquals(0.5f, ChantJudge.score(expected, take(expected, pauses = 4)).rhythm, 1e-4f)
    }
}
