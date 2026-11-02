# Control de calidad de un lote

Eduardo solo ve el resultado final, así que antes de mandárselo cada lote pasa por tres revisiones. Corren **siempre, en cada lote, sin importar cuántos videos tenga** (aunque sea uno): son baratas y lo que se escape llega directo a él.

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

Corrige lo que reporte y vuelve a correr la revisión automática de los videos que cambiaron.

## 3. Auditoría visual: coherencia y ritmo (Eduardo, 1 nov 2026)
Corre después de la 2, en cada lote. Revisa **cómo la imagen acompaña a la voz**, no las cifras ni el copy. Un agente para todo el lote (`subagent_type: general-purpose`, `model: sonnet`), distinto del de la revisión 2.

**Antes del agente, la parte medible (gratis, segundos):**
```bash
python3 -I video/scripts/checa.py auditar <proyecto>
```
Usa `renders/video.mp4` del proyecto (sin la portada) con `tiempos.json`, `escenas.json` y `opciones.json`. ✗ = se corrige antes de seguir: primer cuadro vacío, último cuadro distinto del primero (loop), cola de más de 1.8 s, texto cortado en un borde por 1 s o más, escenas con golpes sin `"adelanto": 0.04`. ⚠ = aviso para revisar a ojo y pasárselo al agente: subtítulos debajo de y=1450 o una palabra con golpe en la que casi nada cambia en pantalla (es una medición aproximada: puede marcar un resaltado sutil que sí está).

**Material que le preparas por video** (el agente no puede ver el mp4 corriendo):
- Hoja de cuadros cada 0.5 s con el segundo impreso:
  ```bash
  ffmpeg -v error -y -i <video.mp4> -vf "fps=2,scale=180:-1,drawtext=text='%{pts\:flt}':fontcolor=yellow:fontsize=20:x=6:y=6,tile=10x10" renders/auditoria-%d.png
  ```
  (si el video dura más de 50 s, sale más de una hoja: `auditoria-1.png`, `auditoria-2.png`…).
- `tiempos.json` del proyecto (cada palabra de la voz con su segundo) y `escenas.json`.

**Prompt** (el de Eduardo, sin cambiarlo; arriba se agrega el material):

> Revisa estos videos de Checa (app para conductores de Uber/DiDi): <lista de videos>. Por cada uno tienes la hoja de cuadros cada 0.5 s con el segundo impreso (`renders/auditoria-*.png`, míralas con Read), `tiempos.json` (cada palabra de la voz con su segundo) y `escenas.json`. Compara segundo a segundo lo que se ve con la palabra que suena en ese instante. No cambies archivos.
>
> Actúa como un Director Creativo experto en diseño visual de interfaces, edición y retención para TikTok / Reels (especializado en frameworks de código como Remotion e HyperFrames).
>
> Tu tarea es realizar una auditoría técnica y estética del **diseño visual** de este vídeo vertical, evaluando estrictamente dos cosas:
> 1. **La coherencia semántica:** Que cada gráfico, tarjeta, tipografía en pantalla, icono o transición tenga sentido exacto con lo que el guión está diciendo en ese momento preciso (cero elementos decorativos desconectados o genéricos).
> 2. **El ritmo y retención (Pacing):** Que el impacto visual frene el scroll en el primer segundo y mantenga estímulos dinámicos para evitar el abandono.
>
> ⚠️ REGLA ESTRICTA: No vas a corregir ni reescribir el guión hablado; asume que el audio es fijo. Tu enfoque es cómo la **interfaz gráfica** acompaña, ilustra y potencia ese guión de forma milimétrica.
>
> Por favor, analiza el vídeo y entrégame un reporte estructurado con las siguientes áreas de oportunidad:
>
> 1. **Sincronización Guión-Gráfico (Coherencia):** ¿Los elementos visuales que aparecen en pantalla ilustran fielmente la idea del locutor en ese segundo exacto, o hay desalineaciones donde el gráfico muestra una cosa y el audio dice otra?
> 2. **Impacto inicial (Hook visual - Segundos 0 a 2):** ¿El primer frame frena el scroll y conecta de inmediato con la premisa principal del audio?
> 3. **Ritmo de revelación visual:** ¿Los gráficos entran al ritmo de los beats de la voz o se sienten atropellados y lentos? ¿La densidad visual en formato vertical es cómoda para el ojo?
> 4. **Puntos de anclaje (Retención):** ¿Hay recursos visuales clave (como resaltados, cambios de escala o llamadas gráficas) que fuercen al espectador a quedarse hasta el cierre?
>
> Para cada área detectada, entrégame:
> - **El problema visual exacto** (en qué segundo ocurre y qué dice el guión en ese momento).
> - **Por qué falla** (ya sea por incoherencia con el mensaje o por falta de ritmo para TikTok).
> - **Una propuesta concreta de solución** aplicable en código/HyperFrames (ej. cambiar el componente visual por uno que represente mejor el concepto, ajustar el timing de aparición, o añadir un resaltado dinámico).
>
> Sé directo, con mentalidad de creador de contenido para redes, y ve al grano con las mejoras visuales y de coherencia.
>
> Responde por video: «OK» si no hay nada que corregir, o la lista de problemas en orden de prioridad.

**Qué hacer con el reporte:** aplica lo que no cambie la voz (el audio es fijo), vuelve a renderizar solo los videos que cambiaron, repite la revisión 1 y vuelve a correr esta auditoría solo de esos videos. Con las tres en verde, manda el lote a Eduardo. Si una propuesta choca con una regla de Eduardo (marca, «el gráfico dice lo que dice la voz», sin logos ajenos), gana la regla.
