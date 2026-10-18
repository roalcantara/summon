#!/usr/bin/env bash
# Downloads the Whisper base q5_1 model (MIT, ~57 MB) into the git-ignored app assets (ADR-0027).
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/app/src/main/assets/whisper/ggml-base-q5_1.bin"
url="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
sha="422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
mkdir -p "$(dirname "$dest")"
if [ ! -f "$dest" ] || [ "$(shasum -a 256 "$dest" | cut -d' ' -f1)" != "$sha" ]; then
  curl -fL --retry 3 -o "$dest.part" "$url"
  [ "$(shasum -a 256 "$dest.part" | cut -d' ' -f1)" = "$sha" ] || { echo "checksum mismatch" >&2; rm -f "$dest.part"; exit 1; }
  mv "$dest.part" "$dest"
fi
echo "model ok: $dest"
