package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.fx.rememberShake
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

enum class CueTone { Neutral, Safe, Error }

private const val SAFE_BORDER_ALPHA = 0.4f
private const val ERROR_BORDER_ALPHA = 0.6f
private const val ERROR_CONTAINER_ALPHA = 0.4f
private val DotSize = 8.dp

/** What to do now (ADR-0014): a pill with a status dot, in the `cue` role. */
@Composable
fun CueChip(text: String, tone: CueTone, modifier: Modifier = Modifier, shake: Int = 0) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val offset = rememberShake(shake)
    val (container, content, border, dot) = when (tone) {
        CueTone.Neutral -> listOf(colors.surfaceContainer, colors.onSurfaceVariant, colors.outlineVariant, colors.outline)
        CueTone.Safe -> listOf(colors.surfaceContainer, safe, safe.copy(alpha = SAFE_BORDER_ALPHA), safe)
        CueTone.Error -> listOf(
            colors.errorContainer.copy(alpha = ERROR_CONTAINER_ALPHA),
            colors.error,
            colors.error.copy(alpha = ERROR_BORDER_ALPHA),
            colors.error,
        )
    }
    Surface(
        shape = PillShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .graphicsLayer { translationX = offset.value.dp.toPx() }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs + 2.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(DotSize).background(dot, CircleShape))
            Text(text.upper(), style = MaterialTheme.textRoles.cue)
        }
    }
}
