package com.summondemons.safely.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.Demon
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.HOLD_TO_CHANT_ONSET_FRAMES
import com.summondemons.safely.ritual.RitualConfig.HOLD_TO_CHANT_RMS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RitualUiState(
    val demon: Demon,
    val ritual: RitualState,
    /** Latest heading reading (fresher than `ritual.azimuth`, which updates per tick). */
    val azimuth: Float? = null,
    /** Smoothed mic level in [0, 1]; drives glow only. */
    val micLevel: Float = 0f,
    /** null = not checked yet. */
    val micGranted: Boolean? = null,
    val simulateHeading: Boolean = false,
    val holdToChant: Boolean = false,
    /** Mic was granted but stopped or never started (busy, unsupported); the ritual can't hear. */
    val micUnavailable: Boolean = false,
    /** The device has no heading sensor; the ritual can't lock (debug can simulate). */
    val headingUnavailable: Boolean = false,
    /** The Ritual screen is started; no ticks run otherwise, so the focus clock pauses (§2). */
    val active: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModel(
    private val demon: Demon,
    private val headingSource: HeadingSource,
    private val pcmSource: PcmSource,
    /** Whether the demon's House is in court now (debug override applied); read only at the moment of lock. */
    private val inCourt: () -> Boolean = { false },
    /** Saves the summon. Called inside the tick, exactly once, on the step that starts the climax (ADR-0013). */
    private val commitSummon: (flawless: Boolean) -> Unit = {},
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val state = MutableStateFlow(
        RitualUiState(
            demon = demon,
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases),
            headingUnavailable = !headingSource.available,
        ),
    )
    private val _effects = MutableSharedFlow<RitualEffect>(extraBufferCapacity = EFFECT_BUFFER)

    /** One-shot events for haptics, sounds and flashes. Lossy by design: nothing durable depends on them. */
    val effects: SharedFlow<RitualEffect> = _effects.asSharedFlow()

    private var holdFrames = 0

    private val azimuths: Flow<Float> = state
        .map { Pair(it.simulateHeading, it.active) }
        .distinctUntilChanged()
        .flatMapLatest { (simulate, active) ->
            when {
                simulate -> flowOf(demon.ritualHeading)
                active -> headingSource.azimuths()
                else -> emptyFlow()
            }
        }

    private val voiceFrames: Flow<VoiceFrame> = state
        .map { it.micGranted == true && it.active }
        .distinctUntilChanged()
        .flatMapLatest { live -> if (live) micFrames() else silentFrames() }
    val uiState: StateFlow<RitualUiState> = channelFlow {
        // On (re)start the sensors restart too: forget the last reading so ticks never run on a stale heading.
        state.update { if (it.simulateHeading) it else it.copy(azimuth = null) }
        launch { azimuths.collect { az -> state.update { it.copy(azimuth = az) } } }
        launch { voiceFrames.collect(::tick) }
        state.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    fun onMicPermission(granted: Boolean) = state.update { it.copy(micGranted = granted) }

    fun setSimulateHeading(on: Boolean) = state.update {
        if (on) it.copy(simulateHeading = true) else it.copy(simulateHeading = false, azimuth = null)
    }

    fun setHoldToChant(on: Boolean) {
        if (!on) holdFrames = 0
        state.update { it.copy(holdToChant = on) }
    }

    /** Called from the Ritual screen's ON_START / ON_STOP. */
    fun setActive(active: Boolean) = state.update {
        if (!active && !it.simulateHeading) it.copy(active = false, azimuth = null)
        else it.copy(active = active)
    }

    /** One 20 ms tick. Only this collector changes `ritual`, so read-then-update is safe. */
    private fun tick(micFrame: VoiceFrame) {
        val current = state.value
        if (!current.active || awaitingHeading(current)) return
        val voice = if (current.holdToChant) syntheticVoice() else micFrame
        val inCourt = if (RitualEngine.canLock(current.ritual, current.azimuth)) inCourt() else false
        val step = RitualEngine.step(current.ritual, RitualInput(current.azimuth, voice, inCourt))
        // The summon is saved here, not through the lossy effects flow, before the climax plays.
        step.effects.filterIsInstance<RitualEffect.ClimaxStart>().firstOrNull()?.let { commitSummon(it.flawless) }
        state.update { it.copy(ritual = step.state, micLevel = smoothMicLevel(it.micLevel, voice.rms)) }
        step.effects.forEach { _effects.tryEmit(it) }
    }

    /**
     * After a (re)start, ticks wait for the first heading reading, so a return from the
     * background never reads as off heading (no drift, no 3× drain, no crack). Devices
     * without a heading sensor tick with a null heading.
     */
    private fun awaitingHeading(s: RitualUiState): Boolean =
        s.azimuth == null && !s.simulateHeading && !s.headingUnavailable

    /** Debug "Hold to Chant": continuous voicing with an onset every ~240 ms. */
    private fun syntheticVoice(): VoiceFrame =
        VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0)

    /** Mic frames → analysed voice; if the mic dies (error or end), flag it and fall back to silence. */
    private fun micFrames(): Flow<VoiceFrame> = flow {
        var analyzer: VoiceAnalyzer? = null
        pcmSource.frames().collect { frame ->
            val current = analyzer?.takeIf { it.sampleRate == frame.sampleRate }
                ?: VoiceAnalyzer(frame.sampleRate).also { analyzer = it }
            emit(current.analyze(frame.samples, frame.count))
        }
    }
        .flowOn(analysisDispatcher)
        .onStart { state.update { it.copy(micUnavailable = false) } }
        .catch { }
        .onCompletion { cause ->
            // cause != null only on cancellation (permission change, screen gone): not a mic failure.
            if (cause == null) {
                state.update { it.copy(micUnavailable = true) }
                emitAll(silentFrames())
            }
        }

    private fun silentFrames(): Flow<VoiceFrame> = flow {
        while (true) {
            delay(FRAME_MS)
            emit(VoiceFrame.SILENT)
        }
    }

    companion object {
        private const val EFFECT_BUFFER = 64

        fun factory(
            demon: Demon,
            headingSource: HeadingSource,
            pcmSource: PcmSource,
            inCourt: () -> Boolean = { false },
            commitSummon: (flawless: Boolean) -> Unit = {},
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon) } }
    }
}
