package com.summondemons.safely.ui.fx

import androidx.compose.ui.geometry.Offset

/** Tilt (radians) that moves parallax layers by their full offset. */
const val PARALLAX_FULL_TILT_RAD = 0.35f

/** Maps [x] in [start, end] to [0, 1], clamped. Used to stage the climax. */
fun segment(x: Float, start: Float, end: Float): Float = ((x - start) / (end - start)).coerceIn(0f, 1f)

/** Layer offset for a tilt relative to the resting pose: roll moves x, pitch moves y. */
fun parallaxOffset(pitchDelta: Float, rollDelta: Float, maxPx: Float): Offset = Offset(
    x = (rollDelta / PARALLAX_FULL_TILT_RAD).coerceIn(-1f, 1f) * maxPx,
    y = (pitchDelta / PARALLAX_FULL_TILT_RAD).coerceIn(-1f, 1f) * maxPx,
)

/** Value at [t] in [0, 1] of evenly spaced keyframes [values], linearly interpolated and clamped. */
fun keyframe(t: Float, values: FloatArray): Float {
    require(values.size >= 2) { "need at least two keyframes" }
    val x = t.coerceIn(0f, 1f) * (values.size - 1)
    val i = x.toInt().coerceAtMost(values.size - 2)
    return values[i] + (values[i + 1] - values[i]) * (x - i)
}

/** Like [keyframe], but each segment eases in and out (smoothstep): no velocity break at a keyframe. */
fun smoothKeyframe(t: Float, values: FloatArray): Float {
    require(values.size >= 2) { "need at least two keyframes" }
    val x = t.coerceIn(0f, 1f) * (values.size - 1)
    val i = x.toInt().coerceAtMost(values.size - 2)
    val s = x - i
    return values[i] + (values[i + 1] - values[i]) * s * s * (3f - 2f * s)
}
