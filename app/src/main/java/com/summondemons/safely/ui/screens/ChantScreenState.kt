package com.summondemons.safely.ui.screens

import com.summondemons.safely.ritual.ChantJudge
import com.summondemons.safely.ritual.ChantVerdict
import com.summondemons.safely.ritual.LatinSyllables
import com.summondemons.safely.ritual.RitualConfig.PHRASE_COUNT
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualState
import com.summondemons.safely.ritual.TurnDirection
import com.summondemons.safely.ritual.WordMark
import com.summondemons.safely.ritual.turnDirection
import kotlin.math.ceil

/** How a word is drawn on the chant screen (ADR-0019): its underline and colour. */
enum class WordLook { Pending, Current, Heard, Doubtful, Rejected }

/** One word: its syllables (trailing punctuation on the last), how many are lit, and its look. */
data class ChantWord(val syllables: List<String>, val lit: Int, val look: WordLook)

/** The one line under the words (ADR-0019). */
enum class VerdictLine { ChantAloud, Paused, Sealed, ChantAgain, SealCracked }

/** Everything the chant screen draws, derived from the ritual (pure; ADR-0019). */
data class ChantScreenState(
    val words: List<ChantWord>,
    val facing: Boolean,
    /** Which way to turn while not facing the demon; null when facing or unknown. */
    val turn: TurnDirection?,
    val verdict: VerdictLine,
    /** Phrase shown as "{I..IV} of IV", 0-based. */
    val numeral: Int,
    /** The sigil ring's arc: focus left, or 1 on phrase I (no clock). */
    val clockFraction: Float,
    /** Whole seconds left, or null on phrase I. */
    val clockSeconds: Int?,
    /** The clock waits while a fresh verdict is on screen. */
    val clockHeld: Boolean,
)

/** Splits a chant phrase into words of syllables for display, matching [LatinSyllables]. */
fun chantWords(text: String): List<List<String>> = text.split(' ').filter { it.isNotBlank() }.map { token ->
    val spans = LatinSyllables.spans(token)
    if (spans.isEmpty()) return@map listOf(token)
    spans.mapIndexed { i, span ->
        val end = if (i == spans.lastIndex) token.length else span.end
        token.substring(if (i == 0) 0 else span.start, end)
    }
}

fun chantScreenState(ritual: RitualState, phrases: List<String>, azimuth: Float?, target: Float): ChantScreenState {
    val phrase = ritual.phrase.coerceAtMost(PHRASE_COUNT - 1)
    val verdict = ritual.verdict?.takeIf { ritual.phrase < PHRASE_COUNT }
    val text = chantWords(phrases[phrase])
    val words = when {
        ritual.phrase >= PHRASE_COUNT -> text.map { ChantWord(it, it.size, WordLook.Heard) }
        verdict?.kind == ChantVerdict.Kind.Rejected -> text.mapIndexed { i, syllables ->
            val look = if (verdict.marks.getOrNull(i) == WordMark.Doubtful) WordLook.Rejected else WordLook.Heard
            ChantWord(syllables, 0, look)
        }
        else -> chantingWords(ritual, phrase, text)
    }
    val line = when {
        verdict?.kind == ChantVerdict.Kind.Rejected -> VerdictLine.ChantAgain
        verdict?.kind == ChantVerdict.Kind.Cracked -> VerdictLine.SealCracked
        verdict?.kind == ChantVerdict.Kind.Sealed -> VerdictLine.Sealed
        ritual.phase == RitualPhase.Wavering || ritual.phase == RitualPhase.Seeking -> VerdictLine.Paused
        else -> VerdictLine.ChantAloud
    }
    val clock = ritual.phrase in 1 until PHRASE_COUNT
    return ChantScreenState(
        words = words,
        facing = ritual.aligned,
        turn = turnDirection(azimuth, target),
        verdict = line,
        numeral = if (verdict?.kind == ChantVerdict.Kind.Sealed) verdict.phrase else phrase,
        clockFraction = if (clock) ritual.focusFraction else 1f,
        clockSeconds = if (clock) ceil(ritual.focusMs / 1_000.0).toInt() else null,
        clockHeld = clock && verdict?.holdsClock == true,
    )
}

/** While chanting: lit syllables, the current word, and hints for words already heard. */
private fun chantingWords(ritual: RitualState, phrase: Int, text: List<List<String>>): List<ChantWord> {
    val marks = ritual.vowels?.let { vowels ->
        ritual.wordSizes?.let { ChantJudge.wordMarks(vowels[phrase], ritual.take.nuclei, it[phrase]) }
    }
    var start = 0
    return text.mapIndexed { i, syllables ->
        val lit = (ritual.lit - start).coerceIn(0, syllables.size)
        val current = ritual.lit in start until start + syllables.size
        start += syllables.size
        val look = when {
            current -> WordLook.Current
            marks?.getOrNull(i) == WordMark.Heard -> WordLook.Heard
            marks?.getOrNull(i) == WordMark.Doubtful -> WordLook.Doubtful
            else -> WordLook.Pending
        }
        ChantWord(syllables, lit, look)
    }
}
