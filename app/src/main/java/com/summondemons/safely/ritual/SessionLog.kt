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
    val judged: Boolean,
    /** Wall-clock start, epoch ms. */
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
    val c: Int = 0,
    val lit: Int = 0,
    val fm: Long = 0L,
    val fx: List<String> = emptyList(),
    val judged: JudgedRecord? = null,
    val note: String? = null,
) {
    val isTick: Boolean get() = ph != null

    fun input(): RitualInput = RitualInput(
        azimuth = az,
        voice = VoiceFrame(rms, voiced = v, onset = o, formants = if (f1 != null && f2 != null) Formants(f1, f2) else null),
        inCourt = court,
    )
}

/** A chunk verdict with its aligned path (ADR-0022). */
@Serializable
data class JudgedRecord(
    val phrase: Int,
    val chunk: Int,
    val passed: Boolean,
    val cost: Double,
    val rank: Double? = null,
    val shuffles: Int,
    val talk: Boolean,
    val slotCosts: List<Double?>,
    val slotFrames: List<Int>,
    val skipped: Int,
)

object SessionRecords {
    val json = Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun tick(t: Long, input: RitualInput, step: Step, hold: Boolean): SessionTick {
        val s = step.state
        val judged = step.effects.filterIsInstance<RitualEffect.Judged>().lastOrNull()
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
            c = s.chunk,
            lit = s.lit,
            fm = s.focusMs,
            fx = step.effects.map(::effectName),
            judged = judged?.let(::record),
        )
    }

    /** Short, stable effect names; a verdict's details go to [JudgedRecord]. */
    fun effectName(effect: RitualEffect): String = when (effect) {
        is RitualEffect.Judged -> "Judged(${effect.phrase},${effect.chunk})"
        else -> effect.toString()
    }

    fun record(j: RitualEffect.Judged): JudgedRecord {
        val v = j.verdict
        return JudgedRecord(
            phrase = j.phrase,
            chunk = j.chunk,
            passed = v.passed,
            cost = v.cost,
            rank = v.rank,
            shuffles = v.shuffles,
            talk = v.talk,
            slotCosts = v.alignment.slotCosts,
            slotFrames = v.alignment.slotFrames,
            skipped = v.alignment.skippedFrames,
        )
    }

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
