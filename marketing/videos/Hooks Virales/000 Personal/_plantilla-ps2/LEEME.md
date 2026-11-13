# Plantilla PS2 Style · videos de Hooks Virales PERSONAL

**Todos los videos de `000 Personal` se hacen con este estilo** (Eduardo, 13 nov 2026). Primer video: `001 …` (ejemplo completo).

## Cómo empezar un video nuevo
1. Copia la carpeta `produccion/` de aquí dentro de la carpeta del video (`000 Personal/NNN …/produccion/`; misma profundidad, así las fuentes cargan).
2. En `estimar_tiempos.py` pon las frases del `guion.md` y córrelo (`python3 -I estimar_tiempos.py`).
3. En `pagina.html` reescribe solo los bloques `// ── 1 ·` … `// ── 5 ·` (una acción por frase). No toques el fondo, los subtítulos ni el CSS.
4. `NODE_PATH=$(npm root -g) node render.js hoja` → revisa `renders/storyboard.png`; `node render.js video` → borrador para Eduardo.
5. Con su grabación: tiempos reales en `palabras.json` (Whisper), `node render.js video llave` y `bash armar.sh <clip> [0–1]`.

## Qué define el estilo (no cambia de un video a otro)
- **Pantalla:** cámara en el 45 % de arriba (1080×864), él centrado y ojos en el tercio superior (y=288); gráficos en el 55 % de abajo, centrados y al 90 %.
- **Fondo PS2 (low-poly):** cielo azul en degradado, nubes facetadas, ciudad de bloques con una cara clara y otra oscura, calle abajo con autos que pasan.
- **Gráficos:** paneles azul marino con borde blanco y sombra sólida; textos blancos con contorno marino; monedas facetadas; oferta de ejemplo en tarjeta blanca.
- **Subtítulos estilo B:** mayúsculas con contorno negro sobre la frontera, sin caja; solo se destaca la palabra que suena, en el momento exacto; la raya amarilla se abre para dejarlos pasar.
- **Reglas de animación:** una acción clara por bloque; textos de máx. 4 palabras; analogías literales; escala para proporciones; los elementos se transforman de un bloque al siguiente; cada bloque cierra con una imagen clara; nada vacío esperando su texto; todo cae en su palabra.
- **Marca:** verde, amarillo y rojo solo para decidir; sin logos ni nombres de apps; sin ciudad ni país; «Oferta de ejemplo» donde haya cifras.
