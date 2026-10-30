# Guion · 26 - Dos ofertas, misma neta (PERSONAL, v2) — hecho con `guion-remotion-checa` el 30 oct 2026; por aprobar

Skill: `guion-remotion-checa` + reglas v2 (`marketing/plan/reglas-v2.md`). Misma idea y cifras que la v1 (`CHECA/26`), otro gancho y orden: abre con el $82 que «parece peor» y revela que deja más que el $95.

Concepto / Problema detectado en el repo:
El conductor decide en segundos con la tarifa bruta. `Parser.kt` calcula gasolina de **todo** el recorrido (`km / kmPorLitro × precioLitro`), `neta()` = tarifa − gasolina, y $/km y $/h sobre km y min **totales** (`nivel()`). `Comparador.kt` ordena primero por nivel (verde > amarillo) y, si empatan, por neta/hora (criterio 0); el ganador recibe `MEJOR`. Con dos ofertas, la de menor tarifa puede ser la mejor.

Números de ejemplo (fórmulas del código; 12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km):
- **A, $95:** recogida 3.8 km / 8 min + viaje 10 km / 22 min → 13.8 km, 30 min. Gasolina 13.8/12×24 = $27.60 → **$67 netos**. $/km 6.88 ✗, $/h 190 ✓ → 🟡 CONSIDERAR (2ª).
- **B, $82:** recogida 1.2 km / 4 min + viaje 6 km / 11 min → 7.2 km, 15 min. Gasolina 7.2/12×24 = $14.40 → **$68 netos**. $/km 11.4 ✓, $/h 328 ✓ → 🟢 ACEPTAR ★ MEJOR.
- Neta/hora: A 67.4/30×60 = $135; B 67.6/15×60 = $270.

Guión Desglosado:
- `0:00 - 0:03` | Audio: «Te cae uno de 82 pesos. [sarcastic] Qué poquito.» | Remotion / Visual: [Flash en el cuadro 0; RideCard «$82» con spring desde zoom 1.9 → 1; contador $0 → $82 con interpolate + Math.round; sello gris «¿POQUITO?» con shake de 6 frames] `[Hook Inicial]`
- `0:03 - 0:06` | Audio: «Y al mismo tiempo, uno de 95.» | Remotion / Visual: [Layout split: segunda RideCard «$95» entra con spring desde la derecha; la de 82 se reduce con scale + translateX y baja de opacidad] `[Open Loop]`
- `0:06 - 0:09` | Audio: «Cualquiera agarra el de 95.» | Remotion / Visual: [Flecha ↑ hueso sobre $95 con spring; zoom scale 1 → 1.15 al monto; la de 82 queda apagada] `[Micro-hook]`
- `0:09 - 0:12` | Audio: «Pero recoges lejos y son casi treinta minutos.» | Remotion / Visual: [Mapa line-art: ruta de recogida (3.8 km) y de viaje (10 km) se dibujan con strokeDashoffset; reloj circular que se llena a 30 min] `[Giro]`
- `0:12 - 0:15` | Audio: «Con la gasolina de todo el recorrido, te quedan 67.» | Remotion / Visual: [Ficha «Gasolina −$28» cae con spring en rojo; contador $95 → $67 con interpolate + Math.round; tachado SVG sobre el 95] `[Payoff parcial]`
- `0:15 - 0:18` | Audio: «El de 82 recoge cerca, dura quince minutos.» | Remotion / Visual: [Vuelve la tarjeta de 82 al centro; fichas JetBrains Mono «1.2 km» y «15 min» con stagger de 4 frames; reloj que se llena a la mitad] `[Re-enganche visual]`
- `0:18 - 0:21` | Audio: «Le restas la gasolina y te quedan 68.» | Remotion / Visual: [Contador $82 → $68 en verde con interpolate; ficha «Gasolina −$14» cae] `[Payoff del Open Loop]`
- `0:21 - 0:24` | Audio: «Uno más, en la mitad del tiempo.» | Remotion / Visual: [Dos contadores en paralelo ($67 / $68, el de 68 en verde); barras de tiempo 30 min vs 15 min que crecen con spring; etiquetas «$135/h» y «$270/h netos»] `[Giro emocional]`
- `0:24 - 0:27` | Audio: «Checa te lo calcula antes de aceptar.» | Remotion / Visual: [Dos Popup «A · Franja» apilados: $68 verde con ★ MEJOR y $67 amarillo «2ª»; entrada con spring escalonada; texto «Números de ejemplo»] `[Prueba]`
- `0:27 - 0:30` | Audio: «Y te marca cuál conviene. Sígueme para más.» | Remotion / Visual: [Zoom scale 1 → 1.3 al ★ MEJOR; después tarjeta de @Eduardoalv9802 con botón «+ Seguir» que se presiona con spring] `[Cambio de ritmo]`
- `0:30 - 0:35` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar sin link: semáforo animado rojo → ámbar → verde → ✓, pastilla «Comenta CHECA» con spring scale, @checa.driver y @Eduardoalv9802] `[CTA específico]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.» (una sola acción; DM a `/registro/`).

Notas:
- Bloques de ~3 s (el último ~5 s por el cierre), ≤ 8 palabras de voz por bloque. Total ≈ 35 s.
- Voz v3 en todas las líneas, 1 tag por línea como máximo, sin `[pause]`. Marca v2; pop-ups con «OFERTA»; sin ciudad ni país; sin logos de Uber/DiDi.
- Producción en Hyperframes (`video/hyperframes/como-funciona-17/construir.py` o la del 26), instrucciones traducidas con el proceso de `videos-checa`.
- Etapa siguiente: tabla de animaciones (se muestra en el chat) → portada (distinta a la v1) → video → copy.
