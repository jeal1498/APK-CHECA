# checa-oferta-web — video del inicio de la web (17 oct 2026)

`PWA-Checa-Multimedia/media/vsl-oferta.mp4` (35 s, 720×1280, marca v2). Es el guion aprobado el 16 oct 2026 de `../checa-lee-la-oferta-v2/` con su misma voz, armado con la plantilla `../checa-marca-v2/` y con el cierre sin cuentas ni link (`"sin_redes": true`), porque la web no tiene salidas.

Regenerar:
1. `cp -r video/hyperframes/checa-marca-v2 video/hyperframes/<carpeta-de-trabajo>` y copiar encima `SCRIPT.md` y `escenas.json` de esta carpeta (cambiar `meta.json` y `package.json`).
2. Copiar `../checa-lee-la-oferta-v2/assets/voice/*` a `assets/voice/` (misma voz, no se vuelve a pedir a ElevenLabs).
3. `python3 -I construir.py --render` (en esta máquina render con `setsid nohup …` y esperar: si el proceso padre termina, el render se cancela).
4. Para la web: `ffmpeg -i renders/video.mp4 -vf scale=720:1280 -c:v libx264 -crf 25 -preset slow -pix_fmt yuv420p -c:a aac -b:a 96k -movflags +faststart vsl-oferta.mp4`.

Las cifras son de ejemplo y cuadran (viaje − gasolina = te quedan); en pantalla va «Números de ejemplo».
