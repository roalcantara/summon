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

- **Status:** changed: the six fonts were rebuilt with Latin Extended-A (`tools/gen_fonts.py`); rendering is pixel-identical, œ Œ Ÿ now draw
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

- **Status:** changed (owner, 2026-10-21)
- **English (9):** Face %1$s
- **Translation (9):** Vire %1$s
- **Lost:** Telegraphic: "Mire Norte" has no article ("Mire o Norte").
- **Alternatives:** Rumo ao %1$s (12) — "Rumo ao Norte", reads best; needs a length exception.

### pt-BR · badge_direction_text

- **Status:** changed (owner, 2026-10-21)
- **English (25):** Face %1$s to summon. %2$s
- **Translation (25):** Vire %1$s e invoque. %2$s
- **Lost:** Same telegraphic "Mire Leste" as chant_face.
- **Alternatives:** Vire ao %1$s e invoque. %2$s (27, needs exception).

### pt-BR · quote_tap_to_summon

- **Status:** changed (owner, 2026-10-21)
- **English (13):** Tap to summon
- **Translation (7):** Invocar
- **Lost:** "Tap" is gone; it reads "Summon him!".
- **Alternatives:** Toque e invoque (15, needs exception).

### pt-BR · intro_summon

- **Status:** accepted (owner, 2026-10-21)
- **English (6):** Summon
- **Translation (5):** Bora!
- **Lost:** Button reads "Let's go!" (BR slang) instead of "Summon".
- **Alternatives:** Invocar (7, needs exception); Vamos (5).

### pt-BR · dossier_secret

- **Status:** accepted (owner, 2026-10-21)
- **English (6):** Secret
- **Translation (6):** Sigilo
- **Lost:** "Segredo" (7) does not fit; "Sigilo" means secrecy. Glossary updated.
- **Alternatives:** Segredo (7, needs exception).

### pt-BR · demon_asmodeus_title

- **Status:** accepted (owner, 2026-10-21)
- **English (34):** Lord of Arithmetic & Hidden Wealth
- **Translation (33):** Senhor da Aritmética e do Tesouro
- **Lost:** "Hidden Wealth" became "Treasure" ("Tesouro"); "hidden" is lost.
- **Alternatives:** Senhor da Aritmética e do Ouro Oculto (37, needs exception).

### pt-BR · demon_asmodeus_ability_1_name

- **Status:** changed (owner, 2026-10-21; from Gemini/GPT round, docs/i18n/prompts/)
- **English (15):** Ring of Virtues
- **Translation (10):** O Precioso
- **Lost:** "Ring of Virtues" became "Virtuous Ring"; offices[0] still says "Anel das Virtudes".
- **Alternatives:** Anel das Virtudes (17, needs exception) — matches the offices page.

### pt-BR · demon_asmodeus_ability_2_name

- **Status:** changed (owner, 2026-10-21; from Gemini/GPT round, docs/i18n/prompts/)
- **English (13):** Now You Don’t
- **Translation (13):** Piscou, Sumiu
- **Lost:** "Now You Don't" (see me) became "Where'd he go? Gone!".
- **Alternatives:** Agora Não Vê (12).

### pt-BR · demon_asmodeus_weakness_name

- **Status:** changed (owner, 2026-10-21; from Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Name-Drop
- **Translation (6):** Crachá
- **Lost:** "Name-Drop" became "By Name"; the name-dropping pun is lost.
- **Alternatives:** Carteirada (10, BR for pulling rank; needs exception).

### pt-BR · demon_astaroth_title

- **Status:** accepted (owner, 2026-10-21)
- **English (30):** Duke of Past, Present & Future
- **Translation (29):** Duque de Ontem, Hoje e Amanhã
- **Lost:** "Past, Present & Future" became "Yesterday, Today and Tomorrow".
- **Alternatives:** Duque do Passado, Presente e Futuro (35, needs exception).

### pt-BR · demon_astaroth_ability_2_name

- **Status:** accepted (owner, 2026-10-21)
- **English (11):** Open Secret
- **Translation (6):** Fofoca
- **Lost:** "Open Secret" pun lost; it became "Gossip".
- **Alternatives:** Segredo Aberto (14, needs exception).

### pt-BR · demon_astaroth_ability_3_name

- **Status:** changed (owner, 2026-10-21)
- **English (25):** The Fall (Director’s Cut)
- **Translation (25):** Tombo (Versão do Diretor)
- **Lost:** "Director's Cut" became "Extended Version".
- **Alternatives:** A Queda (Corte do Diretor) (26, needs exception).

### pt-BR · demon_astaroth_weakness_name

- **Status:** changed (owner, 2026-10-21)
- **English (13):** Dragon Breath
- **Translation (12):** Bafo de Onça
- **Lost:** "Dragon Breath" became "Fatal Breath".
- **Alternatives:** Bafo de Dragão (14, needs exception).

### pt-BR · demon_astaroth_restless_quote

- **Status:** changed (owner, 2026-10-21; from Gemini/GPT round, docs/i18n/prompts/)
- **English (58):** I foresaw you'd forget me. I hate being right. No I don't.
- **Translation (53):** Previ que me esqueceria. Odeio ter razão. Só que não.
- **Lost:** "No I don't." became "Lie." ("Mentira.") — same joke, different form.
- **Alternatives:** Só que não. (11) — BR meme, same length class.

### pt-BR · calling_warlord_label

- **Status:** accepted (owner, 2026-10-21; both models agree)
- **English (7):** Warlord
- **Translation (7):** General
- **Lost:** "Warlord" became "General"; "Guerreiro" (9) does not fit.
- **Alternatives:** Guerreiro (9, needs exception).

### pt-BR · calling_treasurer_label

- **Status:** accepted (owner, 2026-10-21)
- **English (9):** Treasurer
- **Translation (9):** Banqueiro
- **Lost:** "Treasurer" became "Banker" ("Banqueiro"); "Tesoureiro" (10) does not fit.
- **Alternatives:** Tesoureiro (10, needs exception).

### pt-BR · house_sol_tooltip

- **Status:** changed (owner, 2026-10-21; from Gemini/GPT round, docs/i18n/prompts/)
- **English (68):** Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.
- **Translation (58):** Reis de Sol atendem 9h-12h e 15h-poente. Use filtro solar.
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

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
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

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Face %1$s
- **Translation (9):** Mira %1$s
- **Lost:** Telegraphic: "Mira Norte" has no article ("Mira al Norte").
- **Alternatives:** Mira al %1$s (12, needs exception).

### es · badge_direction_text

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (25):** Face %1$s to summon. %2$s
- **Translation (24):** Mira %1$s e invoca. %2$s
- **Lost:** Same telegraphic "Mira Este" as chant_face.
- **Alternatives:** Mira al %1$s e invoca. %2$s (27, needs exception).

### es · dossier_weakness

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (8):** Weakness
- **Translation (8):** Flaqueza
- **Lost:** Glossary changed from "Debilidad" (9) to "Flaqueza" (8).
- **Alternatives:** Debilidad (9, needs exception).

### es · dossier_secret

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Secret
- **Translation (6):** Sigilo
- **Lost:** "Secreto" (7) does not fit; "Sigilo" means secrecy. Glossary updated.
- **Alternatives:** Secreto (7, needs exception).

### es · demon_asmodeus_title

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (34):** Lord of Arithmetic & Hidden Wealth
- **Translation (33):** Señor del Cálculo y el Oro Oculto
- **Lost:** "Arithmetic" became "Calculation" and "Hidden Wealth" became "Hidden Gold".
- **Alternatives:** Señor de la Aritmética y el Oro Oculto (38, needs exception).

### es · demon_asmodeus_ability_2_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (13):** Now You Don’t
- **Translation (13):** Bomba de Humo
- **Lost:** "Now You Don't" became "I'm Not Here Anymore".
- **Alternatives:** Ahora No Me Ves (15, needs exception).

### es · demon_asmodeus_ability_3_line

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (50):** Finds buried treasure, counts it, then sits on it.
- **Translation (40):** Halla tesoros, los cuenta y los empolla.
- **Lost:** "buried" dropped.
- **Alternatives:** Halla tesoros enterrados, los cuenta y se sienta encima. (56, needs exception).

### es · demon_asmodeus_weakness_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Name-Drop
- **Translation (9):** Se Delata
- **Lost:** "Name-Drop" became "The Name"; the pun is lost.
- **Alternatives:** Por Nombre (10, needs exception).

### es · demon_asmodeus_weakness_line

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (101):** Ask “Art thou Asmodeus?” and he must admit it and bow to the floor. Three heads, one ego, zero chill.
- **Translation (98):** Pregunta “¿Sois vos Asmodeus?” y debe admitirlo y postrarse. Tres cabezas, un ego, la mecha corta.
- **Lost:** "Art thou" became archaic "¿Sois vos…?" (kept); "bow to the floor" became "prostrate himself".
- **Alternatives:** …y debe admitirlo y hacer una reverencia hasta el suelo. (over budget)

### es · demon_astaroth_title

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (30):** Duke of Past, Present & Future
- **Translation (27):** Duque de Ayer, Hoy y Mañana
- **Lost:** "Past, Present & Future" became "Yesterday, Today and Tomorrow".
- **Alternatives:** Duque del Pasado, Presente y Futuro (35, needs exception).

### es · demon_astaroth_ability_2_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (11):** Open Secret
- **Translation (11):** Puro Chisme
- **Lost:** "Open Secret" became "Gossip"; Spanish has the exact idiom "Secreto a Voces" (15).
- **Alternatives:** Secreto a Voces (15, needs exception) — the best fit, recommended.

### es · demon_astaroth_ability_3_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (25):** The Fall (Director’s Cut)
- **Translation (22):** La Caída (Sin Censura)
- **Lost:** "Director's Cut" became "Uncut Edition".
- **Alternatives:** La Caída (Versión Extendida) (28, needs exception).

### es · demon_astaroth_appearance

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (138):** An angel with bad intentions on a dragon-ish beast, holding a viper in his right hand like a fancy pen. His breath arrives before he does.
- **Translation (132):** Un ángel malintencionado sobre una bestia medio dragón, con una víbora en la diestra como pluma fina. Su aliento llega antes que él.
- **Lost:** "His breath arrives before he does" became "His breath arrives first".
- **Alternatives:** …Su aliento llega antes que él. (142, needs exception).

### es · demon_astaroth_offices[3]

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (44):** Makes you a scholar of all the liberal arts.
- **Translation (42):** Te vuelve un hacha en las artes liberales.
- **Lost:** "Makes you a scholar" became "Teaches you": "erudito" would give the player a gender.
- **Alternatives:** Te vuelve una eminencia en las artes liberales. (47, needs exception).

### es · calling_warlord_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (7):** Warlord
- **Translation (7):** General
- **Lost:** "Warlord" became "General"; "Caudillo" (8) does not fit and has political baggage.
- **Alternatives:** Caudillo (8, needs exception).

### es · calling_menace_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Menace
- **Translation (5):** Plaga
- **Lost:** "Menace" became "Plague" (Plaga): "Amenaza" (7) does not fit.
- **Alternatives:** Amenaza (7, needs exception).

### it · compass_points[6]

- **Status:** open
- **English (4):** West
- **Translation (5):** Ovest
- **Lost:** Nothing: "Ovest" is the only word for West.
- **Alternatives:** None that fit.

### it · calling_oracle_label

- **Status:** open
- **English (6):** Oracle
- **Translation (7):** Oracolo
- **Lost:** Nothing; one character over, like Oráculo in pt-BR/es.
- **Alternatives:** Veggente (8), Profeta (7) — also over.

### it · action_stay

- **Status:** open
- **English (4):** Stay
- **Translation (5):** Resto
- **Lost:** Nothing; "Resto" is the natural word, one character over.
- **Alternatives:** Sto (3) — reads oddly alone.

### it · chant_face

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Face %1$s
- **Translation (9):** Mira %1$s
- **Lost:** Telegraphic: "Mira Nord" has no preposition ("Mira a Nord").
- **Alternatives:** Mira a %1$s (11, needs exception).

### it · escape_banner

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (12):** Seal broken!
- **Translation (9):** Evasione!
- **Lost:** "Seal broken!" became "Jailbreak!" (Evasione!): "Sigillo rotto!" (14) does not fit.
- **Alternatives:** Sigillo rotto! (14, needs exception).

### it · avatar_sealed

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (12):** %1$s, sealed
- **Translation (12):** %1$s, chiuso
- **Lost:** "sealed" became "closed" (chiuso): "sigillato" does not fit (a11y label).
- **Alternatives:** %1$s sigillato (14, needs exception).

### it · chant_sealed

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Sealed
- **Translation (5):** Fatto
- **Lost:** "Sealed" became "Done" (Fatto) in the result chip.
- **Alternatives:** Sigillo (7, noun, needs exception).

### it · verdict_seal_cracked

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (12):** Seal cracked
- **Translation (7):** Crepato
- **Lost:** "Seal cracked" became "It cracked".
- **Alternatives:** Sigillo rotto (13, needs exception).

### it · home_hint

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (16):** Tap the grimoire
- **Translation (16):** Apri il grimorio
- **Lost:** "Tap the grimoire" became "Open the grimoire" (same as home_open).
- **Alternatives:** Tocca il grimorio (17, needs exception).

### it · dossier_weakness

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (8):** Weakness
- **Translation (7):** Tallone
- **Lost:** "Weakness" became "Heel" (Tallone, as in Achilles' heel): "Debolezza" (9) does not fit.
- **Alternatives:** Debolezza (9, needs exception).

### it · dossier_secret

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Secret
- **Translation (6):** Arcano
- **Lost:** "Secret" became "Arcane" (Arcano): "Segreto" (7) does not fit.
- **Alternatives:** Segreto (7, needs exception).

### it · demon_asmodeus_title

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (34):** Lord of Arithmetic & Hidden Wealth
- **Translation (32):** Re dei Conti e dell’Oro Nascosto
- **Lost:** "Lord of Arithmetic & Hidden Wealth" became "King of Accounts and Hidden Gold".
- **Alternatives:** Signore dell'Aritmetica e dei Tesori (36, needs exception).

### it · demon_asmodeus_ability_2_name

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (13):** Now You Don’t
- **Translation (11):** Non Ci Sono
- **Lost:** "Now You Don't" became "I'm Not Here".
- **Alternatives:** Ora Non Mi Vedi (15, needs exception); Chi l'ha visto? (15, the TV show).

### it · demon_asmodeus_weakness_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Name-Drop
- **Translation (9):** Chi Sono?
- **Lost:** "Name-Drop" became "By Name"; the pun is lost.
- **Alternatives:** none found.

### it · demon_astaroth_title

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (30):** Duke of Past, Present & Future
- **Translation (27):** Duca di Ieri, Oggi e Domani
- **Lost:** "Past, Present & Future" became "Yesterday, Today and Tomorrow".
- **Alternatives:** Duca del Passato, Presente e Futuro (35, needs exception).

### it · demon_astaroth_ability_2_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (11):** Open Secret
- **Translation (11):** Radio Serva
- **Lost:** "Open Secret" became "Gossip"; Italian has the exact idiom "Segreto di Pulcinella" (21).
- **Alternatives:** Segreto di Pulcinella (21, needs exception) — the best fit.

### it · demon_astaroth_ability_3_name

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (25):** The Fall (Director’s Cut)
- **Translation (25):** La Caduta: Edizione Lunga
- **Lost:** "Director's Cut" became "Long Edition".
- **Alternatives:** La Caduta (Director's Cut) (26, needs exception).

### it · demon_astaroth_offices[3]

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (44):** Makes you a scholar of all the liberal arts.
- **Translation (38):** Ti rende un drago nelle arti liberali.
- **Lost:** "Makes you a scholar" became "Teaches you": "erudito" would give the player a gender.
- **Alternatives:** Ti fa sapiente in ogni arte liberale. (37) — "sapiente" is invariant; a valid alternative.

### it · calling_warlord_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (7):** Warlord
- **Translation (7):** Tiranno
- **Lost:** "Warlord" became "Tyrant" (Tiranno): "Condottiero" (11) does not fit.
- **Alternatives:** Generale (8, needs exception).

### it · calling_tutor_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (5):** Tutor
- **Translation (4):** Prof
- **Lost:** "Tutor" became "Prof": "Tutore" (6) does not fit.
- **Alternatives:** Tutore (6, needs exception) — also means legal guardian.

### it · calling_menace_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Menace
- **Translation (5):** Peste
- **Lost:** "Menace" became "Pest" (Peste, also "brat"): "Minaccia" (8) does not fit.
- **Alternatives:** Minaccia (8, needs exception).

### fr · secret wording (œ)

- **Status:** changed: restored « un cœur et un foie de poisson » after the font rebuild
- **English:** n/a
- **Translation:** n/a
- **Lost:** French needs œ (cœur, œuvre, sœur); see the "fr · fonts" entry. The French text avoids it: Asmodeus's secret says "fish offal" (abats) instead of "fish heart and liver" (cœur et foie).
- **Alternatives:** Fix the fonts, then restore "la fumée d’un cœur et d’un foie de poisson".

### fr · compass_points[6]

- **Status:** open
- **English (4):** West
- **Translation (5):** Ouest
- **Lost:** Nothing: "Ouest" is the only word for West.
- **Alternatives:** None that fit.

### fr · chant_score

- **Status:** open
- **English (6):** %1$d%%
- **Translation (7):** %1$d %%
- **Lost:** Nothing: French writes a narrow no-break space before % (spec §4).
- **Alternatives:** %1$d%% (6) — fits but is typographically wrong in French.

### fr · chant_face

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Face %1$s
- **Translation (8):** Cap %1$s
- **Lost:** Nautical: "Cap Nord" (heading North) instead of an article-dependent "Face au Nord / à l’Est".
- **Alternatives:** Face : %1$s (11, needs exception).

### fr · chant_heard

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (13):** heard: “%1$s”
- **Translation (12):** ouï « %1$s »
- **Lost:** "heard" became archaic "ouï"; no colon, to fit with « ».
- **Alternatives:** entendu : « %1$s » (18, needs exception).

### fr · escape_banner

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (12):** Seal broken!
- **Translation (9):** Évasion !
- **Lost:** "Seal broken!" became "Jailbreak!" (Évasion !): "Sceau brisé !" (13) does not fit.
- **Alternatives:** Sceau brisé ! (13, needs exception).

### fr · escape_dismiss

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (14):** Tap to dismiss
- **Translation (6):** Fermer
- **Lost:** "Tap to dismiss" became "Close".
- **Alternatives:** Touche pour fermer (18, needs exception).

### fr · home_hint

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (16):** Tap the grimoire
- **Translation (15):** Touche le livre
- **Lost:** "Tap the grimoire" became "Tap the book".
- **Alternatives:** Touche le grimoire (18, needs exception).

### fr · action_stay

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (4):** Stay
- **Translation (3):** Non
- **Lost:** "Stay" became "No" (answer to "Quitter le rituel ?").
- **Alternatives:** Rester (6, needs exception).

### fr · intro_summon

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (6):** Summon
- **Translation (5):** Hop !
- **Lost:** "Summon" became "Hop!" ("Here we go!").
- **Alternatives:** Invoquer (8, needs exception).

### fr · abandon_body

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (21):** Your seals will fade.
- **Translation (20):** Tes sceaux pâliront.
- **Lost:** "Your seals will fade." became "Farewell, your seals."
- **Alternatives:** Tes sceaux vont s’effacer. (26, needs exception).

### fr · dossier_weakness

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (8):** Weakness
- **Translation (5):** Talon
- **Lost:** "Weakness" became "Heel" (Talon, as in talon d’Achille): "Faiblesse" (9) does not fit.
- **Alternatives:** Faiblesse (9, needs exception).

### fr · chant_help

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (12):** How to chant
- **Translation (12):** Bien chanter
- **Lost:** "How to chant" became "Chanting well".
- **Alternatives:** Comment chanter (15, needs exception).

### fr · demon_asmodeus_appearance

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (214):** Three heads (bull, man and ram), so every meeting is a committee. He breathes fire, swishes a serpent’s tail and waddles on goose feet. He arrives on a dragon with a lance and a banner, because subtle is for Dukes.
- **Translation (213):** Trois têtes (taureau, homme, bélier) : chaque réunion tourne au comité. Crache du feu, fouette d’une queue de serpent, se dandine sur des pattes d’oie. Dragon, lance, bannière : la discrétion, c’est pour les Ducs.
- **Lost:** The banner and the waddle are gone; "waddles on goose feet" became "walks on goose feet".
- **Alternatives:** Restore "se dandine sur des pattes d’oie" and "lance et bannière au poing" (233, needs exception).

### fr · demon_asmodeus_restless_quote

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (64):** I've counted every grain of dust in here. Twice. In three bases.
- **Translation (58):** J’ai compté la poussière d’ici. Deux fois. En trois bases.
- **Lost:** "In three bases." became "Three bases."
- **Alternatives:** En trois bases. (65 total, needs exception).

### fr · demon_asmodeus_ability_2_name

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (13):** Now You Don’t
- **Translation (14):** Ni Vu Ni Connu
- **Lost:** "Now You Don't" became "Vanished!"; French has the perfect idiom "Ni vu ni connu" (14).
- **Alternatives:** Ni Vu Ni Connu (14, needs exception) — the best fit.

### fr · demon_asmodeus_weakness_name

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (9):** Name-Drop
- **Translation (8):** Démasqué
- **Lost:** "Name-Drop" became "Unmasked".
- **Alternatives:** none found.

### fr · demon_astaroth_quote

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (77):** I know your past, your future, and that you didn't read the terms of service.
- **Translation (60):** Je sais ton passé, ton futur, et que tu n’as pas lu les CGU.
- **Lost:** "terms of service" became "CGU" (the French acronym everyone ignores).
- **Alternatives:** …les conditions d’utilisation. (80, needs exception).

### fr · demon_astaroth_restless_quote

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (58):** I foresaw you'd forget me. I hate being right. No I don't.
- **Translation (54):** J’avais prévu cet oubli. Je hais avoir raison. Ou pas.
- **Lost:** "No I don't." became "False."
- **Alternatives:** Mensonge. (+4, needs exception).

### fr · demon_astaroth_ability_2_name

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (11):** Open Secret
- **Translation (7):** Commère
- **Lost:** "Open Secret" became "Gossip" (Commère); French has "secret de Polichinelle".
- **Alternatives:** Polichinelle (12, needs exception).

### fr · demon_astaroth_secret

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (126):** Before the dragon and the breath, his name belonged to Astarte, a goddess of love and war. He’d rather you didn’t bring it up.
- **Translation (122):** Avant le dragon et l’haleine, son nom était celui d’Astarte, déesse de l’amour et de la guerre. Il préfère qu’on l’oublie.
- **Lost:** "He'd rather you didn't bring it up" became "Avoid the subject."
- **Alternatives:** Il préfère qu’on n’en parle pas. (needs exception).

### fr · demon_astaroth_offices[3]

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (44):** Makes you a scholar of all the liberal arts.
- **Translation (36):** Fait de toi un as des arts libéraux.
- **Lost:** "Makes you a scholar" became "Teaches you": "savant" would give the player a gender.
- **Alternatives:** none that stay neutral.

### fr · house_sol_tooltip

- **Status:** changed (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (68):** Kings of Sol hold court 9 to noon and 3 to sunset. Bring sunglasses.
- **Translation (68):** Rois de Sol : cour de 9 h à midi, de 15 h au soir. Lunettes noires !
- **Lost:** "Bring sunglasses" became "Glasses!" (Lunettes !); "to sunset" became "to evening".
- **Alternatives:** …Lunettes de soleil ! (needs exception).

### fr · calling_warlord_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (7):** Warlord
- **Translation (7):** Général
- **Lost:** "Warlord" became "General": "Chef de guerre" (14) does not fit.
- **Alternatives:** Chef de guerre (14, needs exception).

### fr · calling_tutor_label

- **Status:** accepted (Claude for the owner, 2026-10-21; Gemini/GPT round, docs/i18n/prompts/)
- **English (5):** Tutor
- **Translation (4):** Prof
- **Lost:** "Tutor" became "Prof": "Tuteur" (6) does not fit.
- **Alternatives:** Tuteur (6, needs exception).

### en/es/pt-BR/it/fr · intro_title

- **Status:** new (2026-10-22)
- **English:** How to\nchant
- **Translations:** es: "Cómo\nrecitar" · pt-BR: "Como\nrecitar" · it: "Come\ncantare" · fr: "Bien\nchanter"
- **Lost:** Explicit line break added for the narrow parchment ribbon slab.

### en/es/pt-BR/it/fr · intro_direction_title

- **Status:** new (2026-10-22)
- **English:** Face
- **Translations:** es: "Mira" · pt-BR: "Vire" · it: "Mira" · fr: "Vise"
- **Lost:** Dropped "1." prefix since step number medallion now displays it.

### en/es/pt-BR/it/fr · intro_chant_title

- **Status:** new (2026-10-22)
- **English:** Chant
- **Translations:** es: "Canta" · pt-BR: "Cante" · it: "Canta" · fr: "Chant"
- **Lost:** Dropped "2." prefix since step number medallion now displays it.

### en/es/pt-BR/it/fr · intro_lines_title

- **Status:** new (2026-10-22)
- **English:** Watch the words
- **Translations:** es: "Sigue el verso" · pt-BR: "Siga o verso" · it: "Segui i versi" · fr: "Suis les mots"
- **Lost:** Reworded to verb phrase for consistency with steps 1 and 2.

### en/es/pt-BR/it/fr · intro_focus_title

- **Status:** new (2026-10-22)
- **English:** Beat the clock
- **Translations:** es: "Gana al reloj" · pt-BR: "Ganhe do tempo" · it: "Batti il tempo" · fr: "Bats l’horloge"
- **Lost:** Reworded to verb phrase for consistency with steps 1, 2, and 3.

### en/es/pt-BR/it/fr · chant_listening

- **Status:** new (2026-10-22)
- **English (9):** Listening
- **Translations:** es: "Oyendo" (6) · pt-BR: "Ouvindo" (7) · it: "Ascolto" (7) · fr: "J’écoute" (8)

### en/es/pt-BR/it/fr · settings_analytics

- **Status:** new (2026-10-23)
- **English (27):** Share anonymous usage data
- **Translations:** es: "Datos de uso anónimos" (22) · pt-BR: "Dados de uso anônimos" (22) · it: "Dati d’uso anonimi" (18) · fr: "Données d’usage anonymes" (24)
- **Lost:** None: concise toggle label, all shorter than English.

### en/es/pt-BR/it/fr · settings_analytics_hint

- **Status:** new (2026-10-23)
- **English (28):** No audio, no personal data.
- **Translations:** es: "Sin audio ni datos tuyos." (26) · pt-BR: "Sem áudio, sem dados seus." (26) · it: "Niente audio né dati tuoi." (26) · fr: "Ni audio ni données perso." (26)
- **Lost:** None: concise hint, all shorter than English.
- **Lost:** None: concise present state on the mic chip, all shorter than English.
