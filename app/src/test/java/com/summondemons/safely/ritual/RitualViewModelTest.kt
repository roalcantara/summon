package com.summondemons.safely.ritual

import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Sealed
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
import com.summondemons.safely.platform.analytics.Analytics
import com.summondemons.safely.platform.analytics.AnalyticsEvent
import com.summondemons.safely.platform.analytics.NoAnalytics
import com.summondemons.safely.platform.analytics.RecordingAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val demon = DemonCatalog.byId("asmodeus")!!

    private class FakeHeading(override val available: Boolean = true) : HeadingSource {
        val readings = MutableSharedFlow<Float>()
        var activeCollectors = 0
        override fun azimuths(): Flow<Float> = flow {
            activeCollectors++
            try {
                readings.collect { emit(it) }
            } finally {
                activeCollectors--
            }
        }
    }

    /** 16 kHz frames every 20 ms; [amplitude] gives the 150 Hz sine amplitude of frame i (0 = silence). */
    private class FakePcm(private val amplitude: (Int) -> Double) : PcmSource {
        var subscriptions = 0
        var activeCollectors = 0
        override fun frames(): Flow<PcmFrame> = flow {
            subscriptions++
            activeCollectors++
            try {
                var f = 0
                while (true) {
                    delay(FRAME_MS)
                    val amp = amplitude(f)
                    val samples = ShortArray(320) { i -> (amp * sin(2 * PI * 150.0 * (f * 320 + i) / 16_000)).toInt().toShort() }
                    emit(PcmFrame(samples, samples.size, 16_000))
                    f++
                }
            } finally {
                activeCollectors--
            }
        }
    }

    private fun silentPcm() = FakePcm { 0.0 }

    /** 10 voiced frames then 5 silent: one syllable every 300 ms. */
    private fun chantingPcm() = FakePcm { if (it % 15 < 10) 8_000.0 else 0.0 }

    private fun TestScope.observe(vm: RitualViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    private fun newVm(
        demon: Demon,
        heading: HeadingSource,
        pcm: PcmSource,
        inCourt: () -> Boolean = { false },
        commitSummon: (Boolean) -> Unit = {},
        analytics: Analytics = NoAnalytics,
    ) = RitualViewModel(demon, heading, pcm, inCourt, commitSummon, analysisDispatcher = dispatcher, analytics = analytics)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun holdToChantWithSimulatedHeadingCompletesTheRitual() = runTest(dispatcher) {
        val pcm = silentPcm()
        val vm = newVm(demon, { emptyFlow() }, pcm)
        observe(vm)
        val effects = mutableListOf<RitualEffect>()
        backgroundScope.launch { vm.effects.collect { effects += it } }
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)

        advanceTimeBy(20_000); runCurrent()

        assertEquals(RitualPhase.Done, vm.uiState.value.ritual.phase)
        assertEquals(
            listOf(Locked, Sealed(0), Sealed(1), Sealed(2), Sealed(3), ClimaxStart(flawless = true), Summoned),
            effects.filter { it != SyllableLit && it != ProximityTick && it !is RitualEffect.LineChecked },
        )
        assertEquals(0, pcm.subscriptions)
    }

    @Test
    fun theIntroCardMutesTheMicUntilSummon() = runTest(dispatcher) {
        val vm = RitualViewModel(
            demon = demon, headingSource = FakeHeading(), pcmSource = chantingPcm(),
            showIntro = true, analysisDispatcher = dispatcher,
        )
        observe(vm)
        vm.onMicPermission(true)
        vm.setSimulateHeading(true)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase) // the lock still happens
        assertEquals(0, vm.uiState.value.ritual.lit)
        assertTrue(vm.uiState.value.introVisible)
        vm.dismissIntro()
        advanceTimeBy(2_000); runCurrent()
        assertTrue("lit=${vm.uiState.value.ritual.lit}", vm.uiState.value.ritual.lit >= 4)
        vm.openIntro()
        runCurrent()
        assertTrue(vm.uiState.value.introVisible)
        vm.dismissIntro()
        runCurrent()
        assertFalse(vm.uiState.value.introVisible)
    }

    private class FakeRecorder : ClipRecorder {
        val calls = mutableListOf<String>()
        var frames = 0
        override fun start(name: String) {
            calls += "start $name"
        }
        override fun write(frame: PcmFrame) {
            frames++
        }
        override fun stop() {
            calls += "stop"
        }
    }

    @Test
    fun clipLabelCyclesAndTheRecorderGetsMicFrames() = runTest(dispatcher) {
        val recorder = FakeRecorder()
        val vm = RitualViewModel(demon, { emptyFlow() }, chantingPcm(), clipRecorder = recorder, analysisDispatcher = dispatcher)
        observe(vm)
        vm.onMicPermission(true)
        vm.cycleClipLabel()
        runCurrent()
        assertEquals(ClipLabel.Chant, vm.uiState.value.clipLabel)
        advanceTimeBy(1_000); runCurrent()
        vm.cycleClipLabel()
        runCurrent()
        assertEquals(ClipLabel.Nonsense, vm.uiState.value.clipLabel)
        vm.cycleClipLabel()
        runCurrent()
        assertNull(vm.uiState.value.clipLabel)
        assertEquals(listOf("stop", "start asmodeus-chant", "stop", "start asmodeus-nonsense", "stop"), recorder.calls)
        assertTrue(recorder.frames > 0)
    }

    @Test
    fun commitSummonIsCalledOnceAtTheClimaxStartEvenWithoutAnEffectCollector() = runTest(dispatcher) {
        val commits = mutableListOf<Boolean>()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), commitSummon = { commits += it })
        observe(vm)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)

        while (vm.uiState.value.ritual.phase != RitualPhase.Climax) {
            assertTrue("no commit before the climax", commits.isEmpty())
            advanceTimeBy(FRAME_MS); runCurrent()
        }
        assertEquals(listOf(true), commits) // saved before the climax plays

        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Done, vm.uiState.value.ritual.phase)
        assertEquals(listOf(true), commits)
    }

    @Test
    fun inCourtAtTheLockReachesTheEngineAndIsCalledOnlyAtLock() = runTest(dispatcher) {
        var courtChecks = 0
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), inCourt = { courtChecks++; true })
        observe(vm)
        vm.onMicPermission(false)
        // While still seeking without heading: only the ritual_start analytics event checked inCourt
        advanceTimeBy(500); runCurrent()
        assertEquals(1, courtChecks)
        // Simulate heading: LOCK_HOLD_MS = 600 ms. Advance 500 ms (not locked yet)
        vm.setSimulateHeading(true)
        advanceTimeBy(500); runCurrent()
        assertEquals(1, courtChecks)
        // Advance another 200 ms: crosses 600 ms, locks on this tick
        advanceTimeBy(200); runCurrent()
        assertEquals(2, courtChecks)
        assertEquals(1f + COURT_BONUS, vm.uiState.value.ritual.courtMultiplier, 0.0001f)

        // While chanting: inCourt must not be called again
        advanceTimeBy(1_000); runCurrent()
        assertEquals(2, courtChecks)
    }

    @Test
    fun noTicksWhileInactive() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        vm.setActive(false)
        runCurrent()
        assertFalse(vm.uiState.value.active)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
        assertEquals(0f, vm.uiState.value.micLevel)

        vm.setActive(true)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
    }

    @Test
    fun ticksAreHeldUntilTheFirstHeadingArrives() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
        assertEquals(0f, vm.uiState.value.micLevel) // no tick ran

        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
        assertTrue(vm.uiState.value.micLevel > 0f)
    }

    @Test
    fun devicesWithoutAHeadingSensorStillTick() = runTest(dispatcher) {
        val vm = newVm(demon, FakeHeading(available = false), silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        advanceTimeBy(500); runCurrent()
        assertTrue(vm.uiState.value.micLevel > 0f)
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
    }

    @Test
    fun voicedMicLightsSyllables() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, chantingPcm())
        observe(vm)
        vm.onMicPermission(true)
        vm.setSimulateHeading(true)
        advanceTimeBy(2_000); runCurrent()
        val ritual = vm.uiState.value.ritual
        assertEquals(RitualPhase.Chanting, ritual.phase)
        assertTrue("lit=${ritual.lit}", ritual.lit >= 4)
        assertTrue(vm.uiState.value.micLevel > 0f)
    }

    @Test
    fun silentMicLocksButDoesNotChant() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        vm.onMicPermission(true)
        vm.setSimulateHeading(true)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Chanting, vm.uiState.value.ritual.phase)
        assertEquals(0, vm.uiState.value.ritual.lit)
    }

    @Test
    fun driftWaversWithoutLosingProgressAndNeverReturnsToSeeking() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        advanceTimeBy(100); runCurrent()
        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_500); runCurrent()
        val lit = vm.uiState.value.ritual.lit
        assertTrue("lit=$lit", lit > 0)
        assertEquals(0, vm.uiState.value.ritual.phrase) // still phrase I: no clock

        vm.setHoldToChant(false)
        heading.readings.emit(demon.ritualHeading + 90f)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualPhase.Wavering, vm.uiState.value.ritual.phase)
        assertEquals(lit, vm.uiState.value.ritual.lit)
    }

    @Test
    fun simulateHeadingAlignsWithoutASensor() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, silentPcm())
        observe(vm)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)

        vm.setSimulateHeading(true)
        advanceTimeBy(2 * FRAME_MS); runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)
        assertTrue(vm.uiState.value.ritual.aligned)

        vm.setSimulateHeading(false)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
    }

    @Test
    fun restartingThePipelineForgetsTheOldHeading() = runTest(dispatcher) {
        var subscriptions = 0
        val vm = newVm(demon, { flow { if (subscriptions++ == 0) emit(demon.ritualHeading) } }, silentPcm())
        val first = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)

        first.cancel()
        advanceTimeBy(6_000); runCurrent() // past WhileSubscribed(5_000): pipeline stops
        observe(vm)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
    }

    @Test
    fun micThatEndsImmediatelyIsReportedUnavailableAndTheRitualStillTicks() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, { emptyFlow() })
        observe(vm)
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)

        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(1_500); runCurrent()
        assertTrue(vm.uiState.value.ritual.lit > 0)
    }

    @Test
    fun micThatThrowsIsReportedUnavailable() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, { flow { throw IllegalStateException("mic busy") } })
        observe(vm)
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)
    }

    @Test
    fun workingMicIsNotReportedUnavailable() = runTest(dispatcher) {
        val vm = newVm(demon, { emptyFlow() }, chantingPcm())
        observe(vm)
        vm.onMicPermission(true)
        advanceTimeBy(1_000); runCurrent()
        assertFalse(vm.uiState.value.micUnavailable)
    }

    @Test
    fun headingChangesDoNotRestartTheMic() = runTest(dispatcher) {
        val heading = FakeHeading()
        val pcm = chantingPcm()
        val vm = newVm(demon, heading, pcm)
        observe(vm)
        vm.onMicPermission(true)
        heading.readings.emit(demon.ritualHeading); advanceTimeBy(300); runCurrent()
        heading.readings.emit(demon.ritualHeading + 90f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(demon.ritualHeading); advanceTimeBy(300); runCurrent()
        assertEquals(1, pcm.subscriptions)
    }

    @Test
    fun headingAvailabilityComesFromTheSource() = runTest(dispatcher) {
        assertTrue(newVm(demon, FakeHeading(available = false), silentPcm()).uiState.value.headingUnavailable)
        assertFalse(newVm(demon, FakeHeading(available = true), silentPcm()).uiState.value.headingUnavailable)
    }

    @Test
    fun micStopsWhenRitualBecomesInactive() = runTest(dispatcher) {
        val pcm = chantingPcm()
        val vm = newVm(demon, { emptyFlow() }, pcm)
        observe(vm)
        vm.onMicPermission(true)
        advanceTimeBy(100); runCurrent()
        assertEquals(1, pcm.activeCollectors)

        vm.setActive(false)
        advanceTimeBy(100); runCurrent()
        assertEquals(0, pcm.activeCollectors)

        vm.setActive(true)
        advanceTimeBy(100); runCurrent()
        assertEquals(1, pcm.activeCollectors)
    }

    @Test
    fun headingSensorStopsWhenRitualBecomesInactive() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        advanceTimeBy(100); runCurrent()
        assertEquals(1, heading.activeCollectors)

        vm.setActive(false)
        advanceTimeBy(100); runCurrent()
        assertEquals(0, heading.activeCollectors)

        vm.setActive(true)
        advanceTimeBy(100); runCurrent()
        assertEquals(1, heading.activeCollectors)
    }

    @Test
    fun restartAfterInactiveWaitsForAFreshHeading() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        advanceTimeBy(100); runCurrent()
        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(700); runCurrent()
        assertTrue("ritual should lock on heading", vm.uiState.value.ritual.phase != RitualPhase.Seeking)

        // Inactive: should clear heading so ticks don't run on stale azimuth
        vm.setActive(false)
        advanceTimeBy(100); runCurrent()
        assertNull(vm.uiState.value.azimuth)

        // Reactivate: still null until a new heading arrives
        vm.setActive(true)
        advanceTimeBy(100); runCurrent()
        assertNull(vm.uiState.value.azimuth)
    }

    private class FakeLog : SessionLog {
        var header: SessionHeader? = null
        val lines = mutableListOf<SessionTick>()
        override fun start(header: SessionHeader) { this.header = header }
        override fun write(tick: SessionTick) { lines += tick }
        override fun stop() = Unit
    }

    @Test
    fun eachSettledPhraseSizeIsLoggedOnce() {
        val log = FakeLog()
        val vm = RitualViewModel(demon, FakeHeading(), silentPcm(), analysisDispatcher = dispatcher, sessionLog = log)
        val record = UiRecord(0, 64f, 600, 250, 160)
        vm.onPhraseLaid(record)
        vm.onPhraseLaid(record)
        assertEquals(listOf(record), log.lines.mapNotNull { it.ui })
        vm.onPhraseLaid(record.copy(phraseSp = 66f))
        assertEquals(2, log.lines.mapNotNull { it.ui }.size)
    }

    @Test
    fun aCompletedRitualReportsStartLockLinesAndEnd() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        observe(vm)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(20_000); runCurrent()
        vm.clearForTest()

        val names = analytics.events.map { it.name }
        assertEquals(listOf("ritual_start", "direction_locked", "line_result", "line_result", "line_result", "line_result", "ritual_end"), names)
        val end = analytics.events.last() as AnalyticsEvent.RitualEnd
        assertEquals(true, end.completed)
        assertEquals(true, end.flawless)
        assertEquals("lenient", (analytics.events.first() as AnalyticsEvent.RitualStart).params()["mode"])
    }

    @Test
    fun leavingBeforeTheLockReportsAnAbandonInSeeking() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        observe(vm)
        vm.onMicPermission(true)
        advanceTimeBy(2_000); runCurrent()
        vm.clearForTest()
        val end = analytics.events.last() as AnalyticsEvent.RitualEnd
        assertEquals(false, end.completed)
        assertEquals("seeking", end.phase)
    }

    @Test
    fun permissionAnswersAreReportedButResumeChecksAreNot() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        vm.onMicPermission(true) // resume check: not an answer
        vm.onPermissionAnswer(granted = false, permanent = true)
        assertEquals(
            listOf<AnalyticsEvent>(AnalyticsEvent.MicPermission(granted = false, permanent = true)),
            analytics.events.filterIsInstance<AnalyticsEvent.MicPermission>(),
        )
    }
}
