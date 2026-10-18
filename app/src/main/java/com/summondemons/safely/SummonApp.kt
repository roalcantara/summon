package com.summondemons.safely

import android.app.Application
import com.summondemons.safely.platform.DeviceCheck
import kotlin.concurrent.thread

class SummonApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        // Decides once per install whether rituals use Whisper or the lenient check (ADR-0027).
        thread(name = "whisper-device-check", isDaemon = true) { DeviceCheck.runOnce(this) }
    }
}
