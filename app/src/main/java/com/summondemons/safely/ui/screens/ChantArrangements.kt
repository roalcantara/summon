package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.summondemons.safely.ui.theme.spacing

/** The pieces (ChantPieces.kt), handed to an arrangement (ADR-0029). */
internal class ChantParts(
    val buttons: @Composable () -> Unit,
    /** ✕ (null during the climax): the HUD row's end. */
    val close: (@Composable () -> Unit)?,
    /** The demon's name plate (top of the left stack). */
    val name: @Composable () -> Unit,
    /** The I–IV progress track. */
    val track: @Composable () -> Unit,
    val pill: @Composable () -> Unit,
    val sigil: @Composable () -> Unit,
    val slot: @Composable () -> Unit,
    val line: @Composable (Modifier) -> Unit,
)

/** The sigil column's share of the width; the line takes the rest (ADR-0029). */
private const val SIGIL_WEIGHT = 0.3f
/** The HUD row: as tall as a touch target (spec 2026-10-23 §1). */
private val HudHeight = 48.dp
/** Keeps the slot centred on the column and clear of ?/D and ✕: two targets + 8 dp. */
private val HudSideInset = 104.dp
private val BandStackWidth = 160.dp
private val BandStackHeight = 220.dp

/**
 * Landscape (ADR-0029, amended 2026-10-23): no top bar. Left: the stack (name plate, sigil, pill)
 * centred in its column; the I–IV track; right: the HUD row (?/D · slot · ✕) over the line. The
 * sigil, the track and the line share the content box's vertical middle.
 */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.lg, air = spacing.xl, modifier = Modifier.fillMaxHeight().weight(SIGIL_WEIGHT))
        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) { parts.track() }
        Box(Modifier.fillMaxHeight().weight(1f - SIGIL_WEIGHT)) {
            parts.line(Modifier.fillMaxSize())
            HudRow(parts, Modifier.align(Alignment.TopCenter))
        }
    }
}

/** ?/D at the start, the slot centred, ✕ at the end, in one 48 dp row. */
@Composable
private fun HudRow(parts: ChantParts, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(HudHeight)) {
        Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
        Box(Modifier.align(Alignment.Center).padding(horizontal = HudSideInset)) { parts.slot() }
        parts.close?.let { Box(Modifier.align(Alignment.CenterEnd)) { it() } }
    }
}

/** Space the line leaves for the HUD row and its gap. */
internal val LineReservedTop: Dp @Composable get() = HudHeight + MaterialTheme.spacing.xl

/** Large screens that ignore the rotation: the top bar, a band with the stack and the track, then the slot and the line. */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        HudRow(parts)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterHorizontally)) {
            SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.sm, air = spacing.sm, modifier = Modifier.size(BandStackWidth, BandStackHeight))
            parts.track()
        }
        parts.line(Modifier.fillMaxWidth().weight(1f))
    }
}
