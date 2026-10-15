package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.DRIFT_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

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

    private fun chant(s: RitualState, voice: VoiceFrame): Step {
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Wavering, phaseMs = 0L, voicedRunMs = 0L), listOf(RitualEffect.DriftWarning))
        }
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        val lights = voice.voiced && ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        if (!lights) return Step(s.copy(sinceSyllableMs = since, voicedRunMs = run))

        val effects = mutableListOf<RitualEffect>(RitualEffect.SyllableLit)
        val lit = s.lit + 1
        val reset = s.copy(sinceSyllableMs = 0L, voicedRunMs = 0L)
        if (lit < s.syllables[s.phrase]) return Step(reset.copy(lit = lit), effects)

        effects += RitualEffect.PhraseComplete(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) return Step(reset.copy(phrase = phrase, lit = 0), effects)

        effects += RitualEffect.ClimaxStart
        return Step(reset.copy(phase = RitualPhase.Climax, phrase = phrase, lit = 0, phaseMs = 0L), effects)
    }

    private fun waver(s: RitualState): Step {
        if (s.aligned) {
            return Step(s.copy(phase = RitualPhase.Chanting, phaseMs = 0L, voicedRunMs = 0L), listOf(RitualEffect.Resumed))
        }
        val elapsed = s.phaseMs + FRAME_MS
        if (elapsed < DRIFT_GRACE_MS) return Step(s.copy(phaseMs = elapsed))
        val lost = s.copy(phase = RitualPhase.Seeking, phaseMs = 0L, lit = 0, proximityBucket = null)
        return Step(lost, listOf(RitualEffect.PhraseLost))
    }

    private fun climax(s: RitualState): Step {
        val elapsed = s.phaseMs + FRAME_MS
        if (elapsed < CLIMAX_MS) return Step(s.copy(phaseMs = elapsed))
        return Step(s.copy(phase = RitualPhase.Done, phaseMs = CLIMAX_MS), listOf(RitualEffect.Summoned))
    }

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
