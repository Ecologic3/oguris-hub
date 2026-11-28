#!/usr/bin/env python3
"""Apply the Hub module-icon glow finish to a flat-plate proposal.

House style (alarms-final, chips-glow2): the flat background plate gets a
strong soft radial glow, brightest behind the character's head, falling
off to darker-than-plate corners. The character herself is untouched;
only background pixels (segmented by distance to the corner plate color)
are relit, with a feathered mask so the bloom hugs her silhouette.

Usage: apply_glow.py in.png out.png
"""
import sys

import numpy as np
from PIL import Image, ImageFilter


def main() -> None:
    src = Image.open(sys.argv[1]).convert("RGB")
    w, h = src.size
    img = np.asarray(src).astype(np.float32)

    # Plate color: median of the four corners.
    corners = np.concatenate([
        img[:24, :24].reshape(-1, 3), img[:24, -24:].reshape(-1, 3),
        img[-24:, :24].reshape(-1, 3), img[-24:, -24:].reshape(-1, 3),
    ])
    plate = np.median(corners, axis=0)

    # Background mask by color distance to the plate, feathered.
    dist = np.linalg.norm(img - plate, axis=2)
    mask = Image.fromarray((np.clip((60.0 - dist) / 40.0, 0, 1) * 255).astype(np.uint8))
    mask = mask.filter(ImageFilter.GaussianBlur(6))
    bg = np.asarray(mask).astype(np.float32) / 255.0

    # Radial glow field, centered behind the head (upper middle).
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    cx, cy = 0.5 * w, 0.40 * h
    r = np.sqrt(((xx - cx) / (0.72 * w)) ** 2 + ((yy - cy) / (0.72 * h)) ** 2)
    glow = np.exp(-(r ** 2) * 1.9)          # 1 at center -> ~0 at corners
    shade = 0.68 + 0.32 * np.clip(glow * 2.2, 0, 1)  # corners at 68%

    # Relit plate: center blends toward a warm light tint of the plate,
    # corners darken. Applied to the whole frame, masked to background.
    tint = plate + (np.array([255.0, 244.0, 230.0], np.float32) - plate) * 0.62
    relit = plate[None, None, :] * shade[:, :, None] \
        + (tint - plate)[None, None, :] * (glow ** 1.4)[:, :, None] * 0.85
    out = img * (1 - bg[:, :, None]) + relit * bg[:, :, None]

    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(sys.argv[2])
    print("wrote", sys.argv[2])


if __name__ == "__main__":
    main()
