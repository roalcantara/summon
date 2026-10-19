# ADR-0029: Chant screen UX: shared pieces, two arrangements on trial

- **Status:** Accepted (arrangement pending the device comparison)
- **Date:** 2026-10-18
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
2. The line is gated before the lock (dim, lock glyph, outline frame) and unlocks with one flare.
3. The sigil tile tilts toward the demon (±35°) and the ring draws a proximity arc while seeking;
   after the lock the ring is the lock hold, then the focus clock.
4. A score (100 × (1 − distance); 40 % seals) shows for 2.5 s when an attempt ends: on a seal,
   or on a miss after the whole line was lit, with what the phone heard. Mid-line checks show only
   "checking".
5. Paused is amber, not red.
6. Split and centred arrangements of the same pieces sit behind a live debug switch; the session
   log records the layout and the phrase size reached. After the S24 comparison one is kept and
   the other, the switch and the layout fields are removed.

## Consequences

- `RitualStatusBlock` is removed; `ChantScreen` is split into pieces and arrangements.
- Release builds use the split arrangement until the comparison decides.
- Transcripts reach the screen for 2.5 s; they are still never stored or sent in release builds.

## Device comparison

_To be filled after the play-test: per demon and line, the phrase sp in each arrangement, and the
choice._
