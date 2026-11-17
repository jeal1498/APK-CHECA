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
- **Precedencia** (`auditoria.veredicto_vigente`, corregida en la auditoría de cierre; el 17-nov se agregó `referencia_adjudicada`: una discrepancia resuelta por consenso o por una tercera persona cuenta como referencia, sin pasar por doble ciega y con los datos de la adjudicación): ≥ 2 revisores humanos **distintos** validados que coinciden (tolerancias de la guía) → `referencia_doble`, la única que usan las métricas; un solo revisor → `referencia_humana` (todavía no es referencia); que no coinciden → `conflicto_humano` (no se fuerza); sin humano → `solo_ia`; solo heredado → `heredado_no_revisado`. De cada autor cuenta solo su último veredicto.
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

## Decisiones del 16-nov (etiquetado también sin el respaldo E0)
- `etiquetado.py hoja|importar --manifiesto` acepta la **carpeta de capturas** «<id>.webp» que se baja para la revisión del banco; calcula el sha256 de cada imagen y la importación rechaza una fila si la captura cambió después de etiquetar (`test_sin_respaldo_e0_basta_una_carpeta_de_capturas`).
- Lo que lea Haiku sobre esas capturas se registra como `fuente = 'ia'` con `revisor_version` (modelo y prompt). **No** cuenta como referencia: `veredicto_vigente` lo deja en `solo_ia`.
- La muestra humana mínima recomendada: 120 casos con semilla fija, dos revisores; con menos de 30 por segmento las métricas salen `insuficiente`.

## Primera doble revisión real (17-nov-2026)
Dos revisores (`lalo` y `pp`) etiquetaron a ciegas las mismas 66 capturas de la 10.77 (`evidencias/etiquetado/20261117/`): **66 comparadas, 40 coinciden (61 %)**, 26 discrepan; acuerdo en la clase 83 % (kappa 0.68), en tarifa 97 %, en km y min 84 %. 5 de las 26 son solo «tarjeta vacía contra 1» (convención poco clara, corregida en la guía). Pendiente: resolver las 26 (`resolver.html`). Hasta entonces la referencia son las 40 coincidencias.

### Resolución (17-nov-2026)
Se resolvieron las 26 discrepancias (la 1365, oferta tarjeta 2, la decidió el propietario) con `resolver.html` (seudónimo `concenso`, método registrado `tercera_persona`; falta confirmar si fue consenso de los dos o una tercera persona). Referencia final: **66 de 66** (40 doble ciego + 26 adjudicadas, método tercera persona confirmado). Archivos en `evidencias/etiquetado/20261117/final/`. Las adjudicadas no cuentan como doble ciego.

### Cierre de la medición (17-nov-2026)
Lector contra la referencia humana (solo 19 de 66 casos con líneas locales; n < 30, insuficiente): 16 aciertos, 3 fallas. Arregladas 1377 (tramos idénticos seguidos) y 1324 (tarjeta cortada arriba); banco 272/316. 1300 (tarjeta atenuada con círculo de carga) no se puede detectar desde el texto: limitación conocida. Sin probar en teléfono ni en CI de Android. Los otros 47 casos requieren las líneas de `errores_lector` (producción o rama de pruebas de Supabase, sin crear). Evidencia: `evidencias/medicion/lector_vs_humano_20261117.json`.
