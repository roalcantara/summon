package com.summondemons.safely.sigil

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SigilPathsTest {

    private val strokes = listOf(SigilStroke(1, "a"), SigilStroke(1, "b"), SigilStroke(2, "c"))
    private val lengths = listOf(10f, 30f, 5f)

    private fun assertFractions(expected: List<Float>, actual: List<Float>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) -> assertEquals(e, a, 0.0001f) }
    }

    @Test
    fun parseKeepsDrawOrderWithinGroupsAndSortsGroups() {
        val parsed = SigilPaths.parse(listOf("2 M0,0 L1,1", "1 M5,5 L6,6", "", "1 M7,7 L8,8"))
        assertEquals(listOf(1, 1, 2), parsed.map { it.group })
        assertEquals(listOf("M5,5 L6,6", "M7,7 L8,8", "M0,0 L1,1"), parsed.map { it.pathData })
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsUnknownGroups() {
        SigilPaths.parse(listOf("5 M0,0 L1,1"))
    }

    @Test
    fun nothingIsInkedAtZeroAndEverythingAtOne() {
        assertFractions(listOf(0f, 0f, 0f), SigilPaths.inkFractions(strokes, lengths, 0f))
        assertFractions(listOf(1f, 1f, 1f), SigilPaths.inkFractions(strokes, lengths, 1f))
    }

    @Test
    fun theFirstQuarterInksGroupOne() {
        assertFractions(listOf(1f, 1f, 0f), SigilPaths.inkFractions(strokes, lengths, 0.25f))
    }

    @Test
    fun strokesInkInOrderByLengthWithinAGroup() {
        // group 1 half done: 20 of 40 length units → first stroke full, second a third.
        assertFractions(listOf(1f, 1f / 3f, 0f), SigilPaths.inkFractions(strokes, lengths, 0.125f))
    }

    @Test
    fun zeroLengthStrokesAppearWhenTheirGroupCompletes() {
        val dot = listOf(SigilStroke(1, "a"))
        assertFractions(listOf(0f), SigilPaths.inkFractions(dot, listOf(0f), 0.2f))
        assertFractions(listOf(1f), SigilPaths.inkFractions(dot, listOf(0f), 0.25f))
    }

    @Test
    fun shippedSigilsUseAllFourGroups() {
        listOf("asmodeus", "astaroth").forEach { id ->
            val lines = File("src/main/res/raw/sigil_$id.txt").readLines()
            assertEquals(setOf(1, 2, 3, 4), SigilPaths.parse(lines).map { it.group }.toSet())
        }
    }
}
