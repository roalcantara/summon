# 0009 — Vector sigils with ink progress

- Date: 2026-10-15
- Status: Accepted

## Context
Stitch could not render a correct Asmodeus sigil (its rim spelled "ASTAROTH"), and
the Astaroth PNG was not accurate either. PNG sigils with baked backgrounds also
can't be tinted, scaled crisply or animated per stroke.

## Decision
Sigils are stroke-only vector paths traced from public-domain/CC0 sources:
Asmodeus from Wikimedia Commons `File:Seal_of_Asmodeus.svg` (CC0), Astaroth from
`File:Astaroth_Seal.svg` (public domain; Mathers & Crowley 1904). Sources live in
`art/sigils/<id>.svg` (viewBox `0 0 100 100`, groups `ink-1`…`ink-4`, one contour
per path). `tools/sigil_to_raw.py` converts them to `res/raw/sigil_<id>.txt`. The
outer rings and rim letters (the demon's name) are drawn in code.
The sigil **inks itself** as the chant progresses:
`inkProgress = (completed phrases + lit/syllables) / 4`; group *g* inks over
[(g−1)/4, g/4]. It is tinted with the demon accent and glows with the live mic level.

## Consequences
Replaces the opacity steps 0.10/0.25/0.50/0.75/1.00 (PRODUCT.md "Sigil", ADR-0005
consequence). `sigil_astaroth.webp` is retired. A new demon needs one SVG traced into
the same format.
