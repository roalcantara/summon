package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test

class MicLevelTest {
    @Test
    fun risesFastAndFallsSlowly() {
        assertEquals(0.5f, smoothMicLevel(0f, RitualConfig.MIC_LEVEL_CEILING), 0.001f)
        assertEquals(0.92f, smoothMicLevel(1f, 0.0), 0.001f)
    }

    @Test
    fun clampsToOne() {
        assertEquals(1f, smoothMicLevel(1f, RitualConfig.MIC_LEVEL_CEILING * 4), 0.001f)
    }
}
