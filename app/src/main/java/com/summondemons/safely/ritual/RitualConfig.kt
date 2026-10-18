package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0008, ADR-0010, ADR-0018). Per-demon parameters live in `Demon` (ADR-0006). */
object RitualConfig {
    const val PHRASE_COUNT = 4
    const val HEADING_TOLERANCE_DEG = 20f

    // Ritual 2.0 state machine (ADR-0008)
    const val FRAME_MS = 20L
    const val LOCK_HOLD_MS = 600L
    const val CLIMAX_MS = 2_500L
    const val ONSET_REFRACTORY_MS = 120L
    const val SUSTAIN_SYLLABLE_MS = 350L
    const val PROXIMITY_RANGE_DEG = 60f

    // Focus clock (ADR-0010) and the Court Hours bonus (ADR-0012)
    const val FOCUS_BASE_MS = 3_000L
    const val FOCUS_PER_SYLLABLE_MS = 500L
    const val FOCUS_DRIFT_MULTIPLIER = 3
    const val COURT_BONUS = 0.25f
    /** Below this focus fraction the focus bar beats. */
    const val FOCUS_LOW_FRACTION = 0.25f
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

    // Formants by linear prediction (ADR-0018)
    /** Formant analysis decimates the mic to about this rate (F1/F2 live below 3 kHz). */
    const val FORMANT_RATE = 8_000
    const val LPC_ORDER = 10
    const val PRE_EMPHASIS = 0.94
    const val FORMANT_MAX_BANDWIDTH_HZ = 500.0
    const val FORMANT_F1_MIN_HZ = 200.0
    const val FORMANT_F1_MAX_HZ = 1_200.0
    const val FORMANT_F2_MAX_HZ = 3_400.0

    // Chant plausibility check (ADR-0018)
    /** Vocal-tract scales tried per phrase: adult male (1.0) to child (~1.45). */
    val VOWEL_SCALES = listOf(0.9, 1.0, 1.1, 1.2, 1.3, 1.45)
    /** Log-formant distance (natural log units) at which two vowels count as fully different. */
    const val VOWEL_MAX_DISTANCE = 0.8
    const val CHANT_GAP_COST = 0.7
    const val CHANT_UNMEASURED_COST = 0.5
    const val CHANT_SYLLABLES_PER_PAUSE = 6
    const val CHANT_PAUSE_PENALTY = 0.25f
    const val CHANT_FAST_INTERVAL_MS = 170L
    const val CHANT_RHYTHM_WEIGHT = 0.2f
    /** A phrase seals only when its [ChantScore.total] reaches this. Calibrated from tester clips. */
    const val CHANT_PASS_SCORE = 0.6f
    /** After the last syllable lights, its vowel is heard until voicing stops or this elapses. */
    const val SEAL_LISTEN_MS = 300L
    /** An unvoiced run at least this long inside a phrase counts as a pause (breath). */
    const val CHANT_PAUSE_MS = 250L

    // Chant screen (ADR-0019)
    /** After a verdict (sealed, rejected, cracked) the focus clock waits this long, or until the next syllable. */
    const val VERDICT_HOLD_MS = 3_000L
    /** A word whose syllables average at least this vowel distance is marked doubtful (a hint only). */
    const val WORD_DOUBT_DISTANCE = 0.6

    // Mic level for glow only (never progress)
    const val MIC_LEVEL_CEILING = 8_000.0
    const val MIC_LEVEL_ATTACK_MS = 40f
    const val MIC_LEVEL_RELEASE_MS = 250f

    // Debug "Hold to Chant"
    const val HOLD_TO_CHANT_RMS = 4_000.0
    const val HOLD_TO_CHANT_ONSET_FRAMES = 12
}
