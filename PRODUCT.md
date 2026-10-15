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
| Grimoire (Home) | `Scaffold` with top bar "My Grimoire"; a full-bleed `HorizontalPager`, one demon per page: darkened scene art, the demon's complete sigil, the cut-out art, name and CTA. The page indicator is a row of demon avatars. |
| Lock state | Both demons start **locked**. Locked page: near-black silhouette with an accent rim light and drifting smoke, a faint sigil, "Sealed · Face {direction} to summon", **Summon** → Ritual. Unlocked page: full colour, title, **View** → Profile. Unlock state is in-memory only. |
| Ritual setup | Full-bleed altar: the demon's (un-inked) sigil ringed by a compass arc, and a hint naming the demon's direction from `ritualHeading` (e.g. "Turn left · Face North"). |
| Heading gate | `ritualHeading` is a **per-demon property** (compass degrees, tolerance ±20°). Staying within tolerance for **0.6 s** locks the ritual (haptic + sound); chanting only counts while locked. Drifting out while chanting starts a **1.5 s grace** (warning haptic, red compass, flickering sigil): returning in time loses nothing; otherwise the current phrase's progress is lost (completed phrases stay) and the player must lock again (ADR-0008). Nothing may hard-code North. |
| Chant | The incantation is **exactly 4 phrases**: completed ones in the demon's color, the current one highlighted syllable by syllable (karaoke), upcoming ones dim. A syllable lights on each voiced syllable onset (≥ 120 ms apart) or after 350 ms of continuous voicing; sounds that aren't voice-like (claps, taps, wind) don't count. All syllables lit → phrase complete (chime + haptic). Silence pauses without penalty (ADR-0008). |
| Sigil | Vector line art in the demon's accent that **inks itself** as the chant progresses: ink = (completed phrases + lit/syllables) / 4. The rings and rim letters ink on lock; the glow follows the live mic level (ADR-0009). |
| Success | After phrase 4: a 2.5 s climax (screen dims, particles converge, rising sound and haptic crescendo, flash, the demon rises out of the sigil), then Profile with `isNewSummon = true`. The demon art carries over as a shared element. |
| Profile (new summon) | `RibbonBanner` "New Demon Received!", rotating sunburst, floating cut-out hero art, "Tada!" sound, button **Awesome!** pinned to the bottom → Grimoire. |
| Profile (view) | Same layout without banner and sound; button **Back**. |
| Profile content | Cut-out art, name, title, description, quote. No stats. |
| Debug aids | "Simulate heading" (snaps to the demon's `ritualHeading`), "Hold to Chant" (synthetic voiced syllables) and **Unsummon** on Profile (view), which re-locks the demon — **debug builds only**. |
| Orientation | Portrait only (the compass reading assumes it). |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual only if not already granted; if denied, explain why and offer a retry (or a shortcut to app settings once the denial is permanent). |
| Motion | If system animations are off, particles, parallax, floating and shimmer are skipped; the climax keeps its timing but the demon simply fades in. |

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
- Word or speech recognition (only loudness, pitch periodicity and syllable onsets are measured).
- Gemini / AI features.
- Persistence across app restarts.
