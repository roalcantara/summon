# Chant Screen Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A chant screen whose phrase never changes size mid-line, with the demon's name, sigil and direction in one aligned stack on the left, a vertical I–IV progress track, and a labelled mic chip instead of the "•••••" meter.

**Architecture:** The phrase loses its per-word font size (emphasis by colour, underline and glow, drawn behind the text), so its auto-size is stable for the whole line. A small multi-content `Layout`, `SigilStack`, sizes the sigil to the space its column leaves and gives the name plate and the pill the sigil's width; its size math is a pure, unit-tested function. `ChantParts` swaps its centred `title` for `name` and `track` slots, and both arrangements are rebuilt from them.

**Tech Stack:** Kotlin, Jetpack Compose (foundation `Layout` with multiple contents, `TextAutoSize`, `LineBreak.Heading`), JUnit 4, adb.

**Spec:** `docs/superpowers/specs/2026-10-22-chant-screen-polish-design.md`. **Branch:** `fix/pre-release-ui`.

---

## Conventions

- `…/safely/` = `app/src/main/java/com/summondemons/safely/`; tests under `app/src/test/java/com/summondemons/safely/`.
- New player text goes into all five `strings.xml` (en, es, pt-rBR, it, fr), none longer than English.
- Colours, type, shapes and spacing come from the theme.
- Build/install: `./gradlew -q installDebug`; tests: `./gradlew -q testDebugUnitTest` (exit 0 = pass).
- Device checks on the S24; after any `font_scale` change, restore with `adb shell settings put system font_scale 1.0`.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## File map

| File | Change | Responsibility |
|---|---|---|
| `…/safely/ui/screens/SigilStack.kt` | create | `stackDiameter` (pure) and the `SigilStack` layout |
| `app/src/test/java/com/summondemons/safely/ui/screens/SigilStackTest.kt` | create | tests for `stackDiameter` |
| `…/safely/ui/screens/ChantPieces.kt` | modify | stable phrase; `ChantNamePlate`; `SealTrack` (replaces `ChantTitle`); pill `modifier`; mic chip |
| `…/safely/ui/screens/ChantArrangements.kt` | modify | `ChantParts` slots; split and portrait arrangements |
| `…/safely/ui/screens/ChantScreen.kt` | modify | wire the new parts |
| `app/src/main/res/drawable/ic_mic.xml` | create | mic icon |
| `app/src/main/res/values*/strings.xml` | modify | `chant_listening` |
| `docs/decisions/0029-chant-screen-ux.md`, `0033-attention-glow-and-large-text.md`, `DESIGN.md` | modify | amendments |

---

### Task 1: Stable phrase

**Files:** Modify `…/safely/ui/screens/ChantPieces.kt` (constants ~line 160; `Phrase` ~line 650; `drawWordLines` ~line 720)

- [ ] **Step 1: Constants** — replace `private const val CURRENT_WORD_SCALE = 1.15f` with:

```kotlin
/** The current word's underline is this many times the heard/pending thickness (ADR-0033 amended). */
private const val CURRENT_LINE_SCALE = 2f
/** The soft glow behind the current word, at the chanting halo's strength. */
private const val CURRENT_WORD_GLOW_ALPHA = 0.18f
```

- [ ] **Step 2: No per-word size** — in `Phrase`, replace the loop body that pushes the size span:

```kotlin
            val start = length
            val sized = if (word.look == WordLook.Current) pushStyle(SpanStyle(fontSize = CURRENT_WORD_SCALE.em)) else null
            word.syllables.forEachIndexed { si, syllable ->
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
                withStyle(SpanStyle(color = color)) { append(syllable) }
            }
            if (sized != null) pop(sized)
            ranges += start until length
```

with

```kotlin
            val start = length
            // Same size for every word: only colours change as the chant moves, so the
            // auto-sized phrase never refits or re-wraps mid-line (spec 2026-10-22 polish §1).
            word.syllables.forEachIndexed { si, syllable ->
                val color = if (word.look == WordLook.Heard || si < word.lit) accent else dim
                withStyle(SpanStyle(color = color)) { append(syllable) }
            }
            ranges += start until length
```

- [ ] **Step 3: Balanced breaks** — in the same function, change the `BasicText` style line to:

```kotlin
        style = MaterialTheme.textRoles.incantationLarge.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Heading),
```

and pass the glow colour to the line drawing:

```kotlin
            layout?.let { drawWordLines(it, ranges, words, lineColors, accent.copy(alpha = CURRENT_WORD_GLOW_ALPHA)) }
```

Add `import androidx.compose.ui.text.style.LineBreak`.

- [ ] **Step 4: Underline + glow for the current word** — replace `drawWordLines` with:

```kotlin
private fun DrawScope.drawWordLines(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    words: List<ChantWord>,
    lineColors: List<Color>,
    currentGlow: Color,
) {
    ranges.forEachIndexed { i, range ->
        if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return@forEachIndexed
        if (layout.getLineForOffset(range.first) != layout.getLineForOffset(range.last)) return@forEachIndexed
        val first = layout.getBoundingBox(range.first)
        val last = layout.getBoundingBox(range.last)
        val height = last.bottom - last.top
        val thickness = height * LINE_FRACTION
        val look = words[i].look
        if (look == WordLook.Current) {
            // A soft glow behind the word being chanted (drawn first, so the text sits on it).
            val center = Offset((first.left + last.right) / 2f, (first.top + last.bottom) / 2f)
            val radius = maxOf(last.right - first.left, height) * 0.6f
            drawCircle(Brush.radialGradient(listOf(currentGlow, Color.Transparent), center, radius), radius, center)
        }
        val y = last.bottom + thickness
        val width = when (look) {
            WordLook.Pending -> thickness * PENDING_LINE_SCALE
            WordLook.Current -> thickness * CURRENT_LINE_SCALE
            WordLook.Heard -> thickness
        }
        drawLine(lineColors[i], Offset(first.left, y), Offset(last.right, y), width, StrokeCap.Round)
    }
}
```

In the `lineColors` mapping, make the current word's line the accent: `WordLook.Current -> accent` (was `colors.onSurfaceVariant`).

- [ ] **Step 5: Build, tests, device check**

Run: `./gradlew -q testDebugUnitTest && ./gradlew -q installDebug` → exit 0.
S24: summon Astaroth (debug "Simulate heading" to lock), chant "TUI GRATIA LOVIS": the phrase keeps one size and the same line breaks while the current word moves; the current word is accent-coloured with a thick underline and a soft glow.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt
git commit -m "fix(chant): The phrase keeps one size per line; the current word is marked by colour, underline and glow" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Stack size math (pure, test first)

**Files:** Create `…/safely/ui/screens/SigilStack.kt`; Test `app/src/test/java/com/summondemons/safely/ui/screens/SigilStackTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
package com.summondemons.safely.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SigilStackTest {

    @Test
    fun aShortColumnSizesTheSigilByTheHeightLeft() {
        // 300 wide, 400 tall: 400 - 40 (name) - 30 (pill) - 2 * 12 (gaps) = 306, capped by the width.
        assertEquals(300, stackDiameter(width = 300, height = 400, nameHeight = 40, pillHeight = 30, gap = 12))
        assertEquals(206, stackDiameter(width = 300, height = 300, nameHeight = 40, pillHeight = 30, gap = 12))
    }

    @Test
    fun aHiddenPillLeavesItsRowAndGapToTheSigil() {
        assertEquals(248, stackDiameter(width = 300, height = 300, nameHeight = 40, pillHeight = null, gap = 12))
    }

    @Test
    fun neverNegative() {
        assertEquals(0, stackDiameter(width = 300, height = 50, nameHeight = 40, pillHeight = 30, gap = 12))
    }
}
```

- [ ] **Step 2: Run, expect FAIL** — `./gradlew -q testDebugUnitTest --tests '*SigilStackTest*'` → `Unresolved reference: stackDiameter`.

- [ ] **Step 3: Implement `SigilStack.kt`**

```kotlin
package com.summondemons.safely.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * The sigil's diameter in a stack of name plate, sigil and pill (spec 2026-10-22 polish §2): all
 * the height the plate, the pill (null when hidden) and the gaps leave, capped by the width.
 */
fun stackDiameter(width: Int, height: Int, nameHeight: Int, pillHeight: Int?, gap: Int): Int {
    val pillRow = pillHeight?.let { it + gap } ?: 0
    return minOf(width, height - nameHeight - gap - pillRow).coerceAtLeast(0)
}

/**
 * Name plate over sigil over direction pill, top-aligned and centred, all as wide as the sigil,
 * which takes the height the other two leave ([stackDiameter]).
 */
@Composable
internal fun SigilStack(
    name: @Composable () -> Unit,
    sigil: @Composable () -> Unit,
    pill: @Composable () -> Unit,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(name, sigil, pill), modifier = modifier) { (names, sigils, pills), constraints ->
        val gapPx = gap.roundToPx()
        val nameM = names.first()
        val pillM = pills.firstOrNull()
        val d = stackDiameter(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            nameHeight = nameM.maxIntrinsicHeight(constraints.maxWidth),
            pillHeight = pillM?.maxIntrinsicHeight(constraints.maxWidth),
            gap = gapPx,
        )
        val n = nameM.measure(Constraints.fixedWidth(d))
        val s = sigils.first().measure(Constraints.fixed(d, d))
        val p = pillM?.measure(Constraints.fixedWidth(d))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = (constraints.maxWidth - d) / 2
            var y = 0
            n.place(x, y)
            y += n.height + gapPx
            s.place(x, y)
            y += d + gapPx
            p?.place(x, y)
        }
    }
}
```

- [ ] **Step 4: Run, expect PASS** — same command → exit 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/SigilStack.kt app/src/test/java/com/summondemons/safely/ui/screens/SigilStackTest.kt
git commit -m "feat(chant): SigilStack sizes the sigil and gives name and pill its width" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Name plate, vertical track, full-width pill

**Files:** Modify `…/safely/ui/screens/ChantPieces.kt` (`NameSize`, `SealSize` constants; `ChantTitle`; `DirectionPillView`)

- [ ] **Step 1: Constants** — replace `private val SealSize = 22.dp` with `private val SealSize = 28.dp` and add:

```kotlin
private val NameMinSize = 10.sp
private val NameRule = 1.dp
private const val NAME_RULE_ALPHA = 0.35f
private val TrackLink = 20.dp
private val TrackLinkWidth = 2.dp
```

- [ ] **Step 2: Replace `ChantTitle`** with the two composables below (keep `SealMark` unchanged):

```kotlin
/** The demon's name between two thin accent rules, sized to the stack's width (spec 2026-10-22 polish §2). */
@Composable
internal fun ChantNamePlate(name: String, accent: Color) {
    val spacing = MaterialTheme.spacing
    val maxSize = NameSize.fontScaleCapped()
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val rule = NameRule.toPx()
                val color = accent.copy(alpha = NAME_RULE_ALPHA)
                drawLine(color, Offset(0f, rule / 2f), Offset(size.width, rule / 2f), rule)
                drawLine(color, Offset(0f, size.height - rule / 2f), Offset(size.width, size.height - rule / 2f), rule)
            }
            .padding(vertical = spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.uppercase(),
            style = MaterialTheme.textRoles.name.copy(fontSize = maxSize, letterSpacing = 0.12.em, textAlign = TextAlign.Center),
            color = accent,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = NameMinSize, maxFontSize = maxSize),
        )
    }
}

/**
 * The four lines as steps I–IV joined by a line that fills as they seal (spec 2026-10-22 polish §3);
 * each step is a [SealMark] (current breathes, sealed flares, cracked red, paused amber).
 */
@Composable
internal fun SealTrack(seals: List<SealLook>, accent: Color, paused: Boolean, checking: Boolean) {
    val sealed = seals.count { it == SealLook.Sealed }
    val description = pluralStringResource(R.plurals.chant_a11y_seals, sealed, sealed)
    val pending = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        seals.forEachIndexed { i, look ->
            if (i > 0) {
                val filled = seals[i - 1] == SealLook.Sealed
                Box(Modifier.size(TrackLinkWidth, TrackLink).background(if (filled) accent else pending))
            }
            SealMark(ROMAN_NUMERALS[i], look, accent, paused, checking, Modifier.size(SealSize))
        }
    }
}
```

- [ ] **Step 3: Pill takes a modifier and centres its text** — change the signature to

```kotlin
internal fun DirectionPillView(pill: DirectionPill, target: Float, modifier: Modifier = Modifier) {
```

in its `Text(...)` call change the style to

```kotlin
        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = PillTextSize.fontScaleCapped(), letterSpacing = 0.12.em, textAlign = TextAlign.Center),
```

and start its modifier chain with `modifier` (`modifier = modifier.graphicsLayer { alpha = blink }…`).

- [ ] **Step 4: Build** — `./gradlew -q assembleDebug` fails only on `ChantTitle` references in `ChantScreen.kt` (fixed in Task 5). Do not commit yet.

---

### Task 4: Mic chip

**Files:** Create `app/src/main/res/drawable/ic_mic.xml`; Modify five `strings.xml`; Modify `…/safely/ui/screens/ChantPieces.kt` (`FeedbackSlot`, Meter branch)

- [ ] **Step 1: Icon** (Material "mic", Apache 2.0)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z" />
</vector>
```

- [ ] **Step 2: String** — add next to `chant_paused` in each file:

| file | value |
|---|---|
| `values/strings.xml` | `<string name="chant_listening">Listening</string>` |
| `values-es` | `Oyendo` |
| `values-pt-rBR` | `Ouvindo` |
| `values-it` | `Ascolto` |
| `values-fr` | `J’écoute` |

- [ ] **Step 3: Chip** — add a constant `private val MicSize = 16.dp` and replace the Meter branch of `FeedbackSlot`:

```kotlin
            SlotContent.Meter -> Chip(colors.outlineVariant) {
                // Listening (or paused) as a labelled state, never bare bars that read as "•••••".
                Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = if (paused) colors.outline else accent, modifier = Modifier.size(MicSize))
                VoiceMeter(if (paused) 0f else micLevel, if (paused) colors.outline else accent, Modifier.size(MeterWidth, MeterHeight))
                Text(
                    stringResource(if (paused) R.string.chant_paused else R.string.chant_listening).upper(),
                    style = style,
                    color = if (paused) colors.outline else colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
```

- [ ] **Step 4: Translation tests** — `./gradlew -q testDebugUnitTest --tests '*TranslationResourcesTest*'` → exit 0 (compile of main may still fail on `ChantTitle`; if so run after Task 5). Add `chant_listening` to `docs/i18n/review.md` in its entry format, status "new (2026-10-22)".

---

### Task 5: Arrangements and wiring

**Files:** Modify `…/safely/ui/screens/ChantArrangements.kt`, `…/safely/ui/screens/ChantScreen.kt`

- [ ] **Step 1: `ChantParts`** — replace the class with:

```kotlin
internal class ChantParts(
    val buttons: @Composable () -> Unit,
    /** The demon's name plate (top of the left stack). */
    val name: @Composable () -> Unit,
    /** The I–IV progress track. */
    val track: @Composable () -> Unit,
    val pill: @Composable () -> Unit,
    val sigil: @Composable () -> Unit,
    val slot: @Composable () -> Unit,
    val line: @Composable (Modifier) -> Unit,
)
```

- [ ] **Step 2: Split arrangement** — replace `SplitArrangement` and `TopBar` with:

```kotlin
/**
 * Landscape (ADR-0029 amended 2026-10-22): "?" (and "D") in the top bar, ✕ drawn by the screen;
 * below, the left stack (name plate, sigil, direction pill), the vertical I–IV track, and the
 * slot over the line. Both columns are top-aligned, so the slot starts level with the name.
 */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.md, modifier = Modifier.fillMaxHeight().weight(SIGIL_WEIGHT))
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) { parts.track() }
            Column(Modifier.fillMaxHeight().weight(1f - SIGIL_WEIGHT), horizontalAlignment = Alignment.CenterHorizontally) {
                parts.slot()
                parts.line(Modifier.fillMaxWidth().weight(1f).padding(top = spacing.xl))
            }
        }
    }
}

@Composable
private fun TopBar(parts: ChantParts) {
    Box(Modifier.fillMaxWidth().height(TopBarHeight)) {
        Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
    }
}
```

- [ ] **Step 3: Portrait arrangement** — replace `PortraitArrangement` with:

```kotlin
/** Large screens that ignore the rotation: the top bar, a band with the stack and the track, then the slot and the line. */
@Composable
internal fun PortraitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TopBar(parts)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterHorizontally)) {
            SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.sm, modifier = Modifier.size(BandStackWidth, BandStackHeight))
            parts.track()
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            parts.slot()
            parts.line(Modifier.fillMaxWidth().weight(1f).padding(top = spacing.xl))
        }
    }
}
```

Replace `private val BandSigil = 96.dp` with `private val BandStackWidth = 160.dp` and `private val BandStackHeight = 220.dp`. Update the `TopBarHeight` KDoc to "As tall as the round buttons' touch target." Remove imports the compiler reports unused (`aspectRatio`, …).

- [ ] **Step 4: Wire `ChantScreen`** — replace the `ChantParts(` block's entries:

```kotlin
        val parts = ChantParts(
            buttons = { CornerButtons(onHelp, debug) },
            name = { ChantNamePlate(demon.name, accent) },
            track = { SealTrack(screen.seals, accent, screen.drifting, screen.checking) },
            pill = { DirectionPillView(screen.pill, demon.ritualHeading) },
            sigil = { ChantSigil(state, screen, accent, flash, sigilGlow, Modifier.fillMaxSize().onSizeChanged { sigilPx = it.width }) },
            slot = { if (blocker == null) FeedbackSlot(screen.slot, accent, state.micLevel, screen.drifting) },
            line = { m ->
                if (blocker != null) {
                    Box(m, contentAlignment = Alignment.Center) { blocker() }
                } else {
                    ChantLine(screen.words, screen.gated, screen.drifting, screen.lockFraction, accent, phraseGlow, onPhraseSize = { phraseSp = it }, modifier = m.onSizeChanged { box = it })
                }
            },
        )
```

- [ ] **Step 5: Build, tests, device check**

Run: `./gradlew -q testDebugUnitTest && ./gradlew -q installDebug` → exit 0.
S24 at font scale 1.0 and 2.0, both demons (restore 1.0 after):
1. Left: name plate, sigil, pill share one width; the sigil fills the column's height.
2. Track: I–IV vertical between the columns; current step breathes; a sealed step fills its link.
3. Right: the chip reads "LISTENING" with live bars (PAUSED when turned away), level with the name plate; the phrase sits below it with clear space and keeps its size while chanting.
4. Top: only ?/D and ✕.
5. Nothing clipped: the name plate shrinks rather than overflow at 2.0.

- [ ] **Step 6: Commit** (includes Tasks 3–4)

```bash
git add -A app/src/main docs/i18n/review.md
git commit -m "feat(chant): Left stack (name, sigil, pill), vertical I–IV track, mic chip; no centred title" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Docs

**Files:** `docs/decisions/0029-chant-screen-ux.md`, `docs/decisions/0033-attention-glow-and-large-text.md`, `DESIGN.md`

- [ ] **Step 1: ADR-0029** — append:

```markdown
## Amendment (2026-10-22): chant screen polish

Spec: docs/superpowers/specs/2026-10-22-chant-screen-polish-design.md. The centred title goes:
the left column is one stack — name plate, sigil (as large as the column allows), direction pill —
all as wide as the sigil; the I–IV seals become a vertical progress track between the columns;
the slot's idle meter is a labelled mic chip (LISTENING / PAUSED), top-aligned with the name
plate, with the phrase centred below it. The top bar keeps only "?" (and "D") and ✕.
```

and add `; amended 2026-10-22 (polish)` to its `Status:` line.

- [ ] **Step 2: ADR-0033** — append under Decision (or an Amendment section): "The current word is marked by colour, a 2× underline and a soft glow, never by size; the phrase keeps one size per line and breaks balanced (`LineBreak.Heading`)."

- [ ] **Step 3: DESIGN.md** — in the chant screen component contracts, describe `SigilStack`, `ChantNamePlate`, `SealTrack`, the mic chip and the current-word emphasis; remove the centred title description.

- [ ] **Step 4: Commit**

```bash
git add docs/decisions DESIGN.md
git commit -m "docs(adr): Amend ADR-0029 and ADR-0033 for the chant screen polish" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Matrix shots and close-out

- [ ] **Step 1:** `tools/ui_matrix.sh s24-en` — capture at least `intro`, `chant-seeking`, `chant-chanting` (skip the others with `s`).
- [ ] **Step 2:** Same on `Medium_Phone_API_37.0` (`ANDROID_SERIAL=emulator-5554 tools/ui_matrix.sh emu-en` after `./gradlew -q installDebug` on it).
- [ ] **Step 3:** Fix anything clipped per the pre-release spec §5.3; one commit per fix.
- [ ] **Step 4:** Append a "Chant screen polish" note to `docs/reviews/2026-10-22-pre-release-ui-review.md` listing only configurations actually captured (paths under `build/ui-matrix/`), then commit `docs(review): Chant screen polish checks`.

---

## UI skills conformance

Checked against `~/.agents/skills/mobile-android-design` and `~/.agents/skills/mobile-app-ui-design`.

| Skill rule | How this plan conforms |
|---|---|
| **android-design:** read colours/type from the theme | All colours from `colorScheme` / `demonColors` / accent; spacing tokens `xs`, `sm`, `md`, `lg`, `xl`; no literals (Tasks 1, 3, 4). |
| **android-design:** content descriptions on interactive elements | No new tap targets. The mic icon is decorative (`null`); the chip's label is read (the slot is a polite live region); the track keeps the merged `chant_a11y_seals` description (Task 3). |
| **android-design:** 48 dp touch targets | Unchanged: "?", "D", ✕ remain 48 dp `IconButton`s. |
| **android-design:** adaptive layouts | `SigilStack` derives every size from its constraints (`stackDiameter`, unit-tested); portrait arrangement for large screens (Tasks 2, 5). |
| **android-design:** state hoisting / no state in composables | No new state: everything comes from the pure `ChantScreenState`; the new composables only draw (Tasks 3–5). |
| **android-design:** avoid needless recomposition | The current-word glow is drawn in `drawBehind` from the existing layout; no new animated values are read in composition. |
| **android-design:** `@Preview` coverage | **Deviation:** no previews added (the chant screen needs a `RitualUiState`); covered by the device checks and `ui_matrix.sh` instead. Tracked as D4 in the pre-release review. |
| **app-ui-design:** visual hierarchy, one thing first | The phrase is the largest element; one glow at a time (ADR-0033); the name plate and track are secondary (Tasks 1, 3). |
| **app-ui-design:** related things close, consistent alignment | Left = who and where (name, sigil, pill, one width); right = what to say (chip, phrase); both columns top-aligned (Task 5). |
| **app-ui-design:** no layout shift during a task | The phrase keeps one size and its line breaks for the whole line (Task 1). |
| **app-ui-design:** states designed, colour never the only signal | Chip says LISTENING / PAUSED; locked/checking/sealed/missed/cracked chips unchanged; the pill keeps its words (Task 4). |
| **app-ui-design:** 8/4-pt spacing | Gaps `md` (12), `xl` (24), track links 20 dp, nodes 28 dp, rules 1 dp. |
| **app-ui-design:** ≤ 2 font families | Cinzel (name, phrase, numerals) and Inter (chip, pill) only. |
| **app-ui-design:** ≤ 4 font sizes | **Partial:** phrase (auto), name plate (≤ 22 sp), labels (15–16 sp), numerals (10 sp) — four roles on this screen; the one-off sizes stay a post-release item (D1 in the review). |
| **app-ui-design:** visual cues and icons | Mic icon on the chip; Roman numerals on the track nodes. |

## Self-review notes

- Spec §1 → Task 1; §2 → Tasks 2, 3, 5; §3 → Tasks 3, 5; §4 → Task 5 (`TopBar`); §5 incl. placement → Tasks 4, 5; §6 → Task 5 Step 3; validation → Tasks 5, 7; docs → Task 6.
- Names across tasks: `stackDiameter`, `SigilStack`, `ChantNamePlate`, `SealTrack`, `ChantParts.name/track/pill/sigil`, `chant_listening`, `ic_mic`, `CURRENT_LINE_SCALE`, `CURRENT_WORD_GLOW_ALPHA`.
