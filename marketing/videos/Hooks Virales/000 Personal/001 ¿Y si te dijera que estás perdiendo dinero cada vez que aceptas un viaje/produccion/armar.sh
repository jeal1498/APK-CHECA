#!/bin/bash
# Une la grabación de Eduardo con los gráficos. Uso: bash armar.sh <grabacion.mp4> [desplazamiento_y]
# desplazamiento_y: de 0 (parte de arriba del video) a 1 (parte de abajo); 0.3 por defecto (cara en el tercio superior).
# Antes: palabras.json con los tiempos reales (Whisper) y `node render.js video` + `node render.js marco`.
set -e
cd "$(dirname "$0")"
CLIP="$1"; OY="${2:-0.3}"
ffmpeg -y -loglevel error -i renders/graficos.mp4 -i "$CLIP" -i renders/marco.png -filter_complex \
 "[1:v]scale=785:-2,crop=785:440:0:(ih-440)*${OY},setsar=1[cam];[0:v][cam]overlay=72:190:shortest=0[a];[a][2:v]overlay=0:0[v];[1:a]loudnorm=I=-14:TP=-1.5:LRA=11[au]" \
 -map "[v]" -map "[au]" -c:v libx264 -profile:v high -pix_fmt yuv420p -crf 18 -r 30 -c:a aac -b:a 192k -shortest renders/video.mp4
echo "listo: renders/video.mp4"
