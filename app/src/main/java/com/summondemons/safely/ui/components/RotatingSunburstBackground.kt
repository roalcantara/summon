package com.summondemons.safely.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.max

private const val ROTATION_MS = 28_000

@Composable
fun RotatingSunburstBackground(color: Color, modifier: Modifier = Modifier, beamCount: Int = 16) {
    val transition = rememberInfiniteTransition(label = "sunburst")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(ROTATION_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "sunburstRotation",
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = max(size.width, size.height) * 1.2f
        val bounds = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        val step = 360f / beamCount
        rotate(rotation, pivot = center) {
            for (i in 0 until beamCount step 2) {
                val beam = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(bounds, startAngleDegrees = i * step, sweepAngleDegrees = step, forceMoveTo = false)
                    close()
                }
                drawPath(beam, color = color)
            }
        }
    }
}
