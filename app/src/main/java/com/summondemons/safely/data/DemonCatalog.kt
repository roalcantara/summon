package com.summondemons.safely.data

import com.summondemons.safely.R

/** The MVP demons. Text is verbatim from PRODUCT.md. */
object DemonCatalog {

    val all: List<Demon> = listOf(
        Demon(
            id = "asmodeus",
            name = "Asmodeus",
            title = "Lord of Arithmetic & Hidden Wealth",
            description = "Teaches arithmetic, astronomy, geometry, and invisibility. " +
                "Answers all questions—if you can stand the smugness.",
            quote = "Three heads are better than one—especially when two want your soul!",
            restlessQuote = "I've counted every grain of dust in here. Twice. In three bases.",
            accent = DemonAccent.Asmodeus,
            number = 32,
            rank = "Great King",
            legions = 72,
            house = House.Sol,
            liege = "Amaymon, King of the East",
            calling = Calling.Tutor,
            ritualHeading = 90f,
            chantPhrases = listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            appearance = "Three heads (bull, man and ram), so every meeting is a committee. He breathes fire, " +
                "swishes a serpent’s tail and waddles on goose feet. He arrives on a dragon with a lance and " +
                "a banner, because subtle is for Dukes.",
            offices = listOf(
                "Hands out the Ring of Virtues (one per customer).",
                "Tutors arithmetic, astronomy, geometry and every handicraft.",
                "Answers any question fully, with appendices.",
                "Makes you invisible.",
                "Knows where treasure lies, and guards it.",
            ),
            warning = "Summon him outdoors, stand the whole time, and take your hat off. Keep it on and his " +
                "boss Amaymon pranks your entire ritual.",
            abilities = listOf(
                Ability("Ring of Virtues", "Gives you a ring. The virtues are sold separately."),
                Ability("Now You Don’t", "Turns you invisible. Great for heists, terrible for selfies."),
                Ability("Dragon’s Ledger", "Finds buried treasure, counts it, then sits on it."),
            ),
            weakness = Ability(
                "Name-Drop",
                "Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.",
            ),
            secret = "In an older tale, the smoke of a fish’s heart and liver sent him fleeing all the way to " +
                "Egypt. He does not eat seafood. He does not discuss it.",
            sceneArt = R.drawable.demon_asmodeus_full,
            cutoutArt = R.drawable.demon_asmodeus_cutout,
            avatarArt = R.drawable.avatar_asmodeus,
            sigil = R.raw.sigil_asmodeus,
            ambientBed = R.raw.bed_asmodeus,
        ),
        Demon(
            id = "astaroth",
            name = "Astaroth",
            title = "Duke of Past, Present & Future",
            description = "Knows every secret and every tense. Will explain his own fall at length if asked.",
            quote = "I know your past, your future, and that you didn't read the terms of service.",
            restlessQuote = "I foresaw you'd forget me. I hate being right. No I don't.",
            accent = DemonAccent.Astaroth,
            number = 29,
            rank = "Mighty Duke",
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
            appearance = "An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand " +
                "like a fancy pen. His breath arrives before he does.",
            offices = listOf(
                "True answers about past, present and future.",
                "Uncovers every secret, including the ones written in pencil.",
                "Explains how the spirits fell, and why his own fall wasn’t his fault.",
                "Makes you a scholar of all the liberal arts.",
            ),
            warning = "Never let him get close; his breath is a weapon. Hold your magic ring up to your face " +
                "like a tiny gas mask and nod politely.",
            abilities = listOf(
                Ability("All Three Tenses", "He’s read ahead and won’t stop hinting."),
                Ability("Open Secret", "Every secret you have, now his favourite trivia."),
                Ability("The Fall (Director’s Cut)", "A complete, unrequested account. Spoiler: someone else pushed him."),
            ),
            weakness = Ability(
                "Dragon Breath",
                "Brave summoners hold a ring to their nose and win. He respects personal space only when it’s " +
                    "enforced with jewellery.",
            ),
            secret = "Before the dragon and the breath, his name belonged to Astarte, a goddess of love and war. " +
                "He’d rather you didn’t bring it up.",
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
