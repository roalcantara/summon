package com.summondemons.safely.ritual

import com.summondemons.safely.ritual.RitualConfig.LINE_MATCH_MAX
import com.summondemons.safely.ritual.RitualConfig.LOOP_MAX_WORDS
import com.summondemons.safely.ritual.RitualConfig.NO_SPEECH_MAX
import com.summondemons.safely.ritual.RitualConfig.WORD_COVER_MIN
import java.text.Normalizer

/** One piece of a Whisper transcript and how likely Whisper thinks it was silence. */
data class TranscriptSegment(val text: String, val noSpeech: Float = 0f)

/** How a transcript compares with a chant line (ADR-0027). */
data class LineMatch(
    /** Edit distance of the line to the best-matching stretch of the transcript, / the line's letters. */
    val distance: Double,
    val found: Boolean,
    /** Word indices of the line covered by the best stretch. */
    val covered: Set<Int>,
    /** The transcript's letters after filtering and loop collapse. */
    val heardLetters: String,
)

/** Pure transcript scoring for the chant check (ADR-0026, ADR-0027). */
object LineMatcher {
    private val NON_LETTERS = Regex("[^\\p{L}]+")
    private val MARKS = Regex("\\p{Mn}+")

    /** Letters only, upper case, accents removed, J→I and V→U (Latin spelling varies). */
    fun letters(text: String): String {
        val plain = MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").uppercase()
        return buildString {
            for (c in plain) {
                when (c) {
                    'J' -> append('I')
                    'V' -> append('U')
                    in 'A'..'Z' -> append(c)
                }
            }
        }
    }

    fun match(line: String, segments: List<TranscriptSegment>): LineMatch {
        val text = segments.filter { it.noSpeech <= NO_SPEECH_MAX }.joinToString(" ") { it.text }
        val heard = collapseLoops(words(text)).joinToString("")
        val lineWords = words(line)
        val expected = lineWords.joinToString("")
        if (expected.isEmpty()) return LineMatch(0.0, true, lineWords.indices.toSet(), heard)
        val (cost, matched) = bestStretch(expected, heard)
        val distance = cost.toDouble() / expected.length
        val covered = mutableSetOf<Int>()
        var pos = 0
        lineWords.forEachIndexed { w, word ->
            val hits = (pos until pos + word.length).count { matched[it] }
            if (hits >= WORD_COVER_MIN * word.length) covered += w
            pos += word.length
        }
        return LineMatch(distance, distance <= LINE_MATCH_MAX, covered, heard)
    }

    /** A sequence of 1..[LOOP_MAX_WORDS] words repeated three or more times in a row is kept once. */
    fun collapseLoops(words: List<String>): List<String> {
        val out = words.toMutableList()
        var n = 1
        while (n <= LOOP_MAX_WORDS) {
            var i = 0
            var changed = false
            while (i + 3 * n <= out.size) {
                val unit = out.subList(i, i + n).toList()
                if (unit == out.subList(i + n, i + 2 * n) && unit == out.subList(i + 2 * n, i + 3 * n)) {
                    var end = i + 3 * n
                    while (end + n <= out.size && out.subList(end, end + n) == unit) end += n
                    repeat(end - (i + n)) { out.removeAt(i + n) }
                    changed = true
                } else {
                    i++
                }
            }
            n = if (changed) 1 else n + 1
        }
        return out
    }

    private fun words(text: String): List<String> = text.split(NON_LETTERS).map(::letters).filter { it.isNotEmpty() }

    /**
     * Semi-global edit distance: [expected] fully, against any stretch of [heard] (free start and end).
     * Returns the cost and, per expected letter, whether it aligned to an equal heard letter.
     */
    private fun bestStretch(expected: String, heard: String): Pair<Int, BooleanArray> {
        val m = expected.length
        val n = heard.length
        val d = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) d[i][0] = i
        for (i in 1..m) {
            for (j in 1..n) {
                val sub = d[i - 1][j - 1] + if (expected[i - 1] == heard[j - 1]) 0 else 1
                d[i][j] = minOf(sub, d[i - 1][j] + 1, d[i][j - 1] + 1)
            }
        }
        var j = (0..n).minBy { d[m][it] }
        val cost = d[m][j]
        val matched = BooleanArray(m)
        var i = m
        while (i > 0 && j > 0) {
            val same = expected[i - 1] == heard[j - 1]
            when {
                d[i][j] == d[i - 1][j - 1] + (if (same) 0 else 1) -> { matched[i - 1] = same; i--; j-- }
                d[i][j] == d[i - 1][j] + 1 -> i--
                else -> j--
            }
        }
        return cost to matched
    }
}
