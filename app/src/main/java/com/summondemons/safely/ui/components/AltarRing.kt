package com.summondemons.safely.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.isAligned
import com.summondemons.safely.ritual.signedDelta
import com.summondemons.safely.ui.theme.demonColors

private val TrackWidth = 1.5.dp
private val RingWidth = 2.dp
private val MarkerSize = 9.dp
private const val MARKER_HALF_WIDTH = 0.6f

/**
 * The ring around the sigil (ADR-0014): an `outlineVariant` track; while Locking a
 * `ritual-safe` arc fills with [lockFraction]; once locked the full ring is `ritual-safe`,
 * `error` while Wavering. A marker on the ring points to the target relative to the
 * phone's heading, green when within tolerance.
 */
@Composable
fun AltarRing(azimuth: Float?, target: Float, phase: RitualPhase, lockFraction: Float, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val ringColor by animateColorAsState(if (phase == RitualPhase.Wavering) colors.error else safe, label = "altarRing")
    val near = azimuth != null && isAligned(azimuth, target)
    val markerColor by animateColorAsState(if (near) safe else accent, label = "altarMarker")
    val offset = azimuth?.let { signedDelta(it, target) } ?: 0f
    val sweep = when (phase) {
        RitualPhase.Seeking -> 0f
        RitualPhase.Locking -> 360f * lockFraction.coerceIn(0f, 1f)
        else -> 360f
    }
    Canvas(modifier) {
        val marker = MarkerSize.toPx()
        val ring = RingWidth.toPx()
        val radius = size.minDimension / 2f - marker - ring
        drawCircle(colors.outlineVariant, radius, center, style = Stroke(TrackWidth.toPx()))
        if (sweep > 0f) {
            drawArc(
                ringColor, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
                style = Stroke(ring, cap = StrokeCap.Round),
            )
        }
        rotate(offset, pivot = center) {
            val tip = center.y - radius - ring / 2f - marker
            drawPath(
                Path().apply {
                    moveTo(center.x, tip)
                    lineTo(center.x + marker * MARKER_HALF_WIDTH, tip + marker)
                    lineTo(center.x - marker * MARKER_HALF_WIDTH, tip + marker)
                    close()
                },
                markerColor,
            )
        }
    }
}
