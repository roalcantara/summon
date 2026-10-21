# Stitch prompts: Grimoire navigation arrows and title banner

Play-testers could not tell how to switch demons on the Grimoire, and the text arrows added for
the RC (‹ ›) are hard to see over the stage art. These assets replace them with gilded arrow
buttons and add a title banner for the Grimoire screen.

**How to use:** paste each prompt into the Summon Demons Safely Stitch project. Attach two of our
existing assets as the style reference, so the new art matches the game:
`art/splash/chant_summon_button_unpressed.png` (gold rim, plum enamel) and
`art/splash/chant_intro_banner.png` (ribbon shape). Generate the large arrow first, then ask for
the small one "in the same style as the previous image".

Save the outputs as PNG with transparency under `art/ui/`, using the file names below.

---

## 1. Large arrow: `grimoire_arrow_large.png` (switch demons)

| Spec | Value |
|---|---|
| Canvas | 384 × 512, transparent background |
| Shape | A **left**-pointing ornate chevron (the right arrow is the same image mirrored in code) |
| Shown at | About 56 × 75 dp, on the left and right edges of the Grimoire stage, over busy scene art |
| Must read as | "Tap or swipe to see the next demon", from arm's length, on bright and dark backgrounds |
| States | One image; pressed and disabled states are done in code (scale and brightness) |

```text
Game UI icon on a fully transparent background, 384x512: a single large LEFT-pointing arrow
button for a cute dark-fantasy mobile RPG. The arrow is a bold, chunky chevron made of polished
imperial gold (#ffc174 highlights, #b8792a shadows) with a raised bevel and a thin dark bronze
outline, like ornate jewellery. Set in the centre of the chevron, a faceted violet gemstone
(#b98cff with a bright white glint) inside a small gold bezel. A soft gold outer glow so it stays
visible on both light and dark backgrounds. The point of the arrow is on the left; the shape is
symmetrical top to bottom. Clean outlines, soft cel shading, stylised 2D fantasy game UI art in
the same style as the attached reference (gold rim, plum and violet tones). Exactly one arrow. No
text, no letters, no numbers, no background, no frame around the image.
```

## 2. Small arrow: `grimoire_arrow_small.png` (turn dossier pages)

| Spec | Value |
|---|---|
| Canvas | 192 × 256, transparent background |
| Shape | The same **left** chevron, simplified: thicker gold, a smaller plain gem, no fine engraving |
| Shown at | About 24 × 32 dp, beside the dossier text on the demon card (dark navy background) |
| Must read as | The same family as the large arrow, still crisp at 24 dp |

```text
In the same style as the previous image, a SMALL simplified version of the same LEFT-pointing
gold chevron arrow, on a fully transparent background, 192x256. Thicker, simpler gold shapes
with a clear bevel, a small round violet gem (#b98cff) in the centre without fine facets, no
engraving or tiny details, so it stays crisp when shown very small (24 pixels wide) on a dark
navy (#171f33) card. Same gold (#ffc174) and dark bronze outline. Exactly one arrow. No text, no
letters, no background.
```

## 3. Title banner: `grimoire_title_banner.png` (Grimoire screen title)

| Spec | Value |
|---|---|
| Canvas | 1200 × 260, transparent background |
| Shape | Horizontal ribbon banner with folded, swallow-tailed ends in the outer 15 % on each side; a flat, evenly lit centre (≈ 70 % of the width) for the title |
| Title | Drawn in code over the centre, never in the art: "GRIMOIRE" (translated: GRIMÓRIO, GRIMORIO), Cinzel Bold, light parchment colour (#ffe7b0) |
| Shown at | About 220 × 48 dp, centred at the top of the Grimoire screen, over a dark arched stone background |
| Stretch | The centre must tolerate slight horizontal stretching |

```text
Wide game UI asset on a fully transparent background, 1200x260: an empty horizontal title
ribbon banner for a cute dark-fantasy mobile RPG. The ribbon is dark slate-navy enamel (#222a3d
to #171f33 vertical gradient) with a raised polished imperial-gold trim (#ffc174) along the top
and bottom edges and a thin dark bronze outline. Both ends fold back into swallow-tailed ribbon
tails in a deep plum (#5a3d7a), with small gold rivets where they fold. A faint gold filigree
flourish at each end of the flat centre, nothing in the middle. Soft drop shadow beneath. The
centre is flat, empty and evenly lit, for a title to be added later. Clean outlines, soft cel
shading, stylised 2D fantasy game UI art in the same style as the attached references. No text,
no letters, no symbols, no background.
```

---

## Integration (after the art is approved)

- **Files:** convert to WebP (lossless) into `app/src/main/res/drawable-nodpi/` as
  `grimoire_arrow_large.webp`, `grimoire_arrow_small.webp`, `grimoire_title_banner.webp`; add them
  to the DESIGN.md asset catalog.
- **Arrows (drop-in, fine for the RC):** `StageArrow` in `ui/screens/GrimoireScreen.kt` and
  `PageButton` in `ui/components/DossierCarousel.kt` draw the image instead of the ‹ › glyph; the
  "next" arrow is the same image with `graphicsLayer { scaleX = -1f }`. Keep the existing
  `IconButton` (48 dp touch target) and content descriptions (`grimoire_previous/next`,
  `dossier_previous/next`). Pressed: scale 0.92 and slight darkening, skipped under reduce motion.
- **Title banner (after the release):** needs a new top-bar layout on the Grimoire (back arrow,
  avatar rail, title, Debug). Add a `grimoire_title` string ("Grimoire", 8 characters) in all five
  languages; text in Cinzel Bold over the banner centre.
