# Guion · 22 - Cuánto vale tu kilómetro (CHECA, v1, **reescrito el 19 oct 2026**)

Prueba del proceso semanal. Tipo: venta. ~34 s con cierre estándar. Estado: **guion para aprobación de Eduardo** (siguiente etapa: tabla de animaciones; después portada, video y copy).
v1: `marketing/videos/CHECA/22 - Cuánto vale tu kilómetro/` · v2: `marketing/videos/PERSONAL/22 - Cuánto vale tu kilómetro/`

## Por qué este tema y por qué este formato (datos: `marketing/metricas/videos.csv`, `marketing/metricas/aprendizajes.md`)
- Búsquedas reales de la audiencia: «cuánto vale el kilómetro Uber o DiDi», «app para calcular viajes DiDi», «cómo saber tus ganancias de DiDi».
- **Primera versión descartada:** era un método de «Uno, dos, tres», el formato de lista que retiene peor (los «4–5 consejos» de CHECA 03 y 05 dieron 17–23 % de video visto).
- **Esta versión copia lo que tienen los ganadores** (CHECA 04 «6 km» 43 %, CHECA 02 «12 horas» 30 %, «Lo que la app no te dice» 47 %): dolor con cifra en la primera frase, **un solo ejemplo con la cuenta que se va revelando**, giro («más dinero, peor viaje»), Checa entra como solución hacia el final, ~34 s y cero «N consejos».

## Hipótesis y cómo se mide
- **Hipótesis:** cifra en el primer cuadro + una cuenta que se revela (sin pasos) mantiene ≥ 40 % visto y ≥ 10 % completo.
- **Prueba extra:** cierre «Comenta CHECA y te paso el link». Se mide con comentarios, mensajes y descargas del UTM `cuanto-vale-tu-kilometro` (`dm.txt`).
- Medir a los 3–4 días en TikTok Studio; anotar la hora de publicación en `videos.csv`.

## Auditoría de la versión anterior (la de 3 pasos)
- Estructura de tutorial: tres pasos seguidos sin dolor ni sorpresa, justo donde la retención cae.
- Duraba ~40 s; los ganadores duran 31–36 s.
- El giro ($110 es peor) llegaba hasta el corte 7; ahora está en la primera frase.

## Guion optimizado

| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:04 | $110 pesos paga más que $90, y aun así es el peor viaje. | Dos ofertas lado a lado, $110 > $90, «?» rojo; impact-bass | [Hook Inicial] |
| 2 | 0:04–0:08 | Los $110 son 28 kilómetros: ni a $4 por kilómetro llegas. | $110 ÷ 28 km = $3.93/km, cifra roja que baja; whoosh | [Open Loop] [Giro] |
| 3 | 0:08–0:11 | Los $90 son 15 kilómetros: $6 por kilómetro. | $90 ÷ 15 km = $6/km en verde | [Re-enganche visual] |
| 4 | 0:11–0:16 | Si tu kilómetro tiene que valer $5, uno te sirve y el otro te sale caro. | Raya de $5/km: el $6 arriba, el $3.93 abajo | [Payoff del Open Loop] |
| 5 | 0:16–0:19 | Y en segundos, nadie saca esa cuenta. | Reloj de 15 s sobre la oferta | [Cambio de ritmo] |
| 6 | 0:19–0:25 | La app Checa la hace por ti, con la recogida incluida, antes de que aceptes. | Pop-up de Checa con $/km y color | [Giro emocional] |
| 7 | 0:25–0:29 | Comenta CHECA y te paso el link para probarla 7 días gratis. | Texto «Comenta CHECA» | [CTA específico] |
| 8 | 0:29–0:34 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar: logo, @checa.driver, @Eduardoalv9802 y link | [Loop de repetición] |

## Voz limpia (sin etiquetas, para ElevenLabs)
$110 pesos paga más que $90, y aun así es el peor viaje. Los $110 son 28 kilómetros: ni a $4 por kilómetro llegas. Los $90 son 15 kilómetros: $6 por kilómetro. Si tu kilómetro tiene que valer $5, uno te sirve y el otro te sale caro. Y en segundos, nadie saca esa cuenta. La app Checa la hace por ti, con la recogida incluida, antes de que aceptes. Comenta CHECA y te paso el link para probarla 7 días gratis. Descarga Checa y checa cuánto te queda de cada viaje.

## Ajustes de honestidad
«Números de ejemplo» en pantalla. Los $5 por km son un ejemplo: el mínimo lo pone cada conductor. $110 ÷ 28 = $3.93 (por eso «ni a $4»). Checa solo lee la pantalla; el botón lo toca el conductor. «7 días gratis» por la beta. No se promete ganancia.

## Texto de publicación
$110 paga más que $90, y es el peor viaje 💸
Así se ve cuánto vale tu kilómetro en Uber y DiDi: tarifa entre kilómetros, con la recogida incluida. Números de ejemplo.
¿Cuánto pides tú por kilómetro? 👇
Comenta CHECA y te paso el link para probar Checa 7 días gratis (solo Android).
#choferuber #choferdidi #conductores #uberdidi #gananciasuber

## Pendiente
Tabla de animaciones por toma (etapa 2), portada con «$110 > $90» (estilo de marca v2), video y copy, cada etapa con la aprobación de Eduardo. Crear `copy.txt` y `dm.txt` (UTM `cuanto-vale-tu-kilometro`) en la etapa de copy.
