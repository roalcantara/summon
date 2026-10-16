package com.summondemons.safely.time
import com.summondemons.safely.data.House
import com.summondemons.safely.progress.CourtHours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

/** A clock tests move by hand; every change is a tick. */
class FakeGameClock(
    start: ZonedDateTime,
    private val debug: StateFlow<DebugSettings>? = null,
) : GameClock {
    val time = MutableStateFlow(start)
    var courtOverride: CourtOverride = CourtOverride.Auto

    override fun now(): ZonedDateTime = time.value.plusDays(debug?.value?.dayOffset?.toLong() ?: 0L)
    override val ticks: Flow<ClockTick> =
        if (debug != null) {
            merge(time, debug.drop(1).map { time.value }).map { tick() }
        } else {
            time.map { tick() }
        }

    fun advanceDays(days: Long) = time.update { it.plusDays(days) }
    fun setHour(hour: Int) = time.update { it.withHour(hour) }

    override fun inCourt(house: House): Boolean =
        CourtHours.inCourt(house, now().hour, debug?.value?.court ?: courtOverride)
}

class FakeDebugSettings(initial: DebugSettings = DebugSettings()) : DebugSettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<DebugSettings> = state.asStateFlow()
    override suspend fun update(transform: (DebugSettings) -> DebugSettings) = state.update(transform)
}
