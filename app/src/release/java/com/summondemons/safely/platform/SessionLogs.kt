package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.SessionLog

/** Release builds never log sessions (ADR-0025). */
object SessionLogs {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): SessionLog = SessionLog.None
}
