package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ALIGN_OPTIONAL_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.NEUTRAL_VOWEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frame-level alignment (ADR-0022). Hand-made frames: each vowel held for [k] 20 ms frames. */
class ChantAlignerTest {

    private val dae = ChantTemplate.slots("DAEMONIUM RESIDENTE,") // E O I(glide) U(neutral) E I E E(neutral)
    private val ostende = ChantTemplate.slots("OSTENDE MIHI FACIEM") // O E E I I A I E

    private fun frames(vowels: List<Vowel>, k: Int = 6, scale: Double = 1.0): List<Formants?> =
        vowels.flatMap { v -> List(k) { v.formants(scale) } }

    private fun vowels(slots: List<VowelSlot>) = slots.map { it.vowel!! }

    @Test
    fun theRightVowelsAlignAtZeroCost() {
        val a = ChantAligner.align(ostende, frames(vowels(ostende)))
        assertEquals(0.0, a.cost, 1e-9)
        assertTrue(a.slotFrames.all { it >= 3 })
        assertEquals(0, a.skippedFrames)
    }

    @Test
    fun aHigherVoiceAlignsAtItsScale() {
        assertEquals(0.0, ChantAligner.align(ostende, frames(vowels(ostende), scale = 1.2)).cost, 1e-9)
    }

    @Test
    fun shortVowelsStillFillEverySlot() {
        val a = ChantAligner.align(dae, frames(vowels(dae), k = 4))
        assertEquals(0.0, a.cost, 1e-9)
        assertTrue(a.slotFrames.all { it >= 3 })
    }

    @Test
    fun aSkippedGlideCostsLittle() {
        val spoken = vowels(dae).filterIndexed { i, _ -> i != 2 } // "DAE-MO-NYUM": no separate I
        val a = ChantAligner.align(dae, frames(spoken))
        assertNull(a.slotCosts[2])
        assertEquals(ALIGN_OPTIONAL_SKIP_COST / dae.size, a.cost, 1e-6)
    }

    @Test
    fun aNeutralEndingCountsAsHeard() {
        val spoken = frames(vowels(dae).dropLast(1)) + List(6) { NEUTRAL_VOWEL }
        assertEquals(0.0, ChantAligner.align(dae, spoken).cost, 1e-9)
    }

    @Test
    fun aStrayFrameBetweenVowelsIsSkipped() {
        val v = vowels(ostende)
        // A sits 0.65 from O and 0.74 from E: skipping it (0.6) is cheaper than joining either slot.
        val spoken = frames(v.take(1)) + listOf(Vowel.A.formants()) + frames(v.drop(1))
        val a = ChantAligner.align(ostende, spoken)
        assertEquals(1, a.skippedFrames)
        assertTrue(a.cost < 0.1)
    }

    @Test
    fun wrongVowelsCostMore() {
        assertTrue(ChantAligner.align(ostende, frames(List(8) { Vowel.A })).cost > 0.4)
    }

    @Test
    fun nothingHeardSkipsEverySlot() {
        val a = ChantAligner.align(ostende, emptyList())
        assertTrue(a.slotCosts.all { it == null })
        assertTrue(a.cost > 0.6)
    }

    // --- Verdict: shuffle baseline, talk veto, word marks ---

    private val seed = "OSTENDE MIHI FACIEM"

    @Test
    fun theRightOrderBeatsEveryShuffleAndPasses() {
        val v = ChantAligner.judge(ostende, frames(vowels(ostende)), seed)
        assertTrue(v.passed)
        assertEquals(1.0, v.rank!!, 1e-9)
        assertTrue(v.shuffles >= 8)
        assertTrue(!v.talk)
    }

    @Test
    fun aHeldVowelTiesWithEveryShuffleAndFails() {
        listOf(Vowel.A, Vowel.E, Vowel.I).forEach { held ->
            val v = ChantAligner.judge(ostende, frames(List(8) { held }), seed)
            assertTrue("$held passed", !v.passed)
            assertEquals("$held rank", 0.5, v.rank!!, 1e-9)
        }
    }

    @Test
    fun babbleFails() {
        val babble = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O)
        assertTrue(!ChantAligner.judge(dae, frames(babble, k = 10), "DAEMONIUM RESIDENTE").passed)
    }

    @Test
    fun conversationalSpeedIsVetoed() {
        val fast = ChantAligner.judge(ostende, frames(vowels(ostende), k = 3), seed)
        assertTrue(fast.talk)
        assertTrue(!fast.passed)
        assertTrue(!ChantAligner.judge(ostende, frames(vowels(ostende), k = 5), seed).talk)
    }

    @Test
    fun aOneVowelChunkFallsBackToTheCeiling() {
        val estMe = ChantTemplate.slots("EST ME")
        val right = ChantAligner.judge(estMe, frames(listOf(Vowel.E, Vowel.E)), "EST ME")
        assertNull(right.rank)
        assertEquals(0, right.shuffles)
        assertTrue(right.passed)
        assertTrue(!ChantAligner.judge(estMe, frames(listOf(Vowel.A, Vowel.A)), "EST ME").passed)
    }

    @Test
    fun shufflesAreDeterministicAndMoveWholeSlots() {
        val a = ChantAligner.shuffles(dae, "DAEMONIUM RESIDENTE")
        assertEquals(a, ChantAligner.shuffles(dae, "DAEMONIUM RESIDENTE"))
        a.forEach { order -> assertEquals(dae.map { it.copy(word = 0) }.sortedBy { it.toString() }, order.map { it.copy(word = 0) }.sortedBy { it.toString() }) }
        a.forEach { order -> assertEquals(dae.map { it.word }, order.map { it.word }) }
    }

    @Test
    fun aWrongWordIsMarkedDoubtful() {
        val v = vowels(ostende)
        val spoken = v.take(3) + listOf(Vowel.U, Vowel.U) + v.drop(5) // MIHI sung as "MU-HU"
        val verdict = ChantAligner.judge(ostende, frames(spoken), seed)
        assertEquals(listOf(WordMark.Heard, WordMark.Doubtful, WordMark.Heard), verdict.marks)
    }
}
