package com.summondemons.safely.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.RitualConfig.FOCUS_LOW_FRACTION
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** Phrase numerals, I–IV. */
val ROMAN_NUMERALS = listOf("I", "II", "III", "IV")

enum class FocusTone { Normal, Drift, Court }

private enum class MedallionState { Upcoming, Current, Sealed, Cracked }

private const val UPCOMING_ALPHA = 0.55f
private const val WAVERING_BORDER_ALPHA = 0.5f
private const val FLARE_GLOW_BLUR = 14f
private const val FLARE_HALF_MS = 250
private const val MEDALLION_FLARE_SCALE = 2.2f
private const val MEDALLION_SHADE = 0.45f
private const val FOCUS_TWEEN_MS = 120
private const val HEARTBEAT_HALF_MS = 300
private const val HEARTBEAT_MIN = 0.45f
private const val SHIMMER_MS = 2_400
private val MedallionSize = 18.dp
private val MedallionText = 8.sp
private val FocusBarHeight = 4.dp

/**
 * The framed chant panel (ADR-0014): header "Sacred chant" + [header], then one row per phrase
 * in the `incantation` role with an I–IV medallion; the focus bar sits under the current phrase.
 */
@Composable
fun ChantTablet(
    phrases: List<String>,
    phrase: Int,
    lit: Int,
    accent: Color,
    /** False while seeking/locking: the current phrase is readable but dim. */
    active: Boolean,
    header: String,
    headerColor: Color,
    /** Focus left in [0, 1] while the clock runs, else null (no bar). */
    focusFraction: Float?,
    focusTone: FocusTone,
    /** Phrase index that just sealed (flare) or cracked. */
    flare: Int?,
    cracked: Int?,
    micLevel: Float,
    wavering: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val roles = MaterialTheme.textRoles
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, if (wavering) colors.error.copy(alpha = WAVERING_BORDER_ALPHA) else colors.outlineVariant),
    ) {
        Column(Modifier.padding(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.md)) {
            Row(Modifier.fillMaxWidth().padding(bottom = spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.chant_title).upper(), style = roles.meta)
                Text(header, style = roles.meta, color = headerColor)
            }
            phrases.forEachIndexed { i, text ->
                val state = when {
                    cracked == i -> MedallionState.Cracked
                    i < phrase -> MedallionState.Sealed
                    i == phrase -> MedallionState.Current
                    else -> MedallionState.Upcoming
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Medallion(ROMAN_NUMERALS[i], state, accent, flare = flare == i)
                    PhraseText(text, state, lit, accent, active, flare = flare == i, micLevel, Modifier.weight(1f))
                }
                if (i == phrase && focusFraction != null) {
                    FocusBar(focusFraction, focusTone, accent, Modifier.padding(start = MedallionSize + spacing.sm, bottom = spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun PhraseText(
    text: String,
    state: MedallionState,
    lit: Int,
    accent: Color,
    active: Boolean,
    flare: Boolean,
    micLevel: Float,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.textRoles.incantation
    val glow = remember { Animatable(0f) }
    LaunchedEffect(flare) {
        if (flare) {
            glow.animateTo(1f, tween(FLARE_HALF_MS))
            glow.animateTo(0f, tween(FLARE_HALF_MS))
        }
    }
    if (state == MedallionState.Current) {
        KaraokeLine(
            text = text,
            lit = lit,
            accent = accent,
            dim = if (active) colors.onSurface else colors.outline,
            style = style,
            micLevel = micLevel,
            modifier = modifier,
        )
        return
    }
    val color = when (state) {
        MedallionState.Cracked -> colors.error
        MedallionState.Sealed -> accent
        else -> colors.outline.copy(alpha = UPCOMING_ALPHA)
    }
    Text(
        text,
        style = style.copy(shadow = Shadow(accent, blurRadius = FLARE_GLOW_BLUR * glow.value)),
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun Medallion(numeral: String, state: MedallionState, accent: Color, flare: Boolean) {
    val colors = MaterialTheme.colorScheme
    val highlight = MaterialTheme.demonColors.inkHighlight
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(flare, state) {
        if (flare && state == MedallionState.Sealed && !reduceMotion) {
            scale.snapTo(MEDALLION_FLARE_SCALE)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }
    val wobble = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == MedallionState.Cracked && !reduceMotion) {
            wobble.animateTo(0f, keyframes { durationMillis = 400; -12f at 100; 8f at 250 })
        }
    }
    val shade = lerp(accent, colors.surfaceContainerLowest, MEDALLION_SHADE)
    val textColor = when (state) {
        MedallionState.Sealed -> colors.onPrimary
        MedallionState.Cracked -> colors.error
        MedallionState.Current -> accent
        MedallionState.Upcoming -> colors.outlineVariant
    }
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(MedallionSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = wobble.value
            }
            .drawBehind {
                val ring = 1.dp.toPx()
                when (state) {
                    MedallionState.Sealed -> drawCircle(
                        Brush.radialGradient(
                            0f to highlight, 0.55f to accent, 1f to shade,
                            center = Offset(size.width * 0.35f, size.height * 0.3f),
                            radius = size.minDimension,
                        ),
                    )
                    MedallionState.Current -> drawCircle(accent, style = Stroke(ring))
                    MedallionState.Upcoming -> drawCircle(
                        colors.outlineVariant,
                        style = Stroke(ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))),
                    )
                    MedallionState.Cracked -> {
                        drawCircle(colors.error, style = Stroke(ring))
                        drawLine(colors.error, Offset(size.width * 0.3f, 0f), Offset(size.width * 0.7f, size.height), ring)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = MedallionText), color = textColor)
    }
}

/**
 * Focus left for the current phrase (ADR-0010): accent; `error` while wavering; a moving gold
 * shimmer when this attempt has the court bonus; a heartbeat below `FOCUS_LOW_FRACTION`.
 */
@Composable
fun FocusBar(fraction: Float, tone: FocusTone, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val highlight = MaterialTheme.demonColors.inkHighlight
    val reduceMotion = LocalReduceMotion.current
    val width by animateFloatAsState(fraction, tween(FOCUS_TWEEN_MS, easing = LinearEasing), label = "focusWidth")
    val transition = rememberInfiniteTransition(label = "focusBar")
    val beat by transition.animateFloat(
        1f, HEARTBEAT_MIN, infiniteRepeatable(tween(HEARTBEAT_HALF_MS), RepeatMode.Reverse), label = "focusBeat",
    )
    val shimmer by transition.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(SHIMMER_MS, easing = LinearEasing), RepeatMode.Restart), label = "focusShimmer",
    )
    val low = fraction < FOCUS_LOW_FRACTION
    val label = stringResource(R.string.focus_label)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(FocusBarHeight)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(colors.surfaceContainerHighest, cornerRadius = radius)
        val filled = size.width * width
        if (filled <= 0f) return@Canvas
        val brush = when (tone) {
            FocusTone.Drift -> SolidColor(colors.error)
            FocusTone.Normal -> SolidColor(accent)
            FocusTone.Court -> {
                val start = if (reduceMotion) 0f else shimmer * size.width
                Brush.horizontalGradient(listOf(accent, highlight, accent), startX = start, endX = start + size.width, tileMode = TileMode.Mirror)
            }
        }
        drawRoundRect(brush, size = Size(filled, size.height), cornerRadius = radius, alpha = if (low) beat else 1f)
    }
}
