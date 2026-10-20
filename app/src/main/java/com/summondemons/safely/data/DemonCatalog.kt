package com.summondemons.safely.data

import com.summondemons.safely.R

/** The MVP demons. Text lives in res/values/demons.xml (verbatim from PRODUCT.md). */
object DemonCatalog {

    val all: List<Demon> = listOf(
        Demon(
            id = "asmodeus",
            name = "Asmodeus",
            title = R.string.demon_asmodeus_title,
            description = R.string.demon_asmodeus_description,
            quote = R.string.demon_asmodeus_quote,
            restlessQuote = R.string.demon_asmodeus_restless_quote,
            accent = DemonAccent.Asmodeus,
            number = 32,
            rank = R.string.demon_asmodeus_rank,
            legions = 72,
            house = House.Sol,
            liege = R.string.demon_asmodeus_liege,
            calling = Calling.Tutor,
            ritualHeading = 90f,
            chantPhrases = listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            appearance = R.string.demon_asmodeus_appearance,
            offices = R.array.demon_asmodeus_offices,
            warning = R.string.demon_asmodeus_warning,
            abilities = listOf(
                Ability(R.string.demon_asmodeus_ability_1_name, R.string.demon_asmodeus_ability_1_line),
                Ability(R.string.demon_asmodeus_ability_2_name, R.string.demon_asmodeus_ability_2_line),
                Ability(R.string.demon_asmodeus_ability_3_name, R.string.demon_asmodeus_ability_3_line),
            ),
            weakness = Ability(R.string.demon_asmodeus_weakness_name, R.string.demon_asmodeus_weakness_line),
            secret = R.string.demon_asmodeus_secret,
            sceneArt = R.drawable.demon_asmodeus_full,
            cutoutArt = R.drawable.demon_asmodeus_cutout,
            avatarArt = R.drawable.avatar_asmodeus,
            sigil = R.raw.sigil_asmodeus,
            ambientBed = R.raw.bed_asmodeus,
        ),
        Demon(
            id = "astaroth",
            name = "Astaroth",
            title = R.string.demon_astaroth_title,
            description = R.string.demon_astaroth_description,
            quote = R.string.demon_astaroth_quote,
            restlessQuote = R.string.demon_astaroth_restless_quote,
            accent = DemonAccent.Astaroth,
            number = 29,
            rank = R.string.demon_astaroth_rank,
            legions = 40,
            house = House.Venus,
            liege = null,
            calling = Calling.Oracle,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "TUI GRATIA LOVIS", // verbatim, see ADR-0005
                "GRATIA SIT CURA.",
                "HIC EST ME DIGNUS LEGI",
                "SINT BENE OBEDIENTES.",
            ),
            appearance = R.string.demon_astaroth_appearance,
            offices = R.array.demon_astaroth_offices,
            warning = R.string.demon_astaroth_warning,
            abilities = listOf(
                Ability(R.string.demon_astaroth_ability_1_name, R.string.demon_astaroth_ability_1_line),
                Ability(R.string.demon_astaroth_ability_2_name, R.string.demon_astaroth_ability_2_line),
                Ability(R.string.demon_astaroth_ability_3_name, R.string.demon_astaroth_ability_3_line),
            ),
            weakness = Ability(R.string.demon_astaroth_weakness_name, R.string.demon_astaroth_weakness_line),
            secret = R.string.demon_astaroth_secret,
            sceneArt = R.drawable.demon_astaroth_full,
            cutoutArt = R.drawable.demon_astaroth_cutout,
            avatarArt = R.drawable.avatar_astaroth,
            sigil = R.raw.sigil_astaroth,
            ambientBed = R.raw.bed_astaroth,
        ),
    )

    val ids: List<String> = all.map { it.id }

    fun byId(id: String): Demon? = all.find { it.id == id }
}
