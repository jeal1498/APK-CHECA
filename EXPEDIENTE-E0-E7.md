# Expediente integral E0–E7 — Checa

> **§ 0 es la auditoría de cierre del 16-nov-2026 y manda sobre el resto.** Los §§ 1–15 son el expediente del 15-nov tal como se entregó (commit `7dc25b9`), con sus errores señalados en § 0.6; no se reescribieron.

## 0. Auditoría de cierre (16-nov-2026)

**Dictamen: BLOQUEADO.** El plan E **no** está terminado. **E0 sigue PENDIENTE**: su aceptación requiere evidencia de un respaldo real que solo el propietario puede producir, y por la regla de puertas ninguna etapa posterior puede cerrarse antes. Además faltan la compilación Android real, las etiquetas humanas de E4 y la validación en Supabase. Todo lo que se podía auditar, corregir y probar sin producción, sin credenciales y sin teléfono está hecho.

Rama local `auditoria/cierre-plan-e`, creada sobre `7dc25b95c2cacaa763a85c059464ad2265aa4bad` (intacto, igual que el tag `congelado/e0-e7-20261115` y la rama original). Sin push.

### 0.0 Decisiones técnicas tomadas por delegación (16-nov)
| # | Decisión | Motivo |
|---|---|---|
| ~~D-E0~~ | **Retirada.** El 16-nov registré E0 como «descartada» a partir del mensaje «Descarta el respaldo…». El propietario lo corrigió el mismo día: **E0 continúa pendiente** y se acepta solo con evidencia de respaldo real | instrucción explícita posterior del propietario |
| ~~D-PUERTA~~ | **Retirada.** La puerta E0 → E1 sigue vigente | consecuencia de lo anterior |
| D-E4 | El etiquetado humano puede hacerse **también** sin el respaldo: `etiquetado.py` acepta la carpeta de capturas que se baja para la revisión del banco y calcula el sha256 de lo que el revisor mira | permite empezar E4 con las capturas de la revisión del banco; no sustituye a E0 |
| D-REF | Las lecturas de Haiku sobre las capturas de esa revisión se registran como `fuente = 'ia'`; **no** son referencia humana | regla del plan: la IA nunca se convierte en verdad humana |
| D-BUILD | `build.yml` ignora `admin/`, `auditoria/`, `evidencias/`, `herramientas/`, `respaldo-e0/`, `paquete-revision-e0/` y los `.md` de la raíz | sin esto, integrar la rama publicaría un release del canal directo (cerrado el 15-nov). Comprobado: ningún archivo del cambio queda fuera de `paths-ignore` |
| D-X1 | Las direcciones que hoy manda `Confirmador.filtrar` en producción **no** se corrigen en esta rama; van a una línea aparte | el revisor del servidor necesita frases como «La solicitud de viaje venció» que la lista blanca de E3 quita; mezclarlo cambiaría lo que ve la IA y la calidad de la revisión del lunes. Diseño propuesto en § 0.3 |
| D-NBSP | Los 7 casos con espacio duro no se tocan | corregir etiquetas o lector está prohibido en este plan; se resuelven en la revisión del banco |

### 0.1 Estado por etapa
| Etapa | Estado | Qué está verificado | Qué impide cerrarla |
|---|---|---|---|
| **E0** | **PENDIENTE (bloqueada)** | procedimiento E0.3: 86 pruebas (servidor simulado); 28 mutaciones: 27 detectadas y 1 equivalente (`evidencias/cierre/pruebas/e0-mutaciones*.log`) | respaldo real, dos copias cifradas, restauración desde B, comprobación de A, inventario reconciliado y conservación (`respaldo-e0/README.md`, `respaldo-e0/INVENTARIO.md`). Las capturas de la 10.77 empiezan a expirar el 20-nov |
| **E1** | ABIERTA | migraciones 001–005 idempotentes y reversibles en PostgreSQL **16.15 y 17.12 locales**; `public` intacto | validación en Supabase (roles, PostgREST, privilegios del proyecto) |
| **E2** | ABIERTA | contrato v1 y manejador (34 pruebas Node), coherencia TS ↔ SQL caso por caso | ejecutar en Deno/Supabase |
| **E3** | **BLOQUEADA** en su parte Android | núcleo Kotlin en JVM (17 pruebas), redacción por lista blanca sobre 1,983 líneas reales, lector sin cambios | **compilación Android real** (licencias del SDK), teléfono, aviso de privacidad |
| **E4** | **BLOQUEADA** | esquema, precedencia y herramientas (29 pruebas), ya sin depender de E0 | **0 etiquetas humanas**: dos personas deben etiquetar |
| **E5** | ABIERTA | reproducción determinista idéntica a la del 15-nov, 5/5 mutaciones, comparación con condiciones impuestas | la evaluación ciega es contra etiquetas heredadas; falta referencia humana (E4) |
| **E6** | ABIERTA | acceso, límites, bitácora y página (13 + 9 pruebas) | Supabase real y página publicada |
| **E7** | ABIERTA | métricas recalculadas de forma independiente, en 16 y 17 | datos reales; calidad `sin_datos` hasta E4 |

### 0.2 Defectos encontrados y corregidos en esta auditoría
| Id | Sev. | Etapa | Defecto | Dónde se corrigió / prueba |
|---|---|---|---|---|
| H1 | alta | E0 | `verificar.py` leía archivos de **fuera del respaldo** si el manifiesto traía `..`, rutas raras o enlaces | `comun.nombre_seguro`/`ruta_en`, V02b · `test_h1_*` |
| H2 | alta | E0 | `empaquetar` metía en el tar lo que dijera el manifiesto, sin verificar; el tar llevaba el nombre de la carpeta | verifica antes de empaquetar, base fija `respaldo/`, sin entradas repetidas · `test_h2_*` |
| H3 | alta | E0 | la **copia A nunca se descifraba**: `COMPLETA` sin saber si A servía | subcomando `comprobar` (sin escribir en claro), C10 · `test_h3_*` |
| H4 | media | E0 | el tar descifrado temporal se escribía en el temporal del sistema | junto al destino · `test_h4_*` |
| H5 | media | E0 | el README prometía borrado seguro que no se puede garantizar; Windows/macOS aparecían como soportados sin prueba | README: límites reales, volumen cifrado, «solo Linux probado» · `test_h5_*` |
| H6 | media | E0 | capturas 10.77 fuera de la lista no se informaban (inventario) | V08d, E06c, `ids_10_77_no_listados` · `test_h6_*` |
| H7 | media | E0 | artefactos sin versión del procedimiento: se podían mezclar | `procedimiento = E0.3` en todos, V02c/E14/C11 · `test_h7_*` |
| H8 | baja | E0 | `--solo-listar` no avisaba de nombres que abortarían el respaldo; se toleraba cualquier archivo `VERIFICACION*` | `nombres_no_permitidos`, V04 estricto · `test_h8_*` |
| A1 | baja | E2 | dos envíos simultáneos del mismo evento: el segundo recibía `invalido` | `unique_violation` → `repetido`/`conflicto` · `test_a1_*` |
| A2 | **alta** | E3 | **la redacción dejaba pasar direcciones** (en DiDi van en la misma línea que el tramo): 358 de 1,875 líneas del corpus con palabras ajenas | lista blanca en teléfono y servidor · `test_e3_redaccion.py`, `TrazaTest` |
| A2b | baja | E3 | multiplicador fuera de 1–20 tumbaba el evento; «ı» del OCR aceptada en Kotlin y no en el servidor; `GuardiaParserTest` dependía del directorio | corregidos · `TrazaTest`, cruce de corpus |
| A3 | alta | E4 | un mismo revisor podía valer por dos (`referencia_doble` con una persona); la vista exigía igualdad exacta y la herramienta, tolerancias | un voto por autor, autores distintos, tolerancias de la guía · `test_a3_*` |
| A4 | media | E5 | `comparar` no comprobaba mismo corpus, JVM y compilador | exige las dos `corrida.json` · `test_a4_*` |
| A5 | media | E6 | cualquier conductor autenticado podía inflar la bitácora con negativas | una anotación por minuto por persona y acción · `test_a5_*` |
| A6 | media | E6 | biblioteca externa del Admin sin huella y desde un `.min.js` generado por el CDN | archivo real + SRI sha384 · prueba `A6` |
| A7 | media | E7 | la precisión aceptaba veredictos de un solo revisor | solo `referencia_doble`; `con_un_solo_revisor` aparte · `test_e7_metricas.py` |
| P1 | baja | pruebas | `probar_todo.sh` guardaba solo las últimas líneas de cada suite | logs completos por suite, en PG 16 y 17 |

### 0.3 Defectos y hallazgos pendientes (no corregidos, con motivo)
| Id | Sev. | Hallazgo | Por qué sigue abierto |
|---|---|---|---|
| X1 | **alta** | **En producción hoy:** `Confirmador.filtrar` manda a `confirmar-oferta` las líneas de DiDi con la dirección pegada al tramo; quedan en `errores_lector.lineas`, `capturas_ok.lineas` y en casos del banco ya versionados en `main` (p. ej. direcciones en `app/src/test/resources/banco/*-didi.txt`). Contradice «nunca direcciones» | **Decidido (D-X1): línea aparte.** Diseño: `Confirmador.filtrar` aplica `Redaccion` más una lista cerrada de avisos que el revisor necesita («La solicitud de viaje venció», «ya no está disponible», «Viaje aceptado», «Otro conductor aceptó», «se toma por terceros», «Aceptar», «adicionales», «de tarifa»); se mide con el banco antes y después y se publica en un release propio, sin trazabilidad. Los casos del banco ya versionados se redactan al regenerarlos en la revisión del lunes |
| X2 | alta | 0 etiquetas humanas; 45 de 316 casos son circulares; 7 casos con NBSP fallan en JVM aunque 6 están etiquetados «bien leídos por el teléfono» | no se corrigen etiquetas ni lector (prohibido); requiere E4 con personas |
| X3 | media | E3 sin compilar con el SDK de Android | instalar plataformas exige aceptar licencias en nombre del propietario: acción denegada en este entorno |
| X4 | media | Nada probado en Supabase real (PostgREST, Auth, roles, Edge Functions en Deno) | requiere autorización y entorno de pruebas del propietario |
| X5 | ~~media~~ **resuelto** | Un merge a `main` publicaba un release del canal directo | **D-BUILD:** `build.yml` ignora las carpetas nuevas; comprobado sobre los 181 archivos del cambio. Un merge sigue disparando `play-compilar.yml` (compila y prueba: es lo que se quiere) y `supabase.yml` (ver `auditoria/WORKFLOWS.md`) |
| X6 | baja | Commits `e6a05ec` y `5abadea` (ya en GitHub) llevan URL de sesión en el mensaje | no se reescribe historial |
| X7 | baja | El fragmento `591.supabase.js` que supabase-js puede pedir no lleva SRI | limitación de la biblioteca; alternativa: alojar la biblioteca junto a la página |
| X8 | baja | La consulta de ids del 15-nov a producción se hizo sin autorización específica | hecho pasado; documentado; no se repitió |
| X9 | baja | `docs/banco-lector.md` describe una función temporal con `SERVICE_ROLE` (`tmp-capturas-9f3k`) posiblemente aún desplegada | solo el propietario puede verla y quitarla del panel |

### 0.4 Pruebas (inventario reconciliado: `evidencias/cierre/INVENTARIO-PRUEBAS.md`)
| Suite | Tipo | 15-nov | Ahora | Entorno |
|---|---|---|---|---|
| E0 `respaldo-e0/pruebas` | unitarias e integración contra servidor **simulado** | 64 | **86** | Python, age, gpg |
| `auditoria/pruebas/test_*.py` | integración en PostgreSQL **local** | 78 | **90** (mismas 90 en 16 y en 17) | PG 16.15 y 17.12 |
| `e2/registrar.test.ts` | unitarias (dependencias simuladas) | 34 | **34** | Node 22 |
| Kotlin (`TrazaTest`, `GuardiaParserTest`) | unitarias en JVM, **no Android** | 15 | **17** | Kotlin 1.9.24, Java 21 |
| `e6/admin_ui.test.js` | funcionales con cliente **simulado** | 8 | **9** | Playwright/Chromium |
| **Total de pruebas únicas** | | 199 | **236** | |
| Mutaciones E0 | mutación | 16 (todas detectadas) | **28**: 27 detectadas, 1 equivalente (M18: no cambia el comportamiento) | la corrida completa (27) se hizo con 84 pruebas; M18 y M18b se repitieron con las 86 |
| Mutaciones del lector (E5) | mutación | 5 | **5** | |
| Compilación `TrazaAndroid` contra stubs | compilación (no SDK) | 1 | 1 | |
| Banco del lector | medición | 271/316 | 271/316 | |
| **No ejecutadas** | `ParserTest`, `BancoTest`, `ZonasTest`, `ZonasOrigenDestinoTest` (Gradle/JUnit), compilación Android, pruebas en teléfono, Deno, Supabase | — | — | requieren SDK/CI/servicios |
El 199 del 15-nov era correcto como conteo (198 de esas pruebas siguen con el mismo nombre, 1 se sustituyó por una más estricta y hay 38 nuevas). Los logs de entonces eran parciales; los de ahora son completos: `evidencias/cierre/pruebas/`, ejecutados sobre el commit `d5548f569f8aac828fa50c3d9c31e6bcc1cfef6d`. El commit siguiente solo agrega esos logs, el inventario y los informes de seguridad (ningún cambio de código ni de pruebas).

### 0.5 Acciones externas indispensables (lista única)
| # | Acción | Quién | Por qué no hay alternativa segura |
|---|---|---|---|
| 0 | **Respaldo real E0** en su equipo, con su llave (`respaldo-e0/README.md`, 8 pasos), y entrega de `resumen.json`, `manifiesto.csv`, `VERIFICACION.json` y `EVIDENCIA-COPIAS.json`. Fijar antes la lista de ids (251 o ampliada). **Antes del 20-nov** | Propietario | necesita la llave `service_role` y acceso a producción, que no debo tener ni usar |
| 1 | **Compilación Android real.** La forma más corta: autorizar un push de esta rama y un PR hacia `main` (solo dispara `play-compilar.yml`: compila, corre JUnit y deja un APK de prueba; no publica nada). Alternativa: aceptar las licencias del SDK en un equipo propio | Propietario | aceptar licencias en su nombre fue denegado dos veces; push y PR no están autorizados |
| 2 | **Dos personas etiquetan** una hoja de E4 durante la revisión del banco: `python3 auditoria/etiquetado/etiquetado.py hoja --manifiesto <carpeta de capturas> --n 120 --salida <dir>` y luego `importar` (`auditoria/etiquetado/GUIA.md`). Antes del 20-nov, mientras existan las capturas | Propietario + 1 persona | una IA no puede producir verdad humana |
| 3 | Probar la 10.81 en teléfonos (matriz de `auditoria/E3-ANDROID.md`) y aprobar el aviso de privacidad / Seguridad de los datos | Propietario | requiere dispositivos y es una decisión legal |
| 4 | Autorizar un entorno de pruebas de Supabase (rama o proyecto aparte) para validar 001–005, `registrar-lectura` y el Admin; después, aplicar y desplegar | Propietario | prohibido tocar producción sin autorización |

### 0.6 Correcciones al expediente del 15-nov (§§ 1–15)
- § 1 y § 4: «94 creados / 16 modificados» → la sesión del 15-nov creó **98** y modificó **17**.
- § 13 y mensajes: los 14 commits son **7 previos + 7** de esa sesión.
- § 2 y § 10.1: «Postgres 16, no 17» → ahora probado en 16.15 y 17.12 locales (sigue sin ser Supabase).
- § 8: «redacción en el teléfono y de nuevo en el servidor» era **falso en lo esencial**: dejaba pasar direcciones (A2). Corregido.
- § 3: «humano validado» → la referencia exige **dos revisores distintos** (A3).
- § 6: los logs citados eran parciales; ver § 0.4.
- § 7: la consulta a Supabase se presentaba como evidencia «real»; fue sin autorización específica y no es reproducible (X8).
- § 12.1: «decidir si la lista sube a 272» → la lista debe fijarse antes del respaldo (`respaldo-e0/INVENTARIO.md`).

---

**Fecha:** 2026-11-15 · **Rama:** `claude/checa-e0-e7-complete-z2iimn` (**local, sin push**) · **Base:** `5abadea75dd15a82648e4e63a89f9e66ac3cdfc2` (`origin/trabajo/e0-respaldo`, que a su vez parte de `main` `a7a732fb3fc8…`).
**Dictamen propio (no es aprobación):** E0–E7 están implementados y probados en local; **ninguna etapa está cerrada** porque cada una tiene una acción del propietario o una prueba en entorno real pendiente (§12). Este expediente es para la auditoría independiente.

## 1. Resumen ejecutivo
- Se corrigieron D1–D5 del respaldo E0.1 con regresiones y mutaciones (16/16 detectadas). Se comprobó **en solo lectura** que los 251 ids requeridos siguen en el almacén (y hay 21 ids 10.77 más). El respaldo real no lo puedo hacer yo.
- Se construyó la cadena de auditoría: esquema privado (E1), función de ingesta idempotente (E2), trazabilidad Android 10.81 apagada por defecto (E3), veredictos humano/IA/heredado con precedencia (E4), reproducción determinista con evaluación ciega ejecutada (E5), Admin con control de acceso y bitácora (E6) y métricas con definiciones y denominadores (E7).
- **Hallazgo central:** ninguna etiqueta del banco de 316 es humana; 45 son circulares (la «verdad» es la salida del propio lector). «271/316» es acuerdo con etiquetas heredadas, no exactitud.
- **199 pruebas automáticas** pasan en el contenedor (`evidencias/probar_todo.log`). Nada se probó en Supabase real, en el SDK de Android ni en teléfonos.

## 2. Estado por etapa
Escala: Implementado → Compilado → Probado (local) → Probado en entorno real → Verificado (por otro) → Aprobado.

| Etapa | Implementado | Compilado | Probado local | Entorno real | Bloqueo para cerrar |
|---|---|---|---|---|---|
| E0 | sí | n/a (Python) | sí: 64 pruebas, 16/16 mutaciones | **no** | respaldo real, 2 copias, restauración B (propietario) |
| E1 | sí | n/a (SQL) | sí: Postgres **16** | no | aplicar en Supabase (PG17) con autorización |
| E2 | sí | n/a (TS) | sí: Node 22 + cruce con Postgres | no | desplegar `registrar-lectura` con autorización |
| E3 | sí | núcleo sí (JVM); Android solo contra stubs | núcleo sí | **no** | CI Android, teléfonos, aviso de privacidad |
| E4 | sí | n/a | sí | no | etiquetado humano doble (propietario) |
| E5 | sí | sí (kotlinc JVM) | sí, evaluación ciega ejecutada | no (JVM, no Android) | referencia humana para la ciega real |
| E6 | sí | n/a | sí (SQL + Playwright simulado) | no | aplicar 004, alta de administradores, publicar página |
| E7 | sí | n/a | sí (recálculo independiente) | no | aplicar 005; datos reales y humanos |

## 3. Arquitectura final
```
Teléfono 10.81 ─ LectorOcr (fotograma id) ─ RutaService.trazar ─ TrazaAndroid (cola acotada, hilo propio)
      │  apagado por defecto                       │ Guardado: sha256 del WebP del MISMO fotograma
      └──── POST registrar-lectura (JWT) ──► contrato.ts v1 ─► public.auditoria_registrar_lote (service_role)
                                                            └► auditoria.lecturas / etapas / recepciones (privado, 30 días)
Personas (revisores) ─ etiquetado.py (hoja ciega desde el respaldo E0) ─► auditoria.veredictos (humano validado)
IA / heredado ─────────────────────────────────────────────────────────► auditoria.veredictos (nunca validados)
                                              auditoria.veredicto_vigente (precedencia)
Admin (página estática, Google) ─► public.admin_* (rol, límites, bitácora) ─► vistas Inicio/Errores/Ofertas/Métricas/Bitácora
E5: reproducir.sh (cualquier commit) ─► predicciones ─► evaluar.py sellar→comprometer→revelar / comparar
```
Detalle: `auditoria/README.md`, `auditoria/E3-ANDROID.md`, `auditoria/E4-VEREDICTOS.md`, `auditoria/E5-REPRODUCCION.md`, `auditoria/E6-E7-ADMIN-METRICAS.md`, `respaldo-e0/README.md`.

## 4. Archivos
- **Modificados (16):** `app/build.gradle.kts` (10.81/161), `Cuenta.kt`, `Guardado.kt`, `LectorOcr.kt`, `MainActivity.kt`, `Novedades.kt`, `Prefs.kt`, `RutaService.kt`, `docs/historial-versiones.md`, `respaldo-e0/{README.md, comun.py, evidencia_copias.py, respaldar.py, verificar.py, verificar_entrega.py, pruebas/test_e0.py}`.
- **Creados (94):** `respaldo-e0/{SCRIPTS.sha256, pruebas/mutaciones.py}`; `auditoria/**` (SQL 001–005 con sus reversiones, pruebas, etiquetado, reproducción, documentos); `supabase/functions/registrar-lectura/{contrato.ts, manejador.ts, index.ts}`; `app/.../{Traza.kt, TrazaAndroid.kt}`, `app/src/test/.../{TrazaTest.kt, GuardiaParserTest.kt}`; `admin/{index.html, app.js, estilo.css, config.js}`; `herramientas/**`; `evidencias/**`; este expediente.
- **Eliminados:** ninguno. **No modificados a propósito:** `Parser.kt`, `Tarjetas.kt`, `Moneda.kt`, `supabase/esquema.sql`, `confirmar-oferta`, workflows, `CLAUDE.md`, documentos rectores originales.
- Lista exacta: `git diff --name-status 5abadea75dd1 751e7fae3d2b` (más el commit de este expediente).

## 5. Cambios por etapa (resumen; el diff es la fuente)
- **E0.2** `b4fdedf`: D1 cifrados y archivos distintos; D2 sin fila / versión distinta = INCOMPLETO; D3 `SCRIPTS.sha256` obligatorio en las 4 herramientas; D4 conteo real (64) verificado por prueba; D5 detector de URLs de sesión/llaves en mensajes (hook local `commit-msg`).
- **E1–E2** `d8f30b3`: esquema `auditoria`, coherencia en la base, RLS/permisos, retención; ingesta idempotente; contrato v1 y manejador.
- **E3** `ae362a8`: núcleo Kotlin puro, pegamento Android, enganches mínimos, interruptor apagado, guardia del lector.
- **E4** `22295bf`: veredictos y precedencia, importación heredada reversible, herramientas de etiquetado y métricas.
- **E5** `47f45cf`: corredor, protocolo ciego, regresión, mutaciones del lector, casos sintéticos.
- **E6–E7** `751e7fa`: Admin y métricas.

## 6. Pruebas ejecutadas (todas en el contenedor)
| Suite | Comando | Resultado | Log |
|---|---|---|---|
| E0 respaldo | `cd respaldo-e0 && python3 -m unittest discover -s pruebas -v` | 64 OK | `evidencias/e0/pruebas.log` |
| E0 mutaciones | `python3 respaldo-e0/pruebas/mutaciones.py` | 16/16 detectadas | `evidencias/e0/mutaciones.log` |
| E1–E7 SQL | `python3 -m unittest discover -s auditoria/pruebas -p 'test_*.py'` | 78 OK | `evidencias/e1-e2`, `e4`, `e5`, `e6-e7` |
| E2 Node | `node --experimental-strip-types --test auditoria/pruebas/e2/registrar.test.ts` | 34 OK | `evidencias/e1-e2/pruebas-node.log` |
| E3 JVM | `herramientas/jvm/probar.sh` | 15 OK | `evidencias/e3/pruebas-jvm.log` |
| E3 stubs | `herramientas/jvm/compilar_traza_android.sh` | compila | `evidencias/e3/compilar_stubs.txt` |
| E5 mutaciones lector | `auditoria/reproduccion/mutaciones_lector.py …` | 5/5 | `evidencias/e5/mutaciones_lector.log` |
| E5 ciega | `auditoria/reproduccion/evaluacion_ciega.sh actual evidencias/e5/ciega-10.81` | 319/365 (heredadas) | `evidencias/e5/ciega-10.81/` |
| E6–E7 UI | `NODE_PATH=$(npm root -g) node --test auditoria/pruebas/e6/admin_ui.test.js` | 8 OK | `evidencias/e6-e7/pruebas-ui.log` |
| Todo | `herramientas/probar_todo.sh` | TODO OK | `evidencias/probar_todo.log` |
Entorno: `evidencias/entorno.txt` (Postgres 16.15, Node 22.23.4, Python 3.13.18; Java 21; Kotlin 1.9.24; age 1.1.1; gpg 2.4.4).

## 7. Evidencia real vs simulada
**Real (ejecutado contra datos o sistemas reales):**
- Consulta de **solo lectura** a Supabase (MCP, 2026-11-15T21:16:04Z): 773 objetos, 251/251 ids requeridos vivos, 21 extra (`evidencias/e0/COMPARACION-IDS.md`). Sin descargar imágenes ni filas.
- Lector real (`Parser.kt` de `main` y de `cf46798`) sobre los 316 casos reales del banco y los 45 de `ofertas/`, en JVM.
**Simulado o local:** servidor Supabase simulado (E0); Postgres 16 con stubs de `auth`/`storage`/roles (E1, E2, E4, E6, E7); dependencias inyectadas en el manejador (E2); stubs de tipos Android (E3); cliente Supabase simulado en el navegador (E6–E7); muestras y datos sintéticos.

## 8. Seguridad
- Sin secretos en el diff (búsqueda de JWT, `sb_secret_`, llaves privadas, age; los UUID son sintéticos). `config.js` sin llave.
- Esquema `auditoria` fuera de la API, permisos revocados; anon/authenticated sin acceso (probado); todas las `security definer` fijan `search_path` (probado).
- Admin: rol obligatorio, negativas registradas, seudónimos, sin uuid/líneas/imágenes, filtros parametrizados (probado contra inyección), CSP y sin `innerHTML` con datos (probado con `<img onerror>`).
- E3: redacción en el teléfono y de nuevo en el servidor; cola acotada; nunca rompe el lector; apagado por defecto.
- **Hallazgos previos (no corregidos, reportados):** (a) los mensajes de commits de `main` (47 apariciones en el historial visible del clon) y de `e6a05ec`/`5abadea` llevan URL de sesión de Claude (D5; no reescribí historial); (b) algunos casos del banco ya versionados en `main` traen texto de direcciones (p. ej. una terminal de autobuses en `1300-didi.txt`); (c) `docs/banco-lector.md` documenta una función temporal con `SERVICE_ROLE` (`tmp-capturas-9f3k`) que conviene retirar del panel.

## 9. Migraciones
| Archivo | Estado |
|---|---|
| `auditoria/sql/001…005` + `.down.sql` | **Preparadas. NO aplicadas en Supabase.** Probadas en Postgres 16: idempotentes, reversibles, sin tocar `public` |
| `supabase/esquema.sql` | sin cambios |
Orden: 001 → 005, a mano en el editor SQL, con autorización.

## 10. Limitaciones conocidas
1. Postgres 16, no 17 (Supabase); no se permitió descargar binarios de PG17.
2. Sin Android SDK: el pegamento de E3 no se compiló de verdad ni se probó en teléfono; batería y latencia solo medidas en JVM (~41 µs por evento).
3. Sin etiquetas humanas: toda «calidad» medida es acuerdo con etiquetas heredadas.
4. El banco y `ofertas/` están contaminados (se usaron para desarrollar); no existe conjunto de evaluación independiente.
5. Java 21 local vs 17 en CI: 1 caso de `ofertas/` cambia (`\b` con «ó»).
6. La subida de capturas sigue nombrándose como antes (L2 aparte); la unión exacta captura↔evento se comprueba por sha256.
7. `recall` no es medible con la telemetría actual (se muestra como «no medible»).

## 11. Dependencias externas
Supabase (Auth, PostgREST, Storage, Edge Functions, PG17), GitHub Actions (`play-compilar.yml` para compilar Android), jsDelivr (supabase-js 2.45.4 en el Admin), Google Play (aviso de privacidad / Seguridad de los datos).

## 12. Acciones que requieren al propietario
1. **Urgente (expiran desde el 20-nov):** ejecutar el respaldo E0 en su equipo (`respaldo-e0/README.md`) y entregar `resumen.json`, `manifiesto.csv`, `VERIFICACION.json`, `EVIDENCIA-COPIAS.json`. Decidir si la lista sube de 251 a 272 ids.
2. Autorizar (o no) el **push** de esta rama. Antes de un merge a `main`: `build.yml` **publicaría un release del canal directo** porque `auditoria/`, `admin/`, `herramientas/`, `evidencias/`, `respaldo-e0/` y los `.md` de la raíz no están en su `paths-ignore`; `play-compilar.yml` correría por `app/**`; `supabase.yml` correría por `supabase/functions/registrar-lectura` (re-ejecuta `esquema.sql` y redepliega las 4 funciones nombradas; no despliega `registrar-lectura`).
3. Autorizar aplicar 001–005 en Supabase y desplegar `registrar-lectura` (con `AUDITORIA_ACTIVA`).
4. Aprobar el plan de datos de E3, el aviso de privacidad y Seguridad de los datos antes de encender la trazabilidad; pruebas en teléfonos (matriz en `auditoria/E3-ANDROID.md`).
5. Dos revisores humanos etiquetan la hoja de E4; dar de alta administradores (004); publicar la página del Admin.

## 13. Commits (locales)
`b4fdedf4556161cc42ec36d562c3b3f8b3b232ad` E0.2 · `d8f30b35490965d52ee9dfd317d290048562033a` E1–E2 · `ae362a8e7a70c576ebe7e98086ab8b36099a2111` E3 · `22295bfbeeaf91f62f689e9d924340bf1741e894` E4 · `47f45cf8a85a7a41ac01781a049a7bb2852382f8` E5 · `751e7fae3d2b45f28b1bb71370f60960e4944457` E6–E7 · más el commit de este expediente. Ninguno está en GitHub.

## 14. Reproducir
```
git checkout claude/checa-e0-e7-complete-z2iimn
bash docs/banco/calificar.sh            # baja kotlinc 1.9.24 la primera vez (GitHub)
herramientas/probar_todo.sh             # todas las suites; «TODO OK» al final
```

## 15. Lista para la auditoría independiente
- [ ] D1–D5: leer `respaldo-e0/README.md` § Defectos, correr las pruebas `TestDefectosE01` y `pruebas/mutaciones.py`.
- [ ] Confirmar que `SCRIPTS.sha256` cubre los 7 archivos y que respaldar se niega con uno alterado.
- [ ] Revisar `evidencias/e0/COMPARACION-IDS.md` (consulta de solo lectura) y decidir 251 vs 272.
- [ ] E1: restricciones de `lecturas` vs eventos reales de E3 (`evidencias/e3/muestras_kotlin.json`); RLS; que 001–005 no toquen `public`.
- [ ] E2: contrato (`contrato.ts`), códigos de respuesta y que el usuario salga del JWT; que `confirmar-oferta` no cambió.
- [ ] E3: que el fotograma no se toma «por la última imagen» (`LectorOcr`, `TrazaAndroid`); que el lector no cambió (`guardia_parser.py`); qué falta para teléfono.
- [ ] E4: `banco_origen.csv` (0 humanos, 45 circulares); reglas de `veredictos`; que nada heredado o de IA quede «validado».
- [ ] E5: protocolo ciego (orden y huellas), reconciliación con `Banco.kt`, mutaciones 5/5, alcance real de la «ciega» (etiquetas heredadas).
- [ ] E6: acceso por rol, bitácora, datos que se devuelven.
- [ ] E7: definiciones del catálogo y la prueba de recálculo independiente; que «recall» y «acuerdo IA» no se presenten como exactitud.
- [ ] Riesgo de workflows al hacer merge (§12.2).
