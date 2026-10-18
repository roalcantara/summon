package com.summondemons.safely.ritual

/**
 * One expected vowel of a chant text (ADR-0022). [vowel] is null when the syllable has no vowel
 * letter. [neutral]: the unstressed last syllable of a word of two or more syllables, which may
 * also sound as a neutral "uh". [optional]: a glide (I or U right before another vowel in the same
 * word) that may be skipped cheaply. [word] counts the words of the phrase from 0.
 */
data class VowelSlot(
    val vowel: Vowel?,
    val neutral: Boolean = false,
    val optional: Boolean = false,
    val word: Int = 0,
)

/** The expected vowel slots of a chant text, one per syllable of [LatinSyllables] (ADR-0022). */
object ChantTemplate {
    private const val VOWEL_LETTERS = "AEIOUY"
    private val DIPHTHONGS = setOf("AE", "OE", "AU", "EI", "EU")

    fun slots(text: String): List<VowelSlot> {
        val upper = text.uppercase()
        val spans = LatinSyllables.spans(text)
        val vowels = ChantJudge.expectedVowels(text)
        val wordSizes = LatinSyllables.wordSizes(text)
        val wordOf = wordSizes.flatMapIndexed { w, n -> List(n) { w } }
        return spans.mapIndexed { i, span ->
            val word = wordOf[i]
            val lastOfWord = i == spans.lastIndex || wordOf[i + 1] != word
            VowelSlot(
                vowel = vowels[i],
                neutral = lastOfWord && wordSizes[word] >= 2,
                optional = isGlide(upper, span),
                word = word,
            )
        }
    }

    private fun isGlide(text: String, span: SyllableSpan): Boolean {
        val p = (span.start until span.end).firstOrNull { isNucleus(text, it) } ?: return false
        if (text[p] != 'I' && text[p] != 'U') return false
        if (p + 1 >= text.length) return false
        if (text.substring(p, p + 2) in DIPHTHONGS) return false
        return text[p + 1] in VOWEL_LETTERS
    }

    private fun isNucleus(text: String, i: Int): Boolean =
        text[i] in VOWEL_LETTERS && !(text[i] == 'U' && i > 0 && text[i - 1] == 'Q')
}
