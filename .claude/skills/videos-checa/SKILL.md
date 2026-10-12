---
name: videos-checa
description: Hacer videos cortos explicativos de Checa (TikTok, Reels, Shorts) con narrador de IA, un solo mundo animado, marcador fijo y capítulos con gráficas, usando la plantilla de Remotion del repo. Úsala cuando Eduardo pida un video, un guion para video, «otro video como el de los $150», cambiar la voz o la música de un video, o un video sin que él salga a cuadro.
---

# Videos explicativos de Checa

**Único video aprobado y plantilla:** `marketing/videos/gasolina150.mp4` (video 1 «Tus $150 no son $150», 40 s), código en `video/src/Checa/Gasolina150.tsx`, voz en `video/public/gasolina150/`. Los videos anteriores (estilo de golpes y asfalto) se borraron a pedido de Eduardo: no los vuelvas a usar como modelo.

**Formato** (sale de una referencia que le gusta a Eduardo, un explicativo de paneles solares de «1Volt1Amp»):
- **Gancho de paradoja:** lo mismo en dos casos con resultado distinto, «con números de ejemplo» («Dos viajes de $150, mismo carro… uno te deja $130 y el otro $90»).
- **Un solo mundo sin cortes:** calle de noche con dos carros (A en cian `#22D3EE`, B en morado `#A855F7`), estrellas, luna, cable con lucecitas que fluyen. La cámara solo hace un zoom lento.
- **Marcador fijo abajo** con «Ejemplo · gasolina a $2 por km» y los dos resultados con barrita de color.
- **3 capítulos con pestañas** arriba («1 Kilómetros · 2 Recogida · 3 Tiempo»); la activa se ilumina en cian. Cada capítulo dibuja su gráfica en el cielo (barras, ruta con la recogida punteada en rojo, barras de $/hora).
- Etiquetas chicas en **Fira Code**, números grandes en InterDisplay. Movimiento suave (`Easing.out`), **sin sacudidas, destellos ni karaoke**; subtítulos discretos en y≈1500.
- Cierre: los dos **pop-ups idénticos a la app** y luego logo + «7 días gratis · link en el perfil» + pregunta.
- **Duración para TikTok: 40 s** (Eduardo la pidió así). Una línea de voz por capítulo; `armarEscenas(LINEAS, 3, 3, 40)`.

**Antes de cada video lee `referencia/README.md`** (estructura por segundo, recursos visuales y transcripción de la referencia) y mira `referencia/cuadros.jpg`.

Lee `.agents/product-marketing.md` antes de escribir el guion. Responde a Eduardo en español y corto.

**Calendario:** los 20 guiones están en `marketing/calendario-videos.html` (arreglo `V`). Están escritos para el estilo viejo: al producir uno, **adáptalo a este formato** (dos casos A/B, 3 capítulos, 40 s) y presenta el guion adaptado para aprobación. Al terminar, márcalo con `est:"hecho",archivo:"marketing/videos/<nombre>.mp4"` y pon en `L` las líneas finales.

## 1. Guion (primero, y pedir aprobación antes de producir)

Eduardo aprueba el guion antes de gastar créditos. Preséntalo como tabla de segundos con 🎙️ voz y 🖥️ en pantalla, más «La cuenta» con cada cifra.

Estructura (8 líneas, ~40 s):
1. Gancho: los dos casos iguales (misma tarifa, mismo carro).
2. Resultado distinto, «con números de ejemplo» (sube el marcador).
3. «La diferencia está en tres cosas que la oferta no te dice» (aparecen las pestañas).
4–6. Un capítulo por línea: «Uno, …», «Dos, …», «Tres, …», cada uno con su número.
7. Checa hace esa cuenta encima de cada oferta (pop-ups).
8. «Pruébala siete días gratis» + pregunta para comentarios.

Reglas:
- Narrador que le habla al conductor de **tú**. Nada de «yo».
- Números honestos. Gasolina: $24 el litro, 12 km/l = **$2 por km**. Fórmulas de la app (`Parser.kt`): $/km = tarifa ÷ km totales (con recogida), $/h = tarifa ÷ min totales × 60, neta = tarifa − km × $2. Si la voz usa $/h **neta** (ya sin gasolina) dilo así («ya sin gasolina»): el pop-up muestra $/h con la tarifa completa y Eduardo lo nota.
- Nunca inventar testimonios ni ganancias. Sin ciudad ni país. Sin nombrar a la competencia. Sin «no es X, es Y» ni listas «sin X, sin Y».
- Números en la voz con letra («ciento cincuenta pesos»).
- Ofrece 2 ganchos alternativos.

## 2. Voz y música (ElevenLabs)

- Plan **Starter** de Eduardo (30 000 créditos/mes). Un video de 40 s gasta ~550 caracteres.
- La **clave** se lee de `ELEVENLABS_API_KEY` (variable del entorno: menú del entorno en la barra de título → Edit). Si falta, pídele que la agregue ahí (Edit → Variables de entorno → `ELEVENLABS_API_KEY=sk_…` → Guardar → abrir una sesión nueva), no en el chat. Si la pega en el chat, guárdala solo en `/tmp/claude-0/.el_key` (`umask 077`) y recomiéndale cambiarla. **Nunca** en el repo. No tiene permiso de speech-to-text.
- Voz `gncXGfeB8V1Weu7l6QUg`, **velocidad 1.05** (calmada).
- Música: reutiliza `public/musica-calma.mp3` (78 s, ambiental). Efectos: `public/{whoosh,ding}.wav` y `public/pop.mp3`, a volumen bajo (0.12–0.18).

```bash
cd video
C=public/<video>
V=gncXGfeB8V1Weu7l6QUg
python3 -I scripts/elevenlabs.py voz $C l1 "Primera línea." $V 1.05      # … hasta l8
cd $C && for i in 1 2 3 4 5 6 7 8; do ffmpeg -loglevel error -y -i l$i.mp3 -af "loudnorm=I=-14:TP=-1.5:LRA=7" -ar 44100 -b:a 128k t.mp3 && mv t.mp3 l$i.mp3; done && cd ../..
python3 -I scripts/lineas.py $C src/Checa/lineas<Nombre>.ts l1 l2 l3 l4 l5 l6 l7 l8
```

`lineas.py` imprime las palabras con su tiempo. Si la suma pasa de ~37 s, recorta texto. Si solo cambia una parte del guion, reutiliza las voces que no cambian (copia el `.mp3` y el `.json`).

## 3. Escenas en Remotion

Copia `Gasolina150.tsx` a `<Nombre>.tsx` y cambia: el import de `lineas<Nombre>`, la carpeta de voces, los textos de `Intro`, `Cap1–Cap3`, `Marcador`, `Checa`, `Cierre` y los `en(i, "palabra")`. Lo común:
- `plantilla.tsx`: `armarEscenas`, `crearEn` (`en(línea, "palabra", vez)` → cuadro en que se dice; escribe la palabra igual que en `lineas.py`, con puntuación: `"veinte;"`, `"cien."`).
- `graficos.tsx`: `Carro` (de lado, llantas que giran).
- `ui.tsx`: `Popup` con **`exacto`** (igual que `Popup.kt`): pastilla de la app («Uber» gris, «DiDi» con `pastillaColor="#FF7028"`), ✕, montos con 2 decimales como texto (`neta="130.00"`), fichas `"$450/h"` · `"$15.00/km"` · `"20 min"` con etiquetas `"meta $150"`, `"meta $8"`, `"10.0 km total"`; `c:"ok"` verde si cumple la meta, `"mal"` rojo con borde si no, `"neutro"` para el tiempo. Calcula el semáforo con las metas por defecto ($8/km y $150/h): cumple las dos → verde, una → amarillo, ninguna → rojo.
- Helpers del archivo: `ap(frame, desde)` (entrada suave), `vis(frame, desde, hasta)` (entra y sale), `Abs`, `Etiqueta`.

Registra la composición en `video/src/Root.tsx` (1080×1920, 30 fps).

Diseño:
- Pestañas y gráficas entre y≈80 y y≈880; carros en y≈905; calle 1010–1180; marcador 1198–1480; subtítulos y≈1500; nada abajo de ~1570.
- La luna se apaga durante los capítulos para no tapar los textos.
- Cada parte debe tener algo en pantalla desde su primer cuadro.
- Verde/amarillo/rojo solo con su significado de semáforo; rojo también para lo que no se paga; amarillo luna para montos de gasolina.

## 4. Revisar y renderizar

```bash
cd video
npm ci --loglevel=error          # solo si falta node_modules
npx tsc --noEmit
B=/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell
for f in 5 140 300 400 520 640 760 880 1000 1150; do npx remotion still <Id> /tmp/claude-0/st/c$f.png --frame=$f --browser-executable=$B --log=error; done
# juntar en una tira (hstack/vstack) y mirarla con Read
npx remotion render <Id> out/x.mp4 --browser-executable=$B --log=error
ffmpeg -loglevel error -y -i out/x.mp4 -c:v copy -af "loudnorm=I=-14:TP=-1:LRA=9" -ar 48000 -c:a aac -b:a 192k out/<nombre>.mp4
```

Revisa textos encimados (sobre todo con la luna), cosas cortadas en los bordes, partes vacías y que los pop-ups se lean. Volumen final −14 LUFS.

## 5. Entregar

1. `cp video/out/<nombre>.mp4 marketing/videos/`
2. Commit con `[skip ci]` y `git push origin HEAD:main` (más la rama de trabajo si la hay).
3. Manda el video con `SendUserFile` (`display: "render"`) y explica en 3–5 líneas qué trae y qué revisar, con la duración. Si Eduardo pide los montos, dáselos en tabla (concepto · A · B) y avisa de cualquier cifra que pueda verse distinta en el pop-up.

`video/out/` está en `.gitignore`; se versionan el código, `public/<video>/` y la copia en `marketing/videos/`.
