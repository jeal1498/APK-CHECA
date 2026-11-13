# CHECA 001 · Hooks Virales · estilo Pixel Art (prueba)

Primer video de CHECA en **Pixel Art** (ver `../../../estilos/README.md`). **Eduardo sale a cámara** (decisión del 13 nov), con el mismo formato que Personal: cámara en el 45 % de arriba (1080×864), él centrado y ojos en el tercio superior; gráficos centrados en el 55 % de abajo; subtítulos B sobre la frontera.

## Estado (13 nov)
- Listo: gráficos de las 6 frases, subtítulos, fondo y borrador **sin voz** (tiempos estimados, `estimar_tiempos.py`). Unión con la grabación probada con un clip de prueba.
- **Falta la grabación de Eduardo.** Cuando llegue: tiempos reales en `palabras.json` (Whisper), `node render.js hoja`, `node render.js video llave` y `bash armar.sh <clip> [0–1]` → `renders/video.mp4`.
- Faltan también portada, copy y DM. El guion cierra con el eslogan, sin el CTA «Comenta CHECA»: decidir con Eduardo si se agrega.
- La versión a pantalla completa (sin cámara) quedó en el historial de git.

## Qué define el estilo
- Todo sobre cuadrícula de 8 px, sin esquinas redondas ni degradados; sombras sólidas negras.
- Fondo: noche morada en franjas, estrellas que parpadean, luna, nubes moradas, ciudad con ventanas encendidas y un auto que pasa.
- Letra: Press Start 2P (`estilos/fuentes/`, licencia OFL). No trae mayúsculas con acento: rótulos y subtítulos van sin tilde. (Pixelify Sans se descartó: el 5 parecía S y el 2 parecía 8.)
- Pop-up de la app en pixel, con cifras de las fórmulas de `Parser.kt`: $85, 30 min, 13.5 km → $170/h y $6.3/km (no cumple $200/h ni $10/km → RECHAZAR); gasolina $27 → quedan $58. «NUMEROS DE EJEMPLO» visible mientras hay cifras.
- Zona de gráficos: 808×480 en (136, 912), centrada; la ciudad llena la franja de abajo sin tapar los gráficos.
- Reglas de animación: las mismas de Personal (una acción por bloque, máx. 4 palabras, continuidad, nada vacío, todo en su palabra).
