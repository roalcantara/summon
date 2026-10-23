#!/usr/bin/env python3
"""Draw the launcher icon's backdrop (ADR-0030, amended 2026-10-23): a violet glow on the app's
obsidian navy behind the demon's head, with a few faint gold embers. Deterministic (fixed seed).

Usage: python3 -I tools/gen_icon_backdrop.py art/icon/backdrop-violet.png
"""
import math
import os
import random
import sys

for site_path in [
    os.path.expanduser(f"~/Library/Python/{sys.version_info.major}.{sys.version_info.minor}/lib/python/site-packages"),
    os.path.expanduser(f"~/.local/lib/python{sys.version_info.major}.{sys.version_info.minor}/site-packages"),
]:
    if os.path.isdir(site_path) and site_path not in sys.path:
        sys.path.append(site_path)

from PIL import Image, ImageDraw

SIZE = 1024
NAVY = (11, 19, 38)       # DESIGN.md obsidian background
GLOW = (92, 40, 140)      # violet, as the logo and the Grimoire
GOLD = (255, 193, 116)    # DESIGN.md primary
CENTRE = (512, 492)       # behind the head, with the shipped framing (84 dp, raised 10 dp)
REACH = 520
STRENGTH = 1.25
EMBERS = 34


def backdrop():
    im = Image.new("RGB", (SIZE, SIZE), NAVY)
    px = im.load()
    for y in range(SIZE):
        for x in range(SIZE):
            t = (1 - min(1.0, math.hypot(x - CENTRE[0], y - CENTRE[1]) / REACH)) ** 1.6 * STRENGTH
            px[x, y] = tuple(min(255, int(NAVY[i] + (GLOW[i] - NAVY[i]) * t)) for i in range(3))
    im = im.convert("RGBA")
    rnd = random.Random(7)
    draw = ImageDraw.Draw(im, "RGBA")
    for _ in range(EMBERS):
        x, y, r = rnd.randint(150, 870), rnd.randint(150, 870), rnd.choice([3, 4, 5, 6])
        draw.ellipse((x - r, y - r, x + r, y + r), fill=GOLD + (rnd.randint(60, 140),))
    return im.convert("RGB")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    backdrop().save(sys.argv[1])
    print(f"Wrote {sys.argv[1]}")
