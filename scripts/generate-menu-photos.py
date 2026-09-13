#!/usr/bin/env python3
"""Genera foto PNG realistiche per i piatti del menù e le carica sul sito.

Le immagini sono generate da AI a partire da nome, categoria e descrizione:
non sono scatti veri del locale.

Esempi (dal Mac, nella cartella corner-pub-backend):

  # Vedi cosa farebbe, senza creare né caricare nulla
  python3 scripts/generate-menu-photos.py --dry-run

  # Solo i piatti la cui foto attuale non si apre (consigliato)
  OPENAI_API_KEY=sk-... \\
  CORNER_ADMIN_USER=staff \\
  CORNER_ADMIN_PASSWORD='***' \\
  python3 scripts/generate-menu-photos.py --provider openai

  # Gratis, qualità più bassa
  CORNER_ADMIN_USER=staff CORNER_ADMIN_PASSWORD='***' \\
  python3 scripts/generate-menu-photos.py --provider pollinations --limit 5
"""

from __future__ import annotations

import argparse
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from http.cookiejar import CookieJar
from pathlib import Path

BASE_DEFAULT = "http://cornerpubgiovinazzo.com"
OUT_DIR = Path(__file__).resolve().parent / "generated-photos"

BRAND_PROMPTS = {
    "sprite": "a cold lemon-lime soda in a tall glass with ice and condensation, no logos",
    "coca": "a cold dark cola in a glass with ice and condensation, no logos",
    "fanta": "a cold orange soda in a glass with ice, no logos",
    "red bull": "a cold energy drink poured over ice in a glass, no logos",
    "pepsi": "a cold cola in a glass with ice, no logos",
    "acqua": "a clear glass bottle of still water with condensation on a dark bar counter",
}

CATEGORY_HINTS = {
    "panini": "gourmet smash burger or artisan sandwich, toasted bun, melted cheese, close-up 45-degree food photography",
    "wraps": "fresh wrap cut in half to show the filling, on a dark ceramic plate",
    "sfizi": "pub side dish, rustic ceramic plate, warm lighting",
    "dolci": "plated dessert, appetizing, shallow depth of field",
    "bevande": "cold drink in a glass on a dark wooden pub counter, no logos",
    "birre": "fresh pint of craft beer with foam, condensation, pub lighting, no logos",
    "vino": "glass of wine on a dark wooden table, no labels",
    "fritti": "golden fried food on parchment, crispy texture, pub style",
    "starter": "appetizer plate, restaurant photography",
    "insalat": "fresh composed salad, vibrant, overhead three-quarter view",
    "combo": "pub combo platter with burger fries and a drink, no logos",
    "bombette": "grilled Apulian bombette, juicy meat rolls, rustic plate",
    "polpette": "Italian meatballs with sauce, rustic plate",
    "carne": "grilled steak or plated meat, restaurant photography",
    "pinse": "pinsa romana, irregular oval crust, toppings, wood board",
    "toast": "french toast, golden, powdered sugar, brunch photography",
}


def category_hint(name: str) -> str:
    value = (name or "").lower()
    for key, hint in CATEGORY_HINTS.items():
        if key in value:
            return hint
    return "realistic restaurant food photography, dark pub atmosphere"


def build_prompt(item: dict) -> str:
    title = (item.get("titolo") or "dish").strip()
    desc = (item.get("descrizione") or "").strip()
    cat = (item.get("categoryName") or "").strip()
    lower = f"{title} {desc}".lower()

    subject = None
    for brand, prompt in BRAND_PROMPTS.items():
        if brand in lower:
            subject = prompt
            break

    if subject is None:
        details = desc if desc else title
        subject = (
            f"the Italian pub dish named {title}: {details}. "
            f"{category_hint(cat)}"
        )

    return (
        "Photorealistic professional food photograph, square 1:1, natural lighting, "
        "dark wooden pub table, shallow depth of field, appetizing, no text, "
        "no watermark, no logos, no hands, no people, PNG photo look. "
        f"{subject}"
    )


def has_working_image(base: str, url: str) -> bool:
    value = (url or "").strip()
    if not value or "about-img" in value:
        return False
    full = value if value.startswith("http") else f"{base}{value}"
    try:
        req = urllib.request.Request(full, method="GET")
        with urllib.request.urlopen(req, timeout=4) as response:
            if response.status != 200:
                return False
            ctype = (response.headers.get("Content-Type") or "").lower()
            data = response.read(64)
            return "image" in ctype and len(data) > 8
    except Exception:
        return False


def multipart_body(json_data: dict, filename: str, image: bytes, content_type: str) -> tuple[bytes, str]:
    boundary = "----CornerPhoto" + secrets.token_hex(16)
    payload = json.dumps(json_data, ensure_ascii=False).encode("utf-8")
    chunks = [
        f"--{boundary}\r\n".encode(),
        b'Content-Disposition: form-data; name="data"\r\n',
        b"Content-Type: application/json\r\n\r\n",
        payload,
        b"\r\n",
        f"--{boundary}\r\n".encode(),
        (
            f'Content-Disposition: form-data; name="image"; filename="{filename}"\r\n'
        ).encode(),
        f"Content-Type: {content_type}\r\n\r\n".encode(),
        image,
        b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ]
    return b"".join(chunks), boundary


class CornerClient:
    def __init__(self, base: str):
        self.base = base.rstrip("/")
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(CookieJar())
        )

    def request(self, path: str, data: bytes | None = None, headers: dict | None = None, method: str | None = None):
        req = urllib.request.Request(
            self.base + path,
            data=data,
            headers=headers or {},
            method=method,
        )
        try:
            with self.opener.open(req, timeout=120) as response:
                body = response.read()
                return response.status, body
        except urllib.error.HTTPError as err:
            return err.code, err.read()

    def login(self, username: str, password: str) -> None:
        payload = urllib.parse.urlencode(
            {"username": username, "password": password}
        ).encode()
        status, _ = self.request(
            "/login",
            data=payload,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        if status not in (200, 302):
            raise SystemExit(f"Login fallito (HTTP {status}). Controlla utente e password admin.")
        check, body = self.request("/admin/menu")
        if check != 200:
            raise SystemExit(
                f"Sessione admin non valida (HTTP {check}). "
                "Usa le credenziali di login.html, non la password del VPS."
            )
        json.loads(body)

    def menu(self) -> list[dict]:
        status, body = self.request("/admin/menu")
        if status != 200:
            raise SystemExit(f"Impossibile leggere il menù (HTTP {status})")
        return json.loads(body)

    def upload(self, item: dict, png: bytes) -> None:
        allergens = [
            {"code": a.get("code"), "status": a.get("status")}
            for a in (item.get("allergens") or [])
            if a.get("code")
        ]
        data = {
            "titolo": item.get("titolo") or "",
            "categoryName": item.get("categoryName") or "",
            "descrizione": item.get("descrizione") or "",
            "prezzo": item.get("prezzo") or 0,
            "allergens": allergens,
        }
        body, boundary = multipart_body(
            data, f"{item['id']}.png", png, "image/png"
        )
        status, resp = self.request(
            f"/admin/menu/{item['id']}",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            method="PUT",
        )
        if status != 200:
            raise RuntimeError(f"upload {item['id']} HTTP {status}: {resp[:240]!r}")


def grok_api_key() -> str:
    return (
        os.environ.get("XAI_API_KEY")
        or os.environ.get("GROK_API_KEY")
        or os.environ.get("OPENAI_API_KEY")
        or ""
    )


def generate_openai(prompt: str, api_key: str) -> bytes:
    payload = json.dumps(
        {
            "model": os.environ.get("OPENAI_IMAGE_MODEL", "gpt-image-1"),
            "prompt": prompt,
            "size": "1024x1024",
            "n": 1,
        }
    ).encode()
    req = urllib.request.Request(
        "https://api.openai.com/v1/images/generations",
        data=payload,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            result = json.loads(response.read())
    except urllib.error.HTTPError as err:
        raise RuntimeError(f"OpenAI HTTP {err.code}: {err.read()[:400]!r}") from err

    return decode_image_payload(result)


def generate_grok(prompt: str, api_key: str) -> bytes:
    payload = json.dumps(
        {
            "model": os.environ.get("XAI_IMAGE_MODEL", "grok-imagine-image-2.0"),
            "prompt": prompt,
            "n": 1,
            "aspect_ratio": "1:1",
            "resolution": "1k",
            "response_format": "b64_json",
        }
    ).encode()
    req = urllib.request.Request(
        "https://api.x.ai/v1/images/generations",
        data=payload,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            result = json.loads(response.read())
    except urllib.error.HTTPError as err:
        raise RuntimeError(f"Grok HTTP {err.code}: {err.read()[:400]!r}") from err
    return decode_image_payload(result)


def decode_image_payload(result: dict) -> bytes:
    entry = (result.get("data") or [{}])[0]
    if entry.get("b64_json"):
        import base64

        return base64.b64decode(entry["b64_json"])
    image_url = entry.get("url")
    if not image_url:
        raise RuntimeError(f"Nessuna immagine nella risposta: {str(result)[:300]}")
    with urllib.request.urlopen(image_url, timeout=120) as response:
        return response.read()


def generate_pollinations(prompt: str) -> bytes:
    encoded = urllib.parse.quote(prompt[:900])
    seed = secrets.randbelow(1_000_000)
    url = (
        "https://image.pollinations.ai/prompt/"
        f"{encoded}?width=1024&height=1024&nologo=true&seed={seed}&model=flux"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "CornerMenuPhotos/1.0"})
    with urllib.request.urlopen(req, timeout=180) as response:
        data = response.read()
    if len(data) < 2000:
        raise RuntimeError("Pollinations ha restituito un file troppo piccolo")
    return data


def looks_like_png(data: bytes) -> bool:
    return data.startswith(b"\x89PNG\r\n\x1a\n")


def to_png_if_possible(data: bytes) -> bytes:
    if looks_like_png(data):
        return data
    try:
        from PIL import Image
        import io

        image = Image.open(io.BytesIO(data)).convert("RGB")
        out = io.BytesIO()
        image.save(out, format="PNG", optimize=True)
        return out.getvalue()
    except Exception:
        return data


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Genera e carica foto menù Corner Pub")
    parser.add_argument("--base-url", default=os.environ.get("CORNER_BASE_URL", BASE_DEFAULT))
    parser.add_argument("--provider", choices=("grok", "openai", "pollinations"), default=None)
    parser.add_argument("--only-missing", action="store_true", default=True)
    parser.add_argument("--force", action="store_true", help="Rigenera anche se la foto attuale si apre")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--ids", default="", help="Solo questi id, es. 18,27,42")
    parser.add_argument("--sleep", type=float, default=1.2)
    parser.add_argument("--no-upload", action="store_true", help="Salva in locale senza caricare")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    provider = args.provider
    if provider is None:
        if grok_api_key():
            provider = "grok"
        elif os.environ.get("OPENAI_API_KEY"):
            provider = "openai"
        else:
            provider = "pollinations"

    if provider == "grok" and not grok_api_key() and not args.dry_run:
        print("Manca XAI_API_KEY (chiave Grok / xAI).")
        return 1
    if provider == "openai" and not os.environ.get("OPENAI_API_KEY") and not args.dry_run:
        print("Manca OPENAI_API_KEY. Usa --provider grok oppure --provider pollinations.")
        return 1

    client = CornerClient(args.base_url)
    user = os.environ.get("CORNER_ADMIN_USER", "")
    password = os.environ.get("CORNER_ADMIN_PASSWORD", "")
    need_login = not args.dry_run
    if need_login:
        if not user or not password:
            print("Imposta CORNER_ADMIN_USER e CORNER_ADMIN_PASSWORD (login del back office).")
            return 1
        client.login(user, password)
        print("Login admin ok", flush=True)
        items = client.menu()
        print(f"Menu admin: {len(items)} piatti", flush=True)
    else:
        status, body = client.request("/api/menu")
        if status != 200:
            print(f"Impossibile leggere /api/menu (HTTP {status})")
            return 1
        items = json.loads(body)

    wanted = {int(x) for x in args.ids.split(",") if x.strip()} if args.ids else None
    selected = []
    for item in items:
        if wanted and int(item["id"]) not in wanted:
            continue
        if not args.force and args.only_missing:
            if has_working_image(args.base_url, item.get("imageUrl") or ""):
                continue
        selected.append(item)
        if args.limit and len(selected) >= args.limit:
            break

    print(f"Provider: {provider}", flush=True)
    print(f"Piatti da fare: {len(selected)} / {len(items)}", flush=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    done = 0
    failed = 0
    for index, item in enumerate(selected, start=1):
        prompt = build_prompt(item)
        print(f"[{index}/{len(selected)}] #{item['id']} {item.get('titolo')} ({item.get('categoryName')})", flush=True)
        if args.dry_run:
            print(f"    {prompt[:180]}...", flush=True)
            continue
        try:
            raw = (
                generate_grok(prompt, grok_api_key())
                if provider == "grok"
                else generate_openai(prompt, os.environ["OPENAI_API_KEY"])
                if provider == "openai"
                else generate_pollinations(prompt)
            )
            png = to_png_if_possible(raw)
            dest = OUT_DIR / f"{item['id']}.png"
            dest.write_bytes(png)
            if not args.no_upload:
                client.upload(item, png)
                print(f"    ok → /uploads/prodotti/{item['id']}.png", flush=True)
            else:
                print(f"    salvata {dest}", flush=True)
            done += 1
        except Exception as exc:
            failed += 1
            print(f"    ERRORE: {exc}", flush=True)
        if index < len(selected):
            time.sleep(args.sleep)

    print(f"Fatto. ok={done} errori={failed}", flush=True)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
