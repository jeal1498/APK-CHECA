# checa-marca-v2 — plantilla de video con la marca v2

Sustituye a `../checa-promo/` para los videos **nuevos** (17 oct 2026). `checa-promo` se queda como está: es el video ya hecho y su marca anterior (verde y negro, Fira).

**Qué trae (solo lo reutilizable, sin escenas ni voz):**
- `frame.md` — diseño de video en marca v2 (colores, letras, zonas seguras, componentes, qué no hacer).
- `assets/checa-popup.html` — pop-up v2, los tres niveles del mismo tamaño.
- `assets/logo-checa.svg` — semáforo con ✓.
- `assets/fonts/` — Bricolage Grotesque 800/500, DM Sans 400/500/700 y JetBrains Mono 500/700 (woff2, licencia OFL en `LICENCIAS.txt`).
- `compositions/captions.html` — subtítulos v2 (palabra activa en amarillo, sin placa, banda a y = 1160). `captions.mjs build` inyecta `GROUPS` y `DURATION`.
- `AGENTS.md`, `CLAUDE.md`, `hyperframes.json`, `meta.json`, `package.json`, `alinear.py` — igual que `checa-promo`.

**Cómo usarla** (proceso de `videos-checa`, «Estilo HyperFrames»):
1. `cp -r video/hyperframes/checa-marca-v2 video/hyperframes/<tema>` y cambia `meta.json` y `package.json` (nombre).
2. Copia `ejemplo/SCRIPT.md` y `ejemplo/escenas.json` a la raíz y edítalos: una línea de voz por escena y un objeto por escena (tipos `gancho`, `numero`, `popup`, `estados`, `cierre`; el detalle está en la cabecera de `construir.py`).
3. `python3 -I construir.py --render`: pide la voz de Fernando (solo las líneas que falten), calcula tiempos, genera escenas, subtítulos e `index.html`, corre `hyperframes check` y renderiza a `renders/video.mp4` (~25 s). Sin `--render` solo arma; con `--check` también valida.
4. Para una escena que no encaja en esos tipos, escribe a mano `compositions/frames/eNN-<nombre>.html` siguiendo `frame.md` (declara las `@font-face` con `assets/fonts/*.woff2`) y agrega el nombre al index; `construir.py` borra y regenera `compositions/frames/*.html`, así que mejor añade un tipo nuevo al script.

`construir.py` valida que la neta cuadre (viaje − gasolina), pone «Números de ejemplo», usa ids que empiezan con letra y exige el cierre estándar como última escena. No agrega música ni efectos de sonido todavía.

La marca completa vive en `jeal1498/checa-web`, carpeta `marca/` (skill `marca-checa`).

**Verificado (17 oct 2026):** los tres pop-ups y un subtítulo de ejemplo se renderizaron en Chromium con las fuentes de `assets/fonts/`: mismo tamaño, sin desbordes (la ficha «Rec. 6 km» exigió bajar los datos a 40 px).
**Probada de punta a punta (17 oct 2026):** `prueba-marca-v2/` es un video de 13 s (4 escenas, voz de Fernando, subtítulos v2, pantalla final) hecho con esta plantilla: `check` sin errores y render en ~25 s. Después se reprodujo con `construir.py` (mismo resultado visual, 9 subtítulos, 24 s de render).
