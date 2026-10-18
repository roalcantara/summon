package com.summondemons.safely.ritual

import java.io.File

/** Replays a logged session through today's engine and tuning (ADR-0025). */
object SessionReplay {

    data class Replayed(val logged: SessionTick, val step: Step)

    fun read(file: File): Pair<SessionHeader, List<SessionTick>> = SessionRecords.decode(file.readLines())

    fun replay(header: SessionHeader, ticks: List<SessionTick>): List<Replayed> {
        var state = RitualState.start(header.target, header.phrases, judged = header.judged)
        return ticks.filter { it.isTick }.map { logged ->
            val step = RitualEngine.step(state, logged.input())
            state = step.state
            Replayed(logged, step)
        }
    }
}
