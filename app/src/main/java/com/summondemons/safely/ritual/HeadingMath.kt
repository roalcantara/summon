package com.summondemons.safely.ritual

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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

private val COMPASS_LETTERS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

private fun compassIndex(degrees: Float): Int = ((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % COMPASS_POINTS.size

/** Nearest of 8 compass points, e.g. for "face {direction}". */
fun headingLabel(degrees: Float): String = COMPASS_POINTS[compassIndex(degrees)]

/** Abbreviation of [headingLabel] ("N", "NE"…), for the Direction badge glyph. */
fun headingLetter(degrees: Float): String = COMPASS_LETTERS[compassIndex(degrees)]

/**
 * Heading the player faces, in degrees [0, 360), from a rotation matrix as filled by
 * `SensorManager.getRotationMatrix*` (row-major, device → East/North/Up).
 *
 * `SensorManager.getOrientation` only uses the device's top (Y axis), whose horizontal
 * projection vanishes when the phone is held upright. Here the screen's top is used while
 * the phone is closer to flat and the back (−Z axis) while it is closer to upright —
 * whichever has the longer horizontal projection. Without roll both point the same way,
 * so the switch is seamless.
 *
 * [screenRotation] is the display rotation in quarter turns (`Surface.ROTATION_0..270` =
 * 0..3). The back doesn't depend on it; the screen's top does: device +Y in portrait,
 * −X at 90°, −Y at 180°, +X at 270° (the landscape chant screen, ADR-0019).
 */
fun azimuthFromRotationMatrix(r: FloatArray, screenRotation: Int = 0): Float {
    val (topEast, topNorth) = when (screenRotation and 3) {
        1 -> -r[0] to -r[3]
        2 -> -r[1] to -r[4]
        3 -> r[0] to r[3]
        else -> r[1] to r[4]
    }
    val backEast = -r[2]
    val backNorth = -r[5]
    val useBack = hypot(backEast, backNorth) > hypot(topEast, topNorth)
    val east = if (useBack) backEast else topEast
    val north = if (useBack) backNorth else topNorth
    return normalizeDegrees(Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat())
}

/**
 * Holds the last heading for [graceMs] after the display rotation changes (ADR-0025). While the
 * phone is being turned to landscape the rotation reading lags the sensors, and the screen-top
 * axis can point the wrong way for a moment, which used to flip the turn arrow after the lock.
 */
class RotationGrace(private val graceMs: Long = RitualConfig.ROTATION_GRACE_MS) {
    private var rotation: Int? = null
    private var changedAt = Long.MIN_VALUE / 2
    private var last: Float? = null

    fun next(nowMs: Long, rotation: Int, azimuth: Float): Float {
        if (this.rotation != null && rotation != this.rotation) changedAt = nowMs
        this.rotation = rotation
        val held = last
        if (held != null && nowMs - changedAt < graceMs) return held
        last = azimuth
        return azimuth
    }
}

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

/** Signed shortest turn from [from] to [to], in (-180, 180]; positive = clockwise (turn right). */
fun signedDelta(from: Float, to: Float): Float {
    val delta = ((to - from) % 360f + 540f) % 360f - 180f
    return if (delta == -180f) 180f else delta
}

enum class TurnDirection { Left, Right }

/** Which way to turn to face [target]; null when the heading is unknown or already aligned. */
fun turnDirection(azimuth: Float?, target: Float): TurnDirection? = when {
    azimuth == null || isAligned(azimuth, target) -> null
    signedDelta(azimuth, target) > 0f -> TurnDirection.Right
    else -> TurnDirection.Left
}
