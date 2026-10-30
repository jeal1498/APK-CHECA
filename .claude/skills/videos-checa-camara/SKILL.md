---
name: videos-checa-camara
description: Hacer los videos de Checa con la marca v2 (asfalto y hueso, semáforo con significado, Bricolage Grotesque, DM Sans, JetBrains Mono). Dos modos. (1) Eduardo a cámara (cuenta personal @Eduardoalv9802 y tutoriales con su cara): gancho en pantalla, subtítulos v2 palabra por palabra, escenas gráficas animadas que acompañan el guion y pantalla final estándar. (2) Narrador de IA sin su cara (CHECA y su v2 PERSONAL): proyecto de HyperFrames con SCRIPT.md + escenas.json + construir.py, voz de Fernando v3, portada y flujo por etapas. Úsala al editar un video suyo a cámara, o al hacer, rehacer o corregir cualquier video de Checa. Los guiones se escriben con `guion-remotion-checa`.
---

# Videos de Checa (marca v2): a cámara y con narrador de IA

> **30 oct 2026:** esta skill absorbió a `videos-checa` (eliminada a petición de Eduardo; su historial —1Volt, 3D y el proceso HyperFrames viejo— sigue en git). Las secciones de abajo hasta «Cómo habla Eduardo» son del modo **a cámara**; el modo **narrador de IA** está en «Videos con narrador de IA (sin su cara)», al final.

Port de la skill `videos-karen` (repo `jeal1498/videos-karen-trujillo`) a la marca Checa. Misma mecánica: un `spec.json` + `words.json` (transcripción por palabra) → `componer.py` arma `public/index.html` de HyperFrames → `check` → render. **Estado (17 oct 2026):** probada con un video sintético (`check` sin errores, escenas revisadas en fotogramas y un render corto); primer uso real con 5 clips de Eduardo (17 oct 2026): `CHECA/21 - Beta testers corto`.

## Marca (manda `jeal1498/checa-web`, carpeta `marca/`; usa la skill `marca-checa`)
- Asfalto `#0C0C10`, tablero `#16161C`, humo `#3A3A45`, hueso `#F4F4EF`. **El color solo decide:** verde `#22C55E` (lo que ganas), amarillo `#FACC15` (lo que recuerdas), rojo `#EF4444` (lo que pierdes).
- Bricolage Grotesque 800 (títulos y números), DM Sans (texto), JetBrains Mono (datos y etiquetas). Fuentes en `video/hyperframes/checa-marca-v2/assets/fonts/` (`preparar.sh` las copia).
- Sin logos de Uber ni DiDi (pantallas genéricas, `"app": "APP"`), sin ciudad ni país, sin promesas de ganancia, «Números de ejemplo» visible donde haya cifras, la neta cuadra (viaje − gasolina).
- Por ahora no hay prueba de 7 días: no decir «gratis», «sin costo» ni precios.
- Ortografía: «checas», «que cheques», «cheque» (nunca «checes»).

## Flujo con una aprobación por etapa (el de Checa, 16 oct 2026)
1. **Guion / transcripción:** el texto corregido de lo que Eduardo dice, de corrido. Sin etiquetas ni tablas.
2. **Tabla de gráficos por toma** (chat): `Segundo · Frase que dice · Elemento en pantalla` (gancho, rótulo, escenas con su tipo, pantalla final). Se piensa con `plantillas/guion-escenas.md`.
3. **Portada** (estilo marca v2: `video/hyperframes/checa-marca-v2/portada/portada.html`; va antes que el video). Con la tarjeta de oferta parecida a la de una app de viajes y las preferencias de Eduardo del 24 oct 2026 (ver «Portadas» al final de esta skill).
4. **Video:** versión de prueba con `SendUserFile` desde la carpeta de trabajo (no se commitea). Con portada y video aprobados: `video/scripts/portada_primer_cuadro.sh <video.mp4> <portada.jpg> <salida.mp4>`.
5. **Copy** (`copy.txt` sin link y `dm.txt` con el link y su `utm_campaign`): `utm_source=tiktok-personal` en la cuenta de Eduardo y `utm_source=tiktok` en la CHECA oficial. Eduardo aprueba el copy antes de guardar.
6. **Hecho:** copiar a `marketing/videos/PERSONAL/NN - Nombre/` (o `CHECA/` si va a la cuenta oficial; agrega su fila en `marketing/plan/calendario.md`) (`video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`, `guion.md`), una sola vez y ya aprobado. Commit con `[skip ci]` (si no, GitHub Actions compila el APK). «Hecho» no es «Publicado».

## Pasos técnicos (`SK=.claude/skills/videos-checa-camara`)
1. `bash $SK/scripts/preparar.sh <original.mp4> <trabajo>` (trabajo en el scratchpad, no en el repo): re-codifica a 1080×1920, voz a −14 LUFS, copia fuentes y gsap, transcribe con Whisper (`words.json`).
   **Si Eduardo manda varios clips cortos** (lo habitual): transcribe cada uno con `transcribir.py <clip> w<N>.json`, escribe un `plan.json` (ejemplo en `ejemplos/plan-clips.json`) con lo que se conserva de cada clip y corre `python3 $SK/scripts/unir_clips.py <trabajo> plan.json`: recorta, une con corte directo, nivela, genera `public/input-video.mp4`, `words.json` y `union.json` (de ahí salen los tiempos del spec). Copia a mano las fuentes y `gsap.min.js` a `<trabajo>/public/` si no corriste `preparar.sh`.
   **Cierre:** `python3 -I video/scripts/cierre_con_pausa.py <trabajo>/voz 99` (voz de Fernando con su pausa; el texto vigente es el de «Comenta CHECA…») y pásalo en `plan.json` → `cierre`; `unir_clips.py` lo agrega al volumen de Eduardo, sobre el último cuadro congelado, y sus palabras entran a los subtítulos. Pon `cierre.ini` del spec en `cierre_ini` de `union.json`.
2. Revisa la transcripción y corrige palabras dudosas con `ajustes` (`{"16.46": "checa"}`) o `correcciones`.
3. Escribe `spec.json` (copia `ejemplos/spec-escenas.json`). Los tiempos se anclan a palabras: `["decides", 30]` = primera «decides» después de 30 s.
4. `python3 -I $SK/scripts/componer.py <trabajo> spec.json` → `public/index.html`. Lee los `AVISO` (empalmes, escenas fuera de 3 a 5 s, más del 40 %).
5. `cd <trabajo> && npx --yes hyperframes@0.8.163 check public`. Los avisos de estructura (`timeline_track_too_dense`, `nested_structure_needs_subcomposition`) son normales aquí. El de contraste de la palabra por venir sobre el video es el mismo de la plantilla oficial.
6. `bash $SK/scripts/renderizar.sh <trabajo> <video_web.mp4>` (≈5–6 min por 72 s, por software).
7. `bash $SK/scripts/revisar.sh <video_final.mp4> portada.jpg copy.txt spec.json` antes de pedir el «aprobado» del copy.

## Elementos del spec
- `gancho` `{texto, resalta?, ini, fin}`: placa arriba (y=260) con la pregunta del conductor en sus palabras; `resalta` va en amarillo. Entra en 0–3 s.
- `nametag` `{nombre, detalle, ini, fin}`: rótulo a la izquierda (y=1000). No coincide con el gancho.
- `colores` `{palabra: verde|amarillo|rojo}`: la palabra, ya dicha, queda en ese color (cifras que ganas o pierdes).
- `escenas` (a pantalla completa; la voz no se corta y los subtítulos quedan encima). Campos comunes: `id`, `tipo`, `ini`, `fin`, `nivel`? (`verde|amarillo|rojo|hueso`):
  - `dato` `{etiqueta, titulo, texto?}` con la raya de carretera bajo el título.
  - `lista` `{titulo, items[{texto, nivel?, en?}]}`.
  - `comparar` `{a:{etiqueta,texto}, b:{etiqueta,texto,nivel}, b_en?}`: arriba lo que parecía, abajo (en color) lo que fue.
  - `pasos` `{titulo, pasos[{texto, en?}]}`: línea punteada amarilla y una sola luz verde, la del último paso.
  - `numero` `{etiqueta, numero, texto?, ejemplo?}`: cifra grande con el color del nivel; «Números de ejemplo» por defecto.
  - `popup` `{popup: POPUP}`: el pop-up de la plantilla sin rediseñar (mismo formato que `construir.py`: `nivel`, `app`, `neta`, `viaje`, `gasolina`, `chips`).
- **Efectos de sonido (oficiales, aprobados por Eduardo el 17 oct 2026; `checa-web/marca/sonido`):** `componer.py` los agrega solos desde `checa-marca-v2/assets/sfx/` (los copia a `public/sfx/`): `popup` → `popup-entra` + el sonido de su nivel (`aceptar` / `considerar` / `rechazar`), `numero` → `numero-sube`, pantalla final → `logo-semaforo` (una vez, volumen 0.4 porque Fernando habla encima). `dato`, `lista`, `comparar` y `pasos` no llevan; la raya de carretera tampoco. Una escena puede traer `"sfx": false` o `"sfx": [["sello", 1.2], ["popup-entra", "palabra", 0.4]]` (archivo, segundo dentro de la escena o palabra de la voz, volumen); `"sfx": false` en el spec los quita todos. Reglas: un sonido por evento visual, máximo 3 por escena, volumen 0.3 a 0.5 con voz. **Sin música** (descartada el 17 oct 2026).
- **Transición (automática):** la raya de carretera punteada amarilla con una sola luz verde cruza la pantalla de izquierda a derecha en 0.3 s y deja la escena a su paso; al terminar regresa igual. Sin sonido. No hay otras transiciones; los zooms (`[[ini, fin, escala]]`) son discretos (1.0 ↔ 1.08).
- `cierre` `{ini}`: pantalla final estándar (logo animado rojo → ámbar → verde → ✓, pastilla «Comenta CHECA» y «Solo Android», sin link (vigente desde 24 oct 2026, `cierre.pil` y `cierre.link: false`; la voz dice «Comenta CHECA y te paso el acceso a la prueba en Google Play»). `ini` = cuando Eduardo dice «Descarga Checa» (con su pausa de ~0.45 s antes de «y checa cuánto te queda…»).

## Lecciones
- **Subtítulos sobre ropa clara o mapas:** llevan contorno asfalto (`-webkit-text-stroke` 9 px, apagado en la palabra activa) porque el hueso se pierde sobre una camiseta blanca. `hyperframes check` aún marca el contraste de la palabra por venir; lo que manda es verlo sobre cuadros reales.
- **Ver el resultado antes de renderizar:** `scripts/vista_previa.js` captura gancho, escenas y subtítulos sin fondo para componerlos sobre cuadros de `input-video.mp4`.
- **Pausas largas de Eduardo:** `quitar: [[a, b]]` en `plan.json` elimina un tramo del clip y une las orillas con un fundido de 0.08 s (más largo se ve doble la imagen si la cámara se movió). Escoge a y b donde ya no hable y la imagen casi no cambie (se compara cuadro a cuadro entre candidatos); deja unos 0.5 s de pausa natural. Todo lo posterior se recorre solo: subtítulos, escena y pantalla final.
- **Cortar una frase sin dejar rastro:** `fin_en_silencio` en `plan.json` busca el instante más callado; el último subtítulo de un clip cortado lleva punto para no pegarse con el siguiente clip.
- **Audita el guion antes de editar:** si repite una idea («si te conviene o no») se corta una de las dos; un video corto de otro ya hecho es carpeta nueva, no lo reemplaza.
- **Cifras largas** en `numero` bajan de tamaño solas para no pegarse a los bordes.
- **Una toma real vale más que una escena:** si Eduardo tiene una demostración en pantalla, se deja sin tapar y los subtítulos van debajo del pop-up (banda y=1160–1480). Es decisión suya si aparecen logos o calles en ella.
- `elevenlabs.py` no necesita el paquete `elevenlabs` de pip (usa urllib); solo la clave `ELEVENLABS_API_KEY`.

## Reglas de las escenas
Una idea por escena, de 3 a 5 s, máximo 12 palabras; suman como máximo el 40 % del video; entran 0.2 s antes de la palabra clave; el contenido queda entre y=250 y y=1120 (los subtítulos ocupan y=1160 a 1480); sin cuenta animada en `numero` (el render por cuadros no ejecuta callbacks).

## Pendientes
- Ajustar zona del rótulo según el encuadre de Eduardo (en el primer uso quedó bien).
- Si Eduardo quiere un encuadre fijo como el de Karen (`encuadre.py`, ojos en el tercio superior), se porta de `videos-karen-trujillo`.

## Cómo habla Eduardo (aprendido de su corrección, 24 oct 2026, video 23 «Prueba en Google Play»)
Úsalo al escribir cualquier guion para que lo diga él a cámara:
- **Mexicano coloquial, cero lenguaje de anuncio:** «a la mera hora», «no te agüites», «fletarme», «en corto», «ahorita», «hacer valer tu tiempo y tu dinero». Evita «aquí viene lo bueno», «apártalo ya», «se acabó».
- **Consuelo antes que solución:** reconoce el golpe («no te agüites») y luego ofrece la salida.
- **Identidad, no credencial:** «papá, esposo y conductor», «salgo a la calle a fletarme», en lugar de «yo también manejo».
- **Trato entre iguales:** pide ayuda y ofrece algo a cambio («necesito 50 conductores… y a cambio yo les doy un descuento de por vida»). No usa escasez ni presión («solo 50, se acaban», «alguien más ya apartó el suyo»).
- **Menos cifras y menos condiciones en la voz:** un solo número; sin porcentajes ni «gratis hasta…»; esos datos van en pantalla y en el texto de publicación.
- **Ritmo hablado:** frases largas con comas, de un tirón, en lugar de frases cortas de efecto.
- **Abre con la pérdida en palabras crudas** de la vivencia del conductor; si lleva groserías, avisarle del posible efecto en el alcance y ofrecer una alternativa suave, pero decide él.
- **Cuida los absolutos** («nadie te lo dice»): sugerir «casi nadie».

## Videos con narrador de IA (sin su cara) — proceso vigente
Para CHECA y su v2 de PERSONAL. Solo se usa el Estilo HyperFrames con `construir.py` (el 1Volt, el 3D y el proceso con agentes ya no se usan: costaron ~1.7 M de tokens).
1. **Guion** con `guion-remotion-checa`. Flujo con **una aprobación por etapa**: guion → tabla de animaciones → portada → video → copy. Con el copy aprobado el video está «hecho»; «publicado» lo marca Eduardo.
2. **Proyecto:** copia `video/hyperframes/cuanto-vale-km-22/` (el que tiene más tipos de escena; `como-funciona-17-v2/` es el explicador), deja `assets/voice/` vacío (`mv` el anterior, nunca `rm` con comodín) y edita `SCRIPT.md` (una línea de voz por escena, con 4 espacios) y `escenas.json` (una escena por línea, mismo orden). Tipos: `gancho`, `numero`, `popup`, `estados`, `oferta`, `lee`, `aviso`, `ticket`, `fichas`, `metas`, `horas`, `rutas`, `tachado`, `dos`, `duo`, `comparar`, `cuenta`, `meta`, `reloj15` y `cierre`; la cabecera de `construir.py` explica los parámetros. Toda palabra que se pase (`en_*`, `w_*`, `sfx`) debe existir en su línea de voz o el script falla.
3. **Voz:** Fernando (`dlGxemPxFMTY7iXagmOj`) en **v3 en todas las líneas** (`ELEVEN_V3=1 python3 -I construir.py --check`). Si cambia una línea, `mv assets/voice` a un respaldo y regenera. Si ElevenLabs responde 401 `quota_exceeded`, avisa a Eduardo. **Montos sin moneda** en la voz («110», no «$110»: decía «dólares»). Ortografía «checas / que cheques». Sin `[pause]` en el gancho. Sin música.
4. **Render:** `ELEVEN_V3=1 setsid nohup python3 -I construir.py --render > renders/log.txt 2>&1 &` (tarda ~2 min; esperar el archivo). Revisa una hoja de cuadros (`ffmpeg … hstack`) y `ffprobe`: `yuv420p`, perfil High. Manda la prueba con `SendUserFile` desde `renders/` (ignorado en git).
5. **Portada** (antes del video): plantilla `video/hyperframes/cuanto-vale-km-22-v2/portada/portada.html` + `r.js` (Playwright con `NODE_PATH=$(npm root -g)`; fuentes en `assets/fonts/`). Receta: fondo asfalto; etiqueta mono gris; título enorme en Bricolage 800 hueso (máx. 8 palabras, 128–164 px según el largo); tarjeta clara ladeada −3° que imita una oferta de app de viajes (VIAJE, ★ 4.92, precio, recogida y viaje, «Aceptar»; nunca logo ni nombres de apps); **sello amarillo con una cifra borrosa** que no aparezca en el título ni en la tarjeta; semáforo con ✓ + «Checa» y raya punteada. Las cifras cuadran con el video. Eduardo prefiere como título el gancho del video.
6. **Primer cuadro:** `video/scripts/portada_primer_cuadro.sh <video> <portada.jpg> <salida>`; se manda para aprobación y **solo entonces** se commitea `video.mp4` (una vez, para no engordar el repo). Mientras tanto déjalo en `renders/` y sube solo `portada.jpg`.
7. **Copy y DM:** `copy.txt` (sin «Soy Eduardo» en videos sin su cara; CTA «Comenta CHECA y te mando el acceso a la prueba en Google Play (solo Android)») y `dm.txt` con `…/registro/?utm_source=tiktok` (o `tiktok-personal` en PERSONAL) `&utm_campaign=<nombre-sin-acentos>`.
8. **Hecho:** entrada en el calendario web (`PWA-Checa-Multimedia/calendario-d4f81c/data.json`: `th`, `v`, `mb`, `copy`, `dm`, `e: "listo"`, `p: null`; copia `v/XNN.mp4` y `t/XNN.jpg`; `indent=1`) y fila en `marketing/plan/calendario.md`. Sube la rama de la sesión en los dos repos.
9. **v2 de PERSONAL** (`marketing/plan/reglas-v2.md`): mismas cifras, otro gancho, otras palabras, otro orden, **misma numeración**, portada distinta. Carpetas `marketing/videos/CHECA|PERSONAL/NN - Nombre/` con `video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`, `guion.md`.
10. **Reglas de marca y oficio:** pop-ups con «OFERTA» (sin Uber/DiDi), «Números de ejemplo» donde haya cifras, neta = viaje − gasolina, nada importante por debajo de y≈1150 (subtítulos en 1160–1480), sin ciudad ni país, sin «gratis», precios ni promesas de ganancia, nada quieto en los primeros 3 s, texto que entra ~0.25 s antes de su palabra, `mask_in` desde `yPercent` 220, `fromTo` de destello con `immediateRender: false`.
11. **Recursos de movimiento** (catálogo, usar el que cuente mejor cada idea): texto que sube desde una máscara; encadenar movimientos (uno empuja al siguiente); cámara que sigue lo importante; objeto que se transforma en la siguiente escena; entradas sincronizadas con la palabra de la voz.
12. **Videos de la web** (`checa-web/media/*.mp4`): se hacen con este mismo proceso (`cierre` con `sin_redes: true`); ejemplos en `video/hyperframes/checa-oferta-web/` y `video/tutorial-instalar/README-v2.md`.
