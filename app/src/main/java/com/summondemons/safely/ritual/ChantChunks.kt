package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.CHUNK_MAX_SYLLABLES
import com.summondemons.safely.ritual.RitualConfig.CHUNK_MIN_SYLLABLES
import kotlin.math.ceil

/**
 * A group of words judged together (ADR-0022): [text] (letters only, the shuffle seed), its word
 * and syllable index ranges in the phrase, and its expected vowel slots.
 */
data class ChantChunk(val text: String, val words: IntRange, val syllables: IntRange, val slots: List<VowelSlot>)

object ChantChunks {
    private val WORD = Regex("[A-Za-z]+")

    fun of(phrase: String): List<ChantChunk> {
        val sizes = LatinSyllables.wordSizes(phrase)
        val words = WORD.findAll(phrase).map { it.value.uppercase() }.toList()
        val slots = ChantTemplate.slots(phrase)
        var syllable = 0
        return split(sizes).map { range ->
            val count = range.sumOf { sizes[it] }
            val chunk = ChantChunk(
                text = words.slice(range).joinToString(" "),
                words = range,
                syllables = syllable until syllable + count,
                slots = slots.subList(syllable, syllable + count),
            )
            syllable += count
            chunk
        }
    }

    /**
     * Word-index ranges: the whole phrase when it has at most [CHUNK_MAX_SYLLABLES] syllables;
     * otherwise groups of at most that many, as even as possible, never under [CHUNK_MIN_SYLLABLES].
     */
    fun split(sizes: List<Int>): List<IntRange> {
        val total = sizes.sum()
        if (total <= CHUNK_MAX_SYLLABLES) return listOf(sizes.indices)
        val groups = ceil(total / CHUNK_MAX_SYLLABLES.toDouble()).toInt()
        val target = total.toDouble() / groups
        val out = mutableListOf<IntRange>()
        var start = 0
        var acc = 0
        var done = 0
        for (i in sizes.indices) {
            acc += sizes[i]
            val left = total - done - acc
            val groupsLeft = groups - out.size - 1
            val canEnd = groupsLeft > 0 && i < sizes.lastIndex && acc >= CHUNK_MIN_SYLLABLES && left >= CHUNK_MIN_SYLLABLES
            if (canEnd && (acc >= target || acc + sizes[i + 1] > CHUNK_MAX_SYLLABLES)) {
                out += start..i
                done += acc
                start = i + 1
                acc = 0
            }
        }
        out += start..sizes.lastIndex
        return out
    }
}
