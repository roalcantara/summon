package com.summondemons.safely.ritual

/**
 * Debug-only capture of raw mic audio, for calibrating the chant check (ADR-0018). Release
 * builds get [None] from their own source set, so they contain no recording code.
 */
interface ClipRecorder {
    /** Starts a clip named [name]; frames go to it until [stop]. Replaces any clip in progress. */
    fun start(name: String)

    fun write(frame: PcmFrame)

    fun stop()

    object None : ClipRecorder {
        override fun start(name: String) = Unit
        override fun write(frame: PcmFrame) = Unit
        override fun stop() = Unit
    }
}

/** What a debug clip holds; the tag goes into the file name `{demonId}-{tag}-{stamp}.wav`. */
enum class ClipLabel(val tag: String) { Chant("chant"), Nonsense("nonsense") }
