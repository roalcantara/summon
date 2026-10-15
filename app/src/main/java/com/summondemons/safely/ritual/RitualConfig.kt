package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0008). Per-demon parameters live in `Demon` (ADR-0006). */
object RitualConfig {
    const val PHRASE_COUNT = 4
    const val HEADING_TOLERANCE_DEG = 20f

    // Ritual 2.0 state machine (ADR-0008)
    const val FRAME_MS = 20L
    const val LOCK_HOLD_MS = 600L
    const val DRIFT_GRACE_MS = 1_500L
    const val CLIMAX_MS = 2_500L
    const val ONSET_REFRACTORY_MS = 120L
    const val SUSTAIN_SYLLABLE_MS = 350L
    const val PROXIMITY_RANGE_DEG = 60f
    const val PROXIMITY_STEP_DEG = 5f

    // Voice analysis on PCM16 (ADR-0008)
    const val VOICE_RMS_FLOOR = 1_200.0
    const val VOICING_MIN_CORRELATION = 0.5
    const val VOICE_MIN_HZ = 70
    const val VOICE_MAX_HZ = 400
    /** Autocorrelation strides samples above this rate to keep the cost flat. */
    const val ANALYSIS_RATE = 16_000
    const val ONSET_RATIO = 1.8
    const val ONSET_REARM_RATIO = 0.6
    const val ONSET_WINDOW_FRAMES = 5

    // Mic level for glow only (never progress)
    const val MIC_LEVEL_CEILING = 8_000.0
    const val MIC_LEVEL_ATTACK_MS = 40f
    const val MIC_LEVEL_RELEASE_MS = 250f

    // Debug "Hold to Chant"
    const val HOLD_TO_CHANT_RMS = 4_000.0
    const val HOLD_TO_CHANT_ONSET_FRAMES = 12

    // Legacy engine (removed in Task 17)
    const val PHRASE_SUSTAIN_MS = 2_000L
    const val TICK_MS = 100L
    const val SILENCE_DECAY_FACTOR = 0.5f
    const val VOLUME_RMS_THRESHOLD = 1_800.0
    const val SUCCESS_HOLD_MS = 800L
    val OPACITY_STEPS: List<Float> = listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)
}
