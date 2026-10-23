package com.summondemons.safely.platform.analytics

/** Time away after which a return to the foreground counts as a new `app_open` (spec 2026-10-23-analytics). */
private const val AWAY_MS = 30 * 60_000L

/**
 * Which activity starts are an `app_open`: the first in the process, then only returns after
 * [AWAY_MS] in the background. Measured from the last stop, so a recreation (language change,
 * rotation) or a short trip away is not an open, however long the session was.
 */
class ForegroundOpens {
    private var opened = false
    private var stoppedAtMs = 0L

    /** True when this start is an open. */
    fun onStart(nowMs: Long): Boolean {
        if (opened && nowMs - stoppedAtMs < AWAY_MS) return false
        opened = true
        return true
    }

    fun onStop(nowMs: Long) {
        stoppedAtMs = nowMs
    }
}
