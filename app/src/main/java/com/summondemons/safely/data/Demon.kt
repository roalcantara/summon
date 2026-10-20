package com.summondemons.safely.data

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes

/** Accent token; resolved to a color by `DemonColors.accent` (DESIGN.md). */
enum class DemonAccent { Asmodeus, Astaroth }

/** A named dossier line: an ability or the weakness (ADR-0011). Text is in `demons.xml`. */
data class Ability(@param:StringRes val name: Int, @param:StringRes val line: Int)

data class Demon(
    val id: String,
    val name: String,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:StringRes val quote: Int,
    /** Said instead of [quote] for a viewing that began while he was restless (ADR-0015). */
    @param:StringRes val restlessQuote: Int,
    val accent: DemonAccent,
    /** Goetia number, shown on the card medallion. */
    val number: Int,
    @param:StringRes val rank: Int,
    val legions: Int,
    val house: House,
    /** The king he serves; null means "He swears to no king". */
    @param:StringRes val liege: Int?,
    val calling: Calling,
    /** Compass degrees in [0, 360) the player must face (ADR-0006). */
    val ritualHeading: Float,
    /** Exactly 4 phrases (ADR-0005). */
    val chantPhrases: List<String>,
    @param:StringRes val appearance: Int,
    @param:ArrayRes val offices: Int,
    @param:StringRes val warning: Int,
    /** Exactly 3. */
    val abilities: List<Ability>,
    val weakness: Ability,
    /** Readable only after a flawless summon (ADR-0013). */
    @param:StringRes val secret: Int,
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
    }
}
