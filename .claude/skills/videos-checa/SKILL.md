---
name: videos-checa
description: Hacer videos cortos de Checa (TikTok, Reels, Shorts) con narrador de IA, en «Estilo HyperFrames» (el favorito de Eduardo desde el 14 oct 2026, plantilla checa-promo, proceso económico sin agentes y catálogo de recursos de movimiento para escoger según el momento) o en «1Volt a color» (Remotion 2D, line-art en SVG que se dibuja solo, letra mono, una idea por pantalla, colores por escena, subtítulos para redes y cambio de toma cada 3 s; el 3D queda en pausa). Los guiones se trabajan con auditoría de retención, hooks y etiquetas (en los archivos), pero en el chat se muestran solo de corrido y limpios; siempre con su v2 para la cuenta personal de Eduardo; se guardan en marketing/videos/CHECA/NN - Nombre/guion.md y marketing/videos/PERSONAL/NN - Nombre/guion.md. Cada video va en marketing/videos/CHECA/NN - Nombre/ (y la v2 en marketing/videos/PERSONAL/). Úsala cuando Eduardo pida un video, un guion para video, el siguiente tema de marketing/plan/temas.md, cambiar la voz o la música de un video, o un video sin que él salga a cuadro.
---

# Videos de Checa

> **Prioridad entre conversaciones (Eduardo, 16 oct 2026):** esta skill se cambia desde la conversación **principal** (guiones, carpetas, v2). La **secundaria** solo hace cambios puntuales y no debe revertir estas reglas; si algo choca, gana lo que dice esta skill.

## ⭐ Flujo de cada video, con una aprobación por etapa (Eduardo, 16 oct 2026; orden cambiado el mismo día)
Una etapa a la vez; **no pases a la siguiente sin su «aprobado»** y no adelantes archivos de etapas futuras (copy, dm):
1. **Guion** según las instrucciones de abajo. En el chat, solo el texto limpio y de corrido.
2. **Tabla de animaciones y gráficos HTML por toma** (se muestra en el chat para aprobarla).
3. **Portada** (estilo curiosidad). **Va antes que el video.**
4. **Video** (versión de prueba desde `renders/` con `SendUserFile`; no se commitea todavía).
5. Con **portada y video aprobados**: pon la portada como **primer cuadro** del video con `video/scripts/portada_primer_cuadro.sh <video.mp4> <portada.jpg> <salida.mp4>` (0.2 s, para que TikTok la tome de portada), manda esa versión a Eduardo y, ya aprobada, **commitea el `video.mp4` (con la portada incluida) y la `portada.jpg` una sola vez**.
6. **Copy** (`copy.txt`; el `dm.txt` se crea junto con el copy).
7. Con el copy aprobado el video se da por **hecho** (no por publicado). «Hecho» y «Publicado» son columnas distintas en `marketing/plan/calendario.md`; «Publicado» solo lo marca Eduardo al subirlo.

## ⭐ Cómo se hacen los guiones (Eduardo, 16 oct 2026: «me gusta más como está hoy en día»; vale para v1 y v2 y para cualquier estilo)
**Confirmado por Eduardo (16 oct 2026, «2»): en el chat solo el guion limpio; la auditoría, los hooks y las propuestas visuales por toma van en el `guion.md` y se muestran únicamente si él los pide.** Lo que se responde en el chat (Eduardo, 16 oct 2026): SOLO el guion de voz, limpio y de corrido en un párrafo**, sin `[Corte]`, sin etiquetas, sin tablas, sin auditoría, sin ajustes de honestidad, sin texto de publicación, sin saludo ni pregunta final. Eduardo lo escucha con la lectura en voz alta del chat y cualquier otra cosa se la lee también. El trabajo de abajo (auditoría, hooks, tabla con técnicas, honestidad, texto de publicación) **se hace igual, pero se guarda en los archivos** (`guion.md` de la carpeta del video (`marketing/videos/CHECA/…` o `marketing/videos/PERSONAL/…`), y el `copy.txt` de la carpeta). Si él pide ver la tabla, la auditoría o el texto de publicación, se lo das. Si pide cambios, respondes otra vez solo con el guion corregido. Su «aprobado» (o «dale») es la señal para hacer voz y video.

Actúa como experto en contenido viral y retención. Para cada guion que generes (el análisis va a los archivos, no al chat):
1. **Auditoría previa** de la versión base (la v1 publicada o el borrador): 4–5 viñetas con áreas de oportunidad y los puntos donde el ritmo decae (lo plano, lo repetitivo, lo que llega tarde, promesas que no se cumplen, el cierre).
2. **Hook inicial (0–3 s)**: voz + texto en pantalla + imagen a la vez, con la vivencia del conductor en sus propias palabras.
3. **Hooks intermedios**, uno cada ~4–5 s: open loops, giros, cambios de ritmo, re-enganches visuales, pausa dramática antes del payoff, CTA específico y un cierre que enlaza con el inicio (loop de repetición).
4. **Guion optimizado** en tabla `Corte · Tiempo · Voz · En pantalla/sonido · Técnica`, con la técnica entre corchetes y su función: `[Hook Inicial]`, `[Micro-hook]`, `[Open Loop]`, `[Re-enganche visual]`, `[Giro]`, `[Cambio de ritmo]`, `[Pausa dramática]`, `[Payoff del Open Loop]`, `[Giro emocional]`, `[CTA específico]`, `[Loop de repetición]`.
4b. **Propuestas de animaciones/gráficos HTML por toma** (Eduardo, 16 oct 2026; actúa también como Director de Arte / UI Motion Graphic Designer). Estilo: Dark Mode / Neon UI, fondo #0D0D0D, tipografías limpias o monoespaciadas, neones rosa (#F43F5E), verde (#22C55E), azul y amarillo (#FACC15), tarjetas flotantes de bordes suaves, íconos SVG y datos contundentes. Para cada toma, en el `guion.md`, una tabla con: **Segundaje / frase** (momento exacto del audio), **Componente HTML/UI** (ticket de desglose, contador de números o dinero, barra de estado/progreso, alerta neón, comparativa en dos bloques, ícono SVG interactivo…), **Efecto/animación CSS** (glow, barra que se llena, tachado, cambio de verde a rojo, contador incrementable…) y **Objetivo visual** (cómo refuerza el mensaje).
5. **Voz limpia**: el guion de corrido, sin corchetes ni `[Corte]`. Es **lo único que se muestra en el chat** y lo único que va a ElevenLabs (una línea de voz por escena, según la tabla del archivo); las etiquetas son solo para el archivo.
6. **Ajustes de honestidad** (una línea) y **texto de publicación** (fórmula del calendario).
7. **Ya no se pasa por Gemini** (Eduardo, 16 oct 2026). No cierres con preguntas; con su «aprobado» haces voz y video.
8. **El último corte es el cierre estándar** (ver «Cierre estándar de TODOS los videos» más abajo), en v1 y v2.
9. **Prepara siempre también la v2** (ver abajo). En el chat, si ambos van juntos, primero el guion de la v1 y luego el de la v2, cada uno de corrido y separados solo por una línea que diga «Versión 2» (o «Versión 1»).
Guárdalos en `marketing/videos/CHECA/NN - Nombre/guion.md` (v1) y `marketing/videos/PERSONAL/NN - Nombre/guion.md` (v2) con la columna «Técnica». Ejemplo completo del formato: `marketing/videos/PERSONAL/05 - Cansancio y salud/guion.md`. Esto reemplaza la columna 🪝 y el formato viejo de «corte · voz · en pantalla».

### Dónde se guarda cada video
``marketing/videos/CHECA/NN - Nombre del video/`` para la cuenta de Checa y ``marketing/videos/PERSONAL/NN - Nombre del video/`` para la personal, cada carpeta con `video.mp4`, `portada.jpg`, `copy.txt` y **`dm.txt`** (mensaje para quien comente «CHECA», con el link `https://jeal1498.github.io/checa-web/?utm_source=tiktok&utm_campaign=<nombre-del-video-sin-acentos>`; en PERSONAL `utm_source=tiktok-personal`; así el reporte diario cuenta descargas por video). El nombre empieza con el **número del video** (`NN`, p. ej. `03 - Seguridad de noche`; el siguiente es el 20) para que siempre se ordenen; **la v2 de PERSONAL lleva el mismo número que su original de CHECA**. La fecha de publicación va solo en `marketing/plan/calendario.md` (tabla con fecha, tipo y ✅). Cada carpeta lleva también su **`guion.md`** (el guion completo con su análisis). El texto de la publicación vive solo en `copy.txt`. (Reorganizado el 16 oct 2026: antes eran carpetas `DD-MM - Nombre` y los guiones estaban juntos en `marketing/guiones/`.)

### Versión v2 para la cuenta personal de Eduardo (16 oct 2026)
Eduardo sube cada video a la cuenta de Checa y una **v2** a su TikTok personal. **De ahora en adelante, al entregar el guion de un video, entrega también el guion v2** en `marketing/videos/PERSONAL/NN - Nombre/guion.md` (reglas y formato en ese archivo): mismo tema y datos, otro gancho y otras palabras, con «sígueme» antes del cierre estándar (eslogan, TikToks y link, igual que CHECA) y su texto de publicación. La v2 se rehace completa (voz, escenas, render) en `marketing/videos/PERSONAL/NN - Nombre del video/` (misma estructura), con portada distinta de la v1. Empezó con el video 05 (Cansancio y salud).
**Atajo v2 (plantilla más reciente: `video/hyperframes/gasolina-v2/`; también probado con `video/hyperframes/salud-v2/`, ~35 s, sin agentes):** copia ese proyecto, cambia `SCRIPT.md` (una línea de voz por escena) y las escenas en `generar.py` (usa `t(n, "palabra")` para sincronizar con la voz; texto en máscara, sin logo) y corre **`./ensamblar.sh`**: pide la voz de Fernando a `video/scripts/elevenlabs.py` (con tiempos por palabra, sin Whisper), arma `audio_meta.json` y `STORYBOARD.md`, escenas, subtítulos con el estilo de `estilo-captions.txt`, `index.html`, transiciones y mezcla. Luego `npx hyperframes@0.8.154 check`, `snapshot --at ...`, `render --quality high --output renders/video.mp4` (~1.5 min) y portada con `portada/portada.html` (estilo curiosidad). Lecciones: las máscaras de texto parten de `yPercent: 220` (con 105 se asoma un pedazo); texto en máscara máx. ~950 px de ancho; no rotar las tarjetas con texto (el chequeo las marca como superpuestas). Si ElevenLabs responde 401 `quota_exceeded`, es la cuota de créditos de la clave «Claudio»: avisar a Eduardo.

## Estado y pendientes (16 oct 2026, al cerrar la conversación principal)
- **Calendario vigente:** un video diario por cuenta; CHECA va del 06 (sáb 17 oct) al 15, luego 19 y 17; PERSONAL va un día detrás de CHECA con el mismo tema (el 19 abre el sáb 17). Detalle y estados en `marketing/plan/calendario.md`. Eduardo solo ha publicado: en CHECA 02–05 y un video de ayuda «¿Android no te deja instalarla?»; en PERSONAL el 01, el 18 y la v2 del 05.
- **Hecho (los 5 pasos aprobados):** v2 del 06 (`marketing/videos/PERSONAL/06 - Gasolina cara/`). **También hecha:** v2 del 19 (portada y copy aprobados el 16 oct; tiene el pop-up tapando la oferta y el cierre sin la pausa). La v2 del 05 tampoco tiene la pausa del cierre. No se corrigen salvo que él lo pida.
- **Siguiente:** guion v2 del 07 «Pasajeros y cancelaciones» (lunes 19), luego 08, 09… en el mismo orden. Cada uno con el flujo por etapas de arriba.
- **Plantilla más reciente para una v2:** `video/hyperframes/gasolina-v2/` (copiar el proyecto, editar `SCRIPT.md`, las escenas de `generar.py`, y los tipos de SFX/transiciones en `armar.py`; luego `npm i` una vez y `./ensamblar.sh`). Incluye el cierre estándar con pausa, íconos SVG, comparativas y el teléfono con oferta + pop-up.
- **Lecciones de oficio:** `mask_in` arranca en `yPercent: 220` (con 105 se asoma una rendija); un `fromTo` de destello necesita `"immediateRender": False` o el fondo sale teñido desde el cuadro 0; `carve.mjs` requiere `npm i` en el proyecto; el chequeo marca una superposición cuando el pop-up cae sobre la oferta (aceptado) pero **la oferta y el pop-up deben verse completos a la vez, nunca uno tapando al otro** (pop-up arriba, oferta abajo dentro del teléfono); en tarjetas de comparación todo el contenido va dentro de la tarjeta (flex en columna, el tiempo abajo); no pongas cifras habladas al inicio del video, solo en pantalla; las versiones de prueba se mandan con `SendUserFile` desde `renders/` y el `video.mp4` se commitea una sola vez al aprobarse.
- **Cuenta de ElevenLabs:** si responde 401 `quota_exceeded`, avisar a Eduardo (la clave «Claudio» tiene tope de créditos).
- **Dos conversaciones:** la principal (guiones, v2, carpetas, skill) se cerró en esta fecha; la secundaria hace cambios puntuales (p. ej. subir videos sueltos). Cualquier sesión nueva que trabaje videos debe seguir esta skill y empezar con `git pull origin main`.

## ⭐ Estilo HyperFrames (favorito de Eduardo, 14 oct 2026) — proceso ECONÓMICO

Referencia: `marketing/videos/CHECA/17 - Cómo funciona Checa/video.mp4` (40 s, 9:16), proyecto **`video/hyperframes/checa-promo/`**. A Eduardo le gustó mucho. La primera vez costó ~1.7 M tokens (proceso completo de HyperFrames con 13 agentes); **no repetir eso**. Meta: **~150–300 mil tokens por video**.

**Reglas de ahorro (obligatorias):**
1. **No correr el proceso completo de HyperFrames** (`/hyperframes` → intent, captura de la web, `build-frame`, leer las guías de story/visual/motion, `frame-packets`). Todo eso ya está hecho en `checa-promo`. No leer las skills `hyperframes-*` salvo que algo falle.
2. **Sin agentes.** Cada agente gastó ~115 mil tokens solo en leer reglas. Las escenas las escribes tú, copiando y adaptando las de la biblioteca de abajo. Usa un agente solo si una escena es totalmente nueva y no se parece a ninguna, y con modelo `sonnet` o `haiku`.
3. **Una sola hoja de cuadros** (`snapshot` con 6–8 tiempos) y **un solo render**.

**Proceso (cada video nuevo):**
1. Guion: con el formato de «Cómo se hacen los guiones» (arriba), v1 y v2 → Eduardo lo aprueba directo (sin Gemini) → guárdalo en `marketing/videos/CHECA/NN - Nombre/guion.md` y `marketing/videos/PERSONAL/NN - Nombre/guion.md`.
2. Copia el proyecto: `cp -r video/hyperframes/checa-promo video/hyperframes/<tema>` y borra `compositions/frames/*`, `assets/voice`, `transcripcion`, `renders`, `.hyperframes/artifacts`, `index.html`, `audio_meta.json`, `audio_engine_meta.json`. Se quedan: `frame.md` (diseño ya corregido), `assets/checa-popup.html`, `assets/logo-7eb355dc.svg`, `assets/fonts/` (Fira de la app), `.hyperframes/caption-skin.html` (subtítulos con buen contraste), `alinear.py`.
3. Escribe `SCRIPT.md` (una línea por escena, verbatim) y `STORYBOARD.md` corto: frontmatter (`format: 1080x1920`, `music: none`, `mode: autonomous`) + por escena `src`, `voiceover`, `transition_in`, `sfx` (de la librería: chime, click, click-soft, error, glitch-1/2/3, impact-bass-1/2, key-press, notification, ping, pop, riser, sparkle, typing, whoosh, whoosh-cinematic, whoosh-short) y 2–3 líneas de qué pasa. Sin blueprints ni Video direction largos.
4. Música propia: `python3 -I video/scripts/elevenlabs.py musica <proy>/assets/audio bgm "<prompt>" <seg>` y `audio_meta.json` inicial `{"bgm":{"path":"assets/audio/bgm.mp3","source":"host"},"voices":[],"sfx":[]}`.
5. Voz (Fernando): `HYPERFRAMES_TTS_CONCURRENCY=2 node .claude/skills/product-launch-video/scripts/audio.mjs --script ./SCRIPT.md --storyboard ./STORYBOARD.md --hyperframes . --out ./audio_meta.json --provider elevenlabs --voice dlGxemPxFMTY7iXagmOj` (**concurrencia 2**: con 4 ElevenLabs falla). **Nunca** lo corras sin `--voice` ni con `--help`: vuelve a generar voces con la voz por defecto (mujer) y quedan 2 voces mezcladas. Necesita `pip install elevenlabs`.
6. Transcripción en **español** (la del motor sale en inglés): por cada `assets/voice/NN.wav`, `npx hyperframes transcribe assets/voice/NN.wav --language es --model small --json -d transcripcion/NN`. Luego `sync-durations`, `fetch-sfx` y **al final** `python3 -I alinear.py` (fetch-sfx vuelve a meter las palabras mal; alinear siempre después).
7. Escenas: copia la más parecida de la biblioteca a `compositions/frames/NN-nombre.html`, cambia el id de la composición, `data-duration` (= duración de la voz de esa línea) y textos/cifras; ajusta los tiempos de entrada a las palabras (`audio_meta.json` → `voices[].words`). El `data-composition-id` debe ser **igual al nombre del archivo** (`07-cabeza`), o `assemble-index` falla; los ids de los elementos internos sí van sin dígito al inicio (`f07-…`). Una pista (`data-track-index`) por clip, sin traslapes.
8. Ensamble: `captions.mjs build`, `assemble-index.mjs`, `transitions.mjs inject` + `verify`, agrega `data-audio-group="voiceover"` a cada `<audio id="el-…-voice">` y corre `hyperframes-audio/scripts/carve.mjs --comp ./index.html` (baja la música cuando habla). Si `captions.html` vuelve a salir con placa clara, copia los colores de `checa-promo/compositions/captions.html` (placa negra, pendiente #8A938E, actual #04120A sobre #22C55E, dicha #F1F5F2).
9. `npx hyperframes check` → `snapshot` (una hoja) → `render --quality high --output renders/video.mp4` → copia a `marketing/videos/CHECA/NN - Nombre del video/` (`video.mp4`, `portada.jpg`, `copy.txt`). `.gitignore` del proyecto: `.hyperframes/`, `renders/`, `audio_engine_meta.json`. Commit `[skip ci]`, push a `main` y a la rama.
   - **El `video.mp4` se commitea una sola vez, cuando Eduardo lo aprueba** (o cuando se lo mandas como final). Mientras haya correcciones, mándaselo con `SendUserFile` desde `renders/` (ignorado en git) sin copiarlo a `marketing/`. Cada re-render commiteado suma ~12 MB al historial para siempre (el video 19 llegó a 50 MB así). Los proyectos y la portada sí se pueden commitear en cada cambio.

**Atajo probado (video 18, `video/hyperframes/cuanto-ganas-tu/`)**: en lugar de copiar escena por escena, copia ese proyecto y edita su `generar.py`. Arma las 12 escenas desde `audio_meta.json`, con `t(n, "palabra")` para sincronizar, la oferta genérica, el pop-up real, íconos line-art, el reloj y el CTA de `13-cta`. Lecciones:
- **Sincronía con la voz**: cada texto entra **0.25 s antes** de su palabra (`LEAD`) y la máscara dura **0.3 s**. Si entra justo en la palabra, Eduardo lo siente atrasado.
- `STORYBOARD.md` necesita `- duration: Xs` por escena (= voz + 0.15 s; la última + 1 s). `fetch-sfx` vuelve a poner duraciones viejas en `audio_meta.json`: después corre `ffprobe` sobre cada wav y luego `alinear.py`.
- `captions.mjs build` sale con la placa por defecto: conserva el `captions.html` con estilo Checa y cambia solo `GROUPS` y la duración.
- `carve.mjs` necesita `npm i` en el proyecto; agrega `node_modules/` al `.gitignore`.
- Portada: **estilo curiosidad** (ver «Portadas» abajo). Nunca un cuadro del video.

**Biblioteca de escenas** (`video/hyperframes/checa-promo/compositions/frames/`):
| Escena | Sirve para |
|---|---|
| `01-duda` | Gancho: palabra gigante + oferta genérica blanca + «?» |
| `02-cuanto` | Contador de un monto en fondo claro + empuje de cámara + pregunta |
| `03-lee` | Teléfono con oferta + escáner verde + datos que salen como fichas |
| `04-aviso` | El pop-up real cae sobre la oferta |
| `05-hora-km` | Dos fichas grandes lado a lado con pastillas «meta ✓» |
| `06-neta` | Ticket: viaje − gasolina, contador hasta los netos |
| `07-semaforo` | Semáforo + pop-up fijo que cambia verde/amarillo/rojo |
| `08-metas` | Panel de meta con −/+ que cambia el pop-up en vivo |
| `09-recogida` | Mapa de recogida lejana + pop-up rojo + ficha marcada |
| `10-mejor` | Dos pop-ups (Uber/DiDi), el mejor con borde dorado |
| `11-horas` | Cuadrícula de horas con colores de semáforo |
| `12-resultado` | Fondo verde, frase que se tacha y frase gigante |
| `13-cta` | Logo semáforo que se arma + «checa» + pastilla «Descarga CHECA» + cuentas de TikTok y link de descarga (sin prueba gratis por ahora) |

**Catálogo de recursos de movimiento (Eduardo, 14 oct 2026).** Vienen de un video de referencia que le gustó mucho (`referencia/README.md` → *Referencia 8*, cuadros en `cuadros8.jpg`) y evitan que el video «parezca PowerPoint». **No son obligatorios ni hay cuota**: para cada momento del guion escoge el que mejor cuente esa idea (o ninguno, si la escena ya funciona). Si Eduardo manda otra referencia que le guste, agrégala aquí como recurso nuevo:
1. **Revelar el texto desde una máscara**: las palabras suben desde atrás de un borde (contenedor con `overflow:hidden` y el texto en `yPercent: 100 → 0`, línea por línea, con stagger), en lugar de solo aparecer con opacidad. Para títulos, frases clave y la frase final.
2. **Encadenar los movimientos**: un elemento entra, **empuja** al siguiente y este provoca una acción (la ficha $/km entra y empuja a la de $/h; el pop-up cae y empuja la oferta; el botón «Aceptar» se presiona). En GSAP: el siguiente arranca en el momento del choque (`"<0.8"`), con un pequeño rebote del que recibe el golpe. Nada de elementos que entran solos y sin relación.
3. **La cámara sigue algo importante**: el encuadre (escala + desplazamiento del contenedor de la escena) persigue lo que importa: el dedo que toca la oferta, el carro que avanza por la ruta, la ficha que se pone roja. Puede llevar visor tipo «REC» con esquinas. Más dinamismo y la vista va donde debe.
4. **Un objeto se transforma para presentar la siguiente escena**: transición por forma, no por corte. Ej.: la tarjeta de la oferta se expande y se vuelve el teléfono; el pop-up se encoge y se vuelve una ficha; el número gigante se convierte en la barra del semáforo; el logo se arma desde las 3 luces. Ideal cuando dos escenas comparten un objeto.
5. **Sincronizar entradas y énfasis con la voz**: cuando la voz dice una cifra, la cifra aparece justo en esa palabra; si dice «carro», sale el carro; si dice una acción («rechaza», «acepta»), pasa esa animación. Los tiempos salen de `audio_meta.json` → `voices[].words` (no a ojo). Énfasis = resaltado naranja/verde detrás de la palabra o un pulso de escala.

Al presentar el `STORYBOARD.md`, anota en cada escena qué recurso escogiste y por qué en pocas palabras (p. ej. «máscara: frase clave», «transformar: la oferta pasa a ser el teléfono»).

Reglas de marca: el pop-up sale siempre de `assets/checa-popup.html` sin rediseñar; ningún texto es solo un monto; «Números de ejemplo» donde haya cifras; las cifras de ejemplo deben cuadrar (neta = viaje − gasolina, gasolina ≈ $2/km); oferta genérica sin logos de Uber/DiDi; sin ciudad ni país.

## CTA y ganchos (Eduardo, 16 oct 2026; valen para cualquier estilo)
- **Cierre estándar de TODOS los videos, de CHECA y de PERSONAL (Eduardo, 16 oct 2026).** Por ahora no hay prueba de 7 días: no decir «7 días gratis», «sin costo» ni precios. La voz del último corte dice «Descarga Checa y checa cuánto te queda de cada viaje.» **Con una pausa de ~0.45 s entre «Descarga Checa» y «y checa cuánto te queda…»** (Eduardo, 16 oct 2026): genera esa línea con `video/scripts/cierre_con_pausa.py <assets/voice> <NN>` antes de correr `armar.py` («Checa» normal en la voz, no «CHECA», para que no la deletree). **Pantalla final** (dura ≥ 2 s después de la voz: `PAD_FIN = 2.0`): logo + pastilla «Descarga CHECA» + «solo Android» + TikTok de Checa **@checa.driver** («síguenos en tiktok») + TikTok de Eduardo **@Eduardoalv9802** («creador de checa», por si comparten el video) + link de descarga **jeal1498.github.io/checa-web**. Todo por encima de y≈1450; ejemplo de la pantalla en `video/hyperframes/checa-lee-la-oferta/generar.py` (escena 10; ahí dice «Comenta CHECA / y te mando la app»: cámbialo por «Descarga CHECA»). En los videos de consejo y en las v2 el cierre va después del consejo y del «sígueme». El video 19 (ya publicado) dice «Comenta CHECA»: es anterior a esta regla y no se rehace. Texto de la publicación: venta → «Descarga CHECA y checa cuánto te queda de cada viaje (solo Android)»; consejo → «Guárdalo y compártelo». No hay link en la bio de TikTok. Si vuelve la prueba u otra oferta, él lo avisa y se actualiza aquí.
- **Gancho contundente en 0–3 s** (voz + texto en pantalla + imagen a la vez), sacado de la vivencia del conductor con sus propias palabras, no un resumen. Base aprobada para el video 19: «Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo» con el texto «SE VEÍA BUENO. PERDISTE.».
- **Re-ganchos cada 4–5 s** para que la retención no dependa solo del inicio: cada corte lleva uno (pérdida, tensión, bucle abierto como «mira qué hace», cifra que duele, giro, contraste, control como «tú pones la vara», beneficio, objeción resuelta) y un sonido que lo marca (`impact-bass`, `ping`, `whoosh`). Las técnicas se marcan entre corchetes en la columna «Técnica» (ver «Cómo se hacen los guiones»).
- Si Eduardo manda su propia respuesta (p. ej. un mensaje de WhatsApp) como base: conserva el orden de sus ideas, pasa el «yo» a «tú» y cambia lo que no podamos garantizar («no hay bloqueo» → «Checa no toca Uber ni DiDi, tú decides»).
- Evita preguntas que la voz se contesta sola y los «no es X, es Y».
- **Ortografía de «checar»** (Eduardo la corrigió): «checas», «que cheques», «cheque» (nunca «checes»). Revísalo en el guion antes de generar la voz.

## Estilo «1Volt a color» (Remotion; sigue vigente, más barato)

Desde el anuncio «Recogidas lejanas» Eduardo pidió el estilo 1Volt (2D, line-art en SVG; desde el 14 oct prefiere el Estilo HyperFrames de arriba) porque el 3D gasta demasiados tokens y tiempo (render por software). El 3D le gusta mucho, pero queda **en pausa**: no lo uses salvo que él lo pida.

- Plantilla: **`video/src/Checa/Recogida1Volt.tsx`** (`marketing/videos/CHECA/04 - Recogidas lejanas/video.mp4`). Una idea por pantalla: trazos que se dibujan solos (`Trazo`), cotas punteadas (`Cota`), rejilla de puntos, ticket con la cuenta, número gigante en Fira Code, frase clave en MAYÚSCULAS espaciadas con una línea chica debajo, ícono tachado, pop-up exacto (`POPUP`) y cierre con logo.
- **Más colores**: cada escena tiene su color de trazo y tinte de fondo (`TONO`: naranja, rosa, azul, morado, verde, amarillo, rojo, magenta, cian), además de cian/magenta/amarillo con significado.
- **Subtítulos para redes** (`Subs`): abajo en y≈1500, Inter 800 de 70 px con contorno negro, 1–3 palabras, rebote al entrar y palabras clave a color (`CLAVE`). La frase clave va en y≈1270.
- Render **sin** `--gl=swangle`: `npx remotion render <Id> out/x.mp4 --browser-executable=$B --log=error` (~1 min para 30 s).
- **Guiones aprobados** (ya no pasan por Gemini): guarda cada guion final aprobado en `marketing/videos/CHECA/NN - Nombre/guion.md` (corrido + tabla, con el archivo del video) antes de generar la voz. Cada `[Corte]` es una línea de voz y una escena.
- **Cambios cada 3 s** (Eduardo lo pidió para retener la atención): envuelve `<Escena />` en `<Toma esc={esc}>` (`video/src/Checa/Toma.tsx`), que cambia el encuadre (zoom ±6 % y desplazamiento, con un destello suave) cada 3 s dentro de las escenas largas. Además, cada `[Corte]` es una escena nueva; si una línea de voz pasa de ~4 s, divide la escena en dos dibujos.
- **Checa es una app**: en la voz di «la app Checa» cuando pueda confundirse con Uber/DiDi («la app» a secas = la plataforma).
- **Ahorro de tokens**: reutiliza la plantilla y cambia solo escenas, textos y cifras; revisa como máximo una tira de 4 cuadros antes del render; no rehagas la voz si el texto no cambió.

### Proceso actual (cada video)
1. Entrega el guion del siguiente tema de `marketing/plan/temas.md` con el formato de «Cómo se hacen los guiones» (auditoría, hook, hooks intermedios, guion con etiquetas, voz limpia) y su v2; ~70–90 palabras, frases de ~3 s, y la cuenta de cada cifra.
2. Eduardo lo aprueba (o pide cambios): **guárdalo primero** en `marketing/videos/CHECA/NN - Nombre/guion.md` (corrido + tabla + archivo del video + ajustes que hiciste).
3. Revisa honestidad antes de grabar: «la app Checa» cuando pueda confundirse; «números de ejemplo» en pantalla si la voz no lo dice; nada que no podamos garantizar (cobros de cancelación, «menos de un segundo», bloqueos, ciudad o país en hashtags). Avisa cada ajuste.
4. Voz Fernando Martínez (1 línea por `[Corte]`), escena 1Volt a partir del video anterior (`<Nombre>1Volt.tsx`, `<Toma>` incluida), 1 tira de ~6 cuadros, render, carpeta `marketing/videos/CHECA/NN - Nombre del video/` (`video.mp4`, `portada.jpg`, `copy.txt`) + entrada en `marketing/plan/calendario.md`, marca el tema en `marketing/plan/temas.md`, commit `[skip ci]` y push a `main` y a la rama de trabajo.

### Videos hechos (1Volt salvo que se indique; listos en `marketing/videos/CHECA/NN - Nombre del video/` con su portada y copy)
| Tema | Video | Código |
|---|---|---|
| 1 Trabajar mucho y no ver el dinero | `marketing/videos/CHECA/02 - Trabajar mucho sin lana/video.mp4` (+ `sin-lana.mp4` en 3D) | `SinLana1Volt.tsx` |
| 2 Gasolina cara | `marketing/videos/CHECA/06 - Gasolina cara/video.mp4` | `Gasolina1Volt.tsx` |
| 3 Recogidas lejanas | `marketing/videos/CHECA/04 - Recogidas lejanas/video.mp4` (+ `recogida.mp4`, `recogida2.mp4` en 3D) | `Recogida1Volt.tsx` |
| 4 Horas muertas | `marketing/videos/CHECA/10 - Mejores horas/video.mp4` | `Horas1Volt.tsx` |
| 5 Decidir en segundos | `marketing/videos/CHECA/08 - Decidir en segundos/video.mp4` | `Decidir1Volt.tsx` |
| 6 Comisión alta | `marketing/videos/CHECA/12 - Comisión y tarifas/video.mp4` | `Comision1Volt.tsx` |
| 7 Miedo a rechazar | `marketing/videos/CHECA/14 - Miedo a rechazar/video.mp4` | `Rechazar1Volt.tsx` |
| 8 Desgaste / renta | `marketing/videos/CHECA/15 - Desgaste y renta/video.mp4` | `Desgaste1Volt.tsx` |
| 9 Inseguridad (valor) | `marketing/videos/CHECA/03 - Seguridad de noche/video.mp4` | `Seguridad1Volt.tsx` |
| 10 Bloqueo (valor) | `marketing/videos/CHECA/09 - Cuenta desactivada/video.mp4` | `Bloqueo1Volt.tsx` |
| 11 Pasajeros problemáticos (valor) | `marketing/videos/CHECA/07 - Pasajeros y cancelaciones/video.mp4` | `Pasajeros1Volt.tsx` |
| 12 Cansancio y salud (valor) | `marketing/videos/CHECA/05 - Cansancio y salud/video.mp4` | `Salud1Volt.tsx` |
| 13 Ingreso inestable (valor) | `marketing/videos/CHECA/11 - Ingreso inestable/video.mp4` | `Ingreso1Volt.tsx` |
| 14 Multas y papeles (valor) | `marketing/videos/CHECA/13 - Multas y papeles/video.mp4` | `Multas1Volt.tsx` |
| Campaña beta testers (clips de Eduardo) | `marketing/videos/CHECA/01 - Beta testers/video.mp4` | `Beta.tsx` |

Serie de 14 temas completa en 1Volt (octubre 2026).

**Estilo HyperFrames, video 18**: «Cuánto ganas tú» `marketing/videos/CHECA/18 - Cuánto ganas tú/video.mp4` (41 s; responde de forma abierta al comentario «la app ya te da esa información»; proyecto `video/hyperframes/cuanto-ganas-tu/`).

**Tutorial con capturas, video 20** (16 oct 2026): «Cómo instalar Checa» `marketing/videos/CHECA/20 - Cómo instalar Checa/video.mp4` (4:04). Para tutoriales a partir de una grabación de pantalla de Eduardo: Eduardo prefiere **capturas fijas** de los momentos exactos (no el video corrido), con zoom al botón, señalamiento que aparece con su palabra y un «toque» cuando la voz dice la acción, voz de Fernando y datos personales difuminados. Antes de armarlo, mapear la grabación segundo a segundo (hojas de cuadros a 1 fps) y no fiarse de los tiempos de lo que dice Eduardo (su voz va antes o después de los toques). Código en `video/tutorial-instalar/build.py`.

**HyperFrames** (desde el 13 oct 2026, skill `hyperframes`): «Cómo funciona Checa» `marketing/videos/CHECA/17 - Cómo funciona Checa/ (la versión vieja, video 16, se borró)video.mp4` (47 s, estilo nuevo, proyecto `video/hyperframes/checa-como-funciona/` borrado del repo el 16 oct 2026; sigue en el historial).

**Estado al 13 oct 2026:** calendario del 14 al 28 de octubre en `marketing/plan/calendario.md` (1 al día a las 8 pm, beta primero y luego venta/consejo alternados). Pendiente: publicar según el calendario, juntar opiniones de los beta testers (con permiso) para testimonios en `.agents/product-marketing.md`, y si hay nuevos temas, agregarlos a `marketing/plan/temas.md`, al calendario (número siguiente) y a esta tabla.

Componentes reutilizables: `Trazo`, `Cota`, `Numero`, `Etiqueta`, `Frase`, `Subs`, `Fondo` (en cada archivo), `Num` y `Palomita`/`Tache` (Seguridad, Bloqueo, Pasajeros), pop-ups exactos (`Popup` de `ui.tsx`), `Toma` (cambio cada 3 s). Los videos de valor (9–14) cierran con logo + «+ Seguir», sin prueba gratis.

### Portadas y textos para publicar
- **Portadas: estilo curiosidad (obligatorio desde el 14 oct 2026; a Eduardo le encantó).** Plantilla: `video/hyperframes/cuanto-ganas-tu/portada/portada.html` (referencia `marketing/videos/CHECA/18 - Cuánto ganas tú/portada.jpg`). Copia la carpeta `portada/` al proyecto nuevo y cambia solo los textos y el objeto. Reglas:
  1. **Título arriba en 2 líneas**, MAYÚSCULAS, Fira Sans 800: la 1ª en blanco y la 2ª más grande en amarillo #FACC15. Es una promesa que da curiosidad («LO QUE LA APP / NO TE DICE»), no un resumen del video.
  2. **Un objeto concreto al centro**, ladeado unos −4°: la oferta genérica blanca, el pop-up, un ticket, el tablero, etc. Sin logos de Uber/DiDi.
  3. **La respuesta escondida**: una etiqueta roja #F43F5E con la cifra o la palabra clave **borrosa** (`filter: blur(14px)`), p. ej. «Te quedan ▒▒», más un «?» rojo gigante que se asoma por una esquina del objeto. Lo borroso es lo que obliga a ver el video.
  4. **Pregunta corta abajo** en gris #AFB7B2 con la palabra clave en blanco («¿Cuánto **ganas tú** de verdad?»).
  5. Fondo negro con un brillo rojo suave al centro. Nada importante debajo de y≈1700 ni pegado a la derecha (ahí van los botones de TikTok). Las cifras deben cuadrar con el video.
  - Render (sin Remotion): `/opt/pw-browsers/chromium-1194/chrome-linux/chrome --headless --no-sandbox --disable-gpu --hide-scrollbars --window-size=1080,1920 --screenshot=p.png file://$PWD/portada.html` → `ffmpeg -i p.png -q:v 3 marketing/videos/CHECA/NN - Nombre del video/portada.jpg` (la v2, en `marketing/videos/PERSONAL/…`). Revísala con Read antes de mandarla.
- *(Antes)* composición `Portada` (`video/src/Checa/Portada.tsx`, props `titulo` con `|` para alternar color por línea, `sub`, `color`, `etiqueta`, `fondo` opcional). Render: `npx remotion still Portada out.png --props '{...}'` y pásala a JPG (`ffmpeg -q:v 3`). Guarda la portada junto a su video en `marketing/videos/CHECA/NN - Nombre del video/portada.jpg` (y la de la v2 en `marketing/videos/PERSONAL/NN - Nombre del video/portada.jpg`).
- **Textos y calendario**: `marketing/plan/calendario.md` (fecha, tipo y ✅; el texto de cada publicación vive solo en el `copy.txt` de su carpeta). Sigue la **fórmula** de arriba del calendario: gancho, 1–2 frases con palabras de búsqueda, pregunta, «Descarga CHECA y checa cuánto te queda de cada viaje (solo Android)» (venta, sin «7 días gratis») o «Guárdalo» (consejo) y 4–5 hashtags. No pegar el guion completo. Sin ciudad ni país, sin #ubermexico, sin «link en el perfil» (no hay link en la bio de TikTok). Por cada video nuevo: su `copy.txt` y su fila en el calendario.

### Videos con clips de Eduardo (él a cuadro)
Cuando Eduardo mande sus propios clips para unir (p. ej. la campaña de beta testers, `marketing/videos/CHECA/01 - Beta testers/video.mp4`, código `video/src/Checa/Beta.tsx`):
1. Copia los clips a una carpeta temporal y extrae audio: `ffmpeg -i clip.mp4 -ac 1 -ar 16000 clip.wav`.
2. Transcribe con `video/scripts/transcribir.py` (faster-whisper `small`, lee el wav con numpy porque la versión de `av` no acepta archivos). Con el texto identificas qué clip es cuál aunque los nombres no digan el orden.
3. Revisa orientación con una hoja de cuadros (`ffmpeg -ss 2 -frames:v 1`); ffmpeg ya aplica la rotación del teléfono.
4. `video/scripts/preparar_clips.py`: recorta silencios (primera palabra −0.12 s, última +0.25 s), escala a 1080×1920 30 fps, `loudnorm` −14 y genera `lineasBeta.ts` con el texto del guion de Eduardo (los tiempos de palabra salen de whisper; el texto, del guion).
5. Composición tipo `Beta.tsx`: `OffthreadVideo` por clip, zoom suave 1→1.06, destello en cada corte, acercamiento al pop-up en el clip de demo, etiqueta neón arriba por clip con la oferta, subtítulos 1–3 palabras en mayúsculas (y≈1330, abajo de la cara), música a 0.05 y cierre de 1.5 s con logo + llamada.
6. Comprime para WhatsApp/Facebook: `-crf 24 -preset slow -movflags +faststart` (~20 MB por 40 s). Los clips originales van en `video/public/beta/` (ignorado en git, pesan mucho); solo se versiona el video final.

Lo que sigue describe el estilo 3D (en pausa); las reglas de guion, voz, cifras y entrega aplican igual.

## Estilo 3D «punto dulce» (en pausa) (Eduardo, octubre 2026)

Después de varias pruebas, Eduardo aprobó este estilo y **le gustan las 8 vistas**. Es el que se usa en todos los videos nuevos:

- **Una ciudad 3D** (el mismo mundo en todo el video) hecha con `@remotion/three` (three.js): calle con edificios de bloques de colores (rosa, turquesa, mostaza, morado, verde, naranja) con ventanas encendidas, banquetas lila, calle azul marino con rayas blancas, **cielo de atardecer** (degradado `#2B2A7A → #8E4B9C → #F08A5D`) y neblina morada al fondo. El carro del conductor es **blanco**, en bloques; otros carros de colores circulan.
- **Rutas pintadas en el piso**: recogida sin pago en **magenta punteado**, viaje pagado en **cian sólido**, pin del pasajero magenta flotando y destino como cubo cian.
- **Capas en pantalla (estilo @pixelmente):** etiquetas cortas en MAYÚSCULAS dentro de **recuadros neón** con letra pixel **Silkscreen** (`OFERTA · $80`, `8 KM · PAGADOS`, `6 KM · SIN PAGO`), apiladas arriba sobre una franja oscura; **número gigante** en Silkscreen negrita que cuenta o se **tacha** (amarillo → magenta); **subtítulos** abajo (y≈1500) blancos en Inter negrita con **la palabra clave en amarillo**.
- **Colores con significado:** magenta = lo malo, lo que no te pagan, el número que duele; cian = lo que sí te pagan o la solución; amarillo = dinero, la oferta y la palabra clave. Verde/amarillo/rojo de semáforo solo dentro del pop-up de Checa.
- **Pop-up de Checa idéntico a la app** (`Popup` con `exacto`) cuando sale el teléfono.
- **Cierre:** fondo de atardecer + recuadro neón con la frase final + logo Checa (+ el cierre estándar «Descarga CHECA» y la pregunta en los videos completos).
- Música calmada (`public/musica-calma.mp3`, volumen 0.14), efectos muy suaves; narrador Fernando Martínez a velocidad 1.0; volumen final −14 LUFS.

### Las 8 vistas (todas aprobadas)
**No hay una vista principal: la tercera persona es solo una de las 8.** Para cada escena del guion elige la vista que mejor cuente esa idea (la tabla dice para qué sirve cada una) y justifícala en una frase al presentar el guion. Cambia de vista cada 4–8 s sin salir de la misma ciudad (como las referencias); no repitas la misma vista en dos escenas seguidas salvo que la historia lo pida. Código de cada una en `video/src/Checa/Vistas3D.tsx` (componente `Camara`, casos `v === 0…7`, y `Capas` para lo que va encima):

| # | Vista | Para qué sirve |
|---|---|---|
| 1 | **Detrás del carro** (persecución) | Estar manejando: la recogida, los km que no pagan |
| 2 | **Dron que sube** | Enseñar la ruta completa y las distancias (toma de remate) |
| 3 | **Isométrica fija** (maqueta que gira despacio) | Gasolinera, cuadra, carro esperando |
| 4 | **Dentro del carro** (tablero + teléfono en el soporte con la oferta y el pop-up que cae) | Decidir en segundos, mostrar la app |
| 5 | **Del teléfono a la calle** (la pantalla se encoge y entras a la ciudad) | Gancho: «esta oferta dice $80…» |
| 6 | **Vista lateral** con regla de km abajo | Km totales, costo por km |
| 7 | **Mapa de calor de noche** (zonas que palpitan + etiquetas de hora) | Mejores horas |
| 8 | **Detalle en cámara lenta** (bomba con contador pixel subiendo) | El número que duele: gasolina |

Ejemplo de combinación para 40 s (solo ejemplo, no plantilla fija): 5 (gancho) → 1 (explicación) → 8 (número que duele) → 2 (la cuenta completa) → 4 (el pop-up de Checa) → cierre. Cada tema tendrá su propia mezcla.

### Plantillas
- **`video/src/Checa/Calle3D.tsx`** (`Calle3D`, 6 s con voz): base de un video con narración: cámara en tercera persona que sube al final, rutas que se pintan con la voz (`en("palabra")`), etiquetas neón, número gigante, subtítulos con palabra clave. Exporta `Edificios` (prop `soloLado`) y `Carro3D`.
- **`video/src/Checa/Vistas3D.tsx`** (`Vistas3D`, muestrario de 19 s): las 8 cámaras, la `Gasolinera`, el mapa de `Calor`, el tablero con `Telefono` + `Oferta` + `Popup`, la regla de km y el contador de la bomba.
- Las muestras renderizadas (`marketing/videos/pruebas/`) se borraron del repo el 16 oct 2026; siguen en el historial de git si hacen falta.
- El video 1 (`Gasolina150.tsx`; el mp4 se borró del repo, sigue en el historial) es de un estilo anterior (mundo plano). Sirve solo como referencia técnica (voces, `armarEscenas`, `crearEn`); no copies su aspecto.
- Pendiente de pulir en el primer video real: la gasolinera vista de lejos se ve vacía (agregar autos, letrero, árboles) y la bomba de cerca es plana (agregar manguera y pantalla).

## Antes de escribir

1. Lee `.agents/product-marketing.md` y `referencia/README.md` (7 referencias analizadas: 1Volt1Amp y @pixelmente, con cuadros y transcripciones; sección *Estilo Checa*).
2. **Tema:** sigue el orden de `marketing/plan/temas.md` (14 miedos y problemas del conductor, aprobado por Eduardo). El primero es «Trabajar mucho y no ver el dinero». En los temas donde Checa no ayuda (9–14), el video da un consejo útil y honesto y Checa solo firma al final. No armes calendario ni propongas fechas hasta que él lo pida.
3. Carga con la herramienta Skill **`social`** (ganchos, estructura corta, reglas anti-IA) y **`marketing-psychology`** (elige 1–2 principios y nómbralos). Si chocan con esta skill, manda esta. Si Eduardo manda otra referencia de video, usa también **`video`** y agrega el análisis a `referencia/README.md`.

## 1. Guion (pedir aprobación antes de gastar créditos)

Preséntalo con el formato de «Cómo se hacen los guiones» (arriba) y agrega la columna 🎥 vista (1–8) a la tabla. Agrega «La cuenta» con cada cifra, el color del pop-up si sale, los principios de psicología y 2 ganchos alternativos.

Formato narrativo (de las referencias): escena cotidiana del conductor que intriga o contradice lo que cree → descartar la respuesta obvia → mecanismo con una imagen concreta y su número → giro («y lo que casi nadie se imagina…») → consejo concreto → cierre que regresa a él («en tu próxima oferta…») + llamada a descargar («Descarga Checa y checa cuánto te queda de cada viaje», sin «7 días gratis») + pregunta. **40 s**, 7–8 líneas de voz.

Reglas:
- Narrador que le habla al conductor de **tú**. Nada de «yo».
- Números honestos. Gasolina: $24 el litro, 12 km/l = **$2 por km**. Fórmulas de la app (`Parser.kt`): $/km = tarifa ÷ km totales (con recogida), $/h = tarifa ÷ min totales × 60, neta = tarifa − km × $2. Semáforo con metas por defecto ($8/km y $150/h): cumple las dos → verde, una → amarillo, ninguna → rojo. Si la voz usa $/h neta, dilo («ya sin gasolina»): el pop-up usa la tarifa completa.
- Supuestos (regreso vacío, horas, etc.) se dicen como supuestos o «con números de ejemplo». Nunca inventar testimonios ni ganancias. Sin ciudad ni país. Sin nombrar a la competencia. Sin «no es X, es Y» ni listas «sin X, sin Y».
- Números en la voz con letra («ciento cincuenta pesos»).

## 2. Voz y música (ElevenLabs)

- Plan **Starter** (30 000 créditos/mes); un video de 40 s gasta ~550 caracteres.
- La clave se lee de `ELEVENLABS_API_KEY` (variable del entorno). Si falta, pídele que la agregue en el menú del entorno → Edit, no en el chat. Si la pega en el chat, guárdala solo en `/tmp/claude-0/.el_key` (`umask 077`) y recomiéndale cambiarla. **Nunca** en el repo. No tiene permiso de speech-to-text (para transcribir referencias usa `faster-whisper` con pip).
- Voz **Fernando Martínez** (`dlGxemPxFMTY7iXagmOj`, latina neutra, la más usada de la biblioteca), velocidad **1.0**. Eduardo la eligió en octubre 2026; la anterior (`gncXGfeB8V1Weu7l6QUg`) y las mexicanas Juan Limón, Miguel Zermeño y Juan Campillo no le gustaron (la muestra elegida está en `marketing/marca/voz-fernando.mp3`). La clave ya tiene permiso `voices_read`: para buscar voces usa `/v1/shared-voices` con `sort=usage_character_count_1y`.

```bash
cd video
C=public/<video>; V=dlGxemPxFMTY7iXagmOj
python3 -I scripts/elevenlabs.py voz $C l1 "Primera línea." $V 1.0      # … hasta l8
cd $C && for i in 1 2 3 4 5 6 7 8; do ffmpeg -loglevel error -y -i l$i.mp3 -af "loudnorm=I=-14:TP=-1.5:LRA=7" -ar 44100 -b:a 128k t.mp3 && mv t.mp3 l$i.mp3; done && cd ../..
python3 -I scripts/lineas.py $C src/Checa/lineas<Nombre>.ts l1 l2 l3 l4 l5 l6 l7 l8
```

`lineas.py` imprime cada palabra con su tiempo; úsalas para sincronizar (`en(i, "palabra")` con su puntuación exacta, p. ej. `"catorce."`). Si la suma pasa de ~37 s, recorta texto.

## 3. Escenas (Remotion + three.js)

- Arma `<Nombre>.tsx` a partir de `Calle3D.tsx` y `Vistas3D.tsx`: una sola `ThreeCanvas` con la ciudad, una `Camara` que elige la vista según el segmento de voz, y capas HTML encima (etiquetas `Caja`, número gigante, subtítulos, teléfono/pop-up). Usa `armarEscenas` y `crearEn` de `plantilla.tsx` para los tiempos.
- La cámara se mueve en `useLayoutEffect` con `camera.position.set(...)` y `lookAt(...)`; cambia `fov` por vista si hace falta.
- Lo que tape la vista: el número gigante va arriba solo cuando la cámara está alta (si no, tapa el pin); en la vista lateral oculta los edificios del lado de la cámara (`<Edificios soloLado={-1} />`).
- Fuentes cargadas en `Root.tsx`: Inter, InterDisplay, FiraCode y **Silkscreen** (400/700, `public/Silkscreen-*.woff2`).
- `ui.tsx`: `Telefono`, `Mapa`, `Oferta`, `Popup` (`exacto`, `pastillaColor="#FF7028"` para DiDi, montos con 2 decimales como texto, fichas `"$450/h"`, `"$15.00/km"`, `"20 min"` con `"meta $150"`, `"meta $8"`, `"10.0 km total"`), `Logo`.
- Registra la composición en `video/src/Root.tsx` (1080×1920, 30 fps). Nada importante abajo de y≈1570.

## 4. Revisar y renderizar (3D necesita `--gl=swangle`)

```bash
cd video
npm ci --loglevel=error          # solo si falta node_modules
npx tsc --noEmit
B=/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell
for f in 40 120 200 300 420 600 800 1100; do npx remotion still <Id> /tmp/claude-0/st/c$f.png --frame=$f --gl=swangle --browser-executable=$B --log=error; done
# juntar en una tira (hstack/vstack) y mirarla con Read
npx remotion render <Id> out/x.mp4 --gl=swangle --browser-executable=$B --log=error
ffmpeg -loglevel error -y -i out/x.mp4 -c:v copy -af "loudnorm=I=-14:TP=-1:LRA=9" -ar 48000 -c:a aac -b:a 192k out/<nombre>.mp4
```

Revisa: textos encimados con el pin o los edificios, la cámara metida en un edificio, partes vacías, pop-ups legibles. El render 3D tarda más (un video de 40 s, varios minutos).

## 5. Entregar

1. `cp video/out/<nombre>.mp4 "marketing/videos/CHECA/NN - Nombre/video.mp4"`
2. Commit con `[skip ci]` y `git push origin HEAD:main` (más la rama de trabajo si la hay).
3. Manda el video con `SendUserFile` (`display: "render"`) y explica en 3–5 líneas qué trae y qué revisar, con la duración. Si Eduardo pide los montos, dáselos en tabla y avisa de cualquier cifra que se vea distinta en el pop-up.
4. Marca el tema como hecho en `marketing/plan/temas.md` (nota con el archivo del video).

`video/out/` está en `.gitignore`; se versionan el código, `public/<video>/` y la copia en `marketing/videos/CHECA/`.

## Video de la web (VSL)
El inicio de la web usa `checa-web/media/vsl.mp4`: el video de beta (`marketing/videos/CHECA/01 - Beta testers/video.mp4`) recortado a 26.9 s (clips c1–c4 de `lineasBeta.ts`, sin c5–c7 ni la tarjeta «Comenta YO»), 720×1280, CRF 28, con fundido final. Se rehízo con ffmpeg: `-t 26.9 -vf "scale=720:1280,fade=t=out:st=26.3:d=0.6" -af "afade=t=out:st=26.3:d=0.6"`.
