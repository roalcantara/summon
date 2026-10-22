# 0033 — Attention glow and large-text rules

- Date: 2026-10-22
- Status: Accepted
- Spec: docs/superpowers/specs/2026-10-22-pre-release-screens-design.md

## Context
The pre-release review (docs/reviews/2026-10-22-pre-release-ui-review.md) found text clipped
or pushed off-screen at large font sizes on the Grimoire, the intro card and the chant screen,
and players unsure where to look next on the chant screen.

## Decision
1. **Text sizing.** Body text scales fully and its container scrolls. Words on fixed art or HUD
   slots scale at most 1.3× (`fontScaleCapped`) and auto-size down. Labels in one row share one
   size. One-line labels never wrap. A screen's primary action never scrolls away.
2. **One attention cue.** A breathing glow (1.6 s; 0.8 s for alarm; steady with reduce motion)
   marks the one thing to act on: the Grimoire's dossier button while lore is unread, the intro's
   Summon until the first completed summon, the chant sigil while seeking (amber while
   wavering) and a halo behind the phrase while chanting.
3. **Grimoire:** the demon card is a tarot card; its back is the dossier list (amends ADR-0016).
4. **Intro card:** layout B — ribbon, singer and Summon on the left slab, numbered rules on the
   right; the ribbon is drawn in code (amends ADR-0021).
5. **Chant screen polish:** The current word is marked by colour, a 2× underline and a soft glow, never by size; the phrase keeps one size per line and breaks balanced (`LineBreak.Heading`).

## Consequences
- `chant_intro_banner.webp` and `DossierCarousel` are gone.
- Every new screen applies these rules; `tools/ui_matrix.sh` checks them per configuration.
- Open: whether the intro's slab should sit on the right (play-test question).
