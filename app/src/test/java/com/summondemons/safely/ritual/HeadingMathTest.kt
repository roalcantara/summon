package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingMathTest {

    @Test
    fun normalizeWrapsIntoZeroTo360() {
        assertEquals(0f, normalizeDegrees(360f), 0.001f)
        assertEquals(350f, normalizeDegrees(-10f), 0.001f)
        assertEquals(10f, normalizeDegrees(730f), 0.001f)
    }

    @Test
    fun angularDistanceTakesTheShortWayRound() {
        assertEquals(20f, angularDistance(340f, 0f), 0.001f)
        assertEquals(20f, angularDistance(10f, 350f), 0.001f)
        assertEquals(180f, angularDistance(0f, 180f), 0.001f)
        assertEquals(0f, angularDistance(0f, 360f), 0.001f)
    }

    @Test
    fun northToleranceEdges() {
        assertTrue(isAligned(340f, 0f))
        assertTrue(isAligned(20f, 0f))
        assertTrue(isAligned(0f, 0f))
        assertFalse(isAligned(339.9f, 0f))
        assertFalse(isAligned(20.1f, 0f))
    }

    @Test
    fun nonNorthTarget() {
        assertTrue(isAligned(165f, 180f))
        assertTrue(isAligned(200f, 180f))
        assertFalse(isAligned(0f, 180f))
    }

    @Test
    fun headingLabelsRoundToEightPoints() {
        assertEquals("North", headingLabel(0f))
        assertEquals("North", headingLabel(350f))
        assertEquals("North-East", headingLabel(45f))
        assertEquals("East", headingLabel(90f))
        assertEquals("South", headingLabel(180f))
        assertEquals("North-West", headingLabel(315f))
    }

    @Test
    fun smootherPassesFirstValueThrough() {
        assertEquals(123f, AzimuthSmoother().next(123f), 0.01f)
    }

    @Test
    fun smootherCrossesNorthWithoutSwingingSouth() {
        val smoother = AzimuthSmoother(alpha = 0.15f)
        smoother.next(350f)
        val next = smoother.next(10f)
        assertTrue("expected ~353, got $next", next > 350f && next < 360f)
    }

    @Test
    fun signedDeltaIsTheShortestTurn() {
        assertEquals(20f, signedDelta(350f, 10f), 0.001f)
        assertEquals(-20f, signedDelta(10f, 350f), 0.001f)
        assertEquals(-90f, signedDelta(90f, 0f), 0.001f)
        assertEquals(180f, signedDelta(0f, 180f), 0.001f)
    }

    @Test
    fun turnDirectionPointsTowardTheTarget() {
        assertEquals(TurnDirection.Left, turnDirection(90f, 0f))
        assertEquals(TurnDirection.Right, turnDirection(270f, 0f))
        assertEquals(null, turnDirection(5f, 0f))
        assertEquals(null, turnDirection(null, 0f))
    }

    @Test
    fun headingLetterIsTheNearestCompassPointAbbreviation() {
        assertEquals("N", headingLetter(0f))
        assertEquals("E", headingLetter(90f))
        assertEquals("NE", headingLetter(30f))
        assertEquals("N", headingLetter(350f))
        assertEquals("SW", headingLetter(-135f))
    }
}
