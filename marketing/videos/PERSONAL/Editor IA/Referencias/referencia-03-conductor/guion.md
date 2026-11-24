# Referencia 03 · Conductor a conductor (PERSONAL)

Guion: Hooks Virales `000 Personal/001` («¿Y si te dijera que estás perdiendo dinero cada vez que aceptas un viaje?»), elegido por Eduardo el 24 nov 2026.
Composición `EA-Referencia-03` (`video/src/EA/Referencia03.tsx`; componente nuevo `components/OfertaCard.tsx`). 26 s, 1080×1920, 30 fps.
**Tiempos estimados (~2.8 palabras/s), sin voz de Eduardo.** Con su voz: Whisper → `palabras.json` → cambiar solo los fotogramas de `P` (uno por palabra clave; todo cuelga de ahí).

## Voz (sin R fuerte)
¿Y si te dijera que estás perdiendo dinero cada vez que aceptas un viaje? Puede que estés dejando dinero sin darte cuenta. Cuando te llega una oferta, no te fijes solo en cuánto paga. Ten en cuenta los kilómetros, el tiempo y la gasolina que vas a gastar. Un pago alto no siempre significa que te va a quedar más dinero. ¿Qué dato tomas en cuenta antes de aceptar?

## Cuentas (ofertas de ejemplo, rotuladas «Oferta · ejemplo»)
Gasolina = km totales ÷ 12 km/l × $24/l.
- A: $100, 15 km, 30 min → 15 ÷ 12 × 24 = **$30** → te quedan **$70**.
- B: $85, 5 km, 15 min → 5 ÷ 12 × 24 = **$10** → te quedan **$75**.

## Elemento que persiste
Tarjeta $100 → oferta con sus datos → se encoge al lado de otra → se vuelve globo de comentario.

## Acción visual por escena
| Seg. | Acción literal | Texto en pantalla (≤4) | Palabra del pico | Fotograma final |
|---|---|---|---|---|
| 0–4.6 | Entra la tarjeta $100; aparece «?» naranja | «$100?» | perdiendo (f60) | tarjeta $100? |
| 4.6–11.6 | Se quita el «?», rótulo «Oferta · ejemplo»; círculo naranja al monto | «Oferta · ejemplo», «$100» | oferta (f265), paga (f340) | $100 encerrado |
| 11.6–16.6 | La tarjeta crece y salen los datos uno por uno | «15 km», «30 min», «Gasolina −$30» | kilómetros (f395), tiempo (f415), gasolina (f455) | oferta completa con su cuenta |
| 16.6–21.2 | Se encoge a la izquierda; entra la de $85; los montos pasan a «Te queda» y $75 se subraya | «Te queda», «$70», «$75» | alto (f525), más dinero (f610) | dos tarjetas, $75 subrayado |
| 21.2–26 | Sale la segunda; la primera vuelve al centro y se vuelve globo; sello EA. | «¿Qué dato tomas?» | dato (f660), aceptar (f715) | globo + EA. quietos |

Excepción a las 4 palabras: la cuenta de gasolina va en pequeño (regla de mostrar la cuenta).

## Reglas de movimiento aplicadas
- Una animación grande a la vez; 12–24 fotogramas con `bezier(.4,0,.2,1)`.
- Sin `blur` ni sombras; borde de 5 px (al 72 % sigue en 3.6 px); la tarjeta crece por ancho y alto, no por `scale`.
- Los textos de la tarjeta nunca se enciman: uno sale antes de que entre el otro.
