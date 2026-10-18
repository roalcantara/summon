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
    const val CHECK_WINDOW_MS = 25_000L
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

    // Chant check (ADR-0018, ADR-0022)
    /** Vocal-tract scales tried per phrase: adult male (1.0) to child (~1.45). */
    val VOWEL_SCALES = listOf(0.9, 1.0, 1.1, 1.2, 1.3, 1.45)
    /** Log-formant distance (natural log units) at which two vowels count as fully different. */
    const val VOWEL_MAX_DISTANCE = 0.8
    const val CHANT_GAP_COST = 0.7
    const val CHANT_UNMEASURED_COST = 0.5
    /** After the last syllable lights, its vowel is heard until voicing stops or this elapses. */
    const val SEAL_LISTEN_MS = 300L
    // Chant check v2 (ADR-0022): frame-level alignment and shuffle baseline
    /** A neutral "uh" (adult male); accepted on the unstressed last syllable of longer words. */
    val NEUTRAL_VOWEL = Formants(500.0, 1_500.0)
    /** A slot counts as heard only with at least this many frames (3 × 20 ms). */
    const val ALIGN_MIN_SLOT_FRAMES = 3
    /** Cost of leaving one frame out of every slot (a consonant or a transition). */
    const val ALIGN_FRAME_SKIP_COST = 0.6
    /** Cost of skipping a glide slot entirely (an ordinary slot costs CHANT_GAP_COST). */
    const val ALIGN_OPTIONAL_SKIP_COST = 0.15
    /** Weight of the share of skipped frames in a chunk's cost. */
    const val ALIGN_SKIPPED_WEIGHT = 0.6
    /** A chunk never passes above this cost. Calibrated from tester clips. */
    const val ALIGN_MAX_COST = 0.8
    const val BASELINE_SHUFFLES = 24
    /** Below this many valid shuffles only [ALIGN_MAX_COST] decides (low-diversity fallback). */
    const val BASELINE_MIN_SHUFFLES = 8
    /** A shuffle must differ from the real vowel order in at least this share of slots. */
    const val BASELINE_MIN_DIFFERENT = 0.3
    /** The real order must beat at least this share of the shuffles. Calibrated from tester clips. */
    const val BASELINE_PASS_RANK = 0.7
    /** A chunk whose median heard slot is shorter than this is talk, not chanting. */
    const val TALK_MAX_SLOT_MS = 70L
    const val CHUNK_MAX_SYLLABLES = 8
    const val CHUNK_MIN_SYLLABLES = 3

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
