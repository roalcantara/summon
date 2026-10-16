package com.summondemons.safely.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val JITTER_MS = 180
private val JITTER_X = floatArrayOf(0f, -1f, 0f, 1f, 0f)
private val JITTER_Y = floatArrayOf(0f, 1f, 0f, -1f, 0f)
private val JITTER_DEG = floatArrayOf(0f, -2f, 0f, 2f, 0f)
private const val SHAKE_MS = 350

/** A restless demon's nervous jitter (ADR-0015). Off when [enabled] is false or motion is reduced. */
@Composable
fun Jitter(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val active = enabled && !LocalReduceMotion.current
    val phase = if (active) {
        rememberInfiniteTransition(label = "jitter").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(JITTER_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "jitterPhase",
        )
    } else {
        null
    }
    Box(
        modifier.graphicsLayer {
            val p = phase?.value ?: return@graphicsLayer
            translationX = keyframe(p, JITTER_X) * 1.dp.toPx()
            translationY = keyframe(p, JITTER_Y) * 1.dp.toPx()
            rotationZ = keyframe(p, JITTER_DEG)
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Horizontal shake offset in dp, replayed whenever [trigger] increases (cooling cue, cue chip). */
@Composable
fun rememberShake(trigger: Int): Animatable<Float, AnimationVector1D> {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offset.animateTo(
                0f,
                keyframes {
                    durationMillis = SHAKE_MS
                    -6f at 70
                    6f at 140
                    -4f at 210
                    4f at 280
                },
            )
        }
    }
    return offset
}
