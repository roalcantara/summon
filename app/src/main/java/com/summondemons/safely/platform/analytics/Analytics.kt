package com.summondemons.safely.platform.analytics

import android.util.Log

/** Where events go (ADR-0035). Composables never call it; view models and the app do. */
interface Analytics {
    fun track(event: AnalyticsEvent)
}

/** Sends nothing: tests, and release builds without an app ID. */
object NoAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Drops events while the player's switch is off ([enabled] is read at each event). */
class GatedAnalytics(private val delegate: Analytics, private val enabled: () -> Boolean) : Analytics {
    override fun track(event: AnalyticsEvent) {
        if (enabled()) delegate.track(event)
    }
}

/** Debug builds: events go to Logcat (tag "Analytics"), never to the network. */
object LogAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) {
        Log.d("Analytics", "${event.name} ${event.params()}")
    }
}
