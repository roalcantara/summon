# 0021 — Intro before every ritual, slab buttons, debug panel

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0019 (intro card, turn-sideways hint, "?", debug controls)
  and ADR-0020 (verdict line before the lock, intro on the first ritual only).

## Context
A device test of the chant screen with ADR-0020 in place showed five problems:

- The "Turn your phone sideways" hint and the intro card did the same job twice; the
  hint flashed for 2.5 s and was gone before anyone read it.
- The intro card's ✕ called the Ritual's close, so it left for the Grimoire (or asked to
  abandon) instead of revealing the chant screen.
- The "?" sat in the top-right corner next to ✕, small and easy to mistake for it.
- "FIND THE DIRECTION" under the phrase repeated what the sigil and "← FACE EAST" already
  say.
- The debug controls sat under the verdict line, in the layout: in debug builds they
  pushed the phrase up and shrank it, so the screen testers saw was not the release
  screen.

A prototype (v12) was approved on 2026-10-18.

## Decision
- **Intro card before every ritual.** It replaces the turn-sideways hint entirely (the
  card already says to hold the phone sideways by its own layout). `chantIntroSeen`
  leaves `Progress`; older saves that carry it still load (unknown keys are ignored).
- **✕, back and SUMMON all put the card away** and reveal the chant screen. Leaving the
  Ritual is the chant screen's ✕ or back once the card is gone.
- **"?" above the demon's name.** A round button (36 dp disc in a 48 dp touch target),
  centred above the name on the slab; the top-right corner keeps only ✕.
- **"D" next to "?" (debug builds only).** The same round button with a dashed outline in
  the outline colour, filled primary while open. It toggles a small floating panel
  (300 dp wide, under the buttons, kept inside the window) with the debug controls as
  compact chips: Simulate heading, Hold to Chant, Record clip, and the last chant-check
  score. Closed by default; a tap outside or back closes it. It floats, so opening it
  moves nothing, and closed it takes no space: debug and release builds lay out the
  chant screen identically, except for the "D" beside the "?".
- **No verdict text before the lock.** While seeking and locking, the line under the
  phrase is only the count "I of IV"; the sigil (marker, lock fill) and the direction
  line guide. "CHANT ALOUD" and the rest are unchanged.

## Consequences
- PRODUCT.md: "Ritual setup", "Chant screen" and "Debug aids" rows. DESIGN.md:
  `ChantScreen` row; a `DebugControls` row.
- Removed: `TurnSidewaysHint`, `showTurnHint`, `introOffered`, `onIntroSeen`,
  `markChantIntroSeen`, `chantIntroSeen`, `VerdictLine.FindDirection` / `HoldSteady` and
  their strings. Added: `VerdictLine.Count`, the slab buttons, the debug popover.
- No rule changes: lock, clock and chant check are as before. The microphone is still
  ignored while the card is up, so every ritual starts with a deliberate SUMMON (or ✕).
- Plan: `docs/superpowers/plans/2026-10-18-chant-intro-and-debug.md`.
