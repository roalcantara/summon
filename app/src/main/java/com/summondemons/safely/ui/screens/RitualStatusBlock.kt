package com.summondemons.safely.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.headingLabel
import com.summondemons.safely.ui.components.ROMAN_NUMERALS
import com.summondemons.safely.ui.fx.LocalReduceMotion
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private val StepLabelSize = 15.sp
private val SealNumeralSize = 11.sp
private val SealSize = 30.dp
private val RowMinHeight = 40.dp
private val FrameWidth = 2.dp
private const val DIM_ALPHA = 0.45f
private const val BREATH_MIN = 0.55f
private const val BREATH_HALF_MS = 900
private const val BLINK_MIN = 0.55f
private const val BLINK_HALF_MS = 300
private const val FLARE_SCALE = 1.18f
private const val FLARE_MS = 450
private const val PART_GAP_DEG = 14f

/**
 * The Compass-and-Tally block under the sigil (ADR-0024): one frame, two rows. Both labels share
 * one muted style; only the step numbers, the arrow and the frame carry state.
 */
@Composable
internal fun RitualStatusBlock(status: StatusBlock, target: Float, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val safe = MaterialTheme.demonColors.ritualSafe
    val frame = when (status.step) {
        StepState.Seeking -> colors.outlineVariant
        StepState.Aligned -> safe
        StepState.Drifting -> colors.error
        StepState.Done -> accent
    }
    val one = when (status.step) {
        StepState.Seeking -> colors.primary
        StepState.Drifting -> colors.error
        StepState.Aligned, StepState.Done -> safe
    }
    val two = when (status.step) {
        StepState.Aligned -> colors.primary
        StepState.Done -> safe
        StepState.Seeking, StepState.Drifting -> colors.outlineVariant
    }
    val label = MaterialTheme.textRoles.ritualCue.copy(fontSize = StepLabelSize, letterSpacing = 0.12.em)
    Column(
        modifier
            .fillMaxWidth()
            .border(FrameWidth, frame, MaterialTheme.shapes.medium)
            .padding(horizontal = MaterialTheme.spacing.md),
    ) {
        DirectionRow(status, target, one, label)
        HorizontalDivider(color = colors.outlineVariant)
        ChantRow(status, two, accent, label)
    }
}

@Composable
private fun DirectionRow(status: StatusBlock, target: Float, stateColor: Color, label: TextStyle) {
    val reduceMotion = LocalReduceMotion.current
    val blink = if (status.step == StepState.Drifting && !reduceMotion) {
        rememberInfiniteTransition(label = "step1").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "step1Blink",
        ).value
    } else {
        1f
    }
    val aligned = status.step == StepState.Aligned || status.step == StepState.Done
    val direction = headingLabel(target)
    val description = when {
        aligned -> stringResource(R.string.chant_a11y_facing, direction)
        status.turn == TurnDirection.Right -> stringResource(R.string.chant_a11y_turn_right, direction)
        else -> stringResource(R.string.chant_a11y_turn_left, direction)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .graphicsLayer { alpha = blink }
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepLabel("1.", stringResource(R.string.chant_face, direction), stateColor, label, Modifier.weight(1f, fill = false))
        val icon = when {
            aligned -> "✓"
            status.turn == TurnDirection.Right -> "→"
            else -> "←"
        }
        Text(icon, style = label, color = stateColor)
    }
}

@Composable
private fun ChantRow(status: StatusBlock, stateColor: Color, accent: Color, label: TextStyle) {
    val sealed = status.seals.count { it.look == SealLook.Sealed }
    val description = stringResource(R.string.chant_a11y_seals, sealed)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepLabel("2.", stringResource(R.string.chant_step_chant), stateColor, label, Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            status.seals.forEachIndexed { i, seal ->
                Seal(ROMAN_NUMERALS[i], seal, accent, dimmed = status.step == StepState.Drifting, checking = status.checking, modifier = Modifier.size(SealSize))
            }
        }
    }
}

/** "1. FACE EAST": the number in [numberColor], the words always in the muted outline colour. */
@Composable
private fun StepLabel(number: String, text: String, numberColor: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = numberColor)) { append(number) }
            append(" ")
            append(text.upper())
        },
        style = style,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A numeral disc inside a ring split into one arc per chunk; passed arcs in the accent. */
@Composable
private fun Seal(numeral: String, seal: SealState, accent: Color, dimmed: Boolean, checking: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val current = seal.look == SealLook.Current
    val breath = if (current && !dimmed && !reduceMotion) {
        rememberInfiniteTransition(label = "seal").animateFloat(
            1f, BREATH_MIN, infiniteRepeatable(tween(if (checking) BREATH_HALF_MS / 3 else BREATH_HALF_MS), RepeatMode.Reverse), label = "sealBreath",
        ).value
    } else {
        1f
    }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(seal.look) {
        if (seal.look == SealLook.Sealed && !reduceMotion) {
            pulse.snapTo(FLARE_SCALE)
            pulse.animateTo(1f, tween(FLARE_MS))
        }
    }
    val lost = seal.look == SealLook.Cracked || (dimmed && current)
    val ring = when {
        lost -> colors.error
        seal.look == SealLook.Pending -> colors.outlineVariant
        else -> accent
    }
    val numeralColor = when {
        seal.look == SealLook.Sealed -> colors.surfaceContainerLowest
        lost -> colors.error
        seal.look == SealLook.Pending -> colors.outline
        else -> accent
    }
    val track = colors.surfaceContainerHighest
    val alpha = if (dimmed && !current) DIM_ALPHA else breath
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = pulse.value
            scaleY = pulse.value
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.065f
            val outer = size.minDimension / 2f - stroke / 2f
            val disc = outer * 0.72f
            val each = 360f / seal.parts
            val gap = if (seal.parts > 1) PART_GAP_DEG else 0f
            repeat(seal.parts) { k ->
                drawArc(
                    color = if (k < seal.passed) accent else track,
                    startAngle = -90f + k * each + gap / 2f,
                    sweepAngle = each - gap,
                    useCenter = false,
                    topLeft = Offset(center.x - outer, center.y - outer),
                    size = Size(outer * 2f, outer * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            if (seal.look == SealLook.Sealed) drawCircle(accent, radius = disc)
            drawCircle(ring, radius = disc, style = Stroke(stroke * 0.8f))
        }
        Text(numeral, style = MaterialTheme.textRoles.name.copy(fontSize = SealNumeralSize), color = numeralColor, maxLines = 1)
    }
}
