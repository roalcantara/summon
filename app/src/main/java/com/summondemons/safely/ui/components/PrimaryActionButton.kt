package com.summondemons.safely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.PillShape
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.fx.LocalReduceMotion

enum class ButtonTone { Primary, Danger }

private val MinButtonHeight = 52.dp
private val GlowElevation = 8.dp

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Primary,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (tone) {
        ButtonTone.Primary -> colors.primaryContainer
        ButtonTone.Danger -> colors.surfaceContainerHigh
    }
    val content = when (tone) {
        ButtonTone.Primary -> colors.onPrimaryContainer
        ButtonTone.Danger -> colors.error
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(if (pressed && !reduceMotion) 0.96f else 1f, label = "buttonPress")

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        color = if (enabled) container else colors.surfaceContainerHighest,
        contentColor = if (enabled) content else colors.onSurfaceVariant,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = MinButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled && tone == ButtonTone.Primary) GlowElevation else 0.dp,
                shape = PillShape,
                ambientColor = container,
                spotColor = container,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.md),
        ) {
            Text(text = text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
