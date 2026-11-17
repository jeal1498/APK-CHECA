-- E1 · 001 — Esquema de auditoría de lecturas (PREPARADO, NO APLICADO EN SUPABASE).
-- Idempotente: se puede ejecutar varias veces sin error ni cambios. No modifica ni borra tablas existentes
-- (errores_lector, revisiones, capturas_ok… quedan intactas). Reversión: 001_auditoria.down.sql.
-- Vive fuera de supabase/** a propósito: supabase.yml despliega supabase/** al llegar a main.
--
-- Esquema `auditoria`: NO está en los esquemas expuestos de PostgREST (solo `public`), así que ni anon ni
-- authenticated llegan por la API aunque tuvieran permisos; además se revocan todos los permisos.
-- Solo escribe `service_role` (la función registrar-lectura) a través de auditoria.registrar_lote().

create schema if not exists auditoria;
revoke all on schema auditoria from public, anon, authenticated;
grant usage on schema auditoria to service_role;

-- Versión del esquema de auditoría (una fila por migración aplicada).
create table if not exists auditoria.version_esquema (
  version     integer primary key,
  descripcion text not null,
  aplicada    timestamptz not null default now()
);

-- Una fila por evento de lectura (cada vez que el lector produjo un resultado sobre un fotograma).
-- `id` lo genera el teléfono (UUID v4) → idempotencia: reintentar el mismo evento no duplica.
create table if not exists auditoria.lecturas (
  id               uuid primary key,
  usuario          uuid references auth.users (id) on delete cascade,
  contrato         smallint not null check (contrato = 1),
  app              text not null check (app in ('uber', 'didi', 'otra')),
  version_app      text not null check (version_app ~ '^\d+\.\d+(\.\d+)?$'),
  version_parser   text not null check (length(version_parser) between 1 and 64),
  lector           text not null check (lector in ('texto', 'ocr', 'ia')),
  fotograma_id     uuid,                        -- el fotograma exacto que se leyó (null si el lector fue «texto»)
  captura_sha256   text check (captura_sha256 ~ '^[0-9a-f]{64}$'),
  captura_vinculo  text not null default 'sin_captura'
                   check (captura_vinculo in ('exacto', 'sin_captura')),
  ts_fotograma     timestamptz,
  ts_evento        timestamptz not null,
  resultado        text not null check (resultado in
                   ('oferta', 'no_oferta', 'duplicada', 'abstencion', 'varias', 'descartada', 'timeout', 'error')),
  motivo           text check (length(motivo) <= 64),
  tarifa           numeric(10, 2) check (tarifa > 0 and tarifa < 100000),
  km               numeric(8, 2)  check (km >= 0 and km < 1000),
  min              numeric(8, 1)  check (min >= 0 and min < 1440),
  rec_km           numeric(8, 2)  check (rec_km >= 0 and rec_km < 1000),
  rec_min          numeric(8, 1)  check (rec_min >= 0 and rec_min < 1440),
  multi            numeric(4, 2)  check (multi >= 1 and multi < 20),
  moneda           text check (moneda ~ '^[A-Z]{3}$'),
  nivel            text check (nivel in ('verde', 'amarillo', 'rojo')),
  tarjetas         smallint check (tarjetas between 0 and 20),
  ms_lectura       integer check (ms_lectura between 0 and 600000),
  ms_popup         integer check (ms_popup between 0 and 600000),
  lineas_sha256    text check (lineas_sha256 ~ '^[0-9a-f]{64}$'),   -- huella de TODAS las líneas originales
  lineas_redactadas jsonb check (jsonb_typeof(lineas_redactadas) = 'array' and jsonb_array_length(lineas_redactadas) <= 30),
  telefono         text check (length(telefono) <= 64),
  android          text check (length(android) <= 8),
  orientacion      text check (orientacion in ('vertical', 'horizontal')),
  recibido         timestamptz not null default now(),
  expira           timestamptz not null default (now() + interval '30 days'),
  -- Semáforo solo con oferta y datos suficientes; sin oferta no hay semáforo ni tarifa (no se inventan datos).
  constraint lecturas_oferta_completa check (
    resultado <> 'oferta' or (tarifa is not null and km is not null and km > 0 and min is not null and min > 0 and nivel is not null)),
  constraint lecturas_sin_semaforo check (resultado = 'oferta' or nivel is null),
  constraint lecturas_varias_sin_datos check (resultado <> 'varias' or (tarifa is null and km is null and min is null)),
  constraint lecturas_captura_exacta check (
    captura_vinculo <> 'exacto' or (fotograma_id is not null and captura_sha256 is not null)),
  constraint lecturas_lector_foto check (lector <> 'ocr' or fotograma_id is not null),
  constraint lecturas_tiempo check (ts_fotograma is null or ts_fotograma <= ts_evento + interval '5 seconds')
);
create index if not exists lecturas_usuario_ts on auditoria.lecturas (usuario, ts_evento desc);
create index if not exists lecturas_ts on auditoria.lecturas (ts_evento desc);
create index if not exists lecturas_version_app on auditoria.lecturas (version_app, app);
create index if not exists lecturas_expira on auditoria.lecturas (expira);
create unique index if not exists lecturas_fotograma_resultado
  on auditoria.lecturas (fotograma_id, app) where fotograma_id is not null and resultado = 'oferta';

-- Etapas de cada lectura (captura → ocr → parser → dedupe → popup → revisión) con su motivo.
create table if not exists auditoria.etapas (
  lectura_id uuid not null references auditoria.lecturas (id) on delete cascade,
  orden      smallint not null check (orden between 0 and 31),
  etapa      text not null check (etapa in ('captura', 'ocr', 'texto', 'tarjetas', 'parser', 'dedupe', 'popup', 'revision', 'envio')),
  estado     text not null check (estado in ('ok', 'fallo', 'omitida', 'timeout')),
  ms         integer check (ms between 0 and 600000),
  motivo     text check (length(motivo) <= 64),
  primary key (lectura_id, orden)
);

-- Recepción idempotente: guarda la huella del payload de cada evento. Mismo id + misma huella = reintento
-- (se ignora); mismo id + otra huella = conflicto (se rechaza, no se sobrescribe).
create table if not exists auditoria.recepciones (
  lectura_id   uuid primary key references auditoria.lecturas (id) on delete cascade,
  payload_sha256 text not null check (payload_sha256 ~ '^[0-9a-f]{64}$'),
  intentos     integer not null default 1,
  ultima       timestamptz not null default now()
);

-- Ningún rol de cliente toca el esquema; service_role solo por la función.
alter table auditoria.lecturas enable row level security;
alter table auditoria.etapas enable row level security;
alter table auditoria.recepciones enable row level security;
alter table auditoria.version_esquema enable row level security;
revoke all on all tables in schema auditoria from public, anon, authenticated;

-- Purga de lo vencido (30 días). Definida, NO programada: activar pg_cron requiere autorización expresa.
create or replace function auditoria.purgar_vencidas(limite integer default 10000) returns integer
language plpgsql security definer set search_path = auditoria, pg_temp as $$
declare n integer;
begin
  with v as (select id from auditoria.lecturas where expira < now() order by expira limit limite)
  delete from auditoria.lecturas l using v where l.id = v.id;
  get diagnostics n = row_count;
  return n;
end $$;
revoke all on function auditoria.purgar_vencidas(integer) from public, anon, authenticated;

insert into auditoria.version_esquema (version, descripcion)
values (1, 'lecturas, etapas y recepciones (E1)') on conflict (version) do nothing;
