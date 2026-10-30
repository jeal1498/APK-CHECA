# Control de calidad de un lote

Eduardo solo ve el resultado final, así que antes de mandárselo cada lote pasa por dos revisiones. Corren **siempre, en cada lote, sin importar cuántos videos tenga** (aunque sea uno): son baratas y lo que se escape llega directo a él.

## 1. Revisión automática (por video, gratis)
```bash
python3 -I video/scripts/checa.py calidad "marketing/videos/CHECA|PERSONAL/NN - Nombre" <proyecto> [--camara]
```
Revisa: archivos completos, mp4 `yuv420p` High 1080×1920 y duración, que el primer cuadro sea la portada, copy con el CTA vigente, «números de ejemplo», sin «Soy Eduardo» (salvo `--camara`) ni «gratis»/precios, DM con su utm, voz sin «$», la última línea de voz con el CTA, pop-ups con «OFERTA», neta = viaje − gasolina, pantalla final y que el sello de la portada no se delate. Si algo sale ✗, se corrige antes de seguir.

## 2. Revisión con ojos frescos (un agente por lote)
Un solo agente para todo el lote (no uno por video), con `subagent_type: general-purpose` y `model: sonnet`. No hizo los videos, así que ve lo que a quien los hizo se le pasa. Prompt:

> Eres control de calidad de los videos de Checa (app para conductores de Uber/DiDi). Revisa estas carpetas: <lista de carpetas y proyectos>. Por cada video lee `guion.md`, `copy.txt`, `dm.txt`, el `SCRIPT.md` y `escenas.json` del proyecto, y mira con Read la portada (`portada.jpg`) y la hoja de cuadros (`renders/hoja.png` del proyecto; si falta, créala con `python3 -I video/scripts/checa.py revisar <video.mp4>`). Comprueba:
> 1. Las cifras son las mismas en la voz, las escenas, la portada y el copy, y cuadran con las fórmulas (gasolina = km totales ÷ 12 × 24; neta = tarifa − gasolina; $/km = tarifa ÷ km totales; $/h = tarifa ÷ min totales × 60).
> 2. El gancho de la voz y el título de la portada cuentan lo mismo; la cifra escondida en el sello no aparece a la vista.
> 3. CHECA y su v2 PERSONAL usan las mismas cifras pero distinto gancho, palabras y orden, y distinta portada.
> 4. Nada de logos ni nombres de Uber/DiDi en pantalla, ciudad, país, «gratis», precios ni promesas de ganancia; sin «Soy Eduardo» en videos sin su cara; ortografía («checas», «que cheques»).
> 5. En la hoja de cuadros: nada cortado, encimado ni ilegible; texto importante por encima de y≈1150; «Números de ejemplo» visible donde hay cifras.
> No cambies archivos. Responde por video: «OK» o una lista corta de problemas con dónde están y cómo arreglarlos.

Corrige lo que reporte, vuelve a correr la revisión automática de los videos que cambiaron y entonces manda el lote a Eduardo.
