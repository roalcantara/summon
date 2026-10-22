package com.summondemons.safely.ui.fx

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

/** Half a breath: 1.6 s in and out; [FAST_HALF_MS] while wavering (ADR-0033). */
private const val HALF_MS = 800
private const val FAST_HALF_MS = 400
/** Reduced motion: a steady glow at this level instead of breathing. */
private const val STEADY = 0.6f
private const val GLOW_ALPHA = 0.55f
/** The glow reaches this far past the element's half-size. */
private const val GLOW_SPREAD = 1.6f

private val NoAttention: State<Float> = mutableFloatStateOf(0f)
private val SteadyAttention: State<Float> = mutableFloatStateOf(STEADY)

/**
 * The attention level, 0..1 (ADR-0033): 0 when not [active]; a steady [STEADY] with reduced
 * motion; otherwise breathing over 1.6 s, or 0.8 s when [fast]. A [State]: read it only while
 * drawing, so the breathing redraws the glow instead of recomposing the screen every frame.
 */
@Composable
fun rememberAttention(active: Boolean, fast: Boolean = false): State<Float> {
    if (!active) return NoAttention
    if (LocalReduceMotion.current) return SteadyAttention
    return rememberInfiniteTransition(label = "attention").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (fast) FAST_HALF_MS else HALF_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
}

/**
 * The attention glow behind an element (ADR-0033): a soft [color] halo, elliptical for wide
 * elements, as strong as [level] (from [rememberAttention]). Draws nothing at 0.
 */
fun Modifier.attentionGlow(level: State<Float>, color: Color): Modifier = drawBehind {
    val strength = level.value
    if (strength <= 0f) return@drawBehind
    val radius = size.minDimension / 2f * GLOW_SPREAD
    scale(scaleX = size.width / size.minDimension, scaleY = size.height / size.minDimension) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = GLOW_ALPHA * strength), Color.Transparent), center, radius),
            radius,
            center,
        )
    }
}
