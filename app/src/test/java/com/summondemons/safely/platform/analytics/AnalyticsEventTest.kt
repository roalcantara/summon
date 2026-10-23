package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun namesAndParams() {
        assertEquals("app_open" to mapOf("days_since_install" to "3-6"), AnalyticsEvent.AppOpen(4).let { it.name to it.params() })
        assertEquals(
            "ritual_start" to mapOf("demon" to "asmodeus", "mode" to "whisper", "in_court" to "true"),
            AnalyticsEvent.RitualStart("asmodeus", whisper = true, inCourt = true).let { it.name to it.params() },
        )
        assertEquals(mapOf("result" to "denied_permanently"), AnalyticsEvent.MicPermission(granted = false, permanent = true).params())
        assertEquals(mapOf("seconds" to "5-10"), AnalyticsEvent.DirectionLocked(7_000).params())
        assertEquals(mapOf("line" to "2", "result" to "sealed", "score" to "70-84"), AnalyticsEvent.LineResult(1, sealed = true, score = 80).params())
        assertEquals(mapOf("line" to "4", "result" to "cracked"), AnalyticsEvent.LineResult(3, sealed = false, score = null).params())
        assertEquals(
            mapOf("outcome" to "completed", "phase" to "done", "flawless" to "true", "duration" to "1-2m"),
            AnalyticsEvent.RitualEnd(completed = true, phase = "done", flawless = true, durationMs = 90_000).params(),
        )
        assertEquals(
            mapOf("outcome" to "abandoned", "phase" to "seeking", "duration" to "0-30s"),
            AnalyticsEvent.RitualEnd(completed = false, phase = "seeking", flawless = false, durationMs = 5_000).params(),
        )
        assertEquals(mapOf("demon" to "astaroth"), AnalyticsEvent.DemonEscaped("astaroth").params())
        assertEquals(mapOf("mode" to "lenient", "reason" to "too_slow"), AnalyticsEvent.DeviceCheck(whisper = false, reason = "slow: 2400 ms").params())
    }

    @Test
    fun deviceCheckReasonsAreCodesNeverFreeText() {
        assertEquals("ok", AnalyticsEvent.DeviceCheck(true, "812 ms").params()["reason"])
        assertEquals("load_error", AnalyticsEvent.DeviceCheck(false, "load: UnsatisfiedLinkError").params()["reason"])
        assertEquals("other", AnalyticsEvent.DeviceCheck(false, "anything else").params()["reason"])
    }
}
