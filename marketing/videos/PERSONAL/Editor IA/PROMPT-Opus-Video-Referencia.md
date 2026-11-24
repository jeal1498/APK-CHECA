# Prompt para Opus: crear el video de referencia de PERSONAL (sesión nueva)

Copia todo lo que está debajo de la línea y pégalo en una sesión nueva con Opus (repo `jeal1498/APK-CHECA`).

---

Eres Opus trabajando con Eduardo (Lalo, hombre; respuestas cortas, en español; él escucha el chat con voz: nada de textos largos). Repo `jeal1498/APK-CHECA`. Trabaja en la rama de la sesión y sube también a `main` en commits pequeños; no hagas PR. Lee antes: `CLAUDE.md` (secciones «Cómo trabajar con Eduardo» y «Videos»), `marketing/videos/PERSONAL/Editor IA/LEEME.md` y `Identidad/IDENTIDAD.md`.

## Objetivo
Crea **un video de referencia nuevo, desde cero, para la cuenta PERSONAL** (@Eduardoalv9802). No arregles ni continúes ningún video anterior. Este video será **la pieza modelo** que se guarda en `marketing/videos/PERSONAL/Editor IA/Referencias/` y con la que se harán todos los Hooks Virales de PERSONAL. Por eso debe verse **impecable en cualquier teléfono**: movimiento fluido, sin interferencia, sin cortes raros, y con una idea visual clara que se pueda repetir con otros temas.

## Qué debe demostrar el video (esto es lo que se reutilizará)
1. La identidad EA aplicada de punta a punta: editorial minimalista, marfil `#F1EEE6`, negro `#151515`, naranja `#F47732` (solo como énfasis), Montserrat 800 / Roboto, sello EA. al final. El tema vive en `video/src/EA/theme.ts` y los componentes en `video/src/EA/components/` (Subtitles, Headline, Popup, StatCard, Comparison, Chart, LowerThird, Annotations, Logo, Transition, Unidades, Tarjeta, Interruptor). Reutilízalos; crea componentes nuevos solo si hacen falta y déjalos en esa carpeta.
2. Formato 9:16 (1080×1920, 30 fps), 20–30 s. Zona de cámara arriba (0–800 px; recuadro gris que luego se reemplaza por el bruto de Eduardo) y gráficos abajo. Zonas seguras: 200 px arriba, 380 abajo, 60 a los lados. Subtítulos con la palabra clave en naranja (máximo 2 líneas).
3. Estructura de guion: gancho → contexto → desarrollo → conclusión → interacción. Una sola idea. Sin ganchos engañosos y sin vender nada. CHECA solo si aporta como ejemplo (no es obligatorio).
4. **Pautas de dirección de arte (obligatorias):**
   - Antes de programar, escribe en `guion.md` el concepto de acción visual de cada escena (tabla: segundos, acción literal, texto en pantalla, palabra que dispara el pico de movimiento, fotograma final).
   - Texto integrado máximo **4 palabras por escena** (sin contar subtítulos).
   - Narrativa literal: cada analogía del guion se ejecuta como interacción física, no como metáfora decorativa.
   - Escala y persistencia (morphing): un elemento principal que se transforma de una escena a otra (la escala relativa dimensiona magnitudes; enfoque/desenfoque guía la mirada). Evita cortes entre elementos sin relación.
   - Cada pico de movimiento cae en su palabra clave y cada escena resuelve en una composición limpia antes de cortar.
5. Reglas de contenido de Eduardo: guion natural y directo, **sin palabras con R fuerte (R inicial o RR; ejemplos a evitar: «real», «rápido», «recibe», «carro», «correr»)**, no inventes experiencias ni resultados de Eduardo, no uses cifras sin calcularlas. Si usas dinero de conductores, calcula con las fórmulas de la app (gasolina = km totales ÷ km/l × $/l; 12 km/l y $24/l por defecto) y muestra la cuenta.

## Paso 0 (una sola pregunta corta a Eduardo, y sigues mientras responde)
Propón 3 temas (uno por pilar: experiencia real, educación, experimento; si tomas algo de su vida, pregúntale; no lo inventes) y pregunta: «¿Cuál tema y en qué aparato/reproductor lo ves (iPhone, Android, galería, CapCut)?». Mientras tanto avanza con el que más encaje con su posicionamiento («No solo edito videos. Construyo contenido con intención.»).

## Lecciones de un intento anterior (no las repitas)
Un video previo (`video/src/EA/Referencia02.tsx`, 26 s) se vio con interferencia y trabado en el reproductor de Eduardo, y la causa exacta no se identificó. Aprende de sus fallos:
- Transiciones feas: línea negra residual al unir una tarjeta partida; cuadros que al apilarse se estiraban como barras altas; 52 cuadros volando escalonados parecían basura; números encimados con elementos que se desvanecían.
- Easing demasiado brusco (`bezier(.16,1,.3,1)` en 6–18 fotogramas): arranca de golpe y se congela (134 fotogramas idénticos). Usa movimientos de al menos 12 fotogramas con aceleración suave (`bezier(.4,0,.2,1)`) y evita animar muchos elementos a la vez.
- Complejidad en el render: 80 `div` con transform, `filter: blur` sobre un contenedor de 1080×1920 y sombras. **Diseña simple:** pocos elementos en pantalla, rejillas/partículas en un solo `<canvas>` o SVG, desenfoque solo en un elemento pequeño o sustituido por opacidad/escala, sin sombras difusas, bordes de al menos 3 px y texto grueso (el contenido plano de alto contraste se rompe al comprimir).
- Una sola animación grande a la vez; el resto, quieto.

## Exportación obligatoria (no uses el mp4 directo de Remotion)
```bash
cd video   # primero: npm i
npx remotion render src/EA/index.ts <Composicion> /ruta/seq --sequence --image-format=png \
  --browser-executable=/opt/pw-browsers/chromium --chrome-mode=chrome-for-testing
ffmpeg -framerate 30 -i /ruta/seq/element-%03d.png \
  -vf "scale=in_range=pc:out_range=tv:out_color_matrix=bt709,format=yuv420p" \
  -c:v libx264 -preset slow -crf 14 -g 30 -keyint_min 30 -r 30 -fps_mode cfr \
  -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv \
  -movflags +faststart salida.mp4
```
Entrega **dos variantes**: la de arriba y una compatible con `-profile:v main -level 4.0 -bf 0`. Registra la composición en `video/src/EA/Root.tsx` (id `EA-Referencia-03` o el siguiente libre). El entorno trae Node 22 y ffmpeg 6.1; las fuentes ya están en `video/public/ea/`. Renders largos, en segundo plano.

## Verificación antes de entregar (con números)
1. `ffprobe`: `pix_fmt=yuv420p`, CFR 30, fotogramas = segundos × 30, un fotograma clave por segundo, `pts` sin huecos.
2. Diferencia entre fotogramas consecutivos (numpy, video en gris a 270 px): sin picos fuera de las transiciones previstas y sin fotogramas idénticos dentro de una animación.
3. Color: el naranja decodificado a ±3 de `244,119,50`.
4. Revisa a mano, al 200 %, cada transición y el último fotograma de cada escena: sin bordes dobles, líneas residuales, elementos estirados ni texto encimado.
5. Texto en pantalla ≤ 4 palabras por escena y cada pico sobre su palabra clave.

## Entrega
1. Envía los dos mp4 con `SendUserFile` y espera su «aprobado».
2. Con «aprobado», **una sola vez** commitea el mp4 aprobado en `marketing/videos/PERSONAL/Editor IA/Referencias/` (nombre `referencia-03-<tema>.mp4`), agrega su fila en `Referencias/README.md` (qué es / qué tomamos / hooks hechos con ella: ninguno aún), deja `guion.md` junto al video y el código en `video/src/EA/`.
3. Actualiza `marketing/videos/PERSONAL/Editor IA/LEEME.md` solo si descubres algo que cambie el flujo.
4. Termina con una acción de menos de 2 minutos para Eduardo.
