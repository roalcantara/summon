package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

/** The shared pieces (ChantPieces.kt), handed to an arrangement (ADR-0029). */
internal class ChantParts(
    val buttons: @Composable () -> Unit,
    /** `stacked`: the seals under the name (narrow slab) or beside it. */
    val title: @Composable (stacked: Boolean) -> Unit,
    val pill: @Composable () -> Unit,
    val sigil: @Composable (Modifier) -> Unit,
    val slot: @Composable () -> Unit,
    val line: @Composable (Modifier) -> Unit,
)

private const val SLAB_WEIGHT = 0.25f
private const val SLAB_ALPHA = 0.7f
/** Centred: the sigil's side as a fraction of the height under the top row (ADR-0029). */
private const val SIGIL_CENTRED_FRACTION = 0.38f
private val TopRowHeight = 48.dp
private val BandSigil = 96.dp

/** Today's columns (ADR-0019): a 25 % slab with buttons, title, sigil and pill; the slot and line at the right. */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Surface(
            Modifier.fillMaxHeight().weight(SLAB_WEIGHT),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SLAB_ALPHA),
        ) {
            Column(
                Modifier.padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { parts.buttons() }
                parts.title(true)
                parts.sigil(Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true))
                parts.pill()
            }
        }
        Column(
            Modifier.fillMaxHeight().weight(1f - SLAB_WEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** Stitch 02: buttons and title on top, then pill, sigil, slot and the line across the full width. */
@Composable
internal fun CentredArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier) {
        val sigil = (maxHeight - TopRowHeight) * SIGIL_CENTRED_FRACTION
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(Modifier.fillMaxWidth().height(TopRowHeight)) {
                Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
                Box(Modifier.align(Alignment.Center)) { parts.title(false) }
            }
            parts.pill()
            parts.sigil(Modifier.size(sigil))
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** Large screens that ignore the rotation: a top band, then the slot and the line (not compared). */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            parts.buttons()
            parts.sigil(Modifier.size(BandSigil))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                parts.title(true)
                parts.pill()
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f))
        }
    }
}
