---
name: videos-checa
description: Hacer videos cortos explicativos de Checa (TikTok, Reels, Shorts) con narrador de IA, gráficos de calle y ritmo alto, usando la plantilla de Remotion del repo. Úsala cuando Eduardo pida un video, un guion para video, «otro video como el de la recogida», cambiar la voz o la música de un video, o un video sin que él salga a cuadro.
---

# Videos explicativos de Checa

Formato aprobado por Eduardo (video de referencia: `marketing/videos/recogida-v2.mp4`, código en `video/src/Checa/Recogida2.tsx`):
narrador con voz de IA, **sin mascota y sin persona a cuadro**, fondo de asfalto de noche, gráficos del trabajo (carro, letreros de carretera, ticket, bomba de gasolina, billetes, semáforo, teléfono con la oferta y el pop-up de Checa), **algo que se mueve o golpea cada ~1.5 s**, música propia y efectos de sonido.

Lee `.agents/product-marketing.md` antes de escribir el guion. Responde a Eduardo en español y corto.

## 1. Guion (primero, y pedir aprobación antes de producir)

Eduardo quiere ver y aprobar el guion antes de gastar créditos. Preséntalo como tabla de segundos con 🎙️ voz y 🖥️ texto en pantalla.

Estructura que funcionó (30–40 s, 6–7 líneas de voz):
1. **Gancho + promesa** (0–3 s): el dolor del conductor en una frase, y una promesa que se cumple al final («Al final te digo cuánto pierdes al mes»). Imagen, voz y texto desde el cuadro 0.
2. **El problema** en una idea.
3. **Ejemplo concreto** con números de una oferta.
4. **La cuenta** (lo que de verdad queda).
5. **Lo que se acumula** (día → mes): cumple la promesa del gancho.
6. **La solución**: Checa hace la cuenta antes de aceptar.
7. **Cierre**: «Pruébala siete días gratis» + una **pregunta** para comentarios.

Reglas:
- Narrador informativo que le habla al conductor de **tú**. Nada de «yo» (Eduardo no sale en el video).
- Pegarle al dolor real con palabras de conductor («la lana», «te cae un viaje», «corren por tu cuenta»), sin groserías.
- Números honestos y conservadores. Gasolina por defecto: $24 el litro, 12 km/l = **$2 por km**. Las cifras deben salir de las fórmulas de la app (`Parser.kt`): $/km = tarifa ÷ km totales (con recogida), $/h = tarifa ÷ min totales × 60, neta = tarifa − km × costo por km. Explica la cuenta a Eduardo cuando haya una cifra grande.
- Nunca inventar testimonios, clientes ni ganancias. No mencionar ciudad ni país. No nombrar a la competencia.
- Sin muletillas de IA: nada de «no es X, es Y» como revelación ni listas «sin X, sin Y».
- Los números en la voz van escritos con letra («ochenta pesos») para que la IA los lea bien.
- Ofrece 2–3 ganchos alternativos.

Checklist viral (revísalo antes de entregar el guion): gancho en 1–3 s, una sola idea, emoción, ritmo sin pausas, curiosidad que se resuelve al final, 15–40 s, útil o identificable, pregunta para comentarios, funciona sin sonido (subtítulos), cierre fuerte. Ser honesto: nadie puede garantizar que algo se haga viral.

## 2. Voz, música y efectos (ElevenLabs)

- Plan **Starter** de Eduardo (30 000 créditos/mes, licencia comercial). Un video de ~40 s gasta ~700 caracteres de voz, más la música.
- La **clave** la da Eduardo en el chat. Guárdala solo en `/tmp/claude-0/.el_key` (con `umask 077`) o en `ELEVENLABS_API_KEY`. **Nunca** en el repo ni en commits. Debe tener permisos de Text to Speech, Music y Sound Effects.
- Voz del narrador: `gncXGfeB8V1Weu7l6QUg` (ya es la predeterminada del script), velocidad 1.15.

```bash
cd video
C=public/<video>        # carpeta nueva por video, p. ej. public/gasolina
python3 -I scripts/elevenlabs.py voz $C l1 "Primera línea del guion."
# … una línea por escena: l1, l2, … l7
python3 -I scripts/lineas.py $C src/Checa/lineas<Nombre>.ts l1 l2 l3 l4 l5 l6 l7
python3 -I scripts/elevenlabs.py musica $C musica "Energetic instrumental background track for a short social media video about rideshare drivers at night. Modern latin urban beat, punchy drums, deep bass, catchy synth plucks, driving forward momentum, 100 BPM, no vocals, consistent energy, suitable under a voiceover." 42
python3 -I scripts/elevenlabs.py sfx $C frenado "car tires screeching, short sharp brake skid" 1.2
```

Efectos ya hechos que se pueden reutilizar: `public/recogida2/{motor,frenado,caja,pop}.mp3` y `public/{golpe,whoosh,ding,error}.wav`. La música de la recogida (`public/recogida2/musica.mp3`) se puede reutilizar si el video dura ≤ 42 s.

`lineas.py` imprime las palabras con su tiempo: úsalas para sincronizar. Si la suma de duraciones pasa de ~38 s, recorta texto o sube la velocidad (máx. 1.2).

## 3. Escenas en Remotion (copiar la plantilla)

Copia `video/src/Checa/Recogida2.tsx` a `video/src/Checa/<Nombre>.tsx` y cambia solo: el import de `lineas<Nombre>`, `MUSICA`, las escenas `S1…S7` (una por línea de voz), `GOLPES`, `SUB_COLOR` y `TONO`. Todo lo común vive en:
- `plantilla.tsx`: `armarEscenas`, `crearEn` (`en(línea, "palabra", vez)` → cuadro en que se dice), `Golpe` (entra de golpe), `Cae` (cae desde arriba), `Titulo`, `Pill`, `Subs` (karaoke) y `VideoExplicativo` (asfalto, música con fade, voces, efectos, cámara con zoom/sacudida/destello, barrido entre escenas, subtítulos).
- `graficos.tsx`: `Asfalto`, `Letrero` (de carretera: verde, rojo o amarillo), `Carro` (de lado, llantas que giran, humo), `Ticket` (se imprime por líneas), `Bomba` (display de pesos y litros), `Billetes` (salen volando), `SemaforoLuz`, y la paleta `K`.
- `ui.tsx`: `Telefono`, `Mapa`, `Oferta` (oferta genérica, no imita marcas), `Popup` (el pop-up real de Checa), `Logo`.

Registra la composición en `video/src/Root.tsx` (1080×1920, 30 fps, `durationInFrames` exportado por el archivo).

Reglas de diseño:
- Contenido entre y≈90 y y≈1280; subtítulos fijos en y=1340; nada importante abajo de ~1570 (lo tapa la interfaz de TikTok).
- **Un golpe cada 1–2 s** en `GOLPES`, sincronizado con la palabra clave (`en(i, "palabra")`), con su efecto de sonido. Los `fuerte: true` (sacudida) para los números que duelen.
- Cada escena debe tener algo en pantalla desde su primer cuadro (no dejar huecos esperando a la voz).
- Colores: verde/amarillo/rojo **solo** con su significado de semáforo; amarillo de carretera para resaltar; letreros verdes para datos neutros, rojos para pérdidas.
- Escribe las palabras de `en()` exactamente como salen en `lineas.py` (con puntuación: `"catorce."`, `"aceptes:"`). Si se repite, usa el 3er argumento (`vez`).

## 4. Revisar y renderizar

```bash
cd video
npm ci --loglevel=error          # solo si falta node_modules
npx tsc --noEmit
B=/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell
for f in 20 150 300 450 600 750 900 1050; do npx remotion still <Id> /tmp/claude-0/stills/c$f.png --frame=$f --browser-executable=$B --log=error; done
# juntar en una tira y mirarla con Read:
ffmpeg -loglevel error -y -i c20.png … -filter_complex "hstack=8,scale=2000:-1" tira.png
npx remotion render <Id> out/<nombre>.mp4 --browser-executable=$B --log=error
```

Revisa en la tira: textos encimados, cosas cortadas en los bordes, escenas vacías y subtítulos pegados. Una pasada de arreglos y render final.

Volumen: la música va a `volMusica` 0.11 (≈12 dB debajo de la voz). Si cambias de pista, mide con `ffmpeg -i x.mp3 -af volumedetect -f null -` y ajusta.

## 5. Entregar

1. `cp video/out/<nombre>.mp4 marketing/videos/`
2. Commit con `[skip ci]` en el mensaje (no compila APK) y `git push origin HEAD:main` (más la rama de trabajo si la hay).
3. Manda el video con `SendUserFile` (`display: "render"`) y explica en 3–5 líneas qué trae y qué revisar. Menciona la duración.

`video/out/` está en `.gitignore`; lo que se versiona es el código, `public/<video>/` (voz, tiempos, música, efectos) y la copia en `marketing/videos/`.
