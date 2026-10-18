package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The debug session log (ADR-0025): what is written replays to the same ritual. */
class SessionLogTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val header = SessionHeader("asmodeus", 0f, phrases, judged = true, startedMs = 1L)

    private fun frame(vowel: Vowel?, onset: Boolean) =
        if (vowel == null) VoiceFrame.SILENT else VoiceFrame(3_000.0, voiced = true, onset = onset, formants = vowel.formants())

    private fun sing(vowels: List<Vowel?>): List<VoiceFrame> =
        vowels.flatMap { v -> listOf(frame(v, true)) + List(9) { frame(v, false) } + List(3) { frame(null, false) } }

    /** Runs the engine like the ViewModel does and returns the log lines it would write. */
    private fun record(frames: List<VoiceFrame>): List<String> {
        var state = RitualState.start(0f, phrases, judged = true)
        val lines = mutableListOf(SessionRecords.encode(header))
        frames.forEachIndexed { i, voice ->
            val input = RitualInput(0f, voice)
            val step = RitualEngine.step(state, input)
            state = step.state
            lines += SessionRecords.encode(SessionRecords.tick(i * FRAME_MS, input, step, hold = false))
        }
        lines += SessionRecords.encode(SessionTick(t = frames.size * FRAME_MS, note = "intro:closed"))
        return lines
    }

    @Test
    fun aHeaderAndTicksRoundTrip() {
        val tick = SessionTick(t = 40, az = 91.5f, rms = 3_000.0, v = true, o = true, f1 = 480.0, f2 = 1_850.0, ph = "Chanting", p = 1, c = 2, lit = 9, fm = 59_000, fx = listOf("SyllableLit"))
        val (h, ticks) = SessionRecords.decode(listOf(SessionRecords.encode(header), SessionRecords.encode(tick)))
        assertEquals(header, h)
        assertEquals(listOf(tick), ticks)
    }

    @Test
    fun aLoggedSessionReplaysToTheSameEffects() {
        val lock = List((LOCK_HOLD_MS / FRAME_MS).toInt()) { VoiceFrame.SILENT }
        val wrong = sing(List(8) { Vowel.A })
        val right = sing(ChantJudge.expectedVowels(phrases[0]))
        val (h, ticks) = SessionRecords.decode(record(lock + wrong + right))
        val replayed = SessionReplay.replay(h, ticks)
        assertEquals(ticks.count { it.isTick }, replayed.size)
        replayed.forEach { r -> assertEquals(r.logged.fx, r.step.effects.map(SessionRecords::effectName)) }
        val judged = ticks.mapNotNull { it.judged }
        assertEquals(listOf(false, true), judged.map { it.passed })
        assertTrue(ticks.any { it.note == "intro:closed" })
    }
}
