package com.summondemons.safely.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.summondemons.safely.data.Calling
import com.summondemons.safely.data.House
import com.summondemons.safely.ui.fx.svgPath

/** The glyphs are drawn in a 24 × 24 box (the bundled Latin-subset fonts lack ☉ ♀ etc., ADR-0014). */
private const val BOX = 24f
private const val STROKE = 1.7f
private const val COMPASS_STROKE = 1.5f
private const val COMPASS_RING_ALPHA = 0.6f
private const val NEEDLE_TAIL_ALPHA = 0.5f
private const val LETTER_SIZE = 5.5f
private val CENTER = Offset(12f, 12f)

@Composable
private fun Glyph(size: Dp, modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / BOX
        scale(unit, unit, pivot = Offset.Zero) { draw() }
    }
}

private fun DrawScope.line(data: String, color: Color, width: Float = STROKE) =
    drawPath(svgPath(data), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

/** ☉ for Sol, ♀ for Venus: the classical public-domain planetary signs. */
@Composable
fun HouseGlyph(house: House, color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    when (house) {
        House.Sol -> {
            drawCircle(color, 8f, CENTER, style = Stroke(STROKE))
            drawCircle(color, 1.8f, CENTER)
        }
        House.Venus -> {
            drawCircle(color, 5f, Offset(12f, 9f), style = Stroke(STROKE))
            line("M12 14v7M8.5 18h7", color)
        }
    }
}

@Composable
fun CallingGlyph(calling: Calling, color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    when (calling) {
        Calling.Tutor -> {
            line("M12 6c-2-1.5-5-2-8-1.5v13c3-.5 6 0 8 1.5 2-1.5 5-2 8-1.5v-13c-3-.5-6 0-8 1.5z", color)
            line("M12 6v13", color)
        }
        Calling.Oracle -> {
            line("M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z", color)
            drawCircle(color, 3f, CENTER, style = Stroke(STROKE))
            drawCircle(color, 0.8f, CENTER)
        }
        Calling.Warlord -> line("M5 19L19 5M15 5h4v4M19 19L5 5M5 9V5h4", color)
        Calling.Treasurer -> {
            drawRoundRect(color, Offset(4f, 9f), Size(16f, 10f), CornerRadius(2f), style = Stroke(STROKE))
            line("M4 13h16M12 11v4M6 9c0-3 12-3 12 0", color)
        }
        Calling.Menace -> line("M5 6l3 12 4-8 4 8 3-12", color)
    }
}

/** Crossed swords for the legions a demon commands. */
@Composable
fun LegionsGlyph(color: Color, size: Dp, modifier: Modifier = Modifier) = Glyph(size, modifier) {
    line("M14.5 17.5L3 6V3h3l11.5 11.5M13 19l6-6M16 16l4 4M19 21l2-2", color)
    line("M14.5 6.5L18 3h3v3l-3.5 3.5M5 14l4 4M7 17l-3 3M3 19l2 2", color)
}

/** A compass whose needle points at [heading], with the target's letter in the middle. */
@Composable
fun DirectionGlyph(heading: Float, letter: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / BOX
        scale(unit, unit, pivot = Offset.Zero) {
            drawCircle(color.copy(alpha = color.alpha * COMPASS_RING_ALPHA), 9f, CENTER, style = Stroke(COMPASS_STROKE))
            rotate(heading, pivot = CENTER) {
                drawPath(svgPath("M12 4.5l2.4 7.5H9.6z"), color)
                drawPath(
                    svgPath("M12 19.5l2.4-7.5H9.6z"),
                    color.copy(alpha = color.alpha * NEEDLE_TAIL_ALPHA),
                    style = Stroke(COMPASS_STROKE),
                )
            }
        }
        val layout = measurer.measure(
            letter,
            style.copy(color = color, fontSize = (LETTER_SIZE * unit).toSp(), lineHeight = TextUnit.Unspecified),
        )
        drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
    }
}
