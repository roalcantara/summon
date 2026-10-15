package com.summondemons.safely.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.Demon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RitualUiState(
    val demon: Demon,
    val azimuth: Float? = null,
    val aligned: Boolean = false,
    val progress: RitualProgress = RitualProgress(),
    /** null = not checked yet. */
    val micGranted: Boolean? = null,
    val simulateHeading: Boolean = false,
    val holdToChant: Boolean = false,
    /** Mic was granted but stopped or never started (busy, unsupported); the ritual can't hear. */
    val micUnavailable: Boolean = false,
    /** True [RitualConfig.SUCCESS_HOLD_MS] after the 4th phrase; navigate on it. */
    val completed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModel(
    private val demon: Demon,
    private val headingSource: HeadingSource,
    private val volumeSource: VolumeSource,
) : ViewModel() {

    private val state = MutableStateFlow(RitualUiState(demon))

    private val azimuths: Flow<Float> = state
        .map { it.simulateHeading }
        .distinctUntilChanged()
        .flatMapLatest { simulate -> if (simulate) flowOf(demon.ritualHeading) else headingSource.azimuths() }

    private val micLoudTicks: Flow<Boolean> = state
        .map { it.micGranted }
        .distinctUntilChanged()
        .flatMapLatest { granted -> if (granted == true) micTicks() else silentTicks() }

    val uiState: StateFlow<RitualUiState> = channelFlow {
        // On (re)start the sensors restart too: forget the last reading so ticks never run on a stale heading.
        state.update { if (it.simulateHeading) it else it.copy(azimuth = null, aligned = false) }
        launch {
            azimuths.collect { az ->
                state.update { it.copy(azimuth = az, aligned = isAligned(az, demon.ritualHeading)) }
            }
        }
        launch {
            micLoudTicks.collect { micLoud ->
                state.update { s ->
                    if (s.progress.completed) {
                        s
                    } else {
                        s.copy(
                            progress = LegacyRitualEngine.step(
                                s.progress, s.aligned, micLoud || s.holdToChant, RitualConfig.TICK_MS,
                            ),
                        )
                    }
                }
            }
        }
        launch {
            state.first { it.progress.completed }
            delay(RitualConfig.SUCCESS_HOLD_MS)
            state.update { it.copy(completed = true) }
        }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    fun onMicPermission(granted: Boolean) = state.update { it.copy(micGranted = granted) }

    fun setSimulateHeading(on: Boolean) = state.update {
        if (on) it.copy(simulateHeading = true) else it.copy(simulateHeading = false, azimuth = null, aligned = false)
    }

    fun setHoldToChant(on: Boolean) = state.update { it.copy(holdToChant = on) }

    /** Mic RMS → loud flag; if the mic dies (error or end), flag it and fall back to silent ticks. */
    private fun micTicks(): Flow<Boolean> = volumeSource.rmsLevels()
        .onStart { state.update { it.copy(micUnavailable = false) } }
        .map { it > RitualConfig.VOLUME_RMS_THRESHOLD }
        .catch { }
        .onCompletion { cause ->
            // cause != null only on cancellation (permission change, screen gone): not a mic failure.
            if (cause == null) {
                state.update { it.copy(micUnavailable = true) }
                emitAll(silentTicks())
            }
        }

    private fun silentTicks(): Flow<Boolean> = flow {
        while (true) {
            delay(RitualConfig.TICK_MS)
            emit(false)
        }
    }

    companion object {
        fun factory(demon: Demon, headingSource: HeadingSource, volumeSource: VolumeSource): ViewModelProvider.Factory =
            viewModelFactory { initializer { RitualViewModel(demon, headingSource, volumeSource) } }
    }
}
