package com.summondemons.safely.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

enum class BadgeKey { Direction, House, Calling, Legions }

data class BadgeInfo(val key: BadgeKey, val label: String, val text: String)

private const val RIM_ALPHA = 0.7f
private const val SELECTED_TINT = 0.22f
private const val BAND_ALPHA = 0.8f
private const val BAND_BORDER_ALPHA = 0.55f
private const val LAYER_FADE_MS = 150
private val BadgeIconSize = 24.dp
private val BandIconSize = 14.dp
private val BadgeLabelSize = 13.sp
/**
 * Large font sizes shrink the labels back towards this rather than cut one off ("72 legions").
 * It scales too: at the 200% maximum it is about the label's default size.
 */
private val BadgeLabelMinSize = 6.sp
private const val LABEL_STEP_SP = 0.5f
private val SelectedBorder = 1.5.dp
private val BandTextSize = 14.sp
private val SelectedGlow = 8.dp

/** The four facts on a demon's card with their explanations (ADR-0011). */
@Composable
fun rememberBadges(demon: Demon): List<BadgeInfo> {
    val direction = headingLabel(demon.ritualHeading)
    val liege = demon.liege?.let { stringResource(R.string.badge_liege, stringResource(it)) } ?: stringResource(R.string.badge_no_liege)
    return listOf(
        BadgeInfo(BadgeKey.Direction, direction, stringResource(R.string.badge_direction_text, direction, liege)),
        BadgeInfo(BadgeKey.House, demon.house.planet, stringResource(demon.house.tooltip)),
        BadgeInfo(BadgeKey.Calling, stringResource(demon.calling.label), stringResource(demon.calling.tooltip)),
        BadgeInfo(
            BadgeKey.Legions,
            pluralStringResource(R.plurals.badge_legions_label, demon.legions, demon.legions),
            pluralStringResource(R.plurals.badge_legions_text, demon.legions, stringResource(demon.rank), demon.legions),
        ),
    )
}

/** Glyph in the demon accent (the rim carries the House metal). */
@Composable
fun BadgeIcon(key: BadgeKey, demon: Demon, size: Dp) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    when (key) {
        BadgeKey.Direction -> DirectionGlyph(demon.ritualHeading, headingLetter(demon.ritualHeading), accent, size)
        BadgeKey.House -> HouseGlyph(demon.house, MaterialTheme.demonColors.metal(demon.house), size)
        BadgeKey.Calling -> CallingGlyph(demon.calling, accent, size)
        BadgeKey.Legions -> LegionsGlyph(accent, size)
    }
}

/** Four equal tiles; tapping one selects it (tapping it again deselects). */
@Composable
fun BadgeRow(demon: Demon, selected: BadgeKey?, onSelect: (BadgeKey?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val rim = MaterialTheme.demonColors.metal(demon.house)
    val shape = MaterialTheme.shapes.small
    val spacing = MaterialTheme.spacing
    val badges = rememberBadges(demon)
    val labelStyle = MaterialTheme.textRoles.name
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labelWidth = with(LocalDensity.current) {
            ((maxWidth - spacing.sm * (badges.size - 1)) / badges.size - spacing.xs * 2 - SelectedBorder * 2).roundToPx()
        }
        val labels = badges.map { it.label }
        val labelSize = remember(labels, labelWidth, labelStyle) { sharedLabelSize(measurer, labels, labelStyle, labelWidth) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            badges.forEach { badge ->
                val on = badge.key == selected
                val explain = stringResource(R.string.badge_explain, badge.label)
                Column(
                    Modifier
                        .weight(1f)
                        .then(if (on) Modifier.shadow(SelectedGlow, shape, ambientColor = accent, spotColor = accent) else Modifier)
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (on) lerp(colors.surfaceContainerHigh, accent, SELECTED_TINT) else colors.surfaceContainerHigh,
                                    colors.surfaceContainerLow,
                                ),
                            ),
                        )
                        .border(if (on) SelectedBorder else 1.dp, if (on) accent else rim.copy(alpha = RIM_ALPHA), shape)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(if (on) null else badge.key) })
                        .semantics { contentDescription = explain }
                        .padding(vertical = spacing.sm, horizontal = spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    BadgeIcon(badge.key, demon, BadgeIconSize)
                    Text(
                        badge.label,
                        style = labelStyle.copy(fontSize = labelSize),
                        color = colors.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}

/**
 * The largest size, from [BadgeLabelSize] down to [BadgeLabelMinSize], at which every label fits
 * one line of [widthPx]: the four tiles share it so the row reads as one set, at any font scale.
 */
private fun sharedLabelSize(measurer: TextMeasurer, labels: List<String>, style: TextStyle, widthPx: Int): TextUnit {
    var size = BadgeLabelSize.value
    fun fits() = labels.all { measurer.measure(it, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width <= widthPx }
    while (size > BadgeLabelMinSize.value && !fits()) size -= LABEL_STEP_SP
    return size.sp
}

/**
 * The description, or the selected badge's explanation. Every demon's texts are stacked in
 * one Box, only the current one visible and in semantics, so the band is always as tall as the
 * tallest text and nothing below it moves (ADR-0016).
 */
@Composable
fun InfoBand(demons: List<Demon>, demon: Demon, badge: BadgeKey?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val roles = MaterialTheme.textRoles
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val shape = MaterialTheme.shapes.small
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = BAND_ALPHA))
            .then(if (badge != null) Modifier.border(1.dp, accent.copy(alpha = BAND_BORDER_ALPHA), shape) else Modifier)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        demons.forEach { d ->
            val current = d.id == demon.id
            BandLayer(visible = current && badge == null) {
                Text(
                    stringResource(d.description),
                    style = roles.body.copy(fontSize = BandTextSize, fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                )
            }
            rememberBadges(d).forEach { info ->
                BandLayer(visible = current && badge == info.key) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = MaterialTheme.spacing.xs),
                        ) {
                            BadgeIcon(info.key, d, BandIconSize)
                            Text(info.label.upper(), style = roles.meta.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.demonColors.accent(d.accent))
                        }
                        Text(info.text, style = roles.body.copy(fontSize = BandTextSize), color = colors.onSurface, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun BandLayer(visible: Boolean, content: @Composable () -> Unit) {
    // A cross-fade in every motion setting (reduced motion swaps slides for cross-fades, §4).
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(LAYER_FADE_MS), label = "bandLayer")
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { }),
        contentAlignment = Alignment.Center,
    ) { content() }
}
