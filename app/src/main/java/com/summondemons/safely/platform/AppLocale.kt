package com.summondemons.safely.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The app language picked in Settings, on any Android version (ADR-0031, amended for the RC).
 * Null follows the phone (and, on Android 13+, the per-app language in system Settings).
 */
object AppLocale {
    private const val PREFS = "app_locale"
    private const val KEY_TAG = "tag"

    /** Shipped languages; null follows the phone. */
    val options: List<String?> = listOf(null, "en", "es", "pt-BR", "it", "fr")

    /** The saved choice, if it is still offered (a dropped option, e.g. en-XA, falls back to the phone). */
    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)?.takeIf { it in options }

    /** Saves synchronously: the caller recreates the activity right after. */
    fun set(context: Context, tag: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (tag == null) remove(KEY_TAG) else putString(KEY_TAG, tag)
        }.commit()
    }

    /** [base] with the picked language applied; for `Activity.attachBaseContext`. */
    fun wrap(base: Context): Context {
        val tag = get(base) ?: return base
        val config = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        return base.createConfigurationContext(config)
    }

    /** A language's own name ("Español", "Português (Brasil)"), so it never needs translating. */
    fun nativeName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }
}
