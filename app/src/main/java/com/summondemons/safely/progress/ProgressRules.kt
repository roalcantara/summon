package com.summondemons.safely.progress

import com.summondemons.safely.progress.ProgressConfig.ESCAPE_AFTER_DAYS
import com.summondemons.safely.progress.ProgressConfig.RESTLESS_AFTER_DAYS

/**
 * Pure progress rules (ADR-0013, ADR-0015). Days are local epoch days; callers pass
 * `today`, so tests and the debug clock can move time.
 */
object ProgressRules {

    /** One summon per local day. Strictly `today > lastSummonDay`: a clock moved back never unlocks early. */
    fun canSummon(p: Progress, today: Long): Boolean = p.lastSummonDay == null || today > p.lastSummonDay

    /** Saved at ClimaxStart. [day] is the day the ritual started, so 23:59 → 00:01 spends the earlier day. */
    fun commitSummon(p: Progress, id: String, day: Long, flawless: Boolean): Progress {
        val old = p.record(id)
        val record = old.copy(
            bound = true,
            revealed = true,
            flawless = old.flawless || flawless,
            lastVisitDay = day,
            revealPending = RevealPending.All,
            escapeBannerPending = false,
            boundDay = day,
        )
        return p.copy(
            demons = p.demons + (id to record),
            lastSummonDay = maxOf(day, p.lastSummonDay ?: day),
            lastViewed = id,
        )
    }

    /** Viewing a bound demon is the visit. Unbound demons are not visited. */
    fun visit(p: Progress, id: String, today: Long): Progress {
        val record = p.record(id)
        if (!record.bound) return p
        return p.copy(demons = p.demons + (id to record.copy(lastVisitDay = today)), lastViewed = id)
    }

    fun daysSinceVisit(r: DemonRecord, today: Long): Long =
        if (r.bound && r.lastVisitDay != null) maxOf(0L, today - r.lastVisitDay) else 0L

    fun isRestless(r: DemonRecord, today: Long): Boolean = daysSinceVisit(r, today) >= RESTLESS_AFTER_DAYS

    /** Neglected demons escape: unbound again, lore and flawless kept, banner queued. Same instance if nothing changes. */
    fun evaluate(p: Progress, today: Long): Progress {
        val escaped = p.demons.filterValues { it.bound && daysSinceVisit(it, today) >= ESCAPE_AFTER_DAYS }
        if (escaped.isEmpty()) return p
        return p.copy(
            demons = p.demons + escaped.mapValues { (_, r) ->
                r.copy(bound = false, lastVisitDay = null, escapeBannerPending = true, revealPending = RevealPending())
            },
        )
    }

    /** Demons bound in [before] that [after] shows escaped (unbound with the banner queued). */
    fun newlyEscaped(before: Progress, after: Progress): List<String> =
        after.demons.filter { (id, r) -> !r.bound && r.escapeBannerPending && before.record(id).bound }.keys.sorted()

    fun consumeReveal(p: Progress, id: String, what: Reveal): Progress {
        val record = p.record(id)
        val pending = when (what) {
            Reveal.Avatar -> record.revealPending.copy(avatar = false)
            Reveal.Codex -> record.revealPending.copy(codex = false)
            Reveal.Battle -> record.revealPending.copy(battle = false)
        }
        return p.copy(demons = p.demons + (id to record.copy(revealPending = pending)))
    }

    /** Revealed lore the player has not seen ink in yet: the Grimoire's dossier button glows (ADR-0033). */
    fun dossierUnread(record: DemonRecord): Boolean =
        record.revealed && (record.revealPending.codex || record.revealPending.battle)


    /**
     * The "Psst" bubble shows (ADR-0036, amended 2026-10-23): bound and his new lore read, from the
     * summon day on. [today] is kept for the rule's callers and future day-based variants.
     */
    @Suppress("UNUSED_PARAMETER")
    fun teaserEligible(record: DemonRecord, today: Long): Boolean =
        record.bound && !dossierUnread(record)

    /** Taps that burst the "Psst" bubble (ADR-0036, amended). */
    const val TEASER_TAPS = 3

    /** The third tap burst the bubble: the day is kept, so later bursts that day count as replays. */
    fun openTeaser(p: Progress, id: String, day: Long): Progress =
        p.copy(demons = p.demons + (id to p.record(id).copy(teaserOpenedDay = day)))
    /**
     * No summon was ever committed: the intro's Summon glows (ADR-0033). A revealed demon also
     * counts as summoned, since a reset of the daily limit clears [Progress.lastSummonDay].
     */
    fun neverSummoned(p: Progress): Boolean = p.lastSummonDay == null && p.demons.values.none { it.revealed }

    fun dismissEscapeBanners(p: Progress): Progress =
        p.copy(demons = p.demons.mapValues { (_, r) -> r.copy(escapeBannerPending = false) })

    /** Debug Unsummon: forget everything about this demon. */
    fun unsummon(p: Progress, id: String): Progress = p.copy(demons = p.demons + (id to DemonRecord()))

    /**
     * Where Home opens the Grimoire: the first restless demon; else, while today's summon is
     * available, the first unbound one; else the last viewed if still valid; else the first.
     */
    fun focusDemon(p: Progress, ids: List<String>, today: Long): String {
        ids.firstOrNull { isRestless(p.record(it), today) }?.let { return it }
        if (canSummon(p, today)) ids.firstOrNull { !p.record(it).bound }?.let { return it }
        p.lastViewed?.takeIf { it in ids }?.let { return it }
        return ids.first()
    }
}
