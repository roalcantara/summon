package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.summondemons.safely.data.DemonAccent

/** DESIGN.md tokens with no Material slot. */
@Immutable
data class DemonColors(
    val asmodeus: Color,
    val astaroth: Color,
    val ritualSafe: Color,
) {
    /** The only place an accent is resolved to a color; UI never branches on demon id. */
    fun accent(accent: DemonAccent): Color = when (accent) {
        DemonAccent.Asmodeus -> asmodeus
        DemonAccent.Astaroth -> astaroth
    }
}

internal val DefaultDemonColors = DemonColors(
    asmodeus = Palette.demonAsmodeus,
    astaroth = Palette.demonAstaroth,
    ritualSafe = Palette.ritualSafe,
)

val LocalDemonColors = staticCompositionLocalOf { DefaultDemonColors }

val MaterialTheme.demonColors: DemonColors
    @Composable @ReadOnlyComposable get() = LocalDemonColors.current
