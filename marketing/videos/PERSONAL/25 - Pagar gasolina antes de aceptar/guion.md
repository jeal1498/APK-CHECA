# Guion · 25 - Pagar gasolina antes de aceptar (PERSONAL, borrador Remotion)

**Concepto / Problema detectado en el repo:** la oferta de Uber/DiDi muestra tarifa, minutos y km, pero no descuenta gasolina ni cuenta la recogida. Checa lee la pantalla (AccessibilityService + OCR) y calcula neta, $/h, $/km y semáforo antes de aceptar. Fricción: decidir en segundos con números incompletos.

**Notas:** cifras de ejemplo ($60 → $38, gasolina −$22). Cambiar por un viaje real antes de grabar. Oferta de 3 meses gratis confirmada por Eduardo.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «Aceptaste un viaje de 60 pesos. Te dejó 38.» | Remotion / Visual: [Componente RideCard con spring entry desde abajo; contador de 0 a 60; a 2.5 s el 38 entra en rojo con flash]
- `0:03 - 0:06` | Audio: «La app te dice la tarifa. No te dice cuánto se va en gasolina.» | Remotion / Visual: [Interpolación de texto palabra por palabra en «NO TE DICE»; lista: Tarifa $60 y Gasolina −$22 aparece a 0:05]
- `0:06 - 0:09` | Audio: «Minutos y kilómetros, pero sin la recogida.» | Remotion / Visual: [Barra de tramos; el segmento de recogida parpadea en gris con interpolación de opacidad]
- `0:09 - 0:12` | Audio: «Por eso hice Checa: lee la oferta en pantalla antes de que aceptes.» | Remotion / Visual: [Pantalla dividida con dos apps; línea de escaneo horizontal interpolada de 0 a 100 %; logo Checa con spring]
- `0:12 - 0:15` | Audio: «Resta la gasolina y te dice cuánto ganas por hora y por kilómetro.» | Remotion / Visual: [Fichas $/h y $/km con contador animado; la gasolina se resta con interpolación de ancho]
- `0:15 - 0:18` | Audio: «Y te pone el semáforo: verde, amarillo o rojo.» | Remotion / Visual: [Semáforo con spring scale 0.8 → 1.1 → 1; activa solo el color que corresponde]
- `0:18 - 0:21` | Audio: «Ese de 60 sale en rojo. Lo decides en segundos, no de cabeza.» | Remotion / Visual: [Pop-up con franja roja, slide-in lateral con spring; texto «de cabeza» tachado con interpolación]
- `0:21 - 0:24` | Audio: «Busco 50 conductores que la usen gratis tres meses.» | Remotion / Visual: [Contador de 0 a 50; badge «3 meses gratis» entra con stagger de 100 ms]
- `0:24 - 0:27` | Audio: «Comenta YO aquí abajo y te paso los accesos.» | Remotion / Visual: [Botón pill «COMENTA YO» con pulse en loop; flecha SVG apuntando abajo con interpolación de opacidad]

**CTA final:** Comenta YO. Un solo paso: la palabra en el comentario y los accesos por mensaje.
