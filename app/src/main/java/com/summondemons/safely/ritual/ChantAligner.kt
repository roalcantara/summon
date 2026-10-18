package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.ALIGN_FRAME_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_MIN_SLOT_FRAMES
import com.summondemons.safely.ritual.RitualConfig.ALIGN_OPTIONAL_SKIP_COST
import com.summondemons.safely.ritual.RitualConfig.ALIGN_SKIPPED_WEIGHT
import com.summondemons.safely.ritual.RitualConfig.CHANT_GAP_COST
import com.summondemons.safely.ritual.RitualConfig.CHANT_UNMEASURED_COST
import com.summondemons.safely.ritual.RitualConfig.NEUTRAL_VOWEL
import com.summondemons.safely.ritual.RitualConfig.VOWEL_SCALES

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
