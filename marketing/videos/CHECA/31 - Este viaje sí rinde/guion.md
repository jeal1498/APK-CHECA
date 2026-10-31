# Guion · 31 - Este viaje sí rinde (CHECA) — 31 oct 2026

**Estado:** guion aprobado por Eduardo (31 oct 2026), escrito desde cero en el chat (sin la skill `guion-checa`, a pedido suyo). Cifras ajustadas a las fórmulas de `Parser.kt` ($/h y $/km sobre la tarifa).

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km)
- **Oferta A (rojo):** $48 · recogida 2 km / 5 min · viaje 10 km / 22 min → 12 km, 27 min. Gasolina $24 → **$24 netos**. $/h 48 ÷ 27 × 60 = **$107** ✗ · $/km **$4.0** ✗ → RECHAZAR.
- **Oferta B (verde):** $151 · recogida 2 km / 5 min · viaje 16 km / 37 min → 18 km, 42 min. Gasolina $36 → **$115 netos**. $/h 151 ÷ 42 × 60 = **$216** ✓ · $/km **$8.4** ✓ → ACEPTAR.
- **Comparador:** B verde gana a A rojo → ★ MEJOR.

Guión Desglosado:
- `0:00 - 0:04` | Audio: «Aceptas el viaje sin saber si ganaste o solo quemaste gasolina.» | Visual: [Flash; teléfono entra girando con la oferta de 48 y la ruta que se dibuja; título «¿Ganaste?»; ficha roja «−gasolina»] `[Hook]`
- `0:04 - 0:06` | Audio: «Mira esta: 48. Se ve bien, ¿no?» | Visual: [Zoom a la oferta; título «¿Conviene?» en amarillo]
- `0:06 - 0:10` | Audio: «Ya con gasolina te quedan 24. Y son 107 la hora.» | Visual: [Cae el pop-up RECHAZAR; título «$24 netos.» en rojo; ficha «$107/h»]
- `0:10 - 0:12` | Audio: «Por kilómetro, 4. La mitad de tu meta.» | Visual: [Fichas «$4.0/km» roja y «$8/km tu meta»; título «Mitad.»]
- `0:12 - 0:15` | Audio: «Ahora esta otra: 151.» | Visual: [Flash; entra la oferta de 151 con el monto que sube]
- `0:15 - 0:18` | Audio: «Te quedan 115. Por hora, 216. Esta sí.» | Visual: [Cae el pop-up ACEPTAR; fichas «$115 netos» y «$216/h»; título «Esta sí.» en verde]
- `0:18 - 0:22` | Audio: «Y si te caen dos al mismo tiempo, te marca cuál conviene más.» | Visual: [Dos teléfonos entran; el de 151 gana con ★ MEJOR y el de 48 se apaga]
- `0:22 - 0:24` | Audio: «Lee la pantalla en tu teléfono, sin internet.» | Visual: [Teléfono gira; línea de escaneo verde; título «Sin internet.»]
- `0:24 - 0:29` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Visual: [Pantalla final estándar, pastilla «Comenta CHECA», sin link] `[cierre]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Tabla de animaciones por toma (sacada del desglose)
| Toma | Frase | Animación |
|---|---|---|
| 0:00–0:04 | «Aceptas el viaje sin saber si ganaste o solo quemaste gasolina.» | Flash; teléfono entra girando con la oferta de 48 y la ruta que se dibuja; título «¿Ganaste?»; ficha roja «−gasolina» |
| 0:04–0:06 | «Mira esta: 48. Se ve bien, ¿no?» | Zoom a la oferta; título «¿Conviene?» en amarillo |
| 0:06–0:10 | «Ya con gasolina te quedan 24. Y son 107 la hora.» | Cae el pop-up RECHAZAR; título «$24 netos.» en rojo; ficha «$107/h» |
| 0:10–0:12 | «Por kilómetro, 4. La mitad de tu meta.» | Fichas «$4.0/km» roja y «$8/km tu meta»; título «Mitad.» |
| 0:12–0:15 | «Ahora esta otra: 151.» | Flash; entra la oferta de 151 con el monto que sube |
| 0:15–0:18 | «Te quedan 115. Por hora, 216. Esta sí.» | Cae el pop-up ACEPTAR; fichas «$115 netos» y «$216/h»; título «Esta sí.» en verde |
| 0:18–0:22 | «Y si te caen dos al mismo tiempo, te marca cuál conviene más.» | Dos teléfonos entran; el de 151 gana con ★ MEJOR y el de 48 se apaga |
| 0:22–0:24 | «Lee la pantalla en tu teléfono, sin internet.» | Teléfono gira; línea de escaneo verde; título «Sin internet.» |
| 0:24–0:29 | «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Pantalla final estándar, pastilla «Comenta CHECA», sin link |
