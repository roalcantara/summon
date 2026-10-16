package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.summondemons.safely.ui.theme.Stage
import com.summondemons.safely.ui.theme.demonColors

private const val HOME_LIGHT_Y = 0.4f
private const val STAGE_LIGHT_Y = 0.3f
private const val MID_STOP = 0.45f
private const val EDGE_STOP = 0.85f
private const val SCRIM_TOP_EXTRA = 0.15f
private const val SCRIM_MID_FACTOR = 0.4f
private const val SCRIM_BOTTOM_EXTRA = 0.5f
private const val SCRIM_MAX = 0.95f

/** Placeholder stage (DESIGN.md Gaps): a radial gradient under a top/bottom scrim of strength [dim]. */
@Composable
fun StageBackground(stage: Stage, dim: Float, modifier: Modifier = Modifier) {
    val gradient = MaterialTheme.demonColors.stage(stage)
    val scrim = MaterialTheme.colorScheme.surfaceContainerLowest
    val lightY = if (stage == Stage.Home) HOME_LIGHT_Y else STAGE_LIGHT_Y
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                0f to gradient.glow, MID_STOP to gradient.mid, EDGE_STOP to gradient.edge, 1f to gradient.edge,
                center = Offset(size.width / 2f, size.height * lightY),
                radius = size.maxDimension,
            ),
        )
        val bottom = scrim.copy(alpha = (dim + SCRIM_BOTTOM_EXTRA).coerceAtMost(SCRIM_MAX))
        drawRect(
            Brush.verticalGradient(
                0f to scrim.copy(alpha = (dim + SCRIM_TOP_EXTRA).coerceAtMost(1f)),
                0.35f to scrim.copy(alpha = dim * SCRIM_MID_FACTOR),
                0.78f to bottom,
                1f to bottom,
            ),
        )
    }
}
