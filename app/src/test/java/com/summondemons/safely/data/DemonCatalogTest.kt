package com.summondemons.safely.data

import com.summondemons.safely.R
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
            assertTrue(demon.offices.isNotEmpty())
            listOf(demon.appearance, demon.warning, demon.secret, demon.weakness.name, demon.weakness.line, demon.restlessQuote)
                .forEach { assertTrue("${demon.id}: blank dossier text", it.isNotBlank()) }
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
        assertEquals("Lord of Arithmetic & Hidden Wealth", asmodeus.title)
        assertEquals(32, asmodeus.number)
        assertEquals("Great King", asmodeus.rank)
        assertEquals(72, asmodeus.legions)
        assertEquals(House.Sol, asmodeus.house)
        assertEquals("Amaymon, King of the East", asmodeus.liege)
        assertEquals(Calling.Tutor, asmodeus.calling)
        assertEquals(
            "Teaches arithmetic, astronomy, geometry, and invisibility. Answers all questions—if you can stand the smugness.",
            asmodeus.description,
        )
        assertEquals("I've counted every grain of dust in here. Twice. In three bases.", asmodeus.restlessQuote)
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
        assertEquals("Duke of Past, Present & Future", astaroth.title)
        assertEquals(29, astaroth.number)
        assertEquals("Mighty Duke", astaroth.rank)
        assertEquals(40, astaroth.legions)
        assertEquals(House.Venus, astaroth.house)
        assertNull(astaroth.liege)
        assertEquals(Calling.Oracle, astaroth.calling)
        assertEquals("Knows every secret and every tense. Will explain his own fall at length if asked.", astaroth.description)
        assertEquals(
            "I know your past, your future, and that you didn't read the terms of service.",
            astaroth.quote,
        )
        assertEquals("I foresaw you'd forget me. I hate being right. No I don't.", astaroth.restlessQuote)
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
