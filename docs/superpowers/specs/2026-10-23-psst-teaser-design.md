# "Psst": a fake-door test for new demon content

- Date: 2026-10-23
- Status: Approved (brainstorming, visual companion `teaser-balloon.html`, option A); amended
  2026-10-23: three taps that burst (prototype "Psst, three taps", https://claude.ai/code/artifact/a960ad9b-7789-4f38-82af-843527dd624e)
- Depends on: the anonymous analytics (`docs/superpowers/plans/2026-10-23-analytics.md`) being implemented
- Decision record: ADR-0036 (written by the plan)

## Why
ADR-0034's v1.1 needs a **new reward** to make players return (today a bound demon offers nothing
new). Before building it, measure whether players want more from their demons. A balloon by the
demon's head invites a tap; the tap gets an in-character "not yet", and the taps are counted.

## Behaviour
- **Eligible:** the shown demon is bound, has no unread lore (`!ProgressRules.dossierUnread`), and
  there is no celebration on screen. From the summon day on (amended 2026-10-23: waiting a day hid
  it from testers, and release builds cannot skip ahead).
- **Invitation:** "Psst… come closer." in a comic speech bubble by his head (top right of his
  art), tail toward him, floating with him. Under the line, three empty seal pips. Action ticks in
  his accent colour breathe at two corners (the screen's one attention glow; steady with reduce
  motion).
- **Three taps** (the bubble or the demon himself, while eligible), each stronger than the last,
  like opening a chest:
  1. pip 1 lights; the demon squashes, the bubble shakes, a faint accent flash; `Haptics.tick`.
  2. pip 2 lights; a harder shake, a stronger flash, the ticks flicker fast; `Haptics.clunk`.
  3. pip 3 lights; a summon-style flash, the bubble vanishes, the sunburst turns behind him and a
     spiky burst bubble springs in with "Heh heh heh… Come back soon… if you dare, mortal.";
     `Haptics.success` and, if the clip exists, the laugh.
  The reply floats with him for 7 s between the stage arrows (his face stays clear), then it and
  the rays fade out slowly (1.2 s) and the invite comes back with empty pips: the player may play
  it again as often as they like. `DemonRecord.teaserOpenedDay` keeps the day of the last burst, so
  later bursts that day are reported as replays. Leaving before the third tap forgets the count.
  Nothing else happens (no penalty, no navigation).
- **Reduce motion:** no float, shake, squash, flash or ray spin; the pips light, the burst fades in,
  the haptics stay.
- **Look:** drawn in code. Bubble and burst: `parchmentLight` fill with a `parchmentDark`
  highlight, `surfaceContainerLowest` outline, text in `parchmentInk` (the intro ribbon's parchment); ticks and lit pips in the demon's
  accent. Text: `name` role, fixed-art rule (`fontScaleCapped`, auto-size down, at most 3 lines).
- **Accessibility:** the bubble is a button labelled with its line, its state "n/3"; the demon is a
  button while eligible; the reply is a polite live region.

## Measurement
- `teaser_shown` — `demon`, `days_since_summon` (buckets as `days_since_install`): once per demon
  per day per app process.
- `teaser_tap` — `demon`, `target` (balloon, demon), `step` (1, 2, 3).
- `teaser_open` — `demon`, `repeat` (true when this demon already burst today; persisted day).
- Playbook additions (metric and rule): **teaser tap rate** = users with ≥ 1 `teaser_tap` / users
  with ≥ 1 `teaser_shown`, over the v1 window; **open rate** = users with ≥ 1 `teaser_open` / tappers; **return
  openers** = users opening on ≥ 2 different days / openers. Rule: ≥ 20 % → new demon content (lore over days, daily lines) is v1.1's lever;
  5–20 % → build it and test another hook; < 5 % → rethink v1.1.

## Model
`DemonRecord.boundDay: Long?` (null by default; set by `ProgressRules.commitSummon`) and
`DemonRecord.teaserOpenedDay: Long?` (set by `ProgressRules.openTeaser`; marks replays);
old saves load unchanged (unknown/absent keys are fine, ADR-0013).

## The laugh (optional)
Only if `app/src/main/res/raw/demon_laugh.ogg` (CC0, ≤ 2 s, normalised quieter than `tada`) is
added by the operator: a `GameSounds.Cue.Laugh`. Without the file, nothing about the laugh ships.

## Strings
`psst_invite` and `psst_reply` in 5 languages, none longer than English; `psst_progress` ("%1$d/3",
untranslatable) for the bubble's state.

## Out of scope
Real new content (v1.1), persisting tap history across processes, Stitch art for the balloon (a
DESIGN.md Gap if wanted later).
