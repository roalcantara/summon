#!/usr/bin/env python3
# /// script
# dependencies = ["pillow"]
# ///
"""Compose the Play Store phone screenshots: a headline, a subline and the cropped screen in a
gilded frame, over a navy backdrop glowing in the demon's accent.

Portrait frames are 1080 x 1920 (9:16), landscape 1920 x 1080 (16:9), as Play requires. The raw
captures (S24, 1080 x 2340) live in docs/play-store/screenshots/source/; the status bar and the
navigation handle are cropped away. Copy is English only (en-US listing).

Usage: uv run tools/gen_store_screenshots.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "docs/play-store/screenshots/source"
OUT = ROOT / "docs/play-store/screenshots"
FONTS = ROOT / "app/src/main/res/font"

NAVY = (11, 19, 38)
DEEP = (6, 14, 32)
GOLD = (255, 193, 116)
TEXT = (218, 226, 253)
ASMODEUS = (255, 213, 79)
ASTAROTH = (224, 64, 251)

# (output, source, landscape, accent, headline, subline, crop box in the raw capture or None)
FRAMES = [
    ("01-summon.png", "celebration.png", False, ASTAROTH, "Summon tiny, smug demons", "Bind them to your Grimoire. They will not be grateful.", None),
    ("02-chant.png", "chant.png", True, ASMODEUS, "Chant his Latin out loud", "The words light up as you speak, like ominous karaoke.", None),
    ("03-face.png", "face.png", True, ASTAROTH, "Turn to face him", "Every demon demands his own direction.", None),
    ("04-grimoire.png", "grimoire.png", False, ASMODEUS, "Collect them all", "Legions, Houses, Callings, and opinions.", None),
    # The tarot card only, so the lore reads larger.
    ("05-dossier.png", "dossier.png", False, ASMODEUS, "Unlock ridiculous lore", "Summon flawlessly to reveal each demon's Secret.", (0, 1560, 1080, 2276)),
    ("06-home.png", "home.png", False, GOLD, "One summon a day", "Visit your demons, or they slip their seals.", None),
]
MARGIN = 72
# The demon shown above a partial screen (the dossier frame).
HERO = "app/src/main/res/drawable-nodpi/demon_asmodeus_cutout.webp"

# Pixels to drop from the raw captures (status bar, navigation handle), per orientation.
CROP_PORTRAIT = (0, 112, 0, 64)   # left, top, right, bottom
CROP_LANDSCAPE = (0, 84, 0, 48)


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONTS / name), size)


def backdrop(size: tuple[int, int], accent: tuple[int, int, int]) -> Image.Image:
    """Navy with a soft accent glow behind the screen."""
    w, h = size
    img = Image.new("RGB", size, DEEP)
    glow = Image.new("RGB", size, NAVY)
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).ellipse((-w * 0.2, h * 0.25, w * 1.2, h * 1.25), fill=255)
    img.paste(glow, mask=mask.filter(ImageFilter.GaussianBlur(min(w, h) // 5)))
    tint = Image.new("RGB", size, accent)
    halo = Image.new("L", size, 0)
    ImageDraw.Draw(halo).ellipse((w * 0.15, h * 0.35, w * 0.85, h * 0.95), fill=70)
    img.paste(tint, mask=halo.filter(ImageFilter.GaussianBlur(min(w, h) // 4)))
    return img


def framed(shot: Image.Image, width: int, radius: int) -> Image.Image:
    """The screen scaled to [width], rounded, with a thin gold rim."""
    h = round(shot.height * width / shot.width)
    shot = shot.resize((width, h), Image.LANCZOS)
    rim = 4
    out = Image.new("RGBA", (width + 2 * rim, h + 2 * rim), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle((0, 0, out.width - 1, out.height - 1), radius + rim, fill=GOLD + (255,))
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, h - 1), radius, fill=255)
    out.paste(shot, (rim, rim), mask)
    return out


def shadow(card: Image.Image, blur: int) -> Image.Image:
    s = Image.new("RGBA", (card.width + 4 * blur, card.height + 4 * blur), (0, 0, 0, 0))
    alpha = card.split()[3].point(lambda a: 150 if a else 0)
    s.paste((0, 0, 0, 255), (2 * blur, 2 * blur), alpha)
    return s.filter(ImageFilter.GaussianBlur(blur))


def centered(d: ImageDraw.ImageDraw, y: int, text: str, f, fill, width: int) -> int:
    x0, y0, x1, y1 = d.textbbox((0, 0), text, font=f)
    d.text(((width - (x1 - x0)) / 2 - x0, y), text, font=f, fill=fill)
    return y + (y1 - y0)


def fitted(d: ImageDraw.ImageDraw, text: str, name: str, size: int, width: int):
    """The largest size up to [size] at which [text] fits [width] (never below 70 %)."""
    floor = round(size * 0.7)
    while size > floor:
        f = font(name, size)
        x0, _, x1, _ = d.textbbox((0, 0), text, font=f)
        if x1 - x0 <= width:
            return f
        size -= 2
    return font(name, floor)


def compose(source: Path, landscape: bool, accent, headline: str, subline: str, box) -> Image.Image:
    raw = Image.open(source).convert("RGB")
    l, t, r, b = CROP_LANDSCAPE if landscape else CROP_PORTRAIT
    shot = raw.crop(box or (l, t, raw.width - r, raw.height - b))
    size = (1920, 1080) if landscape else (1080, 1920)
    img = backdrop(size, accent).convert("RGBA")
    d = ImageDraw.Draw(img)
    head = fitted(d, headline, "cinzel_bold.ttf", 66 if landscape else 72, size[0] - 2 * MARGIN)
    sub = fitted(d, subline, "inter_medium.ttf", 34 if landscape else 38, size[0] - 2 * MARGIN)
    y = centered(d, 56 if landscape else 96, headline, head, GOLD, size[0])
    y = centered(d, y + 26, subline, sub, TEXT, size[0])
    top = y + (44 if landscape else 64)
    # As large as fits under the text and inside the side margins.
    room_w, room_h = size[0] - 2 * MARGIN, size[1] - top - 48
    width = min(room_w, round(room_h * shot.width / shot.height))
    if not landscape and box is None:
        width = min(width, 780)  # a whole phone screen: leave the backdrop some room
    card = framed(shot, width, 28 if landscape else 44)
    x = (size[0] - card.width) // 2
    sh = shadow(card, 24)
    # A partial screen (crop box) sits at the bottom, with the demon's cut-out above it.
    if box is not None:
        hero = Image.open(ROOT / HERO).convert("RGBA")
        room = size[1] - 48 - card.height - top
        hero = hero.resize((round(hero.width * room / hero.height), room), Image.LANCZOS)
        top = size[1] - 48 - card.height
        img.alpha_composite(hero, ((size[0] - hero.width) // 2, top - room + 40))
    img.alpha_composite(sh, (x - 48, top - 36))
    img.alpha_composite(card, (x, top))
    return img.convert("RGB")


def main() -> None:
    for out, src, landscape, accent, headline, subline, box in FRAMES:
        img = compose(SRC / src, landscape, accent, headline, subline, box)
        img.save(OUT / out, optimize=True)
        print(f"{out}: {img.width}x{img.height}")


if __name__ == "__main__":
    main()
