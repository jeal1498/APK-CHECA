---
name: videos-checa
description: Hacer videos cortos explicativos de Checa (TikTok, Reels, Shorts) con narrador de IA y el estilo visual de las referencias «1Volt1Amp» (line-art sobre fondo oscuro, tipografía mono, diagramas animados), usando Remotion en el repo. Úsala cuando Eduardo pida un video, un guion para video, «otro video como el de los $150», cambiar la voz o la música de un video, o un video sin que él salga a cuadro.
---

# Videos explicativos de Checa

**Único video aprobado y plantilla:** `marketing/videos/gasolina150.mp4` (video 1 «Tus $150 no son $150», 40 s), código en `video/src/Checa/Gasolina150.tsx`, voz en `video/public/gasolina150/`. Los videos anteriores (estilo de golpes y asfalto) se borraron a pedido de Eduardo: no los vuelvas a usar como modelo.

**Lo que Eduardo quiere copiar de sus referencias es el DISEÑO VISUAL y el FORMATO, no la historia.** Hay 4 referencias de «1Volt1Amp» analizadas en `referencia/README.md` (léelo completo antes de cada video y mira `referencia/cuadros1–4.jpg`). **No** es obligatorio comparar dos casos A/B: eso solo era el tema de la referencia 1 (y del video 1). Cada video elige la historia que mejor explique su idea.

**Diseño visual (obligatorio en todos):**
- Fondo casi negro azulado, mucho espacio vacío, **una idea por pantalla**.
- **Line-art**: objetos con contorno lavanda de 3–4 px y relleno muy oscuro (carro, teléfono, surtidor, persona, pin de recogida). Sin texturas ni sombras.
- Colores con significado: **magenta = lo malo / el mito / el número que duele**, **cian = lo bueno / la solución**, ámbar = dinero o gasolina. Verde/amarillo/rojo de semáforo solo dentro del pop-up de Checa.
- **Todo en monoespaciada (Fira Code)**: número grande + etiqueta chica debajo; frase clave en MAYÚSCULAS espaciadas abajo con una línea que la explica.
- Recursos: ícono tachado con diagonal magenta (el mito), palabra tachada, rejilla de puntos para contar, cotas punteadas con su valor, flechas de medida, ✗ magenta / ✓ cian, partículas y brillos suaves.
- Movimiento: trazos que se dibujan, puntos que aparecen, números que cuentan, objetos que se transforman; zoom a un detalle. Sin sacudidas, destellos ni karaoke; en pantalla solo la frase clave (subtítulos discretos opcionales en y≈1500).
- Cierre: frase que repite la idea + logo Checa + «7 días gratis · link en el perfil» + pregunta.

**Formato narrativo (patrones de las referencias):** gancho que contradice lo que todos creen → dato con fuente o número de ejemplo → mecanismo paso a paso con su diagrama → giro («y lo que casi nadie se imagina…») → consejo concreto con ✓/✗ → frase final. Narrador calmado, de tú, voz a 1.05. **40 s** para TikTok (una sola idea).

**Video 1 (`Gasolina150.tsx`)**: sirve como base técnica (voces, tiempos con `en()`, marcador, pestañas, `Popup exacto`, cierre, volumen), pero su estilo visual (carros rellenos, calle con luna) es anterior a este análisis: los siguientes deben usar el line-art de arriba.

**Calendario:** los 20 guiones están en `marketing/calendario-videos.html` (arreglo `V`), escritos para el estilo viejo. Al producir uno, **adáptalo a este formato** y presenta el guion para aprobación. Al terminar, márcalo con `est:"hecho",archivo:"marketing/videos/<nombre>.mp4"` y pon en `L` las líneas finales.

Lee `.agents/product-marketing.md` antes de escribir el guion. Responde a Eduardo en español y corto.

## 1. Guion (primero, y pedir aprobación antes de producir)

Eduardo aprueba el guion antes de gastar créditos. Preséntalo como tabla de segundos con 🎙️ voz y 🖥️ en pantalla, más «La cuenta» con cada cifra.

Estructura sugerida (7–8 líneas, ~40 s; adáptala a la idea, no es obligatoria):
1. Gancho que contradice lo que el conductor cree (con el ícono o la palabra tachada).
2. Dato o ejemplo con números («con números de ejemplo»).
3–5. Mecanismo paso a paso, cada paso con su diagrama y su número (pueden ser capítulos con pestañas si hay tres partes).
6. Consejo concreto (✓/✗) y cómo Checa lo hace por ti (pop-up idéntico a la app).
7. Frase final que repite la idea + «Pruébala siete días gratis» + pregunta.

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
