---
name: video-checa
description: Produce los videos de Checa para TikTok, Reels y Shorts con la marca v2, desde un guion ya aprobado hasta el video listo para subir. Cubre voz, escenas, render, portada, primer cuadro, copy, DM y calendarios, en dos modos. Modo narrador de IA (cuenta CHECA y su v2 PERSONAL, sin la cara de Eduardo), con HyperFrames y construir.py. Modo a cámara (videos donde sale Eduardo), con spec.json y componer.py. Úsala siempre que haya que hacer, rehacer, corregir, renderizar o entregar un video de Checa, su portada o su copy, o marcarlo en el calendario, aunque Eduardo no nombre la skill. El guion se escribe antes con `guion-checa`.
---

# Video Checa

Lleva un guion aprobado a un video terminado, con una aprobación de Eduardo en cada etapa. Él revisa desde el teléfono y escucha el chat en voz alta. Por eso cada entrega es un archivo o un texto corto, sin explicaciones largas.

## Elige el modo
- **Sin la cara de Eduardo** (CHECA y su v2 PERSONAL): lee `references/narrador.md`.
- **Con Eduardo a cámara** (cuenta @Eduardoalv9802, tutoriales): lee `references/camara.md`. Para elegir qué escena acompaña cada frase usa `plantillas/guion-escenas.md`.

## Flujo por etapas (los dos modos)
Avanza solo con su «aprobado» y no adelantes archivos de etapas futuras. Si adelantas, él revisa cosas que pueden cambiar y pierde tiempo.
1. **Guion**, con la skill `guion-checa`.
2. **Tabla de animaciones por toma**: una tabla corta en el chat (toma, frase, animación) y la versión completa en `guion.md`.
3. **Portada**, antes que el video (ver «Portada»).
4. **Video de prueba**: mándalo con `SendUserFile` desde `renders/`, que git ignora.
5. **Primer cuadro**: con portada y video aprobados, corre `video/scripts/portada_primer_cuadro.sh <video.mp4> <portada.jpg> <salida.mp4>` y manda el resultado. Mientras esperas su aprobación, sube a git solo `portada.jpg` y deja el mp4 en `renders/`. Con su «aprobado», copia el mp4 como `video.mp4` y commitéalo **una sola vez**. Cada mp4 commiteado engorda el repo para siempre.
6. **Copy y DM** (ver «Copy y DM»).
7. **Hecho**: con el copy aprobado, registra el video en los calendarios (ver «Calendarios»). «Publicado» solo lo marca Eduardo cuando lo sube.

Carpetas: `marketing/videos/CHECA/NN - Nombre/` y `marketing/videos/PERSONAL/NN - Nombre/`. Cada una lleva `video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt` y `guion.md`. La v2 usa el mismo `NN` que su original.

## Marca (los dos modos)
La fuente es `jeal1498/checa-web`, carpeta `marca/`. Para cualquier duda usa la skill `marca-checa`.
- **Colores:** asfalto `#0C0C10`, tablero `#16161C`, hueso `#F4F4EF`. El color solo decide: verde `#22C55E` es lo que ganas, amarillo `#FACC15` lo que recuerdas, rojo `#EF4444` lo que pierdes.
- **Letras:** Bricolage Grotesque 800 en títulos y números, DM Sans en texto y JetBrains Mono en datos. Las fuentes están en `video/hyperframes/checa-marca-v2/assets/fonts/`.
- **Contenido:** sin logos ni nombres de Uber o DiDi (los pop-ups dicen «OFERTA»). Sin ciudad ni país. Sin «gratis», precios ni promesas de ganancia. «Números de ejemplo» donde haya cifras, y la neta siempre cuadra (viaje − gasolina).
- **Mp4:** `yuv420p` con perfil High. Revísalo con `ffprobe` antes de mandarlo, porque el teléfono de Eduardo no reproduce 4:4:4.

## Portada
- **Plantilla:** `video/hyperframes/cuanto-vale-km-22-v2/portada/portada.html`. Se renderiza con `r.js` y Playwright: `NODE_PATH=$(npm root -g) node r.js <ruta>/portada.html <salida.png>`, y luego `ffmpeg -q:v 3 portada.jpg`.
- **Receta:**
  - Fondo asfalto y etiqueta mono gris.
  - Título enorme en Bricolage 800 hueso, de máx. 8 palabras, a 128–164 px según el largo.
  - Tarjeta clara ladeada −3° que imita una oferta de app de viajes: VIAJE, ★ 4.92, precio, recogida, viaje y «Aceptar». Nunca logo ni nombre de apps.
  - Sello amarillo con una cifra borrosa.
  - Semáforo con ✓ más «Checa», y la raya punteada.
- **Preferencias de Eduardo:**
  - El título funciona mejor si es el **gancho del video** (rechazó «Pagan 90 y deja más» y eligió «Te ofrecen 110 y te quedan 54»).
  - El sello esconde una cifra que **no** aparece ni en el título ni en la tarjeta. Si no, ya no genera curiosidad.
  - Las cifras cuadran con el video.
  - La portada de la v2 es distinta a la de la v1.

## Copy y DM
- **`copy.txt`:** gancho con cifra, una línea de qué hace Checa con «(números de ejemplo)», una pregunta para comentar, el CTA «Comenta «CHECA» y te mando el acceso a la prueba en Google Play (solo Android).» y 5–6 hashtags.
  - En videos **sin su cara** no digas «Soy Eduardo» ni «armé Checa»: quien lo lee espera ver a alguien y no hay nadie.
  - Solo se presenta en los videos donde sale a cámara.
- **`dm.txt`:** el mensaje de siempre con el link `https://jeal1498.github.io/checa-web/registro/?utm_source=tiktok&utm_campaign=<nombre-sin-acentos>`.
  - En PERSONAL usa `utm_source=tiktok-personal`.
  - Con el UTM se cuenta qué video trae registros.

## Calendarios
- **Calendario web** (`PWA-Checa-Multimedia/calendario-d4f81c/data.json`, el estado real):
  1. Agrega la entrada copiando la forma de otra: `c`, `n`, `t`, `f`, `e: "listo"`, `p: null`, `tipo`, `copy`, `dm`, `th`, `v`, `mb`, `files`.
  2. Copia el video a `v/CNN.mp4` o `v/PNN.mp4`.
  3. Pon una miniatura de 360 px en `t/`.
  4. Guarda con `indent=1`.
- **`marketing/plan/calendario.md`:** agrega la fila con ✅ (hecho).
- **Fecha:** elige el primer día libre de esa cuenta y díselo a Eduardo.
- **Publicado:** cuando él avise que lo subió, pon `e: "pub"` y `p: <fecha>` en los dos calendarios.
- **Git:** commit y push a `main` en los dos repos, y también la rama de la sesión. `build.yml` ignora `marketing/**`, `video/**` y `.claude/**`, así que no hace falta `[skip ci]`.
