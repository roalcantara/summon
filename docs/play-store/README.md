# Play Store Graphics

Assets required for the Google Play Store listing (see `docs/STORE_LISTING.md` → Graphics checklist).

## Assets

| Asset | Spec | Location | How to generate |
|---|---|---|---|
| App icon | 512 × 512 PNG, 32-bit, full-bleed opaque, ≤ 1 MB | `icon-512.png` | `python3 -I tools/gen_icons.py app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store` |
| Feature graphic | 1024 × 500 PNG, RGB (no alpha) | `feature-graphic.png` | `python3 -I tools/gen_icons.py app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store` |
| Phone screenshots | 2–8 portrait images (9:16, 1080 × 1920 recommended) | `screenshots/` | Captured on device / emulator following the screenshot order in `docs/STORE_LISTING.md` |

## Screenshot Guide

Recommended sequence from `docs/STORE_LISTING.md`:
1. Home — *"The grimoire is restless."*
2. Grimoire, sealed demon — *"Pick your demon. Read the fine print."*
3. Ritual, locked and chanting — *"Face his direction. Chant out loud."*
4. Climax / "New Demon Received!" — *"Bind him. Try not to gloat."*
5. Demon card with dossier — *"Six pages of lore. One of them is a secret."*
6. Restless demon / escape banner — *"Neglect him and he escapes."*
