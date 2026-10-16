package com.summondemons.safely.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

/** Accent token; resolved to a color by `DemonColors.accent` (DESIGN.md). */
enum class DemonAccent { Asmodeus, Astaroth }

/** A named dossier line: an ability or the weakness (ADR-0011). */
data class Ability(val name: String, val line: String)

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val quote: String,
    /** Said instead of [quote] for a viewing that began while he was restless (ADR-0015). */
    val restlessQuote: String,
    val accent: DemonAccent,
    /** Goetia number, shown on the card medallion. */
    val number: Int,
    val rank: String,
    val legions: Int,
    val house: House,
    /** The king he serves; null means "He swears to no king". */
    val liege: String?,
    val calling: Calling,
    /** Compass degrees in [0, 360) the player must face (ADR-0006). */
    val ritualHeading: Float,
    /** Exactly 4 phrases (ADR-0005). */
    val chantPhrases: List<String>,
    val appearance: String,
    val offices: List<String>,
    val warning: String,
    /** Exactly 3. */
    val abilities: List<Ability>,
    val weakness: Ability,
    /** Readable only after a flawless summon (ADR-0013). */
    val secret: String,
    /** Full scene with background (dimmed backdrop behind a bound demon on the Grimoire stage). */
    @param:DrawableRes val sceneArt: Int,
    /** Character on a transparent background (Grimoire stage, climax, shared element). */
    @param:DrawableRes val cutoutArt: Int,
    /** Circular portrait (Grimoire avatar rail once bound). */
    @param:DrawableRes val avatarArt: Int,
    /** Stroke data from `tools/sigil_to_raw.py` (ADR-0009). */
    @param:RawRes val sigil: Int,
    /** Looping ambient sound played while the ritual is locked. */
    @param:RawRes val ambientBed: Int,
) {
    init {
        require(chantPhrases.size == 4) { "$id must have exactly 4 chant phrases" }
        require(ritualHeading in 0f..<360f) { "$id ritualHeading must be in [0, 360)" }
        require(abilities.size == 3) { "$id must have exactly 3 abilities" }
        require(offices.isNotEmpty()) { "$id needs at least one office" }
    }
}
