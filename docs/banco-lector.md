# Banco del lector: cómo se mide el avance

Tres números. Ninguno es opinión.

| Número | Qué mide | Dónde sale |
|---|---|---|
| **Banco** | Aciertos del lector en 235 ofertas reales con respuesta confirmada. El examen no cambia, así que las versiones se comparan entre sí. | `docs/banco/calificar.sh` y la prueba `BancoTest` en «Compilar flavor play» |
| **Calle** | Por versión de la app: lecturas revisadas, % que coincidió, pop-ups falsos, rescates y fallas confirmadas por cada 100. | Vista `calidad_version` en Supabase |
| **Conductor** | Reportes «Leyó mal» por cada 100 lecturas (columna `reportes_por_100`). | La misma vista; los casos están en `errores_lector` con `tipo = 'reporte'` |

## Marcador del banco

| Versión | Aciertos | % |
|---|---|---|
| 10.69 | 175 de 235 | 74.5 |
| 10.72 | 175 de 235 | 74.5 |
| 10.74 | 177 de 235 | 75.3 |
| 10.76 | 185 de 235 | 78.7 |
| 10.77 | 185 de 235 | 78.7 (sin cambios en el lector) |

Medido aquí con Java 21. En Actions (Java 17) sale un acierto más: `0210-didi.txt` depende de cómo trata `\b` la «ó» de «canceló»; en Android también acierta.

## Reglas

- Un caso entra al banco solo con respuesta confiable: veredicto `ia_bien` (vale la revisión), `ia_mal` con nota «lector» (vale el lector) o revisión manual de las líneas. Los `dudoso` no entran.
- Las líneas son las que guarda `errores_lector`: cortas y con números (sin direcciones). No son todo lo que vio el teléfono.
- `banco/minimo.txt` es el piso: `BancoTest` falla si el lector baja de ahí. Cuando un arreglo sube los aciertos, se sube ese número y se agrega la fila al marcador.
- El banco solo mide errores ya conocidos. Un formato nuevo de Uber o DiDi aparece primero en `calidad_version`.
- Las pruebas de `ofertas/` siguen igual: esas sí exigen 100 %.

## Cómo agregar casos

1. En la revisión semanal, pon el veredicto a los casos nuevos de `errores_lector`.
2. Crea `banco/<id>-<app>.txt` con el mismo formato que `ofertas/` (líneas, `---`, `tarifa= km= min=` o `nada`).
3. Corre `docs/banco/calificar.sh` y anota el marcador. Con casos nuevos el total cambia: anota desde qué versión.

## Qué falla hoy (10.76, 50 casos)

- 23: el lector no entiende la oferta (rescates).
- 18: kilómetros mal (tarjetas de DiDi mezcladas, tramos iguales, tramo partido, «/km» ajeno).
- 4: pop-up cuando no era oferta.
- 3: minutos mal.
- 2: tarifa equivocada.

## Revisión con capturas (pendiente, lunes 16 nov)

**Para qué:** hoy la respuesta correcta de cada caso del banco sale de texto y de juicios, no de la pantalla. Las capturas (desde la 10.72, WebP de la zona de Uber/DiDi) dejan confirmarla viendo la oferta real. El resultado es un banco nuevo y la precisión del lector en los casos dudosos.

**Desde la 10.78** el almacén también trae capturas de ofertas normales, como `ok-<color>-<tarifa>-<km>-<min>-<hora>.webp` (lo que mostró el pop-up). No se enlazan con `errores_lector`.

**Qué se revisa:** todas las capturas de la versión más reciente que haya ese día. Solo existen para casos de `errores_lector` (diferencia, desacuerdo, rescate): no hay de las ofertas donde los lectores coincidieron, así que el número mide casos difíciles, no la precisión en todas las ofertas.

**Cómo:**
1. Lista: `storage.objects` del almacén `capturas` unido con `errores_lector` (`name = id || '.webp'`) para ver versión y tipo. Quitar duplicados (mismas líneas leídas o misma tarifa, km y min).
2. Bajarlas: el almacén es **privado** y no hay llave de servicio en la sesión. Se despliega una función temporal que entrega la imagen por id solo con una clave aleatoria en un encabezado (`SUPABASE_SERVICE_ROLE_KEY` ya está en su entorno), se bajan con `curl` y se **desactiva** (se vuelve a desplegar como stub que responde 410). Ya existe `tmp-capturas-9f3k` desactivada: se despliega encima con clave nueva. No hay herramienta para borrar funciones: Eduardo la quita en el panel si quiere.
3. Las imágenes **no van al repositorio**: traen direcciones y nombres. Se convierten a PNG con `ffmpeg` en el directorio de la sesión y se borran al terminar. Al final solo entran al banco las líneas cortas con números.
4. Por cada una: tarifa, km y min que se ven, contra lo leído por el teléfono y por la revisión. Veredicto: lector, revisión, los dos, ninguno o «no era oferta». Si la oferta cambió entre la captura y la lectura, se marca dudosa y no entra.
5. Para borrar capturas de Supabase se usa la API de Storage (la función temporal con `DELETE /storage/v1/object/capturas` y `prefixes`); borrar filas de `storage.objects` por SQL no quita el archivo.
6. Agregar los casos confirmados a `banco/` (o a un banco nuevo), subir `minimo.txt`, corregir el lector, medir con `docs/banco/calificar.sh` y anotar el marcador arriba.

Las capturas se borran a los 7 días (`limpiarCapturas`, al azar en 2 % de las llamadas): las de la 10.77 vencen desde el 20 nov.
