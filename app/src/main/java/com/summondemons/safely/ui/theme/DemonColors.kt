package com.summondemons.safely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.summondemons.safely.data.DemonAccent
import com.summondemons.safely.data.House

/** The three placeholder stage backgrounds (DESIGN.md Gaps). */
enum class Stage { Home, Sol, Venus }

fun House.stage(): Stage = when (this) {
    House.Sol -> Stage.Sol
    House.Venus -> Stage.Venus
}

/** Radial stage gradient: [glow] at the light, [mid] around it, [edge] at the borders. */
@Immutable
data class StageGradient(val glow: Color, val mid: Color, val edge: Color)

/** DESIGN.md tokens with no Material slot. */
@Immutable
data class DemonColors(
    val asmodeus: Color,
    val astaroth: Color,
    val ritualSafe: Color,
    val houseSol: Color,
    val houseVenus: Color,
    val inkHighlight: Color,
    val parchmentLight: Color,
    val parchmentDark: Color,
    val parchmentEdge: Color,
    val parchmentInk: Color,
    val stageHome: StageGradient,
    val stageSol: StageGradient,
    val stageVenus: StageGradient,
) {
    /** The only place an accent is resolved to a color; UI never branches on demon id. */
    fun accent(accent: DemonAccent): Color = when (accent) {
        DemonAccent.Asmodeus -> asmodeus
        DemonAccent.Astaroth -> astaroth
    }

    /** House metal: badge rims, number medallion. */
    fun metal(house: House): Color = when (house) {
        House.Sol -> houseSol
        House.Venus -> houseVenus
    }

    fun stage(stage: Stage): StageGradient = when (stage) {
        Stage.Home -> stageHome
        Stage.Sol -> stageSol
        Stage.Venus -> stageVenus
    }
}

internal val DefaultDemonColors = DemonColors(
    asmodeus = Palette.demonAsmodeus,
    astaroth = Palette.demonAstaroth,
    ritualSafe = Palette.ritualSafe,
    houseSol = Palette.houseSol,
    houseVenus = Palette.houseVenus,
    inkHighlight = Palette.inkHighlight,
    parchmentLight = Palette.parchmentLight,
    parchmentDark = Palette.parchmentDark,
    parchmentEdge = Palette.parchmentEdge,
    parchmentInk = Palette.parchmentInk,
    stageHome = StageGradient(Palette.stageHomeGlow, Palette.stageHomeMid, Palette.surfaceContainerLowest),
    stageSol = StageGradient(Palette.stageSolGlow, Palette.stageSolMid, Palette.background),
    stageVenus = StageGradient(Palette.stageVenusGlow, Palette.stageVenusMid, Palette.background),
)

val LocalDemonColors = staticCompositionLocalOf { DefaultDemonColors }

val MaterialTheme.demonColors: DemonColors
    @Composable @ReadOnlyComposable get() = LocalDemonColors.current
