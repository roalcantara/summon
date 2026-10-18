package com.summondemons.safely.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.data.House
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.ProgressStore
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.time.ClockTick
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.DebugSettingsStore
import com.summondemons.safely.time.GameClock
import com.summondemons.safely.time.epochDay
import com.summondemons.safely.time.hoursToMidnight
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** The new-summon party on the Grimoire stage (ADR-0016). Not restored after process death. */
data class Celebration(val demonId: String, val flawless: Boolean)

/** Everything Home and Grimoire render (§3). */
data class GameUiState(
    /** False until the first stored progress has loaded; nothing acts on an unloaded `Progress()`. */
    val loaded: Boolean = false,
    val progress: Progress = Progress(),
    val now: ZonedDateTime,
    val selectedId: String,
    val celebration: Celebration? = null,
    /** Set when a flawless celebration is dismissed: the Grimoire opens the Secret page once. */
    val openSecret: Boolean = false,
    /** Houses in court now, debug override applied. */
    val courtHouses: Set<House> = emptySet(),
) {
    val today: Long get() = now.epochDay()
    val canSummonToday: Boolean get() = ProgressRules.canSummon(progress, today)
    val hoursToNextSummon: Int get() = hoursToMidnight(now)
    fun record(id: String): DemonRecord = progress.record(id)
}

/** Activity-scoped game state (§3). Every progress change goes through [store]. */
class GameViewModel(
    private val store: ProgressStore,
    private val clock: GameClock,
    private val debug: DebugSettingsStore,
    private val savedState: SavedStateHandle,
    private val demonIds: List<String> = DemonCatalog.ids,
) : ViewModel() {

    private data class Session(val celebration: Celebration? = null, val openSecret: Boolean = false)

    /** Null until the first stored value arrives. */
    private val stored: StateFlow<Progress?> = store.progress.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Re-reads the clock on every activity start: hourly delays don't advance while the phone sleeps. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val time: StateFlow<ClockTick> =
        merge(clock.ticks, refresh.map { clock.tick() }).stateIn(viewModelScope, SharingStarted.Eagerly, clock.tick())
    private val session = MutableStateFlow(Session())
    private val selected: StateFlow<String> = savedState.getStateFlow(KEY_SELECTED, savedState[KEY_SELECTED] ?: "")

    val uiState: StateFlow<GameUiState> =
        combine(stored, time, selected, session) { progress, tick, selectedId, s ->
            GameUiState(
                loaded = progress != null,
                progress = progress ?: Progress(),
                now = tick.now,
                selectedId = selectedId.ifEmpty { demonIds.first() },
                celebration = s.celebration,
                openSecret = s.openSecret,
                courtHouses = tick.courtHouses,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GameUiState(now = clock.now(), selectedId = selected.value, courtHouses = clock.courtHouses()),
        )

    init {
        viewModelScope.launch {
            val first = stored.filterNotNull().first()
            val day = today()
            if (savedState.get<String>(KEY_SELECTED).isNullOrEmpty()) select(ProgressRules.focusDemon(ProgressRules.evaluate(first, day), demonIds, day))
            store.update { ProgressRules.evaluate(it, today()) }
            // Keeps time-derived state right while the app stays open across midnight (§1).
            time.map { it.now.epochDay() }.distinctUntilChanged().drop(1).collect { day -> update { ProgressRules.evaluate(it, day) } }
        }
    }

    fun today(): Long = clock.now().epochDay()

    /** For the ritual's court provider: the demon's House against the clock, debug override applied. */
    fun inCourtNow(house: House): Boolean = clock.inCourt(house)

    /** On every activity start. Skipped until loaded (the first load runs its own evaluate). */
    fun evaluate() {
        refresh.tryEmit(Unit)
        if (stored.value == null) return
        val day = today()
        update { ProgressRules.evaluate(it, day) }
    }

    fun openGrimoireFromHome() {
        val progress = stored.value ?: return
        select(ProgressRules.focusDemon(progress, demonIds, today()))
        update { it.copy(homeHintSeen = true) }
    }

    /** The chant screen's intro card was dismissed: show it automatically only once (ADR-0019). */
    fun markChantIntroSeen() = update { it.copy(chantIntroSeen = true) }

    fun select(id: String) {
        savedState[KEY_SELECTED] = id
    }

    /** Viewing a bound demon. Evaluates first, so a demon neglected past the limit escapes instead (§1). */
    fun visit(id: String) {
        val day = today()
        update { ProgressRules.visit(ProgressRules.evaluate(it, day), id, day) }
    }

    /** [day] is the day the ritual started (captured when the Ritual opened). */
    fun commitSummon(id: String, day: Long, flawless: Boolean) =
        update { ProgressRules.commitSummon(it, id, day, flawless) }

    fun celebrate(id: String, flawless: Boolean) {
        select(id)
        session.value = Session(celebration = Celebration(id, flawless))
    }

    fun dismissCelebration() = session.update { Session(openSecret = it.celebration?.flawless == true) }

    fun secretShown() = session.update { it.copy(openSecret = false) }

    fun consumeReveal(id: String, what: Reveal) = update { ProgressRules.consumeReveal(it, id, what) }

    fun dismissEscapeBanners() = update(ProgressRules::dismissEscapeBanners)

    // --- Debug sheet (§7) ---

    fun advanceDay() {
        val next = today() + 1
        viewModelScope.launch {
            debug.update { it.copy(dayOffset = it.dayOffset + 1) }
            store.update { ProgressRules.evaluate(it, next) }
        }
    }

    fun resetDaily() = update { it.copy(lastSummonDay = null) }

    fun setCourt(court: CourtOverride) {
        viewModelScope.launch { debug.update { it.copy(court = court) } }
    }

    fun bind(id: String, flawless: Boolean) = commitSummon(id, today(), flawless)

    fun unsummon(id: String) = update { ProgressRules.unsummon(it, id) }

    fun resetAll() {
        session.value = Session()
        viewModelScope.launch {
            store.update { Progress() }
            debug.update { DebugSettings() }
        }
    }

    private fun update(transform: (Progress) -> Progress) {
        viewModelScope.launch { store.update(transform) }
    }

    companion object {
        const val KEY_SELECTED = "selectedDemon"

        fun factory(store: ProgressStore, clock: GameClock, debug: DebugSettingsStore): ViewModelProvider.Factory =
            viewModelFactory { initializer { GameViewModel(store, clock, debug, createSavedStateHandle()) } }
    }
}
