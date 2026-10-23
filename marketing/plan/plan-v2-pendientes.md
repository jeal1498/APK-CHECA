# Plan para la siguiente conversación: v2 y CTA pendientes (23 oct 2026)

Escrito al cerrar la conversación del 23 oct. Eduardo decidió: **no más videos de consejo** (03, 05, 07, 09, 11, 13 omitidos). Trabajar solo videos de venta/explicativos, por lotes, con una aprobación de Eduardo por etapa.

## 0. Antes de empezar
1. Leer `CLAUDE.md` y la skill `.claude/skills/videos-checa/SKILL.md` (puntos 8a y 8b, «Portadas», «Estado y pendientes»).
2. Traer el trabajo: los commits del 23 oct están en la rama **`ccr-30d86c06-ncj35q`** (no en `main`). `git fetch origin ccr-30d86c06-ncj35q && git checkout ccr-30d86c06-ncj35q` (o fusionar a `main` si Eduardo lo pide).
3. Entorno: Chromium en `/opt/pw-browsers/chromium-1243/chrome-linux64/chrome`; `ffmpeg`; `npx hyperframes@0.8.154`; `pip install faster-whisper` (para revisar voces); `ELEVENLABS_API_KEY` ya viene en el entorno.
4. Responder en español, conciso, en masculino. En el chat solo el guion limpio de corrido (sin etiquetas ni preguntas); análisis y tablas van al `guion.md`.

## 1. Reglas nuevas (23 oct 2026)
- **CTA de comentarios** en todo video nuevo (CHECA y PERSONAL, v1 y v2), antes del cierre estándar (en v2, antes del «Sígueme»): pregunta concreta ligada al tema. Ya está en la skill (8b) y en `CLAUDE.md`.
- **Cifras para la voz:** nunca «$100»; escribir «100 pesos». Con «$» Fernando dice «dólares» o deforma («72 tolos»). Después de generar cada voz, transcribirla (convertir a 16 kHz mono antes: `ffmpeg -i NN.wav -ar 16000 -ac 1 r.wav`; `faster_whisper` con `language="es"`) y confirmar que no diga «dólares». Skill 8a.
- **Hashtags:** fijos `#uber #didi #conductores #choferes #checaapp`, cambiando uno por uno del tema; evitar los de pocas publicaciones. **Pendiente de Eduardo:** captura con el conteo de `#uberdriver`, `#didiconductor`, `#choferesdeapp`, `#conductoresdeapp`, `#conductoresuber`, `#ingresoextra`, `#ahorrodegasolina`, `#trabajoenapp`, `#fyp` para el ranking final → guardar en `marketing/plan/hashtags.md` y actualizar la regla.
- **Flujo por etapa** (una aprobación de Eduardo por etapa, no se adelanta la siguiente): guion → tabla de animaciones → portada → video de prueba → portada como primer cuadro → commit del `video.mp4` (una sola vez) → copy y `dm.txt` → marcar «Hecho» en `marketing/plan/calendario.md`. «Publicado» solo lo marca él.
- Una palabra suelta de Eduardo como «Roger» **no es una aprobación** (lo fue por error el 23 oct): ante la duda, preguntar.
- Portada: marca v2, plantilla `video/hyperframes/checa-marca-v2/portada/portada.html`; si Eduardo no la aprueba, ofrecer 4 ganchos y dejarlo elegir. Zona segura y 300–1620.

## 2. Plantilla de video lista para copiar
`video/hyperframes/comision-v2/` (video 12 v2, probado de punta a punta). Contiene `construir.py` con las escenas: `billete`, `ofertas`, `neto`, `diferencia`, `rejilla`, `control`, `dospopups`, `comentarios` (CTA), más las heredadas `hook`, `anillo`, `datos`, `logo`, `escaneo`, `voz`, `popup_oferta`, `reloj`, `seguir`, `gancho`, `numero`, `popup`, `estados`, `cierre`.
Pasos por video:
1. `cp -r video/hyperframes/comision-v2 video/hyperframes/<tema>-v2`; borrar `assets/voice`, `compositions/frames`, `index.html`, `tiempos.json`, `renders`, `.hyperframes`, `portada/`; cambiar `name`/`id` en `package.json` y `meta.json`.
2. Editar `SCRIPT.md` (una línea de voz por escena, 4 espacios al inicio) y `escenas.json` (un objeto por línea, mismo orden; la última es `{"tipo":"cierre"}`; el CTA de comentarios va antes de `seguir`). `sfx` por escena: `["archivo", "palabra o segundo", volumen]`.
3. Si una escena nueva no existe, añadir una función `esc_<tipo>` en `construir.py` y registrarla en `TIPOS_V12` (ver `esc_comentarios` como modelo). Reglas: textos que empiecen con letra en los ids, nada en la banda de subtítulos y = 1160–1480, «Números de ejemplo» donde haya cifras, nada de logos de Uber/DiDi, pop-ups del mismo tamaño y neta = viaje − gasolina.
4. `python3 -I construir.py` (genera voz solo para líneas nuevas) → transcribir y revisar la voz → `npx --yes hyperframes@0.8.154 check` → `snapshot --at <tiempos>` y revisar la hoja de cuadros → `render --quality high --output renders/video.mp4` (~2 min).
5. Con portada y video aprobados: `bash video/scripts/portada_primer_cuadro.sh renders/video.mp4 <portada.jpg> renders/final.mp4`; mandar `final.mp4` con `SendUserFile`; al aprobarlo, copiarlo como `video.mp4` a la carpeta del video y commitear (los renders de prueba no se commitean).
6. Si cambia el orden de las líneas de voz, renombrar o borrar los `assets/voice/NN.*` que cambien (se cachean por número).

## 3. Lote 1 (en este orden)
| # | Trabajo | Estado al 23 oct | Fecha objetivo |
|---|---|---|---|
| 10 | **v2 PERSONAL** «Mejores horas» | Guion hecho (`marketing/videos/PERSONAL/10 - Mejores horas/guion.md`), **falta aprobación**; sigue tabla, portada, video, copy | Ya va tarde (CHECA lo publicó el 22) |
| 14 | **v1 CHECA revisado + v2 PERSONAL** «Miedo a rechazar» | Guiones hechos (v1 revisado en el `guion.md` de CHECA; v2 en `PERSONAL/14 - Miedo a rechazar/guion.md`), **falta aprobación**. El `video.mp4` actual de CHECA (1Volt) no tiene CTA, usa «link del perfil» y cifras con «$»: se rehace en HyperFrames | CHECA dom 25, v2 lun 26 |
| 15 | **v1 CHECA revisado + v2 PERSONAL** «Desgaste y renta» | Sin hacer: leer `CHECA/15…/guion.md`, revisar con las reglas nuevas, escribir v1 revisado y v2 | CHECA lun 26, v2 mar 27 |

Hacer cada etapa a la vez para los 3 videos (los 3 guiones, luego las 3 tablas, luego las 3 portadas…), así Eduardo aprueba en lote. Sus datos: 14 → $70/13 km → $44 y $120/12 km → $96 (gasolina $2/km); en 10 → $6/km a las 3 pm y $11/km a las 7 pm como ejemplo.

## 4. Lotes siguientes
- **Lote 2:** `22 - Cuánto vale tu kilómetro` (v1 y v2 con guion ya hecho en sus carpetas, sin fecha fija), `19 - Checa lee la oferta` (solo v1 CHECA, agregar CTA; CHECA lo publica mar 27; la v2 ya está publicada) y `17 - Cómo funciona Checa` (v1 con CTA, CHECA mié 28; su v2 aún no existe).
- **Lote 3:** v2 de `04 - Recogidas lejanas` (la de más vistas en CHECA, 980) y de `02 - Trabajar mucho sin lana`.
- **Dudosos (preguntar a Eduardo):** `20 - Cómo instalar Checa` (tutorial) y `21 - Beta testers corto` (él a cámara): probablemente no necesitan v2.
- Revisar las voces de los videos aún sin publicar que tengan cifras con «$» (14, 15, 17, 19, 22).

## 5. Estado de los videos al 23 oct 2026
- **Publicados:** CHECA 02, 03, 04, 05, 06, 08, 10, 12, 21 y el de «Ayuda Android»; PERSONAL 01, 05, 06, 07, 08, 12 (23 oct), 18, 19.
- **Hecha pero sin publicar:** CHECA 14, 15, 17, 18, 19, 20 y 22 (en borrador, con guion por aprobar).
- **Tareas ajenas a los videos que siguen pendientes (ver `CLAUDE.md`):** gráficos de la ficha de Play, declaraciones de accesibilidad y servicio en primer plano, prueba cerrada con 12 testers, Fase 7 (Play Billing antes del 1 feb 2027).

## 6. Lote 1 terminado (23 oct 2026, noche)
- **Hechos y aprobados por Eduardo** (guion, tabla, portada, video con portada de primer cuadro y copy): PERSONAL 10 v2, CHECA 14 v1 (rehecho en HyperFrames, sustituye al 1Volt), PERSONAL 14 v2, CHECA 15 v1 (rehecho en HyperFrames) y PERSONAL 15 v2. Los `video.mp4` ya están en git; «Publicado» lo marca Eduardo.
- Proyectos HyperFrames: `video/hyperframes/{mejores-horas-v2,rechazar-v1,rechazar-v2,desgaste-v1,desgaste-v2}/` (cada uno con `construir.py` copiado de `comision-v2`, que ahora trae las escenas del lote: `vueltas, tachado, relojes, barras, balanza, horas, comentarios2, arrepentido, dedo, netos, cuenta, odometro, bomba, desgaste, renta, cobros, escaneo2, alcancia, carro`). Portadas: `video/hyperframes/{mejores-horas-v2,rechazar,desgaste}/portada/`.
- `comentarios2` (CTA) se parametriza con `pregunta`, `opciones` y `fuente`; `dospopups` acepta `p1`, `p2` y `palabra`.
- Todo el copy usa los 5 hashtags fijos; en 10 v2 cambia `#choferes` por `#mejoreshoras` y en 15 (v1 y v2) por `#rentadeauto`. El `dm.txt` de CHECA conserva su `utm_source=tiktok`; el de PERSONAL usa `tiktok-personal`.
- **Siguiente: Lote 2** (22 v1 y v2, 19 v1 con CTA, 17 v1; la v2 del 17 aún no existe). Pendiente de Eduardo: captura con el conteo de hashtags.
