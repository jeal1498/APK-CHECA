#!/bin/bash
# Prepara trabajo-v2/ para renderizar el tutorial de la web en marca v2 (17 oct 2026).
# Luego, dentro de trabajo-v2/: poner f4/ (cuadros de la grabación a 4 fps) y correr
#   python3 -I build-v2.py audio && python3 -I build-v2.py     -> out.mp4 (1080x1920, ~3 min)
set -e
cd "$(dirname "$0")"
mkdir -p trabajo-v2/voz trabajo-v2/f4
cp build-v2.py trabajo-v2/
cp voz/l01.mp3 voz/l01.json trabajo-v2/voz/
for k in $(seq 4 42); do
  a=$(printf "%02d" "$k"); b=$(printf "%02d" $((k-2)))
  cp "voz/l$a.mp3" "trabajo-v2/voz/l$b.mp3"; cp "voz/l$a.json" "trabajo-v2/voz/l$b.json"
done
cp voz-v2-fin/fin1.mp3 trabajo-v2/voz/l41.mp3; cp voz-v2-fin/fin1.json trabajo-v2/voz/l41.json
cp voz-v2-fin/fin2.mp3 trabajo-v2/voz/l42.mp3; cp voz-v2-fin/fin2.json trabajo-v2/voz/l42.json
# Fuentes de la marca en TTF (PIL no lee woff2)
cd trabajo-v2
css=$(curl -s "https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,800&family=DM+Sans:wght@400;500;700&family=JetBrains+Mono:wght@500;700")
python3 -I - "$css" <<'PY'
import re, subprocess, sys
for fam, w, url in re.findall(r"font-family: '([^']+)';.*?font-weight: (\d+);.*?url\((https://[^)]+\.ttf)\)", sys.argv[1], re.S):
    fn = {"Bricolage": "bricolage", "DM": "dm", "JetBrains": "jetbrains"}[fam.split()[0]] + f"-{w}.ttf"
    subprocess.run(["curl", "-s", "-o", fn, url], check=True)
PY
echo "Listo: trabajo-v2/. Falta f4/ (ffmpeg -i grabacion.mp4 -vf fps=4 -q:v 2 f4/%05d.jpg)."
