package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.summondemons.safely.R
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.keyframe
import com.summondemons.safely.ui.fx.svgPath
import com.summondemons.safely.ui.theme.demonColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val VIEW_W = 180f
private const val BOB_HALF_MS = 2_000
private const val BOB_UNITS = 8f
private const val HOP_MS = 600
private val HOP_Y = floatArrayOf(0f, -22f, 0f, -6f, 0f)
private val HOP_DEG = floatArrayOf(0f, -3f, 2f, -1f, 0f)
private const val PEEK_MS = 300
private const val PEEK_HOLD_MS = 1_300L
private const val COVER_PEEK_DEG = -9f
private val OPEN_SCALE = floatArrayOf(1f, 1.15f, 0.2f)
private val OPEN_ALPHA = floatArrayOf(1f, 1f, 0f)
private const val OPEN_MS = 700
private const val OPEN_REDUCED_MS = 150
private const val BURST_SHARE = 600f / 700f
private const val SILHOUETTE = "M58 36 q6 -24 14 -2 q18 -10 36 0 q8 -22 14 2 q-12 10 -32 10 q-20 0 -32 -10z"
private const val STRAPS = "M22 44h20M22 176h20M130 26v18M130 176v18"
private const val SEAL = "M86 92v36M76 102h20M74 118q12 10 24 0"
private val LOCK = Offset(86f, 110f)

private fun hopDelayMs(restless: Boolean) = if (restless) 2_000L + Random.nextLong(1_000) else 5_000L + Random.nextLong(3_000)
private fun peekDelayMs(restless: Boolean) = if (restless) 4_000L + Random.nextLong(2_000) else 10_000L + Random.nextLong(5_000)

private class GrimoireInk(
    val shadow: Color,
    val pages: Color,
    val cover: Color,
    val coverEdge: Color,
    val trim: Color,
    val chain: Color,
    val silhouette: Color,
    val burst: Color,
)

/**
 * The Home book (ADR-0014), a vector placeholder (DESIGN.md Gaps): idle bob, a hop every 5–8 s
 * (2–3 s when [restless]), a silhouette peeking every 10–15 s (4–6 s), the lock glowing in
 * [accent]. [opening] plays the scale-up and burst (a 150 ms fade under reduced motion).
 */
@Composable
fun RestlessGrimoire(restless: Boolean, accent: Color, opening: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val colors = MaterialTheme.colorScheme
    val ink = GrimoireInk(
        shadow = colors.surfaceContainerLowest,
        pages = colors.onSurfaceVariant,
        cover = colors.outlineVariant,
        coverEdge = colors.surfaceContainerHighest,
        trim = colors.primaryContainer,
        chain = colors.outline,
        silhouette = colors.surfaceContainerLowest,
        burst = MaterialTheme.demonColors.inkHighlight,
    )
    val hop = remember { Animatable(0f) }
    val peek = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(restless, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(hopDelayMs(restless))
            hop.snapTo(0f)
            hop.animateTo(1f, tween(HOP_MS, easing = LinearEasing))
        }
    }
    LaunchedEffect(restless, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(peekDelayMs(restless))
            peek.animateTo(1f, tween(PEEK_MS))
            delay(PEEK_HOLD_MS)
            peek.animateTo(0f, tween(PEEK_MS))
        }
    }
    LaunchedEffect(opening) {
        if (opening) open.animateTo(1f, tween(if (reduceMotion) OPEN_REDUCED_MS else OPEN_MS, easing = LinearEasing)) else open.snapTo(0f)
    }
    val bob = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "grimoire").animateFloat(
            0f, -BOB_UNITS, infiniteRepeatable(tween(BOB_HALF_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
        )
    }
    val label = stringResource(R.string.home_open)
    Canvas(
        modifier
            .graphicsLayer {
                val unit = size.width / VIEW_W
                translationY = ((bob?.value ?: 0f) + keyframe(hop.value, HOP_Y)) * unit
                rotationZ = keyframe(hop.value, HOP_DEG)
                val o = open.value
                if (reduceMotion) {
                    alpha = 1f - o
                } else {
                    val s = keyframe(o, OPEN_SCALE)
                    scaleX = s
                    scaleY = s
                    alpha = keyframe(o, OPEN_ALPHA)
                }
            }
            .clickable(onClickLabel = label, role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = label },
    ) {
        val unit = size.width / VIEW_W
        scale(unit, unit, pivot = Offset.Zero) {
            drawGrimoire(ink, accent, peek.value, restless, if (reduceMotion) 0f else open.value)
        }
    }
}

private fun DrawScope.drawGrimoire(ink: GrimoireInk, accent: Color, peek: Float, restless: Boolean, open: Float) {
    drawOval(ink.shadow.copy(alpha = 0.45f), topLeft = Offset(20f, 194f), size = Size(140f, 16f))
    drawRoundRect(ink.pages, Offset(30f, 34f), Size(124f, 160f), CornerRadius(6f))
    if (peek > 0f) {
        translate(0f, (1f - peek) * 10f) {
            drawPath(svgPath(SILHOUETTE), ink.silhouette, alpha = peek)
            drawCircle(accent, 3f, Offset(80f, 30f), alpha = peek)
            drawCircle(accent, 3f, Offset(100f, 30f), alpha = peek)
        }
    }
    rotate(COVER_PEEK_DEG * peek, pivot = Offset(22f, 190f)) {
        drawRoundRect(
            Brush.horizontalGradient(listOf(ink.cover, ink.coverEdge), startX = 22f, endX = 150f),
            Offset(22f, 26f), Size(128f, 168f), CornerRadius(8f),
        )
        drawRoundRect(ink.trim, Offset(22f, 26f), Size(128f, 168f), CornerRadius(8f), style = Stroke(3f))
        drawPath(svgPath(STRAPS), ink.trim, style = Stroke(6f, cap = StrokeCap.Round))
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.9f), Color.Transparent), center = LOCK, radius = 34f),
            34f, LOCK, alpha = if (restless) 1f else 0.6f,
        )
        drawCircle(accent, 22f, LOCK, style = Stroke(2f))
        drawPath(svgPath(SEAL), accent, style = Stroke(2f, cap = StrokeCap.Round))
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
        drawLine(ink.chain, Offset(14f, 80f), Offset(160f, 140f), 5f, StrokeCap.Round, dash)
        drawLine(ink.chain, Offset(14f, 140f), Offset(160f, 80f), 5f, StrokeCap.Round, dash)
        drawRoundRect(ink.trim, Offset(78f, 102f), Size(16f, 18f), CornerRadius(3f))
    }
    if (open > 0f) {
        val b = (open / BURST_SHARE).coerceAtMost(1f)
        drawCircle(ink.burst, 10f + 150f * b, LOCK, alpha = 1f - b)
    }
}
