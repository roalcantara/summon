package com.summondemons.safely.platform.analytics

/**
 * The only things the app ever sends (spec 2026-10-23 analytics, ADR-0035): typed, bucketed,
 * never free text, audio, location or personal data.
 */
sealed interface AnalyticsEvent {
    val name: String
    fun params(): Map<String, String>

    data class AppOpen(val daysSinceInstall: Long) : AnalyticsEvent {
        override val name = "app_open"
        override fun params() = mapOf("days_since_install" to Buckets.daysSinceInstall(daysSinceInstall))
    }

    data class RitualStart(val demon: String, val whisper: Boolean, val inCourt: Boolean) : AnalyticsEvent {
        override val name = "ritual_start"
        override fun params() = mapOf("demon" to demon, "mode" to mode(whisper), "in_court" to inCourt.toString())
    }

    data class MicPermission(val granted: Boolean, val permanent: Boolean) : AnalyticsEvent {
        override val name = "mic_permission"
        override fun params() = mapOf(
            "result" to when {
                granted -> "granted"
                permanent -> "denied_permanently"
                else -> "denied"
            },
        )
    }

    data class DirectionLocked(val sinceStartMs: Long) : AnalyticsEvent {
        override val name = "direction_locked"
        override fun params() = mapOf("seconds" to Buckets.lockSeconds(sinceStartMs))
    }

    /** [line] is 0-based (phrase index); sent 1-based. [score] is null in lenient mode and for cracks. */
    data class LineResult(val line: Int, val sealed: Boolean, val score: Int?) : AnalyticsEvent {
        override val name = "line_result"
        override fun params() = buildMap {
            put("line", (line + 1).toString())
            put("result", if (sealed) "sealed" else "cracked")
            score?.let { put("score", Buckets.score(it)) }
        }
    }

    /** [phase]: intro, seeking, locking, chanting, wavering, error_mic, error_heading, or done. */
    data class RitualEnd(val completed: Boolean, val phase: String, val flawless: Boolean, val durationMs: Long) : AnalyticsEvent {
        override val name = "ritual_end"
        override fun params() = buildMap {
            put("outcome", if (completed) "completed" else "abandoned")
            put("phase", phase)
            if (completed) put("flawless", flawless.toString())
            put("duration", Buckets.duration(durationMs))
        }
    }

    data class DemonEscaped(val demon: String) : AnalyticsEvent {
        override val name = "demon_escaped"
        override fun params() = mapOf("demon" to demon)
    }

    /** [reason] is `DeviceCheck`'s raw reason; only a fixed code is sent. */
    data class DeviceCheck(val whisper: Boolean, val reason: String) : AnalyticsEvent {
        override val name = "device_check"
        override fun params() = mapOf(
            "mode" to mode(whisper),
            "reason" to when {
                reason.endsWith(" ms") && !reason.startsWith("slow") -> "ok"
                reason.startsWith("slow") -> "too_slow"
                reason.startsWith("load") -> "load_error"
                else -> "other"
            },
        )
    }

    data class TeaserShown(val demon: String, val daysSinceSummon: Long) : AnalyticsEvent {
        override val name = "teaser_shown"
        override fun params() = mapOf("demon" to demon, "days_since_summon" to Buckets.daysSinceInstall(daysSinceSummon))
    }

    data class TeaserTap(val demon: String, val onDemon: Boolean, val repeat: Boolean) : AnalyticsEvent {
        override val name = "teaser_tap"
        override fun params() = mapOf("demon" to demon, "target" to if (onDemon) "demon" else "balloon", "repeat" to repeat.toString())
    }
}

private fun mode(whisper: Boolean) = if (whisper) "whisper" else "lenient"
