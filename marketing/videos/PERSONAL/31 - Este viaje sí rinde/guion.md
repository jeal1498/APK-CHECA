# Guion · 31 - Este viaje sí rinde (PERSONAL, v2) — 31 oct 2026, skill `guion-checa`

**Estado:** propuesto → producido; falta la revisión final de Eduardo. Mismas cifras que la v1 de CHECA, con otro gancho (abre con lo que te queda de la buena), otro orden (verde → rojo → comparador) y otras palabras.

Concepto / Problema detectado en el repo:
El conductor decide en segundos con la tarifa bruta. Checa (`Parser.kt`) resta la gasolina de todo el camino (km totales ÷ 12 × 24), calcula $/h y $/km sobre km y min totales, pinta el semáforo (`nivel()`) y con dos ofertas marca la mejor (`Comparador.kt`).

## Números de ejemplo (12 km/l, $24/l, meta $150/h y $8/km)
- **Oferta B (verde):** $151 · recogida 2 km / 5 min · viaje 16 km / 37 min → 18 km, 42 min. Gasolina $36 → **$115 netos**. $/h 151 ÷ 42 × 60 = **$216** ✓ · $/km 151 ÷ 18 = **$8.4** ✓ → ACEPTAR.
- **Oferta A (rojo):** $48 · recogida 2 km / 5 min · viaje 10 km / 22 min → 12 km, 27 min. Gasolina $24 → **$24 netos**. $/h **$107** ✗ · $/km **$4.0** ✗ → RECHAZAR.
- **Comparador:** B verde gana a A rojo → ★ MEJOR.

Guión Desglosado:
- `0:00 - 0:03` | Audio: «Te ofrecen 151 y te quedan 115.» | Remotion / Visual: [Flash; teléfono entra girando, el monto sube a 151 y cae el pop-up ACEPTAR; título «$115 netos.» en verde] `[Hook]`
- `0:03 - 0:07` | Audio: «Por hora, 216. Por kilómetro, 8.4. Verde, va.» | Remotion / Visual: [Fichas «$216/h» y «$8.4/km» flotan una por una; título «Verde.»]
- `0:07 - 0:10` | Audio: «Ahora te cae otra: 48.» | Remotion / Visual: [Flash; entra la oferta de 48 y se dibuja la ruta; título «Ahora otra.»]
- `0:10 - 0:14` | Audio: «Sin la gasolina, te quedan 24. Y son 107 la hora.» | Remotion / Visual: [Cae el pop-up RECHAZAR; título «$24 netos.» en rojo; ficha «$107/h»]
- `0:14 - 0:18` | Audio: «Por kilómetro, 4, y tu meta es 8. Rojo, siguiente.» | Remotion / Visual: [Fichas «$4.0/km» roja y «$8/km tu meta»; título «Rojo.»]
- `0:18 - 0:22` | Audio: «Si te caen las dos juntas, la app Checa te marca la que conviene.» | Remotion / Visual: [Dos teléfonos entran; el de 151 gana con ★ MEJOR y el de 48 se apaga] `[iphone_duo]`
- `0:22 - 0:25` | Audio: «Lee la pantalla de tu teléfono, sin internet.» | Remotion / Visual: [Teléfono gira; línea de escaneo verde; título «Sin internet.»]
- `0:25 - 0:30` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Remotion / Visual: [Pantalla final estándar, pastilla «Comenta CHECA», sin link] `[cierre]`

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Tabla de animaciones por toma (sacada del desglose)
| Toma | Frase | Animación |
|---|---|---|
| 0:00–0:03 | «Te ofrecen 151 y te quedan 115.» | Flash; teléfono entra girando, el monto sube a 151 y cae el pop-up ACEPTAR; título «$115 netos.» en verde |
| 0:03–0:07 | «Por hora, 216. Por kilómetro, 8.4. Verde, va.» | Fichas «$216/h» y «$8.4/km» flotan una por una; título «Verde.» |
| 0:07–0:10 | «Ahora te cae otra: 48.» | Flash; entra la oferta de 48 y se dibuja la ruta; título «Ahora otra.» |
| 0:10–0:14 | «Sin la gasolina, te quedan 24. Y son 107 la hora.» | Cae el pop-up RECHAZAR; título «$24 netos.» en rojo; ficha «$107/h» |
| 0:14–0:18 | «Por kilómetro, 4, y tu meta es 8. Rojo, siguiente.» | Fichas «$4.0/km» roja y «$8/km tu meta»; título «Rojo.» |
| 0:18–0:22 | «Si te caen las dos juntas, la app Checa te marca la que conviene.» | Dos teléfonos entran; el de 151 gana con ★ MEJOR y el de 48 se apaga |
| 0:22–0:25 | «Lee la pantalla de tu teléfono, sin internet.» | Teléfono gira; línea de escaneo verde; título «Sin internet.» |
| 0:25–0:30 | «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Pantalla final estándar, pastilla «Comenta CHECA», sin link |
