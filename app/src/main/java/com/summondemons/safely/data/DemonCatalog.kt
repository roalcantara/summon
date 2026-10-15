package com.summondemons.safely.data

import com.summondemons.safely.R

/** The MVP demons. Text is verbatim from PRODUCT.md. */
object DemonCatalog {

    val all: List<Demon> = listOf(
        Demon(
            id = "asmodeus",
            name = "Asmodeus",
            title = "Lord of Arithmetic & Hidden Wealth",
            description = "Teaches arithmetic, astronomy, geometry, and invincibility. " +
                "Answers all questions—if you can stand the smugness.",
            quote = "Three heads are better than one—especially when two want your soul!",
            accent = DemonAccent.Asmodeus,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            cardArt = R.drawable.demon_asmodeus_card,
            profileArt = R.drawable.demon_asmodeus_full,
            sigil = null, // DESIGN.md gap 1
        ),
        Demon(
            id = "astaroth",
            name = "Astaroth",
            title = "Duke of Past, Present & Future",
            description = "Detects secret desires and grants protection to the great. " +
                "Will explain his own fall at length if asked.",
            quote = "I know your past, your future, and that you didn't read the terms of service.",
            accent = DemonAccent.Astaroth,
            ritualHeading = 0f,
            chantPhrases = listOf(
                "TUI GRATIA LOVIS", // verbatim, see ADR-0005
                "GRATIA SIT CURA.",
                "HIC EST ME DIGNUS LEGI",
                "SINT BENE OBEDIENTES.",
            ),
            cardArt = R.drawable.demon_astaroth_card,
            profileArt = R.drawable.demon_astaroth_card, // DESIGN.md gap 2
            sigil = R.drawable.sigil_astaroth,
        ),
    )

    fun byId(id: String): Demon? = all.find { it.id == id }
}
