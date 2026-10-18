package com.summondemons.safely.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.stage
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private const val INTRO_SLAB_WEIGHT = 0.25f
private const val INTRO_SLAB_ALPHA = 0.7f
private const val ART_FRACTION = 0.8f
private val BannerTitleColor = Color(0xFF3A2400)
private val BannerHeight = 56.dp
private val BannerTitleSize = 24.sp
private val SummonButtonHeight = 56.dp
private val SummonLabelSize = 20.sp
private val RuleTitleSize = 18.sp
private val RuleBodySize = 16.sp
private val RuleIcon = 40.dp
private val BandHeight = 112.dp

/**
 * The intro card (ADR-0019): shown before every ritual, and from the chant screen's "?"
 * (ADR-0021). Mirrors the chant screen: art where the sigil goes, the rules where the
 * words go. Generic: nothing in it names a demon or a direction. The microphone is ignored
 * while it is up (RitualViewModel); **Summon** and ✕ start the chant.
 *
 * Art: `chant_intro_singer.webp` on the slab, `chant_intro_banner.webp` for the header,
 * and `chant_summon_button.webp` / `_pressed.webp` for the action (DESIGN.md, ADR-0019).
 */
@Composable
fun ChantIntro(
    demon: Demon,
    onSummon: () -> Unit,
    showClose: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                ArtSlab(Modifier.fillMaxHeight().weight(INTRO_SLAB_WEIGHT))
                Rules(accent, onSummon, Modifier.fillMaxHeight().weight(1f - INTRO_SLAB_WEIGHT))
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                ArtSlab(Modifier.fillMaxWidth().height(BandHeight))
                Rules(accent, onSummon, Modifier.fillMaxWidth().weight(1f))
            }
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.intro_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArtSlab(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = INTRO_SLAB_ALPHA)) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.chant_intro_singer),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(ART_FRACTION),
            )
        }
    }
}

@Composable
private fun Rules(accent: Color, onSummon: () -> Unit, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier.padding(horizontal = spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically)) {
        Box(
            modifier = Modifier
                .height(BannerHeight)
                .semantics { heading() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chant_intro_banner),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            Text(
                text = stringResource(R.string.intro_title).upper(),
                style = MaterialTheme.textRoles.name.copy(fontSize = BannerTitleSize),
                color = BannerTitleColor,
                modifier = Modifier.padding(horizontal = spacing.xl),
                maxLines = 1,
            )
        }
        Rule(R.string.intro_direction_title, R.string.intro_direction_body) { CheckIcon() }
        Rule(R.string.intro_chant_title, R.string.intro_chant_body) { SealsIcon(accent) }
        Rule(R.string.intro_lines_title, R.string.intro_lines_body) { LinesIcon() }
        Rule(R.string.intro_focus_title, R.string.intro_focus_body) { FocusIcon(accent) }
        SummonButton(onClick = onSummon)
    }
}

@Composable
private fun SummonButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val label = stringResource(R.string.intro_summon).upper()
    val buttonDrawable = if (isPressed) R.drawable.chant_summon_button_pressed else R.drawable.chant_summon_button
    val inkHighlight = MaterialTheme.demonColors.inkHighlight
    Box(
        modifier = Modifier
            .height(SummonButtonHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
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
            style = MaterialTheme.textRoles.name.copy(fontSize = SummonLabelSize),
            color = inkHighlight,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl),
            maxLines = 1,
        )
    }
}

/** One rule: an icon that matches the chant screen, a short title, a one-line description. */
@Composable
private fun Rule(title: Int, body: Int, icon: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
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

@Composable
private fun LinesIcon() {
    val safe = MaterialTheme.demonColors.ritualSafe
    val error = MaterialTheme.colorScheme.error
    Canvas(Modifier.size(RuleIcon, RuleIcon * 0.7f)) {
        val w = size.height * 0.12f
        drawLine(safe, Offset(0f, size.height * 0.3f), Offset(size.width * 0.42f, size.height * 0.3f), w, StrokeCap.Round)
        val path = Path().apply {
            val y = size.height * 0.72f
            moveTo(size.width * 0.55f, y)
            var x = size.width * 0.55f
            var up = true
            val step = size.width * 0.11f
            while (x < size.width) {
                val next = minOf(x + step, size.width)
                quadraticTo((x + next) / 2f, if (up) y - w * 1.5f else y + w * 1.5f, next, y)
                x = next
                up = !up
            }
        }
        drawPath(path, error, style = Stroke(w * 0.8f, cap = StrokeCap.Round))
    }
}

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
