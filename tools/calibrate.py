"""Calibra i preset "Moody" sulle foto di riferimento.

Replica in numpy il Grade di Film.kt e cerca i parametri per cui una foto "da telefono" neutra,
sviluppata, ha le stesse statistiche della foto di riferimento: distribuzione della luminanza e
dominante/saturazione per fascia di luce.

Uso: python tools/calibrate.py <neutra.png> <riferimento.png> [<neutra> <rif> ...]
Stampa i parametri trovati, da copiare in Film.kt.
"""
import sys

import numpy as np
from PIL import Image

LUMA = np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)
KEYS = ["exposure", "contrast", "toe", "roll", "fade", "saturation", "shadowSat", "warmth", "tint", "greens", "blues",
        "sr", "sg", "sb", "mr", "mg", "mb", "hr", "hg", "hb"]
RANGES = {
    "exposure": (-1.2, 0.3), "contrast": (-0.3, 0.6), "toe": (0.0, 1.2), "roll": (0.0, 0.4), "fade": (0.0, 0.06),
    "saturation": (0.45, 1.1), "shadowSat": (0.0, 1.0), "warmth": (-0.5, 0.7), "tint": (-0.3, 0.3), "greens": (0.0, 1.0), "blues": (0.0, 1.0),
    "sr": (-0.08, 0.08), "sg": (-0.08, 0.08), "sb": (-0.08, 0.08),
    "mr": (-0.08, 0.08), "mg": (-0.08, 0.08), "mb": (-0.08, 0.08),
    "hr": (-0.15, 0.15), "hg": (-0.15, 0.15), "hb": (-0.15, 0.15),
}


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grade(img, p):
    """Stessa matematica di Grade.apply in Film.kt (tenerle allineate)."""
    r, g, b = [img[..., i].copy() for i in range(3)]
    gain = 2 ** p["exposure"]
    r *= gain * (1 + 0.12 * p["warmth"] + 0.03 * p["tint"])
    g *= gain * (1 - 0.06 * p["tint"])
    b *= gain * (1 - 0.14 * p["warmth"] + 0.03 * p["tint"])
    l = r * LUMA[0] + g * LUMA[1] + b * LUMA[2]
    s = p["saturation"] * (p["shadowSat"] + (1 - p["shadowSat"]) * smooth(0.03, 0.4, l))
    r, g, b = l + (r - l) * s, l + (g - l) * s, l + (b - l) * s
    gx = np.maximum(0, g - np.maximum(r, b))
    g = g - p["greens"] * gx * 0.8
    r = r + p["greens"] * gx * 0.3
    bx = np.maximum(0, b - np.maximum(r, g))
    b = b - p["blues"] * bx * 0.6
    g = g + p["blues"] * bx * 0.35

    def curve(x):
        x = np.maximum(x, 0)
        x = np.where(x > 0.8, 0.8 + (1 - 1 / (1 + (x - 0.8) * 5)) * 0.2, x)
        x = np.minimum(x, 1)
        k = p["contrast"]
        x = x + (x * x * (3 - 2 * x) - x) * k if k >= 0 else 0.5 + (x - 0.5) * (1 + k)
        return np.power(np.clip(x, 0, 1), 1 + p["toe"] * (1 - x))

    r, g, b = curve(r), curve(g), curve(b)
    l = np.clip(r * LUMA[0] + g * LUMA[1] + b * LUMA[2], 0, 1)
    sw, hw = (1 - l) ** 2, l * l
    mw = 1 - sw - hw
    r = r + p["sr"] * sw + p["mr"] * mw + p["hr"] * hw
    g = g + p["sg"] * sw + p["mg"] * mw + p["hg"] * hw
    b = b + p["sb"] * sw + p["mb"] * mw + p["hb"] * hw
    span = 1 - p["fade"] - p["roll"]
    return np.stack([p["fade"] + np.clip(c, 0, 1) * span for c in (r, g, b)], -1)


def stats(img):
    a = img.reshape(-1, 3)
    l = a @ LUMA
    out = list(np.percentile(l, [2, 10, 25, 50, 75, 90, 99]) * 3)
    sat = a.max(1) - a.min(1)
    order = np.argsort(l)
    for part in np.array_split(order, 4):
        c = (a[part] - l[part, None]).mean(0)
        out += list(c * 6) + [sat[part].mean() * 3]
    return np.array(out)


def load(path, w=220):
    im = Image.open(path).convert("RGB")
    if "image" in path:  # screenshot di Reddit: via la barra in basso
        im = im.crop((0, 0, im.width, int(im.height * 0.92)))
    im = im.resize((w, int(im.height * w / im.width)), Image.BILINEAR)
    return np.asarray(im, dtype=np.float32) / 255


def fit(pairs, seed=1):
    rng = np.random.default_rng(seed)
    p = {k: 0.0 for k in KEYS}
    p.update(saturation=1.0, shadowSat=1.0)
    targets = [(src, stats(ref)) for src, ref in pairs]

    def loss(q):
        # Piccola penalità verso i valori neutri: meglio un look credibile che un fit perfetto.
        reg = 0.02 * sum(((q[k] - (1.0 if k in ("saturation", "shadowSat") else 0.0)) / (RANGES[k][1] - RANGES[k][0])) ** 2 for k in KEYS)
        return sum(np.sum((stats(grade(src, q)) - t) ** 2) for src, t in targets) + reg

    best = loss(p)
    step = 0.5
    for it in range(60):
        improved = False
        for k in rng.permutation(KEYS):
            lo, hi = RANGES[k]
            for d in (step, -step):
                q = dict(p)
                q[k] = float(np.clip(p[k] + d * (hi - lo), lo, hi))
                v = loss(q)
                if v < best:
                    best, p, improved = v, q, True
        if not improved:
            step *= 0.5
            if step < 0.004:
                break
    return p, best


if __name__ == "__main__":
    args = sys.argv[1:]
    pairs = [(load(args[i]), load(args[i + 1])) for i in range(0, len(args), 2)]
    p, err = fit(pairs)
    print(f"errore {err:.4f}")
    print(", ".join(f"{k}={v:.3f}" for k, v in p.items()))
    if len(pairs) == 1:
        out = (np.clip(grade(pairs[0][0], p), 0, 1) * 255).astype(np.uint8)
        Image.fromarray(out).save("calibrated.png")
