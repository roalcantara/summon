package com.summondemons.safely.platform

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.DebugSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "DebugSettings"
private val DAY_OFFSET = intPreferencesKey("day_offset")
private val COURT = stringPreferencesKey("court")

/** A separate file, excluded from backup (ADR-0013). Process-wide: one delegate per file. */
private val Context.debugSettingsDataStore by preferencesDataStore(name = "debug_settings")

/** Debug builds only. [settings] starts at the defaults and follows the file once loaded. */
class DataStoreDebugSettingsStore(context: Context, scope: CoroutineScope) : DebugSettingsStore {

    private val dataStore = context.applicationContext.debugSettingsDataStore
    private val state = MutableStateFlow(DebugSettings())
    private val firstLoad = CompletableDeferred<Unit>()

    override val settings: StateFlow<DebugSettings> = state.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
                .collect { prefs ->
                    state.value = prefs.toSettings()
                    firstLoad.complete(Unit)
                }
        }
    }

    suspend fun awaitLoaded() = firstLoad.await()

    override suspend fun update(transform: (DebugSettings) -> DebugSettings) {
        try {
            dataStore.edit { prefs ->
                val next = transform(prefs.toSettings())
                prefs[DAY_OFFSET] = next.dayOffset
                prefs[COURT] = next.court.name
            }
        } catch (e: IOException) {
            Log.w(TAG, "debug settings write failed", e)
        }
    }

    private fun Preferences.toSettings() = DebugSettings(
        dayOffset = this[DAY_OFFSET] ?: 0,
        court = this[COURT]?.let { name -> CourtOverride.entries.firstOrNull { it.name == name } } ?: CourtOverride.Auto,
    )
}
