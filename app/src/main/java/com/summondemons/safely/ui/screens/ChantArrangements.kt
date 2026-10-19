package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

/** The pieces (ChantPieces.kt), handed to an arrangement (ADR-0029). */
internal class ChantParts(
    val buttons: @Composable () -> Unit,
    val title: @Composable () -> Unit,
    val pill: @Composable () -> Unit,
    val sigil: @Composable (Modifier) -> Unit,
    val slot: @Composable () -> Unit,
    val line: @Composable (Modifier) -> Unit,
)

/** The sigil column's share of the width; the line takes the rest (ADR-0029). */
private const val SIGIL_WEIGHT = 0.3f
/** As tall as the round buttons' touch target, so ✕ and "?" line up with the title. */
private val TopBarHeight = 48.dp
private val BandSigil = 96.dp

/**
 * Landscape (ADR-0029, after Stitch "stage HUD"): "?" left and the title centred in a top bar
 * (✕ is drawn by the screen, top right); below, the sigil with its direction pill on the left and
 * the feedback slot over the line on the right. No cards: everything floats over the stage.
 */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Column(
                Modifier.fillMaxHeight().weight(SIGIL_WEIGHT),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                parts.sigil(Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true))
                parts.pill()
            }
            Column(
                Modifier.fillMaxHeight().weight(1f - SIGIL_WEIGHT),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                parts.slot()
                parts.line(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** Large screens that ignore the rotation: the top bar, a band with the sigil and pill, then the slot and the line. */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterHorizontally)) {
            parts.sigil(Modifier.size(BandSigil))
            parts.pill()
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun TopBar(parts: ChantParts) {
    Box(Modifier.fillMaxWidth().height(TopBarHeight)) {
        Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
        Box(Modifier.align(Alignment.Center)) { parts.title() }
    }
}
