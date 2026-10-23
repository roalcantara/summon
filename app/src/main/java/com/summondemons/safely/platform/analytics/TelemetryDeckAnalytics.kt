package com.summondemons.safely.platform.analytics

import android.app.Application
import com.telemetrydeck.sdk.TelemetryDeck

/** Release builds: events go to TelemetryDeck with the hashed app-instance ID (ADR-0035). */
class TelemetryDeckAnalytics(application: Application, appId: String, instanceId: String, testMode: Boolean) : Analytics {
    init {
        val builder = TelemetryDeck.Builder()
            .appID(appId)
            .defaultUser(instanceId)
            .testMode(testMode)
            .showDebugLogs(false)
            .sendNewSessionBeganSignal(false)
        TelemetryDeck.start(application, builder)
    }

    override fun track(event: AnalyticsEvent) {
        TelemetryDeck.signal(event.name, params = event.params())
    }
}
