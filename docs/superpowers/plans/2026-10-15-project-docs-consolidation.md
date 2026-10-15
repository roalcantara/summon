# Project Docs Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the repo one source of truth — root `AGENTS.md`, `CLAUDE.md`, `PRODUCT.md`, `DESIGN.md` and ADRs — and archive all generated inputs under `reference/`.

**Architecture:** Pure documentation and file moves. `assets/` is renamed to `reference/` with `git mv` (history kept), the out-of-repo `assets/web` symlink is deleted, and canonical docs are written at the root. No Kotlin or Gradle file changes.

**Tech Stack:** Markdown, git, POSIX shell. Spec: `docs/superpowers/specs/2026-10-15-project-docs-consolidation-design.md`.

**Branch:** `docs/consolidation` (already checked out). All commands run from the repo root `/Users/roalcantara/Work/mydemon/mydemon`.

**Commit trailer:** every commit message ends with a blank line and
`Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

---

## File map

| Path | Action | Responsibility |
|---|---|---|
| `assets/` → `reference/` | Move | Archive of generated inputs |
| `assets/web` | Delete | Broken out-of-repo symlink |
| `reference/README.md` | Create | Marks the folder non-canonical |
| `reference/prompts/01-game-mvp.md` | Create | Prompt 1, verbatim |
| `reference/prompts/02-ui-design-system.md` | Create | Prompt 2, verbatim |
| `PRODUCT.md` | Create | Game rules, data, scope |
| `DESIGN.md` | Create | Tokens, type, shapes, components, assets |
| `docs/decisions/0001…0006-*.md` | Create | ADRs |
| `AGENTS.md` | Create | Agent entry point |
| `CLAUDE.md` | Create | `@AGENTS.md` import |

---

### Task 1: Move `assets/` to `reference/` and drop the web symlink

**Files:**
- Move: `assets/stitch_summon_demons_safely_ui/` → `reference/stitch/`
- Move: `assets/docs/` → `reference/docs/`
- Move: `assets/src/` → `reference/src/`
- Delete: `assets/web`

- [ ] **Step 1: Verify the starting state**

Run: `test -L assets/web && test -d assets/src && test ! -e reference && echo OK`
Expected: `OK`

- [ ] **Step 2: Remove the symlink**

```bash
git rm -q assets/web
```

- [ ] **Step 3: Move the folders**

```bash
mkdir -p reference
git mv assets/stitch_summon_demons_safely_ui reference/stitch
git mv assets/docs reference/docs
git mv assets/src reference/src
rmdir assets 2>/dev/null || true
```

- [ ] **Step 4: Verify**

Run: `test ! -e assets && ls reference && git status --short | grep -c '^R'`
Expected: `docs  src  stitch` listed, and a rename count of `31` (21 files in stitch, 3 in docs, 7 Kotlin).

Run: `git status --short | grep '^D'`
Expected: exactly one line, `D  assets/web`.

- [ ] **Step 5: Confirm Gradle is unaffected**

Run: `./gradlew -q help >/dev/null && echo GRADLE_OK`
Expected: `GRADLE_OK`

- [ ] **Step 6: Commit**

```bash
git commit -q -m "chore(reference): Move generated inputs from assets/ to reference/

Drop the assets/web symlink, which pointed outside the repo.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `reference/README.md` and the verbatim prompts

**Files:**
- Create: `reference/README.md`
- Create: `reference/prompts/01-game-mvp.md`
- Create: `reference/prompts/02-ui-design-system.md`

- [ ] **Step 1: Write `reference/README.md`**

````markdown
# reference/ — generated input, NOT canonical

Everything here was produced by AI tools (Google Stitch, AI Studio) or written as
one-off prompts. It is kept for history and as visual reference only.

**When anything here disagrees with the canon, the canon wins:**
`PRODUCT.md` / `DESIGN.md` → `docs/decisions/` → code → `reference/`.

Do not edit files in this folder. Record new decisions in `docs/decisions/` and
update the root docs instead.

| Path | What it is |
|---|---|
| `prompts/01-game-mvp.md` | Prompt 1 given to Stitch: game loop, sensors, audio, demon data |
| `prompts/02-ui-design-system.md` | Prompt 2 given to Stitch: theme, components, Home and Profile screens |
| `stitch/` | Stitch output: mockups (`code.html` + `screen.png`), character art, logo, sigil, Stitch's own `DESIGN.md`, asset guide |
| `docs/` | Stitch/AI Studio chat summaries (`implementation_plan.md`, `design.md`, `astaroh.md`) |
| `src/` | Draft Jetpack Compose code from Stitch. Input for the Compose port; it does not compile as-is |

The interactive web simulator lives in a sibling repo, `../mdweb` (see `AGENTS.md`).
````

- [ ] **Step 2: Write `reference/prompts/01-game-mvp.md`**

Content is the header line below followed by prompt 1 **exactly** as the user gave it (including the "Damons" typo):

```markdown
# Prompt 1 — Game MVP (given to Stitch, verbatim)

Role & Goal:
Act as an expert Android Developer. Build a one-day MVP for a native Android game called "Summon Damons Safely" using Kotlin and Jetpack Compose. The aesthetic is colorful, cute, and slightly childish (similar to Pokémon Go), contrasting the occult theme. Ensure the code follows Modern Android Development (MAD) guidelines.
Game Flow & Mechanics:
The Grimoire (Home Screen): A Jetpack Compose LazyColumn showing 2 playable Demons (Asmodeus and Astaroth). Each card shows a cute placeholder illustration, attributes, and their sigil.
The Ritual Setup: Tapping a demon navigates to the Ritual Screen. Display a large, opaque version of their sigil. Text instructs the user to "Hold the phone with both hands and face North".
Sensor Integration: Use Android's SensorManager (Magnetometer/Accelerometer) to calculate the device's azimuth. The game state only becomes "Active" when the phone is facing North (azimuth within 340° to 20°).
The Chanting Phase (Audio Detection):
Display the demon's incantation text on the screen, initially colored grey.
Use AudioRecord to monitor microphone input volume (request RECORD_AUDIO permissions).
Instead of complex speech-to-text, use a volume threshold approach: if the user produces sustained audio above a specific amplitude threshold for 2 seconds while facing North, advance the highlight to the next phrase (using the demon's theme color) and increase the alpha/opacity of the sigil by 25%.
If the device orientation drops away from North, reset the progress state to 0.
Success: When the chant is complete, the full sigil glows (opacity 1.0f). Trigger a simple animateDpAsState or updateTransition bounce animation of a cute demon appearing, accompanied by a placeholder "Tada!" sound effect using MediaPlayer.
Demon Data:
Asmodeus:
Theme Color: Color.Yellow
Chant: "DAEMONIUM RESIDENTE, OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS, UT VOTA OMNIA OPUS EST".
Astaroth:
Theme Color: Color.Magenta (Purple)
Chant: "TUI GRATIA LOVIS GRATIA SIT CURA. HIC EST ME DIGNUS LEGI SINT BENE OBEDIENTES".
Implementation Steps:
Please provide the complete code for:
The MainActivity.kt setting up the Compose Navigation.
The SensorManager and AudioRecord logic abstracted into a clean ViewModel (or separate helper classes) using Kotlin Coroutines/Flow to emit state updates.
The Compose UI functions for the Grimoire List and the Ritual Screen.
Instructions on what permissions (e.g., RECORD_AUDIO) to add to the AndroidManifest.xml.
```

- [ ] **Step 3: Write `reference/prompts/02-ui-design-system.md`**

```markdown
# Prompt 2 — UI design system (given to Stitch, verbatim)

Role & Goal:
Act as an expert Android Developer using Jetpack Compose. I need to build the UI layer for the "Summon Damons Safely" MVP. First, establish a centralized, cute, and colorful Design System. Then, generate the HomeScreen (Grimoire) and the DemonProfileScreen (Success/View) using those exact shared components so the game looks highly cohesive.
Step 1: Establish the Theme (Theme.kt & Components.kt)
Colors: Define a custom MaterialTheme with a bright, "Pokémon Go" style palette. Use a soft off-white/cream for the background, a vibrant primary color (e.g., Amber or Gold), and distinct accent colors for the demons (Yellow for Asmodeus, Purple for Astaroth).
Shapes: Define a Shapes object heavily relying on RoundedCornerShape(16.dp) or 24.dp to give everything a friendly, bubbly, and childish look.
Reusable Components: Create a PrimaryActionButton composable that takes text and an onClick lambda. It should have a vibrant background color, rounded corners, white bold text, and a slight drop shadow. Create a DemonCardBase composable with an elevated surface and rounded corners to be used as the foundation for grid items.
Step 2: Generate the HomeScreen (Grimoire)
Use a Scaffold with a top bar titled "My Grimoire" in bold, friendly typography.
The body should be a LazyVerticalGrid displaying the demons.
Use the DemonCardBase. If the demon is unlocked, show their specific theme color as a subtle border or background tint, display their cute illustration, and use the PrimaryActionButton to say "View".
If locked, tint the card grey, show a silhouette, and use the PrimaryActionButton to say "Summon".
Step 3: Generate the DemonProfileScreen (Unified Success & View)
Use the same Scaffold and background color from the MaterialTheme.
The Banner: If isNewSummon == true, display a custom composable RibbonBanner at the top using the theme's primary color, displaying "New Demon Received!" in white bold text.
The Background: Implement the rotating shiny sunburst effect behind the character using a standard Compose Canvas and infiniteRepeatable rotation animation.
The Character & Info: Center the demon image. Place their name below in the theme's largest header typography, followed by their description in a clean, readable sans-serif.
The Action: Pin the PrimaryActionButton to the bottom of the screen. If isNewSummon, it says "Awesome!". If not, it says "Back".
Output Requirements:
Provide the complete Kotlin code separated into clear blocks for:
Theme.kt (Colors, Typography, Shapes).
Components.kt (The reusable buttons and cards).
HomeScreen.kt.
DemonProfileScreen.kt.
Ensure all composables strictly utilize the custom MaterialTheme rather than hardcoding colors and padding on the fly.
```

- [ ] **Step 4: Verify**

Run: `grep -c "LOVIS" reference/prompts/01-game-mvp.md; grep -c "LazyVerticalGrid" reference/prompts/02-ui-design-system.md`
Expected: `1` and `1`.

- [ ] **Step 5: Commit**

```bash
git add reference/README.md reference/prompts
git commit -q -m "docs(reference): Add README and the two original Stitch prompts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `PRODUCT.md`

**Files:**
- Create: `PRODUCT.md`

- [ ] **Step 1: Write `PRODUCT.md`**

````markdown
# PRODUCT — Summon Demons Safely

Canonical product spec. Supersedes `reference/prompts/*` and everything else in
`reference/`. Decisions behind these rules live in `docs/decisions/`.

## Pitch

A small native Android game where the player summons cute chibi Goetic demons by
facing each demon's required direction (North for the first two) and chanting a
Latin incantation into the microphone. Dark-fantasy RPG presentation, comedic tone.

## Core loop

Grimoire → tap demon → Ritual (face the demon's required direction + chant) →
Profile (doubles as the success screen) → back to Grimoire.

## Screens and rules

| Topic | Rule |
|---|---|
| Grimoire (Home) | `Scaffold` with top bar "My Grimoire"; `LazyVerticalGrid` of demon cards. |
| Lock state | Both demons start **locked**. Locked card: grey tint, silhouette, **Summon** → Ritual. Unlocked card: theme-color tint/border, illustration, **View** → Profile. Unlock state is in-memory only. |
| Ritual setup | Large faint sigil; text "Hold the phone with both hands and face {direction}", where {direction} comes from the demon's `ritualHeading` (North for both MVP demons). |
| Heading gate | `ritualHeading` is a **per-demon property** (compass degrees, tolerance ±20°). The ritual is *Active* only while the device azimuth is within tolerance of it — for North that is [340°, 360°) ∪ [0°, 20°]. Leaving the range **resets progress to 0**. Future demons may require South, East, etc.; nothing may hard-code North. |
| Chant | The incantation is shown as **exactly 4 phrases**, grey until reached. Sustained mic volume above threshold for **2 s** while Active advances one phrase and highlights it in the demon's color. |
| Sigil | Opacity 0.10 at start, then 0.25 / 0.50 / 0.75 / 1.00 after phrases 1–4. |
| Success | After phrase 4 the sigil fully glows, then the app navigates to Profile with `isNewSummon = true`. |
| Profile (new summon) | `RibbonBanner` "New Demon Received!", rotating sunburst, bouncing demon, "Tada!" sound, button **Awesome!** pinned to the bottom → Grimoire. |
| Profile (view) | Same layout without banner and sound; button **Back**. |
| Profile content | Image, name, title, description, quote. No stats. |
| Debug aids | "Simulate heading" (snaps to the demon's `ritualHeading`) and "Hold to Chant" controls, **debug builds only**. |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual; if denied, explain why and offer a retry. |

## Demons

| | Asmodeus | Astaroth |
|---|---|---|
| id | `asmodeus` | `astaroth` |
| Title | Lord of Arithmetic & Hidden Wealth | Duke of Past, Present & Future |
| Accent | `demon-asmodeus` (gold, see DESIGN.md) | `demon-astaroth` (magenta, see DESIGN.md) |
| Ritual heading | North (0°, ±20°) | North (0°, ±20°) |
| Quote | "Three heads are better than one—especially when two want your soul!" | "I know your past, your future, and that you didn't read the terms of service." |
| Description | Teaches arithmetic, astronomy, geometry, and invincibility. Answers all questions—if you can stand the smugness. | Detects secret desires and grants protection to the great. Will explain his own fall at length if asked. |

### Chants (4 phrases each)

**Asmodeus**
1. DAEMONIUM RESIDENTE,
2. OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.
3. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,
4. UT VOTA OMNIA OPUS EST.

**Astaroth**
1. TUI GRATIA LOVIS
2. GRATIA SIT CURA.
3. HIC EST ME DIGNUS LEGI
4. SINT BENE OBEDIENTES.

"LOVIS" is kept verbatim from the original prompt; see ADR-0005.

## Non-goals (MVP)

- More than two demons; the roster rail from the mockups.
- Levels, power, stars, stats, synergies.
- Currencies, bottom navigation, reagents.
- Containment-breach / failure screen.
- Speech recognition (volume threshold only).
- Gemini / AI features.
- Persistence across app restarts.
````

- [ ] **Step 2: Verify**

Run: `grep -c "ritualHeading" PRODUCT.md; grep -n "face North" PRODUCT.md`
Expected: count ≥ 3; the second command prints nothing (no hard-coded "face North" instruction).

- [ ] **Step 3: Commit**

```bash
git add PRODUCT.md
git commit -q -m "docs(product): Add canonical PRODUCT.md merging both prompts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: `DESIGN.md`

**Files:**
- Create: `DESIGN.md`

- [ ] **Step 1: Write `DESIGN.md`**

````markdown
---
name: Summon Demons Safely
source: reference/stitch/summoner_s_grimoire_tactical_rpg/DESIGN.md
colors:
  background: '#0b1326'
  on-background: '#dae2fd'
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  surface-variant: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#d8c3ad'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#a08e7a'
  outline-variant: '#534434'
  surface-tint: '#ffb95f'
  primary: '#ffc174'
  on-primary: '#472a00'
  primary-container: '#f59e0b'
  on-primary-container: '#613b00'
  inverse-primary: '#855300'
  secondary: '#7bd0ff'
  on-secondary: '#00354a'
  secondary-container: '#00a6e0'
  on-secondary-container: '#00374d'
  tertiary: '#fcb4ff'
  on-tertiary: '#580065'
  tertiary-container: '#f487ff'
  on-tertiary-container: '#770088'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  demon-asmodeus: '#ffd54f'
  demon-astaroth: '#e040fb'
  ritual-safe: '#4ade80'
typography:
  display: { family: Cinzel, size: 32sp, weight: 700, line: 40sp }
  headline-large: { family: Cinzel, size: 24sp, weight: 700, line: 32sp }
  headline-medium: { family: Cinzel, size: 20sp, weight: 700, line: 28sp }
  title: { family: Cinzel, size: 18sp, weight: 600, line: 24sp }
  body-large: { family: Inter, size: 16sp, weight: 500, line: 24sp }
  body-medium: { family: Inter, size: 14sp, weight: 400, line: 20sp }
  body-small: { family: Inter, size: 12sp, weight: 400, line: 16sp }
  label-medium: { family: Inter, size: 12sp, weight: 600, line: 16sp }
  label-small: { family: Inter, size: 10sp, weight: 700, line: 12sp }
shapes:
  small: 12dp
  medium: 16dp
  large: 24dp
  full: pill
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 24dp
---

# DESIGN — Summon Demons Safely

Canonical design system. Product rules are in `PRODUCT.md`; the rationale for the
dark theme is ADR-0001.

## Direction

Dark tactical RPG chrome (obsidian navy surfaces, imperial gold, arcane cyan,
infernal magenta) carrying cute, comedic chibi demon art. The cuteness lives in the
characters and copy; the interface stays dark and glowing.

## Color → Compose mapping

Each `colors` key in the frontmatter maps 1:1 to the `darkColorScheme` slot of the
same name in camelCase (`surface-container-high` → `surfaceContainerHigh`). The app
ships a dark scheme only. Extras that have no Material slot are exposed as a
`DemonColors` object provided through a `CompositionLocal`:

| Token | Use |
|---|---|
| `demon-asmodeus` | Asmodeus accent: card tint, highlighted chant phrase, sigil glow |
| `demon-astaroth` | Astaroth accent: same roles |
| `ritual-safe` | Compass "aligned" state |

## Typography

- Cinzel for display, headlines and titles (names, banners, screen titles).
- Inter for body and labels; numbers use tabular figures.
- Until the fonts are bundled in `res/font/`, fall back to `FontFamily.Serif` and
  `FontFamily.SansSerif`.

## Shapes

12 / 16 / 24 dp rounded corners; buttons are pills.
**Deliberate deviation:** Stitch's own DESIGN.md asks for tight 4px chamfered
corners. We keep the rounder shapes from prompt 2 so the UI stays friendly; the
dark palette carries the RPG feel.

## Rules

- Composables read colors, type, shapes and spacing from `MaterialTheme` (and
  `DemonColors`) only. No hardcoded colors or ad-hoc dimensions.
- A demon's accent comes from its data, never from a `when (id)` in UI code.

## Components (contracts for the port)

| Component | Purpose | States / inputs |
|---|---|---|
| `PrimaryActionButton` | Main CTA (Summon, View, Awesome!, Back) | `text`, `onClick`, `enabled`; gold container, bold label, soft glow |
| `DemonCardBase` | Grid card foundation | `locked` (grey tint, silhouette) / `unlocked` (accent border + tint) |
| `RibbonBanner` | "New Demon Received!" header on Profile | `text`; primary container color |
| `RotatingSunburstBackground` | Rays behind the demon on Profile | `color`; infinite rotation |
| `SigilView` | The demon's sigil on the Ritual screen | `opacity` 0–1, `accent`; image when available, Canvas fallback otherwise |
| `CompassIndicator` | Heading feedback on the Ritual screen | current azimuth, target heading, aligned / not aligned |

## Asset catalog

Sources are under `reference/stitch/`. Target names are `res/drawable/` resource names.

| Drawable | Source | Notes |
|---|---|---|
| `ic_game_logo` | `summon_demons_safely_game_logo/screen.png` | 1024×1024, opaque |
| `avatar_asmodeus` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_1/screen.png` | 1024×1024, opaque |
| `avatar_astaroth` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_2/screen.png` | 1024×1024, background shows UI bleed — regenerate (gap 4) |
| `demon_asmodeus_full` | `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_1/screen.png` | 896×1200, unframed |
| `demon_asmodeus_card` | `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_2/screen.png` | 896×1200, framed with name plates |
| `demon_astaroth_card` | `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_3/screen.png` | 896×1200, framed with name plates |
| `sigil_astaroth` | `the_sacred_magical_sigil_of_astaroth_drawn_in_the_identical_style_as_the/screen.png` | 1024×1024, opaque |

All PNGs are RGB without an alpha channel (backgrounds baked in).

### Screen mockups (visual reference only)

`the_grimoire_demon_roster_details`, `the_ritual_active_summoning_gameplay`,
`the_ritual_active_chant_resonance`, `the_ritual_astaroth_summoning`,
`the_ritual_summoning_success_manifested`,
`the_ritual_containment_breach_emergency_banish`.
Use them for mood, color and composition. Ignore everything listed under
Non-goals in `PRODUCT.md` (roster rail, stats, currencies, bottom nav, reagents,
breach screen).

## Gaps — request from Stitch

Paste each prompt into the same Stitch project so the style stays consistent.

1. **`sigil_asmodeus`**
   > Draw the sacred magical sigil of Asmodeus in the identical style as the existing Astaroth sigil: a glowing Solomonic seal of thin golden lines on a dark obsidian parchment background, outer double ring with Latin letters, centered composition, no text other than the seal's letters. Gold (#FFD54F) glow. Square, 1024×1024.

2. **`demon_astaroth_full`**
   > Full-body cute funny stylized 2D cartoon fantasy RPG character artwork of demon ASTAROTH, identical character design and art style to the existing Astaroth card art (chibi proportions, thick smooth line art, vibrant gradient cell shading, Hero Wars style): masquerade mask, dark feathered wings, glowing viper, riding a chubby miniature dragon. No frame, no name plates. Atmospheric dark fantasy altar background. 896×1200.

3. **Transparent cut-outs: `demon_asmodeus_cutout`, `demon_astaroth_cutout`**
   > Re-render the full-body Asmodeus artwork [then: Astaroth] exactly as before but isolated on a fully transparent background (PNG with alpha), no pedestal, no environment, soft outer glow allowed. 896×1200.

4. **`avatar_astaroth` (regenerate)**
   > Hero Wars style mobile RPG avatar icon portrait of cute funny chibi demon Astaroth wearing a violet mask and dark feathered wings, framed in an ornate circular golden RPG avatar frame with purple gems, on a plain solid #0B1326 background with no UI elements behind it. 1024×1024.

### Non-Stitch gap

- **`tada` sound** (`res/raw/tada.ogg`, ≤ 2 s fanfare): source from a CC0 library
  (e.g. freesound.org filtered by CC0) or generate one. Record the file's source URL
  and licence in this section when added.
````

- [ ] **Step 2: Verify every catalog source exists**

Run:
```bash
for d in summon_demons_safely_game_logo \
  hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_1 \
  hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_2 \
  full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_1 \
  full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_2 \
  full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon_3 \
  the_sacred_magical_sigil_of_astaroth_drawn_in_the_identical_style_as_the; do
  test -f "reference/stitch/$d/screen.png" || echo "MISSING $d"; done; echo DONE
```
Expected: only `DONE`.

- [ ] **Step 3: Commit**

```bash
git add DESIGN.md
git commit -q -m "docs(design): Add canonical DESIGN.md with tokens, components and asset catalog

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: ADRs

**Files:**
- Create: `docs/decisions/0001-dark-tactical-rpg-theme.md`
- Create: `docs/decisions/0002-package-com-summondemons-safely.md`
- Create: `docs/decisions/0003-docs-first-reference-folder.md`
- Create: `docs/decisions/0004-web-demo-external.md`
- Create: `docs/decisions/0005-chant-phrasing-and-text.md`
- Create: `docs/decisions/0006-ritual-heading-per-demon.md`

- [ ] **Step 1: Write `0001-dark-tactical-rpg-theme.md`**

```markdown
# 0001 — Dark tactical RPG theme

- Date: 2026-10-15
- Status: Accepted

## Context
Both original prompts asked for a cute, colorful, light ("Pokémon Go", cream
background) look. Stitch instead produced a dark tactical RPG system (obsidian
navy, gold, cyan, magenta) plus Hero Wars–style chibi art, and the draft Compose
code followed it.

## Decision
The dark tactical RPG system is canonical. The "cute" requirement is carried by the
chibi art and comedic copy, and by keeping rounded 12–24dp shapes from prompt 2.

## Consequences
DESIGN.md uses the Stitch tokens and ships a dark color scheme only. The prompts'
light palette is not implemented.
```

- [ ] **Step 2: Write `0002-package-com-summondemons-safely.md`**

```markdown
# 0002 — Package and applicationId `com.summondemons.safely`

- Date: 2026-10-15
- Status: Accepted

## Context
Three names were in use: `com.example.mydemon` (Android Studio template),
`com.example.summondemonssafely` (draft Kotlin) and `com.summondemons.safely`
(Stitch asset guide). Play Store rejects `com.example.*`.

## Decision
Use `com.summondemons.safely` for namespace, applicationId and Kotlin packages.

## Consequences
The rename happens during the Compose port, not in the docs pass. Until then,
`app/` still uses `com.example.mydemon`.
```

- [ ] **Step 3: Write `0003-docs-first-reference-folder.md`**

```markdown
# 0003 — Docs first; generated inputs live in `reference/`

- Date: 2026-10-15
- Status: Accepted

## Context
Prompts, Stitch output, AI chat summaries and draft code disagreed with each other
and sat in `assets/`, a name that collides with Android's `src/main/assets`.

## Decision
Write root canon first (`AGENTS.md`, `PRODUCT.md`, `DESIGN.md`, ADRs), then port
code. Move all generated inputs to `reference/` and treat it as read-only history.
Truth order: PRODUCT/DESIGN → ADRs → code → `reference/`.

## Consequences
The Compose port gets its own spec built on these docs. Agents must not edit
`reference/`.
```

- [ ] **Step 4: Write `0004-web-demo-external.md`**

```markdown
# 0004 — Web demo stays in its own repo

- Date: 2026-10-15
- Status: Accepted

## Context
`assets/web` was a symlink to `../../mdweb`, an AI Studio React/Vite simulator in a
separate git repo. The link only resolved on the author's machine.

## Decision
Delete the symlink. Document the sibling repo `../mdweb` and how to run it in
`AGENTS.md`.

## Consequences
Clones of this repo no longer contain a dangling link. The web demo is optional
and not kept in sync with the Android app.
```

- [ ] **Step 5: Write `0005-chant-phrasing-and-text.md`**

```markdown
# 0005 — Chant phrasing and text

- Date: 2026-10-15
- Status: Accepted (one open question)

## Context
Prompt 1 says each 2 s of sustained chanting raises sigil opacity by 25%, which
implies 4 steps. The drafts split Asmodeus into 4 phrases and Astaroth into 3.
Astaroth's chant contains "LOVIS", likely a typo for "IOVIS" (Jove).

## Decision
Every demon's chant has exactly 4 phrases (see PRODUCT.md). Keep "LOVIS" verbatim.

## Consequences
Opacity runs 0.10 → 0.25 → 0.50 → 0.75 → 1.00 for every demon.
Open question: confirm "LOVIS" vs "IOVIS" with the product owner, then update
PRODUCT.md and this ADR.
```

- [ ] **Step 6: Write `0006-ritual-heading-per-demon.md`**

```markdown
# 0006 — Ritual heading is per-demon data

- Date: 2026-10-15
- Status: Accepted

## Context
Prompt 1 hard-codes "face North" (azimuth 340°–20°). Facing North is a trait of
Asmodeus and Astaroth, not of rituals in general; future demons may require
South, East, etc.

## Decision
Each demon carries a `ritualHeading` (compass degrees) with a ±20° tolerance. The
ritual instruction text, the Active gate, the compass target and the debug
"Simulate heading" control all derive from it. Both MVP demons use 0° (North).

## Consequences
No code or copy may hard-code North. Adding a demon with another direction is a
data change only.
```

- [ ] **Step 7: Verify**

Run: `ls docs/decisions | wc -l; grep -L "Status:" docs/decisions/*.md`
Expected: `6`; the second command prints nothing.

- [ ] **Step 8: Commit**

```bash
git add docs/decisions
git commit -q -m "docs(decisions): Add ADRs 0001-0006

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: `AGENTS.md` and `CLAUDE.md`

**Files:**
- Create: `AGENTS.md`
- Create: `CLAUDE.md`

- [ ] **Step 1: Write `AGENTS.md`**

````markdown
# AGENTS.md

Entry point for coding agents (and humans) working in this repo.

## What this is

**Summon Demons Safely** — a small native Android game (Kotlin + Jetpack Compose).
The player summons cute chibi Goetic demons by facing each demon's required
direction and chanting its Latin incantation into the microphone. Dark-fantasy RPG
presentation, comedic tone. Full rules: `PRODUCT.md`.

## Source of truth (highest first)

1. `PRODUCT.md` — game rules, demon data, scope and non-goals
2. `DESIGN.md` — tokens, typography, shapes, component contracts, asset catalog
3. `docs/decisions/` — ADRs explaining the decisions above
4. Code in `app/`
5. `reference/` — generated input (prompts, Stitch output, draft code). Never canonical.

If two sources disagree, the higher one wins. Fix the lower one or note it in an ADR.

## Repo map

| Path | Contents |
|---|---|
| `app/` | Android app module |
| `gradle/libs.versions.toml` | Dependency versions |
| `docs/decisions/` | ADRs |
| `docs/superpowers/specs/`, `docs/superpowers/plans/` | Design specs and implementation plans |
| `reference/prompts/` | The two original Stitch prompts, verbatim |
| `reference/stitch/` | Stitch mockups, character art, logo, sigil |
| `reference/src/` | Draft Compose code (input for the port; does not compile as-is) |

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # install on a connected device/emulator
```

The interactive web simulator lives in the sibling repo `../mdweb` (not part of this
repo, see ADR-0004):

```bash
cd ../mdweb && npm install && npm run dev   # http://localhost:3000
```

## Current state

- `app/` is still the Android Studio "Basic Views Activity" template (XML, fragments,
  package `com.example.mydemon`).
- Next step: port `reference/src/` to Compose in `app/`, per a dedicated spec in
  `docs/superpowers/specs/`.

## Conventions

- Kotlin + Jetpack Compose, Modern Android Development guidelines.
- Package / applicationId: `com.summondemons.safely` (ADR-0002).
- Sensor (azimuth) and audio (mic volume) logic sits behind a ViewModel exposing
  `StateFlow`; composables only render state and send events.
- Ritual parameters (heading, chant phrases, accent color) are demon data, never
  constants in UI code (ADR-0006).
- UI reads colors, type, shapes and spacing from the theme only (DESIGN.md).
- Commits: Conventional Commits, e.g. `docs(product): ...`, `feat(ritual): ...`.

## Rules for agents

- Do not edit anything under `reference/`.
- When a product or design decision changes, update `PRODUCT.md` / `DESIGN.md` and
  add an ADR in `docs/decisions/` (next free number).
- Missing art: use the ready-made Stitch prompts in DESIGN.md → "Gaps".
````

- [ ] **Step 2: Write `CLAUDE.md`**

```markdown
@AGENTS.md
```

- [ ] **Step 3: Verify every path cited in AGENTS.md exists**

Run:
```bash
for p in PRODUCT.md DESIGN.md docs/decisions app gradle/libs.versions.toml \
  docs/superpowers/specs docs/superpowers/plans reference/prompts reference/stitch reference/src; do
  test -e "$p" || echo "MISSING $p"; done; echo DONE
```
Expected: only `DONE`.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md CLAUDE.md
git commit -q -m "docs(agents): Add AGENTS.md entry point and CLAUDE.md import

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Final verification

- [ ] **Step 1: No stale `assets/` references outside `reference/` and the spec/plan**

Run: `grep -rn "assets/" --include=*.md . | grep -v '^./reference/' | grep -v '^./docs/superpowers/'`
Expected: no output.

- [ ] **Step 2: History follows the move**

Run: `git log --follow --oneline -- reference/src/Demon.kt | tail -1`
Expected: `0e6096b chore(bootstrap): Initial commit`

- [ ] **Step 3: Build still configures**

Run: `./gradlew -q help >/dev/null && echo GRADLE_OK`
Expected: `GRADLE_OK`

- [ ] **Step 4: Clean tree**

Run: `git status --short`
Expected: no output (everything committed).
