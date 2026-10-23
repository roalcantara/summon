package com.summondemons.safely

import android.content.Context
import com.summondemons.safely.platform.DataStoreDebugSettingsStore
import com.summondemons.safely.platform.SystemGameClock
import com.summondemons.safely.platform.analytics.Analytics
import com.summondemons.safely.platform.analytics.AnalyticsEvent
import com.summondemons.safely.platform.analytics.AnalyticsSettings
import com.summondemons.safely.platform.analytics.ForegroundOpens
import com.summondemons.safely.platform.analytics.GatedAnalytics
import com.summondemons.safely.platform.analytics.LogAnalytics
import com.summondemons.safely.platform.analytics.NoAnalytics
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
    val analyticsSettings = AnalyticsSettings(appContext)
    /**
     * v1 ships without an analytics provider (ADR-0035, amended): release builds send nothing and
     * hide the switch; debug builds log the events to Logcat. v1.1 adds the release sink here.
     */
    private val sink: Analytics = if (BuildConfig.DEBUG) LogAnalytics else NoAnalytics
    /** Whether events go anywhere, so Settings shows the opt-out switch only when it controls something. */
    val analyticsWired: Boolean = sink !== NoAnalytics
    val analytics: Analytics = GatedAnalytics(sink) { analyticsSettings.enabled }

    /** Days since this install, for `app_open` (first install time from the package manager). */
    fun daysSinceInstall(): Long {
        val installed = appContext.packageManager.getPackageInfo(appContext.packageName, 0).firstInstallTime
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installed)
    }

    private val opens = ForegroundOpens()

    /** `app_open` at start and on returns to the foreground after 30 minutes away (activity onStart). */
    fun reportOpen() {
        if (opens.onStart(System.currentTimeMillis())) analytics.track(AnalyticsEvent.AppOpen(daysSinceInstall()))
    }

    /** The activity left the foreground (onStop): the time away counts from here. */
    fun reportStop() = opens.onStop(System.currentTimeMillis())

    /** Returns once progress (and, in debug builds, the debug settings) have loaded. */
    suspend fun awaitReady() {
        progressStore.progress.first()
        debugStore?.awaitLoaded()
    }
}
