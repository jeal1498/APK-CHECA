# ENTREGA E0.1 — Revisión

**Fecha:** 2026-11-15 · **Estado:** preparado y probado localmente con servidor simulado; revisión pendiente. **E0 NO está cerrado** (no existe respaldo real, copias ni restauración).

## 1. Alcance
Autorizado: auditar scripts existentes, hacer reproducible y seguro el procedimiento de respaldo 10.77, corregir defectos, dar instrucciones al propietario, definir artefactos y preparar la verificación de lo que entregue.
Fuera de alcance (no hecho): respaldo real, uso de llaves, cualquier acceso a Supabase, push, PR, merge, E1–E7, L1.

## 2. Estado y hallazgo principal
- **No había scripts existentes que auditar.** Búsqueda en `main`, `docs/seguimiento-maestro` y el contenedor: ninguno. Las ramas `trabajo/e1-e2-esquema`, `trabajo/e5-reproduccion`, `trabajo/l1-uber-est`, `trabajo/e4-veredictos` **no existen** en este entorno ni en GitHub (solo `main`, `docs/seguimiento-maestro` y la rama de sesión). Se escribió el procedimiento desde cero. *(Hecho verificado por ejecución.)*
- Único método previo documentado: `docs/banco-lector.md` §2 (desplegar una función temporal que usa `SERVICE_ROLE`). Contradice los límites de E0 → **no se usa**. Queda desplegada `tmp-capturas-9f3k` (stub 410 según la doc, **no verificado**); recomendación: que el propietario la quite del panel.
- La lista de 251 ids requeridos sale de `pruebas/lector/v77/resultados_haiku_v77.json` (251 ids únicos, 947–1380), sha256 `01716729…e679c`. *(Es la lista vigente del repositorio; el propietario debe confirmar que es la lista de «requeridos».)*

## 3. Archivos (todos nuevos salvo `SEGUIMIENTO-MAESTRO.md`)
`respaldo-e0/`: `README.md`, `comun.py`, `respaldar.py`, `verificar.py`, `evidencia_copias.py`, `verificar_entrega.py`, `referencia.sql`, `ids_requeridos_10.77.txt`, `.gitignore`, `pruebas/servidor_simulado.py`, `pruebas/test_e0.py`. Modificado: `SEGUIMIENTO-MAESTRO.md` (fila E0 y entrada de registro). Eliminados: ninguno. Tampoco se tocó nada de `app/`, `supabase/` ni `.github/`.

## 4. Diff / commits
Rama **local** `trabajo/e0-respaldo`, base `bedfa2483cdd433cd94d91188865ddf418b21e90` (`origin/docs/seguimiento-maestro`). SHA del commit: se informa en el mensaje de entrega (no puede incluirse en el propio commit). Diff completo: `git diff bedfa24..trabajo/e0-respaldo`; también en `ENTREGA-E0.patch` (sin versionar). **No está en GitHub** hasta que el propietario autorice el push.

## 5. Diseño (qué impone el código)
| Riesgo | Control | Probado por |
|---|---|---|
| Escritura/borrado accidental | Lista blanca de método+ruta; el código no contiene DELETE/PUT/PATCH | `test_solo_lectura_y_rutas_permitidas` |
| Llave enviada a otro host | URL fija `https://gqsadclvftkzwrihfipz.supabase.co`; sin redirecciones | `test_rechaza_url_ajena…`, `test_redireccion_no_reenvia_la_llave` |
| Llave equivocada/filtrada | Solo por variable de entorno; exige rol `service_role`/`sb_secret_`; errores y archivos sin la llave | `test_la_llave_no_aparece…`, `test_clave_incorrecta_401` |
| Respaldo dentro del repo / sobre datos previos | Rechaza carpeta en repo git y carpeta no vacía | `test_salida_dentro_de_un_repo…` |
| Listado incompleto | Paginación hasta página vacía, recursión en carpetas, duplicados abortan | `test_paginacion_y_carpetas` |
| Archivo corrupto o HTML con 200 | Tamaño = listado, cabecera WebP, sha256, md5 vs eTag (best-effort) | `test_html_con_200…`, `test_descarga_truncada…`, `test_etag…` |
| Expiración durante el respaldo (`limpiarCapturas`, 7 días) | Listado T0/T1, desaparecidos registrados; requerido ausente = INCOMPLETO | `test_requerido_que_desaparece…` |
| Evidencia manipulada | `verificar.py` recalcula todo desde disco; 9 alteraciones de prueba | `TestMutaciones` |
| Referencia circular | `referencia.sql` calcula huellas con otra vía (SQL) | ver §6 |
| Restauración no ligada a B | `restaurar` hashea el archivo B y el tar descifrado; `registrar` los cruza | `test_restauracion_etiquetada_B…` |
| Tar malicioso | Extracción validada (sin `..`, rutas absolutas, enlaces) | `test_tar_malicioso…` |

## 6. Pruebas (comandos y resultados reales)
- `cd respaldo-e0 && python3 -W ignore -m unittest discover -s pruebas` → **Ran 49 tests … OK** (Python 3.13.18, age 1.1.1, gpg 2.4.4, Linux). ~35 s.
- Mutaciones sobre copia temporal (cada una debía hacer fallar la suite): seguir redirecciones, no validar WebP, no comparar sha256, `E06b` siempre ok, aceptar llave `anon`, no paginar, no ligar restauración a B. **Resultado inicial: 5 detectadas, 2 sobrevivieron** (`E06b` y ligado a B: las pruebas pasaban por otra comprobación). Se agregaron `test_lista_de_ids_del_repo_con_un_id…` y `test_restauracion_etiquetada_B…`; repetidas esas dos: detectadas.
- `referencia.sql` ejecutada en PostgreSQL 16 local (tablas simuladas `storage.objects`, `errores_lector`, `capturas_ok`): `huella_listado` `d95e8ecc…`, `huella_ids_errores_lector` `b9bc07e8…`, `…_10_77` `8c03336f…` **idénticas** a `comun.py` con los mismos datos (orden `collate "C"` = orden por bytes).
- Demostración por CLI (proceso real contra el servidor simulado): `respaldar.py` → `Resultado: COMPLETO. 149 capturas`; `verificar.py` → `APROBADO_CON_ADVERTENCIAS` solo por `R00` (sin referencia). La llave simulada no apareció en stdout/stderr.

## 7. Regresión
No aplica: no se modificó código existente de Checa. `SEGUIMIENTO-MAESTRO.md` solo recibió texto.

## 8. Seguridad y privacidad
- El respaldo completo contiene datos personales (UUID de usuario, líneas leídas, direcciones en imágenes); queda en el equipo del propietario, fuera del repo. Se comprobó por prueba que **resumen.json, manifiesto.csv, VERIFICACION.json y EVIDENCIA-COPIAS.json no contienen** llaves, correos ni UUID (patrón en `comun.RE_SECRETOS`); el manifiesto lleva el nombre del objeto (id numérico, color/tarifa/km/min/hora en las normales) y la versión.
- Búsqueda de secretos en los archivos nuevos: solo aparecen los prefijos literales que el código usa para reconocer llaves; ninguna llave.
- Workflows: todos los de `.github/workflows` disparan con push a `main`; `play-compilar.yml` también con PR a `main` pero solo por `app/**`. Un push de `trabajo/e0-respaldo` **no activaría ninguno**; un PR a `main` tampoco (solo toca `respaldo-e0/` y `SEGUIMIENTO-MAESTRO.md`). Aun así, push/PR requieren autorización.

## 9. Limitaciones (qué NO se probó)
1. **Nada contra Supabase real.** Supuestos no verificados: forma exacta del listado (`metadata.size`, `eTag`), que la ausencia de un objeto devuelva 404 o 400 (ambos tratados como «desaparecido»), el límite de 1,000 filas del SQL Editor, `storage.objects.metadata->>'size'`, PostgREST `Range`/`Content-Range`.
2. PostgreSQL 16 local, no 17 de Supabase; datos simulados.
3. Windows no probado (rutas, `shred`, variables de entorno): instrucciones escritas, sin ejecutar.
4. El script no demuestra independencia física de los medios ni conservación: son **declaraciones** del propietario.
5. No valida que las capturas sean «correctas» para el banco, solo su integridad.
6. Si hay subidas/borrados entre el respaldo y la consulta SQL se reportan como advertencias, no se reconcilian.
7. Un solo hilo; con ~740 archivos ≤ 500 KB se estima 5–15 min (estimación, no medida).

## 10. Riesgos y reversión
Riesgo operativo principal: **tiempo** — expiración desde el 20-11. Reversión: no hay nada que revertir (solo lectura; carpeta nueva). Si el propietario sospecha filtración de llave, rotarla en Supabase (decisión suya). No borrar el respaldo hasta cerrar E0.

## 11. Puerta de aceptación (E0)
| Criterio | Estado |
|---|---|
| Scripts auditados/reproducibles y seguros | Cumplido localmente (simulado) |
| Instrucciones al propietario + artefactos definidos | Cumplido (`README.md`) |
| Respaldo íntegro de objetos requeridos | **Pendiente** (lo ejecuta el propietario) |
| Ids requeridos presentes (lista vigente) | **Pendiente** |
| Manifiesto/hashes verifican | **Pendiente** |
| Conteos/huella vs consulta del mismo momento | **Pendiente** |
| Dos copias cifradas independientes | **Pendiente** |
| Restauración de prueba desde B | **Pendiente** |
| Propietario confirma que conserva copias | **Pendiente** |

**Decisiones solicitadas al propietario:** (a) autorizar el push de `trabajo/e0-respaldo` para poder bajar los scripts, o indicar otro canal; (b) autorizar el cuarto artefacto `EVIDENCIA-COPIAS.json` (necesario para verificar copias y restauración; la lista original solo nombra tres); (c) confirmar que los 251 ids de `resultados_haiku_v77.json` son los «requeridos».

## Material para ChatGPT
Este informe, `git diff bedfa24..trabajo/e0-respaldo` (o `ENTREGA-E0.patch`), `respaldo-e0/` completo y `SEGUIMIENTO-MAESTRO.md`. Pedir revisión de: lógica de `respaldar.py` (§5), supuestos de §9.1, y si el cuarto artefacto es aceptable.
