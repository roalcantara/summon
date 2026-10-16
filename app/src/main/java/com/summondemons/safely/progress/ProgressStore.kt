package com.summondemons.safely.progress

import kotlinx.coroutines.flow.Flow

/** The single source of truth for progress (ADR-0013). */
interface ProgressStore {
    /** The stored progress; emits after every change. Unreadable data reads as `Progress()`. */
    val progress: Flow<Progress>

    /** Applies [transform] to the latest stored value and saves it. A failed write is logged and lost. */
    suspend fun update(transform: (Progress) -> Progress)
}
