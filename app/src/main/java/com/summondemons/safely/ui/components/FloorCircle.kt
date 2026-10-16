package com.summondemons.safely.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.fx.LocalReduceMotion

private const val PULSE_HALF_MS = 800
private const val PULSE_MIN_ALPHA = 0.55f
private const val STATIC_ALPHA = 0.8f
private const val PULSE_SCALE = 1.04f
private const val GLOW_ALPHA = 0.12f
private const val GLOW_PASSES = 3
private const val INNER_ALPHA = 0.6f
private const val INNER_INSET_X = 0.10f
private const val INNER_INSET_Y = 0.14f

/** Accent ellipse with a dashed inner ring at the demon's feet; pulses while he can be summoned. */
@Composable
fun FloorCircle(accent: Color, pulse: Boolean, modifier: Modifier = Modifier) {
    val animate = pulse && !LocalReduceMotion.current
    val phase = if (animate) {
        rememberInfiniteTransition(label = "floorCircle").animateFloat(
            0f, 1f, infiniteRepeatable(tween(PULSE_HALF_MS), RepeatMode.Reverse), label = "floorPulse",
        )
    } else {
        null
    }
    Canvas(
        modifier.graphicsLayer {
            val p = phase?.value
            alpha = if (p == null) STATIC_ALPHA else PULSE_MIN_ALPHA + (1f - PULSE_MIN_ALPHA) * p
            val s = if (p == null) 1f else 1f + (PULSE_SCALE - 1f) * p
            scaleX = s
            scaleY = s
        },
    ) {
        val line = 1.5.dp.toPx()
        for (i in GLOW_PASSES downTo 1) drawOval(accent.copy(alpha = GLOW_ALPHA), style = Stroke(line + i * 6.dp.toPx()))
        drawOval(accent, style = Stroke(line))
        val inset = Offset(size.width * INNER_INSET_X, size.height * INNER_INSET_Y)
        drawOval(
            accent.copy(alpha = INNER_ALPHA),
            topLeft = inset,
            size = Size(size.width - 2 * inset.x, size.height - 2 * inset.y),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }
}
