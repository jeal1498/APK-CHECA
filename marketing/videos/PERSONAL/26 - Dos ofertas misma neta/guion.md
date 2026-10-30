# Guion · 26 - Dos ofertas, misma neta (PERSONAL, v2) — propuesto, sin aprobar

Skill: `guion-remotion-checa` + reglas v2 (`marketing/plan/reglas-v2.md`). Misma idea y mismas cifras que la v1 (`CHECA/26`), con otro gancho y orden: abre con la oferta de $82 que «parece peor» y revela al final que deja más que la de $95.

## Auditoría de la v1 (qué cambia)
- La v1 abre con la de $95 tachada y descubre el $67. La v2 invierte el orden: primero el viaje que nadie quiere ($82), luego el «cualquiera agarra el de 95», y el giro llega a la mitad.
- Re-enganche cada ~3 s; la revelación («el de 82 deja más») va antes del pop-up para que el pop-up sea la prueba, no el descubrimiento.

## Números de ejemplo (fórmulas del código; 12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km)
- **A, $95:** recogida 3.8 km / 8 min + viaje 10 km / 22 min → 13.8 km, 30 min. Gasolina $27.60 → **$67 netos**. $6.9/km ✗, $190/h ✓ → CONSIDERAR.
- **B, $82:** recogida 1.2 km / 4 min + viaje 6 km / 11 min → 7.2 km, 15 min. Gasolina $14.40 → **$68 netos**. $11.4/km ✓, $328/h ✓ → ACEPTAR ★ mejor.

## Guion de voz (limpio, ~33 s)
Te cae un viaje de 82 pesos. [sarcastic] Qué poquito. Y al mismo tiempo, uno de 95. Cualquiera agarra el de 95. Pero ese tiene recogida larga y casi media hora de viaje, y con la gasolina de todo el recorrido te quedan 67. El de 82 recoge cerca y dura quince minutos. Le restas la gasolina y te quedan [excited] 68. Uno más, en la mitad del tiempo. Checa te lo calcula en el pop-up, antes de aceptar, y te marca cuál conviene. Sígueme para más cuentas como esta. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión desglosado
- `0:00 - 0:03` | Audio: «Te cae un viaje de 82 pesos. [sarcastic] Qué poquito.» | Visual: [Gancho con golpe: flash en el cuadro 0, tarjeta «$82» que se estrella con zoom, el monto sube de $0, sello gris «¿POQUITO?» que tiembla] `[Hook Inicial]`
- `0:03 - 0:06` | Audio: «Y al mismo tiempo, uno de 95. Cualquiera agarra el de 95.» | Visual: [Layout split: entra la tarjeta «$95» desde la derecha, la de 82 se reduce y se apaga; flecha ↑ en 95] `[Open Loop]`
- `0:06 - 0:10` | Audio: «Pero ese tiene recogida larga y casi media hora de viaje, y con la gasolina de todo el recorrido te quedan 67.» | Visual: [Mapa line-art: recogida 3.8 km y viaje 10 km se dibujan; ficha «Gasolina −$28» cae; contador $95 → $67 en rojo] `[Giro]`
- `0:10 - 0:13` | Audio: «El de 82 recoge cerca y dura quince minutos.» | Visual: [La tarjeta de 82 vuelve; fichas «1.2 km» y «15 min» en escalera, reloj que se llena a la mitad] `[Re-enganche visual]`
- `0:13 - 0:17` | Audio: «Le restas la gasolina y te quedan 68. Uno más, en la mitad del tiempo.» | Visual: [Dos contadores en paralelo: 95 → 67 y 82 → 68 (este en verde); barras de tiempo 30 min vs 15 min con «$135/h» vs «$270/h netos»] `[Payoff]`
- `0:17 - 0:21` | Audio: «Checa te lo calcula en el pop-up, antes de aceptar, y te marca cuál conviene.» | Visual: [Dos pop-ups «A · Franja» apilados: $68 verde ★ MEJOR y $67 amarillo «2ª»; «Números de ejemplo»] `[Prueba]`
- `0:21 - 0:24` | Audio: «Sígueme para más cuentas como esta.» | Visual: [Tarjeta de seguir @Eduardoalv9802 con botón «+ Seguir» que se presiona] `[Cambio de ritmo]`
- `0:24 - 0:29` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Visual: [Pantalla final estándar sin link: logo ✓, pastilla «Comenta CHECA», @checa.driver y @Eduardoalv9802] `[CTA específico]`

CTA final: «Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
- Marca v2, sin logos ni nombres de Uber/DiDi (pastilla «OFERTA»), sin ciudad ni país, «Números de ejemplo» en pantalla.
- Plantilla: `video/hyperframes/como-funciona-17/construir.py` (o la del 26); voz v3 en todas las líneas, sin `[pause]`.
- Portada distinta a la v1 (etapa 3).
