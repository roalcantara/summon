package com.summondemons.safely.platform

import android.content.Context
import java.io.File

/** The bundled Whisper model, copied once from assets to private storage (whisper.cpp needs a path; ADR-0027). */
object WhisperModelFile {
    private const val ASSET = "whisper/ggml-base-q5_1.bin"
    const val BYTES = 59_707_625L

    fun ensure(context: Context): File {
        val file = File(context.filesDir, ASSET)
        if (file.length() == BYTES) return file
        file.parentFile?.mkdirs()
        val part = File(file.path + ".part")
        context.assets.open(ASSET).use { input -> part.outputStream().use { input.copyTo(it, 1 shl 20) } }
        check(part.length() == BYTES) { "model copy truncated: ${part.length()} bytes" }
        check(part.renameTo(file)) { "could not move the model into place" }
        return file
    }
}
