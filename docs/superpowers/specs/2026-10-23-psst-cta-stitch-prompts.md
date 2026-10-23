# "Psst" call to action: Stitch prompts for 10 directions

- Date: 2026-10-23
- Status: Ready for Stitch (operator runs it)
- Replaces the look of: the balloon in `docs/superpowers/specs/2026-10-23-psst-teaser-design.md`
  (ADR-0036). The test itself, its eligibility and its events do not change.

## Why
The shipped balloon reads as a chat bubble pasted onto the art: a flat violet rounded rectangle
foreign to the game's gilded plaques, parchment ribbons and dashed quote bubble; a second speech
bubble from a demon who already speaks in the quote slot; still while the demon floats; covering
the art's corner. We want options that belong to the game, including ones where the player
interacts with the demon himself instead of a separate balloon.

## How to run it
1. In Stitch, start a new design in the existing "Summon Demons Safely" project (or attach its
   design system). Attach as references:
   - two Grimoire screenshots of the current balloon (invite and reply; the ones from the
     2026-10-23 review, Asmodeus, English);
   - one Grimoire screenshot of Astaroth (purple accent) so each direction is checked on both demons;
   - `reference/stitch/the_grimoire_demon_roster_details/` and
     `reference/stitch/summoner_s_grimoire_tactical_rpg/` for the house style.
2. Paste **the shared context** below, then **one** direction prompt. Generate. Repeat with a fresh
   design for each of the 10 directions (one direction per run keeps them distinct).
3. Save each result as `reference/stitch/psst_cta_NN_<slug>/` (`screen.png` per frame, plus any asset
   PNG and Stitch's code if it gives some). Agents never edit `reference/`.
4. Then we compare them side by side and pick one (or a blend); the pick becomes an amendment to
   ADR-0036 and a short plan.

## Shared context (paste first, every run)

```text
Mobile game UI, Android portrait, 412 x 915 dp. "Summon Demons Safely": a dark tactical fantasy RPG
look carrying cute, comedic chibi demon art. Redesign ONE element of the existing Grimoire screen
(attached screenshots): the "Psst" call to action. Keep the rest of the screen exactly as it is:
header plaque "GRIMOIRE" with back arrow and gear, the stage with the floating chibi demon on his
glowing summoning floor circle, gilded side arrows, the dashed quote bubble under him, and the
tarot demon card below.

What the element must do (a playful fake-door test, comedic tone):
- Invite: a bound demon wants the player to come closer. Copy: "Psst… come closer."
- When tapped, he answers in character for 3 seconds, then returns to the invite.
  Copy: "Heh heh heh… Come back soon… if you dare, mortal."
- Tapping the demon himself must also work; tapping does nothing else (no navigation, no penalty).

Visual language to follow (from the app's DESIGN.md):
- Surfaces obsidian navy #0b1326 / #171f33 / #222a3d; text #dae2fd; imperial gold #ffc174 for rims,
  plaques and borders; muted bronze #a08e7a for secondary lines.
- Each demon has an accent: Asmodeus gold #ffd54f, Astaroth magenta #e040fb. The element must use
  the CURRENT DEMON'S ACCENT (or neutral gold), and must look right on both demons.
- Type: Cinzel Bold for names and short in-world words; Inter for body text. Cute lives in the
  characters and the copy; the interface stays dark, gilded and glowing.
- Existing shapes to rhyme with: gilded chamfered plaque, parchment ribbon with notched tails,
  dashed rounded quote bubble with the accent border, pill buttons, 12-24 dp corners.
- One "attention glow" on screen at a time: a soft breathing glow in the accent colour (1.6 s).
  The invite may use it; nothing else on the screen glows while it does.

Hard constraints:
- At most ONE new illustrated asset, shared by every demon (no per-demon art). Prefer none:
  code-drawable shapes, text, light, smoke, motion and the existing demon art are all fine.
- Never cover the demon's face or the quote bubble's text. Tap target at least 48 x 48 dp.
- Text must fit 5 languages (English is the longest) and the system font at 200 %: short lines,
  up to 3 lines for the reply, shrinks to fit inside fixed art.
- If it moves, it moves WITH the floating demon (he bobs ±6 dp on a 4 s cycle), never stuck on top.
- Reduced motion: describe a still version (no floating, no particles, steady glow).

Deliver for this direction:
1. Frame A: the Grimoire with the invite (Asmodeus).
2. Frame B: the same moment with the reply showing (Asmodeus).
3. Frame C: the invite on Astaroth.
4. A short note: what is tapped, how it moves (durations), the reduced-motion version, and the
   single new asset if any (as a separate transparent PNG).
```

## The 10 directions (paste one per run, after the shared context)

At least half let the player interact with the demon himself.

### 01 · He leans in (demon only, no new art)
```text
Direction: no separate element at rest. Every few seconds the demon himself leans toward the viewer
(a small tilt and 6 % scale-up toward the camera, as if whispering), and a tiny curl of accent-coloured
smoke escapes beside his mouth. A small cue chip under the floor circle reads "PSST… TAP HIM". Tapping
the demon makes him lean in fully and the reply appears in the existing dashed quote bubble, replacing
his quote for 3 s, then the quote returns.
```

### 02 · The quote bubble whispers (demon only, no new art)
```text
Direction: reuse the existing dashed quote bubble under the demon. While eligible, it shows the
invite instead of his usual quote, in the accent colour with a small beckoning-hand glyph and a slow
breathing glow on its border. Tap the demon or the bubble: the bubble pops (small spring) and shows
the reply in italic, then returns to the invite. No other new element on the screen.
```

### 03 · Smoke-written words (no new art)
```text
Direction: the invite is written in the air beside the demon's head in wisps of accent-coloured
smoke, Cinzel letters that slowly drift and re-form, floating with him. No container at all. Tapping
the demon or the words blows the smoke away and the reply forms in the same smoky lettering for 3 s.
Show how the smoke letters stay readable over the stage art (soft dark halo behind them).
```

### 04 · Press and hold to listen (demon only, no new art)
```text
Direction: a ritual gesture instead of a tap. A small gilded ear glyph with "HOLD TO LISTEN" sits on
the floor circle under the demon. The player presses and holds on the demon: a ring fills around the
floor circle in the accent colour (about 1 s, like the ritual's lock), the demon leans in, and on
release the reply appears in the quote bubble. A quick tap shows the hint "Closer… hold". Design the
ring-filling state as an extra frame.
```

### 05 · A sealed letter at his feet (one shared asset)
```text
Direction: one shared illustrated asset: a small folded parchment letter closed with a wax seal,
the seal tinted with the demon's accent colour. It lies on the floor circle at the demon's feet,
breathing softly, with "For you, mortal" in tiny Cinzel on it. Tap the letter or the demon: the seal
cracks, the letter unfolds into a short parchment ribbon showing the reply for 3 s, then folds back.
Provide the letter (closed and open) as a transparent PNG.
```

### 06 · Runes rise from the circle (no new art)
```text
Direction: the demon's summoning floor circle is the element. While eligible, faint runes rise from
the circle like sparks and the circle's inner ring breathes. The invite sits on a slim parchment
ribbon lying on the floor in front of the circle. Tap the demon or the circle: the runes swirl up
around him, and the ribbon turns to show the reply for 3 s. Everything stays below the demon's chest.
```

### 07 · A note pinned to the frame (one shared asset)
```text
Direction: one shared asset: a torn parchment scrap pinned to the stage's top-right corner by a small
gilded dagger, fluttering slightly, with the invite handwritten in ink. A faint accent glow on the
dagger. Tap the note or the demon: the scrap flips over and the reply is written on the back, ink
appearing word by word, for 3 s. Show it does not cover the demon's head.
```

### 08 · A tag on a chain (no new art)
```text
Direction: a small gilded plaque, matching the screen's "GRIMOIRE" header plaque, hangs on a thin
chain from the demon's raised hand or staff tip and swings gently as he floats. It reads "PSST…" in
Cinzel. Tap the plaque or the demon: the plaque spins once and widens into a ribbon with the reply for
3 s. Design for both a demon holding a staff (Asmodeus) and one riding a beast (Astaroth).
```

### 09 · He knocks from inside the card (demon only, no new art)
```text
Direction: the stage reads as a window. Every few seconds the demon knocks on it from inside: a small
ripple of accent light spreads from a point near his hand, with a tiny "knock knock" in Cinzel beside
it and a micro-shake of the stage. Tap the demon: he presses his face to the glass (scale toward the
viewer) and the reply appears in the quote bubble for 3 s. Keep the shake under 2 dp and describe the
still version.
```

### 10 · His shadow beckons (demon only, no new art)
```text
Direction: the demon's shadow, cast on the stage wall behind him, comes alive: it curls a long
beckoning finger toward the viewer while the demon himself stays innocent. A subtle accent rim light
outlines the shadow. A small cue under the floor circle reads "PSST… HIS SHADOW WANTS A WORD". Tap the
demon or the shadow: the shadow leans out and the reply appears in a smoky speech curl coming from the
shadow's mouth for 3 s.
```

## What we judge the results on
1. Belongs to the game (gilded, parchment, ink, glow), not to a chat app.
2. Reads as the demon talking, and invites a tap on him.
3. Works on both demons with no per-demon art.
4. Text legible at 200 % font and in Spanish; face and quote never covered.
5. Cost to build: code-drawn and existing art beat a new asset; one gesture beats two.
