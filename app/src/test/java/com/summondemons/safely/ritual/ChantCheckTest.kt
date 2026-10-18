package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.SEAL_LISTEN_MS
import com.summondemons.safely.ritual.RitualConfig.VERDICT_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_IDLE_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_REJECT_COST
import kotlin.math.roundToLong
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Cracked
import com.summondemons.safely.ritual.RitualEffect.Judged
import com.summondemons.safely.ritual.RitualEffect.Rejected
import com.summondemons.safely.ritual.RitualEffect.Sealed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine with the chant check on (ADR-0018), fed hand-made voice frames. */
class ChantCheckTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()

    private fun frame(vowel: Vowel?, onset: Boolean) =
        if (vowel == null) VoiceFrame.SILENT else VoiceFrame(3_000.0, voiced = true, onset = onset, formants = vowel.formants())

    private fun run(start: RitualState, frames: List<VoiceFrame>): Step {
        var state = start
        val effects = mutableListOf<RitualEffect>()
        frames.forEach {
            val step = RitualEngine.step(state, RitualInput(0f, it))
            state = step.state
            effects += step.effects
        }
        return Step(state, effects)
    }

    private fun locked() = run(RitualState.start(0f, phrases, judged = true), List(lockFrames) { VoiceFrame.SILENT }).state

    /** Each vowel as one 200 ms syllable (onset + 9 voiced frames), then 60 ms of silence. */
    private fun sing(vowels: List<Vowel>): List<VoiceFrame> =
        vowels.flatMap { v -> listOf(frame(v, true)) + List(9) { frame(v, false) } + List(3) { frame(null, false) } }

    private fun expected(phrase: Int) = ChantJudge.expectedVowels(phrases[phrase]).map { it!! }

    @Test
    fun startJudgedBuildsTheChunks() {
        assertNull(RitualState.start(0f, phrases).chunks)
        assertEquals(phrases.map(ChantChunks::of), RitualState.start(0f, phrases, judged = true).chunks)
    }

    @Test
    fun theRightVowelsSealAfterTheLastSyllableEnds() {
        val step = run(locked(), sing(expected(0)))
        assertEquals(1, step.state.phrase)
        assertEquals(0, step.state.lit)
        val judged = step.effects.filterIsInstance<Judged>().single()
        assertTrue(judged.verdict.passed)
        assertEquals(listOf(judged, Sealed(0)), step.effects.filter { it is Judged || it is Sealed })
        assertEquals(ChantTake(), step.state.take)
    }

    @Test
    fun theSealWaitsForTheLastVowel() {
        val vowels = expected(0)
        val upToLastOnset = sing(vowels).take((vowels.size - 1) * 13 + 1)
        val step = run(locked(), upToLastOnset)
        assertEquals(0, step.state.phrase)
        assertEquals(vowels.size, step.state.lit) // every syllable lit…
        assertTrue(step.state.take.closing) // …but still listening
        assertTrue(step.effects.none { it is Sealed })
    }

    @Test
    fun aHeldVowelIsJudgedAfterTheListenWindow() {
        val vowels = expected(0)
        val lastOnset = (vowels.size - 1) * 13
        val listen = (SEAL_LISTEN_MS / FRAME_MS).toInt()
        val held = sing(vowels).take(lastOnset + 1) + List(100) { frame(vowels.last(), false) }
        assertTrue(run(locked(), held.take(lastOnset + listen)).effects.none { it is Sealed })
        assertTrue(Sealed(0) in run(locked(), held.take(lastOnset + listen + 1)).effects)
    }

    @Test
    fun babbleIsRejectedAndThePhraseReopens() {
        val babble = listOf(Vowel.O, Vowel.A, Vowel.E, Vowel.U, Vowel.A, Vowel.I, Vowel.O, Vowel.O)
        val step = run(locked(), sing(babble))
        assertEquals(0, step.state.phrase)
        assertEquals(0, step.state.lit)
        assertFalse(step.effects.filterIsInstance<Judged>().single().verdict.passed)
        assertTrue(Rejected(0, 0) in step.effects)
        assertTrue(step.effects.none { it is Sealed })
        assertTrue("a rejection is not a crack", step.state.flawless)
    }

    @Test
    fun afterARejectionTheRightChantStillSeals() {
        val babble = List(8) { Vowel.A }
        val step = run(locked(), sing(babble) + sing(expected(0)))
        assertEquals(1, step.state.phrase)
        assertEquals(listOf(Rejected(0, 0), Sealed(0)), step.effects.filter { it is Rejected || it is Sealed })
    }

    @Test
    fun aVerdictHoldsTheClockUntilChantingResumesOrTheHoldEnds() {
        val second = run(locked(), sing(expected(0))).state
        val budget = second.focusMs
        assertEquals(ChantVerdict.Kind.Sealed, second.verdict!!.kind)
        // Silence: the clock waits out the rest of VERDICT_HOLD_MS, then drains.
        val holdFrames = ((VERDICT_HOLD_MS - second.verdict!!.heldMs) / FRAME_MS).toInt()
        assertEquals(budget, run(second, List(holdFrames) { VoiceFrame.SILENT }).state.focusMs)
        assertTrue(run(second, List(holdFrames + 5) { VoiceFrame.SILENT }).state.focusMs < budget)
        // A syllable clears the verdict and the clock runs again.
        val chanting = run(second, listOf(frame(Vowel.A, true)) + List(5) { frame(Vowel.A, false) }).state
        assertNull(chanting.verdict)
        assertEquals(budget, chanting.focusMs) // chanting never drains
    }

    @Test
    fun aRejectionKeepsTheDoubtfulWordsAndHoldsTheClock() {
        val second = run(locked(), sing(expected(0)) + List(200) { VoiceFrame.SILENT }).state
        val step = run(second, sing(List(expected(1).size) { Vowel.U }))
        assertTrue(Rejected(1, 0) in step.effects)
        val verdict = step.state.verdict!!
        assertEquals(ChantVerdict.Kind.Rejected, verdict.kind)
        assertTrue(WordMark.Doubtful in verdict.marks)
        assertEquals(second.focusMs - (second.focusBudgetMs * FOCUS_REJECT_COST).roundToLong(), step.state.focusMs)
        val focus = step.state.focusMs
        assertEquals(focus, run(step.state, List(10) { VoiceFrame.SILENT }).state.focusMs)
    }

    @Test
    fun aCrackLeavesACrackedVerdict() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = FRAME_MS, verdict = null, quietMs = FOCUS_IDLE_GRACE_MS)
        val step = run(second, listOf(frame(null, false)))
        assertTrue(Cracked(0) in step.effects)
        assertEquals(ChantVerdict(ChantVerdict.Kind.Cracked, 0), step.state.verdict)
    }

    @Test
    fun aRejectionThatEmptiesFocusCracksAndClearsTheTake() {
        val second = run(locked(), sing(expected(0))).state.copy(focusMs = 1_000L, verdict = null)
        val step = run(second, sing(List(expected(1).size) { Vowel.U }))
        assertTrue(Rejected(1, 0) in step.effects)
        assertTrue(Cracked(0) in step.effects)
        assertEquals(ChantTake(), step.state.take)
        assertEquals(0, step.state.phrase)
    }

    @Test
    fun theWholeRitualWithTheRightVowelsSummons() {
        val all = (0 until 4).flatMap { sing(expected(it)) + List(10) { VoiceFrame.SILENT } }
        val step = run(locked(), all)
        assertEquals(listOf(Sealed(0), Sealed(1), Sealed(2), Sealed(3)), step.effects.filterIsInstance<Sealed>())
        assertTrue(ClimaxStart(flawless = true) in step.effects)
    }

    @Test
    fun withoutTheCheckTheLastOnsetSealsAtOnce() {
        val unjudged = run(RitualState.start(0f, phrases), List(lockFrames) { VoiceFrame.SILENT }).state
        val vowels = expected(0)
        val step = run(unjudged, sing(vowels).take((vowels.size - 1) * 13 + 1))
        assertEquals(listOf(Sealed(0)), step.effects.filterIsInstance<Sealed>())
        assertEquals(ChantTake(), step.state.take)
    }

    @Test
    fun aLongPhraseIsJudgedChunkByChunkAndARejectionReopensOnlyItsChunk() {
        val long = listOf("DAEMONIUM RESIDENTE,", "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", "HIC EST", "UT VOTA")
        val start = run(RitualState.start(0f, long, judged = true), List(lockFrames) { VoiceFrame.SILENT }).state
        val second = run(start, sing(ChantJudge.expectedVowels(long[0]).map { it!! }) + List(200) { VoiceFrame.SILENT }).state
        assertEquals(1, second.phrase)
        val vowels = ChantJudge.expectedVowels(long[1]).map { it!! }

        val first = run(second, sing(vowels.take(8)))
        assertTrue(RitualEffect.ChunkPassed(1, 0) in first.effects)
        assertEquals(1, first.state.chunk)
        assertEquals(8, first.state.lit)

        val wrong = run(first.state, sing(List(6) { Vowel.U }))
        assertTrue(Rejected(1, 1) in wrong.effects)
        assertEquals(1, wrong.state.chunk)
        assertEquals(8, wrong.state.lit)
        assertEquals(WordMark.Heard, wrong.state.verdict!!.marks[0])
        assertEquals(WordMark.Pending, wrong.state.verdict!!.marks[7])

        val rest = run(wrong.state, sing(vowels.drop(8)))
        assertTrue(Sealed(1) in rest.effects)
        assertEquals(2, rest.state.phrase)
        assertEquals(0, rest.state.chunk)
    }
}
