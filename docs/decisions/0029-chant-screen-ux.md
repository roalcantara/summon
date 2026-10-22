# ADR-0029: Chant screen UX: one place per fact, split layout in the Stitch style

- **Status:** Accepted; amended 2026-10-22 (polish)
- **Date:** 2026-10-18 (decided 2026-10-19 after the device comparison)
- **Amends:** ADR-0019 (chant screen), ADR-0020 (direction in the sigil), ADR-0024 (status column)
- **Spec:** `docs/superpowers/specs/2026-10-18-chant-screen-ux-design.md`

## Context

Play-testers did not see that they must face the direction before chanting, could not tell how
close they were, did not know how well a line went or what the phone heard, and missed that
turning away paused the chant. Two Stitch rounds (proposals 01, 02, 03, 08) gave the pieces; the
open question is whether a centred layout keeps the phrase as large as today's columns do.

## Decision

1. One place per fact: the direction pill is the only place that names the direction; the title
   carries the four seal marks; the slot above the line carries all feedback.
2. The line is gated before the lock: dim, with a "LOCKED" chip in the slot. It brightens as the
   lock fills and opens with a soft accent flare.
3. The sigil tilts toward the demon (±35°), the ring draws a proximity arc while seeking, and
   chevrons on the screen edge point the way to turn (amber while paused). After the lock the
   ring shows the lock hold, then the focus clock. The sigil's glow grows with the lock, flares on
   it, then follows the voice.
4. A score (100 × (1 − distance); 40 % seals) shows for 2.5 s when an attempt ends: on a seal,
   or on a miss after the whole line was lit, with what the phone heard. Mid-line checks show only
   "checking".
5. Paused is amber, not red.
6. Split layout, in the Stitch "stage HUD" style (proposal 08). "?" sits top-left and the title
   (name and seals) is centred in a top bar. The sigil and pill take the left 30 %. The feedback
   chip and the line take the right. There are no cards or frames: everything floats over the
   House stage.
7. The phrase's auto-size never breaks a word across lines (the stock auto-size let
   "DAEMONIUM" wrap as "DAEMONIU / M").

## Consequences

- `RitualStatusBlock` is removed; `ChantScreen` is split into pieces (`ChantPieces.kt`) and
  arrangements (`ChantArrangements.kt`: split, plus a portrait band for large screens).
- The session log keeps a `ui` record of the phrase size reached (`UiRecord`), printed by
  `SessionReplayTest`.
- Transcripts reach the screen for 2.5 s; they are still never stored or sent in release builds.

## Device comparison

Played on the S24 (2026-10-19). The split arrangement read best: the phrase keeps the full height
of the right side. The centred one shrank it to fit under the sigil. The first port of the split
arrangement looked heavier than Stitch 08, with a framed box, a dark tile and a slab card, and it
split a word mid-word. It was restyled toward 08 as above. The centred arrangement, the debug
switch and the `layout` log fields were removed; old logs still decode, because unknown keys are
ignored.

## Amendment (2026-10-22): chant screen polish

Spec: docs/superpowers/specs/2026-10-22-chant-screen-polish-design.md. The centred title goes:
the left column is one stack — name plate, sigil (as large as the column allows), direction pill —
all as wide as the sigil; the I–IV seals become a vertical progress track between the columns;
the slot's idle meter is a labelled mic chip (LISTENING / PAUSED), top-aligned with the name
plate, with the phrase centred below it. The top bar keeps only "?" (and "D") and ✕.
The sigil breathes while seeking and wavering; the phrase has a halo while chanting; ADR-0033.
