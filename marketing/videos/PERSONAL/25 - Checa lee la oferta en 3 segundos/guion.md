# Guion · 25 - Checa lee la oferta en 3 segundos (PERSONAL, borrador)

> **Descartado por Eduardo el 30 oct 2026:** usa la campaña vieja «Busco 50»; el video 26 cuenta la misma idea. Se conserva sin hacerse.

**Estado:** borrador (29 oct 2026), formato de la skill `guion-vertical-checa`. Cifras de ejemplo: $60 y $38. Cambiar por un viaje real.

**Concepto / Problema detectado en el repo:** durante una oferta el conductor tiene segundos para decidir, y la app de Uber o DiDi solo muestra el monto bruto. Checa lee la pantalla de la oferta y muestra la neta después de gasolina y desgaste, $/hora, $/km y semáforo (`CapturaService`, `RutaService`, `Parser`, `Popup`).

## Guion desglosado
- `0:00 - 0:03` | Audio: «Aceptaste ese viaje de 60 pesos… y te quedó en 38.» | Visual (Edición obligatoria): corte seco a tarjeta «Nueva solicitud · $60» a pantalla completa; zoom de 110 % en 0.3 s; el «$60» entra con golpe.
- `0:03 - 0:06` | Audio: «Cuando llegas, ya te comiste la gasolina y el desgaste.» | Visual (Edición obligatoria): la tarjeta se parte en dos con sacudida de 0.2 s; aparece «−$22 gasolina» en rojo palabra por palabra.
- `0:06 - 0:09` | Audio: «La app te mostró 60, nada más.» | Visual (Edición obligatoria): maqueta propia de oferta con zoom al monto; overlay «solo monto bruto» en hueso; corte a la mitad del bloque.
- `0:09 - 0:12` | Audio: «Por eso hice Checa: lee la oferta y te dice cuánto te queda.» | Visual (Edición obligatoria): barrido verde y aparece el logo de Checa; texto «Checa» con zoom de 120 % a 100 % en 0.4 s.
- `0:12 - 0:15` | Audio: «Resta la gasolina y el desgaste.» | Visual (Edición obligatoria): pop-up de Checa sobre la tarjeta; la cifra cuenta de $60 a $38 en 1.5 s.
- `0:15 - 0:18` | Audio: «Y te dice si vale la pena: cuánto por hora y cuánto por kilómetro.» | Visual (Edición obligatoria): dos fichas ($/h y $/km) entran una tras otra con rebote; zoom alternado entre ambas.
- `0:18 - 0:21` | Audio: «Verde, amarillo o rojo. Este es rojo.» | Visual (Edición obligatoria): franja roja del pop-up; «ROJO» en texto grande, palabra por palabra; sacudida corta al final del bloque.
- `0:21 - 0:24` | Audio: «Decides en segundos, sin hacer cuentas de cabeza.» | Visual (Edición obligatoria): contador de 5 s en esquina superior que baja con cada sílaba; corte al llegar a cero.
- `0:24 - 0:27` | Audio: «Busco 50 conductores que la usen gratis tres meses.» | Visual (Edición obligatoria): overlay «50 conductores · 3 meses gratis» en lista grande; el «50» hace zoom de 130 % a 100 %.
- `0:27 - 0:30` | Audio: «Comenta YO aquí abajo y te paso los accesos.» | Visual (Edición obligatoria): fondo asfalto, texto «COMENTA YO» en hueso; flecha animada hacia abajo; cierre en seco.

## CTA final
Comentar «YO» y recibir accesos. Un solo paso.

## Notas
- La oferta de 3 meses gratis sigue vigente (confirmó Eduardo el 29 oct 2026).
- Este guion puede servir como versión A (con cara) o B (gráficos) del A/B del guion de DiDi: mismo texto, solo cambia la columna Visual.
