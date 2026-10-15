package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0007). Per-demon parameters live in `Demon` (ADR-0006). */
object RitualConfig {
    const val PHRASE_COUNT = 4
    const val HEADING_TOLERANCE_DEG = 20f
    const val PHRASE_SUSTAIN_MS = 2_000L
    const val TICK_MS = 100L
    const val SILENCE_DECAY_FACTOR = 0.5f
    const val VOLUME_RMS_THRESHOLD = 1_800.0
    const val SUCCESS_HOLD_MS = 800L

    /** Sigil opacity indexed by phrases completed (PRODUCT.md). */
    val OPACITY_STEPS: List<Float> = listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)
}
