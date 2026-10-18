package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormantEstimatorTest {

    /** Formants of a 40 ms window taken from the middle of a 300 ms synthetic vowel. */
    private fun measure(voice: SyntheticVoice, vowel: Vowel, rate: Int = 16_000): Formants? {
        val pcm = voice.vowel(vowel, 300)
        val window = rate * 40 / 1_000
        val start = pcm.size / 2 - window / 2
        return FormantEstimator(rate).estimate(pcm.copyOfRange(start, start + window))
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double, label: String) =
        assertEquals(label, expected, actual, expected * tolerance)

    @Test
    fun findsF1AndF2OfEveryVowelForAMaleVoice() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(f0 = 110.0), v)!!
            assertNear(v.f1, f.f1, 0.15, "$v F1")
            assertNear(v.f2, f.f2, 0.15, "$v F2")
        }
    }

    @Test
    fun findsScaledFormantsForAFemaleVoice() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(f0 = 210.0, scale = 1.2), v)!!
            assertNear(v.f1 * 1.2, f.f1, 0.2, "$v F1")
            assertNear(v.f2 * 1.2, f.f2, 0.2, "$v F2")
        }
    }

    @Test
    fun worksAt44kHz() {
        Vowel.entries.forEach { v ->
            val f = measure(SyntheticVoice(rate = 44_100, f0 = 120.0), v, 44_100)!!
            assertNear(v.f1, f.f1, 0.2, "$v F1")
            assertNear(v.f2, f.f2, 0.2, "$v F2")
        }
    }

    @Test
    fun silenceHasNoFormants() {
        assertNull(FormantEstimator(16_000).estimate(ShortArray(640)))
    }
}
