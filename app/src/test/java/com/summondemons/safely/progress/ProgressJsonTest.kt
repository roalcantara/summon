package com.summondemons.safely.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressJsonTest {

    private val full = Progress(
        demons = mapOf(
            "asmodeus" to DemonRecord(
                bound = true,
                revealed = true,
                flawless = true,
                lastVisitDay = 20_000L,
                revealPending = RevealPending(avatar = true, codex = false, battle = true),
                escapeBannerPending = false,
            ),
            "astaroth" to DemonRecord(revealed = true, escapeBannerPending = true),
        ),
        lastSummonDay = 20_001L,
        homeHintSeen = true,
        lastViewed = "astaroth",
    )

    @Test
    fun roundTripKeepsEveryField() {
        assertEquals(full, ProgressJson.decode(ProgressJson.encode(full)))
    }

    @Test
    fun emptyProgressRoundTrips() {
        assertEquals(Progress(), ProgressJson.decode(ProgressJson.encode(Progress())))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """{"demons":{"asmodeus":{"bound":true,"visitHintSeen":true}},"lastSummonDay":5,"stats":{"might":5}}"""
        val progress = ProgressJson.decode(json)
        assertTrue(progress.record("asmodeus").bound)
        assertEquals(5L, progress.lastSummonDay)
    }

    @Test
    fun corruptOrMissingJsonStartsFresh() {
        assertNull(ProgressJson.decodeOrNull("{nope"))
        assertNull(ProgressJson.decodeOrNull("""{"lastSummonDay":"soon"}"""))
        assertEquals(Progress(), ProgressJson.decode("{nope"))
        assertEquals(Progress(), ProgressJson.decode(null))
    }

    @Test
    fun aMissingRecordIsTheDefault() {
        assertEquals(DemonRecord(), Progress().record("baal"))
    }
}
