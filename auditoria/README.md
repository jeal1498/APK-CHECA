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
- **Privacidad:** no hay columnas para direcciones, nombres ni texto libre. Las líneas van **redactadas** (máx. 30 de 60 caracteres, solo montos/tramos) y su huella `lineas_sha256` del texto completo permite comparar sin guardar el texto. El servidor rechaza líneas con forma de correo, teléfono o URL.
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
python3 -m unittest discover -s auditoria/pruebas -p 'test_*.py' -v      # Postgres local 16 (migraciones, RLS, E4, E6, E7)
node --experimental-strip-types --no-warnings --test auditoria/pruebas/e2/registrar.test.ts
```
`pruebas/pg_local.py` crea un clúster desechable con stubs de Supabase (roles `anon`/`authenticated`/`service_role`, `auth.users`, `auth.uid()` por `request.jwt.claims`, `storage`). Carga primero `supabase/esquema.sql` real con datos sintéticos.

## Limitaciones (no probado)
- **PostgreSQL 16 local, no 17 de Supabase** (no se permitió bajar binarios de 17 en este entorno). Nada usa funciones nuevas de 17, pero no está validado allí.
- Los stubs no son Supabase: no prueban la exposición real de esquemas en PostgREST, el verificador de JWT ni `storage`.
- La función Deno no se ejecutó con Deno ni en Supabase: se probó su lógica con Node 22 (`manejador.ts` con dependencias simuladas) e `index.ts` solo se revisó.
- Aplicar 001–005 y desplegar `registrar-lectura` requiere autorización expresa del propietario.
