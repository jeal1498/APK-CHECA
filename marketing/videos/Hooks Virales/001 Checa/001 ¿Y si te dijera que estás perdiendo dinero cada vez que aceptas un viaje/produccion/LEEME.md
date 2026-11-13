# CHECA 001 · Hooks Virales · estilo Pixel Art (prueba)

Primer video de CHECA en **Pixel Art** (ver `../../../estilos/README.md`). A pantalla completa, sin cámara: voz de narrador (Fernando).

## Estado (13 nov)
- Listo: gráficos de las 6 frases, subtítulos, fondo y borrador **sin voz** (tiempos estimados, `estimar_tiempos.py`).
- **Falta la voz:** ElevenLabs respondió «payment_issue» (factura pendiente en la cuenta). Cuando Eduardo la pague: `python3 -I voz.py` (genera `voz/NN.mp3`, `renders/voz.wav` y `palabras.json` con tiempos reales), `node render.js hoja`, `node render.js video` y unir: `ffmpeg -i renders/graficos.mp4 -i renders/voz.wav -c:v copy -c:a aac -b:a 192k -shortest renders/video.mp4`.
- Faltan también portada, copy y DM (van con el video aprobado). El guion cierra con el eslogan, sin el CTA «Comenta CHECA»: decidir con Eduardo si se agrega.

## Qué define el estilo
- Todo sobre cuadrícula de 8 px, sin esquinas redondas ni degradados; sombras sólidas negras.
- Fondo: noche morada en franjas, estrellas que parpadean, luna, nubes moradas, ciudad con ventanas encendidas y un auto que pasa.
- Letra: Press Start 2P (`estilos/fuentes/`, licencia OFL). No trae mayúsculas con acento: rótulos y subtítulos van sin tilde. (Pixelify Sans se descartó: el 5 parecía S y el 2 parecía 8.)
- Pop-up de la app en pixel, con cifras de las fórmulas de `Parser.kt`: $85, 30 min, 13.5 km → $170/h y $6.3/km (no cumple $200/h ni $10/km → RECHAZAR); gasolina $27 → quedan $58. «NUMEROS DE EJEMPLO» visible mientras hay cifras.
- Zonas seguras: gráficos en x 60–868, y 200–1160; subtítulos en y≈1250; la ciudad llena la franja de abajo.
- Reglas de animación: las mismas de Personal (una acción por bloque, máx. 4 palabras, continuidad, nada vacío, todo en su palabra).
