# Pre-release Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the Grimoire tarot flip, the intro card layout B and the chant screen's attention glow, correct at every font scale, display size and language.

**Architecture:** One attention cue (`rememberAttention` + `Modifier.attentionGlow` in `ui/fx/`) and the text-sizing rules from the spec are shared by all screens. What glows on the chant screen is decided in the pure `chantScreenState` (unit-tested); what glows elsewhere comes from two pure `ProgressRules` helpers (unit-tested). The Grimoire card moves into its own file and becomes a `FlipCard` with the identity on the front and a scrolling `DossierList` on the back.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, foundation `TextAutoSize`), JUnit 4, adb for device checks.

**Spec:** `docs/superpowers/specs/2026-10-22-pre-release-screens-design.md`. **Branch:** `fix/pre-release-ui`.

---

## Conventions for every task

- Paths are relative to the repo root; Kotlin sources live under `app/src/main/java/com/summondemons/safely/` (abbreviated `…/safely/`), tests under `app/src/test/java/com/summondemons/safely/`.
- Player text lives only in string resources. Every new or changed string is edited in all five files: `app/src/main/res/values/strings.xml`, `values-es`, `values-pt-rBR`, `values-it`, `values-fr`. No translation longer than English (`TranslationResourcesTest.noLongerThanEnglish`).
- UI reads colours, type, shapes and spacing from the theme (`MaterialTheme.colorScheme`, `demonColors`, `textRoles`, `spacing`), never literals.
- Build + install: `./gradlew -q installDebug`. Unit tests: `./gradlew -q testDebugUnitTest` (exit code 0 = pass).
- Device check (S24 connected): `adb shell settings put system font_scale 2.0`, check the screen, then **always** `adb shell settings put system font_scale 1.0`.
- Commit messages: Conventional Commits, ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## File map

| File | Change | Responsibility |
|---|---|---|
| `…/safely/progress/ProgressRules.kt` | modify | `dossierUnread`, `neverSummoned` |
| `…/safely/ui/screens/ChantScreenState.kt` | modify | `Attention` enum + field |
| `…/safely/ui/fx/AttentionGlow.kt` | create | `rememberAttention`, `Modifier.attentionGlow` |
| `…/safely/ui/screens/ChantScreen.kt` | modify | wires attention to sigil and line |
| `…/safely/ui/screens/ChantPieces.kt` | modify | sigil breathing term, phrase halo + dim, one-line pill |
| `…/safely/ui/theme/Color.kt`, `DemonColors.kt` | modify | parchment colours |
| `…/safely/ui/components/ParchmentRibbon.kt` | create | code-drawn two-line title ribbon |
| `…/safely/ui/screens/ChantIntro.kt` | modify | layout B, numbered rules, Summon glow |
| `…/safely/ui/screens/RitualScreen.kt`, `…/safely/navigation/AppNavHost.kt` | modify | pass `firstSummon` |
| `app/src/main/res/drawable-nodpi/chant_intro_banner.webp` | delete | retired art |
| `app/src/main/res/drawable/ic_flip.xml` | create | flip icon |
| `…/safely/ui/components/FlipCard.kt` | create | two-faced card animation |
| `…/safely/ui/components/DossierCarousel.kt` → `Dossier.kt` | rename + modify | `DossierList` replaces `DossierCarousel` |
| `…/safely/ui/components/DemonCard.kt` | create | card moved out of `GrimoireScreen.kt`, front/back |
| `…/safely/ui/screens/GrimoireScreen.kt` | modify | side state, flip rules, ink on flip |
| `tools/ui_matrix.sh` | create | configuration matrix screenshots |
| `docs/decisions/0033-attention-glow-and-large-text.md` + ADR amendments, `PRODUCT.md`, `DESIGN.md` | create/modify | docs |

---

### Task 1: Progress helpers for the two glows

**Files:**
- Modify: `…/safely/progress/ProgressRules.kt` (add two functions inside `object ProgressRules`)
- Test: `app/src/test/java/com/summondemons/safely/progress/ProgressRulesTest.kt`

- [ ] **Step 1: Write the failing tests** — append inside `class ProgressRulesTest` (add static imports `com.summondemons.safely.progress.ProgressRules.dossierUnread` and `…ProgressRules.neverSummoned` next to the existing `ProgressRules` imports, plus `consumeReveal` and `commitSummon` if not already imported):

```kotlin
    // --- Attention glow (ADR-0033) ---

    @Test
    fun aFreshSummonLeavesTheDossierUnreadUntilBothRevealsAreConsumed() {
        var p = commitSummon(Progress(), "asmodeus", 10, flawless = false)
        assertTrue(dossierUnread(p.record("asmodeus")))
        p = consumeReveal(p, "asmodeus", Reveal.Codex)
        assertTrue(dossierUnread(p.record("asmodeus")))
        p = consumeReveal(p, "asmodeus", Reveal.Battle)
        assertFalse(dossierUnread(p.record("asmodeus")))
    }

    @Test
    fun anUnrevealedDemonHasNothingUnread() {
        assertFalse(dossierUnread(DemonRecord()))
        assertFalse(dossierUnread(DemonRecord(revealPending = RevealPending.All)))
    }

    @Test
    fun neverSummonedUntilTheFirstCommit() {
        assertTrue(neverSummoned(Progress()))
        assertFalse(neverSummoned(commitSummon(Progress(), "astaroth", 3, flawless = true)))
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew -q testDebugUnitTest --tests '*ProgressRulesTest*'`
Expected: compilation FAIL, `Unresolved reference: dossierUnread`.

- [ ] **Step 3: Implement** — add inside `object ProgressRules` (after `consumeReveal`):

```kotlin
    /** Revealed lore the player has not seen ink in yet: the Grimoire's dossier button glows (ADR-0033). */
    fun dossierUnread(record: DemonRecord): Boolean =
        record.revealed && (record.revealPending.codex || record.revealPending.battle)

    /** No summon was ever committed: the intro's Summon glows (ADR-0033). */
    fun neverSummoned(p: Progress): Boolean = p.lastSummonDay == null
```

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew -q testDebugUnitTest --tests '*ProgressRulesTest*'`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/progress/ProgressRules.kt app/src/test/java/com/summondemons/safely/progress/ProgressRulesTest.kt
git commit -m "feat(progress): dossierUnread and neverSummoned for the attention glow" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: What glows on the chant screen (pure state)

**Files:**
- Modify: `…/safely/ui/screens/ChantScreenState.kt`
- Test: `app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt`

- [ ] **Step 1: Write the failing tests** — append inside `class ChantScreenStateTest`:

```kotlin
    // --- Attention (ADR-0033): one thing glows at a time ---

    @Test
    fun seekingDrawsTheEyeToTheSigil() {
        assertEquals(Attention.Sigil, state(seeking).attention)
    }

    @Test
    fun lockingGlowsNothingWhileTheRingFills() {
        assertEquals(Attention.None, state(seeking.copy(phase = RitualPhase.Locking, azimuth = 90f, lockMs = 300L)).attention)
    }

    @Test
    fun chantingHandsTheGlowToThePhrase() {
        assertEquals(Attention.Phrase, state(chanting).attention)
    }

    @Test
    fun waveringCallsTheEyeBackToTheSigil() {
        assertEquals(Attention.SigilAlarm, state(chanting.copy(phase = RitualPhase.Wavering, azimuth = 0f), azimuth = 0f).attention)
    }

    @Test
    fun theClimaxGlowsNothing() {
        assertEquals(Attention.None, state(chanting.copy(phase = RitualPhase.Climax, phrase = 4)).attention)
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew -q testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: compilation FAIL, `Unresolved reference: Attention`.

- [ ] **Step 3: Implement**

Add after `enum class PillLook …`:

```kotlin
/** The one element that glows to say "act here" (ADR-0033): the sigil while seeking, amber and faster while wavering, the phrase while chanting. */
enum class Attention { None, Sigil, SigilAlarm, Phrase }
```

Add a field to `data class ChantScreenState`, after `val gated: Boolean,`:

```kotlin
    /** What glows now (ADR-0033); the screen turns it off while an error panel shows. */
    val attention: Attention,
```

In `chantScreenState(...)`, add the argument after `gated = gated,`:

```kotlin
        attention = when {
            done -> Attention.None
            ritual.phase == RitualPhase.Seeking -> Attention.Sigil
            ritual.phase == RitualPhase.Wavering -> Attention.SigilAlarm
            ritual.phase == RitualPhase.Chanting -> Attention.Phrase
            else -> Attention.None
        },
```

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew -q testDebugUnitTest --tests '*ChantScreenStateTest*'`
Expected: exit 0 (the existing tests still pass).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreenState.kt app/src/test/java/com/summondemons/safely/ui/screens/ChantScreenStateTest.kt
git commit -m "feat(chant): Attention in the chant screen state (sigil, phrase, alarm)" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: The shared attention glow

**Files:**
- Create: `…/safely/ui/fx/AttentionGlow.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.summondemons.safely.ui.fx

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

/** Half a breath: 1.6 s in and out; [FAST_HALF_MS] while wavering (ADR-0033). */
private const val HALF_MS = 800
private const val FAST_HALF_MS = 400
/** Reduced motion: a steady glow at this level instead of breathing. */
private const val STEADY = 0.6f
private const val GLOW_ALPHA = 0.55f
/** The glow reaches this far past the element's half-size. */
private const val GLOW_SPREAD = 1.6f

/**
 * The attention level, 0..1 (ADR-0033): 0 when not [active]; a steady [STEADY] with reduced
 * motion; otherwise breathing over 1.6 s, or 0.8 s when [fast].
 */
@Composable
fun rememberAttention(active: Boolean, fast: Boolean = false): Float {
    if (!active) return 0f
    if (LocalReduceMotion.current) return STEADY
    return rememberInfiniteTransition(label = "attention").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (fast) FAST_HALF_MS else HALF_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    ).value
}

/**
 * The attention glow behind an element (ADR-0033): a soft [color] halo, elliptical for wide
 * elements, as strong as [level] (from [rememberAttention]). Draws nothing at 0.
 */
fun Modifier.attentionGlow(level: Float, color: Color): Modifier = if (level <= 0f) this else drawBehind {
    val radius = size.minDimension / 2f * GLOW_SPREAD
    scale(scaleX = size.width / size.minDimension, scaleY = size.height / size.minDimension) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = GLOW_ALPHA * level), Color.Transparent), center, radius),
            radius,
            center,
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew -q assembleDebug`
Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/fx/AttentionGlow.kt
git commit -m "feat(fx): Shared breathing attention glow" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Chant screen — glow hand-off and one-line pill

**Files:**
- Modify: `…/safely/ui/screens/ChantScreen.kt` (the `ChantParts(...)` block, ~lines 76–95)
- Modify: `…/safely/ui/screens/ChantPieces.kt` (`DirectionPillView`, `ChantSigil`, `ChantLine`, constants)

- [ ] **Step 1: ChantPieces constants** — after `private const val LINE_FLARE_ALPHA = 0.22f` add:

```kotlin
/** The chanting halo behind the phrase at full breath (ADR-0033). */
private const val LINE_HALO_ALPHA = 0.18f
/** Wavering: the phrase dims while the voice is ignored (ADR-0010, ADR-0033). */
private const val DRIFT_LINE_ALPHA = 0.45f
/** The sigil's extra glow at full breath while it needs the player (ADR-0033). */
private const val GLOW_ATTENTION = 0.35f
private val PillMinSize = 8.sp
```

- [ ] **Step 2: Sigil breathing** — change `ChantSigil`'s signature and glow line:

```kotlin
internal fun ChantSigil(state: RitualUiState, screen: ChantScreenState, accent: Color, flash: SigilFlash?, attention: Float, modifier: Modifier) {
```

and replace

```kotlin
    val glow = (GLOW_BASE + GLOW_LOCK * screen.lockFraction + GLOW_VOICE * voice + GLOW_FLARE * flare).coerceAtMost(1f)
```

with

```kotlin
    val glow = (GLOW_BASE + GLOW_LOCK * screen.lockFraction + GLOW_VOICE * voice + GLOW_FLARE * flare + GLOW_ATTENTION * attention).coerceAtMost(1f)
```

(`glowColor` already turns `colors.primary`, the paused amber, while drifting.)

- [ ] **Step 3: Phrase halo and wavering dim** — replace `ChantLine` with:

```kotlin
@Composable
internal fun ChantLine(
    words: List<ChantWord>,
    gated: Boolean,
    drifting: Boolean,
    lockFraction: Float,
    accent: Color,
    halo: Float,
    onPhraseSize: (Float) -> Unit,
    modifier: Modifier,
) {
    val flare = unlockFlare(gated)
    val alpha = when {
        gated -> lerp(GATED_ALPHA, GATED_ALPHA_LOCKED, lockFraction)
        drifting -> DRIFT_LINE_ALPHA
        else -> 1f
    }
    Box(
        modifier.drawBehind {
            val strength = LINE_FLARE_ALPHA * flare + LINE_HALO_ALPHA * halo
            if (strength > 0f) {
                val radius = size.maxDimension * 0.5f
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = strength), Color.Transparent), radius = radius), radius = radius)
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Phrase(words, accent, onPhraseSize, Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha })
    }
}
```

Update its KDoc first line to: `The phrase floating over the stage, no frame: dim before the lock, brightening as the lock fills, a soft accent flare as it opens, a breathing halo while chanting and dimmed while wavering (ADR-0033).`

- [ ] **Step 4: One-line pill** — in `DirectionPillView`, replace the `Text(label, style = …, color = color, maxLines = 1, modifier = …)` call's first lines with:

```kotlin
    Text(
        label,
        style = MaterialTheme.textRoles.ritualCue.copy(fontSize = PillTextSize.fontScaleCapped(), letterSpacing = 0.12.em),
        color = color,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = PillMinSize, maxFontSize = PillTextSize.fontScaleCapped()),
        modifier = Modifier
```

(the `modifier` chain below it is unchanged).

- [ ] **Step 5: Wire it in ChantScreen** — in `ChantScreen.kt`, just before `val parts = ChantParts(`, add:

```kotlin
        // One thing glows at a time (ADR-0033); nothing while an error panel replaces the line.
        val attention = if (blocker != null) Attention.None else screen.attention
        val sigilGlow = rememberAttention(attention == Attention.Sigil || attention == Attention.SigilAlarm, fast = attention == Attention.SigilAlarm)
        val phraseGlow = rememberAttention(attention == Attention.Phrase)
```

and change the two parts:

```kotlin
            sigil = { m -> ChantSigil(state, screen, accent, flash, sigilGlow, m.onSizeChanged { sigilPx = it.width }) },
```

```kotlin
                    ChantLine(screen.words, screen.gated, screen.drifting, screen.lockFraction, accent, phraseGlow, onPhraseSize = { phraseSp = it }, modifier = m.onSizeChanged { box = it })
```

Add the import `import com.summondemons.safely.ui.fx.rememberAttention`.

- [ ] **Step 6: Build, tests, device check**

Run: `./gradlew -q testDebugUnitTest && ./gradlew -q installDebug`
Expected: exit 0, "Installed on 1 device".
On the S24 (Grimoire → an unbound demon → Tap to summon → Summon): seeking, the sigil breathes; facing the direction, breathing stops while the ring fills; after the lock, the phrase has a soft breathing halo; turning away, the sigil breathes amber, fast, and the phrase dims. At font scale 2.0 the pill reads `FACE EAST →` on one line. Restore font scale 1.0.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/screens/ChantScreen.kt app/src/main/java/com/summondemons/safely/ui/screens/ChantPieces.kt
git commit -m "feat(chant): One thing glows at a time; the direction pill stays on one line" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Parchment colours and the code-drawn ribbon

**Files:**
- Modify: `…/safely/ui/theme/Color.kt` (inside `internal object Palette`)
- Modify: `…/safely/ui/theme/DemonColors.kt`
- Create: `…/safely/ui/components/ParchmentRibbon.kt`

- [ ] **Step 1: Palette** — add inside `internal object Palette`, after `inkHighlight`:

```kotlin
    /** Intro title ribbon (ADR-0033): parchment gradient, its edge and the ink on it. */
    val parchmentLight = Color(0xFFF5DC9C)
    val parchmentDark = Color(0xFFD9B46A)
    val parchmentEdge = Color(0xFF8A6A30)
    val parchmentInk = Color(0xFF3A2400)
```

- [ ] **Step 2: DemonColors** — add the fields to `data class DemonColors` after `val inkHighlight: Color,`:

```kotlin
    val parchmentLight: Color,
    val parchmentDark: Color,
    val parchmentEdge: Color,
    val parchmentInk: Color,
```

and to `DefaultDemonColors` after `inkHighlight = Palette.inkHighlight,`:

```kotlin
    parchmentLight = Palette.parchmentLight,
    parchmentDark = Palette.parchmentDark,
    parchmentEdge = Palette.parchmentEdge,
    parchmentInk = Palette.parchmentInk,
```

- [ ] **Step 3: Create `ParchmentRibbon.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.summondemons.safely.ui.theme.demonColors
import com.summondemons.safely.ui.theme.fontScaleCapped
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

private val RibbonTextSize = 22.sp
/** How far each notched tail reaches in; the text is inset past it. */
private val Tail = 18.dp
private val Edge = 1.dp

/**
 * A title on a parchment ribbon drawn in code (ADR-0033; replaces `chant_intro_banner.webp`): a
 * gradient band with notched tails that grows with its text, so any language, line break or font
 * scale fits. Words on it follow the fixed-art rule (at most 1.3×).
 */
@Composable
fun ParchmentRibbon(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.demonColors
    val spacing = MaterialTheme.spacing
    Box(
        modifier
            .drawBehind {
                val notch = Tail.toPx()
                val band = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - notch, size.height / 2f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    lineTo(notch, size.height / 2f)
                    close()
                }
                drawPath(band, Brush.verticalGradient(listOf(colors.parchmentLight, colors.parchmentDark)))
                drawPath(band, colors.parchmentEdge, style = Stroke(Edge.toPx()))
            }
            .padding(horizontal = Tail + spacing.sm, vertical = spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.upper(),
            style = MaterialTheme.textRoles.name.copy(fontSize = RibbonTextSize.fontScaleCapped(), lineHeight = 1.1.em, textAlign = TextAlign.Center),
            color = colors.parchmentInk,
        )
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew -q assembleDebug`
Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/summondemons/safely/ui/theme/Color.kt app/src/main/java/com/summondemons/safely/ui/theme/DemonColors.kt app/src/main/java/com/summondemons/safely/ui/components/ParchmentRibbon.kt
git commit -m "feat(ui): Code-drawn parchment ribbon and its theme colours" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Intro strings in five languages

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `values-es/strings.xml`, `values-pt-rBR/strings.xml`, `values-it/strings.xml`, `values-fr/strings.xml`

The title gets an explicit line break (the slab is narrow; the break must fall between words in every language). Step titles lose their "1." prefix (a medallion carries the number); steps 3 and 4 become verbs.

- [ ] **Step 1: Replace the five keys in each file** (exact values; bodies are unchanged)

| Key | en | es | pt-BR | it | fr |
|---|---|---|---|---|---|
| `intro_title` | `How to\nchant` | `Cómo\nrecitar` | `Como\nrecitar` | `Come\ncantare` | `Bien\nchanter` |
| `intro_direction_title` | `Face` | `Mira` | `Vire` | `Mira` | `Vise` |
| `intro_chant_title` | `Chant` | `Canta` | `Cante` | `Canta` | `Chant` |
| `intro_lines_title` | `Watch the words` | `Sigue el verso` | `Siga o verso` | `Segui i versi` | `Suis les mots` |
| `intro_focus_title` | `Beat the clock` | `Gana al reloj` | `Ganhe do tempo` | `Batti il tempo` | `Bats l’horloge` |

Example (English):

```xml
    <string name="intro_title">How to\nchant</string>
    <string name="intro_direction_title">Face</string>
    <string name="intro_chant_title">Chant</string>
    <string name="intro_lines_title">Watch the words</string>
    <string name="intro_focus_title">Beat the clock</string>
```

- [ ] **Step 2: Run the translation tests**

Run: `./gradlew -q testDebugUnitTest --tests '*TranslationResourcesTest*' --tests '*EnglishTextTest*'`
Expected: exit 0. If `noLongerThanEnglish` fails for a value, shorten that translation (never lengthen English).

- [ ] **Step 3: Add the new terms to the i18n review queue** — append to `docs/i18n/review.md` one entry per changed key with the five values above, status "new (2026-10-22)", following the file's existing entry format.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values*/strings.xml docs/i18n/review.md
git commit -m "feat(i18n): Intro titles as numbered verbs; two-line title (5 languages)" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Intro card layout B with the Summon glow

**Files:**
- Modify: `…/safely/ui/screens/ChantIntro.kt`
- Modify: `…/safely/ui/screens/RitualScreen.kt` (`RitualScreen`, `RitualContent`, the `ChantIntro(...)` call at ~line 269)
- Modify: `…/safely/navigation/AppNavHost.kt` (the `RitualScreen(` call at ~line 238)
- Delete: `app/src/main/res/drawable-nodpi/chant_intro_banner.webp`

- [ ] **Step 1: Replace `ChantIntro`, `ArtSlab`, `Rules`, `SummonButton`, `Rule`** in `ChantIntro.kt` with the code below; keep `CheckIcon`, `LinesIcon`, `SealsIcon`, `FocusIcon` unchanged. Delete the now-unused constants `BannerTitleColor`, `BannerHeight`, `BannerTitleSize`, `BannerTitleInset`, `BandHeight`, `ART_FRACTION`.

```kotlin
private const val INTRO_SLAB_WEIGHT = 0.27f
private const val INTRO_SLAB_ALPHA = 0.7f
private val SummonButtonHeight = 56.dp
private val SummonLabelSize = 20.sp
/** The button art is fixed-height: large font sizes shrink its word back to fit. */
private val ArtLabelMinSize = 8.sp
private val RuleTitleSize = 18.sp
private val RuleBodySize = 16.sp
private val RuleIcon = 40.dp
private val StepSize = 28.dp
private val StepBorder = 1.5.dp
private val StepNumberSize = 14.sp

/**
 * The intro card (ADR-0019, ADR-0021, layout B of ADR-0033): shown before every ritual and
 * from the chant screen's "?". The left slab holds the title ribbon, the singer and **Summon**
 * (where the direction pill appears next); the right column the four numbered rules, each with
 * the icon the chant screen uses. Generic: nothing names a demon or a direction. The microphone
 * is ignored while it is up (RitualViewModel); **Summon** and ✕ start the chant. Summon glows
 * until the player's first completed summon ([glowSummon]).
 */
@Composable
fun ChantIntro(
    demon: Demon,
    onSummon: () -> Unit,
    showClose: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    glowSummon: Boolean = false,
) {
    val accent = MaterialTheme.demonColors.accent(demon.accent)
    val spacing = MaterialTheme.spacing
    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight
        ChantBackdrop(demon.house.stage(), landscape, Modifier.fillMaxSize())
        if (landscape) {
            Row(Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Slab(accent, glowSummon, onSummon, Modifier.fillMaxHeight().weight(INTRO_SLAB_WEIGHT))
                Rules(accent, Modifier.fillMaxHeight().weight(1f - INTRO_SLAB_WEIGHT))
            }
        } else {
            // Large screens only (the Ritual is landscape): ribbon, rules, then Summon.
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                ParchmentRibbon(stringResource(R.string.intro_title), Modifier.semantics { heading() })
                Rules(accent, Modifier.fillMaxWidth().weight(1f))
                SummonButton(accent, glowSummon, onSummon)
            }
        }
        if (showClose) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding()) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.intro_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Title ribbon over the singer over Summon. */
@Composable
private fun Slab(accent: Color, glowSummon: Boolean, onSummon: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow.copy(alpha = INTRO_SLAB_ALPHA)) {
        Column(
            Modifier.padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ParchmentRibbon(stringResource(R.string.intro_title), Modifier.semantics { heading() })
            Image(
                painter = painterResource(R.drawable.chant_intro_singer),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            SummonButton(accent, glowSummon, onSummon)
        }
    }
}

/** The four rules, centred; they scroll when large font sizes make them taller than the screen. */
@Composable
private fun Rules(accent: Color, modifier: Modifier) {
    val spacing = MaterialTheme.spacing
    Box(modifier.padding(horizontal = spacing.sm), contentAlignment = Alignment.CenterStart) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Rule(1, accent, R.string.intro_direction_title, R.string.intro_direction_body) { CheckIcon() }
            Rule(2, accent, R.string.intro_chant_title, R.string.intro_chant_body) { SealsIcon(accent) }
            Rule(3, accent, R.string.intro_lines_title, R.string.intro_lines_body) { LinesIcon() }
            Rule(4, accent, R.string.intro_focus_title, R.string.intro_focus_body) { FocusIcon(accent) }
        }
    }
}

@Composable
private fun SummonButton(accent: Color, glow: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val label = stringResource(R.string.intro_summon).upper()
    val buttonDrawable = if (isPressed) R.drawable.chant_summon_button_pressed else R.drawable.chant_summon_button
    val inkHighlight = MaterialTheme.demonColors.inkHighlight
    val attention = rememberAttention(glow)
    Box(
        modifier = Modifier
            .attentionGlow(attention, accent)
            .height(SummonButtonHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(buttonDrawable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = label,
            style = MaterialTheme.textRoles.name.copy(fontSize = SummonLabelSize.fontScaleCapped()),
            color = inkHighlight,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = ArtLabelMinSize, maxFontSize = SummonLabelSize.fontScaleCapped()),
        )
    }
}

/** One rule: its step number, the icon that matches the chant screen, a short title, a one-line description. */
@Composable
private fun Rule(step: Int, accent: Color, title: Int, body: Int, icon: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Box(Modifier.size(StepSize).border(StepBorder, accent, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                step.toString(),
                style = MaterialTheme.textRoles.name.copy(fontSize = StepNumberSize.fontScaleCapped()),
                color = accent,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.size(RuleIcon), contentAlignment = Alignment.Center) { icon() }
        Column {
            Text(
                stringResource(title).upper(),
                style = MaterialTheme.textRoles.ritualCue.copy(fontSize = RuleTitleSize),
                color = colors.onSurface,
            )
            Text(
                stringResource(body),
                style = MaterialTheme.textRoles.body.copy(fontSize = RuleBodySize),
                color = colors.onSurfaceVariant,
            )
        }
    }
}
```

Imports to add to `ChantIntro.kt`: `androidx.compose.foundation.border`, `androidx.compose.foundation.shape.CircleShape`, `com.summondemons.safely.ui.components.ParchmentRibbon`, `com.summondemons.safely.ui.fx.attentionGlow`, `com.summondemons.safely.ui.fx.rememberAttention`. Remove imports the compiler reports unused.

- [ ] **Step 2: Pass `firstSummon` down** — in `RitualScreen.kt`:
  - `RitualScreen(...)`: add parameter `firstSummon: Boolean = false,` after `onAbort: () -> Unit,`, and pass `firstSummon = firstSummon,` in the `RitualContent(` call.
  - `RitualContent(...)`: add parameter `firstSummon: Boolean,` after `onAbort: () -> Unit,`.
  - The intro call becomes:

```kotlin
            ChantIntro(demon = demon, onSummon = onDismissIntro, showClose = true, onClose = onDismissIntro, glowSummon = firstSummon, modifier = Modifier.fillMaxSize())
```

  In `AppNavHost.kt`, in the `RitualScreen(` call, add `firstSummon = ProgressRules.neverSummoned(state.progress),` (import `com.summondemons.safely.progress.ProgressRules` if missing).

- [ ] **Step 3: Retire the banner art**

```bash
git rm app/src/main/res/drawable-nodpi/chant_intro_banner.webp
```

(If the file lives in another `drawable*` folder, `git ls-files | grep chant_intro_banner` finds it.)

- [ ] **Step 4: Build, tests, device check**

Run: `./gradlew -q testDebugUnitTest && ./gradlew -q installDebug`
Expected: exit 0.
On the S24: the intro shows the two-line ribbon, singer and Summon on the left, rules 1–4 on the right. With no summon ever made (Settings → Debug controls → reset all), Summon breathes; after one summon it is calm. At font scale 2.0: the ribbon's words stay inside it, Summon is visible, the rules scroll. Restore font scale 1.0.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/com/summondemons/safely/ui/screens/ChantIntro.kt app/src/main/java/com/summondemons/safely/ui/screens/RitualScreen.kt app/src/main/java/com/summondemons/safely/navigation/AppNavHost.kt app/src/main/res
git commit -m "feat(intro): Layout B: ribbon, singer and Summon on the slab; numbered rules; first-summon glow" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: FlipCard and the flip icon

**Files:**
- Create: `…/safely/ui/components/FlipCard.kt`
- Create: `app/src/main/res/drawable/ic_flip.xml`

- [ ] **Step 1: Icon** — `ic_flip.xml` (Material "autorenew", Apache 2.0):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,6v3l4,-4 -4,-4v3c-4.42,0 -8,3.58 -8,8 0,1.57 0.46,3.03 1.24,4.26L6.7,14.8c-0.45,-0.83 -0.7,-1.79 -0.7,-2.8 0,-3.31 2.69,-6 6,-6zM18.76,7.74L17.3,9.2c0.44,0.84 0.7,1.79 0.7,2.8 0,3.31 -2.69,6 -6,6v-3l-4,4 4,4v-3c4.42,0 8,-3.58 8,-8 0,-1.57 -0.46,-3.03 -1.24,-4.26z" />
</vector>
```

- [ ] **Step 2: `FlipCard.kt`**

```kotlin
package com.summondemons.safely.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.summondemons.safely.ui.fx.LocalReduceMotion

/** Which face of the demon card shows (spec §2). UI state, saved across configuration changes. */
enum class CardSide { Front, Back }

private const val FLIP_MS = 400
private const val FADE_MS = 150
private const val CAMERA_DISTANCE = 12f

/**
 * A tarot card with two faces (spec §2): it turns about its vertical axis in 400 ms, showing
 * [front] up to 90° and [back] after; a 150 ms cross-fade with reduced motion.
 */
@Composable
fun FlipCard(side: CardSide, modifier: Modifier = Modifier, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    if (LocalReduceMotion.current) {
        Crossfade(side, modifier, animationSpec = tween(FADE_MS), label = "cardFade") { shown ->
            Box(Modifier.fillMaxSize()) { if (shown == CardSide.Front) front() else back() }
        }
        return
    }
    val angle by animateFloatAsState(if (side == CardSide.Front) 0f else 180f, tween(FLIP_MS), label = "cardFlip")
    val density = LocalDensity.current.density
    Box(
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = CAMERA_DISTANCE * density
        },
    ) {
        if (angle <= 90f) {
            Box(Modifier.fillMaxSize()) { front() }
        } else {
            // Un-mirror the back face.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { back() }
        }
    }
}
```

- [ ] **Step 3: Build and commit**

Run: `./gradlew -q assembleDebug` — expected exit 0.

```bash
git add app/src/main/java/com/summondemons/safely/ui/components/FlipCard.kt app/src/main/res/drawable/ic_flip.xml
git commit -m "feat(ui): FlipCard and the flip icon" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: DossierList replaces DossierCarousel

**Files:**
- Rename: `…/safely/ui/components/DossierCarousel.kt` → `…/safely/ui/components/Dossier.kt`
- Modify: `…/safely/ui/components/Dossier.kt`

- [ ] **Step 1: Rename**

```bash
git mv app/src/main/java/com/summondemons/safely/ui/components/DossierCarousel.kt app/src/main/java/com/summondemons/safely/ui/components/Dossier.kt
```

- [ ] **Step 2: Delete from `Dossier.kt`**: the `DossierCarousel` composable, `PageButton`, `PageArrowWidth`, `PageArrowHeight`, and the constants only they use (`PAGE_ANIM_MS`, `PAGE_SLIDE_DIVISOR`, `Swipe`). Keep `DOSSIER_PAGES`, `SECRET_PAGE`, `pageTitle`, `PageBody`, `InkLine`, `Smudge` and their constants.

- [ ] **Step 3: Seal lines under smudges** — in `PageBody`, after each `Smudge(...)` call in a `!record.revealed` branch (pages 1 and the `else` branch), add:

```kotlin
            SealLine(stringResource(R.string.dossier_sealed))
```

and in the Secret branch replace

```kotlin
            Text(stringResource(R.string.dossier_secret_locked).upper(), style = MaterialTheme.textRoles.meta)
```

with

```kotlin
            SealLine(stringResource(R.string.dossier_secret_locked))
```

Add the helper below `PageBody`:

```kotlin
/** How to unseal a section: a small meta line under its smudge. */
@Composable
private fun SealLine(text: String) {
    Text(text.upper(), style = MaterialTheme.textRoles.meta, modifier = Modifier.padding(top = MaterialTheme.spacing.xs))
}
```

- [ ] **Step 4: Add `DossierList`** (top of the file, after the constants):

```kotlin
/**
 * The dossier on the back of the demon card (spec §2; replaces the carousel): the six sections
 * (ADR-0011) in one scrolling list, each a header and its text. Sealed sections show a smudge
 * and how to unseal them; [inkCodex] / [inkBattle] ink revealed lines in once. When
 * [scrollToSecret] turns true the list scrolls to its end, where the Secret is.
 */
@Composable
fun DossierList(
    demon: Demon,
    record: DemonRecord,
    inkCodex: Boolean,
    inkBattle: Boolean,
    scrollToSecret: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val scroll = rememberScrollState()
    LaunchedEffect(demon.id) { scroll.scrollTo(0) }
    LaunchedEffect(scrollToSecret) { if (scrollToSecret) scroll.animateScrollTo(scroll.maxValue) }
    Column(modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        repeat(DOSSIER_PAGES) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(pageTitle(page, demon, record).upper(), style = MaterialTheme.textRoles.meta)
                PageBody(page, demon, record, inkCodex, inkBattle, colors.onSurface)
            }
        }
    }
}
```

Imports needed (add if missing): `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`, `androidx.compose.runtime.LaunchedEffect`. Remove imports the compiler reports unused.

- [ ] **Step 5: Build** (`GrimoireScreen.kt` still calls `DossierCarousel`, so expect exactly that error)

Run: `./gradlew -q assembleDebug`
Expected: FAIL only with `Unresolved reference: DossierCarousel` in `GrimoireScreen.kt` (fixed in Task 10). Do not commit yet.

---

### Task 10: Demon card in its own file, front and back

**Files:**
- Create: `…/safely/ui/components/DemonCard.kt`
- Modify: `…/safely/ui/screens/GrimoireScreen.kt` (remove `DemonCard`, `CardHeader` and their constants; state and wiring)
- Create: string keys `grimoire_show_dossier`, `grimoire_show_profile` in all five `strings.xml`

- [ ] **Step 1: Strings** (all ≤ English length)

| Key | en | es | pt-BR | it | fr |
|---|---|---|---|---|---|
| `grimoire_show_dossier` | `Show dossier` | `Ver dosier` | `Ver dossiê` | `Vedi dossier` | `Voir dossier` |
| `grimoire_show_profile` | `Show profile` | `Ver perfil` | `Ver perfil` | `Vedi profilo` | `Voir profil` |

- [ ] **Step 2: Create `DemonCard.kt`** — move `DemonCard` and `CardHeader` here from `GrimoireScreen.kt` (with `MedallionSize`, `NameSize`, `NumberSize`, `TitleSize`, `CARD_ALPHA`, `METAL_RING_ALPHA`, `CarouselTopPadding`), make `DemonCard` `internal`, and replace its body and signature with:

```kotlin
/**
 * The demon card (ADR-0016, spec §2), a tarot card with two faces. Front: medallion, name,
 * title, the info band and the badges; [onFixedHeight] reports its height, which the Grimoire
 * keeps for both faces. Back: the dossier list. The ⟲ button in the header flips it and glows
 * while the dossier has unread text ([glowFlip]).
 */
@Composable
internal fun DemonCard(
    demons: List<Demon>,
    demon: Demon,
    record: DemonRecord,
    side: CardSide,
    onFlip: () -> Unit,
    glowFlip: Boolean,
    badge: BadgeKey?,
    onBadge: (BadgeKey?) -> Unit,
    inkCodex: Boolean,
    inkBattle: Boolean,
    scrollToSecret: Boolean,
    onFixedHeight: (Int) -> Unit,
    onBadgeRowBounds: (Rect) -> Unit,
    modifier: Modifier,
) {
    val spacing = MaterialTheme.spacing
    FlipCard(
        side = side,
        modifier = modifier,
        front = {
            CardFace {
                Column(Modifier.onSizeChanged { onFixedHeight(it.height) }) {
                    CardHeader(demon, record.bound, side, onFlip, glowFlip, Modifier.padding(horizontal = spacing.lg))
                    InfoBand(demons, demon, badge, Modifier.padding(horizontal = spacing.lg).padding(top = spacing.lg))
                    BadgeRow(
                        demon = demon,
                        selected = badge,
                        onSelect = onBadge,
                        modifier = Modifier
                            .padding(horizontal = spacing.lg)
                            .padding(top = spacing.lg)
                            .onGloballyPositioned { onBadgeRowBounds(it.boundsInRoot()) },
                    )
                }
            }
        },
        back = {
            CardFace {
                CardHeader(demon, record.bound, side, onFlip, glowFlip, Modifier.padding(horizontal = spacing.lg))
                DossierList(
                    demon = demon,
                    record = record,
                    inkCodex = inkCodex,
                    inkBattle = inkBattle,
                    scrollToSecret = scrollToSecret,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = CarouselTopPadding).padding(horizontal = spacing.lg),
                )
            }
        },
    )
}

/** One face: the card's surface, border and insets; [content] is laid out in a column. */
@Composable
private fun CardFace(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        color = colors.surfaceContainerLow.copy(alpha = CARD_ALPHA),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = spacing.lg, bottom = spacing.md), content = content)
    }
}
```

In `CardHeader`, change the signature to `private fun CardHeader(demon: Demon, bound: Boolean, side: CardSide, onFlip: () -> Unit, glowFlip: Boolean, modifier: Modifier)` and add, as the last child of its `Row` (after the name/title `Column(Modifier.weight(1f))`):

```kotlin
        val flipLabel = stringResource(if (side == CardSide.Front) R.string.grimoire_show_dossier else R.string.grimoire_show_profile)
        val attention = rememberAttention(glowFlip)
        IconButton(onClick = onFlip, modifier = Modifier.attentionGlow(attention, accent)) {
            Icon(painterResource(R.drawable.ic_flip), contentDescription = flipLabel, tint = if (glowFlip) accent else colors.onSurfaceVariant)
        }
```

Imports for `DemonCard.kt` (in addition to those the moved code already used): `androidx.compose.foundation.layout.ColumnScope`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.material3.Icon`, `androidx.compose.material3.IconButton`, `androidx.compose.ui.res.painterResource`, `com.summondemons.safely.ui.fx.attentionGlow`, `com.summondemons.safely.ui.fx.rememberAttention`.

- [ ] **Step 3: GrimoireScreen state** — in `GrimoireScreen(...)`:

Remove `var page by remember(demon.id) { mutableIntStateOf(0) }` and the `LaunchedEffect(demon.id) { if (inkCodex) … if (inkBattle) … }` block. Add after the `inkBattle` line:

```kotlin
    // The card's face (spec §2): kept when switching demon; back turns it over before leaving.
    var side by rememberSaveable { mutableStateOf(CardSide.Front) }
    var scrollToSecret by remember(demon.id) { mutableStateOf(false) }
    // Ink plays where it can be seen: the reveals are consumed when the back is shown.
    LaunchedEffect(demon.id, side) {
        if (side == CardSide.Back) {
            if (inkCodex) onConsumeReveal(demon.id, Reveal.Codex)
            if (inkBattle) onConsumeReveal(demon.id, Reveal.Battle)
        }
    }
    // Awesome! (or back) ends the celebration and turns the card to the newly revealed dossier.
    var wasParty by remember { mutableStateOf(party != null) }
    LaunchedEffect(party != null) {
        if (wasParty && party == null) side = CardSide.Back
        wasParty = party != null
    }
```

Replace the `openSecret` effect body:

```kotlin
    LaunchedEffect(state.openSecret) {
        if (state.openSecret) {
            side = CardSide.Back
            scrollToSecret = true
            onSecretShown()
        }
    }
```

After `BackHandler(enabled = party != null) { onDismissCelebration() }` add:

```kotlin
    BackHandler(enabled = party == null && side == CardSide.Back) { side = CardSide.Front }
```

- [ ] **Step 4: GrimoireScreen card height and call** — replace the `minimum`/`cardHeight` lines with:

```kotlin
                // The front's natural height (header + band + badges), kept for both faces; never below the fraction.
                val minimum = with(density) { fixedPx.toDp() } + spacing.lg + spacing.md + navBottom
                val cardHeight = maxOf(maxHeight * CARD_HEIGHT_FRACTION, minimum)
```

set `private const val CARD_HEIGHT_FRACTION = 0.36f` (was 0.5; the carousel's space goes to the stage), delete `CarouselMinHeight`, and replace the `DemonCard(` call with:

```kotlin
                    DemonCard(
                        demons = demons,
                        demon = demon,
                        record = record,
                        side = side,
                        onFlip = { side = if (side == CardSide.Front) CardSide.Back else CardSide.Front },
                        glowFlip = ProgressRules.dossierUnread(record),
                        badge = badge,
                        onBadge = {
                            badge = it
                            badgeTaps++
                        },
                        inkCodex = inkCodex,
                        inkBattle = inkBattle,
                        scrollToSecret = scrollToSecret,
                        onFixedHeight = { fixedPx = it },
                        onBadgeRowBounds = { badgeRowBounds = it },
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
                    )
```

Remove imports the compiler reports unused (`SECRET_PAGE`, `DossierCarousel`, …); add `com.summondemons.safely.ui.components.CardSide` and `com.summondemons.safely.ui.components.DemonCard`. Update the `GrimoireScreen` KDoc to "a stage over the demon card, a tarot card whose back is the dossier (ADR-0016 amended, spec §2)".

- [ ] **Step 5: Build, tests, device check**

Run: `./gradlew -q testDebugUnitTest && ./gradlew -q installDebug`
Expected: exit 0.
On the S24 at 1.0 and 2.0 (restore 1.0 after): the front shows header, band, badges with no empty gap — if the front leaves a gap larger than one badge row at 1.0, lower `CARD_HEIGHT_FRACTION` in steps of 0.02 until it doesn't; ⟲ flips (400 ms); back lists six sections, sealed ones with a smudge and a seal line; system back flips to the front, then leaves; switching demon keeps the side. Summon a demon (debug: "Bind" from the debug panel, or a real ritual): after Awesome! the card flips and the lore inks in; before flipping, ⟲ glows.

- [ ] **Step 6: Commit** (includes Task 9)

```bash
git add -A app/src/main/java/com/summondemons/safely/ui app/src/main/res/values*/strings.xml
git commit -m "feat(grimoire): Tarot flip: identity on the front, the dossier list on the back" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Configuration matrix script

**Files:**
- Create: `tools/ui_matrix.sh` (executable)

- [ ] **Step 1: Create the script**

```bash
#!/usr/bin/env bash
# Screenshots every screen in every configuration of the pre-release matrix (spec §5).
# Usage: tools/ui_matrix.sh [label]        label: e.g. s24-en (default: device model + "run")
# Needs: a debug build installed, one device on adb. Sets font scale and display size per run,
# asks you to bring the app to each screen/state (the debug panel reaches the states), captures,
# and always restores the device settings.
set -euo pipefail

PKG=com.summondemons.safely
LABEL=${1:-"$(adb shell getprop ro.product.model | tr -d '\r')-run"}
OUT="build/ui-matrix/$LABEL"
FONT_SCALES=(1.0 1.3 2.0)
SHOTS=(
  "home"            "Home"
  "grimoire-front"  "Grimoire, unbound demon, front"
  "grimoire-back"   "Grimoire, same demon, back (tap the flip button)"
  "grimoire-bound"  "Grimoire, a bound demon (debug: Bind), front"
  "grimoire-cool"   "Grimoire, cooling (after today's summon), front"
  "celebration"     "Celebration (right after a real summon; s to skip)"
  "intro"           "Intro card (Tap to summon)"
  "chant-seeking"   "Chant screen, seeking"
  "chant-chanting"  "Chant screen, after the lock (debug: Simulate heading)"
  "abort"           "Abort confirmation (after the lock, tap X)"
  "settings"        "Settings sheet"
)

orig_font=$(adb shell settings get system font_scale | tr -d '\r')
orig_density=$(adb shell wm density | sed -n 's/.*Override density: //p' | tr -d '\r')
phys_density=$(adb shell wm density | sed -n 's/Physical density: //p' | tr -d '\r')
base_density=${orig_density:-$phys_density}
DENSITIES=("$base_density" "$((base_density * 115 / 100))")

restore() {
  adb shell settings put system font_scale "$orig_font"
  if [ -n "$orig_density" ]; then adb shell wm density "$orig_density"; else adb shell wm density reset; fi
  echo "Restored font_scale=$orig_font density=${orig_density:-physical}"
}
trap restore EXIT

for density in "${DENSITIES[@]}"; do
  for font in "${FONT_SCALES[@]}"; do
    dir="$OUT/font${font}-dpi${density}"
    mkdir -p "$dir"
    adb shell wm density "$density"
    adb shell settings put system font_scale "$font"
    adb shell am force-stop "$PKG"
    adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
    echo "== font $font, density $density -> $dir"
    for ((i = 0; i < ${#SHOTS[@]}; i += 2)); do
      read -r -p "  ${SHOTS[i + 1]}: Enter to capture, s to skip " answer
      [ "$answer" = "s" ] && continue
      adb exec-out screencap -p > "$dir/${SHOTS[i]}.png"
    done
  done
done
echo "Screenshots in $OUT"
```

- [ ] **Step 2: Make executable, syntax check, commit**

```bash
chmod +x tools/ui_matrix.sh && bash -n tools/ui_matrix.sh
git add tools/ui_matrix.sh
git commit -m "chore(tools): ui_matrix.sh screenshots the pre-release configuration matrix" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Expected: `bash -n` prints nothing.

- [ ] **Step 3: Note it in AGENTS.md** — in the repo-map `tools/` row, add `ui_matrix.sh` for configuration screenshots (spec 2026-10-22 §5); commit with `docs(agents): …`.

---

### Task 12: Docs

**Files:**
- Create: `docs/decisions/0033-attention-glow-and-large-text.md`
- Modify: `docs/decisions/0016-demon-card-replaces-profile.md`, `0020-direction-in-the-sigil.md`, `0021-intro-every-ritual-and-slab-buttons.md`, `0029-chant-screen-ux.md`, `PRODUCT.md`, `DESIGN.md`

- [ ] **Step 1: ADR-0033** — create with this content:

```markdown
# 0033 — Attention glow and large-text rules

- Date: 2026-10-22
- Status: Accepted
- Spec: docs/superpowers/specs/2026-10-22-pre-release-screens-design.md

## Context
The pre-release review (docs/reviews/2026-10-22-pre-release-ui-review.md) found text clipped
or pushed off-screen at large font sizes on the Grimoire, the intro card and the chant screen,
and players unsure where to look next on the chant screen.

## Decision
1. **Text sizing.** Body text scales fully and its container scrolls. Words on fixed art or HUD
   slots scale at most 1.3× (`fontScaleCapped`) and auto-size down. Labels in one row share one
   size. One-line labels never wrap. A screen's primary action never scrolls away.
2. **One attention cue.** A breathing glow (1.6 s; 0.8 s for alarm; steady with reduce motion)
   marks the one thing to act on: the Grimoire's dossier button while lore is unread, the intro's
   Summon until the first completed summon, the chant sigil while seeking (amber while
   wavering) and a halo behind the phrase while chanting.
3. **Grimoire:** the demon card is a tarot card; its back is the dossier list (amends ADR-0016).
4. **Intro card:** layout B — ribbon, singer and Summon on the left slab, numbered rules on the
   right; the ribbon is drawn in code (amends ADR-0021).

## Consequences
- `chant_intro_banner.webp` and `DossierCarousel` are gone.
- Every new screen applies these rules; `tools/ui_matrix.sh` checks them per configuration.
- Open: whether the intro's slab should sit on the right (play-test question).
```

- [ ] **Step 2: Amendments** — append an `## Amendment (2026-10-22)` section to ADR-0016 ("The dossier carousel is replaced by the back of a flipping card; the card's height is the front's natural height, at least 36 % of the area below the header; ADR-0033"), ADR-0021 ("Layout B: ribbon, singer and Summon on the left slab; rules numbered 1–4 with their icons; ADR-0033"), and a one-line note to ADR-0020 and ADR-0029 ("The sigil breathes while seeking and wavering; the phrase has a halo while chanting; ADR-0033"). Update each `Status:` line with `; amended 2026-10-22`.

- [ ] **Step 3: PRODUCT.md** — Grimoire and Demon card rows: replace "a six-page dossier carousel" with "a tarot card: the front shows identity, description and badges; the ⟲ button turns it to the dossier, six sections in one list"; Ritual setup row: add "Before every ritual, the intro card: title, singer and Summon on the left, the four numbered rules on the right (ADR-0033)."

- [ ] **Step 4: DESIGN.md** — add a "Text sizing" and an "Attention glow" subsection with §1 of the spec; in the asset table, mark `chant_intro_banner` as retired (ParchmentRibbon, ADR-0033); describe `ParchmentRibbon`, `FlipCard`, `DossierList` in the component contracts.

- [ ] **Step 5: Commit**

```bash
git add docs/decisions PRODUCT.md DESIGN.md
git commit -m "docs(adr): ADR-0033 attention glow and large-text rules; amend 0016/0020/0021/0029" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 13: Run the matrix and fix

- [ ] **Step 1: S24, English** — `tools/ui_matrix.sh s24-en` (set the app language to English in Settings first). Check every screenshot against spec §5.3.
- [ ] **Step 2: S24, French and pt-BR** — switch the app language in Settings; run `tools/ui_matrix.sh s24-fr` and `tools/ui_matrix.sh s24-ptbr`.
- [ ] **Step 3: Small phone** — `emulator -avd Medium_Phone_API_37.0 &`, wait for boot (`adb wait-for-device`), `./gradlew -q installDebug`, `tools/ui_matrix.sh emu-en` (unplug or `adb -s` select if the S24 is also connected: export `ANDROID_SERIAL=emulator-5554`).
- [ ] **Step 4: Fix** — for each failing screenshot, apply the matching §1.1 rule to the offending text, rebuild, re-capture only that configuration. One commit per fix: `fix(ui): <screen>: <what> at <config>`.
- [ ] **Step 5: Final verification**

Run: `./gradlew -q testDebugUnitTest; echo $?` → `0`. `git status --short` → clean. Update `docs/reviews/2026-10-22-pre-release-ui-review.md` with a "Matrix run" section (configurations covered, fixes made) and commit `docs(review): Matrix run results`.

---

## Self-review notes

- Spec §1.1 → Tasks 4 (pill), 5 (ribbon), 7 (button, step numbers, scrolling rules), 10 (card); §1.2 → Tasks 1, 3, 4, 7, 10; §2 → Tasks 8–10; §3 → Tasks 5–7; §4 → Tasks 2, 4; §5 → Tasks 11, 13; §6 docs → Task 12.
- Names used across tasks: `rememberAttention`, `attentionGlow`, `Attention`, `CardSide`, `FlipCard`, `DossierList`, `DemonCard`, `ParchmentRibbon`, `dossierUnread`, `neverSummoned`, `firstSummon`, `glowSummon`, `glowFlip`, `scrollToSecret`.
