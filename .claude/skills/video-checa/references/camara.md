# Modo a cámara: pasos técnicos

Para videos donde Eduardo sale a cámara. Mecánica: `spec.json` + `words.json` (transcripción por palabra) → `componer.py` arma `public/index.html` de HyperFrames → `check` → render. Port de la skill `videos-karen` (repo `jeal1498/videos-karen-trujillo`). Usos reales: `CHECA/21 - Beta testers corto` y `CHECA/23 - Prueba en Google Play`. Qué escena usar según lo que dice Eduardo: `plantillas/guion-escenas.md`.

## Pasos (`SK=.claude/skills/video-checa`)
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
- Si Eduardo quiere un encuadre fijo como el de Karen (`encuadre.py`, ojos en el tercio superior), se porta de `videos-karen-trujillo`.
