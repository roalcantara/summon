package com.summondemons.safely.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private const val RIM_SCALE = 1.03f
private const val RIM_ALPHA = 0.55f
/** Below API 31 `blur` is a no-op, so the rim is dimmed instead. */
private const val RIM_ALPHA_UNBLURRED = 0.2f
private const val SILHOUETTE_ALPHA = 0.94f
private val RimBlur = 10.dp

/**
 * Cut-out art with an accent rim glow behind it. [silhouette] 0..1 darkens the art toward
 * a near-black silhouette (locked demons, the start of the climax).
 */
@Composable
fun DemonCutout(
    @DrawableRes art: Int,
    contentDescription: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    silhouette: Float = 0f,
) {
    val shadow = MaterialTheme.colorScheme.surfaceContainerLowest
    val painter = painterResource(art)
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val rimAlpha = if (blurSupported) RIM_ALPHA else RIM_ALPHA_UNBLURRED
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(accent.copy(alpha = rimAlpha), BlendMode.SrcIn),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = RIM_SCALE
                    scaleY = RIM_SCALE
                }
                .blur(RimBlur, BlurredEdgeTreatment.Unbounded),
        )
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = if (silhouette > 0f) {
                ColorFilter.tint(shadow.copy(alpha = SILHOUETTE_ALPHA * silhouette), BlendMode.SrcAtop)
            } else {
                null
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
