# 22 - Cómo calcula Checa (borrador de guion, v1)

Orden real de la app (`Parser.kt`): 1) lee tarifa, km y min (recogida + viaje); 2) filtros de recogida y duración (si fallan, rojo); 3) $/km = tarifa ÷ km totales y $/h = tarifa ÷ min × 60 contra las metas; cumple las dos verde, una amarillo, ninguna rojo; 4) gasolina (km ÷ km/l × $/l) y "Te quedan $x", solo informativo (no cambia el color).

Ejemplo en pantalla ("Números de ejemplo"): metas $10/km y $200/h; viaje de $100, 8 km, 25 min → $12.5/km, $240/h → verde; gasolina $16 → te quedan $84.

## Voz (de corrido)
Te llega una oferta y no sabes si te conviene. Checa la revisa siempre en el mismo orden. Primero lee la tarifa, los kilómetros y los minutos, con la recogida incluida. Segundo, revisa que la recogida no quede muy lejos y que el viaje no sea muy largo. Tercero, calcula cuánto paga cada kilómetro y cada hora, y lo compara con tus metas. Si cumple las dos, verde. Si cumple una, amarillo. Si no cumple ninguna, rojo. Al final descuenta la gasolina y te muestra cuánto te queda. Tú pones las metas, Checa solo las mide. Descarga Checa y checa cuánto te queda de cada viaje.

Pendiente: auditoría, hooks y tabla de animaciones (etapa 2) tras aprobación del guion.
