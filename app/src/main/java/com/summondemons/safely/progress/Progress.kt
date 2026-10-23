package com.summondemons.safely.progress

import kotlinx.serialization.Serializable

/** One-time effects still to play after a summon (ADR-0013): avatar burn-away, dossier ink-in. */
@Serializable
data class RevealPending(
    val avatar: Boolean = false,
    /** Offices, Appearance and Summoner's warning pages. */
    val codex: Boolean = false,
    /** Weakness and Abilities pages. */
    val battle: Boolean = false,
) {
    companion object {
        val All = RevealPending(avatar = true, codex = true, battle = true)
    }
}

/** Which one-time effect to clear (`ProgressRules.consumeReveal`). */
enum class Reveal { Avatar, Codex, Battle }

@Serializable
data class DemonRecord(
    /** Summoned and not escaped. Drives the stage (silhouette, summon cue, avatar sigil). */
    val bound: Boolean = false,
    /** Lore unlocked; stays true after an escape. Drives the dossier (smudged pages). */
    val revealed: Boolean = false,
    /** Ever summoned without a crack (sticky); unlocks the Secret page. */
    val flawless: Boolean = false,
    /** Local epoch day of the last visit while bound. */
    val lastVisitDay: Long? = null,
    val revealPending: RevealPending = RevealPending(),
    val escapeBannerPending: Boolean = false,
    /** Local epoch day of the last summon; null in saves from before 2026-10-23 (ADR-0036). */
    val boundDay: Long? = null,
    /** Local epoch day the "Psst" bubble last burst; it comes back the next day (ADR-0036, amended). */
    val teaserOpenedDay: Long? = null,
)

/** Everything that survives a restart (ADR-0013). Days are local epoch days. */
@Serializable
data class Progress(
    /** A missing id means `DemonRecord()`. */
    val demons: Map<String, DemonRecord> = emptyMap(),
    val lastSummonDay: Long? = null,
    val homeHintSeen: Boolean = false,
    val lastViewed: String? = null,
) {
    fun record(id: String): DemonRecord = demons[id] ?: DemonRecord()
}
