package com.summondemons.safely.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    private val TIMINGS = longArrayOf(0, 80, 60, 140)
    private val AMPLITUDES = intArrayOf(0, 180, 0, 255)

    /** 80 ms pulse, 60 ms gap, 140 ms burst. */
    fun success(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(TIMINGS, AMPLITUDES, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(TIMINGS, -1)
        }
    }
}
