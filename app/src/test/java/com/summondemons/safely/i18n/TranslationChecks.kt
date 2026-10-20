package com.summondemons.safely.i18n

import java.awt.Font
import java.io.File

/** A translation longer than its English source. */
data class Overrun(val key: String, val length: Int, val english: Int)

/** A reviewed exception to the length rule (`length-exceptions.txt`). */
data class LengthException(val locale: String, val key: String, val reason: String)

private val PLACEHOLDER = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")

private fun ResourceTexts.translatableKeys(): Set<String> = keys - untranslatable

/** Length in characters as a reader sees them (code points). */
private fun String.cpLength(): Int = codePointCount(0, length)

/** Every translatable English key exists, nothing extra does, arrays match in size, plurals have their forms. */
fun completeness(en: ResourceTexts, tr: ResourceTexts, requiredQuantities: Set<String>): List<String> {
    val expected = en.translatableKeys()
    val problems = mutableListOf<String>()
    (expected - tr.keys).sorted().forEach { problems += "missing: $it" }
    (tr.keys - expected).sorted().forEach { problems += "extra: $it" }
    for ((key, items) in en.arrays) {
        val translated = tr.arrays[key] ?: continue
        if (translated.size != items.size) problems += "$key: ${translated.size} items, English has ${items.size}"
    }
    for (key in en.plurals.keys) {
        val forms = tr.plurals[key] ?: continue
        (requiredQuantities - forms.keys).sorted().forEach { problems += "$key: missing quantity $it" }
    }
    return problems
}

private fun placeholdersOf(text: String): List<String> = PLACEHOLDER.findAll(text).map { it.value }.sorted().toList()

/** Each translated text uses the same format specifiers as its English source. */
fun placeholders(en: ResourceTexts, tr: ResourceTexts): List<String> {
    val problems = mutableListOf<String>()
    fun compare(key: String, english: String, translated: String) {
        val want = placeholdersOf(english)
        val got = placeholdersOf(translated)
        if (want != got) problems += "$key: placeholders $got, English has $want"
    }
    for ((key, text) in tr.strings) en.strings[key]?.let { compare(key, it, text) }
    for ((key, items) in tr.arrays) en.arrays[key]?.zip(items)?.forEach { (e, t) -> compare(key, e, t) }
    for ((key, forms) in tr.plurals) {
        val english = en.plurals[key]?.get("other") ?: continue
        forms.values.forEach { compare(key, english, it) }
    }
    return problems.distinct()
}

/** Texts longer than English: strings 1:1, array items by position, plural forms vs English's longest form. */
fun overLength(en: ResourceTexts, tr: ResourceTexts): List<Overrun> {
    val over = mutableListOf<Overrun>()
    for ((key, text) in tr.strings) {
        val english = en.strings[key] ?: continue
        if (text.cpLength() > english.cpLength()) over += Overrun(key, text.cpLength(), english.cpLength())
    }
    for ((key, items) in tr.arrays) {
        val english = en.arrays[key] ?: continue
        items.zip(english).forEachIndexed { i, (t, e) ->
            if (t.cpLength() > e.cpLength()) over += Overrun("$key[$i]", t.cpLength(), e.cpLength())
        }
    }
    for ((key, forms) in tr.plurals) {
        val longest = en.plurals[key]?.values?.maxOf { it.cpLength() } ?: continue
        forms.forEach { (quantity, t) ->
            if (t.cpLength() > longest) over += Overrun("$key#$quantity", t.cpLength(), longest)
        }
    }
    return over.sortedBy { it.key }
}

/**
 * Overruns must be listed exceptions; listed exceptions must still be needed and have a heading
 * `### <locale> · <key>` in docs/i18n/review.md.
 */
fun exceptionProblems(locale: String, over: List<Overrun>, exceptions: List<LengthException>, reviewText: String): List<String> {
    val listed = exceptions.filter { it.locale == locale }.associateBy { it.key }
    val problems = mutableListOf<String>()
    for (o in over) if (o.key !in listed) problems += "${o.key}: ${o.length} characters, English has ${o.english}"
    for ((key, _) in listed) {
        if (over.none { it.key == key }) problems += "$key: exception no longer needed"
        else if ("### $locale · $key" !in reviewText) problems += "$key: exception has no entry in docs/i18n/review.md"
    }
    return problems
}

fun loadExceptions(file: File): List<LengthException> =
    file.takeIf { it.isFile }?.readLines().orEmpty()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val parts = line.split(Regex("\\s+"), limit = 3)
            require(parts.size == 3) { "length-exceptions.txt: expected '<locale> <key> <reason>', got '$line'" }
            LengthException(parts[0], parts[1], parts[2])
        }

/** Letters in [text] that some font in [fonts] cannot draw. Punctuation and symbols are not checked. */
fun missingGlyphs(text: String, fonts: List<File>): List<String> {
    System.setProperty("java.awt.headless", "true")
    val loaded = fonts.map { it.name to Font.createFont(Font.TRUETYPE_FONT, it) }
    val letters = text.codePoints().toArray().filter { Character.isLetter(it) }.distinct()
    return loaded.flatMap { (name, font) ->
        letters.filterNot { font.canDisplay(it) }.map { "$name lacks '${String(Character.toChars(it))}'" }
    }
}
