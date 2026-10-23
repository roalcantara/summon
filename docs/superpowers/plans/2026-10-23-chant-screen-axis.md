# Chant Screen Axis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the chant screen's top bar, put ?/D · slot · ✕ in one HUD row, and centre the sigil stack, the track and the phrase on one horizontal axis with equal 16 / 24 dp spacing.

**Architecture:** Two pure, unit-tested functions carry the geometry: `stackLayout` (sigil diameter and group top in the left column) and `phraseTop` (where the phrase sits around the axis). `SigilStack` and a small `AxisLine` layout use them. `ChantParts` gains a `close` slot; `SplitArrangement` builds the HUD row and overlays it on the right column.

**Tech Stack:** Kotlin, Jetpack Compose (`Layout`), JUnit 4, adb.

**Spec:** `docs/superpowers/specs/2026-10-23-chant-screen-axis-design.md`. **Branch:** `fix/pre-release-ui`.

---

## Conventions

- `…/screens/` = `app/src/main/java/com/summondemons/safely/ui/screens/`; tests in `app/src/test/java/com/summondemons/safely/ui/screens/`.
- **The operator has uncommitted `@Preview` edits** in `ChantScreen.kt` and `ChantIntro.kt`. Before starting, run `git diff --stat`; if those files still show changes you did not make, stop and ask the operator to commit or stash them. Never discard them.
- Tests: `./gradlew -q testDebugUnitTest` (exit 0). Install: `./gradlew -q installDebug`.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: Geometry (pure, test first)

**Files:** Modify `…/screens/SigilStack.kt`; Modify `app/src/test/java/com/summondemons/safely/ui/screens/SigilStackTest.kt`

- [ ] **Step 1: Replace the tests** in `SigilStackTest.kt` with:

```kotlin
package com.summondemons.safely.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SigilStackTest {

    // S24 landscape: column 228 × 334 dp, name 38, pill 31, gaps 16, air 24.
    @Test
    fun theSigilTakesWhatTheSpacingLeavesAndTheGroupIsCentred() {
        val s = stackLayout(width = 228, height = 334, nameHeight = 38, pillHeight = 31, gap = 16, air = 24)
        assertEquals(185, s.diameter)
        assertEquals(24, s.top)
    }

    @Test
    fun aWideColumnIsCappedByItsWidthAndTheRestBecomesAir() {
        val s = stackLayout(width = 150, height = 334, nameHeight = 38, pillHeight = 31, gap = 16, air = 24)
        assertEquals(150, s.diameter)
        assertEquals((334 - (38 + 16 + 150 + 16 + 31)) / 2, s.top)
    }

    @Test
    fun aHiddenPillGivesUpItsRowAndGap() {
        val s = stackLayout(width = 300, height = 334, nameHeight = 38, pillHeight = null, gap = 16, air = 24)
        assertEquals(334 - 48 - 16 - 38, s.diameter)
    }

    @Test
    fun neverNegative() {
        assertEquals(0, stackLayout(width = 228, height = 80, nameHeight = 38, pillHeight = 31, gap = 16, air = 24).diameter)
    }

    // The phrase box below the HUD row is 72..334 in a 334 tall column; the axis is 167.
    @Test
    fun aPhraseThatFitsIsCentredOnTheAxis() {
        assertEquals(84, phraseTop(axis = 167, top = 72, bottom = 334, height = 166))
    }

    @Test
    fun aTallPhraseSitsAsCloseToTheAxisAsTheSpaceAllows() {
        assertEquals(72, phraseTop(axis = 167, top = 72, bottom = 334, height = 250))
        assertEquals(72, phraseTop(axis = 167, top = 72, bottom = 334, height = 400))
    }
}
```

- [ ] **Step 2: Run, expect FAIL** — `./gradlew -q testDebugUnitTest --tests '*SigilStackTest*'` → `Unresolved reference: stackLayout`.

- [ ] **Step 3: Replace `stackDiameter`** in `SigilStack.kt` with:

```kotlin
/** The left stack's sigil diameter and the group's top (spec 2026-10-23 §4). */
internal data class StackLayout(val diameter: Int, val top: Int)

/**
 * Name plate, sigil and pill as one group centred in the column: [gap] between them, at least
 * [air] above and below; the sigil takes what is left, capped by the width. A hidden pill
 * (null) gives up its row and gap.
 */
internal fun stackLayout(width: Int, height: Int, nameHeight: Int, pillHeight: Int?, gap: Int, air: Int): StackLayout {
    val pillRow = pillHeight?.let { it + gap } ?: 0
    val fixed = nameHeight + gap + pillRow
    val d = minOf(width, height - 2 * air - fixed).coerceAtLeast(0)
    return StackLayout(d, ((height - fixed - d) / 2).coerceAtLeast(0))
}

/** Top of a phrase [height] tall, centred on [axis] but kept inside [top]..[bottom] (spec §6). */
internal fun phraseTop(axis: Int, top: Int, bottom: Int, height: Int): Int =
    (axis - height / 2).coerceAtMost(bottom - height).coerceAtLeast(top)
```

- [ ] **Step 4: Update `SigilStack`** — signature gains `air: Dp`; the measure block becomes:

```kotlin
    Layout(contents = listOf(name, sigil, pill), modifier = modifier) { (names, sigils, pills), constraints ->
        val gapPx = gap.roundToPx()
        val nameM = names.first()
        val pillM = pills.firstOrNull()
        pillM?.let { reservedPill[0] = it.maxIntrinsicHeight(constraints.maxWidth) }
        val nameH = nameM.maxIntrinsicHeight(constraints.maxWidth)
        val s = stackLayout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
            nameHeight = nameH,
            pillHeight = reservedPill[0].takeIf { it > 0 },
            gap = gapPx,
            air = air.roundToPx(),
        )
        val d = s.diameter
        val n = nameM.measure(Constraints.fixedWidth(d))
        val sg = sigils.first().measure(Constraints.fixed(d, d))
        val p = pillM?.measure(Constraints.fixedWidth(d))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = (constraints.maxWidth - d) / 2
            var y = s.top
            n.place(x, y)
            y += nameH + gapPx
            sg.place(x, y)
            y += d + gapPx
            p?.place(x, y)
        }
    }
```

Update the KDoc: "Name plate over sigil over direction pill, one group centred in the column (spec 2026-10-23 §4)…".

- [ ] **Step 5: Run, expect PASS**; build fails only where `SigilStack(` is called without `air` (fixed in Task 3). Do not commit yet.

---

### Task 2: `AxisLine` — the phrase around the axis

**Files:** Modify `…/screens/SigilStack.kt` (append)

- [ ] **Step 1: Append**

```kotlin
/**
 * Lays [content] (the phrase, auto-sized) in the space from [reservedTop] to the bottom, then
 * places it with its centre on this box's vertical middle, the screen's axis, as far as that
 * space allows ([phraseTop]). [content] must wrap its height (no fillMaxSize).
 */
@Composable
internal fun AxisLine(reservedTop: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val top = reservedTop.roundToPx()
        val room = (constraints.maxHeight - top).coerceAtLeast(0)
        val placeable = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = room))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = phraseTop(axis = constraints.maxHeight / 2, top = top, bottom = constraints.maxHeight, height = placeable.height)
            placeable.place((constraints.maxWidth - placeable.width) / 2, y)
        }
    }
}
```

- [ ] **Step 2:** `./gradlew -q testDebugUnitTest --tests '*SigilStackTest*'` → exit 0 (compile of the arrangement may still fail until Task 3; then run after Task 3).

---

### Task 3: HUD row and the split arrangement

**Files:** Modify `…/screens/ChantArrangements.kt`

- [ ] **Step 1: `ChantParts`** — add after `buttons`:

```kotlin
    /** ✕ (null during the climax): the HUD row's end. */
    val close: (@Composable () -> Unit)?,
```

- [ ] **Step 2: Constants** — replace `TopBarHeight` with:

```kotlin
/** The HUD row: as tall as a touch target (spec 2026-10-23 §1). */
private val HudHeight = 48.dp
/** Keeps the slot centred on the column and clear of ?/D and ✕: two targets + 8 dp. */
private val HudSideInset = 104.dp
```

- [ ] **Step 3: Replace `SplitArrangement` and `TopBar`** with:

```kotlin
/**
 * Landscape (ADR-0029, amended 2026-10-23): no top bar. Left: the stack (name plate, sigil, pill)
 * centred in its column; the I–IV track; right: the HUD row (?/D · slot · ✕) over the line. The
 * sigil, the track and the line share the content box's vertical middle.
 */
@Composable
internal fun SplitArrangement(parts: ChantParts, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SigilStack(parts.name, parts.sigil, parts.pill, gap = spacing.lg, air = spacing.xl, modifier = Modifier.fillMaxHeight().weight(SIGIL_WEIGHT))
        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) { parts.track() }
        Box(Modifier.fillMaxHeight().weight(1f - SIGIL_WEIGHT)) {
            parts.line(Modifier.fillMaxSize())
            HudRow(parts, Modifier.align(Alignment.TopCenter))
        }
    }
}

/** ?/D at the start, the slot centred, ✕ at the end, in one 48 dp row. */
@Composable
private fun HudRow(parts: ChantParts, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(HudHeight)) {
        Box(Modifier.align(Alignment.CenterStart)) { parts.buttons() }
        Box(Modifier.align(Alignment.Center).padding(horizontal = HudSideInset)) { parts.slot() }
        parts.close?.let { Box(Modifier.align(Alignment.CenterEnd)) { it() } }
    }
}

/** Space the line leaves for the HUD row and its gap. */
internal val LineReservedTop: Dp @Composable get() = HudHeight + MaterialTheme.spacing.xl
```

- [ ] **Step 4: Portrait** — in `PortraitArrangement` replace `TopBar(parts)` with `HudRow(parts)`, the `SigilStack(` call gains `air = spacing.sm`, and replace its last `Column { parts.slot(); parts.line(…) }` with `parts.line(Modifier.fillMaxWidth().weight(1f))` (the slot now lives in the HUD row). Add imports `androidx.compose.ui.unit.Dp`; remove unused ones.

---

### Task 4: Wire `ChantScreen`

**Files:** Modify `…/screens/ChantScreen.kt` (`ChantParts(` block; remove the separate ✕)

- [ ] **Step 1:** In `ChantParts(`, add after `buttons = …`:

```kotlin
            close = if (showClose) {
                {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_abort),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                null
            },
```

and replace the `line = { m -> … }` entry with:

```kotlin
            line = { m ->
                val reserved = LineReservedTop
                if (blocker != null) {
                    Box(m.padding(top = reserved), contentAlignment = Alignment.Center) { blocker() }
                } else {
                    AxisLine(reservedTop = reserved, modifier = m.onSizeChanged { box = it }) {
                        ChantLine(screen.words, screen.gated, screen.drifting, screen.lockFraction, accent, phraseGlow, emphasize = attention == Attention.Phrase, onPhraseSize = { phraseSp = it }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
```

- [ ] **Step 2:** Delete the `if (showClose) { IconButton(… .align(Alignment.TopEnd).safeDrawingPadding().padding(…)) … }` block at the end of the `BoxWithConstraints`.

- [ ] **Step 3: The phrase must wrap its height** — in `ChantPieces.kt` `ChantLine`, change the `Phrase(…)` call's modifier from `Modifier.fillMaxSize().graphicsLayer { … }` to `Modifier.fillMaxWidth().graphicsLayer { … }`, and in `Phrase` keep `wrapContentHeight` (it now measures to the text's height).

- [ ] **Step 4: Build, tests, device check**

`./gradlew -q testDebugUnitTest && ./gradlew -q installDebug` → exit 0. Then `tools/ui_matrix.sh s24-axis en` and check `chant-seeking` / `chant-chanting` at 1.0 and 2.0:
1. No top bar; ?/D, the chip and ✕ in one row; ✕ 16 dp from the safe edge.
2. Left stack centred with equal gaps; air above the name equals air below the pill.
3. The sigil's centre, the track's centre and a two-row phrase's centre on one line.
4. At 2.0 nothing clipped; the sigil smaller, the spacing unchanged.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/SigilStack.kt app/src/test/java/com/summondemons/safely/ui/screens/SigilStackTest.kt app/src/main/java/com/summondemons/safely/ui/screens/ChantArrangements.kt app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt
git commit -m "feat(chant): One axis for sigil, track and phrase; HUD row replaces the top bar" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Docs and record

- [ ] **Step 1:** ADR-0029: append `## Amendment (2026-10-23): one axis` — "No top bar: ?/D · slot · ✕ form the HUD row at the top of the right column. The stack (name plate, sigil, pill) is one group centred in its column (16 dp gaps, ≥ 24 dp air); the sigil, the track and the phrase are centred on the content box's vertical middle. Spec 2026-10-23." Update its Status line.
- [ ] **Step 2:** DESIGN.md chant screen contract: describe the HUD row, `stackLayout` spacing and the axis.
- [ ] **Step 3:** Append to `docs/reviews/2026-10-22-pre-release-ui-review.md` a "Chant screen axis" note listing only the captures actually taken (paths under `build/ui-matrix/s24-axis/`).
- [ ] **Step 4:** Commit `docs(adr): Chant screen axis (ADR-0029 amended)`.

## Best-practice conformance (from the visual check)

| Practice | How |
|---|---|
| Align what belongs together | Sigil, track and phrase on one axis (Tasks 1–4). |
| Related closer than groups | 16 dp inside the stack, ≥ 24 dp around it (`stackLayout`). |
| 8/4 grid, existing tokens | `lg`, `xl`, 48 dp; no new token. |
| Consistent edge insets | One 16 dp inset; ✕ in the HUD row (Task 4 Step 2). |
| 48 dp targets | `IconButton`s unchanged. |
| True shapes | Sigil measured square (`Constraints.fixed(d, d)`). |
| Large font sizes | The sigil shrinks; spacing fixed; phrase clamps (`phraseTop`). |

## Self-review

- Spec §1 → Task 3 (`HudRow`, `HudSideInset`); §2 → Task 4 Step 2; §3–6 → Tasks 1–4; §7 → Task 3 Step 4; validation → Task 4 Step 4, Task 5; docs → Task 5.
- Names: `StackLayout`, `stackLayout`, `phraseTop`, `AxisLine`, `HudRow`, `HudHeight`, `HudSideInset`, `LineReservedTop`, `ChantParts.close`.
