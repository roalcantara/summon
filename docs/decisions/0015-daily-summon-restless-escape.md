# 0015 — Daily summon, restless and escape

- Date: 2026-10-16
- Status: Accepted

## Context
With two demons the game ended after two rituals. A light daily rhythm and a cost for
neglect give a reason to come back without currencies or timers to grind.

## Decision
- **One summon per local calendar day** across all demons:
  `canSummon = lastSummonDay == null || today > lastSummonDay`. A clock moved backwards
  never unlocks early. A ritual begun at 23:59 that climaxes after midnight spends the
  day it started on.
- **Visit:** viewing a bound demon in the Grimoire is the visit (`lastVisitDay = today`).
  Tapping a bound demon does nothing.
- **Restless:** 2 days without a visit (`RESTLESS_AFTER_DAYS`). He jitters, says his
  restless quote for that viewing, and the Home grimoire gets jumpier.
- **Escape:** 3 days without a visit (`ESCAPE_AFTER_DAYS`). He is unbound again, his
  one-time effects are cleared and an escape banner is queued; `revealed` and
  `flawless` are kept ("lore kept").
- `evaluate` runs after the first load and on every activity start. A visit applies
  `evaluate` first in the same update, so a demon left on screen while the app sat in
  the background for 3+ days escapes instead of being visited.
- Where Home opens the Grimoire: the first restless demon; else, if a summon is
  available, the first unbound one; else the last viewed; else the first.

## Consequences
Days are local epoch days taken from an injected clock, so tests and the debug sheet
can move time. Notifications and reminders stay out of scope.
