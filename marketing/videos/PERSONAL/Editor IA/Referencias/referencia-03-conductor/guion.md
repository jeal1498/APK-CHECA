# Referencia 03 · Conductor a conductor (PERSONAL)

Guion: Hooks Virales `000 Personal/001` («¿Y si te dijera que estás perdiendo dinero cada vez que aceptas un viaje?»), elegido por Eduardo el 24 nov 2026.
Composición `EA-Referencia-03` (`video/src/EA/Referencia03.tsx`). 26 s, 1080×1920, 30 fps.
**Tiempos estimados (~2.8 palabras/s), sin voz de Eduardo.** Con su voz: Whisper → `palabras.json` → cambiar solo los fotogramas de `P` (uno por palabra clave; todo cuelga de ahí).

## Voz (sin R fuerte)
¿Y si te dijera que estás perdiendo dinero cada vez que aceptas un viaje? Puede que estés dejando dinero sin darte cuenta. Cuando te llega una oferta, no te fijes solo en cuánto paga. Ten en cuenta los kilómetros, el tiempo y la gasolina que vas a gastar. Un pago alto no siempre significa que te va a quedar más dinero. ¿Qué dato tomas en cuenta antes de aceptar?

## Cuentas (ofertas de ejemplo, rotuladas «Oferta · ejemplo»)
Gasolina = km totales ÷ 12 km/l × $24/l.
- A: $100, 15 km, 30 min → 15 ÷ 12 × 24 = **$30** → te quedan **$70**.
- B: $85, 5 km, 15 min → 5 ÷ 12 × 24 = **$10** → te quedan **$75**.

## Estilo
**AB** (25 nov 2026): marfil, titular grande con la cifra en naranja, objetos de una sola línea que se dibujan y barra que reparte el monto. Ver `Identidad/IDENTIDAD.md` §4.

## Elemento que persiste
La barra del viaje ($100, 960 px): pierde gotas → se llena con la gasolina (30 %) → sube y se compara con la de $85 (816 px = 85 %) → da paso al globo de comentario dibujado en línea.

## Acción visual por escena
| Seg. | Acción literal | Texto en pantalla (≤4) | Palabra del pico | Fotograma final |
|---|---|---|---|---|
| 0–4.6 | «UN VIAJE DE $100» cuenta de 0 a 100; se dibuja la barra; una gota naranja se escapa por abajo | «Un viaje de $100» | perdiendo (f60) | titular + barra vacía |
| 4.6–11.6 | Otra gota se escapa; rótulo «Oferta · ejemplo»; se subraya el $100 | «Oferta · ejemplo» | dejando (f165), oferta (f265), paga (f340) | $100 subrayado |
| 11.6–16.6 | Se dibujan en línea un camino, un reloj y una bomba; la gota cae de la manguera a la barra y llena el 30 % | «15 km», «30 min», «Gasolina» | kilómetros (f395), tiempo (f415), gasolina (f455) | barra $30 / $70 con su cuenta |
| 16.6–21.2 | Los iconos salen; la barra sube; entra la de $85 (más corta) con $10 de gasolina; se subraya $75 | «$100 o $85» | alto (f525), más dinero (f610) | dos barras, $75 subrayado |
| 21.2–26 | Las barras salen; se dibuja un globo de comentario en línea; sello EA. | «¿Qué dato tomas?» | dato (f660), aceptar (f715) | globo + EA. quietos |

Excepción a las 4 palabras: la cuenta de gasolina va en pequeño (regla de mostrar la cuenta).

## Reglas de movimiento aplicadas
- Una animación grande a la vez; 10–24 fotogramas con `bezier(.4,0,.2,1)`.
- Sin `blur` ni sombras; trazos de 6 px y bordes de 5 px; fondo liso (sin cuadrícula) para que no se ensucie al comprimir.
- Los textos nunca se enciman: uno sale antes de que entre el otro.
