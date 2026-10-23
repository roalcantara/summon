package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class BucketsTest {

    @Test
    fun daysSinceInstall() {
        assertEquals("0", Buckets.daysSinceInstall(0))
        assertEquals("1", Buckets.daysSinceInstall(1))
        assertEquals("2", Buckets.daysSinceInstall(2))
        assertEquals("3-6", Buckets.daysSinceInstall(3))
        assertEquals("3-6", Buckets.daysSinceInstall(6))
        assertEquals("7-13", Buckets.daysSinceInstall(7))
        assertEquals("14-29", Buckets.daysSinceInstall(29))
        assertEquals("30+", Buckets.daysSinceInstall(30))
        assertEquals("0", Buckets.daysSinceInstall(-2)) // clock moved back
    }

    @Test
    fun lockSeconds() {
        assertEquals("0-5", Buckets.lockSeconds(0))
        assertEquals("0-5", Buckets.lockSeconds(4_999))
        assertEquals("5-10", Buckets.lockSeconds(5_000))
        assertEquals("10-30", Buckets.lockSeconds(29_999))
        assertEquals("30-60", Buckets.lockSeconds(30_000))
        assertEquals("60+", Buckets.lockSeconds(60_000))
    }

    @Test
    fun duration() {
        assertEquals("0-30s", Buckets.duration(29_999))
        assertEquals("30-60s", Buckets.duration(30_000))
        assertEquals("1-2m", Buckets.duration(60_000))
        assertEquals("2-5m", Buckets.duration(120_000))
        assertEquals("5m+", Buckets.duration(300_000))
    }

    @Test
    fun score() {
        assertEquals("0-49", Buckets.score(0))
        assertEquals("0-49", Buckets.score(49))
        assertEquals("50-69", Buckets.score(50))
        assertEquals("70-84", Buckets.score(84))
        assertEquals("85-100", Buckets.score(85))
        assertEquals("85-100", Buckets.score(100))
    }
}
