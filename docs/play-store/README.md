# Play Store Graphics

Assets required for the Google Play Store listing (see `docs/STORE_LISTING.md` → Graphics checklist).

## Assets

| Asset | Spec | Location | How to generate |
|---|---|---|---|
| App icon | 512 × 512 PNG, 32-bit, full-bleed opaque, ≤ 1 MB | `icon-512.png` | `mise run icon` |
| Feature graphic | 1024 × 500 PNG, RGB (no alpha) | `feature-graphic.png` | `mise run icon` |
| Feature graphic, with the demons (recommended) | 1024 × 500 PNG, RGB (no alpha) | `feature-graphic-demons.png` | Composed from `stage_home`, `ic_game_logo` and the two demon cut-outs in `res/drawable-nodpi` |
| Phone screenshots | 2–8, 9:16 (1080 × 1920) or 16:9 (1920 × 1080) | `screenshots/0N-*.png` | `uv run tools/gen_store_screenshots.py`: captions, crops and frames over the raw S24 captures in `screenshots/source/` (status bar and navigation handle cropped away) |

## Screenshot Guide

Upload order, captions and what each shows: `docs/STORE_LISTING.md` → Graphics. Raw device captures
are 1080 × 2340, which Play rejects (one side more than twice the other); always upload the framed ones.
