package com.summondemons.safely.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ui.theme.demonColors

enum class CompassMode { Seeking, Locked, Wavering }

private val TrackWidth = 2.dp
private val NotchLength = 10.dp
private val MarkerSize = 8.dp
private const val GUIDE_ALPHA = 0.8f

/**
 * Heading ring around the sigil. The top is where the phone points (marker); the notch is
 * the target, so turning brings the notch to the marker. Wavering drains a red arc with [graceFraction].
 */
@Composable
fun CompassArc(azimuth: Float?, target: Float, mode: CompassMode, graceFraction: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val color by animateColorAsState(
        when (mode) {
            CompassMode.Seeking -> colors.outline
            CompassMode.Locked -> safe
            CompassMode.Wavering -> colors.error
        },
        label = "compassColor",
    )
    val track = colors.outlineVariant
    val offset = azimuth?.let { signedDelta(it, target) }
    Canvas(modifier) {
        val width = TrackWidth.toPx()
        val radius = size.minDimension / 2f - NotchLength.toPx()
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawCircle(track, radius, center, style = Stroke(width))
        if (offset != null) {
            drawArc(
                color.copy(alpha = GUIDE_ALPHA), startAngle = -90f, sweepAngle = offset, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width * 2, cap = StrokeCap.Round),
            )
            rotate(offset, pivot = center) {
                drawLine(
                    color,
                    Offset(center.x, center.y - radius - NotchLength.toPx()),
                    Offset(center.x, center.y - radius + NotchLength.toPx()),
                    strokeWidth = width * 2, cap = StrokeCap.Round,
                )
            }
        }
        if (mode == CompassMode.Wavering) {
            drawArc(
                colors.error, startAngle = -90f, sweepAngle = 360f * (1f - graceFraction), useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width * 3, cap = StrokeCap.Round),
            )
        }
        val marker = MarkerSize.toPx()
        val top = center.y - radius - NotchLength.toPx() - marker
        drawPath(
            Path().apply {
                moveTo(center.x - marker / 2, top)
                lineTo(center.x + marker / 2, top)
                lineTo(center.x, top + marker)
                close()
            },
            color,
        )
    }
}
