package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Test

class ChantJudgeTest {

    private val phrase = "DAEMONIUM RESIDENTE,"
    private val expected = ChantJudge.expectedVowels(phrase)

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
    fun aVowelIsAtZeroDistanceFromItselfAtItsScale() {
        assertEquals(0.0, ChantJudge.distance(E.formants(1.2), E, 1.2), 1e-9)
    }

    @Test
    fun distantVowelsCapAtOne() {
        assertEquals(1.0, ChantJudge.distance(U.formants(), A, 1.0), 1e-9)
    }

    @Test
    fun unmeasuredCostsHalf() {
        assertEquals(CHANT_UNMEASURED_COST, ChantJudge.distance(null, A, 1.0), 1e-9)
    }
}
