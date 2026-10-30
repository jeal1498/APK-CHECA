# 26 · Dos ofertas, misma neta (Remotion, regla de 3 s)

Skill: `guion-remotion-checa`. Estado: guion propuesto, sin aprobar.

## Concepto / problema detectado en el repo
El conductor decide en segundos viendo la **tarifa bruta**. La app no le dice cuánto se come la gasolina de la recogida + viaje ni cuánto tiempo le cuesta. Checa lo calcula (`Parser.kt`: `gasolina()` = km totales / km·l × $/l; `neta()`; `porKm` y `porHora` sobre km y min totales), pinta el semáforo con sus metas (`nivel()`) y, con dos ofertas a la vez, marca la mejor por **neta por hora** (`Comparador.kt`, criterio 0).

## Números de ejemplo (calculados con las fórmulas del código)
Ajustes por defecto: 12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km.
- **Oferta A (Uber):** $95 · recogida 3.8 km / 8 min · viaje 10 km / 22 min → 13.8 km, 30 min. Gasolina $27.60 → **$67 netos**. $6.88/km ✗, $190/h ✓ → 🟡 CONSIDERAR.
- **Oferta B (DiDi):** $82 · recogida 1.2 km / 4 min · viaje 6 km / 11 min → 7.2 km, 15 min. Gasolina $14.40 → **$68 netos**. $11.39/km ✓, $328/h ✓ → 🟢 ACEPTAR, ★ mejor.

## Guion desglosado
- `0:00 - 0:03` | Audio: «Te cae un viaje de 95 pesos… pero en tu bolsa nomás se quedan 67.» | Remotion / Visual: [RideCard «$95» con spring entry desde abajo; a 0:01.5 tachado SVG que se dibuja (strokeDashoffset) y debajo contador $95 → $67 con interpolate + Math.round]
- `0:03 - 0:06` | Audio: «La gasolina de la recogida y del viaje también la pagas tú.» | Remotion / Visual: [Mapa line-art: ruta de recogida (3.8 km) y de viaje (10 km) se dibujan; ficha «Gasolina −$27.60» entra con spring y empuja el monto]
- `0:06 - 0:09` | Audio: «Y te va a tomar media hora.» | Remotion / Visual: [Reloj circular que se llena de 0 a 30 min (interpolate sobre el arco); texto palabra por palabra]
- `0:09 - 0:12` | Audio: «Al mismo tiempo cae otra: 82 pesos.» | Remotion / Visual: [Layout split: segunda RideCard «$82» entra desde la derecha con spring; la primera se reduce a la mitad izquierda (scale + translateX interpolados)]
- `0:12 - 0:15` | Audio: «Parece peor. Es menos dinero.» | Remotion / Visual: [Flecha roja ↓ sobre $82 con spring; leve shake (sin) de 6 frames]
- `0:15 - 0:18` | Audio: «Pero la recogida es corta y el viaje dura 15 minutos.» | Remotion / Visual: [En la tarjeta B: fichas JetBrains Mono «1.2 km» y «15 min» aparecen escalonadas (stagger 4 frames)]
- `0:18 - 0:21` | Audio: «Restas gasolina: 68 netos. Uno más que la de 95.» | Remotion / Visual: [Dos contadores en paralelo: A $95 → $67, B $82 → $68; el de B termina en verde]
- `0:21 - 0:24` | Audio: «En la mitad del tiempo.» | Remotion / Visual: [Barras de tiempo: A 30 min, B 15 min, crecen con spring; etiqueta «$135/h netos» vs «$270/h netos»]
- `0:24 - 0:27` | Audio: «Eso te lo dice Checa en el pop-up, antes de aceptar.» | Remotion / Visual: [Componente Popup «A · Franja»: franja verde ACEPTAR con pastilla «★ DIDI», «$68 netos»; el de A con franja amarilla CONSIDERAR y «2ª» apagado. Entrada spring escalonada]
- `0:27 - 0:30` | Audio: «Con tus metas por hora y por kilómetro.» | Remotion / Visual: [Zoom (scale 1 → 1.4) a la fila de fichas: $/h y $/km en verde con borde; texto «Números de ejemplo» en esquina]
- `0:30 - 0:33` | Audio: «Comenta CHECA y te paso el acceso a la prueba.» | Remotion / Visual: [Pantalla final estándar: logo semáforo animado, @checa.driver, @Eduardoalv9802 y link; palabra «CHECA» con spring scale en hueso]

## CTA final
«Comenta CHECA y te paso el acceso a la prueba.» (Una sola acción; la app aún está en prueba cerrada de Play. Confirmar con Eduardo si se usa el link de `/registro/` mientras siga abierto, hasta el 15 nov 2026.)

## Notas
- Marca v2: asfalto/hueso; verde/amarillo/rojo solo para decidir. Bricolage para números, DM Sans texto, JetBrains Mono datos.
- Sin ciudad ni país. Sin logos de Uber/DiDi (solo el texto en la pastilla, como el pop-up real).
- $/h netos de la toma 0:21 = neta / min × 60 (A: 67.40/30·60 = $135; B: 67.60/15·60 = $270). Ajustar el texto si se redondea distinto.
