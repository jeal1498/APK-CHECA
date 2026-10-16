#!/bin/bash
# Reconstruye todo desde SCRIPT.md: audio_meta, storyboard, escenas, subtítulos, index, transiciones y mezcla.
set -e
cd "$(dirname "$0")"
S=../../../.claude/skills/product-launch-video/scripts
python3 -I armar.py | tail -1
python3 -I generar.py
node $S/captions.mjs build --storyboard ./STORYBOARD.md --audio-meta ./audio_meta.json --hyperframes . --out ./caption_groups.json > /dev/null
python3 -I - <<'PY'
import json, re
g = json.load(open('caption_groups.json')); groups = g if isinstance(g, list) else g.get('groups', g)
tot = round(sum(v['duration_s'] for v in json.load(open('audio_meta.json'))['voices']) + 0.15 * 9 + 1.0, 2)
tpl = open('estilo-captions.txt', encoding='utf-8').read()
new = re.sub(r'var GROUPS = \[.*?\];\n', lambda m: 'var GROUPS = ' + json.dumps(groups, ensure_ascii=False, separators=(',', ':')) + ';\n', tpl, count=1, flags=re.S).replace('40.607', str(tot))
open('compositions/captions.html', 'w', encoding='utf-8').write(new)
PY
node $S/assemble-index.mjs --storyboard ./STORYBOARD.md --hyperframes . --audio-meta ./audio_meta.json | tail -1
node $S/transitions.mjs inject --storyboard ./STORYBOARD.md --hyperframes . > /dev/null
node $S/transitions.mjs verify --storyboard ./STORYBOARD.md --index ./index.html | tail -1
python3 -I - <<'PY'
import re
s = open('index.html', encoding='utf-8').read()
s = re.sub(r'(<audio\s+id="el-[^"]+-voice")(?![^>]*data-audio-group)', r'\1 data-audio-group="voiceover"', s)
open('index.html', 'w', encoding='utf-8').write(s)
PY
node ../../../.claude/skills/hyperframes-audio/scripts/carve.mjs --comp ./index.html | tail -1
