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
**La barra del viaje** (negra = tu dinero, 960 px = $100). Gotea, se enfoca como oferta, la gasolina la pinta de naranja, se compara con la de $85, suelta su parte naranja y da paso al globo. El ✓ de «aceptar» abre y cierra.

## Diseño bloque por bloque (animador)
| # | Seg. | Voz | Acción clave (literal) | Texto (≤4) | Golpe en | Cierre limpio |
|---|---|---|---|---|---|---|
| 1 | 0–2.6 | ¿Y si te dijera que estás perdiendo dinero | Se dibuja la barra negra «$100»; por abajo se escapa una gota naranja | $100 | perdiendo (f60) | barra + primera gota en el piso |
| 2 | 2.6–4.6 | cada vez que aceptas un viaje? | Un ✓ se sella arriba a la derecha; al sellar, cae otra gota | Aceptar | viaje (f132) | barra, ✓ y charco chico |
| 3 | 4.6–7.4 | Puede que estés dejando dinero sin darte cuenta | Siguen las gotas; el charco crece con cada una (escala); un ojo dibujado se cierra | — | dejando (f165), cuenta (f210) | ojo cerrado, charco grande |
| 4 | 7.4–9.2 | Cuando te llega una oferta | Ojo, ✓ y charco se apagan; la barra baja al centro de atención | Oferta · ejemplo | oferta (f265) | barra sola, rótulo |
| 5 | 9.2–11.6 | no te fijes solo en cuánto paga | Un círculo naranja encierra solo el $100 | (el $100) | paga (f340) | $100 encerrado |
| 6 | 11.6–13.4 | Ten en cuenta los kilómetros | Se dibuja un camino; su línea central avanza | 15 km | kilómetros (f395) | camino + barra |
| 7 | 13.4–14.2 | el tiempo | Se dibuja un reloj y su manecilla avanza | 30 min | tiempo (f415) | camino, reloj, barra |
| 8 | 14.2–16.6 | y la gasolina que vas a gastar | Se dibuja la bomba; la gota cae a la barra y pinta de naranja el 30 % | Gasolina | gasolina (f455) | barra $30 / $70 con su cuenta |
| 9 | 16.6–18.8 | Un pago alto no siempre significa | Los iconos salen; la barra sube; debajo crece la de $85, más corta (85 %) | $100, $85 | alto (f525) | dos barras con su tramo naranja |
| 10 | 18.8–21.2 | que te va a quedar más dinero | Los tramos naranjas caen; lo negro se alinea a la izquierda: $75 sale más largo que $70 | $70, $75 | más dinero (f610) | $75 subrayado, línea de corte |
| 11 | 21.2–23 | ¿Qué dato tomas en cuenta | Las barras se van; se dibuja un globo de comentario | ¿Qué dato tomas? | dato (f660) | globo con la pregunta |
| 12 | 23–26 | antes de aceptar? | Vuelve el ✓ del bloque 2, ahora naranja, sobre el globo; sello EA. | (igual) | aceptar (f715) | globo + ✓ + EA. |

Excepción a las 4 palabras: la cuenta de gasolina va en pequeño (regla de mostrar la cuenta).

## Reglas de movimiento aplicadas
- Una animación grande a la vez; 10–24 fotogramas con `bezier(.4,0,.2,1)`.
- Sin `blur` ni sombras; trazos de 6 px y bordes de 5 px; fondo liso (sin cuadrícula) para que no se ensucie al comprimir.
- Los textos nunca se enciman: uno sale antes de que entre el otro.
