# 0012 — Court Hours

- Date: 2026-10-16
- Status: Accepted

## Context
Houses (ADR-0011) needed a reason to matter in play. The Goetia ties Kings and Dukes
to planetary hours; a simple, local-time version gives the player a reason to summon at
certain times without punishing them at others.

## Decision
- Sunrise and sunset are fixed at 06:00 and 18:00 local time (`ProgressConfig`).
- Court ranges (start inclusive, end exclusive): **Sol** 09–12 and 15–18; **Venus** 06–12.
- A ritual whose demon's House is in court **at the moment of lock** gets
  `courtMultiplier = 1.25` (`COURT_BONUS`) for every focus budget of that attempt. It is
  fixed at lock and never changes during the attempt.
- Venus' "clear weather only" is flavour text in the tooltip; weather is not read.
- Home shows which Houses are in court now ("In court · Sol & Venus" / "Out of office").

## Consequences
No location, sunrise tables or weather APIs. A debug override (auto / in / out) exists
in debug builds only.
