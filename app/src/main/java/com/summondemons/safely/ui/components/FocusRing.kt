package com.summondemons.safely.ui.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

private val FocusRingWidth = 3.dp
/** Drawn outside the control: on gilded art a ring on the edge blends into the gold frame. */
private val FocusRingGap = 4.dp

/**
 * A light outline around the control while [interaction] has focus (keyboard, D-pad, Switch
 * Access), for controls whose press feedback is their own art (`indication = null`). Touch never
 * focuses them, so touch shows nothing. [inside] draws it just within the edge, for controls in a
 * container that clips (the quote slot's cross-fade).
 */
@Composable
fun Modifier.focusRing(interaction: InteractionSource, shape: Shape, inside: Boolean = false): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val color = MaterialTheme.colorScheme.onSurface
    return drawWithContent {
        drawContent()
        if (focused) {
            val gap = if (inside) -FocusRingWidth.toPx() else FocusRingGap.toPx()
            val outline = shape.createOutline(Size(size.width + 2 * gap, size.height + 2 * gap), layoutDirection, this)
            translate(-gap, -gap) { drawOutline(outline, color, style = Stroke(FocusRingWidth.toPx())) }
        }
    }
}
