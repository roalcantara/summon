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
    /** The latest Whisper check, for the debug readout (ADR-0027). */
    val lastCheck: CheckReport? = null,
    /** A Whisper check is running (the current seal pulses faster). */
    val checking: Boolean = false,
    /** Why the ritual runs in lenient mode, for the debug readout; empty in Whisper mode. */
    val modeReason: String = "",
    /** Debug clip recorder label; null = not recording (ADR-0018). */
    val clipLabel: ClipLabel? = null,
    /** The intro card is on screen; the microphone is ignored meanwhile (ADR-0019). */
    val introVisible: Boolean = false,
)

/** One analysed mic frame plus its 16 kHz audio for the line listener (null when silent or synthetic). */
class MicFrame(val voice: VoiceFrame, val pcm16k: FloatArray?)

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModel(
    private val demon: Demon,
    private val headingSource: HeadingSource,
    private val pcmSource: PcmSource,
    /** Whether the demon's House is in court now (debug override applied); read only at the moment of lock. */
    private val inCourt: () -> Boolean = { false },
    /** Saves the summon. Called inside the tick, exactly once, on the step that starts the climax (ADR-0013). */
    private val commitSummon: (flawless: Boolean) -> Unit = {},
    /** Debug builds record tester clips here; release builds pass [ClipRecorder.None] (ADR-0018). */
    private val clipRecorder: ClipRecorder = ClipRecorder.None,
    /** Show the intro card when the ritual opens: the app does on every ritual (ADR-0021). */
    private val showIntro: Boolean = false,
    private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Debug builds log every ritual here; release builds pass [SessionLog.None] (ADR-0025). */
    private val sessionLog: SessionLog = SessionLog.None,
    /** Whisper on device; null in tests and in lenient mode (ADR-0027). */
    private val transcriber: Transcriber? = null,
    /** The device's check mode and its reason, read once when the ritual opens (ADR-0027). */
    private val checkMode: () -> Pair<CheckMode, String> = { CheckMode.Lenient to "" },
) : ViewModel() {

    private val mode = checkMode()
    private val state = MutableStateFlow(
        RitualUiState(
            demon = demon,
            ritual = RitualState.start(demon.ritualHeading, demon.chantPhrases, mode = mode.first),
            headingUnavailable = !headingSource.available,
            introVisible = showIntro,
            modeReason = mode.second,
        ),
    )

    private val listener: LineListener? =
        if (mode.first == CheckMode.Whisper && transcriber != null) LineListener(transcriber, viewModelScope) else null
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

    private val voiceFrames: Flow<MicFrame> = state
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

    fun setSimulateHeading(on: Boolean) {
        note("simulateHeading:$on")
        state.update { if (on) it.copy(simulateHeading = true) else it.copy(simulateHeading = false, azimuth = null) }
    }

    fun setHoldToChant(on: Boolean) {
        if (!on) holdFrames = 0
        state.update { it.copy(holdToChant = on) }
    }

    /** **Summon**, ✕ or back on the intro card: the chant starts listening (ADR-0019, ADR-0021). */
    fun dismissIntro() {
        note("intro:closed")
        state.update { it.copy(introVisible = false) }
    }

    /** "?" on the chant screen: show the intro card again. */
    fun openIntro() = state.update { it.copy(introVisible = true) }

    /** Debug: Off → Chant → Nonsense → Off. Each label starts a new clip (ADR-0018). */
    fun cycleClipLabel() {
        val next = when (state.value.clipLabel) {
            null -> ClipLabel.Chant
            ClipLabel.Chant -> ClipLabel.Nonsense
            ClipLabel.Nonsense -> null
        }
        clipRecorder.stop()
        if (next != null) clipRecorder.start("${demon.id}-${next.tag}")
        note("clip:${next?.tag ?: "off"}")
        state.update { it.copy(clipLabel = next) }
    }

    override fun onCleared() {
        clipRecorder.stop()
        sessionLog.stop()
    }

    /** Ticks run so far; the session log's clock (ADR-0025). */
    private var ticks = 0L

    private fun note(text: String) = sessionLog.write(SessionTick(t = ticks * RitualConfig.FRAME_MS, note = text))

    init {
        sessionLog.start(SessionHeader(demon.id, demon.ritualHeading, demon.chantPhrases, mode = mode.first.name, startedMs = System.currentTimeMillis()))
        if (mode.second.isNotEmpty()) note("mode:${mode.first}:${mode.second}")
    }

    /** Called from the Ritual screen's ON_START / ON_STOP. */
    fun setActive(active: Boolean) = state.update {
        if (!active && !it.simulateHeading) it.copy(active = false, azimuth = null)
        else it.copy(active = active)
    }

    /** One 20 ms tick. Only this collector changes `ritual`, so read-then-update is safe. */
    private fun tick(mic: MicFrame) {
        val current = state.value
        if (!current.active || awaitingHeading(current)) return
        val voice = when {
            current.introVisible -> VoiceFrame.SILENT // nothing counts while the rules are up
            current.holdToChant -> syntheticVoice(current.ritual)
            else -> mic.voice
        }
        val ritual = current.ritual
        val listening = ritual.phase == RitualPhase.Chanting || ritual.phase == RitualPhase.Wavering
        var report: CheckReport? = null
        if (listener != null && listening && ritual.phrase < RitualConfig.PHRASE_COUNT) {
            listener.start(ritual.phrase, demon.chantPhrases[ritual.phrase])
            val pcm = mic.pcm16k
            if (pcm != null && !current.introVisible && !current.holdToChant) listener.onFrame(pcm, voice.voiced)
            report = listener.poll()
        }
        var heard = report?.heard
        // Debug "Hold to Chant" can't speak: once every syllable is lit, the line counts as heard.
        if (current.holdToChant && ritual.mode == CheckMode.Whisper && ritual.phase == RitualPhase.Chanting &&
            ritual.phrase < RitualConfig.PHRASE_COUNT && ritual.lit >= ritual.syllables[ritual.phrase]
        ) {
            val words = LatinSyllables.wordSizes(demon.chantPhrases[ritual.phrase]).indices.toSet()
            heard = LineHeard(ritual.phrase, found = true, distance = 0.0, covered = words)
        }

        val inCourt = if (RitualEngine.canLock(ritual, current.azimuth)) inCourt() else false
        val input = RitualInput(current.azimuth, voice, inCourt, heard)
        val step = RitualEngine.step(ritual, input)
        sessionLog.write(SessionRecords.tick(ticks++ * RitualConfig.FRAME_MS, input, step, hold = current.holdToChant, check = report))
        // The summon is saved here, not through the lossy effects flow, before the climax plays.
        step.effects.filterIsInstance<RitualEffect.ClimaxStart>().firstOrNull()?.let { commitSummon(it.flawless) }
        state.update {
            it.copy(
                ritual = step.state,
                micLevel = smoothMicLevel(it.micLevel, voice.rms),
                lastCheck = report ?: it.lastCheck,
                checking = listener?.checking ?: false,
            )
        }
        step.effects.forEach { _effects.tryEmit(it) }
    }

    /**
     * After a (re)start, ticks wait for the first heading reading, so a return from the
     * background never reads as off heading (no drift, no 3× drain, no crack). Devices
     * without a heading sensor tick with a null heading.
     */
    private fun awaitingHeading(s: RitualUiState): Boolean =
        s.azimuth == null && !s.simulateHeading && !s.headingUnavailable

    /** Debug "Hold to Chant": continuous voicing with an onset every ~240 ms (ADR-0027). */
    private fun syntheticVoice(ritual: RitualState): VoiceFrame {
        val onset = holdFrames++ % HOLD_TO_CHANT_ONSET_FRAMES == 0
        return VoiceFrame(HOLD_TO_CHANT_RMS, voiced = true, onset = onset)
    }

    /** Mic frames → analysed voice; if the mic dies (error or end), flag it and fall back to silence. */
    private fun micFrames(): Flow<MicFrame> = flow {
        var analyzer: VoiceAnalyzer? = null
        var resampler: Resampler16k? = null
        pcmSource.frames().collect { frame ->
            clipRecorder.write(frame)
            val current = analyzer?.takeIf { it.sampleRate == frame.sampleRate }
                ?: VoiceAnalyzer(frame.sampleRate).also { analyzer = it }
            val voice = current.analyze(frame.samples, frame.count)
            val pcm = if (listener != null) {
                val r = resampler?.takeIf { it.rate == frame.sampleRate } ?: Resampler16k(frame.sampleRate).also { resampler = it }
                r.convert(frame.samples, frame.count)
            } else {
                null
            }
            emit(MicFrame(voice, pcm))
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

    private fun silentFrames(): Flow<MicFrame> = flow {
        while (true) {
            delay(FRAME_MS)
            emit(MicFrame(VoiceFrame.SILENT, null))
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
            clipRecorder: ClipRecorder = ClipRecorder.None,
            showIntro: Boolean = false,
            sessionLog: SessionLog = SessionLog.None,
            transcriber: Transcriber? = null,
            checkMode: () -> Pair<CheckMode, String> = { CheckMode.Lenient to "" },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RitualViewModel(demon, headingSource, pcmSource, inCourt, commitSummon, clipRecorder, showIntro, sessionLog = sessionLog, transcriber = transcriber, checkMode = checkMode)
            }
        }
    }
}
