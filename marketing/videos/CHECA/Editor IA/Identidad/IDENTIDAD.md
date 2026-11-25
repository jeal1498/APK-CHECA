# Identidad de video · CHECA (cuenta @checa.driver)

Elegida por Eduardo el 25 nov 2026. **La marca no cambia:** vive en `jeal1498/PWA-Checa-Multimedia/marca/` (colores, letras, logo, sonidos oficiales) y aquí no se copia; este documento solo dice cómo se ve la marca **en video**.

## Estilo vigente: «La raya» (propuesta C)
Opciones vistas: `propuestas-estilo/estilos-25nov.html` (A Asfalto vivo, B Tablero, **C La raya**).

- **Personaje:** la raya amarilla de la carretera (`#FACC15`). Viene avanzando y se transforma en lo que cuenta la historia: una barra de dinero (cada raya = $10), una medida, una línea de tiempo. Escala real: el largo representa la cantidad.
- **Color que decide:** lo que te quitan se pinta de rojo `#EF4444` y se cae; lo que te queda, de verde `#22C55E`; lo que hay que recordar, amarillo. Todo lo demás en hueso `#F4F4EF` sobre asfalto `#0C0C10`; tarjetas en tablero `#16161C`.
- **Letras:** Bricolage Grotesque 800 (títulos y cifras), DM Sans (texto), JetBrains Mono (datos, etiquetas, «NÚMEROS DE EJEMPLO»).
- **Pantalla:** Eduardo a cámara arriba (45 %, 0–864 px); gráficos abajo. Nada importante en la orilla derecha (x > 900, y 900–1600: botones de las redes).
- **Subtítulos:** palabra por palabra al centro, sobre la línea de la cámara; mayúsculas Bricolage 800 en hueso con contorno negro; la palabra clave en el color que decide (verde = lo que ganas, amarillo = a recordar, rojo = lo que pierdes). 150 ms por palabra (marca).
- **Movimiento:** siempre hay algo moviéndose (la raya avanza); curva de la marca `cubic-bezier(.2,.8,.2,1)`, sin rebote.
- **Cifras:** con las fórmulas de la app (gasolina = km ÷ 12 km/l × $24; metas $200/h y $10/km), cuenta en pequeño y «NÚMEROS DE EJEMPLO» a la vista. Sin logos ni nombres de apps de viajes, sin ciudad ni país, sin promesas de ganancia.
- **Semáforo de Checa:** se enciende en el color del veredicto; cierra cada video con la luz verde y la ✓.

## Decisiones del 25 nov (opciones en `propuestas-estilo/pendientes-25nov.html`)
1. **Pop-up: sale del semáforo.** La luz del veredicto se enciende, crece y se abre en el pop-up de Checa (el de la app: tablero, borde y chip del color; los tres colores miden igual). Al salir, regresa a la luz.
2. **Cámara: acercamiento en los golpes.** Arriba (45 %); en cada palabra clave se acerca 14 % y regresa. Cortar silencios de más de 0.35 s; color frío de noche.
3. **Sonido: los 7 efectos oficiales** de `PWA-Checa-Multimedia/marca/sonido/`, sin música (como decidió la marca el 17 oct). Un efecto por evento visual, máx. 3 por escena, al 30–50 % con voz.
4. **Transiciones: la raya se transforma.** Las mismas rayas pasan de barra de dinero a reloj, a regla de km, etc. Nunca hay corte.
5. **Final: cierre de marca + bucle.** Semáforo rojo → ámbar → verde → ✓ (1.2 s, sonido `logo-semaforo`), mensaje y cuentas; después las rayas vuelven a la carretera y el último cuadro empata con el primero.

## Final con Google Play (25 nov)
Checa ya está en producción en Google Play. Elegido por Eduardo (propuesta C «Distintivo», 25 nov): subtítulos «YA DISPONIBLE EN **PLAY STORE**» y «BÚSCALA COMO **CHECA**»; semáforo rojo → ámbar → verde → ✓; la luz verde crece y se abre en el distintivo negro «YA DISPONIBLE EN / Play Store»; arriba el ícono de la app y abajo el nombre tal como sale en la tienda, **«Checa: Rentabilidad de viajes»** (sin «Beta»), y las cuentas; después todo regresa a la carretera (bucle). Se dice «Play Store» (como lo conocen los conductores). Sin el logo de Google Play (sus reglas de uso); si se quiere el distintivo oficial, se usa tal cual. Código: `video/src/Raya/FinalPlay.tsx` (`Raya-Final-Play`, entrada `src/Raya/index.ts`).

## Pendiente
Pieza modelo completa con un guion de Checa.
