#!/usr/bin/env python3
"""Write docs/i18n/prompts/<locale>.md: a prompt asking another model (Gemini, GPT…) for
alternatives to the open entries of docs/i18n/review.md (ADR-0031).

Entries that are length exceptions are left out: they are correct as they are and go through
docs/i18n/layout-checklist.md instead. Budgets, current text and compass names come from the
string resources, so they are exact.

Usage: python3 -I tools/gen_i18n_prompt.py <locale>     (pt-BR | es | it | fr; run from the repo root)
"""
import re
import sys
import xml.etree.ElementTree as ET

LOCALES = {
    "pt-BR": dict(
        qualifier="pt-rBR", language="Brazilian Portuguese (pt-BR)", culture="Brazilian",
        register='informal and playful, "você" or the imperative',
        gender='no "pronto/pronta", "preparado/preparada"', extra=None),
    "es": dict(
        qualifier="es", language="Spanish (es), one version for Spain and Latin America",
        culture="Spanish-speaking (understood on both sides of the Atlantic)",
        register='informal and playful, "tú"',
        gender='no "listo/lista", "preparado/preparada"', extra=None),
    "it": dict(
        qualifier="it", language="Italian (it)", culture="Italian",
        register='informal and playful, "tu"',
        gender='no "pronto/pronta", "concentrato/concentrata"', extra=None),
    "fr": dict(
        qualifier="fr", language="French (fr)", culture="French-speaking",
        register='informal and playful, "tu"',
        gender='no "prêt/prête", "concentré/concentrée"',
        extra="**French typography:** a narrow no-break space (U+202F) goes before ! ? : ; and %, and "
              "inside « ». It counts as 1 character against the budget."),
}

# Where each string shows and what it is for. {key} refers to the current translation of that key.
WHERE = {
    "chant_face": "Chant screen, direction pill above the Latin line, upper case. Tells the player which compass point to turn the phone to before chanting.",
    "badge_direction_text": "Demon card, text shown when the Direction badge is tapped. %2$s is the next sentence (who the demon serves, e.g. \"{badge_liege}\"). Must use the same verb as chant_face (\"{chant_face}\").",
    "badge_legions_label": "Legions badge on the demon card (\"72 …\"), upper case. The badge is narrow: the English \"72 LEGIONS\" (10 characters) is the widest that fits.",
    "quote_tap_to_summon": "Grimoire stage, cue in a dashed speech bubble under a demon that is not bound yet, upper case. Tapping starts the ritual.",
    "intro_summon": "Button on the \"How to chant\" intro card that starts the ritual. Short, upper case.",
    "dossier_weakness": "Title of the 1st dossier page (\"<title> · <weakness name>\"), upper case.",
    "dossier_secret": "Title of the 6th dossier page, upper case, unlocked only by a flawless summon.",
    "escape_banner": "Big banner on the Home screen when a neglected demon has escaped his seal.",
    "escape_dismiss": "Small hint under the escape banner: tapping closes it.",
    "home_hint": "Hint chip on the Home screen pointing at the grimoire to tap (the Grimoire opens on tap).",
    "avatar_sealed": "Screen-reader label of the avatar of a demon not bound yet (%1$s is his name).",
    "chant_sealed": "Result chip under the Latin line when a line is sealed, upper case, e.g. \"85% · <text> ✓\".",
    "verdict_seal_cracked": "Verdict when the focus ran out and a seal cracked; the player chants the line again.",
    "chant_heard": "Shows what the phone heard after a missed line: \"40% · <text>\", %1$s is the heard text.",
    "chant_help": "Title of the help card and its \"?\" button: how to chant.",
    "action_stay": "Button in the \"Abandon the ritual?\" dialog that keeps the ritual going; the other button is \"{action_abandon}\".",
    "abandon_body": "Second line of the \"Abandon the ritual?\" dialog: the seals already earned will be lost.",
    "demon_asmodeus_title": "Subtitle under ASMODEUS on the demon card; one line, cut with … if too long. He teaches arithmetic and knows where hidden treasure lies.",
    "demon_asmodeus_appearance": "Dossier page \"Appearance\": three heads (bull, man, ram), breathes fire, serpent tail, goose feet, rides a dragon with lance and banner; \"subtle is for Dukes\" is the punchline.",
    "demon_asmodeus_restless_quote": "Speech bubble when the player neglected him: he got so bored he counted the dust twice, in three number bases.",
    "demon_asmodeus_ability_2_name": "Ability name for invisibility; English plays on \"now you see me, now you don't\".",
    "demon_asmodeus_ability_3_line": "Ability line: he finds buried treasure, counts it, then sits on it like a dragon on its hoard.",
    "demon_asmodeus_weakness_name": "Weakness name. The weakness: asked \"Art thou Asmodeus?\" he must admit his name and bow. English \"Name-Drop\" puns on name-dropping.",
    "demon_asmodeus_weakness_line": "Weakness text: ask \"Art thou Asmodeus?\" (archaic on purpose) and he must admit it and bow to the floor; ends \"Three heads, one ego, zero chill.\"",
    "demon_astaroth_title": "Subtitle under ASTAROTH; he knows past, present and future. One line.",
    "demon_astaroth_quote": "His speech bubble: he knows your past, your future, and that you didn't read the terms of service.",
    "demon_astaroth_restless_quote": "Speech bubble when the player neglected him. He predicted it, hates being right, then contradicts himself: \"No I don't.\" is the punchline.",
    "demon_astaroth_appearance": "Dossier page \"Appearance\": an angel with bad intentions on a dragon-like beast, a viper in his right hand like a fancy pen; \"his breath arrives before he does\" is the punchline.",
    "demon_astaroth_ability_2_name": "Ability name; he learns all your secrets and treats them as trivia. English \"Open Secret\" is the idiom.",
    "demon_astaroth_ability_3_name": "Ability name; the long, unrequested story of his fall from heaven. English parodies a film's \"Director's Cut\".",
    "demon_astaroth_secret": "Secret page (flawless summon): his name once belonged to Astarte, a goddess of love and war, and he would rather you didn't bring it up.",
    "demon_astaroth_offices[3]": "Dossier list item: he makes you learned in all the liberal arts. Must not give the player a gender (\"erudito/erudita\").",
    "house_sol_tooltip": "Tooltip of the House badge: the Kings of Sol hold court 9–12 and 15–sunset (real-world hours). \"Sol\" is the planet name and stays as is; a sun pun is welcome.",
    "calling_warlord_label": "Calling badge label (upper case, inside a small badge): summoned for an army.",
    "calling_tutor_label": "Calling badge label (upper case, inside a small badge): summoned to learn things.",
    "calling_menace_label": "Calling badge label (upper case, inside a small badge): summoned at your own risk, a troublemaker.",
    "calling_treasurer_label": "Calling badge label (upper case, inside a small badge): summoned for loot.",
}
FUNCTIONAL = {
    "chant_face", "badge_direction_text", "badge_legions_label", "quote_tap_to_summon", "intro_summon",
    "dossier_weakness", "dossier_secret", "escape_banner", "escape_dismiss", "home_hint", "avatar_sealed",
    "chant_sealed", "verdict_seal_cracked", "chant_heard", "chant_help", "action_stay",
    "calling_warlord_label", "calling_tutor_label", "calling_menace_label", "calling_treasurer_label",
}
DIRECTION = {"chant_face", "badge_direction_text"}
GLOSSARY_ROWS = ["summon (verb)", "seal / sealed", "bound", "restless", "Grimoire", "line (of the chant)",
                 "face (turn to a direction)"]


def load(qualifier):
    texts = {}
    for f in ("strings.xml", "demons.xml"):
        folder = "values" if qualifier is None else f"values-{qualifier}"
        for el in ET.parse(f"app/src/main/res/{folder}/{f}").getroot():
            name = el.get("name")
            plain = lambda t: re.sub(r"\\(.)", r"\1", (t or "").strip('"'))
            if el.tag == "string":
                texts[name] = plain(el.text)
            elif el.tag == "string-array":
                for i, item in enumerate(el):
                    texts[f"{name}[{i}]"] = plain(item.text)
            else:
                for item in el:
                    texts[f"{name}#{item.get('quantity')}"] = plain(item.text)
    return texts


def english_of(en, key):
    if key in en:
        return en[key]
    return max((v for k, v in en.items() if k.startswith(key + "#")), key=len)


def glossary(locale):
    lines = open("docs/i18n/glossary.md").read().splitlines()
    header = next(l for l in lines if l.startswith("| English |"))
    col = [c.strip() for c in header.strip("|").split("|")].index(locale)
    terms = []
    for line in lines:
        cells = [c.strip() for c in line.strip("|").split("|")]
        if cells and cells[0] in GLOSSARY_ROWS:
            terms.append(f"{cells[col]} = {cells[0]}")
    return ", ".join(terms)


def main():
    locale = sys.argv[1]
    cfg = LOCALES[locale]
    en, tr = load(None), load(cfg["qualifier"])
    review = open("docs/i18n/review.md").read()
    exceptions = {tuple(l.split()[:2]) for l in open("app/src/test/resources/i18n/length-exceptions.txt")
                  if l.strip() and not l.startswith("#")}
    keys = [k for k in re.findall(rf"^### {re.escape(locale)} · (\S+)\n\n- \*\*Status:\*\* open", review, re.M)
            if (locale, k) not in exceptions and (k in tr or any(t.startswith(k + "#") for t in tr))]
    unknown = [k for k in keys if k not in WHERE]
    if unknown:
        sys.exit(f"Add a WHERE description for: {unknown}")
    if not keys:
        print(f"{locale}: no open strings outside the length exceptions; prompt left as is")
        return
    compass = [tr[f"compass_points[{i}]"] for i in range(8)]
    context = {n: v for n, v in tr.items() if "[" not in n and "#" not in n}
    context["badge_liege"] = tr["badge_liege"].replace("%1$s", tr["demon_asmodeus_liege"])
    rule = 10 if cfg["extra"] else 9
    out = [f"""# Translation alternatives: {locale}

> Prompt for another model (Gemini, GPT, …). Paste everything below the line. Generated by
> `tools/gen_i18n_prompt.py {locale}` from `docs/i18n/review.md` and the string resources; budgets
> are exact. Bring the YAML reply back to check it against the length test.

---

## Task

You are a game localizer for {cfg['language']}. For each string below, propose **at least 3
alternatives**, best first, that fit the **character budget**.

## The game

"Summon Demons Safely" is a small mobile game with a dark-fantasy look and a comedic tone. The
player picks a cute chibi demon from an old grimoire, turns the phone to face the compass
direction that demon demands, and chants his Latin incantation out loud until four lines are
sealed. Demons have ranks (Great King, Mighty Duke), Houses (Sol, Venus) and Callings. The
humour is dry and silly: demons are smug, petty and over-dramatic.

## Rules

1. **Budget:** an alternative may have at most the given number of characters, counted as a
   person reads them (spaces and punctuation count; an accented letter is 1). Each placeholder
   (`%1$s`, `%2$s`, `%1$d`) counts as **4 characters** and must appear exactly once, unchanged.
2. **Keep the joke, not the words.** A local idiom, pun or slang that lands better is welcome,
   as long as it fits the budget and suits a dark-fantasy comedy.
3. **Never give the player a gender** ({cfg['gender']}). Use imperatives or neutral phrasing.
   Demons are male.
4. **Register:** {cfg['register']}.
5. **Do not translate** demon names (Asmodeus, Astaroth, Amaymon, Astarte), planets (Sol, Venus)
   or Latin.
6. **Fixed terms** (use these words): {glossary(locale)}.
7. **Flavour strings** (demon titles, ability and weakness names, quotes, lore, tooltips) should
   reach for **{cfg['culture']} pop culture**: popular sayings, idioms, classic TV, films, comedy,
   long-lived memes. The target, from the Brazilian Portuguese version: "Dragon Breath" became
   "Bafo de Onça" (an everyday idiom for awful breath), "Open Secret" became "Fofoca" (gossip),
   "The Fall (Director's Cut)" became "Tombo (Versão do Diretor)", "Ring of Virtues" became "O
   Precioso". Limits: the reference must be widely known and lasting (no trend of the month), no
   song lyrics beyond a short phrase, no real people or brands, and the text must still make
   sense to a player who misses the reference.
8. **Munchkin-style names:** for ability and weakness names, the tone of the card game Munchkin
   (in its local edition) is the target: a grand fantasy item or title with a petty, crude or
   everyday twist. Write new names in that spirit; never reuse Munchkin's actual card names.
9. **Functional strings** (buttons, directions, status labels, badge labels) stay plain and
   clear: no jokes a player has to decode to know what to tap or where to turn.""",
    ]
    if cfg["extra"]:
        out.append(f"{rule}. {cfg['extra']}")
    out.append(f"""{rule + 1}. Strings marked **direction** take a compass point in `%1$s`. The alternative must read
   correctly with **all eight**: {", ".join(compass)}. Avoid wording that needs an article or
   preposition that changes per point.

## Output

Reply with YAML only, one block per key, alternatives ordered best first:

```yaml
<key>:
  - text: "…"
    length: <characters, each placeholder counted as 4>
    back_translation: "<literal English>"
    reference: "<the idiom, show or meme it plays on, or none>"
    note: "<why it works>"
```

## Strings
""")
    for k in keys:
        kind = "functional" if k in FUNCTIONAL else "flavour"
        current = tr.get(k) or max((v for t, v in tr.items() if t.startswith(k + "#")), key=len)
        english = english_of(en, k)
        lines = [f"\n### `{k}` ({kind}{', direction' if k in DIRECTION else ''})\n",
                 f"- **English:** `{english}`",
                 f"- **Budget:** {len(english)} characters",
                 f"- **Where it shows:** {WHERE[k].format_map(context)}",
                 f"- **Current {locale}:** `{current}` ({len(current)})"]
        if k in DIRECTION:
            lines.append("- **Check with:** " + ", ".join(f"`{current.replace('%1$s', c)}`" for c in compass[:3]) + " …")
        out.append("\n".join(lines) + "\n")
    path = f"docs/i18n/prompts/{locale}.md"
    open(path, "w").write("\n".join(out))
    print(f"{path}: {len(keys)} strings")


if __name__ == "__main__":
    main()
