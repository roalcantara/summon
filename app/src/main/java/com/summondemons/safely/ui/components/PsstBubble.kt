package com.summondemons.safely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.fx.rememberShake
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Taps that burst the bubble; one seal pip each. */
private const val PIPS = 3
private val BubbleMaxWidth = 180.dp
private val BurstWidth = 260.dp
private val Outline = 3.dp
private val TickStroke = 3.dp
private val PipSize = 10.dp
private val PipEdge = 2.dp
private val Tail = 16.dp
private val InviteSize = 14.sp
private val ReplySize = 15.sp
private val MinSize = 8.sp
/** The bubble's outline in its own 176 × 112 sketch (prototype "Psst, three taps"); the body ends at 92. */
private const val SKETCH_W = 176f
private const val SKETCH_H = 112f
/** Shake strength after the second tap: harder, like a chest about to burst. */
private const val HARD_SHAKE = 1.7f
private const val SHAKE_TILT = 0.35f
private const val BURST_SPIKES = 14
/** Spike lengths, fixed so the burst looks the same every time. */
private val SpikeJitter = floatArrayOf(0.04f, 0.11f, 0.02f, 0.09f, 0.06f, 0.12f, 0.01f, 0.08f, 0.05f, 0.10f, 0.03f, 0.07f, 0.11f, 0.04f)

/**
 * The "Psst" invite (ADR-0036, amended): a parchment comic bubble with accent action ticks and
 * three seal pips lit in [accent], one per tap. [shake] retriggers the shake; it is harder once [taps] ≥ 2.
 */
@Composable
fun PsstBubble(text: String, taps: Int, shake: Int, accent: Color, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val outline = MaterialTheme.colorScheme.surfaceContainerLowest
    val reduceMotion = LocalReduceMotion.current
    val ticks = rememberAttention(active = true, fast = taps >= 2)
    val offset = rememberShake(if (reduceMotion) 0 else shake)
    val strength = if (taps >= 2) HARD_SHAKE else 1f
    val fontSize = InviteSize.fontScaleCapped()
    val progress = stringResource(R.string.psst_progress, taps)
    Box(
        modifier
            .widthIn(max = BubbleMaxWidth)
            .graphicsLayer {
                translationX = offset.value.dp.toPx() * strength
                rotationZ = offset.value * strength * SHAKE_TILT
            }
            .drawBehind {
                drawTicks(accent.copy(alpha = 0.45f + 0.55f * ticks.value))
                drawBubble(colors.parchmentLight, colors.parchmentDark, outline)
            }
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onTap)
            .semantics {
                contentDescription = text
                stateDescription = progress
            }
            .padding(start = 18.dp, end = 14.dp, top = MaterialTheme.spacing.md, bottom = MaterialTheme.spacing.sm + Tail),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            Text(
                text,
                style = MaterialTheme.textRoles.name.copy(fontSize = fontSize, textAlign = TextAlign.Center),
                color = colors.parchmentInk,
                maxLines = 3,
                autoSize = TextAutoSize.StepBased(minFontSize = MinSize, maxFontSize = fontSize),
                modifier = Modifier.clearAndSetSemantics { },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                repeat(PIPS) { i ->
                    val lit = i < taps
                    Box(
                        Modifier
                            .size(PipSize)
                            .then(if (lit) Modifier.background(accent, CircleShape) else Modifier)
                            .border(PipEdge, colors.parchmentInk, CircleShape),
                    )
                }
            }
        }
    }
}

/** The reply after the third tap: a spiky comic burst in the same parchment and ink. */
@Composable
fun PsstBurst(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val outline = MaterialTheme.colorScheme.surfaceContainerLowest
    val fontSize = ReplySize.fontScaleCapped()
    Box(
        modifier
            .widthIn(max = BurstWidth)
            .drawBehind { drawBurst(colors.parchmentLight, outline) }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 52.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.textRoles.name.copy(fontSize = fontSize, textAlign = TextAlign.Center),
            color = colors.parchmentInk,
            maxLines = 3,
            autoSize = TextAutoSize.StepBased(minFontSize = MinSize, maxFontSize = fontSize),
        )
    }
}

private fun DrawScope.sketch(x: Float, y: Float) = Offset(x / SKETCH_W * size.width, y / SKETCH_H * size.height)

private fun DrawScope.drawBubble(fill: Color, shade: Color, outline: Color) {
    fun p(x: Float, y: Float) = sketch(x, y)
    val body = Path().apply {
        p(18f, 12f).let { moveTo(it.x, it.y) }
        quad(p(16f, 4f), p(26f, 4f))
        p(156f, 2f).let { lineTo(it.x, it.y) }
        quad(p(168f, 2f), p(168f, 14f))
        p(164f, 78f).let { lineTo(it.x, it.y) }
        quad(p(163f, 90f), p(152f, 90f))
        p(64f, 92f).let { lineTo(it.x, it.y) }
        p(30f, 110f).let { lineTo(it.x, it.y) }
        p(42f, 92f).let { lineTo(it.x, it.y) }
        p(26f, 92f).let { lineTo(it.x, it.y) }
        quad(p(14f, 92f), p(14f, 80f))
        close()
    }
    drawPath(body, fill)
    val highlight = Path().apply {
        p(24f, 70f).let { moveTo(it.x, it.y) }
        quad(p(30f, 86f), p(62f, 86f))
        p(120f, 86f).let { lineTo(it.x, it.y) }
    }
    drawPath(highlight, shade, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
    drawPath(body, outline, style = Stroke(width = Outline.toPx(), join = StrokeJoin.Round))
}

private fun Path.quad(control: Offset, end: Offset) = quadraticTo(control.x, control.y, end.x, end.y)

/** Comic "action" ticks at the top-start and bottom-end corners. */
private fun DrawScope.drawTicks(color: Color) {
    val stroke = TickStroke.toPx()
    listOf(
        14f to 6f to (4f to -4f), 24f to 2f to (20f to -10f), 6f to 16f to (-6f to 12f),
        162f to 72f to (174f to 80f), 156f to 82f to (162f to 94f),
    ).forEach { (from, to) ->
        drawLine(color, sketch(from.first, from.second), sketch(to.first, to.second), stroke, StrokeCap.Round)
    }
}

private fun DrawScope.drawBurst(fill: Color, outline: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val path = Path()
    for (i in 0 until BURST_SPIKES * 2) {
        val a = i.toFloat() / (BURST_SPIKES * 2) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val r = if (i % 2 == 0) 1f + SpikeJitter[i / 2] else 0.76f + SpikeJitter[(i / 2 + 5) % BURST_SPIKES] / 2f
        val x = cx + cos(a) * cx * r
        val y = cy + sin(a) * cy * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, fill)
    drawPath(path, outline, style = Stroke(width = Outline.toPx(), join = StrokeJoin.Round))
}
