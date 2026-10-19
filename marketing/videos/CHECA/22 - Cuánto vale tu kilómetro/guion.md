# Guion · 22 - Cuánto vale tu kilómetro (CHECA, v1)

**Prueba del domingo 19 oct 2026** (primera corrida del proceso semanal). Tipo: venta. Duración objetivo ~40 s con cierre estándar. Estado: **guion para aprobación de Eduardo** (siguiente etapa: tabla de animaciones; después portada, video y copy).
v1: `marketing/videos/CHECA/22 - Cuánto vale tu kilómetro/` · v2: `marketing/videos/PERSONAL/22 - Cuánto vale tu kilómetro/`

## Por qué este tema (datos, ver `marketing/metricas/videos.csv` y `marketing/plan/propuesta-semana-19-oct.md`)
- Los videos con pesos y comparación retienen más (43–47 % del video visto) que los consejos generales (17–23 %).
- Búsquedas reales en TikTok: «cuánto vale el kilómetro Uber o DiDi», «app para calcular viajes DiDi», «cómo saber tus ganancias de DiDi». Es un tema con demanda de búsqueda y lo resuelve Checa (pago por km).
- No repite al 12 (dos ofertas de $100: 14 km contra 8 km) ni al 06 (gasolina): aquí se entrega un método de tres pasos y un «mínimo por km».

## Hipótesis y cómo se mide
- **Hipótesis:** abrir con cifra + dar un método de 3 pasos + giro («más dinero, peor viaje») mantiene el ≥ 40 % visto y ≥ 10 % completo.
- **Prueba extra:** cierre «Comenta CHECA y te paso el link» (en lugar de «link del perfil»). Se mide con comentarios, mensajes y descargas del UTM `cuanto-vale-tu-kilometro` (`dm.txt`).
- Medir a los 3–4 días en TikTok Studio y anotar en `videos.csv` (con hora de publicación).

## Hook inicial (0–3 s)
Voz + texto en pantalla + imagen a la vez: «$90 por 15 km» con la oferta genérica y un «?» (números de ejemplo). La cifra va desde el primer cuadro (la portada es el primer cuadro).

## Hooks intermedios (uno cada ~4–5 s)
Micro-hook «un número que no da tiempo de calcular» → pasos 1-2-3 con cuenta visible → giro de la segunda oferta (más dinero, peor viaje) → payoff con colores → cierre que enlaza con el inicio.

## Guion optimizado

| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:04 | Te cae una oferta de $90 pesos por 15 kilómetros. | Oferta $90 · 15 km, «?» grande; impact-bass | [Hook Inicial] |
| 2 | 0:04–0:08 | Para saber si conviene, necesitas un número: cuánto vale tu kilómetro. | «¿$ por km?» se enciende; ping | [Open Loop] |
| 3 | 0:08–0:11 | Uno: suma los kilómetros de recoger y del viaje. | 4 km + 11 km = 15 km | [Micro-hook] |
| 4 | 0:11–0:14 | Dos: divide la tarifa entre esos kilómetros. | $90 ÷ 15 km | [Re-enganche visual] |
| 5 | 0:14–0:19 | Tres: compáralo con tu mínimo: gasolina más lo que quieres ganar. Digamos $5. | Mínimo: $2 + $3 = $5/km | [Cambio de ritmo] |
| 6 | 0:19–0:22 | $90 entre 15 kilómetros son $6 por kilómetro: pasa. | $6/km en verde sobre la raya de $5 | [Payoff del Open Loop] |
| 7 | 0:22–0:28 | Ahora cae otra de $110, más dinero... pero son 28 kilómetros: $3.90 por kilómetro. No pasa. | $110 grande, pausa, 28 km, $3.90/km en rojo; whoosh + error | [Giro] [Pausa dramática] |
| 8 | 0:28–0:32 | La app Checa hace esta cuenta por ti, antes de que aceptes. | Pop-up de Checa con $/km y color | [Giro emocional] |
| 9 | 0:32–0:36 | Comenta CHECA y te paso el link para probarla 7 días gratis. | Texto «Comenta CHECA» | [CTA específico] |
| 10 | 0:36–0:40 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar: logo, @checa.driver, @Eduardoalv9802 y link | [Loop de repetición] |

## Voz limpia (sin etiquetas, para ElevenLabs)
Te cae una oferta de $90 pesos por 15 kilómetros. Para saber si conviene, necesitas un número: cuánto vale tu kilómetro. Uno: suma los kilómetros de recoger y del viaje. Dos: divide la tarifa entre esos kilómetros. Tres: compáralo con tu mínimo: gasolina más lo que quieres ganar. Digamos $5. $90 entre 15 kilómetros son $6 por kilómetro: pasa. Ahora cae otra de $110, más dinero... pero son 28 kilómetros: $3.90 por kilómetro. No pasa. La app Checa hace esta cuenta por ti, antes de que aceptes. Comenta CHECA y te paso el link para probarla 7 días gratis. Descarga Checa y checa cuánto te queda de cada viaje.

## Ajustes de honestidad
Cifras de ejemplo con «números de ejemplo» en pantalla ($2 de gasolina por km es el mismo ejemplo del video 06). El mínimo lo pone cada conductor. Checa solo lee la pantalla; el botón lo toca el conductor. «7 días gratis» por la beta. No se promete ganancia.

## Texto de publicación
¿Cuánto vale tu kilómetro en Uber o DiDi? 🛣️
Cómo saber si una oferta conviene: divide la tarifa entre los kilómetros (recoger + viaje) y compárala con tu mínimo. Números de ejemplo.
¿Cuál es tu mínimo por kilómetro? 👇
Comenta CHECA y te paso el link para probar Checa 7 días gratis (solo Android).
#choferuber #choferdidi #conductores #uberdidi #gananciasuber

## Pendiente
Tabla de animaciones por toma (etapa 2), portada con «$90 por 15 km», video y copy, cada etapa con la aprobación de Eduardo. Crear `copy.txt` y `dm.txt` (UTM `cuanto-vale-tu-kilometro`) en la etapa de copy.
