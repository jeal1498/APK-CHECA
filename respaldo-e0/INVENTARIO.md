# E0 — Reconciliación del inventario: **no aplica** (E0 descartada por el propietario el 16-nov-2026)

Se conserva el análisis para que conste por qué nunca se reconcilió y qué haría falta si se retoma.

## Qué hay como evidencia
| Fuente | Qué dice | Valor como evidencia |
|---|---|---|
| `ids_requeridos_10.77.txt` (251 ids, sha256 `01716729…e679c`) | ids de `errores_lector` 10.77 que tenían captura el 14-nov, derivados de `pruebas/lector/v77/resultados_haiku_v77.json` | lista del repositorio; nunca confirmada por el propietario como «la lista de requeridos» |
| `evidencias/e0/COMPARACION-IDS.md` (consulta del 15-nov 21:16 UTC) | 773 objetos, 272 ids 10.77 con captura = 251 + 21 | **reportado por una herramienta, sin autorización específica**; un instante; no reproducible por el revisor. **No se repitió** |
| Respaldo real (`manifiesto.csv`, `resumen.json`, `VERIFICACION.json`) | — | **no existe** |
| CSV de `referencia.sql` exportados por el propietario | — | **no existen** |

## Por qué no se puede reconciliar hoy
No hay ninguna evidencia local **y autorizada** del contenido del almacén. La única observación (251 + 21, 773) es de un instante pasado y no autorizada; desde entonces el almacén sigue recibiendo y borrando (limpieza de 7 días, al azar en ~2 % de las llamadas; las primeras capturas son del 13-nov y empiezan a caducar el **20-nov**). No se excluyó ni se añadió ningún id para hacer cuadrar nada: la lista sigue en 251.

## Cómo queda reconciliado al hacer el respaldo (ya implementado, sin consultas mías)
1. `respaldar.py` guarda **todo** el almacén y las dos tablas, no solo los requeridos.
2. `verificar.py` con los CSV de `referencia.sql` del mismo momento: conteos y huellas contra la base (R01–R08), los 251 presentes y con fila 10.77 (V08b/V08c) y, nuevo, **V08d**: lista de capturas 10.77 respaldadas que no están en la lista (`ids_10_77_no_listados` en `VERIFICACION.json`; `verificar_entrega.py` E06c).
3. Decisión del propietario, por escrito, **sin cambiar la lista después del respaldo**: el respaldo se hace con la lista de 251 (queda anotada con su sha256 en `resumen.json`); los ids 10.77 adicionales que informe V08d ya están respaldados, y el propietario declara si los acepta como parte del banco histórico. Cambiar `ids_requeridos_10.77.txt` después haría fallar V08a/E06a a propósito (la lista usada no sería la del repositorio): si se quiere una lista ampliada como «requerida», hay que fijarla **antes** de respaldar.
4. Ids requeridos que ya hubieran expirado se documentan como **pérdida** (`INCOMPLETO`), no se quitan de la lista.

## Qué falta exactamente para cerrar E0
- [ ] Respaldo real ejecutado por el propietario (`README.md`, pasos 1–3).
- [ ] CSV de referencia del mismo momento y `verificar.py` APROBADO (o con advertencias explicadas, incluida V08d).
- [ ] Dos copias cifradas distintas, restauración desde B y comprobación de A (`EVIDENCIA-COPIAS.json` = COMPLETA).
- [ ] Decisión escrita sobre la lista (251 o ampliada) y declaración de conservación de las copias.
- [ ] Los 4 artefactos sanitizados entregados y `verificar_entrega.py` = APROBADO.
