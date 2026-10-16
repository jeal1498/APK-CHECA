# Guion · 06 - Gasolina cara (PERSONAL, v2) · guion, tabla y video aprobados (16 oct 2026)

v1: `marketing/videos/CHECA/06 - Gasolina cara/` · v2: esta carpeta.

**Auditoría de la v1:** abre con una cifra ($800) y encadena cinco más antes del 0:20; la pregunta «Suena bien, ¿no?» se contesta sola; la cuenta tarda en llegar a «te quedan $60»; cierra con «7 días de prueba» (ya no es el estándar).

**Voz limpia:** Cargas gasolina el lunes y el jueves ya andas en la reserva. Cada kilómetro te cuesta más o menos dos pesos de pura gasolina. Imagina un viaje que paga noventa pesos. Entre la recogida y el viaje, la gasolina se lleva treinta. Te quedan sesenta. Un tercio de tu viaje se fue al tanque y ni lo notaste. La app Checa hace esa resta antes de que aceptes y te dice si el viaje conviene. Así la gasolina no se come tu ganancia. Descarga Checa y checa cuánto te queda de cada viaje.

| Corte | Voz | Técnica |
|---|---|---|
| 1 | Cargas gasolina el lunes y el jueves ya andas en la reserva. | [Hook Inicial] situación reconocible, medidor de gasolina que se vacía |
| 2 | Cada kilómetro te cuesta más o menos dos pesos de pura gasolina. | [Micro-hook] la causa |
| 3 | Imagina un viaje que paga noventa pesos. | [Open Loop] |
| 4 | Entre la recogida y el viaje, la gasolina se lleva treinta. | [Giro] −$30 |
| 5 | Te quedan sesenta. | [Pausa dramática] |
| 6 | Un tercio de tu viaje se fue al tanque y ni lo notaste. | [Giro emocional] |
| 7 | La app Checa hace esa resta antes de que aceptes y te dice si el viaje conviene. | [Re-enganche visual] pop-up |
| 8 | Así la gasolina no se come tu ganancia. | [Beneficio] |
| 9 | Descarga Checa y checa cuánto te queda de cada viaje. | [Cierre estándar] |

**Ajustes de honestidad:** «Números de ejemplo» en pantalla; $2/km con 12 km/l y $24/l, dicho como «más o menos»; sin «7 días gratis».

## Propuestas de animaciones/gráficos HTML por toma
Estilo: Dark Mode / Neon UI (#0D0D0D, mono, neones rosa/verde/azul/amarillo, tarjetas flotantes, SVG).

| Segundaje / frase | Componente HTML/UI | Efecto/animación CSS | Objetivo visual |
|---|---|---|---|
| 0:00 «Cargas gasolina el lunes y el jueves ya andas en la reserva» | Tarjeta flotante con medidor de gasolina (arco SVG) y chips «LUN» → «JUE» | Aguja que cae a «E», glow rojo que parpadea, «LUN» tachado | Reconocer el dolor en el primer segundo |
| 0:03 «dos pesos de pura gasolina» | Ticket tipo recibo con contador de km y dinero | Contador mono: cada km suma $2.00, glow amarillo | Volver tangible el costo por km |
| 0:07 «viaje que paga noventa pesos» | Tarjeta de oferta genérica con «$90.00» | Cae con rebote, pulso verde en la cifra | Anclar la expectativa |
| 0:10 «se lleva treinta» | Comparativa en dos bloques: verde «$60» y rojo «−$30» | Corte con glitch, la barra se divide, verde→rojo | Mostrar la pérdida |
| 0:14 «Te quedan sesenta» | Cifra gigante «$60» sobre fondo casi vacío | Zoom lento, glow verde, silencio visual | Pausa dramática |
| 0:16 «Un tercio… al tanque» | Barra de 3 bloques y un ícono SVG de tanque | Un bloque se vuelve rojo y cae al tanque | Visualizar la fracción |
| 0:20 «La app Checa hace esa resta» | Teléfono con la oferta y pop-up real de Checa | Cae el pop-up, «$90» se tacha y queda «$60 netos» | Presentar la solución |
| 0:25 «no se come tu ganancia» | Barra de ganancia con escudo SVG y semáforo | Escudo que frena el −$30, borde verde | Reforzar el beneficio |
| 0:28 «Descarga Checa…» | Cierre estándar | Logo que se arma, cuentas y link | Llamada a la acción |

_Etapa 2: tabla presentada a Eduardo, pendiente de aprobación._
