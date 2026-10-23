package com.summondemons.safely.progress

import com.summondemons.safely.progress.ProgressRules.canSummon
import com.summondemons.safely.progress.ProgressRules.commitSummon
import com.summondemons.safely.progress.ProgressRules.consumeReveal
import com.summondemons.safely.progress.ProgressRules.daysSinceVisit
import com.summondemons.safely.progress.ProgressRules.dismissEscapeBanners
import com.summondemons.safely.progress.ProgressRules.evaluate
import com.summondemons.safely.progress.ProgressRules.focusDemon
import com.summondemons.safely.progress.ProgressRules.isRestless
import com.summondemons.safely.progress.ProgressRules.unsummon
import com.summondemons.safely.progress.ProgressRules.visit
import com.summondemons.safely.progress.ProgressRules.dossierUnread
import com.summondemons.safely.progress.ProgressRules.neverSummoned
import com.summondemons.safely.progress.ProgressRules.newlyEscaped
import com.summondemons.safely.progress.ProgressRules.openTeaser
import com.summondemons.safely.progress.ProgressRules.teaserEligible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {

    private val ids = listOf("asmodeus", "astaroth")

    // --- Daily summon ---

    @Test
    fun oneSummonPerLocalDayAndNeverUnlocksOnAClockMovedBack() {
        var p = Progress()
        assertTrue(canSummon(p, 100))
        p = commitSummon(p, "asmodeus", 100, flawless = false)
        assertFalse(canSummon(p, 100))
        assertFalse(canSummon(p, 99))
        assertTrue(canSummon(p, 101))
    }

    @Test
    fun commitSetsEveryField() {
        val p = commitSummon(Progress(), "astaroth", 50, flawless = true)
        assertEquals(
            DemonRecord(
                bound = true,
                revealed = true,
                flawless = true,
                lastVisitDay = 50,
                revealPending = RevealPending.All,
                escapeBannerPending = false,
                boundDay = 50,
            ),
            p.record("astaroth"),
        )
        assertEquals(50L, p.lastSummonDay)
        assertEquals("astaroth", p.lastViewed)
    }

    @Test
    fun commitNeverMovesTheSummonDayBack() {
        val p = commitSummon(Progress(lastSummonDay = 10), "asmodeus", 9, flawless = false)
        assertEquals(10L, p.lastSummonDay)
    }

    @Test
    fun commitClearsAPendingEscapeBanner() {
        val escaped = evaluate(commitSummon(Progress(), "asmodeus", 100, false), 103)
        assertTrue(escaped.record("asmodeus").escapeBannerPending)
        assertFalse(commitSummon(escaped, "asmodeus", 104, false).record("asmodeus").escapeBannerPending)
    }

    // --- Restless and escape ---

    @Test
    fun turnsRestlessAtTwoDaysAndEscapesAtThreeKeepingLoreAndFlawless() {
        var p = commitSummon(Progress(), "asmodeus", 100, flawless = true)
        assertFalse(isRestless(p.record("asmodeus"), 101))
        assertTrue(isRestless(p.record("asmodeus"), 102))
        assertTrue(evaluate(p, 102).record("asmodeus").bound)
        p = evaluate(p, 103)
        val r = p.record("asmodeus")
        assertFalse(r.bound)
        assertTrue(r.revealed)
        assertTrue(r.flawless)
        assertTrue(r.escapeBannerPending)
        assertEquals(null, r.lastVisitDay)
        assertEquals(RevealPending(), r.revealPending)
    }

    @Test
    fun evaluateWithNothingToDoReturnsTheSameProgress() {
        val p = commitSummon(Progress(), "asmodeus", 100, false)
        assertSame(p, evaluate(p, 101))
    }

    @Test
    fun aVisitResetsTheCount() {
        var p = commitSummon(Progress(), "asmodeus", 100, false)
        p = visit(p, "asmodeus", 102)
        assertFalse(isRestless(p.record("asmodeus"), 103))
        assertTrue(evaluate(p, 104).record("asmodeus").bound)
        assertEquals("asmodeus", p.lastViewed)
    }

    @Test
    fun visitingAnUnboundDemonChangesNothing() {
        val p = Progress(lastViewed = "asmodeus")
        assertSame(p, visit(p, "astaroth", 5))
    }

    @Test
    fun daysSinceVisitIsNeverNegative() {
        val p = commitSummon(Progress(), "asmodeus", 100, false)
        assertEquals(0L, daysSinceVisit(p.record("asmodeus"), 98))
        assertEquals(0L, daysSinceVisit(DemonRecord(), 500))
    }

    @Test
    fun aReSummonKeepsAnEarnedFlawless() {
        var p = commitSummon(Progress(), "asmodeus", 100, flawless = true)
        p = evaluate(p, 103)
        p = commitSummon(p, "asmodeus", 104, flawless = false)
        assertTrue(p.record("asmodeus").flawless)
    }

    // --- One-time flags ---

    @Test
    fun revealFlagsClearOneByOne() {
        var p = commitSummon(Progress(), "astaroth", 1, false)
        p = consumeReveal(p, "astaroth", Reveal.Codex)
        assertEquals(RevealPending(avatar = true, codex = false, battle = true), p.record("astaroth").revealPending)
        p = consumeReveal(p, "astaroth", Reveal.Avatar)
        p = consumeReveal(p, "astaroth", Reveal.Battle)
        assertEquals(RevealPending(), p.record("astaroth").revealPending)
    }

    @Test
    fun dismissingClearsEveryEscapeBanner() {
        var p = commitSummon(commitSummon(Progress(), "asmodeus", 100, false), "astaroth", 101, false)
        p = evaluate(p, 110)
        assertTrue(ids.all { p.record(it).escapeBannerPending })
        p = dismissEscapeBanners(p)
        assertTrue(ids.none { p.record(it).escapeBannerPending })
    }

    @Test
    fun unsummonForgetsTheDemon() {
        val p = unsummon(commitSummon(Progress(), "astaroth", 1, flawless = true), "astaroth")
        assertEquals(DemonRecord(), p.record("astaroth"))
    }

    // --- Focus demon ---

    @Test
    fun prefersRestlessThenUnboundWhileASummonIsAvailableThenLastViewed() {
        var p = Progress()
        assertEquals("asmodeus", focusDemon(p, ids, 10))
        p = commitSummon(p, "asmodeus", 10, false)
        assertEquals("asmodeus", focusDemon(p, ids, 10)) // day used → last viewed
        assertEquals("astaroth", focusDemon(p, ids, 11))
        assertEquals("asmodeus", focusDemon(p, ids, 12)) // restless wins
    }

    @Test
    fun anUnknownLastViewedFallsBackToTheFirstDemon() {
        val p = Progress(lastSummonDay = 10, lastViewed = "baal")
        assertEquals("asmodeus", focusDemon(p, ids, 10))
    }

    // --- Attention glow (ADR-0033) ---

    @Test
    fun aFreshSummonLeavesTheDossierUnreadUntilBothRevealsAreConsumed() {
        var p = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        assertTrue(dossierUnread(p.record("asmodeus")))
        p = consumeReveal(p, "asmodeus", Reveal.Codex)
        assertTrue(dossierUnread(p.record("asmodeus")))
        p = consumeReveal(p, "asmodeus", Reveal.Battle)
        assertFalse(dossierUnread(p.record("asmodeus")))
    }

    @Test
    fun anUnrevealedDemonHasNothingUnread() {
        assertFalse(dossierUnread(DemonRecord()))
        assertFalse(dossierUnread(DemonRecord(revealPending = RevealPending.All)))
    }

    @Test
    fun neverSummonedUntilTheFirstCommit() {
        assertTrue(neverSummoned(Progress()))
        assertFalse(neverSummoned(commitSummon(Progress(), "astaroth", 3, flawless = true)))
    }

    @Test
    fun aResetDailyLimitDoesNotMakeTheFirstSummonGlowAgain() {
        val summoned = commitSummon(Progress(), "astaroth", 3, flawless = false)
        assertFalse(neverSummoned(summoned.copy(lastSummonDay = null)))
    }

    @Test
    fun newlyEscapedListsOnlyDemonsThatJustSlippedTheirSeal() {
        val bound = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        val escaped = evaluate(bound, 13)
        assertEquals(listOf("asmodeus"), newlyEscaped(bound, escaped))
        assertEquals(emptyList<String>(), newlyEscaped(escaped, escaped))
        assertEquals(emptyList<String>(), newlyEscaped(Progress(), bound))
    }

    @Test
    fun commitRecordsTheBoundDay() {
        assertEquals(10L, commitSummon(Progress(), "asmodeus", 10, flawless = false).record("asmodeus").boundDay)
    }

    @Test
    fun theTeaserShowsOnceTheLoreIsReadFromTheSummonDayOn() {
        var p = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        assertFalse(teaserEligible(p.record("asmodeus"), today = 10)) // lore still unread
        p = consumeReveal(consumeReveal(p, "asmodeus", Reveal.Codex), "asmodeus", Reveal.Battle)
        assertTrue(teaserEligible(p.record("asmodeus"), today = 10)) // the summon day itself
        assertTrue(teaserEligible(p.record("asmodeus"), today = 11))
        assertFalse(teaserEligible(DemonRecord(), today = 11)) // unbound
        assertTrue(teaserEligible(DemonRecord(bound = true, revealed = true), today = 11)) // old save, no boundDay
    }

    @Test
    fun anOpenedTeaserRecordsTheDayAndStaysOpenToReplays() {
        val read = DemonRecord(bound = true, revealed = true, boundDay = 5)
        val p = openTeaser(Progress(demons = mapOf("asmodeus" to read)), "asmodeus", day = 11)
        assertEquals(11L, p.record("asmodeus").teaserOpenedDay)
        assertTrue(teaserEligible(p.record("asmodeus"), today = 11))
    }
}
