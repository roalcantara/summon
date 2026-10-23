# 0036 — "Psst": a fake-door test for new demon content

- Date: 2026-10-23
- Status: Accepted
- Spec: docs/superpowers/specs/2026-10-23-psst-teaser-design.md

## Context
ADR-0034's v1.1 depends on a new reward from bound demons (lore over days, daily lines). Before
building it, v1 measures whether players want more from them.

## Decision
From the day after a summon, once his lore is read, a bound demon shows a balloon "Psst… come
closer."; tapping it (or him) answers "Heh heh heh… Come back soon… if you dare, mortal." with a
haptic (and a laugh if a CC0 clip is added). `teaser_shown` and `teaser_tap` measure interest; the
playbook's rule decides v1.1's lever.

## Consequences
- Tapping a bound demon now does something (PRODUCT.md updated).
- `DemonRecord.boundDay` is stored from now on; old saves load (null counts as earlier).
- When v1.1 ships real content, the balloon becomes its entry point.

## Amendment 2026-10-23: three taps that burst
The single-tap balloon read as a chat bubble pasted onto the art, and one tap gave no build-up.
Stitch variations did not help; a playable prototype ("Psst, three taps") settled it:
- A parchment comic bubble with accent action ticks and three seal pips floats with the demon.
- Three taps (bubble or demon), each with a stronger shake, flash and haptic; the third bursts like
  a summon (flash, sunburst, spiky reply bubble). The reply floats 7 s, below his face, and fades out slowly.
- After the reply fades the invite comes back and can be played again at will; the day of the last
  burst is kept (`DemonRecord.teaserOpenedDay`) so replays are told apart from first opens.
- Events: `teaser_tap` carries `step` (1–3) instead of `repeat`; `teaser_open` marks the burst, with
  `repeat` for later bursts the same day. The
  playbook adds the open rate and return openers.
- Risk: a chest-like build-up promises loot and pays with a joke; watch whether openers return the
  next day.
