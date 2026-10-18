# Stitch prompt: ritual status block (10 alternatives)

Paste the block below into the Summon Demons Safely Stitch project, so Stitch reuses its
design system (DESIGN.md tokens). Attach a screenshot of the chant screen prototype
(v11, https://claude.ai/artifact/9rCV9zSJdqY5mJscEn5EiQ) as the "current" reference.

---

Design 10 distinct alternatives for one small component of the mobile game "Summon Demons Safely": the **ritual status block** on the chant screen. Keep everything else on the screen unchanged.

**Context**
- Android phone in **landscape**, held at mouth distance while the player reads a Latin chant aloud. It must be readable at a glance from ~30 cm, including by players with glasses.
- Screen layout: a left panel (25 % of the width, about 170–220 dp wide) and a right panel (75 %) showing the chant text in large Cinzel. The left panel is a translucent tonal slab with, from top to bottom: the demon's name, the demon's sigil (a large circle whose outer ring shows direction and a focus meter), then **the status block you are designing**.
- Dark tactical RPG theme. Use only these tokens: surface #0B1326, surface-container #171F33, surface-container-high #222A3D, surface-container-highest #2D3449, on-surface #DAE2FD, on-surface-variant #D8C3AD, outline #A08E7A, outline-variant #534434, primary #FFC174, ritual-safe green #4ADE80, error red #FF6B5E. The demon's accent is gold #FFD54F (Asmodeus) or magenta #E040FB (Astaroth): show both.
- Fonts: Cinzel (display, Roman numerals) and Inter (labels). Shapes: 12/16/24 dp rounded corners.

**What the block must communicate**
The ritual has two steps, and step 2 only counts while step 1 holds:
1. **Face the direction.** The text is "FACE EAST" (or NORTH, SOUTH, WEST, depending on the demon). States: *turning* (an arrow ← or → shows which way to turn), *aligned* (✓, green), *drifting* (red, blinking, arrow back).
2. **Chant the four lines.** The chant has 4 lines, I–IV. Each line is a seal that lights when the line is completed. Long lines are judged in 2–3 parts ("chunks"), so the current seal shows partial progress by part. States per seal: *pending*, *current* (with its part progress), *sealed*, *cracked* (a sealed line breaks and reopens: red, brief animation), and *all sealed*.

**Hard constraints**
- **At most two rows** inside the block: one row per step. No separate heading row for a step.
- **The same typographic treatment for both step labels** (same family, weight, case and size, e.g. Inter ExtraBold upper case, letter-spacing 0.05 em). Roman numerals may use Cinzel.
- The two steps must read as **one ordered task** (step 1, then step 2), and it must be clear that step 2 pauses when step 1 is lost.
- Colour is never the only signal: every state also has an icon, a word or a shape change.
- Fits the panel width (≥ 170 dp) without wrapping the direction text; minimum touchless legibility at 30 cm: labels ≥ 16 sp, seals ≥ 28 dp.
- No new progress numbers like "II of IV" elsewhere; this block is the only status display besides the sigil.

**Deliver**
For each of the 10 alternatives, render the left panel in these 4 states, side by side:
(a) turning to face, chant not started; (b) aligned, chanting line II, part 2 of 3; (c) drifting during line III; (d) all four lines sealed.
Label each alternative with a short name and one sentence on the idea (e.g. stepper, segmented bar, compass-plus-seals, single pill with two halves, checklist, ring around the sigil…). Make the alternatives genuinely different in structure, not colour variations of one layout. Show at least one alternative in the Astaroth magenta accent.
