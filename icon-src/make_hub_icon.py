#!/usr/bin/env python3
"""Hub launcher icon for 1.5: Adam's approved orb scene
(icon-proposals/hub-proposal-3.png), processed with the family's
established make_icons recipe (the Alarms/Steps pipeline, as used
for the 1.4 module icons): the full art as the legacy icon per
density, the art at 68% of the 108dp adaptive canvas with
alpha-feathered square edges as the foreground (4x supersample),
and the adaptive background colour sampled from the art's plate.

Sampling note: the recipe samples points just inside the art's
edges; the bottom-centre point is skipped here because the orb's
light pool sits there, so the two bottom corners stand in for it.

Run from anywhere:  python3 icon-src/make_hub_icon.py
Writes into ../res relative to this script.
"""
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
HUB = os.path.dirname(HERE)
RES = os.path.join(HUB, "res")
SRC = os.path.join(HUB, "icon-proposals", "hub-proposal-3.png")

img = Image.open(SRC).convert("RGB")
w, h = img.size
print("master size", w, h)

samples = [img.getpixel((w // 2, int(h * 0.03))),
           img.getpixel((int(w * 0.04), h // 2)),
           img.getpixel((int(w * 0.96), h // 2)),
           img.getpixel((8, 8)), img.getpixel((w - 9, 8)),
           img.getpixel((8, h - 9)), img.getpixel((w - 9, h - 9))]
bg = tuple(sum(s[i] for s in samples) // len(samples) for i in range(3))
print("sampled BG", bg, "#%02X%02X%02X" % bg)
master = img.convert("RGBA")

# --- foreground at 68%, feathered (the family recipe) --------------------
FRACTION = 0.68
SS = 4
canvas_hi = 432 * SS
art_hi = int(round(canvas_hi * FRACTION))
feather = max(8, int(art_hi * 0.12))
art = master.resize((art_hi, art_hi), Image.LANCZOS)
mask = Image.new("L", (art_hi, art_hi), 0)
d = ImageDraw.Draw(mask)
inset = feather // 2
d.rectangle([inset, inset, art_hi - inset - 1, art_hi - inset - 1], fill=255)
mask = mask.filter(ImageFilter.GaussianBlur(feather / 2.0))
art.putalpha(mask)
fg_hi = Image.new("RGBA", (canvas_hi, canvas_hi), (0, 0, 0, 0))
off = (canvas_hi - art_hi) // 2
fg_hi.alpha_composite(art, (off, off))

DENSITIES = {"mipmap-mdpi": (48, 108), "mipmap-hdpi": (72, 162),
             "mipmap-xhdpi": (96, 216), "mipmap-xxhdpi": (144, 324),
             "mipmap-xxxhdpi": (192, 432)}
for folder, (legacy, fg) in DENSITIES.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)
    fg_hi.resize((fg, fg), Image.LANCZOS).save(
        os.path.join(out_dir, "ic_launcher_foreground.png"))
    master.resize((legacy, legacy), Image.LANCZOS).convert("RGB").save(
        os.path.join(out_dir, "ic_launcher.png"))
    print("wrote", folder)

# --- Pixel mask preview ----------------------------------------------------
PREVIEW = 192
BG = (bg[0], bg[1], bg[2], 255)


def apply_mask(imgc, kind):
    m = Image.new("L", (PREVIEW * SS, PREVIEW * SS), 0)
    px = m.load()
    s = PREVIEW * SS
    for y in range(s):
        for x in range(s):
            nx = (x + 0.5) / s * 2 - 1
            ny = (y + 0.5) / s * 2 - 1
            inside = (nx * nx + ny * ny <= 1.0) if kind == "circle" \
                else (abs(nx) ** 4 + abs(ny) ** 4 <= 1.0)
            px[x, y] = 255 if inside else 0
    m = m.resize((PREVIEW, PREVIEW), Image.LANCZOS)
    out = imgc.copy()
    out.putalpha(m)
    return out


def render(kind):
    bgimg = Image.new("RGBA", (PREVIEW, PREVIEW), BG)
    comp = Image.alpha_composite(
        bgimg, fg_hi.resize((PREVIEW, PREVIEW), Image.LANCZOS))
    return apply_mask(comp, kind)


preview = Image.new("RGBA", (PREVIEW * 2 + 24, PREVIEW + 16), (24, 24, 32, 255))
preview.alpha_composite(render("circle"), (8, 8))
preview.alpha_composite(render("squircle"), (16 + PREVIEW, 8))
preview.save(os.path.join(HUB, "icon-preview-1.5.png"))
print("wrote icon-preview-1.5.png")
print("BG #%02X%02X%02X" % bg)
