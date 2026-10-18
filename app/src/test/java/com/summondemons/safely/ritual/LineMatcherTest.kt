package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Expectations come from the real transcripts of ADR-0026 (Whisper base, Latin). */
class LineMatcherTest {

    private fun heard(vararg text: String) = text.map { TranscriptSegment(it) }

    @Test
    fun lettersFoldCaseAccentsAndLatinSpelling() {
        assertEquals("HOLAMAIUUENIT", LineMatcher.letters("Holāmā, jvvénit!"))
    }

    @Test
    fun honestRealLinesAreFound() {
        listOf(
            "DAEMONIUM RESIDENTE," to "Daemonium residente.",
            "TUI GRATIA LOVIS" to "Tui gracia, lovi, lovi.",
            "GRATIA SIT CURA." to "Gratia, si pi, cura!",
            "SINT BENE OBEDIENTES." to "singt bine, obe, dien, test",
            "HIC EST ME DIGNUS LEGI" to "Ik ezt me di noz legi.",
        ).forEach { (line, text) ->
            val m = LineMatcher.match(line, heard(text))
            assertTrue("$text → ${m.distance}", m.found)
        }
        assertEquals(0.0, LineMatcher.match("DAEMONIUM RESIDENTE,", heard("Daemonium residente.")).distance, 1e-9)
    }

    @Test
    fun repeatsAndRestartsBeforeTheLineCostNothing() {
        val m = LineMatcher.match(
            "OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
            heard("Ostende, mihi, faciem, tuam.", "potenti-am", "Ad, vendit, adme."),
        )
        assertTrue(m.found)
        assertEquals((0..7).toSet(), m.covered)
    }

    @Test
    fun nonsenseAndTheOtherDemonsLinesAreNotFound() {
        assertFalse(LineMatcher.match("DAEMONIUM RESIDENTE,", heard("Holamio Miku Holamio Miku Holamio Miku Holamio Miku")).found)
        assertFalse(LineMatcher.match("DAEMONIUM RESIDENTE,", heard("O lamio migu, O lamio migu, O lamio migu")).found)
        assertFalse(LineMatcher.match("TUI GRATIA LOVIS", heard("Daemonium residente.")).found)
    }

    @Test
    fun hallucinatedLoopsCollapseAndFail() {
        val m = LineMatcher.match("DAEMONIUM RESIDENTE,", heard("I'm sorry, I'm sorry, I'm sorry, I'm sorry"))
        assertEquals("IMSORRY", m.heardLetters)
        assertFalse(m.found)
    }

    @Test
    fun likelySilenceIsDropped() {
        val m = LineMatcher.match("DAEMONIUM RESIDENTE,", listOf(TranscriptSegment("Daemonium residente.", noSpeech = 0.9f)))
        assertFalse(m.found)
        assertEquals("", m.heardLetters)
    }

    @Test
    fun loopCollapseKeepsShortRepeats() {
        assertEquals(listOf("HOLAMIO", "MIKU"), LineMatcher.collapseLoops(List(4) { listOf("HOLAMIO", "MIKU") }.flatten()))
        assertEquals(listOf("A", "B"), LineMatcher.collapseLoops(listOf("A", "A", "A", "B")))
        assertEquals(listOf("X", "Y", "X", "Y", "Z"), LineMatcher.collapseLoops(listOf("X", "Y", "X", "Y", "Z")))
    }

    @Test
    fun halfOfALongLinePassesAtTheHonestFirstThreshold() {
        // Documented leniency (plan note 4): real Latin from the line, but only half of it.
        val m = LineMatcher.match("OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.", heard("Ostende, mihi, faciem, tuam."))
        assertTrue(m.found)
        assertEquals(0.462, m.distance, 0.001)
    }
}
