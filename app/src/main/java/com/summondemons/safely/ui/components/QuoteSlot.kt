package com.summondemons.safely.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.fx.rememberShake
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** What fills the slot under the demon (§4). */
enum class QuoteSlotState { Celebration, Bound, Ready, Cooling }

private const val BORDER_ALPHA = 0.67f
private const val BUBBLE_ALPHA = 0.92f
private const val GHOST_ALPHA = 0.6f
private const val DOT_ALPHA = 0.53f
private const val PULSE_HALF_MS = 900
private const val PULSE_MIN_ALPHA = 0.7f
private const val PULSE_SCALE = 1.03f
private const val POP_FROM = 0.6f
private const val SLOT_FADE_IN_MS = 200
private const val SLOT_FADE_OUT_MS = 150
val QuoteSlotHeight = 72.dp
private val QuoteSlotMaxWidth = 320.dp
private val QuoteSize = 15.sp
/** Large font sizes shrink the quote back towards this rather than cut it off. */
private val QuoteMinSize = 8.sp
/** Three lines + padding must fit the 72 dp slot; relative, so it shrinks with the quote. */
private val QuoteLineHeight = 1.2.em
private val SmallCueSize = 10.sp
private val DashOn = 6.dp
private val DashOff = 4.dp

/** One fixed-height slot, always filled, so the art never resizes between demons or states. */
@Composable
fun QuoteSlot(
    state: QuoteSlotState,
    demon: Demon,
    quote: String,
    hours: Int,
    shake: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    Box(modifier.widthIn(max = QuoteSlotMaxWidth).fillMaxWidth().height(QuoteSlotHeight), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(SLOT_FADE_IN_MS)) togetherWith fadeOut(tween(SLOT_FADE_OUT_MS)) },
            label = "quoteSlot",
        ) { s ->
            when (s) {
                QuoteSlotState.Celebration ->
                    PrimaryActionButton(stringResource(R.string.action_awesome), onTap, Modifier.fillMaxWidth())
                QuoteSlotState.Bound -> SpeechBubble(quote, accent)
                QuoteSlotState.Ready -> SummonCue(demon, accent, onTap)
                QuoteSlotState.Cooling -> CoolingCue(accent, hours, shake, onTap)
            }
        }
    }
}

@Composable
private fun SpeechBubble(quote: String, accent: Color) {
    val reduceMotion = LocalReduceMotion.current
    val pop = remember(quote) { Animatable(if (reduceMotion) 1f else POP_FROM) }
    LaunchedEffect(pop) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = BUBBLE_ALPHA),
        border = BorderStroke(2.dp, accent.copy(alpha = BORDER_ALPHA)),
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        },
    ) {
        Text(
            quote,
            style = MaterialTheme.textRoles.body.copy(fontSize = QuoteSize, lineHeight = QuoteLineHeight, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = QuoteMinSize, maxFontSize = QuoteSize),
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
        )
    }
}

/** Sealed and ready: a dashed ghost bubble that dares you to summon him. */
@Composable
private fun SummonCue(demon: Demon, accent: Color, onTap: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val pulse = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "summonCue").animateFloat(
            0f, 1f, infiniteRepeatable(tween(PULSE_HALF_MS), RepeatMode.Reverse), label = "summonPulse",
        )
    }
    val label = stringResource(R.string.cd_summon, demon.name)
    DashedBubble(
        color = accent.copy(alpha = BORDER_ALPHA),
        modifier = Modifier
            .graphicsLayer {
                val p = pulse?.value ?: 1f
                alpha = PULSE_MIN_ALPHA + (1f - PULSE_MIN_ALPHA) * p
                val s = 1f + (PULSE_SCALE - 1f) * p
                scaleX = s
                scaleY = s
            }
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        Text(stringResource(R.string.quote_tap_to_summon).upper(), style = MaterialTheme.textRoles.cue, color = accent)
    }
}

/** Sealed while the circle cools: dimmed; tapping it (or him) shakes it. */
@Composable
private fun CoolingCue(accent: Color, hours: Int, shake: Int, onTap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val offset = rememberShake(shake)
    val label = pluralStringResource(R.plurals.cd_cooling, hours, hours)
    DashedBubble(
        color = colors.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = offset.value.dp.toPx() }
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(MaterialTheme.spacing.sm).clip(CircleShape).background(accent.copy(alpha = DOT_ALPHA)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.quote_cooling).upper(), style = MaterialTheme.textRoles.cue, color = colors.onSurfaceVariant)
                Text(
                    stringResource(R.string.quote_next_summon, hours).upper(),
                    style = MaterialTheme.textRoles.cue.copy(fontSize = SmallCueSize),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DashedBubble(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = GHOST_ALPHA))
            .drawBehind {
                val corner = shape.topStart.toPx(size, this)
                drawRoundRect(
                    color,
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))),
                )
            }
            .padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm + 2.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
