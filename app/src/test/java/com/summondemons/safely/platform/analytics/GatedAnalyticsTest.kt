package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class GatedAnalyticsTest {

    @Test
    fun theSwitchDecidesAtEachEvent() {
        val sink = RecordingAnalytics()
        var on = true
        val gated = GatedAnalytics(sink) { on }
        gated.track(AnalyticsEvent.DemonEscaped("asmodeus"))
        on = false
        gated.track(AnalyticsEvent.DemonEscaped("astaroth"))
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.DemonEscaped("asmodeus")), sink.events)
    }
}
