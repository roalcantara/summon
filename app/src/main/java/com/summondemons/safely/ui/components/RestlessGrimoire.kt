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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.summondemons.safely.R
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.keyframe
import com.summondemons.safely.ui.theme.demonColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val VIEW_W = 180f
private const val BOB_HALF_MS = 2_000
private const val BOB_UNITS = 8f
private const val HOP_MS = 600
private val HOP_Y = floatArrayOf(0f, -22f, 0f, -6f, 0f)
private val HOP_DEG = floatArrayOf(0f, -3f, 2f, -1f, 0f)
private val OPEN_SCALE = floatArrayOf(1f, 1.15f, 0.2f)
private val OPEN_ALPHA = floatArrayOf(1f, 1f, 0f)
private const val OPEN_MS = 700
private const val OPEN_REDUCED_MS = 150
private const val BURST_SHARE = 600f / 700f
private val LOCK = Offset(86f, 110f)

private fun hopDelayMs(restless: Boolean) = if (restless) 2_000L + Random.nextLong(1_000) else 5_000L + Random.nextLong(3_000)

/**
 * The Home grimoire (`ic_home_grimoire` art, ADR-0014): idle bob, a hop every 5–8 s
 * (2–3 s when [restless]), the lock glowing in [accent]. [opening] plays the scale-up and burst.
 */
@Composable
fun RestlessGrimoire(restless: Boolean, accent: Color, opening: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val hop = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(restless, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(hopDelayMs(restless))
            hop.snapTo(0f)
            hop.animateTo(1f, tween(HOP_MS, easing = LinearEasing))
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
    Box(
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
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_home_grimoire),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (open.value > 0f && !reduceMotion) {
            val b = (open.value / BURST_SHARE).coerceAtMost(1f)
            val burstColor = MaterialTheme.demonColors.inkHighlight
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.width / VIEW_W
                drawCircle(burstColor, (10f + 150f * b) * unit, Offset(LOCK.x * unit, LOCK.y * unit), alpha = 1f - b)
            }
        }
    }
}
