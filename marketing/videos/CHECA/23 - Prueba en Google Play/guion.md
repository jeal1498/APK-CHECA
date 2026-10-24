# Guion · 23 - Prueba en Google Play (CHECA, v1)

**Objetivo:** reclutar testers para la prueba cerrada de Google Play (hasta 50) y llevarlos al registro (`/registro/`) y a la comunidad de WhatsApp. Estado: guion para aprobar (24 oct 2026). v2: `marketing/videos/PERSONAL/23 - Prueba en Google Play/`.

## Auditoría previa (borrador, sin versión publicada)
- El tema es una oferta con fecha límite (50 lugares, registro hasta el 15 de noviembre o hasta llenarse): la urgencia es real, hay que usarla sin exagerar.
- Riesgo de arrancar con la app y no con el dolor del conductor: se abre con la pérdida (viaje que se ve bueno y no lo es), igual que el video 19.
- Riesgo de sonar a promesa de ganancias: solo se habla de saber cuánto te queda «ya sin gasolina», nunca de cuánto vas a ganar.
- Las cifras habladas (50, 40 %) llegan después del segundo 10: no abrir con números.
- El cierre estándar («Descarga Checa…») no aplica: la app aún no se descarga libremente. Se cambia por «Comenta CHECA» + link de registro (ver ajustes).

## Hook inicial (0–3 s)
Voz: «Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo.» Texto en pantalla: «SE VEÍA BUENO. PERDISTE.» Imagen: oferta genérica blanca que se tiñe de rojo. (Base aprobada del video 19.)

## Hooks intermedios (~cada 4–5 s)
Giro «todavía no está en la tienda» (curiosidad) → «cincuenta conductores» (escasez) → beneficio de por vida (recompensa) → «cuando se llenen, se acabó» (pérdida) → CTA específico (comentar CHECA).

## Voz limpia (para ElevenLabs)
Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo. La app Checa te dice cuánto te queda de verdad, ya sin gasolina, antes de que aceptes. Y todavía no está en la tienda. La vamos a probar con cincuenta conductores. Los primeros cincuenta conservan cuarenta por ciento de descuento de por vida. Gratis hasta enero, solo para Android. Cuando se llenen los lugares, se acabó. Comenta CHECA y te mando el link para apartar tu lugar.

## Guion optimizado
| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:04 | Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo. | «SE VEÍA BUENO. PERDISTE.»; oferta que se pone roja; impact-bass | [Hook Inicial] |
| 2 | 0:04–0:09 | La app Checa te dice cuánto te queda de verdad, ya sin gasolina, antes de que aceptes. | Pop-up de Checa cae sobre la oferta (“Te quedan $x”, números de ejemplo); ping | [Micro-hook: solución concreta] |
| 3 | 0:09–0:11 | Y todavía no está en la tienda. | Ícono de Google Play con candado; pausa de medio segundo | [Open Loop: exclusividad] |
| 4 | 0:11–0:14 | La vamos a probar con cincuenta conductores. | Contador de 50 lugares (cuadrícula de 50 puntos) | [Giro: escasez] |
| 5 | 0:14–0:19 | Los primeros cincuenta conservan cuarenta por ciento de descuento de por vida. | Ficha «−40 % DE POR VIDA» en amarillo; riser corto | [Payoff + beneficio] |
| 6 | 0:19–0:22 | Gratis hasta enero, solo para Android. | Chips «GRATIS HASTA ENERO» y «SOLO ANDROID» | [Cambio de ritmo: objeción resuelta] |
| 7 | 0:22–0:25 | Cuando se llenen los lugares, se acabó. | Los puntos se llenan de verde hasta el último; cierra con «0 lugares» | [Pausa dramática + pérdida] |
| 8 | 0:25–0:30 | Comenta CHECA y te mando el link para apartar tu lugar. | Globo de comentario «CHECA», mensaje directo que llega | [CTA específico] |
| 9 | 0:30–0:34 | (pantalla final, sin voz) | Logo semáforo con ✓, @checa.driver, @Eduardoalv9802, «solo Android» y `jeal1498.github.io/checa-web` | [Cierre estándar de pantalla; ver ajustes] |

## Propuestas de animaciones por toma (marca v2: asfalto #0C0C10, tarjetas #16161C, hueso #F4F4EF; el color solo decide)
| Segundaje / frase | Componente | Efecto | Objetivo visual |
|---|---|---|---|
| «buen billete… sales perdiendo» | Oferta genérica blanca (sin logos) | Pasa de blanco a rojo #EF4444, texto «SE VEÍA BUENO. PERDISTE.» sube desde una máscara | Dolor reconocible en la primera mirada |
| «cuánto te queda… sin gasolina» | Pop-up real (`checa-popup.html`) | Cae sobre la oferta; «Te quedan $x» con contador; «Números de ejemplo» | Mostrar el producto sin explicar |
| «todavía no está en la tienda» | Ícono Play con candado | Candado vibra una vez; fondo se oscurece | Curiosidad y exclusividad |
| «cincuenta conductores» | Cuadrícula de 50 puntos | Los puntos se encienden en cascada | Hacer visible el número |
| «cuarenta por ciento… de por vida» | Ficha amarilla #FACC15 | Entra con máscara; el «−40 %» late una vez | Recompensa clara |
| «gratis hasta enero, solo Android» | Dos pastillas | Entran empujándose | Resolver objeciones |
| «cuando se llenen, se acabó» | La misma cuadrícula | Se llena de verde #22C55E, último punto en rojo y «0» | Pérdida concreta |
| «Comenta CHECA…» | Burbuja de comentario + DM | La burbuja sube y llega el mensaje | Acción exacta |

## Ajustes de honestidad
- «Ya sin gasolina»: el pop-up descuenta gasolina con los datos que el conductor configura; la voz lo dice como estimación (no cifras de ganancia).
- «Conservan cuarenta por ciento de descuento de por vida»: es el Plan Fundador para los primeros 50 registrados en la tabla `testers`; el descuento aplica a los planes de pago que lleguen después de la beta.
- «Todavía no está en la tienda»: es cierto, está en prueba cerrada de Google Play.
- Cifras solo las tres habladas (50, 40 %, enero); sin precios; «números de ejemplo» en pantalla.
- **Cierre distinto al estándar:** no se dice «Descarga Checa» porque la app no se descarga libremente; la llamada es «Comenta CHECA» y el dm lleva a `/registro/` (el `dm.txt` se crea en la etapa del copy con `utm_source=tiktok` y `utm_campaign=prueba-en-google-play`). La pantalla final conserva logo, cuentas y link.
- Fecha límite del registro (15 de noviembre o 50 lugares): no se dice en la voz para que el video no caduque; sí puede ir en el texto de publicación mientras esté vigente.

## Texto de publicación (se pasa a `copy.txt` en la etapa del copy)
Aceptas el viaje porque se ve buen billete… y sales perdiendo. 😮‍💨
Checa te dice cuánto te queda ya sin gasolina, antes de aceptar. Está en prueba y busco 50 conductores (solo Android) para probarla primero. Los primeros 50 conservan −40 % de por vida.
¿Quieres tu lugar? Comenta CHECA 👇
#choferuber #choferdidi #conductores #appdeconductores #ganarmas
