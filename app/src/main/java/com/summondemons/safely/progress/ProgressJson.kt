package com.summondemons.safely.progress

import kotlinx.serialization.json.Json

/** `Progress` ⇄ the JSON string stored under `progress_v1` (ADR-0013). Pure: no Android types. */
object ProgressJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(progress: Progress): String = json.encodeToString(Progress.serializer(), progress)

    /** Null when [text] is not a readable `Progress`. */
    fun decodeOrNull(text: String): Progress? =
        try {
            json.decodeFromString(Progress.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            null
        }

    /** Missing, unreadable or corrupt data starts from `Progress()`. */
    fun decode(text: String?): Progress = text?.let(::decodeOrNull) ?: Progress()
}
