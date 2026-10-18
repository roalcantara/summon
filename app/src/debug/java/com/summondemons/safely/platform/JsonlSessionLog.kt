package com.summondemons.safely.platform

import com.summondemons.safely.ritual.SessionHeader
import com.summondemons.safely.ritual.SessionLog
import com.summondemons.safely.ritual.SessionRecords
import com.summondemons.safely.ritual.SessionTick
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug builds only (ADR-0025): each ritual goes to `{demon}-{yyyyMMdd-HHmmss}.jsonl` in [dir]
 * (the app's private storage, excluded from backups like the clips). Pull with
 * `tools/pull_sessions.sh`. Thread-safe.
 */
class JsonlSessionLog(
    private val dir: File,
    private val now: () -> Date = { Date() },
) : SessionLog {
    private var out: BufferedWriter? = null

    @Synchronized
    override fun start(header: SessionHeader) {
        stop()
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now())
        out = File(dir, "${header.demon}-$stamp.jsonl").bufferedWriter().also {
            it.write(SessionRecords.encode(header))
            it.newLine()
        }
    }

    @Synchronized
    override fun write(tick: SessionTick) {
        val writer = out ?: return
        writer.write(SessionRecords.encode(tick))
        writer.newLine()
    }

    @Synchronized
    override fun stop() {
        out?.close()
        out = null
    }
}
