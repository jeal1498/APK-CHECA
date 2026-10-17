# Tutorial de instalación en marca v2 (para la web, 17 oct 2026)

Salida: `checa-web/media/instalar.mp4` (3:03, 720×1280). Es el tutorial de Eduardo (`marketing/videos/CHECA/20 - Cómo instalar Checa`) rehecho en marca v2: misma voz y pasos (líneas 1 y 4–42 de `lineas.txt`, del «Paso dos» al «Paso doce»), mismas capturas de su grabación original, más una tarjeta de inicio, «Listo» y «¿Te atoraste? Escríbenos por WhatsApp» (`voz-v2-fin/`, voz de Fernando).

- `build-v2.py`: `build.py` adaptado (marca v2, subtítulos con palabra activa, tarjetas de inicio/Listo/fin, render en paralelo, difumina cuentas y nombre).
- `armar-v2.sh`: arma `trabajo-v2/` con las voces en el orden que espera el script y baja las fuentes TTF.
- La grabación original está en Google Drive (`1_konXN6bdc1hyDYrOt2tt0kqwunm8ycR`, «Paso a pasó para instalar CHECA.mp4», 73 MB); **tiene datos personales**, no se sube al repo. `build-v2.py` difumina cuentas y nombre en los planos donde salen; revisar el video completo antes de publicar.
- Para la web: `ffmpeg -i out.mp4 -vf scale=720:1280 -c:v libx264 -crf 27 -preset slow -pix_fmt yuv420p -c:a aac -b:a 80k -movflags +faststart instalar.mp4` y un póster con `ffmpeg -ss 3.4 -i out.mp4 -frames:v 1 -vf scale=540:960 instalar.jpg`.
- Si cambian los pasos de la app, hay que volver a grabar y ajustar la lista `S` de `build-v2.py` (tiempos de captura y zonas a tocar).
