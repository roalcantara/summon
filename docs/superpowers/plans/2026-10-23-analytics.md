# Anonymous Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Send 8 anonymous, typed events (ritual funnel, daily activity, escapes, device check) to TelemetryDeck in release builds, behind an opt-out switch, so the success criteria in the evaluation playbook can be measured.

**Architecture:** Pure, unit-tested pieces carry the logic: `Buckets` (raw values to fixed buckets), `AnalyticsEvent` (one typed class per event), `ProgressRules.newlyEscaped`. An `Analytics` interface hides the vendor; `GatedAnalytics` applies the switch; `RitualViewModel`, `GameViewModel`, `SummonApp` and `MainActivity` report events. Debug builds log to Logcat and send nothing.

**Tech Stack:** Kotlin, Jetpack Compose, TelemetryDeck Kotlin SDK, SharedPreferences, JUnit 4, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-23-analytics-design.md`. **Evaluation (after release):** `docs/success/2026-10-23-evaluation-playbook.md`. **Branch:** `fix/pre-release-ui`.

**Order:** run this plan **after** the final-pass review fixes (`docs/reviews/2026-10-23-final-pass/`) are applied, and **before** the full `tools/ui_matrix.sh s24-en en` run.

---

## Conventions

- `…/safely/` = `app/src/main/java/com/summondemons/safely/`; tests in `app/src/test/java/com/summondemons/safely/`.
- Tests: `./gradlew -q testDebugUnitTest` (exit 0 = pass). Install: `./gradlew -q installDebug`.
- New player text in all five `app/src/main/res/values*/strings.xml`, none longer than English (`TranslationResourcesTest`).
- No personal data in any event: only the enum and bucket values listed in the spec.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## File map

| File | Change | Responsibility |
|---|---|---|
| `…/safely/platform/analytics/Buckets.kt` | create | raw numbers → bucket labels |
| `…/safely/platform/analytics/AnalyticsEvent.kt` | create | the 8 typed events |
| `…/safely/platform/analytics/Analytics.kt` | create | `Analytics`, `NoAnalytics`, `GatedAnalytics`, `LogAnalytics` |
| `…/safely/platform/analytics/TelemetryDeckAnalytics.kt` | create | vendor adapter (only file that imports the SDK) |
| `…/safely/platform/analytics/AnalyticsSettings.kt` | create | the switch, the app-instance ID |
| `…/safely/progress/ProgressRules.kt` | modify | `newlyEscaped` |
| `…/safely/ui/GameViewModel.kt` | modify | `demon_escaped` |
| `…/safely/ritual/RitualViewModel.kt` | modify | ritual events |
| `…/safely/ui/screens/RitualScreen.kt` | modify | permission answers |
| `…/safely/AppGraph.kt`, `SummonApp.kt`, `MainActivity.kt`, `navigation/AppNavHost.kt` | modify | wiring, `app_open`, `device_check` |
| `…/safely/ui/components/SettingsSheet.kt` | modify | the switch row |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml` | modify | SDK, app ID, INTERNET |
| tests: `platform/analytics/*Test.kt`, `progress/ProgressRulesTest.kt`, `ritual/RitualViewModelTest.kt`, `ui/GameViewModelTest.kt` | create/modify | |
| `docs/decisions/0035-anonymous-analytics.md`, `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md`, `PRODUCT.md`, `AGENTS.md` | create/modify | docs |

---

### Task 1: Buckets (pure, test first)

**Files:** Create `…/safely/platform/analytics/Buckets.kt`; Test `app/src/test/java/com/summondemons/safely/platform/analytics/BucketsTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class BucketsTest {

    @Test
    fun daysSinceInstall() {
        assertEquals("0", Buckets.daysSinceInstall(0))
        assertEquals("1", Buckets.daysSinceInstall(1))
        assertEquals("2", Buckets.daysSinceInstall(2))
        assertEquals("3-6", Buckets.daysSinceInstall(3))
        assertEquals("3-6", Buckets.daysSinceInstall(6))
        assertEquals("7-13", Buckets.daysSinceInstall(7))
        assertEquals("14-29", Buckets.daysSinceInstall(29))
        assertEquals("30+", Buckets.daysSinceInstall(30))
        assertEquals("0", Buckets.daysSinceInstall(-2)) // clock moved back
    }

    @Test
    fun lockSeconds() {
        assertEquals("0-5", Buckets.lockSeconds(0))
        assertEquals("0-5", Buckets.lockSeconds(4_999))
        assertEquals("5-10", Buckets.lockSeconds(5_000))
        assertEquals("10-30", Buckets.lockSeconds(29_999))
        assertEquals("30-60", Buckets.lockSeconds(30_000))
        assertEquals("60+", Buckets.lockSeconds(60_000))
    }

    @Test
    fun duration() {
        assertEquals("0-30s", Buckets.duration(29_999))
        assertEquals("30-60s", Buckets.duration(30_000))
        assertEquals("1-2m", Buckets.duration(60_000))
        assertEquals("2-5m", Buckets.duration(120_000))
        assertEquals("5m+", Buckets.duration(300_000))
    }

    @Test
    fun score() {
        assertEquals("0-49", Buckets.score(0))
        assertEquals("0-49", Buckets.score(49))
        assertEquals("50-69", Buckets.score(50))
        assertEquals("70-84", Buckets.score(84))
        assertEquals("85-100", Buckets.score(85))
        assertEquals("85-100", Buckets.score(100))
    }
}
```

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*BucketsTest*'` → FAIL (`Unresolved reference: Buckets`).

- [ ] **Step 3: Implement**

```kotlin
package com.summondemons.safely.platform.analytics

/** Raw numbers become fixed labels before they leave the device (spec 2026-10-23 analytics). */
object Buckets {
    fun daysSinceInstall(days: Long): String = when {
        days <= 0 -> "0"
        days == 1L -> "1"
        days == 2L -> "2"
        days <= 6 -> "3-6"
        days <= 13 -> "7-13"
        days <= 29 -> "14-29"
        else -> "30+"
    }

    fun lockSeconds(ms: Long): String = when {
        ms < 5_000 -> "0-5"
        ms < 10_000 -> "5-10"
        ms < 30_000 -> "10-30"
        ms < 60_000 -> "30-60"
        else -> "60+"
    }

    fun duration(ms: Long): String = when {
        ms < 30_000 -> "0-30s"
        ms < 60_000 -> "30-60s"
        ms < 120_000 -> "1-2m"
        ms < 300_000 -> "2-5m"
        else -> "5m+"
    }

    /** A chant-check score, 0..100 (`LineFeedback.score`). */
    fun score(score: Int): String = when {
        score < 50 -> "0-49"
        score < 70 -> "50-69"
        score < 85 -> "70-84"
        else -> "85-100"
    }
}
```

- [ ] **Step 4:** same command → exit 0.
- [ ] **Step 5: Commit** — `git add` both files; `git commit -m "feat(analytics): Buckets for event values" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"`.

---

### Task 2: Events and the Analytics interface (test first)

**Files:** Create `…/safely/platform/analytics/AnalyticsEvent.kt`, `…/safely/platform/analytics/Analytics.kt`; Test `…/platform/analytics/AnalyticsEventTest.kt`, `…/platform/analytics/GatedAnalyticsTest.kt`, `…/platform/analytics/RecordingAnalytics.kt` (test helper)

- [ ] **Step 1: Test helper** `app/src/test/java/com/summondemons/safely/platform/analytics/RecordingAnalytics.kt`:

```kotlin
package com.summondemons.safely.platform.analytics

/** Records events for assertions. */
class RecordingAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()
    override fun track(event: AnalyticsEvent) {
        events += event
    }
}
```

- [ ] **Step 2: Failing tests**

`AnalyticsEventTest.kt`:

```kotlin
package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun namesAndParams() {
        assertEquals("app_open" to mapOf("days_since_install" to "3-6"), AnalyticsEvent.AppOpen(4).let { it.name to it.params() })
        assertEquals(
            "ritual_start" to mapOf("demon" to "asmodeus", "mode" to "whisper", "in_court" to "true"),
            AnalyticsEvent.RitualStart("asmodeus", whisper = true, inCourt = true).let { it.name to it.params() },
        )
        assertEquals(mapOf("result" to "denied_permanently"), AnalyticsEvent.MicPermission(granted = false, permanent = true).params())
        assertEquals(mapOf("seconds" to "5-10"), AnalyticsEvent.DirectionLocked(7_000).params())
        assertEquals(mapOf("line" to "2", "result" to "sealed", "score" to "70-84"), AnalyticsEvent.LineResult(1, sealed = true, score = 80).params())
        assertEquals(mapOf("line" to "4", "result" to "cracked"), AnalyticsEvent.LineResult(3, sealed = false, score = null).params())
        assertEquals(
            mapOf("outcome" to "completed", "phase" to "done", "flawless" to "true", "duration" to "1-2m"),
            AnalyticsEvent.RitualEnd(completed = true, phase = "done", flawless = true, durationMs = 90_000).params(),
        )
        assertEquals(
            mapOf("outcome" to "abandoned", "phase" to "seeking", "duration" to "0-30s"),
            AnalyticsEvent.RitualEnd(completed = false, phase = "seeking", flawless = false, durationMs = 5_000).params(),
        )
        assertEquals(mapOf("demon" to "astaroth"), AnalyticsEvent.DemonEscaped("astaroth").params())
        assertEquals(mapOf("mode" to "lenient", "reason" to "too_slow"), AnalyticsEvent.DeviceCheck(whisper = false, reason = "slow: 2400 ms").params())
    }

    @Test
    fun deviceCheckReasonsAreCodesNeverFreeText() {
        assertEquals("ok", AnalyticsEvent.DeviceCheck(true, "812 ms").params()["reason"])
        assertEquals("load_error", AnalyticsEvent.DeviceCheck(false, "load: UnsatisfiedLinkError").params()["reason"])
        assertEquals("other", AnalyticsEvent.DeviceCheck(false, "anything else").params()["reason"])
    }
}
```

`GatedAnalyticsTest.kt`:

```kotlin
package com.summondemons.safely.platform.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class GatedAnalyticsTest {

    @Test
    fun theSwitchDecidesAtEachEvent() {
        val sink = RecordingAnalytics()
        var on = true
        val gated = GatedAnalytics(sink) { on }
        gated.track(AnalyticsEvent.DemonEscaped("asmodeus"))
        on = false
        gated.track(AnalyticsEvent.DemonEscaped("astaroth"))
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.DemonEscaped("asmodeus")), sink.events)
    }
}
```

- [ ] **Step 3:** `./gradlew -q testDebugUnitTest --tests '*AnalyticsEventTest*' --tests '*GatedAnalyticsTest*'` → FAIL (unresolved references).

- [ ] **Step 4: Implement** `AnalyticsEvent.kt`:

```kotlin
package com.summondemons.safely.platform.analytics

/**
 * The only things the app ever sends (spec 2026-10-23 analytics, ADR-0035): typed, bucketed,
 * never free text, audio, location or personal data.
 */
sealed interface AnalyticsEvent {
    val name: String
    fun params(): Map<String, String>

    data class AppOpen(val daysSinceInstall: Long) : AnalyticsEvent {
        override val name = "app_open"
        override fun params() = mapOf("days_since_install" to Buckets.daysSinceInstall(daysSinceInstall))
    }

    data class RitualStart(val demon: String, val whisper: Boolean, val inCourt: Boolean) : AnalyticsEvent {
        override val name = "ritual_start"
        override fun params() = mapOf("demon" to demon, "mode" to mode(whisper), "in_court" to inCourt.toString())
    }

    data class MicPermission(val granted: Boolean, val permanent: Boolean) : AnalyticsEvent {
        override val name = "mic_permission"
        override fun params() = mapOf(
            "result" to when {
                granted -> "granted"
                permanent -> "denied_permanently"
                else -> "denied"
            },
        )
    }

    data class DirectionLocked(val sinceStartMs: Long) : AnalyticsEvent {
        override val name = "direction_locked"
        override fun params() = mapOf("seconds" to Buckets.lockSeconds(sinceStartMs))
    }

    /** [line] is 0-based (phrase index); sent 1-based. [score] is null in lenient mode and for cracks. */
    data class LineResult(val line: Int, val sealed: Boolean, val score: Int?) : AnalyticsEvent {
        override val name = "line_result"
        override fun params() = buildMap {
            put("line", (line + 1).toString())
            put("result", if (sealed) "sealed" else "cracked")
            score?.let { put("score", Buckets.score(it)) }
        }
    }

    /** [phase]: intro, seeking, locking, chanting, wavering, error_mic, error_heading, or done. */
    data class RitualEnd(val completed: Boolean, val phase: String, val flawless: Boolean, val durationMs: Long) : AnalyticsEvent {
        override val name = "ritual_end"
        override fun params() = buildMap {
            put("outcome", if (completed) "completed" else "abandoned")
            put("phase", phase)
            if (completed) put("flawless", flawless.toString())
            put("duration", Buckets.duration(durationMs))
        }
    }

    data class DemonEscaped(val demon: String) : AnalyticsEvent {
        override val name = "demon_escaped"
        override fun params() = mapOf("demon" to demon)
    }

    /** [reason] is `DeviceCheck`'s raw reason; only a fixed code is sent. */
    data class DeviceCheck(val whisper: Boolean, val reason: String) : AnalyticsEvent {
        override val name = "device_check"
        override fun params() = mapOf(
            "mode" to mode(whisper),
            "reason" to when {
                reason.endsWith(" ms") && !reason.startsWith("slow") -> "ok"
                reason.startsWith("slow") -> "too_slow"
                reason.startsWith("load") -> "load_error"
                else -> "other"
            },
        )
    }
}

private fun mode(whisper: Boolean) = if (whisper) "whisper" else "lenient"
```

`Analytics.kt`:

```kotlin
package com.summondemons.safely.platform.analytics

import android.util.Log

/** Where events go (ADR-0035). Composables never call it; view models and the app do. */
interface Analytics {
    fun track(event: AnalyticsEvent)
}

/** Sends nothing: tests, and release builds without an app ID. */
object NoAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Drops events while the player's switch is off ([enabled] is read at each event). */
class GatedAnalytics(private val delegate: Analytics, private val enabled: () -> Boolean) : Analytics {
    override fun track(event: AnalyticsEvent) {
        if (enabled()) delegate.track(event)
    }
}

/** Debug builds: events go to Logcat (tag "Analytics"), never to the network. */
object LogAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) {
        Log.d("Analytics", "${event.name} ${event.params()}")
    }
}
```

- [ ] **Step 5:** same command → exit 0.
- [ ] **Step 6: Commit** — the 5 files; `feat(analytics): Typed events, the Analytics interface and the opt-out gate`.

---

### Task 3: Escapes (pure rule, test first) and GameViewModel

**Files:** Modify `…/safely/progress/ProgressRules.kt`, `…/safely/ui/GameViewModel.kt`; Test `progress/ProgressRulesTest.kt`, `ui/GameViewModelTest.kt`

- [ ] **Step 1: Failing test** — append to `ProgressRulesTest` (add the static import `com.summondemons.safely.progress.ProgressRules.newlyEscaped`):

```kotlin
    @Test
    fun newlyEscapedListsOnlyDemonsThatJustSlippedTheirSeal() {
        val bound = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        val escaped = evaluate(bound, 13)
        assertEquals(listOf("asmodeus"), newlyEscaped(bound, escaped))
        assertEquals(emptyList<String>(), newlyEscaped(escaped, escaped))
        assertEquals(emptyList<String>(), newlyEscaped(Progress(), bound))
    }
```

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*ProgressRulesTest*'` → FAIL.

- [ ] **Step 3: Implement** in `object ProgressRules` (after `evaluate`):

```kotlin
    /** Demons bound in [before] that [after] shows escaped (unbound with the banner queued). */
    fun newlyEscaped(before: Progress, after: Progress): List<String> =
        after.demons.filter { (id, r) -> !r.bound && r.escapeBannerPending && before.record(id).bound }.keys.sorted()
```

- [ ] **Step 4:** same command → exit 0.

- [ ] **Step 5: GameViewModel** — add a constructor parameter (last, with a default so other callers compile):

```kotlin
    private val analytics: Analytics = NoAnalytics,
```

At the end of the `init { viewModelScope.launch { … } }` block's coroutine is a `collect` that never returns, so add a **second** launch in `init`:

```kotlin
        // Escapes, as the stored progress changes (first load included): ADR-0035.
        viewModelScope.launch {
            var previous: Progress? = null
            stored.filterNotNull().collect { now ->
                previous?.let { before -> ProgressRules.newlyEscaped(before, now).forEach { analytics.track(AnalyticsEvent.DemonEscaped(it)) } }
                previous = now
            }
        }
```

Update the factory: `fun factory(store: ProgressStore, clock: GameClock, debug: DebugSettingsStore, analytics: Analytics = NoAnalytics)` and pass `analytics = analytics` to the constructor. Imports: `com.summondemons.safely.platform.analytics.Analytics`, `AnalyticsEvent`, `NoAnalytics`.

- [ ] **Step 6: Test** — append to `GameViewModelTest` (helper `vm(store)` gains an optional `analytics` parameter passed as `analytics = analytics`):

```kotlin
    @Test
    fun anEscapeIsReportedOnce() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val store = InMemoryProgressStore(boundOn(day0 - 3))
        val vm = vm(store, analytics = analytics)
        observe(vm); runCurrent()
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.DemonEscaped("asmodeus")), analytics.events)
    }
```

(Imports: `com.summondemons.safely.platform.analytics.AnalyticsEvent`, `RecordingAnalytics`.) Run `./gradlew -q testDebugUnitTest --tests '*GameViewModelTest*'` → exit 0.

- [ ] **Step 7: Commit** — `feat(analytics): Report escapes (ProgressRules.newlyEscaped)`.

---

### Task 4: Ritual events (test first)

**Files:** Modify `…/safely/ritual/RitualViewModel.kt`, `…/safely/ui/screens/RitualScreen.kt`; Test `ritual/RitualViewModelTest.kt`

- [ ] **Step 1: Failing tests** — in `RitualViewModelTest`, extend `newVm` with `analytics: Analytics = NoAnalytics` passed as `analytics = analytics`, then append:

```kotlin
    @Test
    fun aCompletedRitualReportsStartLockLinesAndEnd() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        observe(vm)
        vm.onMicPermission(false)
        vm.setSimulateHeading(true)
        vm.setHoldToChant(true)
        advanceTimeBy(20_000); runCurrent()
        vm.clearForTest()

        val names = analytics.events.map { it.name }
        assertEquals(listOf("ritual_start", "direction_locked", "line_result", "line_result", "line_result", "line_result", "ritual_end"), names)
        val end = analytics.events.last() as AnalyticsEvent.RitualEnd
        assertEquals(true, end.completed)
        assertEquals(true, end.flawless)
        assertEquals("lenient", (analytics.events.first() as AnalyticsEvent.RitualStart).params()["mode"])
    }

    @Test
    fun leavingBeforeTheLockReportsAnAbandonInSeeking() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        observe(vm)
        vm.onMicPermission(true)
        advanceTimeBy(2_000); runCurrent()
        vm.clearForTest()
        val end = analytics.events.last() as AnalyticsEvent.RitualEnd
        assertEquals(false, end.completed)
        assertEquals("seeking", end.phase)
    }

    @Test
    fun permissionAnswersAreReportedButResumeChecksAreNot() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = newVm(demon, { emptyFlow() }, silentPcm(), analytics = analytics)
        vm.onMicPermission(true) // resume check: not an answer
        vm.onPermissionAnswer(granted = false, permanent = true)
        assertEquals(
            listOf<AnalyticsEvent>(AnalyticsEvent.MicPermission(granted = false, permanent = true)),
            analytics.events.filterIsInstance<AnalyticsEvent.MicPermission>(),
        )
    }
```

Imports: `com.summondemons.safely.platform.analytics.Analytics`, `AnalyticsEvent`, `NoAnalytics`, `RecordingAnalytics`.

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*RitualViewModelTest*'` → FAIL.

- [ ] **Step 3: Implement in `RitualViewModel`**

Constructor, last parameter: `private val analytics: Analytics = NoAnalytics,`. Factory: same parameter, passed through.

Fields (after `private var ticks = 0L`):

```kotlin
    /** Tick at which the intro card closed (or 0 when it never showed): the lock's clock starts there. */
    private var introClosedTick = 0L
    private var ended = false
```

In `init { … }` (first line):

```kotlin
        analytics.track(AnalyticsEvent.RitualStart(demon.id, whisper = mode.first == CheckMode.Whisper, inCourt = inCourt()))
```

In `dismissIntro()` add `introClosedTick = ticks`.

New method:

```kotlin
    /** An answer from the permission dialog (not the resume re-check): reported, then applied. */
    fun onPermissionAnswer(granted: Boolean, permanent: Boolean) {
        analytics.track(AnalyticsEvent.MicPermission(granted, permanent))
        onMicPermission(granted)
    }
```

In `tick`, after `val step = RitualEngine.step(ritual, input)` and before `step.effects.forEach { _effects.tryEmit(it) }`:

```kotlin
        reportEffects(step.effects)
```

with:

```kotlin
    private fun reportEffects(effects: List<RitualEffect>) {
        val checked = effects.filterIsInstance<RitualEffect.LineChecked>()
        effects.forEach { e ->
            when (e) {
                RitualEffect.Locked -> analytics.track(AnalyticsEvent.DirectionLocked((ticks - introClosedTick) * RitualConfig.FRAME_MS))
                is RitualEffect.Sealed -> {
                    val score = checked.firstOrNull { it.phrase == e.phrase && it.found }?.let { LineFeedback.score(it.distance) }
                    analytics.track(AnalyticsEvent.LineResult(e.phrase, sealed = true, score = score))
                }
                is RitualEffect.Cracked -> analytics.track(AnalyticsEvent.LineResult(e.phrase, sealed = false, score = null))
                else -> Unit
            }
        }
    }
```

End of the ritual, in `onCleared()` (first line), plus a test hook:

```kotlin
    override fun onCleared() {
        reportEnd()
        clipRecorder.stop()
        sessionLog.stop()
    }

    /** Tests: run the end-of-ritual report as `onCleared` would. */
    internal fun clearForTest() = reportEnd()

    private fun reportEnd() {
        if (ended) return
        ended = true
        val s = state.value
        val phase = s.ritual.phase
        val completed = phase == RitualPhase.Climax || phase == RitualPhase.Done
        val where = when {
            completed -> "done"
            s.introVisible -> "intro"
            s.micGranted == false || s.micUnavailable -> "error_mic"
            s.headingUnavailable && !s.simulateHeading -> "error_heading"
            else -> phase.name.lowercase()
        }
        analytics.track(AnalyticsEvent.RitualEnd(completed, where, flawless = s.ritual.flawless, durationMs = ticks * RitualConfig.FRAME_MS))
    }
```

(`RitualState.flawless` exists and is used by the climax; if its name differs, use the field `ClimaxStart(flawless)` reads. Note: the test's `onMicPermission(false)` path runs with "Hold to Chant", which does not use the mic, so `where` resolves to `done` there because `completed` is checked first.)

- [ ] **Step 4: RitualScreen** — in the permission launcher callback replace `viewModel.onMicPermission(granted)` with:

```kotlin
        val permanent = !granted && deniedBefore && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        viewModel.onPermissionAnswer(granted, permanent)
        permanentlyDenied = permanent
```

(and delete the old `permanentlyDenied = …` assignment it replaces; keep `if (!granted) deniedBefore = true`).

- [ ] **Step 5:** `./gradlew -q testDebugUnitTest` → exit 0 (all tests).
- [ ] **Step 6: Commit** — `feat(analytics): Ritual funnel events (start, permission, lock, lines, end)`.

---

### Task 5: Settings store, app-instance ID and the switch row

**Files:** Create `…/safely/platform/analytics/AnalyticsSettings.kt`; Modify `…/safely/ui/components/SettingsSheet.kt`, five `strings.xml`

- [ ] **Step 1: `AnalyticsSettings.kt`**

```kotlin
package com.summondemons.safely.platform.analytics

import android.content.Context
import java.util.UUID

/** The player's switch (on by default) and the random app-instance ID (ADR-0035). */
class AnalyticsSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("analytics", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    /** Created on first use; never linked to a person; gone when the app's data is cleared. */
    val instanceId: String
        get() = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ID = "instance_id"
    }
}
```

- [ ] **Step 2: Strings** (next to `settings_language`):

| Key | en | es | pt-BR | it | fr |
|---|---|---|---|---|---|
| `settings_analytics` | `Share anonymous usage data` | `Datos de uso anónimos` | `Dados de uso anônimos` | `Dati d’uso anonimi` | `Données d’usage anonymes` |
| `settings_analytics_hint` | `No audio, no personal data.` | `Sin audio ni datos tuyos.` | `Sem áudio, sem dados seus.` | `Niente audio né dati tuoi.` | `Ni audio ni données perso.` |

Run `./gradlew -q testDebugUnitTest --tests '*TranslationResourcesTest*'` → exit 0; shorten a translation if it fails, never English. Add both keys to `docs/i18n/review.md` (status "new (2026-10-23)").

- [ ] **Step 3: SettingsSheet** — signature becomes
`fun SettingsSheet(language: String?, onLanguage: (String?) -> Unit, analyticsEnabled: Boolean, onAnalytics: (Boolean) -> Unit, onDismiss: () -> Unit, onDebug: (() -> Unit)? = null)`; after the language `FlowRow` add:

```kotlin
            Row(
                Modifier.fillMaxWidth().toggleable(value = analyticsEnabled, role = Role.Switch, onValueChange = onAnalytics),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_analytics), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_analytics_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = analyticsEnabled, onCheckedChange = null)
            }
```

Imports: `androidx.compose.foundation.selection.toggleable`, `androidx.compose.material3.Switch`, `androidx.compose.ui.semantics.Role`, `Row`, `Column`, `fillMaxWidth`, `Alignment` as needed.

- [ ] **Step 4:** build: `./gradlew -q assembleDebug` fails only at the `SettingsSheet(` call in `AppNavHost.kt` (fixed in Task 6). Do not commit yet.

---

### Task 6: Platform wiring (SDK, build config, app, activity)

**Files:** Modify `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `…/safely/AppGraph.kt`, `…/safely/SummonApp.kt`, `…/safely/MainActivity.kt`, `…/safely/navigation/AppNavHost.kt`; Create `…/safely/platform/analytics/TelemetryDeckAnalytics.kt`

- [ ] **Step 1: Dependency** — look up the current TelemetryDeck Kotlin SDK version on its README (github.com/TelemetryDeck/KotlinSDK) and add, pinned:

```toml
# versions
telemetrydeck = "<current release from the README>"
# libraries
telemetrydeck = { group = "com.telemetrydeck", name = "kotlin-sdk", version.ref = "telemetrydeck" }
```

and in `app/build.gradle.kts` dependencies: `implementation(libs.telemetrydeck)`.

- [ ] **Step 2: App ID** — in `app/build.gradle.kts`, after the signing values:

```kotlin
// Analytics (ADR-0035): like the upload key, from the environment or local.properties; never in git.
val telemetryDeckAppId = signingValue("SUMMON_TELEMETRYDECK_APP_ID").orEmpty()
val telemetryDeckTestMode = signingValue("SUMMON_TELEMETRYDECK_TEST_MODE") == "true"
if (telemetryDeckAppId.isBlank()) logger.warn("SUMMON_TELEMETRYDECK_APP_ID not set: release builds send no analytics.")
```

and in `defaultConfig`:

```kotlin
        buildConfigField("String", "TELEMETRYDECK_APP_ID", "\"$telemetryDeckAppId\"")
        buildConfigField("boolean", "TELEMETRYDECK_TEST_MODE", "$telemetryDeckTestMode")
```

- [ ] **Step 3: Manifest** — add `<uses-permission android:name="android.permission.INTERNET" />` with the comment `<!-- Anonymous analytics only (ADR-0035); mic and compass stay on the device. -->`.

- [ ] **Step 4: Adapter** `TelemetryDeckAnalytics.kt` (the only file importing the SDK; check the calls against the README of the pinned version and adapt here only):

```kotlin
package com.summondemons.safely.platform.analytics

import android.app.Application
import com.telemetrydeck.sdk.TelemetryDeck

/** Release builds: events go to TelemetryDeck with the hashed app-instance ID (ADR-0035). */
class TelemetryDeckAnalytics(application: Application, appId: String, instanceId: String, testMode: Boolean) : Analytics {
    init {
        val builder = TelemetryDeck.Builder()
            .appID(appId)
            .defaultUser(instanceId)
            .testMode(testMode)
            .showDebugLogs(false)
        TelemetryDeck.start(application, builder)
    }

    override fun track(event: AnalyticsEvent) {
        TelemetryDeck.signal(event.name, params = event.params())
    }
}
```

If the SDK sends automatic lifecycle or session signals, turn them off in the builder (only our 8 events and the SDK's metadata are declared).

- [ ] **Step 5: AppGraph** — add:

```kotlin
    val analyticsSettings = AnalyticsSettings(appContext)
    private val sink: Analytics = when {
        BuildConfig.DEBUG -> LogAnalytics
        BuildConfig.TELEMETRYDECK_APP_ID.isBlank() -> NoAnalytics
        else -> TelemetryDeckAnalytics(
            appContext as Application,
            BuildConfig.TELEMETRYDECK_APP_ID,
            analyticsSettings.instanceId,
            BuildConfig.TELEMETRYDECK_TEST_MODE,
        )
    }
    val analytics: Analytics = GatedAnalytics(sink) { analyticsSettings.enabled }

    /** Days since this install, for `app_open` (first install time from the package manager). */
    fun daysSinceInstall(): Long {
        val installed = appContext.packageManager.getPackageInfo(appContext.packageName, 0).firstInstallTime
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installed)
    }

    private var lastOpenMs = 0L

    /** `app_open` at start and on returns to the foreground after 30 minutes away. */
    fun reportOpen() {
        val now = System.currentTimeMillis()
        if (now - lastOpenMs < 30 * 60_000L) return
        lastOpenMs = now
        analytics.track(AnalyticsEvent.AppOpen(daysSinceInstall()))
    }
```

(`appContext` is the application context; cast is safe. Imports from `platform.analytics.*`, `android.app.Application`.)

- [ ] **Step 6: SummonApp** — replace the device-check thread body:

```kotlin
        thread(name = "whisper-device-check", isDaemon = true) {
            val known = DeviceCheck.cached(this) != null
            val result = DeviceCheck.runOnce(this)
            if (!known) graph.analytics.track(AnalyticsEvent.DeviceCheck(result.mode == CheckMode.Whisper, result.reason))
        }
```

- [ ] **Step 7: MainActivity** — add:

```kotlin
    override fun onStart() {
        super.onStart()
        (application as SummonApp).graph.reportOpen()
    }
```

- [ ] **Step 8: AppNavHost** — pass `graph.analytics` to `GameViewModel.factory(…, analytics = graph.analytics)` and `RitualViewModel.factory(…, analytics = graph.analytics)`; for Settings:

```kotlin
            var analyticsOn by remember { mutableStateOf(graph.analyticsSettings.enabled) }
            SettingsSheet(
                language = AppLocale.get(appContext),
                onLanguage = { tag ->
                    AppLocale.set(appContext, tag)
                    activity?.recreate()
                },
                analyticsEnabled = analyticsOn,
                onAnalytics = {
                    graph.analyticsSettings.enabled = it
                    analyticsOn = it
                },
                onDismiss = { settingsOpen = false },
                onDebug = if (BuildConfig.DEBUG) ({ settingsOpen = false; debugOpen = true }) else null,
            )
```

- [ ] **Step 9: Build and test**

```bash
./gradlew -q testDebugUnitTest && ./gradlew -q assembleDebug && ./gradlew -q assembleRelease
```

Expected: exit 0 (release unsigned without the upload key is fine; the build warns if the app ID is missing). If R8 strips SDK classes, add the keep rules from the SDK README to `app/proguard-rules.pro`.

- [ ] **Step 10: Device check (debug)** — `./gradlew -q installDebug`; `adb logcat -c; adb logcat -s Analytics:D` in one terminal; open the app, open Settings (the switch shows on), run one ritual with debug "Simulate heading" + "Hold to Chant", close it. Expected lines: `app_open`, `ritual_start`, `direction_locked`, 4 × `line_result`, `ritual_end {outcome=completed …}`. Turn the switch off, run a ritual: no lines.

- [ ] **Step 11: Commit** — all files of Tasks 5–6; `feat(analytics): TelemetryDeck in release builds, Logcat in debug, opt-out in Settings`.

---

### Task 7: Docs

**Files:** Create `docs/decisions/0035-anonymous-analytics.md`; Modify `docs/PRIVACY_POLICY.md`, `docs/STORE_LISTING.md`, `PRODUCT.md`, `docs/decisions/0017-release-and-distribution.md`, `AGENTS.md`

- [ ] **Step 1: ADR-0035**

```markdown
# 0035 — Anonymous analytics

- Date: 2026-10-23
- Status: Accepted. Supersedes ADR-0017 §3 (privacy stance).
- Spec: docs/superpowers/specs/2026-10-23-analytics-design.md
- Evaluation: docs/success/2026-10-23-evaluation-playbook.md

## Context
ADR-0034 needs to know whether players return after day 2, and the chant check needs calibration
from real devices (ADR-0018). The Play Console shows neither.

## Decision
Release builds send 8 typed, bucketed events (app_open, ritual_start, mic_permission,
direction_locked, line_result, ritual_end, demon_escaped, device_check) to TelemetryDeck (EU), with
a random app-instance ID hashed by the SDK. On by default; the player turns it off in Settings
("Share anonymous usage data"). No audio, text, location or personal data; debug builds log to
Logcat and send nothing. The vendor sits behind `Analytics`; moving to Firebase with AdMob (v1.1)
is one class.

## Consequences
- `INTERNET` permission; privacy policy and Play Data safety updated; no consent banner (no
  advertising or device IDs).
- The app ID comes from `SUMMON_TELEMETRYDECK_APP_ID` (environment or local.properties).
```

- [ ] **Step 2: ADR-0017** — under §3 add: "*Superseded 2026-10-23 by ADR-0035 (anonymous analytics).*"; Status line: `; §3 superseded by ADR-0035`.

- [ ] **Step 3: PRIVACY_POLICY.md** — replace the "no data collected" section with: what is collected (the 8 events and their values as in the spec table; app version, OS version, device model and locale added by TelemetryDeck; a random app-instance ID, hashed), what is never collected (audio — analysed in memory only; text; location; contacts; account or advertising IDs), the processor (TelemetryDeck GmbH, EU), purpose (improving the game: whether rituals work and whether players return), how to turn it off (Settings → "Share anonymous usage data"), and deletion (clearing the app's data or uninstalling removes the ID; stored data per TelemetryDeck's retention). Update the effective date to the release date.

- [ ] **Step 4: STORE_LISTING.md** — the Data safety section: **Data collected:** App activity → App interactions; Device or other IDs (random app-instance ID). For each: collected, **not shared**, **processed ephemerally: no**, **required: no (users can turn it off)**, purpose **Analytics**, not linked to the user's identity; **data encrypted in transit: yes**; **users can request deletion:** by clearing the app's data. Note: re-check each answer against the Play Console form wording when filling it in.

- [ ] **Step 5: PRODUCT.md** — Privacy row: "No ads, no accounts. Anonymous usage events (ritual funnel, daily activity) are sent unless the player turns them off in Settings; the mic and heading sensor run only while the Ritual is in the foreground and audio never leaves the device (ADR-0035, docs/PRIVACY_POLICY.md)." Remove "no network access, no data collection".

- [ ] **Step 6: AGENTS.md** — Current state: add "Anonymous analytics (ADR-0035): `platform/analytics/`; debug builds log events to Logcat (tag `Analytics`); success evaluation in `docs/success/`."

- [ ] **Step 7: Commit** — `docs(adr): ADR-0035 anonymous analytics; privacy policy, Data safety, PRODUCT`.

---

### Task 8: Pre-flight against TelemetryDeck (operator steps)

Needs a TelemetryDeck account (the operator creates the app and gets its app ID).

- [ ] **Step 1:** Put `SUMMON_TELEMETRYDECK_APP_ID=<id>` and `SUMMON_TELEMETRYDECK_TEST_MODE=true` in `local.properties`.
- [ ] **Step 2:** `./gradlew -q installRelease` (needs the upload key; otherwise use `bundleRelease` + internal track). Run one ritual; in TelemetryDeck (test mode) the signals appear within ~10 minutes with the app version.
- [ ] **Step 3:** Turn the switch off; run a ritual; no new signals.
- [ ] **Step 4:** Set `SUMMON_TELEMETRYDECK_TEST_MODE=false` before building the release bundle.
- [ ] **Step 5:** Set up the dashboard insights listed in the playbook's checklist A.

## Self-review

- Spec: Decisions 1–5 → Tasks 2, 5, 6 (vendor behind interface, opt-out default on, debug Logcat, 8 events, instance ID); Architecture → Tasks 1–6; Events table → Tasks 2–4, 6 (app_open, device_check); Settings → Task 5; Privacy and store → Tasks 6–7; Testing → Tasks 1–4, 6 Step 10, 8.
- Names: `Buckets`, `AnalyticsEvent.{AppOpen, RitualStart, MicPermission, DirectionLocked, LineResult, RitualEnd, DemonEscaped, DeviceCheck}`, `Analytics`, `NoAnalytics`, `GatedAnalytics`, `LogAnalytics`, `TelemetryDeckAnalytics`, `RecordingAnalytics`, `AnalyticsSettings.{enabled, instanceId}`, `ProgressRules.newlyEscaped`, `RitualViewModel.{onPermissionAnswer, clearForTest}`, `AppGraph.{analytics, analyticsSettings, reportOpen}`.
- External API (TelemetryDeck builder and `signal`) is the one unverified surface: confined to `TelemetryDeckAnalytics.kt`, checked against the pinned README in Task 6.
