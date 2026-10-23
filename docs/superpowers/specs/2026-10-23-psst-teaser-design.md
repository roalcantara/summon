# "Psst": a fake-door test for new demon content

- Date: 2026-10-23
- Status: Approved (brainstorming, visual companion `teaser-balloon.html`, option A)
- Depends on: the anonymous analytics (`docs/superpowers/plans/2026-10-23-analytics.md`) being implemented
- Decision record: ADR-0036 (written by the plan)

## Why
ADR-0034's v1.1 needs a **new reward** to make players return (today a bound demon offers nothing
new). Before building it, measure whether players want more from their demons. A balloon by the
demon's head invites a tap; the tap gets an in-character "not yet", and the taps are counted.

## Behaviour
- **Eligible:** the shown demon is bound, was summoned on an earlier day (`boundDay < today`; a
  record without `boundDay`, saved before this change, counts as earlier), has no unread lore
  (`!ProgressRules.dossierUnread`), and there is no celebration on screen.
- **Invitation:** "Psst… come closer." in a balloon by his head (top right of his art), with a tail
  toward him.
- **Tap** (the balloon, or the demon himself while eligible): the balloon shows the reply
  "Heh heh heh… Come back soon… if you dare, mortal." for 3 s, with a short haptic
  (`Haptics.chime`) and, if the clip exists, the laugh; then it returns to the invitation. Taps
  during the reply restart the 3 s; nothing else happens (no penalty, no navigation).
- **Look:** violet fill (`Palette`-based tokens), gold rim (primary), the shared attention glow
  (`attentionGlow`, steady with reduce motion) plus a faint green halo. Text: `name` role, fixed-art
  rule (`fontScaleCapped`, auto-size down, at most 3 lines). Drawn in code.
- **Accessibility:** a button labelled with the visible line; the reply is a polite live region.

## Measurement
- `teaser_shown` — `demon`, `days_since_summon` (buckets as `days_since_install`): once per demon
  per day per app process.
- `teaser_tap` — `demon`, `target` (balloon, demon), `repeat` (true if this demon's balloon was
  tapped earlier today in this process).
- Playbook additions (metric and rule): **teaser tap rate** = users with ≥ 1 `teaser_tap` / users
  with ≥ 1 `teaser_shown`, over the v1 window; **return tappers** = users tapping on ≥ 2 different
  days / tappers. Rule: ≥ 20 % → new demon content (lore over days, daily lines) is v1.1's lever;
  5–20 % → build it and test another hook; < 5 % → rethink v1.1.

## Model
`DemonRecord.boundDay: Long?` (null by default; set by `ProgressRules.commitSummon`); old saves
load unchanged (unknown/absent keys are fine, ADR-0013).

## The laugh (optional)
Only if `app/src/main/res/raw/demon_laugh.ogg` (CC0, ≤ 2 s, normalised quieter than `tada`) is
added by the operator: a `GameSounds.Cue.Laugh`. Without the file, nothing about the laugh ships.

## Strings
`psst_invite` and `psst_reply` in 5 languages, none longer than English.

## Out of scope
Real new content (v1.1), persisting tap history across processes, Stitch art for the balloon (a
DESIGN.md Gap if wanted later).
