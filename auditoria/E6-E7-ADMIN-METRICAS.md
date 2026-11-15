# E6 — Checa Admin MVP · E7 — Dashboard y métricas

**Estado:** implementado y probado en local (Postgres 16 con stubs de Supabase; página con Playwright y un cliente simulado). **No desplegado**, sin datos reales, sin administradores dados de alta.

## E6 — Admin (`admin/` + `sql/004_admin.sql`)
- **Vistas:** Inicio (volumen, resultados, ingesta, reintentos, fallas de IA, alertas), Errores (`errores_lector` filtrable), Ofertas (eventos de E3 con etapas, veredicto y referencia humana), Métricas (E7) y Bitácora.
- **Acceso:** página estática con sesión de Google (Supabase Auth). Toda consulta pasa por `public.admin_*` (`security definer`): sin fila en `auditoria.administradores` → `{"error":"no_autorizado"}` y queda en la bitácora. `anon` no puede ejecutarlas. Ningún rol de cliente lee las tablas directamente.
- **Roles:** `lector` (consulta) y `revisor` (además registra veredictos humanos con la huella de la captura vista, `admin_veredicto`). El alta la hace el propietario a mano en SQL.
- **Bitácora** `auditoria.acciones_admin`: acción, filtros, filas, resultado (`ok`/`denegado`/`invalido`), con **seudónimo** (8 hex del md5), sin uuid ni correo; no se edita ni se borra.
- **Protección de datos:** se devuelven solo números de la lectura y de la IA (sin `usuario`, sin `lineas`, sin imágenes); el conductor aparece como seudónimo. La captura se indica como «sí (acceso restringido)»: verla requiere un flujo aparte con aprobación de privacidad (no incluido).
- **Límites:** rango ≤ 92 días, `por_pagina` ≤ 100, `pagina` ≤ 1000, valores de filtros cerrados; los filtros son parámetros (probado contra inyección).
- **Página:** CSP estricta (solo `self` y jsDelivr para supabase-js 2.45.4, conexión solo al proyecto), sin `innerHTML` con datos, sin almacenamiento en el navegador, `config.js` sin llave (la pone el propietario).

## E7 — Métricas (`sql/005_metricas.sql`)
Catálogo `auditoria.metricas_def`: cada métrica con fórmula, población, fuente y exclusiones. `auditoria.metricas()` devuelve por cada una `k`, `n`, `valor` y `estado`.

| Clave | Grupo | Fórmula | Nota |
|---|---|---|---|
| `lecturas`, `ofertas_mostradas` | operación | conteos en la ventana | volumen detectado, **no** cobertura |
| `duplicadas` | operación | duplicada / (oferta + duplicada) | |
| `abstenciones` | operación | (abstención + varias) / (oferta + abstención + varias) | |
| `captura_exacta` | operación | captura exacta / ofertas | |
| `ms_popup_p50`, `ms_popup_p90` | operación | percentiles de `ms_popup` | redondeo mitad hacia arriba |
| `reintentos` | operación | Σ(intentos − 1) | salud de la ingesta |
| `con_referencia_humana` | calidad | ofertas con referencia humana / ofertas | cobertura del etiquetado |
| `precision_humana` | calidad | correctas / con referencia (tarifa ±0.01, km ±0.05, min ±0.5) | excluye conflictos, ilegibles, cambio de pantalla |
| `falsos_positivos_humana` | calidad | referencia `no_oferta` / con referencia | |
| `mezcla_tarjetas_humana` | calidad | referencia `varias` / con referencia | |
| `recall` | calidad | **no medible** | la telemetría no registra ofertas reales no vistas |
| `conflictos_humanos` | calidad | casos en conflicto | |
| `acuerdo_ia_lector` | acuerdo IA | coincide / revisadas (`revisiones`) | rotulado «NO es exactitud» |
| `banco_heredado` | banco | última fila de `banco_lector` | rotulado «etiquetas NO humanas» |

Estados: `sin_datos` (n = 0, valor nulo), `insuficiente` (n < 30), `suficiente`, `no_medible` (con motivo). Segmentos app × versión × fabricante con su n y marca de muestra pequeña; tendencia diaria sin atribuir causas. Los grupos se muestran por separado en el panel.

## Pruebas
- `test_e6_admin.py` (11): anon sin permiso; no-admin denegado y registrado; sin lectura directa de tablas; lector vs revisor; Inicio cuadra con las tablas; filtros y paginación sin repetidos; sin datos personales; límites; inyección; bitácora inalterable; reversión.
- `test_e7_metricas.py` (8): **cada métrica se recalcula en Python desde los mismos datos y coincide** (total, Uber y DiDi); recall no medible; IA no cuenta como referencia; segmentos y tendencia suman el total; definiciones completas; acceso y bitácora; sin datos no inventa valores; reversión.
- `e6/admin_ui.test.js` (8, Playwright/Chromium): sin sesión no consulta; vistas; filtros llegan a la función; paginación; texto con `<img onerror>` no se ejecuta; avisos de no autorizado, error y red caída; sin scroll horizontal a 390 px; sin llaves en los archivos.

## No probado / pendiente
1. Supabase real: Auth de Google, exposición de funciones por PostgREST, rendimiento con volumen real (índices definidos, no medidos).
2. La página no se publicó; necesita la llave anon en `config.js`, el SRI de supabase-js y un lugar de alojamiento aprobado.
3. Acceso a las imágenes desde el Admin (requiere política de privacidad y diseño aparte).
4. Con cero etiquetas humanas, todas las métricas de calidad saldrán `sin_datos` hasta completar E4.
