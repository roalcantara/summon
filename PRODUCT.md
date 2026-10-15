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
