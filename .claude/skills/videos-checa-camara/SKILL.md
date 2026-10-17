---
name: videos-checa-camara
description: Editar los videos de Eduardo a cámara (cuenta personal de TikTok @Eduardoalv9802 y tutoriales con su cara) con la marca Checa v2 (asfalto y hueso, semáforo con significado, Bricolage Grotesque, DM Sans, JetBrains Mono): gancho en pantalla, subtítulos v2 palabra por palabra, escenas gráficas animadas que acompañan el guion (pop-up, comparar, pasos, número, lista, dato) con la raya de carretera como transición, y la pantalla final estándar. Úsala cuando Eduardo pase un video suyo a cámara y pida editarlo, agregar escenas o tomas gráficas, o armar el spec. Para videos con narrador de IA y sin su cara, usa `videos-checa`.
---

# Videos de Eduardo a cámara (marca Checa v2)

Port de la skill `videos-karen` (repo `jeal1498/videos-karen-trujillo`) a la marca Checa. Misma mecánica: un `spec.json` + `words.json` (transcripción por palabra) → `componer.py` arma `public/index.html` de HyperFrames → `check` → render. **Estado (17 oct 2026):** probada con un video sintético (`check` sin errores, escenas revisadas en fotogramas y un render corto); falta el primer uso con un video real de Eduardo.

## Marca (manda `jeal1498/checa-web`, carpeta `marca/`; usa la skill `marca-checa`)
- Asfalto `#0C0C10`, tablero `#16161C`, humo `#3A3A45`, hueso `#F4F4EF`. **El color solo decide:** verde `#22C55E` (lo que ganas), amarillo `#FACC15` (lo que recuerdas), rojo `#EF4444` (lo que pierdes).
- Bricolage Grotesque 800 (títulos y números), DM Sans (texto), JetBrains Mono (datos y etiquetas). Fuentes en `video/hyperframes/checa-marca-v2/assets/fonts/` (`preparar.sh` las copia).
- Sin logos de Uber ni DiDi (pantallas genéricas, `"app": "APP"`), sin ciudad ni país, sin promesas de ganancia, «Números de ejemplo» visible donde haya cifras, la neta cuadra (viaje − gasolina).
- Por ahora no hay prueba de 7 días: no decir «gratis», «sin costo» ni precios.
- Ortografía: «checas», «que cheques», «cheque» (nunca «checes»).

## Flujo con una aprobación por etapa (el de Checa, 16 oct 2026)
1. **Guion / transcripción:** el texto corregido de lo que Eduardo dice, de corrido. Sin etiquetas ni tablas.
2. **Tabla de gráficos por toma** (chat): `Segundo · Frase que dice · Elemento en pantalla` (gancho, rótulo, escenas con su tipo, pantalla final). Se piensa con `plantillas/guion-escenas.md`.
3. **Portada** (estilo marca v2: `video/hyperframes/checa-marca-v2/portada/portada.html`; va antes que el video).
4. **Video:** versión de prueba con `SendUserFile` desde la carpeta de trabajo (no se commitea). Con portada y video aprobados: `video/scripts/portada_primer_cuadro.sh <video.mp4> <portada.jpg> <salida.mp4>`.
5. **Copy** (`copy.txt` y `dm.txt` con el link y `utm_source=tiktok-personal`).
6. **Hecho:** copiar a `marketing/videos/PERSONAL/NN - Nombre/` (`video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`, `guion.md`), una sola vez y ya aprobado. Commit con `[skip ci]` (si no, GitHub Actions compila el APK). «Hecho» no es «Publicado».

## Pasos técnicos (`SK=.claude/skills/videos-checa-camara`)
1. `bash $SK/scripts/preparar.sh <original.mp4> <trabajo>` (trabajo en el scratchpad, no en el repo): re-codifica a 1080×1920, voz a −14 LUFS, copia fuentes y gsap, transcribe con Whisper (`words.json`).
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
- **Transición (automática):** la raya de carretera punteada amarilla con una sola luz verde cruza la pantalla de izquierda a derecha en 0.3 s y deja la escena a su paso; al terminar regresa igual. Sin sonido. No hay otras transiciones; los zooms (`[[ini, fin, escala]]`) son discretos (1.0 ↔ 1.08).
- `cierre` `{ini}`: pantalla final estándar (logo animado rojo → ámbar → verde → ✓, «Descarga Checa», «Solo Android», `@checa.driver`, `@Eduardoalv9802`, `jeal1498.github.io/checa-web`). `ini` = cuando Eduardo dice «Descarga Checa» (con su pausa de ~0.45 s antes de «y checa cuánto te queda…»).

## Reglas de las escenas
Una idea por escena, de 3 a 5 s, máximo 12 palabras; suman como máximo el 40 % del video; entran 0.2 s antes de la palabra clave; el contenido queda entre y=250 y y=1120 (los subtítulos ocupan y=1160 a 1480); sin cuenta animada en `numero` (el render por cuadros no ejecuta callbacks).

## Pendientes
- Efectos de sonido: no hay en esta skill. Si se agregan, solo los oficiales de `checa-web/marca/sonido` y según sus reglas (un sonido por evento visual, máximo 3 por escena, volumen 0.3 a 0.5 con voz). Sin música.
- Primer uso con un video real de Eduardo; ajustar zona del rótulo según su encuadre.
- Si Eduardo quiere un encuadre fijo como el de Karen (`encuadre.py`, ojos en el tercio superior), se porta de `videos-karen-trujillo`.
