package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Replays audio through the real pipeline (VoiceAnalyzer → RitualEngine, chant check on, always on heading). */
object ChantReplay {

    /** Every [RitualEffect.Judged] the ritual produces while hearing [pcm] (plus 1 s of trailing silence). */
    fun judge(pcm: ShortArray, rate: Int, phrases: List<String>): List<RitualEffect.Judged> {
        var state = RitualState.start(0f, phrases, judged = true)
        repeat((LOCK_HOLD_MS / FRAME_MS).toInt()) { state = RitualEngine.step(state, RitualInput(0f, VoiceFrame.SILENT)).state }
        val analyzer = VoiceAnalyzer(rate)
        val judged = mutableListOf<RitualEffect.Judged>()
        (pcm + ShortArray(rate)).frames(rate).forEach { frame ->
            val step = RitualEngine.step(state, RitualInput(0f, analyzer.analyze(frame, frame.size)))
            state = step.state
            judged += step.effects.filterIsInstance<RitualEffect.Judged>()
        }
        return judged
    }

    /** One judged chunk with the voiced frames it was judged on (for offline tuning, ADR-0025). */
    data class Take(val judged: RitualEffect.Judged, val frames: List<Formants?>, val slots: List<VowelSlot>)

    /** Like [judge], but also returns each chunk's frames; the last frame of the take is included. */
    fun takes(pcm: ShortArray, rate: Int, phrases: List<String>): List<Take> {
        var state = RitualState.start(0f, phrases, judged = true)
        repeat((LOCK_HOLD_MS / FRAME_MS).toInt()) { state = RitualEngine.step(state, RitualInput(0f, VoiceFrame.SILENT)).state }
        val analyzer = VoiceAnalyzer(rate)
        val out = mutableListOf<Take>()
        (pcm + ShortArray(rate)).frames(rate).forEach { frame ->
            val voice = analyzer.analyze(frame, frame.size)
            val before = state
            val step = RitualEngine.step(state, RitualInput(0f, voice))
            state = step.state
            step.effects.filterIsInstance<RitualEffect.Judged>().forEach { j ->
                val frames = before.take.frames + if (voice.voiced) listOf(voice.formants) else emptyList()
                out += Take(j, frames, before.chunks!![j.phrase][j.chunk].slots)
            }
        }
        return out
    }
}

/** A mono PCM16 WAV file. */
class Wav(val rate: Int, val samples: ShortArray) {
    companion object {
        /** Reads the `fmt ` and `data` chunks; rejects anything but mono 16-bit PCM. */
        fun read(file: File): Wav {
            val raw = file.readBytes()
            require(raw.size >= 12 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") { "$file: not a WAV file" }
            val bytes = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
            bytes.position(12)
            var rate = 0
            while (bytes.remaining() >= 8) {
                val id = ByteArray(4).also { bytes.get(it) }.toString(Charsets.US_ASCII)
                val size = bytes.int
                when (id) {
                    "fmt " -> {
                        val format = bytes.short.toInt()
                        val channels = bytes.short.toInt()
                        rate = bytes.int
                        bytes.position(bytes.position() + 6)
                        val bits = bytes.short.toInt()
                        require(format == 1 && channels == 1 && bits == 16) { "$file: need mono 16-bit PCM" }
                        bytes.position(bytes.position() + size - 16)
                    }
                    "data" -> {
                        val n = minOf(size, bytes.remaining()) / 2
                        return Wav(rate, ShortArray(n) { bytes.short })
                    }
                    else -> bytes.position(bytes.position() + size)
                }
            }
            error("$file: no data chunk")
        }
    }
}
