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
