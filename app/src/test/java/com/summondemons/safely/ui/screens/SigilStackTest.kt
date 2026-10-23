package com.summondemons.safely.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SigilStackTest {

    // S24 landscape: column 228 × 334 dp, name 38, pill 31, gaps 16, air 24.
    @Test
    fun theSigilTakesWhatTheSpacingLeavesAndTheGroupIsCentred() {
        val s = stackLayout(width = 228, height = 334, nameHeight = 38, pillHeight = 31, gap = 16, air = 24)
        assertEquals(185, s.diameter)
        assertEquals(24, s.top)
    }

    @Test
    fun aWideColumnIsCappedByItsWidthAndTheRestBecomesAir() {
        val s = stackLayout(width = 150, height = 334, nameHeight = 38, pillHeight = 31, gap = 16, air = 24)
        assertEquals(150, s.diameter)
        assertEquals((334 - (38 + 16 + 150 + 16 + 31)) / 2, s.top)
    }

    @Test
    fun aHiddenPillGivesUpItsRowAndGap() {
        val s = stackLayout(width = 300, height = 334, nameHeight = 38, pillHeight = null, gap = 16, air = 24)
        assertEquals(334 - 48 - 16 - 38, s.diameter)
    }

    @Test
    fun neverNegative() {
        assertEquals(0, stackLayout(width = 228, height = 80, nameHeight = 38, pillHeight = 31, gap = 16, air = 24).diameter)
    }

    // The phrase box below the HUD row is 72..334 in a 334 tall column; the axis is 167.
    @Test
    fun aPhraseThatFitsIsCentredOnTheAxis() {
        assertEquals(84, phraseTop(axis = 167, top = 72, bottom = 334, height = 166))
    }

    @Test
    fun aTallPhraseSitsAsCloseToTheAxisAsTheSpaceAllows() {
        assertEquals(72, phraseTop(axis = 167, top = 72, bottom = 334, height = 250))
        assertEquals(72, phraseTop(axis = 167, top = 72, bottom = 334, height = 400))
    }
}
