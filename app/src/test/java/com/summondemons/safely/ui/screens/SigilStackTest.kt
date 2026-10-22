package com.summondemons.safely.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SigilStackTest {

    @Test
    fun aShortColumnSizesTheSigilByTheHeightLeft() {
        // 300 wide, 400 tall: 400 - 40 (name) - 30 (pill) - 2 * 12 (gaps) = 306, capped by the width.
        assertEquals(300, stackDiameter(width = 300, height = 400, nameHeight = 40, pillHeight = 30, gap = 12))
        assertEquals(206, stackDiameter(width = 300, height = 300, nameHeight = 40, pillHeight = 30, gap = 12))
    }

    @Test
    fun aHiddenPillLeavesItsRowAndGapToTheSigil() {
        assertEquals(248, stackDiameter(width = 300, height = 300, nameHeight = 40, pillHeight = null, gap = 12))
    }

    @Test
    fun neverNegative() {
        assertEquals(0, stackDiameter(width = 300, height = 50, nameHeight = 40, pillHeight = 30, gap = 12))
    }
}
