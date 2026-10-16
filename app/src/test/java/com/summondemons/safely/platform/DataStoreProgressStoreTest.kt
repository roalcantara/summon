package com.summondemons.safely.platform

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Collections

class DataStoreProgressStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logs: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
    }
    private val file by lazy { File(folder.root, "progress.preferences_pb") }

    private fun store() = DataStoreProgressStore(dataStore) { message, _ -> logs += message }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun anEmptyFileReadsFreshProgress() = runBlocking {
        assertEquals(Progress(), store().progress.first())
        assertTrue(logs.isEmpty())
    }

    @Test
    fun updatesApplyToTheLatestValueAndRoundTrip() = runBlocking {
        val store = store()
        store.update { ProgressRules.commitSummon(it, "asmodeus", 10, flawless = true) }
        store.update { ProgressRules.visit(it, "asmodeus", 11) }
        val record = store.progress.first().record("asmodeus")
        assertTrue(record.flawless)
        assertEquals(11L, record.lastVisitDay)
        // A second store over the same DataStore reads the same value.
        assertEquals(11L, store().progress.first().record("asmodeus").lastVisitDay)
    }

    @Test
    fun corruptDataReadsFreshAndIsLoggedOnce() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("progress_v1")] = "{nope" }
        val store = store()
        assertEquals(Progress(), store.progress.first())
        assertEquals(Progress(), store.progress.first())
        assertEquals(1, logs.size)
    }

    @Test
    fun aCorruptFileIsReplacedAndWritesKeepWorking() = runBlocking {
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42))
        val store = store()
        assertEquals(Progress(), store.progress.first())
        store.update { it.copy(homeHintSeen = true) }
        assertEquals(Progress(homeHintSeen = true), store.progress.first())
    }

    @Test
    fun anUpdateOverCorruptDataStartsFromFreshProgress() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("progress_v1")] = "{nope" }
        val store = store()
        store.update { it.copy(homeHintSeen = true) }
        assertEquals(Progress(homeHintSeen = true), store.progress.first())
    }
}
