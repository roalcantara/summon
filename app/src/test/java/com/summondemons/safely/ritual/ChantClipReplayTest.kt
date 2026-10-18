package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0018). Replays tester clips through the real
 * pipeline and prints how each judged phrase scores, per voice.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices), named
 * `{demonId}-{label}-{voice}-{stamp}.wav` (label `chant` or `nonsense`); the older
 * `{demonId}-{label}-{stamp}.wav` names count as voice `unknown`. Without clips the test is
 * skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 * The report is also written to `app/build/reports/chant-clips.txt`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Clip(val name: String, val demon: String, val label: String, val voice: String)

    private data class Result(val clip: Clip, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsReport() {
        val files = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", files.isNotEmpty())
        val results = files.flatMap { file ->
            val clip = parse(file)
            val demon = DemonCatalog.byId(clip.demon) ?: error("${file.name}: unknown demon '${clip.demon}'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(clip, it) }
        }
        val report = buildString {
            results.forEach { r ->
                val s = r.judged.score
                appendLine(
                    "%-48s %-8s %-8s phrase %-3s vowels %.2f rhythm %.2f total %.2f %s".format(
                        r.clip.name, r.clip.label, r.clip.voice, ROMAN[r.judged.phrase],
                        s.vowels, s.rhythm, s.total, if (s.total >= CHANT_PASS_SCORE) "PASS" else "fail",
                    ),
                )
            }
            appendLine()
            summary(results.map { Triple(it.clip, it.judged.phrase, it.judged.score.total >= CHANT_PASS_SCORE) }).forEach(::appendLine)
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/chant-clips.txt").writeText(report)
    }

    private fun parse(file: File): Clip {
        val parts = file.nameWithoutExtension.split('-')
        require(parts.size >= 3) { "${file.name}: expected {demonId}-{label}-{voice}-{stamp}.wav" }
        val voice = if (parts.size >= 4) parts[2] else "unknown"
        return Clip(file.name, parts[0], parts[1], voice)
    }

    /** Pass rates per voice and label, per phrase for honest chants, and whether the data is enough. */
    private fun summary(rows: List<Triple<Clip, Int, Boolean>>): List<String> = buildList {
        add("voice     label     judged  passed")
        rows.groupBy { it.first.voice to it.first.label }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            add("%-9s %-9s %6d  %5.1f %%".format(key.first, key.second, list.size, list.count { it.third } * 100.0 / list.size))
        }
        add("")
        add("honest chants per phrase: takes, voices, passed")
        val chant = rows.filter { it.first.label == "chant" }
        chant.groupBy { it.first.demon to it.second }.toSortedMap(compareBy({ it.first }, { it.second })).forEach { (key, list) ->
            val takes = list.map { it.first.name }.distinct().size
            val voices = list.map { it.first.voice }.distinct().size
            add("%-10s %-3s takes %2d voices %d passed %5.1f %%".format(key.first, ROMAN[key.second], takes, voices, list.count { it.third } * 100.0 / list.size))
        }
        add("")
        add("enough data? " + (sufficiency(chant) ?: "yes"))
    }

    /** Null when every judged phrase has ≥ 4 honest takes from ≥ 2 voices; else what is missing. */
    private fun sufficiency(chant: List<Triple<Clip, Int, Boolean>>): String? {
        if (chant.isEmpty()) return "insufficient: no honest chants"
        val missing = chant.groupBy { it.first.demon to it.second }.mapNotNull { (key, list) ->
            val takes = list.map { it.first.name }.distinct().size
            val voices = list.map { it.first.voice }.distinct().size
            if (takes < MIN_TAKES || voices < MIN_VOICES) "${key.first} ${ROMAN[key.second]} ($takes takes, $voices voices)" else null
        }
        return if (missing.isEmpty()) null else "insufficient: " + missing.joinToString()
    }

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        const val MIN_TAKES = 4
        const val MIN_VOICES = 2
    }
}
