package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundOpensTest {

    private val minute = 60_000L

    @Test
    fun theFirstStartIsAnOpen() {
        assertTrue(ForegroundOpens().onStart(nowMs = 0L))
    }

    @Test
    fun aShortTripAwayOrARecreationIsNot() {
        val opens = ForegroundOpens()
        opens.onStart(0L)
        // 40 minutes of play, then a language change or rotation: stop and start at once.
        opens.onStop(40 * minute)
        assertFalse(opens.onStart(40 * minute + 50))
        // Away for 29 minutes.
        opens.onStop(50 * minute)
        assertFalse(opens.onStart(79 * minute))
    }

    @Test
    fun aReturnAfterThirtyMinutesAwayIs() {
        val opens = ForegroundOpens()
        opens.onStart(0L)
        opens.onStop(5 * minute)
        assertTrue(opens.onStart(35 * minute))
    }
}
