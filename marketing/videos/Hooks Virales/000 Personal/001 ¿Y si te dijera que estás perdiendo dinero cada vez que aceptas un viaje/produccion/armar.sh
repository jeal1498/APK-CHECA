#!/bin/bash
# Une la grabación de Eduardo con los gráficos. Uso: bash armar.sh <grabacion.mp4> [desplazamiento_y]
# La cámara ocupa el 45 % de arriba (1080×864). desplazamiento_y: 0 = parte de arriba de su video, 1 = parte de abajo
# (0.15 por defecto). Meta: ojos en la línea del tercio superior (y=288) y él centrado.
# Antes: palabras.json con los tiempos reales (Whisper) y `node render.js video llave`.
set -e
cd "$(dirname "$0")"
CLIP="$1"; OY="${2:-0.15}"
ffmpeg -y -loglevel error -i renders/graficos-llave.mp4 -i "$CLIP" -filter_complex \
 "[1:v]scale=1080:-2,crop=1080:864:0:(ih-864)*${OY},pad=1080:1920:0:0:color=0x16161C,setsar=1,fps=30[cam];[0:v]colorkey=0xFF00FF:0.25:0.08[fg];[cam][fg]overlay=0:0[v];[1:a]loudnorm=I=-14:TP=-1.5:LRA=11[au]" \
 -map "[v]" -map "[au]" -c:v libx264 -profile:v high -pix_fmt yuv420p -crf 18 -r 30 -c:a aac -b:a 192k -shortest renders/video.mp4
echo "listo: renders/video.mp4"
