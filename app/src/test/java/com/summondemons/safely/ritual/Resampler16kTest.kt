package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Test

class Resampler16kTest {

    @Test
    fun sixteenKilohertzPassesThroughScaled() {
        val out = Resampler16k(16_000).convert(shortArrayOf(0, 16_384, -32_768), 3)
        assertEquals(listOf(0f, 0.5f, -1f), out.toList())
    }
    @Test
    fun fortyEightKilohertzKeepsAThirdAndConstantsStayConstant() {
        val r = Resampler16k(48_000)
        val total = (0 until 10).sumOf { r.convert(ShortArray(960) { 8_192 }, 960).also { f -> f.forEach { assertEquals(0.25f, it, 1e-6f) } }.size }
        assertEquals(3_200.0, total.toDouble(), 2.0)
    }
}
