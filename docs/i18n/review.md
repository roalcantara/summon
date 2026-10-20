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
