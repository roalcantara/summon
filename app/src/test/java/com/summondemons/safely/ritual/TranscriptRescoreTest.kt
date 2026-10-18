package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Re-scores every logged Whisper transcript (ADR-0027) at a range of thresholds: how many checks
 * of the line being chanted would pass, and how many would match a different line of either demon.
 * Sessions come from `tools/pull_sessions.sh` into `app/src/test/sessions/` (git-ignored).
 * Run: `./gradlew testDebugUnitTest --tests '*TranscriptRescoreTest*' -i`.
 */
class TranscriptRescoreTest {

    private val dir = File(System.getProperty("sessions") ?: "src/test/sessions")

    @Test
    fun rescoreLoggedTranscripts() {
        val checks = dir.listFiles { f -> f.extension == "jsonl" }.orEmpty().flatMap { file ->
            val (header, ticks) = SessionReplay.read(file)
            ticks.mapNotNull { t -> t.check?.let { header to it } }
        }
        assumeTrue("no logged Whisper checks in ${dir.absolutePath}", checks.isNotEmpty())
        val allLines = DemonCatalog.all.flatMap { d -> d.chantPhrases.map { d.id to it } }
        println("threshold  own-line pass  other-line match   (checks n=${checks.size})")
        for (t in listOf(0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70)) {
            var own = 0
            var other = 0
            checks.forEach { (header, c) ->
                val segments = listOf(TranscriptSegment(c.transcript))
                if (LineMatcher.match(header.phrases[c.phrase], segments).distance <= t) own++
                val otherBest = allLines.filter { it != header.demon to header.phrases[c.phrase] }
                    .minOf { (_, line) -> LineMatcher.match(line, segments).distance }
                if (otherBest <= t) other++
            }
            println("  %.2f       %5.1f %%         %5.1f %%".format(t, own * 100.0 / checks.size, other * 100.0 / checks.size))
        }
    }
}
