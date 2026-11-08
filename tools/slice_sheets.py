"""Ritaglia i fogli generati con ChatGPT (branding/film/sheet-*.png) in un'immagine per pellicola.

Uso: python tools/slice_sheets.py  (dalla cartella del progetto)
Ogni foglio è una griglia di oggetti su sfondo trasparente: righe e colonne si trovano dalle
proiezioni dell'alpha, poi ogni ritaglio viene ripulito dagli aloni e ridimensionato.
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

SHEETS = {
    "sheet-a": ["trip35", "ae1", "fm2", "m6", "lca", "t4", "k1000", "t2"],
    "sheet-b": ["x700", "ql17", "pop", "zenit", "mju2", "gr1", "sureshot", "f3"],
    "sheet-c": ["purple", "pentax17", "colorplus", "holga", "rz67", "brownie", "instamatic", "super8"],
    "sheet-d": ["sx70", "p600", "instax", "funsaver"],
    "sheet-e": ["golden", "teal", "moody", "bleach", "noir", "night", "matrix", "pastel", "vision500", "techni"],
    "sheet-f": ["dusk", "oak", "canyon", "lastlight", "tealroom", "coast"],
}
OUT = Path("app/src/main/res/drawable-nodpi")
HEIGHT = 300


def runs(profile, min_len):
    """Tratti consecutivi in cui la proiezione è piena."""
    out, start = [], None
    for i, v in enumerate(list(profile) + [False]):
        if v and start is None:
            start = i
        elif not v and start is not None:
            if i - start >= min_len:
                out.append((start, i))
            start = None
    return out


def slice_sheet(path, names):
    im = Image.open(path).convert("RGBA")
    a = np.array(im)[..., 3]
    solid = a > 160
    rows = runs(solid.sum(axis=1) > 3, 40)
    boxes = []
    for y0, y1 in rows:
        cols = runs(solid[y0:y1].sum(axis=0) > 3, 40)
        for x0, x1 in cols:
            sub = solid[y0:y1, x0:x1]
            ys = np.where(sub.any(axis=1))[0]
            boxes.append((x0, y0 + ys[0], x1, y0 + ys[-1] + 1))
    assert len(boxes) == len(names), f"{path}: trovati {len(boxes)} oggetti, attesi {len(names)}"
    for name, (x0, y0, x1, y1) in zip(names, boxes):
        pad = 6
        crop = im.crop((max(0, x0 - pad), max(0, y0 - pad), min(im.width, x1 + pad), min(im.height, y1 + pad)))
        r, g, b, alpha = crop.split()
        # Via aloni e frange: alpha netto, poi eroso di un pixel e ammorbidito.
        alpha = alpha.point(lambda v: 255 if v > 160 else 0).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.7))
        crop = Image.merge("RGBA", (r, g, b, alpha))
        k = HEIGHT / crop.height
        crop = crop.resize((max(1, round(crop.width * k)), HEIGHT), Image.LANCZOS)
        crop.save(OUT / f"film_{name}.webp", "WEBP", quality=88, method=6)
        print(name, crop.size)


if __name__ == "__main__":
    for sheet, names in SHEETS.items():
        slice_sheet(Path("branding/film") / f"{sheet}.png", names)
