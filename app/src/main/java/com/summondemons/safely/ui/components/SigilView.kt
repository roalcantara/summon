package com.summondemons.safely.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val GLOW_ALPHA = 0.45f
private const val IMAGE_FRACTION = 0.9f

@Composable
fun SigilView(
    @DrawableRes sigil: Int?,
    opacity: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(opacity, label = "sigilOpacity")
    val centerGlyph = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = alpha * GLOW_ALPHA), Color.Transparent)))
        }
        if (sigil != null) {
            Image(
                painter = painterResource(sigil),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = alpha,
                modifier = Modifier.fillMaxSize(IMAGE_FRACTION).clip(CircleShape),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f * 0.85f
                drawCircle(accent.copy(alpha = alpha), radius, style = Stroke(3.dp.toPx()))
                drawCircle(
                    accent.copy(alpha = alpha * 0.8f),
                    radius * 0.82f,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))),
                )
                val points = (0 until 5).map { i ->
                    val angle = Math.toRadians(i * 72.0 - 90.0)
                    Offset(
                        center.x + radius * 0.8f * cos(angle).toFloat(),
                        center.y + radius * 0.8f * sin(angle).toFloat(),
                    )
                }
                for (i in 0 until 5) {
                    drawLine(accent.copy(alpha = alpha), points[i], points[(i + 2) % 5], strokeWidth = 2.dp.toPx())
                }
                drawCircle(centerGlyph.copy(alpha = alpha), radius * 0.22f, style = Stroke(2.dp.toPx()))
            }
        }
    }
}
