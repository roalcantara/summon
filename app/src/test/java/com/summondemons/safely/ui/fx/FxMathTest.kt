package com.summondemons.safely.ui.fx

import org.junit.Assert.assertEquals
import org.junit.Test

class FxMathTest {

    @Test
    fun segmentMapsAWindowToZeroOne() {
        assertEquals(0f, segment(0.1f, 0.2f, 0.6f), 0.0001f)
        assertEquals(0.5f, segment(0.4f, 0.2f, 0.6f), 0.0001f)
        assertEquals(1f, segment(0.9f, 0.2f, 0.6f), 0.0001f)
    }

    @Test
    fun parallaxFollowsTiltAndClamps() {
        val half = parallaxOffset(pitchDelta = PARALLAX_FULL_TILT_RAD / 2, rollDelta = -PARALLAX_FULL_TILT_RAD / 2, maxPx = 20f)
        assertEquals(-10f, half.x, 0.0001f)
        assertEquals(10f, half.y, 0.0001f)
        val beyond = parallaxOffset(pitchDelta = 10f, rollDelta = 10f, maxPx = 20f)
        assertEquals(20f, beyond.x, 0.0001f)
        assertEquals(20f, beyond.y, 0.0001f)
    }
}
