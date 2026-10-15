# 0005 — Chant phrasing and text

- Date: 2026-10-15
- Status: Accepted (one open question)

## Context
Prompt 1 says each 2 s of sustained chanting raises sigil opacity by 25%, which
implies 4 steps. The drafts split Asmodeus into 4 phrases and Astaroth into 3.
Astaroth's chant contains "LOVIS", likely a typo for "IOVIS" (Jove).

## Decision
Every demon's chant has exactly 4 phrases (see PRODUCT.md). Keep "LOVIS" verbatim.

## Consequences
Opacity runs 0.10 → 0.25 → 0.50 → 0.75 → 1.00 for every demon.
Open question: confirm "LOVIS" vs "IOVIS" with the product owner, then update
PRODUCT.md and this ADR.
