# Final-pass review fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix every confirmed finding of the three final-pass reviews (spec
`docs/superpowers/specs/2026-10-23-final-pass-fixes-design.md`), each verified on the device.

**Architecture:** Small, local edits in existing Compose files; one new pure helper
(`smoothKeyframe`, unit-tested), one modifier (`focusRing`) and one vector drawable. No new player
strings, no new dependencies. The repo has no Compose UI tests: behaviour is checked with
`uiautomator dump`, screenshots and the existing unit tests.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), JUnit 4, adb / UI Automator, `tools/ui_matrix.sh`.

**Paths:** `UI` below means `app/src/main/java/com/summondemons/safely/ui`. Line numbers are at
commit `95afa99`; they shift as you edit, so match on the quoted code, not the number.

**Evidence folder:** put every dump and manual screenshot under `build/final-pass/` (git-ignored
because it is under `build/`). Create it once: `mkdir -p build/final-pass`.

**Device helpers** (S24 on adb):

```bash
# Screenshot the current screen
adb exec-out screencap -p > build/final-pass/<name>.png
# Dump the semantics tree of the current screen
adb shell uiautomator dump /sdcard/ui.xml >/dev/null && adb pull /sdcard/ui.xml build/final-pass/<name>.xml >/dev/null
# Install the current debug build
./gradlew -q installDebug
```

Reduce motion is the system "Remove animations" setting: `adb shell settings put global
transition_animation_scale 0` (restore with `1`). Font scale: `adb shell settings put system
font_scale 2.0` (restore with `1.0`). If anything leaves the density changed:
`adb shell wm density 420`.

---

## File map

| File | Tasks |
|---|---|
| `UI/screens/ChantPieces.kt` | 1, 4, 8 |
| `UI/components/RotatingSunburstBackground.kt` | 1 |
| `UI/components/FlipCard.kt` | 1 |
| `UI/screens/HomeScreen.kt` | 2, 7 |
| `UI/screens/GrimoireScreen.kt` | 2, 7, 8 |
| `UI/screens/ChantIntro.kt` | 3, 7, 8 |
| `UI/components/TitlePlaque.kt`, `UI/components/DemonCard.kt`, `UI/components/SettingsSheet.kt`, `UI/screens/DebugSheet.kt` | 5 |
| `UI/components/Dossier.kt` | 6, 8 |
| `UI/components/PrimaryActionButton.kt` | 6 |
| `UI/components/CueChip.kt`, `UI/components/QuoteSlot.kt` | 7, 8 |
| `UI/screens/ChantArrangements.kt` | 7 |
| Create `UI/components/FocusRing.kt`, `app/src/main/res/drawable/ic_arrow_back.xml` | 7 |
| `UI/fx/FxMath.kt`, `app/src/test/java/com/summondemons/safely/ui/fx/FxMathTest.kt`, `UI/components/RestlessGrimoire.kt`, `UI/screens/RitualScreen.kt`, `UI/components/Badges.kt` | 8 |
| `docs/superpowers/specs/2026-10-23-final-pass-fixes-design.md`, `DESIGN.md`, `TODO.md` | 9 |

---

## Task 0: Baseline

- [ ] **Step 1:** `git status --short` shows no modified tracked files. If it does, stop and report.
- [ ] **Step 2:** `./gradlew -q testDebugUnitTest` exits 0. If not, stop and report the failure.
- [ ] **Step 3:** `mkdir -p build/final-pass`; if `adb devices` lists a device, `./gradlew -q installDebug`
  and take "before" screenshots for Tasks 3, 4 and 5: the intro card (`before-intro.png`), a
  paused chant (turn away from the heading while chanting: `before-chant-paused.png`), and the
  Grimoire front at font 2.0 (`before-grimoire-font2.png`).

---

## Task 1: Read animated values in the draw phase

Findings R1-3..6 / R2-1..5, R2-14 (part). An infinite transition's `.value` read in a composable
body recomposes that composable every frame; reading it inside `graphicsLayer {}` or a draw
lambda only redraws.

**Files:** `UI/screens/ChantPieces.kt`, `UI/components/RotatingSunburstBackground.kt`, `UI/components/FlipCard.kt`

- [ ] **Step 1: Confirm the four sites**

Run: `grep -n '^\s*)\.value' app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt`
Expected: four lines (330, 404, 439, 655).

- [ ] **Step 2: `SealMark`** — replace the `breath` block and its use.

```kotlin
    val breath = if (current && !paused && !reduceMotion) {
        rememberInfiniteTransition(label = "seal").animateFloat(
            1f, BREATH_MIN, infiniteRepeatable(tween(if (checking) BREATH_HALF_MS / 3 else BREATH_HALF_MS), RepeatMode.Reverse), label = "sealBreath",
        )
    } else {
        null
    }
```

and in the `Box` modifier:

```kotlin
        modifier.graphicsLayer {
            alpha = breath?.value ?: 1f
            scaleX = pulse.value
            scaleY = pulse.value
        },
```

- [ ] **Step 3: `DirectionPillView`** — same pattern.

```kotlin
    val blink = if (pill.look == PillLook.Paused && !reduceMotion) {
        rememberInfiniteTransition(label = "pill").animateFloat(
            1f, BLINK_MIN, infiniteRepeatable(tween(BLINK_HALF_MS), RepeatMode.Reverse), label = "pillBlink",
        )
    } else {
        null
    }
```

and `.graphicsLayer { alpha = blink?.value ?: 1f }`.

- [ ] **Step 4: `EdgeChevrons`** — the phase becomes nullable and is read inside the Canvas.

```kotlin
    val phase = if (LocalReduceMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "chevrons").animateFloat(
            0f, 1f, infiniteRepeatable(tween(CHEVRON_CYCLE_MS, easing = LinearEasing)), label = "chevronPhase",
        )
    }
    val right = turn == TurnDirection.Right
    Canvas(modifier.size(ChevronWidth, ChevronHeight)) {
        val p = phase?.value ?: -1f
```

and inside the `repeat`, use `p` instead of `phase`:

```kotlin
            val alpha = if (p < 0f) 1f else {
                val d = ((p * count - k) % count + count) % count
                lerp(1f, CHEVRON_MIN_ALPHA, (d / count * 2f).coerceAtMost(1f))
            }
```

- [ ] **Step 5: `VoiceMeter` / `CheckingMeter`** — split the drawing so the level is read while drawing.

```kotlin
/** The meter pulsing on its own while the phone checks what was said. */
@Composable
private fun CheckingMeter(color: Color) {
    val level = if (LocalReduceMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "checking").animateFloat(
            METER_FLOOR, 1f, infiniteRepeatable(tween(CHECKING_HALF_MS), RepeatMode.Reverse), label = "checkingLevel",
        )
    }
    MeterBars({ level?.value ?: CHECKING_LEVEL }, color, Modifier.size(MeterWidth, MeterHeight))
}

/** Five ember bars that follow the mic level. Shows that the mic hears you; decides nothing. */
@Composable
fun VoiceMeter(level: Float, color: Color, modifier: Modifier = Modifier) {
    val animated = animateFloatAsState(level.coerceIn(0f, 1f), tween(METER_TWEEN_MS), label = "meter")
    MeterBars({ animated.value }, color, modifier)
}

/** The bars; [level] is read while drawing, so a moving level redraws without recomposing. */
@Composable
private fun MeterBars(level: () -> Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val l = level()
        val bar = size.width / (METER_HEIGHTS.size * 2 - 1)
        METER_HEIGHTS.forEachIndexed { i, h ->
            val height = size.height * (METER_FLOOR + l * h * (1f - METER_FLOOR))
            drawRoundRect(
                color,
                topLeft = Offset(i * 2 * bar, (size.height - height) / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}
```

If `getValue` is now unused in the file, the compiler warns; remove that import only if so.

- [ ] **Step 6: Sunburst** — replace the whole body of `UI/components/RotatingSunburstBackground.kt` after the imports:

```kotlin
private const val ROTATION_MS = 28_000

/** Rays behind the summoned demon; the layer turns (no recomposition), the beam paths are built once per size. */
@Composable
fun RotatingSunburstBackground(color: Color, modifier: Modifier = Modifier, beamCount: Int = 16, rotating: Boolean = true) {
    val angle = if (rotating) {
        rememberInfiniteTransition(label = "sunburst").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(ROTATION_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "sunburstRotation",
        )
    } else {
        null
    }
    Spacer(
        modifier
            .graphicsLayer { rotationZ = angle?.value ?: 0f }
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = max(size.width, size.height) * 1.2f
                val bounds = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
                val step = 360f / beamCount
                val beams = (0 until beamCount step 2).map { i ->
                    Path().apply {
                        moveTo(center.x, center.y)
                        arcTo(bounds, startAngleDegrees = i * step, sweepAngleDegrees = step, forceMoveTo = false)
                        close()
                    }
                }
                onDrawBehind { beams.forEach { drawPath(it, color = color) } }
            },
    )
}
```

Imports: add `androidx.compose.foundation.layout.Spacer`, `androidx.compose.ui.draw.drawWithCache`,
`androidx.compose.ui.graphics.graphicsLayer`; remove `androidx.compose.foundation.Canvas`,
`androidx.compose.runtime.getValue`, `androidx.compose.ui.graphics.drawscope.rotate`.

- [ ] **Step 7: FlipCard** — the face switch reads a derived boolean.

```kotlin
    val angle = animateFloatAsState(if (side == CardSide.Front) 0f else 180f, tween(FLIP_MS), label = "cardFlip")
    val showFront by remember { derivedStateOf { angle.value <= 90f } }
    val density = LocalDensity.current.density
    Box(
        modifier.graphicsLayer {
            rotationY = angle.value
            cameraDistance = CAMERA_DISTANCE * density
        },
    ) {
        if (showFront) {
```

Imports: add `androidx.compose.runtime.derivedStateOf`, `androidx.compose.runtime.remember`.

- [ ] **Step 8: Verify**

Run: `grep -n '^\s*)\.value' app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt` → no output.
Run: `./gradlew -q testDebugUnitTest` → exit 0.
Device: install; on the chant screen the seal breathes, the paused pill blinks, the chevrons wave,
the checking meter pulses; with reduce motion on, all four are still. Grimoire: the card flips.
Note what you saw in the report.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt \
  app/src/main/java/com/summondemons/safely/ui/components/RotatingSunburstBackground.kt \
  app/src/main/java/com/summondemons/safely/ui/components/FlipCard.kt
git commit -m "perf(ui): Read looping animation values while drawing, not composing"
```

---

## Task 2: Back is consistent

Findings R3-1, R3-2.

**Files:** `UI/screens/HomeScreen.kt`, `UI/screens/GrimoireScreen.kt`

- [ ] **Step 1: Escape banner** — first line inside `EscapeBanner`'s body:

```kotlin
private fun BoxScope.EscapeBanner(escaped: List<Demon>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val colors = MaterialTheme.colorScheme
```

Import `androidx.activity.compose.BackHandler`.

- [ ] **Step 2: Grimoire header `←`** — replace the comment and call (≈ line 379–380):

```kotlin
            // Like system back (the BackHandlers above): dismiss the celebration, else turn the card to its front, else leave.
            GrimoireHeader(
                {
                    when {
                        party != null -> onDismissCelebration()
                        side == CardSide.Back -> side = CardSide.Front
                        else -> onBack()
                    }
                },
                onMenu,
            )
```

- [ ] **Step 3: Verify on device**
  1. Debug panel: make a bound demon escape (or advance the clock until one does), open the app on
     Home with the banner up, press system Back (`adb shell input keyevent KEYCODE_BACK`): the banner
     closes, the app stays on Home. Screenshot `task2-home-after-back.png`.
  2. Grimoire, flip the card to the dossier, tap `←`: the front shows (`task2-front.png`); tap `←`
     again: Home.
- [ ] **Step 4:** `./gradlew -q testDebugUnitTest` → exit 0.
- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/HomeScreen.kt app/src/main/java/com/summondemons/safely/ui/screens/GrimoireScreen.kt
git commit -m "fix(nav): Back dismisses the escape banner; the header arrow turns the card first"
```

---

## Task 3: Chant intro ✕ position and Rule 3 icon

Findings R3-3, R3-4.

**Files:** `UI/screens/ChantIntro.kt`

- [ ] **Step 1: ✕ inset** — in the `showClose` block:

```kotlin
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(spacing.lg)) {
```

- [ ] **Step 2: `LinesIcon`** — replace the function and pass the accent at its call site
(`{ LinesIcon(accent) }` in Rule 3):

```kotlin
/** What the line shows: a green line under the words heard, the accent line with its halo under the current one. */
@Composable
private fun LinesIcon(accent: Color) {
    val safe = MaterialTheme.demonColors.ritualSafe
    Canvas(Modifier.size(RuleIcon, RuleIcon * 0.7f)) {
        val w = size.height * 0.12f
        val y = size.height * 0.5f
        drawLine(safe, Offset(0f, y), Offset(size.width * 0.42f, y), w, StrokeCap.Round)
        drawLine(accent.copy(alpha = LINES_HALO_ALPHA), Offset(size.width * 0.55f, y), Offset(size.width, y), w * 3f, StrokeCap.Round)
        drawLine(accent, Offset(size.width * 0.55f, y), Offset(size.width, y), w, StrokeCap.Round)
    }
}

private const val LINES_HALO_ALPHA = 0.3f
```

Remove imports the file no longer uses (`Path`, `Stroke`) only if the compiler reports them unused.

- [ ] **Step 3: Verify**
  - `grep -n "colorScheme.error" app/src/main/java/com/summondemons/safely/ui/screens/ChantIntro.kt` → no output.
  - Device: open a ritual; on the intro, dump `task3-intro.xml` and screenshot `task3-intro.png`; tap ✕;
    on the chant screen dump `task3-chant.xml`. Find the close node in both
    (`grep -o 'content-desc="[^"]*"[^>]*bounds="[^"]*"'` and match the `intro_close` / chant close
    description). Each edge of the two `bounds` differs by ≤ 6 px. If not, adjust the intro's padding
    until it does and record the change under Deviations.
- [ ] **Step 4:** `./gradlew -q testDebugUnitTest` → exit 0.
- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantIntro.kt
git commit -m "fix(intro): The close button sits where the chant screen's does; Rule 3 shows the real line marks"
```

---

## Task 4: Paused is amber

Finding R3-5 (ADR-0029 §5).

**Files:** `UI/screens/ChantPieces.kt` (`FeedbackSlot`, `SlotContent.Meter` branch)

- [ ] **Step 1:** Replace the branch:

```kotlin
            SlotContent.Meter -> {
                val tint = if (paused) colors.primary else accent
                Chip(if (paused) colors.primary else colors.outlineVariant) {
                    // Listening (or paused) as a labelled state, never bare bars that read as "•••••".
                    Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = tint, modifier = Modifier.size(MicSize))
                    VoiceMeter(if (paused) 0f else micLevel, tint, Modifier.size(MeterWidth, MeterHeight))
                    Text(
                        stringResource(if (paused) R.string.chant_paused else R.string.chant_listening).upper(),
                        style = style,
                        color = if (paused) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        // Visual only: the slot is a live region, and announcing "Listening" after every
                        // line (or "Paused" next to the pill's own description) would only add noise.
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
```

- [ ] **Step 2: Verify** — device: pause while chanting; screenshot `task4-chant-paused.png`: the slot
chip, mic and PAUSED are amber like the pill. Listening (`task4-chant-listening.png`) unchanged.
- [ ] **Step 3:** `./gradlew -q testDebugUnitTest` → exit 0.
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt
git commit -m "fix(chant): The paused mic chip is amber, like every other paused cue (ADR-0029)"
```

---

## Task 5: Large text and sheets

Findings R3-6, R3-7, R3-8, R1-12.

**Files:** `UI/components/TitlePlaque.kt`, `UI/components/DemonCard.kt`, `UI/components/SettingsSheet.kt`, `UI/screens/DebugSheet.kt`

- [ ] **Step 1: TitlePlaque** — add `private val TitleMinSize = 12.sp` next to `TitleSize`, and:

```kotlin
        Text(
            title.upper(),
            style = MaterialTheme.textRoles.name.copy(fontSize = TitleSize.fontScaleCapped(), letterSpacing = 0.08.em),
            color = gold,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = TitleMinSize, maxFontSize = TitleSize.fontScaleCapped()),
            modifier = Modifier.semantics { heading() },
        )
```

Imports: `androidx.compose.foundation.text.TextAutoSize`, `com.summondemons.safely.ui.theme.fontScaleCapped`.

- [ ] **Step 2: Medallion** (`DemonCard.kt` ≈ line 152):

```kotlin
            Text(demon.number.toString(), style = roles.name.copy(fontSize = NumberSize.fontScaleCapped()), color = metal)
```

Import `com.summondemons.safely.ui.theme.fontScaleCapped` if not present.

- [ ] **Step 3: Settings scrolls** — the sheet's `Column` modifier:

```kotlin
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
```

Imports: `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`.

- [ ] **Step 4: Bottom inset check (decides R1-12)** — device, 3-button navigation
(`adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton`; restore with
`...navbar.gestural`): open Settings, screenshot `task5-settings-3button.png`. If the space under the
last item is more than the 16 dp vertical padding plus the nav bar (i.e. a second nav-bar-height
gap), remove `.navigationBarsPadding()` from both `SettingsSheet.kt` and `DebugSheet.kt`, reinstall,
re-take the screenshot. If there is no double gap, keep it and record "R1-12: no double inset seen"
in the report. Restore gestural navigation.

- [ ] **Step 5: Verify at font 2.0, five languages** — `adb shell settings put system font_scale 2.0`;
for each of en, es, pt-BR, it, fr (Settings → language): open the Grimoire, screenshot
`task5-grimoire-font2-<lang>.png` and dump `task5-grimoire-font2-<lang>.xml`. The plaque text is one
line inside the plaque; its bounds do not intersect the back and menu buttons' bounds. The medallion
numeral has a visible gap to the rim. Open Settings and scroll to the end (`task5-settings-font2.png`).
Restore: `adb shell settings put system font_scale 1.0`, language back to en.
- [ ] **Step 6:** `./gradlew -q testDebugUnitTest` → exit 0 (includes `TranslationResourcesTest`).
- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/components/TitlePlaque.kt \
  app/src/main/java/com/summondemons/safely/ui/components/DemonCard.kt \
  app/src/main/java/com/summondemons/safely/ui/components/SettingsSheet.kt \
  app/src/main/java/com/summondemons/safely/ui/screens/DebugSheet.kt
git commit -m "fix(ui): Cap the plaque title and medallion numeral at large font sizes; Settings scrolls"
```

(Leave `DebugSheet.kt` out of `git add` if Step 4 kept it unchanged.)

---

## Task 6: Reduce-motion gaps

Findings R2-8, R2-13, R1-17 / R2-9.

**Files:** `UI/components/Dossier.kt`, `UI/components/PrimaryActionButton.kt`

- [ ] **Step 1: Secret scroll** (Dossier ≈ line 80):

```kotlin
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(scrollToSecret) {
        if (scrollToSecret) {
            if (reduceMotion) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
        }
    }
```

- [ ] **Step 2: InkLine** — under reduce motion the text is simply there, no blur:

```kotlin
/** Revealed text soaking in once: blur → sharp, staggered 120 ms per line, 900 ms each (§4); at once with reduced motion. */
@Composable
fun InkLine(ink: Boolean, index: Int, content: @Composable () -> Unit) {
    val animate = ink && !LocalReduceMotion.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            delay(index * INK_STAGGER_MS)
            progress.animateTo(1f, tween(INK_MS, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.graphicsLayer { alpha = progress.value }.blur(InkBlur * (1f - progress.value))) { content() }
}
```

Import `com.summondemons.safely.ui.fx.LocalReduceMotion` in `Dossier.kt` if absent.

- [ ] **Step 3: PrimaryActionButton**

```kotlin
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(if (pressed && !reduceMotion) 0.96f else 1f, label = "buttonPress")
```

Import `com.summondemons.safely.ui.fx.LocalReduceMotion`.

- [ ] **Step 4: Verify** — device, reduce motion on: a flawless summon (or the debug panel's
reveal) shows the Secret without blur and jumps to it; pressing a primary button (the ritual's error-panel action or the abort dialog's "Stay") does not shrink it. Reduce
motion off: both animate as before. Record in the report.
- [ ] **Step 5:** `./gradlew -q testDebugUnitTest` → exit 0.
- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/components/Dossier.kt app/src/main/java/com/summondemons/safely/ui/components/PrimaryActionButton.kt
git commit -m "fix(a11y): Reduced motion skips the ink blur, the secret scroll and the button press scale"
```

---

## Task 7: Accessibility semantics

Findings R1-1 (verify), R1-7, R1-8, R1-9, R1-10, R1-11, R1-18.

**Files:** `UI/components/QuoteSlot.kt` (only if Step 1 fails), `UI/components/CueChip.kt`,
`UI/screens/HomeScreen.kt`, `UI/screens/GrimoireScreen.kt`, `UI/screens/ChantIntro.kt`,
`UI/screens/ChantArrangements.kt`; create `UI/components/FocusRing.kt`,
`app/src/main/res/drawable/ic_arrow_back.xml`.

- [ ] **Step 1: R1-1, prove or fix** — device: Grimoire with an unbound demon and the circle ready
("Tap to summon" showing). Dump `task7-summon-cue.xml`. Find the node whose `content-desc` is the
`cd_summon` text. If it has `clickable="true"`: R1-1 is a false positive, change nothing, record it.
Otherwise replace its modifier tail in `SummonCue` with:

```kotlin
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick { onTap(); true }
            },
```

(and the same `onClick { onTap(); true }` in `CoolingCue`), import
`androidx.compose.ui.semantics.onClick`, and re-dump to confirm `clickable="true"`.

- [ ] **Step 2: CueChip without a live region** — in `CueChip.kt` remove
`.semantics { liveRegion = LiveRegionMode.Polite }` from the `Surface` modifier, and the imports
`semantics`, `liveRegion`, `LiveRegionMode` if now unused. Its only caller is Home's hint.

- [ ] **Step 3: Home hint is a button** — in `HomeScreen.kt` the hint's last modifier:

```kotlin
                    .clickable(role = Role.Button, onClick = open),
```

Import `androidx.compose.ui.semantics.Role` if absent.

- [ ] **Step 4: DemonCutout is a button** — in `GrimoireScreen.kt` `Stage`:

```kotlin
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !bound || party,
                                    role = Role.Button,
                                    onClick = onPrimary,
                                )
```

- [ ] **Step 5: Focus ring** — create `UI/components/FocusRing.kt`:

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val FocusRingWidth = 2.dp

/**
 * A gold outline while [interaction] has focus (keyboard, D-pad, Switch Access), for controls whose
 * press feedback is their own art (`indication = null`). Touch never focuses them, so touch shows nothing.
 */
@Composable
fun Modifier.focusRing(interaction: InteractionSource, shape: Shape): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    return if (focused) border(FocusRingWidth, MaterialTheme.colorScheme.primary, shape) else this
}
```

Use it before each `clickable` that shares the interaction source:

`GrimoireScreen.kt` `StageArrow`:

```kotlin
            .focusRing(interaction, MaterialTheme.shapes.medium)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
```

`ChantIntro.kt` `SummonButton` (after `.height(SummonButtonHeight)`):

```kotlin
            .focusRing(interactionSource, CircleShape)
            .clickable(
```

Import `com.summondemons.safely.ui.components.focusRing` (and `CircleShape` in `ChantIntro.kt` if absent).

- [ ] **Step 6: Back arrow icon** — create `app/src/main/res/drawable/ic_arrow_back.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:autoMirrored="true"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
</vector>
```

and in `GrimoireHeader`:

```kotlin
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = back }) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
```

Imports `androidx.compose.material3.Icon`, `androidx.compose.ui.res.painterResource` if absent.

- [ ] **Step 7: Traversal order** — the navigation is read before the content.

`GrimoireScreen.kt`, the overlay column:

```kotlin
        Column(Modifier.fillMaxSize().statusBarsPadding().semantics { isTraversalGroup = true; traversalIndex = -1f }) {
```

`ChantArrangements.kt`, `SplitArrangement`:

```kotlin
            HudRow(parts, Modifier.align(Alignment.TopCenter).semantics { isTraversalGroup = true; traversalIndex = -1f })
```

Imports `androidx.compose.ui.semantics.isTraversalGroup`, `traversalIndex`, `semantics` where absent.

- [ ] **Step 8: Verify**
  - `grep -rn 'Text("←")' app/src/main` → no output.
  - Dumps: `task7-grimoire.xml` (the stage node with the demon's label has `clickable="true"`),
    `task7-home.xml` (hint chip `clickable="true"`).
  - TalkBack (`adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService`;
    turn off afterwards with `adb shell settings delete secure enabled_accessibility_services`): on the Grimoire,
    swipe right from the top: Back, title, menu come before any card content; on the chant screen:
    ?, D, the slot, ✕ before the phrase; Home's hint is not announced unprompted. If TalkBack can't
    be driven, mark this "not run" — do not guess.
  - Keyboard focus: on the Grimoire, `adb shell input keyevent KEYCODE_TAB` until an arrow is focused,
    screenshot `task7-focus-arrow.png` (gold outline); same on the intro Summon (`task7-focus-summon.png`).
- [ ] **Step 9:** `./gradlew -q testDebugUnitTest` → exit 0.
- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui app/src/main/res/drawable/ic_arrow_back.xml
git commit -m "fix(a11y): Buttons announce as buttons, navigation reads first, focus is visible, the back arrow mirrors"
```

---

## Task 8: Cheap polish

Findings R2-10, R2-11, R2-12, R2-18, R1-14 / R3-9, R1-16, R1-19, R3-10.

**Files:** `UI/fx/FxMath.kt`, `app/src/test/java/com/summondemons/safely/ui/fx/FxMathTest.kt`,
`UI/components/RestlessGrimoire.kt`, `UI/screens/ChantPieces.kt`, `UI/screens/RitualScreen.kt`,
`UI/screens/ChantIntro.kt`, `UI/components/Badges.kt`, `UI/components/CueChip.kt`,
`UI/components/Dossier.kt`, `UI/components/QuoteSlot.kt`, `UI/screens/GrimoireScreen.kt`

- [ ] **Step 1: Failing test for `smoothKeyframe`** — add to `FxMathTest`:

```kotlin
    @Test
    fun smoothKeyframeEasesInAndOutOfEachKeyframe() {
        val values = floatArrayOf(0f, -22f, 0f, -6f, 0f)
        // On the keyframes and halfway between them it matches the linear version.
        assertEquals(0f, smoothKeyframe(0f, values), 0.0001f)
        assertEquals(-11f, smoothKeyframe(0.125f, values), 0.0001f)
        assertEquals(-22f, smoothKeyframe(0.25f, values), 0.0001f)
        assertEquals(-3f, smoothKeyframe(0.625f, values), 0.0001f)
        assertEquals(0f, smoothKeyframe(1f, values), 0.0001f)
        // A quarter into the first segment: smoothstep(0.25) = 0.15625, so -22 * 0.15625 (linear: -5.5).
        assertEquals(-3.4375f, smoothKeyframe(0.0625f, values), 0.0001f)
        // Clamped like keyframe().
        assertEquals(0f, smoothKeyframe(-1f, values), 0.0001f)
        assertEquals(0f, smoothKeyframe(2f, values), 0.0001f)
    }
```

- [ ] **Step 2:** Run `./gradlew -q testDebugUnitTest --tests '*FxMathTest*'` → FAIL: unresolved reference `smoothKeyframe`.

- [ ] **Step 3: Implement** in `FxMath.kt`, under `keyframe`:

```kotlin
/** Like [keyframe], but each segment eases in and out (smoothstep): no velocity break at a keyframe. */
fun smoothKeyframe(t: Float, values: FloatArray): Float {
    require(values.size >= 2) { "need at least two keyframes" }
    val x = t.coerceIn(0f, 1f) * (values.size - 1)
    val i = x.toInt().coerceAtMost(values.size - 2)
    val s = x - i
    return values[i] + (values[i + 1] - values[i]) * s * s * (3f - 2f * s)
}
```

- [ ] **Step 4:** Run `./gradlew -q testDebugUnitTest --tests '*FxMathTest*'` → PASS.

- [ ] **Step 5: The hop uses it** — `RestlessGrimoire.kt` ≈ lines 85–86:

```kotlin
                translationY = ((bob?.value ?: 0f) + smoothKeyframe(hop.value, HOP_Y)) * unit
                rotationZ = smoothKeyframe(hop.value, HOP_DEG)
```

Import `com.summondemons.safely.ui.fx.smoothKeyframe` (keep `keyframe`, still used for the opening).

- [ ] **Step 6: Seal flare springs** — `ChantPieces.kt` `SealMark`:

```kotlin
            pulse.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
```

Imports `androidx.compose.animation.core.spring`, `androidx.compose.animation.core.Spring`; delete
`FLARE_MS` if nothing else uses it (`grep -n FLARE_MS` on the file).

- [ ] **Step 7: Climax rise eases** — `RitualScreen.kt` ≈ line 325:

```kotlin
    val rise = if (reduceMotion) fraction else FastOutSlowInEasing.transform(segment(fraction, 0.7f, 1f))
```

Import `androidx.compose.animation.core.FastOutSlowInEasing` if absent.

- [ ] **Step 8: Haptic on Summon** — `ChantIntro.kt` `SummonButton`: add
`val context = LocalContext.current` at the top and change the `clickable`'s `onClick`:

```kotlin
                onClick = {
                    Haptics.tick(context)
                    onClick()
                },
```

Imports `androidx.compose.ui.platform.LocalContext`, `com.summondemons.safely.platform.Haptics`.

- [ ] **Step 9: Spacing on the grid** — replace each with `MaterialTheme.spacing.sm` (8 dp):
  - `Badges.kt` `Arrangement.spacedBy(MaterialTheme.spacing.xs + 2.dp)` → `Arrangement.spacedBy(MaterialTheme.spacing.sm)`
  - `CueChip.kt` `vertical = MaterialTheme.spacing.xs + 2.dp` → `vertical = MaterialTheme.spacing.sm`
  - `Dossier.kt` `Arrangement.spacedBy(MaterialTheme.spacing.xs + 2.dp)` → `Arrangement.spacedBy(MaterialTheme.spacing.sm)`
  - `QuoteSlot.kt` `vertical = MaterialTheme.spacing.sm + 2.dp` → `vertical = MaterialTheme.spacing.sm`

  Run `grep -rnE 'spacing\.(xs|sm) \+ 2\.dp' app/src/main` → no output. Remove a now-unused `dp` import only if the compiler reports it.

- [ ] **Step 10: Unused import** — `grep -n '\.sp\b' app/src/main/java/com/summondemons/safely/ui/screens/GrimoireScreen.kt`;
if the only hit is the import, delete `import androidx.compose.ui.unit.sp`.

- [ ] **Step 11: Badge survives recreation** — `GrimoireScreen.kt`:

```kotlin
    var badge by rememberSaveable(demon.id) { mutableStateOf<BadgeKey?>(null) }
```

(`BadgeKey` is an enum, saved by the default saver.)

- [ ] **Step 12: Badge header weight** — `Badges.kt` `InfoBand`:

```kotlin
                            Text(info.label.upper(), style = roles.meta.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.demonColors.accent(d.accent))
```

Import `androidx.compose.ui.text.font.FontWeight` if absent.

- [ ] **Step 13: Verify** — `./gradlew -q testDebugUnitTest` → exit 0. Device: the Home book hops
smoothly; a sealed phrase's numeral overshoots slightly; the climax demon rises fast then settles;
tapping Summon on the intro buzzes; open a badge, change language in Settings, return: the same badge
is open. Screenshots `task8-home.png`, `task8-grimoire-badge.png`, `task8-cue-chip.png`. Record what you saw.
- [ ] **Step 14: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui app/src/test/java/com/summondemons/safely/ui/fx/FxMathTest.kt
git commit -m "feat(motion): Springier seal flare, smooth hop, eased climax rise, Summon haptic; spacing on the grid"
```

---

## Task 9: Verify and close

**Files:** `docs/superpowers/specs/2026-10-23-final-pass-fixes-design.md`, `DESIGN.md`, `TODO.md`

- [ ] **Step 1:** `./gradlew -q testDebugUnitTest` and `./gradlew -q assembleDebug` → both exit 0.
- [ ] **Step 2:** `./gradlew -q installDebug`, app language en, then `tools/ui_matrix.sh s24-final en`.
List the produced folder (`ls -R build/ui-matrix/s24-final | head -50`) in the report. Then
`adb shell wm size; adb shell wm density; adb shell settings get system font_scale` → physical
size, density 420 (override), font scale 1.0.
- [ ] **Step 3: Spec triage outcomes** — in the spec's Triage table add a column `Outcome` with,
per row: `Fixed <short hash>`, `Verified, no change` (R1-1 if Step 1 of Task 7 passed, R1-12 if
kept), `Deferred` or `Won't fix`.
- [ ] **Step 4: DESIGN.md** — `grep -n -i "paused\|rule 3\|intro" DESIGN.md`. If DESIGN.md describes
the paused slot colour or the intro's Rule 3 icon, update that sentence to match (amber paused chip;
green heard line + accent current line). Change nothing else.
- [ ] **Step 5: TODO.md** — add one open entry in the same format as its neighbours, e.g.
`- [ ] FPdf **-** Final-pass deferred items: NavHost transitions (needs ADR), typography tokens, font-scale previews, sigil ring spring harmony (spec 2026-10-23-final-pass-fixes §Triage)`.
- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/specs/2026-10-23-final-pass-fixes-design.md DESIGN.md TODO.md
git commit -m "docs(review): Final-pass outcomes recorded; deferred items in TODO"
```
