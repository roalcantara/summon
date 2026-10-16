package com.summondemons.safely.data

import com.summondemons.safely.progress.ProgressConfig.SUNRISE_HOUR
import com.summondemons.safely.progress.ProgressConfig.SUNSET_HOUR

/**
 * A demon's House (ADR-0011, ADR-0012). It carries only what is shown: the planet (badge
 * label), the court hours (local hours; each range's end is exclusive) and the badge
 * tooltip. Its metal color is resolved by `DemonColors.metal`.
 */
enum class House(val planet: String, val court: List<IntRange>, val tooltip: String) {
    Sol(
        planet = "Sol",
        court = listOf(9 until 12, 15 until SUNSET_HOUR),
        tooltip = "Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.",
    ),
    Venus(
        planet = "Venus",
        court = listOf(SUNRISE_HOUR until 12),
        tooltip = "Dukes of Venus hold court from sunrise to noon, in clear weather only. Very particular.",
    ),
}
