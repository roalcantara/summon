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
7. **Flavour strings** (demon titles, ability and weakness names, quotes, tooltips) should
   reach for **Brazilian pop culture**: popular sayings, idioms, classic TV, novelas, films,
   comedy shows, long-lived memes. Example of the target: "Dragon Breath" became "Bafo de
   Onça" (an everyday idiom for awful breath); "Open Secret" became "Fofoca"; "The Fall
   (Director's Cut)" became "Tombo (Versão do Diretor)". Limits: the reference must be widely
   known and lasting (no trend of the month), no song lyrics beyond a short phrase, no real
   people or brands, and the text must still make sense to a player who misses the reference.
8. **Functional strings** (buttons, directions, status labels, badge labels) stay plain and
   clear: no jokes a player has to decode to know what to tap or where to turn.
9. Strings marked **direction** take a compass point in `%1$s`. The alternative must read
   correctly with **all eight**: Norte, Nordeste, Leste, Sudeste, Sul, Sudoeste, Oeste, Noroeste. Avoid wording that needs an article
   that changes per point.

## Output

Reply with YAML only, one block per key, alternatives ordered best first:

```yaml
<key>:
  - text: "…"
    length: <characters, each placeholder counted as 4>
    back_translation: "<literal English>"
    reference: "<the idiom, show or meme it plays on, or none>"
    note: "<why it works, or the pun>"
```

## Strings


### `demon_asmodeus_ability_1_name` (flavour)

- **English:** `Ring of Virtues`
- **Budget:** 15 characters
- **Where it shows:** Ability name, bold, before its line "Te dá um anel. As virtudes são vendidas à parte.". The offices page calls the ring "Anel das Virtudes".
- **Current pt-BR:** `Anel Virtuoso` (13)


### `demon_asmodeus_ability_2_name` (flavour)

- **English:** `Now You Don’t`
- **Budget:** 13 characters
- **Where it shows:** Ability name for invisibility; English plays on "now you see me, now you don't".
- **Current pt-BR:** `Cadê? Sumiu!` (12)


### `demon_asmodeus_weakness_name` (flavour)

- **English:** `Name-Drop`
- **Budget:** 9 characters
- **Where it shows:** Weakness name shown as "FRAQUEZA · <name>". The weakness: asked "Art thou Asmodeus?" he must admit his name and bow. English "Name-Drop" puns on name-dropping.
- **Current pt-BR:** `Pelo Nome` (9)


### `demon_astaroth_restless_quote` (flavour)

- **English:** `I foresaw you'd forget me. I hate being right. No I don't.`
- **Budget:** 58 characters
- **Where it shows:** Speech bubble when the player neglected him. He predicted it, hates being right, then immediately contradicts himself: "No I don't." is the punchline.
- **Current pt-BR:** `Previ que você me esqueceria. Odeio ter razão. Mentira.` (55)


### `calling_warlord_label` (functional)

- **English:** `Warlord`
- **Budget:** 7 characters
- **Where it shows:** Calling badge label (upper case, Cinzel, inside a small badge): what the demon is summoned for, here an army.
- **Current pt-BR:** `General` (7)


### `calling_treasurer_label` (functional)

- **English:** `Treasurer`
- **Budget:** 9 characters
- **Where it shows:** Calling badge label (upper case, inside a small badge): the demon is summoned for loot.
- **Current pt-BR:** `Banqueiro` (9)


### `house_sol_tooltip` (flavour)

- **English:** `Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.`
- **Budget:** 68 characters
- **Where it shows:** Tooltip of the House badge: the Kings of Sol hold court 9–12 and 15–sunset (real-world hours). "Sol" is the planet name and stays as is; a sun/sunglasses pun is welcome.
- **Current pt-BR:** `Reis de Sol: corte das 9 às 12 e das 15 ao poente. Óculos de sol!` (65)
