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
| Grimoire | One column under a header (← back to Home, a **GRIMOIRE** title banner, the menu gear): a **stage** with the demon over a fixed-height **demon card**. Neither changes size between demons or states. Swiping the stage or tapping its ‹ › arrows switches demon (ADR-0016, ADR-0032). System back returns Home. |
| Lock state | A demon is **unbound** (never summoned, or escaped) or **bound**. His sigil lies on the summoning circle under him in both states. Unbound: near-black silhouette with an accent rim glow, drifting smoke. Bound: full colour in front of his dimmed scene art, and his quote under him — his restless quote if he was restless when this viewing began. A demon whose lore is not yet revealed shows smudged dossier pages; an escaped demon is unbound but keeps his lore. |
| Summon | No Summon button: tapping the sealed demon, or the pulsing "Tap to summon" cue under him, starts the Ritual. If today's summon is spent the cue reads "Circle cooling · Next summon in {h} h" (hours to local midnight, at least 1) and shakes when tapped. Tapping a bound demon does nothing. |
| Demon card | Number medallion, name, title; an info band with the description; four badges — Direction, House, Calling, Legions. Tapping a badge shows its explanation in the band until it is tapped again, something else is tapped, the demon changes or 5 s pass. Below: a tarot card: the front shows identity, description and badges; the ⟲ button turns it to the dossier, six sections in one list: Weakness, Abilities, Offices, Appearance, Summoner's warning, Secret (ADR-0011, ADR-0016, ADR-0033). The Secret is readable only after a **flawless** summon. |
| Daily summon | One summon per local calendar day across all demons; a ritual started before midnight spends the day it started on (ADR-0015). |
| Neglect | Viewing a bound demon in the Grimoire is the visit. 2 days without a visit make him **restless** (jitter, restless quote, a jumpier Home grimoire); 3 days and he **escapes**: unbound again, lore and flawless kept (ADR-0015). |
| Court Hours | Each demon belongs to a House with court hours (Sol 09–12 and 15–18, Venus 06–12, local time). A ritual that locks while his House is in court gets 25 % more focus for the whole attempt (ADR-0012). |
| Ritual setup | Before every ritual, the intro card: title, singer and Summon on the left, the four numbered rules on the right (ADR-0033). The whole Ritual happens on the chant screen (ADR-0020). Its sigil guides to the direction: a marker on its ring points at the demon relative to the phone (at the top and green when facing him), the ring fills green during the 0.6 s lock, and the sigil shows faintly before it inks. Below it: "← FACE EAST" (neutral) until he is faced; under the phrase, only the count "I of IV" until the lock (ADR-0021). Microphone or heading problems show their panel in place of the phrase. |
| Chant screen | For the whole Ritual, a dedicated **landscape** screen (ADR-0019, ADR-0029) over the demon's House stage, with no cards or frames (after Stitch "stage HUD"). A top bar holds **?** (rules) on the left, the demon's name with four seal marks I–IV centred (the current one breathes, faster while the phone checks), and **✕** alone on the right. On the left 30 %: the **sigil**, tilting toward the demon, inside a ring that shows how far off you are while turning, fills over the 0.6 s lock, then drains as the focus clock. Its glow grows with the lock, flares on it, and then follows your voice. Under it, the **direction pill**: "FACE EAST →" while turning, "FACE EAST ✓" once facing, amber "PAUSED ←" after turning away. Chevrons on the screen edge point the way to turn. On the right, a **feedback chip** above the line: "LOCKED" before the lock, the voice meter, "CHECKING…", "82 % · SEALED ✓", "25 % · heard: “…” ↺" when a whole recital missed, or "SEAL CRACKED ✕"; each score shows for 2.5 s. Below it, the **line**, as large as fits without breaking a word: dim until the direction is locked, brightening as the lock fills and opening with a soft flare, its syllables lighting in turn with a green line under each heard word. Before every ritual the "How to chant" card comes down over it (ADR-0021). Large screens that ignore the rotation get the sigil and pill in a band under the top bar. |
| Heading gate | `ritualHeading` is a **per-demon property** (compass degrees, tolerance ±20°). Staying within tolerance for **0.6 s** locks the ritual (haptic + sound), once per attempt. Once locked the ritual never returns to seeking: leaving the tolerance makes it **waver** (red ring with the marker showing the way back, red blinking direction line, voice ignored) until the player faces him again; wavering itself loses nothing (ADR-0010). Nothing may hard-code a direction. |
| Chant | The incantation is **exactly 4 phrases**, read on the chant screen one at a time, the current one lit syllable by syllable (karaoke). Before the lock the chant tablet previews them dim. A syllable lights on each voiced syllable onset (≥ 120 ms apart) or after 350 ms of continuous voicing; sounds that aren't voice-like (claps, taps, wind) don't count (ADR-0008). The line seals when it is heard: after each pause the phone transcribes, on the device and in Latin, everything said since the line began, and the line seals as soon as it is found in it (ADR-0027). Repeats and restarts are fine; there is no rejection: after the last syllable, or when a check misses after the whole line, the words clear and the line can simply be recited again (ADR-0028). Words turn green as they are heard. On phones too slow for this, a line seals when every syllable has lit. |
| Focus timer | Each part of lines II–IV has **60 s** (+25 % in court; ADR-0025), shown by the sigil's ring with no numbers. It drains while the line is open and 3× as fast while wavering; after a verdict it waits up to 3 s, or until the next syllable. **Every passed part refills it**, and a rejection costs nothing. When it empties the last seal **cracks**: that line reopens with a full timer (line I has none). Failure never resets the whole ritual (ADR-0010). |
| Sigil | Vector line art in the demon's accent that **inks itself** as the chant progresses: ink = (sealed phrases + lit/syllables) / 4. The rings and rim letters ink on lock; the glow follows the live mic level above the voice floor (ADR-0009). A seal flares that phrase's stroke group; a crack drains it in red. |
| Success | After phrase 4: a 2.5 s climax (abort, debug controls and error panels are hidden; touches and back are ignored; screen dims, particles converge, rising sound and haptic crescendo, flash, the demon rises out of the sigil), then back to the Grimoire with that demon selected: "New Demon Received!", rotating sunburst, "Tada!" and **Awesome!**. The demon art carries over as a shared element. The summon is saved when the climax starts. A summon with no crack is **flawless** and unlocks his Secret; **Awesome!** then opens the Secret page. |
| Persistence | Progress survives restarts (ADR-0013). |
| Debug aids | Ritual, in a small floating panel behind a round dashed "D" next to the "?" (closed by default, takes no space; ADR-0021): "Simulate heading" (snaps to the demon's `ritualHeading`), "Hold to Chant" (synthetic voiced syllables sounding the expected vowels), the last chant-check score, and "Record clip" (Off / chant / nonsense: saves raw mic audio as WAV for calibration, ADR-0018). A "Debug" chip on Home and Grimoire opens a sheet: advance one day, reset the daily summon, court auto / in / out, bind / bind flawless / unsummon per demon, reset all, and a status line per demon. **Debug builds only.** |
| Orientation | Portrait, except the chant screen, which is landscape (ADR-0019). The heading follows the screen's rotation. |
| Permissions | `RECORD_AUDIO` requested on entering the Ritual only if not already granted; if denied, explain why and offer a retry (or a shortcut to app settings once the denial is permanent). |
| Motion | If system animations are off: no particles, parallax, floating, jitter, shimmer, sunburst rotation, grimoire bob/hop/peek, dust puff, pulses or climax flash; the grimoire opening and badge/carousel changes become short fades; the climax keeps its timing but the demon simply fades in. Ink-in, karaoke and the focus bar still update. |
| Privacy | No network access, no data collection. The mic and heading sensor run only while the Ritual is in the foreground; audio is analysed in memory and never stored or sent (ADR-0017, docs/PRIVACY_POLICY.md). Debug builds can save tester clips on the device (ADR-0018); release builds contain no recording code. |

## Languages

English, Spanish, Brazilian Portuguese, Italian and French (ADR-0031). The phone's language
picks one (on Android 13+ also the per-app language in system Settings), unless the player picks
one in Settings (the gear at the top right of Home and the Grimoire); any other language gets English. The chant is Latin in every language. No translation is longer than its English
text. The app name, demon names and planets stay as they are.

## Demons

The English demon text in this section is the source for `res/values/demons.xml`.

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
