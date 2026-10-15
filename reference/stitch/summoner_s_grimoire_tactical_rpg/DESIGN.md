---
name: Summoner's Grimoire Tactical RPG
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
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
  primary-fixed: '#ffddb8'
  primary-fixed-dim: '#ffb95f'
  on-primary-fixed: '#2a1700'
  on-primary-fixed-variant: '#653e00'
  secondary-fixed: '#c4e7ff'
  secondary-fixed-dim: '#7bd0ff'
  on-secondary-fixed: '#001e2c'
  on-secondary-fixed-variant: '#004c69'
  tertiary-fixed: '#ffd6fd'
  tertiary-fixed-dim: '#fbabff'
  on-tertiary-fixed: '#36003e'
  on-tertiary-fixed-variant: '#7c008e'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  headline-xl:
    fontFamily: Cinzel
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-xl-mobile:
    fontFamily: Cinzel
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-lg:
    fontFamily: Cinzel
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-lg-mobile:
    fontFamily: Cinzel
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  headline-md:
    fontFamily: Cinzel
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  stat-numeric:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '700'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-mobile: 0.5rem
  margin: 1.25rem
  margin-mobile: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

The design system embodies a high-fidelity, mid-core dark fantasy RPG interface tailored for deep character progression, summoning rituals, and tactical combat. The aesthetic merges **tactile skeuomorphism** with **arcane occult elegance**: midnight navy stone and obsidian slates serve as the foundational bedrock, framed by burnished ormolu bronze, chiseled gold filigree, and faceted gemstone iconography. 

The emotional target is mastery, arcane wonder, and high-stakes collection. Players interact with physical, tactile slabs of enchanted armor and grimoires rather than flat digital components. Interactive states rely on glowing summoning sigils, inner ambient mana blooms, and sharp high-relief metallic bezels that invoke the physical satisfaction of rare physical artifacts.

## Colors

The palette is tuned around high-contrast luminescence over abyssal, slate-tinted darks:

- **Primary (`#f59e0b` - Imperial Amber Gold)**: The primary currency, triumph, and legendary tier color. Utilized for metallic bevel highlights, header banners, golden hero rating stars, and high-priority call-to-action buttons.
- **Secondary (`#38bdf8` - Arcane Cyan / Mana)**: The divine and magical channel. Employed for hero stat affinities (Agility/Mana), energy bars, sigil runes, and passive synergy links.
- **Tertiary (`#d946ef` - Infernal Magenta / Nether)**: The demonic corruption and dark magic accent. Used for demon summon portals, epic rank borders, magic attack statistics, and critical alert tags.
- **Neutral Palette (`#0a0f1d` to `#1e293b`)**: Deep obsidian base layers, midnight blue containment plates, and muted slate dividers that provide atmospheric contrast without pure, stark black.
- **Supportive Accents**: Emerald vitality (`#22c55e`) for health pools and combat readiness, and blood ruby (`#ef4444`) for physical prowess and defeat states.

## Typography

The type system blends authoritative, classical serif proportions for fantasy grandeur with hyper-legible modern sans-serifs for complex RPG mathematical statistics:

- **Headlines (`Cinzel`)**: Classical proportions evoke ancient tomes, imperial banners, and stone tablet carvings. Used strictly for character names, panel headers, dialogue titles, and result banners (`VICTORY`, `DEFEAT`).
- **Body & Metrics (`Inter`)**: Chosen for absolute legibility at compact mobile scale. Statistical readouts, talent tooltips, level counters, and inventory values utilize heavy weights (`600`, `700`) to remain razor-sharp over ambient glowing textures and dark backgrounds.
- **Numbers & Counters**: Numbers are always displayed in tabular or uniform widths with high-contrast colored values (e.g., golden values for main stats, cyan for mana, lime for health).

## Layout & Spacing

This design system adheres to a mobile-first, landscape-dominant fixed HUD structure, typical of mid-core RPGs, while adapting seamlessly to portrait inventory screens:

- **Grid & Spatial Ratios**: Uses an 8pt base grid with dense spatial packing. Compact margins (`margin-mobile: 0.75rem`) maximize usable canvas for live 3D/2D spine rigs, battlefield dioramas, and stat matrices.
- **Side Dock Rails**: Navigation tabs (Hero, Inventory, Summoning, Realm) sit in vertical brass-accented rail containers pinned to canvas edges.
- **Hero & Unit Carousels**: Left-aligned vertical thumbnail reels utilize `space-xs` and `space-sm` for tight, stackable browsing with notification pips overlapping tile corners.

## Elevation & Depth

Visual hierarchy is built via skeuomorphic metallics, inner containment bevels, and atmospheric glow layers rather than modern drop shadows:

- **Base Canvas (Level 0)**: Atmospheric, vignette-shadowed environment art rendered with a dark gradient overlay (`rgba(10, 15, 29, 0.85)`).
- **Secondary Containers (Level 1)**: Solid slate-navy sheets (`#141c30`) framed with a 1px inner metallic bronze line (`#785324`) and an outer 2px burnished edge (`#b47e28`).
- **Raised Panels & Cards (Level 2)**: Slate containers with top-lit gradient fills (from `#1e293b` to `#0f172a`), creating a recessed or extruded stone slab presence.
- **Interactive Controls & Jewels (Level 3)**: Buttons, level badges, and equipment slots have multi-stage outer glows (`0 0 12px rgba(245, 158, 11, 0.45)`) and 3D beveled surfaces with high-specular top rims.
- **Modals & Summoning Circles (Level 4)**: Heavy, dark scrim with central glowing runes radiating back-lit mana rings (`rgba(56, 189, 248, 0.25)`).

## Shapes

The design uses tight, controlled corners (`roundedness: 1` — 4px base radius) paired with chamfered and bracketed corner accents to convey fortified stone and forged metal:

- **Panels & Plates**: Slight 4px to 6px radii to prevent modern rounded pill aesthetics, retaining a sturdy armor-plated feel.
- **Stat Badges & Class Icons**: Rhombus (diamond), hexagonal, and shield crest geometries.
- **Level & Rank Plates**: Pinned rectangular tabs with cut or clipped bottom corners, anchored directly to top edges of hero cards.

## Components

### Hero Portraits & Unit Badges
- **Frame**: Square or tall-ratio portrait tile bordered by a high-relief metallic frame matching hero rarity (Silver `#94a3b8`, Gold `#f59e0b`, Royal Amethyst `#c084fc`).
- **Level Ribbon**: Pinned top-left corner badge with an enameled sapphire or crimson backing, housing bold white level numerals.
- **Star Tier**: Centered along the bottom edge, micro gold-embossed 5-point stars with dark shadows.
- **Notification Pip**: Circular amber-red gem perched over the top-right corner with a golden exclamation mark.

### Action Buttons
- **Primary CTA (Summon / Level Up)**: Golden brass or emerald gradient surface with a high-gloss 3D beveled edge, 2px light source highlight on top, and dark bronze bevel underneath.
- **Destructive / Boss CTA**: Ruby-enamelled deep red gradient with golden runic border trim.
- **Secondary / Utility**: Dark slate button framed with a 1.5px burnished gold stroke.

### RPG Stat & Progress Bars
- **Track**: Recessed deep charcoal channel (`#090d16`) with an inset shadow (`inset 0 2px 4px rgba(0, 0, 0, 0.8)`).
- **Fill**: High-saturation segment bars with vertical dividing notches.
  - Green (`#22c55e`) for HP.
  - Cyan (`#38bdf8`) for Energy/Mana.
  - Gold (`#fbbf24`) for Experience/Rank progression.
- **Cap**: Highlighted leading edge giving the visual impression of molten liquid or glowing crystal.

### Segmented Attribute Clusters
- **Layout**: Two-column matrix featuring faceted 3D diamond icons for Agility (boot), Strength (bicep), Intelligence (wizard hat), Physical Attack (sword), and Magic Attack (spiral flame).
- **Divider**: Subtle arcane filigree watermark etched at low opacity (`rgba(56, 189, 248, 0.15)`) behind numerical values.

### Result Banners & Modal Wrappers
- **Header**: Ornate scarlet or imperial royal ribbon crowned with a golden laurel crest.
- **Backing**: Solid slate vault plate fastened with corner rivets and glowing runic borders.