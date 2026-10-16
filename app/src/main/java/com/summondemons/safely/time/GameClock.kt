package com.summondemons.safely.time

import com.summondemons.safely.data.House
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.ceil


/** A single clock tick combining the current local time and the set of Houses in court. */
data class ClockTick(val now: ZonedDateTime, val courtHouses: Set<House>)
private const val MS_PER_HOUR = 3_600_000.0

/** Local time for the game (§3). In debug builds the debug day offset is already applied. */
interface GameClock {
    fun now(): ZonedDateTime

    /**
     * Emits [now] on collection, at every top of the hour (this covers midnight), on system
     * time / zone / date changes and whenever the debug settings change.
     */
    val ticks: Flow<ClockTick>

    /** Whether [house] is in court now (debug override applied in debug builds). */
    fun inCourt(house: House): Boolean

    /** Houses in court now (debug override applied in debug builds). */
    fun courtHouses(): Set<House> = House.entries.filterTo(mutableSetOf()) { inCourt(it) }

    fun tick(): ClockTick = ClockTick(now(), courtHouses())
}

/** Local epoch day: the unit of every progress rule. */
fun ZonedDateTime.epochDay(): Long = toLocalDate().toEpochDay()

/** Whole hours to the next local midnight, rounded up, at least 1 ("Next summon in {h} h"). */
fun hoursToMidnight(now: ZonedDateTime): Int {
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return maxOf(1, ceil(Duration.between(now, midnight).toMillis() / MS_PER_HOUR).toInt())
}
