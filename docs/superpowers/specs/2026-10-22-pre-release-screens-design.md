# Pre-release screens: Grimoire flip, intro layout, chant attention

- Date: 2026-10-22
- Status: Approved (brainstorming session, visual companion)
- Branch: `fix/pre-release-ui`
- Builds on: `docs/reviews/2026-10-22-pre-release-ui-review.md`, ADR-0016, ADR-0020, ADR-0021, ADR-0029, ADR-0032
- Goal: release in two days with the Grimoire, intro card and chant screen correct in every
  configuration (font scale, display size, language, device, game state).

## 1. Shared rules (DESIGN.md, ADR-0033)

These rules apply to every screen and keep the three changes consistent.

### 1.1 Text sizing

| Text kind | Rule | Examples |
|---|---|---|
| Body text | Scales fully with the system font scale. Where it can overflow, its container scrolls. | Description band, dossier entries, intro rules |
| Words on fixed art or HUD slots | Scale up to 1.3× (`TextUnit.fontScaleCapped()`, `FIXED_SLOT_MAX_FONT_SCALE`), then auto-size down to fit | Intro ribbon, seal numerals, chant feedback slot, Summon label |
| Labels in one row | Share one size: the largest at which all fit (as `sharedLabelSize` in `Badges.kt`) | Badge labels |
| One-line labels | Never wrap: `maxLines = 1`, `softWrap = false`, shrink to fit | Direction pill (every state), badges, title plaque, seal numerals |

The primary action of a screen is never inside a scrolling region and never off-screen.

### 1.2 Attention glow

One cue across the game: a soft **breathing glow** (accent colour, 1.6 s period) marks the one
thing the player should act on next. With reduce motion (`LocalReduceMotion`) it is a steady
glow. Colour is never the only signal: the element's text still says what to do.

A single composable, `Modifier.attentionGlow(active: Boolean, color: Color, fast: Boolean = false)`
in `ui/fx/`, draws it (`fast` = 0.8 s period, used for wavering). Used by:

| Where | Glows while |
|---|---|
| Grimoire dossier button (⟲) | the shown demon's dossier has unread text: `record.revealPending.codex || record.revealPending.battle` |
| Intro Summon button | the player has never completed a summon: `progress.lastSummonDay == null` |
| Chant sigil ring | seeking (accent) and wavering (the paused amber, `colors.primary`, `fast`) |
| Chant phrase (halo behind the current line) | chanting |

## 2. Grimoire: tarot flip (amends ADR-0016)

### Front (identity)
Header (medallion, name, title), description band, four badges. The dossier carousel leaves
the card; its space goes to the description band and the stage (the demon draws larger). The
badge band behaviour (ADR-0016) is unchanged.

### Back (dossier)
One vertical, scrolling list of the six sections in today's order: Weakness, Abilities,
Offices, Appearance, Summoner's warning, Secret. Each entry: a section header (`meta` role) and
its text (`body` role, full font scaling). Locked entries keep the `Smudge` and add a seal line:
"Summon to reveal" (sections 1–5 while `!record.revealed`) or "Flawless summon" (Secret while
`!record.flawless`). Newly revealed text inks in (`InkLine`) as today and clears the pending
reveal as today. The back is the same size as the front; the list starts at the top for each
demon.

### Flip
- **Trigger:** a ⟲ icon button at the end of the card header (48 dp target, content
  description "Dossier" / "Profile" by side). Tapping the card body does not flip.
  The button carries the attention glow (§1.2).
- **Back:** system back on the back side flips to the front first; then back leaves the
  Grimoire (the celebration keeps its priority).
- **Switching demon** keeps the current side.
- **After a summon:** tapping **Awesome!** ends the celebration and flips the card to the back,
  so the reveal plays in view.
- **Motion:** 400 ms rotation about the vertical axis (front hidden past 90°); reduce motion:
  150 ms cross-fade.
- **State:** the side is `rememberSaveable`; it is UI state, not progress.

### Code
`DossierCarousel` is replaced by `DossierList` (same inputs: demon, record, ink flags). A
`FlipCard` composable holds front/back and the animation. `GrimoireScreen.kt` (607 lines) moves
the card into its own file (`ui/components/DemonCard.kt`) while it changes.

## 3. Intro card: layout B (amends ADR-0021)

Landscape (the only orientation the Ritual uses):

- **Left slab (25 %):** top to bottom: the title ribbon, the singer art, the **Summon** button.
  - **Ribbon:** a parchment ribbon drawn in code (gradient, notched tails), sized to its text.
    The title `intro_title` breaks into two lines naturally ("HOW TO / CHANT"); its words use the
    fixed-art rule (§1.1). `chant_intro_banner.webp` is retired.
  - **Summon:** the existing button art; label per §1.1; attention glow until the first
    completed summon (§1.2).
- **Right column:** the four rules, vertically centred, scrolling if they don't fit. Each rule:
  a numbered medallion (1–4, `name` role in the accent colour), its HUD icon, title, body.
  Titles become verbs so the numbers read as a sequence:

  | # | Key | English title | Body (unchanged) |
  |---|---|---|---|
  | 1 | `intro_direction_title` | Face | Turn until the frame turns green. |
  | 2 | `intro_chant_title` | Chant | Read the line aloud until it seals. |
  | 3 | `intro_lines_title` | Watch the words | Green: heard. |
  | 4 | `intro_focus_title` | Beat the clock | Each line gets a minute; the ring shows what is left. |

  The "1." / "2." prefixes leave the strings (the medallion carries the number). All five
  languages are updated, none longer than English where possible (ADR-0031), and pass
  `TranslationResourcesTest`.
- **✕** stays top-right; it still starts the chant (ADR-0021).
- Portrait (unused by the Ritual) keeps the band layout with the same pieces.

## 4. Chant screen: one thing glows at a time (notes on ADR-0020, ADR-0029)

| Phase | Sigil ring | Phrase | Direction pill |
|---|---|---|---|
| Seeking | breathing glow, accent | dim, slot shows LOCKED | "FACE EAST →", neutral |
| Locking | no breathing; ring fills green (today) | dim | "FACING EAST ✓", green |
| Chanting | quiet (focus clock only) | soft halo behind the current line | ✓ |
| Wavering | breathing glow, paused amber (`colors.primary`, as today), `fast` | dimmed further | paused, amber, blinking (today) |
| Climax / error panel | no glow | — | — |

The direction pill is always one line in every state and language (§1.1).

## 5. Validation

### 5.1 `tools/ui_matrix.sh`
An adb script (debug build, one connected device or emulator) that, per configuration, sets
`font_scale` and display size, forces the language, walks Home → Grimoire (front, back, both
demons) → intro → chant screen, reaches states through the debug panel, saves screenshots to
`build/ui-matrix/<config>/<screen>.png`, and restores the device settings on exit (also on
Ctrl-C).

### 5.2 Matrix
- Font scale: 1.0, 1.3, 2.0
- Display size: default, largest
- Language: en, fr, pt-BR
- Device: Galaxy S24 (SM-S921B), `Medium_Phone_API_37.0` emulator
- States: unbound, bound, cooling, restless, celebration, abort confirmation, mic denied

### 5.3 Pass criteria (every screenshot)
1. No text is clipped or overlaps art edges; truncation only where §1.1 allows an ellipsis
   (demon title, dossier header).
2. The screen's primary action is fully visible without scrolling.
3. One-line labels are on one line.
4. At most one element glows, and only when §1.2 says it should.
5. At 1.0× in English, the screens match the approved mockups.

Unit tests stay green (`./gradlew testDebugUnitTest`), including `TranslationResourcesTest`.

## 6. Delivery

Slices, each built, installed and checked on the S24 at 1.0× and 2.0× before the next:

1. Shared rules: `attentionGlow`, one-line pill, DESIGN.md text rules.
2. Chant screen glow hand-off.
3. Intro layout B (ribbon, slab, numbered rules, strings ×5).
4. Grimoire flip (`FlipCard`, `DemonCard.kt`, `DossierList`).
5. `tools/ui_matrix.sh`, full matrix run, fixes.

Docs: ADR-0033 "Attention glow and large-text rules"; amendments to ADR-0016 (flip replaces
the carousel), ADR-0021 (intro layout, numbered rules) and notes on ADR-0020/0029 (glow);
PRODUCT.md (Grimoire, Demon card and Ritual setup rows) and DESIGN.md (text rules, glow, ribbon, retired art).

## 7. Out of scope

Tablet layouts, new Stitch art, a swapped intro layout (kept as a play-test question: watch
where testers look after the first Summon), screenshot-test infrastructure (Roborazzi).
