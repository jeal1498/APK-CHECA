# Referencia del estilo A3 con loop (único estilo vigente, Eduardo 2 nov 2026)

Fuente del C19 «Checa lee la oferta» en 45 s, aprobado por Eduardo como **A3 con loop**. Es una página HTML que se anima con `window.set(t)`; se renderiza cuadro por cuadro con Playwright y se mezcla con ffmpeg. No usa `construir.py`: hay que **copiarla y adaptarla** a cada video nuevo.

| Archivo | Para qué |
|---|---|
| `index.html` | La página. Arriba del `<script>`: el CSS y los elementos (teléfono, tarjeta de oferta, pop-up, ojo, altavoz, logo). En el script: `CAPS` (subtítulos), `W` (segundo de cada palabra: `W["rojo"]=[21.23]`), `CUT` (inicio de cada línea) y un bloque por línea de voz dentro de `set()` |
| `render.js` | `NODE_PATH=$(npm root -g) node render.js index.html <carpeta> <desde> <hasta>`; cuadro i = segundo i/30. Corre 4 procesos en paralelo (~15 min para 45 s) |
| `mezclar.py` | `python3 -I mezclar.py <cuadros> <voz.wav> sonidos.json <salida.mp4>`: voz + efectos oficiales + whoosh/thump sintetizados |
| `sonidos.json` | Los efectos del C19 (nombre, milisegundo, volumen): ejemplo del formato |

## Cómo se adapta a un video nuevo
1. Voz y tiempos como siempre (`checa.py construir <proyecto>` sin render → `tiempos.json`); `checa.py plan <proyecto>`.
2. Copia esta carpeta a `video/hyperframes/<proyecto>/a3/`. Cambia `CUT` (inicio de cada línea, de `tiempos.json`), `W` (todas las palabras con su segundo) y `CAPS`.
3. Reescribe el bloque de cada línea: qué entra en qué palabra (reglas de `narrador.md` §0 y §10). El C19 sirve de modelo para cada tipo de escena (gancho, tensión, app, neto, rojo, verde, metas, voz, «no acepta», CTA).
4. El loop ya viene en el código (`setLoop`: últimos 0.6 s se disuelven al cuadro 0); `render.js` de 0 a total×30 cuadros.
5. `mezclar.py` con la voz y un `sonidos.json` propio; `checa.py auditar <proyecto> <video.mp4>` (con `tiempos.json` y `escenas.json` de golpes `{"en": palabra}` por línea) y `checa.py calidad`.
Pendiente: portar esta página al motor (`construir.py`) para generarla desde `escenas.json` en vez de adaptarla a mano.
