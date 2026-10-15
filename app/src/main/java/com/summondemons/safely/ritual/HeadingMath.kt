package com.summondemons.safely.ritual

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/** Smallest absolute difference between two headings, in [0, 180]. */
fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(normalizeDegrees(a) - normalizeDegrees(b))
    return if (diff > 180f) 360f - diff else diff
}

fun isAligned(
    azimuth: Float,
    target: Float,
    tolerance: Float = RitualConfig.HEADING_TOLERANCE_DEG,
): Boolean = angularDistance(azimuth, target) <= tolerance

private val COMPASS_POINTS = listOf(
    "North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West",
)

/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String =
    COMPASS_POINTS[((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size]

/** Exponential low-pass on the unit vector so 359° → 0° doesn't swing through 180°. */
class AzimuthSmoother(private val alpha: Float = 0.15f) {
    private var sinAcc = 0f
    private var cosAcc = 0f
    private var initialized = false

    fun next(degrees: Float): Float {
        val radians = Math.toRadians(degrees.toDouble())
        val s = sin(radians).toFloat()
        val c = cos(radians).toFloat()
        if (initialized) {
            sinAcc += alpha * (s - sinAcc)
            cosAcc += alpha * (c - cosAcc)
        } else {
            sinAcc = s
            cosAcc = c
            initialized = true
        }
        return normalizeDegrees(Math.toDegrees(atan2(sinAcc.toDouble(), cosAcc.toDouble())).toFloat())
    }
}
