#!/usr/bin/env python3
"""Generate launcher icons and Play Store graphics from the game logo.

Usage: python3 -I tools/gen_icons.py app/src/main/res/drawable-nodpi/ic_game_logo.webp app/src/main/res docs/play-store
"""
import os
import re
import sys
# Ensure user/system site-packages are reachable even if invoked with -I (isolated mode)
for site_path in [
    os.path.expanduser(f"~/Library/Python/{sys.version_info.major}.{sys.version_info.minor}/lib/python/site-packages"),
    os.path.expanduser(f"~/.local/lib/python{sys.version_info.major}.{sys.version_info.minor}/site-packages"),
]:
    if os.path.isdir(site_path) and site_path not in sys.path:
        sys.path.append(site_path)

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter


def parse_obsidian_color(res_dir):
    colors_xml = os.path.join(res_dir, "values", "colors.xml")
    if os.path.isfile(colors_xml):
        with open(colors_xml, "r", encoding="utf-8") as f:
            content = f.read()
        match = re.search(r'<color\s+name="obsidian">#([0-9a-fA-F]+)</color>', content)
        if match:
            hex_val = match.group(1)
            if len(hex_val) == 8:  # AARRGGBB
                return (
                    int(hex_val[2:4], 16),
                    int(hex_val[4:6], 16),
                    int(hex_val[6:8], 16),
                    int(hex_val[0:2], 16),
                )
            elif len(hex_val) == 6:  # RRGGBB
                return (
                    int(hex_val[0:2], 16),
                    int(hex_val[2:4], 16),
                    int(hex_val[4:6], 16),
                    255,
                )
    # Default fallback: #FF0B1326
    return (11, 19, 38, 255)


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


def extract_emblem(logo_img):
    """
    Fit emblem circle (~92% of the logo) into a transparent square,
    feathering the outer corners to transparent.
    """
    w, h = logo_img.size
    emblem_w = int(w * 0.92)
    left = (w - emblem_w) // 2
    top = (h - emblem_w) // 2
    cropped = logo_img.crop((left, top, left + emblem_w, top + emblem_w)).convert("RGBA")

    # Feather outside the emblem circle
    mask = make_circular_mask((emblem_w, emblem_w), feather=2.5)
    cropped.putalpha(mask)
    return cropped


def generate_adaptive_foregrounds(emblem, res_dir):
    # Dp to px mappings for adaptive icon (108 dp canvas, 72 dp safe zone)
    # 72 / 108 = 2/3 ≈ 0.6667
    densities = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432,
    }

    for density, canvas_px in densities.items():
        # Safe zone diameter is 66 dp (the strictest circle mask).
        safe_zone_px = int(canvas_px * (66.0 / 108.0))
        scaled_emblem = emblem.resize((safe_zone_px, safe_zone_px), Image.Resampling.LANCZOS)

        canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
        offset = (canvas_px - safe_zone_px) // 2
        canvas.paste(scaled_emblem, (offset, offset), scaled_emblem)

        out_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "ic_launcher_foreground.webp")
        canvas.save(out_path, format="WEBP", lossless=True)
        print(f"Wrote {out_path} ({canvas_px}x{canvas_px})")


def generate_legacy_icons(emblem, res_dir, obsidian_rgba):
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    for density, size_px in densities.items():
        out_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)

        # 1. Square icon: obsidian background with rounded corners (radius ~20%)
        # Emblem sized at ~85%
        sq_img = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
        sq_mask = Image.new("L", (size_px * 4, size_px * 4), 0)
        sq_draw = ImageDraw.Draw(sq_mask)
        corner_r = int(size_px * 4 * 0.20)
        sq_draw.rounded_rectangle((0, 0, size_px * 4 - 1, size_px * 4 - 1), radius=corner_r, fill=255)
        sq_mask = sq_mask.resize((size_px, size_px), Image.Resampling.LANCZOS)

        bg = Image.new("RGBA", (size_px, size_px), obsidian_rgba)
        sq_img.paste(bg, (0, 0), sq_mask)

        emblem_size = int(size_px * 0.85)
        scaled_emblem = emblem.resize((emblem_size, emblem_size), Image.Resampling.LANCZOS)
        offset = (size_px - emblem_size) // 2
        sq_img.paste(scaled_emblem, (offset, offset), scaled_emblem)

        out_sq = os.path.join(out_dir, "ic_launcher.webp")
        sq_img.save(out_sq, format="WEBP", lossless=True)
        print(f"Wrote {out_sq} ({size_px}x{size_px})")

        # 2. Round icon: circular-masked with transparent corners
        round_img = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
        circ_mask = make_circular_mask((size_px, size_px), feather=1.0)
        round_img.paste(bg, (0, 0), circ_mask)
        round_img.paste(scaled_emblem, (offset, offset), scaled_emblem)

        # Apply circular alpha mask overall to clean edges
        round_alpha = round_img.split()[3]
        combined_alpha = Image.new("L", (size_px, size_px), 0)
        combined_alpha = Image.composite(round_alpha, combined_alpha, circ_mask)
        round_img.putalpha(combined_alpha)

        out_round = os.path.join(out_dir, "ic_launcher_round.webp")
        round_img.save(out_round, format="WEBP", lossless=True)
        print(f"Wrote {out_round} ({size_px}x{size_px})")


def generate_store_icon(emblem, store_dir, obsidian_rgba):
    os.makedirs(store_dir, exist_ok=True)
    # 512x512, 32-bit PNG, full bleed opaque, emblem at ~90% on obsidian
    size = 512
    icon = Image.new("RGBA", (size, size), obsidian_rgba)
    emblem_size = int(size * 0.90)
    scaled_emblem = emblem.resize((emblem_size, emblem_size), Image.Resampling.LANCZOS)
    offset = (size - emblem_size) // 2
    icon.paste(scaled_emblem, (offset, offset), scaled_emblem)

    out_path = os.path.join(store_dir, "icon-512.png")
    icon.save(out_path, format="PNG")
    print(f"Wrote {out_path} ({size}x{size})")


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


def main():
    if len(sys.argv) < 4:
        print("Usage: python3 -I tools/gen_icons.py <logo.webp> <res_dir> <store_dir>")
        sys.exit(1)

    logo_path = sys.argv[1]
    res_dir = sys.argv[2]
    store_dir = sys.argv[3]

    if not os.path.isfile(logo_path):
        print(f"Error: {logo_path} not found")
        sys.exit(1)

    obsidian_rgba = parse_obsidian_color(res_dir)
    print(f"Using obsidian color: {obsidian_rgba}")

    logo_img = Image.open(logo_path)
    emblem = extract_emblem(logo_img)

    generate_adaptive_foregrounds(emblem, res_dir)
    generate_legacy_icons(emblem, res_dir, obsidian_rgba)
    generate_store_icon(emblem, store_dir, obsidian_rgba)
    generate_feature_graphic(logo_img, res_dir, store_dir)
    print("All icons and graphics generated successfully.")


if __name__ == "__main__":
    main()
