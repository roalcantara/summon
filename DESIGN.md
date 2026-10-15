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

- Cinzel for display, headlines and titles (names, banners, screen titles, sigil rim letters).
- Inter for body and labels; numbers use tabular figures.
- Both are bundled in `res/font/` as static, Latin-subset instances generated from the
  Google Fonts variable fonts (SIL OFL 1.1; licences in `app/src/main/assets/licenses/`).

## Shapes

12 / 16 / 24 dp rounded corners; buttons are pills.
**Deliberate deviation:** Stitch's own DESIGN.md asks for tight 4px chamfered
corners. We keep the rounder shapes from prompt 2 so the UI stays friendly; the
dark palette carries the RPG feel.

## Rules

- Composables read colors, type, shapes and spacing from `MaterialTheme` (and
  `DemonColors`) only. No hardcoded colors or ad-hoc dimensions.
- A demon's accent comes from its data, never from a `when (id)` in UI code.
- Motion respects `LocalReduceMotion` (system animator scale 0): no particles,
  parallax, floating or shimmer.

## Components (contracts)

| Component | Purpose | States / inputs |
|---|---|---|
| `PrimaryActionButton` | Main CTA (Summon, View, Awesome!, Back) | `text`, `onClick`, `enabled`, `tone`; gold container, bold label, soft glow |
| `RibbonBanner` | "New Demon Received!" header on Profile | `text`; primary container color |
| `RotatingSunburstBackground` | Rays behind the demon on Profile | `color`, `rotating` |
| `InkedSigil` | The demon's vector sigil | `sigil` (raw), `inkProgress` 0–1, `ringProgress` 0–1, `rimText`, `accent`, `micLevel` (glow), `flicker` |
| `CompassArc` | Heading ring around the sigil | `azimuth`, `target`, `mode` (Seeking / Locked / Wavering), `graceFraction` |
| `KaraokeLine` | Current chant phrase | `text`, `lit` syllables, `accent`, `dim` |
| `DemonCutout` | Cut-out art with accent rim glow | `art`, `accent`, `silhouette` 0–1 |
| `ParticleField` | Embers / smoke / converging climax sparks | `color`, `style`, `count`, `converge` 0–1 |
| `HeatShimmer` | Rising heat haze | `color`; AGSL on API 33+, breathing gradient below |
| `rememberParallax` | Gyro offset for art layers | max offset; `TYPE_GAME_ROTATION_VECTOR`, none if absent |

## Asset catalog

Sources are under `reference/stitch/`. Target names are resource names; the app
stores them in `res/drawable-nodpi/` as lossy WebP (`cwebp -q 85`, alpha kept) so
large sources are not density-scaled and the APK stays small.

| Drawable | Source | Notes |
|---|---|---|
| `ic_game_logo` | `summon_demons_safely_game_logo/screen.png` | 1024×1024, opaque |
| `avatar_asmodeus` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon_1/screen.png` | 1024×1024, opaque; circle-clipped |
| `avatar_astaroth` | `hero_wars_style_mobile_rpg_avatar_icon_portrait_of_cute_funny_chibi_demon/screen.png` | 1024×1024, opaque; circle-clipped |
| `demon_asmodeus_full` | previous `full_body_…_demon_1/screen.png` (git history before Task 2) | 896×1200 scene, already shipped |
| `demon_astaroth_full` | `full_body_…_demon_1/screen.png` | 896×1200 scene |
| `demon_asmodeus_cutout` | `full_body_…_demon_2/screen.png` | 896×1200, alpha |
| `demon_astaroth_cutout` | `full_body_…_demon_3/screen.png` | 896×1200, alpha |

`full_body_…` = `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon`.
Unused references: `the_sacred_magical_sigil_of_*` (superseded by vector sigils, ADR-0009).

### Sigils

| Raw | Source | Licence |
|---|---|---|
| `sigil_asmodeus` | `art/sigils/asmodeus.svg`, traced from Wikimedia Commons `File:Seal_of_Asmodeus.svg` | CC0 |
| `sigil_astaroth` | `art/sigils/astaroth.svg`, traced from Wikimedia Commons `File:Astaroth_Seal.svg` | Public domain (Mathers & Crowley 1904) |

Format: viewBox `0 0 100 100`, glyph within radius 37 of the centre, groups
`ink-1`…`ink-4` in draw order, one contour per `<path>`. Regenerate with
`python3 -I tools/sigil_to_raw.py art/sigils/<id>.svg app/src/main/res/raw/sigil_<id>.txt`.

### Audio

All sounds are original synthesized works (CC0) from `tools/gen_audio.py`, mono
22.05 kHz WAV: `bed_asmodeus`, `bed_astaroth` (8 s seamless loops), `chime_1`…`chime_4`,
`lock`, `riser`, `boom`, `tada`. Regenerate with
`python3 -I tools/gen_audio.py app/src/main/res/raw`. To replace one with a recording,
use a CC0 file of the same name and record its source URL and licence here.

### Screen mockups (visual reference only)

`the_grimoire_demon_roster_details`, `the_ritual_active_summoning_gameplay`,
`the_ritual_active_chant_resonance`, `the_ritual_astaroth_summoning`,
`the_ritual_summoning_success_manifested`,
`the_ritual_containment_breach_emergency_banish`.
Use them for mood, color and composition. Ignore everything listed under
Non-goals in `PRODUCT.md` (roster rail, stats, currencies, bottom nav, reagents,
breach screen).

## Gaps

None open. Asmodeus has no new scene art; `demon_asmodeus_full` keeps the earlier render.
