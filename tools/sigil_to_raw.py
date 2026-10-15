#!/usr/bin/env python3
"""Convert a stroke-only sigil SVG (art/sigils/*.svg) to the app's raw format (ADR-0009).

The SVG has viewBox "0 0 100 100" and groups <g id="ink-1"> ... <g id="ink-4">, each
holding <path d="..."> elements in draw order. Every path must be one contour (exactly
one moveto) so PathMeasure sees its full length.
Output: one line per path, "<group> <path-data>".
Usage: python3 -I tools/sigil_to_raw.py art/sigils/asmodeus.svg app/src/main/res/raw/sigil_asmodeus.txt
"""
import re
import sys
import xml.etree.ElementTree as ET

NS = "{http://www.w3.org/2000/svg}"


def convert(svg_path):
    root = ET.parse(svg_path).getroot()
    if root.get("viewBox") != "0 0 100 100":
        raise SystemExit(f"{svg_path}: viewBox must be '0 0 100 100'")
    lines = []
    for group in root.iter(NS + "g"):
        match = re.fullmatch(r"ink-([1-4])", group.get("id", ""))
        if not match:
            raise SystemExit(f"{svg_path}: unexpected group id {group.get('id')!r}")
        for path in group.iter(NS + "path"):
            d = " ".join(path.get("d", "").split())
            if len(re.findall(r"[Mm]", d)) != 1:
                raise SystemExit(f"{svg_path}: path must be exactly one contour: {d}")
            lines.append(f"{match.group(1)} {d}")
    if {int(line.split(" ", 1)[0]) for line in lines} != {1, 2, 3, 4}:
        raise SystemExit(f"{svg_path}: groups ink-1..ink-4 must all have paths")
    return lines


def main(src, dst):
    lines = convert(src)
    with open(dst, "w") as out:
        out.write("\n".join(lines) + "\n")
    print(f"{dst}: {len(lines)} strokes")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
