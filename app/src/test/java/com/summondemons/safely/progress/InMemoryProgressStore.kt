package com.summondemons.safely.progress

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** A store whose first value can be held back ([loaded] = false until [load]); counts every update. */
class InMemoryProgressStore(initial: Progress = Progress(), loaded: Boolean = true) : ProgressStore {
    val state = MutableStateFlow(initial)
    private val gate = CompletableDeferred<Unit>().apply { if (loaded) complete(Unit) }
    var updates = 0
        private set

    override val progress: Flow<Progress> = flow {
        gate.await()
        emitAll(state)
    }

    fun load() {
        gate.complete(Unit)
    }

    override suspend fun update(transform: (Progress) -> Progress) {
        gate.await()
        updates++
        state.update(transform)
    }
}
