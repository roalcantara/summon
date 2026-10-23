package com.summondemons.safely.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val FocusRingWidth = 2.dp

/**
 * A gold outline while [interaction] has focus (keyboard, D-pad, Switch Access), for controls whose
 * press feedback is their own art (`indication = null`). Touch never focuses them, so touch shows nothing.
 */
@Composable
fun Modifier.focusRing(interaction: InteractionSource, shape: Shape): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    return if (focused) border(FocusRingWidth, MaterialTheme.colorScheme.primary, shape) else this
}
