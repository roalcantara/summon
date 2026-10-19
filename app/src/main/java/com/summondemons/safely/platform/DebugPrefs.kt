package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ChantLayout

/** Debug-only switches that outlive a ritual (ADR-0029). Release builds never read them. */
object DebugPrefs {
    private const val PREFS = "debug"
    private const val KEY_LAYOUT = "chant_layout"

    fun chantLayout(context: Context): ChantLayout =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAYOUT, null)
            ?.let { name -> ChantLayout.entries.firstOrNull { it.name == name } }
            ?: ChantLayout.Split

    fun setChantLayout(context: Context, layout: ChantLayout) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAYOUT, layout.name).apply()
    }
}
