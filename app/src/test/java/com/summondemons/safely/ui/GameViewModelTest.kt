package com.summondemons.safely.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.summondemons.safely.data.House
import com.summondemons.safely.platform.analytics.Analytics
import com.summondemons.safely.platform.analytics.AnalyticsEvent
import com.summondemons.safely.platform.analytics.NoAnalytics
import com.summondemons.safely.platform.analytics.RecordingAnalytics
import com.summondemons.safely.progress.InMemoryProgressStore
import com.summondemons.safely.progress.Progress
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.progress.Reveal
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.FakeDebugSettings
import com.summondemons.safely.time.FakeGameClock
import com.summondemons.safely.time.epochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("UTC"))
    private val day0 = start.epochDay()
    private val debug = FakeDebugSettings()
    private val clock = FakeGameClock(start, debug.settings)

    private fun boundOn(day: Long, id: String = "asmodeus", flawless: Boolean = false) =
        ProgressRules.commitSummon(Progress(), id, day, flawless)

    private fun vm(store: InMemoryProgressStore, saved: SavedStateHandle = SavedStateHandle(), analytics: Analytics = NoAnalytics) =
        GameViewModel(store, clock, debug, saved, listOf("asmodeus", "astaroth"), analytics = analytics)

    private fun TestScope.observe(vm: GameViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun evaluateWaitsForTheFirstLoadThenRuns() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 3), loaded = false)
        val vm = vm(store)
        observe(vm)
        vm.evaluate(); runCurrent()
        assertFalse(vm.uiState.value.loaded)
        assertEquals(0, store.updates)

        store.load(); runCurrent()
        assertTrue(vm.uiState.value.loaded)
        assertEquals(1, store.updates) // the evaluate after the first load
        val record = store.state.value.record("asmodeus")
        assertFalse(record.bound)
        assertTrue(record.escapeBannerPending)

        vm.evaluate(); runCurrent()
        assertEquals(2, store.updates)
    }

    @Test
    fun theFirstLoadSelectsTheFocusDemonUnlessASelectionWasRestored() = runTest(dispatcher) {
        val progress = boundOn(day0 - 1) // a summon is available today: the unbound demon is the focus
        val fresh = vm(InMemoryProgressStore(progress))
        observe(fresh); runCurrent()
        assertEquals("astaroth", fresh.uiState.value.selectedId)

        val restored = vm(InMemoryProgressStore(progress), SavedStateHandle(mapOf(GameViewModel.KEY_SELECTED to "asmodeus")))
        observe(restored); runCurrent()
        assertEquals("asmodeus", restored.uiState.value.selectedId)
    }

    @Test
    fun aVisitAfterADayChangeUsesTheNewDay() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(1); runCurrent()
        assertEquals(day0 + 1, vm.uiState.value.today)
        vm.visit("asmodeus"); runCurrent()
        assertEquals(day0 + 1, store.state.value.record("asmodeus").lastVisitDay)
    }

    @Test
    fun aVisitThreeDaysLaterEscapesTheDemonInsteadOfVisitingHim() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(3)
        vm.visit("asmodeus"); runCurrent()
        val record = store.state.value.record("asmodeus")
        assertFalse(record.bound)
        assertTrue(record.escapeBannerPending)
        assertNull(record.lastVisitDay)
    }

    @Test
    fun openingFromHomeSelectsTheFocusDemonAndHidesTheHint() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 2)) // restless today
        val vm = vm(store)
        observe(vm); runCurrent()
        vm.select("astaroth")
        vm.openGrimoireFromHome(); runCurrent()
        assertEquals("asmodeus", vm.uiState.value.selectedId)
        assertTrue(store.state.value.homeHintSeen)
    }

    @Test
    fun aFlawlessCelebrationOpensTheSecretOnce() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        vm.celebrate("astaroth", flawless = true); runCurrent()
        assertEquals("astaroth", vm.uiState.value.selectedId)
        assertEquals(Celebration("astaroth", flawless = true), vm.uiState.value.celebration)

        vm.dismissCelebration(); runCurrent()
        assertNull(vm.uiState.value.celebration)
        assertTrue(vm.uiState.value.openSecret)

        vm.secretShown(); runCurrent()
        assertFalse(vm.uiState.value.openSecret)
    }

    @Test
    fun aCrackedSummonDoesNotOpenTheSecret() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        vm.celebrate("asmodeus", flawless = false)
        vm.dismissCelebration(); runCurrent()
        assertFalse(vm.uiState.value.openSecret)
    }

    @Test
    fun theCommitUsesTheDayTheRitualStarted() = runTest(dispatcher) {
        val store = InMemoryProgressStore()
        val vm = vm(store)
        observe(vm); runCurrent()
        clock.advanceDays(1) // the climax lands after midnight
        vm.commitSummon("asmodeus", day = day0, flawless = false); runCurrent()
        assertEquals(day0, store.state.value.lastSummonDay)
        assertTrue(vm.uiState.value.canSummonToday)
    }

    @Test
    fun everyMutationGoesThroughTheStore() = runTest(dispatcher) {
        val store = InMemoryProgressStore()
        val vm = vm(store)
        observe(vm); runCurrent()
        assertEquals(1, store.updates) // first-load evaluate
        vm.commitSummon("astaroth", day0, flawless = true); runCurrent()
        vm.visit("astaroth"); runCurrent()
        vm.consumeReveal("astaroth", Reveal.Codex); runCurrent()
        vm.dismissEscapeBanners(); runCurrent()
        assertEquals(5, store.updates)
        assertEquals(store.state.value, vm.uiState.value.progress)
    }

    @Test
    fun courtFollowsTheClockAndTheDebugOverride() = runTest(dispatcher) {
        val vm = vm(InMemoryProgressStore())
        observe(vm); runCurrent()
        assertEquals(setOf(House.Sol, House.Venus), vm.uiState.value.courtHouses) // 10:00

        clock.setHour(13); runCurrent()
        assertEquals(emptySet<House>(), vm.uiState.value.courtHouses)
        assertFalse(vm.inCourtNow(House.Sol))

        vm.setCourt(CourtOverride.In); runCurrent()
        assertEquals(setOf(House.Sol, House.Venus), vm.uiState.value.courtHouses)
        assertTrue(vm.inCourtNow(House.Venus))
    }

    @Test
    fun advancingADayMovesTheOffsetAndEvaluatesTheNextDay() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 2))
        val vm = vm(store)
        observe(vm); runCurrent()
        vm.advanceDay(); runCurrent()
        assertEquals(1, debug.settings.value.dayOffset)
        assertFalse(store.state.value.record("asmodeus").bound)
    }

    @Test
    fun resetAllClearsProgressAndDebugSettings() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0))
        val vm = vm(store)
        observe(vm); runCurrent()
        debug.update { DebugSettings(dayOffset = 2, court = CourtOverride.Out) }
        vm.celebrate("asmodeus", flawless = true)
        vm.resetAll(); runCurrent()
        assertEquals(Progress(), store.state.value)
        assertEquals(DebugSettings(), debug.settings.value)
        assertNull(vm.uiState.value.celebration)
    }

    @Test
    fun fakeClockCrossingMidnightMakesThreeDaysNeglectedDemonEscapeWithoutEvaluateCall() = runTest(dispatcher) {
        val store = InMemoryProgressStore(boundOn(day0 - 2))
        val vm = vm(store)
        observe(vm); runCurrent()
        assertTrue(store.state.value.record("asmodeus").bound)

        // Advancing 1 day crosses midnight from day0 to day0 + 1.
        // The demon was last visited on day0 - 2, so neglected days = (day0 + 1) - (day0 - 2) = 3 >= 3.
        clock.advanceDays(1); runCurrent()

        val record = store.state.value.record("asmodeus")
        assertFalse(record.bound)
        assertTrue(record.escapeBannerPending)
    }

    @Test
    fun commitSummonFromRitualCreatedOnDayNPlusOneRecordsDayNPlusOne() = runTest(dispatcher) {
        val store = InMemoryProgressStore()
        val vm = vm(store)
        observe(vm); runCurrent()

        // Move to day N+1
        clock.advanceDays(1); runCurrent()
        val dayNPlus1 = vm.today()
        assertEquals(day0 + 1, dayNPlus1)

        // When a ritual is opened on day N+1, its factory captures startDay = game.today()
        val startDay = dayNPlus1

        // Even if time moves forward again (e.g. crossing midnight to day N+2 during the ritual)
        clock.advanceDays(1); runCurrent()
        assertEquals(day0 + 2, vm.today())

        // The ritual finishes and commits the summon using the startDay captured at creation
        vm.commitSummon("asmodeus", startDay, flawless = true); runCurrent()

        val progress = store.state.value
        val record = progress.record("asmodeus")
        assertTrue(record.bound)
        assertTrue(record.flawless)
        assertEquals(dayNPlus1, record.lastVisitDay)
        assertEquals(dayNPlus1, progress.lastSummonDay)
    }

    @Test
    fun anEscapeIsReportedOnce() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val store = InMemoryProgressStore(boundOn(day0 - 3))
        val vm = vm(store, analytics = analytics)
        observe(vm); runCurrent()
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.DemonEscaped("asmodeus")), analytics.events)
    }

    @Test
    fun theTeaserIsReportedOncePerDayAndEveryThirdTapOpensIt() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val store = InMemoryProgressStore(boundOn(day0 - 2))
        val vm = vm(store, analytics = analytics)
        observe(vm); runCurrent()
        vm.teaserShown("asmodeus"); vm.teaserShown("asmodeus")
        vm.teaserTapped("asmodeus", onDemon = false, step = 1)
        vm.teaserTapped("asmodeus", onDemon = true, step = 2)
        vm.teaserTapped("asmodeus", onDemon = true, step = 3)
        runCurrent()
        vm.teaserTapped("asmodeus", onDemon = false, step = 3) // a replay the same day
        runCurrent()
        assertEquals(
            listOf(
                AnalyticsEvent.TeaserShown("asmodeus", 2),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = false, step = 1),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = true, step = 2),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = true, step = 3),
                AnalyticsEvent.TeaserOpen("asmodeus", repeat = false),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = false, step = 3),
                AnalyticsEvent.TeaserOpen("asmodeus", repeat = true),
            ),
            analytics.events.filter { it is AnalyticsEvent.TeaserShown || it is AnalyticsEvent.TeaserTap || it is AnalyticsEvent.TeaserOpen },
        )
        assertEquals(day0, store.state.value.record("asmodeus").teaserOpenedDay)
    }
}
