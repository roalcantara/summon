package com.summondemons.safely.ritual

/** Ritual-wide tuning (ADR-0008, ADR-0010, ADR-0018, ADR-0019). Per-demon parameters live in `Demon` (ADR-0006). */
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

    // Focus timer (ADR-0025) and the Court Hours bonus (ADR-0012)
    /** Time allowed per part of phrases II–IV; refilled whenever a part passes (ADR-0025). */
    const val FOCUS_BUDGET_MS = 60_000L
    /** After the display rotates, the last heading is held this long while the phone turns (ADR-0025). */
    const val ROTATION_GRACE_MS = 1_500L

    // On-device Whisper chant check (ADR-0027)
    /** Whisper's input rate. */
    const val WHISPER_RATE = 16_000
    /** A line is found when the best-matching stretch of the transcript is within this distance. */
    const val LINE_MATCH_MAX = 0.60
    /** A word is covered (green) when this share of its letters align to equal letters. */
    const val WORD_COVER_MIN = 0.6
    /** Transcript segments Whisper rates as likelier silence than this are dropped. */
    const val NO_SPEECH_MAX = 0.6f
    /** Loops of up to this many words, repeated three times or more, collapse to one. */
    const val LOOP_MAX_WORDS = 6
    const val CHECK_PAUSE_MS = 600L
    const val CHECK_MIN_VOICED_MS = 800L
    const val CHECK_CONTINUOUS_MS = 6_000L
    const val CHECK_WINDOW_MS = 12_000L
    /** Past the line's last syllable, it starts over only after a pause this long (ADR-0028). */
    const val RECITE_AGAIN_PAUSE_MS = 1_000L
    /** Words turn green only from checks at most this far from the line (no green from babble; ADR-0028). */
    const val COVER_MAX_DISTANCE = 0.75
    /** Lenient mode when a 5 s device check takes longer than this. */
    const val DEVICE_MAX_CHECK_MS = 4_000L
    /** Once locked, the ritual wavers beyond this and resumes within [DRIFT_EXIT_DEG]. */
    const val DRIFT_ENTER_DEG = 25f
    const val DRIFT_EXIT_DEG = 20f
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

    // Chant screen (ADR-0019)
    /** After a verdict (sealed, rejected, cracked) the focus clock waits this long, or until the next syllable. */
    const val VERDICT_HOLD_MS = 3_000L
    /** How long the chant screen shows a line's score (and, on a miss, what was heard) (ADR-0029). */
    const val FEEDBACK_HOLD_MS = 2_500L

    // Mic level for glow only (never progress)
    const val MIC_LEVEL_CEILING = 8_000.0
    const val MIC_LEVEL_ATTACK_MS = 40f
    const val MIC_LEVEL_RELEASE_MS = 250f

    // Debug "Hold to Chant"
    const val HOLD_TO_CHANT_RMS = 4_000.0
    const val HOLD_TO_CHANT_ONSET_FRAMES = 12
}
