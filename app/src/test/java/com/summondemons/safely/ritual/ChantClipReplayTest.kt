package com.summondemons.safely.ritual

import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.ritual.RitualConfig.CHANT_PASS_SCORE
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Calibration harness for the chant check (ADR-0018). Replays tester clips recorded by the
 * debug build's clip recorder through the real pipeline and prints how each label scores.
 *
 * Clips live in `app/src/test/chant-clips/` (git-ignored: they are people's voices) and are
 * named `{demonId}-{label}-{stamp}.wav`, label `chant` or `nonsense`. Without clips the test
 * is skipped. Run: `./gradlew testDebugUnitTest --tests '*ChantClipReplayTest*' -i`.
 */
class ChantClipReplayTest {

    private val dir = File(System.getProperty("chantClips") ?: "src/test/chant-clips")

    private data class Result(val clip: String, val label: String, val judged: RitualEffect.Judged)

    @Test
    fun testerClipsMeetTheTargets() {
        val clips = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }.orEmpty().sortedBy { it.name }
        assumeTrue("no clips in ${dir.absolutePath}", clips.isNotEmpty())
        val results = clips.flatMap { file ->
            val (demonId, label) = file.nameWithoutExtension.split('-').let { it[0] to it.getOrElse(1) { "" } }
            val demon = DemonCatalog.byId(demonId) ?: error("${file.name}: unknown demon '$demonId'")
            val wav = Wav.read(file)
            ChantReplay.judge(wav.samples, wav.rate, demon.chantPhrases).map { Result(file.name, label, it) }
        }
        results.forEach { r ->
            val s = r.judged.score
            println("%-44s %-8s phrase %s  vowels %.2f  rhythm %.2f  total %.2f  %s".format(
                r.clip, r.label, ROMAN[r.judged.phrase], s.vowels, s.rhythm, s.total, if (s.passed) "PASS" else "fail",
            ))
        }
        val chant = results.filter { it.label == "chant" }.map { it.judged.score.total }
        val nonsense = results.filter { it.label == "nonsense" }.map { it.judged.score.total }
        println("\nthreshold  chant pass  nonsense pass   (chant n=${chant.size}, nonsense n=${nonsense.size})")
        for (t in THRESHOLDS) {
            println("  %.2f%s     %5.1f %%      %5.1f %%".format(t, if (t == CHANT_PASS_SCORE) "*" else " ", rate(chant, t), rate(nonsense, t)))
        }
        assumeTrue("need ≥ $MIN_JUDGED judged phrases per label", chant.size >= MIN_JUDGED && nonsense.size >= MIN_JUDGED)
        assertTrue("honest chants rejected too often", rate(chant, CHANT_PASS_SCORE) >= 95.0)
        assertTrue("nonsense accepted too often", rate(nonsense, CHANT_PASS_SCORE) <= 40.0)
    }

    private fun rate(scores: List<Float>, threshold: Float) =
        if (scores.isEmpty()) 0.0 else scores.count { it >= threshold } * 100.0 / scores.size

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
        val THRESHOLDS = listOf(0.45f, 0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f)
        const val MIN_JUDGED = 20
    }
}
