package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualEffect.Judged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End to end (ADR-0018): synthetic PCM → VoiceAnalyzer → RitualEngine with the chant check on. */
class SyntheticChantTest {

    private val phrases = listOf(
        "DAEMONIUM RESIDENTE,",
        "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
        "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
        "UT VOTA OMNIA OPUS EST.",
    )

    private fun judge(pcm: ShortArray, rate: Int = 16_000): List<Judged> = ChantReplay.judge(pcm, rate, phrases)

    private fun vowels(phrase: Int) = ChantJudge.expectedVowels(phrases[phrase])

    @Test
    fun aMaleVoiceChantingPhraseISealsIt() {
        val judged = judge(SyntheticVoice(f0 = 110.0).syllables(vowels(0)))
        assertEquals(1, judged.size)
        assertTrue("verdict=${judged.single().verdict}", judged.single().verdict.passed)
    }

    @Test
    fun aFemaleVoiceChantingPhraseISealsIt() {
        val judged = judge(SyntheticVoice(f0 = 210.0, scale = 1.2).syllables(vowels(0)))
        assertTrue("verdict=${judged.single().verdict}", judged.single().verdict.passed)
    }

    @Test
    fun aChantRecordedAt44kHzSealsIt() {
        val judged = judge(SyntheticVoice(rate = 44_100, f0 = 120.0).syllables(vowels(0)), 44_100)
        assertTrue("verdict=${judged.single().verdict}", judged.single().verdict.passed)
    }

    @Test
    fun theFirstTwoPhrasesInARowBothSeal() {
        val voice = SyntheticVoice(f0 = 130.0)
        val pcm = voice.syllables(vowels(0)) + voice.vowel(null, 600) + voice.syllables(vowels(1))
        val judged = judge(pcm)
        assertEquals(listOf(0, 1), judged.map { it.phrase }.distinct())
        assertEquals(3, judged.count { it.phrase == 1 })
        assertTrue(judged.all { it.verdict.passed })
    }

    @Test
    fun repeatingOlaMeuAmigoIsRejected() {
        // "O-lá meu a-mi-go, co-mo es-tá ho-je?" three times, as in the play-test.
        val ola = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O, Vowel.O, Vowel.E, Vowel.A, Vowel.O, Vowel.E)
        val voice = SyntheticVoice(f0 = 120.0)
        val pcm = List(3) { voice.syllables(ola, vowelMs = 160, gapMs = 50) + voice.vowel(null, 400) }
            .fold(ShortArray(0)) { acc, s -> acc + s }
        val judged = judge(pcm)
        assertTrue("judged ${judged.size}", judged.size >= 3)
        assertTrue("verdicts=${judged.map { it.verdict }}", judged.any { !it.verdict.passed })
    }

    @Test
    fun holdingOneVowelNeverSealsPhraseI() {
        Vowel.entries.forEach { v ->
            val judged = judge(SyntheticVoice(f0 = 120.0).syllables(List(16) { v }))
            assertTrue("$v verdicts=${judged.map { it.verdict }}", judged.isNotEmpty() && judged.none { it.verdict.passed })
        }
    }
}
