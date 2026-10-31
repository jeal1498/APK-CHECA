# Guion · 30 - Checa en tres segundos (CHECA) — 31 oct 2026, skill `guion-checa`

**Estado:** guion y piloto aprobados (31 oct 2026); video producido con control de calidad, falta la revisión final de Eduardo. Pedido: «video promocional de cómo funciona Checa, nuevo, con estilo de iPhone».

Concepto / Problema detectado en el repo:
El conductor tiene segundos para decidir y solo ve la tarifa bruta. Checa (`Parser.kt`) resta la gasolina de recogida + viaje, calcula $/h y $/km sobre km y min **totales**, pinta el semáforo (`nivel()`, `motivoFiltro()` con recogida máx.) y elige la mejor de dos (`Comparador.kt`: primero el nivel). Mismo explicador que el 17, pero con **estilo de anuncio de iPhone**: fondo negro (asfalto), un teléfono flotando que gira despacio con luz suave, frases de 2–4 palabras gigantes que entran una por una, mucho aire y cortes limpios.

## Estilo «iPhone» (para producción)
- Teléfono mockup (marco titanio oscuro, esquinas redondas, isla arriba) flotando al centro, con giro 3D lento (±12°) y reflejo suave; la pantalla muestra la oferta y el pop-up de Checa.
- Títulos tipo keynote: Bricolage 800 hueso, 140–200 px, una frase por toma, entran desde máscara con desenfoque → nítido.
- Cámara: push-in lento continuo en cada toma; transiciones donde el teléfono se transforma en la siguiente escena (zoom a la pantalla, sale la cifra).
- Color solo para decidir (verde/amarillo/rojo); todo lo demás asfalto, tablero y hueso. Pop-ups con «OFERTA». «Números de ejemplo» donde haya cifras. Todo lo importante arriba de y≈1150.
- Necesita un tipo de escena nuevo `iphone` en `video/hyperframes/_base/construir.py` (mockup + giro + título keynote); lo demás sale con `numero`, `fichas`, `estados`, `duo`, `metas` y `cierre`.

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km)
- **Oferta A (verde):** tarifa $108 · recogida 2 km / 4 min · viaje 10 km / 18 min → 12 km, 22 min. Gasolina 12 ÷ 12 × 24 = **$24** → **$84 netos**. $/km 108 ÷ 12 = **$9.0** ✓ · $/h 108 ÷ 22 × 60 = 294.5 → **$295** ✓ → ACEPTAR.
- **Oferta B (rojo):** tarifa $130 · recogida 5 km / 10 min · viaje 15 km / 32 min → 20 km, 42 min. Gasolina 20 ÷ 12 × 24 = **$40** → **$90 netos**. $/km 6.5 ✗ · $/h 185.7 → $186 ✓ · recogida 5 km > 4 → RECHAZAR («recogida lejos»).
- **Comparador:** A verde gana a B rojo por nivel → ★ MEJOR la de 108, aunque B paga más.

## Guion de voz (limpio, ~32 s, voz Fernando)
Te ofrecen 108. Tienes tres segundos para decidir. La app Checa lee la oferta por ti. Le resta la gasolina de todo el camino. Te quedan 84. Al instante. Por hora, 295. Por kilómetro, 9. Verde, acéptalo. Amarillo, piénsalo. Rojo, déjalo pasar. Caen dos ofertas: 130 y 108. Gana la de 108. La de 130 trae la recogida lejos. Tus metas. Tu semáforo. Checa: lee, calcula y decide. Comenta CHECA y te paso el acceso a la prueba en Google Play.

Concepto / Problema detectado en el repo: ver arriba.
Guión Desglosado:
- `0:00 - 0:03` | Audio: «Te ofrecen 108. Tienes tres segundos para decidir.» | Remotion / Visual: [Flash en el cuadro 0; teléfono entra girando desde abajo con zoom; en su pantalla la tarjeta «$108.00 · Aceptar»; un «3» gigante detrás cuenta 3 → 2 → 1 con cada golpe y la cámara sacude] `[Hook]`
- `0:03 - 0:06` | Audio: «La app Checa lee la oferta por ti.» | Remotion / Visual: [Teléfono de frente, giro lento; línea de escaneo verde baja por la pantalla; título keynote «Lee.» entra desde máscara] `[iphone]`
- `0:06 - 0:09` | Audio: «Le resta la gasolina de todo el camino.» | Remotion / Visual: [Zoom a la pantalla; ticket «Viaje $108 · Gasolina −$24» con la gasolina cayendo en rojo; título «Calcula.»] `[iphone]`
- `0:09 - 0:12` | Audio: «Te quedan 84. Al instante.» | Remotion / Visual: [El teléfono se hace a un lado y sale «$84 netos» gigante en verde, contador 108 → 84; «Números de ejemplo»] `[numero]`
- `0:12 - 0:15` | Audio: «Por hora, 295. Por kilómetro, 9.» | Remotion / Visual: [Fichas «$295/h» y «$9/km» flotan una por una en su palabra, borde verde, push-in lento] `[fichas]`
- `0:15 - 0:18` | Audio: «Verde, acéptalo. Amarillo, piénsalo. Rojo, déjalo pasar.» | Remotion / Visual: [Pantalla del teléfono cambia de pop-up en cada color: ACEPTAR → CONSIDERAR → RECHAZAR, mismo tamaño] `[estados]`
- `0:18 - 0:21` | Audio: «Caen dos ofertas: 130 y 108. Gana la de 108.» | Remotion / Visual: [Dos teléfonos lado a lado entran con stagger; el de $108 (ACEPTAR, $84 netos) se adelanta con ★ MEJOR; el de $130 se apaga] `[duo]`
- `0:21 - 0:24` | Audio: «La de 130 trae la recogida lejos.» | Remotion / Visual: [Zoom al pop-up de $130: RECHAZAR $90 netos, ficha roja «recogida 5 km» que late] `[popup]`
- `0:24 - 0:27` | Audio: «Tus metas. Tu semáforo.» | Remotion / Visual: [Tarjetas «Meta por hora $150» y «Meta por km $8» flotan girando, el valor sube con interpolate] `[metas]`
- `0:27 - 0:30` | Audio: «Checa: lee, calcula y decide.» | Remotion / Visual: [Toma de producto: teléfono gira 360° despacio y queda de frente con el pop-up verde; las tres palabras entran una por una en su palabra] `[iphone]`
- `0:30 - 0:33` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA», sin link] `[cierre]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Tabla de animaciones por toma (sacada del desglose)
| Toma | Frase | Animación |
|---|---|---|
| 0:00–0:03 | «Te ofrecen 108. Tienes tres segundos para decidir.» | Flash en el cuadro 0; teléfono entra girando desde abajo con zoom; en su pantalla la tarjeta «$108.00 · Aceptar»; un «3» gigante detrás cuenta 3 → 2 → 1 con cada golpe y la cámara sacude |
| 0:03–0:06 | «La app Checa lee la oferta por ti.» | Teléfono de frente, giro lento; línea de escaneo verde baja por la pantalla; título keynote «Lee.» entra desde máscara |
| 0:06–0:09 | «Le resta la gasolina de todo el camino.» | Zoom a la pantalla; ticket «Viaje $108 · Gasolina −$24» con la gasolina cayendo en rojo; título «Calcula.» |
| 0:09–0:12 | «Te quedan 84. Al instante.» | El teléfono se hace a un lado y sale «$84 netos» gigante en verde, contador 108 → 84; «Números de ejemplo» |
| 0:12–0:15 | «Por hora, 295. Por kilómetro, 9.» | Fichas «$295/h» y «$9/km» flotan una por una en su palabra, borde verde, push-in lento |
| 0:15–0:18 | «Verde, acéptalo. Amarillo, piénsalo. Rojo, déjalo pasar.» | Pantalla del teléfono cambia de pop-up en cada color: ACEPTAR → CONSIDERAR → RECHAZAR, mismo tamaño |
| 0:18–0:21 | «Caen dos ofertas: 130 y 108. Gana la de 108.» | Dos teléfonos lado a lado entran con stagger; el de $108 (ACEPTAR, $84 netos) se adelanta con ★ MEJOR; el de $130 se apaga |
| 0:21–0:24 | «La de 130 trae la recogida lejos.» | Zoom al pop-up de $130: RECHAZAR $90 netos, ficha roja «recogida 5 km» que late |
| 0:24–0:27 | «Tus metas. Tu semáforo.» | Tarjetas «Meta por hora $150» y «Meta por km $8» flotan girando, el valor sube con interpolate |
| 0:27–0:30 | «Checa: lee, calcula y decide.» | Toma de producto: teléfono gira 360° despacio y queda de frente con el pop-up verde; las tres palabras entran una por una en su palabra |
| 0:30–0:33 | «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Pantalla final estándar: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA», sin link |
