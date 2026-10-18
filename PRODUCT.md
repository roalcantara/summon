# PRODUCT — Summon Demons Safely

Canonical product spec. Supersedes `reference/prompts/*` and everything else in
`reference/`. Decisions behind these rules live in `docs/decisions/`.

## Pitch

A small native Android game where the player summons cute chibi Goetic demons by
facing each demon's own direction and chanting a Latin incantation into the
microphone. One summon a day; bound demons get restless and escape when neglected.
Dark-fantasy RPG presentation, comedic tone.

## Core loop

Home → tap the grimoire → Grimoire (one demon at a time) → tap a sealed demon →
Ritual (face the demon's direction + chant) → back to the Grimoire, which celebrates
the new summon. One summon per local calendar day across all demons (ADR-0015).

## Screens and rules

| Topic | Rule |
|---|---|
| Home | "The Restless Grimoire" (ADR-0014): the game logo, a court chip ("In court · Sol & Venus" for the Houses in court now, or "Out of office"), a chained grimoire on an inlaid compass rose, "{n} of 2 spirits bound" and, until first used, the hint "Tap the grimoire". The book bobs, hops and has something peek out now and then; while any demon is restless it does so more often and its lock glows in that demon's accent. Tapping it opens the Grimoire on the demon that most needs attention. |
| Escape banner | If a demon escaped while the player was away, Home shows "Seal broken!" over "{Name} slipped his seal while you were away!" (or "Both spirits slipped their seals while you were away!") and "Tap to dismiss". |
| Grimoire | One column under an avatar rail (← back to Home, then one avatar per demon): a **stage** with the demon over a fixed-height **demon card**. Neither changes size between demons or states. Swiping the stage or tapping an avatar switches demon (ADR-0016). System back returns Home. |
| Lock state | A demon is **unbound** (never summoned, or escaped) or **bound**. Unbound: near-black silhouette with an accent rim glow, drifting smoke, his sigil as the avatar. Bound: full colour in front of his dimmed scene art, his portrait as the avatar (the sigil burns away once after a summon), and his quote under him — his restless quote if he was restless when this viewing began. A demon whose lore is not yet revealed shows smudged dossier pages; an escaped demon is unbound but keeps his lore. |
| Summon | No Summon button: tapping the sealed demon, or the pulsing "Tap to summon" cue under him, starts the Ritual. If today's summon is spent the cue reads "Circle cooling · Next summon in {h} h" (hours to local midnight, at least 1) and shakes when tapped. Tapping a bound demon does nothing. |
| Demon card | Number medallion, name, title; an info band with the description; four badges — Direction, House, Calling, Legions. Tapping a badge shows its explanation in the band until it is tapped again, something else is tapped, the demon changes or 5 s pass. Below, a six-page dossier carousel: Weakness, Abilities, Offices, Appearance, Summoner's warning, Secret (ADR-0011, ADR-0016). The Secret is readable only after a **flawless** summon. |
| Daily summon | One summon per local calendar day across all demons; a ritual started before midnight spends the day it started on (ADR-0015). |
| Neglect | Viewing a bound demon in the Grimoire is the visit. 2 days without a visit make him **restless** (jitter, restless quote, a jumpier Home grimoire); 3 days and he **escapes**: unbound again, lore and flawless kept (ADR-0015). |
| Court Hours | Each demon belongs to a House with court hours (Sol 09–12 and 15–18, Venus 06–12, local time). A ritual that locks while his House is in court gets 25 % more focus for the whole attempt (ADR-0012). |
| Ritual setup | Full-bleed altar: a faint ghost of the demon's sigil inside the altar ring (a marker on the ring shows where his direction lies relative to the phone), a cue chip (e.g. "Turn left · Face East") and the chant tablet. |
| Heading gate | `ritualHeading` is a **per-demon property** (compass degrees, tolerance ±20°). Staying within tolerance for **0.6 s** locks the ritual (haptic + sound), once per attempt. Once locked the ritual never returns to seeking: leaving the tolerance makes it **waver** (red ring, pulsing sigil, voice ignored) until the player faces him again; wavering itself loses nothing (ADR-0010). Nothing may hard-code a direction. |
| Chant | The incantation is **exactly 4 phrases** on the chant tablet: sealed phrases in the demon's colour with a filled medallion, the current one lit syllable by syllable (karaoke), upcoming ones dim. A syllable lights on each voiced syllable onset (≥ 120 ms apart) or after 350 ms of continuous voicing; sounds that aren't voice-like (claps, taps, wind) don't count (ADR-0008). All syllables lit → the phrase is **judged** once the last vowel ends (voicing stops, or after 300 ms): the vowel of each syllable and the pacing must plausibly match the text (ADR-0018). A match **seals** the phrase (chime + haptic); otherwise the demon scoffs (rotating cue line, warning haptic, red flash) and the phrase reopens with nothing lit. A rejection is not a crack and costs only time. |
| Focus clock | Phrases II–IV each have a focus clock of 3 s + 0.5 s per syllable (+25 % in court). It drains while the phrase is open — also in silence — and 3× as fast while wavering. When it runs out the last seal **cracks**: that phrase reopens with a full clock (phrase I has no clock). Failure never resets the whole ritual (ADR-0010). |
| Abandon | ✕ or back with no phrase sealed returns to the Grimoire. With a seal it asks "Abandon the ritual? / Your seals will fade." (**Abandon** / **Stay**); the ritual keeps running behind the question. |
| Sigil | Vector line art in the demon's accent that **inks itself** as the chant progresses: ink = (sealed phrases + lit/syllables) / 4. The rings and rim letters ink on lock; the glow follows the live mic level above the voice floor (ADR-0009). A seal flares that phrase's stroke group; a crack drains it in red. |
| Success | After phrase 4: a 2.5 s climax (abort, debug controls and error panels are hidden; touches and back are ignored; screen dims, particles converge, rising sound and haptic crescendo, flash, the demon rises out of the sigil), then back to the Grimoire with that demon selected: "New Demon Received!", rotating sunburst, "Tada!" and **Awesome!**. The demon art carries over as a shared element. The summon is saved when the climax starts. A summon with no crack is **flawless** and unlocks his Secret; **Awesome!** then opens the Secret page. |
| Persistence | Progress survives restarts (ADR-0013). |
| Debug aids | Ritual: "Simulate heading" (snaps to the demon's `ritualHeading`), "Hold to Chant" (synthetic voiced syllables sounding the expected vowels), the last chant-check score, and "Record clip" (Off / chant / nonsense: saves raw mic audio as WAV for calibration, ADR-0018). A "Debug" chip on Home and Grimoire opens a sheet: advance one day, reset the daily summon, court auto / in / out, bind / bind flawless / unsummon per demon, reset all, and a status line per demon. **Debug builds only.** |
| Orientation | Portrait only (the compass reading assumes it). |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual only if not already granted; if denied, explain why and offer a retry (or a shortcut to app settings once the denial is permanent). |
| Motion | If system animations are off: no particles, parallax, floating, jitter, shimmer, sunburst rotation, grimoire bob/hop/peek, dust puff, pulses or climax flash; the grimoire opening, the avatar burn-away and badge/carousel changes become short fades; the climax keeps its timing but the demon simply fades in. Ink-in, karaoke and the focus bar still update. |
| Privacy | No network access, no data collection. The mic and heading sensor run only while the Ritual is in the foreground; audio is analysed in memory and never stored or sent (ADR-0017, docs/PRIVACY_POLICY.md). Debug builds can save tester clips on the device (ADR-0018); release builds contain no recording code. |

## Demons

| | Asmodeus | Astaroth |
|---|---|---|
| id | `asmodeus` | `astaroth` |
| Number | 32 | 29 |
| Rank | Great King | Mighty Duke |
| Legions | 72 | 40 |
| House | Sol | Venus |
| Liege | Amaymon, King of the East | — (swears to no king) |
| Calling | Tutor | Oracle |
| Title | Lord of Arithmetic & Hidden Wealth | Duke of Past, Present & Future |
| Accent | `demon-asmodeus` (gold, see DESIGN.md) | `demon-astaroth` (magenta, see DESIGN.md) |
| Ritual heading | East (90°, ±20°) | North (0°, ±20°) |
| Quote | "Three heads are better than one—especially when two want your soul!" | "I know your past, your future, and that you didn't read the terms of service." |
| Restless quote | "I've counted every grain of dust in here. Twice. In three bases." | "I foresaw you'd forget me. I hate being right. No I don't." |
| Description | Teaches arithmetic, astronomy, geometry, and invisibility. Answers all questions—if you can stand the smugness. | Knows every secret and every tense. Will explain his own fall at length if asked. |

The description "invisibility" fixes an earlier typo ("invincibility"); the Goetia
credits Asmodeus with making men invisible.

### Houses and Callings

| House | Planet (badge) | Court (local) | Metal token | Tooltip |
|---|---|---|---|---|
| Sol | Sol | 09–12 and 15–18 | `house-sol` | Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses. |
| Venus | Venus | 06–12 | `house-venus` | Dukes of Venus hold court from sunrise to noon, in clear weather only. Very particular. |

| Calling | Tooltip |
|---|---|
| Warlord | Summon him for an army. |
| Tutor | Summon him to learn things. |
| Oracle | Summon him to know things. |
| Treasurer | Summon him for loot. |
| Menace | Summon him at your own risk. |

### Dossiers

**Asmodeus**
- Appearance: Three heads (bull, man and ram), so every meeting is a committee. He breathes fire, swishes a serpent’s tail and waddles on goose feet. He arrives on a dragon with a lance and a banner, because subtle is for Dukes.
- Offices: Hands out the Ring of Virtues (one per customer). · Tutors arithmetic, astronomy, geometry and every handicraft. · Answers any question fully, with appendices. · Makes you invisible. · Knows where treasure lies, and guards it.
- Summoner's warning: Summon him outdoors, stand the whole time, and take your hat off. Keep it on and his boss Amaymon pranks your entire ritual.
- Abilities: **Ring of Virtues** — Gives you a ring. The virtues are sold separately. · **Now You Don’t** — Turns you invisible. Great for heists, terrible for selfies. · **Dragon’s Ledger** — Finds buried treasure, counts it, then sits on it.
- Weakness: **Name-Drop** — Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.
- Secret: In an older tale, the smoke of a fish’s heart and liver sent him fleeing all the way to Egypt. He does not eat seafood. He does not discuss it.

**Astaroth**
- Appearance: An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand like a fancy pen. His breath arrives before he does.
- Offices: True answers about past, present and future. · Uncovers every secret, including the ones written in pencil. · Explains how the spirits fell, and why his own fall wasn’t his fault. · Makes you a scholar of all the liberal arts.
- Summoner's warning: Never let him get close; his breath is a weapon. Hold your magic ring up to your face like a tiny gas mask and nod politely.
- Abilities: **All Three Tenses** — He’s read ahead and won’t stop hinting. · **Open Secret** — Every secret you have, now his favourite trivia. · **The Fall (Director’s Cut)** — A complete, unrequested account. Spoiler: someone else pushed him.
- Weakness: **Dragon Breath** — Brave summoners hold a ring to their nose and win. He respects personal space only when it’s enforced with jewellery.
- Secret: Before the dragon and the breath, his name belonged to Astarte, a goddess of love and war. He’d rather you didn’t bring it up.

### Chants (4 phrases each)

**Asmodeus**
1. DAEMONIUM RESIDENTE,
2. OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME.
3. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS,
4. UT VOTA OMNIA OPUS EST.

**Astaroth**
1. TUI GRATIA LOVIS
2. GRATIA SIT CURA.
3. HIC EST ME DIGNUS LEGI
4. SINT BENE OBEDIENTES.

"LOVIS" is kept verbatim from the original prompt; see ADR-0005.

## Non-goals (MVP)

- More than two demons; the roster rail from the mockups.
- Levels, power, stars, stats, synergies.
- Currencies, bottom navigation, reagents.
- Containment-breach / failure screen.
- Word or speech recognition (only loudness, pitch periodicity, syllable onsets and, per ADR-0018, vowel sounds and rhythm are measured).
- Gemini / AI features.
- More than one summon per day; notifications or reminders; cloud sync.
- Weather (Venus' "clear weather only" is flavour text).
