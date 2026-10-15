package com.summondemons.safely.ritual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class VoiceAnalyzerTest {

    private fun frameSize(rate: Int) = (rate * RitualConfig.FRAME_MS / 1_000).toInt()

    /** One 20 ms frame per amplitude, phase-continuous sine at [freq]. */
    private fun sine(freq: Double, rate: Int, amplitudes: List<Double>): List<ShortArray> {
        val n = frameSize(rate)
        return amplitudes.mapIndexed { f, amp ->
            ShortArray(n) { i -> (amp * sin(2 * PI * freq * (f * n + i) / rate)).toInt().toShort() }
        }
    }

    private fun analyze(rate: Int, frames: List<ShortArray>): List<VoiceFrame> {
        val analyzer = VoiceAnalyzer(rate)
        return frames.map { analyzer.analyze(it, it.size) }
    }

    @Test
    fun steadyVoiceIsVoiced() {
        val result = analyze(16_000, sine(150.0, 16_000, List(10) { 8_000.0 }))
        assertTrue(result.all { it.voiced })
    }

    @Test
    fun lowVoiceAt80HzIsVoicedOnceTwoFramesAreBuffered() {
        val result = analyze(16_000, sine(80.0, 16_000, List(10) { 8_000.0 }))
        assertTrue(result.drop(1).all { it.voiced })
    }

    @Test
    fun worksAt44kHz() {
        val result = analyze(44_100, sine(150.0, 44_100, List(6) { 8_000.0 }))
        assertTrue(result.all { it.voiced })
    }

    @Test
    fun loudNoiseIsNotVoiced() {
        val random = Random(42)
        val frames = List(10) { ShortArray(320) { random.nextInt(-8_000, 8_000).toShort() } }
        val result = analyze(16_000, frames)
        assertTrue(result.all { it.rms > RitualConfig.VOICE_RMS_FLOOR })
        assertTrue(result.none { it.voiced })
    }

    @Test
    fun clickIsNotVoiced() {
        val click = ShortArray(320).also { for (i in 0 until 10) it[i] = 30_000 }
        val result = analyze(16_000, listOf(ShortArray(320), ShortArray(320), click))
        assertTrue(result.last().rms > RitualConfig.VOICE_RMS_FLOOR)
        assertFalse(result.last().voiced)
    }

    @Test
    fun quietVoiceIsNotVoiced() {
        assertTrue(analyze(16_000, sine(150.0, 16_000, List(5) { 1_000.0 })).none { it.voiced })
    }

    @Test
    fun silenceIsNothing() {
        val result = analyze(16_000, List(5) { ShortArray(320) })
        assertTrue(result.all { it == VoiceFrame(0.0, voiced = false, onset = false) })
    }

    @Test
    fun oneOnsetPerBurst() {
        val amplitudes = (List(10) { 8_000.0 } + List(5) { 0.0 }).let { it + it + it }
        val result = analyze(16_000, sine(150.0, 16_000, amplitudes))
        assertEquals(listOf(0, 15, 30), result.indices.filter { result[it].onset })
    }

    @Test
    fun sustainedToneHasASingleOnset() {
        val result = analyze(16_000, sine(150.0, 16_000, List(50) { 8_000.0 }))
        assertEquals(1, result.count { it.onset })
    }

    @Test
    fun voicedDipsBetweenSyllablesReArmTheDetector() {
        val amplitudes = (List(5) { 8_000.0 } + List(3) { 2_500.0 }).let { it + it + it }
        val result = analyze(16_000, sine(150.0, 16_000, amplitudes))
        assertTrue(result.all { it.voiced })
        assertEquals(listOf(0, 8, 16), result.indices.filter { result[it].onset })
    }
}
