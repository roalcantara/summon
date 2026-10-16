package com.summondemons.safely.time

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

/** A clock tests move by hand; every change is a tick. */
class FakeGameClock(start: ZonedDateTime) : GameClock {
    val time = MutableStateFlow(start)
    override fun now(): ZonedDateTime = time.value
    override val ticks: Flow<ZonedDateTime> = time

    fun advanceDays(days: Long) = time.update { it.plusDays(days) }
    fun setHour(hour: Int) = time.update { it.withHour(hour) }
}

class FakeDebugSettings(initial: DebugSettings = DebugSettings()) : DebugSettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<DebugSettings> = state.asStateFlow()
    override suspend fun update(transform: (DebugSettings) -> DebugSettings) = state.update(transform)
}
