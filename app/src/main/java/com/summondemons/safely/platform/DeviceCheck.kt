package com.summondemons.safely.platform

import android.content.Context
import android.os.SystemClock
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.ritual.RitualConfig.DEVICE_MAX_CHECK_MS
import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlin.random.Random

/**
 * Once per install (ADR-0027): can this phone run Whisper fast enough? Loads the model and times
 * a 5 s transcription; the result is cached. Until it has run, rituals use [CheckMode.Lenient].
 */
object DeviceCheck {
    private const val PREFS = "device_check"
    private const val KEY_MODE = "mode_v1"
    private const val KEY_REASON = "reason_v1"

    data class Result(val mode: CheckMode, val reason: String)

    fun cached(context: Context): Result? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, null) ?: return null
        return Result(CheckMode.valueOf(mode), prefs.getString(KEY_REASON, "") ?: "")
    }

    /** Blocking; run off the main thread. */
    fun runOnce(context: Context): Result {
        cached(context)?.let { return it }
        val result = try {
            val transcriber = WhisperTranscriber.get(context)
            val random = Random(5)
            val pcm = FloatArray(5 * WHISPER_RATE) { (random.nextFloat() - 0.5f) * 0.02f }
            val t0 = SystemClock.elapsedRealtime()
            transcriber.transcribeNow(pcm)
            val ms = SystemClock.elapsedRealtime() - t0
            if (ms <= DEVICE_MAX_CHECK_MS) Result(CheckMode.Whisper, "${ms} ms") else Result(CheckMode.Lenient, "slow: ${ms} ms")
        } catch (e: Throwable) {
            Result(CheckMode.Lenient, "load: ${e.javaClass.simpleName}")
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, result.mode.name).putString(KEY_REASON, result.reason).apply()
        return result
    }
}
