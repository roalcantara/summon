package com.summondemons.safely.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.summondemons.safely.BuildConfig
import java.util.Locale

/**
 * Debug builds only: an app-language override from the debug sheet, for checking translations
 * on any Android version (ADR-0031). Release builds always follow the device or the Android 13+
 * per-app language.
 */
object DebugLocale {
    private const val PREFS = "debug_locale"
    private const val KEY_TAG = "tag"

    /** Language tags offered in the debug sheet; null follows the device. en-XA is the pseudo-locale. */
    val options: List<String?> = listOf(null, "en", "es", "pt-BR", "it", "fr", "en-XA")

    fun get(context: Context): String? =
        if (!BuildConfig.DEBUG) null else context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)

    /** Saves synchronously: the caller recreates the activity right after. */
    fun set(context: Context, tag: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (tag == null) remove(KEY_TAG) else putString(KEY_TAG, tag)
        }.commit()
    }

    /** [base] with the override applied; for `Activity.attachBaseContext`. */
    fun wrap(base: Context): Context {
        val tag = get(base) ?: return base
        val config = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        return base.createConfigurationContext(config)
    }
}
