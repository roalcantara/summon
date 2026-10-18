package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LineListenerTest {

    private val line = "DAEMONIUM RESIDENTE,"
    private val frame = FloatArray(320)

    /** Answers with [text]; counts calls; optionally waits on [gate] so a check stays in flight. */
    private class FakeTranscriber(var text: String = "Daemonium residente.") : Transcriber {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment> {
            calls++
            gate?.await()
            return listOf(TranscriptSegment(text))
        }
    }

    private fun LineListener.voiced(ms: Long) = repeat((ms / FRAME_MS).toInt()) { onFrame(frame, voiced = true) }
    private fun LineListener.quiet(ms: Long) = repeat((ms / FRAME_MS).toInt()) { onFrame(frame, voiced = false) }

    private fun TestScope.listener(t: FakeTranscriber) = LineListener(t, this, now = { 0L }).also { it.start(0, line) }

    @Test
    fun aPauseAfterEnoughVoiceRunsACheckAndReportsTheLine() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertEquals(1, t.calls)
        val report = l.poll()!!
        assertTrue(report.heard.found)
        assertEquals(0, report.heard.phrase)
        assertNull(l.poll())
    }

    @Test
    fun tooLittleVoiceRunsNoCheck() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(500); l.quiet(1_000)
        advanceUntilIdle()
        assertEquals(0, t.calls)
    }

    @Test
    fun continuousVoicingChecksEverySixSeconds() = runTest {
        val t = FakeTranscriber()
        val l = listener(t)
        l.voiced(6_000)
        advanceUntilIdle()
        assertEquals(1, t.calls)
    }

    @Test
    fun triggersDuringACheckCoalesceIntoOneMore() = runTest {
        val t = FakeTranscriber().apply { gate = CompletableDeferred() }
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertTrue(l.checking)
        repeat(3) { l.voiced(1_000); l.quiet(600) }
        t.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, t.calls)
        assertFalse(l.checking)
    }

    @Test
    fun aRestartDropsTheHeardAudioButKeepsALateHit() = runTest {
        val t = FakeTranscriber().apply { gate = CompletableDeferred() }
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        l.restart()
        l.quiet(600) // no new voice since the restart: nothing to check
        t.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, t.calls)
        assertTrue(l.poll()!!.heard.found) // the check that was running still counts
    }

    @Test
    fun aResultForAnOldLineIsDiscarded() = runTest {
        val t = FakeTranscriber().apply { gate = CompletableDeferred() }
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        l.start(1, "GRATIA SIT CURA.")
        t.gate!!.complete(Unit)
        advanceUntilIdle()
        assertNull(l.poll())
    }

    @Test
    fun nonsenseIsReportedAsNotFound() = runTest {
        val t = FakeTranscriber("Holamio Miku Holamio Miku Holamio Miku")
        val l = listener(t)
        l.voiced(1_000); l.quiet(600)
        advanceUntilIdle()
        assertFalse(l.poll()!!.heard.found)
    }
}
