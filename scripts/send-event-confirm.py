#!/usr/bin/env python3
"""Prepara il messaggio WhatsApp di conferma evento.

Di default manda SOLO una prova al numero 3288826170.
Non invia in massa: apre (o stampa) un link wa.me da confermare a mano.

  python3 scripts/send-event-confirm.py
  python3 scripts/send-event-confirm.py --event-date 2026-09-11 --test-phone 3288826170
  python3 scripts/send-event-confirm.py --event-id 12 --print-only
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from http.cookiejar import CookieJar

BASE_DEFAULT = "https://cornerpubgiovinazzo.com"


class CornerClient:
    def __init__(self, base: str):
        self.base = base.rstrip("/")
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(CookieJar()))

    def request(self, path: str, data=None, headers=None, method=None):
        req = urllib.request.Request(self.base + path, data=data, headers=headers or {}, method=method)
        try:
            with self.opener.open(req, timeout=60) as response:
                return response.status, response.read()
        except urllib.error.HTTPError as err:
            return err.code, err.read()

    def login(self, username: str, password: str) -> None:
        payload = urllib.parse.urlencode({"username": username, "password": password}).encode()
        status, _ = self.request(
            "/login",
            data=payload,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        if status not in (200, 302):
            raise SystemExit(f"Login fallito (HTTP {status})")
        check, _ = self.request("/admin/events")
        if check != 200:
            raise SystemExit("Sessione admin non valida")

    def events(self) -> list:
        status, body = self.request("/admin/events")
        if status != 200:
            raise SystemExit(f"Impossibile leggere gli eventi (HTTP {status})")
        return json.loads(body)

    def reminders(self, event_id: int, test_phone: str | None) -> dict:
        query = f"?testPhone={urllib.parse.quote(test_phone)}" if test_phone else ""
        status, body = self.request(
            f"/admin/events/{event_id}/attendance-reminders{query}",
            method="POST",
        )
        if status != 200:
            raise SystemExit(f"Reminder fallito (HTTP {status}): {body[:300]!r}")
        return json.loads(body)


def find_event(events: list, event_id: int | None, event_date: str | None):
    if event_id:
        for event in events:
            if int(event["id"]) == event_id:
                return event
        raise SystemExit(f"Evento {event_id} non trovato")
    wanted = datetime.strptime(event_date, "%Y-%m-%d").date()
    matches = []
    for event in events:
        raw = str(event.get("data") or "")
        try:
            day = datetime.fromisoformat(raw.replace("Z", "")).date()
        except ValueError:
            continue
        if day == wanted:
            matches.append(event)
    if not matches:
        raise SystemExit(f"Nessun evento in data {event_date}")
    return matches[0]


def main() -> int:
    parser = argparse.ArgumentParser(description="WhatsApp conferma evento Corner Pub")
    parser.add_argument("--base-url", default=os.environ.get("CORNER_BASE_URL", BASE_DEFAULT))
    parser.add_argument("--event-id", type=int, default=0)
    parser.add_argument("--event-date", default="2026-09-11")
    parser.add_argument("--test-phone", default="3288826170", help="Vuoto = tutti gli iscritti (non usarlo ora)")
    parser.add_argument("--all", action="store_true", help="Prepara i link per tutti. Non aprire WhatsApp in massa.")
    parser.add_argument("--print-only", action="store_true")
    args = parser.parse_args()

    user = os.environ.get("CORNER_ADMIN_USER", "")
    password = os.environ.get("CORNER_ADMIN_PASSWORD", "")
    if not user or not password:
        print("Imposta CORNER_ADMIN_USER e CORNER_ADMIN_PASSWORD")
        return 1

    test_phone = None if args.all else (args.test_phone or "3288826170")
    client = CornerClient(args.base_url)
    client.login(user, password)
    event = find_event(client.events(), args.event_id or None, args.event_date)
    data = client.reminders(int(event["id"]), test_phone)
    print(f"Evento: {data.get('eventTitle')} (id {data.get('eventId')})")
    print(f"Messaggi preparati: {data.get('targeted')}")
    if not data.get("targeted"):
        print("Nessun iscritto con quel numero. Il test non parte.")
        return 1
    for item in data.get("messages") or []:
        print(f"- {item.get('name')} {item.get('phone')}")
        print(f"  {item.get('confirmUrl')}")
        print(f"  {item.get('waLink')[:90]}...")
        if args.print_only or args.all:
            continue
        subprocess.run(["open", item["waLink"]], check=False)
        print("Aperto WhatsApp: invia tu il messaggio dalla chat.")
        break
    return 0


if __name__ == "__main__":
    sys.exit(main())
