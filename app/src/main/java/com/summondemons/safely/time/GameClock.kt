package com.summondemons.safely.time

import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.ceil

private const val MS_PER_HOUR = 3_600_000.0

/** Local time for the game (§3). In debug builds the debug day offset is already applied. */
interface GameClock {
    fun now(): ZonedDateTime

    /**
     * Emits [now] on collection, at every top of the hour (this covers midnight), on system
     * time / zone / date changes and whenever the debug settings change.
     */
    val ticks: Flow<ZonedDateTime>
}

/** Local epoch day: the unit of every progress rule. */
fun ZonedDateTime.epochDay(): Long = toLocalDate().toEpochDay()

/** Whole hours to the next local midnight, rounded up, at least 1 ("Next summon in {h} h"). */
fun hoursToMidnight(now: ZonedDateTime): Int {
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return maxOf(1, ceil(Duration.between(now, midnight).toMillis() / MS_PER_HOUR).toInt())
}
