package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import com.summondemons.safely.ui.fx.svgPath
import com.summondemons.safely.ui.theme.textRoles

private const val VIEWBOX = 200f
private val C = Offset(100f, 100f)
private const val CARDINAL = "M100 12 L108 100 L100 92 L92 100Z"
private const val INTERCARDINAL = "M100 36 L105 100 L100 95 L95 100Z"
/** Decorative floor letters, not instructions (directions in copy come from demon data). */
private val LETTERS = listOf("N", "E", "S", "W")
private const val LETTER_SIZE = 9f
private const val LETTER_TOP = 2f

/** Inlaid compass rose for the Home floor; static, in `primary` (DESIGN.md). */
@Composable
fun CompassRose(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.primary
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.textRoles.name
    Canvas(modifier.aspectRatio(1f)) {
        val unit = size.minDimension / VIEWBOX
        scale(unit, unit, pivot = Offset.Zero) {
            drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.35f), Color.Transparent), center = C, radius = 96f), 96f, C)
            drawCircle(gold.copy(alpha = 0.53f), 92f, C, style = Stroke(1.2f))
            drawCircle(gold.copy(alpha = 0.33f), 80f, C, style = Stroke(0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f))))
            for (i in 0 until 8) {
                val cardinal = i % 2 == 0
                val point = svgPath(if (cardinal) CARDINAL else INTERCARDINAL)
                rotate(i * 45f, pivot = C) {
                    drawPath(point, gold.copy(alpha = if (cardinal) 0.4f else 0.2f))
                    drawPath(point, gold.copy(alpha = 0.67f), style = Stroke(0.6f))
                }
            }
        }
        LETTERS.forEachIndexed { i, letter ->
            val layout = measurer.measure(
                letter,
                letterStyle.copy(color = gold, fontSize = (LETTER_SIZE * unit).toSp(), lineHeight = TextUnit.Unspecified),
            )
            rotate(i * 90f, pivot = center) {
                drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, LETTER_TOP * unit))
            }
        }
    }
}
