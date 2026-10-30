# Guion · 22 - Cuánto vale tu kilómetro (PERSONAL v2) — 30 oct 2026, con la skill `guion-remotion-checa`

**Estado:** guion aprobado el 30 oct 2026; tabla de animaciones por aprobar. Versión anterior: `guion_anterior.md`. Misma idea y cifras que `CHECA/22`; otro gancho (arranca con lo que te queda, no con la comparación), otras palabras y otro orden (primero la de 110, luego la de 90). Montos sin moneda en la voz.

Concepto / Problema detectado en el repo:
El conductor compara tarifas, no kilómetros. `Parser.kt`: `porKm` = tarifa / km **totales** (recogida + viaje); `gasolina()` = km / km por litro × $/litro; `neta()` = tarifa − gasolina; `nivel()` pinta el semáforo con la meta por km. La oferta que más paga puede ser la peor.

## Números de ejemplo (12 km/l, $24/l, meta 5/km)
- **A:** tarifa 110, 28 km totales → 110/28 = **3.93/km**; gasolina 28/12×24 = **56** → **54 netos**.
- **B:** tarifa 90, 15 km totales → **6.00/km**; gasolina 15/12×24 = **30** → **60 netos**.

## Guion de voz (limpio, ~28 s, Fernando v3)
Te ofrecen 110 y te quedan 54. Son 28 kilómetros, a 3.93 por kilómetro. La otra oferta paga 90, pero son 15 kilómetros, a 6 por kilómetro, y te quedan 60. Si tu kilómetro tiene que valer 5, la de 110 es la que sale cara. Y en segundos, nadie saca esa cuenta. Checa la hace por ti, antes de que aceptes. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «Te ofrecen 110 y te quedan 54.» | Remotion / Visual: [Flash en el cuadro 0; tarjeta de oferta (OFERTA) se estrella con zoom grande; contador 110 → 54 con interpolate + Math.round; cámara sacude la cifra; sello «¿$?»; «Números de ejemplo»] `[Hook]`
- `0:03 - 0:06` | Audio: «Son 28 kilómetros, a 3.93 por kilómetro.» | Remotion / Visual: [Cuenta 110 ÷ 28 km = 3.93/km que se revela en rojo; ticket Gasolina −56] `[Giro]`
- `0:06 - 0:09` | Audio: «La otra oferta paga 90, pero son 15 kilómetros, a 6 por kilómetro, y te quedan 60.» | Remotion / Visual: [Segunda tarjeta entra con spring, la primera se reduce con scale + translateX; 90 ÷ 15 = 6/km en verde; contador 90 → 60] `[Re-enganche visual]`
- `0:09 - 0:12` | Audio: «Si tu kilómetro tiene que valer 5, la de 110 es la que sale cara.» | Remotion / Visual: [Raya de meta «5/km» que se dibuja; barra de 6 sube sobre ella en verde, la de 3.93 queda abajo en rojo] `[Payoff]`
- `0:12 - 0:15` | Audio: «Y en segundos, nadie saca esa cuenta.» | Remotion / Visual: [Reloj que corre 15 → 0 sobre las dos cuentas con «?»; los números tiemblan] `[Cambio de ritmo]`
- `0:15 - 0:18` | Audio: «Checa la hace por ti, antes de que aceptes.» | Remotion / Visual: [Dos pop-ups «A · Franja»: RECHAZAR (54 netos) y ACEPTAR ★ (60 netos)] `[Giro emocional]`
- `0:18 - 0:21` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA»] `[CTA]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
- Marca v2, pop-ups con «OFERTA», sin ciudad ni país, sin promesa de ganancia; la meta de 5/km es ejemplo. Voz v3 sin `[pause]`. Plantilla: `video/hyperframes/cuanto-vale-km-22/`.

## Tabla de animaciones por toma (30 oct 2026, por aprobar)
| Toma | Frase | Componente | Animación | Objetivo |
|---|---|---|---|---|
| 0–3 s | «Te ofrecen 110 y te quedan 54.» | Tarjeta de oferta «110» + sello «¿$?» + «Números de ejemplo» | Flash en el cuadro 0; la tarjeta se estrella con zoom; contador 110 → 54; cámara sacude la cifra | Golpe: lo que te queda |
| 3–6 s | «Son 28 kilómetros, a 3.93 por kilómetro.» | Cuenta 110 ÷ 28 km + ticket Gasolina −56 | La cifra 3.93/km se revela en rojo; el ticket cae | Por qué queda poco |
| 6–9 s | «La otra oferta paga 90, pero son 15 kilómetros…» | Segunda tarjeta + cuenta 90 ÷ 15 = 6/km | La 2ª entra con spring y la 1ª se reduce; 6/km en verde; contador 90 → 60 | Contraste |
| 9–12 s | «Si tu kilómetro tiene que valer 5…» | Raya de meta «5/km» + dos barras | La raya se dibuja; la barra de 6 sube sobre ella y la de 3.93 queda abajo | Payoff |
| 12–15 s | «Y en segundos, nadie saca esa cuenta.» | Reloj + dos cuentas con «?» | El reloj corre y los números tiemblan | Cambio de ritmo |
| 15–18 s | «Checa la hace por ti, antes de que aceptes.» | Dos pop-ups «A · Franja»: RECHAZAR 54 y ACEPTAR ★ 60 | Caen con spring uno tras otro | Checa como solución |
| 18–21 s | «Comenta CHECA y te paso el acceso…» | Pantalla final estándar sin link | Logo rojo → ámbar → verde → ✓; pastilla «Comenta CHECA» | Una sola acción |

Marca v2, pop-ups con «OFERTA», subtítulos palabra por palabra, efectos oficiales, sin música, todo por encima de y≈1150.
