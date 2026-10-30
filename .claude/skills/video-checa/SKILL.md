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
Una aprobación de Eduardo por etapa, con **CHECA y su v2 PERSONAL juntos en cada etapa** (dos guiones, dos tablas, dos portadas, dos videos, dos copys): así son 5 aprobaciones por tema y no 10. No adelantes etapas: él revisaría cosas que aún pueden cambiar.
1. **Guion** de las dos versiones con la skill `guion-checa`.
2. **Tabla de animaciones**: `checa.py tabla <guion.md> --guardar` la saca del «Guión Desglosado» (no la reescribas a mano); en el chat va la tabla corta.
3. **Portadas** (ver «Portada»), antes que el video.
4. **Videos de prueba**: `SendUserFile` desde `renders/` (ignorado en git).
5. **Con su «aprobado» del video, entrega sin volver a preguntar**: `checa.py entregar <proyecto> <CHECA|PERSONAL> <NN>` pone la portada aprobada como primer cuadro (0.2 s), copia `video.mp4` y `portada.jpg` a la carpeta y revisa el formato. Es un paso mecánico sobre dos cosas ya aprobadas; el mp4 se commitea **una sola vez** (cada mp4 commiteado engorda el repo para siempre).
6. **Copy y DM**: escribe `copy.txt` y genera el DM con `checa.py dm <carpeta>`; Eduardo aprueba el copy.
7. **Hecho**: `checa.py hecho <carpeta> <AAAA-MM-DD> [tipo]` (calendario web + `calendario.md`). «Publicado» lo marca Eduardo; cuando avise: `checa.py publicado <carpeta> <AAAA-MM-DD>`.
8. **Subir**: `checa.py subir "<mensaje con las líneas de atribución>"` hace commit y push a `main` y a la rama de la sesión en los dos repos. `build.yml` ignora `marketing/**`, `video/**` y `.claude/**`: no hace falta `[skip ci]`.

Carpetas: `marketing/videos/CHECA/NN - Nombre/` y `marketing/videos/PERSONAL/NN - Nombre/` (`video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`, `guion.md`); la v2 usa el mismo `NN`.

## Herramienta: `video/scripts/checa.py`
Todo lo repetible vive aquí (corre sin argumentos para ver la ayuda): `nuevo`, `construir`, `portada`, `revisar`, `entregar`, `dm`, `tabla`, `hecho`, `publicado`, `subir`. Úsala en vez de rehacer los pasos a mano: ya probó que da el mismo resultado y evita errores (p. ej. `portada` se niega si la cifra del sello ya está a la vista).

## Marca (los dos modos)
La fuente es `jeal1498/checa-web`, carpeta `marca/`. Para cualquier duda usa la skill `marca-checa`.
- **Colores:** asfalto `#0C0C10`, tablero `#16161C`, hueso `#F4F4EF`. El color solo decide: verde `#22C55E` es lo que ganas, amarillo `#FACC15` lo que recuerdas, rojo `#EF4444` lo que pierdes.
- **Letras:** Bricolage Grotesque 800 en títulos y números, DM Sans en texto y JetBrains Mono en datos. Las fuentes están en `video/hyperframes/checa-marca-v2/assets/fonts/`.
- **Contenido:** sin logos ni nombres de Uber o DiDi (los pop-ups dicen «OFERTA»). Sin ciudad ni país. Sin «gratis», precios ni promesas de ganancia. «Números de ejemplo» donde haya cifras, y la neta siempre cuadra (viaje − gasolina).
- **Mp4:** `yuv420p` con perfil High. Revísalo con `ffprobe` antes de mandarlo, porque el teléfono de Eduardo no reproduce 4:4:4.

## Portada
- **Cómo:** escribe `portada/portada.json` en el proyecto y corre `checa.py portada <proyecto>`. Campos: `etiqueta`, `titulo` (lista de 1–2 líneas), `precio`, `linea`, `recogida`, `viaje`, `sello_texto`, `sello_valor` (la cifra real escondida) y, si hace falta, `titulo_px`. La plantilla es `video/hyperframes/_base/portada/portada.html`.
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
- El estado real está en el calendario web (`PWA-Checa-Multimedia/calendario-d4f81c/data.json`); `calendario.md` lo acompaña. `checa.py hecho` y `checa.py publicado` actualizan los dos (entrada, mp4, miniatura y fila). Elige el primer día libre de esa cuenta y díselo a Eduardo.
