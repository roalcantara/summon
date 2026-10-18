package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COVER_MAX_DISTANCE
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.DRIFT_ENTER_DEG
import com.summondemons.safely.ritual.RitualConfig.DRIFT_EXIT_DEG
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

/**
 * Pure ritual rules (ADR-0008, the timer of ADR-0010/ADR-0025, verdicts of ADR-0019, the
 * on-device chant check of ADR-0027): one call per 20 ms tick.
 */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s, input.inCourt)
            RitualPhase.Chanting, RitualPhase.Wavering -> chant(s, input.voice, input.heard)
            RitualPhase.Climax -> climax(s)
            RitualPhase.Done -> Step(s)
        }
    }

    /**
     * Whether a step with [state] and [azimuth] can lock on this tick.
     * Keeps RitualEngine pure: inputs and state only.
     */
    fun canLock(state: RitualState, azimuth: Float?): Boolean {
        if (state.phase != RitualPhase.Seeking && state.phase != RitualPhase.Locking) return false
        val s = state.copy(azimuth = azimuth)
        if (!s.aligned) return false
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        return held >= LOCK_HOLD_MS
    }

    private fun seek(s: RitualState, inCourt: Boolean): Step {
        val effects = mutableListOf<RitualEffect>()
        val bucket = s.azimuth?.let { proximityBucket(angularDistance(it, s.target)) }
        val previous = s.proximityBucket
        if (bucket != null && (previous == null || bucket < previous)) effects += RitualEffect.ProximityTick
        if (!s.aligned) {
            return Step(s.copy(phase = RitualPhase.Seeking, lockMs = 0L, proximityBucket = bucket), effects)
        }
        val held = (if (s.phase == RitualPhase.Locking) s.lockMs else 0L) + FRAME_MS
        if (held < LOCK_HOLD_MS) {
            return Step(s.copy(phase = RitualPhase.Locking, lockMs = held, proximityBucket = bucket), effects)
        }
        effects += RitualEffect.Locked
        val chanting = s.copy(
            phase = RitualPhase.Chanting,
            lockMs = 0L,
            proximityBucket = bucket,
            courtMultiplier = if (inCourt) 1f + COURT_BONUS else 1f,
            sinceSyllableMs = ONSET_REFRACTORY_MS,
            voicedRunMs = 0L,
        )
        return Step(chanting, effects)
    }

    private fun chant(s: RitualState, voice: VoiceFrame, heard: LineHeard?): Step {
        val effects = mutableListOf<RitualEffect>()
        if (!onCourse(s)) {
            // Off heading: nothing counts; once locked, never back to Seeking (ADR-0010).
            if (s.phase == RitualPhase.Chanting) effects += RitualEffect.Drift
            return drain(s.copy(phase = RitualPhase.Wavering, voicedRunMs = 0L), FOCUS_DRIFT_MULTIPLIER, effects)
        }
        if (s.phase == RitualPhase.Wavering) effects += RitualEffect.Resumed
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        var chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run)

        val total = s.syllables[s.phrase]
        if (s.mode == CheckMode.Whisper && heard != null && heard.phrase == s.phrase) {
            effects += RitualEffect.LineChecked(heard.phrase, heard.found, heard.distance)
            if (heard.found) {
                return seal(chanting.copy(covered = chanting.covered + heard.covered, verdict = ChantVerdict(ChantVerdict.Kind.Sealed, s.phrase)), effects)
            }
            // Only checks close to the line turn words green: babble never does (ADR-0028).
            if (heard.distance <= COVER_MAX_DISTANCE) chanting = chanting.copy(covered = chanting.covered + heard.covered)
            // A miss after the whole line was recited: the words clear for another try (ADR-0028).
            if (chanting.lit >= total) {
                effects += RitualEffect.Retry(s.phrase)
                chanting = chanting.copy(lit = 0, covered = emptySet())
            }
        }

        // In Whisper mode the line can always be recited again; lenient seals at its last syllable.
        val lights = voice.voiced && (chanting.lit < total || s.mode == CheckMode.Whisper) &&
            ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        if (!lights) return drain(chanting, 1, effects)

        effects += RitualEffect.SyllableLit
        val again = chanting.lit >= total
        if (again) effects += RitualEffect.Retry(s.phrase)
        // Chanting again clears the last verdict (ADR-0019); past the end, the line starts over (ADR-0028).
        val lit = chanting.copy(
            sinceSyllableMs = 0L,
            voicedRunMs = 0L,
            lit = if (again) 1 else chanting.lit + 1,
            covered = if (again) emptySet() else chanting.covered,
            verdict = null,
        )
        if (s.mode == CheckMode.Lenient && lit.lit == total) return seal(lit, effects)
        return drain(lit, 1, effects)
    }

    /** Locking needs ±20°; once locked the ritual wavers beyond 25° and resumes within 20° (ADR-0027). */
    private fun onCourse(s: RitualState): Boolean {
        val azimuth = s.azimuth ?: return false
        val limit = if (s.phase == RitualPhase.Chanting) DRIFT_ENTER_DEG else DRIFT_EXIT_DEG
        return angularDistance(azimuth, s.target) <= limit
    }

    /** Seals the current phrase: the next one starts with a full timer, or the 4th starts the climax. */
    private fun seal(s: RitualState, effects: MutableList<RitualEffect>): Step {
        effects += RitualEffect.Sealed(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) {
            val budget = focusBudget(phrase, s.courtMultiplier)
            return Step(s.copy(phrase = phrase, lit = 0, covered = emptySet(), focusMs = budget, focusBudgetMs = budget), effects)
        }
        effects += RitualEffect.ClimaxStart(s.flawless)
        val climax = s.copy(
            phase = RitualPhase.Climax, phrase = phrase, lit = 0, covered = emptySet(),
            focusMs = 0L, focusBudgetMs = 0L, climaxMs = 0L,
        )
        return Step(climax, effects)
    }

    /**
     * Drains the timer by [multiplier] frames (phrases II–IV only); at 0 the last seal cracks.
     * While a fresh verdict is on screen the timer waits instead (ADR-0019, ADR-0025).
     */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val verdict = s.verdict
        if (verdict != null && verdict.holdsClock) return Step(s.copy(verdict = verdict.copy(heldMs = verdict.heldMs + FRAME_MS)), effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        return crack(s, effects)
    }

    /** The timer ran out: the last seal cracks and its phrase reopens with a full timer (ADR-0010, ADR-0025). */
    private fun crack(s: RitualState, effects: MutableList<RitualEffect>): Step {
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(phrase, s.courtMultiplier)
        val cracked = if (s.mode == CheckMode.Whisper) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null
        return Step(
            s.copy(
                phrase = phrase, lit = 0, covered = emptySet(), focusMs = budget, focusBudgetMs = budget,
                flawless = false, verdict = cracked,
            ),
            effects,
        )
    }

    private fun climax(s: RitualState): Step {
        val elapsed = s.climaxMs + FRAME_MS
        if (elapsed < CLIMAX_MS) return Step(s.copy(climaxMs = elapsed))
        return Step(s.copy(phase = RitualPhase.Done, climaxMs = CLIMAX_MS), listOf(RitualEffect.Summoned))
    }

    private fun proximityBucket(distance: Float): Int? =
        if (distance <= PROXIMITY_RANGE_DEG) (distance / PROXIMITY_STEP_DEG).toInt() else null
}
