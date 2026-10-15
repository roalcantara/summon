package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class LatinSyllablesTest {

    private fun pieces(word: String) = LatinSyllables.spans(word).map { word.substring(it.start, it.end) }

    @Test
    fun countsFollowTheSpecRules() {
        assertEquals(4, LatinSyllables.count("DAEMONIUM"))
        assertEquals(3, LatinSyllables.count("GRATIA"))
        assertEquals(5, LatinSyllables.count("OBEDIENTES"))
        assertEquals(4, LatinSyllables.count("POTENTIAM"))
        assertEquals(2, LatinSyllables.count("TUI"))
        assertEquals(1, LatinSyllables.count("QUOD"))
    }

    @Test
    fun diphthongsAreOneNucleusAndConsonantsLeadTheNextSyllable() {
        assertEquals(listOf("DAE", "MO", "NI", "UM"), pieces("DAEMONIUM"))
        assertEquals(listOf("GRA", "TI", "A"), pieces("GRATIA"))
        assertEquals(listOf("QUOD"), pieces("QUOD"))
    }

    @Test
    fun punctuationAndSpacesAreSkipped() {
        val text = "DAEMONIUM RESIDENTE,"
        val spans = LatinSyllables.spans(text)
        assertEquals(8, spans.size)
        assertEquals("RE", text.substring(spans[4].start, spans[4].end))
        assertEquals("NTE", text.substring(spans[7].start, spans[7].end))
    }

    @Test
    fun spansCoverEveryLetterOfEveryWord() {
        val text = "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME."
        val covered = LatinSyllables.spans(text).joinToString("") { text.substring(it.start, it.end) }
        assertEquals(text.filter { it.isLetter() }, covered)
    }

    @Test
    fun chantPhraseTotals() {
        assertEquals(
            listOf(8, 18, 17, 9),
            DemonCatalog.byId("asmodeus")!!.chantPhrases.map(LatinSyllables::count),
        )
        assertEquals(
            listOf(7, 6, 7, 8),
            DemonCatalog.byId("astaroth")!!.chantPhrases.map(LatinSyllables::count),
        )
    }
}
