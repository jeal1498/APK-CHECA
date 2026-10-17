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
2. Escribe `SCRIPT.md` y `STORYBOARD.md`; voz de Fernando con `--voice dlGxemPxFMTY7iXagmOj` (concurrencia 2).
3. En cada escena declara las `@font-face` con `assets/fonts/*.woff2` (el popup y el logo no las traen) y respeta `frame.md`.
4. `captions.mjs build`, ensamblar, `npx hyperframes@0.8.154 check` y render.

La marca completa vive en `jeal1498/checa-web`, carpeta `marca/` (skill `marca-checa`).

**Verificado (17 oct 2026):** los tres pop-ups y un subtítulo de ejemplo se renderizaron en Chromium con las fuentes de `assets/fonts/`: mismo tamaño, sin desbordes (la ficha «Rec. 6 km» exigió bajar los datos a 40 px).
**Pendiente de probar:** un render completo con esta plantilla (`captions.mjs build`, ensamble, `check` y render).
