package com.summondemons.safely.platform.analytics

/** Records events for assertions. */
class RecordingAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()
    override fun track(event: AnalyticsEvent) {
        events += event
    }
}
