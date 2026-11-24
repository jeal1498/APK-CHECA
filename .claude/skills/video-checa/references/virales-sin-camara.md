# Hooks Virales · Personal sin cámara, con la voz de Eduardo (aprobado 24 nov 2026, video 001)

Cuando Eduardo graba **solo la voz** (m4a, sin ElevenLabs y sin su cara). Ejemplo completo y plantilla: `marketing/videos/Hooks Virales/000 Personal/001 …/produccion/` (`pagina-bloques.html`, `render-bloques.js`), copia en `_plantilla-ps2/produccion/`.

## Flujo
1. Guarda el audio en el scratchpad (nunca en el repo). Corta los intentos repetidos (`atrim` + `concat`).
2. Ritmo: `silenceremove=start_periods=1:start_threshold=-38dB:stop_periods=-1:stop_duration=0.18:stop_threshold=-38dB:stop_silence=0.1,atempo=1.18,loudnorm=I=-14:TP=-1.5:LRA=11`. Luego `adelay=150:all=1,apad` (aire al inicio). Eduardo pidió ritmo rápido (24 nov): ~15 s, sin pausas.
3. Transcribe el audio **ya procesado** (`.claude/skills/video-checa/scripts/transcribir.py`) y arma `palabras.json` (`{"palabras":[[palabra,ini,fin]],"total":s}`; suma el `adelay`). Whisper escribe sin tildes y sin «¿»: corrige a mano («cuanto»→«cuánto», «que dato»→«¿Qué dato») porque `W('palabra')` busca el texto exacto. `W('cuenta#2')` = la tercera vez.
4. `NODE_PATH=$(npm root -g) node render-bloques.js hoja t1,t2,…` (cuadros) y `node render-bloques.js video` (renombra `renders/graficos.mp4`) → une con `ffmpeg -i graficos.mp4 -i voz.m4a -c:v copy -c:a aac -shortest`. Borra `renders/cuadros-bloques` antes de cada hoja (si no, mezcla cuadros viejos).
5. El `video.mp4` aprobado se commitea **una sola vez** en la carpeta del video; `renders/` y `palabras.js` están en `.gitignore`.

## Dirección de arte (Eduardo, 24 nov)
- **Antes del código, un concepto de acción visual por bloque** (qué se mueve, qué se transforma, cuadro final).
- Texto en pantalla de **máx. 4 palabras** por bloque (sin contar subtítulos); analogías **literales** (monedas que caen, trozo que se desprende de la barra).
- **Escala real** para magnitudes (barra de $120 con el trozo de gasolina de $52 y 68 restantes) y **enfoque/desenfoque** para guiar la mirada (zoom al precio, el resto borroso).
- **Morphing:** un mismo elemento cambia de estado entre bloques (tarjeta → barra → ficha); nada aparece de la nada.
- Picos de movimiento **en la palabra clave**; cada bloque termina en una composición limpia antes de cortar.
- **Ritmo:** golpe de zoom/giro en cada palabra, destello blanco y cambio de tinte en cada bloque.
- **Fondo PS2 completo** (cielo, ciudad, calle con autos) y textos blancos con contorno marino; subtítulos estilo B. Gráficos en la mitad de arriba (sin cámara no hay frontera a 45 %); nada en la franja de abajo (530 px) ni a la derecha (190 px) salvo el fondo.
- **Cierre del Personal = la pregunta** (para comentarios), con las opciones en pantalla; **no** mostrar a Checa ni su pop-up (eso es de la cuenta Checa). Si hace falta el pop-up, es el de la 10.86 (`docs/propuestas/popup-v9.html`), sin nombre de app («Tarifa · $120.00»).
- Sin nombres ni logos de apps; «Oferta de ejemplo» donde haya cifras; sin promesas de ganancia.
