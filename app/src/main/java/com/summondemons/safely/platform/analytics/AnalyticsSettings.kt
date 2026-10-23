package com.summondemons.safely.platform.analytics

import android.content.Context
import java.util.UUID

/** The player's switch (on by default) and the random app-instance ID (ADR-0035). */
class AnalyticsSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("analytics", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    /** Created on first use; never linked to a person; gone when the app's data is cleared. */
    val instanceId: String
        get() = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ID = "instance_id"
    }
}
