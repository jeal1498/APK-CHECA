# Guion · 07 - Pasajeros y cancelaciones (PERSONAL, v2)

## 07 · Pasajeros y cancelaciones (v2, optimizado para retención)
v1: `marketing/videos/CHECA/07 - Pasajeros y cancelaciones/` · v2: `marketing/videos/PERSONAL/07 - Pasajeros y cancelaciones/` (~38 s con cierre estándar, estilo HyperFrames, proyecto `video/hyperframes/pasajeros-v2/`)

## Auditoría de la v1
- El gancho («Llegas por el pasaje… y te cancela») es bueno, pero el segundo corte se queda en la queja y no avanza.
- «Aplica estos 4 consejos de volada» no deja nada abierto: no hay motivo para quedarse hasta el cuarto.
- Los consejos 1 y 2 caen con el mismo ritmo y el mismo largo; el oído no distingue cuál importa.
- El consejo 4 (el de seguridad) va enterrado y se dice igual que los demás.
- «Comparte esto con un compañero» llega sin razón; no dice a quién ni por qué.

## Voz limpia (sin etiquetas, para ElevenLabs)
Llegas al punto de recogida y el pasajero ya canceló. Ahí te quedas, con el carro parado y sin un peso. [Corte] Te dejo 4 jugadas para que no te vuelva a pasar igual. La cuarta es la que te cuida a ti. [Corte] Uno: antes de que te toque, aprende cuánto espera tu app. [Corte] Cumplido ese tiempo, cancela tú y checa si te toca cobro. [Corte] Dos: si te cancelan estando ahí, captura de pantalla y reporte, en ese momento. [Corte] Tres: nombre y destino, antes de quitar los seguros. [Corte] Cuatro, la que no se negocia: [Corte] Si el pasajero se pone pesado, no discutas. Búscate un lugar seguro y cancela. [Corte] Ningún viaje vale una pelea. [Corte] Mándaselo a un compañero que maneje de noche. Sígueme para más. [Corte] Descarga Checa y checa cuánto te queda de cada viaje.

| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:04 | Llegas al punto de recogida y el pasajero ya canceló. Ahí te quedas, con el carro parado y sin un peso. | Pin de recogida, «CANCELADO» rojo, contador de ganancia en $0; impact-bass | [Hook Inicial: situación reconocible, voz + texto + imagen a la vez] |
| 2 | 0:04–0:08 | Te dejo 4 jugadas para que no te vuelva a pasar igual. La cuarta es la que te cuida a ti. | «4» gigante, el último círculo parpadea en rojo | [Open Loop: la #4 se paga hasta el final] |
| 3 | 0:08–0:11 | Uno: antes de que te toque, aprende cuánto espera tu app. | Reloj de arena «Reglas de espera»; ping | [Re-enganche visual: reloj que corre] |
| 4 | 0:11–0:15 | Cumplido ese tiempo, cancela tú y checa si te toca cobro. | Botón «Cancelar viaje» ✓ verde, «¿te toca cobro?» | [Giro: la acción es tuya] |
| 5 | 0:15–0:19 | Dos: si te cancelan estando ahí, captura de pantalla y reporte, en ese momento. | Captura cayendo en «Reportar» | [Cambio de ritmo: frases cortas, imperativo] |
| 6 | 0:19–0:22 | Tres: nombre y destino, antes de quitar los seguros. | Ficha de usuario con ✓ en nombre y destino; click | [Micro-hook: el seguro como momento clave] |
| 7 | 0:22–0:24 | Cuatro, la que no se negocia: | Pantalla negra, «4» rojo, silencio de medio segundo | [Pausa dramática: tensión antes del payoff] |
| 8 | 0:24–0:29 | Si el pasajero se pone pesado, no discutas. Búscate un lugar seguro y cancela. | Globo de diálogo tachado, pin verde «Lugar seguro»; destello | [Payoff del Open Loop + Re-enganche visual] |
| 9 | 0:29–0:31 | Ningún viaje vale una pelea. | Casa con la luz encendida; música baja | [Giro emocional: valor que se queda] |
| 10 | 0:31–0:34 | Mándaselo a un compañero que maneje de noche. Sígueme para más. | Avión de papel, «+ Seguir» | [CTA específico: a quién y qué hace después] |
| 11 | 0:34–0:39 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar: logo, «Descarga CHECA», @checa.driver, @Eduardoalv9802 y el link | [Cierre estándar: eslogan + cuentas + link] |

## Animaciones y gráficos HTML por toma (Dark Mode / Neon UI, fondo #0D0D0D)
| Toma / frase | Componente HTML/UI | Efecto/animación CSS | Objetivo visual |
|---|---|---|---|
| 1 · 0:00 «el pasajero ya canceló… sin un peso» | Pin de recogida en mapa oscuro + tarjeta «CANCELADO» rosa + contador «Ganado: $0» | Pin cae y rebota; tarjeta con glow rosa y sacudida; contador baja a $0 | Dolor reconocible en 1 s |
| 2 · 0:04 «4 jugadas… la cuarta te cuida» | «4» gigante con 4 círculos | Círculos se encienden uno a uno; el 4.º parpadea en rojo | Open loop: hay que llegar al 4 |
| 3 · 0:08 «cuánto espera tu app» | Reloj de arena + tarjeta «Reglas de espera» | Arena cae; barra de tiempo que se llena en amarillo | Concepto de tiempo de espera |
| 4 · 0:11 «cancela tú y checa si te toca cobro» | Botón «Cancelar viaje» + chip «¿te toca cobro?» | Botón pasa a verde con ✓; chip hace pop | La acción es tuya; sin prometer cobro |
| 5 · 0:15 «captura y reporte, en ese momento» | Captura de pantalla que cae en carpeta «Reportar» | Flash blanco, captura se encoge y entra; sello ✓ verde | Hábito concreto y rápido |
| 6 · 0:19 «nombre y destino, antes de los seguros» | Ficha de usuario con 2 filas y candado | Filas se marcan ✓ en verde; candado se abre al final | Verificar antes de abrir |
| 7 · 0:22 «Cuatro, la que no se negocia:» | Pantalla negra, «4» rojo | Medio segundo de silencio; «4» late | Pausa dramática |
| 8 · 0:24 «no discutas… lugar seguro y cancela» | Globo de diálogo tachado + pin verde «Lugar seguro» | Tachado en rosa; pin cae en verde; destello | Payoff: salir sin pelear |
| 9 · 0:29 «Ningún viaje vale una pelea» | Casa con ventana encendida | Luz amarilla se enciende; música baja | Cierre emocional |
| 10 · 0:31 «Mándaselo… Sígueme» | Avión de papel + botón «+ Seguir» | Avión vuela al borde; botón pulsa | CTA claro |
| 11 · 0:34 «Descarga Checa…» | Cierre estándar | Logo, pastilla «Descarga CHECA», @checa.driver, @Eduardoalv9802, link; pausa 0.45 s | Cierre de marca |

**Ajustes de honestidad:** «checa si te toca cobro» (el cobro por cancelación depende de cada app; no se promete); sin cifras habladas; «Ningún viaje vale una pelea» es consejo, no promesa.

**Texto de publicación (v2):**
Te cancelan cuando ya llegaste y te quedas sin nada 😤
4 jugadas para manejar mejor las cancelaciones y los pasajeros difíciles en Uber y DiDi. La cuarta es la que te cuida.
Guárdalo y mándaselo a un compañero que maneje de noche.
#choferuber #choferdidi #conductores #cancelaciones #consejosdeconductor
