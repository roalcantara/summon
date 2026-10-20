# Translation review queue

Hard cases from the translation work (i18n spec §6.5), one entry per language and key: a
translation that exceeds the English length (and is listed in
`app/src/test/resources/i18n/length-exceptions.txt`), or one that fits only by giving something
up: a weaker or replaced joke, a lost pun, a changed meaning or tone.

The owner reviews one entry at a time and sets its status to `accepted` (keep the translation)
or `changed` (write which alternative, or the new text, and update the resource).

Entry format:

### <locale> · <key>

- **Status:** open
- **English (<n>):** …
- **Translation (<n>):** …
- **Lost:** what the translation gives up, or why it is longer
- **Alternatives:** one or two other phrasings, with lengths

---

### fr · fonts

- **Status:** open
- **English:** n/a (font coverage, found by the glyph check in Task 7)
- **Translation:** French needs œ / Œ (cœur, œuvre, sœur) and rarely Ÿ. The bundled Cinzel and
  Inter files are Latin-1 subsets and lack all three; every letter es, pt-BR and it need is present.
- **Lost:** with no fix, French text must avoid those letters (the glyph test fails otherwise), or
  Android draws them in the system font, which stands out inside Cinzel titles.
- **Alternatives:** (a) replace the six font files with Latin Extended-A builds of Cinzel and Inter
  (a few KB more each); (b) French avoids œ by wording (e.g. « cœur » → « âme »), the test stays as is.

### pt-BR · compass_points[2]

- **Status:** open
- **English (4):** East
- **Translation (5):** Leste
- **Lost:** Nothing: "Leste" is the standard pt-BR word; "Este" (4) is European usage.
- **Alternatives:** Este (4) — fits, but reads Portuguese-from-Portugal.

### pt-BR · compass_points[6]

- **Status:** open
- **English (4):** West
- **Translation (5):** Oeste
- **Lost:** Nothing: "Oeste" is the only word for West.
- **Alternatives:** None that fit.

### pt-BR · calling_oracle_label

- **Status:** open
- **English (6):** Oracle
- **Translation (7):** Oráculo
- **Lost:** Nothing; one character over.
- **Alternatives:** Vidente (7, "seer"), Profeta (7) — also over; Sábio (5, "sage") fits but loses the oracle.

### pt-BR · chant_face

- **Status:** open
- **English (9):** Face %1$s
- **Translation (9):** Mire %1$s
- **Lost:** Telegraphic: "Mire Norte" has no article ("Mire o Norte").
- **Alternatives:** Rumo ao %1$s (12) — "Rumo ao Norte", reads best; needs a length exception.

### pt-BR · badge_direction_text

- **Status:** open
- **English (25):** Face %1$s to summon. %2$s
- **Translation (25):** Mire %1$s e invoque. %2$s
- **Lost:** Same telegraphic "Mire Leste" as chant_face.
- **Alternatives:** Vire ao %1$s e invoque. %2$s (27, needs exception).

### pt-BR · quote_tap_to_summon

- **Status:** open
- **English (13):** Tap to summon
- **Translation (10):** Invoque-o!
- **Lost:** "Tap" is gone; it reads "Summon him!".
- **Alternatives:** Toque e invoque (15, needs exception).

### pt-BR · intro_summon

- **Status:** open
- **English (6):** Summon
- **Translation (5):** Bora!
- **Lost:** Button reads "Let's go!" (BR slang) instead of "Summon".
- **Alternatives:** Invocar (7, needs exception); Vamos (5).

### pt-BR · dossier_secret

- **Status:** open
- **English (6):** Secret
- **Translation (6):** Sigilo
- **Lost:** "Segredo" (7) does not fit; "Sigilo" means secrecy. Glossary updated.
- **Alternatives:** Segredo (7, needs exception).

### pt-BR · demon_asmodeus_title

- **Status:** open
- **English (34):** Lord of Arithmetic & Hidden Wealth
- **Translation (33):** Senhor da Aritmética e do Tesouro
- **Lost:** "Hidden Wealth" became "Treasure" ("Tesouro"); "hidden" is lost.
- **Alternatives:** Senhor da Aritmética e do Ouro Oculto (37, needs exception).

### pt-BR · demon_asmodeus_ability_1_name

- **Status:** open
- **English (15):** Ring of Virtues
- **Translation (13):** Anel Virtuoso
- **Lost:** "Ring of Virtues" became "Virtuous Ring"; offices[0] still says "Anel das Virtudes".
- **Alternatives:** Anel das Virtudes (17, needs exception) — matches the offices page.

### pt-BR · demon_asmodeus_ability_2_name

- **Status:** open
- **English (13):** Now You Don’t
- **Translation (12):** Cadê? Sumiu!
- **Lost:** "Now You Don't" (see me) became "Where'd he go? Gone!".
- **Alternatives:** Agora Não Vê (12).

### pt-BR · demon_asmodeus_weakness_name

- **Status:** open
- **English (9):** Name-Drop
- **Translation (9):** Pelo Nome
- **Lost:** "Name-Drop" became "By Name"; the name-dropping pun is lost.
- **Alternatives:** Carteirada (10, BR for pulling rank; needs exception).

### pt-BR · demon_astaroth_title

- **Status:** open
- **English (30):** Duke of Past, Present & Future
- **Translation (29):** Duque de Ontem, Hoje e Amanhã
- **Lost:** "Past, Present & Future" became "Yesterday, Today and Tomorrow".
- **Alternatives:** Duque do Passado, Presente e Futuro (35, needs exception).

### pt-BR · demon_astaroth_ability_2_name

- **Status:** open
- **English (11):** Open Secret
- **Translation (6):** Fofoca
- **Lost:** "Open Secret" pun lost; it became "Gossip".
- **Alternatives:** Segredo Aberto (14, needs exception).

### pt-BR · demon_astaroth_ability_3_name

- **Status:** open
- **English (25):** The Fall (Director’s Cut)
- **Translation (25):** A Queda: Versão Estendida
- **Lost:** "Director's Cut" became "Extended Version".
- **Alternatives:** A Queda (Corte do Diretor) (26, needs exception).

### pt-BR · demon_astaroth_weakness_name

- **Status:** open
- **English (13):** Dragon Breath
- **Translation (10):** Bafo Fatal
- **Lost:** "Dragon Breath" became "Fatal Breath".
- **Alternatives:** Bafo de Dragão (14, needs exception).

### pt-BR · demon_astaroth_restless_quote

- **Status:** open
- **English (58):** I foresaw you'd forget me. I hate being right. No I don't.
- **Translation (55):** Previ que você me esqueceria. Odeio ter razão. Mentira.
- **Lost:** "No I don't." became "Lie." ("Mentira.") — same joke, different form.
- **Alternatives:** Só que não. (11) — BR meme, same length class.

### pt-BR · calling_warlord_label

- **Status:** open
- **English (7):** Warlord
- **Translation (7):** General
- **Lost:** "Warlord" became "General"; "Guerreiro" (9) does not fit.
- **Alternatives:** Guerreiro (9, needs exception).

### pt-BR · calling_treasurer_label

- **Status:** open
- **English (9):** Treasurer
- **Translation (9):** Banqueiro
- **Lost:** "Treasurer" became "Banker" ("Banqueiro"); "Tesoureiro" (10) does not fit.
- **Alternatives:** Tesoureiro (10, needs exception).

### pt-BR · house_sol_tooltip

- **Status:** open
- **English (68):** Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.
- **Translation (65):** Reis de Sol: corte das 9 às 12 e das 15 ao poente. Óculos de sol!
- **Lost:** Hours compressed ("9 às 12 e das 15 ao poente"); gains a pun: "Óculos de sol!" (sunglasses = 'Sol' glasses).
- **Alternatives:** Reis de Sol: corte das 9 ao meio-dia e das 15h ao pôr do sol. Leve óculos. (needs exception).
