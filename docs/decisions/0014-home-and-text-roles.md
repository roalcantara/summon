# 0014 — Home, text roles and the chant tablet

- Date: 2026-10-16
- Status: Accepted

## Context
The app opened straight on the Grimoire pager, with no place to show what changed
while the player was away. Ritual text mixed sizes and styles without a system.

## Decision
- **Home — "The Restless Grimoire":** a chained, locked book on an inlaid compass rose.
  It idles, hops every 5–8 s and has a silhouette peek out every 10–15 s; while any
  demon is restless it hops every 2–3 s, peeks every 4–6 s and its lock glows in the
  focus demon's accent. Tapping it opens the Grimoire. It also shows the court chip,
  "{n} of 2 spirits bound", a first-run hint and the escape banner. Until Stitch art
  exists the book is a vector placeholder (DESIGN.md Gaps).
- **Text roles** (DESIGN.md Typography): `cue` (label-medium, +0.08 em, upper case),
  `incantation` (Cinzel 15 sp Bold, line 22 sp, +0.02 em), `meta` (label-small,
  +0.14 em, upper case, `outline`), `name` (Cinzel Bold, size per use), `body`
  (body-medium).
- **Ritual:** a **cue chip** replaces the phase hint; the **altar ring** replaces the
  compass arc; the **chant tablet** replaces the chant stack (one row per phrase in
  `incantation`, I–IV medallions, sealed count or focus seconds in the header, a focus
  bar under the current phrase).

## Consequences
Glyphs that the bundled Latin-subset fonts lack (☉ ♀, compass, swords) are drawn as
vector paths. Compose has no text-transform, so `cue` and `meta` strings are upper-cased
at the call site with `Locale.ROOT`.
