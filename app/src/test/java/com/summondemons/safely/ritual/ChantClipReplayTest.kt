package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0022). Replays tester clips through the real
 * pipeline and prints every chunk verdict with its aligned path, per voice.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices), named
 * `{demonId}-{label}-{voice}-{stamp}.wav` (label `chant` or `nonsense`); the older
 * `{demonId}-{label}-{stamp}.wav` names count as voice `unknown`. Without clips the test is
 * skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 * The report is also written to `app/build/reports/chant-clips.txt`; compare it with
 * `app/src/test/chant-clips/baseline-old-judge.txt`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Clip(val name: String, val demon: String, val label: String, val voice: String)

    private data class Result(val clip: Clip, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsMeetTheGate() {
        val files = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", files.isNotEmpty())
        val results = files.flatMap { file ->
            val clip = parse(file)
            val demon = DemonCatalog.byId(clip.demon) ?: error("${file.name}: unknown demon '${clip.demon}'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(clip, it) }
        }
        val report = buildString {
            results.forEach { r -> appendLine(line(r)); appendLine(path(r)) }
            appendLine()
            summary(results).forEach(::appendLine)
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/chant-clips.txt").writeText(report)

        val chant = results.filter { it.clip.label == "chant" }
        assumeTrue("not enough data", sufficiency(chant) == null)
        chant.groupBy { it.clip.voice }.forEach { (voice, list) ->
            val rejected = list.count { !it.judged.verdict.passed } * 100.0 / list.size
            assertTrue("voice $voice: %.1f %% honest chunks rejected".format(rejected), rejected <= HONEST_REJECT_MAX)
        }
    }

    private fun line(r: Result): String {
        val v = r.judged.verdict
        return "%-48s %-8s %-6s %-3s·%d cost %.2f rank %s shuffles %2d%s %s".format(
            r.clip.name, r.clip.label, r.clip.voice, ROMAN[r.judged.phrase], r.judged.chunk + 1, v.cost,
            v.rank?.let { "%.2f".format(it) } ?: " –  ", v.shuffles, if (v.talk) " talk" else "", if (v.passed) "PASS" else "fail",
        )
    }

    /** Expected vowel → frames (cost, or "skip") for each slot, then skipped frames. */
    private fun path(r: Result): String {
        val demon = DemonCatalog.byId(r.clip.demon)!!
        val slots = ChantChunks.of(demon.chantPhrases[r.judged.phrase])[r.judged.chunk].slots
        val a = r.judged.verdict.alignment
        val parts = slots.mapIndexed { j, slot ->
            val c = a.slotCosts[j]
            "${slot.vowel ?: "-"}${if (slot.optional) "?" else ""}:" + if (c == null) "skip" else "${a.slotFrames[j]}f %.2f".format(c)
        }
        return "    " + parts.joinToString("  ") + "  | skipped ${a.skippedFrames}f"
    }

    private fun parse(file: File): Clip {
        val parts = file.nameWithoutExtension.split('-')
        require(parts.size >= 3) { "${file.name}: expected {demonId}-{label}-{voice}-{stamp}.wav" }
        val voice = if (parts.size >= 4) parts[2] else "unknown"
        return Clip(file.name, parts[0], parts[1], voice)
    }

    private fun summary(results: List<Result>): List<String> = buildList {
        add("voice     label     chunks  passed")
        results.groupBy { it.clip.voice to it.clip.label }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            add("%-9s %-9s %6d  %5.1f %%".format(key.first, key.second, list.size, list.count { it.judged.verdict.passed } * 100.0 / list.size))
        }
        add("")
        add("honest chants per phrase: takes, voices, chunks passed")
        val chant = results.filter { it.clip.label == "chant" }
        chant.groupBy { it.clip.demon to it.judged.phrase }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            val takes = list.map { it.clip.name }.distinct().size
            val voices = list.map { it.clip.voice }.distinct().size
            add("%-10s %-3s takes %2d voices %d passed %5.1f %%".format(key.first, ROMAN[key.second], takes, voices, list.count { it.judged.verdict.passed } * 100.0 / list.size))
        }
        add("")
        add("enough data? " + (sufficiency(chant) ?: "yes"))
    }

    /** Null when every judged phrase has ≥ 4 honest takes from ≥ 2 voices; else what is missing. */
    private fun sufficiency(chant: List<Result>): String? {
        if (chant.isEmpty()) return "insufficient: no honest chants"
        val missing = chant.groupBy { it.clip.demon to it.judged.phrase }.mapNotNull { (key, list) ->
            val takes = list.map { it.clip.name }.distinct().size
            val voices = list.map { it.clip.voice }.distinct().size
            if (takes < MIN_TAKES || voices < MIN_VOICES) "${key.first} ${ROMAN[key.second]} ($takes takes, $voices voices)" else null
        }
        return if (missing.isEmpty()) null else "insufficient: " + missing.joinToString()
    }

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        const val MIN_TAKES = 4
        const val MIN_VOICES = 2
        const val HONEST_REJECT_MAX = 2.0
    }
}
