# Guion · 22 - Cómo calcula Checa (CHECA, v1)

v1: `marketing/videos/CHECA/22 - Cómo calcula Checa/` · v2: `marketing/videos/PERSONAL/22 - Cómo calcula Checa/`. Tema nuevo pedido por Eduardo (18 oct 2026): explicar el proceso de la app. Fecha: la define Eduardo en `calendario.md`.

Orden real de la app (`Parser.kt`, confirmado por Eduardo): 1) lee tarifa, km y min (recogida + viaje); 2) filtros de recogida y duración (si fallan, rojo); 3) $/km = tarifa ÷ km totales y $/h = tarifa ÷ min × 60 contra las metas: cumple las dos verde, una amarillo, ninguna rojo; 4) gasolina (km ÷ km/l × $/l) y «Te quedan $x», solo informativo (no cambia el color).

## Auditoría (del borrador anterior)
- El gancho («Te llega una oferta y no sabes si te conviene») describe, no duele: no dice qué pierdes.
- Los tres pasos se enumeran sin tensión: falta un open loop que prometa algo al final.
- «Si falla un filtro, rojo aunque pague bien» es el giro más fuerte y estaba enterrado.
- Los colores iban seguidos sin cambio de ritmo; el payoff (cuánto te queda) llegaba sin preparación.
- «Tú pones las metas» cierra bien el control, pero faltaba enlazarlo con el inicio (loop).

## Voz limpia (sin etiquetas, para ElevenLabs)
Tienes unos segundos para decidir, y nadie te dice si ese viaje te conviene. Checa lo revisa en tres pasos, y el último te enseña lo que de verdad te queda. Primero lee la tarifa, los kilómetros y los minutos, con la recogida incluida. Segundo, pasa el viaje por tus filtros: que la recogida no quede lejos y que el viaje no sea largo. Si falla uno, sale rojo, aunque pague bien. Tercero, mide cuánto paga cada kilómetro y cada hora, y lo compara con tus metas. Cumple las dos, verde. Cumple una, amarillo. Ninguna, rojo. Y hasta el final descuenta la gasolina y te muestra cuánto te queda. Tú pones las metas, Checa solo las mide. Descarga Checa y checa cuánto te queda de cada viaje.

| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:05 | Tienes unos segundos para decidir, y nadie te dice si ese viaje te conviene. | Oferta genérica con anillo de tiempo que se vacía + texto «¿TE CONVIENE?»; impact-bass | [Hook Inicial: voz + texto + imagen] |
| 2 | 0:05–0:10 | Checa lo revisa en tres pasos, y el último te enseña lo que de verdad te queda. | Logo del semáforo; tres puntos 1-2-3, el 3 con «?» | [Open Loop: promete una revelación al final] |
| 3 | 0:10–0:15 | Primero lee la tarifa, los kilómetros y los minutos, con la recogida incluida. | Línea de escaneo; fichas Tarifa / Km / Min salen de la oferta | [Re-enganche visual] |
| 4 | 0:15–0:23 | Segundo, pasa el viaje por tus filtros: que la recogida no quede lejos y que el viaje no sea largo. Si falla uno, sale rojo, aunque pague bien. | Dos filtros (recogida, duración); uno falla y el pop-up cae en rojo; error | [Giro: el rojo aunque pague bien] |
| 5 | 0:23–0:31 | Tercero, mide cuánto paga cada kilómetro y cada hora, y lo compara con tus metas. Cumple las dos, verde. Cumple una, amarillo. Ninguna, rojo. | Fichas $/km y $/h contra «meta ✓»; tres semáforos en cadena | [Cambio de ritmo: tres cortes cortos] |
| 6 | 0:31–0:36 | Y hasta el final descuenta la gasolina y te muestra cuánto te queda. | Ticket: viaje − gasolina = «Te quedan $84» (números de ejemplo); ping | [Payoff del Open Loop] |
| 7 | 0:36–0:39 | Tú pones las metas, Checa solo las mide. | Panel de metas con −/+; el pop-up cambia | [Giro emocional: control] |
| 8 | 0:39–0:43 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar con pausa 0.45 s | [CTA específico + Loop de repetición] |

## Animaciones y gráficos HTML por toma (marca v2: asfalto #0C0C10, hueso #F4F4EF; verde/amarillo/rojo solo para decidir)
| Toma / frase | Componente HTML/UI | Efecto/animación CSS | Objetivo visual |
|---|---|---|---|
| 1 · «unos segundos para decidir» | Oferta genérica + anillo de tiempo + «¿TE CONVIENE?» | Anillo se vacía, oferta se oscurece | Presión de tiempo |
| 2 · «tres pasos… lo que te queda» | Logo semáforo + tres puntos numerados, el 3 con «?» | Logo rojo→ámbar→verde→✓; puntos entran en cadena | Promete el payoff |
| 3 · «lee tarifa, km y minutos» | Teléfono con oferta + fichas | Línea de escaneo; fichas salen empujando | Qué datos toma |
| 4 · «tus filtros… rojo aunque pague bien» | Dos filtros (recogida, duración) + pop-up rojo | Un filtro se marca en rojo; pop-up cae | Giro: el filtro manda |
| 5 · «km, hora, metas» | Fichas $/km y $/h + semáforo | Pastillas «meta ✓»; el semáforo cambia por cantidad cumplida | Regla clara de colores |
| 6 · «descuenta la gasolina» | Ticket de desglose | Contador baja de $100 a «Te quedan $84»; «Números de ejemplo» | Muestra la cuenta |
| 7 · «tú pones las metas» | Panel de metas con −/+ | Cambia la meta y el pop-up cambia | Control |
| 8 · «Descarga Checa…» | Cierre estándar | Logo, pastilla, cuentas y link | Marca |

Ejemplo en pantalla: metas $10/km y $200/h; viaje de $100, 8 km, 25 min → $12.5/km, $240/h → verde; gasolina $16 → te quedan $84.

**Ajustes de honestidad:** la gasolina depende del km/l y $/l que configure el conductor y es informativa (no cambia el color); cifras solo en pantalla con «números de ejemplo»; «filtros» = recogida y duración que él activa; metas por defecto $8/km y $150/h, el ejemplo usa las del conductor.

**Texto de publicación (v1):**
Así decide Checa si un viaje te conviene, en tres pasos 🚦
Lee la oferta, revisa tus filtros y compara el $/km y $/hora con tus metas. Al final te dice cuánto te queda ya con la gasolina.
¿Qué meta por kilómetro te pones tú? 👇
Descarga CHECA y checa cuánto te queda de cada viaje (solo Android).
#choferuber #choferdidi #conductores #gasolina #uberdidi
