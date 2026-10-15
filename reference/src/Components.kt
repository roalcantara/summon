package com.example.summondemonssafely.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summondemonssafely.ui.theme.*
import kotlin.math.*

// ==========================================
// 1. PrimaryActionButton
// ==========================================
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Dp = 20.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1.0f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        color = if (enabled) containerColor else Color(0xFF333D52),
        contentColor = if (enabled) contentColor else Color.Gray,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = containerColor.copy(alpha = 0.5f),
                spotColor = containerColor
            ),
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}

// ==========================================
// 2. DemonCardBase
// ==========================================
@Composable
fun DemonCardBase(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(10.dp, shape, spotColor = Color.Black.copy(alpha = 0.6f)),
        shape = shape,
        color = backgroundColor,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

// ==========================================
// 3. RibbonBanner ("New Demon Received!")
// ==========================================
@Composable
fun RibbonBanner(
    text: String,
    modifier: Modifier = Modifier,
    bannerColor: Color = Color(0xFFDC2626) // Imperial Crimson
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = bannerColor,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, AmberGoldPrimary),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "★ ",
                    color = AmberGoldPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = " ★",
                    color = AmberGoldPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 4. Rotating Shiny Sunburst Background Canvas
// ==========================================
@Composable
fun RotatingSunburstBackground(
    modifier: Modifier = Modifier,
    beamCount: Int = 16,
    color: Color = AmberGoldPrimary.copy(alpha = 0.18f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SunburstRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = max(size.width, size.height) * 1.2f
        val angleStep = 360f / beamCount

        rotate(rotation, pivot = center) {
            for (i in 0 until beamCount) {
                if (i % 2 == 0) {
                    val startAngle = i * angleStep
                    val sweepAngle = angleStep

                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                center.x - radius,
                                center.y - radius,
                                center.x + radius,
                                center.y + radius
                            ),
                            startAngleDegrees = startAngle,
                            sweepAngleDegrees = sweepAngle,
                            forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(path, color = color)
                }
            }
        }
    }
}

// ==========================================
// 5. Sigil Altar Canvas (Progressive Opacity)
// ==========================================
@Composable
fun SigilAltarCanvas(
    opacity: Float,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.85f

        // Outer glow circle
        drawCircle(
            color = themeColor.copy(alpha = (opacity * 0.3f).coerceIn(0f, 1f)),
            radius = radius * 1.05f
        )

        // Outer Ring
        drawCircle(
            color = themeColor.copy(alpha = opacity),
            radius = radius,
            style = Stroke(width = 3.dp.toPx())
        )

        // Inner Concentric Ring
        drawCircle(
            color = themeColor.copy(alpha = opacity * 0.8f),
            radius = radius * 0.82f,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        )

        // Pentagram / Runic geometric lines
        val points = 5
        val starPoints = mutableListOf<Offset>()
        for (i in 0 until points) {
            val angle = Math.toRadians((i * (360.0 / points) - 90.0)).toFloat()
            starPoints.add(
                Offset(
                    center.x + radius * 0.8f * cos(angle),
                    center.y + radius * 0.8f * sin(angle)
                )
            )
        }

        // Connect star vertices
        for (i in 0 until points) {
            val next = (i + 2) % points
            drawLine(
                color = themeColor.copy(alpha = opacity),
                start = starPoints[i],
                end = starPoints[next],
                strokeWidth = 2.dp.toPx()
            )
        }

        // Center Sanctum Glyph
        drawCircle(
            color = AmberGoldPrimary.copy(alpha = opacity),
            radius = radius * 0.22f,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}