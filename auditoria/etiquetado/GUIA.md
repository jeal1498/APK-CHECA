# Guía de etiquetado humano · versión 1.0

## Cómo se hace (página `etiquetar.html`)
1. Baja las capturas «<id>.webp» a una carpeta de tu computadora (Supabase → Storage → `capturas`). No las subas a ningún otro lado: traen datos de pasajeros.
2. Abre `auditoria/etiquetado/etiquetar.html` en Chrome o Edge (doble clic; funciona sin internet).
3. Escribe tu seudónimo (p. ej. `lalo`), deja «Cuántas capturas» en 120 y elige la carpeta.
4. Por cada imagen: qué es, la app y, si es oferta, tarifa, km y min. «Guardar y siguiente» o Enter. Se guarda en tu navegador por si cierras.
5. Al terminar, «Exportar CSV» → `etiquetas-<seudónimo>.csv`. La otra persona hace lo mismo **por separado**, con la misma carpeta y el mismo número.
6. Junta los dos CSV y corre `python3 auditoria/etiquetado/etiquetado.py importar --manifiesto <carpeta> --hoja etiquetas-a.csv --hoja etiquetas-b.csv --salida <dir>` (o pásamelos: solo traen números, sin imágenes).

## Variante: revisar propuestas de la IA (`revisar.html`)
Decidido por el propietario el 16-nov para ir más rápido. Opus propone las etiquetas (`PROMPT-OPUS.md`) y una persona aprueba, corrige o rechaza cada una en `revisar.html`.
- Un ~10 % (mínimo 5) de las propuestas se **alteran a propósito** sin avisar: si se aprueban, la revisión no es confiable y esas filas no cuentan.
- Se importa con `etiquetado.py importar-revision --manifiesto <carpeta> --hoja revision-<seudónimo>.csv --salida <dir>`.
- Queda registrado como **«propuesta IA revisada por una persona»** (`revisor_version = revision_propuesta_ia`): un solo revisor da `referencia_humana`, **no** `referencia_doble`. Pesa menos que la doble revisión a ciegas y así se informa.
- Las capturas se envían a un proveedor de IA (Anthropic): autorizado por el propietario el 16-nov.

Para quien revisa capturas. Una fila de la hoja = una captura. **Solo mira la imagen**: la hoja no trae lo que leyó Checa ni la IA, a propósito.

## Qué anotar
| Columna | Qué poner |
|---|---|
| `clase` | `oferta`, `no_oferta`, `varias`, `ilegible` o `cambio_pantalla` |
| `tarifa` | el monto grande que paga **esa** oferta (el del botón «Aceptar» en DiDi). Con centavos: `151.34` |
| `km` | suma de los dos tramos (recogida + viaje). `720 m` = `0.72` |
| `min` | suma de los dos tramos en minutos |
| `tarjeta` | solo si hay varias tarjetas: cuál es la del botón (1 = la de arriba) |
| `confianza` | `alta` si se lee sin dudas; `media`; `baja` si adivinas algo |
| `nota` | breve, sin direcciones ni nombres |

## Reglas
1. **No es oferta** (`no_oferta`): «Viaje aceptado», «La solicitud de viaje venció», «La solicitud ya no está disponible», «se toma por terceros», «Otro conductor aceptó», navegación de un viaje en curso, mapa de calor, pantallas sin botón de aceptar.
2. **Varias** (`varias`): hay más de una tarjeta y no puedes saber cuál es la del botón «Aceptar». No mezcles datos de tarjetas distintas. Si sí sabes cuál es, `oferta` + `tarjeta`.
3. **No sumes** bonos («$9.00 adicionales», «+$9.75 por inicio»). En Uber, `+$62.00 incluido` ya va dentro de la tarifa.
4. **Ilegible**: la imagen está cortada, borrosa o tapada por el pop-up de Checa en un dato que hace falta.
5. **Cambio de pantalla**: la imagen muestra otra cosa que la oferta del caso (ya cambió). No adivines.
6. Si dudas entre dos clases, elige la más prudente (`ilegible` o `varias`) y explica en `nota`.

## Ejemplos
- DiDi `$151.34`, `5min (1.7km)`, `37min (13.2km)` → `oferta`, 151.34, 14.9, 42.
- Uber `$48.73`, `A 5 min (1.6 km)`, `Viaje: 8 min (3.2 km)` → `oferta`, 48.73, 4.8, 13.
- DiDi con dos tarjetas completas y el botón bajo la segunda → `oferta`, datos de la segunda, `tarjeta` 2.
- «La solicitud de viaje venció» sobre la tarjeta → `no_oferta`.

## Doble revisión y desacuerdos
- Dos personas distintas etiquetan **la misma hoja por separado**, sin verse.
- `etiquetado.py importar` compara: si coinciden (tarifa ±0.01, km ±0.05, min ±0.5, misma clase y tarjeta) el caso entra a la **referencia**; si no, va a `conflictos.csv`.
- Un conflicto lo resuelve una tercera revisión (otra persona) con su propia fila; mientras no haya acuerdo **el caso queda en conflicto**, no se elige un lado.
- Revisores con seudónimo (`revisor_a`, `lalo`…), nunca nombre ni correo.

## Qué NO es verdad humana
Las respuestas del banco actual (316) salieron de juicios de IA, del propio lector o de leer líneas (ver `banco_origen.csv`): **ninguna** es referencia humana. Tampoco lo es «la IA y el lector coinciden».
