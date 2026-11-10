# CHECA 33 · Facturar no es ganar
Estado: **avance sin voz guardado** (`video-sin-voz.mp4`, 10 nov 2026, concepto «Las casetas», proyecto `video/hyperframes/facturar-ganar-33`). Falta la voz (ElevenLabs sin créditos) y la aprobación de Eduardo.

Concepto / Problema detectado en el repo: el conductor ve lo que factura (tarifa y total del día) y no lo que le queda; `Parser.neta()` = tarifa − gasolina − desgaste y el pop-up muestra ese neto antes de aceptar. Mensaje de marca: «Facturar no es lo mismo que ganar».

Cifras (con su cálculo, ajustes por defecto 12 km/l y $24/l, desgaste 0):
- Día: 200 km ÷ 12 × 24 = **$400** de gasolina → 1500 − 400 = **$1,100**.
- Oferta: $150, 32 min (16 km) totales → gasolina 16 ÷ 12 × 24 = **$32** → neta **$118**.
- Semáforo (`porKm`/`porHora` sobre la tarifa): 150 ÷ 16 = $9.38/km (< $10, falla) · 150 ÷ 32 × 60 = $281/h (≥ $200, cumple) → 🟡 **CONSIDERAR**.

Reglas de pantalla: texto ≤ 4 palabras; cada escena arranca con una acción clara, la escala muestra la proporción, el elemento se transforma en el de la escena siguiente y cierra en una imagen fija clara.

Guión Desglosado:
L1 | 0:00 - 0:03 | Visual: [cuadro 0 ya con «$1,500 HOY» gigante en el teléfono; flash; golpe rojo «NO GANASTE $1,500» cae y sacude. Cierra: el 1,500 tachado]
  texto: Hiciste 1500 hoy. No ganaste 1500.
  voz: golpe seco, baja en "No ganaste"
  fuerte: "No ganaste"
  pausa: antes de "No"
  tono: firme
  pantalla: "No ganaste" = rojo | "1500" = golpe
L2 | 0:03 - 0:06 | Visual: [el $1,500 se convierte en una pila alta de billetes; una manguera de gasolina succiona la rebanada de arriba, del tamaño exacto del 27 % de la pila; golpe «−$400 GASOLINA». Cierra: pila cortada, rebanada roja a un lado]
  texto: Doscientos kilómetros de chamba se tragan 400 de gasolina.
  voz: va contando, sube en "400"
  fuerte: "400"
  pausa: sin pausa
  tono: firme
  pantalla: "400" = rojo
L3 | 0:06 - 0:09 | Visual: [contador baja 1,500 → 1,100 sobre la pila; una llanta rueda y raspa la orilla de la pila. Golpe amarillo «$1,100 REAL». Cierra: pila de 1,100 con la llanta detenida al lado]
  texto: Te quedan 1100... y todavía falta el desgaste.
  voz: más bajo, como dato duro
  fuerte: "1100", "desgaste"
  pausa: antes de "y todavía"
  tono: cansado
  pantalla: "1100" = amarillo | "desgaste" = rojo
L4 | 0:09 - 0:12 | Visual: [la pila se parte en dos columnas lado a lado: FACTURAS (alta, gris) y GANAS (73 % de alto, verde); sello «≠» se estrella entre ellas. Cierra: dos columnas y el ≠]
  texto: Facturar no es lo mismo que ganar.
  voz: lenta, cada palabra pesa
  fuerte: "Facturar", "ganar"
  pausa: antes de "ganar"
  tono: firme
  pantalla: "Facturar" = golpe | "ganar" = verde
L5 | 0:12 - 0:15 | Visual: [la columna GANAS se encoge y se vuelve el teléfono; cae una tarjeta OFERTA y vibra; cursor corre hacia «Aceptar». Golpe «A CIEGAS». Cierra: cursor frenado sobre Aceptar]
  texto: Y pasa en cada viaje que aceptas a ciegas.
  voz: sube la urgencia en "a ciegas"
  fuerte: "a ciegas"
  pausa: sin pausa
  tono: urgente
  pantalla: "a ciegas" = rojo
L6 | 0:15 - 0:18 | Visual: [zoom a la tarjeta: «$150» grande y «32 min (16 km)»; ruta de 16 km se dibuja en el mapa. Golpe «$150». Cierra: tarjeta quieta con el 150 brillando]
  texto: Esta oferta dice 150 por 16 kilómetros.
  voz: neutra, presenta el dato
  fuerte: "150"
  pausa: sin pausa
  tono: tranquilo
  pantalla: "150" = amarillo
L7 | 0:18 - 0:21 | Visual: [entra el pop-up abajo; una gota de gasolina «−$32» se desprende de la tarjeta y cae en el pop-up; el neto marca ···. Golpe «−$32». Cierra: gota dentro del pop-up]
  texto: La app Checa le resta 32 de gasolina antes de aceptar.
  voz: clara, marca "antes"
  fuerte: "32", "antes"
  pausa: sin pausa
  tono: tranquilo
  pantalla: "32" = rojo | "antes" = golpe
L8 | 0:21 - 0:24 | Visual: [neto cuenta 150 → 118 en verde, el número crece al doble; golpe «$118 TUYOS». Cierra: pop-up con $118 grande]
  texto: Te muestra 118. Lo que de verdad te llevas.
  voz: alivio, sube en "118"
  fuerte: "118"
  pausa: antes de "Lo que"
  tono: tranquilo
  pantalla: "118" = verde
L9 | 0:24 - 0:27 | Visual: [franja del pop-up se pinta amarillo «CONSIDERAR»; aparece el semáforo y se enciende la luz amarilla. Golpe «TÚ DECIDES». Cierra: semáforo en amarillo junto al pop-up]
  texto: Con ese número decides tú.
  voz: firme y cercana
  fuerte: "decides tú"
  pausa: antes de "decides"
  tono: firme
  pantalla: "decides tú" = amarillo
L10 | 0:27 - 0:31 | Visual: [vista cta: logo, «Comenta CHECA» y chip de Google Play; loop al cuadro 0]
  texto: Comenta CHECA y te paso el acceso a la prueba en Google Play.
  voz: directa, sin prisa
  fuerte: "CHECA"
  pausa: sin pausa
  tono: tranquilo
  pantalla: "CHECA" = golpe

CTA final: Comenta CHECA y te paso el acceso a la prueba en Google Play.
