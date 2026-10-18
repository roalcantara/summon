package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CLIMAX_MS
import com.summondemons.safely.ritual.RitualConfig.COURT_BONUS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_DRIFT_MULTIPLIER
import com.summondemons.safely.ritual.RitualConfig.FOCUS_IDLE_GRACE_MS
import com.summondemons.safely.ritual.RitualConfig.FOCUS_REJECT_COST
import kotlin.math.roundToLong
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.LOCK_HOLD_MS
import com.summondemons.safely.ritual.RitualConfig.ONSET_REFRACTORY_MS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_RANGE_DEG
import com.summondemons.safely.ritual.RitualConfig.PROXIMITY_STEP_DEG
import com.summondemons.safely.ritual.RitualConfig.SEAL_LISTEN_MS
import com.summondemons.safely.ritual.RitualConfig.SUSTAIN_SYLLABLE_MS

/** Pure ritual rules (ADR-0008, the focus clock of ADR-0010, the chant check of ADR-0018/ADR-0022, verdicts of ADR-0019): one call per 20 ms tick. */
object RitualEngine {

    fun step(state: RitualState, input: RitualInput): Step {
        if (state.phase == RitualPhase.Done) return Step(state)
        val s = state.copy(azimuth = input.azimuth)
        return when (s.phase) {
            RitualPhase.Seeking, RitualPhase.Locking -> seek(s, input.inCourt)
            RitualPhase.Chanting, RitualPhase.Wavering -> chant(s, input.voice)
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

    private fun chant(s: RitualState, voice: VoiceFrame): Step {
        val effects = mutableListOf<RitualEffect>()
        if (!s.aligned) {
            // Off heading: nothing counts; once locked, never back to Seeking (ADR-0010).
            if (s.phase == RitualPhase.Chanting) effects += RitualEffect.Drift
            return drain(s.copy(phase = RitualPhase.Wavering, voicedRunMs = 0L), FOCUS_DRIFT_MULTIPLIER, effects)
        }
        if (s.phase == RitualPhase.Wavering) effects += RitualEffect.Resumed
        val since = s.sinceSyllableMs + FRAME_MS
        val run = if (voice.voiced) s.voicedRunMs + FRAME_MS else 0L
        val quiet = if (voice.voiced) 0L else s.quietMs + FRAME_MS
        val chanting = s.copy(phase = RitualPhase.Chanting, sinceSyllableMs = since, voicedRunMs = run, quietMs = quiet)
        if (s.take.closing) return close(chanting, voice, effects)
        val lights = voice.voiced && ((voice.onset && since >= ONSET_REFRACTORY_MS) || run >= SUSTAIN_SYLLABLE_MS)
        if (!lights) return drain(chanting.hearing { hear(voice, begun = s.lit > s.chunkStart()) }, idle(chanting), effects)

        effects += RitualEffect.SyllableLit
        val lit = s.lit + 1
        val last = lit == s.chunkEnd()
        // Chanting again clears the last verdict (ADR-0019).
        val reset = chanting.copy(sinceSyllableMs = 0L, voicedRunMs = 0L, lit = lit, verdict = null)
            .hearing { light(voice, first = s.lit == s.chunkStart(), last = last) }
        // With the chant check on, the last syllable's vowel is heard before the verdict (ADR-0018).
        if (!last || reset.take.closing) return drain(reset, idle(reset), effects)
        return seal(reset, effects)
    }

    /** Hears the end of the chunk's last syllable, then judges it (ADR-0022). */
    private fun close(s: RitualState, voice: VoiceFrame, effects: MutableList<RitualEffect>): Step {
        val take = s.take.hear(voice, begun = true)
        if (voice.voiced && take.closingMs < SEAL_LISTEN_MS) return drain(s.copy(take = take), idle(s), effects)
        val chunk = s.currentChunk()!!
        val verdict = ChantAligner.judge(chunk.slots, take.frames, chunk.text)
        effects += RitualEffect.Judged(s.phrase, s.chunk, verdict)
        val marks = phraseMarks(s, chunk, verdict.marks)
        if (verdict.passed) {
            if (s.chunk < s.chunks!![s.phrase].lastIndex) {
                effects += RitualEffect.ChunkPassed(s.phrase, s.chunk)
                return drain(s.copy(chunk = s.chunk + 1, take = ChantTake()), idle(s), effects)
            }
            return seal(s.copy(take = ChantTake(), verdict = ChantVerdict(ChantVerdict.Kind.Sealed, s.phrase, marks)), effects)
        }
        effects += RitualEffect.Rejected(s.phrase, s.chunk)
        val rejected = s.copy(lit = chunk.syllables.first, take = ChantTake(), verdict = ChantVerdict(ChantVerdict.Kind.Rejected, s.phrase, marks))
        if (s.phrase < 1) return Step(rejected, effects)
        val focus = rejected.focusMs - (rejected.focusBudgetMs * FOCUS_REJECT_COST).roundToLong()
        return if (focus > 0L) Step(rejected.copy(focusMs = focus), effects) else crack(rejected, effects)
    }

    /** Words before the chunk were heard; the chunk's own come from the verdict; later ones are pending. */
    private fun phraseMarks(s: RitualState, chunk: ChantChunk, marks: List<WordMark>): List<WordMark> =
        List(s.wordSizes!![s.phrase].size) { w ->
            when {
                w < chunk.words.first -> WordMark.Heard
                w in chunk.words -> marks[w - chunk.words.first]
                else -> WordMark.Pending
            }
        }

    private fun RitualState.currentChunk(): ChantChunk? = chunks?.getOrNull(phrase)?.getOrNull(chunk)

    private fun RitualState.chunkStart(): Int = currentChunk()?.syllables?.first ?: 0

    private fun RitualState.chunkEnd(): Int = currentChunk()?.let { it.syllables.last + 1 } ?: syllables[phrase]

    /** Focus drains 1× only after FOCUS_IDLE_GRACE_MS of silence; chanting and short pauses cost nothing (ADR-0023). */
    private fun idle(s: RitualState): Int = if (s.quietMs > FOCUS_IDLE_GRACE_MS) 1 else 0

    /** Seals the current phrase: the next one starts with a full clock, or the 4th starts the climax. */
    private fun seal(s: RitualState, effects: MutableList<RitualEffect>): Step {
        effects += RitualEffect.Sealed(s.phrase)
        val phrase = s.phrase + 1
        if (phrase < PHRASE_COUNT) {
            val budget = focusBudget(phrase, s.courtMultiplier)
            return Step(s.copy(phrase = phrase, lit = 0, chunk = 0, focusMs = budget, focusBudgetMs = budget), effects)
        }
        effects += RitualEffect.ClimaxStart(s.flawless)
        val climax = s.copy(phase = RitualPhase.Climax, phrase = phrase, lit = 0, focusMs = 0L, focusBudgetMs = 0L, climaxMs = 0L)
        return Step(climax, effects)
    }

    /** Advances the chant take only while the chant check is on. */
    private inline fun RitualState.hearing(update: ChantTake.() -> ChantTake): RitualState =
        if (chunks == null) this else copy(take = take.update())
    /**
     * Drains the focus meter by [multiplier] frames (phrases II–IV only); at 0 the last seal cracks. While a fresh
     * verdict is on screen the meter waits instead (ADR-0019, ADR-0023).
     */
    private fun drain(s: RitualState, multiplier: Int, effects: MutableList<RitualEffect>): Step {
        if (s.phrase < 1) return Step(s, effects)
        val verdict = s.verdict
        if (verdict != null && verdict.holdsClock) return Step(s.copy(verdict = verdict.copy(heldMs = verdict.heldMs + FRAME_MS)), effects)
        val focus = s.focusMs - FRAME_MS * multiplier
        if (focus > 0L) return Step(s.copy(focusMs = focus), effects)
        return crack(s, effects)
    }

    /** Focus ran out: the last seal cracks and its phrase reopens with a full meter (ADR-0010, ADR-0023). */
    private fun crack(s: RitualState, effects: MutableList<RitualEffect>): Step {
        val phrase = s.phrase - 1
        effects += RitualEffect.Cracked(phrase)
        val budget = focusBudget(phrase, s.courtMultiplier)
        val cracked = if (s.chunks != null) ChantVerdict(ChantVerdict.Kind.Cracked, phrase) else null
        return Step(
            s.copy(
                phrase = phrase, lit = 0, chunk = 0, focusMs = budget, focusBudgetMs = budget,
                flawless = false, take = ChantTake(), verdict = cracked, quietMs = 0L,
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
