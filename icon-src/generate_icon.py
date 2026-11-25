#!/usr/bin/env python3
"""Generates every icon asset for Oguri Hub.

Launcher icon (1.1): Oguri herself, from her real avatar artwork
(icon-src/oguri-avatar-source.png, the master render also used for
the dashboard lurker sprite). The white studio background is removed
by flood fill from the image borders (interior whites - halter top,
gloves, eye whites - are enclosed by outlines and survive), the
result is cropped to a head-and-shoulders portrait and laid over the
hub plate colour (#0D2030, the home shell's background) as a
sibling-style adaptive icon.

Module icons: each module's original standalone launcher icon is
restored into the hub resources under a collision-safe name
(ic_mod_<module>), copied from the original projects' res trees
(chips only ships single-size art, so its sizes are resampled).

Run from anywhere:  python3 icon-src/generate_icon.py
Writes into ../res relative to this script.
"""

import os
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
HUB = os.path.dirname(HERE)
RES = os.path.join(HUB, "res")
WORKSPACE = os.path.dirname(HUB)

PLATE = (13, 32, 48)  # #0D2030, hub shell background / ic_bg

DENSITIES = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
LEGACY_SIZES = [48, 72, 96, 144, 192]        # 48dp at each density
FOREGROUND_SIZES = [108, 162, 216, 324, 432]  # 108dp at each density

# Head-and-shoulders crop in the 1600x1880 source: ears and headband
# at the top, chin and collar at the bottom, face centred on x=800.
CROP = (100, 30, 1500, 1430)


def cut_out_background(img):
    """Return the artwork as RGBA with the border-connected white
    background made transparent."""
    rgb = img.convert("RGB")
    a = np.asarray(rgb).astype(np.int16)
    near_white = (a.min(axis=2) > 242)
    labels, count = ndimage.label(near_white)
    border = set()
    border.update(np.unique(labels[0, :]))
    border.update(np.unique(labels[-1, :]))
    border.update(np.unique(labels[:, 0]))
    border.update(np.unique(labels[:, -1]))
    border.discard(0)
    background = np.isin(labels, list(border))
    alpha = np.where(background, 0, 255).astype(np.uint8)
    alpha_img = Image.fromarray(alpha, "L").filter(
        ImageFilter.GaussianBlur(1.2))
    out = rgb.convert("RGBA")
    out.putalpha(alpha_img)
    return out


def build_launcher_icon():
    src = Image.open(os.path.join(HERE, "oguri-avatar-source.png"))
    portrait = cut_out_background(src).crop(CROP)
    master = 864  # 2x the xxxhdpi foreground, downsampled per density
    portrait = portrait.resize((master, master), Image.LANCZOS)

    legacy_master = Image.new("RGBA", (master, master), PLATE + (255,))
    legacy_master.alpha_composite(portrait)

    for density, legacy_px, fg_px in zip(
            DENSITIES, LEGACY_SIZES, FOREGROUND_SIZES):
        folder = os.path.join(RES, "mipmap-" + density)
        portrait.resize((fg_px, fg_px), Image.LANCZOS).save(
            os.path.join(folder, "ic_launcher_foreground.png"))
        legacy_master.resize((legacy_px, legacy_px), Image.LANCZOS).save(
            os.path.join(folder, "ic_launcher.png"))
    print("launcher icon: Oguri portrait over plate #0D2030")

    # Preview: the legacy master masked as a circle and as a squircle,
    # the two shapes launchers most commonly apply.
    size = 432
    base = legacy_master.resize((size, size), Image.LANCZOS)
    circle_mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(circle_mask).ellipse([0, 0, size, size], fill=255)
    squircle_mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(squircle_mask).rounded_rectangle(
        [0, 0, size, size], radius=size // 5, fill=255)
    preview = Image.new("RGBA", (size * 2 + 24, size), (24, 24, 28, 255))
    circ = base.copy()
    circ.putalpha(circle_mask)
    squir = base.copy()
    squir.putalpha(squircle_mask)
    preview.alpha_composite(circ, (0, 0))
    preview.alpha_composite(squir, (size + 24, 0))
    preview.save(os.path.join(HUB, "icon-preview-1.1.png"))
    print("wrote icon-preview-1.1.png")


# module -> (original project res dir, background colour)
MODULES = {
    "alarms": ("alarm-app", "#260C5C"),
    "steps": ("steps-app", "#1E0032"),
    "printer": ("printer-app", "#260A5B"),
    "vpn": (os.path.join("android-apk", "oguri-vpn-switch"), "#1B2748"),
    "chips": (os.path.join("poker-chips-app", "app"), "#0A211D"),
}

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_mod_{name}_bg" />
    <foreground android:drawable="@mipmap/ic_mod_{name}_foreground" />
</adaptive-icon>
"""


def build_module_icons():
    for name, (project, _bg) in MODULES.items():
        src_res = os.path.join(WORKSPACE, project, "res")
        for density, legacy_px, fg_px in zip(
                DENSITIES, LEGACY_SIZES, FOREGROUND_SIZES):
            folder = os.path.join(RES, "mipmap-" + density)
            if name == "chips":
                # Chips ships one 512px foreground and one 512px
                # legacy icon; resample to each density.
                fg_src = Image.open(os.path.join(
                    src_res, "drawable", "ic_foreground.png"))
                fg_src.resize((fg_px, fg_px), Image.LANCZOS).save(
                    os.path.join(folder, f"ic_mod_{name}_foreground.png"))
                lg_src = Image.open(os.path.join(
                    src_res, "mipmap", "ic_launcher.png"))
                lg_src.resize((legacy_px, legacy_px), Image.LANCZOS).save(
                    os.path.join(folder, f"ic_mod_{name}.png"))
            else:
                shutil.copyfile(
                    os.path.join(src_res, "mipmap-" + density,
                                 "ic_launcher_foreground.png"),
                    os.path.join(folder, f"ic_mod_{name}_foreground.png"))
                shutil.copyfile(
                    os.path.join(src_res, "mipmap-" + density,
                                 "ic_launcher.png"),
                    os.path.join(folder, f"ic_mod_{name}.png"))
        xml_dir = os.path.join(RES, "mipmap-anydpi-v26")
        with open(os.path.join(xml_dir, f"ic_mod_{name}.xml"), "w") as f:
            f.write(ADAPTIVE_XML.format(name=name))
        print(f"module icon restored: ic_mod_{name}")


if __name__ == "__main__":
    build_launcher_icon()
    build_module_icons()
