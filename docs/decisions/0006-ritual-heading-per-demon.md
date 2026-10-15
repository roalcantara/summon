# 0006 — Ritual heading is per-demon data

- Date: 2026-10-15
- Status: Accepted

## Context
Prompt 1 hard-codes "face North" (azimuth 340°–20°). Facing North is a trait of
Asmodeus and Astaroth, not of rituals in general; future demons may require
South, East, etc.

## Decision
Each demon carries a `ritualHeading` (compass degrees) with a ±20° tolerance. The
ritual instruction text, the Active gate, the compass target and the debug
"Simulate heading" control all derive from it. Both MVP demons use 0° (North).

## Consequences
No code or copy may hard-code North. Adding a demon with another direction is a
data change only.
