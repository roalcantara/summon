package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.SUCCESS_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.TICK_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class RitualViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val demon = DemonCatalog.byId("asmodeus")!!

    private class FakeHeading : HeadingSource {
        val readings = MutableSharedFlow<Float>(replay = 1)
        override fun azimuths(): Flow<Float> = readings
    }

    private class FakeVolume(private val level: Double) : VolumeSource {
        var subscriptions = 0
        override fun rmsLevels(): Flow<Double> = flow {
            subscriptions++
            while (true) {
                delay(TICK_MS)
                emit(level)
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loudAndAlignedForEightSecondsCompletesAfterTheHold() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 5_000.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)

        advanceTimeBy(8_000 + TICK_MS); runCurrent()
        assertEquals(4, vm.uiState.value.progress.phraseIndex)
        assertEquals(1f, vm.uiState.value.progress.sigilOpacity)
        assertFalse(vm.uiState.value.completed)

        advanceTimeBy(SUCCESS_HOLD_MS); runCurrent()
        assertTrue(vm.uiState.value.completed)
    }

    @Test
    fun turningAwayResetsProgress() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 5_000.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)
        advanceTimeBy(2_000 + TICK_MS); runCurrent()
        assertEquals(1, vm.uiState.value.progress.phraseIndex)

        heading.readings.emit(90f)
        advanceTimeBy(2 * TICK_MS); runCurrent()
        assertFalse(vm.uiState.value.aligned)
        assertEquals(RitualProgress(), vm.uiState.value.progress)
    }

    @Test
    fun quietMicDoesNotAdvance() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = RitualViewModel(demon, heading, FakeVolume(level = 100.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(RitualProgress(), vm.uiState.value.progress)
    }

    @Test
    fun simulateHeadingAlignsWithoutASensor() = runTest(dispatcher) {
        val vm = RitualViewModel(demon, { emptyFlow() }, FakeVolume(level = 0.0))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertNull(vm.uiState.value.azimuth)

        vm.setSimulateHeading(true)
        runCurrent()
        assertEquals(demon.ritualHeading, vm.uiState.value.azimuth)
        assertTrue(vm.uiState.value.aligned)

        vm.setSimulateHeading(false)
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
        assertFalse(vm.uiState.value.aligned)
    }

    @Test
    fun holdToChantCountsAsLoudWhenMicDenied() = runTest(dispatcher) {
        val volume = FakeVolume(level = 5_000.0)
        val vm = RitualViewModel(demon, { emptyFlow() }, volume)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(2_000 + TICK_MS); runCurrent()
        assertEquals(1, vm.uiState.value.progress.phraseIndex)
        assertEquals(0, volume.subscriptions)
    }

    @Test
    fun restartingThePipelineForgetsTheOldHeading() = runTest(dispatcher) {
        var subscriptions = 0
        val vm = RitualViewModel(demon, { flow { if (subscriptions++ == 0) emit(0f) } }, FakeVolume(level = 0.0))
        val first = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.uiState.value.aligned)

        first.cancel()
        advanceTimeBy(6_000); runCurrent() // past WhileSubscribed(5_000): pipeline stops
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertNull(vm.uiState.value.azimuth)
        assertFalse(vm.uiState.value.aligned)
    }

    @Test
    fun micThatEndsImmediatelyIsReportedUnavailable() = runTest(dispatcher) {
        val vm = RitualViewModel(demon, { emptyFlow() }, { emptyFlow() })
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)

        // The ritual keeps ticking (debug hold-to-chant still works).
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(2_000 + TICK_MS); runCurrent()
        assertEquals(1, vm.uiState.value.progress.phraseIndex)
    }

    @Test
    fun micThatThrowsIsReportedUnavailable() = runTest(dispatcher) {
        val vm = RitualViewModel(demon, { emptyFlow() }, { flow { throw IllegalStateException("mic busy") } })
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        runCurrent()
        assertTrue(vm.uiState.value.micUnavailable)
    }

    @Test
    fun workingMicIsNotReportedUnavailable() = runTest(dispatcher) {
        val vm = RitualViewModel(demon, { emptyFlow() }, FakeVolume(level = 5_000.0))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        advanceTimeBy(1_000); runCurrent()
        assertFalse(vm.uiState.value.micUnavailable)
    }

    @Test
    fun headingChangesDoNotRestartTheMic() = runTest(dispatcher) {
        val heading = FakeHeading()
        val volume = FakeVolume(level = 5_000.0)
        val vm = RitualViewModel(demon, heading, volume)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onMicPermission(true)
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(90f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        assertEquals(1, volume.subscriptions)
    }
}
