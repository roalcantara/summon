package com.summondemons.safely.ritual

/** Character range [start, end) of one syllable in the original text. */
data class SyllableSpan(val start: Int, val end: Int)

/**
 * Approximate Latin syllables for chant pacing and karaoke (ADR-0008). Not real
 * scansion: each diphthong (AE, OE, AU, EI, EU) or other single vowel is one nucleus;
 * a U after Q is a consonant; consonants join the following nucleus and the last
 * nucleus takes any trailing consonants.
 */
object LatinSyllables {
    private const val VOWELS = "AEIOUY"
    private val DIPHTHONGS = setOf("AE", "OE", "AU", "EI", "EU")
    private val WORD = Regex("[A-Za-z]+")

    fun count(text: String): Int = spans(text).size

    /** Syllables of each word of [text], in order; they sum to [count] (ADR-0019 word hints). */
    fun wordSizes(text: String): List<Int> = WORD.findAll(text).map { wordSpans(it.value.uppercase(), 0).size }.toList()

    fun spans(text: String): List<SyllableSpan> =
        WORD.findAll(text).flatMap { wordSpans(it.value.uppercase(), it.range.first) }.toList()

    private fun wordSpans(word: String, offset: Int): List<SyllableSpan> {
        val nucleusEnds = mutableListOf<Int>() // exclusive end of each nucleus
        var i = 0
        while (i < word.length) {
            if (isVowel(word, i)) {
                val diphthong = i + 1 < word.length && word.substring(i, i + 2) in DIPHTHONGS
                i += if (diphthong) 2 else 1
                nucleusEnds += i
            } else {
                i++
            }
        }
        if (nucleusEnds.isEmpty()) return listOf(SyllableSpan(offset, offset + word.length))
        var start = 0
        return nucleusEnds.mapIndexed { k, nucleusEnd ->
            val end = if (k == nucleusEnds.lastIndex) word.length else nucleusEnd
            SyllableSpan(offset + start, offset + end).also { start = end }
        }
    }

    private fun isVowel(word: String, i: Int): Boolean =
        word[i] in VOWELS && !(word[i] == 'U' && i > 0 && word[i - 1] == 'Q')
}
