package com.summondemons.safely.data

import androidx.annotation.DrawableRes

/** Accent token; resolved to a color by `DemonColors.accent` (DESIGN.md). */
enum class DemonAccent { Asmodeus, Astaroth }

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val quote: String,
    val accent: DemonAccent,
    /** Compass degrees in [0, 360) the player must face (ADR-0006). */
    val ritualHeading: Float,
    /** Exactly 4 phrases (ADR-0005). */
    val chantPhrases: List<String>,
    @param:DrawableRes val cardArt: Int,
    @param:DrawableRes val profileArt: Int,
    /** Null → `SigilView` draws the Canvas fallback. */
    @param:DrawableRes val sigil: Int?,
) {
    init {
        require(chantPhrases.size == 4) { "$id must have exactly 4 chant phrases" }
        require(ritualHeading in 0f..<360f) { "$id ritualHeading must be in [0, 360)" }
    }
}
