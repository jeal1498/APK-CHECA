# Guion · 27 - Cuánto te queda de verdad (PERSONAL) — 30 oct 2026, skill `guion-checa`

**Estado:** propuesto. Para el jue 4 nov (8 pm, @Eduardoalv9802). Solo cuenta personal (sin v1 CHECA por ahora). Responde a la búsqueda «cómo saber tus ganancias de DiDi».

Concepto / Problema detectado en el repo:
La oferta muestra la tarifa, no lo que queda. `Parser.kt`: `gasolina()` = km totales (recogida + viaje) ÷ km/l × $/l; `neta()` = tarifa − gasolina. El pop-up pone «$x netos» y `Viaje $x · Gasolina −$y`. El conductor decide en segundos y no le da tiempo de restar.

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km)
- Tarifa 150; recogida 2 km / 5 min + viaje 13 km / 20 min = **15 km, 25 min**.
- Gasolina 15 ÷ 12 × 24 = **30** → **120 netos**. $/km 150/15 = 10 · $/h 150/25×60 = 360 → 🟢 ACEPTAR.
- 10 viajes así: 10 × 30 = **300** de gasolina al día.

## Guion de voz (limpio, ~25 s)
Te ofrecen 150 y a tu bolsa entran 120. Son 15 kilómetros, contando la recogida. Con 12 kilómetros por litro y el litro a 24, se van 30 de gasolina. Si haces 10 viajes así, son 300 al día que la oferta no te enseña. Y con la oferta parpadeando, casi nadie saca esa cuenta. La app Checa te pone lo que te queda, antes de aceptar. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «Te ofrecen 150 y a tu bolsa entran 120.» | Remotion / Visual: [Flash en el cuadro 0; tarjeta OFERTA $150 se estrella con zoom; contador 150 → 120 que cae; cámara sacude la cifra; «Números de ejemplo»]
- `0:03 - 0:06` | Audio: «Son 15 kilómetros, contando la recogida.» | Remotion / Visual: [Ruta que se dibuja: tramo recogida 2 km + viaje 13 km = 15 km; stagger de 4 frames]
- `0:06 - 0:09` | Audio: «Con 12 kilómetros por litro y el litro a 24, se van 30 de gasolina.» | Remotion / Visual: [Cuenta 15 km ÷ 12 km/l × $24 = $30; ticket «Gasolina −$30» cae en rojo]
- `0:09 - 0:13` | Audio: «Si haces 10 viajes así, son 300 al día que la oferta no te enseña.» | Remotion / Visual: [10 tickets −$30 se apilan rápido; contador 0 → $300 en rojo]
- `0:13 - 0:16` | Audio: «Y con la oferta parpadeando, casi nadie saca esa cuenta.» | Remotion / Visual: [Tarjeta OFERTA con barra de tiempo que se vacía; la cuenta tiembla con «?»]
- `0:16 - 0:20` | Audio: «La app Checa te pone lo que te queda, antes de aceptar.» | Remotion / Visual: [Pop-up «A · Franja» cae con spring: ACEPTAR · «$120 netos» · `Viaje $150 · Gasolina −$30` · fichas $360/h, $10/km]
- `0:20 - 0:24` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA»]

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
Marca v2, pop-up con «OFERTA», montos sin moneda en la voz, sin `[pause]` en el gancho, sin ciudad ni país, sin promesa de ganancia. Todo por encima de y≈1150.
