package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ALIGN_FRAME_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_MAX_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_MIN_SLOT_FRAMES
import com.summondemons.safely.ritual.RitualConfig.ALIGN_OPTIONAL_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_SKIPPED_WEIGHT
import com.summondemons.safely.ritual.RitualConfig.BASELINE_MIN_DIFFERENT
import com.summondemons.safely.ritual.RitualConfig.BASELINE_MIN_SHUFFLES
import com.summondemons.safely.ritual.RitualConfig.BASELINE_PASS_RANK
import com.summondemons.safely.ritual.RitualConfig.BASELINE_SHUFFLES
import com.summondemons.safely.ritual.RitualConfig.CHANT_GAP_COST
import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.NEUTRAL_VOWEL
import com.summondemons.safely.ritual.RitualConfig.TALK_MAX_SLOT_MS
import com.summondemons.safely.ritual.RitualConfig.VOWEL_SCALES
import com.summondemons.safely.ritual.RitualConfig.WORD_DOUBT_DISTANCE
import kotlin.math.abs
import kotlin.random.Random

/** How a chunk's frames line up with its slots at the best vocal-tract scale (ADR-0022). */
data class Alignment(
    /** Per slot: the mean cost of its frames when heard, or null when skipped. */
    val slotCosts: List<Double?>,
    /** Frames per slot; 0 when skipped. */
    val slotFrames: List<Int>,
    /** Frames left out of every slot (consonants, transitions). */
    val skippedFrames: Int,
    /** Mean slot cost (a skipped slot costs its gap) plus [ALIGN_SKIPPED_WEIGHT] × share of skipped frames. */
    val cost: Double,
)

/** The chant check's verdict on one chunk (ADR-0022). */
data class ChunkVerdict(
    val passed: Boolean,
    /** Alignment cost against the real vowel order (0 = perfect). */
    val cost: Double,
    /** Share of valid shuffles the real order beat (ties count half), or null in the low-diversity fallback. */
    val rank: Double?,
    /** Valid shuffles compared. */
    val shuffles: Int,
    /** Vetoed as conversational speed. */
    val talk: Boolean,
    /** One hint per word of the chunk. */
    val marks: List<WordMark>,
    /** The real alignment, for diagnostics. */
    val alignment: Alignment,
)

/**
 * Judges a chanted chunk against its expected vowels (ADR-0022). Not word recognition: it
 * aligns every voiced frame's F1/F2 with the vowel slots the Latin spelling predicts.
 */
object ChantAligner {

    /** The best alignment over the vocal-tract scales. */
    fun align(slots: List<VowelSlot>, frames: List<Formants?>): Alignment =
        VOWEL_SCALES.map { alignAt(slots, frames, it) }.minBy { it.cost }

    /** Cost of putting [frame] in [slot]: distance to its vowel, or to "uh" when that is closer and allowed. */
    fun slotDistance(frame: Formants?, slot: VowelSlot, scale: Double): Double {
        if (frame == null || slot.vowel == null) return CHANT_UNMEASURED_COST
        val vowel = ChantJudge.distance(frame, slot.vowel, scale)
        if (!slot.neutral) return vowel
        val neutral = ChantJudge.distance(frame, Formants(NEUTRAL_VOWEL.f1 * scale, NEUTRAL_VOWEL.f2 * scale))
        return minOf(vowel, neutral)
    }

    fun gapCost(slot: VowelSlot): Double = if (slot.optional) ALIGN_OPTIONAL_SKIP_COST else CHANT_GAP_COST

    /**
     * Passes when the real order costs no more than [ALIGN_MAX_COST], beats at least
     * [BASELINE_PASS_RANK] of the shuffles (unless fewer than [BASELINE_MIN_SHUFFLES] survive), and
     * the median heard slot is not shorter than [TALK_MAX_SLOT_MS]. [seed] is the chunk's text.
     */
    fun judge(slots: List<VowelSlot>, frames: List<Formants?>, seed: String): ChunkVerdict {
        val real = align(slots, frames)
        val shuffled = shuffles(slots, seed).map { align(it, frames).cost }
        val rank = if (shuffled.size >= BASELINE_MIN_SHUFFLES) {
            shuffled.sumOf { c ->
                when {
                    abs(c - real.cost) <= TIE -> 0.5
                    c > real.cost -> 1.0
                    else -> 0.0
                }
            } / shuffled.size
        } else {
            null
        }
        val heard = real.slotFrames.filter { it > 0 }.sorted()
        val talk = heard.isNotEmpty() && heard[heard.size / 2] * FRAME_MS < TALK_MAX_SLOT_MS
        val passed = !talk && real.cost <= ALIGN_MAX_COST && (rank == null || rank >= BASELINE_PASS_RANK)
        return ChunkVerdict(passed, real.cost, rank, shuffled.size, talk, marks(slots, real), real)
    }

    /**
     * [BASELINE_SHUFFLES] reorderings of whole slots (each vowel keeps its glide and neutral flags;
     * word positions stay put), seeded by [seed]; those differing from the real vowel order in
     * fewer than [BASELINE_MIN_DIFFERENT] of the slots are dropped.
     */
    fun shuffles(slots: List<VowelSlot>, seed: String): List<List<VowelSlot>> {
        val random = Random(seed.hashCode())
        val vowels = slots.map { it.vowel }
        return List(BASELINE_SHUFFLES) { slots.shuffled(random) }
            .filter { order -> order.indices.count { order[it].vowel != vowels[it] } >= BASELINE_MIN_DIFFERENT * slots.size }
            .map { order -> order.mapIndexed { i, slot -> slot.copy(word = slots[i].word) } }
    }

    /** Per word of the chunk: doubtful when its slots' mean cost reaches [WORD_DOUBT_DISTANCE]. */
    fun marks(slots: List<VowelSlot>, alignment: Alignment): List<WordMark> =
        slots.indices.groupBy { slots[it].word }.toSortedMap().values.map { indices ->
            val mean = indices.map { alignment.slotCosts[it] ?: gapCost(slots[it]) }.average()
            if (mean >= WORD_DOUBT_DISTANCE) WordMark.Doubtful else WordMark.Heard
        }

    private const val TIE = 1e-9

    /**
     * Viterbi over states (slot j, frames c in it; c = [ALIGN_MIN_SLOT_FRAMES] means "that many or
     * more"). Per frame: join slot j, or skip the frame. Between frames: leave slot j when it has
     * no frames (skipped, its gap cost) or enough (heard, free). The path is the cheapest by these
     * additive costs; the reported [Alignment.cost] is then computed from the path.
     */
    private fun alignAt(slots: List<VowelSlot>, frames: List<Formants?>, scale: Double): Alignment {
        val s = slots.size
        val n = frames.size
        val m = ALIGN_MIN_SLOT_FRAMES
        val width = m + 1
        val size = (s + 1) * width
        fun idx(j: Int, c: Int) = j * width + c
        val d = Array(n) { i -> DoubleArray(s) { j -> slotDistance(frames[i], slots[j], scale) } }

        val advance = Array(n + 1) { IntArray(size) { -1 } }
        val frameBack = Array(n) { IntArray(size) { -1 } }
        val frameJoined = Array(n) { BooleanArray(size) }
        var cost = DoubleArray(size) { Double.POSITIVE_INFINITY }.also { it[idx(0, 0)] = 0.0 }

        fun closure(layer: DoubleArray, back: IntArray) {
            for (j in 0 until s) {
                for (c in intArrayOf(0, m)) {
                    val v = layer[idx(j, c)]
                    if (v == Double.POSITIVE_INFINITY) continue
                    val next = v + if (c == 0) gapCost(slots[j]) else 0.0
                    val target = idx(j + 1, 0)
                    if (next < layer[target]) {
                        layer[target] = next
                        back[target] = idx(j, c)
                    }
                }
            }
        }

        closure(cost, advance[0])
        for (i in 0 until n) {
            val next = DoubleArray(size) { Double.POSITIVE_INFINITY }
            for (state in 0 until size) {
                val v = cost[state]
                if (v == Double.POSITIVE_INFINITY) continue
                val j = state / width
                val c = state % width
                if (j < s) {
                    val joined = idx(j, minOf(c + 1, m))
                    val viaJoin = v + d[i][j]
                    if (viaJoin < next[joined]) {
                        next[joined] = viaJoin
                        frameBack[i][joined] = state
                        frameJoined[i][joined] = true
                    }
                }
                val viaSkip = v + ALIGN_FRAME_SKIP_COST
                if (viaSkip < next[state]) {
                    next[state] = viaSkip
                    frameBack[i][state] = state
                    frameJoined[i][state] = false
                }
            }
            closure(next, advance[i + 1])
            cost = next
        }

        // Walk the path back from "all slots done" after the last frame.
        val assigned = List(s) { mutableListOf<Int>() }
        var skipped = 0
        var state = idx(s, 0)
        var layer = n
        while (true) {
            val previous = advance[layer][state]
            if (previous != -1) {
                state = previous
                continue
            }
            if (layer == 0) break
            val i = layer - 1
            if (frameJoined[i][state]) assigned[state / width] += i else skipped++
            state = frameBack[i][state]
            layer = i
        }

        val slotCosts = assigned.mapIndexed { j, list -> if (list.isEmpty()) null else list.sumOf { d[it][j] } / list.size }
        val mean = if (s == 0) 0.0 else slotCosts.mapIndexed { j, c -> c ?: gapCost(slots[j]) }.sum() / s
        val total = mean + ALIGN_SKIPPED_WEIGHT * skipped / maxOf(1, n)
        return Alignment(slotCosts, assigned.map { it.size }, skipped, total)
    }
}
