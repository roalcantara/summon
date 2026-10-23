# "Psst" call to action: Stitch prompt for the balloon pair

- Date: 2026-10-23 (rewritten the same day: Stitch regenerates whole screens, so we ask for an
  asset sheet instead of a screen redesign)
- Status: Ready for Stitch (operator runs it)
- Replaces the look of: the code-drawn balloon in `docs/superpowers/specs/2026-10-23-psst-teaser-design.md`
  (ADR-0036). The test, its eligibility and its events do not change.

## Why
The shipped balloon reads as a chat bubble pasted onto the art: a flat violet rounded rectangle
foreign to the game's gilded plaques, parchment ribbons and dashed quote bubble.

## Split of work
- **Stitch: the art.** Two balloons, before and after the tap, as an asset sheet (not a screen).
  Empty inside: the text stays in code (5 languages, font scale up to 200 %).
- **Code: the behaviour.** Decided after the art, not by Stitch: the balloon floats with the
  demon (±6 dp, 4 s), tapping the demon also counts (already so), the reply shows for 3 s, a steady
  version with reduced motion.

## How to run it
1. In Stitch, new design in the "Summon Demons Safely" project. Attach as style references: the two
   Grimoire screenshots of the current balloon (invite and reply), and
   `reference/stitch/summoner_s_grimoire_tactical_rpg/`.
2. Paste the **base prompt**, then add **one** style line from the list below. Generate.
3. Repeat with another style line (a fresh design each time) for as many looks as you want; 4–6 is
   plenty.
4. Save each result as `reference/stitch/psst_balloon_NN_<style>/` (the sheet PNG, plus the separate
   asset PNGs if Stitch exports them). Agents never edit `reference/`.

## Base prompt (paste every run)

```text
Asset sheet, not an app screen. Plain flat background #0b1326, nothing else on it: no phone frame,
no status bar, no header, no demon, no other UI.

Draw two speech balloons for a dark fantasy RPG mobile game with cute, comedic chibi demons. A
demon whispers to the player from the top-right of his portrait; the balloon's tail points down
and to the left, toward his head.

Row 1 — "Invite" balloon (before the tap): small, playful, a little mysterious, as if whispered.
Row 2 — "Reply" balloon (after the tap): the same family, slightly larger and livelier, as if he
laughs; it holds up to three short lines.

For each balloon show:
(a) the balloon EMPTY inside (no text), as a clean asset with a transparent background;
(b) the same balloon with sample text, only to preview: Row 1 "Psst… come closer." and Row 2
    "Heh heh heh… Come back soon… if you dare, mortal." in Cinzel Bold, colour #dae2fd.

Style rules:
- Match the game: obsidian navy surfaces (#171f33, #222a3d), imperial gold #ffc174 rims and
  ornaments, muted bronze #a08e7a secondary lines, soft inner glow. Gilded plaques, parchment
  ribbons and dashed accent borders are the game's existing shapes; rhyme with them.
- Neutral enough for every demon: no demon-specific colour baked in; a thin inner accent line may
  be tinted later in code (show it in gold).
- The middle of each balloon must stretch horizontally and vertically without distorting its
  corners and ornaments (nine-patch friendly): keep ornaments in the corners and on the tail only.
- Interior: a calm, dark, even area so light text stays readable (contrast at least 4.5:1).
- Readable at small size: the invite balloon is about 140 x 56 dp on screen, the reply about
  180 x 90 dp.
- No speech bubble clichés from chat apps (no flat rounded rectangle, no bright purple fill).
```

## Style lines (add one per run)

```text
Style: a gilded plaque balloon — chamfered corners like a small title plaque, double gold rim, tiny diamonds in the corners, the tail as a gilded spike.
```
```text
Style: a parchment scrap — torn aged-paper edges darkened by smoke, ink-brown outline, a small wax drop on the tail; the interior is darkened parchment.
```
```text
Style: a smoke wisp — the outline is curling dark smoke with a faint gold edge light, the tail a thinning wisp; interior deep navy with soft fog.
```
```text
Style: a dashed whisper — the same family as the game's dashed quote bubble: rounded, dashed gold border, faint inner glow, small curled tail; reply version with a solid border.
```
```text
Style: an arcane scroll — a short unrolled scroll with curled ends left and right, gold-trimmed edges, the tail a ribbon end; the reply is the scroll unrolled further.
```
```text
Style: a stained-glass shard — dark glass panel in lead-came outline with gold leading, a faint inner glow like candlelight behind glass, tail a narrow glass sliver.
```

## What we judge the results on
1. Belongs to the game (gilded, parchment, ink, glow), not to a chat app.
2. Works on both demons with no per-demon art.
3. The empty versions stretch cleanly (ornaments only in corners and tail).
4. Light text stays legible inside, at small sizes.
5. Cost to build: a nine-patch PNG or a shape we can redraw in code beats a complex illustration.
