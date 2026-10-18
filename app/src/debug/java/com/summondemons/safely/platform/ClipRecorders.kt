package com.summondemons.safely.platform

import android.content.Context
import com.summondemons.safely.ritual.ClipRecorder
import java.io.File

/** Debug builds: tester clips go to the app's private `files/chant-clips/` (ADR-0018). */
object ClipRecorders {
    fun create(context: Context): ClipRecorder = WavClipRecorder(File(context.filesDir, "chant-clips"))
}
