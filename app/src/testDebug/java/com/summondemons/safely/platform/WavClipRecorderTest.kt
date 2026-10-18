package com.summondemons.safely.platform

import com.summondemons.safely.ritual.PcmFrame
import com.summondemons.safely.ritual.Wav
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Date

class WavClipRecorderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun frame(vararg samples: Short, rate: Int = 16_000) = PcmFrame(shortArrayOf(*samples), samples.size, rate)

    @Test
    fun writesAReadableWavWithTheFirstFramesRate() {
        val recorder = WavClipRecorder(folder.root) { Date(0) }
        recorder.start("asmodeus-chant")
        recorder.write(frame(1, -2, 3))
        recorder.write(frame(4, 5, rate = 44_100)) // another rate: dropped
        recorder.write(frame(-6))
        recorder.stop()

        val file = folder.root.listFiles()!!.single()
        assertTrue(file.name, file.name.startsWith("asmodeus-chant-") && file.name.endsWith(".wav"))
        val wav = Wav.read(file)
        assertEquals(16_000, wav.rate)
        assertArrayEquals(shortArrayOf(1, -2, 3, -6), wav.samples)
    }

    @Test
    fun nothingIsWrittenBeforeStartOrAfterStop() {
        val recorder = WavClipRecorder(folder.root)
        recorder.write(frame(1))
        recorder.start("astaroth-nonsense")
        recorder.stop()
        recorder.write(frame(2))
        assertEquals(0, folder.root.listFiles()!!.size)
    }
}
