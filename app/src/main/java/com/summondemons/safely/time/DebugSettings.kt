package com.summondemons.safely.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CourtOverride { Auto, In, Out }

/** Debug-only knobs (§7): a whole-day clock offset and a Court Hours override for every House. */
data class DebugSettings(val dayOffset: Int = 0, val court: CourtOverride = CourtOverride.Auto)

interface DebugSettingsStore {
    val settings: StateFlow<DebugSettings>
    suspend fun update(transform: (DebugSettings) -> DebugSettings)
}

/** Release builds: always the defaults; changes are ignored. */
object NoDebugSettings : DebugSettingsStore {
    override val settings: StateFlow<DebugSettings> = MutableStateFlow(DebugSettings()).asStateFlow()
    override suspend fun update(transform: (DebugSettings) -> DebugSettings) = Unit
}
