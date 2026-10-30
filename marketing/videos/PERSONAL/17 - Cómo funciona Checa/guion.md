# Guion · 17 - Cómo funciona Checa (PERSONAL v2) — 30 oct 2026, con la skill `guion-remotion-checa`

**Estado:** guion propuesto, por aprobar. Misma idea y cifras que `CHECA/17`; otro gancho (arranca con lo que te queda, no con la pregunta), otras palabras y otro orden (metas y recogida al final del semáforo, horas antes del comparador).

Concepto / Problema detectado en el repo:
El conductor decide viendo solo la tarifa bruta. `Parser.kt`: `gasolina()` = km totales / km por litro × $/litro; `neta()` = tarifa − gasolina; $/h y $/km sobre km y min totales; `nivel()` y `motivoFiltro()` pintan el semáforo; `Comparador.kt` elige la mejor de dos; `Popup.kt` muestra franja ACEPTAR/CONSIDERAR/RECHAZAR, «$x netos» y fichas. Explicador sin promesas de ganancia.

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km)
- **Verde:** tarifa $150, 16 km (3 recogida + 13 viaje), 28 min. Gasolina 16/12×24 = **$32** → **$118 netos**; $/km 9.4 ✓; $/h 150/28×60 = **$321** ✓ → ACEPTAR.
- **Rojo:** viaje $120, 24.5 km → gasolina $49 → **$71 netos**, recogida 6 km > 4 → RECHAZAR.
- **Dos ofertas:** $118 (★ mejor) y $84 (viaje $112, gasolina $28, CONSIDERAR).

## Guion de voz (limpio, ~32 s, Fernando v3)
Viaje de 150 pesos, y a ti te quedan 118. Checa te lo muestra antes de que aceptes. Lee la oferta sola y pone su aviso encima. Descuenta la gasolina de ir por el pasajero y del viaje. Ves cuánto sacas por hora y por kilómetro. Verde, acepta. Amarillo, piénsalo. Rojo, déjala pasar. Las metas las pones tú. Si la recogida queda más lejos de lo que aceptas, sale en rojo. Te enseña en qué horas conviene salir. Y si caen dos ofertas, te marca la mejor. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «Viaje de 150 pesos, y a ti te quedan 118.» | Remotion / Visual: [Flash en el cuadro 0; tarjeta de oferta «$150.00» se estrella con zoom grande; contador $150 → $118 con interpolate + Math.round y sacudida de cámara; sello «¿$?»] `[Hook]`
- `0:03 - 0:06` | Audio: «Checa te lo muestra antes de que aceptes.» | Remotion / Visual: [Toque con ondita sobre «Aceptar» que se frena; pop-up «A · Franja» cae con spring encima; «Números de ejemplo»] `[Payoff]`
- `0:06 - 0:09` | Audio: «Lee la oferta sola y pone su aviso encima.» | Remotion / Visual: [Línea de escaneo verde que baja sobre la tarjeta; texto leído en chips mono (tarifa, km, min); logo semáforo ✓ en la esquina] `[Re-enganche visual]`
- `0:09 - 0:12` | Audio: «Descuenta la gasolina de ir por el pasajero y del viaje.» | Remotion / Visual: [Ruta line-art con dos tramos (recogida 3 km, viaje 13 km) que se dibujan con strokeDashoffset; ticket Gasolina −$32 cae en rojo] `[Giro]`
- `0:12 - 0:15` | Audio: «Ves cuánto sacas por hora y por kilómetro.» | Remotion / Visual: [Zoom 1 → 1.4 a las fichas $321/h y $9.4/km; stagger de 4 frames, borde verde] `[Micro-hook]`
- `0:15 - 0:18` | Audio: «Verde, acepta. Amarillo, piénsalo. Rojo, déjala pasar.» | Remotion / Visual: [Tres pop-ups del mismo tamaño que se reemplazan en cada palabra: ACEPTAR → CONSIDERAR → RECHAZAR] `[Cambio de ritmo]`
- `0:18 - 0:21` | Audio: «Las metas las pones tú.» | Remotion / Visual: [Tarjetas «Meta por km $8» y «Meta por hora $150» con − / +; valores suben con interpolate] `[Control]`
- `0:21 - 0:24` | Audio: «Si la recogida queda más lejos de lo que aceptas, sale en rojo.» | Remotion / Visual: [Ruta «6 km» se dibuja; ficha roja; pop-up RECHAZAR $71 netos] `[Giro]`
- `0:24 - 0:27` | Audio: «Te enseña en qué horas conviene salir.» | Remotion / Visual: [Cuadrícula de horas que se pinta con stagger; 17–19 h en verde] `[Re-enganche visual]`
- `0:27 - 0:30` | Audio: «Y si caen dos ofertas, te marca la mejor.» | Remotion / Visual: [Layout split: dos pop-ups; el de $118 con ★ MEJOR, el de $84 apagado «2ª»] `[Payoff]`
- `0:30 - 0:33` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA», sin link] `[CTA]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
- Marca v2; pop-ups con «OFERTA», sin logos ni nombres de apps; sin ciudad ni país; sin pausas largas ni `[pause]`; voz v3 en todas las líneas; mp4 yuv420p High.
- Plantilla: `video/hyperframes/como-funciona-17/construir.py`.
