#!/bin/sh
# Comprime le foto menu già sul volume Docker.
set -eu

APP="${APP_CONTAINER:-corner_app_1}"
if ! docker inspect "$APP" >/dev/null 2>&1; then
  echo "optimize-uploads: container $APP non trovato, skip"
  exit 0
fi

VOL=$(docker inspect -f '{{ range .Mounts }}{{ if eq .Destination "/app/uploads" }}{{ .Name }}{{ end }}{{ end }}' "$APP")
if [ -z "$VOL" ]; then
  echo "optimize-uploads: volume uploads non trovato, skip"
  exit 0
fi

echo "==> Ottimizzazione foto menu ($VOL)"
docker run --rm -v "$VOL":/imgs dpokidov/imagemagick:7.1.1 sh -c '
  find /imgs/prodotti /imgs/eventi -type f \( -iname "*.jpg" -o -iname "*.jpeg" -o -iname "*.png" \) 2>/dev/null | while read -r f; do
    sz=$(wc -c < "$f")
    [ "$sz" -lt 160000 ] && continue
    mogrify -resize "900x900>" -quality 78 "$f" || true
  done
  echo "foto menu ottimizzate"
'
