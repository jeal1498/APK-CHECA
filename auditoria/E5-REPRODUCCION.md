# E5 — Reproducción histórica y pruebas automáticas

**Estado:** implementado y ejecutado en el contenedor (Kotlin 1.9.24 en JVM, Java 21). Evaluación ciega **ejecutada**, pero contra etiquetas **heredadas** (no hay humanas: ver E4). No es precisión de producción.

## Piezas (`auditoria/reproduccion/`)
| Archivo | Qué hace |
|---|---|
| `reproducir.sh <commit\|actual\|DIR>` | compila `Parser.kt`, `Moneda.kt`, `Tarjetas.kt` de esa versión con `Reproducir.kt` y escribe `predicciones.jsonl` + `corrida.json` (sha256 de fuentes, entradas y predicciones, versión de Java y Kotlin) |
| `Reproducir.kt` | corre el lector sobre una carpeta de entradas **sin respuestas** (rechaza cualquier archivo con `---`) |
| `evaluar.py` | `preparar` (separa entradas y etiquetas), `sellar` → `comprometer` → `revelar` (orden impuesto por `registro.json` con huellas), `comparar` (regresión por caso; sale con 1) |
| `evaluacion_ciega.sh` | protocolo completo; las etiquetas salen de la carpeta del lector antes de correrlo |
| `mutaciones_lector.py` | 5 mutaciones del lector sobre copias; cada una debe producir regresiones |
| `sinteticos/` | 4 casos construidos para reglas que el banco no cubría (marcados `sintetico_regla`) |

## Resultados (hechos verificados por ejecución, `evidencias/e5/`)
- **Determinismo:** dos corridas del mismo commit en carpetas distintas → mismo sha256 de predicciones (`9534b19e…`).
- **Reconciliación:** sobre el banco, las fallas del corredor son **idénticas** a las de `Banco.kt` (271/316, mismas 45).
- **Evaluación ciega 10.81** (`ciega-10.81/`): 319/365 de acuerdo con las etiquetas (banco 271/316, `ofertas/` 44/45, sintéticos 4/4). Sin los 45 casos circulares: 281/320. Uber 136/174, DiDi 173/181. Categorías del banco: 233 aciertos, 17 errores de datos, 24 falsos negativos, 4 falsos positivos, 38 verdaderos negativos; «precisión cuando sale pop-up» 0.917 (IC95 0.877–0.945) **contra etiquetas heredadas**.
- **Histórico:** 10.78 (`cf46798`) → 10.81: 66 casos cambian, **0 regresiones**, 60 mejoras (259 → 319) (`comparacion_10.78_a_10.81.json`).
- **Mutaciones:** con solo el banco, 2 de 5 sobrevivían (ofertas caídas y tope de $/km: el banco no las cubre). Con `ofertas/` y los sintéticos: **5/5 detectadas** (`mutaciones_lector.log`).
- Pruebas del protocolo: `auditoria/pruebas/test_e5_reproduccion.py` → 10 OK (orden, manipulación de etiquetas o predicciones, registro de un solo uso, regresión/mejora, tolerancias, determinismo, rechazo de entradas con respuesta, huella del informe).
- Se corrigió un defecto propio durante la prueba: el informe guardaba el registro ya modificado y su huella no correspondía.

## Limitaciones
1. **Etiquetas no humanas:** todo «acuerdo» es contra IA, el propio lector (45 circulares), líneas o reglas sintéticas. La evaluación ciega humana requiere E4 (bloqueo del propietario).
2. **Contaminación:** los 316 casos y `ofertas/` se usaron para desarrollar el lector. No hay conjunto de evaluación independiente todavía.
3. **Java:** `of_didi-pasajero-cancelo` falla con Java 21 y pasa con Java 17 (CI) por `\b` con «ó» (ya documentado en `docs/banco-lector.md`). `corrida.json` registra la versión de Java para no comparar corridas de JVM distintas.
4. **Sin Android:** el corredor usa la JVM; no se ejecutaron pruebas instrumentadas ni JUnit/Gradle reales (requiere CI con autorización).
5. Solo se mide el lector sobre líneas ya guardadas: no OCR, ni elección de tarjeta desde la imagen, ni duplicados ni latencia (esos los traerá la telemetría de E3).

## Auditoría de cierre (16-nov-2026)
- **`comparar` ahora exige las mismas condiciones** (A4): recibe la `corrida.json` de base y candidata y se niega si difieren las entradas (sha256 del corpus), el número de casos, la JVM o el compilador, o si un archivo de predicciones no es el de su corrida. El resultado guarda esas condiciones.
- **Diferencia 271 vs 277 investigada.** El seguimiento cita 277/316 «tras normalizar U+00A0 en el lector del banco» (rama `trabajo/e5-reproduccion`, que no existe en GitHub ni en este entorno). Hecho verificado: **7 casos del banco traen espacios duros (NBSP)** en sus líneas (0293, 0296, 0297, 0300, 0304, 0305, 0309, todos Uber) y **los 7 fallan** con «no leyó la oferta». Seis están etiquetados «respuesta correcta según el lector del teléfono» (`ia_mal`): el teléfono sí los leyó y la JVM no. O el teléfono normaliza el NBSP antes del lector, o el NBSP entró al guardar el caso. No se puede decidir sin el origen; **no se tocó el lector ni los casos**. Mientras tanto 271 es el número del lector tal cual sobre los archivos tal cual, y 277 era el número con un corredor que normalizaba.
- **Reproducción nueva** de la evaluación ciega, de la comparación 10.78 → 10.81 y de las 5 mutaciones: `evidencias/cierre/e5/` (mismos resultados que el 15-nov: 319/365, 0 regresiones y 60 mejoras, 5/5).
- El sellado es evidencia de orden por huellas, no una firma: quien controle los archivos puede rehacer todo el registro. Sirve contra el error, no contra la mala fe.
