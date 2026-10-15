package com.summondemons.safely.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Look of a particle field: rise speed (screens per second), sway, opacity and size range. */
enum class ParticleStyle(val rise: Float, val sway: Dp, val alpha: Float, val minRadius: Dp, val radiusRange: Dp) {
    Embers(rise = 0.12f, sway = 6.dp, alpha = 0.9f, minRadius = 1.dp, radiusRange = 2.dp),
    Smoke(rise = 0.04f, sway = 18.dp, alpha = 0.18f, minRadius = 24.dp, radiusRange = 36.dp),
}

private const val SWAY_RATE = 0.8f
private const val DEFAULT_COUNT = 36

private class Particle(val x: Float, val phase: Float, val speed: Float, val size: Float, val swayPhase: Float)

/**
 * Deterministic drifting particles. [converge] in 0..1 pulls every particle to the
 * centre (climax). Draws nothing when motion is reduced.
 */
@Composable
fun ParticleField(
    color: Color,
    style: ParticleStyle,
    modifier: Modifier = Modifier,
    count: Int = DEFAULT_COUNT,
    converge: Float = 0f,
    seed: Int = 7,
) {
    if (LocalReduceMotion.current) return
    val particles = remember(count, seed) {
        val random = Random(seed)
        List(count) {
            Particle(
                x = random.nextFloat(),
                phase = random.nextFloat(),
                speed = 0.5f + random.nextFloat(),
                size = random.nextFloat(),
                swayPhase = random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val time by rememberFrameTimeMs()
    Canvas(modifier) {
        val t = time / 1_000f
        particles.forEach { p ->
            val cycle = (p.phase + t * p.speed * style.rise) % 1f // 0 = bottom, 1 = top
            val x = p.x * size.width + sin(t * SWAY_RATE + p.swayPhase) * style.sway.toPx()
            val y = size.height * (1f - cycle)
            val at = Offset(lerp(x, center.x, converge), lerp(y, center.y, converge))
            val alpha = sin(cycle * PI).toFloat() * style.alpha * color.alpha
            val radius = (style.minRadius + style.radiusRange * p.size).toPx() * (1f - 0.5f * converge)
            when (style) {
                ParticleStyle.Embers -> drawCircle(color.copy(alpha = alpha), radius, at)
                ParticleStyle.Smoke -> drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = at, radius = radius),
                    radius,
                    at,
                )
            }
        }
    }
}
