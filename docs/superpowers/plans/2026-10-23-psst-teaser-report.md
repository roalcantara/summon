# Psst teaser implementation report

## Result
done

## Commits
| Task | Commit | Message |
|---|---|---|
| Task 1 | `7701ee8` | `feat(progress): boundDay and the teaser eligibility rule` |
| Task 2 | `d5e1944` | `feat(analytics): teaser_shown and teaser_tap` |
| Task 3 | `ff7deb4` | `feat(grimoire): Report the teaser (once per demon per day)` |
| Tasks 4 & 5 | `90563c5` | `feat(grimoire): "Psst" balloon fake-door test (ADR-0036)` |
| Task 6 | `c62af75` | `docs(adr): ADR-0036 Psst fake-door test; PRODUCT, DESIGN, playbook` |

## Verification (commands actually run, with their result)
| Command | Exit code / result |
|---|---|
| `./gradlew -q testDebugUnitTest` | exit 0 |
| `./gradlew -q assembleDebug` | exit 0 |
| `./gradlew -q assembleRelease` | exit 0 |
| Task 5 Step 6 (device) | Device `RXCX10467TP` attached. Bound Asmodeus and advanced +1 day via debug sheet. Opened Grimoire and flipped card once (dossier unread cleared). Observed breathing balloon top right of demon with text "Psiu… vem cá." and gold rim / green halo. Observed `teaser_shown {demon=asmodeus, days_since_summon=1}` in Logcat (tag `Analytics`). Tapped balloon: observed reply "Hehehe… Volte logo… se tiver coragem, mortal." for 3 s with chime haptic, and logged `teaser_tap {demon=asmodeus, target=balloon, repeat=false}`. Tapped demon: logged `teaser_tap {demon=asmodeus, target=demon, repeat=true}`. Tested system font scale 2.0: balloon text remained within bounds and auto-sized down. Restored font scale 1.0 (verified `1.0`). |

## Deviations from the plan
- `pt-BR` string for `psst_invite`: Shortened from initial draft `"Psiu… chegue perto."` (18 chars) to `"Psiu… vem cá."` (13 chars) so it satisfies `TranslationResourcesTest` (English has 18 characters, initial pt-BR had 19 characters before trim/punctuation rule count). Updated in `strings.xml` and `docs/i18n/review.md`.

## Left for the operator
- Add a CC0 laugh clip as app/src/main/res/raw/demon_laugh.ogg (optional)

## Open questions
none
