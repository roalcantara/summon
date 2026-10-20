package com.summondemons.safely.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Locale → `values-` qualifier. Each translation task adds its locale here first. */
val SHIPPED_LOCALES: Map<String, String> = mapOf("pt-BR" to "pt-rBR")

/** Plural forms each language must provide (CLDR); English needs one/other. */
private val QUANTITIES = mapOf(
    "es" to setOf("one", "many", "other"),
    "pt-BR" to setOf("one", "many", "other"),
    "it" to setOf("one", "many", "other"),
    "fr" to setOf("one", "many", "other"),
)

class TranslationResourcesTest {

    private val exceptions = loadExceptions(File("src/test/resources/i18n/length-exceptions.txt"))
    private val review = File("../docs/i18n/review.md").takeIf { it.isFile }?.readText().orEmpty()
    private val fonts = File("src/main/res/font").listFiles()!!.filter { it.extension == "ttf" }

    private fun each(check: (locale: String, tr: ResourceTexts) -> List<String>) {
        val problems = SHIPPED_LOCALES.flatMap { (locale, qualifier) ->
            val dir = valuesDir(qualifier)
            assertTrue("$locale: missing folder $dir", dir.isDirectory)
            check(locale, ResourceTexts.load(dir)).map { "$locale $it" }
        }
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test fun complete() = each { locale, tr -> completeness(english, tr, QUANTITIES.getValue(locale)) }

    @Test fun placeholdersMatch() = each { _, tr -> placeholders(english, tr) }

    @Test fun noLongerThanEnglish() = each { locale, tr -> exceptionProblems(locale, overLength(english, tr), exceptions, review) }

    @Test fun exceptionsAreForShippedLocales() {
        assertEquals(emptyList<LengthException>(), exceptions.filter { it.locale !in SHIPPED_LOCALES })
    }

    @Test fun fontsDrawEveryLetter() = each { _, tr ->
        missingGlyphs((tr.strings.values + tr.arrays.values.flatten() + tr.plurals.values.flatMap { it.values }).joinToString(" "), fonts)
    }
}
