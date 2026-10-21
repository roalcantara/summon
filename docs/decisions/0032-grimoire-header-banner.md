# ADR-0032: Grimoire header: title banner instead of the avatar rail

- **Status:** Accepted
- **Date:** 2026-10-21
- **Amends:** ADR-0016 (the Grimoire under the avatar rail)
- **Proposal:** `docs/superpowers/specs/2026-10-21-grimoire-header-proposal.md`

## Context

In user tests, players could not say what the symbols at the top of the Grimoire meant (the
avatar rail: each demon's sigil, or his portrait once bound), and some were unsure which screen
they were on. The RC added gilded prev/next arrows on the stage to switch demons.

## Decision

1. The Grimoire header is: back arrow (left), "GRIMOIRE" in Cinzel on a title plaque (centre,
   translated: GRIMÓRIO, GRIMORIO), the menu gear (right). The title is a heading for screen
   readers. *Revised the same day:* the Stitch banner art (`art/grimoire/grimoire_title_banner.png`)
   looked muddy, dark navy on the dark stage arch with noisy tails at 48 dp, so the plaque is drawn
   in code instead (`TitlePlaque`: chamfered navy plaque, gold border and inner line, gold diamonds
   beside the title; option B of the header prototype). It stays sharp and grows with the title.
2. The avatar rail and `SigilAvatar` are removed. Switching demons is the stage swipe and the
   arrows; bound vs sealed shows on the stage (full art vs silhouette); restlessness shows as the
   stage jitter and the restless quote.

## Consequences

- The avatar "burn" (the sigil burning away into the portrait on the first visit after binding)
  is gone. `Reveal.Avatar` is still consumed, so no state is left pending.
- Each demon's sigil now lies on the summoning circle under him (`FloorCircle`, todo `JwGM`), so the
  sigils move from the header to where they mean something. A reveal moment there (e.g. the sigil
  flaring once after binding) is a possible follow-up.
- Nothing on screen says which demon of how many is shown; position dots under the banner are the
  open option if testers miss it.
