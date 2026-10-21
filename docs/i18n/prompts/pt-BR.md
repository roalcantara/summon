# Translation alternatives: pt-BR

> Prompt for another model (Gemini, GPT, …). Paste everything below the line. Generated from
> `docs/i18n/review.md` and the string resources; budgets are exact.

---

## Task

You are a game localizer for Brazilian Portuguese (pt-BR). For each string below, propose **at
least 3 alternatives**, best first, that fit the **character budget**.

## The game

"Summon Demons Safely" is a small mobile game with a dark-fantasy look and a comedic tone. The
player picks a cute chibi demon from an old grimoire, turns the phone to face the compass
direction that demon demands, and chants his Latin incantation out loud until four lines are
sealed. Demons have ranks (Great King, Mighty Duke), Houses (Sol, Venus) and Callings. The
humour is dry and silly: demons are smug, petty and over-dramatic.

## Rules

1. **Budget:** an alternative may have at most the given number of characters, counted as a
   person reads them (spaces and punctuation count; an accented letter is 1). Each placeholder
   (`%1$s`, `%2$s`) counts as **4 characters** and must appear exactly once, unchanged.
2. **Keep the joke, not the words.** A local idiom, pun or Brazilian slang that lands better is
   welcome, as long as it fits the budget and suits a dark-fantasy comedy.
3. **Never give the player a gender** (no "pronto/pronta", "preparado"). Use imperatives or
   neutral phrasing. Demons are male.
4. **Register:** informal, playful, "você" (or the imperative).
5. **Do not translate** demon names (Asmodeus, Astaroth, Amaymon), planets (Sol, Venus) or Latin.
6. **Fixed terms** (use these words): invocar (summon), selo/selar (seal), preso (bound),
   inquieto (restless), grimório, verso (a chant line), recitar (chant).
7. Strings marked **direction** take a compass point in `%1$s`. The alternative must read
   correctly with **all eight**: Norte, Nordeste, Leste, Sudeste, Sul, Sudoeste, Oeste, Noroeste. Avoid wording that needs an article
   that changes per point.

## Output

Reply with YAML only, one block per key, alternatives ordered best first:

```yaml
<key>:
  - text: "…"
    length: <characters, each placeholder counted as 4>
    back_translation: "<literal English>"
    note: "<why it works, or the pun>"
```

## Strings


### `chant_face` (direction)

- **English:** `Face %1$s`
- **Budget:** 9 characters
- **Where it shows:** Chant screen, direction pill above the Latin line, upper case. Tells the player which compass point to turn the phone to before chanting.
- **Current pt-BR:** `Mire %1$s` (9)
- **Check with:** `Mire Norte`, `Mire Nordeste`, `Mire Leste` …


### `badge_direction_text` (direction)

- **English:** `Face %1$s to summon. %2$s`
- **Budget:** 25 characters
- **Where it shows:** Demon card, text that appears when the Direction badge is tapped. %2$s is the next sentence (who the demon serves, e.g. "Serve a Amaymon, Rei do Leste."). Must stay consistent with chant_face.
- **Current pt-BR:** `Mire %1$s e invoque. %2$s` (25)
- **Check with:** `Mire Norte e invoque. %2$s`, `Mire Nordeste e invoque. %2$s`, `Mire Leste e invoque. %2$s` …


### `quote_tap_to_summon`

- **English:** `Tap to summon`
- **Budget:** 13 characters
- **Where it shows:** Grimoire stage, cue inside a dashed speech bubble under a demon that is not bound yet, upper case. Tapping starts the ritual.
- **Current pt-BR:** `Invoque-o!` (10)


### `intro_summon`

- **English:** `Summon`
- **Budget:** 6 characters
- **Where it shows:** Button on the "How to chant" intro card; pressing it starts the ritual. Short, upper case.
- **Current pt-BR:** `Bora!` (5)


### `dossier_secret`

- **English:** `Secret`
- **Budget:** 6 characters
- **Where it shows:** Title of the 6th dossier page, upper case, unlocked only by a flawless summon.
- **Current pt-BR:** `Sigilo` (6)


### `demon_asmodeus_title`

- **English:** `Lord of Arithmetic & Hidden Wealth`
- **Budget:** 34 characters
- **Where it shows:** Subtitle under the name ASMODEUS on the demon card; one line, cut with … if too long. He teaches arithmetic and knows where hidden treasure lies.
- **Current pt-BR:** `Senhor da Aritmética e do Tesouro` (33)


### `demon_asmodeus_ability_1_name`

- **English:** `Ring of Virtues`
- **Budget:** 15 characters
- **Where it shows:** Ability name, bold, before its line "Te dá um anel. As virtudes são vendidas à parte.". The offices page calls the ring "Anel das Virtudes".
- **Current pt-BR:** `Anel Virtuoso` (13)


### `demon_asmodeus_ability_2_name`

- **English:** `Now You Don’t`
- **Budget:** 13 characters
- **Where it shows:** Ability name for invisibility; English plays on "now you see me, now you don't".
- **Current pt-BR:** `Cadê? Sumiu!` (12)


### `demon_asmodeus_weakness_name`

- **English:** `Name-Drop`
- **Budget:** 9 characters
- **Where it shows:** Weakness name shown as "FRAQUEZA · <name>". The weakness: asked "Art thou Asmodeus?" he must admit his name and bow. English "Name-Drop" puns on name-dropping.
- **Current pt-BR:** `Pelo Nome` (9)


### `demon_astaroth_title`

- **English:** `Duke of Past, Present & Future`
- **Budget:** 30 characters
- **Where it shows:** Subtitle under the name ASTAROTH; he knows past, present and future. One line.
- **Current pt-BR:** `Duque de Ontem, Hoje e Amanhã` (29)


### `demon_astaroth_ability_2_name`

- **English:** `Open Secret`
- **Budget:** 11 characters
- **Where it shows:** Ability name; he learns all your secrets and treats them as trivia. English "Open Secret" is the idiom.
- **Current pt-BR:** `Fofoca` (6)


### `demon_astaroth_ability_3_name`

- **English:** `The Fall (Director’s Cut)`
- **Budget:** 25 characters
- **Where it shows:** Ability name; he tells the long, unrequested story of his fall from heaven. English parodies a film's "Director's Cut".
- **Current pt-BR:** `A Queda: Versão Estendida` (25)


### `demon_astaroth_weakness_name`

- **English:** `Dragon Breath`
- **Budget:** 13 characters
- **Where it shows:** Weakness name; his breath is a weapon, beaten by holding a ring to your nose.
- **Current pt-BR:** `Bafo Fatal` (10)


### `demon_astaroth_restless_quote`

- **English:** `I foresaw you'd forget me. I hate being right. No I don't.`
- **Budget:** 58 characters
- **Where it shows:** Speech bubble when the player neglected him. He predicted it, hates being right, then immediately contradicts himself: "No I don't." is the punchline.
- **Current pt-BR:** `Previ que você me esqueceria. Odeio ter razão. Mentira.` (55)


### `calling_warlord_label`

- **English:** `Warlord`
- **Budget:** 7 characters
- **Where it shows:** Calling badge label (upper case, Cinzel, inside a small badge): what the demon is summoned for, here an army.
- **Current pt-BR:** `General` (7)


### `calling_treasurer_label`

- **English:** `Treasurer`
- **Budget:** 9 characters
- **Where it shows:** Calling badge label (upper case, inside a small badge): the demon is summoned for loot.
- **Current pt-BR:** `Banqueiro` (9)


### `house_sol_tooltip`

- **English:** `Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.`
- **Budget:** 68 characters
- **Where it shows:** Tooltip of the House badge: the Kings of Sol hold court 9–12 and 15–sunset (real-world hours). "Sol" is the planet name and stays as is; a sun/sunglasses pun is welcome.
- **Current pt-BR:** `Reis de Sol: corte das 9 às 12 e das 15 ao poente. Óculos de sol!` (65)
