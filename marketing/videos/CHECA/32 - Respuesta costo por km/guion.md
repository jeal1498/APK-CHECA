# Guion · 32 - Respuesta costo por km (CHECA) · estado: propuesto

Video respuesta al comentario de un conductor en «14 - Miedo a rechazar»: su gasolina ronda 5 por km y con depreciación y mantenimiento el costo sube a 8–9 por km; para él un viaje aceptable paga 13–14+ por km y uno rentable 15+. Se muestra el comentario parafraseado, **sin su usuario**.

**Concepto / Problema detectado en el repo:** el costo por km cambia con cada carro. `Parser.gasolina()` usa rendimiento y precio del litro del conductor, y el filtro «Por km» (`minPorKm`, pasos de 0.5) deja fijar un mínimo propio que cubra desgaste. La app solo descuenta gasolina; el desgaste entra por su mínimo por km (no se promete otra cosa).

**Cálculo:** 70 ÷ 13 km = 5.38/km. Costo mínimo 13 km × 8 = 104. 70 − 104 = −34.

**Guión Desglosado:**
- `0:00 - 0:03` | Audio: Un conductor dice que 5 por kilómetro no alcanza, y tiene razón. | Visual: [Tarjeta de comentario (sin usuario) se estrella con zoom; resaltado «5 por km»]
- `0:03 - 0:06` | Audio: Su gasolina se lleva 5 por kilómetro, y con desgaste y mantenimiento ya son 8 o 9. | Visual: [Barra de costo sube 5 → 8–9 en rojo, stagger de 4 frames]
- `0:06 - 0:09` | Audio: Un viaje de 70 por 13 kilómetros paga 5 con 38 por kilómetro. | Visual: [Tarjeta OFERTA 70 · 13 km; cuenta 70 ÷ 13 = 5.38/km en rojo]
- `0:09 - 0:12` | Audio: Con ese costo, solo recorrerlo ya son 104 y quedas debajo en 34. | Visual: [Cuenta 13 × 8 = 104; 70 − 104 = −34 en rojo]
- `0:12 - 0:15` | Audio: Cada carro gasta distinto, así que tu mínimo por kilómetro lo pones tú. | Visual: [Tres siluetas de carro con distinto costo por km]
- `0:15 - 0:19` | Audio: En la app Checa fijas tu mínimo, por ejemplo 13, y los kilómetros que rinde tu litro. | Visual: [Ajustes «Por km» sube a 13; campo Rendimiento km/l]
- `0:19 - 0:22` | Audio: Cada oferta se compara con tu número. | Visual: [Pop-up «A · Franja» cae con spring; ficha $/km contra la raya de meta]
- `0:22 - 0:25` | Audio: Lo que no llega a tu mínimo sale en rojo. | Visual: [Franja RECHAZAR roja; «Números de ejemplo»]
- `0:25 - 0:28` | Audio: La app solo lee la pantalla, el botón siempre lo tocas tú. | Visual: [Ojo escaneando; dedo toca]
- `0:28 - 0:31` | Audio: Comenta CHECA y te paso el acceso a la prueba en Google Play. | Visual: [Pantalla final estándar]

**CTA final:** Comenta CHECA y te paso el acceso a la prueba en Google Play.
