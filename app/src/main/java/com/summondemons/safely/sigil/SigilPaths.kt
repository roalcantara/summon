package com.summondemons.safely.sigil

/** One stroke of a sigil: its ink group (1..4) and SVG path data (ADR-0009). */
data class SigilStroke(val group: Int, val pathData: String)

/** Raw sigil format and ink math. Pure: no Android or Compose types. */
object SigilPaths {
    const val GROUPS = 4

    /** Lines are "<group> <path-data>"; blank lines are skipped; draw order is kept within a group. */
    fun parse(lines: List<String>): List<SigilStroke> = lines
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val space = line.indexOf(' ')
            require(space > 0) { "bad sigil line: $line" }
            val group = line.substring(0, space).toIntOrNull()
            require(group != null && group in 1..GROUPS) { "bad sigil group: $line" }
            SigilStroke(group, line.substring(space + 1).trim())
        }
        .sortedBy { it.group }

    /**
     * How much of each stroke (0..1) is inked at [inkProgress]. Group g inks over
     * [(g-1)/4, g/4]; inside a group, strokes ink one after another in proportion to [lengths].
     */
    fun inkFractions(strokes: List<SigilStroke>, lengths: List<Float>, inkProgress: Float): List<Float> {
        require(strokes.size == lengths.size) { "one length per stroke" }
        val groupProgress = FloatArray(GROUPS + 1) { g -> (inkProgress * GROUPS - (g - 1)).coerceIn(0f, 1f) }
        val budget = FloatArray(GROUPS + 1)
        strokes.forEachIndexed { i, stroke -> budget[stroke.group] += lengths[i] }
        for (g in 1..GROUPS) budget[g] *= groupProgress[g]
        val used = FloatArray(GROUPS + 1)
        return strokes.mapIndexed { i, stroke ->
            val length = lengths[i]
            val before = used[stroke.group]
            used[stroke.group] = before + length
            if (length <= 0f) {
                if (groupProgress[stroke.group] >= 1f) 1f else 0f
            } else {
                ((budget[stroke.group] - before) / length).coerceIn(0f, 1f)
            }
        }
    }
}
