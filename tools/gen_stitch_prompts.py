"""Generates the ten single-proposal Stitch prompts for the chant screen (docs/stitch-prompts/chant-screen/).

Run: python3 -I tools/gen_stitch_prompts.py
"""
import os, sys
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docs", "stitch-prompts", "chant-screen")
os.makedirs(out, exist_ok=True)

DEMONS = {
 "asmodeus": dict(name="ASMODEUS", accent="gold #FFD54F", dir="EAST", sigil="the attached Asmodeus sigil",
   l1="DAEMONIUM RESIDENTE,", l2="OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.",
   l3="DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,",
   c_lit="OS·TEN·DE MI·HI FA·CI·EM TU (9 of 18 syllables lit)", c_heard="OSTENDE and MIHI",
   d_heard="ostende bihi", f_lit="OS·TEN·DE MI·HI (5 of 18 syllables lit)", turnA="right", turnF="left"),
 "astaroth": dict(name="ASTAROTH", accent="magenta #E040FB", dir="NORTH", sigil="the attached Astaroth sigil",
   l1="TUI GRATIA LOVIS", l2="GRATIA SIT CURA.", l3="HIC EST ME DIGNUS LEGI",
   c_lit="GRA·TI·A (3 of 6 syllables lit)", c_heard="GRATIA",
   d_heard="gracia tu pi", f_lit="GRA·TI (2 of 6 syllables lit)", turnA="right", turnF="left"),
}
CONCEPTS = [
 ("01-split-stage", "Split stage, refined", "asmodeus", "A left status column and a right chant panel (as today), rethought so the six problems are solved; the most conservative proposal."),
 ("02-sigil-centred", "Sigil-centred", "astaroth", "The sigil sits in the centre and is the main instrument: the chant line sits beneath or around it, and the direction is shown by the sigil itself rotating or tilting toward the demon."),
 ("03-compass-first", "Compass first", "asmodeus", "Before the lock, the turning step owns the whole screen (a large compass or altar); at the lock it transforms into the chant view, so the order of steps is impossible to miss."),
 ("04-teleprompter", "Teleprompter", "asmodeus", "The line runs across one large band like karaoke lyrics, as big as possible; everything else is a slim HUD around the band."),
 ("05-candle-circle", "Candle circle", "astaroth", "Four candles (one per line) stand around a ritual circle; lighting them is the progress; the chant appears on an altar stone in the middle."),
 ("06-grimoire-pages", "Grimoire pages", "asmodeus", "An open book: the left page holds direction and progress, the right page holds the line; sealing a line turns the page."),
 ("07-card-stack", "Card stack", "asmodeus", "Each line is a card; the current card is in front, sealed cards stack aside showing their scores; facing the demon is the first card, which must be completed before the chant cards become playable."),
 ("08-stage-hud", "Minimal HUD over the stage", "astaroth", "Almost no chrome over the demon's stage art: direction and progress are edge glows and corner glyphs; the words float over the scene."),
 ("09-path", "Path to the demon", "asmodeus", "A short path with four waypoints (one per line) leads to the demon; a marker moves along it as lines seal; turning to face the demon is the first waypoint."),
 ("10-wild-card", "Wild card", "astaroth", "A layout concept different from all of these: split stage, sigil-centred, compass-first, teleprompter, candle circle, grimoire pages, card stack, minimal HUD, path to the demon. Name it."),
]

TEMPLATE = """# Stitch prompt {num}/10: chant screen, "{title}"

Paste the block below into the Summon Demons Safely Stitch project, in a new screen request.
Attach **only** the {demonlow} sigil image (and optionally the {demonlow} House stage background).
Do **not** attach screenshots of the current screen: they pull every proposal back toward it.

---

Design **one** proposal for the **chant screen** of the mobile game "Summon Demons Safely", using the concept **"{title}"**:

> {concept}

Deliver it as **six separate screens**, one per state (A–F below), each a full Android phone screen in **landscape, 800 × 360 dp**, exactly as the player would see it. **Do not** produce a spec sheet, a comparison board, annotations, headers, legends, scores tables or design notes: only the six screens.

## The game
The player summons a cute chibi demon in two steps, always in this order: **(1) turn the phone to face the demon's direction**, then **(2) read the demon's four Latin lines aloud**, one at a time. The phone listens and **seals a line when it hears it**. Four sealed lines summon the demon. Tone: dark fantasy with comedy — ominous, never punishing. A line that isn't heard yet is "not yet", **never a failure**.

## Reading conditions
The phone is held **at mouth distance (~30 cm)** while the player reads aloud, often with glasses. The current line must be the largest thing on screen (Cinzel). A whole line must fit without scrolling. The microphone is at the bottom edge; nothing should invite covering it.

## What the screen may show — and nothing else
- The demon's name: **{name}**.
- The direction: **"FACE {dir}"**, whether the phone faces it (✓), and while not facing, **which way to turn** (left or right) and that the player is getting closer.
- The demon's sigil ({sigil}): it may be resized, moved or framed, but not redrawn.
- The current Latin line, as words; its syllables light up one by one in the demon's accent ({accent}) while the player chants; words the phone has heard get a clear "heard" mark.
- Progress through the four lines **I–IV**: pending, current, sealed (filled), cracked.
- From line II on, a **focus timer** (no numbers: a ring, bar or fill). Line I has no timer.
- After a check, a **score from 0 to 100 %** for 2–3 s; on a miss also, small: **heard: "…"** (what the phone understood).
- A subtle **listening/checking** indicator while a check runs (about half a second).
- A small round **?** button (rules), **top-left**, and an **✕** (leave the ritual), **alone in the top-right corner**.
- Short cues of **at most three words** (e.g. "FACE {dir}", "PAUSED", "AGAIN", "SEALED").

**Do not add** anything else: no points, XP, grades, ranks, currencies, combo counters, frequencies (Hz), phonemes, decibels, tolerances or degree read-outs beyond "FACE {dir}", microphone status, "distance OK" labels, timers in seconds, or any sentence longer than three words. No red "FAIL", "ERROR", "DISSONANCE" or penalty language anywhere.

## The six states (use exactly this content)
- **A. Turning** — The phone is 70° away from the demon; the player must turn **{turnA}**. The chant area is **visibly disabled**: line I, **"{l1}"**, is previewed but clearly inactive. Lines I–IV all pending. No timer.
- **B. Locking** — The phone now faces {dir}; the lock fills up (0.6 s). This is the moment the chant area **comes alive**: show it mid-transition from disabled to enabled. Line I still shown.
- **C. Chanting** — Line I is sealed. Line II, **"{l2}"**, is current: **{c_lit}**; the words **{c_heard}** carry the "heard" mark; a check is running (listening indicator on). Timer about 70 % full.
- **D. Not yet** — Line II was just checked: score **25 %**, with small **heard: "{d_heard}"**. The words of line II are **clear again** (nothing lit) so the player simply recites it again; make "say it again" obvious without a sentence. Timer about 55 %.
- **E. Sealed** — Line II sealed with score **82 %**; its progress mark fills; line III, **"{l3}"**, is appearing. Timer full again.
- **F. Paused** — After the lock, mid-line II (**{f_lit}**), the phone turned 30° away; the chant is **paused** (words still visible) and the screen points the way back (**turn {turnF}**). Timer draining.

## Layout rules (mandatory)
1. **Each fact appears in exactly one place.** Direction (face / turn which way), line progress (I–IV), heard words, score, paused, and the timer are each shown **once**. No second copy in another panel, footer, header or pill. In particular: no "LINE II OF IV" text if the I–IV marks exist; no footer or header status lines ("MICROPHONE ACTIVE", "LISTENING…", "STEP 1…", "HOLD DIRECTION STABLE"); heard words carry **one** mark, not a colour plus a badge plus an underline.
2. **Buttons:** **?** sits **top-left**; **✕** sits **alone in the top-right corner**, with nothing else in either corner (no pills, scores or bars there). Nothing may overlap the buttons.
3. **Feedback appears where the eyes are:** the score and the "heard" text appear **next to the chant line** (just above or below it), never in a corner.
4. **The sigil itself shows the direction and the proximity.** Before the lock, the sigil (or a ring, light or pointer bound to it) must point toward the way to turn, and must visibly grow stronger as the phone gets closer to the target (brighter, fuller, steadier). A tiny arrow on the ring is not enough. This replaces any separate direction text while turning except "FACE {dir}" shown once.

## Problems this concept must solve (in its own way)
1. Order of steps: chant area disabled until the lock; enabling it is one clear moment.
2. Wrong direction: a strong, wordless cue on the sigil for "not facing", "which way" and "getting closer" (layout rule 4); colour never the only signal.
3. Performance: the 0–100 % score per check, and the "heard" text on a miss.
4. Retry: after a miss the words clear and the player recites again — no buttons.
5. Paused after the lock: obvious, with the way back, without hiding the words.
6. Listening: a subtle indicator while a check runs.

## Visual language
Dark tactical RPG with cute chibi demons. Tokens: surface #0B1326, on-surface #DAE2FD, outline #A08E7A, gold #FFC174, magenta #FCB4FF, soft error #FFB4AB (only for a cracked seal), green #4ADE80 (facing, heard, sealed). Demon accent: **{accent}**. Fonts: **Cinzel** for the line, the name and numerals; **Inter Bold** upper case for short cues. Every state must differ by shape, icon, word or motion as well as colour. Every animation must have a still equivalent.
"""

for i, (slug, title, demon, concept) in enumerate(CONCEPTS, 1):
    d = DEMONS[demon]
    text = TEMPLATE.format(num=f"{i:02d}", title=title, concept=concept, demonlow=demon.capitalize(), **d)
    with open(os.path.join(out, f"{slug}.md"), "w", encoding="utf-8") as f:
        f.write(text)
print("written", len(CONCEPTS))
