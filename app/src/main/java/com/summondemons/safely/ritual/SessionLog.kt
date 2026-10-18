package com.summondemons.safely.ritual

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Debug-only record of a ritual (ADR-0025): a [SessionHeader] line, then one [SessionTick] line
 * per 20 ms tick, as JSON lines. It holds the analysed mic frames (level, voicing, formants), not
 * audio, and replays exactly through [RitualEngine] in the test harness. Release builds get [None]
 * from their own source set, so they contain no logging code.
 */
interface SessionLog {
    fun start(header: SessionHeader)

    fun write(tick: SessionTick)

    fun stop()

    object None : SessionLog {
        override fun start(header: SessionHeader) = Unit
        override fun write(tick: SessionTick) = Unit
        override fun stop() = Unit
    }
}

@Serializable
data class SessionHeader(
    val demon: String,
    val target: Float,
    val phrases: List<String>,
    /** CheckMode name (ADR-0027). */
    val mode: String,
    val startedMs: Long,
)

/** One tick: its input, the state after it, and its effects. A line with only [t] and [note] is an event. */
@Serializable
data class SessionTick(
    /** Ms since the session started (ticks × 20 ms). */
    val t: Long,
    val az: Float? = null,
    val court: Boolean = false,
    val rms: Double = 0.0,
    val v: Boolean = false,
    val o: Boolean = false,
    val f1: Double? = null,
    val f2: Double? = null,
    /** Debug "Hold to Chant" produced this voice frame. */
    val hold: Boolean = false,
    val ph: String? = null,
    val p: Int = 0,
    /** Words of the current line covered so far (ADR-0027). */
    val c: Int = 0,
    val lit: Int = 0,
    val fm: Long = 0L,
    val fx: List<String> = emptyList(),
    /** The check result fed to the engine on this tick (input; replayed). */
    val heard: HeardRecord? = null,
    /** Details of a check that finished on this tick (debug readout, threshold re-check). */
    val check: CheckRecord? = null,
    val note: String? = null,
) {
    val isTick: Boolean get() = ph != null

    fun input(): RitualInput = RitualInput(
        azimuth = az,
        voice = VoiceFrame(rms, voiced = v, onset = o, formants = if (f1 != null && f2 != null) Formants(f1, f2) else null),
        inCourt = court,
        heard = heard?.let { LineHeard(it.phrase, it.found, it.distance, it.covered.toSet()) },
    )
}

@Serializable
data class HeardRecord(val phrase: Int, val found: Boolean, val distance: Double, val covered: List<Int>)

/** One Whisper check (ADR-0027). */
@Serializable
data class CheckRecord(
    val phrase: Int,
    val found: Boolean,
    val distance: Double,
    val transcript: String,
    val letters: String,
    val windowMs: Long,
    val voicedMs: Long,
    val checkMs: Long,
)
object SessionRecords {
    val json = Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun tick(t: Long, input: RitualInput, step: Step, hold: Boolean, check: CheckReport? = null): SessionTick {
        val s = step.state
        return SessionTick(
            t = t,
            az = input.azimuth,
            court = input.inCourt,
            rms = input.voice.rms,
            v = input.voice.voiced,
            o = input.voice.onset,
            f1 = input.voice.formants?.f1,
            f2 = input.voice.formants?.f2,
            hold = hold,
            ph = s.phase.name,
            p = s.phrase,
            c = s.covered.size,
            lit = s.lit,
            fm = s.focusMs,
            fx = step.effects.map(::effectName),
            heard = input.heard?.let { HeardRecord(it.phrase, it.found, it.distance, it.covered.sorted()) },
            check = check?.let {
                CheckRecord(it.heard.phrase, it.heard.found, it.heard.distance, it.transcript, it.letters, it.windowMs, it.voicedMs, it.checkMs)
            },
        )
    }

    fun effectName(effect: RitualEffect): String = effect.toString()

    fun encode(header: SessionHeader): String = json.encodeToString(SessionHeader.serializer(), header)

    fun encode(tick: SessionTick): String = json.encodeToString(SessionTick.serializer(), tick)

    /** Parses a whole log: the header line, then every tick or note line. */
    fun decode(lines: List<String>): Pair<SessionHeader, List<SessionTick>> {
        val content = lines.filter { it.isNotBlank() }
        require(content.isNotEmpty()) { "empty session log" }
        val header = json.decodeFromString(SessionHeader.serializer(), content.first())
        return header to content.drop(1).map { json.decodeFromString(SessionTick.serializer(), it) }
    }
}
