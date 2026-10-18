# Chant-check experiments (ADR-0026)

Desktop-only scripts that measure whether a recogniser separates honest chants from nonsense
on the calibration corpus (`tools/gen_chant_corpus.py`, ADR-0025). Nothing here ships.

```bash
CLIPS=app/src/test/chant-clips
CATALOG=app/src/main/java/com/summondemons/safely/data/DemonCatalog.kt
# Whisper, language forced to Latin, no prompt (size: tiny | base)
uv run --no-project --python 3.11 --with faster-whisper --with soundfile --with numpy \
  python -I tools/experiments/whisper_latin.py $CLIPS $CATALOG base /tmp/rows-base.json
uv run --no-project --python 3.11 --with numpy python -I tools/experiments/analyze_whisper.py /tmp/rows-base.json
# Allosaurus universal phones vs espeak Latin phonemes
uv run --no-project --python 3.11 --with allosaurus --with piper-tts --with numpy --with soundfile \
  python -I tools/experiments/allosaurus_phones.py $CLIPS $CATALOG /tmp/rows-allo.json
uv run --no-project --python 3.11 --with numpy python -I tools/experiments/analyze_allosaurus.py /tmp/rows-allo.json
```
