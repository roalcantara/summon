package com.summondemons.safely.ritual

import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

/** Cold flow of device azimuths in degrees [0, 360). Collecting starts the sensors. */
fun interface HeadingSource {
    fun azimuths(): Flow<Float>
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
