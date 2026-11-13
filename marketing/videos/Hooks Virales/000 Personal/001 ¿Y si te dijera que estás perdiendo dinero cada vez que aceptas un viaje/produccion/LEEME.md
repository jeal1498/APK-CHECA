# Personal 001 · producción

Diseño aprobado por Eduardo (13 nov): **cámara en el 45 % de arriba** (1080×864, de borde a borde), **gráficos en el 55 % de abajo** (zona 72,912 · 785×510) y **subtítulos en una pastilla justo sobre la frontera** (y=864). En su área, regla de los tercios: **él centrado y los ojos en la línea del tercio superior** (y=288). Se respetan las zonas de exclusión de `../../../plantilla-maestra-safe-zones.svg` (arriba 160 px, derecha 190 px, abajo 530 px). Guía visual: `pagina.html?tercios`.

## Estado (13 nov)
- Listo: gráficos de las 5 frases, subtítulos, marco de la cámara, copy y script de unión (probado con un clip de prueba).
- Los tiempos de `palabras.json` son **estimados** (`estimar_tiempos.py`). Falta la grabación de Eduardo.

## Cuando llegue la grabación
1. Guardarla fuera del repo (scratchpad) y transcribir: `python3 .claude/skills/video-checa/scripts/transcribir.py <clip> w.json`; pasar las palabras a `palabras.json` (`{"palabras": [[palabra, ini, fin], ...], "total": <duración>}`).
2. Si Eduardo cambió alguna palabra del guion, ajustar las claves `W('palabra')` de `pagina.html`.
3. `NODE_PATH=$(npm root -g) node render.js hoja` y revisar `renders/storyboard.png`.
4. `node render.js video llave && bash armar.sh <clip> [0–1]` → `renders/video.mp4` (el número mueve el recorte de la cámara: 0 arriba, 1 abajo; ajustar hasta que los ojos queden en y=288).

## Para grabar
Vertical, 25–35 s, centrado y de frente, con la cara en la parte de arriba del cuadro y aire a los lados: de tu video se usa el 45 % de la altura.
