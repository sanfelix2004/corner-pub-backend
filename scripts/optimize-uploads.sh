#!/bin/sh
# Le miniature JPEG le crea l'app Java all'avvio (GenerateMenuThumbs).
# Non usare ImageMagick qui: senza codec JPEG rovina i file.
echo "optimize-uploads: skip (thumbs gestite da Java)"
exit 0
