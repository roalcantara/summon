# 0024 — Status column and rules on the intro card

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0019 (direction line, verdict line, "I of IV") and ADR-0021 (intro card rules). Amended by ADR-0027 (whole-line seals, no rejection state).

## Context
Progress was split between the sigil's ink and an "I of IV" count in the verdict line;
the direction line and the progress did not read as one task. Prototype iterations v10–v12.1
(https://claude.ai/artifact/9rCV9zSJdqY5mJscEn5EiQ) and a Stitch exploration
(`docs/stitch-prompts/2026-10-18-ritual-status-block.md`, option "Compass-and-Tally")
settled the layout.

## Decision
- Under the sigil, one framed block with two rows: "1. FACE EAST" with → / ← / ✓, and
  "2. CHANT" with four round seals whose rings show the line's parts.
- Both labels: Inter Bold, upper case, letter-spacing 0.12 em, `outline` colour, one line.
  Only the numbers, the arrow and the frame change colour: yellow while turning or
  chanting, green when aligned or done, red when drifting after the lock (the frame then
  red, row 1 blinking); the frame takes the demon accent when all four are sealed.
- The verdict line keeps the voice meter and only "CHANT AGAIN ✕" and "SEAL CRACKED ✕".
- The intro card carries the rules: 1. Face, 2. Chant, Lines, Focus.

## Consequences
The left panel is the single status display. Less text on the chant screen; the intro
card must be read once per ritual (ADR-0021 already shows it every time).
