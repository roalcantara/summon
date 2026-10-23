package com.summondemons.safely.platform.analytics

/** Raw numbers become fixed labels before they leave the device (spec 2026-10-23 analytics). */
object Buckets {
    fun daysSinceInstall(days: Long): String = when {
        days <= 0 -> "0"
        days == 1L -> "1"
        days == 2L -> "2"
        days <= 6 -> "3-6"
        days <= 13 -> "7-13"
        days <= 29 -> "14-29"
        else -> "30+"
    }

    fun lockSeconds(ms: Long): String = when {
        ms < 5_000 -> "0-5"
        ms < 10_000 -> "5-10"
        ms < 30_000 -> "10-30"
        ms < 60_000 -> "30-60"
        else -> "60+"
    }

    fun duration(ms: Long): String = when {
        ms < 30_000 -> "0-30s"
        ms < 60_000 -> "30-60s"
        ms < 120_000 -> "1-2m"
        ms < 300_000 -> "2-5m"
        else -> "5m+"
    }

    /** A chant-check score, 0..100 (`LineFeedback.score`). */
    fun score(score: Int): String = when {
        score < 50 -> "0-49"
        score < 70 -> "50-69"
        score < 85 -> "70-84"
        else -> "85-100"
    }
}
