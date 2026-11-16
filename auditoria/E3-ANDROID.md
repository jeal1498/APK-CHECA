# E3 — Android 10.81 con trazabilidad

**Estado:** implementado; núcleo compilado y probado en JVM; pegamento Android **no compilado** (sin SDK aquí) y **sin probar en teléfono**. Apagado por defecto. No publicado.

## Qué cambia (y qué no)
| Archivo | Cambio |
|---|---|
| `Traza.kt` (nuevo) | Kotlin puro: `Evento` con las mismas reglas de coherencia que el servidor, JSON determinista, `Redaccion`, `ColaTraza` acotada y persistente, `Reintento`, `RespuestaTraza` |
| `TrazaAndroid.kt` (nuevo) | hilo «traza», archivo `filesDir/traza.txt`, espera de 3 s a la captura del mismo fotograma, envío por lotes |
| `LectorOcr.kt` | crea `fotograma` (UUID + hora) al empezar cada imagen y lo fija solo durante SU callback |
| `Guardado.kt` | `guardar(…, fotograma)` avisa el sha256 del WebP de ese fotograma (los mismos bytes que se suben) |
| `RutaService.kt` | `trazar()` en las 4 salidas de `mostrarOferta` (oferta mostrada, y 3 tipos de duplicada) |
| `Cuenta.kt` | `enviarTrazas()` → `registrar-lectura` con el JWT del conductor |
| `Prefs.kt` / `MainActivity.kt` | `trazabilidad` (apagado) e interruptor «Registro técnico de lecturas (prueba)» en Guardar pantallas |
| `Novedades.kt`, `build.gradle.kts` | 10.81 / versionCode 161 |
| **`Parser.kt`, `Tarjetas.kt`, `Moneda.kt`** | **sin cambios** (guardia `herramientas/guardia_parser.py` + `GuardiaParserTest`; banco 271/316 antes y después) |

## Correlación exacta con la captura
1. `LectorOcr.procesar` crea el id del fotograma **antes** de mandar esa imagen a ML Kit y lo guarda en una variable local.
2. En el callback de ESA imagen fija `TrazaAndroid.fotograma` y lo borra al terminar (`finally`). `procesarGrupo → mostrarOferta → trazar` y `Guardado.guardar` corren dentro de ese callback (hilo principal), así que leen el id de su propia imagen, no «la última».
3. El sha256 del WebP llega por `anotarCaptura`; el evento espera ≤ 3 s. Como `Guardado.tomarReciente` sube **ese mismo arreglo de bytes**, el sha256 del objeto en Storage = `captura_sha256` del evento: la unión se puede comprobar después por huella, sin ventanas de tiempo.
4. Sin captura (Guardar pantallas apagado, lector de texto) → `captura_vinculo = sin_captura`. Nunca se rellena con otra imagen.

Pendiente (línea L2, release aparte): que la subida de la captura también use el id del fotograma en el nombre.

## Plan de datos
| Dato | Se guarda | Motivo |
|---|---|---|
| Ids de evento y fotograma (UUID aleatorios) | sí | correlación |
| App, versión, versión del lector, lector, resultado, motivo | sí | trazabilidad |
| Tarifa, km, min, recogida, ⚡, moneda, semáforo | solo si `resultado = oferta` | reconstruir la decisión |
| Líneas del OCR | **solo las piezas de dato** de cada línea, por lista blanca (montos, número + min/km/m/h/x, «/km», ⚡ y 16 palabras fijas; máx. 30×60) + sha256 de todas | la dirección de DiDi viene en la misma línea que el tramo: se quita dentro de la línea; tampoco pasan números sueltos |
| Teléfono (fabricante y modelo), Android, orientación | sí | segmentación por dispositivo |
| Usuario | del JWT, en el servidor | borrado en cascada al eliminar la cuenta |
| Imagen | **no** (solo su sha256) | la imagen sigue el flujo existente de 7 días |
Retención: 30 días (`expira`). La purga existe y **no está programada**.

## Fallos, cola y reintentos
Cola de 500 eventos / 512 KB; si se llena se descartan los más viejos (contados). Respuestas: ver tabla de `auditoria/README.md`. Sin red → reintento con espera 30 s × 2ⁿ (máx. 30 min, con azar). Sin sesión o función apagada → pausa de 15 min. Ninguna excepción sale hacia el lector (`try/catch` en cada entrada).

## Costo (medido solo en la JVM del contenedor, no en teléfono)
`evidencias/e3/medicion_jvm.txt`: ~41 µs por evento con la cola casi vacía (caso normal con red); ~0.9 ms con la cola llena (reescribe 300 KB). Todo en el hilo «traza», fuera del hilo principal salvo copiar datos. Un evento pesa ~560 bytes.

## Pruebas ejecutadas
- `herramientas/jvm/probar.sh` → `TrazaTest` 14 + `GuardiaParserTest` 1: **15 OK** (Kotlin 1.9.24 en JVM, con un sustituto mínimo de JUnit).
- `herramientas/jvm/compilar_traza_android.sh` → `TrazaAndroid.kt` compila contra **stubs de tipos** (no es el SDK real).
- Cruce: `evidencias/e3/muestras_kotlin.json` (7 eventos generados por `Traza.kt`) pasan el contrato TS y se guardan en Postgres local (`test_muestras_de_android_pasan_contrato_y_base`).
- Banco: 271/316 con el código base (`a7a732f`) y con 10.81 (`evidencias/e3/banco_*.txt`).

## No probado / pendiente del propietario
1. **Compilación Android real** (`play-compilar.yml`, se dispara al llegar `app/**` a `main` o en un PR a `main`): requiere autorización de push/PR.
2. **Teléfonos reales** (matriz mínima): Android 11+ por accesibilidad y ≤ 10 por MediaProjection; Samsung, Xiaomi y Motorola; vertical y horizontal dividida; con y sin red; con Guardar pantallas encendido y apagado. Medir batería y latencia del pop-up con el interruptor apagado y encendido.
3. **Aviso de privacidad y Seguridad de los datos de Play** actualizados y aprobados antes de encender en producción.
4. Aplicar `auditoria/sql/001–002` y desplegar `registrar-lectura` (autorización expresa).

## Auditoría de cierre (16-nov-2026)
- **Defecto alto corregido (privacidad):** la redacción original dejaba pasar la línea entera si traía un dato. En DiDi el tramo y la dirección van en la misma línea («(4 min 1.4 km) Calle 46 612,»), así que **salían direcciones** (358 de 1,875 líneas del corpus traían palabras ajenas). Ahora `Redaccion` es una lista blanca de piezas y el servidor exige la misma gramática (`contrato.ts` › `lineaRedactada`). Sobre las 1,983 líneas reales de `banco/` + `ofertas/`: salen 1,851, **0 con palabras ajenas o números sueltos**, y el servidor acepta todas (`auditoria/pruebas/test_e3_redaccion.py`, `evidencias/cierre/e3/redaccion_corpus.json`).
- La prueba cruzada encontró además una diferencia Kotlin/servidor («ı» sin punto del OCR): corregida.
- El multiplicador fuera de 1–20 ya no se manda (el servidor rechazaba el evento entero).
- `GuardiaParserTest` dependía del directorio de trabajo: ahora busca la raíz del repo hacia arriba.
- **Fuera del alcance de E3, sin tocar (hallazgo previo):** `Confirmador.filtrar` (en producción desde antes) manda a `confirmar-oferta` esas mismas líneas con dirección, y quedan en `errores_lector.lineas`, `capturas_ok.lineas` y en los casos del banco versionados. Contradice «nunca direcciones» de `confirmar-oferta/index.ts` y la declaración de datos. Decisión del propietario.
- **Compilación Android real: sigue SIN hacerse.** Aquí hay Gradle 8.14.3 y se bajaron las herramientas de línea de comandos del SDK, pero instalar plataformas exige aceptar las licencias de Android en nombre del propietario y esa acción fue denegada. `TrazaAndroid.kt` y los enganches siguen compilados solo contra stubs.
- Núcleo: 17 pruebas en JVM (antes 15).
