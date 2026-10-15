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
