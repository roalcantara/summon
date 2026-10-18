package com.summondemons.safely.platform

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

/**
 * Device spike (ADR-0027): model load time, check latency for 3/5/10 s of audio with and without
 * a shrunk encoder context, and native memory. If `spike.wav` (mono PCM16 16 kHz) is pushed to the
 * app's external files dir, it is transcribed too. Read results with `adb logcat -s WhisperSpike`.
 */
@RunWith(AndroidJUnit4::class)
class WhisperSpikeTest {

    private val tag = "WhisperSpike"

    @Test
    fun measure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = WhisperModelFile.ensure(context)
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val heapBefore = Debug.getNativeHeapAllocatedSize()
        val loadStart = SystemClock.elapsedRealtime()
        val handle = WhisperNative.init(model.path)
        val loadMs = SystemClock.elapsedRealtime() - loadStart
        assertTrue("model failed to load", handle != 0L)
        Log.i(tag, "load ${loadMs} ms, native heap +${(Debug.getNativeHeapAllocatedSize() - heapBefore) / 1_000_000} MB, threads $threads")
        try {
            for (seconds in listOf(3, 5, 10)) {
                for (shrink in listOf(true, false)) {
                    val pcm = noise(seconds)
                    val t0 = SystemClock.elapsedRealtime()
                    val segments = WhisperNative.transcribe(handle, pcm, threads, shrink)
                    Log.i(tag, "noise ${seconds}s shrink=$shrink: ${SystemClock.elapsedRealtime() - t0} ms, segments $segments")
                }
            }
            val wav = File(context.getExternalFilesDir(null), "spike.wav")
            if (wav.exists()) {
                val pcm = readWav(wav)
                for (shrink in listOf(true, false)) {
                    val t0 = SystemClock.elapsedRealtime()
                    val n = WhisperNative.transcribe(handle, pcm, threads, shrink)
                    val text = (0 until maxOf(n, 0)).joinToString(" ") { String(WhisperNative.segmentText(handle, it), Charsets.UTF_8) }
                    Log.i(tag, "spike.wav ${pcm.size / 16_000.0}s shrink=$shrink: ${SystemClock.elapsedRealtime() - t0} ms: $text")
                }
            }
            Log.i(tag, "native heap peak-ish ${Debug.getNativeHeapAllocatedSize() / 1_000_000} MB")
        } finally {
            WhisperNative.free(handle)
        }
    }

    private fun noise(seconds: Int): FloatArray {
        val random = Random(seconds)
        return FloatArray(seconds * 16_000) { (random.nextFloat() - 0.5f) * 0.02f }
    }

    /** Mono PCM16 WAV → floats; assumes 16 kHz (the debug clip recorder's rate on this phone). */
    private fun readWav(file: File): FloatArray {
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        bytes.position(12)
        while (bytes.remaining() >= 8) {
            val id = ByteArray(4).also { bytes.get(it) }.toString(Charsets.US_ASCII)
            val size = bytes.int
            if (id == "data") return FloatArray(size / 2) { bytes.short / 32_768f }
            bytes.position(bytes.position() + size)
        }
        error("no data chunk")
    }
}
