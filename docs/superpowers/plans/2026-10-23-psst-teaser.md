# "Psst" Teaser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A balloon by a bound demon's head ("Psst… come closer.") that answers taps with "Heh heh heh… Come back soon… if you dare, mortal." and reports `teaser_shown` / `teaser_tap`, to measure interest in new demon content.

**Architecture:** Eligibility is a pure rule (`ProgressRules.teaserEligible`, with a new `DemonRecord.boundDay`); the two events extend `AnalyticsEvent`; `GameViewModel` dedupes and reports; a `PsstBalloon` composable draws and animates; `GrimoireScreen` places it on the current page and routes taps.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-23-psst-teaser-design.md`. **Prerequisite:** `docs/superpowers/plans/2026-10-23-analytics.md` Tasks 1–7 merged (`platform/analytics/` exists). **Branch:** `fix/pre-release-ui`.

---

## Conventions

- `…/safely/` = `app/src/main/java/com/summondemons/safely/`; tests in `app/src/test/java/com/summondemons/safely/`.
- Tests: `./gradlew -q testDebugUnitTest` (exit 0). Strings in 5 languages, none longer than English.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` (a Gemini executor uses its own line).

---

### Task 1: `boundDay` and eligibility (test first)

**Files:** Modify `…/safely/progress/Progress.kt`, `…/safely/progress/ProgressRules.kt`; Test `progress/ProgressRulesTest.kt`, `progress/ProgressJsonTest.kt`

- [ ] **Step 1: Failing tests** — append to `ProgressRulesTest` (static-import `teaserEligible`):

```kotlin
    @Test
    fun commitRecordsTheBoundDay() {
        assertEquals(10L, commitSummon(Progress(), "asmodeus", 10, flawless = false).record("asmodeus").boundDay)
    }

    @Test
    fun theTeaserWaitsForTheNextDayAndForTheLoreToBeRead() {
        var p = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        assertFalse(teaserEligible(p.record("asmodeus"), today = 11)) // lore still unread
        p = consumeReveal(consumeReveal(p, "asmodeus", Reveal.Codex), "asmodeus", Reveal.Battle)
        assertFalse(teaserEligible(p.record("asmodeus"), today = 10)) // summoned today
        assertTrue(teaserEligible(p.record("asmodeus"), today = 11))
        assertFalse(teaserEligible(DemonRecord(), today = 11)) // unbound
        assertTrue(teaserEligible(DemonRecord(bound = true, revealed = true), today = 11)) // old save, no boundDay
    }
```

and to `ProgressJsonTest`:

```kotlin
    @Test
    fun oldSavesWithoutBoundDayStillLoad() {
        val json = """{"demons":{"asmodeus":{"bound":true,"revealed":true}}}"""
        assertEquals(null, ProgressJson.decode(json).record("asmodeus").boundDay)
    }
```

(Use the decode function `ProgressJsonTest` already uses; if it is named differently, call that.)

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*ProgressRulesTest*' --tests '*ProgressJsonTest*'` → FAIL.

- [ ] **Step 3: Implement** — `DemonRecord` gains, after `escapeBannerPending`:

```kotlin
    /** Local epoch day of the last summon; null in saves from before 2026-10-23 (ADR-0036). */
    val boundDay: Long? = null,
```

In `commitSummon`'s `old.copy(…)` add `boundDay = day,`. In `object ProgressRules`:

```kotlin
    /** The "Psst" balloon shows (ADR-0036): bound since an earlier day, lore read. */
    fun teaserEligible(record: DemonRecord, today: Long): Boolean =
        record.bound && (record.boundDay ?: Long.MIN_VALUE) < today && !dossierUnread(record)
```

- [ ] **Step 4:** same command → exit 0. Then the full suite `./gradlew -q testDebugUnitTest` → exit 0 (the `commitSetsEveryField` test expects the full record: add `boundDay = 50` to its expected `DemonRecord`).
- [ ] **Step 5: Commit** — `feat(progress): boundDay and the teaser eligibility rule`.

---

### Task 2: Teaser events (test first)

**Files:** Modify `…/safely/platform/analytics/AnalyticsEvent.kt`; Test `platform/analytics/AnalyticsEventTest.kt`

- [ ] **Step 1: Failing test** — append to `AnalyticsEventTest`:

```kotlin
    @Test
    fun teaserEvents() {
        assertEquals(
            "teaser_shown" to mapOf("demon" to "asmodeus", "days_since_summon" to "3-6"),
            AnalyticsEvent.TeaserShown("asmodeus", 4).let { it.name to it.params() },
        )
        assertEquals(
            "teaser_tap" to mapOf("demon" to "astaroth", "target" to "demon", "repeat" to "true"),
            AnalyticsEvent.TeaserTap("astaroth", onDemon = true, repeat = true).let { it.name to it.params() },
        )
    }
```

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*AnalyticsEventTest*'` → FAIL.
- [ ] **Step 3: Implement** — inside `sealed interface AnalyticsEvent`:

```kotlin
    data class TeaserShown(val demon: String, val daysSinceSummon: Long) : AnalyticsEvent {
        override val name = "teaser_shown"
        override fun params() = mapOf("demon" to demon, "days_since_summon" to Buckets.daysSinceInstall(daysSinceSummon))
    }

    data class TeaserTap(val demon: String, val onDemon: Boolean, val repeat: Boolean) : AnalyticsEvent {
        override val name = "teaser_tap"
        override fun params() = mapOf("demon" to demon, "target" to if (onDemon) "demon" else "balloon", "repeat" to repeat.toString())
    }
```

- [ ] **Step 4:** same command → exit 0.
- [ ] **Step 5: Commit** — `feat(analytics): teaser_shown and teaser_tap`.

---

### Task 3: Reporting in GameViewModel (test first)

**Files:** Modify `…/safely/ui/GameViewModel.kt`; Test `ui/GameViewModelTest.kt`

- [ ] **Step 1: Failing test** — append to `GameViewModelTest`:

```kotlin
    @Test
    fun theTeaserIsReportedOncePerDayAndRepeatsAreMarked() = runTest(dispatcher) {
        val analytics = RecordingAnalytics()
        val vm = vm(InMemoryProgressStore(boundOn(day0 - 2)), analytics = analytics)
        observe(vm); runCurrent()
        vm.teaserShown("asmodeus"); vm.teaserShown("asmodeus")
        vm.teaserTapped("asmodeus", onDemon = false); vm.teaserTapped("asmodeus", onDemon = true)
        assertEquals(
            listOf(
                AnalyticsEvent.TeaserShown("asmodeus", 2),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = false, repeat = false),
                AnalyticsEvent.TeaserTap("asmodeus", onDemon = true, repeat = true),
            ),
            analytics.events.filter { it is AnalyticsEvent.TeaserShown || it is AnalyticsEvent.TeaserTap },
        )
    }
```

(`boundOn` uses `commitSummon`, so `boundDay = day0 - 2`.)

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*GameViewModelTest*'` → FAIL.
- [ ] **Step 3: Implement** in `GameViewModel`:

```kotlin
    // "Psst" teaser (ADR-0036): shown once per demon per day; taps marked as repeats within the day.
    private val teaserShownOn = mutableSetOf<String>()
    private val teaserTappedOn = mutableSetOf<String>()

    fun teaserShown(id: String) {
        val day = today()
        if (!teaserShownOn.add("$id@$day")) return
        val since = stored.value?.record(id)?.boundDay?.let { day - it } ?: 1L
        analytics.track(AnalyticsEvent.TeaserShown(id, since))
    }

    fun teaserTapped(id: String, onDemon: Boolean) {
        val repeat = !teaserTappedOn.add("$id@${today()}")
        analytics.track(AnalyticsEvent.TeaserTap(id, onDemon, repeat))
    }
```

- [ ] **Step 4:** same command → exit 0.
- [ ] **Step 5: Commit** — `feat(grimoire): Report the teaser (once per demon per day)`.

---

### Task 4: The balloon

**Files:** Create `…/safely/ui/components/PsstBalloon.kt`; Modify `…/safely/ui/theme/Color.kt`, `…/safely/ui/theme/DemonColors.kt`, five `strings.xml`

- [ ] **Step 1: Strings** (in `strings.xml` near the quote strings):

| Key | en | es | pt-BR | it | fr |
|---|---|---|---|---|---|
| `psst_invite` | `Psst… come closer.` | `Psst… acércate.` | `Psiu… chegue perto.` | `Psst… avvicinati.` | `Psst… approche.` |
| `psst_reply` | `Heh heh heh… Come back soon… if you dare, mortal.` | `Je je je… Vuelve pronto… si te atreves, mortal.` | `Hehehe… Volte logo… se tiver coragem, mortal.` | `Eh eh eh… Torna presto… se osi, mortale.` | `Hé hé hé… Reviens vite… si tu l’oses, mortel.` |

Run `./gradlew -q testDebugUnitTest --tests '*TranslationResourcesTest*'` → exit 0 (shorten a translation if needed, never English). Add both keys to `docs/i18n/review.md` ("new (2026-10-23)").

- [ ] **Step 2: Colours** — `Palette`: `val psstViolet = Color(0xFF8F3FB0)`, `val psstVioletDeep = Color(0xFF4A1764)`, `val psstHalo = Color(0xFF6BFF9F)`; `DemonColors` gains `psstViolet`, `psstVioletDeep`, `psstHalo` (fields and `DefaultDemonColors` entries), with the KDoc "The Psst balloon (ADR-0036)".

- [ ] **Step 3: `PsstBalloon.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summondemons.safely.ui.fx.attentionGlow
import com.summondemons.safely.ui.fx.rememberAttention
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles

private val BalloonMaxWidth = 128.dp
private val Tail = 12.dp
private val Rim = 2.dp
private val TextSize = 13.sp
private val TextMinSize = 8.sp

/**
 * The "Psst" balloon (ADR-0036): [text] in a violet balloon with a gold rim and a tail at its
 * bottom start (toward the demon), breathing with the attention glow and a faint green halo.
 */
@Composable
fun PsstBalloon(text: String, replying: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val gold = MaterialTheme.colorScheme.primary
    val breath = rememberAttention(active = !replying)
    val size = TextSize.fontScaleCapped()
    Box(
        modifier
            .widthIn(max = BalloonMaxWidth)
            .attentionGlow(breath, colors.psstHalo)
            .attentionGlow(breath, colors.psstViolet)
            .drawBehind {
                val tail = Tail.toPx()
                val body = Size(size.width, size.height - tail)
                val radius = body.height / 2f
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, body.width, body.height, radius, radius))
                    moveTo(radius * 0.6f, body.height - 1f)
                    lineTo(radius * 0.2f, size.height)
                    lineTo(radius * 1.4f, body.height - 1f)
                    close()
                }
                drawPath(path, Brush.radialGradient(listOf(colors.psstViolet, colors.psstVioletDeep), Offset(body.width * 0.4f, body.height * 0.35f), body.width))
                drawPath(path, gold, style = Stroke(Rim.toPx()))
            }
            .clickable(role = Role.Button, onClick = onTap)
            .semantics { if (replying) liveRegion = LiveRegionMode.Polite }
            .padding(start = MaterialTheme.spacing.md, end = MaterialTheme.spacing.md, top = MaterialTheme.spacing.sm, bottom = MaterialTheme.spacing.sm + Tail),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.textRoles.name.copy(fontSize = size, textAlign = TextAlign.Center),
            color = colors.inkHighlight,
            maxLines = 3,
            autoSize = TextAutoSize.StepBased(minFontSize = TextMinSize, maxFontSize = size),
        )
    }
}
```

- [ ] **Step 4:** `./gradlew -q assembleDebug` → exit 0. Do not commit yet.

---

### Task 5: Place it and route taps

**Files:** Modify `…/safely/ui/screens/GrimoireScreen.kt`, `…/safely/navigation/AppNavHost.kt`

- [ ] **Step 1: GrimoireScreen parameters** — add `onTeaserShown: (String) -> Unit = {}` and `onTeaserTap: (String, Boolean) -> Unit = { _, _ -> }` to `GrimoireScreen` (defaults keep the preview compiling).

- [ ] **Step 2: Eligibility per page** — in the pager page, after `val sulks = …`:

```kotlin
            val teaser = current && party == null && ProgressRules.teaserEligible(r, state.today)
            if (teaser) LaunchedEffect(d.id, state.today) { onTeaserShown(d.id) }
```

and pass `teaser = teaser` and `onTeaserTap = { onDemon -> onTeaserTap(d.id, onDemon) }` to `Stage`.

- [ ] **Step 3: Stage** — new parameters `teaser: Boolean` and `onTeaserTap: (onDemon: Boolean) -> Unit`. Inside `Stage`, before the `Column`:

```kotlin
    var replyTick by remember(demon.id) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val sounds = LocalGameSounds.current
    val tapTeaser: (Boolean) -> Unit = { onDemon ->
        onTeaserTap(onDemon)
        Haptics.chime(context)
        sounds?.playLaughIfPresent()
        replyTick++
    }
    var replying by remember(demon.id) { mutableStateOf(false) }
    LaunchedEffect(replyTick) {
        if (replyTick == 0) return@LaunchedEffect
        replying = true
        delay(REPLY_MS)
        replying = false
    }
```

In the `DemonCutout`'s `clickable`, change to:

```kotlin
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !bound || party || teaser,
                                    onClick = { if (teaser && !party) tapTeaser(true) else onPrimary() },
                                )
```

Inside the aspect-ratio `Box` (after `SharedDemonArt { … }`), add:

```kotlin
                if (teaser) {
                    PsstBalloon(
                        text = stringResource(if (replying) R.string.psst_reply else R.string.psst_invite),
                        replying = replying,
                        onTap = { tapTeaser(false) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = spacing.sm),
                    )
                }
```

Constant: `private const val REPLY_MS = 3_000L`. Imports: `PsstBalloon`, `Haptics`, `LocalContext`, `mutableIntStateOf` (already), `delay` (already).

- [ ] **Step 4: The laugh hook** — in `…/safely/platform/GameSounds.kt` add:

```kotlin
    /** The Psst reply's laugh (ADR-0036), only if `res/raw/demon_laugh` was added. */
    fun playLaughIfPresent() {
        laughId?.let { pool.play(it, LAUGH_VOLUME, LAUGH_VOLUME, 1, 0, 1f) }
    }
```

with, in the class body:

```kotlin
    private val laughId: Int? = context.resources.getIdentifier("demon_laugh", "raw", context.packageName)
        .takeIf { it != 0 }?.let { pool.load(context.applicationContext, it, 1) }
```

and `private const val LAUGH_VOLUME = 0.7f` in its companion (or top-level). Without the file, `laughId` is null and nothing plays. (Add `@SuppressLint("DiscouragedApi")` on the property for `getIdentifier`.)

- [ ] **Step 5: AppNavHost** — pass `onTeaserShown = game::teaserShown` and `onTeaserTap = game::teaserTapped` to `GrimoireScreen(…)` (use the view-model variable name used there).

- [ ] **Step 6: Build, tests, device**

`./gradlew -q testDebugUnitTest && ./gradlew -q installDebug` → exit 0. On a device (debug build, Logcat tag `Analytics`): Settings → Debug controls → Bind a demon, then "Advance +1 day" so his summon was yesterday; open the Grimoire on him and flip the card once if the flip button glows (reads the lore). Expected: the balloon "Psst… come closer." top right of his art, breathing; `teaser_shown` once; tapping it shows the reply for 3 s with a haptic and logs `teaser_tap {target=balloon, repeat=false}`; tapping him logs `target=demon, repeat=true`. Font scale 2.0: the balloon text stays inside (shrinks). Restore font scale 1.0.

- [ ] **Step 7: Commit** (Tasks 4–5) — `feat(grimoire): "Psst" balloon fake-door test (ADR-0036)`.

---

### Task 6: Docs

- [ ] **Step 1: ADR-0036** `docs/decisions/0036-psst-fake-door.md`:

```markdown
# 0036 — "Psst": a fake-door test for new demon content

- Date: 2026-10-23
- Status: Accepted
- Spec: docs/superpowers/specs/2026-10-23-psst-teaser-design.md

## Context
ADR-0034's v1.1 depends on a new reward from bound demons (lore over days, daily lines). Before
building it, v1 measures whether players want more from them.

## Decision
From the day after a summon, once his lore is read, a bound demon shows a balloon "Psst… come
closer."; tapping it (or him) answers "Heh heh heh… Come back soon… if you dare, mortal." with a
haptic (and a laugh if a CC0 clip is added). `teaser_shown` and `teaser_tap` measure interest; the
playbook's rule decides v1.1's lever.

## Consequences
- Tapping a bound demon now does something (PRODUCT.md updated).
- `DemonRecord.boundDay` is stored from now on; old saves load (null counts as earlier).
- When v1.1 ships real content, the balloon becomes its entry point.
```

- [ ] **Step 2: PRODUCT.md** — Summon row: replace "Tapping a bound demon does nothing." with "Tapping a bound demon does nothing, except while the "Psst" balloon shows (ADR-0036)." Add a row **Psst** with the spec's Behaviour in one paragraph.

- [ ] **Step 3: DESIGN.md** — component contract for `PsstBalloon` (colours `psstViolet`, `psstVioletDeep`, `psstHalo`; glow; text rule) and, in Gaps, a Stitch prompt for optional hand-drawn balloon art.

- [ ] **Step 4: Playbook** — in `docs/success/2026-10-23-evaluation-playbook.md`: Definitions gains "**Teaser tap rate**" and "**Return tappers**" (spec Measurement); Targets gains a row *Interest | Teaser tap rate | ≥ 20 % | < 5 %*; Decision rules gains the spec's three-way rule; checklist A gains "a teaser insight (shown → tap funnel) is saved".

- [ ] **Step 5: Commit** — `docs(adr): ADR-0036 Psst fake-door test; PRODUCT, DESIGN, playbook`.

## Self-review

- Spec Behaviour → Tasks 1, 4, 5; Measurement → Tasks 2, 3, 6; Model → Task 1; Laugh → Task 5 Step 4; Strings → Task 4; Accessibility → Task 4 (`Role.Button`, live region).
- Names: `DemonRecord.boundDay`, `ProgressRules.teaserEligible`, `AnalyticsEvent.TeaserShown/TeaserTap`, `GameViewModel.teaserShown/teaserTapped`, `PsstBalloon`, `GameSounds.playLaughIfPresent`, `REPLY_MS`.
