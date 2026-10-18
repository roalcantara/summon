package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.Vowel.A
import com.summondemons.safely.ritual.Vowel.E
import com.summondemons.safely.ritual.Vowel.I
import com.summondemons.safely.ritual.Vowel.O
import com.summondemons.safely.ritual.Vowel.U
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChantTemplateTest {

    @Test
    fun daemoniumHasAGlideAndNeutralEndings() {
        val slots = ChantTemplate.slots("DAEMONIUM RESIDENTE,")
        assertEquals(listOf(E, O, I, U, E, I, E, E), slots.map { it.vowel })
        assertEquals(listOf(false, false, true, false, false, false, false, false), slots.map { it.optional })
        assertEquals(listOf(false, false, false, true, false, false, false, true), slots.map { it.neutral })
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1, 1), slots.map { it.word })
    }

    @Test
    fun oneSyllableWordsHaveNoNeutralEnding() {
        assertTrue(ChantTemplate.slots("HIC EST ME").none { it.neutral })
    }

    @Test
    fun quAndDiphthongsAreNotGlides() {
        assertTrue(ChantTemplate.slots("QUOD FOEDUS").none { it.optional })
    }

    @Test
    fun iOrUBeforeAnotherVowelIsAGlide() {
        assertEquals(listOf(false, true, false), ChantTemplate.slots("GRATIA").map { it.optional })
        assertEquals(listOf(true, false), ChantTemplate.slots("TUI").map { it.optional })
        assertEquals(listOf(A, I, A), ChantTemplate.slots("GRATIA").map { it.vowel })
        assertEquals(listOf(U, I), ChantTemplate.slots("TUI").map { it.vowel })
        assertEquals(listOf(O), ChantTemplate.slots("QUOD").map { it.vowel })
    }
}
