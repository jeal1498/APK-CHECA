# E4 — Veredictos humanos e IA

**Estado:** implementado y probado en local. **Sin referencia humana todavía:** nadie ha etiquetado; no se inventó ninguna etiqueta. Bloqueo real del propietario (ver abajo).

## Problema del banco de 316
`etiquetado/origen_banco.py` lee la cabecera de cada caso y escribe `etiquetado/banco_origen.csv`:

| Origen de la «respuesta esperada» | Casos | ¿Sirve como verdad? |
|---|---|---|
| Revisión de IA juzgada correcta (`ia_bien`) | 177 | No: juicio de IA sobre IA |
| Captura leída por Haiku 5.5 (14 nov) | 81 | No: lectura de IA de la imagen |
| **Lo que leyó el propio lector** (`ia_mal`) | **45** | **No, y es circular**: mide al lector contra sí mismo |
| Revisión de las líneas (13 nov) | 13 | No: no hay constancia de revisión humana sobre la imagen |
| **Humano verificado contra la captura** | **0** | — |

Consecuencia: «271/316 (85.8 %)» es **acuerdo con etiquetas heredadas**, no exactitud. Los 45 circulares la inflan.

## Modelo (`sql/003_veredictos.sql`)
- `auditoria.veredictos`: `fuente` = `humano` / `ia` / `heredado`; `autor` (seudónimo o modelo), `revisor_version`, `guia_version`, `evidencia_sha256` (la captura que se miró), `confianza`, `estado`.
- Reglas en la base: solo un **humano** puede dejar un veredicto `validado`, y solo con captura (`evidencia_sha256`) y guía; IA y heredado nunca se validan; un veredicto no se edita ni se borra (se agrega otro con `sustituye`).
- **Precedencia** (`auditoria.veredicto_vigente`): humanos validados que coinciden → `referencia_humana` / `referencia_doble`; que no coinciden → `conflicto_humano` (no se fuerza); sin humano → `solo_ia`; solo heredado → `heredado_no_revisado`.
- **Importación** de `errores_lector.veredicto` como `heredado` (`origen_migracion = '003'`), idempotente y reversible; la tabla original no se toca (probado con md5 de `public`).
- El resultado del lector vive en `auditoria.lecturas` (o `errores_lector.local`); nunca en `veredictos`.

## Herramientas (`etiquetado/`)
- `GUIA.md` (v1.0): criterios, ejemplos, doble revisión y resolución de desacuerdos.
- `etiquetado.py hoja`: muestra estratificada (app × origen, semilla fija) de casos **con captura en el respaldo E0**; hoja ciega por revisor con el sha256 de cada captura.
- `etiquetado.py importar`: valida filas (captura = manifiesto), acuerdo por campo y **kappa** de la clase, `referencia_humana.csv`, `conflictos.csv` y `veredictos_humanos.sql`.
- `metricas.py`: precisión de ofertas, exactitud, tasa de falsos positivos y negativos, abstenciones, con n e intervalo de Wilson; `n < 30` = insuficiente. El acuerdo entre revisores se reporta aparte de la exactitud.

## Calibración y contaminación
- Los 316 casos ya se usaron para **desarrollar** el lector (sus fallas guiaron cada arreglo): son conjunto de desarrollo, no de evaluación.
- Evaluación ciega real (E5) = casos **nuevos** etiquetados por humanos que no se hayan usado para ajustar el lector. Los 21 ids 10.77 posteriores al 14-nov y las capturas normales `-n<id>` de la 10.80 son candidatos, si el respaldo E0 los conserva.
- Sesgo de selección: las capturas dudosas solo existen donde hubo diferencia, rescate o desacuerdo; las normales, hasta 100 por conductor. Ninguna muestra representa todas las ofertas.
- No se mandan capturas a proveedores de IA desde estas herramientas.

## Pruebas
`test_e4_veredictos.py` (13, Postgres local) y `test_e4_etiquetado.py` (13). Comandos en `auditoria/README.md`.

## Bloqueo del propietario
1. Ejecutar el respaldo E0 (las capturas 10.77 expiran desde el **20-nov**; sin respaldo no hay qué etiquetar).
2. Dos personas etiquetan la hoja (`etiquetado.py hoja --manifiesto <respaldo>/manifiesto.csv`), por separado.
3. Correr `importar` y compartir solo `acuerdo.json`, `referencia_humana.csv` y `conflictos.csv` (sin imágenes).
