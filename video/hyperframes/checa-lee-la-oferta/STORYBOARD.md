---
format: 1080x1920
duration: 50s
message: "Checa lee la oferta por ti y te dice cuánto te queda limpio, tú decides"
audience: conductores de Uber y DiDi
mode: autonomous
music: none
---

## Video direction

Mismo diseño que checa-promo (`frame.md`): fondo negro, verde #22C55E, amarillo #FACC15, rojo #F43F5E, Fira Sans / Fira Code. Pop-up de `assets/checa-popup.html` sin rediseñar. «Números de ejemplo» donde haya cifras. Oferta genérica sin logos. Gancho en 0–3 s y un re-gancho en cada corte, marcado con un sonido. CTA sin «7 días gratis»: «Descarga Checa».

## Frame 1 — Gancho

- duration: 5.8s
- voiceover: "Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo."
- transition_in: cut
- status: animated
- src: compositions/frames/01-gancho.html
- sfx: whoosh-short, impact-bass-1

Gancho (pérdida): oferta blanca de $180 cae; «se veía bueno.» y «perdiste.» suben desde máscara; la tarifa pasa a rojo y se acerca a «$100». Recurso: máscara + sincronía con «perdiendo».

## Frame 2 — Tensión

- duration: 5.2s
- voiceover: "Casi siempre pasa igual: la oferta te cae manejando y no hay tiempo de calcular."
- transition_in: crossfade
- status: animated
- src: compositions/frames/02-tension.html
- sfx: ping, riser

Tensión: carro line-art avanza; una oferta cae sobre él; un reloj de 15 s se consume. Recurso: encadenar (la oferta empuja el reloj) + cámara sigue la oferta.

## Frame 3 — Mira

- duration: 4.2s
- voiceover: "Para eso existe la app Checa. Mira qué hace con esa oferta."
- transition_in: cut
- status: animated
- src: compositions/frames/03-mira.html
- sfx: notification, whoosh-short

Bucle abierto: «la app checa» (máscara); teléfono con la oferta, escáner verde y el pop-up cae sobre ella en «qué hace». Recurso: transformar (la oferta pasa a ser el teléfono) + encadenar.

## Frame 4 — Neta

- duration: 5.4s
- voiceover: "Lee la oferta por ti y te dice cuánto te queda limpio, ya sin la gasolina."
- transition_in: crossfade
- status: animated
- src: compositions/frames/04-neta.html
- sfx: click-soft, impact-bass-1

Cifra: ticket Viaje $180 − Gasolina $80 = $100 netos; los renglones entran con la voz y los netos laten en «limpio». Recurso: sincronía con la voz.

## Frame 5 — Rojo

- duration: 5.4s
- voiceover: "Ciento ochenta se ven bien, pero te quedan cien en una hora y cuarto. Rojo."
- transition_in: cut
- status: animated
- src: compositions/frames/05-rojo.html
- sfx: error, glitch-1

Giro: $180 se tacha y queda $100; reloj «1 h 15»; pop-up rojo con $144/h y $4.50/km en rojo. Recurso: sincronía con las cifras + énfasis rojo.

## Frame 6 — Verde

- duration: 5.4s
- voiceover: "Otro, de noventa y seis pesos, te deja ochenta en diecisiete minutos. Verde."
- transition_in: crossfade
- status: animated
- src: compositions/frames/06-verde.html
- sfx: chime, sparkle

Contraste: dos tarjetas lado a lado ($180 → $100 en 75 min, $96 → $80 en 17 min); la segunda se pone verde y cae el pop-up verde. Recurso: cámara sigue la comparación + encadenar.

## Frame 7 — Metas

- duration: 4.5s
- voiceover: "Tú pones la vara: cuánto quieres ganar por kilómetro y por hora."
- transition_in: cut
- status: animated
- src: compositions/frames/07-metas.html
- sfx: click, key-press

Control: panel de metas con −/+ ($8/km, $150/h) y una vara que sube. Recurso: máscara en «tu vara» + sincronía con «kilómetro» y «hora».

## Frame 8 — Voz

- duration: 5.4s
- voiceover: "Y ni miras la pantalla: la app te dice en voz alta aceptar, considerar o rechazar."
- transition_in: crossfade
- status: animated
- src: compositions/frames/08-voz.html
- sfx: notification, pop

Beneficio: bocina line-art con ondas; el pop-up cambia verde → amarillo → rojo con cada palabra. Recurso: sincronía con la voz + énfasis por color.

## Frame 9 — Tú decides

- duration: 5.8s
- voiceover: "Ojo: Checa no acepta los viajes por ti, solo te dice cuál conviene y cuál no."
- transition_in: cut
- status: animated
- src: compositions/frames/09-decides.html
- sfx: pop, click-soft

Objeción resuelta: un dedo toca «Aceptar» en la oferta; el pop-up de Checa solo avisa arriba. Recurso: la cámara sigue el dedo + encadenar (el toque provoca el sello «tú decides»).

## Frame 10 — CTA

- duration: 4.9s
- voiceover: "Descarga Checa y checa cuánto te queda de cada viaje."
- transition_in: crossfade
- status: animated
- src: compositions/frames/10-cta.html
- sfx: chime

Logo semáforo que se arma, «checa» y pastilla «Descarga CHECA · solo Android». Recurso: transformar (el logo se arma desde las 3 luces).
