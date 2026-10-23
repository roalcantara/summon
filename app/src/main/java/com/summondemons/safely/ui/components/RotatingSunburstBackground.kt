package com.summondemons.safely.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.max

private const val ROTATION_MS = 28_000

/** Rays behind the summoned demon; the layer turns (no recomposition), the beam paths are built once per size. */
@Composable
fun RotatingSunburstBackground(color: Color, modifier: Modifier = Modifier, beamCount: Int = 16, rotating: Boolean = true) {
    val angle = if (rotating) {
        rememberInfiniteTransition(label = "sunburst").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(ROTATION_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "sunburstRotation",
        )
    } else {
        null
    }
    Spacer(
        modifier
            .graphicsLayer { rotationZ = angle?.value ?: 0f }
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = max(size.width, size.height) * 1.2f
                val bounds = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
                val step = 360f / beamCount
                val beams = (0 until beamCount step 2).map { i ->
                    Path().apply {
                        moveTo(center.x, center.y)
                        arcTo(bounds, startAngleDegrees = i * step, sweepAngleDegrees = step, forceMoveTo = false)
                        close()
                    }
                }
                onDrawBehind { beams.forEach { drawPath(it, color = color) } }
            },
    )
}
