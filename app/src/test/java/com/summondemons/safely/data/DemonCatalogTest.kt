package com.summondemons.safely.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemonCatalogTest {

    @Test
    fun containsAsmodeusAndAstarothWithUniqueIds() {
        assertEquals(listOf("asmodeus", "astaroth"), DemonCatalog.all.map { it.id })
    }

    @Test
    fun everyDemonHasFourPhrasesAndAValidHeading() {
        DemonCatalog.all.forEach { demon ->
            assertEquals(4, demon.chantPhrases.size)
            assertTrue(demon.ritualHeading >= 0f && demon.ritualHeading < 360f)
        }
    }

    @Test
    fun asmodeusMatchesProductSpec() {
        val asmodeus = DemonCatalog.byId("asmodeus")!!
        assertEquals("Lord of Arithmetic & Hidden Wealth", asmodeus.title)
        assertEquals(0f, asmodeus.ritualHeading)
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
        val astaroth = DemonCatalog.byId("astaroth")!!
        assertEquals("Duke of Past, Present & Future", astaroth.title)
        assertEquals(
            "I know your past, your future, and that you didn't read the terms of service.",
            astaroth.quote,
        )
        assertEquals(
            listOf("TUI GRATIA LOVIS", "GRATIA SIT CURA.", "HIC EST ME DIGNUS LEGI", "SINT BENE OBEDIENTES."),
            astaroth.chantPhrases,
        )
    }

    @Test
    fun unknownIdIsNull() {
        assertEquals(null, DemonCatalog.byId("baal"))
    }
}
