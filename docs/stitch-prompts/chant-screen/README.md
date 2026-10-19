# Chant screen: ten single-proposal Stitch prompts

One prompt per request: each file is self-contained and produces **one** proposal as six
separate 800 × 360 landscape screens (states A–F). Run them one at a time and look at each at
full size.

Attach to each request only the demon's sigil (SVG sources in `art/sigils/`; Asmodeus for
01, 03, 04, 06, 07, 09; Astaroth for 02, 05, 08, 10) and optionally that demon's House stage
background. Do not attach screenshots of the current screen.

What changed from `../2026-10-18-chant-screen-redesign.md` (which produced one tiny sheet of
ten near-unreadable proposals, and invented scores, XP, grades, Hz and phonemes): one proposal
per request; exact content for every state (real Latin line, lit syllables, heard words, score,
heard text); a closed list of what may appear on screen plus an explicit "do not add" list;
"not yet" instead of failure language; six full screens instead of a spec sheet.

Round 2 (after proposal 01) adds four mandatory layout rules: each fact in exactly one place;
? top-left and ✕ alone top-right; score and heard text next to the line; the sigil itself shows
the direction and the proximity. Regenerate with `python3 -I tools/gen_stitch_prompts.py`.

Stitch's exported `screen.png` files come out cropped (the screen sits below the capture), so
judge proposals from `code.html` rendered at 800 × 360, e.g. with headless Chrome:
`"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --hide-scrollbars --force-device-scale-factor=2 --window-size=800,360 --screenshot=out.png file:///…/code.html`

| # | Concept | Demon |
|---|---|---|
| 01 | Split stage, refined | Asmodeus |
| 02 | Sigil-centred | Astaroth |
| 03 | Compass first | Asmodeus |
| 04 | Teleprompter | Asmodeus |
| 05 | Candle circle | Astaroth |
| 06 | Grimoire pages | Asmodeus |
| 07 | Card stack | Asmodeus |
| 08 | Minimal HUD over the stage | Astaroth |
| 09 | Path to the demon | Asmodeus |
| 10 | Wild card | Astaroth |
