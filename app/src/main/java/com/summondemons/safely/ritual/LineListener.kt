package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHECK_CONTINUOUS_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_MIN_VOICED_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_PAUSE_MS
import com.summondemons.safely.ritual.RitualConfig.CHECK_WINDOW_MS
import com.summondemons.safely.ritual.RitualConfig.FRAME_MS
import com.summondemons.safely.ritual.RitualConfig.WHISPER_RATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Speech to text for the chant check; Whisper on device, fakes in tests (ADR-0027). */
fun interface Transcriber {
    suspend fun transcribe(pcm: FloatArray): List<TranscriptSegment>
}

/** What one check found, as engine input (ADR-0027). */
data class LineHeard(val phrase: Int, val found: Boolean, val distance: Double, val covered: Set<Int>)

/** One finished check, for the engine and the session log. */
data class CheckReport(
    val heard: LineHeard,
    val transcript: String,
    val letters: String,
    val windowMs: Long,
    val voicedMs: Long,
    val checkMs: Long,
)

/**
 * Buffers 16 kHz audio since the current line began and checks it with [transcriber] after
 * pauses (ADR-0027): a pause ≥ CHECK_PAUSE_MS after ≥ CHECK_MIN_VOICED_MS of new voice, or
 * CHECK_CONTINUOUS_MS of continuous voice. One check runs at a time; triggers meanwhile
 * coalesce into one more. Thread-safe: frames arrive on the tick, results on [scope].
 */
class LineListener(
    private val transcriber: Transcriber,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val capacity = (CHECK_WINDOW_MS * WHISPER_RATE / 1_000).toInt()
    private val audio = FloatArray(capacity)
    private var audioStart = 0
    private var audioSize = 0
    private val voicedFrames = ArrayDeque<Boolean>()
    private val maxFrames = (CHECK_WINDOW_MS / FRAME_MS).toInt()

    private var token = 0
    private var phrase = -1
    private var line = ""
    private var newVoicedMs = 0L
    private var quietMs = 0L
    private var continuousMs = 0L
    private var running = false
    private var again = false
    private val results = ArrayDeque<CheckReport>()

    /** A check is in flight (the UI pulses the current seal). */
    @Volatile
    var checking = false
        private set

    /** Starts listening for [phrase] (its text is [text]); a no-op if that line is already current. */
    @Synchronized
    fun start(phrase: Int, text: String) {
        if (phrase == this.phrase && text == line) return
        token++
        this.phrase = phrase
        line = text
        audioStart = 0
        audioSize = 0
        voicedFrames.clear()
        newVoicedMs = 0L
        quietMs = 0L
        continuousMs = 0L
        again = false
        results.clear()
    }

    /**
     * The line starts over (ADR-0028): the audio heard so far is dropped so the next attempt is
     * judged on its own. A check already running still reports, so a late hit still seals.
     */
    @Synchronized
    fun restart() {
        audioStart = 0
        audioSize = 0
        voicedFrames.clear()
        newVoicedMs = 0L
        quietMs = 0L
        continuousMs = 0L
        again = false
    }

    /** One 20 ms frame of 16 kHz audio and whether it was voiced. */
    @Synchronized
    fun onFrame(samples: FloatArray, voiced: Boolean) {
        append(samples)
        voicedFrames.addLast(voiced)
        if (voicedFrames.size > maxFrames) voicedFrames.removeFirst()
        if (voiced) {
            newVoicedMs += FRAME_MS
            continuousMs += FRAME_MS
            quietMs = 0L
        } else {
            quietMs += FRAME_MS
            continuousMs = 0L
        }
        val afterPause = quietMs == CHECK_PAUSE_MS && newVoicedMs >= CHECK_MIN_VOICED_MS
        val longVoice = continuousMs >= CHECK_CONTINUOUS_MS
        if (afterPause || longVoice) {
            newVoicedMs = 0L
            continuousMs = 0L
            trigger()
        }
    }

    /** The oldest unread report for the current line, if any. */
    @Synchronized
    fun poll(): CheckReport? = results.removeFirstOrNull()

    private fun trigger() {
        if (voicedFrames.count { it } * FRAME_MS < CHECK_MIN_VOICED_MS) return
        if (running) {
            again = true
            return
        }
        launchCheck()
    }

    private fun launchCheck() {
        running = true
        checking = true
        val myToken = token
        val myPhrase = phrase
        val myLine = line
        val pcm = snapshot()
        val windowMs = pcm.size * 1_000L / WHISPER_RATE
        val voicedMs = voicedFrames.count { it } * FRAME_MS
        scope.launch {
            val started = now()
            val segments = runCatching { transcriber.transcribe(pcm) }.getOrDefault(emptyList())
            val match = LineMatcher.match(myLine, segments)
            val report = CheckReport(
                heard = LineHeard(myPhrase, match.found, match.distance, match.covered),
                transcript = segments.joinToString(" ") { it.text.trim() },
                letters = match.heardLetters,
                windowMs = windowMs,
                voicedMs = voicedMs,
                checkMs = now() - started,
            )
            finished(myToken, report)
        }
    }

    @Synchronized
    private fun finished(myToken: Int, report: CheckReport) {
        running = false
        checking = false
        if (myToken != token) {
            again = false
            return
        }
        results.addLast(report)
        if (again) {
            again = false
            launchCheck()
        }
    }

    private fun append(samples: FloatArray) {
        for (x in samples) {
            if (audioSize < capacity) {
                audio[(audioStart + audioSize) % capacity] = x
                audioSize++
            } else {
                audio[audioStart] = x
                audioStart = (audioStart + 1) % capacity
            }
        }
    }

    private fun snapshot(): FloatArray = FloatArray(audioSize) { audio[(audioStart + it) % capacity] }
}
