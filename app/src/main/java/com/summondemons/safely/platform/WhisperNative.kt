package com.summondemons.safely.platform

/** JNI bindings to `libchantwhisper.so` (whisper.cpp, ADR-0027). Not thread-safe: callers serialise. */
internal object WhisperNative {
    init {
        System.loadLibrary("chantwhisper")
    }

    /** A context handle, or 0 if the model can't be loaded. */
    external fun init(modelPath: String): Long

    /** Transcribes 16 kHz mono [pcm] in Latin; returns the segment count, or -1 on failure. */
    external fun transcribe(handle: Long, pcm: FloatArray, threads: Int, shrinkContext: Boolean): Int

    /** UTF-8 bytes of segment [index] of the last transcription. */
    external fun segmentText(handle: Long, index: Int): ByteArray

    external fun segmentNoSpeech(handle: Long, index: Int): Float

    external fun free(handle: Long)
}
