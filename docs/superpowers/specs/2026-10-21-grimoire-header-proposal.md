# Grimoire header: banner instead of the symbols, sigil on the floor circle (proposal)

- **Date:** 2026-10-21
- **Status:** Proposed. Review after the 1.0 release; nothing decided yet. If accepted, record it in
  an ADR (amends ADR-0016) and update DESIGN.md.
- **Todos:** `TKJp` (banner replaces the symbols), `JwGM` (sigil on the floor circle)

## Why

In user tests, players could not say what the symbols at the top of the Grimoire represent (the
avatar rail: one sigil or portrait per demon), and some were unsure which screen they were on. The
RC added gilded prev/next arrows on the stage; a title banner may be all the header needs.

## Proposal

1. **Banner replaces the avatar rail.** Top bar: back arrow (left), the Stitch title banner with
   "GRIMOIRE" in Cinzel (centre), menu gear (right). Art: `art/grimoire/grimoire_title_banner.png`
   (1200 × 260, centred). New string `grimoire_title` in five languages (GRIMOIRE, GRIMÓRIO,
   GRIMORIO, GRIMOIRE; 8 characters each).
2. **The demon's sigil on the floor circle under him.** Drawn from the sigil paths the rail and the
   chant screen already load (`rememberSigil`), in the demon's accent, squashed vertically to lie in
   perspective inside `FloorCircle`, pulsing with it while he can be summoned. The demon's feet cover
   part of it. This moves the sigils from the header to where they mean something.
3. **Optional: position dots** under the banner (● ○), if "which demon of how many" turns out to
   matter.

## What the avatar rail does today, and what happens without it

| The rail… | Without it | Verdict |
|---|---|---|
| Jumps to a specific demon | The stage arrows step between demons (2 demons) | Covered |
| Shows which demons are bound | The stage shows it (silhouette vs full art) | Covered |
| Marks a restless demon | The stage jitters and he says his restless quote | Covered |
| Shows which demon of how many | Nothing | Small gap; option 3 fills it |
| Plays the avatar "burn" reveal on the first visit after binding | Lost | The real trade-off: a reward moment. No state bug: `Reveal.Avatar` is still consumed by the screen's `LaunchedEffect`. Could come back on the floor circle (sigil burns away to reveal him). |

## Estimate

| Item | Effort | Risk |
|---|---|---|
| Sigil on the floor circle | ~30 min | Low: reuses `rememberSigil` paths |
| Banner replaces the rail (layout, string ×5, art conversion, ADR note, DESIGN.md) | ~1–1.5 h | Low |
| Position dots (optional) | ~15 min | Low |
| Emulator check in two or three languages | included | |

Suggested order: floor sigil (independent), then the banner, then the dots if wanted.
