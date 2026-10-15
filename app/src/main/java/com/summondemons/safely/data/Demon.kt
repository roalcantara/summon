package com.summondemons.safely.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

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
    /** Full scene with background (Grimoire and Profile backdrop). */
    @param:DrawableRes val sceneArt: Int,
    /** Character on a transparent background (hero art, climax, shared element). */
    @param:DrawableRes val cutoutArt: Int,
    /** Circular avatar (Grimoire page indicator). */
    @param:DrawableRes val avatarArt: Int,
    /** Stroke data from `tools/sigil_to_raw.py` (ADR-0009). */
    @param:RawRes val sigil: Int,
    /** Looping ambient sound played while the ritual is locked. */
    @param:RawRes val ambientBed: Int,
) {
    init {
        require(chantPhrases.size == 4) { "$id must have exactly 4 chant phrases" }
        require(ritualHeading in 0f..<360f) { "$id ritualHeading must be in [0, 360)" }
    }
}
