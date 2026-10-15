package com.summondemons.safely.ui.fx

import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.summondemons.safely.sigil.SigilPaths
import com.summondemons.safely.sigil.SigilStroke

/** Sigil geometry in viewBox units (0..100, centre 50,50). See ADR-0009 / DESIGN.md. */
private const val VIEWBOX = 100f
private const val OUTER_RING = 48f
private const val INNER_RING = 41f
private const val RIM_TEXT_RADIUS = 44.5f
private const val RIM_LETTER_SIZE = 5f
private const val STROKE = 1.2f
private const val RING_STROKE = 0.8f
private const val GLOW_WIDTH = 3f
private const val GLOW_MIC_WIDTH = 3f
private const val GLOW_ALPHA = 0.25f
private const val GLOW_MIC_ALPHA = 0.45f
private const val INK_ANIM_MS = 250
private val GlowBlur = 6.dp

@Immutable
class LoadedSigil(val strokes: List<SigilStroke>, val paths: List<Path>, val lengths: List<Float>)

@Composable
fun rememberSigil(@RawRes res: Int): LoadedSigil {
    val context = LocalContext.current
    return remember(res) {
        val lines = context.resources.openRawResource(res).bufferedReader().use { it.readLines() }
        val strokes = SigilPaths.parse(lines)
        val paths = strokes.map { PathParser().parsePathString(it.pathData).toPath() }
        val lengths = paths.map { path -> PathMeasure().apply { setPath(path, false) }.length }
        LoadedSigil(strokes, paths, lengths)
    }
}

/**
 * The demon's vector sigil (ADR-0009). [inkProgress] inks the glyph group by group;
 * [ringProgress] draws the double ring and rim letters ([rimText]); the glow widens and
 * brightens with [micLevel]; [flicker] scales the alpha (drift warning).
 */
@Composable
fun InkedSigil(
    @RawRes sigil: Int,
    inkProgress: Float,
    accent: Color,
    rimText: String,
    modifier: Modifier = Modifier,
    ringProgress: Float = 1f,
    micLevel: Float = 0f,
    flicker: Float = 1f,
) {
    val loaded = rememberSigil(sigil)
    val ink by animateFloatAsState(inkProgress, tween(INK_ANIM_MS), label = "sigilInk")
    val fractions = remember(loaded, ink) { SigilPaths.inkFractions(loaded.strokes, loaded.lengths, ink) }
    val textMeasurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.titleLarge
    val glow = accent.copy(alpha = (GLOW_ALPHA + GLOW_MIC_ALPHA * micLevel) * flicker)
    val core = accent.copy(alpha = accent.alpha * flicker)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().blur(GlowBlur, BlurredEdgeTreatment.Unbounded)) {
            drawSigil(loaded, fractions, glow, STROKE * (GLOW_WIDTH + GLOW_MIC_WIDTH * micLevel), ringProgress)
        }
        Canvas(Modifier.fillMaxSize()) {
            drawSigil(loaded, fractions, core, STROKE, ringProgress)
            drawRim(textMeasurer, rimText, letterStyle, core, ringProgress)
        }
    }
}

/** Glyph strokes and rings, drawn in viewBox units scaled to fit. */
private fun DrawScope.drawSigil(sigil: LoadedSigil, fractions: List<Float>, color: Color, width: Float, ringProgress: Float) {
    val unit = size.minDimension / VIEWBOX
    val origin = Offset((size.width - VIEWBOX * unit) / 2f, (size.height - VIEWBOX * unit) / 2f)
    translate(origin.x, origin.y) {
        scale(unit, unit, pivot = Offset.Zero) {
            val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val measure = PathMeasure()
            sigil.paths.forEachIndexed { i, path ->
                val fraction = fractions[i]
                if (fraction <= 0f) return@forEachIndexed
                val inked = if (fraction >= 1f) {
                    path
                } else {
                    Path().also {
                        measure.setPath(path, false)
                        measure.getSegment(0f, sigil.lengths[i] * fraction, it, true)
                    }
                }
                drawPath(inked, color, style = stroke)
            }
            if (ringProgress > 0f) {
                val sweep = 360f * ringProgress
                val ringStroke = Stroke(RING_STROKE * width / STROKE, cap = StrokeCap.Round)
                for (radius in listOf(OUTER_RING, INNER_RING)) {
                    drawArc(
                        color, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset(VIEWBOX / 2 - radius, VIEWBOX / 2 - radius),
                        size = Size(radius * 2, radius * 2), style = ringStroke,
                    )
                }
            }
        }
    }
}

/** Rim letters spaced evenly around the ring, appearing one by one with [ringProgress]. */
private fun DrawScope.drawRim(measurer: TextMeasurer, text: String, style: TextStyle, color: Color, ringProgress: Float) {
    val letters = text.filterNot { it.isWhitespace() }
    if (letters.isEmpty() || ringProgress <= 0f) return
    val unit = size.minDimension / VIEWBOX
    val letterStyle = style.copy(fontSize = (RIM_LETTER_SIZE * unit).toSp())
    letters.forEachIndexed { i, letter ->
        val alpha = (ringProgress * letters.length - i).coerceIn(0f, 1f)
        if (alpha <= 0f) return@forEachIndexed
        val layout = measurer.measure(letter.toString(), letterStyle.copy(color = color.copy(alpha = color.alpha * alpha)))
        rotate(360f * i / letters.length, pivot = center) {
            drawText(
                layout,
                topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - RIM_TEXT_RADIUS * unit - layout.size.height / 2f,
                ),
            )
        }
    }
}
