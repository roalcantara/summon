package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlin.math.floor

/** Mic PCM16 at [rate] → 16 kHz floats in [-1, 1] for Whisper, by linear interpolation (ADR-0027). */
class Resampler16k(val rate: Int) {
    private val step = rate.toDouble() / WHISPER_RATE
    /** Position of the next output sample, in input samples relative to the frame being converted. */
    private var pos = 0.0
    private var last = 0f

    fun convert(samples: ShortArray, count: Int): FloatArray {
        if (rate == WHISPER_RATE) return FloatArray(count) { samples[it] / 32_768f }
        if (count <= 0) return FloatArray(0)
        val out = FloatArray(((count - pos) / step).toInt() + 2)
        var k = 0
        while (pos < count - 1 && k < out.size) {
            val i = floor(pos).toInt()
            val f = (pos - i).toFloat()
            val a = if (i < 0) last else samples[i] / 32_768f
            val b = samples[i + 1] / 32_768f
            out[k++] = a + (b - a) * f
            pos += step
        }
        pos -= count
        last = samples[count - 1] / 32_768f
        return out.copyOf(k)
    }
}
