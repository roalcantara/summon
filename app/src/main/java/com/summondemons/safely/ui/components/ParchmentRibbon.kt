package com.summondemons.safely.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private val RibbonTextSize = 22.sp
/** How far each notched tail reaches in; the text is inset past it. */
private val Tail = 18.dp
private val Edge = 1.dp

/**
 * A title on a parchment ribbon drawn in code (ADR-0033; replaces `chant_intro_banner.webp`): a
 * gradient band with notched tails that grows with its text, so any language, line break or font
 * scale fits. Words on it follow the fixed-art rule (at most 1.3×).
 */
@Composable
fun ParchmentRibbon(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val spacing = MaterialTheme.spacing
    Box(
        modifier
            .drawBehind {
                val notch = Tail.toPx()
                val band = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - notch, size.height / 2f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    lineTo(notch, size.height / 2f)
                    close()
                }
                drawPath(band, Brush.verticalGradient(listOf(colors.parchmentLight, colors.parchmentDark)))
                drawPath(band, colors.parchmentEdge, style = Stroke(Edge.toPx()))
            }
            .padding(horizontal = Tail + spacing.sm, vertical = spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.upper(),
            style = MaterialTheme.textRoles.name.copy(fontSize = RibbonTextSize.fontScaleCapped(), lineHeight = 1.1.em, textAlign = TextAlign.Center),
            color = colors.parchmentInk,
        )
    }
}
