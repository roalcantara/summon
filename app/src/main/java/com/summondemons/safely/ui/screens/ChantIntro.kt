package com.summondemons.safely.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.summondemons.safely.platform.Haptics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.ui.components.focusRing
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ui.components.ParchmentRibbon
import com.summondemons.safely.ui.fx.attentionGlow
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private const val INTRO_SLAB_WEIGHT = 0.27f
private const val INTRO_SLAB_ALPHA = 0.7f
private val SummonButtonHeight = 56.dp
private val SummonLabelSize = 20.sp
/** The button art is fixed-height: large font sizes shrink its word back to fit. */
private val ArtLabelMinSize = 8.sp
private val RuleTitleSize = 18.sp
private val RuleBodySize = 16.sp
private val RuleIcon = 40.dp
private val StepSize = 28.dp
private val StepBorder = 1.5.dp
private val StepNumberSize = 14.sp

/**
 * The intro card (ADR-0019, ADR-0021, layout B of ADR-0033): shown before every ritual and
 * from the chant screen's "?". The left slab holds the title ribbon, the singer and **Summon**
 * (where the direction pill appears next); the right column the four numbered rules, each with
 * the icon the chant screen uses. Generic: nothing names a demon or a direction. The microphone
 * is ignored while it is up (RitualViewModel); **Summon** and ✕ start the chant. Summon glows
 * until the player's first completed summon ([glowSummon]).
 */
@Composable
fun ChantIntro(
    demon: Demon,
    onSummon: () -> Unit,
    showClose: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    glowSummon: Boolean = false,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(accent, glowSummon, onSummon, Modifier.fillMaxHeight().weight(INTRO_SLAB_WEIGHT))
                Rules(accent, Modifier.fillMaxHeight().weight(1f - INTRO_SLAB_WEIGHT))
            }
        } else {
            // Large screens only (the Ritual is landscape): ribbon, rules, then Summon.
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                ParchmentRibbon(stringResource(R.string.intro_title), Modifier.semantics { heading() })
                Rules(accent, Modifier.fillMaxWidth().weight(1f))
                SummonButton(accent, glowSummon, onSummon)
            }
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(spacing.lg)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.intro_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Title ribbon over the singer over Summon. */
@Composable
private fun Slab(accent: Color, glowSummon: Boolean, onSummon: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = INTRO_SLAB_ALPHA)) {
        Column(
            Modifier.padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ParchmentRibbon(stringResource(R.string.intro_title), Modifier.semantics { heading() })
            Image(
                painter = painterResource(R.drawable.chant_intro_singer),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            SummonButton(accent, glowSummon, onSummon)
        }
    }
}

/** The four rules, centred; they scroll when large font sizes make them taller than the screen. */
@Composable
private fun Rules(accent: Color, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier.padding(horizontal = spacing.sm)) {
        // At least as tall as the slab, so SpaceBetween spreads the rules over it: rule 1's top on the
        // ribbon's line, rule 4's bottom on Summon's (the slab's md inset = xs here + sm per rule).
        // Taller (large font sizes) it scrolls, and the sm around each rule keeps ≥ 16 dp between them.
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(vertical = spacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val rule = Modifier.padding(vertical = spacing.sm)
            Box(rule) { Rule(1, accent, R.string.intro_direction_title, R.string.intro_direction_body) { CheckIcon() } }
            Box(rule) { Rule(2, accent, R.string.intro_chant_title, R.string.intro_chant_body) { SealsIcon(accent) } }
            Box(rule) { Rule(3, accent, R.string.intro_lines_title, R.string.intro_lines_body) { LinesIcon(accent) } }
            Box(rule) { Rule(4, accent, R.string.intro_focus_title, R.string.intro_focus_body) { FocusIcon(accent) } }
        }
    }
}

@Composable
private fun SummonButton(accent: Color, glow: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val label = stringResource(R.string.intro_summon).upper()
    val buttonDrawable = if (isPressed) R.drawable.chant_summon_button_pressed else R.drawable.chant_summon_button
    val inkHighlight = MaterialTheme.demonColors.inkHighlight
    val attention = rememberAttention(glow)
    Box(
        modifier = Modifier
            .attentionGlow(attention, accent)
            .height(SummonButtonHeight)
            .focusRing(interactionSource, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    Haptics.tick(context)
                    onClick()
                },
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(buttonDrawable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = label,
            style = MaterialTheme.textRoles.name.copy(fontSize = SummonLabelSize.fontScaleCapped()),
            color = inkHighlight,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = ArtLabelMinSize, maxFontSize = SummonLabelSize.fontScaleCapped()),
        )
    }
}

/** One rule: its step number, the icon that matches the chant screen, a short title, a one-line description. */
@Composable
private fun Rule(step: Int, accent: Color, title: Int, body: Int, icon: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Box(Modifier.size(StepSize).border(StepBorder, accent, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                step.toString(),
                style = MaterialTheme.textRoles.name.copy(fontSize = StepNumberSize.fontScaleCapped()),
                color = accent,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.size(RuleIcon), contentAlignment = Alignment.Center) { icon() }
        Column {
            Text(
                stringResource(title).upper(),
                style = MaterialTheme.textRoles.ritualCue.copy(fontSize = RuleTitleSize),
                color = colors.onSurface,
            )
            Text(
                stringResource(body),
                style = MaterialTheme.textRoles.body.copy(fontSize = RuleBodySize),
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckIcon() {
    val safe = MaterialTheme.demonColors.ritualSafe
    Canvas(Modifier.size(RuleIcon * 0.8f)) {
        val w = size.minDimension * 0.08f
        drawCircle(safe, radius = size.minDimension / 2f - w, style = Stroke(w))
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.52f)
            lineTo(size.width * 0.45f, size.height * 0.66f)
            lineTo(size.width * 0.72f, size.height * 0.36f)
        }
        drawPath(path, safe, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** What the line shows: a green line under the words heard, the accent line with its halo under the current one. */
@Composable
private fun LinesIcon(accent: Color) {
    val safe = MaterialTheme.demonColors.ritualSafe
    Canvas(Modifier.size(RuleIcon, RuleIcon * 0.7f)) {
        val w = size.height * 0.12f
        val y = size.height * 0.5f
        drawLine(safe, Offset(0f, y), Offset(size.width * 0.42f, y), w, StrokeCap.Round)
        drawLine(accent.copy(alpha = LINES_HALO_ALPHA), Offset(size.width * 0.55f, y), Offset(size.width, y), w * 3f, StrokeCap.Round)
        drawLine(accent, Offset(size.width * 0.55f, y), Offset(size.width, y), w, StrokeCap.Round)
    }
}

private const val LINES_HALO_ALPHA = 0.3f

/** Four small seals, the first lit and the second under way: "a finished line lights its seal". */
@Composable
private fun SealsIcon(accent: Color) {
    val dim = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(RuleIcon, RuleIcon * 0.7f)) {
        val r = size.width / 9f
        repeat(4) { k ->
            val c = Offset(r + k * (size.width - 2f * r) / 3f, size.height / 2f)
            when (k) {
                0 -> drawCircle(accent, r, c)
                1 -> drawCircle(accent, r, c, style = Stroke(r * 0.3f))
                else -> drawCircle(dim, r, c, style = Stroke(r * 0.3f))
            }
        }
    }
}

/** The focus ring, three quarters left. */
@Composable
private fun FocusIcon(accent: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.size(RuleIcon * 0.8f)) {
        val w = size.minDimension * 0.12f
        drawCircle(track, size.minDimension / 2f - w / 2f, style = Stroke(w))
        drawArc(
            accent, startAngle = -90f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w),
            style = Stroke(w, cap = StrokeCap.Round),
        )
    }
}

@Preview(name = "Landscape", device = "spec:parent=pixel_5,orientation=landscape")
@Preview(name = "Portrait")
@Composable
private fun ChantIntroPreview() {
    val demon = DemonCatalog.all.first()
    SummonDemonsSafelyTheme {
        ChantIntro(
            demon = demon,
            onSummon = {},
            showClose = true,
            onClose = {},
        )
    }
}

