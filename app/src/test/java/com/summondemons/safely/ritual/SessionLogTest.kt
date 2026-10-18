package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The debug session log (ADR-0025): what is written replays to the same ritual. */
class SessionLogTest {

    private val phrases = listOf("DAEMONIUM RESIDENTE,", "GRATIA SIT CURA.", "HIC EST", "UT VOTA")
    private val header = SessionHeader("asmodeus", 0f, phrases, mode = "Whisper", startedMs = 1L)

    /** Runs the engine like the ViewModel does and returns the log lines it would write. */
    private fun record(frames: List<VoiceFrame>, heardAt: Map<Int, LineHeard> = emptyMap()): List<String> {
        var state = RitualState.start(0f, phrases, mode = CheckMode.Whisper)
        val lines = mutableListOf(SessionRecords.encode(header))
        frames.forEachIndexed { i, voice ->
            val input = RitualInput(0f, voice, heard = heardAt[i])
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
        val lockFrames = (LOCK_HOLD_MS / FRAME_MS).toInt()
        val frames = List(lockFrames + 50) { VoiceFrame.SILENT }
        val heardAt = mapOf(
            lockFrames + 10 to LineHeard(0, found = false, distance = 0.7, covered = setOf(1)),
            lockFrames + 20 to LineHeard(0, found = true, distance = 0.1, covered = setOf(0, 1)),
        )
        val (h, ticks) = SessionRecords.decode(record(frames, heardAt))
        val replayed = SessionReplay.replay(h, ticks)
        replayed.forEach { r -> assertEquals(r.logged.fx, r.step.effects.map(SessionRecords::effectName)) }
        assertTrue(ticks.any { "Sealed(phrase=0)" in it.fx })
        assertEquals(listOf(false, true), ticks.mapNotNull { it.heard?.found })
    }
}
