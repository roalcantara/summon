# 10x Analysis: Summon Demons Safely, regular play
Session 1 | Date: 2026-10-23

## Current Value

A beautiful, playful take on "how to summon demons" apps (inspired by
`com.rmx.howsummondemons`, presentation inspired by Hero Wars). The core verb is unique and
physical: **face a direction and chant Latin aloud**, with a sigil inking itself and a climax.
The product will be ad-funded, with a deliberately friendlier ad approach than the original
(which shows an ad on nearly every tap). Ads only pay if players come back.

What the loop offers today (PRODUCT.md, ADR-0015):
- One summon per local day, across all demons; 2 demons (Asmodeus, Astaroth).
- Viewing a bound demon is a visit; 2 days without one → restless; 3 days → he escapes
  (lore and flawless kept).
- Court hours: +25 % focus while his House is in court (ADR-0012).
- A flawless summon unlocks the Secret page (ADR-0013).
- Non-goals today: levels, currencies, notifications or reminders, more than one summon a day,
  more than two demons.

## The Question

What small change gives a player a **reason to open the app every day**, without turning a
charming toy into a grind or an ad farm?

## Diagnosis: the loop runs out on day 2

1. **Day 1:** summon demon 1 (the best moment: the ritual and the climax).
2. **Day 2:** summon demon 2.
3. **Day 3 onwards:** nothing new to *do*. The only obligation is to open the Grimoire every two
   days so nobody escapes, and that visit is passive (a glance, no ritual).
4. **The best part of the app, the chant, is locked away** once both demons are bound. The
   player only chants again after a demon escapes, i.e. after the game has punished them.

So retention today rests on loss aversion (escape) with no positive pull. The fix is not more
screens: it's giving the existing verb, the ritual, a daily purpose, and making progress
visible across days.

---

## Massive Opportunities

### 1. A growing court: more demons over time
**What**: Release new Goetic demons as content drops (there are 72), e.g. one every two weeks,
or unlocked by days at court.
**Why 10x**: Collection is the strongest long-term pull in Hero Wars–like games; each new demon
is a new ritual, new lore, a reason to return and a reason to update.
**Unlocks**: Release cadence, "what's new" moments, store featuring, sets/Houses collections.
**Effort**: High per demon: cutout and scene art, sigil, chant phrases checked against Whisper,
lore in 5 languages.
**Risk**: Content cost; art consistency (Stitch pipeline exists).
**Score**: 👍 (the strategic bet; not a quick win)

---

## Medium Opportunities

### 1. Renew the pact: a daily ritual for bound demons  ⭐
**What**: When today's summon is unused, a **bound** demon can be chanted again ("Renew the
pact") instead of being untouchable. It uses the same daily summon. Each renewal advances that
demon's **bond** (a day counter shown on his card, e.g. "Bound 5 days") and resets his
restlessness.
**Why 10x**: It gives the app's most delightful action a daily purpose. The daily summon stops
being a dead resource after day 2, and the escape mechanic becomes a counterweight rather than
the only reason to return.
**Impact**: Every session can include a ritual; a habit loop of about 2 minutes a day.
**Effort**: Medium: allow the ritual on bound demons (today `blocked` pops it), a `bondDays` /
`lastRenewalDay` field in `DemonRecord`, a card line, strings ×5.
**Score**: 🔥

### 2. Lore that unlocks over days, not all at once
**What**: Today a first summon reveals the whole dossier. Instead, the first summon reveals the
first entries, and each renewal (or each day bound) inks in the next one: Weakness, Abilities,
Offices, Appearance, Warning; the Secret still needs a flawless summon.
**Why 10x**: Turns existing, already-written content (6 sections × 2 demons) into a **drip-feed
reward** for coming back. The ink-in animation and the glowing flip button already exist.
**Impact**: A visible "what's next" on every card ("2 of 6 revealed").
**Effort**: Low–medium: a reveal count per record instead of one `revealed` flag; no new content.
**Score**: 🔥 (pairs with Medium #1)

### 3. Rewarded ads, offered at moments of need (later, with the ad SDK)
**What**: Player-initiated ads only: "Watch to calm him" when a demon is about to escape, or
"Watch to open the circle again" for a second summon today. Never interstitials on taps.
**Why 10x**: The friendly ad model in one feature: the player chooses the ad, gets real value,
and the escape and daily-limit mechanics give it natural demand.
**Impact**: Revenue without the "ad on every click" the original is known for.
**Effort**: Medium, plus policy work: the app currently has **no INTERNET permission and "no data
collected"** (ADR-0017); an ad SDK changes the privacy policy, the Data safety form and the
consent flow (UMP).
**Score**: 👍 (the monetisation step; needs its own ADR)

---

## Small Gems

### 1. Opt-in daily reminder at court hours
**What**: A local notification (no network) once a day, e.g. "Asmodeus holds court until 12:00"
or "Astaroth grows restless". Off unless the player turns it on (asked after the first summon);
max one a day.
**Why powerful**: The single strongest retention lever for daily games; court hours already
give it a fresh, in-world reason each day.
**Effort**: Low–medium (WorkManager, POST_NOTIFICATIONS on Android 13+, a Settings toggle). Today
a non-goal: needs an ADR.
**Score**: 🔥

### 2. Bond counter on the card
**What**: "Bound 5 days" (or a small flame with a number) under the demon's title.
**Why powerful**: Makes progress visible; a streak players don't want to break. Free once
Medium #1 exists.
**Effort**: Low.
**Score**: 👍

### 3. A daily line from each bound demon
**What**: On the first visit of the day the speech bubble shows a new quip from a small pool
(e.g. 7 per demon) instead of the same quote.
**Why powerful**: Rewards the visit itself with something new; it fits the comedic tone.
**Effort**: Low code; the cost is writing and translating about 14 lines × 5 languages.
**Score**: 👍

### 4. "Next on the agenda" on Home
**What**: One line under the grimoire saying what today offers: "Asmodeus awaits renewal",
"Circle cooling · 7 h", "Astaroth is restless".
**Why powerful**: Removes "what do I do now?" on returning, and points straight at the daily action.
**Effort**: Low.
**Score**: 🤔 (useful once there is a daily action to point at)

### 5. Share the summon
**What**: After "Awesome!", a share button that renders the demon card as an image ("I summoned
Asmodeus flawlessly").
**Why powerful**: Free word of mouth for a visual, funny app.
**Effort**: Low–medium.
**Score**: 🤔

---

## Recommended Priority

### Do Now (v1.1, right after release)
1. **Renew the pact** (Medium #1) — gives the ritual a daily purpose; the core fix for "nothing to
   do after day 2".
2. **Lore unlocks over days** (Medium #2) — turns existing content into the reward for returning.
3. **Bond counter** (Small #2) — makes the streak visible; comes almost free with #1.

### Do Next
1. **Opt-in court-hour reminder** (Small #1) — the return trigger, once there is something worth
   returning for.
2. **Daily demon line** (Small #3) — cheap delight on each visit.
3. **Rewarded ads at moments of need** (Medium #3) — the friendly monetisation, with its privacy work.

### Explore
1. **More demons as content drops** (Massive #1) — the long-term collection pull; Risk: content
   cost per demon. Upside: a reason to update and to return for months.

### Backlog
1. **Share the summon** — growth, after retention works.
2. **Next on the agenda** — after the daily action exists.

---

## Questions

### Answered
- **Q**: Why don't players come back today? **A**: After both demons are bound (day 2) the daily
  summon has no use, and the only return reason is avoiding an escape.
- **Q**: Does any idea need new art? **A**: Only more demons. Renewal, drip lore, the counter and
  the reminder reuse what exists.

### Blockers
- **Q**: Ship v1 as is and add these in v1.1, or hold the release? (Recommendation: ship v1, then
  v1.1 with items 1–3 within a couple of weeks; the release is otherwise ready.)
- **Q**: Reminders and renewals touch current non-goals (notifications; levels). OK to revise
  PRODUCT.md's non-goals with an ADR?
- **Q**: When will the ad SDK come in? It ends the "no network, no data collected" stance and
  needs a consent flow.

## Next Steps
- [ ] Decide: ship v1 now vs. include Renew the pact
- [ ] Decide: revise non-goals (notifications, renewal) via ADR
- [ ] Measure: define D1/D7/D30 retention and daily-ritual rate before v1.1 (topic 2)
- [ ] Spec: "Renew the pact" + drip lore (brainstorming → spec → plan)
