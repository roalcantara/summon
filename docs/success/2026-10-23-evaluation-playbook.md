# Success evaluation playbook

- Date: 2026-10-23
- Decisions: ADR-0034 (retention roadmap), ADR-0035 (analytics)
- Events: `docs/superpowers/specs/2026-10-23-analytics-design.md`
- Audience: whoever runs the evaluation (a person or an agent). Everything needed to run it is here.

## What is being tested

| Experiment | Hypothesis |
|---|---|
| **v1 launch** | The game works in the wild (ritual completes, stable), the listing converts, and some players return; but retention drops after day 2 because there is nothing new to do (ADR-0034). |
| **v1.1 retention** | A daily reason to return (Renew the pact, lore over days, bond counter) raises day-7 retention and daily rituals versus v1. |

## Data sources

| Source | Where | What |
|---|---|---|
| Play Console | Statistics; Grow users → Store performance / Store listing conversion; User acquisition → Retained installers; Quality → Ratings and reviews; Quality → Android vitals | installs, conversion, retained installers (1/7/15/30 days), ratings, crash and ANR rates |
| TelemetryDeck | the app's dashboard (Insights) | the 8 events: active users, retention by install cohort, ritual funnel, chant-check health |

Play Console menu names change; if a path is missing, search the Console for the report name.
Reports lag about 2 days; never read a day earlier than 2 days ago.

## Definitions

- **Cohort:** the users who installed in a given window (Play: by install date; TelemetryDeck: by
  the first `app_open` with `days_since_install = 0`).
- **Dn retention:** share of a cohort that opens the app on day n after install (TelemetryDeck
  `app_open`). Play's "retained installers" (still installed) is reported alongside, never mixed.
- **Active player-day:** a user with at least one `app_open` that day.
- **Ritual completion:** `ritual_end(outcome=completed)` / `ritual_start`.
- **Lock within 30 s:** `direction_locked` with seconds in 0-5, 5-10 or 10-30, over rituals that
  did not end in `intro` or an error phase.
- **Crack rate:** `line_result(cracked)` / all `line_result`.
- **Lenient share:** `device_check(mode=lenient)` / all `device_check`.
- **Rituals per active player-day:** `ritual_start` count / active player-days, for users on day 3+.
- **Teaser tap rate:** users with ≥ 1 `teaser_tap` / users with ≥ 1 `teaser_shown`, over the v1 window.
- **Return tappers:** users tapping on ≥ 2 different days (`repeat = true` or `teaser_tap` across separate days) / all tappers.

## Targets

| Area | Metric | Target | Alarm (act) |
|---|---|---|---|
| Acquisition | Store listing conversion | ≥ 25 % | < 15 % |
| Stability | User-perceived crash rate (Android vitals) | ≤ 0.5 % | > 1.09 % (Google's bad-behaviour threshold) |
| Stability | User-perceived ANR rate | ≤ 0.2 % | > 0.47 % |
| Satisfaction | Average rating (≥ 20 ratings) | ≥ 4.3 | < 3.8 |
| Ritual | Microphone granted (first answer) | ≥ 80 % | < 60 % |
| Ritual | Lock within 30 s | ≥ 85 % | < 70 % |
| Ritual | Completion | ≥ 60 % | < 50 % |
| Ritual | Crack rate | ≤ 15 % | > 25 % |
| Ritual | Abandoned in `chanting` or `wavering` | ≤ 40 % of abandons | > 50 % |
| Device | Lenient share | ≤ 20 % | > 35 % |
| Retention (v1) | D1 / D7 / D30 | ≥ 35 % / ≥ 12 % / ≥ 5 % | D1 < 25 %, D7 < 8 % |
| Interest | Teaser tap rate | ≥ 20 % | < 5 % |
| Engagement (v1.1) | Rituals per active player-day (day 3+) | ≥ 0.4 | < 0.2 |
| Retention (v1.1) | D7 vs v1 | + 5 points, significant (see Method) | no significant change |

## Periods

Let **L** be the v1 launch day and **R** the v1.1 release day (calendar days, local time of the
evaluator; use the same time zone throughout).

| When | Check | Output |
|---|---|---|
| L − 1 | Pre-flight (checklist A) | go / no-go for launch |
| L + 3 | Stability and funnel smoke check: vitals, mic grant, completion, lenient share | short note; act on any alarm |
| L + 9 | Week-1 read: conversion, ratings, funnel, D1 and D7 of the L..L+1 cohort | weekly report |
| L + 16 | Week-2 read: same, D7 of cohort L..L+7 | weekly report |
| L + 39 | **v1 baseline report:** D1/D7/D30 of cohort L..L+7 (D30 available at L+37, plus 2 days lag) | baseline report; decision on v1.1 timing |
| R + 23 | v1.1 early read: D7 of cohort R..R+14 vs v1 cohort L..L+14 | weekly report |
| R + 46 | **v1.1 report:** D7 and D30 of cohort R..R+14 vs L..L+14; rituals per active player-day | decision report |

**Minimum sample:** a cohort needs **≥ 300 installs** for retention comparisons and **≥ 200
rituals** for funnel rates. If a window doesn't reach it, extend the cohort window in 7-day steps
(up to 28 days) and shift the report date by the same amount; if still short, report the numbers
as **low confidence** and decide nothing on them except alarms.

## Method

- **Rates** are computed per window as count / denominator with the denominators in Definitions.
- **v1 vs v1.1 retention** uses a two-proportion z-test on Dn:
  `p = (x1 + x2) / (n1 + n2)`, `z = (p2 − p1) / sqrt(p · (1 − p) · (1/n1 + 1/n2))`, where `x` is
  users retained and `n` the cohort size. Significant if `|z| ≥ 1.96` (95 %). Report `p1`, `p2`,
  the difference in points, `z` and the verdict.
- Compare cohorts of the **same length** and the **same weekday mix** (start both on the same weekday).
- Exclude debug installs (they send nothing) and internal-testing tracks (filter by Play track or
  app version where the dashboard allows).
- Do not change the store listing, run Play listing experiments or promotions during a comparison
  window without noting it in the report; they change who installs.

## Decision rules

| Finding | Action |
|---|---|
| Any stability alarm | Fix first (hotfix release); pause other work. |
| Completion < 50 %, crack rate > 25 % or > 50 % of abandons in chanting/wavering | Recalibrate the chant check (ADR-0018/0027) before v1.1: split by `mode` and `score` buckets, and by device model in TelemetryDeck. |
| Lock within 30 s < 70 % | Investigate the compass: device models with slow locks; consider a calibration hint. |
| Mic granted < 60 % | Revisit the permission rationale copy and timing. |
| Conversion < 15 % | Iterate the store listing (icon, screenshots, short description); use Play listing experiments outside comparison windows. |
| D1 < 25 % | Onboarding problem: look at the funnel's first ritual (intro → seeking → lock) before retention features. |
| D7 < 8 % with healthy funnel | Confirms ADR-0034's diagnosis: ship v1.1 as planned. |
| v1.1: D7 + 5 points, significant, and rituals per active player-day ≥ 0.4 | v1.1 succeeds: proceed with "Next" items (reminder, daily line, rewarded ads). |
| v1.1: no significant change | Stop adding features; look at where day-3+ players drop (funnel, rituals per day), and revisit the daily loop. |
| Teaser tap rate ≥ 20 % | New demon content (lore over days, daily lines) is v1.1's lever. |
| Teaser tap rate 5–20 % | Build it and test another hook. |
| Teaser tap rate < 5 % | Rethink v1.1. |

## Checklists

### A. Pre-flight (L − 1)
- [ ] Release build installed from the Play internal track sends a test ritual; the signals appear
      in TelemetryDeck within 10 minutes, with app version and all 8 event types reachable.
- [ ] Turning off "Share anonymous usage data" stops new signals (send one ritual, check none arrive).
- [ ] Privacy policy URL in the listing matches `docs/PRIVACY_POLICY.md`; Data safety answers match
      the spec.
- [ ] TelemetryDeck dashboard has these insights saved: active users (daily), retention by install
      week (D1/D7/D30), ritual funnel (`ritual_start` → `direction_locked` → `line_result` line 1
      sealed → `ritual_end` completed), crack rate, lenient share, `ritual_end` by phase.
- [ ] TelemetryDeck dashboard has a teaser insight (shown → tap funnel) saved.
- [ ] Play Console access for the evaluator.

### B. Each report
- [ ] Note the dates, cohort windows and sample sizes; mark low confidence where the minimum isn't met.
- [ ] Fill the targets table with actual values and a status (✅ / ⚠️ alarm / ⏳ not enough data).
- [ ] Apply the decision rules; list actions with owners.
- [ ] Note anything that changed in the window (releases, listing changes, featuring).

## Report template

```markdown
# <v1 week N | v1 baseline | v1.1 report>, <date>
Window: <dates> · Cohort: <dates>, n = <installs> · Rituals: <count> · Confidence: <normal | low>

| Area | Metric | Target | Actual | Status |
|---|---|---|---|---|
| ... |

## Retention comparison (v1.1 only)
D7: v1 <p1> (n1) · v1.1 <p2> (n2) · Δ <points> · z = <z> · <significant | not significant>

## Decisions
- <rule> → <action>, <owner>, <due>

## Notes
<releases, listing changes, anomalies>
```

Reports go to `docs/success/reports/<date>-<name>.md`. Never commit raw exports or anything with
per-user data.
