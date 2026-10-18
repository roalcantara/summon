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
  house-sol: '#f2c14e'
  house-venus: '#c87a3c'
  ink-highlight: '#fff6d0'
  stage-home-glow: '#3a2a55'
  stage-home-mid: '#1a1530'
  stage-sol-glow: '#6b4a12'
  stage-sol-mid: '#2a1d0b'
  stage-venus-glow: '#3b2a4a'
  stage-venus-mid: '#2a1a26'
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

Each Material `colors` key in the frontmatter maps 1:1 to the `darkColorScheme` slot of
the same name in camelCase (`surface-container-high` → `surfaceContainerHigh`). The app
ships a dark scheme only. Extras that have no Material slot are exposed as a
`DemonColors` object provided through a `CompositionLocal`:

| Token | Use |
|---|---|
| `demon-asmodeus` | Asmodeus accent: stage particles, highlighted chant phrase, sigil, badges |
| `demon-astaroth` | Astaroth accent: same roles |
| `ritual-safe` | Altar ring locked / aligned, safe cue chip |
| `house-sol` | Sol metal: badge rims, number medallion, Home court chip |
| `house-venus` | Venus metal: same roles |
| `ink-highlight` | Hot core of flashes: focus-bar court shimmer, sealed medallion, sigil flare, grimoire opening burst |
| `stage-home-glow`, `stage-home-mid` | Home background gradient (edge: `surface-container-lowest`) |
| `stage-sol-glow`, `stage-sol-mid` | Sol stage gradient (edge: `background`) |
| `stage-venus-glow`, `stage-venus-mid` | Venus stage gradient (edge: `background`) |

Smoke particles use `on-surface-variant`; Home embers and the compass rose use `primary`.

## Typography

- Cinzel for display, headlines and titles (names, banners, screen titles, sigil rim letters).
- Inter for body and labels; numbers use tabular figures.
- Both are bundled in `res/font/` as static, Latin-subset instances generated from the
  Google Fonts variable fonts (SIL OFL 1.1; licences in `app/src/main/assets/licenses/`).

### Text roles (ADR-0014)

Read through `MaterialTheme.textRoles`. Compose has no text-transform: `cue` and `meta`
strings are upper-cased at the call site with `upper()` (`uppercase(Locale.ROOT)`).

| Role | Style | Used for |
|---|---|---|
| `cue` | `label-medium`, letter spacing 0.08 em, upper case | What to do now: cue chips, "Tap to summon", "Circle cooling" |
| `incantation` | Cinzel 15 sp Bold, line 22 sp, letter spacing 0.02 em | Every chant phrase on the tablet |
| `incantationLarge` | Cinzel Bold, line 1.3 em, letter spacing 0.02 em, auto-sized 18–96 sp | The phrase on the chant screen (ADR-0019) |
| `ritualCue` | Inter Bold, upper case, letter spacing 0.05 em; 18 sp (direction) or 22 sp (verdict) | The two lines of the chant screen, read at mouth distance (ADR-0019) |
| `meta` | `label-small`, letter spacing 0.14 em, upper case, `outline` | Small headers and counters: "Sacred chant", "1 / 6", "2 of 2 spirits bound" |
| `name` | Cinzel Bold; size per use (`title` / `headline-*`, or a size fixed by the screen spec) | Demon names, numbers, badge labels, ability names |
| `body` | `body-medium` | Descriptions, quotes, dossier pages |

## Shapes

6 (extra-small / smudges) / 12 / 16 / 24 dp rounded corners; buttons are pills.
**Deliberate deviation:** Stitch's own DESIGN.md asks for tight 4px chamfered
corners. We keep the rounder shapes from prompt 2 so the UI stays friendly; the
dark palette carries the RPG feel.

## Rules

- Composables read colors, type, shapes and spacing from `MaterialTheme` (and
  `DemonColors`, `textRoles`) only. No hardcoded colors or ad-hoc dimensions.
- A demon's accent and House metal come from its data, never from a `when (id)` in UI code.
- Motion respects `LocalReduceMotion` (system animator scale 0): no particles,
  parallax, floating, jitter, shimmer, sunburst rotation, grimoire bob/hop/peek, dust
  puff, quote-slot and floor-circle pulses, or climax flash. The grimoire opening keeps
  a 150 ms fade; the avatar burn-away and badge/carousel changes cross-fade. Ink-in,
  karaoke and the focus bar still update.

## Components (contracts)

| Component | Purpose | States / inputs |
|---|---|---|
| `PrimaryActionButton` | Main CTA (**Awesome!**, **Stay**, Allow microphone) | `text`, `onClick`, `enabled`, `tone`; gold container, bold label, soft glow |
| `RibbonBanner` | "New Demon Received!" on the Grimoire celebration; "Seal broken!" on Home | `text`; primary container color |
| `RotatingSunburstBackground` | Rays behind the demon during the Grimoire celebration | `color`, `rotating` |
| `InkedSigil` | The demon's vector sigil | `sigil` (raw), `inkProgress` 0–1, `ringProgress` 0–1, `rimText`, `accent`, `micLevel` (glow), `ghostAlpha`, `flash` (group, color, duration: seal flare / crack drain) |
| `AltarRing` | Ring around the sigil on the Ritual | `azimuth`, `target`, `phase`, `lockFraction`, `accent`; outline track, `ritual-safe` lock fill / locked ring, `error` while wavering, a marker toward the target |
| `CueChip` | One-line cue | `text`, `tone` (neutral / safe / error), `shake` |
| `FocusBar` | Focus left under the current phrase | `fraction`, `tone` (normal / drift / court), `accent`; heartbeat below `FOCUS_LOW_FRACTION` |
| `ChantScreen` | Landscape chant screen (ADR-0019) | `state`, `flash`, `showClose`, `onClose`, `debug`; House-stage backdrop (scrim strongest behind the words), slab (surface-container-low 70 %, `large` shape) with name, `ChantSigil` and the direction line; the phrase (`incantationLarge`) with a line per word: pending outline-variant, current on-surface-variant, heard ritual-safe, doubtful primary 70 %, rejected error wavy + ✕ badge; verdict line (`ritualCue`) with `VoiceMeter`. Portrait (large screens) puts the slab in a top band. |
| `ChantSigil` | `InkedSigil` inside a ring | Ring colour = direction (ritual-safe / error), arc = focus clock (full on phrase I), seconds or "Paused" under it |
| `VoiceMeter` | Five ember bars | `level` (mic level, 0–1), `color`; flat at 22 % when silent; decides nothing |
| `ChantTablet` | Framed chant panel |
| `KaraokeLine` | Current chant phrase | `text`, `lit` syllables, `accent`, `dim`, `micLevel` (newest-syllable glow) |
| `BadgeRow` | Direction / House / Calling / Legions tiles | `demon`, `selected`, `onSelect`; rim = House metal, glyph = accent (House glyph = metal); `BadgeIcon` draws one glyph |
| `InfoBand` | Description or the selected badge's explanation | `demons`, `demon`, `badge`; height of the tallest text |
| `DossierCarousel` | Six dossier pages | `demon`, `record`, `inkCodex`, `inkBattle`, `page`, `onPage`; ‹ › and swipe, wrapping |
| `Smudge` / `InkLine` | Sealed page lines / ink-in of revealed lines | `lines`; `ink`, `index` |
| `QuoteSlot` | Fixed 72 dp slot under the demon | `state` (celebration / bound / ready / cooling), quote, hours, `shake` |
| `SigilAvatar` | Avatar rail item | `demon`, `bound`, `burn`, `restless`, `selected`, `enabled` |
| `FloorCircle` | Accent ellipse under the demon | `accent`, `pulse` |
| `StageBackground` | Gradient stage (Home, Sol, Venus) with a scrim | `stage`, `dim` |
| `CompassRose` | Inlaid rose on the Home floor | — (`primary`) |
| `RestlessGrimoire` | Vector placeholder for the Home book | `restless`, `accent`, `opening`, `onOpen` |
| Glyphs | `DirectionGlyph`, `HouseGlyph`, `CallingGlyph`, `LegionsGlyph` | vector paths in a 24-unit box; `color`, `size` |
| `DemonCutout` | Cut-out art with accent rim glow | `art`, `accent`, `silhouette` 0–1, `alignment` |
| `ParticleField` | Embers / smoke / converging climax sparks | `color`, `style`, `count`, `converge` 0–1 |
| `DebugChip` | Opens the debug sheet (debug builds only) | `onClick` |
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
| `demon_asmodeus_full` | `art/etc/full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon.png` | 896×1200 scene |
| `demon_astaroth_full` | `full_body_…_demon_1/screen.png` | 896×1200 scene |
| `demon_asmodeus_cutout` | `full_body_…_demon_2/screen.png` | 896×1200, alpha |
| `demon_astaroth_cutout` | `full_body_…_demon_3/screen.png` | 896×1200, alpha |
| `stage_home` | `art/etc/stage_home.png` | 768×1376 scene, Home stage background |
| `stage_sol` | `art/etc/stage_sol.png` | 768×1376 scene, Sol House stage background |
| `stage_venus` | `art/etc/stage_venus.png` | 768×1376 scene, Venus House stage background |
| `chant_intro_singer` | `art/splash/chant_intro_singer_full.png` | 1024×1024, alpha; generic chibi demon singing into karaoke mic |
| `chant_intro_banner` | `art/splash/chant_intro_banner.png` | 1200×531, alpha; parchment ribbon banner |
| `chant_summon_button` | `art/splash/chant_summon_button_unpressed.png` | 800×359, alpha; plum-enamel stone plaque button |
| `chant_summon_button_pressed` | `art/splash/chant_summon_button_pressed.png` | 800×359, alpha; pressed state |
| `ic_home_grimoire` | `art/etc/RestlessGrimoire.png` | 1024×1024 opaque square asset, Home grimoire book |
| `ic_launcher_foreground` | `tools/gen_icons.py` | Generated mipmaps (`mipmap-{m,h,xh,xxh,xxxh}dpi`), adaptive icon foreground |
| `ic_launcher_monochrome` | Hand-crafted vector | 108×108 dp vector drawable (`res/drawable/`), themed icon layer |

`full_body_…` = `full_body_cute_funny_stylized_2d_cartoon_fantasy_rpg_character_artwork_of_demon`.
Regenerate launcher and store graphics with `python3 -I tools/gen_icons.py app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store`.
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
`chime_4` also opens the Home grimoire (50 %); `boom` at 35 % marks a crack.

### Screen mockups (visual reference only)

`the_grimoire_demon_roster_details`, `the_ritual_active_summoning_gameplay`,
`the_ritual_active_chant_resonance`, `the_ritual_astaroth_summoning`,
`the_ritual_summoning_success_manifested`,
`the_ritual_containment_breach_emergency_banish`.
Use them for mood, color and composition. Ignore everything listed under
Non-goals in `PRODUCT.md` (roster rail, stats, currencies, bottom nav, reagents,
breach screen).

## Gaps

All gaps closed. Stage backgrounds (`stage_home`, `stage_sol`, `stage_venus`), Home grimoire (`ic_home_grimoire`), Asmodeus scene art (`demon_asmodeus_full`), and the chant intro card art (`chant_intro_singer`, `chant_intro_banner`, `chant_summon_button`, `chant_summon_button_pressed`) have been processed and integrated into `app/src/main/res/drawable-nodpi/`.
