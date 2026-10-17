#!/bin/bash
# Pone la portada como primer cuadro del video (0.2 s, 6 cuadros a 30 fps) para que TikTok la tome de portada.
# Uso: video/scripts/portada_primer_cuadro.sh <video.mp4> <portada.jpg> <salida.mp4>
set -e
V="$1"; P="$2"; O="$3"
ffmpeg -y -loglevel error -loop 1 -framerate 30 -t 0.2 -i "$P" -f lavfi -t 0.2 -i anullsrc=r=48000:cl=stereo -i "$V" \
  -filter_complex "[0:v]scale=1080:1920,format=yuv420p,setsar=1[a];[2:v]format=yuv420p,setsar=1[b];[a][1:a][b][2:a]concat=n=2:v=1:a=1[v][au]" \
  -map "[v]" -map "[au]" -r 30 -c:v libx264 -preset medium -crf 17 -c:a aac -b:a 192k -movflags +faststart "$O"
