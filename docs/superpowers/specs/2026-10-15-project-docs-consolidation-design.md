# Project Docs Consolidation — Design

- **Date:** 2026-10-15
- **Status:** Approved in brainstorming; pending implementation plan
- **Scope:** Documentation and repo layout only. No Kotlin/Gradle changes.
- **Follow-up:** The Compose port of `reference/src` into `app/` gets its own spec, built on these docs.

## 1. Problem

The repo has conflicting, generated inputs and no single source of truth:

- Two Stitch prompts (game MVP + UI design system) disagree on layout and scope, and their aesthetic ("cute, cream, Pokémon Go") differs from what Stitch produced (dark tactical RPG).
- Three package names are in play: `com.example.mydemon` (template), `com.example.summondemonssafely` (drafts), `com.summondemons.safely` (Stitch asset guide).
- `assets/` mixes Stitch output, draft Kotlin and AI chat summaries, and its name collides with Android's `src/main/assets`.
- `assets/web` is a symlink to `../../mdweb`, outside the repo; it breaks on any other clone.
- There is no agent entry point (`AGENTS.md`).

## 2. Decisions taken

| # | Decision |
|---|---|
| D1 | Docs pass first; the Compose port is a separate, later spec. |
| D2 | Canonical aesthetic: **dark tactical RPG** (Stitch tokens). "Cute" lives in the chibi art and comedic copy. |
| D3 | Package / applicationId: **`com.summondemons.safely`** (applied during the port, recorded now). |
| D4 | Artifacts: `AGENTS.md` + `CLAUDE.md` pointer, `PRODUCT.md`, `DESIGN.md`, `docs/decisions/` ADRs. |
| D5 | `assets/web` symlink removed; web demo documented as the sibling repo `../mdweb`. |
| D6 | Layout: canonical docs at repo root; generated inputs archived under `reference/`. |
| D7 | Missing visual assets are requested from Stitch using ready-to-paste prompts kept in `DESIGN.md`; only the "Tada" sound needs another source. |

## 3. Target structure

```
AGENTS.md
CLAUDE.md                      # single line: @AGENTS.md
PRODUCT.md
DESIGN.md
docs/
  decisions/
    0001-dark-tactical-rpg-theme.md
    0002-package-com-summondemons-safely.md
    0003-docs-first-reference-folder.md
    0004-web-demo-external.md
    0005-chant-phrasing-and-text.md
    0006-ritual-heading-per-demon.md
  superpowers/specs/2026-10-15-project-docs-consolidation-design.md   # this file
reference/                     # git mv from assets/ (history preserved)
  README.md                    # "generated input, NOT canonical"
  prompts/
    01-game-mvp.md             # prompt 1, verbatim
    02-ui-design-system.md     # prompt 2, verbatim
  stitch/                      # was assets/stitch_summon_demons_safely_ui/ (contents untouched)
  docs/                        # was assets/docs/ (implementation_plan.md, design.md, astaroh.md)
  src/                         # was assets/src/ (draft Kotlin, untouched until the port)
```

`assets/web` (symlink) is deleted. `app/` and Gradle files are not touched.

**Source-of-truth order** (stated in `AGENTS.md`):
`PRODUCT.md` / `DESIGN.md` → `docs/decisions/` → code → `reference/`.
Anything in `reference/` that disagrees with the canon is wrong by definition.

## 4. PRODUCT.md — content

### 4.1 Pitch
"Summon Demons Safely": a small Android game where the player summons cute chibi Goetic demons by facing each demon's required direction (North for the first two) and chanting a Latin incantation into the microphone.

### 4.2 Core loop
Grimoire → tap demon → Ritual (face the demon's required direction + chant) → Profile (doubles as success screen) → back to Grimoire.

### 4.3 Reconciled rules

| Topic | Canonical rule |
|---|---|
| Home | `Scaffold` with top bar "My Grimoire"; `LazyVerticalGrid` of demon cards. |
| Lock state | Both demons start **locked**. Locked card: grey tint, silhouette, **Summon** → Ritual. Unlocked card: theme-color tint/border, illustration, **View** → Profile. Unlock state is in-memory only. |
| Ritual setup | Large faint sigil; text "Hold the phone with both hands and face {direction}", where {direction} comes from the demon's required heading (North for both MVP demons). |
| Heading gate | The required heading is a **per-demon property** (`ritualHeading`, compass degrees, tolerance ±20°). Ritual is *Active* only while the device azimuth is within tolerance of it — for North that is [340°, 360°) ∪ [0°, 20°]. Leaving the range **resets progress to 0**. Future demons may require South, East, etc.; nothing in the rules may hard-code North. |
| Chant | Incantation shown as **exactly 4 phrases**, grey until reached. Sustained mic volume above threshold for **2 s** while Active advances one phrase and highlights it in the demon's color. |
| Sigil | Opacity 0.10 at start, then 0.25 / 0.50 / 0.75 / 1.00 after phrases 1–4. |
| Success | At phrase 4: sigil fully glows, then navigate to Profile with `isNewSummon = true`. |
| Profile (new summon) | `RibbonBanner` "New Demon Received!", rotating sunburst, bouncing demon, "Tada!" sound, pinned button **Awesome!** → Grimoire. |
| Profile (view) | Same layout without banner and sound; button **Back**. |
| Profile content | Image, name, title, description, quote. No stats. |
| Debug aids | "Simulate heading" (snaps to the demon's required heading) and "Hold to Chant" controls exist in **debug builds only**. |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual; if denied, explain and offer retry. |

### 4.4 Demon data

| | Asmodeus | Astaroth |
|---|---|---|
| Title | Lord of Arithmetic & Hidden Wealth | Duke of Past, Present & Future |
| Accent | gold (DESIGN.md `demon-asmodeus`) | magenta (DESIGN.md `demon-astaroth`) |
| Ritual heading | North (0°, ±20°) | North (0°, ±20°) |
| Quote | "Three heads are better than one—especially when two want your soul!" | "I know your past, your future, and that you didn't read the terms of service." |
| Description | Teaches arithmetic, astronomy, geometry, and invincibility. Answers all questions—if you can stand the smugness. | Detects secret desires and grants protection to the great. Will explain his own fall at length if asked. |

Chant phrases:

- **Asmodeus**
  1. DAEMONIUM RESIDENTE,
  2. OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.
  3. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,
  4. UT VOTA OMNIA OPUS EST.
- **Astaroth**
  1. TUI GRATIA LOVIS
  2. GRATIA SIT CURA.
  3. HIC EST ME DIGNUS LEGI
  4. SINT BENE OBEDIENTES.

"LOVIS" is kept verbatim from the prompt; ADR-0005 tracks whether it should be "IOVIS".

### 4.5 Non-goals (MVP)
More than two demons; roster rail; levels, power, stars, stats, synergies; currencies; bottom navigation; reagents; containment-breach/failure screen; speech recognition; Gemini/AI features; persistence across app restarts.

## 5. DESIGN.md — content

Follows the design-md skill format (frontmatter tokens + prose).

- **Color tokens:** full Material 3 set from `reference/stitch/summoner_s_grimoire_tactical_rpg/DESIGN.md` (e.g. `surface #0B1326`, `primary #FFC174`, `primary-container #F59E0B`, `secondary #7BD0FF`, `tertiary #FCB4FF`), each mapped to its Compose `colorScheme` slot. Extras: `demon-asmodeus #FFD54F`, `demon-astaroth #E040FB`, `ritual-safe #4ADE80`.
- **Typography:** Cinzel (display/headlines), Inter (body/labels, tabular numbers); fallbacks `FontFamily.Serif` / `FontFamily.SansSerif` until fonts are bundled.
- **Shapes:** small 12dp, medium 16dp, large 24dp, full pill for buttons.
- **Component contracts** (names, purpose, states — no code): `PrimaryActionButton`, `DemonCardBase` (locked/unlocked), `RibbonBanner`, `RotatingSunburstBackground`, `SigilView` (opacity 0–1, theme color), `CompassIndicator` (current azimuth, target heading, aligned/not aligned).
- **Rule:** composables use `MaterialTheme` tokens only; no hardcoded colors or ad-hoc dimensions.

### 5.1 Asset catalog

| Target drawable | Source in `reference/stitch/` | Notes |
|---|---|---|
| `ic_game_logo` | `summon_demons_safely_game_logo/screen.png` | 1024², opaque |
| `avatar_asmodeus` | `hero_wars_style_..._chibi_demon_1/screen.png` | 1024², opaque |
| `avatar_astaroth` | `hero_wars_style_..._chibi_demon_2/screen.png` | 1024², background shows UI bleed — regenerate |
| `demon_asmodeus_full` | `full_body_..._demon_1/screen.png` | 896×1200, unframed |
| `demon_asmodeus_card` | `full_body_..._demon_2/screen.png` | 896×1200, framed with name plates |
| `demon_astaroth_card` | `full_body_..._demon_3/screen.png` | 896×1200, framed with name plates |
| `sigil_astaroth` | `the_sacred_magical_sigil_of_astaroth_.../screen.png` | 1024², opaque |

Screen mockups (`the_grimoire_*`, `the_ritual_*`) are listed as visual reference with a note that out-of-scope elements (see PRODUCT.md non-goals) are ignored.

### 5.2 Gaps and Stitch prompts
`DESIGN.md` lists each missing asset with a ready-to-paste Stitch prompt that restates the art style (Hero Wars chibi, thick line art, cell shading) and output spec:

1. `sigil_asmodeus` — sigil in the same style as `sigil_astaroth`, gold.
2. `demon_astaroth_full` — unframed full body, matching `demon_asmodeus_full`.
3. Transparent-background cut-outs of both full-body images (for the Profile sunburst).
4. `avatar_astaroth` regenerated without background UI bleed.

Non-Stitch gap: `tada.ogg` (short fanfare) — source from a CC0 library or generate; recorded with its licence in `DESIGN.md`.

## 6. AGENTS.md — content (~80 lines)

1. One-paragraph pitch.
2. Source-of-truth order (§3).
3. Repo map.
4. Commands: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew installDebug`; web demo: `cd ../mdweb && npm install && npm run dev` (port 3000).
5. Conventions: Kotlin + Jetpack Compose, MAD; ritual parameters (heading, phrases, color) are demon data, not constants; sensor and audio logic behind a ViewModel exposing `StateFlow`; theme tokens only; package `com.summondemons.safely`.
6. Current state: `app/` is still the XML "Basic Views Activity" template; the Compose port is the next spec.
7. Rules for agents: never edit `reference/`; when a decision changes, update PRODUCT/DESIGN and add an ADR.

`CLAUDE.md` contains only `@AGENTS.md`.

## 7. ADRs

Format: title, date, status, Context / Decision / Consequences, ~15 lines each.

- **0001** Dark tactical RPG theme (D2) — notes the prompts asked for a light theme and why the Stitch direction won.
- **0002** Package `com.summondemons.safely` (D3) — rename happens during the port.
- **0003** Docs-first and the `reference/` folder (D1, D6).
- **0004** Web demo stays external (D5).
- **0005** Chant phrasing: 4 phrases per demon so the 25% rule holds; "LOVIS" kept verbatim, open question.
- **0006** Ritual heading is per-demon data (North for the MVP pair; other directions for future demons), never a hard-coded rule.

## 8. Verification

- Every path cited in `AGENTS.md`, `DESIGN.md` and `reference/README.md` exists.
- `grep -rn "assets/" --include=*.md` outside `reference/` returns no stale references.
- `git log --follow` on a moved file shows pre-move history.
- `./gradlew help` still configures (sanity check that nothing build-relevant moved).
