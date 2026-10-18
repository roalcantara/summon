# 0019 — Chant screen

- Date: 2026-10-18
- Status: Accepted. Amends ADR-0010 (clock length, verdict hold), ADR-0014 (two text
  roles), ADR-0018 (word hints, verdict shown until the next syllable) and the
  "Portrait only" rule in PRODUCT.md. Amended by ADR-0020: the chant screen covers the
  whole Ritual and its sigil carries the direction marker and the lock fill. Amended by
  ADR-0021: the intro card shows before every ritual and replaces the turn-sideways hint,
  its ✕ starts the chant, "?" moves above the name, and the debug controls move to a
  floating panel. Amended by ADR-0023 (no seconds label) and ADR-0024 (status column, reduced verdict line).

## Context
Device testing of the chant check (ADR-0018) showed two problems. The scoff appeared in
the 12 sp cue chip for 1.5 s, while the player's eyes were on the tablet, so nobody
could read it or tell what had happened. And the chant itself (Cinzel 15 sp) is hard to
read with the phone held at mouth distance, especially for players with glasses: the
issue is size at that distance, not the font.

A web prototype was iterated with the product owner on a phone, in landscape, at mouth
distance (claude.ai artifact "Chant Screen Prototype", versions 1–9). This ADR records
the approved result.

## Decision
- **A dedicated chant screen.** The Ritual keeps today's portrait layout while seeking
  and locking (the big compass ring is needed to find the direction). From the lock until
  the Ritual closes (chanting, wavering, climax), the chant screen replaces it.
- **Landscape.** The Ritual requests sensor-landscape from the lock and restores portrait
  when it closes. The activity handles the rotation itself (`configChanges`) so the mic,
  sensors and ritual don't restart. On screens where Android ignores the request (large
  screens, Android 16+), or until the phone turns, a portrait fallback puts the left panel
  in a top band. On entering, a short overlay asks to turn the phone sideways and to keep
  the bottom edge (microphone) clear; it goes after 2.5 s or the first syllable.
- **Left panel (25 %), a soft tonal slab** (surface-container-low, large radius, no
  divider line): the demon's name (`name` role, accent), then the sigil at full panel
  width, then one direction line.
  - The sigil inks phrase by phrase (ADR-0009) and is the only progress display.
  - Its outer ring is the direction (ritual-safe green when facing the demon, error red
    when not) and the focus clock (the arc drains; seconds below it, "Paused" while held).
  - Direction line: "FACE EAST ✓" in green; off course "← FACE EAST" / "→ FACE EAST" in
    red, blinking, the arrow giving the way to turn. No box.
- **Right panel (75 %): the phrase being chanted,** auto-sized to fill the space
  (`incantationLarge`), syllables split by middle dots, lit syllables in the accent, the
  current word 15 % larger. A line under each word:
  - pending: thin outline-variant; current: on-surface-variant;
  - heard: ritual-safe green; doubtful: primary, dimmer (a hint only);
  - rejected: error, wavy, with a ✕ badge on the word.
- **One verdict line under the words** (`ritualCue`, no box): "CHANT ALOUD · I of IV"
  (grey, with a five-bar voice meter driven by the mic level), "PAUSED · II of IV" while
  off course (flat grey meter), "SEALED ✓ · I of IV" (green), "CHANT AGAIN ✕ · II of IV"
  and "SEAL CRACKED ✕ · I of IV" (red). The numeral is in Cinzel and the accent.
- **Colour is never alone:** every state also has an icon (✓ ✕ ← →), words, and a line
  shape (solid or wavy).
- **Word hints.** When all syllables of a word have closed, the word is marked heard or
  doubtful from the average distance of its vowels to the expected ones (at the
  vocal-tract scale that best fits the phrase so far; ≥ 0.6 is doubtful). Too short to
  judge alone, so it never decides anything; the phrase verdict (ADR-0018) does.
- **Verdicts stay until the next syllable.** Sealed, rejected (with that attempt's word
  marks) and cracked are kept in the ritual state until a syllable lights. While a verdict
  is fresh the focus clock waits, at most 3 s, so reading it costs nothing; then it drains
  as before. A rejection still never refills the clock.
- **Longer clock:** 4 s + 0.75 s per syllable (was 3 s + 0.5 s), +25 % in court.
- **Text roles** (DESIGN.md): `ritualCue` (Inter Bold, upper case, 0.05 em; 18 sp for
  direction, 22 sp for the verdict) and `incantationLarge` (Cinzel Bold, line 1.3 em,
  0.02 em, auto-sized 18–96 sp). The app's other roles are sized for reading distance.
- **Backdrop.** The demon's House stage (`stage_sol` / `stage_venus`, the existing art
  used on the Grimoire) fills the screen behind both panels, darkened by a scrim that is
  lightest behind the left panel and strongest behind the words, so the phrase keeps its
  contrast. The left slab is translucent over it. No new art.
- **Intro card ("How to chant").** At the lock on the player's **first** ritual, a card
  comes down from the top over the chant screen; **Summon** sends it back up, revealing the
  screen (reduced motion: a fade). A "?" on the chant screen reopens it. While it is up the
  microphone is ignored (the heading still counts; phrase I has no clock). It mirrors the
  chant screen: on the left slab a singing demon; on the right a banner "HOW TO CHANT", four
  generic rules (title + one-line description, each with the chant screen's own cue), and
  the **SUMMON** button:
  - ✓ **DIRECTION**: Turn until the symbol turns green.
  - voice meter **CHANT**: Read each line aloud, following the glowing syllables.
  - green line + red wavy line **LINES**: Green: heard. Red: say it again.
  - I II III IV **SEAL**: Seal all four lines to summon the demon.
  Nothing on it names a demon or a direction. Seen once, it is remembered in progress
  (`chantIntroSeen`, reset by the debug "Reset all"); later rituals show the short
  turn-sideways hint instead. The singer, the banner and the SUMMON button are new Stitch
  art (DESIGN.md "Gaps"); until it lands the card uses the demon's avatar, `RibbonBanner`
  and `PrimaryActionButton`.
- **Removed from this screen:** the medallions, the focus bar, the cue chip, the screen
  title and the demon's scoff text. The chant tablet stays on the portrait screen while
  seeking and locking.
- **Unchanged:** effects, haptics and sounds; the debug controls (shown under the
  verdict in debug builds); the abandon flow (✕ at the top right and back).

## Consequences
- The heading code learns the screen rotation: the screen's top is device +Y in
  portrait, −X at 90°, −Y at 180° and +X at 270°. Upright readings (the phone's back) are
  unaffected.
- Progress gains `chantIntroSeen` (default false, so older saves show the card once).
- The climax overlay must fit landscape (the demon art fits both dimensions).
- PRODUCT.md: the Orientation row becomes "Portrait, except the chant screen (landscape)";
  the Chant, Focus clock, Ritual setup and Sigil rows describe the new screen.
- DESIGN.md gains the two text roles, the chant screen components and their states.
- The rejection cue row of the portrait cue chip (ADR-0018) can no longer show, since
  rejections only happen on the chant screen; removing it is a follow-up cleanup.
- The word-hint distance (0.6) and the verdict hold (3 s) are first guesses: calibrate
  them with the same tester clips as ADR-0018.
- Plan: `docs/superpowers/plans/2026-10-18-chant-screen.md`.
