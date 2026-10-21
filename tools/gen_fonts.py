# /// script
# requires-python = ">=3.10"
# dependencies = ["fonttools>=4.47"]
# ///
"""Rebuild the bundled Cinzel and Inter fonts (res/font/*.ttf) from the Google Fonts variable fonts.

Static instances at the weights the app uses, subset to Basic Latin, Latin-1, Latin Extended-A
(œ Œ Ÿ for French, ADR-0031), general punctuation (’ “ ” — … • and the narrow no-break space) plus
every code point the previous files already had, so a rebuild never loses a glyph. Layout
features are kept (tabular figures).

Usage: uv run tools/gen_fonts.py            (writes app/src/main/res/font/)
"""
import io
import sys
import tempfile
import urllib.request
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = Path(__file__).resolve().parent.parent
FONT_DIR = ROOT / "app/src/main/res/font"
BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl"
SOURCES = {
    "cinzel": (f"{BASE}/cinzel/Cinzel%5Bwght%5D.ttf", {"semibold": 600, "bold": 700}),
    "inter": (f"{BASE}/inter/Inter%5Bopsz,wght%5D.ttf", {"regular": 400, "medium": 500, "semibold": 600, "bold": 700}),
}
RANGES = [(0x0020, 0x007E), (0x00A0, 0x017F), (0x2000, 0x206F), (0x20AC, 0x20AC), (0x2122, 0x2122)]


def wanted(existing: set[int]) -> set[int]:
    codes = set(existing)
    for lo, hi in RANGES:
        codes.update(range(lo, hi + 1))
    return codes


def main() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        for family, (url, weights) in SOURCES.items():
            print(f"Downloading {family} …")
            data = urllib.request.urlopen(url, timeout=60).read()
            (Path(tmp) / f"{family}.ttf").write_bytes(data)
            for name, wght in weights.items():
                out = FONT_DIR / f"{family}_{name}.ttf"
                existing = set(TTFont(out).getBestCmap()) if out.exists() else set()
                font = TTFont(io.BytesIO(data))
                axes = {"wght": wght}
                if "opsz" in {a.axisTag for a in font["fvar"].axes}:
                    axes["opsz"] = None  # pin to the axis default
                static = instancer.instantiateVariableFont(font, axes, updateFontNames=False)
                options = subset.Options()
                options.layout_features = ["*"]
                options.name_IDs = ["*"]
                options.notdef_outline = True
                sub = subset.Subsetter(options)
                sub.populate(unicodes=wanted(existing))
                sub.subset(static)
                static.save(out)
                cmap = static.getBestCmap()
                lost = sorted(existing - set(cmap))
                print(f"Wrote {out.relative_to(ROOT)} ({out.stat().st_size // 1024} KB, "
                      f"{len(cmap)} code points, œ={'œ' in map(chr, cmap)}, lost={len(lost)})")
                if lost:
                    sys.exit(f"Lost code points: {[hex(c) for c in lost]}")


if __name__ == "__main__":
    main()
