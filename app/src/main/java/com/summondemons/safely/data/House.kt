package com.summondemons.safely.data

import androidx.annotation.StringRes
import com.summondemons.safely.R
import com.summondemons.safely.progress.ProgressConfig.SUNRISE_HOUR
import com.summondemons.safely.progress.ProgressConfig.SUNSET_HOUR

/**
 * A demon's House (ADR-0011, ADR-0012). It carries only what is shown: the planet (badge
 * label), the court hours (local hours; each range's end is exclusive) and the badge
 * tooltip's string resource. Its metal color is resolved by `DemonColors.metal`.
 */
enum class House(val planet: String, val court: List<IntRange>, @param:StringRes val tooltip: Int) {
    Sol(
        planet = "Sol",
        court = listOf(9 until 12, 15 until SUNSET_HOUR),
        tooltip = R.string.house_sol_tooltip,
    ),
    Venus(
        planet = "Venus",
        court = listOf(SUNRISE_HOUR until 12),
        tooltip = R.string.house_venus_tooltip,
    ),
}
