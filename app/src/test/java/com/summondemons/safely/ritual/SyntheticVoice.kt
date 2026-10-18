package com.summondemons.safely.ritual

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Test-only source-filter voice: a glottal pulse train at [f0] through cascade resonators
 * at F1–F4 (ADR-0018). Deterministic for a given [seed].
 */
class SyntheticVoice(
    private val rate: Int = 16_000,
    private val f0: Double = 120.0,
    /** Vocal-tract scale: 1.0 adult male, ~1.2 adult female, ~1.4 child. */
    private val scale: Double = 1.0,
    seed: Int = 7,
) {
    private val random = Random(seed)

    /** [ms] of [vowel] (or silence when null) as PCM16, peak ≈ [amplitude], with 20 ms ramps. */
    fun vowel(vowel: Vowel?, ms: Int, amplitude: Double = 8_000.0): ShortArray {
        val n = rate * ms / 1_000
        if (vowel == null) return ShortArray(n)
        val f = vowel.formants(scale)
        return render(n, listOf(f.f1, f.f2, 2_600.0 * scale, 3_400.0 * scale), amplitude)
    }

    /** [ms] of a vowel with explicit formants. */
    fun formants(f1: Double, f2: Double, ms: Int, amplitude: Double = 8_000.0): ShortArray =
        render(rate * ms / 1_000, listOf(f1, f2, 2_600.0 * scale, 3_400.0 * scale), amplitude)

    /** Syllables: [gapMs] of silence, then [vowelMs] of each vowel. */
    fun syllables(vowels: List<Vowel?>, vowelMs: Int = 220, gapMs: Int = 60): ShortArray =
        vowels.map { vowel(null, gapMs) + vowel(it, vowelMs) }.fold(ShortArray(0)) { acc, s -> acc + s }

    private fun render(n: Int, formants: List<Double>, amplitude: Double): ShortArray {
        // Glottal source: pulses every period (±1 % jitter), smoothed by a one-pole low-pass (spectral tilt).
        val source = DoubleArray(n)
        var next = 0.0
        var tilt = 0.0
        for (i in 0 until n) {
            var pulse = 0.0
            if (i >= next) {
                pulse = 1.0
                next += rate / f0 * (1 + (random.nextDouble() - 0.5) * 0.02)
            }
            tilt = 0.9 * tilt + pulse
            source[i] = tilt
        }
        var signal = source
        formants.forEachIndexed { k, freq ->
            if (freq < rate / 2.0 - 200) signal = resonate(signal, freq, BANDWIDTHS[k])
        }
        val peak = signal.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val ramp = rate / 50
        return ShortArray(n) { i ->
            val envelope = minOf(1.0, i / ramp.toDouble(), (n - 1 - i) / ramp.toDouble())
            val noise = (random.nextDouble() - 0.5) * 0.01
            ((signal[i] / peak + noise) * amplitude * envelope).roundToInt().coerceIn(-32_768, 32_767).toShort()
        }
    }

    private fun resonate(x: DoubleArray, freq: Double, bandwidth: Double): DoubleArray {
        val c = -exp(-2 * PI * bandwidth / rate)
        val b = 2 * exp(-PI * bandwidth / rate) * cos(2 * PI * freq / rate)
        val a = 1 - b - c
        val y = DoubleArray(x.size)
        for (i in x.indices) {
            y[i] = a * x[i] + b * (if (i > 0) y[i - 1] else 0.0) + c * (if (i > 1) y[i - 2] else 0.0)
        }
        return y
    }

    private companion object {
        val BANDWIDTHS = listOf(80.0, 100.0, 140.0, 180.0)
    }
}

/** Splits PCM into 20 ms frames, like `AndroidPcmSource`. */
fun ShortArray.frames(rate: Int): List<ShortArray> {
    val size = (rate * RitualConfig.FRAME_MS / 1_000).toInt()
    return (0 until this.size / size).map { copyOfRange(it * size, (it + 1) * size) }
}
