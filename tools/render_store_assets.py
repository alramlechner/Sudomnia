#!/usr/bin/env python3
"""
Renders the Google Play assets from the same geometry as the app icon.

    python3 tools/render_store_assets.py

Produces:
    store/play-icon-512.svg      + .png   512x512, the icon Play shows everywhere
    store/play-feature-1024x500-en/-de.svg + .png   1024x500, one per listing language

### Why this is a script and not two exported files

The mark is defined once, in generate_app_icon.py, as numbers: a 50dp block on a
108dp grid, three filled cells on the diagonal, four colours. Every asset that
shows the mark derives from those numbers. Exported PNGs would be four copies of
a decision, and the copies would be the ones that go stale -- Play's icon is
seen more often than the launcher's.

### What the assets may *not* be

Screenshots. Play requires those to show the real app on a real device, and a
rendered mock-up would misrepresent it -- see store/screenshots/README.md.

### The feature graphic

1024x500, and Play crops it on some surfaces, so nothing that matters may sit
near an edge. It is deliberately quiet: the mark on the left, the name and one
line of claim next to it, on the same navy as the app chrome. A busy banner
competes with the screenshots directly below it, which are the thing that
actually says what the app is.
"""
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "store"

NAVY = "#2B303E"
EMPTY = "#4A5570"
BLUE = "#3193C6"
WHITE = "#FFFFFF"
PAPER = "#F4F1E8"
TEXT_DIM = "#9AA5B8"

# The mark, exactly as in generate_app_icon.py: a 3x3 block, three cells filled
# on the diagonal -- boxes 1, 5 and 9 are the only three sharing no unit, which
# is why the generator seeds them first.
FILLED = {(0, 0): BLUE, (1, 1): WHITE, (2, 2): BLUE}


def mark(x, y, size, gap_ratio=0.035, radius_ratio=0.06):
    """The 3x3 block as SVG rects, top-left at (x, y), `size` across."""
    gap = size * gap_ratio
    cell = (size - 2 * gap) / 3
    r = size * radius_ratio
    out = []
    for row in range(3):
        for col in range(3):
            colour = FILLED.get((row, col), EMPTY)
            cx = x + col * (cell + gap)
            cy = y + row * (cell + gap)
            out.append(
                f'<rect x="{cx:.2f}" y="{cy:.2f}" width="{cell:.2f}" height="{cell:.2f}" '
                f'rx="{r:.2f}" fill="{colour}"/>'
            )
    return "\n  ".join(out)


def icon_svg():
    # Play's icon is shown as a rounded square with no launcher mask, so the mark
    # may be larger than the 50/108 the adaptive icon is limited to. 60 % of the
    # canvas keeps the same optical weight as on the home screen.
    size = 512
    block = size * 0.60
    off = (size - block) / 2
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}">
  <rect width="{size}" height="{size}" fill="{NAVY}"/>
  {mark(off, off, block)}
</svg>
'''


def feature_svg(tagline):
    """
    Stacked and centred, not left-aligned with a text block beside the mark.

    Play crops the feature graphic on several surfaces, and the first attempt --
    mark on the left, name and tagline to its right -- ran the tagline straight
    off the right edge at 1024 px. Anything centred survives a crop from either
    side, and rsvg cannot measure text, so a layout that depends on knowing the
    text width would have to be eyeballed every time the wording changes.
    """
    w, h = 1024, 500
    block = 140
    top = 104
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
  <rect width="{w}" height="{h}" fill="{NAVY}"/>
  {mark((w - block) / 2, top, block)}
  <text x="{w / 2}" y="{top + block + 96}" text-anchor="middle"
        font-family="DejaVu Sans, Helvetica, Arial, sans-serif"
        font-size="76" font-weight="600" fill="{PAPER}">Sudomnia</text>
  <text x="{w / 2}" y="{top + block + 142}" text-anchor="middle"
        font-family="DejaVu Sans, Helvetica, Arial, sans-serif"
        font-size="26" fill="{TEXT_DIM}">{tagline}</text>
</svg>
'''


def write(name, svg, width, height):
    svg_path = OUT / f"{name}.svg"
    png_path = OUT / f"{name}.png"
    svg_path.write_text(svg)
    subprocess.run(
        ["rsvg-convert", str(svg_path), "-w", str(width), "-h", str(height), "-o", str(png_path)],
        check=True,
    )
    print(f"  {svg_path.relative_to(ROOT)}  ->  {png_path.relative_to(ROOT)}  ({width}x{height})")


def main():
    OUT.mkdir(exist_ok=True)
    print("Store-Grafiken:")
    write("play-icon-512", icon_svg(), 512, 512)
    # One per listing language, like the listings themselves.
    write("play-feature-1024x500-en", feature_svg("Sudoku. No guessing. No ads. No account."), 1024, 500)
    write("play-feature-1024x500-de", feature_svg("Sudoku. Kein Raten. Keine Werbung. Kein Konto."), 1024, 500)


if __name__ == "__main__":
    main()
