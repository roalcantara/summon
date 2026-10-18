package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RotationAzimuthTest {

    /**
     * Rotation matrix (row-major, device → East/North/Up) for a portrait phone whose
     * back faces [heading] and that is tilted [tilt] degrees up from lying flat
     * (0 = screen up on a table, 90 = upright, >90 = leaning back past vertical),
     * then rolled [roll] degrees about the screen normal (a hand that isn't level).
     */
    private fun pose(heading: Float, tilt: Float, roll: Float = 0f): FloatArray {
        val h = Math.toRadians(heading.toDouble())
        val t = Math.toRadians(tilt.toDouble())
        val r = Math.toRadians(roll.toDouble())
        val forward = doubleArrayOf(sin(h), cos(h), 0.0)
        val up = doubleArrayOf(0.0, 0.0, 1.0)
        val right = doubleArrayOf(cos(h), -sin(h), 0.0)
        val top = DoubleArray(3) { cos(t) * forward[it] + sin(t) * up[it] }
        val x = DoubleArray(3) { cos(r) * right[it] + sin(r) * top[it] }
        val y = DoubleArray(3) { -sin(r) * right[it] + cos(r) * top[it] }
        val z = DoubleArray(3) { cos(t) * up[it] - sin(t) * forward[it] }
        return FloatArray(9) { i ->
            val row = i / 3
            when (i % 3) {
                0 -> x[row]
                1 -> y[row]
                else -> z[row]
            }.toFloat()
        }
    }

    private fun assertHeading(expected: Float, actual: Float) =
        assertEquals("expected $expected, got $actual", 0f, angularDistance(expected, actual), 0.5f)

    @Test
    fun flatPhoneUsesItsTop() {
        assertHeading(0f, azimuthFromRotationMatrix(pose(heading = 0f, tilt = 0f)))
        assertHeading(90f, azimuthFromRotationMatrix(pose(heading = 90f, tilt = 0f)))
    }

    @Test
    fun uprightPhoneUsesItsBack() {
        assertHeading(0f, azimuthFromRotationMatrix(pose(heading = 0f, tilt = 90f)))
        assertHeading(90f, azimuthFromRotationMatrix(pose(heading = 90f, tilt = 90f)))
        assertHeading(180f, azimuthFromRotationMatrix(pose(heading = 180f, tilt = 90f)))
    }

    @Test
    fun nearlyUprightPhoneStaysStable() {
        listOf(80f, 85f, 89f, 89.9f).forEach { tilt ->
            assertHeading(0f, azimuthFromRotationMatrix(pose(heading = 0f, tilt = tilt)))
        }
    }

    @Test
    fun uprightPhoneWithSlightRollKeepsItsHeading() {
        // Y-only (getOrientation) reads ~90° off here: the top's horizontal part is all roll.
        assertHeading(0f, azimuthFromRotationMatrix(pose(heading = 0f, tilt = 88f, roll = 5f)))
        assertHeading(180f, azimuthFromRotationMatrix(pose(heading = 180f, tilt = 90f, roll = -5f)))
    }

    @Test
    fun tiltedPhoneAgreesWithBothAxes() {
        assertHeading(270f, azimuthFromRotationMatrix(pose(heading = 270f, tilt = 60f)))
        assertHeading(270f, azimuthFromRotationMatrix(pose(heading = 270f, tilt = 30f)))
    }

    @Test
    fun leaningBackPastVerticalDoesNotFlip() {
        // The top now points slightly backwards; a Y-axis-only heading would read 180°.
        assertHeading(0f, azimuthFromRotationMatrix(pose(heading = 0f, tilt = 100f)))
    }

    @Test
    fun landscapeFlatUsesTheScreensTop() {
        // Rolled ±90° flat: the screen's top is the device's ±X axis, not its Y axis.
        assertHeading(30f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = 90f), screenRotation = 3))
        assertHeading(30f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = -90f), screenRotation = 1))
        // Ignoring the rotation would read 90° off.
        assertHeading(300f, azimuthFromRotationMatrix(pose(heading = 30f, tilt = 0f, roll = 90f)))
    }

    @Test
    fun landscapeUprightUsesTheBack() {
        assertHeading(120f, azimuthFromRotationMatrix(pose(heading = 120f, tilt = 90f, roll = 90f), screenRotation = 3))
        assertHeading(120f, azimuthFromRotationMatrix(pose(heading = 120f, tilt = 85f, roll = -90f), screenRotation = 1))
    }

    @Test
    fun landscapeTiltedAgreesWithBothAxes() {
        assertHeading(200f, azimuthFromRotationMatrix(pose(heading = 200f, tilt = 45f, roll = 90f), screenRotation = 3))
    }
}
