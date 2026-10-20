package com.summondemons.safely.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishTextTest {

    @Test
    fun countedStringsArePlurals() {
        assertEquals(
            mapOf("one" to "%1\$d of %2\$d spirit bound", "other" to "%1\$d of %2\$d spirits bound"),
            english.plurals["home_bound"],
        )
        assertEquals(
            mapOf("one" to "Circle cooling. Next summon in %1\$d hour.", "other" to "Circle cooling. Next summon in %1\$d hours."),
            english.plurals["cd_cooling"],
        )
        assertEquals(mapOf("one" to "%1\$d legion", "other" to "%1\$d legions"), english.plurals["badge_legions_label"])
        assertEquals(
            mapOf(
                "one" to "A %1\$s at the head of %2\$d legion of spirits.",
                "other" to "A %1\$s at the head of %2\$d legions of spirits.",
            ),
            english.plurals["badge_legions_text"],
        )
        assertEquals(
            mapOf("one" to "%1\$d of 4 lines sealed", "other" to "%1\$d of 4 lines sealed"),
            english.plurals["chant_a11y_seals"],
        )
    }

    @Test
    fun chantScoreIsAString() {
        assertEquals("%1\$d%%", english.strings["chant_score"]) // formats to "85%"
    }
}
