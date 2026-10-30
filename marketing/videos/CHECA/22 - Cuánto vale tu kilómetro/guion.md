# Guion · 22 - Cuánto vale tu kilómetro (CHECA v1) — reescrito el 30 oct 2026 con la skill `guion-remotion-checa`

**Estado:** guion por aprobar. Antes: `guion_19oct_anterior.md` (cierre «7 días gratis» y «Descarga Checa», ya no vigentes). Misma idea (cuenta que se revela, giro «más dinero, peor viaje»), con la neta del código y el CTA vigente.

Concepto / Problema detectado en el repo:
El conductor compara tarifas, no kilómetros. `Parser.kt`: `porKm` = tarifa / km **totales** (recogida + viaje); `gasolina()` = km / km por litro × $/litro; `neta()` = tarifa − gasolina; `nivel()` pinta el semáforo con la meta por km. La oferta más cara puede ser la peor.

## Números de ejemplo (12 km/l, $24/l, meta $5/km)
- **A:** tarifa $110, 28 km totales → $/km 110/28 = **$3.93** (bajo la meta). Gasolina 28/12×24 = **$56** → **$54 netos**.
- **B:** tarifa $90, 15 km totales → $/km = **$6.00** (cumple). Gasolina 15/12×24 = **$30** → **$60 netos**.
- La de $90 deja $6 más, aunque pague $20 menos.

## Guion de voz (limpio, ~30 s, Fernando v3)
$110 pagan más que $90, y son el peor viaje de los dos. Los $110 son 28 kilómetros: a $3.93 cada uno, y te quedan $54. Los $90 son 15 kilómetros: a $6 cada uno, y te quedan $60. Con una meta de $5 por kilómetro, uno te sirve y el otro te sale caro. Y en segundos, nadie saca esa cuenta. Checa la hace por ti, con la recogida incluida, antes de que aceptes. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «$110 pagan más que $90, y son el peor viaje de los dos.» | Remotion / Visual: [Flash en el cuadro 0; dos tarjetas de oferta (OFERTA) se estrellan con zoom, $110 grande a la izquierda y $90 a la derecha; cifras suben con interpolate; «Números de ejemplo»] `[Hook]`
- `0:03 - 0:06` | Audio: «Los $110 son 28 kilómetros: a $3.93 cada uno, y te quedan $54.» | Remotion / Visual: [Cámara a la de $110; $110 ÷ 28 km = $3.93/km en rojo que baja; ticket Gasolina −$56 → $54] `[Giro]`
- `0:06 - 0:09` | Audio: «Los $90 son 15 kilómetros: a $6 cada uno, y te quedan $60.» | Remotion / Visual: [Cámara a la de $90; $90 ÷ 15 km = $6/km en verde; ticket −$30 → $60] `[Re-enganche visual]`
- `0:09 - 0:12` | Audio: «Con una meta de $5 por kilómetro, uno te sirve y el otro te sale caro.» | Remotion / Visual: [Raya de meta «$5/km» (strokeDashoffset); el $6 sube por encima en verde y el $3.93 queda abajo en rojo] `[Payoff]`
- `0:12 - 0:15` | Audio: «Y en segundos, nadie saca esa cuenta.» | Remotion / Visual: [Reloj de 15 s que corre sobre la oferta; los números se mezclan y tiemblan] `[Cambio de ritmo]`
- `0:15 - 0:18` | Audio: «Checa la hace por ti, con la recogida incluida, antes de que aceptes.» | Remotion / Visual: [Pop-up «A · Franja» cae sobre cada oferta: RECHAZAR con $3.9/km en rojo y ACEPTAR con $6/km en verde; ficha «16 km total»] `[Giro emocional]`
- `0:18 - 0:21` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA»] `[CTA]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
- Sin ciudad ni país, sin logos ni nombres de apps, «Números de ejemplo», sin promesa de ganancia; la meta de $5/km es ejemplo (cada conductor pone la suya). Duración real ~20–24 s (el borrador de 34 s dejaba pausas); si Eduardo la quiere más larga, se agrega un bloque con la comparación de $/hora.
- La v2 de PERSONAL se hace después, con otro gancho (p. ej. «Te ofrecen $110 y te quedan $54»).
