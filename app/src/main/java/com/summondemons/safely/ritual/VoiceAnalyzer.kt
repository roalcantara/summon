package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ANALYSIS_RATE
import com.summondemons.safely.ritual.RitualConfig.ONSET_RATIO
import com.summondemons.safely.ritual.RitualConfig.ONSET_REARM_RATIO
import com.summondemons.safely.ritual.RitualConfig.ONSET_WINDOW_FRAMES
import com.summondemons.safely.ritual.RitualConfig.VOICE_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.VOICE_MIN_HZ
import com.summondemons.safely.ritual.RitualConfig.VOICE_RMS_FLOOR
import com.summondemons.safely.ritual.RitualConfig.VOICING_MIN_CORRELATION
import kotlin.math.sqrt

/** One analysed 20 ms mic frame (ADR-0008); [formants] only on voiced frames (ADR-0018). */
data class VoiceFrame(val rms: Double, val voiced: Boolean, val onset: Boolean, val formants: Formants? = null) {
    companion object {
        val SILENT = VoiceFrame(0.0, voiced = false, onset = false)
    }
}

/**
 * Turns consecutive PCM16 frames into [VoiceFrame]s. Stateful (keeps the previous frame
 * and a short RMS history) but pure: no Android types.
 * A new instance is needed when the sample rate changes.
 * - voiced: loud enough and periodic at a voice pitch (70–400 Hz) over the last 40 ms.
 * - onset: a voiced energy rise while armed; re-arms after a dip or an unvoiced frame.
 * - formants: F1/F2 of voiced frames over the same 40 ms window (ADR-0018).
 */
class VoiceAnalyzer(val sampleRate: Int) {
    private val stride = maxOf(1, sampleRate / ANALYSIS_RATE)
    private val minLag = sampleRate / VOICE_MAX_HZ
    private val maxLag = sampleRate / VOICE_MIN_HZ
    private var previous = ShortArray(0)
    private val recent = ArrayDeque<Double>()
    private var armed = true
    private var peak = 0.0
    private val formantEstimator = FormantEstimator(sampleRate)

    fun analyze(samples: ShortArray, count: Int): VoiceFrame {
        if (count <= 0) return VoiceFrame.SILENT
        val n = count.coerceAtMost(samples.size)
        val current = samples.copyOf(n)
        val level = rms(current, n)
        val window = previous + current
        previous = current
        val voiced = level > VOICE_RMS_FLOOR && periodicity(window) >= VOICING_MIN_CORRELATION

        val floor = recent.minOrNull() ?: 0.0
        var onset = false
        if (armed && voiced && level >= ONSET_RATIO * floor) {
            onset = true
            armed = false
            peak = level
        } else if (!armed) {
            peak = maxOf(peak, level)
            if (!voiced || level < ONSET_REARM_RATIO * peak) armed = true
        }
        recent.addLast(level)
        if (recent.size > ONSET_WINDOW_FRAMES) recent.removeFirst()
        return VoiceFrame(level, voiced, onset, if (voiced) formantEstimator.estimate(window) else null)
    }

    /** Highest normalized autocorrelation over the voice-pitch lags, in [0, 1]. */
    private fun periodicity(x: ShortArray): Double {
        val upper = minOf(maxLag, x.size / 2)
        var best = 0.0
        var lag = minLag
        while (lag <= upper) {
            var num = 0.0
            var e1 = 0.0
            var e2 = 0.0
            var i = 0
            while (i < x.size - lag) {
                val a = x[i].toDouble()
                val b = x[i + lag].toDouble()
                num += a * b
                e1 += a * a
                e2 += b * b
                i += stride
            }
            if (e1 > 0.0 && e2 > 0.0) best = maxOf(best, num / sqrt(e1 * e2))
            lag += stride
        }
        return best
    }
}
