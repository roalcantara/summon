package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/** The word groups judged together, pinned to the spec's table (§5). */
class ChantChunksTest {

    private fun texts(phrase: String) = ChantChunks.of(phrase).map { it.text to (it.syllables.last - it.syllables.first + 1) }

    @Test
    fun asmodeusSplitsHisLongLines() {
        val p = DemonCatalog.byId("asmodeus")!!.chantPhrases
        assertEquals(listOf("DAEMONIUM RESIDENTE" to 8), texts(p[0]))
        assertEquals(listOf("OSTENDE MIHI FACIEM" to 8, "TUAM POTENTIAM" to 6, "VENIT AD ME" to 4), texts(p[1]))
        assertEquals(listOf("DONA MIHI PER FOEDUS" to 7, "QUOD EST IRE AD PRAESENS" to 7, "SUM VOBIS" to 3), texts(p[2]))
        assertEquals(listOf("UT VOTA OMNIA" to 6, "OPUS EST" to 3), texts(p[3]))
    }

    @Test
    fun astarothKeepsEveryLineWhole() {
        DemonCatalog.byId("astaroth")!!.chantPhrases.forEach { assertEquals(1, ChantChunks.of(it).size) }
    }

    @Test
    fun chunksCoverThePhraseInOrder() {
        val phrase = DemonCatalog.byId("asmodeus")!!.chantPhrases[2]
        val chunks = ChantChunks.of(phrase)
        assertEquals(0, chunks.first().syllables.first)
        assertEquals(LatinSyllables.count(phrase) - 1, chunks.last().syllables.last)
        chunks.zipWithNext { a, b -> assertEquals(a.syllables.last + 1, b.syllables.first) }
        chunks.zipWithNext { a, b -> assertEquals(a.words.last + 1, b.words.first) }
        assertEquals(ChantTemplate.slots(phrase), chunks.flatMap { it.slots })
    }
}
