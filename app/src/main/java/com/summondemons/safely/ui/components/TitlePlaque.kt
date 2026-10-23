package com.summondemons.safely.ui.components

import androidx.compose.foundation.text.TextAutoSize
import com.summondemons.safely.ui.theme.fontScaleCapped
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private val PlaqueHeight = 40.dp
private val Chamfer = 12.dp
private val InnerInset = 3.5.dp
private val TitlePadding = 38.dp // chamfer + diamond + gap on each side
private val DiamondOffset = 27.dp // diamond centre from each end
private val DiamondRadius = 3.5.dp
private val TitleSize = 20.sp
private val TitleMinSize = 12.sp
private const val FILL_ALPHA = 0.94f
private const val INNER_LINE_ALPHA = 0.45f

/** Six-sided outline: chamfered left and right points, [inset] from the box edges. */
private fun chamferPath(size: Size, chamfer: Float, inset: Float = 0f) = Path().apply {
    val (w, h) = size
    moveTo(chamfer + inset, inset)
    lineTo(w - chamfer - inset, inset)
    lineTo(w - inset * 1.4f, h / 2)
    lineTo(w - chamfer - inset, h - inset)
    lineTo(chamfer + inset, h - inset)
    lineTo(inset * 1.4f, h / 2)
    close()
}

private class ChamferShape(private val chamfer: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(chamferPath(size, with(density) { chamfer.toPx() }))
}

private val PlaqueShape = ChamferShape(Chamfer)

/**
 * Screen title on a chamfered plaque (ADR-0032, option B of the header prototype): navy fill,
 * gold border and inner line, gold diamonds beside the title. Drawn in code, so it stays sharp
 * and grows with the translated title.
 */
@Composable
fun TitlePlaque(title: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val gold = colors.primary
    val shape = PlaqueShape
    Box(
        modifier
            .height(PlaqueHeight)
            .shadow(6.dp, shape)
            .background(colors.surfaceContainerHigh.copy(alpha = FILL_ALPHA), shape)
            .border(1.5.dp, gold, shape)
            .drawBehind {
                drawPath(chamferPath(size, Chamfer.toPx(), InnerInset.toPx()), gold.copy(alpha = INNER_LINE_ALPHA), style = Stroke(1.dp.toPx()))
                val r = DiamondRadius.toPx()
                for (cx in listOf(DiamondOffset.toPx(), size.width - DiamondOffset.toPx())) {
                    drawPath(
                        Path().apply {
                            moveTo(cx, size.height / 2 - r)
                            lineTo(cx + r, size.height / 2)
                            lineTo(cx, size.height / 2 + r)
                            lineTo(cx - r, size.height / 2)
                            close()
                        },
                        gold,
                    )
                }
            }
            .padding(horizontal = TitlePadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.upper(),
            style = MaterialTheme.textRoles.name.copy(fontSize = TitleSize.fontScaleCapped(), letterSpacing = 0.08.em),
            color = gold,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = TitleMinSize, maxFontSize = TitleSize.fontScaleCapped()),
            modifier = Modifier.semantics { heading() },
        )
    }
}
