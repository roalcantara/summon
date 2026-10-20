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

### es · compass_points[6]

- **Status:** open
- **English (4):** West
- **Translation (5):** Oeste
- **Lost:** Nothing: "Oeste" is the only word for West.
- **Alternatives:** None that fit.

### es · calling_oracle_label

- **Status:** open
- **English (6):** Oracle
- **Translation (7):** Oráculo
- **Lost:** Nothing; one character over.
- **Alternatives:** Vidente (7), Profeta (7) — also over; Sabio (5) fits but loses the oracle.

### es · badge_legions_label

- **Status:** open
- **English (12):** %1$d legions
- **Translation (12):** %1$d huestes
- **Lost:** "legiones" (13) was first logged as an exception, but on the device "72 LEGIONES" wrapped out of the badge, leaving only "72". The badge has no spare room, so the label became "huestes" (hosts); the badge explanation still says "legiones".
- **Alternatives:** none that keep "legiones" on the badge; "%1$d leg." (abbreviation) fits but reads poorly.

### es · chant_sealed

- **Status:** open
- **English (6):** Sealed
- **Translation (7):** Sellado
- **Lost:** Nothing; "Sellado" is one character over in the result chip.
- **Alternatives:** Listo (5, "done") — loses "sealed".

### es · chant_face

- **Status:** open
- **English (9):** Face %1$s
- **Translation (9):** Mira %1$s
- **Lost:** Telegraphic: "Mira Norte" has no article ("Mira al Norte").
- **Alternatives:** Mira al %1$s (12, needs exception).

### es · badge_direction_text

- **Status:** open
- **English (25):** Face %1$s to summon. %2$s
- **Translation (24):** Mira %1$s e invoca. %2$s
- **Lost:** Same telegraphic "Mira Este" as chant_face.
- **Alternatives:** Mira al %1$s e invoca. %2$s (27, needs exception).

### es · dossier_weakness

- **Status:** open
- **English (8):** Weakness
- **Translation (8):** Flaqueza
- **Lost:** Glossary changed from "Debilidad" (9) to "Flaqueza" (8).
- **Alternatives:** Debilidad (9, needs exception).

### es · dossier_secret

- **Status:** open
- **English (6):** Secret
- **Translation (6):** Sigilo
- **Lost:** "Secreto" (7) does not fit; "Sigilo" means secrecy. Glossary updated.
- **Alternatives:** Secreto (7, needs exception).

### es · demon_asmodeus_title

- **Status:** open
- **English (34):** Lord of Arithmetic & Hidden Wealth
- **Translation (33):** Señor del Cálculo y el Oro Oculto
- **Lost:** "Arithmetic" became "Calculation" and "Hidden Wealth" became "Hidden Gold".
- **Alternatives:** Señor de la Aritmética y el Oro Oculto (38, needs exception).

### es · demon_asmodeus_ability_2_name

- **Status:** open
- **English (13):** Now You Don’t
- **Translation (11):** Ya No Estoy
- **Lost:** "Now You Don't" became "I'm Not Here Anymore".
- **Alternatives:** Ahora No Me Ves (15, needs exception).

### es · demon_asmodeus_ability_3_line

- **Status:** open
- **English (50):** Finds buried treasure, counts it, then sits on it.
- **Translation (45):** Halla tesoros, los cuenta y se sienta encima.
- **Lost:** "buried" dropped.
- **Alternatives:** Halla tesoros enterrados, los cuenta y se sienta encima. (56, needs exception).

### es · demon_asmodeus_weakness_name

- **Status:** open
- **English (9):** Name-Drop
- **Translation (9):** El Nombre
- **Lost:** "Name-Drop" became "The Name"; the pun is lost.
- **Alternatives:** Por Nombre (10, needs exception).

### es · demon_asmodeus_weakness_line

- **Status:** open
- **English (101):** Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.
- **Translation (94):** Pregunta “¿Sois vos Asmodeus?” y debe admitirlo y postrarse. Tres cabezas, un ego, cero calma.
- **Lost:** "Art thou" became archaic "¿Sois vos…?" (kept); "bow to the floor" became "prostrate himself".
- **Alternatives:** …y debe admitirlo y hacer una reverencia hasta el suelo. (over budget)

### es · demon_astaroth_title

- **Status:** open
- **English (30):** Duke of Past, Present & Future
- **Translation (27):** Duque de Ayer, Hoy y Mañana
- **Lost:** "Past, Present & Future" became "Yesterday, Today and Tomorrow".
- **Alternatives:** Duque del Pasado, Presente y Futuro (35, needs exception).

### es · demon_astaroth_ability_2_name

- **Status:** open
- **English (11):** Open Secret
- **Translation (6):** Chisme
- **Lost:** "Open Secret" became "Gossip"; Spanish has the exact idiom "Secreto a Voces" (15).
- **Alternatives:** Secreto a Voces (15, needs exception) — the best fit, recommended.

### es · demon_astaroth_ability_3_name

- **Status:** open
- **English (25):** The Fall (Director’s Cut)
- **Translation (25):** La Caída: Edición Íntegra
- **Lost:** "Director's Cut" became "Uncut Edition".
- **Alternatives:** La Caída (Versión Extendida) (28, needs exception).

### es · demon_astaroth_appearance

- **Status:** open
- **English (138):** An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand like a fancy pen. His breath arrives before he does.
- **Translation (134):** Un ángel con mala intención sobre una bestia medio dragón, con una víbora en la diestra como pluma elegante. Su aliento llega primero.
- **Lost:** "His breath arrives before he does" became "His breath arrives first".
- **Alternatives:** …Su aliento llega antes que él. (142, needs exception).

### es · demon_astaroth_offices[3]

- **Status:** open
- **English (44):** Makes you a scholar of all the liberal arts.
- **Translation (36):** Te enseña todas las artes liberales.
- **Lost:** "Makes you a scholar" became "Teaches you": "erudito" would give the player a gender.
- **Alternatives:** Te vuelve una eminencia en las artes liberales. (47, needs exception).

### es · calling_warlord_label

- **Status:** open
- **English (7):** Warlord
- **Translation (7):** General
- **Lost:** "Warlord" became "General"; "Caudillo" (8) does not fit and has political baggage.
- **Alternatives:** Caudillo (8, needs exception).

### es · calling_menace_label

- **Status:** open
- **English (6):** Menace
- **Translation (5):** Plaga
- **Lost:** "Menace" became "Plague" (Plaga): "Amenaza" (7) does not fit.
- **Alternatives:** Amenaza (7, needs exception).
