# Control de calidad de un lote

Eduardo solo ve el resultado final, así que cada lote pasa por **una revisión antes de renderizar** (un agente) y **una revisión automática después** (gratis). Corren **siempre, en cada lote, aunque sea de un video**. La idea (Eduardo, 2 nov 2026): los errores de coherencia y ritmo se ven en el plan, y corregirlos en `escenas.json` cuesta segundos; corregirlos después del render cuesta otro render de 15–25 min.

## 1. Antes de renderizar: un agente para todo el lote
Para cada video, con la voz ya hecha:
```bash
python3 -I video/scripts/checa.py construir <proyecto> --solo-tiempos   # voz y tiempos.json
python3 -I video/scripts/checa.py a3 <proyecto> --hoja                  # renders/plan.md (cada palabra → gráfico) y renders/storyboard-a3.png
```
Portada, `copy.txt` y `dm.txt` ya hechos.

Un solo agente para todo el lote (no uno por video), con `subagent_type: general-purpose` y `model: sonnet`. No hizo los videos, así que ve lo que a quien los hizo se le pasa. Prompt (parte A: cifras y marca; parte B: el de Eduardo, sin cambiarlo):

> Revisa estos videos de Checa (app para conductores de Uber/DiDi) **antes de renderizarlos**: <lista de carpetas y proyectos>. Por cada video tienes `guion.md`, `copy.txt` y `dm.txt` de la carpeta, y del proyecto `SCRIPT.md` (la voz), `escenas.json`, `renders/plan.md` (cada palabra de la voz con su segundo y el gráfico que entra en ella; lo que no está ligado a una palabra se ve desde el inicio de su línea), el storyboard y `portada/portada.jpg` (míralos con Read). No cambies archivos.
>
> **Parte A · cifras y marca.**
> 1. Las cifras son las mismas en la voz, las escenas, la portada y el copy, y cuadran con las fórmulas (gasolina = km totales ÷ 12 × 24; neta = tarifa − gasolina; $/km = tarifa ÷ km totales; $/h = tarifa ÷ min totales × 60).
> 2. El gancho de la voz y el título de la portada cuentan lo mismo; la cifra escondida en el sello no aparece a la vista.
> 3. CHECA y su v2 PERSONAL usan las mismas cifras pero distinto gancho, palabras y orden, y distinta portada.
> 4. Nada de logos ni nombres de Uber/DiDi en pantalla, ciudad, país, «gratis», precios ni promesas de ganancia; sin «Soy Eduardo» en videos sin su cara; ortografía («checas», «que cheques»).
> 5. En el storyboard: nada encimado ni ilegible; «Números de ejemplo» visible donde hay cifras.
>
> **Parte B · coherencia y ritmo.** Usa `renders/plan.md` como si fuera el video: compara segundo a segundo lo que entra en pantalla con la palabra que suena.
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
> Responde por video: «OK» si no hay nada que corregir, o la lista de problemas (A y B) en orden de prioridad.

Aplica lo que reporte en `a3.json`, la portada o el copy (la voz es fija), vuelve a correr `a3 --hoja` de los que cambiaron y revisa tú que quedó. Si una propuesta choca con una regla de Eduardo (marca, «el gráfico dice lo que dice la voz», sin logos ajenos), gana la regla.

## 2. Después de renderizar: revisión automática (por video, gratis)
```bash
python3 -I video/scripts/checa.py a3 <proyecto> --render   # ya trae el loop (el último cuadro es el primero)
python3 -I video/scripts/checa.py auditar <proyecto>       # lo medible de la parte B
python3 -I video/scripts/checa.py entregar <proyecto> <CHECA|PERSONAL> <NN>
python3 -I video/scripts/checa.py calidad "marketing/videos/CHECA|PERSONAL/NN - Nombre" <proyecto> [--camara]
```
- `checa.py loop <proyecto>` solo se usa para rehacer videos de estilos viejos (recorta la cola de 2 s del motor y funde el final; guarda el original en `renders/video-sin-loop.mp4`).
- `auditar` usa `renders/video.mp4` (sin la portada). ✗ = se corrige antes de seguir: primer cuadro vacío, último cuadro distinto del primero, cola de más de 1.8 s, texto cortado en un borde por 1 s o más, golpes sin `"adelanto": 0.04`. ⚠ = míralo tú en la hoja de cuadros: subtítulos debajo de y=1450 o una palabra con golpe donde casi nada cambia (medición aproximada; puede marcar un resaltado sutil que sí está).
- `calidad` revisa: archivos completos, mp4 `yuv420p` High 1080×1920 y duración, que el primer cuadro sea la portada, copy con el CTA vigente, «números de ejemplo», sin «Soy Eduardo» (salvo `--camara`) ni «gratis»/precios, DM con su utm, voz sin «$», la última línea de voz con el CTA, pop-ups con «OFERTA», neta = viaje − gasolina, pantalla final y que el sello de la portada no se delate.
- Mira tú la hoja de cuadros con el segundo impreso, sobre todo donde `auditar` dio ⚠:
  ```bash
  ffmpeg -v error -y -i <proyecto>/renders/video.mp4 -vf "fps=2,scale=180:-1,drawtext=text='%{pts\:flt}':fontcolor=yellow:fontsize=20:x=6:y=6,tile=10x10" <proyecto>/renders/auditoria-%d.png
  ```

Con el agente en «OK», todo en ✓ y la hoja revisada, se le manda el lote a Eduardo. **Solo si algo falla aquí se vuelve a renderizar**, y solo ese video.
