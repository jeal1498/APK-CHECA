---
name: video-checa
description: Produce los videos de Checa para TikTok, Reels y Shorts con la marca v2, desde un guion ya aprobado hasta el video listo para subir. Cubre voz, escenas, render, portada, primer cuadro, copy, DM y calendarios, en dos modos. Modo narrador de IA (cuenta CHECA y su v2 PERSONAL, sin la cara de Eduardo), con HyperFrames y construir.py. Modo a cámara (videos donde sale Eduardo), con spec.json y componer.py. Úsala siempre que haya que hacer, rehacer, corregir, renderizar o entregar un video de Checa, su portada o su copy, o marcarlo en el calendario, aunque Eduardo no nombre la skill. El guion se escribe antes con `guion-checa`.
---

# Video Checa

Lleva un guion aprobado a un video terminado, con una aprobación de Eduardo en cada etapa. Él revisa desde el teléfono y escucha el chat en voz alta. Por eso cada entrega es un archivo o un texto corto, sin explicaciones largas.

## Elige el modo
- **Sin la cara de Eduardo** (CHECA y su v2 PERSONAL): lee `references/narrador.md`.
- **Con Eduardo a cámara** (cuenta @Eduardoalv9802, tutoriales): lee `references/camara.md`. Para elegir qué escena acompaña cada frase usa `plantillas/guion-escenas.md`.

## Flujo: dos aprobaciones por lote (Eduardo, 30 oct 2026)
Eduardo aprueba **dos veces por lote** y no cinco por video: así el tiempo no se le va en idas y vueltas. Un lote son los temas que él dé (3–4 lo normal), cada uno con CHECA y su v2 PERSONAL.
1. **Aprobación 1 · guiones:** todos los guiones del lote en un solo mensaje, con la skill `guion-checa`. Si pide cambios, se corrigen solo esos y se vuelve a mostrar el lote.
2. **Producción sin parar** (no le preguntes nada en medio), para cada video:
   - Tabla de animaciones: `checa.py tabla <guion.md> --guardar` (no se reescribe a mano).
   - Portada: `portada/portada.json` + `checa.py portada <proyecto>`.
   - Video: `checa.py construir <proyecto> --render` y `checa.py revisar`.
   - Entrega: `checa.py entregar <proyecto> <CHECA|PERSONAL> <NN>` (portada como primer cuadro; copia `video.mp4` y `portada.jpg`).
   - Copy y DM: escribe `copy.txt` y corre `checa.py dm <carpeta>`.
   Revisa tú cada hoja de cuadros y cada portada antes de seguir: él solo verá el resultado final, así que lo que se te pase llega hasta él.
   **Control de calidad (siempre, en cada lote, aunque sea de un video):** `checa.py calidad <carpeta> <proyecto>` por video y un agente revisor con ojos frescos para todo el lote; cómo, en `references/calidad.md`. Solo con todo en ✓ y el revisor en «OK» se le manda el lote.
3. **Aprobación 2 · revisión final:** un solo mensaje con todo el lote: por video, el mp4 (con `SendUserFile`; la portada ya va en el primer cuadro), el copy en texto y la fecha propuesta. Si algo no le gusta, se corrige **solo eso** (portada, una escena, una línea de voz o el copy) y se manda de nuevo solo lo corregido.
4. **Hecho y subida:** con su «aprobado», `checa.py hecho <carpeta> <AAAA-MM-DD> [tipo]` por video y `checa.py subir "<mensaje con las líneas de atribución>"`. El `video.mp4` se commitea **una sola vez**, ya aprobado: hasta la aprobación 2 los mp4 viven en `renders/` y en la carpeta sin commitear (si un hook pide subir, apártalos con `mv` a `renders/`). «Publicado» lo marca Eduardo; cuando avise: `checa.py publicado <carpeta> <AAAA-MM-DD>`.
`build.yml` ignora `marketing/**`, `video/**` y `.claude/**`: no hace falta `[skip ci]`.

Carpetas: `marketing/videos/CHECA/NN - Nombre/` y `marketing/videos/PERSONAL/NN - Nombre/` (`video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`, `guion.md`); la v2 usa el mismo `NN`.

## Herramienta: `video/scripts/checa.py`
Todo lo repetible vive aquí (corre sin argumentos para ver la ayuda): `nuevo`, `construir`, `portada`, `revisar`, `entregar`, `dm`, `tabla`, `calidad`, `hecho`, `publicado`, `subir`. Úsala en vez de rehacer los pasos a mano: ya probó que da el mismo resultado y evita errores (p. ej. `portada` se niega si la cifra del sello ya está a la vista).

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
