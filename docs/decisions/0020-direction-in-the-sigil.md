# 0020 — Direction in the sigil

- Date: 2026-10-18
- Status: Accepted; amended 2026-10-22. Amends ADR-0019 (the chant screen now covers the whole ritual) and
  supersedes the portrait altar of ADR-0014. Amended by ADR-0021: no verdict text
  before the lock (only the count), and the intro card shows before every ritual.

## Context
Commit `a7f4c27` made the landscape chant screen (ADR-0019) run from the very start of
the Ritual and removed the portrait altar. That is the intended flow: the altar has no
use any more. But the altar carried three features the chant screen lacked:

- the **direction marker** on `AltarRing`, which showed where the demon lies relative to
  the phone and is the only way to *find* the direction;
- the **lock fill**, the ring filling green over the 0.6 s hold;
- the **error panels**: microphone denied (with Retry / Open settings), microphone
  unavailable, no heading sensor.

A device test showed the result: off course, the player sees "← FACE EAST" and a red
ring, but nothing on the sigil points the way, and before the lock nothing guides at all.

## Decision
The chant screen covers the whole Ritual, and the sigil carries the direction:

- **Marker on the sigil ring:** a triangle in the gap between the ring and the sigil,
  pointing outwards at the demon relative to where the phone faces (the same angle as
  `AltarRing`: `signedDelta(azimuth, target)`, 0 = top). Ritual-safe green when within
  tolerance, the demon's accent otherwise; hidden while the heading is unknown.
- **The ring by phase:** seeking, the track only (plus the marker); locking, a
  ritual-safe arc fills with the 0.6 s hold; after the lock, the focus clock (ADR-0019);
  wavering, red.
- **Faint sigil before it inks:** the sigil is drawn at 12 % opacity (the altar's ghost)
  so the symbol is visible before the first syllable.
- **Direction line colours:** green with ✓ when facing; red, blinking, with the arrow
  only when **drifting** after the lock; the neutral on-surface colour, with the arrow,
  while seeking (not an error yet).
- **Verdict line before the lock:** "FIND THE DIRECTION" while seeking, "HOLD STEADY"
  while locking (then "CHANT ALOUD", ADR-0019).
- **Error panels on the chant screen:** when the microphone is denied or unavailable, or
  the device has no heading sensor, the panel replaces the phrase and the verdict line
  (same copy and actions as the altar had).
- **Intro card on the first ritual only:** `a7f4c27` also passed `showIntro = true`, so
  the card showed on every ritual; it goes back to ADR-0019's rule
  (`!progress.chantIntroSeen`, then "?").
- **Cleanup:** the altar's components go: `AltarRing`, `ChantTablet` (with `FocusBar`,
  `Medallion`, `FocusTone`), `KaraokeLine`, `RitualCue` (cue chip rows and the chant
  header) and their tests, and the strings only they used. `CueChip` stays (Home uses
  it). `ROMAN_NUMERALS` moves to its own file.

## Consequences
- PRODUCT.md: "Ritual setup" describes the chant screen from the start; "Chant screen"
  no longer says "from the lock"; "Heading gate" mentions the marker and lock fill.
- DESIGN.md: `ChantSigil` gains the marker and lock fill; the removed components leave
  the component table.
- No rule changes: lock, tolerance, wavering and the clock are as in ADR-0010/0019.
- Plan: `docs/superpowers/plans/2026-10-18-chant-direction.md`.

## Amendment (2026-10-22)
The sigil breathes while seeking and wavering; the phrase has a halo while chanting; ADR-0033.
