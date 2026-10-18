package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ClipRecorder

/** Release builds never record audio (ADR-0018). */
object ClipRecorders {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): ClipRecorder = ClipRecorder.None
}
