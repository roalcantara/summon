package com.summondemons.safely.progress

/** Daily summon, neglect and Court Hours tuning (ADR-0012, ADR-0015). Days are local calendar days. */
object ProgressConfig {
    const val RESTLESS_AFTER_DAYS = 2L
    const val ESCAPE_AFTER_DAYS = 3L

    /** Fixed local sunrise / sunset for Court Hours; weather and location are not read. */
    const val SUNRISE_HOUR = 6
    const val SUNSET_HOUR = 18
}
