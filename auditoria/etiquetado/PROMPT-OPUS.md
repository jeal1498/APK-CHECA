# Prompt para que Opus proponga las etiquetas (E4, revisión de propuesta IA)

Uso: en claude.ai con Opus, pega el bloque de abajo y adjunta las capturas en tandas de **hasta 20** (`<id>.webp`, con su nombre de archivo). Copia cada respuesta JSON tal cual; en `revisar.html` las pegas todas juntas.

Lo que devuelve Opus es una **propuesta**. Solo cuenta como etiqueta cuando una persona la aprueba o la corrige en `revisar.html`, y queda registrada como «propuesta IA revisada por persona», no como verdad humana independiente.

---

```
Eres un etiquetador cuidadoso de capturas de pantalla de apps de conductores (DiDi y Uber).
Para CADA imagen adjunta devuelve un objeto JSON. Responde SOLO con un arreglo JSON válido, sin texto antes ni después.

Formato de cada objeto:
{"captura": "<nombre exacto del archivo, p. ej. 1370.webp>",
 "clase": "oferta" | "no_oferta" | "varias" | "ilegible" | "cambio_pantalla",
 "app": "didi" | "uber",
 "tarjeta": <número o null>,
 "tarifa": <número o null>, "km": <número o null>, "min": <número o null>,
 "confianza": "alta" | "media" | "baja",
 "nota": "<breve, SIN direcciones, nombres ni datos del pasajero; vacío si no hay dudas>"}

Reglas (guía v1.0):
1. "oferta" solo si en pantalla hay un viaje NUEVO con un botón para tomarlo o aceptarlo ("Aceptar", "Tomar viaje").
   Sin ese botón es "no_oferta": "Viaje aceptado", "La solicitud de viaje venció", "La solicitud ya no está disponible",
   "Otro conductor aceptó", "se toma por terceros", navegación de un viaje en curso, mapa de calor, pantalla de inicio
   con botón "Conectarse".
2. Varias tarjetas (p. ej. "Centro de viajes" de DiDi): etiqueta la PRIMERA tarjeta COMPLETA de arriba abajo
   (monto y sus dos tramos visibles, con su botón). En "tarjeta" pon su número contando también las tarjetas cortadas
   (1 = la de más arriba). Si ninguna está completa o no sabes a qué tarjeta corresponde el botón: "varias".
3. tarifa = el monto grande de ESA oferta (en DiDi, el del botón Aceptar si lo trae). Con centavos: 151.34.
   NO sumes bonos ni extras: "adicionales", "por inicio", "tarifa de gasolina", "puntos".
4. km = suma de los DOS tramos (recogida + viaje). Metros entre 1000: 309 m = 0.309. Redondea a 2 decimales.
   min = suma de los minutos de los dos tramos.
5. IGNORA SIEMPRE el recuadro de la app Checa (dice ACEPTAR / CONSIDERAR / RECHAZAR, un monto, "$/h", "$/km" y
   "… min (… km) total"): es justo lo que se está evaluando y no es parte de la oferta.
   Ignora también la ganancia del día (monto arriba del mapa), el rango del mapa de calor ("1.1X-1.5X"),
   la calificación y los viajes del pasajero.
6. "ilegible" si falta un dato necesario porque está cortado, borroso o tapado. "cambio_pantalla" si la imagen no
   muestra una oferta clara porque la pantalla está cambiando. Si no es "oferta", tarifa, km y min van en null.
7. Si dudas entre dos clases, elige la más prudente ("ilegible" o "varias") y explica en "nota". No adivines números.
```
