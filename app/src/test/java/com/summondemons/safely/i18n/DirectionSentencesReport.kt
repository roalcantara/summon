package com.summondemons.safely.i18n

import org.junit.Test

/**
 * Not a check: prints every sentence that takes a compass point, with all 8 points, per language,
 * so a reviewer can read them (i18n spec §6). Run:
 * ./gradlew testDebugUnitTest --tests '*DirectionSentencesReport*' -i | grep '^DIR'
 */
class DirectionSentencesReport {

    private val keys = listOf("chant_face", "chant_a11y_paused", "chant_a11y_facing", "chant_a11y_turn_left", "chant_a11y_turn_right")

    @Test
    fun print() {
        val locales = mapOf("en" to null) + SHIPPED_LOCALES
        for ((locale, qualifier) in locales) {
            val texts = ResourceTexts.load(valuesDir(qualifier))
            val points = texts.arrays.getValue("compass_points")
            for (key in keys) points.forEach { point -> println("DIR $locale $key: ${texts.strings.getValue(key).replace("%1\$s", point)}") }
            points.forEach { point ->
                val direction = texts.strings.getValue("badge_direction_text").replace("%1\$s", point).replace("%2\$s", "…")
                println("DIR $locale badge_direction_text: $direction")
            }
        }
    }
}
