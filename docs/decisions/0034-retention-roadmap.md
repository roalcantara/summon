# 0034 — Retention roadmap: ship v1, then a daily reason to return

- Date: 2026-10-23
- Status: Accepted
- Analysis: `.claude/docs/ai/gameplay/10x/session-1.md`

## Context
The game will be ad-funded (with a friendlier approach than the app that inspired it), so it
only works if players return. With two demons and one summon a day, both are bound by day 2;
from day 3 the daily summon has no use, the chant (the app's most distinctive action) is locked
until a demon escapes, and the only return reason is avoiding an escape (ADR-0015).

## Decision
1. **Ship v1 as it is.** The release is otherwise ready; real usage will show whether players
   drop off after day 2.
2. **v1.1, shortly after release** (each item needs its own spec and plan):
   - **Renew the pact:** the day's summon may be spent on a bound demon; each renewal adds to his
     bond and clears restlessness.
   - **Lore over days:** the first summon reveals the first dossier entries; each renewal inks in
     the next (the Secret still needs a flawless summon).
   - **Bond counter** on the demon card.
3. **Next:** an opt-in daily reminder at court hours (local, no network); a new daily line from
   each bound demon; player-chosen rewarded ads at moments of need (calm a restless demon, a second
   summon).
4. **Explore:** more demons released over time.

## Consequences
- v1.1 revises two PRODUCT.md non-goals (levels: the bond counter; notifications: the reminder)
  in its own ADRs; v1 changes nothing.
- Ads end the "no network, no data collected" stance (ADR-0017): a separate ADR covers the SDK,
  the privacy policy, the Play Data safety answers and consent.
- Success measurement (retention after day 2, daily rituals) is defined before v1.1 so the
  change can be judged.
