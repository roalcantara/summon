package com.summondemons.safely.data

import com.summondemons.safely.R
import com.summondemons.safely.i18n.en
import com.summondemons.safely.i18n.enArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemonCatalogTest {

    private val asmodeus = DemonCatalog.byId("asmodeus")!!
    private val astaroth = DemonCatalog.byId("astaroth")!!

    @Test
    fun containsAsmodeusAndAstarothWithUniqueIds() {
        assertEquals(listOf("asmodeus", "astaroth"), DemonCatalog.all.map { it.id })
    }

    @Test
    fun everyDemonHasFourPhrasesThreeAbilitiesAndADossier() {
        DemonCatalog.all.forEach { demon ->
            assertEquals(4, demon.chantPhrases.size)
            assertEquals(3, demon.abilities.size)
            assertTrue(enArray(demon.offices).isNotEmpty())
            val texts = listOf(demon.title, demon.description, demon.quote, demon.restlessQuote, demon.appearance,
                demon.warning, demon.secret, demon.rank, demon.weakness.name, demon.weakness.line) +
                demon.abilities.flatMap { listOf(it.name, it.line) }
            texts.forEach { assertTrue("${demon.id}: blank text", en(it).isNotBlank()) }
            assertTrue(demon.ritualHeading >= 0f && demon.ritualHeading < 360f)
        }
    }

    @Test
    fun headingsAreEastAndNorth() {
        assertEquals(90f, asmodeus.ritualHeading)
        assertEquals(0f, astaroth.ritualHeading)
    }

    @Test
    fun asmodeusMatchesProductSpec() {
        assertEquals("Lord of Arithmetic & Hidden Wealth", en(asmodeus.title))
        assertEquals(32, asmodeus.number)
        assertEquals("Great King", en(asmodeus.rank))
        assertEquals(72, asmodeus.legions)
        assertEquals(House.Sol, asmodeus.house)
        assertEquals("Amaymon, King of the East", en(asmodeus.liege!!))
        assertEquals(Calling.Tutor, asmodeus.calling)
        assertEquals(
            "Teaches arithmetic, astronomy, geometry, and invisibility. Answers all questions—if you can stand the smugness.",
            en(asmodeus.description),
        )
        assertEquals("I've counted every grain of dust in here. Twice. In three bases.", en(asmodeus.restlessQuote))
        assertEquals(
            listOf(
                "DAEMONIUM RESIDENTE,",
                "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
                "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
                "UT VOTA OMNIA OPUS EST.",
            ),
            asmodeus.chantPhrases,
        )
    }

    @Test
    fun astarothMatchesProductSpec() {
        assertEquals("Duke of Past, Present & Future", en(astaroth.title))
        assertEquals(29, astaroth.number)
        assertEquals("Mighty Duke", en(astaroth.rank))
        assertEquals(40, astaroth.legions)
        assertEquals(House.Venus, astaroth.house)
        assertNull(astaroth.liege)
        assertEquals(Calling.Oracle, astaroth.calling)
        assertEquals("Knows every secret and every tense. Will explain his own fall at length if asked.", en(astaroth.description))
        assertEquals("I know your past, your future, and that you didn't read the terms of service.", en(astaroth.quote))
        assertEquals("I foresaw you'd forget me. I hate being right. No I don't.", en(astaroth.restlessQuote))
        assertEquals(
            listOf("TUI GRATIA LOVIS", "GRATIA SIT CURA.", "HIC EST ME DIGNUS LEGI", "SINT BENE OBEDIENTES."),
            astaroth.chantPhrases,
        )
    }

    @Test
    fun housesHaveTheirPlanetsAndCourtHours() {
        assertEquals("Sol", House.Sol.planet)
        assertEquals(listOf(9..11, 15..17), House.Sol.court)
        assertEquals("Venus", House.Venus.planet)
        assertEquals(listOf(6..11), House.Venus.court)
    }

    @Test
    fun housesAndCallingsHaveEnglishText() {
        assertEquals("Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.", en(House.Sol.tooltip))
        assertEquals(
            "Dukes of Venus hold court from sunrise to noon, in clear weather only. Very particular.",
            en(House.Venus.tooltip),
        )
        assertEquals(
            listOf("Warlord", "Tutor", "Oracle", "Treasurer", "Menace"),
            Calling.entries.map { en(it.label) },
        )
        assertEquals("Summon him at your own risk.", en(Calling.Menace.tooltip))
    }

    @Test
    fun unknownIdIsNull() {
        assertNull(DemonCatalog.byId("baal"))
    }

    @Test
    fun everyDemonHasDistinctArtSigilAndBed() {
        assertEquals(R.raw.sigil_asmodeus, asmodeus.sigil)
        assertEquals(R.raw.sigil_astaroth, astaroth.sigil)
        assertEquals(R.raw.bed_asmodeus, asmodeus.ambientBed)
        assertEquals(R.raw.bed_astaroth, astaroth.ambientBed)
        assertEquals(R.drawable.demon_asmodeus_cutout, asmodeus.cutoutArt)
        assertEquals(R.drawable.demon_astaroth_cutout, astaroth.cutoutArt)
        assertEquals(R.drawable.demon_astaroth_full, astaroth.sceneArt)
        assertEquals(R.drawable.avatar_astaroth, astaroth.avatarArt)
    }
}
