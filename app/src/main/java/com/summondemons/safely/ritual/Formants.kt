package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FORMANT_F1_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_F1_MIN_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_F2_MAX_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_MAX_BANDWIDTH_HZ
import com.summondemons.safely.ritual.RitualConfig.FORMANT_RATE
import com.summondemons.safely.ritual.RitualConfig.LPC_ORDER
import com.summondemons.safely.ritual.RitualConfig.PRE_EMPHASIS
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

/** The first two formants of a voiced sound, in Hz (ADR-0018). */
data class Formants(val f1: Double, val f2: Double)

/**
 * The formants measured on the frames of one syllable. Immutable: [plus] returns a new list.
 * The median resists the odd frame where the estimator locks onto a pitch harmonic.
 */
data class SyllableFormants(val frames: List<Formants> = emptyList()) {
    operator fun plus(f: Formants?): SyllableFormants = if (f == null) this else SyllableFormants(frames + f)

    /** Null when no frame of the syllable had measurable formants. */
    fun median(): Formants? {
        if (frames.isEmpty()) return null
        return Formants(median(frames.map { it.f1 }), median(frames.map { it.f2 }))
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}

/**
 * Estimates F1 and F2 from a PCM16 window by linear prediction (ADR-0018). Pure, no Android
 * types; one instance per sample rate. The window is low-pass filtered and decimated to
 * about [FORMANT_RATE], pre-emphasised, Hamming-windowed, fitted with an order-[LPC_ORDER]
 * all-pole model (autocorrelation + Levinson-Durbin), and the model's roots give the
 * resonances. Returns null when no plausible F1/F2 pair is found.
 */
class FormantEstimator(val sampleRate: Int) {
    private val decimation = maxOf(1, sampleRate / FORMANT_RATE)
    private val rate = sampleRate.toDouble() / decimation
    private val lowPass = if (decimation > 1) lowPassTaps(decimation) else doubleArrayOf(1.0)

    fun estimate(samples: ShortArray, count: Int = samples.size): Formants? {
        val x = emphasise(decimate(samples, count.coerceAtMost(samples.size)))
        if (x.size <= LPC_ORDER * 2) return null
        val a = lpc(x) ?: return null
        val resonances = roots(a)
            .filter { it.second < FORMANT_MAX_BANDWIDTH_HZ }
            .map { it.first }
            .sorted()
        val f1 = resonances.firstOrNull { it in FORMANT_F1_MIN_HZ..FORMANT_F1_MAX_HZ } ?: return null
        val f2 = resonances.firstOrNull { it > f1 && it <= FORMANT_F2_MAX_HZ } ?: return null
        return Formants(f1, f2)
    }

    private fun decimate(samples: ShortArray, n: Int): DoubleArray {
        if (decimation == 1) return DoubleArray(n) { samples[it].toDouble() }
        val half = lowPass.size / 2
        return DoubleArray(n / decimation) { k ->
            val center = k * decimation
            var acc = 0.0
            for (t in lowPass.indices) {
                val i = center + t - half
                if (i in 0 until n) acc += lowPass[t] * samples[i]
            }
            acc
        }
    }

    /** Pre-emphasis then a Hamming window. */
    private fun emphasise(x: DoubleArray): DoubleArray {
        val n = x.size
        return DoubleArray(n) { i ->
            val e = x[i] - if (i > 0) PRE_EMPHASIS * x[i - 1] else 0.0
            e * (0.54 - 0.46 * cos(2 * PI * i / (n - 1)))
        }
    }

    /** Prediction polynomial [1, a1..ap] by Levinson-Durbin; null for silence or an unstable fit. */
    private fun lpc(x: DoubleArray): DoubleArray? {
        val p = LPC_ORDER
        val r = DoubleArray(p + 1) { lag ->
            var s = 0.0
            for (i in 0 until x.size - lag) s += x[i] * x[i + lag]
            s
        }
        if (r[0] <= 0.0) return null
        r[0] *= 1.0 + 1e-9 // white-noise floor keeps the recursion stable
        val a = DoubleArray(p + 1).also { it[0] = 1.0 }
        var error = r[0]
        for (i in 1..p) {
            var acc = r[i]
            for (j in 1 until i) acc += a[j] * r[i - j]
            val k = -acc / error
            val previous = a.copyOf()
            for (j in 1 until i) a[j] = previous[j] + k * previous[i - j]
            a[i] = k
            error *= 1 - k * k
            if (error <= 0.0) return null
        }
        return a
    }

    /** (frequency, bandwidth) in Hz of each upper-half-plane root of the polynomial (Durand-Kerner). */
    private fun roots(a: DoubleArray): List<Pair<Double, Double>> {
        val p = a.size - 1
        val re = DoubleArray(p) { k -> ROOT_SEED_RADIUS * cos(ROOT_SEED_ANGLE * k + 0.25) }
        val im = DoubleArray(p) { k -> ROOT_SEED_RADIUS * sin(ROOT_SEED_ANGLE * k + 0.25) }
        for (iteration in 0 until ROOT_ITERATIONS) {
            var moved = 0.0
            for (i in 0 until p) {
                // Numerator: polynomial value at root i (Horner).
                var nr = 1.0
                var ni = 0.0
                for (c in 1..p) {
                    val tr = nr * re[i] - ni * im[i] + a[c]
                    ni = nr * im[i] + ni * re[i]
                    nr = tr
                }
                // Denominator: product of (z_i - z_j).
                var dr = 1.0
                var di = 0.0
                for (j in 0 until p) {
                    if (j == i) continue
                    val xr = re[i] - re[j]
                    val xi = im[i] - im[j]
                    val tr = dr * xr - di * xi
                    di = dr * xi + di * xr
                    dr = tr
                }
                val mag = dr * dr + di * di
                if (mag == 0.0) continue
                val qr = (nr * dr + ni * di) / mag
                val qi = (ni * dr - nr * di) / mag
                re[i] -= qr
                im[i] -= qi
                moved = maxOf(moved, hypot(qr, qi))
            }
            if (moved < ROOT_TOLERANCE) break
        }
        return (0 until p).filter { im[it] > 0.0 }.map { i ->
            val radius = hypot(re[i], im[i])
            val frequency = atan2(im[i], re[i]) * rate / (2 * PI)
            val bandwidth = -ln(radius) * rate / PI
            frequency to bandwidth
        }
    }

    private companion object {
        const val ROOT_ITERATIONS = 200
        const val ROOT_TOLERANCE = 1e-10
        const val ROOT_SEED_RADIUS = 0.9
        const val ROOT_SEED_ANGLE = 2 * PI / 10.3

        /** Windowed-sinc low-pass at 0.45 × the decimated rate. */
        fun lowPassTaps(decimation: Int): DoubleArray {
            val n = 12 * decimation + 1
            val cutoff = 0.45 / decimation // cycles per input sample
            val m = n / 2
            val taps = DoubleArray(n) { i ->
                val t = (i - m).toDouble()
                val sinc = if (t == 0.0) 2 * cutoff else sin(2 * PI * cutoff * t) / (PI * t)
                sinc * (0.54 - 0.46 * cos(2 * PI * i / (n - 1)))
            }
            val sum = taps.sum()
            return DoubleArray(n) { taps[it] / sum }
        }
    }
}
