#!/usr/bin/env python3
"""Render Corner Pub motion design reel (9:16) from brand frames + photos."""

from __future__ import annotations

import math
import os
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "src/main/resources/static/videos"
FRAMES = Path("/tmp/corner-motion-frames")
W, H = 1080, 1920
FPS = 30
DURATION = 16.0  # seconds
TOTAL = int(DURATION * FPS)

GOLD = (232, 197, 71)
GOLD_SOFT = (255, 225, 168)
WHITE = (255, 255, 255)
CREAM = (255, 244, 220)

FONT_BRAND = "/usr/share/fonts/truetype/noto/NotoSerif-Bold.ttf"
FONT_BODY = "/usr/share/fonts/truetype/macos/Inter-SemiBold.ttf"
FONT_LIGHT = "/usr/share/fonts/truetype/macos/Inter-Regular.ttf"

ASSETS = [
    ROOT / "src/main/resources/static/videos/assets/corner-motion-burger.jpg",
    ROOT / "src/main/resources/static/videos/assets/corner-motion-panini.jpg",
    ROOT / "src/main/resources/static/videos/assets/corner-motion-beer.jpg",
    ROOT / "src/main/resources/static/images/slide2.jpg",
    ROOT / "src/main/resources/static/images/slide3.jpg",
]


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def ease_out_cubic(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return 1 - (1 - t) ** 3


def ease_in_out(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return 0.5 - 0.5 * math.cos(math.pi * t)


def load_cover(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGB")
    scale = max(W / im.width, H / im.height)
    nw, nh = int(im.width * scale * 1.12), int(im.height * scale * 1.12)
    return im.resize((nw, nh), Image.Resampling.LANCZOS)


def crop_zoom(im: Image.Image, progress: float, drift: float = 0.0) -> Image.Image:
    # progress 0..1 ken burns; drift shifts focus vertically
    zoom = 1.0 + 0.08 * progress
    cw, ch = int(W * zoom), int(H * zoom)
    cw = min(cw, im.width)
    ch = min(ch, im.height)
    cx = im.width // 2 + int((im.width - cw) * 0.08 * math.sin(progress * math.pi))
    cy = im.height // 2 + int((im.height - ch) * (0.12 * drift + 0.05 * progress))
    left = max(0, min(im.width - cw, cx - cw // 2))
    top = max(0, min(im.height - ch, cy - ch // 2))
    return im.crop((left, top, left + cw, top + ch)).resize((W, H), Image.Resampling.LANCZOS)


def vignette(base: Image.Image, strength: float = 0.55) -> Image.Image:
    """Darken edges with a soft radial mask."""
    mask = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(mask)
    for i in range(1, 21):
        t = i / 20
        alpha = int(255 * strength * (t ** 1.6))
        inset_x = int((W * 0.42) * t)
        inset_y = int((H * 0.38) * t)
        x0, y0 = inset_x, inset_y
        x1, y1 = W - inset_x, H - inset_y
        if x1 <= x0 or y1 <= y0:
            continue
        d.ellipse([x0, y0, x1, y1], fill=alpha)
    overlay = Image.new("RGB", (W, H), (0, 0, 0))
    return Image.composite(overlay, base, mask)


def gradient_bg() -> Image.Image:
    im = Image.new("RGB", (W, H))
    px = im.load()
    for y in range(H):
        t = y / (H - 1)
        r = int(12 + (28 - 12) * t)
        g = int(10 + (18 - 10) * t)
        b = int(8 + (12 - 8) * t)
        for x in range(W):
            # subtle side warmth
            edge = abs(x - W / 2) / (W / 2)
            rr = min(255, int(r + 18 * (1 - edge) * (1 - t)))
            gg = min(255, int(g + 10 * (1 - edge) * (1 - t)))
            px[x, y] = (rr, gg, b)
    return im


def draw_centered_text(draw: ImageDraw.ImageDraw, text: str, y: int, fnt, fill, stroke=0, stroke_fill=None):
    bbox = draw.textbbox((0, 0), text, font=fnt)
    tw = bbox[2] - bbox[0]
    x = (W - tw) // 2
    kwargs = {"font": fnt, "fill": fill}
    if stroke:
        kwargs["stroke_width"] = stroke
        kwargs["stroke_fill"] = stroke_fill or (0, 0, 0)
    draw.text((x, y), text, **kwargs)
    return tw, bbox[3] - bbox[1]


def alpha_paste(base: Image.Image, layer: Image.Image, alpha: float):
    if alpha <= 0:
        return base
    if alpha >= 1:
        return layer
    return Image.blend(base, layer, alpha)


def scene_open(t: float) -> Image.Image:
    """0-3.2s brand reveal."""
    im = gradient_bg()
    # warm flare
    flare = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(flare)
    pulse = 0.5 + 0.5 * math.sin(t * 2.2)
    r = int(220 + 80 * pulse)
    fd.ellipse([W // 2 - r, H * 0.28 - r // 2, W // 2 + r, H * 0.28 + r // 2], fill=(232, 197, 71, 28))
    im = Image.alpha_composite(im.convert("RGBA"), flare).convert("RGB")

    draw = ImageDraw.Draw(im)
    kicker_a = ease_out_cubic((t - 0.15) / 0.6)
    if kicker_a > 0:
        f = font(FONT_BODY, 28)
        text = "PIAZZA DUOMO 58  ·  GIOVINAZZO"
        # fade via color
        c = tuple(int(v * kicker_a) for v in GOLD_SOFT)
        draw_centered_text(draw, text, int(H * 0.22 + (1 - kicker_a) * 24), f, c)

    brand = "CORNER"
    f_brand = font(FONT_BRAND, 168)
    # measure full width for centering letters
    gaps = []
    total_w = 0
    for ch in brand:
        bb = draw.textbbox((0, 0), ch, font=f_brand)
        w = bb[2] - bb[0]
        gaps.append(w)
        total_w += w
    total_w += 8 * (len(brand) - 1)
    x0 = (W - total_w) // 2
    y_brand = int(H * 0.34)
    x = x0
    for i, ch in enumerate(brand):
        local = ease_out_cubic((t - 0.35 - i * 0.07) / 0.55)
        if local <= 0:
            x += gaps[i] + 8
            continue
        yy = y_brand + int((1 - local) * 50)
        col = tuple(int(255 * local if c == 255 else c * local) for c in ((255, 255, 255) if i % 2 == 0 else GOLD))
        # always gold-white mix: all white with gold glow via stroke
        draw.text(
            (x, yy),
            ch,
            font=f_brand,
            fill=(255, 255, 255, ),
            stroke_width=2,
            stroke_fill=(int(40 * local), int(30 * local), 0),
        )
        # gold underline grow later
        x += gaps[i] + 8

    line_a = ease_out_cubic((t - 1.1) / 0.5)
    if line_a > 0:
        lw = int(220 * line_a)
        y = y_brand + 190
        draw.rectangle([W // 2 - lw, y, W // 2 + lw, y + 4], fill=GOLD)

    sub_a = ease_out_cubic((t - 1.35) / 0.55)
    if sub_a > 0:
        f = font(FONT_BODY, 42)
        c = tuple(int(v * sub_a) for v in GOLD)
        draw_centered_text(draw, "Hamburgeria & Pub", int(H * 0.52 + (1 - sub_a) * 20), f, c)

    tag_a = ease_out_cubic((t - 1.8) / 0.55)
    if tag_a > 0:
        f = font(FONT_LIGHT, 34)
        c = tuple(int(v * tag_a) for v in CREAM)
        draw_centered_text(draw, "Panini · Birre · Tanto altro", int(H * 0.58 + (1 - tag_a) * 16), f, c)

    return vignette(im, 0.45)


def scene_photo(base_img: Image.Image, t_local: float, duration: float, title: str, subtitle: str, drift: float) -> Image.Image:
    progress = t_local / max(duration, 0.001)
    im = crop_zoom(base_img, ease_in_out(progress), drift=drift)
    # darken lower third for type
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for y in range(int(H * 0.55), H):
        a = int(200 * ((y - H * 0.55) / (H * 0.45)) ** 1.2)
        od.line([(0, y), (W, y)], fill=(0, 0, 0, a))
    im = Image.alpha_composite(im.convert("RGBA"), overlay).convert("RGB")
    im = vignette(im, 0.35)

    draw = ImageDraw.Draw(im)
    enter = ease_out_cubic(t_local / 0.7)
    exit_t = ease_out_cubic((duration - t_local) / 0.45) if t_local > duration - 0.45 else 1.0
    a = enter * exit_t
    if a <= 0:
        return im

    f1 = font(FONT_BRAND, 72)
    f2 = font(FONT_LIGHT, 34)
    c1 = tuple(int(v * a) for v in WHITE)
    c2 = tuple(int(v * a) for v in GOLD_SOFT)
    y1 = int(H * 0.72 + (1 - enter) * 30)
    draw_centered_text(draw, title, y1, f1, c1, stroke=3, stroke_fill=(0, 0, 0))
    draw_centered_text(draw, subtitle, y1 + 90, f2, c2, stroke=2, stroke_fill=(0, 0, 0))
    # gold rule
    lw = int(140 * a)
    draw.rectangle([W // 2 - lw, y1 - 24, W // 2 + lw, y1 - 20], fill=tuple(int(v * a) for v in GOLD))
    return im


def scene_end(t: float) -> Image.Image:
    im = gradient_bg()
    flare = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(flare)
    fd.ellipse([W // 2 - 340, int(H * 0.38) - 180, W // 2 + 340, int(H * 0.38) + 180], fill=(232, 197, 71, 36))
    im = Image.alpha_composite(im.convert("RGBA"), flare).convert("RGB")
    draw = ImageDraw.Draw(im)

    a1 = ease_out_cubic(t / 0.6)
    f = font(FONT_BRAND, 120)
    c = tuple(int(255 * a1) for _ in range(3))
    draw_centered_text(draw, "CORNER", int(H * 0.34 + (1 - a1) * 24), f, c)

    a2 = ease_out_cubic((t - 0.35) / 0.55)
    if a2 > 0:
        f2 = font(FONT_BODY, 36)
        draw_centered_text(draw, "Vieni a trovarci", int(H * 0.48), f2, tuple(int(v * a2) for v in GOLD))
        f3 = font(FONT_LIGHT, 30)
        draw_centered_text(
            draw,
            "Piazza Duomo 58 · Giovinazzo",
            int(H * 0.54),
            f3,
            tuple(int(v * a2) for v in CREAM),
        )

    a3 = ease_out_cubic((t - 0.8) / 0.5)
    if a3 > 0:
        f4 = font(FONT_BODY, 28)
        draw_centered_text(
            draw,
            "Panini artigianali  ·  Birre  ·  Pub",
            int(H * 0.64),
            f4,
            tuple(int(v * a3) for v in GOLD_SOFT),
        )
        # CTA pill
        label = "CORNERPUBGIOVINAZZO.COM"
        f5 = font(FONT_BODY, 26)
        bb = draw.textbbox((0, 0), label, font=f5)
        tw, th = bb[2] - bb[0], bb[3] - bb[1]
        pad_x, pad_y = 36, 18
        x0 = (W - tw) // 2 - pad_x
        y0 = int(H * 0.72)
        rect = [x0, y0, x0 + tw + pad_x * 2, y0 + th + pad_y * 2]
        # rounded-ish via rectangle
        fill = (int(232 * a3), int(197 * a3), int(71 * a3))
        draw.rounded_rectangle(rect, radius=28, fill=fill)
        draw.text((x0 + pad_x, y0 + pad_y), label, font=f5, fill=(20, 14, 4))

    return vignette(im, 0.4)


def crossfade(a: Image.Image, b: Image.Image, t: float) -> Image.Image:
    return Image.blend(a, b, max(0.0, min(1.0, t)))


def pick_assets() -> list[Image.Image]:
    loaded = []
    for p in ASSETS:
        if p.exists():
            loaded.append(load_cover(p))
    if len(loaded) < 3:
        # fallback slides
        for name in ("slide2.jpg", "slide3.jpg", "slide4.jpg", "slide5.jpg"):
            p = ROOT / "src/main/resources/static/images" / name
            if p.exists():
                loaded.append(load_cover(p))
    if not loaded:
        raise SystemExit("No image assets found")
    while len(loaded) < 3:
        loaded.append(loaded[0])
    return loaded[:3]


def render():
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    photos = pick_assets()
    print(f"Rendering {TOTAL} frames @ {FPS}fps…")

    # timeline (seconds)
    # 0.0-3.2 open
    # 3.0-7.2 photo1 (overlap fade)
    # 6.9-11.1 photo2
    # 10.8-14.0 photo3
    # 13.6-16.0 end
    open_end = 3.2
    p1_start, p1_dur = 3.0, 4.2
    p2_start, p2_dur = 6.9, 4.2
    p3_start, p3_dur = 10.8, 3.2
    end_start = 13.6

    for i in range(TOTAL):
        t = i / FPS
        layers = []

        if t <= open_end + 0.35:
            layers.append((scene_open(t), 1.0 if t < open_end else 1.0 - (t - open_end) / 0.35))

        if p1_start <= t <= p1_start + p1_dur:
            tl = t - p1_start
            img = scene_photo(photos[0], tl, p1_dur, "Panini artigianali", "Preparati ogni giorno", drift=-0.3)
            a = 1.0
            if tl < 0.35:
                a = tl / 0.35
            if tl > p1_dur - 0.35:
                a = (p1_dur - tl) / 0.35
            layers.append((img, max(0.0, min(1.0, a))))

        if p2_start <= t <= p2_start + p2_dur:
            tl = t - p2_start
            img = scene_photo(photos[1], tl, p2_dur, "Smash & gourmet", "Sapori da angolo", drift=0.4)
            a = 1.0
            if tl < 0.35:
                a = tl / 0.35
            if tl > p2_dur - 0.35:
                a = (p2_dur - tl) / 0.35
            layers.append((img, max(0.0, min(1.0, a))))

        if p3_start <= t <= p3_start + p3_dur:
            tl = t - p3_start
            img = scene_photo(photos[2], tl, p3_dur, "Birre selezionate", "L’atmosfera del pub", drift=0.1)
            a = 1.0
            if tl < 0.35:
                a = tl / 0.35
            if tl > p3_dur - 0.35:
                a = (p3_dur - tl) / 0.35
            layers.append((img, max(0.0, min(1.0, a))))

        if t >= end_start:
            layers.append((scene_end(t - end_start), 1.0 if t > end_start + 0.3 else (t - end_start) / 0.3))

        frame = Image.new("RGB", (W, H), (8, 8, 8))
        for img, a in layers:
            frame = alpha_paste(frame, img, a)

        frame.save(FRAMES / f"frame_{i:05d}.jpg", quality=90, optimize=True)
        if i % 30 == 0:
            print(f"  {i}/{TOTAL}")

    out = OUT_DIR / "corner-motion-design.mp4"
    preview = OUT_DIR / "corner-motion-design-preview.jpg"
    # mid frame preview
    shutil.copy(FRAMES / f"frame_{TOTAL // 2:05d}.jpg", preview)

    cmd = [
        "ffmpeg", "-y",
        "-framerate", str(FPS),
        "-i", str(FRAMES / "frame_%05d.jpg"),
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-crf", "18",
        "-movflags", "+faststart",
        str(out),
    ]
    print("Encoding", out)
    subprocess.run(cmd, check=True)
    print("Done:", out, "size=", out.stat().st_size)


if __name__ == "__main__":
    render()
