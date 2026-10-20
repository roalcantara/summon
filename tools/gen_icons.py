#!/usr/bin/env python3
"""Generate launcher icons and Play Store graphics.

The icons come from one square, opaque, full-bleed image (flat mode), or from a cutout (alpha)
plus a backdrop (layered mode, `--backdrop`). The themed-icon layer is the silhouette of a cutout
(`--mono`, or the icon itself in layered mode). The feature graphic comes from the game logo.

Usage (or `mise run icon`): python3 -I tools/gen_icons.py art/icon/cutout-dark.png app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store --mono art/icon/cutout.png --zoom 84 --shift 10
"""
import os
import sys
# Ensure user/system site-packages are reachable even if invoked with -I (isolated mode)
for site_path in [
    os.path.expanduser(f"~/Library/Python/{sys.version_info.major}.{sys.version_info.minor}/lib/python/site-packages"),
    os.path.expanduser(f"~/.local/lib/python{sys.version_info.major}.{sys.version_info.minor}/site-packages"),
]:
    if os.path.isdir(site_path) and site_path not in sys.path:
        sys.path.append(site_path)

import argparse

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter, ImageOps

# Adaptive icon: 108 dp canvas, of which launchers show the central 72 dp.
# Layered mode: the cutout is drawn at CUTOUT_DP, centred horizontally and resting on the canvas
# bottom (the body runs off the edge), so the face fills the visible area.
# Flat mode: the art is drawn at FLAT_DP, so the visible part keeps ~90 % of it; the margin repeats
# the art's edge pixels so parallax and pulse effects show no seam.
CANVAS_DP = 108
VISIBLE_DP = 72
CUTOUT_DP = 88
FLAT_DP = 80
# Monochrome (themed icon) layer: pixels darker than this are the ink lines, cut out of the
# silhouette so the face still reads in one colour.
INK_LUMA = 70

DENSITY_SCALE = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def make_circular_mask(size, feather=2.0):
    w, h = size
    # Draw at 4x for smooth anti-aliased feathering
    scale = 4
    mask = Image.new("L", (w * scale, h * scale), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, w * scale - 1, h * scale - 1), fill=255)
    if feather > 0:
        mask = mask.filter(ImageFilter.GaussianBlur(feather * scale))
    return mask.resize((w, h), Image.Resampling.LANCZOS)


def make_rounded_mask(size_px, radius_ratio=0.20):
    scale = 4
    mask = Image.new("L", (size_px * scale, size_px * scale), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size_px * scale - 1, size_px * scale - 1), radius=int(size_px * scale * radius_ratio), fill=255
    )
    return mask.resize((size_px, size_px), Image.Resampling.LANCZOS)


def cutout_layer(cutout, canvas_px, art_dp=CUTOUT_DP, shift_dp=0.0):
    """Cutout centred horizontally, resting on the canvas bottom, raised by shift_dp."""
    canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    art_px = round(canvas_px * art_dp / CANVAS_DP)
    shift_px = round(canvas_px * shift_dp / CANVAS_DP)
    scaled = cutout.resize((art_px, art_px), Image.Resampling.LANCZOS)
    layer = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    layer.paste(scaled, ((canvas_px - art_px) // 2, canvas_px - art_px - shift_px), scaled)
    canvas.alpha_composite(layer)
    return canvas


def flat_layer(art, canvas_px, art_dp=FLAT_DP, shift_dp=0.0):
    """Art centred and raised by shift_dp; the margins repeat the art's edge pixels outward, so
    the background and the body running off the bottom continue without a seam."""
    art_px = round(canvas_px * art_dp / CANVAS_DP)
    shift_px = round(canvas_px * shift_dp / CANVAS_DP)
    x0 = (canvas_px - art_px) // 2
    y0 = x0 - shift_px
    scaled = art.resize((art_px, art_px), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (canvas_px, canvas_px))
    canvas.paste(scaled, (x0, y0))
    top, bottom = max(y0, 0), min(y0 + art_px, canvas_px)
    if top > 0:
        canvas.paste(scaled.crop((0, 0, art_px, 1)).resize((art_px, top)), (x0, 0))
    if bottom < canvas_px:
        canvas.paste(scaled.crop((0, art_px - 1, art_px, art_px)).resize((art_px, canvas_px - bottom)), (x0, bottom))
    if x0 > 0:
        canvas.paste(canvas.crop((x0, 0, x0 + 1, canvas_px)).resize((x0, canvas_px)), (0, 0))
        right = x0 + art_px
        canvas.paste(canvas.crop((right - 1, 0, right, canvas_px)).resize((canvas_px - right, canvas_px)), (right, 0))
    return canvas.convert("RGBA")


def monochrome_layer(cutout, canvas_px):
    alpha = cutout.getchannel("A")
    ink = ImageOps.grayscale(cutout.convert("RGB")).point(lambda v: 0 if v < INK_LUMA else 255)
    mono = Image.new("RGBA", cutout.size, (255, 255, 255, 0))
    mono.putalpha(ImageChops.multiply(alpha, ink))
    return cutout_layer(mono, canvas_px)


class Icon:
    """Builds the adaptive layers at any canvas size, from flat art or a cutout over a backdrop."""

    def __init__(self, art, backdrop=None, mono=None, zoom_dp=None, shift_dp=0.0):
        self.art, self.backdrop, self.mono = art, backdrop, mono
        self.zoom_dp = zoom_dp or (FLAT_DP if backdrop is None else CUTOUT_DP)
        self.shift_dp = shift_dp

    def foreground(self, canvas_px):
        if self.backdrop is None:
            return flat_layer(self.art, canvas_px, self.zoom_dp, self.shift_dp)
        return cutout_layer(self.art, canvas_px, self.zoom_dp, self.shift_dp)

    def background(self, canvas_px):
        if self.backdrop is None:
            # Hidden behind the full-bleed foreground; the art's corner colour
            return Image.new("RGB", (canvas_px, canvas_px), self.art.getpixel((0, 0))[:3])
        return self.backdrop.resize((canvas_px, canvas_px), Image.Resampling.LANCZOS)

    def flattened(self, size_px):
        """What a launcher shows: both layers, cropped to the visible 72 dp."""
        canvas_px = round(size_px * CANVAS_DP / VISIBLE_DP)
        layers = self.background(canvas_px).convert("RGBA")
        layers.alpha_composite(self.foreground(canvas_px))
        offset = (canvas_px - size_px) // 2
        return layers.crop((offset, offset, offset + size_px, offset + size_px))


def generate_adaptive_layers(icon, res_dir):
    for density, scale in DENSITY_SCALE.items():
        canvas_px = round(CANVAS_DP * scale)
        out_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)
        layers = {
            "ic_launcher_foreground": icon.foreground(canvas_px),
            "ic_launcher_background": icon.background(canvas_px),
            "ic_launcher_monochrome": monochrome_layer(icon.mono, canvas_px),
        }
        for name, img in layers.items():
            out_path = os.path.join(out_dir, f"{name}.webp")
            img.save(out_path, format="WEBP", lossless=True)
            print(f"Wrote {out_path} ({canvas_px}x{canvas_px})")


def generate_legacy_icons(icon, res_dir):
    """Pre-API-26 launchers (minSdk 24) draw these as-is."""
    for density, scale in DENSITY_SCALE.items():
        size_px = round(48 * scale)
        out_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)
        flat = icon.flattened(size_px)

        sq_img = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
        sq_img.paste(flat, (0, 0), make_rounded_mask(size_px))
        out_sq = os.path.join(out_dir, "ic_launcher.webp")
        sq_img.save(out_sq, format="WEBP", lossless=True)
        print(f"Wrote {out_sq} ({size_px}x{size_px})")

        round_img = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
        round_img.paste(flat, (0, 0), make_circular_mask((size_px, size_px), feather=1.0))
        out_round = os.path.join(out_dir, "ic_launcher_round.webp")
        round_img.save(out_round, format="WEBP", lossless=True)
        print(f"Wrote {out_round} ({size_px}x{size_px})")


def generate_store_icon(icon, store_dir):
    os.makedirs(store_dir, exist_ok=True)
    # 512x512, 32-bit PNG, full bleed opaque; Play applies its own corner mask
    out_path = os.path.join(store_dir, "icon-512.png")
    icon.flattened(512).save(out_path, format="PNG")
    print(f"Wrote {out_path} (512x512)")


def generate_feature_graphic(logo_img, res_dir, store_dir):
    os.makedirs(store_dir, exist_ok=True)
    # 1024x500, RGB (no alpha)
    w, h = 1024, 500
    stage_path = os.path.join(res_dir, "drawable-nodpi", "stage_home.webp")
    if os.path.isfile(stage_path):
        stage = Image.open(stage_path).convert("RGB")
        # Cover-crop stage to 1024x500
        sw, sh = stage.size
        target_ratio = w / h
        src_ratio = sw / sh
        if src_ratio > target_ratio:
            # Source is wider than target
            new_w = int(sh * target_ratio)
            left = (sw - new_w) // 2
            cropped_stage = stage.crop((left, 0, left + new_w, sh))
        else:
            # Source is taller than target
            new_h = int(sw / target_ratio)
            top = (sh - new_h) // 2
            cropped_stage = stage.crop((0, top, sw, top + new_h))
        bg = cropped_stage.resize((w, h), Image.Resampling.LANCZOS)
        # Darken by ~35% (brightness 0.65)
        enhancer = ImageEnhance.Brightness(bg)
        bg = enhancer.enhance(0.65)
    else:
        bg = Image.new("RGB", (w, h), (11, 19, 38))

    # Logo at 440 px height, centred-left, outer 10% clear
    lw, lh = logo_img.size
    target_lh = 440
    target_lw = int(lw * (target_lh / lh))
    logo_rgba = logo_img.convert("RGBA")
    # Feather logo edges slightly
    mask = make_circular_mask((lw, lh), feather=2.0)
    logo_rgba.putalpha(mask)
    scaled_logo = logo_rgba.resize((target_lw, target_lh), Image.Resampling.LANCZOS)

    # Centred-left placement: margin left ~ 60px (outer 10% is ~100px width safe zone)
    # 10% of 1024 is ~102px
    left_pos = 100
    top_pos = (h - target_lh) // 2

    # Paste onto RGB canvas using alpha channel
    bg.paste(scaled_logo, (left_pos, top_pos), scaled_logo)

    out_path = os.path.join(store_dir, "feature-graphic.png")
    bg.save(out_path, format="PNG")
    print(f"Wrote {out_path} ({w}x{h})")



def generate_preview(icon, out_path):
    """Contact sheet: the icon on a dark wallpaper, on the splash colour, and on white, at launcher
    sizes from large to tiny, in squircle and circle masks."""
    sizes = [192, 128, 96, 64, 48, 32]
    pad, row_h = 24, 192 + 48
    width = pad + sum(s + pad for s in sizes)
    grounds = [("dark wallpaper", (8, 14, 40)), ("splash (obsidian)", (11, 19, 38)), ("white", (245, 245, 245))]
    sheet = Image.new("RGB", (width, row_h * len(grounds)), (0, 0, 0))
    draw = ImageDraw.Draw(sheet)
    for row, (label, colour) in enumerate(grounds):
        top = row * row_h
        draw.rectangle((0, top, width, top + row_h), fill=colour)
        ink = (20, 20, 20) if sum(colour) > 380 else (230, 230, 230)
        draw.text((pad, top + 6), label, fill=ink)
        x = pad
        for i, size in enumerate(sizes):
            flat = icon.flattened(size)
            # Splash screens and some launchers use a circle; the rest a squircle
            m = make_circular_mask((size, size), 1.0) if (row == 1 or i % 2) else make_rounded_mask(size, 0.32)
            sheet.paste(flat, (x, top + 28 + (192 - size) // 2), m)
            x += size + pad
    os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
    sheet.save(out_path)
    print(f"Wrote {out_path}")


def load_square(path, mode):
    if not os.path.isfile(path):
        sys.exit(f"Error: {path} not found")
    img = Image.open(path).convert(mode)
    if img.width != img.height:
        sys.exit(f"Error: {path} must be square, got {img.width}x{img.height}")
    return img


def main():
    parser = argparse.ArgumentParser(description="Generate launcher icons and Play Store graphics.")
    parser.add_argument("icon", help="flat art (opaque), or the cutout (alpha) with --backdrop")
    parser.add_argument("logo", help="game logo, source of the feature graphic")
    parser.add_argument("res_dir")
    parser.add_argument("store_dir")
    parser.add_argument("--backdrop", help="layered mode: opaque background layer for the cutout")
    parser.add_argument("--mono", help="cutout (alpha) for the themed-icon layer; default: the icon in layered mode")
    parser.add_argument("--zoom", type=float,
                        help=f"art size in dp on the 108 dp canvas (72 dp visible); default {FLAT_DP} flat, {CUTOUT_DP} layered")
    parser.add_argument("--shift", type=float, default=0.0, help="raise the art by this many dp (negative lowers it)")
    parser.add_argument("--preview", help="also write a contact sheet PNG to this path")
    args = parser.parse_args()

    if args.backdrop:
        art = load_square(args.icon, "RGBA")
        mono = load_square(args.mono, "RGBA") if args.mono else art
        icon = Icon(art, load_square(args.backdrop, "RGB"), mono, args.zoom, args.shift)
    else:
        if not args.mono:
            sys.exit("Error: flat mode needs --mono <cutout.png> for the themed-icon layer")
        icon = Icon(load_square(args.icon, "RGB"), mono=load_square(args.mono, "RGBA"), zoom_dp=args.zoom, shift_dp=args.shift)

    if not os.path.isfile(args.logo):
        sys.exit(f"Error: {args.logo} not found")

    generate_adaptive_layers(icon, args.res_dir)
    generate_legacy_icons(icon, args.res_dir)
    generate_store_icon(icon, args.store_dir)
    generate_feature_graphic(Image.open(args.logo), args.res_dir, args.store_dir)
    if args.preview:
        generate_preview(icon, args.preview)
    print("All icons and graphics generated successfully.")


if __name__ == "__main__":
    main()
