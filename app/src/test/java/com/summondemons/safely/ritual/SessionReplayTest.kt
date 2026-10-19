package com.summondemons.safely.ritual

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Replays sessions logged on a device (ADR-0025) through today's engine and tuning, and prints
 * every verdict as it was on the phone and as it would be now, the aligned path, and the phrase size each chant layout reached (ADR-0029).
 *
 * Put the `.jsonl` files from `tools/pull_sessions.sh` in `app/src/test/sessions/` (git-ignored).
 * Run: `./gradlew testDebugUnitTest --tests '*SessionReplayTest*' -i`. The report is also
 * written to `app/build/reports/sessions.txt`.
 */
class SessionReplayTest {

    private val dir = File(System.getProperty("sessions") ?: "src/test/sessions")

    @Test
    fun loggedSessionsReport() {
        val files = dir.listFiles { f -> f.extension == "jsonl" }.orEmpty().sortedBy { it.name }
        assumeTrue("no sessions in ${dir.absolutePath}", files.isNotEmpty())
        val report = buildString {
            files.forEach { file ->
                val (header, ticks) = SessionReplay.read(file)
                appendLine("== ${file.name}  ${header.demon}  ticks ${ticks.count { it.isTick }}")
                ticks.filter { it.note != null }.forEach { appendLine("   %6.1fs note %s".format(it.t / 1000.0, it.note)) }
                appendLine("   layout ${header.layout}")
                val sizes = ticks.mapNotNull { it.ui }
                if (sizes.isNotEmpty()) {
                    appendLine("   layout    line  phrase sp  box dp      sigil dp")
                    sizes.forEach { u ->
                        appendLine("   %-8s  %-4s  %9.0f  %4dx%-5d  %d".format(u.layout, ROMAN[u.phrase.coerceAtMost(3)], u.phraseSp, u.boxWidthDp, u.boxHeightDp, u.sigilDp))
                    }
                }
                val replayed = SessionReplay.replay(header, ticks)
                replayed.forEach { r ->
                    r.logged.check?.let { c ->
                        appendLine("   %6.1fs %s check %s dist %.2f %d ms “%s”".format(
                            r.logged.t / 1000.0, ROMAN[c.phrase], verdict(c.found), c.distance, c.checkMs, c.transcript.take(60),
                        ))
                    }
                }
                val seals = replayed.count { r -> r.step.effects.any { it is RitualEffect.Sealed } }
                appendLine("   sealed $seals; phase changes ${replayed.zipWithNext().count { (a, b) -> a.logged.ph != b.logged.ph }}")
                appendLine()
            }
        }
        println(report)
        File("build/reports").mkdirs()
        File("build/reports/sessions.txt").writeText(report)
    }

    private fun verdict(passed: Boolean) = if (passed) "PASS" else "fail"

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV")
    }
}
