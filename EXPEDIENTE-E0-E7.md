# Expediente integral E0–E7 — Checa

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
