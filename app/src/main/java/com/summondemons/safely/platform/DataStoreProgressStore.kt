package com.summondemons.safely.platform

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressJson
import com.summondemons.safely.progress.ProgressStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ProgressStore"
private const val READ_RETRY_MS = 1_000L
private val PROGRESS_KEY = stringPreferencesKey("progress_v1")

/**
 * Process-wide singleton: a top-level delegate, always used through the application context.
 * A corrupt file is replaced by an empty one, so writes keep working (spec §10).
 */
private val Context.progressDataStore by preferencesDataStore(
    name = "progress",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

fun Context.progressStore(): ProgressStore = DataStoreProgressStore(applicationContext.progressDataStore)

/** `Progress` as kotlinx-serialization JSON under one string key (ADR-0013). */
class DataStoreProgressStore(
    private val dataStore: DataStore<Preferences>,
    private val log: (String, Throwable?) -> Unit = { message, error -> Log.w(TAG, message, error) },
) : ProgressStore {

    private val corruptLogged = AtomicBoolean(false)

    override val progress: Flow<Progress> = dataStore.data
        // An unreadable file reads as Progress() but keeps the flow alive (a plain `catch`
        // would complete it), so later successful writes still reach the UI.
        .retryWhen { e, _ ->
            if (e !is IOException) return@retryWhen false
            log("progress unreadable; starting fresh", e)
            emit(emptyPreferences())
            delay(READ_RETRY_MS)
            true
        }
        .map { decode(it[PROGRESS_KEY]) }

    override suspend fun update(transform: (Progress) -> Progress) {
        try {
            dataStore.edit { prefs -> prefs[PROGRESS_KEY] = ProgressJson.encode(transform(decode(prefs[PROGRESS_KEY]))) }
        } catch (e: IOException) {
            log("progress write failed; change lost", e)
        }
    }

    private fun decode(raw: String?): Progress {
        if (raw == null) return Progress()
        return ProgressJson.decodeOrNull(raw) ?: Progress().also {
            if (corruptLogged.compareAndSet(false, true)) log("progress corrupt; starting fresh", null)
        }
    }
}
