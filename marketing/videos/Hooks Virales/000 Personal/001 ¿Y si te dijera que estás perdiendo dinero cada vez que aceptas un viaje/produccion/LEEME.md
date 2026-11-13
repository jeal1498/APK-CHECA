# Personal 001 · producción

Diseño según `../../../plantilla-maestra-safe-zones.svg`: cámara arriba (72,190 · 785×440), subtítulos (y 650–820) y gráficos (72,840 · 785×510). Nada en las zonas de exclusión.

## Estado (13 nov)
- Listo: gráficos de las 5 frases, subtítulos, marco de la cámara, copy y script de unión (probado con un clip de prueba).
- Los tiempos de `palabras.json` son **estimados** (`estimar_tiempos.py`). Falta la grabación de Eduardo.

## Cuando llegue la grabación
1. Guardarla fuera del repo (scratchpad) y transcribir: `python3 .claude/skills/video-checa/scripts/transcribir.py <clip> w.json`; pasar las palabras a `palabras.json` (`{"palabras": [[palabra, ini, fin], ...], "total": <duración>}`).
2. Si Eduardo cambió alguna palabra del guion, ajustar las claves `W('palabra')` de `pagina.html`.
3. `NODE_PATH=$(npm root -g) node render.js hoja` y revisar `renders/storyboard.png`.
4. `node render.js video && node render.js marco && bash armar.sh <clip> [0–1]` → `renders/video.mp4` (el número mueve el recorte de la cámara: 0 arriba, 1 abajo).

## Para grabar
Vertical, 25–35 s, la cara en el tercio superior del cuadro y sin moverse mucho hacia arriba o abajo: de tu video solo se usa una franja horizontal (785×440).
