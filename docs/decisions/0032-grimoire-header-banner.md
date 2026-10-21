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

1. The Grimoire header is: back arrow (left), a title banner with "GRIMOIRE" in Cinzel (centre,
   translated: GRIMÓRIO, GRIMORIO), the menu gear (right). Art: `grimoire_title_banner`
   (`art/grimoire/`, Stitch). The title is a heading for screen readers.
2. The avatar rail and `SigilAvatar` are removed. Switching demons is the stage swipe and the
   arrows; bound vs sealed shows on the stage (full art vs silhouette); restlessness shows as the
   stage jitter and the restless quote.

## Consequences

- The avatar "burn" (the sigil burning away into the portrait on the first visit after binding)
  is gone. `Reveal.Avatar` is still consumed, so no state is left pending. Bringing a reveal moment
  back, e.g. the sigil on the floor circle under the demon, is the open proposal item (todo `JwGM`).
- Nothing on screen says which demon of how many is shown; position dots under the banner are the
  open option if testers miss it.
