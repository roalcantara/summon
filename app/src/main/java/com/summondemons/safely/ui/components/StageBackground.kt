package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import com.summondemons.safely.R
import com.summondemons.safely.ui.theme.Stage

private const val SCRIM_TOP_EXTRA = 0.15f
private const val SCRIM_MID_FACTOR = 0.4f
private const val SCRIM_BOTTOM_EXTRA = 0.5f
private const val SCRIM_MAX = 0.95f

/** Stage background image (`stage_home`, `stage_sol`, `stage_venus`) under a top/bottom scrim of strength [dim]. */
@Composable
fun StageBackground(stage: Stage, dim: Float, modifier: Modifier = Modifier) {
    val resId = when (stage) {
        Stage.Home -> R.drawable.stage_home
        Stage.Sol -> R.drawable.stage_sol
        Stage.Venus -> R.drawable.stage_venus
    }
    val scrim = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(modifier) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
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
}
