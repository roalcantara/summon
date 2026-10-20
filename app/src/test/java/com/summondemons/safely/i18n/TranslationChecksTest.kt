package com.summondemons.safely.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TranslationChecksTest {

    private val en = ResourceTexts.load(File("src/test/resources/i18n/fixtures/values"))
    private val xx = ResourceTexts.load(File("src/test/resources/i18n/fixtures/values-xx"))

    @Test
    fun completenessFindsMissingExtraShortArraysAndPluralForms() {
        val problems = completeness(en, xx, requiredQuantities = setOf("one", "many", "other"))
        assertTrue(problems.any { "missing: quoted" in it })
        assertTrue(problems.any { "extra: extra" in it })
        assertTrue(problems.any { "list: 1 items, English has 2" in it })
        assertTrue(problems.any { "count: missing quantity many" in it })
        assertTrue(problems.none { "fixed" in it }) // untranslatable keys are not expected
    }

    @Test
    fun placeholdersMustMatchEnglish() {
        assertEquals(listOf("escaped: placeholders [%1\$d], English has [%1\$s]"), placeholders(en, xx))
    }

    @Test
    fun lengthFindsTextLongerThanEnglish() {
        val over = overLength(en, xx)
        assertEquals(listOf(Overrun("plain", 21, 5)), over)
    }

    @Test
    fun exceptionsMustStillBeNeededAndReviewed() {
        val exceptions = listOf(LengthException("xx", "plain", "wraps"), LengthException("xx", "escaped", "stale"))
        val problems = exceptionProblems("xx", overLength(en, xx), exceptions, reviewText = "### xx · plain\n")
        assertEquals(listOf("escaped: exception no longer needed"), problems)
        val unreviewed = exceptionProblems("xx", overLength(en, xx), exceptions.take(1), reviewText = "")
        assertEquals(listOf("plain: exception has no entry in docs/i18n/review.md"), unreviewed)
    }

    @Test
    fun overrunsWithoutExceptionAreReported() {
        val problems = exceptionProblems("xx", overLength(en, xx), emptyList(), reviewText = "")
        assertEquals(listOf("plain: 21 characters, English has 5"), problems)
    }

    @Test
    fun glyphsFindsLettersMissingFromAFont() {
        val fonts = File("src/main/res/font").listFiles()!!.filter { it.extension == "ttf" }
        assertEquals(emptyList<String>(), missingGlyphs("Ação élan ñ", fonts))
        // The bundled fonts are Latin-1 subsets: no œ, Œ or Ÿ (docs/i18n/review.md, "fr · fonts")
        assertTrue(missingGlyphs("œ", fonts).isNotEmpty())
        assertTrue(missingGlyphs("漢", fonts).isNotEmpty())
    }
}
