package com.summondemons.safely.ritual

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Replays sessions logged on a device (ADR-0025) through today's engine and tuning, and prints
 * every verdict as it was on the phone and as it would be now, with the aligned path.
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
                val replayed = SessionReplay.replay(header, ticks)
                var phoneRejects = 0
                var nowRejects = 0
                replayed.forEach { r ->
                    val then = r.logged.judged
                    val now = r.step.effects.filterIsInstance<RitualEffect.Judged>().lastOrNull()
                    if (then != null) {
                        if (!then.passed) phoneRejects++
                        appendLine("   %6.1fs %s·%d phone %s cost %.2f rank %s%s".format(
                            r.logged.t / 1000.0, ROMAN[then.phrase], then.chunk + 1, verdict(then.passed), then.cost,
                            then.rank?.let { "%.2f".format(it) } ?: "–", if (then.talk) " talk" else "",
                        ))
                    }
                    if (now != null) {
                        val v = now.verdict
                        if (!v.passed) nowRejects++
                        appendLine("   %6.1fs %s·%d now   %s cost %.2f rank %s%s".format(
                            r.logged.t / 1000.0, ROMAN[now.phrase], now.chunk + 1, verdict(v.passed), v.cost,
                            v.rank?.let { "%.2f".format(it) } ?: "–", if (v.talk) " talk" else "",
                        ))
                        val slots = ChantChunks.of(header.phrases[now.phrase])[now.chunk].slots
                        appendLine("            " + slots.mapIndexed { j, slot ->
                            val c = v.alignment.slotCosts[j]
                            "${slot.vowel ?: "-"}${if (slot.optional) "?" else ""}:" + if (c == null) "skip" else "${v.alignment.slotFrames[j]}f %.2f".format(c)
                        }.joinToString("  ") + "  | skipped ${v.alignment.skippedFrames}f")
                    }
                }
                val turns = replayed.zipWithNext().count { (a, b) -> a.logged.ph != b.logged.ph }
                appendLine("   rejected: phone $phoneRejects, now $nowRejects; phase changes $turns")
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
