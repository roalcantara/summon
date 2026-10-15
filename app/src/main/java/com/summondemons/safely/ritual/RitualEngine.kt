package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG

/** Pure Ritual 2.0 rules (ADR-0008): one call per 20 ms tick. */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s)
            RitualPhase.Chanting -> chant(s, input.voice)
            RitualPhase.Wavering -> waver(s)
            RitualPhase.Climax -> climax(s)
            RitualPhase.Done -> Step(s)
        }
    }

    private fun seek(s: RitualState): Step {
        val effects = mutableListOf<RitualEffect>()
        val bucket = s.azimuth?.let { proximityBucket(angularDistance(it, s.target)) }
        val previous = s.proximityBucket
        if (bucket != null && (previous == null || bucket < previous)) effects += RitualEffect.ProximityTick
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Seeking, phaseMs = 0L, proximityBucket = bucket), effects)
        }
        val held = (if (s.phase == RitualPhase.Locking) s.phaseMs else 0L) + FRAME_MS
        if (held < LOCK_HOLD_MS) {
            return Step(s.copy(phase = RitualPhase.Locking, phaseMs = held, proximityBucket = bucket), effects)
        }
        effects += RitualEffect.Locked
        val chanting = s.copy(
            phase = RitualPhase.Chanting,
            phaseMs = 0L,
            proximityBucket = bucket,
            sinceSyllableMs = ONSET_REFRACTORY_MS,
            voicedRunMs = 0L,
        )
        return Step(chanting, effects)
    }

    private fun chant(s: RitualState, voice: VoiceFrame): Step = Step(s)

    private fun waver(s: RitualState): Step = Step(s)

    private fun climax(s: RitualState): Step = Step(s)

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
