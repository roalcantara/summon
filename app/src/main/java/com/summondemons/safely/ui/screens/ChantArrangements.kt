package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
/** As tall as the round buttons' touch target. */
private val TopBarHeight = 48.dp
private val BandStackWidth = 160.dp
private val BandStackHeight = 220.dp

/**
 * Landscape (ADR-0029 amended 2026-10-22): "?" (and "D") in the top bar, ✕ drawn by the screen;
 * below, the left stack (name plate, sigil, direction pill), the vertical I–IV track, and the
 * slot over the line. Both columns are top-aligned, so the slot starts level with the name.
 */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.md, modifier = Modifier.fillMaxHeight().weight(SIGIL_WEIGHT))
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) { parts.track() }
            Column(Modifier.fillMaxHeight().weight(1f - SIGIL_WEIGHT), horizontalAlignment = Alignment.CenterHorizontally) {
                parts.slot()
                parts.line(Modifier.fillMaxWidth().weight(1f).padding(top = spacing.xl))
            }
        }
    }
}

/** Large screens that ignore the rotation: the top bar, a band with the stack and the track, then the slot and the line. */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterHorizontally)) {
            SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.sm, modifier = Modifier.size(BandStackWidth, BandStackHeight))
            parts.track()
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f).padding(top = spacing.xl))
        }
    }
}

@Composable
private fun TopBar(parts: ChantParts) {
    Box(Modifier.fillMaxWidth().height(TopBarHeight)) {
        Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
    }
}
