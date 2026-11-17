# Auditoría de lecturas (E1–E2, y base de E4–E7)

Todo lo de esta carpeta está **preparado y probado en local**; nada se aplicó en Supabase ni se desplegó.

## Migraciones (`sql/`)
| # | Archivo | Etapa | Qué hace | Reversión |
|---|---|---|---|---|
| 001 | `001_auditoria.sql` | E1 | esquema `auditoria`: `lecturas`, `etapas`, `recepciones`, `version_esquema`, `purgar_vencidas()` | `001_auditoria.down.sql` (borra solo `auditoria`) |
| 002 | `002_registrar.sql` | E2 | `auditoria.registrar_lote()` idempotente + puerta `public.auditoria_registrar_lote` (solo `service_role`) | `002_registrar.down.sql` (conserva datos) |
| 003 | `003_veredictos.sql` | E4 | veredictos humano / IA / heredado / lector | `003_veredictos.down.sql` |
| 004 | `004_admin.sql` | E6 | administradores, bitácora de acciones, RPC del Admin | `004_admin.down.sql` |
| 005 | `005_metricas.sql` | E7 | definiciones y cálculo de métricas | `005_metricas.down.sql` |

Orden de aplicación: 001 → 005. Cada archivo es idempotente (se puede repetir). **Ninguna toca tablas existentes** (`errores_lector`, `revisiones`, `capturas_ok`…): la prueba `test_migraciones_repetidas_no_fallan_ni_cambian_datos_previos` compara el md5 de todas las tablas de `public` antes y después.

**Por qué fuera de `supabase/**`:** `supabase.yml` ejecuta `supabase/esquema.sql` al llegar a `main`. Estas migraciones solo se aplican a mano, con autorización, en el editor SQL (001 → 005).

## Diseño
- **Esquema privado `auditoria`:** no está en los esquemas que expone PostgREST y se revoca todo a `public`, `anon` y `authenticated`. Ni siquiera `service_role` escribe directo: solo por `registrar_lote` (probado).
- **Clave = id del evento (UUID del teléfono):** reintentar no duplica. Misma id + mismo contenido → `repetido`; otro contenido u otro usuario → `conflicto` sin sobrescribir.
- **Coherencia impuesta en la base** (y repetida en el contrato TS): semáforo solo con `resultado = oferta` y tarifa, km > 0, min > 0; `varias` sin tarifa/km/min (nunca se mezclan tarjetas); `captura_vinculo = exacto` exige `fotograma_id` y `captura_sha256`; lector `ocr` exige `fotograma_id`.
- **Privacidad:** no hay columnas para direcciones, nombres ni texto libre. Las líneas van **redactadas por lista blanca** (máx. 30 de 60 caracteres; solo piezas de dato) y su huella `lineas_sha256` del texto completo permite comparar sin guardar el texto. El servidor rechaza cualquier línea que no cumpla esa misma gramática (`lineaRedactada`), así que una dirección, un nombre, un correo, una URL o un número suelto no entran aunque el teléfono fallara.
- **Retención:** `expira` = recepción + 30 días; `purgar_vencidas()` existe pero **no está programada** (pg_cron requiere autorización).
- **Cascada:** borrar la cuenta (`auth.users`) borra sus lecturas, etapas y recepciones (eliminar-cuenta sigue funcionando).

## Contrato v1 (`supabase/functions/registrar-lectura/`)
`POST /functions/v1/registrar-lectura` con `Authorization: Bearer <JWT del conductor>` y cuerpo `{"contrato":1,"eventos":[…]}` (1–50 eventos, ≤ 64 KB). Campos por evento: ver `CAMPOS` en `contrato.ts` (tipos, rangos y requeridos). Campos desconocidos → `invalido`.

| Respuesta | Significado | El teléfono… |
|---|---|---|
| 200 `{ok:true, resultados:[{id, estado}]}` | `guardado` / `repetido` / `conflicto` / `invalido` por evento | quita de la cola todos menos ninguno (los 4 estados son definitivos) |
| 400 / 413 | forma del lote inválida | descarta el lote (no reintenta) |
| 401 | sin sesión válida | conserva la cola hasta tener sesión |
| 503 `reintentar:true` | base o auth no disponibles | reintenta con espera creciente |
| 503 `reintentar:false` | función desactivada (`AUDITORIA_ACTIVA` ≠ 1) | conserva la cola (acotada) sin reintentar en bucle |

El usuario sale **siempre del JWT** (nunca del cuerpo). Compatibilidad: las versiones anteriores de la app no llaman a esta función y `confirmar-oferta` no cambió (prueba `compatibilidad: confirmar-oferta no se modificó`).

## Pruebas
```
PG_BIN=/usr/lib/postgresql/17/bin python3 -m unittest discover -s auditoria/pruebas -p 'test_*.py' -v   # o …/16/bin (por defecto 16)
herramientas/probar_todo.sh DIR_LOGS      # todas las suites, en 16 y 17, con logs completos
node --experimental-strip-types --no-warnings --test auditoria/pruebas/e2/registrar.test.ts
```
`pruebas/pg_local.py` crea un clúster desechable con stubs de Supabase (roles `anon`/`authenticated`/`service_role`, `auth.users`, `auth.uid()` por `request.jwt.claims`, `storage`). Carga primero `supabase/esquema.sql` real con datos sintéticos.

## Limitaciones (no probado)
- **PostgreSQL local, no Supabase.** Desde la auditoría de cierre todas las pruebas SQL corren en PostgreSQL **16.15 y 17.12** locales (paquete oficial de apt.postgresql.org) con el mismo resultado. Eso valida la sintaxis y la semántica en el motor 17; **no** valida Supabase: roles reales, extensiones, privilegios por defecto del proyecto, PostgREST ni la versión menor exacta del proyecto.
- Los stubs no son Supabase: no prueban la exposición real de esquemas en PostgREST, el verificador de JWT ni `storage`.
- La función Deno no se ejecutó con Deno ni en Supabase: se probó su lógica con Node 22 (`manejador.ts` con dependencias simuladas) e `index.ts` solo se revisó.
- Aplicar 001–005 y desplegar `registrar-lectura` requiere autorización expresa del propietario.

## Cambios de la auditoría de cierre (16-nov-2026)
| Id | Etapa | Defecto | Corrección | Prueba |
|---|---|---|---|---|
| A1 | E2 | dos envíos simultáneos del mismo evento: el segundo recibía `invalido` | `unique_violation` → `repetido` / `conflicto` según lo guardado | `test_a1_fila_previa_sin_recepcion_no_es_invalido` |
| A2 | E3 | redacción dejaba pasar direcciones; multiplicador fuera de rango tumbaba el evento | lista blanca en teléfono y servidor | `test_e3_redaccion.py`, `TrazaTest` |
| A3 | E4 | un mismo revisor podía sumar dos votos (`referencia_doble` con una persona); la vista exigía igualdad exacta y la herramienta de etiquetado tolerancias | un voto por autor (el último), autores distintos, tolerancias de la guía en la vista; el Admin solo muestra referencia doble | `test_a3_*` |
| A4 | E5 | `comparar` no comprobaba que base y candidata usaran el mismo corpus, JVM y compilador | exige las dos `corrida.json` y se niega si difieren | `test_a4_comparar_exige_mismo_corpus_y_condiciones` |
| A5 | E6 | cualquier conductor autenticado podía inflar la bitácora con llamadas denegadas | negativas e inválidos: una anotación por minuto por persona y acción | `test_a5_un_conductor_no_puede_inflar_la_bitacora` |
| A6 | E6 | la biblioteca externa del Admin se cargaba sin huella (y de un `.min.js` generado por el CDN) | archivo real del paquete + SRI sha384 | `A6:` en `admin_ui.test.js` |
| A7 | E7 | la precisión contaba veredictos de un solo revisor como referencia | solo `referencia_doble`; métrica aparte `con_un_solo_revisor` | `test_e7_metricas.py` |
