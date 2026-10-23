# Guion · 14 - Miedo a rechazar (CHECA)

## Tema 7 · Miedo a rechazar (calificación / tasa de aceptación)
Video: `marketing/videos/CHECA/14 - Miedo a rechazar/video.mp4` (`Rechazar1Volt.tsx`) · en la voz, «La app solo lee la pantalla» quedó como «La app Checa solo lee la pantalla» (para no confundir con Uber/DiDi); en pantalla, «números de ejemplo»

Te da miedo rechazar viajes [Corte] porque sientes que la app te va a castigar. [Corte] Así que terminas aceptando de todo, [Corte] hasta los viajes que te hacen perder dinero. [Corte] Cada plataforma tiene sus reglas y hay que saber jugarlas... [Corte] pero si vas a rechazar, que sea el viaje que de verdad no conviene. [Corte] Checa esto: $70 pesos por 13 kilómetros solo te dejan $44 limpios. [Corte] En cambio, $120 pesos por 12 kilómetros te dejan $96 en la bolsa. [Corte] Checa te dice cuál es cuál, y la decisión siempre es tuya. [Corte] La app solo lee la pantalla, [Corte] el botón siempre lo tocas tú. [Corte] Pide tus 7 días de prueba sin costo en el link del perfil.

| Corte | Tiempo | Voz | En pantalla |
|---|---|---|---|
| 1 | 0:00–0:04 | Te da miedo rechazar viajes porque sientes que la app te va a castigar. | Dedo dudando entre «Aceptar» y «Rechazar» |
| 2 | 0:04–0:08 | Así que terminas aceptando de todo, hasta los viajes que te hacen perder dinero. | Lista de viajes con varios en rojo |
| 3 | 0:08–0:11 | Cada plataforma tiene sus reglas y hay que saber jugarlas... | Libreta/reglamento abriéndose |
| 4 | 0:11–0:14 | ...pero si vas a rechazar, que sea el viaje que de verdad no conviene. | Un viaje malo resaltado en rojo |
| 5 | 0:14–0:18 | Checa esto: $70 pesos por 13 kilómetros solo te dejan $44 limpios. | Pop-up rojo RECHAZAR · $44 |
| 6 | 0:18–0:21 | En cambio, $120 pesos por 12 kilómetros te dejan $96 en la bolsa. | Pop-up verde ACEPTAR · $96 |
| 7 | 0:21–0:25 | Checa te dice cuál es cuál, y la decisión siempre es tuya. | Los dos pop-ups y una mano eligiendo |
| 8 | 0:25–0:28 | La app solo lee la pantalla, el botón siempre lo tocas tú. | Ojo escaneando la pantalla, dedo tocando |
| 9 | 0:28–0:31 | Pide tus 7 días de prueba sin costo en el link del perfil. | Logo + 7 días de prueba sin costo |



## Guion revisado (23 oct 2026, para rehacer el video antes de publicarlo; estado: guion para aprobar)
Cambios: cifras con «pesos» (sin «$», la voz decía «dólares»), CTA de comentarios (regla del 23 oct) y cierre estándar en lugar de «link del perfil». Mismos datos ($70/13 km → $44; $120/12 km → $96; gasolina $2/km).

### Voz limpia
Te da miedo rechazar viajes porque sientes que la app te va a castigar. Así que terminas aceptando de todo, hasta lo que te hace perder dinero. Cada plataforma tiene sus reglas, pero si vas a rechazar, que sea el viaje que de verdad no conviene. Mira: 70 pesos por 13 kilómetros solo te dejan 44 pesos limpios. En cambio, 120 pesos por 12 kilómetros te dejan 96 en la bolsa. La app Checa te marca cuál es cuál, y el botón siempre lo tocas tú. Dime en los comentarios: ¿rechazas los viajes que no convienen o aceptas de todo? Descarga Checa y checa cuánto te queda de cada viaje.

## Guion v1 APROBADO por Eduardo (23 oct 2026) — sustituye al «Guion revisado» de arriba
Cambio: hook más simple y cercano (el anterior no generaba intriga). Mismos datos ($70/13 km → $44; $120/12 km → $96).

### Voz limpia
¿Te ha pasado que aceptas un viaje y a media ruta ya te arrepentiste? Pasa por el miedo a rechazar. Un viaje de 70 pesos por 13 kilómetros, ya con la gasolina, solo te deja 44. Otro de 120 pesos por 12 kilómetros te deja 96. Más del doble, casi con los mismos kilómetros. La app Checa te marca cuál es cuál antes de que toques, y el botón siempre lo tocas tú. Dime en los comentarios: ¿rechazas los viajes que no convienen o aceptas de todo? Descarga Checa y checa cuánto te queda de cada viaje.

## Tabla de animaciones APROBADA por Eduardo (23 oct 2026)
| # | Voz | En pantalla |
|---|---|---|
| 1 | ¿Te ha pasado que aceptas un viaje y a media ruta ya te arrepentiste? | Mapa con ruta en curso; aparece «¿ARREPENTIDO?» (escena nueva) |
| 2 | Pasa por el miedo a rechazar. | Dedo temblando entre «Aceptar» y «Rechazar» |
| 3 | Un viaje de 70 pesos por 13 kilómetros, ya con la gasolina, solo te deja 44. | Pop-up rojo: 70 − gasolina = 44 netos. «Números de ejemplo» |
| 4 | Otro de 120 pesos por 12 kilómetros te deja 96. | Pop-up verde del mismo tamaño: 120 − gasolina = 96 |
| 5 | Más del doble, casi con los mismos kilómetros. | Los dos netos lado a lado; 96 crece sobre 44 con «×2» |
| 6 | La app Checa te marca cuál es cuál antes de que toques, y el botón siempre lo tocas tú. | `dospopups` / `control` |
| 7 | Dime en los comentarios: ¿rechazas los viajes que no convienen o aceptas de todo? | `comentarios` |
| 8 | Descarga Checa y checa cuánto te queda de cada viaje. | `cierre` (sin «Sígueme») |
