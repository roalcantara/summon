# 0016 — The demon card replaces Profile

- Date: 2026-10-16
- Status: Accepted; amended 2026-10-22

## Context
Profile duplicated the Grimoire page with a bigger picture, and the success screen
lived there. The prototype review merged both into the Grimoire.

## Decision
- The Grimoire is one column under the avatar rail: a **stage** (the demon, his floor
  circle and a fixed 72 dp quote slot) over a **static demon card** (no expand/collapse):
  header, info band, four badges and a six-page dossier carousel.
- The card is 50 % of the area below the rail; the stage takes the rest. Neither changes
  size between demons or states (at the default font scale; at large font scales the
  card may grow so its minimum content fits).
- Tapping a badge shows its explanation in the info band (5 s, another tap, a tap
  elsewhere, or switching demon brings the description back). The band is as tall as
  the tallest text of every demon and badge.
- The new-summon celebration ("New Demon Received!", sunburst, "Tada!", **Awesome!**)
  plays on the Grimoire stage. There is no Summon / View / Back button: tapping the
  sealed demon or the "Tap to summon" cue starts the ritual.
- Viewing a bound demon is the visit (ADR-0015).

## Consequences
`ProfileScreen`, the `Profile` route and the debug Unsummon on Profile are removed
(Unsummon moves to the debug sheet).

## Amendment (2026-10-22)
The dossier carousel is replaced by the back of a flipping card; the card's height is the front's natural height, at least 36 % of the area below the header; ADR-0033.

## Amendment (2026-10-23): demons as pages

Moving between demons uses the M3 lateral pattern as a pager: each demon is a full page (scene,
stage and card) that follows the finger and snaps, wrapping both ways; ‹ › animate it, and with
reduce motion they switch instantly. The header and the arrows stay fixed above the pages; swiping
is off during the celebration. A settled page is the selection.
