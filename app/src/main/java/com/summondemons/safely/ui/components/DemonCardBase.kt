package com.summondemons.safely.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

private const val UNLOCKED_TINT = 0.08f
private val CardElevation = 6.dp

/** Locked: grey container, neutral border. Unlocked: accent border and faint accent tint. */
@Composable
fun DemonCardBase(
    locked: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (locked) colors.surfaceContainer else lerp(colors.surfaceContainerHigh, accent, UNLOCKED_TINT),
        border = if (locked) BorderStroke(1.dp, colors.outlineVariant) else BorderStroke(2.dp, accent.copy(alpha = 0.7f)),
        shadowElevation = CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
