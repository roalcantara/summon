# 0011 — Houses, Callings and the demon dossier

- Date: 2026-10-16
- Status: Accepted

## Context
The Profile screen showed only a name, title, description and quote. The prototype
review asked for a card that says at a glance where to face, when the demon is
strongest, what he is for and how mighty he is, plus lore worth unlocking.

## Decision
- Each demon has a **House** (`Sol` or `Venus`). A House carries only what is shown:
  its planet (the badge label), a metal colour token (`house-sol`, `house-venus`), its
  court hours (ADR-0012) and a badge tooltip.
- Each demon has a **Calling**: Warlord, Tutor, Oracle, Treasurer or Menace (name and
  tooltip). Asmodeus is a Tutor, Astaroth an Oracle.
- Each demon has a number, a rank, a legion count and an optional liege.
- The demon card shows four badges: **Direction** (with the liege, or "He swears to no
  king"), **House**, **Calling** and **Legions** ("A {rank} at the head of {n} legions of
  spirits."). The badge rim is the House metal; the glyphs are the demon accent, except the
  House glyph, which is drawn in its metal.
- The **dossier** holds six pages of copy per demon: Weakness, Abilities (exactly 3),
  Offices, Appearance, Summoner's warning and Secret. The copy is demon data
  (`DemonCatalog`, PRODUCT.md "Dossiers").

## Consequences
Adding a demon means adding a House/Calling choice and dossier copy, not code. Stats,
levels and power numbers stay a non-goal (PRODUCT.md).
