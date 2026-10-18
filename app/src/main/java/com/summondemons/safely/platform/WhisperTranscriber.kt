package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.Transcriber
import com.summondemons.safely.ritual.TranscriptSegment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Whisper base in Latin through whisper.cpp (ADR-0027). The model loads once per process. */
class WhisperTranscriber private constructor(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Transcriber {
    private val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    override suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment> = withContext(dispatcher) {
        lock.withLock { transcribeNow(pcm) }
    }

    /** Blocking; callers hold [lock] or run before any ritual (the device check). */
    internal fun transcribeNow(pcm: FloatArray): List<TranscriptSegment> {
        val h = handle()
        val n = WhisperNative.transcribe(h, pcm, threads, SHRINK_CONTEXT)
        if (n <= 0) return emptyList()
        return (0 until n).map { i ->
            TranscriptSegment(String(WhisperNative.segmentText(h, i), Charsets.UTF_8), WhisperNative.segmentNoSpeech(h, i))
        }
    }

    private fun handle(): Long = synchronized(Companion) {
        if (loaded == 0L) {
            loaded = WhisperNative.init(WhisperModelFile.ensure(context).path)
            check(loaded != 0L) { "Whisper model failed to load" }
        }
        loaded
    }

    companion object {
        /** From the device spike (Task 1): true unless the spike showed shrunk context garbles text. */
        private const val SHRINK_CONTEXT = true
        private val lock = Mutex()
        private var loaded = 0L
        @Volatile
        private var instance: WhisperTranscriber? = null

        fun get(context: Context): WhisperTranscriber =
            instance ?: synchronized(this) { instance ?: WhisperTranscriber(context.applicationContext).also { instance = it } }
    }
}
