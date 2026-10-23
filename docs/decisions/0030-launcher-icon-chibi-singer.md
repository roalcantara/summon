# ADR-0030: Launcher icon from the singing chibi demon on obsidian

- **Status:** Accepted; amended 2026-10-23 (violet backdrop)
- **Date:** 2026-10-20
- **Amends:** ADR-0017 (release prep: launcher and store icon)

## Context

The launcher icon was the game logo's circular emblem ("SUMMON DEMONS SAFELY" lettering) on
obsidian. At launcher size the lettering is unreadable and the icon says nothing about the game.

New art shows a chibi demon singing into a mic. Three versions were tried on a Samsung phone
(dark night-sky wallpaper, smallest icon size) and on an emulator (white drawer):

1. `art/icon/full.png`: the demon in front of a glowing rune ring on dark navy. Beautiful on
   white; on the dark wallpaper the navy matched the sky and the icon lost its edge.
2. Two layers, `art/icon/cutout.png` (alpha) over `art/icon/backdrop.png` (gold-to-plum glow):
   good contrast everywhere, but much less charming.
3. The demon alone on black: it read well, but did not match the splash screen, which shows the
   launcher icon on `obsidian` (`#0B1326`).

## Decision

1. The icon art is `art/icon/cutout-dark.png`: the demon alone, no ring, on a flat `obsidian`
   background, so the icon and the splash screen are one surface.
2. Adaptive icon: the art is the foreground, drawn at 84 of the 108 dp canvas and raised 10 dp so
   the face fills the visible 72 dp; the margins repeat the art's edge pixels, so the body runs
   off the bottom and parallax shows no seam. The background layer is solid `obsidian`.
3. Themed icon (monochrome layer): the silhouette of `cutout.png` (same pose) with its dark ink
   lines cut out, so the face reads in one colour. It replaces the hand-drawn sigil-ring vector.
4. Legacy icons (API 24–25) and the 512 × 512 Play icon are the layers flattened and cropped to
   the visible 72 dp, so every surface shows the same framing.
5. `mise run icon` rebuilds all of it with `tools/gen_icons.py` and writes a preview sheet (dark
   wallpaper, splash colour, white; 192 to 32 px). `--zoom`, `--shift`, `--backdrop` and
   `--install` make further experiments one command each.
6. `full.png` and `backdrop.png` stay in the repo as reference art; no build step reads them.

## Consequences

- The demon's dark outline and gold glow carry the contrast on dark wallpapers; the obsidian
  square carries it on light ones.
- The splash screen shows the demon on its own background with no visible disc.
- The icon matches the chant intro singer (`chant_intro_singer`), so it promises the game's core
  action.

## Amendment (2026-10-23): violet backdrop, layered

The icon is layered again: the demon cutout (`art/icon/cutout.png`, 84 dp, raised 10 dp) over
`art/icon/backdrop-violet.png`, a violet glow on obsidian navy behind his head with a few faint gold
embers, drawn by `tools/gen_icon_backdrop.py` (deterministic). Compared on launcher masks at
160–32 px on dark, light and warm wallpapers against the flat navy icon, a gold summoning ring, a
ring with sigil marks and no background: the violet glow keeps the red demon clear at every size,
ties the icon to the app's palette, and stays predictable on any wallpaper (no background did not:
red on warm wallpapers, and some launchers put it on a white plate). `mise run icon` with no
arguments rebuilds this icon.
