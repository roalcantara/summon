package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.SessionLog
import java.io.File

/** Debug builds: every ritual is logged to the app's private `files/sessions/` (ADR-0025). */
object SessionLogs {
    fun create(context: Context): SessionLog = JsonlSessionLog(File(context.filesDir, "sessions"))
}
