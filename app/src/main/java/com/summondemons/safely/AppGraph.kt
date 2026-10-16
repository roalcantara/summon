package com.summondemons.safely

import android.content.Context
import com.summondemons.safely.platform.DataStoreDebugSettingsStore
import com.summondemons.safely.platform.SystemGameClock
import com.summondemons.safely.platform.progressStore
import com.summondemons.safely.progress.ProgressStore
import com.summondemons.safely.time.DebugSettingsStore
import com.summondemons.safely.time.GameClock
import com.summondemons.safely.time.NoDebugSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/** Process-wide singletons: one DataStore per file, one clock (ADR-0013). */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val debugStore = if (BuildConfig.DEBUG) DataStoreDebugSettingsStore(appContext, scope) else null

    val progressStore: ProgressStore = appContext.progressStore()
    val debugSettings: DebugSettingsStore = debugStore ?: NoDebugSettings
    val clock: GameClock = SystemGameClock(appContext, debugSettings.settings)

    /** Returns once progress (and, in debug builds, the debug settings) have loaded. */
    suspend fun awaitReady() {
        progressStore.progress.first()
        debugStore?.awaitLoaded()
    }
}
