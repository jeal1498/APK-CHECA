# Guion · 28 - El 1.4x que paga menos por hora (CHECA) — 30 oct 2026, skill `guion-checa`

**Estado:** propuesto. Para el jue 5 nov (8 pm, @checa.driver). Solo cuenta CHECA (sin v2 PERSONAL por ahora).

Concepto / Problema detectado en el repo:
El ⚡ jala el dedo, pero una dinámica con recogida lejana puede dejar menos por hora. `Comparador.kt`: con dos ofertas del mismo nivel gana la de mejor neta por hora (`criterio` 0 por defecto) y lleva ★. `Popup.kt` muestra `⚡1.4x` junto a `Viaje · Gasolina`.

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km, filtro de recogida apagado como viene)
- **A (⚡1.4x):** tarifa 126; recogida 5 km / 12 min + viaje 9 km / 20 min = 14 km, 32 min. Gasolina 14 ÷ 12 × 24 = 28 → **98 netos**. Neta por hora 98 ÷ 32 × 60 = **184**. ($/km 9, $/h 236 → 🟢)
- **B (sin dinámica):** tarifa 95; recogida 1 km / 3 min + viaje 9 km / 17 min = 10 km, 20 min. Gasolina 20 → **75 netos**. Neta por hora 75 ÷ 20 × 60 = **225**. ($/km 9.5, $/h 285 → 🟢)
- Los dos 🟢 → empate de nivel → gana B por neta/hora (225 > 184) → ★ en B.

## Guion de voz (limpio, ~30 s)
Este 1.4 paga más y te deja menos por hora. Paga 126, pero la recogida es de 5 kilómetros. Son 32 minutos y te quedan 98. Por hora, 184. La otra, sin dinámica, paga 95. Recogida a 1 kilómetro, 20 minutos, te quedan 75. Por hora, 225. Con el 1.4 ganas más en ese viaje, pero tardas más en ganarlo. La app Checa compara las dos y le pone estrella a la que más te deja por hora. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «Este 1.4 paga más y te deja menos por hora.» | Remotion / Visual: [Flash en el cuadro 0; sello ⚡1.4x se estrella con zoom; cámara sacude; título grande «−$/HORA» con sello rojo; «Números de ejemplo»]
- `0:03 - 0:06` | Audio: «Paga 126, pero la recogida es de 5 kilómetros.» | Remotion / Visual: [Tarjeta OFERTA ⚡1.4x con contador 0 → $126; tramo de recogida 5 km que se estira en rojo]
- `0:06 - 0:09` | Audio: «Son 32 minutos y te quedan 98. Por hora, 184.» | Remotion / Visual: [Reloj 32 min; ticket Gasolina −$28; contador 126 → 98; cifra $184/h se revela]
- `0:09 - 0:12` | Audio: «La otra, sin dinámica, paga 95.» | Remotion / Visual: [Layout split: la 2ª tarjeta OFERTA $95 entra con spring y la 1ª se reduce]
- `0:12 - 0:16` | Audio: «Recogida a 1 kilómetro, 20 minutos, te quedan 75. Por hora, 225.» | Remotion / Visual: [Tramo 1 km corto; reloj 20 min; contador 95 → 75; $225/h se revela]
- `0:16 - 0:20` | Audio: «Con el 1.4 ganas más en ese viaje, pero tardas más en ganarlo.» | Remotion / Visual: [Dos barras de neta por hora: $225 sube por encima de $184; raya de meta $150/h que se dibuja debajo de las dos]
- `0:20 - 0:24` | Audio: «La app Checa compara las dos y le pone estrella a la que más te deja por hora.» | Remotion / Visual: [Dos pop-ups «A · Franja» ACEPTAR: $98 netos con ⚡1.4x y $75 netos con ★ y borde hueso]
- `0:24 - 0:28` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA»]

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
Marca v2, pop-ups con «OFERTA», montos sin moneda en la voz, sin `[pause]` en el gancho, sin ciudad ni país, sin promesa de ganancia. La ★ sigue el criterio por defecto (neta por hora). Todo por encima de y≈1150.
