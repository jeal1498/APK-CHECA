---
name: videos-checa
description: Hacer videos cortos de Checa (TikTok, Reels, Shorts) con narrador de IA en el estilo aprobado «ciudad 3D» (Remotion + three.js, cada escena con la vista de cámara que mejor la cuente, edificios de colores, cielo de atardecer, etiquetas neón en letra pixel, número gigante, subtítulos con palabra clave en amarillo y 8 vistas de cámara). Úsala cuando Eduardo pida un video, un guion para video, el siguiente tema de marketing/temas-videos.md, cambiar la voz o la música de un video, o un video sin que él salga a cuadro.
---

# Videos de Checa

## Estilo aprobado: «punto dulce» (Eduardo, octubre 2026)

Después de varias pruebas, Eduardo aprobó este estilo y **le gustan las 8 vistas**. Es el que se usa en todos los videos nuevos:

- **Una ciudad 3D** (el mismo mundo en todo el video) hecha con `@remotion/three` (three.js): calle con edificios de bloques de colores (rosa, turquesa, mostaza, morado, verde, naranja) con ventanas encendidas, banquetas lila, calle azul marino con rayas blancas, **cielo de atardecer** (degradado `#2B2A7A → #8E4B9C → #F08A5D`) y neblina morada al fondo. El carro del conductor es **blanco**, en bloques; otros carros de colores circulan.
- **Rutas pintadas en el piso**: recogida sin pago en **magenta punteado**, viaje pagado en **cian sólido**, pin del pasajero magenta flotando y destino como cubo cian.
- **Capas en pantalla (estilo @pixelmente):** etiquetas cortas en MAYÚSCULAS dentro de **recuadros neón** con letra pixel **Silkscreen** (`OFERTA · $80`, `8 KM · PAGADOS`, `6 KM · SIN PAGO`), apiladas arriba sobre una franja oscura; **número gigante** en Silkscreen negrita que cuenta o se **tacha** (amarillo → magenta); **subtítulos** abajo (y≈1500) blancos en Inter negrita con **la palabra clave en amarillo**.
- **Colores con significado:** magenta = lo malo, lo que no te pagan, el número que duele; cian = lo que sí te pagan o la solución; amarillo = dinero, la oferta y la palabra clave. Verde/amarillo/rojo de semáforo solo dentro del pop-up de Checa.
- **Pop-up de Checa idéntico a la app** (`Popup` con `exacto`) cuando sale el teléfono.
- **Cierre:** fondo de atardecer + recuadro neón con la frase final + logo Checa (+ «7 días gratis · link en el perfil» y la pregunta en los videos completos).
- Música calmada (`public/musica-calma.mp3`, volumen 0.14), efectos muy suaves; narrador Fernando Martínez a velocidad 1.0; volumen final −14 LUFS.

### Las 8 vistas (todas aprobadas)
**No hay una vista principal: la tercera persona es solo una de las 8.** Para cada escena del guion elige la vista que mejor cuente esa idea (la tabla dice para qué sirve cada una) y justifícala en una frase al presentar el guion. Cambia de vista cada 4–8 s sin salir de la misma ciudad (como las referencias); no repitas la misma vista en dos escenas seguidas salvo que la historia lo pida. Código de cada una en `video/src/Checa/Vistas3D.tsx` (componente `Camara`, casos `v === 0…7`, y `Capas` para lo que va encima):

| # | Vista | Para qué sirve |
|---|---|---|
| 1 | **Detrás del carro** (persecución) | Estar manejando: la recogida, los km que no pagan |
| 2 | **Dron que sube** | Enseñar la ruta completa y las distancias (toma de remate) |
| 3 | **Isométrica fija** (maqueta que gira despacio) | Gasolinera, cuadra, carro esperando |
| 4 | **Dentro del carro** (tablero + teléfono en el soporte con la oferta y el pop-up que cae) | Decidir en segundos, mostrar la app |
| 5 | **Del teléfono a la calle** (la pantalla se encoge y entras a la ciudad) | Gancho: «esta oferta dice $80…» |
| 6 | **Vista lateral** con regla de km abajo | Km totales, costo por km |
| 7 | **Mapa de calor de noche** (zonas que palpitan + etiquetas de hora) | Mejores horas |
| 8 | **Detalle en cámara lenta** (bomba con contador pixel subiendo) | El número que duele: gasolina |

Ejemplo de combinación para 40 s (solo ejemplo, no plantilla fija): 5 (gancho) → 1 (explicación) → 8 (número que duele) → 2 (la cuenta completa) → 4 (el pop-up de Checa) → cierre. Cada tema tendrá su propia mezcla.

### Plantillas
- **`video/src/Checa/Calle3D.tsx`** (`Calle3D`, 6 s con voz): base de un video con narración: cámara en tercera persona que sube al final, rutas que se pintan con la voz (`en("palabra")`), etiquetas neón, número gigante, subtítulos con palabra clave. Exporta `Edificios` (prop `soloLado`) y `Carro3D`.
- **`video/src/Checa/Vistas3D.tsx`** (`Vistas3D`, muestrario de 19 s): las 8 cámaras, la `Gasolinera`, el mapa de `Calor`, el tablero con `Telefono` + `Oferta` + `Popup`, la regla de km y el contador de la bomba.
- Muestras renderizadas en `marketing/videos/pruebas/` (`prueba-3d.mp4`, `prueba-vistas.mp4`; las demás son pruebas descartadas de estilos anteriores).
- El video 1 (`marketing/videos/gasolina150.mp4`, `Gasolina150.tsx`) es de un estilo anterior (mundo plano). Sirve solo como referencia técnica (voces, `armarEscenas`, `crearEn`); no copies su aspecto.
- Pendiente de pulir en el primer video real: la gasolinera vista de lejos se ve vacía (agregar autos, letrero, árboles) y la bomba de cerca es plana (agregar manguera y pantalla).

## Antes de escribir

1. Lee `.agents/product-marketing.md` y `referencia/README.md` (7 referencias analizadas: 1Volt1Amp y @pixelmente, con cuadros y transcripciones; sección *Estilo Checa*).
2. **Tema:** sigue el orden de `marketing/temas-videos.md` (14 miedos y problemas del conductor, aprobado por Eduardo). El primero es «Trabajar mucho y no ver el dinero». En los temas donde Checa no ayuda (9–14), el video da un consejo útil y honesto y Checa solo firma al final. No armes calendario ni propongas fechas hasta que él lo pida.
3. Carga con la herramienta Skill **`social`** (ganchos, estructura corta, reglas anti-IA) y **`marketing-psychology`** (elige 1–2 principios y nómbralos). Si chocan con esta skill, manda esta. Si Eduardo manda otra referencia de video, usa también **`video`** y agrega el análisis a `referencia/README.md`.

## 1. Guion (pedir aprobación antes de gastar créditos)

Preséntalo como tabla: segundos · 🎙️ voz · 🖥️ en pantalla · 🎥 vista (1–8). Agrega «La cuenta» con cada cifra, el color del pop-up si sale, los principios de psicología y 2 ganchos alternativos.

Formato narrativo (de las referencias): escena cotidiana del conductor que intriga o contradice lo que cree → descartar la respuesta obvia → mecanismo con una imagen concreta y su número → giro («y lo que casi nadie se imagina…») → consejo concreto → cierre que regresa a él («en tu próxima oferta…») + «Pruébala siete días gratis» + pregunta. **40 s**, 7–8 líneas de voz.

Reglas:
- Narrador que le habla al conductor de **tú**. Nada de «yo».
- Números honestos. Gasolina: $24 el litro, 12 km/l = **$2 por km**. Fórmulas de la app (`Parser.kt`): $/km = tarifa ÷ km totales (con recogida), $/h = tarifa ÷ min totales × 60, neta = tarifa − km × $2. Semáforo con metas por defecto ($8/km y $150/h): cumple las dos → verde, una → amarillo, ninguna → rojo. Si la voz usa $/h neta, dilo («ya sin gasolina»): el pop-up usa la tarifa completa.
- Supuestos (regreso vacío, horas, etc.) se dicen como supuestos o «con números de ejemplo». Nunca inventar testimonios ni ganancias. Sin ciudad ni país. Sin nombrar a la competencia. Sin «no es X, es Y» ni listas «sin X, sin Y».
- Números en la voz con letra («ciento cincuenta pesos»).

## 2. Voz y música (ElevenLabs)

- Plan **Starter** (30 000 créditos/mes); un video de 40 s gasta ~550 caracteres.
- La clave se lee de `ELEVENLABS_API_KEY` (variable del entorno). Si falta, pídele que la agregue en el menú del entorno → Edit, no en el chat. Si la pega en el chat, guárdala solo en `/tmp/claude-0/.el_key` (`umask 077`) y recomiéndale cambiarla. **Nunca** en el repo. No tiene permiso de speech-to-text (para transcribir referencias usa `faster-whisper` con pip).
- Voz **Fernando Martínez** (`dlGxemPxFMTY7iXagmOj`, latina neutra, la más usada de la biblioteca), velocidad **1.0**. Eduardo la eligió en octubre 2026; la anterior (`gncXGfeB8V1Weu7l6QUg`) y las mexicanas Juan Limón, Miguel Zermeño y Juan Campillo no le gustaron (muestras en `marketing/voces/`). La clave ya tiene permiso `voices_read`: para buscar voces usa `/v1/shared-voices` con `sort=usage_character_count_1y`.

```bash
cd video
C=public/<video>; V=dlGxemPxFMTY7iXagmOj
python3 -I scripts/elevenlabs.py voz $C l1 "Primera línea." $V 1.0      # … hasta l8
cd $C && for i in 1 2 3 4 5 6 7 8; do ffmpeg -loglevel error -y -i l$i.mp3 -af "loudnorm=I=-14:TP=-1.5:LRA=7" -ar 44100 -b:a 128k t.mp3 && mv t.mp3 l$i.mp3; done && cd ../..
python3 -I scripts/lineas.py $C src/Checa/lineas<Nombre>.ts l1 l2 l3 l4 l5 l6 l7 l8
```

`lineas.py` imprime cada palabra con su tiempo; úsalas para sincronizar (`en(i, "palabra")` con su puntuación exacta, p. ej. `"catorce."`). Si la suma pasa de ~37 s, recorta texto.

## 3. Escenas (Remotion + three.js)

- Arma `<Nombre>.tsx` a partir de `Calle3D.tsx` y `Vistas3D.tsx`: una sola `ThreeCanvas` con la ciudad, una `Camara` que elige la vista según el segmento de voz, y capas HTML encima (etiquetas `Caja`, número gigante, subtítulos, teléfono/pop-up). Usa `armarEscenas` y `crearEn` de `plantilla.tsx` para los tiempos.
- La cámara se mueve en `useLayoutEffect` con `camera.position.set(...)` y `lookAt(...)`; cambia `fov` por vista si hace falta.
- Lo que tape la vista: el número gigante va arriba solo cuando la cámara está alta (si no, tapa el pin); en la vista lateral oculta los edificios del lado de la cámara (`<Edificios soloLado={-1} />`).
- Fuentes cargadas en `Root.tsx`: Inter, InterDisplay, FiraCode y **Silkscreen** (400/700, `public/Silkscreen-*.woff2`).
- `ui.tsx`: `Telefono`, `Mapa`, `Oferta`, `Popup` (`exacto`, `pastillaColor="#FF7028"` para DiDi, montos con 2 decimales como texto, fichas `"$450/h"`, `"$15.00/km"`, `"20 min"` con `"meta $150"`, `"meta $8"`, `"10.0 km total"`), `Logo`.
- Registra la composición en `video/src/Root.tsx` (1080×1920, 30 fps). Nada importante abajo de y≈1570.

## 4. Revisar y renderizar (3D necesita `--gl=swangle`)

```bash
cd video
npm ci --loglevel=error          # solo si falta node_modules
npx tsc --noEmit
B=/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell
for f in 40 120 200 300 420 600 800 1100; do npx remotion still <Id> /tmp/claude-0/st/c$f.png --frame=$f --gl=swangle --browser-executable=$B --log=error; done
# juntar en una tira (hstack/vstack) y mirarla con Read
npx remotion render <Id> out/x.mp4 --gl=swangle --browser-executable=$B --log=error
ffmpeg -loglevel error -y -i out/x.mp4 -c:v copy -af "loudnorm=I=-14:TP=-1:LRA=9" -ar 48000 -c:a aac -b:a 192k out/<nombre>.mp4
```

Revisa: textos encimados con el pin o los edificios, la cámara metida en un edificio, partes vacías, pop-ups legibles. El render 3D tarda más (un video de 40 s, varios minutos).

## 5. Entregar

1. `cp video/out/<nombre>.mp4 marketing/videos/`
2. Commit con `[skip ci]` y `git push origin HEAD:main` (más la rama de trabajo si la hay).
3. Manda el video con `SendUserFile` (`display: "render"`) y explica en 3–5 líneas qué trae y qué revisar, con la duración. Si Eduardo pide los montos, dáselos en tabla y avisa de cualquier cifra que se vea distinta en el pop-up.
4. Marca el tema como hecho en `marketing/temas-videos.md` (nota con el archivo del video).

`video/out/` está en `.gitignore`; se versionan el código, `public/<video>/` y la copia en `marketing/videos/`.
