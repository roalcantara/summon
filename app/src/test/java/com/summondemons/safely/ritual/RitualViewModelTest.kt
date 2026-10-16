package com.summondemons.safely.ritual

import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualEffect.ClimaxStart
import com.summondemons.safely.ritual.RitualEffect.Locked
import com.summondemons.safely.ritual.RitualEffect.PhraseComplete
import com.summondemons.safely.ritual.RitualEffect.ProximityTick
import com.summondemons.safely.ritual.RitualEffect.Summoned
import com.summondemons.safely.ritual.RitualEffect.SyllableLit
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
        val readings = MutableSharedFlow<Float>(replay = 1)
        override fun azimuths(): Flow<Float> = readings
    }

    /** 16 kHz frames every 20 ms; [amplitude] gives the 150 Hz sine amplitude of frame i (0 = silence). */
    private class FakePcm(private val amplitude: (Int) -> Double) : PcmSource {
        var subscriptions = 0
        override fun frames(): Flow<PcmFrame> = flow {
            subscriptions++
            var f = 0
            while (true) {
                delay(FRAME_MS)
                val amp = amplitude(f)
                val samples = ShortArray(320) { i -> (amp * sin(2 * PI * 150.0 * (f * 320 + i) / 16_000)).toInt().toShort() }
                emit(PcmFrame(samples, samples.size, 16_000))
                f++
            }
        }
    }

    private fun silentPcm() = FakePcm { 0.0 }

    /** 10 voiced frames then 5 silent: one syllable every 300 ms. */
    private fun chantingPcm() = FakePcm { if (it % 15 < 10) 8_000.0 else 0.0 }

    private fun TestScope.observe(vm: RitualViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    private fun newVm(demon: Demon, heading: HeadingSource, pcm: PcmSource) =
        RitualViewModel(demon, heading, pcm, dispatcher)

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
            listOf(Locked, PhraseComplete(0), PhraseComplete(1), PhraseComplete(2), PhraseComplete(3), ClimaxStart, Summoned),
            effects.filter { it != SyllableLit && it != ProximityTick },
        )
        assertEquals(0, pcm.subscriptions)
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
    fun driftLosesTheCurrentPhraseOnlyAfterTheGrace() = runTest(dispatcher) {
        val heading = FakeHeading()
        val vm = newVm(demon, heading, silentPcm())
        observe(vm)
        vm.onMicPermission(false)
        vm.setHoldToChant(true)
        heading.readings.emit(demon.ritualHeading)
        advanceTimeBy(1_500); runCurrent()
        val lit = vm.uiState.value.ritual.lit
        assertTrue("lit=$lit", lit > 0)

        vm.setHoldToChant(false)
        heading.readings.emit(demon.ritualHeading + 90f)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(RitualPhase.Wavering, vm.uiState.value.ritual.phase)
        assertEquals(lit, vm.uiState.value.ritual.lit)

        advanceTimeBy(600); runCurrent()
        assertEquals(RitualPhase.Seeking, vm.uiState.value.ritual.phase)
        assertEquals(0, vm.uiState.value.ritual.lit)
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
        val vm = newVm(demon, { flow { if (subscriptions++ == 0) emit(0f) } }, silentPcm())
        val first = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(0f, vm.uiState.value.azimuth)

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
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(90f); advanceTimeBy(300); runCurrent()
        heading.readings.emit(0f); advanceTimeBy(300); runCurrent()
        assertEquals(1, pcm.subscriptions)
    }

    @Test
    fun headingAvailabilityComesFromTheSource() = runTest(dispatcher) {
        assertTrue(newVm(demon, FakeHeading(available = false), silentPcm()).uiState.value.headingUnavailable)
        assertFalse(newVm(demon, FakeHeading(available = true), silentPcm()).uiState.value.headingUnavailable)
    }
}
