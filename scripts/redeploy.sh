#!/usr/bin/env bash
# Redeploy Corner Pub sul VPS (pull main + rebuild Docker).
# Uso:
#   bash scripts/redeploy.sh
# oppure dalla root del progetto:
#   ./scripts/redeploy.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Cartella: $ROOT"

if ! command -v git >/dev/null 2>&1; then
  echo "ERRORE: git non trovato" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERRORE: docker non trovato" >&2
  exit 1
fi

COMPOSE=(docker compose)
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
  else
    echo "ERRORE: né 'docker compose' né 'docker-compose' disponibili" >&2
    exit 1
  fi
fi

echo "==> Fetch / pull origin main"
git fetch origin main
git checkout main
git pull --ff-only origin main

echo "==> Build & restart app"
"${COMPOSE[@]}" build app
"${COMPOSE[@]}" up -d app

echo "==> Stato container"
"${COMPOSE[@]}" ps

echo "==> Verifica API booking-settings"
sleep 3
if curl -fsS "https://cornerpubgiovinazzo.com/api/booking-settings"; then
  echo
  echo "OK: redeploy completato."
else
  echo
  echo "ATTENZIONE: build ok, ma /api/booking-settings non risponde ancora (attendi qualche secondo o controlla i log)." >&2
  echo "Log: ${COMPOSE[*]} logs --tail=80 app" >&2
  exit 2
fi
