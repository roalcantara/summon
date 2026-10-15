package com.summondemons.safely.ritual

import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

/** Cold flow of device azimuths in degrees [0, 360). Collecting starts the sensors. */
fun interface HeadingSource {
    fun azimuths(): Flow<Float>

    /** False when the device has no usable heading sensor (the ritual then can't start). */
    val available: Boolean get() = true
}

/** One mono PCM16 mic frame of ~[RitualConfig.FRAME_MS]. */
class PcmFrame(val samples: ShortArray, val count: Int, val sampleRate: Int)

/** Cold flow of mic frames. Collecting opens the mic; the flow completes if it can't. */
fun interface PcmSource {
    fun frames(): Flow<PcmFrame>
}

/** Cold flow of microphone RMS, one value per ~[RitualConfig.TICK_MS] window. Collecting opens the mic. */
fun interface VolumeSource {
    fun rmsLevels(): Flow<Double>
}

fun rms(samples: ShortArray, count: Int): Double {
    if (count <= 0) return 0.0
    var sum = 0.0
    for (i in 0 until count) {
        val s = samples[i].toDouble()
        sum += s * s
    }
    return sqrt(sum / count)
}
