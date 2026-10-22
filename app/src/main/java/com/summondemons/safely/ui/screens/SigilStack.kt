package com.summondemons.safely.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * The sigil's diameter in a stack of name plate, sigil and pill (spec 2026-10-22 polish §2): all
 * the height the plate, the pill (null when hidden) and the gaps leave, capped by the width.
 */
internal fun stackDiameter(width: Int, height: Int, nameHeight: Int, pillHeight: Int?, gap: Int): Int {
    val pillRow = pillHeight?.let { it + gap } ?: 0
    return minOf(width, height - nameHeight - gap - pillRow).coerceAtLeast(0)
}

/**
 * Name plate over sigil over direction pill, top-aligned and centred, all as wide as the sigil,
 * which takes the height the other two leave ([stackDiameter]).
 */
@Composable
internal fun SigilStack(
    name: @Composable () -> Unit,
    sigil: @Composable () -> Unit,
    pill: @Composable () -> Unit,
    gap: Dp,
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
        val d = stackDiameter(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            nameHeight = nameM.maxIntrinsicHeight(constraints.maxWidth),
            pillHeight = reservedPill[0].takeIf { it > 0 },
            gap = gapPx,
        )
        val n = nameM.measure(Constraints.fixedWidth(d))
        val s = sigils.first().measure(Constraints.fixed(d, d))
        val p = pillM?.measure(Constraints.fixedWidth(d))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = (constraints.maxWidth - d) / 2
            var y = 0
            n.place(x, y)
            y += n.height + gapPx
            s.place(x, y)
            y += d + gapPx
            p?.place(x, y)
        }
    }
}
