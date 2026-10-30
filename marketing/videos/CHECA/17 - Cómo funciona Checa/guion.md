# Guion · 17 - Cómo funciona Checa (CHECA) — rehecho el 30 oct 2026 con la skill `guion-remotion-checa`

**Estado:** guion, tabla, portada y video aprobados el 30 oct 2026; falta el copy. Reemplaza al guion del 16 oct (cierre «pruébala 7 días sin costo», marca anterior); el video viejo sigue en `video.mp4` hasta que se apruebe el nuevo.

Concepto / Problema detectado en el repo:
El conductor decide en segundos viendo solo la tarifa bruta. Checa (`Parser.kt`, `Popup.kt`) calcula gasolina de recogida + viaje, neta, $/h y $/km sobre km y min **totales**, pinta el semáforo con sus metas y marca recogida lejana o la mejor de dos ofertas (`Comparador.kt`). Explicador: qué hace Checa, en orden, sin promesas de ganancia.

## Números de ejemplo (fórmulas del código; 12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km)
- **Verde:** tarifa $150 · recogida 3 km / 6 min · viaje 13 km / 22 min → 16 km, 28 min. Gasolina 16/12×24 = **$32** → **$118 netos**. $/km 150/16 = **$9.4** ✓, $/h 150/28×60 = **$321** ✓ → ACEPTAR.
- **Rojo (recogida):** viaje $120, gasolina $49 (24.5 km) → **$71 netos**, recogida 6 km > 4 → RECHAZAR.
- **Dos ofertas:** $118 netos (ACEPTAR, mejor) y $84 (viaje $112, gasolina $28 → CONSIDERAR).

## Guion de voz (limpio, ~33 s, voz Fernando)
Te cae un viaje de 150 pesos. ¿Cuánto te queda de verdad? Checa lee la oferta en cuanto aparece, y te muestra un aviso encima, antes de aceptar. Resta la gasolina de la recogida y del viaje: te quedan 118. Te dice cuánto ganas por hora y por kilómetro. Un semáforo decide: verde, acepta; amarillo, piénsalo; rojo, recházalo. Tú pones las metas que quieres ganar. Si la recogida queda lejos, te lo marca en rojo. Si caen dos ofertas, te dice cuál conviene más. También te enseña a qué horas te pagan mejor. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión desglosado
- `0:00 - 0:03` | Audio: «Te cae un viaje de 150 pesos. ¿Cuánto te queda de verdad?» | Remotion / Visual: [Gancho con movimiento desde el cuadro 0: tarjeta de oferta tipo app de viajes «$150.00» con spring desde abajo y leve zoom; sello amarillo «¿$?» que late; texto palabra por palabra] `[Hook Inicial]`
- `0:03 - 0:06` | Audio: «Checa lee la oferta en cuanto aparece.» | Remotion / Visual: [Teléfono con la oferta; línea de escaneo verde que baja con interpolate; logo semáforo ✓ en la esquina] `[Re-enganche visual]`
- `0:06 - 0:09` | Audio: «Y te muestra un aviso encima, antes de aceptar.» | Remotion / Visual: [Pop-up «A · Franja» entra con spring sobre la oferta, ambos visibles completos; «Números de ejemplo»] `[Payoff]`
- `0:09 - 0:12` | Audio: «Resta la gasolina de la recogida y del viaje: te quedan 118.» | Remotion / Visual: [Ticket Viaje $150 → Gasolina −$32 (cae en rojo) → contador $150 → $118 con interpolate + Math.round] `[Giro]`
- `0:12 - 0:15` | Audio: «Te dice cuánto ganas por hora y por kilómetro.» | Remotion / Visual: [Zoom 1 → 1.4 a las fichas $321/h y $9.4/km; entran con stagger de 4 frames, borde verde] `[Micro-hook]`
- `0:15 - 0:18` | Audio: «Un semáforo decide: verde, acepta; amarillo, piénsalo; rojo, recházalo.» | Remotion / Visual: [Tres pop-ups del mismo tamaño que se reemplazan: ACEPTAR → CONSIDERAR → RECHAZAR, cada uno en su palabra] `[Cambio de ritmo]`
- `0:18 - 0:21` | Audio: «Tú pones las metas que quieres ganar.» | Remotion / Visual: [Tarjetas «Meta por km $8» y «Meta por hora $150» con − / + animados; el valor cambia con interpolate] `[Control]`
- `0:21 - 0:24` | Audio: «Si la recogida queda lejos, te lo marca en rojo.» | Remotion / Visual: [Mapa line-art: ruta de recogida «6 km» que se dibuja (strokeDashoffset); ficha roja; pop-up RECHAZAR $71 netos] `[Giro]`
- `0:24 - 0:27` | Audio: «Si caen dos ofertas, te dice cuál conviene más.» | Remotion / Visual: [Layout split: dos pop-ups apilados; el de $118 con ★ MEJOR y el de $84 apagado «2ª»] `[Re-enganche visual]`
- `0:27 - 0:30` | Audio: «También te enseña a qué horas te pagan mejor.» | Remotion / Visual: [Cuadrícula de horas que se pinta con stagger; 17–19 h en verde con «lo mejor que viene»] `[Open Loop cerrado]`
- `0:30 - 0:33` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar: logo animado rojo → ámbar → verde → ✓, pastilla «Comenta CHECA», sin link (como el 26)] `[CTA específico]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.» (DM a `/registro/`, ya en `dm.txt`.)

## Notas
- Marca v2 en todo el video (asfalto/hueso, Bricolage, DM Sans, JetBrains Mono); sin «UBER»/«DiDi» en los pop-ups (pastilla genérica o sin app). Sin ciudad ni país; sin «7 días», precios ni promesas de ganancia.
- Hecho con Hyperframes (`checa-marca-v2`/`construir.py` del 26), voz v3 con 1–2 tags por línea como máximo, igual que el 26.
- Versión del 16 oct (legado): voz «¿Aceptas viajes sin saber si de verdad te conviene?…», cierre «pruébala 7 días sin costo».

## Tabla de animaciones por toma (30 oct 2026, por aprobar)
| Toma | Frase | Componente | Animación | Objetivo |
|---|---|---|---|---|
| 0–3 s | «Te cae un viaje de 150 pesos. ¿Cuánto te queda de verdad?» | Tarjeta de oferta tipo app de viajes («$150.00», «Aceptar») + sello amarillo «¿$?» | Entra desde abajo con zoom lento desde el cuadro 0; el sello late | Pérdida/duda en el primer segundo |
| 3–6 s | «Checa lee la oferta en cuanto aparece.» | Teléfono con la oferta + logo semáforo ✓ | Línea de escaneo verde que baja | Mostrar que lee sola |
| 6–9 s | «Y te muestra un aviso encima, antes de aceptar.» | Pop-up «A · Franja» sobre la oferta (ambos visibles) | Pop-up cae con spring; «Números de ejemplo» | El pop-up es el producto |
| 9–12 s | «Resta la gasolina… te quedan 118.» | Ticket Viaje $150 / Gasolina −$32 | Gasolina cae en rojo; contador $150 → $118 | La cifra que importa |
| 12–15 s | «…por hora y por kilómetro.» | Fichas $321/h y $9.4/km | Zoom 1 → 1.4, entran en escalera, borde verde | Datos en una mirada |
| 15–18 s | «Un semáforo decide…» | Tres pop-ups del mismo tamaño | ACEPTAR → CONSIDERAR → RECHAZAR en su palabra | Decisión sin pensar |
| 18–21 s | «Tú pones las metas…» | Tarjetas «Meta por km $8» y «Meta por hora $150» | Los valores suben con − / + | Control del conductor |
| 21–24 s | «Si la recogida queda lejos, te lo marca en rojo.» | Mapa con ruta «6 km» + pop-up RECHAZAR $71 | La ruta se dibuja; ficha roja se marca | Alerta concreta |
| 24–27 s | «Si caen dos ofertas…» | Dos pop-ups apilados ($118 ★ MEJOR, $84 apagado) | Entran escalonados | Comparador |
| 27–30 s | «…a qué horas te pagan mejor.» | Cuadrícula de horas | Se pinta en escalera; 17–19 h en verde | Mejores horas |
| 30–33 s | «Comenta CHECA y te paso el acceso…» | Pantalla final estándar sin link | Logo rojo → ámbar → verde → ✓; pastilla «Comenta CHECA» | Una sola acción |

Marca v2, sin «UBER»/«DiDi» en pop-ups, subtítulos v2 palabra por palabra, efectos de sonido oficiales, sin música.
