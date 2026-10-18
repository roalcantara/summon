# 0023 — Focus meter

- Date: 2026-10-18
- Status: Superseded by ADR-0025 (60 s per part, refilled on each passed part, free rejections). Amended ADR-0010 and ADR-0019.

## Context
The focus clock drained while the player chanted, so careful readers of long lines
cracked seals without doing anything wrong.

## Decision
Phrases II–IV have a flat 8 s meter of lost focus (+25 % in court), full at the start of
each line and refilled on each seal. Chanting and pauses under 2 s cost nothing; silence
beyond 2 s drains 1×; wavering drains 3×; a rejected chunk costs 25 % at once. Empty →
the last seal cracks, as before. The verdict hold of ADR-0019 stands. The ring shows the
meter; the seconds and "PAUSED" labels are gone.

## Consequences
Reading speed no longer matters; attention and accuracy do. Repeated nonsense still
cracks after about four rejected chunks.
