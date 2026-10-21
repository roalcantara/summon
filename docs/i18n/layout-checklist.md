# Layout checklist for accepted translations

Translations that are correct as they are but longer than English (the entries in
`app/src/test/resources/i18n/length-exceptions.txt`), and the spots where hard cases show. Each
item says where the text renders, how to reach that state, and what "pass" means. Every item
is written so a test can check it (see "Automation" below); until that test exists, tick them by
hand on a device.

**Pass** = the text is fully visible: not wrapped where English stays on one line, not cut, no
"…", and nothing else on screen moves or overlaps.

## Items

| # | Locale | Key | Text | Where it renders | How to reach it | Checked |
|---|---|---|---|---|---|---|
| 1 | pt-BR | `compass_points[2]` | Leste | Direction badge label on Asmodeus's card; the badge explanation ("Mire Leste e invoque…"); the chant direction pill ("MIRE LESTE") | Grimoire → Asmodeus; tap the Direction badge; start the ritual | [x] badge (emulator, 2026-10-20) · [ ] pill |
| 2 | pt-BR | `compass_points[6]` | Oeste | Same three places, for a demon facing West | No demon faces West yet: render the components with heading 270° (test) | [ ] |
| 3 | pt-BR | `calling_oracle_label` | Oráculo | Calling badge label on Astaroth's card; the explanation header when the badge is selected | Grimoire → Astaroth; tap the Calling badge | [ ] |
| 4 | es | `compass_points[6]` | Oeste | As item 2 | As item 2 | [ ] |
| 5 | es | `calling_oracle_label` | Oráculo | As item 3 | As item 3 | [x] badge (emulator, 2026-10-20) · [ ] header |
| 6 | es | `chant_sealed` | Sellado | Result chip under the Latin line ("SELLADO ✓"), with the score ("85% · SELLADO ✓") | Ritual: seal a line | [ ] |
| 7 | it | `compass_points[6]` | Ovest | As item 2 | As item 2 | [ ] |
| 8 | it | `calling_oracle_label` | Oracolo | As item 3 | As item 3 | [x] badge (emulator, 2026-10-20) · [ ] header |
| 9 | it | `action_stay` | Resto | "Stay" button of the abandon dialog, next to "Lascia" | Ritual → back → abandon dialog | [ ] |
| 10 | fr | `compass_points[6]` | Ouest | As item 2 | As item 2 | [ ] |
| 11 | fr | `chant_score` | 85 % | Score in the result chip ("85 % · SCELLÉ ✓") and in a missed line ("40 % · ouï « … »") | Ritual: seal a line; miss a line | [ ] |
| 12 | es | `badge_legions_label` | 72 huestes | Legions badge (the narrowest badge: "72 LEGIONES" wrapped here) | Grimoire → Asmodeus | [x] (emulator, 2026-10-20) |
| 13 | all | `compass_points[*]` | Longest point per locale: Nordeste/Noroeste (pt-BR, es), Nord-Ovest (it), Nord-Ouest (fr) | Chant direction pill and Direction badge | Heading 45°/315° (test) | [ ] |
| 14 | fr | `demon_asmodeus_ability_2_name` | Ni Vu Ni Connu | Abilities dossier page, bold name inline before its line; wraps freely | Grimoire → Asmodeus (bound) → dossier page 2 | [ ] |

## Automation

The check is "no text in the component overflows its box", for every locale. Two layers, cheapest
first.

1. **Text overflow test (recommended, no screenshots to read).** A Compose UI test renders each
   component above with the real string resources of one locale, then, for every node that
   exposes `SemanticsActions.GetTextLayoutResult`, calls it and asserts
   `!result.hasVisualOverflow` and `result.lineCount == 1` where the English text is one line
   (badges, pill, chips, buttons). Parameterise it over `en, es, pt-BR, it, fr` and the eight
   compass headings. It fails on exactly the bug the device found ("72 LEGIONES"), with the key
   and locale in the message.
   - Run on the JVM with Robolectric (`@Config(qualifiers = "+pt-rBR")`) and
     `androidx.compose.ui:ui-test-junit4`, so it runs inside `./gradlew testDebugUnitTest` with no
     device; or as an instrumented test on the emulator, wrapping the content in a
     `LocalContext` built with `createConfigurationContext(locale)`.
   - Components (all exist today): `BadgeRow` and `InfoBand` in `ui/components/Badges.kt` for
     both demons and every selected badge; `DirectionPillView` (`ChantPieces.kt`) at headings
     0–315°; `FeedbackSlot` (`ChantPieces.kt`) with `SlotContent.Sealed(85)` and
     `SlotContent.Missed(40, "…")`; the abandon dialog in `RitualScreen.kt`; `ChantIntro`.
2. **Screenshots for a human (optional).** Add Roborazzi to the same Robolectric tests to save one
   PNG per component and locale under `app/build/outputs/roborazzi/`. They are for reviewing
   wording in context, not for pass/fail; layer 1 decides.

Until then: on the emulator, `adb shell "setprop persist.sys.locale <tag>; setprop ctl.restart
zygote"` switches the language (Android 7 image, root), the debug panel ("D") reaches every
state, and `adb exec-out screencap -p` captures it.
