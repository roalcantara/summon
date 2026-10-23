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
    @Test
    fun keyframeInterpolatesEvenlySpacedValues() {
        val values = floatArrayOf(0f, -22f, 0f, -6f, 0f)
        assertEquals(0f, keyframe(0f, values), 0.0001f)
        assertEquals(-11f, keyframe(0.125f, values), 0.0001f)
        assertEquals(-22f, keyframe(0.25f, values), 0.0001f)
        assertEquals(-3f, keyframe(0.625f, values), 0.0001f)
        assertEquals(0f, keyframe(1f, values), 0.0001f)
        assertEquals(0f, keyframe(2f, values), 0.0001f) // clamped
    }
    @Test
    fun smoothKeyframeEasesInAndOutOfEachKeyframe() {
        val values = floatArrayOf(0f, -22f, 0f, -6f, 0f)
        // On the keyframes and halfway between them it matches the linear version.
        assertEquals(0f, smoothKeyframe(0f, values), 0.0001f)
        assertEquals(-11f, smoothKeyframe(0.125f, values), 0.0001f)
        assertEquals(-22f, smoothKeyframe(0.25f, values), 0.0001f)
        assertEquals(-3f, smoothKeyframe(0.625f, values), 0.0001f)
        assertEquals(0f, smoothKeyframe(1f, values), 0.0001f)
        // A quarter into the first segment: smoothstep(0.25) = 0.15625, so -22 * 0.15625 (linear: -5.5).
        assertEquals(-3.4375f, smoothKeyframe(0.0625f, values), 0.0001f)
        // Clamped like keyframe().
        assertEquals(0f, smoothKeyframe(-1f, values), 0.0001f)
        assertEquals(0f, smoothKeyframe(2f, values), 0.0001f)
    }
}
