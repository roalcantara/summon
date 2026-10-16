package com.summondemons.safely.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Ritual haptics (spec §7). Degrades to timing-only patterns, or nothing without a vibrator. */
object Haptics {
    private val SUCCESS_TIMINGS = longArrayOf(0, 80, 60, 140)
    private val SUCCESS_AMPLITUDES = intArrayOf(0, 180, 0, 255)
    private val WARN_TIMINGS = longArrayOf(0, 40, 60, 40)
    private val WARN_AMPLITUDES = intArrayOf(0, 200, 0, 200)
    /** Crack: 80 ms on, 50 ms off, 80 ms on, strong (§5). */
    private val CRACK_TIMINGS = longArrayOf(0, 80, 50, 80)
    private val CRACK_AMPLITUDES = intArrayOf(0, 255, 0, 255)

    /** 2 s ramp in ten 200 ms steps (amplitude 40 → 255), a short gap, then a final burst. */
    private val CRESCENDO_TIMINGS = longArrayOf(0) + LongArray(10) { 200 } + longArrayOf(60, 140)
    private val CRESCENDO_AMPLITUDES = intArrayOf(0) + IntArray(10) { 40 + it * 215 / 9 } + intArrayOf(0, 255)

    /** Without amplitude control: pulses that grow while the gaps shrink. */
    private val CRESCENDO_FALLBACK = longArrayOf(0, 30, 170, 50, 150, 80, 120, 120, 80, 160, 40, 200, 60, 140)

    fun tick(context: Context) = predefined(context, VibrationEffect.EFFECT_TICK, fallbackMs = 10)
    fun syllable(context: Context) = predefined(context, VibrationEffect.EFFECT_TICK, fallbackMs = 8)
    fun chime(context: Context) = predefined(context, VibrationEffect.EFFECT_CLICK, fallbackMs = 25)
    fun clunk(context: Context) = predefined(context, VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMs = 40)
    fun warn(context: Context) = waveform(context, WARN_TIMINGS, WARN_AMPLITUDES, WARN_TIMINGS)
    fun crack(context: Context) = waveform(context, CRACK_TIMINGS, CRACK_AMPLITUDES, CRACK_TIMINGS)
    fun crescendo(context: Context) = waveform(context, CRESCENDO_TIMINGS, CRESCENDO_AMPLITUDES, CRESCENDO_FALLBACK)

    /** 80 ms pulse, 60 ms gap, 140 ms burst. */
    fun success(context: Context) = waveform(context, SUCCESS_TIMINGS, SUCCESS_AMPLITUDES, SUCCESS_TIMINGS)

    private fun vibrator(context: Context): Vibrator? {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        return vibrator?.takeIf { it.hasVibrator() }
    }

    private fun predefined(context: Context, effectId: Int, fallbackMs: Long) {
        val vibrator = vibrator(context) ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(fallbackMs)
        }
    }

    private fun waveform(context: Context, timings: LongArray, amplitudes: IntArray, fallback: LongArray) {
        val vibrator = vibrator(context) ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl() ->
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(VibrationEffect.createWaveform(fallback, -1))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(fallback, -1)
        }
    }
}
