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
