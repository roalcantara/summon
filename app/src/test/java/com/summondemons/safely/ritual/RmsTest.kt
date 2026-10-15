package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test

class RmsTest {

    @Test
    fun rmsOfConstantSignalIsItsMagnitude() {
        assertEquals(1_000.0, rms(shortArrayOf(1_000, -1_000, 1_000, -1_000), 4), 0.001)
    }

    @Test
    fun rmsOnlyUsesTheFirstCountSamples() {
        assertEquals(3.0, rms(shortArrayOf(3, 3, 30_000), 2), 0.001)
    }

    @Test
    fun rmsOfNothingIsZero() {
        assertEquals(0.0, rms(ShortArray(0), 0), 0.0)
    }
}
