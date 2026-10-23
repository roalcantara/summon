package com.summondemons.safely.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/** The left stack's sigil diameter and the group's top (spec 2026-10-23 §4). */
internal data class StackLayout(val diameter: Int, val top: Int)

/**
 * Name plate, sigil and pill as one group centred in the column: [gap] between them, at least
 * [air] above and below; the sigil takes what is left, capped by the width. A hidden pill
 * (null) gives up its row and gap.
 */
internal fun stackLayout(width: Int, height: Int, nameHeight: Int, pillHeight: Int?, gap: Int, air: Int): StackLayout {
    val pillRow = pillHeight?.let { it + gap } ?: 0
    val fixed = nameHeight + gap + pillRow
    val d = minOf(width, height - 2 * air - fixed).coerceAtLeast(0)
    return StackLayout(d, ((height - fixed - d) / 2).coerceAtLeast(0))
}

/** Top of a phrase [height] tall, centred on [axis] but kept inside [top]..[bottom] (spec §6). */
internal fun phraseTop(axis: Int, top: Int, bottom: Int, height: Int): Int =
    (axis - height / 2).coerceAtMost(bottom - height).coerceAtLeast(top)

/**
 * Name plate over sigil over direction pill, one group centred in the column (spec 2026-10-23 §4),
 * all as wide as the sigil, which takes the height the other two leave ([stackLayout]).
 */
@Composable
internal fun SigilStack(
    name: @Composable () -> Unit,
    sigil: @Composable () -> Unit,
    pill: @Composable () -> Unit,
    gap: Dp,
    air: Dp,
    modifier: Modifier = Modifier,
) {
    // The pill's row stays reserved once it has been seen: when it hides (climax) the sigil must
    // not jump larger mid-animation.
    val reservedPill = remember { IntArray(1) }
    Layout(contents = listOf(name, sigil, pill), modifier = modifier) { (names, sigils, pills), constraints ->
        val gapPx = gap.roundToPx()
        val nameM = names.first()
        val pillM = pills.firstOrNull()
        pillM?.let { reservedPill[0] = it.maxIntrinsicHeight(constraints.maxWidth) }
        val nameH = nameM.maxIntrinsicHeight(constraints.maxWidth)
        val s = stackLayout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            nameHeight = nameH,
            pillHeight = reservedPill[0].takeIf { it > 0 },
            gap = gapPx,
            air = air.roundToPx(),
        )
        val d = s.diameter
        val n = nameM.measure(Constraints.fixedWidth(d))
        val sg = sigils.first().measure(Constraints.fixed(d, d))
        val p = pillM?.measure(Constraints.fixedWidth(d))
        // Placed by the plate's real height at the sigil's width (it can differ from the estimate),
        // re-centred so nothing overlaps.
        val group = n.height + gapPx + d + (p?.let { gapPx + it.height } ?: 0)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = (constraints.maxWidth - d) / 2
            var y = ((constraints.maxHeight - group) / 2).coerceAtLeast(0)
            n.place(x, y)
            y += n.height + gapPx
            sg.place(x, y)
            y += d + gapPx
            p?.place(x, y)
        }
    }
}

/**
 * Lays [content] (the phrase, auto-sized) in the space from [reservedTop] to the bottom, then
 * places it with its centre on this box's vertical middle, the screen's axis, as far as that
 * space allows ([phraseTop]). [content] must wrap its height (no fillMaxSize).
 */
@Composable
internal fun AxisLine(reservedTop: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val top = reservedTop.roundToPx()
        val room = (constraints.maxHeight - top).coerceAtLeast(0)
        val placeable = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = room))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = phraseTop(axis = constraints.maxHeight / 2, top = top, bottom = constraints.maxHeight, height = placeable.height)
            placeable.place((constraints.maxWidth - placeable.width) / 2, y)
        }
    }
}
