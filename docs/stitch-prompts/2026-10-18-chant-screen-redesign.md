# Stitch prompt: chant screen redesign (10 genuinely different proposals)

> Superseded by `chant-screen/` (one proposal per request, exact content per state). Kept for history.

Paste the block below into the Summon Demons Safely Stitch project, so Stitch reuses its
design system. Attach two screenshots as the **current** reference: the chant screen while
turning (not facing the demon) and while chanting (line locked, words lighting).

Why the prompt is built this way: the previous request produced ten near-identical variants.
This one assigns each proposal a different layout concept and requires a different mechanism
for each UX need, so the ten can't converge.

---

Design **10 genuinely different proposals** for the **chant screen** of the mobile game "Summon Demons Safely". Every proposal must solve **all** the UX problems listed below, but each must solve them **in its own way**. Proposals that look like small variations of each other (same layout with different colours, icons or borders) do not count. Before drawing, write one sentence per proposal on what makes it different from the other nine.

## The game, in one paragraph
The player summons a cute chibi demon by doing two things in order: **(1) turn the phone to face the demon's direction** (North, East, … — each demon has its own), and **(2) read the demon's four Latin chant lines aloud**, one line at a time. The phone listens on the device and **seals a line when it hears it**. Four sealed lines summon the demon (a celebration plays). The tone is dark fantasy with comedy: ominous, but never punishing.

## Device and reading conditions
- Android phone in **landscape** (about 800 × 360 dp, 20:9), held **at mouth distance (~30 cm)** while reading aloud, often by players with glasses. Everything that must be read while chanting must be legible at that distance.
- The longest line has 9 words / 18 syllables (e.g. "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,"); the shortest has 2 words. A whole line must fit on screen without scrolling.
- The microphone is at the phone's bottom edge: nothing should invite the player to cover it.

## What the screen must show (all of it, in every proposal)
- **The current chant line**, very large (Cinzel), as words. While the player chants, its syllables light up one by one in the demon's accent (karaoke). Words the phone has heard get a clear "heard" mark.
- **The direction** to face (e.g. "FACE NORTH") and whether the phone is facing it; while not facing, **which way to turn** (left or right).
- **The demon's sigil** (existing line art; it fills in, "inks", as lines are sealed). It can be resized or repositioned, but not redrawn.
- **Progress through the four lines** (I–IV): pending, current, sealed, cracked.
- **The focus timer** for lines II–IV: 60 s per line, refilled when the line seals; when it runs out, the last sealed line cracks (breaks) and must be chanted again. Line I has no timer.
- **The demon's name.**
- Small round "?" (rules) and, in debug builds, "D" (debug panel) buttons, and an ✕ to leave the ritual.

## The UX problems every proposal must solve
1. **Order of steps is not obvious.** Testers didn't understand that they must face the direction first and chant second. Required: **before the direction is locked, the chant area is visibly disabled** (the words may be previewed, but clearly inactive), and **when the lock happens, the chant area is enabled** in one clear, satisfying moment. Only what can be done now should look actionable.
2. **"Wrong direction" is not clear enough.** Required: a stronger, wordless cue that the phone is not facing the demon **and which way to turn**, that also conveys getting closer. Colour must never be the only signal.
3. **Players don't know how well they did.** Required: after each check of a line, show a **score for that line (0–100 %)** for 2–3 s and then dismiss it; 40 % or more seals the line. On a miss, also offer (small, optional) **what the phone heard**, e.g. *heard: "gratia sit kura"*, so the player can learn instead of trying blindly. A miss is never shown as an error or punishment: it means "not yet".
4. **Retrying must feel natural.** After a miss, the line's words clear and the player simply recites it again (no buttons). The design should make "say it again" obvious without long text.
5. **Losing the direction after the lock** (turning away while chanting) pauses the chant: the screen must show it is paused and point the way back, without hiding the words.
6. **Listening and checking.** While the phone is checking what was said (about half a second), show a subtle "listening / checking" state.

## The screen's states (draw every one for each proposal)
For **each proposal** draw these six frames side by side, same proposal, same layout:
- **A. Turning:** not facing the demon (e.g. 70° off, turn right). Chant area disabled; words previewed dim.
- **B. Locking:** facing, the 0.6 s lock filling up.
- **C. Chanting:** line II, about half of its syllables lit, two words already "heard", a check running.
- **D. Miss:** line II just checked, score **25 %**, "heard: …" shown, words cleared for another try.
- **E. Sealed:** line II sealed, score **82 %**, its progress mark filling, the next line appearing.
- **F. Paused:** after the lock, turned 30° away mid-line: paused, pointing back.

## Forced diversity: each proposal has its own concept
Assign the concepts below one per proposal, in order. Do not reuse a concept.
1. **Split stage (refined current):** left status column + right chant panel, but rethought for the six problems.
2. **Full-screen compass first:** the turning step owns the whole screen (big compass/altar); on lock it transforms into the chant view.
3. **Sigil-centred:** the sigil sits in the centre; the chant line wraps around or beneath it; direction is shown by the sigil itself rotating or tilting.
4. **Ritual circle / altar:** four candles or runes around a circle stand for lines I–IV; lighting them is the progress; the chant appears on an altar stone.
5. **Teleprompter:** the chant scrolls on a large single-line band across the screen like karaoke lyrics; everything else is a slim HUD around it.
6. **Grimoire pages:** an open book; the left page is direction/progress, the right page is the line; sealing turns the page.
7. **Card stack:** each line is a card; the current card is in front, sealed cards stack aside with their scores; turning the phone is a separate card that must be "played" first.
8. **Minimal HUD over the demon's stage art:** almost no chrome; direction and progress are edge glows and corner glyphs; the words float over the scene.
9. **Path / map:** a short path with four waypoints (one per line) leading to the demon; the player's position moves along it; the compass is the first waypoint.
10. **Your wild card:** a concept not covered by 1–9 that still meets every requirement.

**Different mechanisms, not just layouts.** Across the ten proposals, vary the mechanism used for each problem. For each problem, use at least **five different mechanisms** across the ten, for example:
- Gating (problem 1): dim + lock icon; blurred/fogged text that clears; text behind a closed door/curtain; greyed card that flips; text hidden until lock; chains that break; a progress gate that opens.
- Direction cue (problem 2): rotating arrow; pulsing chevrons on the screen edge pointing the way; a compass needle; a beam/cone of light; the sigil turning; a hot/cold glow; a horizon line with the target marker.
- Score (problem 3): percentage badge; 1–3 stars or sigil marks; a gauge/meter; a stamp or wax seal with the number; a ribbon; a fill level in the line's progress mark.
- Retry (problem 4): words fading and re-appearing; a soft "again" glyph; the karaoke cursor rewinding; the card flipping back; a short shake + reset.

## Visual language (keep)
- Dark tactical RPG with cute chibi demons. Use the project's design tokens: surface `#0B1326`, on-surface `#DAE2FD`, outline `#A08E7A`, primary (gold) `#FFC174`, tertiary (magenta) `#FCB4FF`, error `#FFB4AB`, ritual-safe green `#4ADE80`. Demon accent: **gold `#FFD54F` for Asmodeus, magenta `#E040FB` for Astaroth**; show at least three proposals with Astaroth.
- Fonts: **Cinzel** for the chant line, demon name and numerals; **Inter Bold** upper case for labels and short cues.
- Colour is never the only signal: every state also changes an icon, a shape, a word or motion.
- Keep text short: no instructions longer than three words on the chant screen (rules live on the separate "How to chant" card).
- Respect reduced motion: every animation must have a still equivalent.

## Deliver
For each of the 10 proposals:
1. A name and two sentences: the concept, and what makes it different from the other nine.
2. The six frames A–F side by side.
3. A short list mapping each UX problem (1–6) to the mechanism used.

## Notes for implementation (keep the design within these)
The app already computes, per frame: the words of the current line with how many syllables are lit and whether each word is pending / current / heard; whether the phone faces the demon; which way to turn; how far off it is (degrees); the lock progress (0–1); whether the ritual is paused (wavering); the four lines' states (pending / current / sealed / cracked); whether a check is running; the timer fraction (0–1); and, after each check, the score (0–100 %) and the text the phone heard. A design should only need these values.
