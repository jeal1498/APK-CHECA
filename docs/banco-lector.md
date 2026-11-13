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
