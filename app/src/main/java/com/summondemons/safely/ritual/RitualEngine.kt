package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.OPACITY_STEPS
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualConfig.PHRASE_SUSTAIN_MS
import com.summondemons.safely.ritual.RitualConfig.SILENCE_DECAY_FACTOR

/** `phraseIndex` = phrases completed so far (0..4). */
data class RitualProgress(val phraseIndex: Int = 0, val sustainedMs: Long = 0L) {
    val completed: Boolean get() = phraseIndex >= PHRASE_COUNT
    val sigilOpacity: Float get() = OPACITY_STEPS[phraseIndex.coerceAtMost(PHRASE_COUNT)]
    val sustainFraction: Float get() = (sustainedMs.toFloat() / PHRASE_SUSTAIN_MS).coerceIn(0f, 1f)
}

/** Pure ritual rules (PRODUCT.md "Heading gate", "Chant", ADR-0007). */
object RitualEngine {

    fun step(progress: RitualProgress, aligned: Boolean, loud: Boolean, dtMs: Long): RitualProgress {
        if (progress.completed) return progress
        if (!aligned) return RitualProgress()
        if (loud) {
            val sustained = progress.sustainedMs + dtMs
            return if (sustained >= PHRASE_SUSTAIN_MS) {
                RitualProgress(phraseIndex = progress.phraseIndex + 1, sustainedMs = 0L)
            } else {
                progress.copy(sustainedMs = sustained)
            }
        }
        val decayed = progress.sustainedMs - (dtMs * SILENCE_DECAY_FACTOR).toLong()
        return progress.copy(sustainedMs = decayed.coerceAtLeast(0L))
    }
}
