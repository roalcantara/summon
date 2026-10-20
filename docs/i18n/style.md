# Translation style

Applies to every string in `values-*/strings.xml` and `values-*/demons.xml`, the store listings
and the privacy policy (i18n spec §6).

1. **Never longer than English**, counted in characters as `TranslationResourcesTest` counts
   them. If it cannot fit without losing the point, write the best version that does fit, add an
   entry to `docs/i18n/review.md`, and only then consider a length exception.
2. **Keep the joke, not the words.** Each line is comedy first. Replace an untranslatable pun
   with a local one of the same kind; log the swap in the review queue.
3. **Never give the player a gender.** Use imperatives and neutral phrasing ("Volte amanhã",
   not "Você está pronto"). Demons are male; agree with him.
4. **Leave alone:** demon names (Asmodeus, Astaroth, Amaymon, Astarte), planets (Sol, Venus),
   the Latin chant, "Summon Demons Safely".
5. **Glossary terms are fixed** (`docs/i18n/glossary.md`); use them everywhere, same form.
6. **Direction sentences** (`chant_face`, `badge_direction_text`, `chant_a11y_*`) must read
   correctly with all eight compass points; prefer phrasings with no article ("Cap : %1$s").
   Check with `DirectionSentencesReport`.
7. **Punctuation:** French « », a narrow no-break space before ! ? : ; and %; Spanish ¡ ¿;
   typographic apostrophes (’) as in the English source. Escape a straight apostrophe as `\'`.
8. **Register:** informal and playful (tu in French/Italian/Spanish; você in pt-BR), matching
   the English tone.
9. **Placeholders** (`%1$s`, `%2$d`) stay exactly as in English; their order in the sentence may
   change.
