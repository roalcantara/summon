package com.summondemons.safely.i18n

import com.summondemons.safely.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResourceTextsTest {

    private val fixture = ResourceTexts.load(File("src/test/resources/i18n/fixtures/values"))

    @Test
    fun readsStringsArraysAndPlurals() {
        assertEquals("Hello", fixture.strings["plain"])
        assertEquals(listOf("One", "Two's"), fixture.arrays["list"])
        assertEquals(mapOf("one" to "%1\$d legion", "other" to "%1\$d legions"), fixture.plurals["count"])
    }

    @Test
    fun unescapesAndroidText() {
        assertEquals("Can't stop · “%1\$s”", fixture.strings["escaped"])
        assertEquals(" & ", fixture.strings["quoted"])
    }

    @Test
    fun remembersUntranslatableKeys() {
        assertTrue("fixed" in fixture.untranslatable)
        assertTrue("plain" !in fixture.untranslatable)
    }

    @Test
    fun mapsResourceIdsToNamesAndEnglishText() {
        assertEquals("app_name", resName(R.string.app_name))
        assertEquals("Summon Demons Safely", en(R.string.app_name))
    }
}
