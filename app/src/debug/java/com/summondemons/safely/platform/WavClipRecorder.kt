package com.summondemons.safely.platform

import com.summondemons.safely.ritual.ClipRecorder
import com.summondemons.safely.ritual.PcmFrame
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug builds only (ADR-0018): writes each clip as a mono PCM16 WAV file
 * `{name}-{yyyyMMdd-HHmmss}.wav` in [dir]. The file opens on the first frame and takes
 * its sample rate; frames at another rate are dropped. Thread-safe: frames arrive on
 * the analysis thread, start/stop on the main thread.
 */
class WavClipRecorder(
    private val dir: File,
    private val now: () -> Date = { Date() },
) : ClipRecorder {
    private var name: String? = null
    private var file: RandomAccessFile? = null
    private var rate = 0
    private var dataBytes = 0L

    @Synchronized
    override fun start(name: String) {
        stop()
        this.name = name
    }

    @Synchronized
    override fun write(frame: PcmFrame) {
        val base = name ?: return
        val out = file ?: open(base, frame.sampleRate)
        if (frame.sampleRate != rate || frame.count <= 0) return
        val bytes = ByteBuffer.allocate(frame.count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frame.count) bytes.putShort(frame.samples[i])
        out.write(bytes.array())
        dataBytes += frame.count * 2
    }

    @Synchronized
    override fun stop() {
        file?.let {
            it.seek(0)
            it.write(header(rate, dataBytes))
            it.close()
        }
        file = null
        name = null
        dataBytes = 0L
    }

    private fun open(base: String, sampleRate: Int): RandomAccessFile {
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now())
        rate = sampleRate
        return RandomAccessFile(File(dir, "$base-$stamp.wav"), "rw").also {
            it.setLength(0)
            it.write(header(sampleRate, 0L))
            file = it
        }
    }

    companion object {
        /** The 44-byte RIFF/WAVE header for mono PCM16. */
        fun header(sampleRate: Int, dataBytes: Long): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((36 + dataBytes).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1) // PCM
            putShort(1) // mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataBytes.toInt())
        }.array()
    }
}
