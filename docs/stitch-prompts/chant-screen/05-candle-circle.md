# Stitch prompt 05/10: chant screen, "Candle circle"

Paste the block below into the Summon Demons Safely Stitch project, in a new screen request.
Attach **only** the Astaroth sigil image (and optionally the Astaroth House stage background).
Do **not** attach screenshots of the current screen: they pull every proposal back toward it.

---

Design **one** proposal for the **chant screen** of the mobile game "Summon Demons Safely", using the concept **"Candle circle"**:

> Four candles (one per line) stand around a ritual circle; lighting them is the progress; the chant appears on an altar stone in the middle.

Deliver it as **six separate screens**, one per state (A–F below), each a full Android phone screen in **landscape, 800 × 360 dp**, exactly as the player would see it. **Do not** produce a spec sheet, a comparison board, annotations, headers, legends, scores tables or design notes: only the six screens.

## The game
The player summons a cute chibi demon in two steps, always in this order: **(1) turn the phone to face the demon's direction**, then **(2) read the demon's four Latin lines aloud**, one at a time. The phone listens and **seals a line when it hears it**. Four sealed lines summon the demon. Tone: dark fantasy with comedy — ominous, never punishing. A line that isn't heard yet is "not yet", **never a failure**.

## Reading conditions
The phone is held **at mouth distance (~30 cm)** while the player reads aloud, often with glasses. The current line must be the largest thing on screen (Cinzel). A whole line must fit without scrolling. The microphone is at the bottom edge; nothing should invite covering it.

## What the screen may show — and nothing else
- The demon's name: **ASTAROTH**.
- The direction: **"FACE NORTH"**, whether the phone faces it (✓), and while not facing, **which way to turn** (left or right) and that the player is getting closer.
- The demon's sigil (the attached Astaroth sigil): it may be resized, moved or framed, but not redrawn.
- The current Latin line, as words; its syllables light up one by one in the demon's accent (magenta #E040FB) while the player chants; words the phone has heard get a clear "heard" mark.
- Progress through the four lines **I–IV**: pending, current, sealed (filled), cracked.
- From line II on, a **focus timer** (no numbers: a ring, bar or fill). Line I has no timer.
- After a check, a **score from 0 to 100 %** for 2–3 s; on a miss also, small: **heard: "…"** (what the phone understood).
- A subtle **listening/checking** indicator while a check runs (about half a second).
- A small round **?** button (rules), **top-left**, and an **✕** (leave the ritual), **alone in the top-right corner**.
- Short cues of **at most three words** (e.g. "FACE NORTH", "PAUSED", "AGAIN", "SEALED").

**Do not add** anything else: no points, XP, grades, ranks, currencies, combo counters, frequencies (Hz), phonemes, decibels, tolerances or degree read-outs beyond "FACE NORTH", microphone status, "distance OK" labels, timers in seconds, or any sentence longer than three words. No red "FAIL", "ERROR", "DISSONANCE" or penalty language anywhere.

## The six states (use exactly this content)
- **A. Turning** — The phone is 70° away from the demon; the player must turn **right**. The chant area is **visibly disabled**: line I, **"TUI GRATIA LOVIS"**, is previewed but clearly inactive. Lines I–IV all pending. No timer.
- **B. Locking** — The phone now faces NORTH; the lock fills up (0.6 s). This is the moment the chant area **comes alive**: show it mid-transition from disabled to enabled. Line I still shown.
- **C. Chanting** — Line I is sealed. Line II, **"GRATIA SIT CURA."**, is current: **GRA·TI·A (3 of 6 syllables lit)**; the words **GRATIA** carry the "heard" mark; a check is running (listening indicator on). Timer about 70 % full.
- **D. Not yet** — Line II was just checked: score **25 %**, with small **heard: "gracia tu pi"**. The words of line II are **clear again** (nothing lit) so the player simply recites it again; make "say it again" obvious without a sentence. Timer about 55 %.
- **E. Sealed** — Line II sealed with score **82 %**; its progress mark fills; line III, **"HIC EST ME DIGNUS LEGI"**, is appearing. Timer full again.
- **F. Paused** — After the lock, mid-line II (**GRA·TI (2 of 6 syllables lit)**), the phone turned 30° away; the chant is **paused** (words still visible) and the screen points the way back (**turn left**). Timer draining.

## Layout rules (mandatory)
1. **Each fact appears in exactly one place.** Direction (face / turn which way), line progress (I–IV), heard words, score, paused, and the timer are each shown **once**. No second copy in another panel, footer, header or pill. In particular: no "LINE II OF IV" text if the I–IV marks exist; no footer or header status lines ("MICROPHONE ACTIVE", "LISTENING…", "STEP 1…", "HOLD DIRECTION STABLE"); heard words carry **one** mark, not a colour plus a badge plus an underline.
2. **Buttons:** **?** sits **top-left**; **✕** sits **alone in the top-right corner**, with nothing else in either corner (no pills, scores or bars there). Nothing may overlap the buttons.
3. **Feedback appears where the eyes are:** the score and the "heard" text appear **next to the chant line** (just above or below it), never in a corner.
4. **The sigil itself shows the direction and the proximity.** Before the lock, the sigil (or a ring, light or pointer bound to it) must point toward the way to turn, and must visibly grow stronger as the phone gets closer to the target (brighter, fuller, steadier). A tiny arrow on the ring is not enough. This replaces any separate direction text while turning except "FACE NORTH" shown once.

## Problems this concept must solve (in its own way)
1. Order of steps: chant area disabled until the lock; enabling it is one clear moment.
2. Wrong direction: a strong, wordless cue on the sigil for "not facing", "which way" and "getting closer" (layout rule 4); colour never the only signal.
3. Performance: the 0–100 % score per check, and the "heard" text on a miss.
4. Retry: after a miss the words clear and the player recites again — no buttons.
5. Paused after the lock: obvious, with the way back, without hiding the words.
6. Listening: a subtle indicator while a check runs.

## Visual language
Dark tactical RPG with cute chibi demons. Tokens: surface #0B1326, on-surface #DAE2FD, outline #A08E7A, gold #FFC174, magenta #FCB4FF, soft error #FFB4AB (only for a cracked seal), green #4ADE80 (facing, heard, sealed). Demon accent: **magenta #E040FB**. Fonts: **Cinzel** for the line, the name and numerals; **Inter Bold** upper case for short cues. Every state must differ by shape, icon, word or motion as well as colour. Every animation must have a still equivalent.
